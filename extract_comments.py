"""Extract media from comments loaded by a user-owned Douyin Chrome session."""

import argparse
import base64
import hashlib
import json
import time
from pathlib import Path
from urllib.parse import parse_qs, urlparse

import requests
import websocket
from PIL import Image
from io import BytesIO


class Browser:
    def __init__(self, port: int, note_id: str):
        tabs = requests.get(f"http://127.0.0.1:{port}/json", timeout=3).json()
        tab = next(t for t in tabs if t.get("type") == "page" and note_id in t.get("url", ""))
        self.ws = websocket.create_connection(tab["webSocketDebuggerUrl"], timeout=10)
        self.sequence = 0
        self.requests = {}
        self.ready = []
        self.page_urls = []
        self.other_comment_urls = []

    def _read(self, timeout=1):
        self.ws.settimeout(timeout)
        message = json.loads(self.ws.recv())
        params = message.get("params", {})
        if message.get("method") in ("Network.requestWillBeSent", "Network.responseReceived"):
            source = params.get("response") or params.get("request") or {}
            url = source.get("url", "")
            if urlparse(url).path == "/aweme/v1/web/comment/list/":
                cursor = parse_qs(urlparse(url).query).get("cursor", ["?"])[0]
                self.requests[params["requestId"]] = cursor
                self.page_urls.append(url)
            elif "comment" in url and "/aweme/" in url:
                self.other_comment_urls.append(url)
        elif message.get("method") == "Network.loadingFinished":
            request_id = params["requestId"]
            if request_id in self.requests:
                self.ready.append(request_id)
        return message

    def call(self, method, params=None):
        self.sequence += 1
        current = self.sequence
        self.ws.send(json.dumps({"id": current, "method": method, "params": params or {}}))
        deadline = time.time() + 20
        while time.time() < deadline:
            try:
                message = self._read(min(2, max(.1, deadline - time.time())))
            except websocket.WebSocketTimeoutException:
                continue
            if message.get("id") == current:
                if "error" in message:
                    raise RuntimeError(message["error"])
                return message.get("result", {})
        raise TimeoutError(method)

    def evaluate(self, expression):
        result = self.call("Runtime.evaluate", {"expression": expression, "returnByValue": True})
        return result.get("result", {}).get("value")

    def drain(self, seconds):
        deadline = time.time() + seconds
        while time.time() < deadline:
            try:
                self._read(min(1, max(.1, deadline - time.time())))
            except websocket.WebSocketTimeoutException:
                pass

    def pages(self):
        while self.ready:
            request_id = self.ready.pop(0)
            cursor = self.requests.pop(request_id)
            try:
                result = self.call("Network.getResponseBody", {"requestId": request_id})
                body = result["body"]
                if result.get("base64Encoded"):
                    body = base64.b64decode(body).decode("utf-8")
                yield cursor, json.loads(body)
            except (KeyError, ValueError, RuntimeError):
                print(f"跳过无法读取的分页响应：cursor={cursor}", flush=True)

    def close(self):
        self.ws.close()

    def fetch_page(self, template_url, cursor):
        script = """(async () => {
          const url = new URL(%s);
          url.searchParams.set('cursor', %s);
          const response = await fetch(url.toString(), {credentials: 'include'});
          const data = await response.json();
          return {http: response.status, data};
        })()""" % (json.dumps(template_url), json.dumps(str(cursor)))
        result = self.call("Runtime.evaluate", {"expression": script, "awaitPromise": True,
                                                "returnByValue": True})
        value = result.get("result", {}).get("value")
        if not value or value.get("http") != 200 or value["data"].get("status_code") != 0:
            raise RuntimeError(f"评论请求失败：{value and value.get('http')}")
        return value["data"]


def first_url(value):
    if isinstance(value, dict):
        urls = value.get("url_list") or []
        return urls[0] if urls else None
    return None


def media_from(comment):
    for image in comment.get("image_list") or []:
        for key in ("origin_url", "original_url", "large_url", "medium_url", "thumb_url"):
            url = first_url(image.get(key))
            if url:
                yield "image", url
                break
    sticker = comment.get("sticker") or {}
    url = first_url(sticker.get("animate_url")) or first_url(sticker.get("static_url"))
    if url:
        yield "sticker", url


