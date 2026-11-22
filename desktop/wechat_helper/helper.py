"""User-started, paired HTTPS bridge. It never accepts file paths from the phone."""
import datetime
import hashlib
import hmac
import ipaddress
import json
import secrets
import shutil
import socket
import ssl
import tempfile
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlencode

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID


def certificate(folder, host):
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, 'Xiuyi local helper')])
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name)
            .public_key(key.public_key()).serial_number(x509.random_serial_number())
            .not_valid_before(now - datetime.timedelta(minutes=5))
            .not_valid_after(now + datetime.timedelta(days=1))
            .add_extension(x509.SubjectAlternativeName([x509.IPAddress(ipaddress.ip_address(host))]), False)
            .sign(key, hashes.SHA256()))
    cert_path, key_path = folder/'cert.pem', folder/'key.pem'
    cert_path.write_bytes(cert.public_bytes(serialization.Encoding.PEM))
    key_path.write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8,
                                         serialization.NoEncryption()))
    return cert_path, key_path, cert.fingerprint(hashes.SHA256()).hex()


class Bridge:
    def __init__(self, host, exporter, changed=lambda: None):
        address = ipaddress.ip_address(host)
        if address.version != 4 or not address.is_private or address.is_unspecified:
            raise ValueError('请选择本机的局域网 IPv4 地址')
        self.host, self.exporter, self.changed = host, exporter, changed
        self.token = secrets.token_hex(32)
        self.expires = time.monotonic()+3600
        self.folder = Path(tempfile.mkdtemp(prefix='xiuyi-helper-'))
        self.lock = threading.Lock()
        self.job = {'state': 'idle', 'done': 0, 'total': 0, 'message': '等待手机请求'}
        self.archive = None
        self.closed = False
        bridge = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args): pass  # Never log pairing tokens or URLs.
            def setup(self):
                super().setup()
                self.connection.settimeout(30)

            def reply(self, code, value):
                data = json.dumps(value, ensure_ascii=False).encode('utf-8')
                self.send_response(code)
                self.send_header('Content-Type', 'application/json; charset=utf-8')
                self.send_header('Content-Length', str(len(data)))
                self.send_header('Cache-Control', 'no-store')
                self.end_headers()
                self.wfile.write(data)

            def authorized(self):
                expected = 'Bearer '+bridge.token
                if time.monotonic() >= bridge.expires or not hmac.compare_digest(
                        self.headers.get('Authorization', '').encode('utf-8'), expected.encode('utf-8')):
                    self.reply(401, {'message': '配对已失效，请重新连接'})
                    return False
                return True

            def do_GET(self):
                if not self.authorized(): return
                if self.path == '/status':
                    with bridge.lock: status = dict(bridge.job)
                    self.reply(200, status)
                elif self.path == '/archive':
                    with bridge.lock:
                        path = bridge.archive if bridge.job['state'] == 'ready' else None
                    if path is None: self.reply(409, {'message': '导出尚未完成'}); return
                    try:
                        with path.open('rb') as source:
                            self.send_response(200)
                            self.send_header('Content-Type', 'application/zip')
                            self.send_header('Content-Length', str(path.stat().st_size))
                            self.send_header('Cache-Control', 'no-store')
                            self.end_headers()
                            shutil.copyfileobj(source, self.wfile)
                    except (OSError, ConnectionError): pass
                else: self.reply(404, {'message': '接口不存在'})

            def do_POST(self):
                if not self.authorized(): return
                if self.path != '/export': self.reply(404, {'message': '接口不存在'}); return
                if self.headers.get('Transfer-Encoding') or self.headers.get('Content-Length', '0') != '0':
                    self.reply(400, {'message': '不接受上传内容'}); return
                with bridge.lock:
                    if bridge.job['state'] not in ('running', 'ready'):
                        bridge.job = {'state': 'running', 'done': 0, 'total': 0, 'message': '正在电脑上导出'}
                        threading.Thread(target=bridge.run_export, daemon=True).start()
                    status = dict(bridge.job)
                self.reply(202, status)

        cert, key, self.pin = certificate(self.folder, host)
        self.server = ThreadingHTTPServer((host, 0), Handler)
        self.server.daemon_threads = True
        self.server.block_on_close = False
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.minimum_version = ssl.TLSVersion.TLSv1_2
        context.load_cert_chain(cert, key)
        self.server.socket = context.wrap_socket(self.server.socket, server_side=True)
        self.port = self.server.server_port
        self.pairing = 'xiuyi://wechat?'+urlencode({'host': host, 'port': self.port,
                                                   'token': self.token, 'pin': self.pin})
        threading.Thread(target=self.server.serve_forever, daemon=True).start()

    def progress(self, done, total):
        with self.lock:
            self.job.update(done=done, total=total, message=f'已处理 {done} / {total} 条表情')
        self.changed()

    def run_export(self):
        try:
            target = self.exporter(self.folder/'export', self.progress)
            if self.closed: return
            if not target.is_file() or target.stat().st_size > 512*1024*1024:
                raise ValueError('文件缺失或超过 512 MB')
            with self.lock:
                self.archive = target
                self.job.update(state='ready', message='导出完成，手机可以接收')
        except Exception as error:
            # Export errors can contain local paths or media URLs; return only curated copy.
            message = str(error) if isinstance(error, UserError) else '电脑导出失败，请检查已选文件、微信登录状态和版本兼容性'
            with self.lock: self.job.update(state='error', message=message)
        finally:
            self.changed()
            if self.closed: shutil.rmtree(self.folder, ignore_errors=True)

    def close(self):
        self.closed = True
        self.token = secrets.token_hex(32)
        self.server.shutdown()
        self.server.server_close()
        with self.lock: running = self.job['state'] == 'running'
        if not running: shutil.rmtree(self.folder, ignore_errors=True)


