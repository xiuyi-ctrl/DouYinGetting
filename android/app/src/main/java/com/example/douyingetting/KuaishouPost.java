package com.example.douyingetting;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class KuaishouPost {
    private static final Pattern LINK = Pattern.compile("https://(?:v\\.kuaishou\\.com|(?:www\\.)?kuaishou\\.com|v\\.m\\.chenzhongtech\\.com)/[^\\s<>\"，。；]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATLAS = Pattern.compile("\"atlas\"\\s*:\\s*(?=\\{)");
    private static final Pattern CAPTION = Pattern.compile("\"caption\"\\s*:\\s*(?=\")");
    private static final Pattern MANIFEST = Pattern.compile("\"manifest\"\\s*:\\s*(?=\\{)");
    private static final Pattern VIDEO_PATH = Pattern.compile("/short-video/([A-Za-z0-9_-]+)/?");
    final ArrayList<ArrayList<URL>> images = new ArrayList<>();
    final ArrayList<URL> videos = new ArrayList<>();
    String caption = "";
    String link = "";

    static String extractLink(String shareText) {
        Matcher match = LINK.matcher(shareText.replace("\\:", ":"));
        return match.find() ? match.group() : null;
    }

    static KuaishouPost open(String shareText) throws Exception {
        String link = extractLink(shareText);
        if (link == null) throw new IOException("请粘贴快手作品链接或分享文案");
        URL url = new URL(link);
        Matcher video = VIDEO_PATH.matcher(url.getPath());
        if (video.matches()) url = new URL("https://v.m.chenzhongtech.com/fw/photo/" + video.group(1));
        String html = new String(read(url, 4 * 1024 * 1024, false), StandardCharsets.UTF_8);
        KuaishouPost post = parse(html);
        post.link = link;
        if (post.images.isEmpty() && post.videos.isEmpty() && post.caption.isEmpty())
            throw new IOException("公开分享页未返回作品正文，请检查链接是否有效");
        return post;
    }

    static KuaishouPost parse(String html) {
        KuaishouPost post = new KuaishouPost();
        Matcher caption = CAPTION.matcher(html);
        if (caption.find()) {
            try {
                Object value = new JSONTokener(html.substring(caption.end())).nextValue();
                if (value instanceof String) post.caption = ((String) value).trim();
            } catch (org.json.JSONException ignored) { }
        }
        Matcher atlases = ATLAS.matcher(html);
        // Keep the order of the share page's atlas. Later atlases may supply alternate formats.
        while (atlases.find()) {
            try {
                JSONObject atlas = (JSONObject) new JSONTokener(html.substring(atlases.end())).nextValue();
                JSONArray paths = atlas.optJSONArray("list");
                JSONArray hosts = atlas.optJSONArray("cdn");
                if (paths == null || hosts == null || paths.length() == 0 || paths.length() > 100) continue;
                if (post.images.isEmpty()) for (int i = 0; i < paths.length(); i++) post.images.add(new ArrayList<>());
                if (paths.length() != post.images.size()) continue;
                for (int i = 0; i < paths.length(); i++) {
                    String path = paths.optString(i);
                    if (!path.matches("/ufile/atlas/[A-Za-z0-9_./-]+") || path.contains("..")) continue;
                    for (int j = 0; j < Math.min(hosts.length(), 4); j++) {
                        String host = hosts.optString(j);
                        if (!host.matches("[A-Za-z0-9.-]+\\.(yximgs|kwimgs)\\.com")) continue;
                        URL url = new URL("https://" + host + path);
                        ArrayList<URL> choices = post.images.get(i);
                        if (choices.size() < 6 && choices.stream().noneMatch(u -> u.toString().equals(url.toString()))) choices.add(url);
                    }
                }
            } catch (org.json.JSONException | IOException ignored) { }
        }
        if (post.images.isEmpty()) post.readVideoManifest(html);
        return post;
    }

    private void readVideoManifest(String html) {
        Matcher manifests = MANIFEST.matcher(html);
        while (manifests.find()) {
            ArrayList<JSONObject> choices = new ArrayList<>();
            try {
                JSONObject manifest = (JSONObject) new JSONTokener(html.substring(manifests.end())).nextValue();
                JSONArray sets = manifest.optJSONArray("adaptationSet");
                if (sets == null) continue;
                for (int i = 0; i < sets.length(); i++) {
                    JSONArray representations = sets.getJSONObject(i).optJSONArray("representation");
                    if (representations == null) continue;
                    for (int j = 0; j < representations.length(); j++) {
                        JSONObject video = representations.getJSONObject(j);
                        if (video.optInt("width") > 0 && video.optInt("height") > 0 && !video.optBoolean("mute")) choices.add(video);
                    }
                }
                choices.sort(java.util.Comparator.comparing((JSONObject v) -> !"avc".equals(v.optString("videoCodec")))
                        .thenComparing(java.util.Comparator.comparingInt((JSONObject v) -> v.optInt("height")).reversed()));
                for (JSONObject candidate : choices) {
                    addVideoUrl(candidate.optString("url"));
                    JSONArray backups = candidate.optJSONArray("backupUrl");
                    if (backups != null) for (int i = 0; i < backups.length() && videos.size() < 4; i++)
                        addVideoUrl(backups.optString(i));
                    if (videos.size() >= 4) return;
                }
                if (!videos.isEmpty()) return;
            } catch (org.json.JSONException ignored) { }
        }
    }

    private void addVideoUrl(String value) {
        try {
            URL url = new URL(value);
            if ("https".equals(url.getProtocol()) && isMediaHost(url.getHost())
                    && url.getPath().toLowerCase(java.util.Locale.ROOT).endsWith(".mp4")
                    && videos.stream().noneMatch(item -> item.toString().equals(url.toString()))) videos.add(url);
        } catch (IOException ignored) { }
    }

    private static boolean isMediaHost(String host) {
        host = host.toLowerCase(java.util.Locale.ROOT);
        return host.endsWith(".yximgs.com") || host.endsWith(".kwimgs.com") || host.endsWith(".kwaicdn.com")
                || host.endsWith(".djvod.ndcimgs.com");
    }

    static byte[] read(URL url, int limit, boolean media) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transfer(url, limit, media, output);
        return output.toByteArray();
    }

    static void transfer(URL url, int limit, boolean media, OutputStream output) throws IOException {
        for (int redirects = 0; redirects <= 5; redirects++) {
            String host = url.getHost().toLowerCase(java.util.Locale.ROOT);
            boolean allowed = media ? isMediaHost(host)
                    : host.equals("kuaishou.com") || host.endsWith(".kuaishou.com") || host.equals("v.m.chenzhongtech.com");
            if (!"https".equals(url.getProtocol()) || !allowed || url.getUserInfo() != null
                    || (url.getPort() != -1 && url.getPort() != 443)) throw new IOException("快手资源地址无效");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    String next = connection.getHeaderField("Location");
                    if (next == null) throw new IOException("快手链接无法跳转");
                    url = new URL(url, next);
                    continue;
                }
                if (status != 200 || connection.getContentLengthLong() > limit) throw new IOException("读取快手资源失败");
                long length = connection.getContentLengthLong();
                try (InputStream input = connection.getInputStream()) {
                    byte[] buffer = new byte[16384];
                    int count, total = 0;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (total > limit) throw new IOException("快手资源超出大小限制");
                        output.write(buffer, 0, count);
                    }
                    if (length >= 0 && total != length) throw new IOException("快手资源下载不完整");
                    return;
                }
            } finally { connection.disconnect(); }
        }
        throw new IOException("快手链接跳转次数过多");
    }
}
