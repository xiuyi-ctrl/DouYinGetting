import http.client
import json
import ssl
import tempfile
import threading
import time
import unittest
from unittest.mock import patch
import zipfile
from pathlib import Path
from helper import Bridge, prepare_export_target
from export_wechat import checked_url


class BridgeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.zip = Path(self.temp.name)/'stickers.zip'
        with zipfile.ZipFile(self.zip, 'w') as archive: archive.writestr('README.txt', 'fixture')
        self.calls = 0
        def export(folder, progress):
            self.calls += 1
            progress(1, 1)
            return self.zip
        self.bridge = Bridge('127.0.0.1', export)
        self.context = ssl.create_default_context(cafile=str(self.bridge.folder/'cert.pem'))

    def tearDown(self):
        self.bridge.close()
        self.temp.cleanup()

    def request(self, method, path, token=True):
        connection = http.client.HTTPSConnection('127.0.0.1', self.bridge.port, context=self.context, timeout=5)
        try:
            headers = {'Authorization': 'Bearer '+self.bridge.token} if token else {}
            connection.request(method, path, headers=headers)
            response = connection.getresponse()
            return response.status, response.read()
        finally: connection.close()

    def test_auth_download_and_job_reuse(self):
        self.assertEqual(self.request('GET', '/status', False)[0], 401)
        self.assertEqual(self.request('POST', '/export')[0], 202)
        for _ in range(50):
            code, raw = self.request('GET', '/status')
            if json.loads(raw)['state'] == 'ready': break
            time.sleep(.02)
        self.assertEqual(json.loads(raw)['state'], 'ready')
        self.assertEqual(self.request('GET', '/archive'), (200, self.zip.read_bytes()))
        self.assertEqual(self.request('POST', '/export')[0], 202)
        self.assertEqual(self.calls, 1)
        self.assertEqual(self.request('GET', '/../cert.pem')[0], 404)
        self.assertEqual(self.request('GET', '/archive?file=anything')[0], 404)
        self.bridge.expires = 0
        self.assertEqual(self.request('GET', '/archive')[0], 401)

    def test_tls_requires_trust(self):
        connection = http.client.HTTPSConnection('127.0.0.1', self.bridge.port, timeout=5)
        with self.assertRaises(ssl.SSLCertVerificationError): connection.request('GET', '/status')
        connection.close()

    def test_error_does_not_leak_paths_or_urls(self):
        def failing(folder, progress): raise RuntimeError('PRIVATE_KEY=secret https://private.invalid/resource')
        self.bridge.exporter = failing
        self.request('POST', '/export')
        for _ in range(50):
            _, raw = self.request('GET', '/status')
            if json.loads(raw)['state'] == 'error': break
            time.sleep(.02)
        self.assertEqual(json.loads(raw)['state'], 'error')
        self.assertNotIn(b'PRIVATE_KEY', raw)
        self.assertNotIn(b'private.invalid', raw)

    def test_media_destination_validation(self):
        self.assertEqual(checked_url('http://wxapp.tc.qq.com/file?x=1'), 'https://wxapp.tc.qq.com/file?x=1')
        for url in ('https://example.com/file', 'https://wxapp.tc.qq.com.evil.invalid/file',
                    'https://user@wxapp.tc.qq.com/file', 'file:///tmp/picture', 'http://127.0.0.1/x'):
            with self.assertRaises(ValueError): checked_url(url)


class ExportTests(unittest.TestCase):
    def test_save_folder_preflight_and_unique_names(self):
        with tempfile.TemporaryDirectory() as temp:
            first = prepare_export_target(temp)
            second = prepare_export_target(temp)
            self.assertEqual(first.parent, Path(temp))
            self.assertNotEqual(first, second)
            self.assertEqual(first.suffix, '.zip')
            self.assertEqual(list(Path(temp).iterdir()), [])
            with patch('helper.tempfile.NamedTemporaryFile', side_effect=PermissionError('denied')):
                with self.assertRaises(PermissionError): prepare_export_target(temp)

    def test_export_pipeline_without_reading_wechat(self):
        import io
        import sqlite3
        from PIL import Image
        from export_wechat import export_stickers
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            db = root/'emoticon.db'
            with sqlite3.connect(db) as con:
                con.execute('create table kNonStoreEmoticonTable(md5, aes_key, cdn_url, extern_url, encrypt_url)')
                con.executemany('insert into kNonStoreEmoticonTable values(?,?,?,?,?)', [
                    ('a', '', 'https://wxapp.tc.qq.com/static', '', ''),
                    ('b', '', 'https://wxapp.tc.qq.com/animated', '', ''),
                    ('c', '', 'https://unapproved.invalid/image', '', ''),
                    ('d', '', '', '', '')])
            con.close()
            static = io.BytesIO()
            Image.new('RGB', (2, 2), 'red').save(static, format='PNG')
            animated = io.BytesIO()
            Image.new('RGB', (2, 2), 'red').save(animated, format='GIF', save_all=True,
                    append_images=[Image.new('RGB', (2, 2), 'blue')], duration=100)
            class Reader:
                def open_process(self, *args): return 1
                def close_handle(self, *args): pass
                def scan_wcdb_config_key(self, pid, handle, page, deadline, tested, result):
                    result.keys.add('00'*32)
                    return True
            def download(url, timeout):
                return animated.getvalue() if url.endswith('animated') else static.getvalue()
            with patch('export_wechat.get_weixin_pids', return_value=[1]), \
                 patch('export_wechat.ProcessMemoryReader', Reader), \
                 patch('export_wechat.detect_page_reserve', return_value=80), \
                 patch('export_wechat.decrypt_page', side_effect=lambda page, index, key, reserve: page), \
                 patch('export_wechat.download', side_effect=download) as downloader:
                archive = export_stickers(db, root/'export')
            self.assertEqual(downloader.call_count, 2)
            with zipfile.ZipFile(archive) as zip:
                report = json.loads(zip.read('report.json'))
                self.assertEqual(sum(r['status']=='exported' for r in report), 2)
                self.assertEqual(sum(r.get('frames', 0)>1 for r in report), 1)
                self.assertNotIn('https://', zip.read('report.json').decode())


if __name__ == '__main__': unittest.main()