class UserError(Exception): pass


def local_addresses():
    addresses = {'127.0.0.1'}
    for entry in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET):
        value = entry[4][0]
        if ipaddress.ip_address(value).is_private and not value.startswith('127.'):
            addresses.add(value)
    return sorted(addresses, key=lambda value: value.startswith('127.'))


def prepare_export_target(folder):
    """Test real write access in the selected folder before reading WeChat."""
    folder = Path(folder)
    with tempfile.NamedTemporaryFile(dir=folder, prefix='xiuyi-write-check-', delete=False) as probe:
        check = Path(probe.name)
        try: probe.write(b'write-check')
        finally:
            probe.close()
            check.unlink(missing_ok=True)
    stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    return folder/f'wechat-stickers-{stamp}-{secrets.token_hex(2)}.zip'


def main():
    import tkinter as tk
    from tkinter import ttk, filedialog, messagebox
    from export_wechat import export_stickers
    window = tk.Tk()
    window.title('咻咿提取 · 电脑助手')
    window.geometry('680x550')
    window.configure(padx=24, pady=20)
    bridge = None
    local_running = False
    mode = tk.StringVar(value='导出微信表情')
    host = tk.StringVar(value=local_addresses()[0])
    source = tk.StringVar()
    consent = tk.BooleanVar()
    state = tk.StringVar(value='尚未启动。手机与电脑需在同一局域网。')
    ttk.Label(window, text='微信表情 · 电脑助手', font=('Microsoft YaHei', 20, 'bold')).pack(anchor='w')
    ttk.Label(window, text='导出只在本机处理；手机仅接收表情文件。配对有效期 1 小时。').pack(anchor='w', pady=10)
    options = ttk.Frame(window); options.pack(fill='x')
    ttk.Combobox(options, textvariable=mode, values=['导出微信表情', '传送已有 ZIP'], state='readonly', width=20).pack(side='left')
    ttk.Combobox(options, textvariable=host, values=local_addresses(), state='readonly', width=22).pack(side='right')
    ttk.Label(window, text='导出模式：选择自己账号的 db_storage/emoticon/emoticon.db\n传送模式：选择已经导出的表情 ZIP，不读取微信。').pack(anchor='w', pady=12)
    row = ttk.Frame(window); row.pack(fill='x')
    ttk.Entry(row, textvariable=source).pack(side='left', fill='x', expand=True)
    def choose():
        path = filedialog.askopenfilename(title='选择表情库或已导出的 ZIP', filetypes=[('表情库或压缩包', '*.db *.zip')])
        if path: source.set(path)
    ttk.Button(row, text='选择文件', command=choose).pack(side='right', padx=(8, 0))
    tk.Checkbutton(window, variable=consent, wraplength=620, justify='left', anchor='w',
                   text='我了解导出微信表情采用非官方方式，仍有账号风险，并同意向微信媒体服务器下载。').pack(anchor='w', pady=14)
    ttk.Label(window, text='配对内容（仅粘贴到自己的手机，不要公开）：').pack(anchor='w')
    pairing = tk.Text(window, height=5, wrap='char', font=('Consolas', 10))
    pairing.pack(fill='x', pady=8)
    pairing.configure(state='disabled')
    ttk.Label(window, textvariable=state, wraplength=620).pack(anchor='w', pady=8)
    buttons = ttk.Frame(window); buttons.pack(fill='x', pady=8)
    def refresh():
        if bridge:
            with bridge.lock: state.set(bridge.job['message'])
    def start():
        nonlocal bridge
        if local_running: messagebox.showinfo('正在导出', '请等待本次本地导出完成。'); return
        if bridge: messagebox.showinfo('已启动', '请先停止当前连接再更改配置。'); return
        path = Path(source.get())
        exporting = mode.get() == '导出微信表情'
        if not path.is_file() or (exporting and path.name != 'emoticon.db') or (not exporting and path.suffix.lower() != '.zip'):
            messagebox.showerror('请选择文件', '请选择自己的 emoticon.db，或已导出的 ZIP。'); return
        if exporting and not consent.get():
            messagebox.showwarning('需要确认', '请先阅读并勾选非官方导出风险说明；也可切换到传送已有 ZIP。'); return
        def export(folder, progress):
            if exporting: return export_stickers(path, folder, progress)
            return path
        def changed():
            try: window.after(0, refresh)
            except (tk.TclError, RuntimeError): pass
        try: bridge = Bridge(host.get(), export, changed)
        except Exception: messagebox.showerror('无法启动', '无法监听所选地址，请选择其他局域网地址。'); return
        pairing.configure(state='normal'); pairing.delete('1.0', 'end'); pairing.insert('1.0', bridge.pairing); pairing.configure(state='disabled')
        state.set('已启动。复制配对内容到手机。若连接失败，请检查专用网络的防火墙许可；不要关闭防火墙。')
    def stop():
        nonlocal bridge
        if bridge:
            bridge.close(); bridge = None
        pairing.configure(state='normal'); pairing.delete('1.0', 'end'); pairing.configure(state='disabled')
        state.set('已停止，旧配对内容失效。正在进行的导出会完成本次处理。')
    def copy():
        if bridge: window.clipboard_clear(); window.clipboard_append(bridge.pairing)
    def export_local():
        nonlocal local_running
        if local_running or bridge:
            messagebox.showinfo('请先完成当前操作', '请等待本次导出完成，或停止当前助手连接。'); return
        path = Path(source.get())
        if mode.get() != '导出微信表情' or path.name != 'emoticon.db' or not path.is_file():
            messagebox.showwarning('请选择表情库', '切换到导出微信表情，并选择自己的 emoticon.db。'); return
        if not consent.get():
            messagebox.showwarning('需要确认', '请先阅读并勾选非官方导出风险说明。'); return
        folder = filedialog.askdirectory(title='选择表情 ZIP 保存文件夹', mustexist=True)
        if not folder: return
        try: target = prepare_export_target(folder)
        except OSError:
            messagebox.showerror('此文件夹无法写入', '助手未能在所选文件夹创建文件。\n请选择自己可写的文件夹，或检查系统安全软件的拦截提示。\n尚未开始读取或导出微信表情。')
            return
        local_running = True
        state.set('正在导出到电脑，请保持微信登录并等待…')
        def notify(text):
            def update():
                nonlocal local_running
                local_running = False
                state.set(text)
            try: window.after(0, update)
            except (tk.TclError, RuntimeError): pass
        def run():
            pending = None
            try:
                with tempfile.TemporaryDirectory(prefix='xiuyi-local-export-') as temp:
                    archive = export_stickers(path, Path(temp)/'export')
                    # An adjacent temporary file prevents a failed copy from truncating a previous ZIP.
                    with tempfile.NamedTemporaryFile(dir=str(Path(target).parent), suffix='.tmp', delete=False) as out:
                        pending = Path(out.name)
                        with archive.open('rb') as source_file: shutil.copyfileobj(source_file, out)
                    pending.replace(target)
                    pending = None
                notify(f'ZIP 已保存：{target}')
            except PermissionError:
                notify('文件访问被拒绝：请检查所选表情库、保存文件夹或系统安全软件的拦截提示。')
            except Exception: notify('本地导出失败，请检查表情库、微信登录状态和版本兼容性。')
            finally:
                if pending: pending.unlink(missing_ok=True)
        threading.Thread(target=run, daemon=True).start()
    ttk.Button(buttons, text='启动助手', command=start).pack(side='left')
    ttk.Button(buttons, text='复制配对内容', command=copy).pack(side='left', padx=10)
    ttk.Button(buttons, text='停止连接', command=stop).pack(side='left')
    ttk.Button(buttons, text='导出 ZIP 到电脑', command=export_local).pack(side='right')
    def close():
        stop(); window.destroy()
    window.protocol('WM_DELETE_WINDOW', close)
    window.mainloop()


if __name__ == '__main__': main()
