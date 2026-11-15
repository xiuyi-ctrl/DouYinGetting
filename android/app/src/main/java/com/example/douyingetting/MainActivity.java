package com.example.douyingetting;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final Pattern LINK = Pattern.compile("https?://[^\\s]+douyin\\.com/[^\\s]+", Pattern.CASE_INSENSITIVE);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<Media> media = new ArrayList<>();
    private final HashSet<String> hashes = new HashSet<>();
    private WebView browser;
    private EditText input;
    private TextView status;
    private LinearLayout results;
    private Button scanButton;
    private volatile String commentRequest;
    private volatile String lastCommentPath;
    private PermissionRequest pendingCameraRequest;
    private boolean scanning;
    private int requestWaits;
    private int requestToken;
    private long cursor;
    private int scanned;

    private static class Media {
        final File file;
        final String mime;
        final String commentId;
        final String kind;

        Media(File file, String mime, String commentId, String kind) {
            this.file = file;
            this.mime = mime;
            this.commentId = commentId;
            this.kind = kind;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);
        root.setPadding(16, 16, 16, 16);
        setContentView(root);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("粘贴抖音作品链接或分享文案");
        root.addView(input);
        Button open = new Button(this);
        open.setText("打开作品");
        open.setOnClickListener(v -> openLink());
        root.addView(open);
        status = new TextView(this);
        status.setText("扫码请使用抖音 App；同机登录可在网页切换验证码登录");
        root.addView(status);

        Button codeLogin = new Button(this);
        codeLogin.setText("切换验证码登录");
        codeLogin.setOnClickListener(v -> browser.evaluateJavascript(
                "(() => {const tab=[...document.querySelectorAll('button,div,span')].find(x=>x.children.length===0&&x.textContent.trim()==='验证码登录'&&x.getBoundingClientRect().width>0);if(tab){tab.click();return true}return false})()",
                found -> status.setText("true".equals(found) ? "请在抖音网页中输入手机号和验证码" : "请先在网页中点登录，再选择验证码登录")));
        root.addView(codeLogin);

        browser = new WebView(this);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setUseWideViewPort(true);
        browser.getSettings().setLoadWithOverviewMode(true);
        browser.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser, true);
        browser.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String path = uri.getPath();
                String host = uri.getHost();
                if (path != null && path.contains("comment")) {
                    lastCommentPath = path;
                    if (path.contains("comment/list") && host != null && (host.equals("douyin.com") || host.endsWith(".douyin.com"))) {
                        commentRequest = uri.toString();
                    }
                }
                return null;
            }
            @Override public void onPageFinished(WebView view, String url) {
                status.setText("作品已打开。请在抖音网页登录并进入评论区，再开始扫描");
            }
        });
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> askCameraPermission(request));
            }

            @Override public void onPermissionRequestCanceled(PermissionRequest request) {
                if (pendingCameraRequest == request) pendingCameraRequest = null;
            }
        });
        root.addView(browser, new LinearLayout.LayoutParams(-1, dp(300)));

        scanButton = new Button(this);
        scanButton.setText("扫描主评论");
        scanButton.setOnClickListener(v -> {
            if (scanning) {
                scanning = false;
                requestToken++;
                scanButton.setText("继续扫描");
            } else {
                scanning = true;
                requestWaits = 0;
                scanButton.setText("暂停扫描");
                startPage();
            }
        });
        root.addView(scanButton);
        Button saveAll = new Button(this);
        saveAll.setText("保存全部已提取文件");
        saveAll.setOnClickListener(v -> worker.execute(() -> {
            int saved = 0;
            for (Media item : new ArrayList<>(media)) {
                try { saveToDownloads(item); saved++; } catch (Exception ignored) { }
            }
            int count = saved;
            runOnUiThread(() -> status.setText("已保存 " + count + " 个文件到下载/DouYinGetting"));
        }));
        root.addView(saveAll);
        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            String shared = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null) input.setText(shared);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void askCameraPermission(PermissionRequest request) {
        boolean cameraRequested = false;
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) cameraRequested = true;
        }
        if (!cameraRequested || !"https".equals(request.getOrigin().getScheme())) {
            request.deny();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("抖音网页请求相机")
                .setMessage("验证页面 " + request.getOrigin().getHost() + " 请求使用相机。只有你选择刷脸验证并同意后才会授权。")
                .setPositiveButton("允许", (dialog, which) -> {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    } else {
                        pendingCameraRequest = request;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1001);
                    }
                })
                .setNegativeButton("拒绝", (dialog, which) -> request.deny())
                .setOnCancelListener(dialog -> request.deny())
                .show();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code != 1001 || pendingCameraRequest == null) return;
        PermissionRequest request = pendingCameraRequest;
        pendingCameraRequest = null;
        if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
        } else {
            request.deny();
        }
    }

    private void openLink() {
        String text = input.getText().toString().replace("\\:", ":");
        Matcher matcher = LINK.matcher(text);
        if (!matcher.find()) {
            status.setText("没有找到抖音链接");
            return;
        }
        String link = matcher.group().replaceAll("[，。,.]+$", "");
        commentRequest = null;
        lastCommentPath = null;
        cursor = 0;
        scanned = 0;
        requestWaits = 0;
        requestToken++;
        scanning = false;
        scanButton.setText("扫描主评论");
        media.clear();
        hashes.clear();
        results.removeAllViews();
        input.clearFocus();
        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
        browser.loadUrl(link);
    }

    private void startPage() {
        String page = browser.getUrl();
        if (page == null || !page.startsWith("https://www.douyin.com/")) {
            status.setText("请先打开抖音作品页面");
            scanning = false;
            return;
        }
        if (commentRequest == null) {
            browser.evaluateJavascript("(() => {let path='';const entries=performance.getEntriesByType('resource');for(let i=entries.length-1;i>=0;i--){const u=new URL(entries[i].name);if(u.pathname.includes('comment')){path=u.pathname;if(u.pathname.includes('comment/list')&&(u.hostname==='douyin.com'||u.hostname.endsWith('.douyin.com')))return JSON.stringify({url:u.href,path})}}return JSON.stringify({url:null,path})})()", value -> {
                if (!scanning) return;
                if (value != null && !"null".equals(value)) {
                    try {
                        JSONObject found = javascriptObject(value);
                        if (commentRequest == null && !found.isNull("url")) commentRequest = found.getString("url");
                        if (lastCommentPath == null) lastCommentPath = found.optString("path", "");
                    }
                    catch (Exception ignored) { }
                }
                if (commentRequest != null) {
                    startPage();
                } else if (++requestWaits >= 10) {
                    scanning = false;
                    scanButton.setText("扫描主评论");
                    status.setText("未找到评论列表请求。最近相关路径：" + (lastCommentPath == null || lastCommentPath.isEmpty() ? "无" : lastCommentPath) + "。请截图发给开发者");
                } else {
                    status.setText("请打开评论区，正在等待评论列表请求…");
                    browser.postDelayed(this::startPage, 1500);
                }
            });
            return;
        }
        int token = ++requestToken;
        String key = "__dyScanResult" + token;
        String script = "(()=>{const k=" + JSONObject.quote(key) + ";window[k]=null;(async()=>{try{" +
                "const u=new URL(" + JSONObject.quote(commentRequest) + ");" +
                "u.searchParams.set('cursor'," + JSONObject.quote(Long.toString(cursor)) + ");" +
                "const r=await fetch(u.toString(),{credentials:'include'});" +
                "window[k]=JSON.stringify({http:r.status,data:await r.json()})" +
                "}catch(e){window[k]=JSON.stringify({error:String(e)})}})()})()";
        status.setText("已扫描 " + scanned + " 条主评论，正在等待下一页…");
        browser.evaluateJavascript(script, ignored -> readPageResult(key, token, 0));
    }

    private void readPageResult(String key, int token, int polls) {
        if (!scanning || token != requestToken) return;
        browser.evaluateJavascript("window." + key, value -> {
            if (!scanning || token != requestToken) return;
            if (value == null || "null".equals(value)) {
                if (polls >= 100) {
                    scanning = false;
                    scanButton.setText("继续扫描");
                    status.setText("扫描暂停：等待评论接口响应超时");
                } else {
                    browser.postDelayed(() -> readPageResult(key, token, polls + 1), 200);
                }
                return;
            }
            try {
                JSONObject response = javascriptObject(value);
                int http = response.optInt("http", -1);
                JSONObject pageData = response.optJSONObject("data");
                if (pageData == null) throw new Exception("评论接口 HTTP " + http + "，" + response.optString("error", "非 JSON 响应"));
                int code = pageData.optInt("status_code", -1);
                if (http != 200 || code != 0) throw new Exception("评论接口 HTTP " + http + "，状态 " + code + "，" + pageData.optString("status_msg", "无错误说明"));
                JSONArray comments = pageData.optJSONArray("comments");
                if (comments == null) throw new Exception("没有评论数据");
                long next = pageData.optLong("cursor", cursor);
                boolean more = pageData.optInt("has_more", 0) != 0;
                if (more && next == cursor) throw new Exception("分页游标没有前进");
                cursor = next;
                scanned += comments.length();
                status.setText("已扫描 " + scanned + " 条主评论，正在保存媒体…");
                worker.execute(() -> processPage(comments, more));
            } catch (Exception error) {
                scanning = false;
                scanButton.setText("继续扫描");
                status.setText("扫描暂停：" + error.getMessage());
            }
        });
    }

    private static JSONObject javascriptObject(String value) throws Exception {
        if (value == null) throw new Exception("网页没有返回数据");
        Object parsed = new JSONTokener(value).nextValue();
        if (parsed instanceof String) parsed = new JSONTokener((String) parsed).nextValue();
        if (parsed instanceof JSONObject) return (JSONObject) parsed;
        throw new Exception("网页没有返回 JSON 对象");
    }

    private static String url(JSONObject object) {
        if (object == null) return null;
        JSONArray list = object.optJSONArray("url_list");
        return list == null ? null : list.optString(0, null);
    }

    private void processPage(JSONArray comments, boolean more) {
        for (int i = 0; i < comments.length(); i++) {
            JSONObject comment = comments.optJSONObject(i);
            if (comment == null) continue;
            String commentId = comment.optString("cid");
            JSONArray images = comment.optJSONArray("image_list");
            if (images != null) for (int j = 0; j < images.length(); j++) {
                JSONObject image = images.optJSONObject(j);
                if (image != null) download(url(image.optJSONObject("origin_url")), commentId, "图片");
            }
            JSONObject sticker = comment.optJSONObject("sticker");
            if (sticker != null) {
                String source = url(sticker.optJSONObject("animate_url"));
                if (source == null) source = url(sticker.optJSONObject("static_url"));
                download(source, commentId, "表情");
            }
        }
        runOnUiThread(() -> {
            status.setText("已扫描 " + scanned + " 条主评论，找到 " + media.size() + " 个不同媒体" + (more ? "（继续扫描）" : "（主评论扫描完成）"));
            if (scanning && more) {
                browser.postDelayed(this::startPage, 350);
            } else {
                scanning = false;
                scanButton.setText(more ? "继续扫描" : "扫描完成");
            }
        });
    }

    private void download(String source, String commentId, String kind) {
        if (source == null) return;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(source).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Referer", "https://www.douyin.com/");
            byte[] bytes;
            try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[8192];
                int count;
                while ((count = stream.read(chunk)) != -1) buffer.write(chunk, 0, count);
                bytes = buffer.toByteArray();
            }
            String mime = format(bytes);
            if (mime == null) return;
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b & 0xff));
            String hash = hex.toString();
            if (!hashes.add(hash)) return;
            String extension = mime.equals("image/jpeg") ? ".jpg" : mime.equals("image/png") ? ".png" : mime.equals("image/gif") ? ".gif" : ".webp";
            File folder = new File(getFilesDir(), "media");
            folder.mkdirs();
            File file = new File(folder, hash.substring(0, 20) + extension);
            try (OutputStream out = new FileOutputStream(file)) { out.write(bytes); }
            Media item = new Media(file, mime, commentId, kind);
            runOnUiThread(() -> { media.add(item); addRow(item); });
        } catch (Exception ignored) { }
    }

    private static String format(byte[] b) {
        if (b.length < 12) return null;
        if (b[0] == (byte) 0xff && b[1] == (byte) 0xd8) return "image/jpeg";
        if (b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F') return "image/gif";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }

    private void addRow(Media item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView name = new TextView(this);
        name.setText(item.kind + " · " + item.mime.substring(6) + " · 评论 " + item.commentId);
        row.addView(name, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button preview = new Button(this);
        preview.setText("预览");
        preview.setOnClickListener(v -> preview(item));
        row.addView(preview);
        Button save = new Button(this);
        save.setText("保存");
        save.setOnClickListener(v -> worker.execute(() -> {
            try {
                saveToDownloads(item);
                runOnUiThread(() -> status.setText("已保存 " + item.file.getName()));
            } catch (Exception error) {
                runOnUiThread(() -> status.setText("保存失败：" + error.getMessage()));
            }
        }));
        row.addView(save);
        results.addView(row);
    }

    private void preview(Media item) {
        try {
            Drawable drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(item.file));
            ImageView image = new ImageView(this);
            image.setImageDrawable(drawable);
            image.setAdjustViewBounds(true);
            new AlertDialog.Builder(this).setTitle(item.file.getName()).setView(image).setPositiveButton("关闭", null).show();
            if (drawable instanceof Animatable) ((Animatable) drawable).start();
        } catch (Exception error) {
            status.setText("预览失败：" + error.getMessage());
        }
    }

    private void saveToDownloads(Media item) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, item.file.getName());
        values.put(MediaStore.Downloads.MIME_TYPE, item.mime);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DouYinGetting");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri target = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (target == null) throw new Exception("无法创建下载文件");
        try (InputStream in = new java.io.FileInputStream(item.file);
             OutputStream out = getContentResolver().openOutputStream(target)) {
            if (out == null) throw new Exception("无法写入下载文件");
            byte[] chunk = new byte[8192];
            int count;
            while ((count = in.read(chunk)) != -1) out.write(chunk, 0, count);
        } catch (Exception error) {
            getContentResolver().delete(target, null, null);
            throw error;
        }
        values.clear();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        getContentResolver().update(target, values, null, null);
    }

    @Override protected void onDestroy() {
        scanning = false;
        worker.shutdownNow();
        browser.destroy();
        super.onDestroy();
    }
}
