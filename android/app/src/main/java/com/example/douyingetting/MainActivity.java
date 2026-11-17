package com.example.douyingetting;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.content.ContentValues;
import android.content.ClipData;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageDecoder;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.Context;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.FrameLayout;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final Pattern LINK = Pattern.compile("https?://[^\\s]+douyin\\.com/[^\\s]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern XHS_LINK = Pattern.compile("https?://(?:[^\\s/]+\\.)?(?:xiaohongshu\\.com|xhslink\\.cn)/[^\\s]+", Pattern.CASE_INSENSITIVE);
    private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<Media> media = new ArrayList<>();
    private final ArrayList<ScanRecord> records = new ArrayList<>();
    private ScanRecord currentRecord;
    private String activeLink;
    private boolean viewingSavedRecord;
    private boolean xhsMode;
    private TextView readerHeading;
    private TextView inputHeading;
    private TextView browserHint;
    private Button codeLogin;
    private WebView browser;
    private EditText input;
    private TextView status;
    private TextView scanCountLine;
    private ProgressBar scanProgress;
    private LinearLayout results;
    private Button scanButton;
    private LinearLayout readerRoot;
    private LinearLayout browserPanel;
    private TextView webToggle;
    private LinearLayout resultRow;
    private ScrollView readerScroll;
    private TextView countLabel;
    private TextView resultSummary;
    private TextView allFilter;
    private TextView gifFilter;
    private TextView imageFilter;
    private TextView emptyResults;
    private View resultAnchor;
    private final ArrayList<Animatable> animations = new ArrayList<>();
    private String selectedFilter = "all";
    private int displayedCount;
    private boolean showingReader;
    private boolean showingRecords;
    private volatile String commentRequest;
    private volatile String lastCommentPath;
    private PermissionRequest pendingCameraRequest;
    private boolean scanning;
    private int requestWaits;
    private int requestToken;
    private long cursor;
    private int scanned;
    private int xhsIdlePolls;
    private volatile boolean xhsMainDone;
    private final HashSet<String> xhsCommentIds = new HashSet<>();
    private boolean xhsCaptureSupported;
    private String xhsCaptureScript;
    private String lastXhsPage;
    private int xhsAppRedirects;
    private static final int BG = Color.rgb(247, 248, 246);
    private static final int DARK = Color.rgb(25, 47, 55);
    private static final int GREEN = Color.rgb(28, 158, 120);
    private static final int MUTED = Color.rgb(125, 143, 143);

    private static class Media {
        final File file;
        final String hash;
        final String mime;
        final String commentId;
        final String kind;
        volatile Drawable thumbnail;
        final boolean animated;

        Media(File file, String hash, String mime, String commentId, String kind, Drawable thumbnail, boolean animated) {
            this.file = file;
            this.hash = hash;
            this.mime = mime;
            this.commentId = commentId;
            this.kind = kind;
            this.thumbnail = thumbnail;
            this.animated = animated || "video/mp4".equals(mime) || "image/gif".equals(mime) || thumbnail instanceof Animatable;
        }
    }

    private static class ScanRecord {
        final String id;
        final String link;
        final long createdAt;
        final File folder;
        final ArrayList<Media> items = new ArrayList<>();
        final HashSet<String> hashes = new HashSet<>();
        long cursor;
        int scanned;
        int mainScanned;
        int replyScanned;
        boolean complete;

        ScanRecord(String id, String link, long createdAt, File folder) {
            this.id = id;
            this.link = link;
            this.createdAt = createdAt;
            this.folder = folder;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildReader();
        loadRecords();
        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            String shared = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null) input.setText(shared);
        }
        showHome();
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private void showHome() {
        if (scanning) {
            scanning = false;
            requestToken++;
            scanButton.setText("继续扫描");
        }
        showingReader = false;
        showingRecords = false;
        for (Animatable animation : animations) animation.stop();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(20), dp(18), dp(32));
        scroll.addView(page);
        LinearLayout hero = new LinearLayout(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setBackground(shape(Color.WHITE, 26));
        hero.setClipToOutline(true);
        page.addView(hero, new LinearLayout.LayoutParams(-1, dp(212)));
        ImageView artwork = new ImageView(this);
        artwork.setImageResource(R.drawable.hero_art);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.addView(artwork, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout heroWords = new LinearLayout(this);
        heroWords.setOrientation(LinearLayout.VERTICAL);
        heroWords.setPadding(dp(4), 0, dp(10), 0);
        hero.addView(heroWords, new LinearLayout.LayoutParams(0, -2, 1));
        TextView tag = label("● 评论表情收藏夹", 10, GREEN, true);
        tag.setPadding(dp(7), dp(5), dp(7), dp(5));
        tag.setBackground(shape(Color.rgb(235, 248, 241), 12));
        heroWords.addView(tag);
        TextView title = label("咻咿\n提取", 31, Color.rgb(94, 105, 232), true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(12);
        heroWords.addView(title, titleParams);
        TextView subtitle = label("评论素材提取\n预览后再保存", 11, MUTED, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(8);
        heroWords.addView(subtitle, subtitleParams);

        TextView heading = label("选择功能", 20, DARK, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(28);
        page.addView(heading, headingParams);
        TextView description = label("从你想读取的平台开始", 12, MUTED, false);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
        descriptionParams.topMargin = dp(3);
        descriptionParams.bottomMargin = dp(18);
        page.addView(description, descriptionParams);
        LinearLayout features = new LinearLayout(this);
        features.setGravity(Gravity.TOP);
        page.addView(features);
        LinearLayout douyin = homeFeature(false);
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, dp(178), 1);
        first.rightMargin = dp(9);
        features.addView(douyin, first);
        LinearLayout xhs = homeFeature(true);
        LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, dp(178), 1);
        second.leftMargin = dp(9);
        features.addView(xhs, second);

        FrameLayout navArea = new FrameLayout(this);
        root.addView(navArea, new LinearLayout.LayoutParams(-1, dp(96)));
        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setPadding(dp(18), dp(9), dp(18), 0);
        dock.setBackground(shape(Color.WHITE, 26));
        navArea.addView(dock, new FrameLayout.LayoutParams(-1, dp(74), Gravity.BOTTOM));
        dock.addView(homeNavItem(false), new LinearLayout.LayoutParams(0, -1, 1));
        dock.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 1));
        dock.addView(homeNavItem(true), new LinearLayout.LayoutParams(0, -1, 1));
        ImageView homeIcon = new ImageView(this);
        homeIcon.setImageResource(R.drawable.home_nav);
        homeIcon.setElevation(dp(5));
        navArea.addView(homeIcon, new FrameLayout.LayoutParams(dp(72), dp(72), Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        TextView homeLabel = label("首页", 13, Color.rgb(94, 105, 232), true);
        homeLabel.setGravity(Gravity.CENTER);
        navArea.addView(homeLabel, new FrameLayout.LayoutParams(dp(80), dp(28), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        setContentView(root);
    }

    private LinearLayout homeNavItem(boolean xhs) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        ImageView icon = new ImageView(this);
        icon.setImageResource(xhs ? R.drawable.xhs_nav : R.drawable.douyin_nav);
        item.addView(icon, new LinearLayout.LayoutParams(dp(27), dp(27)));
        TextView name = label(xhs ? "小红书" : "抖音", 12, MUTED, true);
        name.setGravity(Gravity.CENTER);
        item.addView(name);
        item.setOnClickListener(v -> selectMode(xhs));
        return item;
    }

    private LinearLayout homeFeature(boolean xhs) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(14), dp(12));
        card.setBackground(shape(xhs ? Color.rgb(255, 236, 232) : Color.rgb(232, 233, 255), 24));
        card.setOnClickListener(v -> selectMode(xhs));
        ImageView icon = new ImageView(this);
        icon.setImageResource(xhs ? R.drawable.xhs_feature : R.drawable.douyin_feature);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView name = label(xhs ? "小红书链接读取" : "抖音链接读取", 16, DARK, true);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(6);
        card.addView(name, nameParams);
        card.addView(label(xhs ? "图片 · 回复 · 实况" : "表情包 · 图片 · GIF", 11, MUTED, false));
        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        card.addView(footer, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView arrow = label("→", 20, xhs ? Color.rgb(243, 125, 106) : Color.rgb(94, 105, 232), true);
        arrow.setGravity(Gravity.CENTER);
        arrow.setBackground(shape(Color.WHITE, 18));
        footer.addView(arrow, new LinearLayout.LayoutParams(dp(42), dp(27)));
        return card;
    }

    private void selectMode(boolean xhs) {
        scanning = false;
        requestToken++;
        xhsMode = xhs;
        currentRecord = null;
        viewingSavedRecord = false;
        activeLink = null;
        input.setText("");
        media.clear();
        for (Animatable animation : animations) animation.stop();
        animations.clear();
        selectedFilter = "all";
        renderResults();
        updateResultLabels();
        configureReaderMode();
        browserPanel.setVisibility(View.GONE);
        webToggle.setText("网页操作  ▾");
        browser.loadUrl("about:blank");
        showReader();
    }

    private void configureReaderMode() {
        browser.getSettings().setUserAgentString(DESKTOP_USER_AGENT);
        browser.getSettings().setUseWideViewPort(!xhsMode);
        browser.getSettings().setLoadWithOverviewMode(!xhsMode);
        browser.getLayoutParams().height = xhsMode
                ? Math.min(dp(400), (int) (getResources().getDisplayMetrics().heightPixels * 0.48f)) : dp(290);
        browser.requestLayout();
        readerHeading.setText(xhsMode ? "小红书链接读取" : "抖音链接读取");
        inputHeading.setText(xhsMode ? "笔记链接" : "作品链接");
        input.setHint(xhsMode ? "粘贴小红书笔记链接或分享文案" : "粘贴抖音作品链接或分享文案");
        browserHint.setText(xhsMode ? "在网页中登录并打开笔记评论，扫描时会读取评论素材" : "在网页中登录并打开评论区，然后扫描主评论");
        codeLogin.setVisibility(xhsMode ? View.GONE : View.VISIBLE);
        scanButton.setText(xhsMode ? "扫描评论" : "扫描主评论");
        status.setText(xhsMode ? "粘贴笔记链接，打开网页后进入评论区" : "粘贴作品链接，打开网页后进入评论区");
    }

    private void showRecords() {
        if (scanning) {
            scanning = false;
            requestToken++;
            scanButton.setText("继续扫描");
        }
        showingReader = false;
        showingRecords = true;
        for (Animatable animation : animations) animation.stop();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setFitsSystemWindows(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(16), dp(18), dp(28));
        scroll.addView(page);
        TextView back = label("‹  返回读取", 14, GREEN, true);
        back.setPadding(0, dp(8), 0, dp(8));
        back.setOnClickListener(v -> showReader());
        page.addView(back);
        addRecordCards(page);
        setContentView(scroll);
    }

    private void addRecordCards(LinearLayout page) {
        TextView heading = label("扫描记录", 19, DARK, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(28);
        headingParams.bottomMargin = dp(12);
        page.addView(heading, headingParams);
        ArrayList<ScanRecord> visibleRecords = new ArrayList<>();
        for (ScanRecord record : records) if (isXhsLink(record.link) == xhsMode) visibleRecords.add(record);
        if (visibleRecords.isEmpty()) {
            TextView empty = label("扫描过的作品会暂存在这里，方便随时查看和删除。", 12, MUTED, false);
            empty.setPadding(dp(18), dp(19), dp(18), dp(19));
            empty.setBackground(shape(Color.WHITE, 16));
            page.addView(empty);
            return;
        }
        SimpleDateFormat date = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        for (ScanRecord record : visibleRecords) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(17), dp(15), dp(17), dp(13));
            card.setBackground(shape(Color.WHITE, 17));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = dp(9);
            page.addView(card, cardParams);
            card.addView(label(date.format(record.createdAt) + (record.complete ? " · 已完成" : " · 暂存中"), 11, GREEN, true));
            TextView link = label(record.link, 12, DARK, true);
            link.setSingleLine(true);
            link.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams linkParams = new LinearLayout.LayoutParams(-1, -2);
            linkParams.topMargin = dp(7);
            card.addView(link, linkParams);
            int count;
            synchronized (record) { count = record.items.size(); }
            String progress = isXhsLink(record.link)
                    ? record.scanned + " 条评论（主评论 " + record.mainScanned + " · 回复 " + record.replyScanned + "）"
                    : record.scanned + " 条主评论";
            if (isXhsLink(record.link) && record.scanned > 0 && record.mainScanned + record.replyScanned == 0)
                progress = record.scanned + " 条评论（旧版计数）";
            TextView detail = label("legacy".equals(record.id) ? "旧版缓存 · 提取 " + count + " 个素材"
                    : "已扫描 " + progress + " · 提取 " + count + " 个素材", 11, MUTED, false);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
            detailParams.topMargin = dp(5);
            card.addView(detail, detailParams);
            LinearLayout actions = new LinearLayout(this);
            actions.setGravity(Gravity.RIGHT);
            LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
            actionsParams.topMargin = dp(9);
            card.addView(actions, actionsParams);
            TextView view = label("查看预览", 12, GREEN, true);
            view.setPadding(dp(10), dp(6), dp(10), dp(6));
            view.setOnClickListener(v -> showRecord(record));
            actions.addView(view);
            if (record.link.startsWith("http")) {
                TextView copy = label("复制链接", 12, GREEN, true);
                copy.setPadding(dp(10), dp(6), dp(10), dp(6));
                copy.setOnClickListener(v -> copyLink(record.link));
                actions.addView(copy);
            }
            TextView delete = label("删除记录", 12, MUTED, false);
            delete.setPadding(dp(10), dp(6), 0, dp(6));
            delete.setOnClickListener(v -> confirmDelete(record));
            actions.addView(delete);
            card.setOnClickListener(v -> showRecord(record));
        }
    }

    private void showRecord(ScanRecord record) {
        xhsMode = isXhsLink(record.link);
        configureReaderMode();
        currentRecord = record;
        viewingSavedRecord = true;
        activeLink = record.link.startsWith("http") ? record.link : null;
        input.setText(activeLink == null ? "" : activeLink);
        cursor = record.cursor;
        scanned = record.scanned;
        media.clear();
        synchronized (record) { media.addAll(record.items); }
        selectedFilter = "all";
        browserPanel.setVisibility(View.GONE);
        webToggle.setText("网页操作  ▾");
        scanButton.setText("重新扫描");
        showReader();
        renderResults();
        updateResultLabels();
        status.setText("已打开本机暂存记录；重新扫描会生成新记录");
        readerScroll.post(() -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()));
    }

    private void confirmDelete(ScanRecord record) {
        new AlertDialog.Builder(this).setTitle("删除扫描记录？")
                .setMessage("将删除这条记录和 App 内暂存的图片、动图。已保存到下载目录的文件不会删除。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    if (currentRecord == record) {
                        scanning = false;
                        requestToken++;
                        currentRecord = null;
                        viewingSavedRecord = false;
                        media.clear();
                        renderResults();
                        updateResultLabels();
                    }
                    worker.execute(() -> {
                        boolean deleted = deleteRecordFiles(record);
                        runOnUiThread(() -> {
                            if (deleted) {
                                records.remove(record);
                                showRecords();
                            } else {
                                new AlertDialog.Builder(this).setMessage("删除失败，请重试").setPositiveButton("确定", null).show();
                            }
                        });
                    });
                }).show();
    }

    private void showReader() {
        showingReader = true;
        showingRecords = false;
        setContentView(readerRoot);
        updateScanCard();
        for (Animatable animation : animations) animation.start();
    }

    private void buildReader() {
        readerRoot = new LinearLayout(this);
        readerRoot.setOrientation(LinearLayout.VERTICAL);
        readerRoot.setBackgroundColor(BG);
        readerRoot.setFitsSystemWindows(true);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(18), dp(8), dp(18), dp(8));
        readerRoot.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(68)));
        TextView back = label("‹", 32, DARK, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(shape(Color.rgb(237, 242, 239), 12));
        back.setOnClickListener(v -> showHome());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(38), dp(38)));
        readerHeading = label("抖音链接读取", 19, DARK, true);
        readerHeading.setGravity(Gravity.CENTER);
        toolbar.addView(readerHeading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView recordsButton = label("扫描记录", 12, GREEN, true);
        recordsButton.setGravity(Gravity.CENTER);
        recordsButton.setBackground(shape(Color.rgb(234, 247, 241), 19));
        recordsButton.setOnClickListener(v -> showRecords());
        toolbar.addView(recordsButton, new LinearLayout.LayoutParams(dp(72), dp(38)));

        FrameLayout scrollArea = new FrameLayout(this);
        readerRoot.addView(scrollArea, new LinearLayout.LayoutParams(-1, 0, 1));
        readerScroll = new ScrollView(this);
        readerScroll.setFillViewport(true);
        scrollArea.addView(readerScroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(8), dp(18), dp(72));
        readerScroll.addView(content);
        TextView scrollTop = label("↑ 置顶", 13, GREEN, true);
        scrollTop.setGravity(Gravity.CENTER);
        scrollTop.setBackground(shape(Color.WHITE, 22));
        scrollTop.setElevation(dp(4));
        scrollTop.setOnClickListener(v -> readerScroll.smoothScrollTo(0, 0));
        FrameLayout.LayoutParams scrollTopParams = new FrameLayout.LayoutParams(dp(90), dp(42), Gravity.RIGHT | Gravity.BOTTOM);
        scrollTopParams.setMargins(0, 0, dp(18), dp(10));
        scrollArea.addView(scrollTop, scrollTopParams);

        LinearLayout inputCard = new LinearLayout(this);
        inputCard.setOrientation(LinearLayout.VERTICAL);
        inputCard.setPadding(dp(20), dp(20), dp(20), dp(18));
        inputCard.setBackground(shape(Color.WHITE, 25));
        inputCard.setElevation(dp(3));
        content.addView(inputCard);
        LinearLayout inputHead = new LinearLayout(this);
        inputHead.setGravity(Gravity.CENTER_VERTICAL);
        inputCard.addView(inputHead);
        inputHeading = label("作品链接", 15, DARK, true);
        inputHead.addView(inputHeading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView connection = label("● 网页登录", 10, GREEN, true);
        connection.setPadding(dp(8), dp(5), dp(8), dp(5));
        connection.setBackground(shape(Color.rgb(230, 248, 238), 12));
        inputHead.addView(connection);
        LinearLayout urlBox = new LinearLayout(this);
        urlBox.setGravity(Gravity.CENTER_VERTICAL);
        urlBox.setPadding(dp(10), 0, dp(8), 0);
        urlBox.setBackground(shape(Color.rgb(247, 250, 248), 13));
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(-1, dp(52));
        urlParams.topMargin = dp(14);
        inputCard.addView(urlBox, urlParams);
        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(12);
        input.setTextColor(DARK);
        input.setHintTextColor(MUTED);
        input.setHint("粘贴抖音作品链接或分享文案");
        input.setBackgroundColor(Color.TRANSPARENT);
        urlBox.addView(input, new LinearLayout.LayoutParams(0, -1, 1));
        TextView paste = label("粘贴", 12, GREEN, true);
        paste.setGravity(Gravity.CENTER);
        paste.setBackground(shape(Color.rgb(220, 247, 232), 9));
        paste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0)
                input.setText(clipboard.getPrimaryClip().getItemAt(0).coerceToText(this));
        });
        urlBox.addView(paste, new LinearLayout.LayoutParams(dp(48), dp(36)));
        TextView copy = label("复制", 12, GREEN, true);
        copy.setGravity(Gravity.CENTER);
        copy.setOnClickListener(v -> {
            String link = extractLink(input.getText().toString(), xhsMode);
            if (link == null && input.getText().length() == 0) link = activeLink;
            if (link == null) showSaveResult("没有可复制的链接");
            else copyLink(link);
        });
        urlBox.addView(copy, new LinearLayout.LayoutParams(dp(42), dp(36)));
        TextView hint = label("支持分享文案、短链接与作品地址", 11, MUTED, false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(11);
        inputCard.addView(hint, hintParams);
        Button open = new Button(this);
        open.setAllCaps(false);
        open.setText("打开作品");
        open.setTextColor(Color.WHITE);
        open.setTextSize(14);
        open.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        open.setBackground(shape(GREEN, 16));
        open.setOnClickListener(v -> openLink());
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(-1, dp(48));
        openParams.topMargin = dp(14);
        inputCard.addView(open, openParams);

        LinearLayout scanCard = new LinearLayout(this);
        scanCard.setOrientation(LinearLayout.VERTICAL);
        scanCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        scanCard.setBackground(shape(Color.rgb(232, 244, 239), 20));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(18);
        content.addView(scanCard, statusParams);
        status = label("粘贴作品链接，打开网页后进入评论区", 12, DARK, true);
        scanCard.addView(status);
        scanCountLine = label("", 11, GREEN, false);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(-1, -2);
        countParams.topMargin = dp(5);
        scanCard.addView(scanCountLine, countParams);
        scanCountLine.setVisibility(View.GONE);
        scanProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        scanProgress.setIndeterminate(true);
        scanProgress.setIndeterminateTintList(ColorStateList.valueOf(GREEN));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(4));
        progressParams.topMargin = dp(10);
        scanCard.addView(scanProgress, progressParams);
        scanProgress.setVisibility(View.GONE);
        status.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable text) { updateScanCard(); }
        });
        webToggle = label("网页操作  ▾", 12, GREEN, true);
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(-1, -2);
        toggleParams.topMargin = dp(10);
        content.addView(webToggle, toggleParams);
        webToggle.setOnClickListener(v -> {
            browserPanel.setVisibility(browserPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            webToggle.setText(browserPanel.getVisibility() == View.VISIBLE ? "收起网页  ▴" : "网页操作  ▾");
        });
        browserPanel = new LinearLayout(this);
        browserPanel.setOrientation(LinearLayout.VERTICAL);
        browserPanel.setPadding(dp(10), dp(10), dp(10), dp(10));
        browserPanel.setBackground(shape(Color.WHITE, 16));
        LinearLayout.LayoutParams browserParams = new LinearLayout.LayoutParams(-1, -2);
        browserParams.topMargin = dp(9);
        content.addView(browserPanel, browserParams);
        browserPanel.setVisibility(View.GONE);
        browserHint = label("在网页中登录并打开评论区，然后扫描主评论", 11, MUTED, false);
        browserPanel.addView(browserHint);
        setupBrowser();
        browserPanel.addView(browser, new LinearLayout.LayoutParams(-1, dp(290)));
        LinearLayout controls = new LinearLayout(this);
        browserPanel.addView(controls);
        codeLogin = new Button(this);
        codeLogin.setAllCaps(false);
        codeLogin.setText("验证码登录");
        codeLogin.setOnClickListener(v -> browser.evaluateJavascript(
                "(() => {const tab=[...document.querySelectorAll('button,div,span')].find(x=>x.children.length===0&&x.textContent.trim()==='验证码登录'&&x.getBoundingClientRect().width>0);if(tab){tab.click();return true}return false})()",
                found -> status.setText("true".equals(found) ? "请在抖音网页中输入手机号和验证码" : "请先在网页中点登录，再选择验证码登录")));
        controls.addView(codeLogin, new LinearLayout.LayoutParams(0, dp(48), 1));
        scanButton = new Button(this);
        scanButton.setAllCaps(false);
        scanButton.setText("扫描主评论");
        scanButton.setOnClickListener(v -> {
            if (viewingSavedRecord) {
                openLink();
                status.setText(xhsMode ? "笔记已重新打开，请进入评论区后再次点击扫描" : "作品已重新打开，请进入评论区后再次点击扫描");
                return;
            }
            if (xhsMode) {
                toggleXhsScan();
                return;
            }
            if (scanning) {
                scanning = false;
                requestToken++;
                scanButton.setText("继续扫描");
                status.setText("扫描已暂停，已提取素材保留在暂存记录中");
            } else {
                String page = browser.getUrl();
                if (page == null || !page.startsWith("https://www.douyin.com/")) {
                    status.setText("请先打开抖音作品页面");
                    return;
                }
                if (currentRecord == null || currentRecord.complete) {
                    String id = UUID.randomUUID().toString();
                    ScanRecord record = new ScanRecord(id, activeLink == null ? page : activeLink,
                            System.currentTimeMillis(), new File(recordsFolder(), id));
                    currentRecord = record;
                    records.add(0, record);
                    cursor = 0;
                    scanned = 0;
                    media.clear();
                    for (Animatable animation : animations) animation.stop();
                    animations.clear();
                    renderResults();
                    updateResultLabels();
                    if (!persistRecord(record)) {
                        records.remove(record);
                        currentRecord = null;
                        return;
                    }
                }
                scanning = true;
                requestWaits = 0;
                scanButton.setText("暂停扫描");
                startPage();
            }
        });
        controls.addView(scanButton, new LinearLayout.LayoutParams(0, dp(48), 1));

        resultAnchor = label("提取结果", 20, DARK, true);
        LinearLayout.LayoutParams resultHeadParams = new LinearLayout.LayoutParams(-1, -2);
        resultHeadParams.topMargin = dp(28);
        content.addView(resultAnchor, resultHeadParams);
        resultSummary = label("提取后自动显示图片与动图预览", 11, MUTED, false);
        content.addView(resultSummary);
        LinearLayout filters = new LinearLayout(this);
        LinearLayout.LayoutParams filtersParams = new LinearLayout.LayoutParams(-1, -2);
        filtersParams.topMargin = dp(15);
        filtersParams.bottomMargin = dp(14);
        content.addView(filters, filtersParams);
        allFilter = filter("全部 0", "all", filters);
        gifFilter = filter("动图 0", "gif", filters);
        imageFilter = filter("图片 0", "image", filters);
        emptyResults = label("还没有提取结果。打开作品并扫描评论后，图片和动态素材会显示在这里。", 12, MUTED, false);
        emptyResults.setPadding(dp(18), dp(25), dp(18), dp(25));
        emptyResults.setBackground(shape(Color.WHITE, 16));
        content.addView(emptyResults);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results);

        LinearLayout saveBar = new LinearLayout(this);
        saveBar.setGravity(Gravity.CENTER_VERTICAL);
        saveBar.setPadding(dp(18), dp(8), dp(18), dp(8));
        saveBar.setBackgroundColor(Color.WHITE);
        readerRoot.addView(saveBar, new LinearLayout.LayoutParams(-1, dp(78)));
        countLabel = label("已提取 0 个", 13, DARK, true);
        saveBar.addView(countLabel, new LinearLayout.LayoutParams(0, -2, 1));
        Button saveAll = new Button(this);
        saveAll.setAllCaps(false);
        saveAll.setText("保存全部");
        saveAll.setTextColor(Color.WHITE);
        saveAll.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        saveAll.setBackground(shape(DARK, 17));
        saveAll.setOnClickListener(v -> {
            ArrayList<Media> snapshot = new ArrayList<>(media);
            worker.execute(() -> {
                int saved = 0;
                for (Media item : snapshot) {
                    try { saveToDownloads(item); saved++; } catch (Exception ignored) { }
                }
                int success = saved;
                int failed = snapshot.size() - saved;
                runOnUiThread(() -> showSaveResult(snapshot.isEmpty() ? "没有可保存的文件"
                        : failed == 0 ? "保存成功：" + success + " 个文件已写入下载/DouYinGetting"
                        : success == 0 ? "保存失败：" + failed + " 个文件未能保存"
                        : "部分保存成功：成功 " + success + " 个，失败 " + failed + " 个"));
            });
        });
        saveBar.addView(saveAll, new LinearLayout.LayoutParams(dp(112), dp(46)));
        updateResultLabels();
    }

    private void updateScanCard() {
        if (scanProgress == null) return;
        scanProgress.setVisibility(scanning ? View.VISIBLE : View.GONE);
        if (scanning && currentRecord != null) {
            String counts = xhsMode
                    ? "主评论 " + currentRecord.mainScanned + " · 回复 " + currentRecord.replyScanned + " · 合计 " + currentRecord.scanned
                    : "主评论 " + currentRecord.scanned;
            scanCountLine.setText(counts);
            scanCountLine.setVisibility(View.VISIBLE);
        } else scanCountLine.setVisibility(View.GONE);
    }

    private TextView filter(String text, String type, LinearLayout parent) {
        TextView button = label(text, 11, type.equals(selectedFilter) ? Color.WHITE : MUTED, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setBackground(shape(type.equals(selectedFilter) ? DARK : Color.rgb(236, 242, 239), 18));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.rightMargin = dp(7);
        parent.addView(button, params);
        button.setOnClickListener(v -> {
            selectedFilter = type;
            renderResults();
        });
        return button;
    }

    private void setupBrowser() {
        browser = new WebView(this);
        browser.setOnTouchListener((view, event) -> {
            if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(
                    event.getActionMasked() != MotionEvent.ACTION_UP && event.getActionMasked() != MotionEvent.ACTION_CANCEL);
            return false;
        });
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setUseWideViewPort(true);
        browser.getSettings().setLoadWithOverviewMode(true);
        browser.getSettings().setBuiltInZoomControls(true);
        browser.getSettings().setDisplayZoomControls(false);
        browser.getSettings().setUserAgentString(DESKTOP_USER_AGENT);
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser, true);
        try (InputStream source = getAssets().open("xhs-capture.js"); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = source.read(chunk)) != -1) buffer.write(chunk, 0, count);
            xhsCaptureScript = buffer.toString(StandardCharsets.UTF_8.name());
            xhsCaptureSupported = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);
            if (xhsCaptureSupported) WebViewCompat.addDocumentStartJavaScript(browser, xhsCaptureScript,
                    Collections.singleton("https://www.xiaohongshu.com"));
        } catch (Exception error) {
            xhsCaptureSupported = false;
        }
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (!xhsMode || !request.isForMainFrame()) return false;
                if ("https".equals(url.getScheme()) && "www.xiaohongshu.com".equals(url.getHost())) {
                    if (url.getPath() != null && (url.getPath().contains("/discovery/item/") || url.getPath().contains("/explore/")))
                        lastXhsPage = url.toString();
                    return false;
                }
                if (!"xhsdiscover".equals(url.getScheme())) return false;
                if (++xhsAppRedirects == 1) {
                    Matcher note = Pattern.compile("[a-fA-F0-9]{24}").matcher(url.toString());
                    if (note.find()) {
                        String noteId = note.group();
                        Uri previous = lastXhsPage == null ? null : Uri.parse(lastXhsPage);
                        Uri.Builder page = Uri.parse("https://www.xiaohongshu.com/explore/" + noteId).buildUpon();
                        String token = previous == null ? null : previous.getQueryParameter("xsec_token");
                        if (token != null) page.appendQueryParameter("xsec_token", token);
                        page.appendQueryParameter("xsec_source", "app_share");
                        view.loadUrl(page.build().toString());
                        status.setText("已拦截小红书 App 跳转，正在打开网页笔记");
                        return true;
                    }
                }
                status.setText("小红书网页要求在官方 App 中打开；此页面暂无法读取评论");
                return true;
            }
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
                if ("about:blank".equals(url)) return;
                if (xhsMode) {
                    if (!xhsCaptureSupported && xhsCaptureScript != null && url.startsWith("https://www.xiaohongshu.com/"))
                        browser.evaluateJavascript(xhsCaptureScript, null);
                    status.setText(url.contains("/login") ? "请在小红书网页中完成登录，再打开笔记"
                            : "笔记已打开。请进入评论区，再扫描评论素材");
                } else status.setText("作品已打开。请在网页中登录并进入评论区，再扫描主评论");
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
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private File recordsFolder() {
        return new File(getFilesDir(), "records");
    }

    private void loadRecords() {
        File[] folders = recordsFolder().listFiles();
        if (folders == null) folders = new File[0];
        for (File folder : folders) {
            if (!folder.isDirectory()) continue;
            File manifest = new File(folder, "manifest.json");
            if (!manifest.isFile()) continue;
            try {
                JSONObject data = new JSONObject(new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8));
                ScanRecord record = new ScanRecord(folder.getName(), data.getString("link"), data.getLong("createdAt"), folder);
                record.scanned = data.optInt("scanned");
                record.mainScanned = data.optInt("mainScanned");
                record.replyScanned = data.optInt("replyScanned");
                record.cursor = data.optLong("cursor");
                record.complete = data.optBoolean("complete");
                JSONArray saved = data.optJSONArray("media");
                boolean repaired = false;
                if (saved != null) for (int i = 0; i < saved.length(); i++) {
                    JSONObject entry = saved.optJSONObject(i);
                    if (entry == null) continue;
                    String name = entry.optString("file");
                    if (!name.equals(new File(name).getName())) continue;
                    File file = new File(folder, name);
                    if (!file.isFile()) continue;
                    String mime = entry.optString("mime");
                    boolean wasVideo = "video/mp4".equals(mime);
                    if ("video/mp4".equals(mime)) {
                        try {
                            byte[] header = new byte[12];
                            int count;
                            try (InputStream stream = new FileInputStream(file)) { count = stream.read(header); }
                            if (count == header.length) {
                                String actual = format(header);
                                if ("image/heic".equals(actual) || "image/avif".equals(actual)) {
                                    String extension = "image/heic".equals(actual) ? ".heic" : ".avif";
                                    File corrected = new File(folder, name.substring(0, name.length() - 4) + extension);
                                    Files.move(file.toPath(), corrected.toPath());
                                    file = corrected;
                                    mime = actual;
                                    repaired = true;
                                }
                            }
                        } catch (Exception ignored) { }
                    }
                    String hash = entry.optString("hash");
                    record.hashes.add(hash);
                    record.items.add(new Media(file, hash, mime, entry.optString("commentId"),
                            entry.optString("kind"), null, entry.optBoolean("animated") && (!wasVideo || "video/mp4".equals(mime))));
                }
                if (repaired) persistRecord(record);
                records.add(record);
            } catch (Exception ignored) { }
        }
        File legacy = new File(getFilesDir(), "media");
        File[] oldFiles = legacy.listFiles();
        if (oldFiles != null && oldFiles.length > 0) {
            ScanRecord old = new ScanRecord("legacy", "旧版提取结果（来源链接未知）", legacy.lastModified(), legacy);
            old.complete = true;
            for (File file : oldFiles) {
                if (!file.isFile()) continue;
                String name = file.getName().toLowerCase(Locale.ROOT);
                String mime = name.endsWith(".gif") ? "image/gif" : name.endsWith(".png") ? "image/png"
                        : name.endsWith(".jpg") ? "image/jpeg" : name.endsWith(".webp") ? "image/webp" : null;
                if (mime == null) continue;
                old.items.add(new Media(file, name, mime, "", "旧版", null, mime.equals("image/gif")));
            }
            if (!old.items.isEmpty()) records.add(old);
        }
        records.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
    }

    private boolean persistRecord(ScanRecord record) {
        try {
            JSONObject data = new JSONObject();
            data.put("id", record.id);
            data.put("link", record.link);
            data.put("createdAt", record.createdAt);
            data.put("scanned", record.scanned);
            data.put("mainScanned", record.mainScanned);
            data.put("replyScanned", record.replyScanned);
            data.put("cursor", record.cursor);
            data.put("complete", record.complete);
            JSONArray saved = new JSONArray();
            synchronized (record) {
                for (Media item : record.items) {
                    JSONObject entry = new JSONObject();
                    entry.put("file", item.file.getName());
                    entry.put("hash", item.hash);
                    entry.put("mime", item.mime);
                    entry.put("commentId", item.commentId);
                    entry.put("kind", item.kind);
                    entry.put("animated", item.animated);
                    saved.put(entry);
                }
            }
            data.put("media", saved);
            if (!record.folder.isDirectory() && !record.folder.mkdirs()) throw new Exception("无法创建记录目录");
            File temporary = new File(record.folder, "manifest.tmp");
            Files.write(temporary.toPath(), data.toString().getBytes(StandardCharsets.UTF_8));
            Files.move(temporary.toPath(), new File(record.folder, "manifest.json").toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception error) {
            runOnUiThread(() -> {
                if (currentRecord == record) status.setText("记录暂存失败：" + error.getMessage());
            });
            return false;
        }
    }

    private boolean deleteRecordFiles(ScanRecord record) {
        File root = recordsFolder();
        try {
            File parent = record.folder.getCanonicalFile().getParentFile();
            boolean storedRecord = parent.equals(root.getCanonicalFile());
            boolean legacyRecord = "legacy".equals(record.id)
                    && record.folder.getCanonicalFile().equals(new File(getFilesDir(), "media").getCanonicalFile());
            if (!storedRecord && !legacyRecord) return false;
            File[] files = record.folder.listFiles();
            if (files != null) for (File file : files) {
                if (!file.isFile() || !file.delete()) return false;
            }
            return !record.folder.exists() || record.folder.delete();
        } catch (Exception error) { return false; }
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
        String link = extractLink(input.getText().toString(), xhsMode);
        if (link == null) {
            status.setText(xhsMode ? "没有找到小红书笔记链接" : "没有找到抖音链接");
            return;
        }
        activeLink = link;
        xhsAppRedirects = 0;
        lastXhsPage = null;
        currentRecord = null;
        viewingSavedRecord = false;
        commentRequest = null;
        lastCommentPath = null;
        cursor = 0;
        scanned = 0;
        xhsCommentIds.clear();
        requestWaits = 0;
        requestToken++;
        scanning = false;
        scanButton.setText(xhsMode ? "扫描评论" : "扫描主评论");
        media.clear();
        for (Animatable animation : animations) animation.stop();
        animations.clear();
        results.removeAllViews();
        resultRow = null;
        displayedCount = 0;
        selectedFilter = "all";
        updateResultLabels();
        renderResults();
        browserPanel.setVisibility(View.VISIBLE);
        webToggle.setText("收起网页  ▴");
        input.clearFocus();
        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
        browser.loadUrl(link);
        readerScroll.post(() -> readerScroll.smoothScrollTo(0, browserPanel.getTop()));
    }

    private static String extractLink(String text, boolean xhs) {
        Matcher matcher = (xhs ? XHS_LINK : LINK).matcher(text.replace("\\:", ":"));
        return matcher.find() ? matcher.group().replaceAll("[，。,.]+$", "") : null;
    }

    private void copyLink(String link) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            showSaveResult("复制失败：剪贴板不可用");
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("作品链接", link));
        showSaveResult("链接已复制");
    }

    private static boolean isXhsLink(String link) {
        return link.contains("xiaohongshu.com/") || link.contains("xhslink.cn/");
    }

    private void toggleXhsScan() {
        if (scanning) {
            scanning = false;
            requestToken++;
            scanButton.setText("继续扫描");
            status.setText("扫描已暂停，已提取的图片保留在暂存记录中");
            return;
        }
        String page = browser.getUrl();
        if (page == null || !page.startsWith("https://www.xiaohongshu.com/")
                || !page.matches(".*(?:/explore/|/discovery/item/)[a-fA-F0-9]{24}.*")) {
            status.setText("请先在网页中打开小红书笔记");
            return;
        }
        if (xhsCaptureScript == null) {
            status.setText("评论读取组件未加载，请重启 App 后重试");
            return;
        }
        if (currentRecord == null || currentRecord.complete) {
            String id = UUID.randomUUID().toString();
            ScanRecord record = new ScanRecord(id, activeLink == null ? page : activeLink,
                    System.currentTimeMillis(), new File(recordsFolder(), id));
            currentRecord = record;
            records.add(0, record);
            scanned = 0;
            xhsCommentIds.clear();
            xhsMainDone = false;
            media.clear();
            for (Animatable animation : animations) animation.stop();
            animations.clear();
            renderResults();
            updateResultLabels();
            if (!persistRecord(record)) {
                records.remove(record);
                currentRecord = null;
                return;
            }
        }
        scanning = true;
        xhsIdlePolls = 0;
        scanButton.setText("暂停扫描");
        status.setText("正在读取小红书评论媒体…");
        int token = ++requestToken;
        browser.evaluateJavascript("window.__xhsCapture?.seed()", ignored -> pollXhs(token));
    }

    private void pollXhs(int token) {
        if (!scanning || token != requestToken || !xhsMode) return;
        browser.evaluateJavascript("window.__xhsCapture?.snapshot();JSON.stringify(window.__xhsCapture?.drain() || [])", value -> {
            if (!scanning || token != requestToken || !xhsMode) return;
            try {
                Object parsed = new JSONTokener(value).nextValue();
                if (parsed instanceof String) parsed = new JSONTokener((String) parsed).nextValue();
                JSONArray packets = (JSONArray) parsed;
                if (packets.length() > 0) {
                    xhsIdlePolls = 0;
                    ScanRecord record = currentRecord;
                    worker.execute(() -> processXhsPackets(record, packets, token));
                    return;
                }
            } catch (Exception error) {
                scanning = false;
                scanButton.setText("继续扫描");
                status.setText("扫描暂停：评论响应解析失败");
                return;
            }
            browser.evaluateJavascript("window.__xhsCapture?.expandReplies()", expanded -> {
                if (!scanning || token != requestToken) return;
                if ("true".equals(expanded)) xhsIdlePolls = 0;
                else xhsIdlePolls++;
                if (xhsMainDone && xhsIdlePolls >= 10) {
                    finishXhsScan(currentRecord);
                    return;
                }
                if (xhsIdlePolls == 30) {
                    status.setText("正在等待后续评论；如网页不再加载，请在评论区向下滑动");
                    browserPanel.setVisibility(View.VISIBLE);
                    webToggle.setText("收起网页  ▴");
                    readerScroll.post(() -> readerScroll.smoothScrollTo(0, browserPanel.getTop()));
                }
                if (xhsIdlePolls % 4 == 0) browser.evaluateJavascript("window.__xhsCapture?.scroll()", null);
                browser.postDelayed(() -> pollXhs(token), 500);
            });
        });
    }

    private void processXhsPackets(ScanRecord record, JSONArray packets, int token) {
        if (record == null) return;
        for (int p = 0; p < packets.length(); p++) {
            JSONObject packet = packets.optJSONObject(p);
            if (packet == null) continue;
            JSONArray comments = packet.optJSONArray("comments");
            if (comments == null) continue;
            for (int i = 0; i < comments.length(); i++) {
                JSONObject comment = comments.optJSONObject(i);
                if (comment == null) continue;
                String id = comment.optString("id");
                if (!id.isEmpty() && !xhsCommentIds.add(id)) continue;
                if (!packet.optBoolean("dom") && !id.isEmpty()) {
                    if (comment.optBoolean("reply")) record.replyScanned++;
                    else record.mainScanned++;
                    record.scanned = record.mainScanned + record.replyScanned;
                }
                JSONArray pictures = comment.optJSONArray("pictures");
                if (pictures == null) continue;
                for (int j = 0; j < pictures.length(); j++) {
                    JSONObject picture = pictures.optJSONObject(j);
                    if (picture != null) {
                        download(record, picture.optString("url", null), id, "图片");
                        download(record, picture.optString("video", null), id, "视频");
                    }
                }
            }
            if (packet.optBoolean("replies")) continue;
            if (!packet.optBoolean("hasMore", true)) xhsMainDone = true;
            persistRecord(record);
        }
        runOnUiThread(() -> {
            if (currentRecord != record || token != requestToken) return;
            status.setText("扫描中 · 已发现 " + media.size() + " 个素材");
            if (scanning) {
                browser.evaluateJavascript("window.__xhsCapture?.scroll()", null);
                browser.postDelayed(() -> pollXhs(token), 800);
            }
        });
    }

    private void finishXhsScan(ScanRecord record) {
        if (record == null || !scanning) return;
        scanning = false;
        record.complete = true;
        persistRecord(record);
        scanButton.setText("扫描完成");
        status.setText("已扫描 " + record.scanned + " 条评论（主评论 " + record.mainScanned + "、回复 " + record.replyScanned + "），找到 " + media.size() + " 个不同媒体（扫描完成）");
        browserPanel.setVisibility(View.GONE);
        webToggle.setText("网页操作  ▾");
        if (showingReader) new AlertDialog.Builder(this)
                .setTitle("小红书评论扫描完成")
                .setMessage("已扫描 " + record.scanned + " 条评论（主评论 " + record.mainScanned + "、回复 " + record.replyScanned + "），提取 " + media.size() + " 个不同素材。结果已暂存。")
                .setPositiveButton("查看结果", (dialog, which) -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()))
                .show();
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
                if (pageData == null) {
                    scanning = false;
                    scanButton.setText("继续扫描");
                    status.setText("评论接口返回了无法读取的内容，扫描已暂停；已提取的素材仍在暂存记录中");
                    updateScanCard();
                    new AlertDialog.Builder(this)
                            .setTitle("评论扫描已暂停")
                            .setMessage("抖音评论接口返回了无法读取的内容。已提取的素材仍在暂存记录中。可以重试当前页；如果反复出现，请稍后再试。")
                            .setPositiveButton("继续扫描", (dialog, which) -> scanButton.performClick())
                            .setNegativeButton("稍后再试", null)
                            .show();
                    return;
                }
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
                ScanRecord record = currentRecord;
                int scannedNow = scanned;
                worker.execute(() -> processPage(record, comments, more, next, scannedNow));
            } catch (Exception error) {
                scanning = false;
                scanButton.setText("继续扫描");
                status.setText("扫描暂停：" + error.getMessage());
                updateScanCard();
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

    private void processPage(ScanRecord record, JSONArray comments, boolean more, long next, int scannedNow) {
        if (record == null) return;
        for (int i = 0; i < comments.length(); i++) {
            JSONObject comment = comments.optJSONObject(i);
            if (comment == null) continue;
            String commentId = comment.optString("cid");
            JSONArray images = comment.optJSONArray("image_list");
            if (images != null) for (int j = 0; j < images.length(); j++) {
                JSONObject image = images.optJSONObject(j);
                if (image != null) download(record, url(image.optJSONObject("origin_url")), commentId, "图片");
            }
            JSONObject sticker = comment.optJSONObject("sticker");
            if (sticker != null) {
                String source = url(sticker.optJSONObject("animate_url"));
                if (source == null) source = url(sticker.optJSONObject("static_url"));
                download(record, source, commentId, "表情");
            }
        }
        record.scanned = scannedNow;
        record.cursor = next;
        record.complete = !more;
        persistRecord(record);
        runOnUiThread(() -> {
            if (currentRecord != record) return;
            status.setText(more ? "扫描中 · 已发现 " + media.size() + " 个素材"
                    : "已扫描 " + record.scanned + " 条主评论，找到 " + media.size() + " 个不同媒体（扫描完成）");
            if (scanning && more) {
                browser.postDelayed(this::startPage, 350);
            } else {
                scanning = false;
                scanButton.setText(more ? "继续扫描" : "扫描完成");
                updateScanCard();
                if (!more) {
                    browserPanel.setVisibility(View.GONE);
                    webToggle.setText("网页操作  ▾");
                    readerScroll.post(() -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()));
                    if (showingReader) new AlertDialog.Builder(this)
                            .setTitle("主评论扫描完成")
                            .setMessage("已扫描 " + record.scanned + " 条主评论，提取 " + media.size() + " 个不同素材。结果已暂存，可预览或保存。")
                            .setPositiveButton("查看结果", (dialog, which) -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()))
                            .show();
                }
            }
        });
    }

    private void download(ScanRecord record, String source, String commentId, String kind) {
        if (source == null || source.isEmpty()) return;
        try {
            boolean xhs = isXhsLink(record.link);
            String original = xhs && !"视频".equals(kind) ? originalXhsUrl(source) : source;
            byte[] bytes = null;
            if (!original.equals(source)) {
                try {
                    bytes = readMedia(original, true);
                    if (format(bytes) == null) bytes = null;
                } catch (Exception ignored) { }
            }
            if (bytes == null) bytes = readMedia(source, xhs);
            String mime = format(bytes);
            if (mime == null) return;
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b & 0xff));
            String hash = hex.toString();
            synchronized (record) {
                if (!record.hashes.add(hash)) return;
            }
            String extension = mime.equals("video/mp4") ? ".mp4" : mime.equals("image/heic") ? ".heic" : mime.equals("image/avif") ? ".avif" : mime.equals("image/jpeg") ? ".jpg" : mime.equals("image/png") ? ".png" : mime.equals("image/gif") ? ".gif" : ".webp";
            if (!record.folder.isDirectory() && !record.folder.mkdirs()) throw new Exception("无法创建记录目录");
            File file = new File(record.folder, hash.substring(0, 20) + extension);
            try (OutputStream out = new FileOutputStream(file)) { out.write(bytes); }
            Drawable thumbnail = decodeThumbnail(file);
            Media item = new Media(file, hash, mime, commentId, kind, thumbnail, thumbnail instanceof Animatable);
            synchronized (record) { record.items.add(item); }
            persistRecord(record);
            runOnUiThread(() -> {
                if (currentRecord != record || !showingReader) return;
                if (media.contains(item)) return;
                media.add(item);
                updateResultLabels();
                if (matchesFilter(item)) addTile(item);
                if (media.size() == 1) readerScroll.postDelayed(() -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()), 200);
            });
        } catch (Exception ignored) { }
    }

    private static String originalXhsUrl(String source) {
        Uri uri = Uri.parse(source);
        String host = uri.getHost();
        String path = uri.getPath();
        if (host == null || path == null || !(host.endsWith(".xhscdn.com")
                || host.endsWith(".rednotecdn.com") || host.equals("ci.xiaohongshu.com"))) return source;
        int start = -1;
        for (String folder : new String[]{"/comment/", "/spectrum/", "/notes_pre_post/"}) {
            int found = path.indexOf(folder);
            if (found >= 0 && (start < 0 || found < start)) start = found;
        }
        if (start < 0) return source;
        String original = path.substring(start).split("!", 2)[0];
        return "https://ci.xiaohongshu.com" + original;
    }

    private static byte[] readMedia(String source, boolean xhs) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(source).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Referer", xhs ? "https://www.xiaohongshu.com/" : "https://www.douyin.com/");
        try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = stream.read(chunk)) != -1) buffer.write(chunk, 0, count);
            return buffer.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private Drawable decodeThumbnail(File file) {
        try {
            if (file.getName().endsWith(".mp4")) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(file.getAbsolutePath());
                    Bitmap frame = null;
                    for (long time : new long[]{0, 100000, 500000}) {
                        try {
                            frame = retriever.getScaledFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST, dp(120), dp(120));
                            if (frame != null) break;
                        } catch (Exception ignored) { }
                    }
                    return frame == null ? null : new BitmapDrawable(getResources(), frame);
                } finally { retriever.release(); }
            }
            return ImageDecoder.decodeDrawable(ImageDecoder.createSource(file), (decoder, info, sourceInfo) -> {
                int width = info.getSize().getWidth();
                int height = info.getSize().getHeight();
                float scale = Math.min(1f, dp(120) / (float) Math.max(width, height));
                decoder.setTargetSize(Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
            });
        } catch (Exception ignored) { return null; }
    }

    private static String format(byte[] b) {
        if (b.length < 12) return null;
        if (b[0] == (byte) 0xff && b[1] == (byte) 0xd8) return "image/jpeg";
        if (b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F') return "image/gif";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        if (b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            String brand = new String(b, 8, 4, StandardCharsets.US_ASCII);
            if (brand.equals("heic") || brand.equals("heix") || brand.equals("hevc") || brand.equals("hevx") || brand.equals("mif1")) return "image/heic";
            if (brand.equals("avif") || brand.equals("avis")) return "image/avif";
            return "video/mp4";
        }
        return null;
    }

    private boolean matchesFilter(Media item) {
        return "all".equals(selectedFilter) || ("gif".equals(selectedFilter) == item.animated);
    }

    private void updateResultLabels() {
        int gifCount = 0;
        for (Media item : media) if (item.animated) gifCount++;
        allFilter.setText("全部 " + media.size());
        gifFilter.setText("动态 " + gifCount);
        imageFilter.setText("图片 " + (media.size() - gifCount));
        resultSummary.setText(media.isEmpty() ? "提取后自动显示图片与动态媒体预览" : "已找到 " + media.size() + " 个不重复素材 · 默认预览中");
        countLabel.setText("已提取 " + media.size() + " 个");
        emptyResults.setText(media.isEmpty() ? "还没有提取结果。打开作品并扫描评论后，图片和动态素材会显示在这里。" : "当前分类没有结果");
        emptyResults.setVisibility(displayedCount == 0 ? View.VISIBLE : View.GONE);
    }

    private void renderResults() {
        for (Animatable animation : animations) animation.stop();
        animations.clear();
        results.removeAllViews();
        resultRow = null;
        displayedCount = 0;
        for (Media item : media) if (matchesFilter(item)) addTile(item);
        emptyResults.setText(media.isEmpty() ? "还没有提取结果。打开作品并扫描评论后，图片和动态素材会显示在这里。" : "当前分类没有结果");
        emptyResults.setVisibility(displayedCount == 0 ? View.VISIBLE : View.GONE);
        for (TextView button : new TextView[]{allFilter, gifFilter, imageFilter}) {
            boolean active = button == allFilter && "all".equals(selectedFilter)
                    || button == gifFilter && "gif".equals(selectedFilter)
                    || button == imageFilter && "image".equals(selectedFilter);
            button.setTextColor(active ? Color.WHITE : MUTED);
            button.setBackground(shape(active ? DARK : Color.rgb(236, 242, 239), 18));
        }
    }

    private void addTile(Media item) {
        if (displayedCount % 3 == 0) {
            resultRow = new LinearLayout(this);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(8);
            results.addView(resultRow, rowParams);
        }
        int tileWidth = (getResources().getDisplayMetrics().widthPixels - dp(52)) / 3;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(shape(Color.WHITE, 15));
        card.setClipToOutline(true);
        card.setElevation(dp(2));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(tileWidth, -2);
        if (displayedCount % 3 != 0) cardParams.leftMargin = dp(8);
        resultRow.addView(card, cardParams);
        LinearLayout row = resultRow;
        FrameLayout art = new FrameLayout(this);
        art.setBackgroundColor(item.animated ? Color.rgb(255, 232, 238) : Color.rgb(227, 241, 246));
        card.addView(art, new LinearLayout.LayoutParams(-1, tileWidth));
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(dp(5), dp(5), dp(5), dp(5));
        if (item.thumbnail != null) {
            bindThumbnail(image, item.thumbnail);
        } else {
            worker.execute(() -> {
                Drawable thumbnail = decodeThumbnail(item.file);
                item.thumbnail = thumbnail;
                runOnUiThread(() -> {
                    if (thumbnail != null && row.getParent() == results && showingReader) bindThumbnail(image, thumbnail);
                });
            });
        }
        art.addView(image, new FrameLayout.LayoutParams(-1, -1));
        if ("video/mp4".equals(item.mime)) {
            TextView play = label("▶ 点击播放", 11, DARK, true);
            play.setGravity(Gravity.CENTER);
            play.setBackground(shape(Color.WHITE, 12));
            FrameLayout.LayoutParams playParams = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
            play.setPadding(dp(8), dp(5), dp(8), dp(5));
            art.addView(play, playParams);
        }
        TextView badge = label("video/mp4".equals(item.mime) ? "视频" : item.animated ? ("image/gif".equals(item.mime) ? "GIF" : "动图") : "图片", 10, Color.WHITE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(6), dp(3), dp(6), dp(3));
        badge.setBackground(shape(item.animated ? Color.rgb(237, 98, 136) : DARK, 5));
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.RIGHT | Gravity.TOP);
        badgeParams.setMargins(0, dp(6), dp(6), 0);
        art.addView(badge, badgeParams);
        TextView caption = label("video/mp4".equals(item.mime) ? ("表情".equals(item.kind) ? "表情 · 视频" : "评论视频") : item.animated ? "表情 · 动图" : "评论图片", 10, Color.rgb(91, 110, 111), false);
        caption.setPadding(dp(7), dp(7), dp(4), dp(8));
        card.addView(caption);
        card.setOnClickListener(v -> preview(item));
        displayedCount++;
        emptyResults.setVisibility(View.GONE);
    }

    private void bindThumbnail(ImageView image, Drawable thumbnail) {
        image.setImageDrawable(thumbnail);
        if (thumbnail instanceof Animatable) {
            Animatable animation = (Animatable) thumbnail;
            animations.add(animation);
            if (showingReader) animation.start();
        }
    }

    private void preview(Media item) {
        try {
            if ("video/mp4".equals(item.mime)) {
                VideoView video = new VideoView(this);
                video.setVideoPath(item.file.getAbsolutePath());
                video.setOnPreparedListener(player -> video.start());
                video.setOnErrorListener((player, what, extra) -> {
                    showSaveResult("视频无法播放，可能是设备不支持该编码");
                    return true;
                });
                MediaController controls = new MediaController(this);
                video.setMediaController(controls);
                FrameLayout frame = new FrameLayout(this);
                frame.addView(video, new FrameLayout.LayoutParams(-1, dp(260)));
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle(item.file.getName()).setView(frame)
                        .setPositiveButton("保存此段", (ignored, which) -> worker.execute(() -> {
                            try {
                                saveToDownloads(item);
                                runOnUiThread(() -> showSaveResult("保存成功：" + item.file.getName()));
                            } catch (Exception error) {
                                runOnUiThread(() -> showSaveResult("保存失败：" + error.getMessage()));
                            }
                        }))
                        .setNegativeButton("关闭", null).create();
                dialog.setOnDismissListener(ignored -> video.stopPlayback());
                dialog.show();
                return;
            }
            Drawable drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(item.file));
            ImageView image = new ImageView(this);
            image.setImageDrawable(drawable);
            image.setAdjustViewBounds(true);
            new AlertDialog.Builder(this).setTitle(item.file.getName()).setView(image)
                    .setPositiveButton("保存此张", (dialog, which) -> worker.execute(() -> {
                        try {
                            saveToDownloads(item);
                            runOnUiThread(() -> showSaveResult("保存成功：" + item.file.getName()));
                        } catch (Exception error) {
                            runOnUiThread(() -> showSaveResult("保存失败：" + error.getMessage()));
                        }
                    }))
                    .setNegativeButton("关闭", null).show();
            if (drawable instanceof Animatable) ((Animatable) drawable).start();
        } catch (Exception error) {
            status.setText("预览失败：" + error.getMessage());
        }
    }

    private void showSaveResult(String message) {
        status.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
        for (Animatable animation : animations) animation.stop();
        worker.shutdownNow();
        browser.destroy();
        super.onDestroy();
    }

    @Override protected void onPause() {
        for (Animatable animation : animations) animation.stop();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (showingReader) for (Animatable animation : animations) animation.start();
    }

    @Override public void onBackPressed() {
        if (showingRecords) showReader();
        else if (showingReader) showHome();
        else super.onBackPressed();
    }
}
