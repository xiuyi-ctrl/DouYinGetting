package com.example.douyingetting;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class WorkMedia {
    static boolean isShareLink(String link, boolean xhs) {
        if (link == null) return false;
        Uri uri = Uri.parse(link);
        String host = uri.getHost();
        if (!"https".equals(uri.getScheme()) || host == null || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) return false;
        String platform = xhs ? "xiaohongshu.com" : "douyin.com";
        return host.equals(platform) || host.endsWith("." + platform) || (xhs && host.equals("xhslink.cn"));
    }

    static String pageId(String page, boolean xhs) {
        if (page == null) return null;
        Uri uri = Uri.parse(page);
        if (!"https".equals(uri.getScheme()) || !(xhs ? "www.xiaohongshu.com" : "www.douyin.com").equals(uri.getHost())) return null;
        String path = uri.getPath();
        if (path == null || !path.matches(xhs ? "/(?:explore|discovery/item)/[a-fA-F0-9]{24}/?" : "/(?:video|note)/[0-9]+/?")) return null;
        java.util.List<String> segments = uri.getPathSegments();
        return segments.get(segments.size() - 1);
    }

    static void transfer(URL url, boolean xhs, int limit, OutputStream output) throws IOException {
        String[] domains = xhs ? new String[]{"xhscdn.com", "xiaohongshu.com", "rednotecdn.com"}
                : new String[]{"douyinvod.com", "douyinpic.com", "douyin.com", "byteimg.com", "bytecdn.cn", "zjcdn.com", "iesdouyin.com"};
        for (int redirect = 0; redirect <= 5; redirect++) {
            boolean allowed = false;
            String host = url.getHost().toLowerCase(java.util.Locale.ROOT);
            for (String domain : domains) {
                if (host.equals(domain) || host.endsWith("." + domain)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed || !"https".equals(url.getProtocol()) || url.getUserInfo() != null
                    || (url.getPort() != -1 && url.getPort() != 443)) throw new IOException("素材地址无效");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setRequestProperty("Referer", xhs ? "https://www.xiaohongshu.com/" : "https://www.douyin.com/");
                int status = connection.getResponseCode();
                String location = connection.getHeaderField("Location");
                if (status >= 300 && status < 400 && location != null) {
                    url = new URL(url, location);
                    continue;
                }
                if (status != 200) throw new IOException("素材下载失败");
                if (connection.getContentLengthLong() > limit) throw new IOException("文件超过提取容量限制");
                try (InputStream stream = connection.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int total = 0;
                    int count;
                    while ((count = stream.read(buffer)) != -1) {
                        total += count;
                        if (total > limit) throw new IOException("文件超过提取容量限制");
                        output.write(buffer, 0, count);
                    }
                }
                return;
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("素材重定向过多");
    }
}
