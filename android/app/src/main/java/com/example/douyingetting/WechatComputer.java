package com.example.douyingetting;

import android.net.Uri;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.net.ssl.*;

/** Session-pinned HTTPS; pairing credentials are never persisted. */
final class WechatComputer {
    private final String origin, token;
    private final SSLSocketFactory sockets;
    WechatComputer(String pairing) throws Exception {
        Uri uri = Uri.parse(pairing.trim());
        String host = uri.getQueryParameter("host"), port = uri.getQueryParameter("port");
        token = uri.getQueryParameter("token");
        String pin = uri.getQueryParameter("pin");
        if (!"xiuyi".equals(uri.getScheme()) || !"wechat".equals(uri.getHost()) || !privateAddress(host)
                || port == null || !port.matches("[0-9]{1,5}") || Integer.parseInt(port) < 1024
                || Integer.parseInt(port) > 65535 || token == null || !token.matches("[a-f0-9]{64}")
                || pin == null || !pin.matches("[a-f0-9]{64}"))
            throw new IOException("配对内容无效，请从电脑助手重新复制");
        origin = "https://" + host + ":" + port;
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] chain, String auth) throws java.security.cert.CertificateException {
                throw new java.security.cert.CertificateException("不支持客户端证书");
            }
            public void checkServerTrusted(X509Certificate[] chain, String auth) throws java.security.cert.CertificateException {
                try {
                    if (chain == null || chain.length == 0) throw new Exception();
                    chain[0].checkValidity();
                    byte[] digest = MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded());
                    StringBuilder actual = new StringBuilder();
                    for (byte b : digest) actual.append(String.format(Locale.ROOT, "%02x", b & 255));
                    if (!MessageDigest.isEqual(actual.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                            pin.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) throw new Exception();
                } catch (Exception error) { throw new java.security.cert.CertificateException("电脑证书与配对内容不符"); }
            }
        }}, null);
        sockets = context.getSocketFactory();
    }
    private static boolean privateAddress(String host) {
        if (host == null || !host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) return false;
        String[] parts = host.split("\\."); int[] n = new int[4];
        for (int i = 0; i < 4; i++) { n[i] = Integer.parseInt(parts[i]); if (n[i] > 255) return false; }
        return n[0] == 10 || n[0] == 127 || (n[0] == 192 && n[1] == 168)
                || (n[0] == 172 && n[1] >= 16 && n[1] <= 31);
    }
    private HttpsURLConnection connect(String path, boolean post) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(origin + path).openConnection();
        connection.setSSLSocketFactory(sockets);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(8000); connection.setReadTimeout(20000);
        connection.setRequestProperty("Authorization", "Bearer " + token);
        if (post) {
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(0);
        }
        return connection;
    }
    private JSONObject request(String path, boolean post) throws Exception {
        HttpsURLConnection connection = connect(path, post);
        try {
            int code = connection.getResponseCode();
            if (code == 401) throw new IOException("配对已失效，请重新启动电脑助手并复制配对内容");
            if (code != 200 && code != 202) throw new IOException("电脑助手暂时无法处理请求");
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096]; int size;
                while ((size = in.read(chunk)) != -1) {
                    if (body.size() + size > 65536) throw new IOException("电脑响应无效");
                    body.write(chunk, 0, size);
                }
                byte[] bytes = body.toByteArray();
                return new JSONObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            }
        } finally { connection.disconnect(); }
    }
    void receive(File target, Consumer<String> progress, BooleanSupplier cancelled) throws Exception {
        boolean completed = false;
        try {
            JSONObject state = request("/export", true);
            long deadline = System.currentTimeMillis() + 15 * 60 * 1000;
            while (!cancelled.getAsBoolean()) {
                String status = state.optString("state");
                if (status.equals("ready")) break;
                if (status.equals("error")) throw new IOException(state.optString("message", "电脑导出失败"));
                if (System.currentTimeMillis() > deadline) throw new IOException("等待电脑导出超时，可稍后重新连接");
                progress.accept(state.optString("message", "等待电脑导出"));
                Thread.sleep(1000); state = request("/status", false);
            }
            if (cancelled.getAsBoolean()) throw new InterruptedIOException("已取消接收");
            progress.accept("正在接收表情原文件…");
            HttpsURLConnection connection = connect("/archive", false);
            try {
                if (connection.getResponseCode() != 200) throw new IOException("电脑未能传送表情压缩包，请重试");
                long declared = connection.getContentLengthLong(), total = 0;
                if (declared > 512L * 1024 * 1024) throw new IOException("表情压缩包超过 512 MB");
                try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(target)) {
                    byte[] buffer = new byte[65536]; int count;
                    while ((count = in.read(buffer)) != -1) {
                        if (cancelled.getAsBoolean()) throw new InterruptedIOException("已取消接收");
                        total += count;
                        if (total > 512L * 1024 * 1024) throw new IOException("表情压缩包超过 512 MB");
                        out.write(buffer, 0, count);
                    }
                }
                if (declared >= 0 && declared != total) throw new IOException("表情文件接收不完整，请重试");
                completed = true;
            } finally { connection.disconnect(); }
        } finally { if (!completed) target.delete(); }
    }
}