def save_media(url, output, index, comment_id, kind):
    response = requests.get(url, timeout=30, headers={"User-Agent": "Mozilla/5.0"})
    response.raise_for_status()
    image = Image.open(BytesIO(response.content))
    extension = {"JPEG": ".jpg", "PNG": ".png", "GIF": ".gif", "WEBP": ".webp"}.get(image.format)
    if not extension:
        raise ValueError(f"不支持的媒体格式：{image.format}")
    digest = hashlib.sha256(response.content).hexdigest()
    filename = digest[:20] + extension
    path = output / filename
    if not path.exists():
        path.write_bytes(response.content)
    item = index.setdefault(digest, {"sha256": digest, "file": filename, "kind": kind, "format": image.format,
                                     "frames": getattr(image, "n_frames", 1), "comments": []})
    if comment_id not in item["comments"]:
        item["comments"].append(comment_id)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("note_id", help="抖音作品 ID")
    parser.add_argument("--port", type=int, default=9232, help="已登录 Chrome 的调试端口")
    parser.add_argument("--max-pages", type=int, default=0, help="验证用页数上限；0 表示扫描到末页")
    parser.add_argument("--output", type=Path, default=Path("exports"))
    args = parser.parse_args()
    output = args.output / args.note_id
    output.mkdir(parents=True, exist_ok=True)
    browser = Browser(args.port, args.note_id)
    manifest_path = output / "manifest.json"
    previous = json.loads(manifest_path.read_text(encoding="utf-8")) if manifest_path.exists() else {}
    index = {item["sha256"]: item for item in previous.get("files", []) if "sha256" in item}
    scanned = previous.get("scanned", 0)
    cursor = previous.get("cursor", 0)
    complete = previous.get("complete", False)
    failures = previous.get("download_failures", [])
    page_count = 0
    try:
        if complete:
            print("已有完整结果，无需重复扫描", flush=True)
            return
        browser.call("Network.enable")
        browser.call("Page.reload", {"ignoreCache": True})
        browser.drain(3)
        for _ in range(10):
            browser.evaluate("document.querySelector('.JPPmtWk8')?.click()")
            browser.drain(1)
            if browser.page_urls:
                break
        if not browser.page_urls:
            for _ in range(5):
                browser.evaluate("document.querySelector('.EqrWeopv')?.children[0]?.click()")
                browser.drain(.5)
                browser.evaluate("document.querySelector('.JPPmtWk8')?.click()")
                browser.drain(1)
                if browser.page_urls:
                    break
        if not browser.page_urls:
            raise RuntimeError("未捕获评论请求，请检查浏览器是否完成登录")
        template_url = browser.page_urls[-1]
        while True:
            if args.max_pages and page_count >= args.max_pages:
                break
            try:
                data = browser.fetch_page(template_url, cursor)
            except RuntimeError as error:
                print(f"{error}；停止并保留部分结果", flush=True)
                break
            comments = data.get("comments") or []
            next_cursor = data.get("cursor")
            if next_cursor == cursor:
                print("分页游标没有前进，停止并保留部分结果", flush=True)
                break
            for comment in comments:
                for kind, url in media_from(comment):
                    try:
                        save_media(url, output, index, str(comment.get("cid")), kind)
                    except (requests.RequestException, OSError, ValueError) as error:
                        print(f"媒体下载失败：{comment.get('cid')} {error}", flush=True)
                        failures.append({"comment_id": str(comment.get("cid")), "kind": kind,
                                         "reason": str(error)[:160]})
            scanned += len(comments)
            cursor = next_cursor
            page_count += 1
            complete = not data.get("has_more")
            manifest_path.write_text(json.dumps({"scope": "primary_comments", "complete": complete,
                "scanned": scanned, "cursor": cursor, "download_failures": failures,
                "files": list(index.values())}, ensure_ascii=False, indent=2), encoding="utf-8")
            print(f"页 {page_count}：扫描 {scanned}/{data.get('total', '?')} 条评论，保存 {len(index)} 个不同媒体", flush=True)
            if complete:
                break
            time.sleep(.3)
        print(f"结束：完整={complete}，扫描评论={scanned}，不同媒体={len(index)}", flush=True)
    finally:
        browser.close()


if __name__ == "__main__":
    main()
