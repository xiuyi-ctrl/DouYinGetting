"""Desktop-only export. Database plaintext and keys stay in process memory."""
import time, sqlite3, json, struct, io, hashlib, zipfile
from pathlib import Path
from urllib.parse import urlparse, urlsplit, urlunsplit
from urllib.request import Request, build_opener, HTTPSHandler, HTTPRedirectHandler
from vendor.memory import ProcessMemoryReader, ProcessScan, get_weixin_pids, PROCESS_QUERY_INFORMATION, PROCESS_VM_READ
from vendor.decrypt import detect_page_reserve, decrypt_page
from vendor.exporter import detect_format, aes_decrypt_variants
from PIL import Image

MEDIA_HOSTS = ('vweixinf.tc.qq.com', 'wxapp.tc.qq.com', 'mmbiz.qpic.cn')
def checked_url(url):
    parts=urlsplit(url)
    if parts.hostname not in MEDIA_HOSTS or parts.username or parts.password or parts.port not in (None,80,443):
        raise ValueError('不支持的媒体地址')
    if parts.scheme not in ('http','https'): raise ValueError('不支持的媒体协议')
    return urlunsplit(('https',parts.netloc.removesuffix(':80'),parts.path,parts.query,''))
class MediaRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return super().redirect_request(req,fp,code,msg,headers,checked_url(newurl))
def download(url,timeout=12):
    with build_opener(MediaRedirect(),HTTPSHandler()).open(Request(checked_url(url),headers={'User-Agent':'Mozilla/5.0'}),timeout=timeout) as response:
        data=response.read(32*1024*1024+1)
        if len(data)>32*1024*1024: raise ValueError('媒体文件过大')
        return data

def export_stickers(db, output, progress=lambda done,total: None):
    db=Path(db)
    if db.name != 'emoticon.db' or not db.is_file(): raise ValueError('请选择 emoticon.db 表情库')
    for attempt in range(3):
        wal_path = Path(str(db)+'-wal')
        wal = wal_path.read_bytes() if wal_path.exists() else b''
        raw = db.read_bytes()
        if not wal_path.exists() or wal == wal_path.read_bytes():
            break
    else:
        raise RuntimeError('Database changing; try again after synchronization')

    def checksum(data, seed, endian):
        a,b=seed
        words=struct.unpack(endian+str(len(data)//4)+'I',data)
        for n in range(0,len(words),2):
            a=(a+words[n]+b)&0xffffffff
            b=(b+words[n+1]+a)&0xffffffff
        return a,b

    frames=[]
    last_commit=0
    db_pages=len(raw)//4096
    if wal:
        magic,version,page_size=struct.unpack('>III',wal[:12])
        if magic not in (0x377f0682,0x377f0683) or page_size !=4096:
            raise RuntimeError('Unsupported WAL header')
        endian='<' if magic==0x377f0682 else '>'
        seed=checksum(wal[:24],(0,0),endian)
        if seed!=struct.unpack('>II',wal[24:32]):
            raise RuntimeError('Invalid WAL checksum')
        for offset in range(32,len(wal)-4119,4120):
            header=wal[offset:offset+24]
            page=wal[offset+24:offset+4120]
            if header[8:16]!=wal[16:24]:
                break
            seed=checksum(header[:8]+page,seed,endian)
            if seed!=struct.unpack('>II',header[16:24]):
                break
            number,commit=struct.unpack('>II',header[:8])
            if not number:
                break
            frames.append((number,page))
            if commit:
                last_commit=len(frames)
                db_pages=commit
        current=bytearray(raw)
        current.extend(b'\0'*max(0,db_pages*4096-len(current)))
        for number,page in frames[:last_commit]:
            if number<=db_pages:
                current[(number-1)*4096:number*4096]=page
        raw=bytes(current[:db_pages*4096])
    reader = ProcessMemoryReader()
    for pid in get_weixin_pids():
        handle = reader.open_process(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, False, pid)
        if not handle:
                continue
        result = ProcessScan()
        try:
            found = reader.scan_wcdb_config_key(pid, handle, raw[:4096], time.monotonic()+20, set(), result)
        finally:
            reader.close_handle(handle)
        if not found:
            continue
        key = bytes.fromhex(next(iter(result.keys)))
        reserve = detect_page_reserve(raw[:4096], key)
        plain = b''.join(decrypt_page(raw[i:i+4096], i//4096, key, reserve) if any(raw[i:i+4096]) else raw[i:i+4096] for i in range(0,len(raw),4096))
        # The private in-memory copy has no WAL sidecar. Use rollback-journal
        # header flags only in this copy so SQLite does not seek a disk WAL.
        plain = bytearray(plain)
        plain[18:20] = b'\x01\x01'
        con = sqlite3.connect(':memory:')
        con.deserialize(plain)
        integrity=con.execute('pragma quick_check').fetchone()[0]
        if integrity!='ok':
            raise RuntimeError('Integrity check failed')
        con.row_factory=sqlite3.Row
        rows=con.execute('select md5,aes_key,cdn_url,extern_url,encrypt_url from kNonStoreEmoticonTable order by md5').fetchall()
        output=Path(output)
        images=output/'images'
        images.mkdir(parents=True,exist_ok=True)
        report=[]
        for index,row in enumerate(rows,1):
            record={'item':index,'status':'failed'}
            for column in ('cdn_url','extern_url','encrypt_url'):
                url=row[column]
                if not url:
                    continue
                host=(urlparse(url).hostname or '').lower()
                if host not in MEDIA_HOSTS:
                    record['error']='unrecognized_media_host'
                    continue
                try:
                    data=download(url,timeout=12)
                    fmt=detect_format(data)
                    if not fmt and row['aes_key']:
                        decoded=aes_decrypt_variants(data,row['aes_key'])
                        if decoded:
                            data=decoded[0]
                            fmt=detect_format(data)
                    if not fmt:
                        record['error']='unsupported_original_format'
                        record['header_hex']=data[:16].hex()
                        record['bytes']=len(data)
                        continue
                    with Image.open(io.BytesIO(data)) as image:
                        frame_count=getattr(image,'n_frames',1)
                        size=image.size
                        for frame in range(frame_count):
                            image.seek(frame)
                            image.load()
                    name=hashlib.sha256(data).hexdigest()+'.'+fmt
                    (images/name).write_bytes(data)
                    record.update(status='exported',file='images/'+name,format=fmt,frames=frame_count,width=size[0],height=size[1],bytes=len(data))
                    record.pop('error',None)
                    break
                except Exception as exc:
                    record['error']=type(exc).__name__
            report.append(record)
            if index%10==0 or index==len(rows):
                progress(index,len(rows))
        (output/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
        succeeded=[r for r in report if r['status']=='exported']
        archive=output.parent/'wechat-stickers.zip'
        with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
            for item in output.rglob('*'):
                if item.is_file():
                    z.write(item,item.relative_to(output))
        con.close()
        return archive
    raise RuntimeError("未找到匹配的表情库配置。请登录微信、打开表情面板后重试；当前微信版本也可能不兼容。")
