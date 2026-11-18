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
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Switch;
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
    private boolean savedRecordPageOpened;
    private boolean loadingSavedRecordPage;
    private boolean xhsMode;
    private TextView readerHeading;
    private TextView inputHeading;
    private TextView browserHint;
    private Button codeLogin;
    private WebView browser;
    private EditText input;
    private TextView status;
    private TextView scanCountLine;
    private TextView scanDetails;
    private ProgressBar scanProgress;
    private LinearLayout results;
    private Button scanButton;
    private PopupWindow noticePopup;
    private View favoriteNotice;
    private LinearLayout readerRoot;
    private LinearLayout browserPanel;
    private TextView webToggle;
    private TextView webToggleArrow;
    private LinearLayout resultRow;
    private ScrollView readerScroll;
    private TextView countLabel;
    private TextView saveSub;
    private TextView resultSummary;
    private TextView selectionAction;
    private Button saveButton;
    private Button shareSelectedButton;
    private TextView allFilter;
    private TextView gifFilter;
    private TextView imageFilter;
    private TextView emptyResults;
    private View resultAnchor;
    private final ArrayList<Animatable> animations = new ArrayList<>();
    private String selectedFilter = "all";
    private final HashSet<Media> selectedMedia = new HashSet<>();
    private boolean selectingMedia;
    private int displayedCount;
    private boolean showingReader;
    private boolean showingRecords;
    private boolean showingRecordPlatforms;
    private boolean showingSettings;
    private boolean showingFavorites;
    private FavoritesScreen favoriteScreen;
    private Runnable returnFromFavorites;
    private boolean recordsFromSettings;
    private int douyinPageDelayMs = 2000;
    private boolean douyinReadReplies;
    private volatile String commentRequest;
    private volatile String replyRequest;
    private volatile String lastCommentPath;
    private PermissionRequest pendingCameraRequest;
    private boolean scanning;
    private String retryReplyParent;
    private long retryReplyCursor;
    private int requestWaits;
    private int requestToken;
    private int pageRetries;
    private long cursor;
    private int scanned;
    private int xhsIdlePolls;
    private volatile boolean xhsMainDone;
    private boolean xhsCaptureSupported;
    private String xhsCaptureScript;
    private String lastXhsPage;
    private int xhsAppRedirects;
    private static final int BG = Color.rgb(247, 248, 246);
    private static final int DARK = Color.rgb(25, 47, 55);
    private static final int GREEN = Color.rgb(28, 158, 120);
    private static final int MUTED = Color.rgb(125, 143, 143);
    // 读取页 / 记录页设计令牌
    private static final int SURFACE = Color.WHITE;
    private static final int LINE = Color.rgb(233, 236, 234);
    private static final int INK = Color.rgb(23, 37, 43);
    private static final int INK2 = Color.rgb(95, 110, 110);
    private static final int INK3 = Color.rgb(154, 166, 166);
    private static final int ACCENT = Color.rgb(20, 128, 107);
    private static final int ACCENT_SOFT = Color.rgb(232, 242, 238);
    private static final int TRACK = Color.rgb(237, 238, 234);
    private static final int DANGER = Color.rgb(178, 100, 92);

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

    private static class FailedMedia {
        final String source;
        final String commentId;
        final String kind;

        FailedMedia(String source, String commentId, String kind) {
            this.source = source;
            this.commentId = commentId;
            this.kind = kind;
        }
    }

    private static class ScanRecord {
        final String id;
        final String link;
        final long createdAt;
        final File folder;
        final ArrayList<Media> items = new ArrayList<>();
        final HashSet<String> hashes = new HashSet<>();
        final HashSet<String> xhsCommentIds = new HashSet<>();
        boolean xhsNeedsRecount;
        volatile boolean processingPage;
        long cursor;
        int scanned;
        int mainScanned;
        int replyScanned;
        int emptyReplyLists;
        int loggedEmptyReplyLists;
        final java.util.LinkedHashMap<String, FailedMedia> failedMedia = new java.util.LinkedHashMap<>();
        final java.util.LinkedHashMap<String, Long> failedReplies = new java.util.LinkedHashMap<>();
        final ArrayList<String> scanIssues = new ArrayList<>();
        int issueCount;

        synchronized void addIssue(String issue) {
            issueCount++;
            if (scanIssues.size() >= 50) scanIssues.remove(0);
            scanIssues.add(issue);
        }
        boolean includeReplies;
        boolean mainDone;
        final ArrayList<String> replyParents = new ArrayList<>();
        int replyIndex;
        long replyCursor;
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
        douyinPageDelayMs = getPreferences(MODE_PRIVATE).getInt("douyin_page_delay_ms", 2000);
        douyinReadReplies = getPreferences(MODE_PRIVATE).getBoolean("douyin_read_replies", false);
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

    private GradientDrawable card(int radius) {
        return outlined(radius, SURFACE);
    }

    private GradientDrawable outlined(int radius, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(1, LINE);
        return drawable;
    }

    private TextView segment(String text, String type, LinearLayout parent) {
        TextView button = label(text, 12, INK2, true);
        button.setGravity(Gravity.CENTER);
        parent.addView(button, new LinearLayout.LayoutParams(0, -1, 1));
        button.setOnClickListener(v -> {
            selectedFilter = type;
            renderResults();
        });
        return button;
    }

    private void showHome() {
        retryReplyParent = null;
        if (scanning) {
            scanning = false;
            requestToken++;
            scanButton.setText("继续扫描");
        }
        showingReader = false;
        showingRecords = false;
        showingRecordPlatforms = false;
        showingSettings = false;
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
        dock.addView(favoritesNavItem(), new LinearLayout.LayoutParams(0, -1, 1));
        dock.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout settingsNav = new LinearLayout(this);
        settingsNav.setOrientation(LinearLayout.VERTICAL);
        settingsNav.setGravity(Gravity.CENTER);
        TextView settingsIcon = label("⚙", 26, MUTED, false);
        settingsIcon.setGravity(Gravity.CENTER);
        settingsNav.addView(settingsIcon, new LinearLayout.LayoutParams(-1, dp(30)));
        TextView settingsLabel = label("设置", 12, MUTED, true);
        settingsLabel.setGravity(Gravity.CENTER);
        settingsNav.addView(settingsLabel);
        settingsNav.setOnClickListener(v -> showSettings());
        dock.addView(settingsNav, new LinearLayout.LayoutParams(0, -1, 1));
        ImageView homeIcon = new ImageView(this);
        homeIcon.setImageResource(R.drawable.home_nav);
        homeIcon.setElevation(dp(5));
        navArea.addView(homeIcon, new FrameLayout.LayoutParams(dp(72), dp(72), Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        TextView homeLabel = label("首页", 13, Color.rgb(94, 105, 232), true);
        homeLabel.setGravity(Gravity.CENTER);
        navArea.addView(homeLabel, new FrameLayout.LayoutParams(dp(80), dp(28), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        setContentView(root);
    }

    private void showSettings() {
        showingFavorites = false;
        showingSettings = true;
        showingReader = false;
        showingRecords = false;
        showingRecordPlatforms = false;
        for (Animatable animation : animations) animation.stop();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(20), dp(6), dp(20), dp(6));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(54)));
        TextView back = label("‹", 22, INK, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(card(10));
        back.setOnClickListener(v -> showHome());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView title = label("设置", 16, INK, true);
        title.setGravity(Gravity.CENTER);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        toolbar.addView(new View(this), new LinearLayout.LayoutParams(dp(34), dp(34)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(2), dp(20), dp(20));
        scroll.addView(page);

        page.addView(groupTitle("扫描", 6));
        LinearLayout scanCard = new LinearLayout(this);
        scanCard.setOrientation(LinearLayout.VERTICAL);
        scanCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        scanCard.setBackground(card(16));
        page.addView(scanCard);
        LinearLayout scanHead = new LinearLayout(this);
        scanHead.setGravity(Gravity.CENTER_VERTICAL);
        scanCard.addView(scanHead);
        scanHead.addView(label("翻页间隔", 14, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        scanHead.addView(label("抖音评论", 11, INK3, false));
        LinearLayout speedSwitch = new LinearLayout(this);
        speedSwitch.setOrientation(LinearLayout.HORIZONTAL);
        speedSwitch.setPadding(dp(3), dp(3), dp(3), dp(3));
        speedSwitch.setBackground(shape(TRACK, 10));
        LinearLayout.LayoutParams speedParams = new LinearLayout.LayoutParams(-1, dp(38));
        speedParams.topMargin = dp(12);
        scanCard.addView(speedSwitch, speedParams);
        addSpeedSegment(speedSwitch, 2000, "稳定 · 2 秒");
        addSpeedSegment(speedSwitch, 350, "快速 · 0.35 秒");
        TextView speedNote = label("快速模式扫描更快，但更容易触发评论接口暂停。选择会立即保存。", 11, INK3, false);
        speedNote.setLineSpacing(0, 1.35f);
        LinearLayout.LayoutParams speedNoteParams = new LinearLayout.LayoutParams(-1, -2);
        speedNoteParams.topMargin = dp(10);
        scanCard.addView(speedNote, speedNoteParams);
        LinearLayout replyRow = new LinearLayout(this);
        replyRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams replyParams = new LinearLayout.LayoutParams(-1, -2);
        replyParams.topMargin = dp(16);
        scanCard.addView(replyRow, replyParams);
        replyRow.addView(label("读取评论回复", 14, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        Switch replySwitch = new Switch(this);
        replySwitch.setChecked(douyinReadReplies);
        replySwitch.setOnCheckedChangeListener((button, checked) -> {
            douyinReadReplies = checked;
            getPreferences(MODE_PRIVATE).edit().putBoolean("douyin_read_replies", checked).apply();
        });
        replyRow.addView(replySwitch);
        scanCard.addView(label("开启后会逐条读取抖音主评论下的回复，扫描时间会增加。", 11, INK3, false));

        page.addView(groupTitle("本机数据", 18));
        LinearLayout dataCard = new LinearLayout(this);
        dataCard.setOrientation(LinearLayout.VERTICAL);
        dataCard.setBackground(card(16));
        page.addView(dataCard);
        dataCard.addView(settingRow("扫描记录", records.size() + " 条", true, v -> {
            recordsFromSettings = true;
            showRecordPlatforms();
        }));
        dataCard.addView(rowDivider());
        dataCard.addView(settingRow("收藏夹", "查看收藏", true, v -> openFavorites(this::showSettings)));
        dataCard.addView(rowDivider());
        dataCard.addView(settingRow("暂存占用", storageSize(), false, null));
        dataCard.addView(rowDivider());
        dataCard.addView(settingRow("保存位置", "下载 / DouYinGetting", false, null));

        page.addView(groupTitle("说明", 18));
        LinearLayout aboutCard = new LinearLayout(this);
        aboutCard.setOrientation(LinearLayout.VERTICAL);
        aboutCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        aboutCard.setBackground(card(16));
        page.addView(aboutCard);
        TextView note1 = label("登录在抖音 / 小红书的网页内完成，App 不读取手机上的账号；未登录时只能看到公开的少量评论。", 11, INK2, false);
        note1.setLineSpacing(0, 1.4f);
        aboutCard.addView(note1);
        TextView note2 = label("素材先暂存在 App 内，点「保存」后才写入下载目录。", 11, INK2, false);
        note2.setLineSpacing(0, 1.4f);
        LinearLayout.LayoutParams note2Params = new LinearLayout.LayoutParams(-1, -2);
        note2Params.topMargin = dp(8);
        aboutCard.addView(note2, note2Params);

        page.addView(groupTitle("关于", 18));
        LinearLayout aboutRowCard = new LinearLayout(this);
        aboutRowCard.setOrientation(LinearLayout.VERTICAL);
        aboutRowCard.setBackground(card(16));
        page.addView(aboutRowCard);
        aboutRowCard.addView(settingRow("版本", versionName(), false, null));

        TextView footnote = label("咻咿提取 · 仅处理你在网页中已登录可见的评论", 11, INK3, false);
        LinearLayout.LayoutParams footnoteParams = new LinearLayout.LayoutParams(-1, -2);
        footnoteParams.topMargin = dp(22);
        page.addView(footnote, footnoteParams);

        setContentView(root);
    }

    private TextView groupTitle(String text, int topMargin) {
        TextView view = label(text, 12, INK3, true);
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.topMargin = dp(topMargin);
        params.bottomMargin = dp(8);
        return view;
    }

    private LinearLayout settingRow(String name, String value, boolean chevron, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.addView(label(name, 14, INK, false), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(label(value, 12, INK3, false));
        if (chevron) {
            TextView arrow = label("›", 18, INK3, false);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(-2, -2);
            arrowParams.leftMargin = dp(6);
            row.addView(arrow, arrowParams);
        }
        if (click != null) row.setOnClickListener(click);
        return row;
    }

    private View rowDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, 1);
        params.setMargins(dp(16), 0, dp(16), 0);
        divider.setLayoutParams(params);
        return divider;
    }

    private String storageSize() {
        long bytes = 0;
        for (ScanRecord record : records) {
            File[] files = record.folder.listFiles();
            if (files == null) continue;
            for (File file : files) if (file.isFile()) bytes += file.length();
        }
        if (bytes <= 0) return "0 MB";
        double mb = bytes / (1024d * 1024d);
        return String.format(Locale.US, mb >= 10 ? "%.0f MB" : "%.1f MB", mb);
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception error) {
            return "-";
        }
    }

    private void addSpeedSegment(LinearLayout parent, int delay, String text) {
        boolean selected = douyinPageDelayMs == delay;
        TextView button = label(text, 12, selected ? INK : INK2, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(selected ? shape(SURFACE, 8) : null);
        parent.addView(button, new LinearLayout.LayoutParams(0, -1, 1));
        button.setOnClickListener(v -> {
            douyinPageDelayMs = delay;
            getPreferences(MODE_PRIVATE).edit().putInt("douyin_page_delay_ms", delay).apply();
            showSettings();
            if (delay == 350) Toast.makeText(this, "快速模式可能频繁暂停", Toast.LENGTH_LONG).show();
        });
    }

    private LinearLayout favoritesNavItem() {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        TextView icon = label("☆", 26, MUTED, false);
        icon.setGravity(Gravity.CENTER);
        item.addView(icon, new LinearLayout.LayoutParams(-1, dp(30)));
        TextView name = label("收藏", 12, MUTED, true);
        name.setGravity(Gravity.CENTER);
        item.addView(name);
        item.setOnClickListener(v -> openFavorites(this::showHome));
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
        clearMediaSelection();
        media.clear();
        for (Animatable animation : animations) animation.stop();
        animations.clear();
        selectedFilter = "all";
        renderResults();
        updateResultLabels();
        configureReaderMode();
        browserPanel.setVisibility(View.GONE);
        updateWebToggle();
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
        browserHint.setText(xhsMode ? "在网页中登录并打开笔记评论，扫描时会读取评论素材" : douyinReadReplies
                ? "在网页中登录并打开评论区，然后扫描主评论与回复" : "在网页中登录并打开评论区，然后扫描主评论");
        codeLogin.setVisibility(xhsMode ? View.GONE : View.VISIBLE);
        scanButton.setText(xhsMode || douyinReadReplies ? "扫描评论" : "扫描主评论");
        status.setText(xhsMode ? "粘贴笔记链接，打开网页后进入评论区" : "粘贴作品链接，打开网页后进入评论区");
    }

    private void showRecordPlatforms() {
        showingReader = false;
        showingRecords = false;
        showingSettings = false;
        showingRecordPlatforms = true;
        int douyinCount = 0;
        int xhsCount = 0;
        for (ScanRecord record : records) {
            if (isXhsLink(record.link)) xhsCount++;
            else douyinCount++;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(20), dp(6), dp(20), dp(6));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(54)));
        TextView back = label("‹", 22, INK, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(card(10));
        back.setOnClickListener(v -> showSettings());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView heading = label("选择平台", 16, INK, true);
        heading.setGravity(Gravity.CENTER);
        toolbar.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        toolbar.addView(new View(this), new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(18), dp(20), 0);
        root.addView(page);
        page.addView(label("选择要查看的扫描记录", 13, INK2, false));
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        choices.setBackground(card(16));
        LinearLayout.LayoutParams choicesParams = new LinearLayout.LayoutParams(-1, -2);
        choicesParams.topMargin = dp(16);
        page.addView(choices, choicesParams);
        choices.addView(settingRow("抖音扫描记录", douyinCount + " 条", true, v -> {
            xhsMode = false;
            showRecords();
        }));
        choices.addView(rowDivider());
        choices.addView(settingRow("小红书扫描记录", xhsCount + " 条", true, v -> {
            xhsMode = true;
            showRecords();
        }));
        setContentView(root);
    }

    private void showRecords() {
        retryReplyParent = null;
        if (scanning) {
            scanning = false;
            requestToken++;
            scanButton.setText("继续扫描");
        }
        showingReader = false;
        showingRecords = true;
        showingRecordPlatforms = false;
        for (Animatable animation : animations) animation.stop();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(20), dp(6), dp(20), dp(6));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(54)));
        TextView back = label("‹", 22, INK, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(card(10));
        back.setOnClickListener(v -> leaveRecords());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView heading = label("扫描记录", 16, INK, true);
        heading.setGravity(Gravity.CENTER);
        toolbar.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        int visibleCount = 0;
        for (ScanRecord record : records) if (isXhsLink(record.link) == xhsMode) visibleCount++;
        TextView total = label("共 " + visibleCount + " 条", 12, INK2, true);
        total.setGravity(Gravity.CENTER);
        total.setBackground(card(16));
        toolbar.addView(total, new LinearLayout.LayoutParams(dp(66), dp(32)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(6), dp(20), dp(28));
        scroll.addView(page);
        addRecordCards(page);
        setContentView(root);
    }

    private void leaveRecords() {
        if (recordsFromSettings) showRecordPlatforms();
        else if (viewingSavedRecord) selectMode(xhsMode);
        else showReader();
    }

    private void addRecordCards(LinearLayout page) {
        TextView hint = label("本机暂存 · 删除记录不会影响已保存到下载目录的文件", 11, INK3, false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(2);
        hintParams.bottomMargin = dp(14);
        page.addView(hint, hintParams);
        ArrayList<ScanRecord> visibleRecords = new ArrayList<>();
        for (ScanRecord record : records) if (isXhsLink(record.link) == xhsMode) visibleRecords.add(record);
        if (visibleRecords.isEmpty()) {
            TextView empty = label("扫描过的作品会暂存在这里，方便随时查看和删除。", 12, INK3, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(26), dp(18), dp(26));
            empty.setBackground(card(16));
            page.addView(empty);
            return;
        }
        SimpleDateFormat date = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        for (ScanRecord record : visibleRecords) {
            boolean xhsRecord = isXhsLink(record.link);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(card(16));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = dp(10);
            page.addView(card, cardParams);

            LinearLayout head = new LinearLayout(this);
            head.setGravity(Gravity.CENTER_VERTICAL);
            head.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.addView(head);

            ImageView icon = new ImageView(this);
            icon.setImageResource(xhsRecord ? R.drawable.xhs_nav : R.drawable.douyin_nav);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setPadding(dp(9), dp(9), dp(9), dp(9));
            icon.setBackground(shape(xhsRecord ? Color.rgb(255, 237, 232) : ACCENT_SOFT, 11));
            head.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1);
            infoParams.leftMargin = dp(12);
            infoParams.rightMargin = dp(14);
            head.addView(info, infoParams);
            String shortLink = record.link.replaceFirst("^https?://", "").replaceFirst("\\?.*$", "").replaceFirst("/$", "");
            if (shortLink.length() > 24) shortLink = shortLink.substring(0, 23) + "…";
            TextView title = label(shortLink, 13, INK, true);
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(title);

            int count;
            synchronized (record) { count = record.items.size(); }
            String progress = xhsRecord || record.includeReplies
                    ? "主 " + record.mainScanned + " · 回 " + record.replyScanned
                    : record.scanned + " 条评论";
            if (xhsRecord && record.scanned > 0 && record.mainScanned + record.replyScanned == 0)
                progress = record.scanned + " 条评论";
            String detail = ("legacy".equals(record.id) ? "旧版缓存" : date.format(record.createdAt) + " · " + progress)
                    + " · " + count + " 素材";
            TextView sub = label(detail, 11, INK3, false);
            sub.setSingleLine(true);
            sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
            subParams.topMargin = dp(4);
            info.addView(sub, subParams);

            TextView state = label(record.complete ? "已完成" : "暂存中", 11, record.complete ? ACCENT : INK3, true);
            state.setGravity(Gravity.CENTER);
            head.addView(state, new LinearLayout.LayoutParams(dp(44), -2));

            View divider = new View(this);
            divider.setBackgroundColor(LINE);
            card.addView(divider, new LinearLayout.LayoutParams(-1, 1));

            LinearLayout actions = new LinearLayout(this);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            actions.setPadding(dp(14), dp(10), dp(14), dp(11));
            card.addView(actions);
            TextView view = label("查看预览", 12, INK2, true);
            view.setOnClickListener(v -> showRecord(record));
            actions.addView(view);
            if (canResume(record)) {
                TextView resume = label("继续扫描", 12, ACCENT, true);
                LinearLayout.LayoutParams resumeParams = new LinearLayout.LayoutParams(-2, -2);
                resumeParams.leftMargin = dp(12);
                actions.addView(resume, resumeParams);
                resume.setOnClickListener(v -> {
                    showRecord(record);
                    openSavedRecordPage(record);
                });
            }
            if (record.link.startsWith("http")) {
                TextView copy = label("复制链接", 12, INK2, true);
                LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(-2, -2);
                copyParams.leftMargin = dp(12);
                copy.setOnClickListener(v -> copyLink(record.link));
                actions.addView(copy, copyParams);
            }
            TextView delete = label("删除记录", 12, DANGER, true);
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-2, -2);
            deleteParams.leftMargin = dp(12);
            delete.setOnClickListener(v -> confirmDelete(record));
            actions.addView(delete, deleteParams);

            card.setOnClickListener(v -> showRecord(record));
        }
    }

    private void showRecord(ScanRecord record) {
        xhsMode = isXhsLink(record.link);
        configureReaderMode();
        currentRecord = record;
        viewingSavedRecord = true;
        savedRecordPageOpened = false;
        loadingSavedRecordPage = false;
        activeLink = record.link.startsWith("http") ? record.link : null;
        input.setText(activeLink == null ? "" : activeLink);
        cursor = record.cursor;
        scanned = xhsMode ? record.scanned : record.mainScanned > 0 ? record.mainScanned : record.scanned;
        clearMediaSelection();
        media.clear();
        synchronized (record) { media.addAll(record.items); }
        selectedFilter = "all";
        browserPanel.setVisibility(View.GONE);
        updateWebToggle();
        scanButton.setText(canResume(record) ? "继续扫描" : "重新扫描");
        showReader();
        renderResults();
        updateResultLabels();
        status.setText(canResume(record) ? "已打开本机暂存记录，可继续扫描"
                : "已打开本机暂存记录；重新扫描会生成新记录");
        readerScroll.post(() -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()));
    }

    private boolean canResume(ScanRecord record) {
        return record != null && !record.complete && extractLink(record.link, isXhsLink(record.link)) != null;
    }

    private void openSavedRecordPage(ScanRecord record) {
        int token = ++requestToken;
        savedRecordPageOpened = true;
        loadingSavedRecordPage = true;
        status.setText("正在打开原作品，已提取的素材会保留");
        worker.execute(() -> runOnUiThread(() -> {
            if (token != requestToken || currentRecord != record || !showingReader) return;
            cursor = record.cursor;
            scanned = record.mainScanned > 0 ? record.mainScanned : record.scanned;
            synchronized (record) {
                media.clear();
                media.addAll(record.items);
            }
            renderResults();
            updateResultLabels();
            commentRequest = null;
            replyRequest = null;
            lastCommentPath = null;
            xhsAppRedirects = 0;
            lastXhsPage = null;
            xhsMainDone = false;
            browser.stopLoading();
            browserPanel.setVisibility(View.VISIBLE);
            updateWebToggle();
            browser.loadUrl(record.link);
            readerScroll.post(() -> readerScroll.smoothScrollTo(0, browserPanel.getTop()));
        }));
    }

    private void showConfirmation(int iconRes, boolean destructive, String title, String message,
                                  String cancelLabel, String confirmLabel, Runnable onCancel, Runnable onConfirm) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        int accent = destructive ? Color.rgb(173, 71, 65) : ACCENT;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(20), dp(22), dp(20));
        panel.setBackground(shape(Color.WHITE, 24));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(accent));
        icon.setBackground(shape(destructive ? Color.rgb(250, 228, 222) : ACCENT_SOFT, 20));
        icon.setPadding(dp(9), dp(9), dp(9), dp(9));
        panel.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(12);
        panel.addView(label(title, 20, INK, true), titleParams);
        TextView detail = label(message, 14, INK2, false);
        detail.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(8);
        panel.addView(detail, detailParams);

        LinearLayout buttons = new LinearLayout(this);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(20);
        panel.addView(buttons, rowParams);
        Button cancel = shareButton(cancelLabel, v -> {
            dialog.dismiss();
            if (onCancel != null) onCancel.run();
        });
        cancel.setTextColor(INK);
        cancel.setBackground(shape(TRACK, 12));
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button confirm = shareButton(confirmLabel, v -> {
            dialog.dismiss();
            onConfirm.run();
        });
        confirm.setTextColor(Color.WHITE);
        confirm.setBackground(shape(accent, 12));
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        confirmParams.leftMargin = dp(8);
        buttons.addView(confirm, confirmParams);

        if (onCancel != null) dialog.setOnCancelListener(ignored -> onCancel.run());
        showCardDialog(dialog, panel);
    }

    private void showCardDialog(android.app.Dialog dialog, View panel) {
        dialog.setContentView(panel);
        android.view.Window window = dialog.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.38f);
        dialog.show();
        window.setLayout(Math.min(dp(360), getResources().getDisplayMetrics().widthPixels - dp(40)), -2);
    }

    private void confirmDelete(ScanRecord record) {
        showConfirmation(android.R.drawable.ic_menu_delete, true, "删除扫描记录？",
                "这条记录及其中暂存的素材将被删除。\n已保存到下载目录的文件仍会保留。",
                "保留记录", "删除记录", null, () -> {
            if (currentRecord == record) {
                scanning = false;
                requestToken++;
                currentRecord = null;
                viewingSavedRecord = false;
                clearMediaSelection();
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
        });
    }

    private void showReader() {
        showingReader = true;
        showingRecords = false;
        showingRecordPlatforms = false;
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
        toolbar.setPadding(dp(20), dp(6), dp(20), dp(6));
        readerRoot.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(54)));
        TextView back = label("‹", 22, INK, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(card(10));
        back.setOnClickListener(v -> {
            if (viewingSavedRecord) showRecords();
            else showHome();
        });
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(34), dp(34)));
        readerHeading = label("抖音链接读取", 16, INK, true);
        readerHeading.setGravity(Gravity.CENTER);
        toolbar.addView(readerHeading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView recordsButton = label("记录", 12, INK2, true);
        recordsButton.setGravity(Gravity.CENTER);
        recordsButton.setBackground(card(16));
        recordsButton.setOnClickListener(v -> {
            recordsFromSettings = false;
            showRecords();
        });
        toolbar.addView(recordsButton, new LinearLayout.LayoutParams(dp(58), dp(32)));

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
        inputCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        inputCard.setBackground(card(16));
        content.addView(inputCard);
        LinearLayout inputHead = new LinearLayout(this);
        inputHead.setGravity(Gravity.CENTER_VERTICAL);
        inputCard.addView(inputHead);
        inputHeading = label("作品链接", 14, INK, true);
        inputHead.addView(inputHeading, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout connection = new LinearLayout(this);
        connection.setGravity(Gravity.CENTER_VERTICAL);
        View connectionDot = new View(this);
        connectionDot.setBackground(shape(ACCENT, 3));
        connection.addView(connectionDot, new LinearLayout.LayoutParams(dp(5), dp(5)));
        TextView connectionText = label("网页已登录", 11, ACCENT, true);
        LinearLayout.LayoutParams connectionParams = new LinearLayout.LayoutParams(-2, -2);
        connectionParams.leftMargin = dp(5);
        connection.addView(connectionText, connectionParams);
        inputHead.addView(connection);
        LinearLayout urlBox = new LinearLayout(this);
        urlBox.setGravity(Gravity.CENTER_VERTICAL);
        urlBox.setPadding(dp(10), 0, dp(6), 0);
        urlBox.setBackground(outlined(10, Color.rgb(247, 248, 246)));
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(-1, dp(44));
        urlParams.topMargin = dp(12);
        inputCard.addView(urlBox, urlParams);
        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(13);
        input.setTextColor(INK);
        input.setHintTextColor(INK3);
        input.setHint("粘贴抖音作品链接或分享文案");
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(0, 0, 0, 0);
        urlBox.addView(input, new LinearLayout.LayoutParams(0, -1, 1));
        TextView paste = label("粘贴", 11, INK2, true);
        paste.setGravity(Gravity.CENTER);
        paste.setBackground(card(8));
        paste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0)
                input.setText(clipboard.getPrimaryClip().getItemAt(0).coerceToText(this));
        });
        LinearLayout.LayoutParams pasteParams = new LinearLayout.LayoutParams(dp(40), dp(32));
        pasteParams.leftMargin = dp(6);
        urlBox.addView(paste, pasteParams);
        TextView copy = label("复制", 11, INK2, true);
        copy.setGravity(Gravity.CENTER);
        copy.setBackground(card(8));
        copy.setOnClickListener(v -> {
            String link = extractLink(input.getText().toString(), xhsMode);
            if (link == null && input.getText().length() == 0) link = activeLink;
            if (link == null) showSaveResult("没有可复制的链接");
            else copyLink(link);
        });
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(dp(40), dp(32));
        copyParams.leftMargin = dp(6);
        urlBox.addView(copy, copyParams);
        TextView hint = label("支持分享文案、短链接与作品地址", 11, INK3, false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(10);
        inputCard.addView(hint, hintParams);
        Button open = new Button(this);
        open.setAllCaps(false);
        open.setText("打开作品");
        open.setTextColor(Color.WHITE);
        open.setTextSize(15);
        open.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        open.setBackground(shape(ACCENT, 12));
        open.setMinHeight(0);
        open.setStateListAnimator(null);
        open.setOnClickListener(v -> openLink());
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(46));
        actionParams.topMargin = dp(12);
        inputCard.addView(actionRow, actionParams);
        actionRow.addView(open, new LinearLayout.LayoutParams(0, -1, 1));

        LinearLayout scanCard = new LinearLayout(this);
        scanCard.setOrientation(LinearLayout.HORIZONTAL);
        scanCard.setGravity(Gravity.CENTER_VERTICAL);
        scanCard.setBackground(card(16));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(12);
        content.addView(scanCard, statusParams);
        View scanBar = new View(this);
        scanBar.setBackground(shape(ACCENT, 2));
        LinearLayout.LayoutParams scanBarParams = new LinearLayout.LayoutParams(dp(3), dp(40));
        scanBarParams.leftMargin = dp(8);
        scanCard.addView(scanBar, scanBarParams);
        LinearLayout scanBody = new LinearLayout(this);
        scanBody.setOrientation(LinearLayout.VERTICAL);
        scanBody.setGravity(Gravity.CENTER_VERTICAL);
        scanBody.setPadding(dp(14), dp(14), dp(14), dp(14));
        scanCard.addView(scanBody, new LinearLayout.LayoutParams(0, -2, 1));
        status = label("粘贴作品链接，打开网页后进入评论区", 14, INK, true);
        scanBody.addView(status);
        scanCountLine = label("", 12, INK2, false);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(-1, -2);
        countParams.topMargin = dp(6);
        scanBody.addView(scanCountLine, countParams);
        scanCountLine.setVisibility(View.GONE);
        scanProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        scanProgress.setIndeterminate(true);
        scanProgress.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(3));
        progressParams.topMargin = dp(10);
        scanBody.addView(scanProgress, progressParams);
        scanProgress.setVisibility(View.GONE);
        scanDetails = label("查看扫描明细", 13, ACCENT, true);
        scanDetails.setGravity(Gravity.CENTER_VERTICAL);
        scanDetails.setMinHeight(dp(44));
        scanDetails.setVisibility(View.GONE);
        scanDetails.setOnClickListener(v -> {
            if (currentRecord != null) showScanDetails(currentRecord);
        });
        scanBody.addView(scanDetails);
        status.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable text) { updateScanCard(); }
        });
        LinearLayout webToggleRow = new LinearLayout(this);
        webToggleRow.setGravity(Gravity.CENTER_VERTICAL);
        webToggleRow.setPadding(dp(16), 0, dp(14), 0);
        webToggleRow.setBackground(card(14));
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(-1, dp(54));
        toggleParams.topMargin = dp(12);
        content.addView(webToggleRow, toggleParams);
        webToggle = label("网页操作", 15, INK, true);
        webToggleRow.addView(webToggle, new LinearLayout.LayoutParams(0, -2, 1));
        webToggleArrow = label("⌄", 19, Color.WHITE, true);
        webToggleArrow.setGravity(Gravity.CENTER);
        webToggleArrow.setBackground(shape(ACCENT, 10));
        webToggleRow.addView(webToggleArrow, new LinearLayout.LayoutParams(dp(34), dp(30)));
        webToggleRow.setOnClickListener(v -> {
            browserPanel.setVisibility(browserPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            updateWebToggle();
        });
        browserPanel = new LinearLayout(this);
        browserPanel.setOrientation(LinearLayout.VERTICAL);
        browserPanel.setPadding(dp(10), dp(10), dp(10), dp(10));
        browserPanel.setBackground(card(14));
        LinearLayout.LayoutParams browserParams = new LinearLayout.LayoutParams(-1, -2);
        browserParams.topMargin = dp(9);
        content.addView(browserPanel, browserParams);
        browserPanel.setVisibility(View.GONE);
        browserHint = label("在网页中登录并打开评论区，然后扫描主评论", 11, INK3, false);
        browserPanel.addView(browserHint);
        setupBrowser();
        browserPanel.addView(browser, new LinearLayout.LayoutParams(-1, dp(290)));
        codeLogin = new Button(this);
        codeLogin.setAllCaps(false);
        codeLogin.setText("验证码登录");
        codeLogin.setTextColor(INK2);
        codeLogin.setTextSize(13);
        codeLogin.setBackground(card(10));
        codeLogin.setMinHeight(0);
        codeLogin.setStateListAnimator(null);
        codeLogin.setOnClickListener(v -> browser.evaluateJavascript(
                "(() => {const tab=[...document.querySelectorAll('button,div,span')].find(x=>x.children.length===0&&x.textContent.trim()==='验证码登录'&&x.getBoundingClientRect().width>0);if(tab){tab.click();return true}return false})()",
                found -> showTopNotice("true".equals(found) ? "请在抖音网页中输入手机号和验证码" : "请先在网页中点登录，再选择验证码登录")));
        LinearLayout.LayoutParams loginParams = new LinearLayout.LayoutParams(-1, dp(44));
        loginParams.topMargin = dp(8);
        browserPanel.addView(codeLogin, loginParams);
        scanButton = new Button(this);
        scanButton.setAllCaps(false);
        scanButton.setText("扫描主评论");
        scanButton.setTextColor(Color.WHITE);
        scanButton.setTextSize(14);
        scanButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scanButton.setBackground(shape(ACCENT, 10));
        scanButton.setMinHeight(0);
        scanButton.setStateListAnimator(null);
        scanButton.setOnClickListener(v -> {
            if (viewingSavedRecord && loadingSavedRecordPage) {
                showTopNotice("作品正在打开，请稍候");
                return;
            }
            if (viewingSavedRecord && !savedRecordPageOpened && canResume(currentRecord)) {
                openSavedRecordPage(currentRecord);
                return;
            }
            if (viewingSavedRecord && !savedRecordPageOpened) {
                openLink();
                status.setText(xhsMode ? "笔记已重新打开，请进入评论区后再次点击扫描" : "作品已重新打开，请进入评论区后再次点击扫描");
                return;
            }
            if (!scanning && currentRecord != null && currentRecord.processingPage) {
                showTopNotice("正在保存上一页，请稍候");
                return;
            }
            if (xhsMode) {
                toggleXhsScan();
                return;
            }
            if (scanning && retryReplyParent != null) {
                endReplyRetry("已停止重试，剩余失败项已保留");
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
                    showTopNotice("请先打开抖音作品页面");
                    return;
                }
                if (currentRecord == null || currentRecord.complete) {
                    String id = UUID.randomUUID().toString();
                    ScanRecord record = new ScanRecord(id, activeLink == null ? page : activeLink,
                            System.currentTimeMillis(), new File(recordsFolder(), id));
                    currentRecord = record;
                    record.includeReplies = douyinReadReplies;
                    records.add(0, record);
                    cursor = 0;
                    scanned = 0;
                    clearMediaSelection();
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
                pageRetries = 0;
                scanButton.setText("暂停扫描");
                startPage();
            }
        });
        LinearLayout.LayoutParams scanButtonParams = new LinearLayout.LayoutParams(0, -1, 1);
        scanButtonParams.leftMargin = dp(8);
        actionRow.addView(scanButton, scanButtonParams);

        resultAnchor = label("提取结果", 20, INK, true);
        LinearLayout.LayoutParams resultHeadParams = new LinearLayout.LayoutParams(-1, -2);
        resultHeadParams.topMargin = dp(22);
        content.addView(resultAnchor, resultHeadParams);
        resultSummary = label("提取后自动显示图片与动图预览", 12, INK3, false);
        LinearLayout summaryRow = new LinearLayout(this);
        summaryRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.topMargin = dp(4);
        content.addView(summaryRow, summaryParams);
        summaryRow.addView(resultSummary, new LinearLayout.LayoutParams(0, -2, 1));
        selectionAction = label("多选", 12, ACCENT, true);
        selectionAction.setGravity(Gravity.CENTER);
        selectionAction.setPadding(dp(12), dp(6), dp(12), dp(6));
        selectionAction.setBackground(shape(ACCENT_SOFT, 10));
        selectionAction.setOnClickListener(v -> {
            selectingMedia = !selectingMedia;
            selectedMedia.clear();
            renderResults();
            updateResultLabels();
        });
        summaryRow.addView(selectionAction);
        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.setPadding(dp(3), dp(3), dp(3), dp(3));
        filters.setBackground(shape(TRACK, 10));
        LinearLayout.LayoutParams filtersParams = new LinearLayout.LayoutParams(-1, dp(38));
        filtersParams.topMargin = dp(14);
        filtersParams.bottomMargin = dp(12);
        content.addView(filters, filtersParams);
        allFilter = segment("全部 0", "all", filters);
        gifFilter = segment("动图 0", "gif", filters);
        imageFilter = segment("图片 0", "image", filters);
        emptyResults = label("还没有提取结果。打开作品并扫描评论后，图片和动态素材会显示在这里。", 12, INK3, false);
        emptyResults.setGravity(Gravity.CENTER);
        emptyResults.setPadding(dp(18), dp(26), dp(18), dp(26));
        emptyResults.setBackground(card(16));
        content.addView(emptyResults);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results);

        View saveDivider = new View(this);
        saveDivider.setBackgroundColor(LINE);
        readerRoot.addView(saveDivider, new LinearLayout.LayoutParams(-1, 1));
        LinearLayout saveBar = new LinearLayout(this);
        saveBar.setGravity(Gravity.CENTER_VERTICAL);
        saveBar.setPadding(dp(20), dp(10), dp(20), dp(14));
        saveBar.setBackgroundColor(BG);
        readerRoot.addView(saveBar, new LinearLayout.LayoutParams(-1, dp(72)));
        LinearLayout saveInfo = new LinearLayout(this);
        saveInfo.setOrientation(LinearLayout.VERTICAL);
        saveBar.addView(saveInfo, new LinearLayout.LayoutParams(0, -2, 1));
        countLabel = label("已提取 0 个", 14, INK, true);
        saveInfo.addView(countLabel);
        saveSub = label("保存到 下载 / DouYinGetting", 11, INK3, false);
        LinearLayout.LayoutParams saveSubParams = new LinearLayout.LayoutParams(-1, -2);
        saveSubParams.topMargin = dp(3);
        saveInfo.addView(saveSub, saveSubParams);
        saveButton = new Button(this);
        saveButton.setAllCaps(false);
        saveButton.setText("保存全部");
        saveButton.setTextColor(Color.WHITE);
        saveButton.setTextSize(13);
        saveButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        saveButton.setBackground(shape(ACCENT, 10));
        saveButton.setMinHeight(0);
        saveButton.setStateListAnimator(null);
        saveButton.setOnClickListener(v -> {
            ArrayList<Media> snapshot = new ArrayList<>();
            for (Media item : media) if (!selectingMedia || selectedMedia.contains(item)) snapshot.add(item);
            saveMediaBatch(snapshot, this::showSaveResult);
        });
        saveBar.addView(saveButton, new LinearLayout.LayoutParams(dp(104), dp(40)));
        shareSelectedButton = shareButton("分享", v -> {
            ArrayList<Media> items = new ArrayList<>();
            for (Media item : media) if (selectedMedia.contains(item)) items.add(item);
            shareMedia(items);
        });
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(dp(62), dp(40));
        shareParams.rightMargin = dp(8);
        saveBar.addView(shareSelectedButton, saveBar.indexOfChild(saveButton), shareParams);
        updateResultLabels();
    }

    private void updateScanCard() {
        if (scanProgress == null) return;
        scanProgress.setVisibility(scanning ? View.VISIBLE : View.GONE);
        ScanRecord record = currentRecord;
        scanDetails.setVisibility(record == null ? View.GONE : View.VISIBLE);
        scanCountLine.setVisibility(record == null ? View.GONE : View.VISIBLE);
        if (record == null) return;
        scanCountLine.setText(scanCounts(record));
        synchronized (record) {
            int issues = record.issueCount + Math.max(0, record.emptyReplyLists - record.loggedEmptyReplyLists);
            scanDetails.setText(issues > 0 ? "查看扫描明细 · " + issues + " 条异常" : "查看扫描明细");
        }
    }

    private String scanCounts(ScanRecord record) {
        if (isXhsLink(record.link) || record.includeReplies) {
            String counts = "主评论 " + record.mainScanned + " · 回复 " + record.replyScanned;
            if (record.mainScanned + record.replyScanned == 0 && record.scanned > 0)
                counts = "已读取 " + record.scanned + " 条评论";
            if (!isXhsLink(record.link) && record.mainDone)
                counts += " · 回复组 " + record.replyIndex + "/" + record.replyParents.size();
            return counts;
        }
        return "已读取 " + Math.max(record.scanned, record.mainScanned) + " 条主评论";
    }

    private void showScanDetails(ScanRecord record) {
        ArrayList<String> issues;
        int olderEmpty;
        int issueCount;
        int mediaFailures;
        int replyFailures;
        int mediaCount;
        synchronized (record) {
            issues = new ArrayList<>(record.scanIssues);
            olderEmpty = Math.max(0, record.emptyReplyLists - record.loggedEmptyReplyLists);
            issueCount = record.issueCount;
            mediaFailures = record.failedMedia.size();
            replyFailures = record.failedReplies.size();
            mediaCount = record.items.size();
        }
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(16));
        panel.setBackground(shape(SURFACE, 24));
        panel.addView(label("扫描明细", 20, INK, true));

        ScrollView scroll = new ScrollView(this) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int limit = getResources().getDisplayMetrics().heightPixels / 2;
                if (View.MeasureSpec.getMode(heightSpec) != View.MeasureSpec.UNSPECIFIED)
                    limit = Math.min(limit, View.MeasureSpec.getSize(heightSpec));
                super.onMeasure(widthSpec, View.MeasureSpec.makeMeasureSpec(limit, View.MeasureSpec.AT_MOST));
            }
        };
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, -2, 1));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(12), 0, dp(4));
        scroll.addView(body);

        String state = record.complete ? "扫描已完成" : scanning && currentRecord == record ? "扫描进行中" : "扫描未完成";
        TextView summary = label(state + " · " + mediaCount + " 个素材\n" + scanCounts(record), 14, INK, true);
        summary.setLineSpacing(dp(4), 1f);
        summary.setPadding(dp(12), dp(10), dp(12), dp(10));
        summary.setBackground(shape(ACCENT_SOFT, 12));
        body.addView(summary, new LinearLayout.LayoutParams(-1, -2));

        boolean canRetry = mediaFailures + replyFailures > 0;
        if (canRetry) body.addView(groupTitle("待重试 · 素材 " + mediaFailures + " · 回复组 " + replyFailures, 12));
        body.addView(groupTitle("异常记录 · " + (issueCount + olderEmpty) + " 条", 12));
        if (issueCount > issues.size()) body.addView(label("显示最近 " + issues.size() + " 条", 12, INK2, false));
        if (olderEmpty > 0) issues.add(0, "旧记录中有 " + olderEmpty + " 组回复未返回数据");
        if (issues.isEmpty()) {
            body.addView(label("暂无已记录的异常", 13, INK2, false));
        } else {
            for (String issue : issues) {
                TextView line = label(issue, 13, INK2, false);
                line.setPadding(dp(10), dp(8), dp(10), dp(8));
                line.setBackground(shape(BG, 8));
                LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(-1, -2);
                lineParams.topMargin = dp(6);
                body.addView(line, lineParams);
            }
        }

        LinearLayout buttons = new LinearLayout(this);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(48));
        rowParams.topMargin = dp(12);
        panel.addView(buttons, rowParams);
        Button close = shareButton("关闭", v -> dialog.dismiss());
        close.setTextColor(INK);
        close.setBackground(shape(TRACK, 12));
        buttons.addView(close, new LinearLayout.LayoutParams(0, -1, 1));
        if (canRetry) {
            Button retry = shareButton("重试失败项", v -> {
                dialog.dismiss();
                showRetryOptions(record);
            });
            retry.setTextColor(Color.WHITE);
            retry.setBackground(shape(ACCENT, 12));
            LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(0, -1, 1);
            retryParams.leftMargin = dp(8);
            buttons.addView(retry, retryParams);
        }
        showCardDialog(dialog, panel);
    }

    private void showRetryOptions(ScanRecord record) {
        if (currentRecord != record || scanning || record.processingPage) {
            showTopNotice("请暂停扫描并等待当前页保存完成");
            return;
        }
        ArrayList<String> choices = new ArrayList<>();
        ArrayList<Runnable> actions = new ArrayList<>();
        synchronized (record) {
            if (!record.failedMedia.isEmpty()) {
                choices.add("重试失败素材（" + record.failedMedia.size() + " 个）");
                actions.add(() -> retryFailedMedia(record));
            }
            for (String parent : record.failedReplies.keySet()) {
                choices.add("重试回复组 · " + parent);
                actions.add(() -> retryFailedReply(record, parent));
            }
        }
        if (choices.isEmpty()) return;
        new AlertDialog.Builder(this).setTitle("选择失败项")
                .setItems(choices.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
                .setNegativeButton("取消", null).show();
    }

    private void retryFailedMedia(ScanRecord record) {
        ArrayList<FailedMedia> pending;
        synchronized (record) { pending = new ArrayList<>(record.failedMedia.values()); }
        record.processingPage = true;
        status.setText("正在重试 " + pending.size() + " 个失败素材");
        worker.execute(() -> {
            int success = 0;
            try {
                for (FailedMedia item : pending) {
                    if (download(record, item.source, item.commentId, item.kind)) success++;
                }
                persistRecord(record);
            } finally { record.processingPage = false; }
            int recovered = success;
            runOnUiThread(() -> {
                if (currentRecord != record || !showingReader) return;
                int remaining = pending.size() - recovered;
                status.setText("素材重试完成 · 成功 " + recovered + " · 仍失败 " + remaining);
                if (remaining > 0) showTopNotice("若素材地址已失效，请重新打开作品获取");
            });
        });
    }

    private void retryFailedReply(ScanRecord record, String parent) {
        if (viewingSavedRecord && loadingSavedRecordPage) {
            showTopNotice("作品正在打开，请稍候");
            return;
        }
        if (viewingSavedRecord && !savedRecordPageOpened) {
            openSavedRecordPage(record);
            showTopNotice("请打开评论区，再从扫描明细重试回复组");
            return;
        }
        String page = browser.getUrl();
        if (page == null || !page.startsWith("https://www.douyin.com/")) {
            showTopNotice("请打开原作品的评论区");
            return;
        }
        synchronized (record) {
            Long checkpoint = record.failedReplies.get(parent);
            if (checkpoint == null) return;
            retryReplyCursor = checkpoint;
        }
        retryReplyParent = parent;
        scanning = true;
        requestWaits = 0;
        pageRetries = 0;
        scanButton.setText("停止重试");
        startPage();
    }

    private void endReplyRetry(String message) {
        retryReplyParent = null;
        scanning = false;
        requestToken++;
        scanButton.setText(currentRecord != null && currentRecord.complete ? "重新扫描" : "继续扫描");
        status.setText(message);
    }

    private void recordScanPause(String reason) {
        ScanRecord record = currentRecord;
        if (record == null) return;
        String location = isXhsLink(record.link) ? "网页评论"
                : record.mainDone && record.replyIndex < record.replyParents.size()
                ? "回复组 " + (record.replyIndex + 1) + "（主评论 ID " + record.replyParents.get(record.replyIndex) + "）"
                : "主评论分页（已读取 " + scanned + " 条）";
        record.addIssue(location + "： " + reason);
        worker.execute(() -> persistRecord(record));
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
                        if (path.contains("comment/list/reply")) replyRequest = uri.toString();
                        else commentRequest = uri.toString();
                    }
                }
                return null;
            }
            @Override public void onPageFinished(WebView view, String url) {
                if ("about:blank".equals(url)) return;
                boolean resumedPage = viewingSavedRecord && loadingSavedRecordPage && url.equals(view.getUrl())
                        && (xhsMode ? url.startsWith("https://www.xiaohongshu.com/") : url.startsWith("https://www.douyin.com/"));
                if (resumedPage) {
                    loadingSavedRecordPage = false;
                }
                if (xhsMode) {
                    if (!xhsCaptureSupported && xhsCaptureScript != null && url.startsWith("https://www.xiaohongshu.com/"))
                        browser.evaluateJavascript(xhsCaptureScript, null);
                    status.setText(url.contains("/login") ? "请在小红书网页中完成登录，再打开笔记"
                            : "笔记已打开。请进入评论区，再扫描评论素材");
                } else status.setText("作品已打开。请在网页中登录并进入评论区，再扫描主评论");
                if (resumedPage && !url.contains("/login")) status.setText("请打开评论区，再点击继续扫描");
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame() && viewingSavedRecord && loadingSavedRecordPage) {
                    loadingSavedRecordPage = false;
                    savedRecordPageOpened = false;
                    status.setText("打开原作品失败，点击继续扫描可重试");
                }
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
                record.emptyReplyLists = data.optInt("emptyReplyLists");
                record.loggedEmptyReplyLists = data.optInt("loggedEmptyReplyLists");
                JSONArray issues = data.optJSONArray("scanIssues");
                if (issues != null) {
                    for (int i = 0; i < issues.length(); i++) record.addIssue(issues.optString(i));
                }
                record.issueCount = Math.max(record.issueCount, data.optInt("issueCount"));
                record.includeReplies = data.optBoolean("includeReplies");
                record.mainDone = data.optBoolean("mainDone");
                record.replyIndex = data.optInt("replyIndex");
                record.replyCursor = data.optLong("replyCursor");
                JSONArray failedMedia = data.optJSONArray("failedMedia");
                if (failedMedia != null) for (int i = 0; i < failedMedia.length(); i++) {
                    JSONObject failure = failedMedia.optJSONObject(i);
                    if (failure == null) continue;
                    String source = failure.optString("source");
                    if (!source.startsWith("https://") && !source.startsWith("http://")) continue;
                    record.failedMedia.put(source, new FailedMedia(source, failure.optString("commentId"), failure.optString("kind")));
                }
                JSONObject failedReplies = data.optJSONObject("failedReplies");
                if (failedReplies != null && !isXhsLink(record.link)) {
                    java.util.Iterator<String> parentsWithFailures = failedReplies.keys();
                    while (parentsWithFailures.hasNext()) {
                        String parent = parentsWithFailures.next();
                        record.failedReplies.put(parent, failedReplies.optLong(parent));
                    }
                }
                JSONArray parents = data.optJSONArray("replyParents");
                if (parents != null) for (int i = 0; i < parents.length(); i++) record.replyParents.add(parents.optString(i));
                record.cursor = data.optLong("cursor");
                record.complete = data.optBoolean("complete");
                JSONArray xhsIds = data.optJSONArray("xhsCommentIds");
                if (xhsIds != null) {
                    for (int i = 0; i < xhsIds.length(); i++) {
                        String id = xhsIds.optString(i);
                        if (!id.isEmpty()) record.xhsCommentIds.add(id);
                    }
                }
                record.xhsNeedsRecount = isXhsLink(record.link) && (data.optBoolean("xhsNeedsRecount")
                        || (xhsIds == null && record.scanned > 0));
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
            synchronized (record) {
                data.put("emptyReplyLists", record.emptyReplyLists);
                data.put("loggedEmptyReplyLists", record.loggedEmptyReplyLists);
                data.put("scanIssues", new JSONArray(record.scanIssues));
                data.put("issueCount", record.issueCount);
                JSONArray failures = new JSONArray();
                for (FailedMedia item : record.failedMedia.values()) {
                    JSONObject entry = new JSONObject();
                    entry.put("source", item.source);
                    entry.put("commentId", item.commentId);
                    entry.put("kind", item.kind);
                    failures.put(entry);
                }
                data.put("failedMedia", failures);
                data.put("failedReplies", new JSONObject(record.failedReplies));
            }
            data.put("includeReplies", record.includeReplies);
            data.put("mainDone", record.mainDone);
            data.put("replyIndex", record.replyIndex);
            data.put("replyCursor", record.replyCursor);
            data.put("replyParents", new JSONArray(record.replyParents));
            data.put("cursor", record.cursor);
            data.put("complete", record.complete);
            if (isXhsLink(record.link)) {
                data.put("xhsCommentIds", new JSONArray(record.xhsCommentIds));
                data.put("xhsNeedsRecount", record.xhsNeedsRecount);
            }
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
        showConfirmation(android.R.drawable.ic_menu_camera, false, "抖音网页请求相机",
                "验证页面 " + request.getOrigin().getHost() + " 请求使用相机。请选择是否允许刷脸验证使用相机。",
                "拒绝", "允许", request::deny, () -> {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    } else {
                        pendingCameraRequest = request;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1001);
                    }
                });
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

    private void updateWebToggle() {
        boolean expanded = browserPanel.getVisibility() == View.VISIBLE;
        webToggle.setText(expanded ? "收起网页" : "网页操作");
        webToggleArrow.setText(expanded ? "⌃" : "⌄");
    }

    private void openLink() {
        String link = extractLink(input.getText().toString(), xhsMode);
        if (link == null) {
            showTopNotice(xhsMode ? "没有找到小红书笔记链接" : "没有找到抖音链接");
            return;
        }
        retryReplyParent = null;
        activeLink = link;
        xhsAppRedirects = 0;
        lastXhsPage = null;
        currentRecord = null;
        viewingSavedRecord = false;
        commentRequest = null;
        replyRequest = null;
        lastCommentPath = null;
        cursor = 0;
        scanned = 0;
        requestWaits = 0;
        pageRetries = 0;
        requestToken++;
        scanning = false;
        scanButton.setText(xhsMode || douyinReadReplies ? "扫描评论" : "扫描主评论");
        clearMediaSelection();
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
        updateWebToggle();
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
            showTopNotice("请先在网页中打开小红书笔记");
            return;
        }
        if (xhsCaptureScript == null) {
            showTopNotice("评论读取组件未加载，请重启 App 后重试");
            return;
        }
        if (currentRecord == null || currentRecord.complete) {
            String id = UUID.randomUUID().toString();
            ScanRecord record = new ScanRecord(id, activeLink == null ? page : activeLink,
                    System.currentTimeMillis(), new File(recordsFolder(), id));
            currentRecord = record;
            records.add(0, record);
            scanned = 0;
            xhsMainDone = false;
            clearMediaSelection();
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
                    record.processingPage = true;
                    worker.execute(() -> {
                        try { processXhsPackets(record, packets, token); }
                        finally { record.processingPage = false; }
                    });
                    return;
                }
            } catch (Exception error) {
                scanning = false;
                scanButton.setText("继续扫描");
                recordScanPause("评论响应解析失败");
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
                    updateWebToggle();
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
                boolean countedComment = !packet.optBoolean("dom") && !id.isEmpty();
                if (countedComment && record.xhsCommentIds.contains(id)) continue;
                JSONArray pictures = comment.optJSONArray("pictures");
                if (pictures != null) {
                    for (int j = 0; j < pictures.length(); j++) {
                        JSONObject picture = pictures.optJSONObject(j);
                        if (picture == null) continue;
                        download(record, picture.optString("url", null), id, "图片");
                        download(record, picture.optString("video", null), id, "视频");
                    }
                }
                if (countedComment) {
                    if (record.xhsNeedsRecount) {
                        record.mainScanned = 0;
                        record.replyScanned = 0;
                        record.xhsNeedsRecount = false;
                    }
                    record.xhsCommentIds.add(id);
                    if (comment.optBoolean("reply")) record.replyScanned++;
                    else record.mainScanned++;
                    record.scanned = record.mainScanned + record.replyScanned;
                }
            }
            if (!packet.optBoolean("replies") && !packet.optBoolean("hasMore", true)) xhsMainDone = true;
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
        updateWebToggle();
        if (showingReader) new AlertDialog.Builder(this)
                .setTitle("小红书评论扫描完成")
                .setMessage("已扫描 " + record.scanned + " 条评论（主评论 " + record.mainScanned + "、回复 " + record.replyScanned + "），提取 " + media.size() + " 个不同素材。结果已暂存。")
                .setPositiveButton("查看结果", (dialog, which) -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()))
                .show();
    }

    private void startPage() {
        if (!scanning) return;
        String page = browser.getUrl();
        if (page == null || !page.startsWith("https://www.douyin.com/")) {
            if (retryReplyParent != null) {
                endReplyRetry("请打开原作品评论区后再重试");
                return;
            }
            showTopNotice("请先打开抖音作品页面");
            scanning = false;
            return;
        }
        if (commentRequest == null) {
            int waitingToken = requestToken;
            browser.evaluateJavascript("(() => {let path='';const entries=performance.getEntriesByType('resource');for(let i=entries.length-1;i>=0;i--){const u=new URL(entries[i].name);if(u.pathname.includes('comment')){path=u.pathname;if(u.pathname.includes('comment/list')&&!u.pathname.includes('comment/list/reply')&&(u.hostname==='douyin.com'||u.hostname.endsWith('.douyin.com')))return JSON.stringify({url:u.href,path})}}return JSON.stringify({url:null,path})})()", value -> {
                if (!scanning || waitingToken != requestToken) return;
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
                    if (retryReplyParent != null) {
                        endReplyRetry("未找到评论请求，请重新打开原作品评论区后重试");
                        return;
                    }
                    scanning = false;
                    scanButton.setText(douyinReadReplies ? "扫描评论" : "扫描主评论");
                    recordScanPause("未找到评论列表请求，请确认网页评论区已打开");
                    status.setText("未找到评论列表请求。最近相关路径：" + (lastCommentPath == null || lastCommentPath.isEmpty() ? "无" : lastCommentPath) + "。请截图发给开发者");
                } else {
                    status.setText("请打开评论区，正在等待评论列表请求…");
                    browser.postDelayed(() -> {
                        if (waitingToken == requestToken) startPage();
                    }, 1500);
                }
            });
            return;
        }
        ScanRecord record = currentRecord;
        boolean retryingReply = retryReplyParent != null;
        boolean reply = retryingReply || (record != null && record.mainDone && record.includeReplies);
        if (!retryingReply && reply && record.replyIndex >= record.replyParents.size()) {
            finishDouyinScan(record);
            return;
        }
        int token = ++requestToken;
        String key = "__dyScanResult" + token;
        String script = "(()=>{const k=" + JSONObject.quote(key) + ";window[k]=null;(async()=>{try{" +
                "const u=new URL(" + JSONObject.quote(reply && replyRequest != null ? replyRequest : commentRequest) + ");" +
                (reply ? "u.pathname=u.pathname.replace(/comment\\/list\\/?$/,'comment/list/reply/');" +
                        "const id=u.searchParams.get('item_id')||u.searchParams.get('aweme_id');if(!id)throw Error('缺少作品 ID');" +
                        "u.searchParams.delete('aweme_id');u.searchParams.set('item_id',id);" +
                        "u.searchParams.set('comment_id'," + JSONObject.quote(retryingReply ? retryReplyParent : record.replyParents.get(record.replyIndex)) + ");" +
                        "u.searchParams.set('count','20');" : "") +
                "u.searchParams.set('cursor'," + JSONObject.quote(Long.toString(retryingReply ? retryReplyCursor : reply ? record.replyCursor : cursor)) + ");" +
                "const r=await fetch(u.toString(),{credentials:'include'});" +
                "let data=null;try{data=await r.json()}catch(e){}" +
                "window[k]=JSON.stringify({http:r.status,data})" +
                "}catch(e){window[k]=JSON.stringify({error:String(e)})}})()})()";
        status.setText(retryingReply ? "正在重试失败的回复组…" : reply ? "已扫描 " + record.mainScanned + " 条主评论，正在读取回复 " + (record.replyIndex + 1) + "/" + record.replyParents.size()
                : "已扫描 " + scanned + " 条主评论，正在等待下一页…");
        browser.evaluateJavascript(script, ignored -> readPageResult(key, token, 0, reply));
    }

    private void readPageResult(String key, int token, int polls, boolean reply) {
        if (!scanning || token != requestToken) return;
        browser.evaluateJavascript("window." + key, value -> {
            if (!scanning || token != requestToken) return;
            if (value == null || "null".equals(value)) {
                if (polls >= 100) {
                    retryPage("等待评论接口响应超时");
                } else {
                    browser.postDelayed(() -> readPageResult(key, token, polls + 1, reply), 200);
                }
                return;
            }
            try {
                JSONObject response = javascriptObject(value);
                int http = response.optInt("http", -1);
                JSONObject pageData = response.optJSONObject("data");
                if (pageData == null) {
                    retryPage("评论接口返回了无法读取的内容（HTTP " + http + "）");
                    return;
                }
                int code = pageData.optInt("status_code", -1);
                if (http != 200 || code != 0) {
                    retryPage("评论接口 HTTP " + http + "，状态 " + code);
                    return;
                }
                JSONArray pageComments = pageData.optJSONArray("comments");
                if (pageComments == null && !reply) throw new Exception("没有评论数据");
                JSONArray comments = pageComments == null ? new JSONArray() : pageComments;
                long currentCursor = retryReplyParent != null ? retryReplyCursor : reply ? currentRecord.replyCursor : cursor;
                long next = pageData.optLong("cursor", currentCursor);
                boolean more = pageData.optInt("has_more", 0) != 0;
                if (more && next == currentCursor) {
                    if (reply) {
                        retryPage("回复分页游标没有前进");
                        return;
                    }
                    throw new Exception("分页游标没有前进");
                }
                pageRetries = 0;
                if (retryReplyParent != null) {
                    if (pageComments == null) {
                        endReplyRetry("该回复组仍未返回数据，可稍后再次重试");
                        return;
                    }
                    processRetriedReplies(currentRecord, retryReplyParent, comments, more, next, token);
                    return;
                }
                if (reply) {
                    ScanRecord replyRecord = currentRecord;
                    boolean emptyList = pageComments == null;
                    replyRecord.processingPage = true;
                    worker.execute(() -> {
                        try { processReplyPage(replyRecord, comments, more, next, emptyList); }
                        finally { replyRecord.processingPage = false; }
                    });
                    return;
                }
                cursor = next;
                scanned += comments.length();
                status.setText("已扫描 " + scanned + " 条主评论，正在保存媒体…");
                ScanRecord record = currentRecord;
                int scannedNow = scanned;
                record.processingPage = true;
                worker.execute(() -> {
                    try { processPage(record, comments, more, next, scannedNow); }
                    finally { record.processingPage = false; }
                });
            } catch (Exception error) {
                if (retryReplyParent != null) {
                    endReplyRetry("回复响应解析失败，请稍后重试");
                    return;
                }
                scanning = false;
                scanButton.setText("继续扫描");
                recordScanPause("评论响应解析失败");
                status.setText("扫描暂停：评论响应解析失败");
                updateScanCard();
            }
        });
    }

    private void retryPage(String reason) {
        if (!scanning) return;
        if (retryReplyParent != null) {
            endReplyRetry(reason + "，失败项已保留");
            return;
        }
        if (pageRetries < 3) {
            int delay = 5000 << pageRetries++;
            int token = requestToken;
            ScanRecord record = currentRecord;
            long waitingCursor = cursor;
            status.setText(reason + "，" + delay / 1000 + " 秒后自动重试当前页（" + pageRetries + "/3）");
            browser.postDelayed(() -> {
                if (scanning && token == requestToken && record == currentRecord && waitingCursor == cursor) startPage();
            }, delay);
            return;
        }
        pageRetries = 0;
        scanning = false;
        scanButton.setText("继续扫描");
        recordScanPause(reason + "（重试后仍失败）");
        status.setText(reason + "，扫描已暂停；已提取的素材仍在暂存记录中");
        updateScanCard();
        if (showingReader) showConfirmation(android.R.drawable.ic_popup_sync, false, "评论扫描已暂停",
                "当前页连续重试后仍无法读取。已提取的素材仍在暂存记录中，可以继续扫描当前页。",
                "稍后再试", "继续扫描", null, () -> scanButton.performClick());
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
            downloadCommentMedia(record, comment, false);
            if (record.includeReplies && comment.optInt("reply_comment_total") > 0) {
                String id = comment.optString("cid");
                if (!id.isEmpty() && !record.replyParents.contains(id)) record.replyParents.add(id);
            }
        }
        record.mainScanned = scannedNow;
        record.scanned = record.mainScanned + record.replyScanned;
        record.cursor = next;
        record.mainDone = !more;
        record.complete = !more && (!record.includeReplies || record.replyParents.isEmpty());
        persistRecord(record);
        runOnUiThread(() -> {
            if (currentRecord != record) return;
            status.setText(record.mainDone && record.includeReplies && !record.complete ? "主评论已完成，正在读取回复…"
                    : "扫描中 · 已发现 " + media.size() + " 个素材");
            if (scanning && !record.complete) {
                browser.postDelayed(this::startPage, douyinPageDelayMs);
            } else {
                scanning = false;
                scanButton.setText(record.complete ? "扫描完成" : "继续扫描");
                updateScanCard();
                if (record.complete) finishDouyinScan(record);
            }
        });
    }

    private void processRetriedReplies(ScanRecord record, String parent, JSONArray comments, boolean more, long next, int token) {
        record.processingPage = true;
        worker.execute(() -> {
            try {
                for (int i = 0; i < comments.length(); i++) {
                    JSONObject comment = comments.optJSONObject(i);
                    if (comment != null) downloadCommentMedia(record, comment, true);
                }
                record.replyScanned += comments.length();
                record.scanned = record.mainScanned + record.replyScanned;
                synchronized (record) {
                    if (more) record.failedReplies.put(parent, next);
                    else record.failedReplies.remove(parent);
                }
                persistRecord(record);
            } finally { record.processingPage = false; }
            runOnUiThread(() -> {
                if (record != currentRecord || token != requestToken || !parent.equals(retryReplyParent)) return;
                if (more) {
                    retryReplyCursor = next;
                    browser.postDelayed(() -> {
                        if (token == requestToken && parent.equals(retryReplyParent)) startPage();
                    }, douyinPageDelayMs);
                } else endReplyRetry("回复组重试完成，新增素材已追加到原记录");
            });
        });
    }

    private void processReplyPage(ScanRecord record, JSONArray comments, boolean more, long next, boolean emptyList) {
        if (record == null) return;
        if (emptyList && !more) {
            synchronized (record) {
                record.emptyReplyLists++;
                record.loggedEmptyReplyLists++;
                record.failedReplies.put(record.replyParents.get(record.replyIndex), record.replyCursor);
                record.addIssue("回复组 " + (record.replyIndex + 1) + "（主评论 ID "
                        + record.replyParents.get(record.replyIndex) + "）：未返回回复数据");
            }
        }
        for (int i = 0; i < comments.length(); i++) {
            JSONObject comment = comments.optJSONObject(i);
            if (comment != null) downloadCommentMedia(record, comment, true);
        }
        record.replyScanned += comments.length();
        record.scanned = record.mainScanned + record.replyScanned;
        if (more) record.replyCursor = next;
        else {
            record.replyIndex++;
            record.replyCursor = 0;
        }
        record.complete = record.replyIndex >= record.replyParents.size();
        persistRecord(record);
        runOnUiThread(() -> {
            if (currentRecord != record) return;
            updateScanCard();
            if (scanning && !record.complete) browser.postDelayed(this::startPage, douyinPageDelayMs);
            else {
                scanning = false;
                if (record.complete) finishDouyinScan(record);
                else scanButton.setText("继续扫描");
            }
        });
    }

    private void downloadCommentMedia(ScanRecord record, JSONObject comment, boolean reply) {
        String id = comment.optString("cid");
        JSONArray images = comment.optJSONArray("image_list");
        if (images != null) for (int i = 0; i < images.length(); i++) {
            JSONObject image = images.optJSONObject(i);
            if (image != null) download(record, url(image.optJSONObject("origin_url")), id, reply ? "回复图片" : "图片");
        }
        JSONObject sticker = comment.optJSONObject("sticker");
        if (sticker != null) {
            String source = url(sticker.optJSONObject("animate_url"));
            if (source == null) source = url(sticker.optJSONObject("static_url"));
            download(record, source, id, reply ? "回复表情" : "表情");
        }
    }

    private void finishDouyinScan(ScanRecord record) {
        if (currentRecord != record || !record.complete) return;
        scanning = false;
        scanButton.setText("扫描完成");
        updateScanCard();
        status.setText("已扫描 " + record.scanned + (record.includeReplies ? " 条评论" : " 条主评论")
                + "，找到 " + media.size() + " 个不同媒体（扫描完成）"
                + (record.emptyReplyLists > 0 ? "；" + record.emptyReplyLists + " 组回复未返回数据" : ""));
        browserPanel.setVisibility(View.GONE);
        updateWebToggle();
        readerScroll.post(() -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()));
        if (showingReader) new AlertDialog.Builder(this)
                .setTitle(record.includeReplies ? "评论扫描完成" : "主评论扫描完成")
                .setMessage((record.includeReplies ? "主评论 " + record.mainScanned + " 条，回复 " + record.replyScanned + " 条"
                        : "主评论 " + record.mainScanned + " 条") + "，提取 " + media.size() + " 个不同素材。结果已暂存。"
                        + (record.emptyReplyLists > 0 ? "\n" + record.emptyReplyLists + " 组回复未返回数据，未计入扫描数。" : ""))
                .setPositiveButton("查看结果", (dialog, which) -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()))
                .show();
    }

    private boolean download(ScanRecord record, String source, String commentId, String kind) {
        if (source == null || source.isEmpty()) return false;
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
            if (mime == null) throw new Exception("媒体格式无法识别");
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b & 0xff));
            String hash = hex.toString();
            synchronized (record) {
                if (record.hashes.contains(hash)) {
                    record.failedMedia.remove(source);
                    return true;
                }
            }
            String extension = mime.equals("video/mp4") ? ".mp4" : mime.equals("image/heic") ? ".heic" : mime.equals("image/avif") ? ".avif" : mime.equals("image/jpeg") ? ".jpg" : mime.equals("image/png") ? ".png" : mime.equals("image/gif") ? ".gif" : ".webp";
            if (!record.folder.isDirectory() && !record.folder.mkdirs()) throw new Exception("无法创建记录目录");
            File file = new File(record.folder, hash.substring(0, 20) + extension);
            try (OutputStream out = new FileOutputStream(file)) { out.write(bytes); }
            Drawable thumbnail = decodeThumbnail(file);
            Media item = new Media(file, hash, mime, commentId, kind, thumbnail, thumbnail instanceof Animatable);
            synchronized (record) {
                record.hashes.add(hash);
                record.items.add(item);
                record.failedMedia.remove(source);
            }
            persistRecord(record);
            runOnUiThread(() -> {
                if (currentRecord != record || !showingReader) return;
                if (media.contains(item)) return;
                media.add(item);
                updateResultLabels();
                if (matchesFilter(item)) addTile(item);
                if (media.size() == 1) readerScroll.postDelayed(() -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()), 200);
            });
            return true;
        } catch (Exception error) {
            String reason = "媒体格式无法识别".equals(error.getMessage()) ? "媒体格式无法识别" : "下载或写入暂存失败";
            synchronized (record) {
                if (!record.failedMedia.containsKey(source)) {
                    record.failedMedia.put(source, new FailedMedia(source, commentId, kind));
                    record.addIssue(kind + (commentId == null || commentId.isEmpty() ? "" : "（评论 ID " + commentId + "）") + "： " + reason);
                }
            }
            return false;
        }
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

    private void clearMediaSelection() {
        selectingMedia = false;
        selectedMedia.clear();
    }

    private void updateResultLabels() {
        int gifCount = 0;
        for (Media item : media) if (item.animated) gifCount++;
        allFilter.setText("全部 " + media.size());
        gifFilter.setText("动图 " + gifCount);
        imageFilter.setText("图片 " + (media.size() - gifCount));
        resultSummary.setText(selectingMedia ? "点击素材选择，可跨分类多选"
                : media.isEmpty() ? "提取后自动显示图片与动图预览" : media.size() + " 个不重复素材 · 默认预览中");
        selectionAction.setText(selectingMedia ? "完成" : "多选");
        countLabel.setText(selectingMedia ? "已选 " + selectedMedia.size() + " / " + media.size() + " 个" : "已提取 " + media.size() + " 个");
        saveButton.setText(selectingMedia ? "保存选中" : "保存全部");
        saveButton.setEnabled(!selectingMedia || !selectedMedia.isEmpty());
        saveButton.setAlpha(saveButton.isEnabled() ? 1f : 0.45f);
        shareSelectedButton.setVisibility(selectingMedia ? View.VISIBLE : View.GONE);
        shareSelectedButton.setEnabled(!selectedMedia.isEmpty());
        shareSelectedButton.setAlpha(shareSelectedButton.isEnabled() ? 1f : 0.45f);
        saveSub.setText(media.isEmpty() ? "保存到 下载 / DouYinGetting"
                : gifCount + " 个动图 · " + (media.size() - gifCount) + " 张图片");
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
        styleSegment(allFilter, "all");
        styleSegment(gifFilter, "gif");
        styleSegment(imageFilter, "image");
    }

    private void styleSegment(TextView button, String type) {
        boolean active = type.equals(selectedFilter);
        button.setTextColor(active ? INK : INK2);
        button.setBackground(active ? shape(SURFACE, 8) : null);
    }

    private void addTile(Media item) {
        if (displayedCount % 3 == 0) {
            resultRow = new LinearLayout(this);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(6);
            results.addView(resultRow, rowParams);
        }
        int tileWidth = (getResources().getDisplayMetrics().widthPixels - dp(52)) / 3;
        LinearLayout row = resultRow;
        FrameLayout art = new FrameLayout(this);
        art.setBackground(shape(SURFACE, 10));
        art.setClipToOutline(true);
        LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(tileWidth, tileWidth);
        if (displayedCount % 3 != 0) artParams.leftMargin = dp(6);
        resultRow.addView(art, artParams);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.addView(image, new FrameLayout.LayoutParams(-1, -1));
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
        if ("video/mp4".equals(item.mime)) {
            TextView play = label("▶", 14, Color.WHITE, true);
            play.setGravity(Gravity.CENTER);
            play.setBackground(shape(Color.argb(110, 0, 0, 0), 17));
            art.addView(play, new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));
        }
        if (item.animated || "video/mp4".equals(item.mime)) {
            String tag = "video/mp4".equals(item.mime) ? "视频" : ("image/gif".equals(item.mime) ? "GIF" : "动图");
            TextView badge = label(tag, 9, Color.WHITE, true);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(6), 0, dp(6), 0);
            badge.setBackground(shape(Color.argb(190, 23, 37, 43), 5));
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, dp(17), Gravity.LEFT | Gravity.BOTTOM);
            badgeParams.setMargins(dp(6), 0, 0, dp(6));
            art.addView(badge, badgeParams);
        }
        if (item.kind.startsWith("回复")) {
            TextView replyBadge = label("回复", 9, Color.WHITE, true);
            replyBadge.setGravity(Gravity.CENTER);
            replyBadge.setBackground(shape(ACCENT, 5));
            art.addView(replyBadge, new FrameLayout.LayoutParams(dp(32), dp(18), Gravity.RIGHT | Gravity.TOP));
        }
        TextView check = label(selectedMedia.contains(item) ? "✓" : "", 15, selectedMedia.contains(item) ? Color.WHITE : ACCENT, true);
        check.setGravity(Gravity.CENTER);
        check.setBackground(shape(selectedMedia.contains(item) ? ACCENT : Color.argb(225, 255, 255, 255), 13));
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.LEFT | Gravity.TOP);
        checkParams.setMargins(dp(6), dp(6), 0, 0);
        art.addView(check, checkParams);
        check.setVisibility(selectingMedia ? View.VISIBLE : View.GONE);
        art.setOnClickListener(v -> {
            if (!selectingMedia) {
                preview(item);
                return;
            }
            if (!selectedMedia.add(item)) selectedMedia.remove(item);
            boolean selected = selectedMedia.contains(item);
            check.setText(selected ? "✓" : "");
            check.setTextColor(selected ? Color.WHITE : ACCENT);
            check.setBackground(shape(selected ? ACCENT : Color.argb(225, 255, 255, 255), 13));
            updateResultLabels();
        });
        art.setOnLongClickListener(v -> {
            if (selectingMedia) return false;
            selectingMedia = true;
            selectedMedia.add(item);
            renderResults();
            updateResultLabels();
            return true;
        });
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

    private Button shareButton(String text, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(ACCENT);
        button.setTextSize(13);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(shape(ACCENT_SOFT, 10));
        button.setStateListAnimator(null);
        button.setOnClickListener(action);
        return button;
    }

    private void shareMedia(java.util.List<Media> items) {
        if (items.isEmpty()) return;
        try {
            ArrayList<Uri> uris = new ArrayList<>();
            String mime = items.get(0).mime;
            boolean imagesOnly = true;
            for (Media item : items) {
                if (!item.file.isFile()) throw new java.io.IOException("素材文件已不存在");
                uris.add(androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".share", item.file));
                if (!mime.equals(item.mime)) mime = "*/*";
                imagesOnly &= item.mime.startsWith("image/");
            }
            if ("*/*".equals(mime) && imagesOnly) mime = "image/*";
            boolean multiple = uris.size() > 1;
            Intent send = new Intent(multiple ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND).setType(mime);
            if (multiple) send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            else send.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            ClipData clip = ClipData.newRawUri("素材", uris.get(0));
            for (int i = 1; i < uris.size(); i++) clip.addItem(new ClipData.Item(uris.get(i)));
            send.setClipData(clip);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "分享素材"));
        } catch (java.io.IOException | RuntimeException error) {
            Toast.makeText(this, "无法分享，请确认素材仍在本机", Toast.LENGTH_SHORT).show();
        }
    }

    private void addPreviewAction(LinearLayout row, String text, View.OnClickListener action) {
        Button button = new Button(row.getContext(), null, android.R.attr.borderlessButtonStyle);
        button.setText(text);
        button.setTextColor(ACCENT);
        button.setMaxLines(1);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(2), 0, dp(2), 0);
        button.setAutoSizeTextTypeUniformWithConfiguration(10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        button.setOnClickListener(action);
        row.addView(button, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void preview(Media item) {
        try {
            boolean isVideo = "video/mp4".equals(item.mime);
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle(item.file.getName()).create();
            LinearLayout panel = new LinearLayout(dialog.getContext());
            panel.setOrientation(LinearLayout.VERTICAL);
            Animatable animation = null;
            if (isVideo) {
                VideoView video = new VideoView(this);
                video.setVideoPath(item.file.getAbsolutePath());
                video.setOnPreparedListener(player -> video.start());
                video.setOnErrorListener((player, what, extra) -> {
                    showSaveResult("视频无法播放，可能是设备不支持该编码");
                    return true;
                });
                video.setMediaController(new MediaController(this));
                panel.addView(video, new LinearLayout.LayoutParams(-1, dp(260), 1));
                dialog.setOnDismissListener(ignored -> video.stopPlayback());
            } else {
                Drawable drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(item.file));
                ImageView image = new ImageView(this);
                image.setImageDrawable(drawable);
                image.setAdjustViewBounds(true);
                panel.addView(image, new LinearLayout.LayoutParams(-1, -2, 1));
                if (drawable instanceof Animatable) {
                    Animatable animatedImage = (Animatable) drawable;
                    animation = animatedImage;
                    dialog.setOnDismissListener(ignored -> animatedImage.stop());
                }
            }

            LinearLayout actions = new LinearLayout(dialog.getContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(dp(8), 0, dp(8), 0);
            panel.addView(actions, new LinearLayout.LayoutParams(-1, dp(52)));
            addPreviewAction(actions, isFavorite(item) ? "取消收藏" : "收藏", v -> {
                dialog.dismiss();
                toggleFavorite(item);
            });
            addPreviewAction(actions, "分享", v -> shareMedia(Collections.singletonList(item)));
            addPreviewAction(actions, "关闭", v -> dialog.dismiss());
            addPreviewAction(actions, isVideo ? "保存此段" : "保存此张", v -> {
                dialog.dismiss();
                worker.execute(() -> {
                    try {
                        boolean saved = saveToDownloads(item);
                        runOnUiThread(() -> showSaveResult(saved ? "保存成功：" + item.file.getName() : "已保存过，已跳过重复素材"));
                    } catch (Exception error) {
                        runOnUiThread(() -> showSaveResult("保存失败：" + error.getMessage()));
                    }
                });
            });
            dialog.setView(panel);
            dialog.show();
            if (animation != null) animation.start();
        } catch (Exception error) {
            status.setText("预览失败：" + error.getMessage());
        }
    }

    private File favoriteFile(Media item) {
        String name = item.file.getName();
        return new File(new File(getFilesDir(), "favorites"), item.hash + name.substring(name.lastIndexOf('.')));
    }

    private boolean isFavorite(Media item) {
        return favoriteFile(item).isFile();
    }

    private void toggleFavorite(Media item) {
        boolean adding = !isFavorite(item);
        worker.execute(() -> {
            String message;
            try {
                File target = favoriteFile(item);
                if (target.isFile()) {
                    if (!target.delete()) throw new Exception("无法移除收藏");
                    message = "已取消收藏";
                } else {
                    File folder = target.getParentFile();
                    if (!folder.isDirectory() && !folder.mkdirs()) throw new Exception("无法创建收藏夹");
                    File temp = new File(folder, target.getName() + ".tmp");
                    try {
                        Files.copy(item.file.toPath(), temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } finally { temp.delete(); }
                    message = "已收藏，相同素材只保留一份";
                }
            } catch (Exception error) { message = "收藏操作失败：" + error.getMessage(); }
            String result = message;
            runOnUiThread(() -> {
                if (adding && isFavorite(item) && showingReader && !isFinishing()) {
                    showFavoriteNotice();
                    return;
                }
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                if (showingFavorites) showFavorites();
            });
        });
    }

    private void showFavoriteNotice() {
        FrameLayout area = (FrameLayout) readerScroll.getParent();
        if (favoriteNotice != null) area.removeView(favoriteNotice);

        LinearLayout notice = new LinearLayout(this);
        notice.setGravity(Gravity.CENTER_VERTICAL);
        notice.setPadding(dp(16), dp(4), dp(6), dp(4));
        notice.setBackground(card(14));
        notice.setElevation(dp(6));
        notice.addView(label("已收藏", 14, INK, true), new LinearLayout.LayoutParams(0, -2, 1));

        TextView action = label("查看收藏", 12, ACCENT, true);
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(12), 0, dp(12), 0);
        action.setBackground(shape(ACCENT_SOFT, 10));
        action.setOnClickListener(v -> {
            area.removeView(notice);
            favoriteNotice = null;
            openFavorites(this::returnToReaderFromFavorites);
        });
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-2, dp(40));
        actionParams.setMarginStart(dp(16));
        notice.addView(action, actionParams);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(192), -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.bottomMargin = dp(62);
        favoriteNotice = notice;
        area.addView(notice, params);
        notice.setTranslationY(dp(16));
        notice.setAlpha(0f);
        notice.animate().translationY(0).alpha(1f).setDuration(150).start();
        getWindow().getDecorView().postDelayed(() -> {
            if (favoriteNotice != notice) return;
            area.removeView(notice);
            favoriteNotice = null;
        }, 2000);
    }

    private void openFavorites(Runnable onBack) {
        returnFromFavorites = onBack;
        if (scanning) {
            scanning = false;
            requestToken++;
            scanButton.setText("继续扫描");
            status.setText("扫描已暂停，可返回读取页继续扫描");
        }
        showFavorites();
    }

    private void returnToReaderFromFavorites() {
        if (currentRecord != null) {
            synchronized (currentRecord) {
                for (Media item : currentRecord.items) if (!media.contains(item)) media.add(item);
            }
            renderResults();
            updateResultLabels();
        }
        showReader();
    }

    private void leaveFavorites() {
        showingFavorites = false;
        favoriteScreen = null;
        Runnable onBack = returnFromFavorites;
        returnFromFavorites = null;
        onBack.run();
    }

    private void showFavorites() {
        showingFavorites = true;
        showingReader = false;
        showingRecords = false;
        showingRecordPlatforms = false;
        showingSettings = false;
        for (Animatable animation : animations) animation.stop();
        new FavoritesScreen().show();
    }

    private void saveMediaBatch(ArrayList<Media> items, java.util.function.Consumer<String> onResult) {
        worker.execute(() -> {
            int saved = 0;
            int skipped = 0;
            for (Media item : items) {
                try { if (saveToDownloads(item)) saved++; else skipped++; } catch (Exception ignored) { }
            }
            String message = items.isEmpty() ? "没有可保存的文件"
                    : "保存结果：成功 " + saved + " 个，重复跳过 " + skipped + " 个，失败 " + (items.size() - saved - skipped) + " 个";
            runOnUiThread(() -> onResult.accept(message));
        });
    }

    private class FavoritesScreen {
        private final LinearLayout root = new LinearLayout(MainActivity.this);
        private final LinearLayout list = new LinearLayout(MainActivity.this);
        private final ArrayList<Media> items = new ArrayList<>();
        private final HashSet<Media> selected = new HashSet<>();
        private final String[] categories = {"全部", "图片", "动图", "视频"};
        private final TextView[] filters = new TextView[categories.length];
        private LinearLayout actions;
        private TextView title;
        private Button select;
        private Button share;
        private Button save;
        private Button remove;
        private int category;
        private boolean selecting;
        private boolean busy;
        private boolean loaded;

        void show() {
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(BG);
            root.setFitsSystemWindows(true);
            LinearLayout header = new LinearLayout(MainActivity.this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(20), 0, dp(20), 0);
            root.addView(header, new LinearLayout.LayoutParams(-1, dp(54)));
            TextView back = label("‹", 24, INK, true);
            back.setGravity(Gravity.CENTER);
            back.setOnClickListener(v -> back());
            header.addView(back, new LinearLayout.LayoutParams(dp(44), -1));
            title = label("收藏夹", 18, INK, true);
            header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            select = shareButton("多选", v -> setSelecting(!selecting));
            header.addView(select, new LinearLayout.LayoutParams(dp(68), dp(44)));
            select.setEnabled(false);

            LinearLayout tabs = new LinearLayout(MainActivity.this);
            tabs.setPadding(dp(3), dp(3), dp(3), dp(3));
            tabs.setBackground(shape(TRACK, 10));
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(-1, dp(44));
            tabParams.setMargins(dp(20), dp(8), dp(20), 0);
            root.addView(tabs, tabParams);
            for (int i = 0; i < categories.length; i++) {
                int target = i;
                TextView tab = label(categories[i], 13, INK2, true);
                tab.setGravity(Gravity.CENTER);
                tab.setOnClickListener(v -> {
                    category = target;
                    render();
                });
                filters[i] = tab;
                tabs.addView(tab, new LinearLayout.LayoutParams(0, -1, 1));
            }

            ScrollView scroll = new ScrollView(MainActivity.this);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(20), dp(12), dp(20), dp(20));
            scroll.addView(list);

            actions = new LinearLayout(MainActivity.this);
            actions.setPadding(dp(12), dp(6), dp(12), dp(6));
            root.addView(actions, new LinearLayout.LayoutParams(-1, dp(60)));
            share = addAction("分享", v -> shareMedia(selection()));
            save = addAction("保存", v -> {
                busy = true;
                updateActions();
                saveMediaBatch(selection(), message -> {
                    busy = false;
                    updateActions();
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                });
            });
            remove = addAction("取消收藏", v -> confirmRemove());
            remove.setTextColor(DANGER);
            favoriteScreen = this;
            setContentView(root);
            render();

            worker.execute(() -> {
                ArrayList<Media> found = readFavorites();
                runOnUiThread(() -> {
                    if (favoriteScreen != this || !root.isAttachedToWindow()) return;
                    items.addAll(found);
                    loaded = true;
                    render();
                });
            });
        }

        private Button addAction(String text, View.OnClickListener action) {
            Button button = shareButton(text, action);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
            params.setMargins(dp(4), 0, dp(4), 0);
            actions.addView(button, params);
            return button;
        }

        void back() {
            if (selecting) setSelecting(false);
            else leaveFavorites();
        }

        private void setSelecting(boolean value) {
            selecting = value;
            selected.clear();
            render();
        }

        private ArrayList<Media> selection() {
            ArrayList<Media> snapshot = new ArrayList<>();
            for (Media item : items) if (selected.contains(item)) snapshot.add(item);
            return snapshot;
        }

        private int type(Media item) {
            if (item.mime.startsWith("video/")) return 3;
            return item.animated ? 2 : 1;
        }

        private void render() {
            for (int i = 0; i < filters.length; i++) {
                filters[i].setSelected(category == i);
                filters[i].setTextColor(category == i ? INK : INK2);
                filters[i].setBackground(category == i ? shape(SURFACE, 8) : null);
            }
            list.removeAllViews();
            LinearLayout row = null;
            int count = 0;
            int size = (getResources().getDisplayMetrics().widthPixels - dp(52)) / 3;
            for (Media item : items) {
                if (category != 0 && category != type(item)) continue;
                if (count % 3 == 0) {
                    row = new LinearLayout(MainActivity.this);
                    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                    rp.topMargin = dp(8);
                    list.addView(row, rp);
                }
                FrameLayout tile = new FrameLayout(MainActivity.this);
                tile.setBackground(card(10));
                tile.setClipToOutline(true);
                LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(size, size);
                if (count % 3 != 0) tileParams.leftMargin = dp(6);
                row.addView(tile, tileParams);
                ImageView image = new ImageView(MainActivity.this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setImageDrawable(item.thumbnail);
                tile.addView(image, new FrameLayout.LayoutParams(-1, -1));
                if (type(item) != 1) {
                    TextView badge = label(categories[type(item)], 10, Color.WHITE, true);
                    badge.setBackground(shape(DARK, 6));
                    badge.setPadding(dp(6), dp(2), dp(6), dp(2));
                    FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
                    badgeParams.setMargins(dp(6), 0, 0, dp(6));
                    tile.addView(badge, badgeParams);
                }
                TextView check = label("", 16, Color.WHITE, true);
                check.setGravity(Gravity.CENTER);
                if (selecting) {
                    FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.END | Gravity.TOP);
                    checkParams.setMargins(0, dp(6), dp(6), 0);
                    tile.addView(check, checkParams);
                }
                updateTile(tile, check, item);
                tile.setOnClickListener(v -> {
                    if (!selecting) {
                        preview(item);
                        return;
                    }
                    if (!selected.add(item)) selected.remove(item);
                    updateTile(tile, check, item);
                    updateActions();
                });
                count++;
            }
            if (count == 0) {
                String message = !loaded ? "正在读取收藏…"
                        : items.isEmpty() ? "还没有收藏。在素材预览里点「收藏」即可添加。" : "这个分类还没有收藏";
                TextView empty = label(message, 14, INK2, false);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, dp(24), 0, dp(24));
                list.addView(empty, new LinearLayout.LayoutParams(-1, -2));
            }
            updateActions();
        }

        private void updateTile(FrameLayout tile, TextView check, Media item) {
            boolean checked = selected.contains(item);
            tile.setSelected(checked);
            tile.setContentDescription((selecting ? checked ? "已选 " : "选择 " : "预览 ")
                    + categories[type(item)] + " " + item.file.getName());
            check.setText(checked ? "✓" : "");
            check.setBackground(shape(checked ? ACCENT : Color.argb(165, 23, 37, 43), 14));
        }

        private void updateActions() {
            title.setText(selecting ? "已选 " + selected.size() + " 项" : "收藏夹 · " + items.size());
            select.setText(selecting ? "完成" : "多选");
            select.setEnabled(selecting || !items.isEmpty());
            actions.setVisibility(selecting ? View.VISIBLE : View.GONE);
            for (Button button : new Button[]{share, save, remove}) {
                button.setEnabled(!selected.isEmpty() && !busy);
                button.setAlpha(button.isEnabled() ? 1f : 0.45f);
            }
        }

        private void confirmRemove() {
            ArrayList<Media> snapshot = selection();
            showConfirmation(R.drawable.ic_remove_favorite, true, "取消收藏 " + snapshot.size() + " 项？",
                    "这 " + snapshot.size() + " 项将从收藏夹移除。\n扫描记录和已保存的文件仍会保留。",
                    "保留收藏", "取消收藏", null, () -> {
                busy = true;
                updateActions();
                worker.execute(() -> {
                    ArrayList<Media> removed = new ArrayList<>();
                    for (Media item : snapshot) {
                        if (item.file.delete() || !item.file.exists()) removed.add(item);
                    }
                    runOnUiThread(() -> {
                        if (favoriteScreen == this) {
                            busy = false;
                            items.removeAll(removed);
                            selected.removeAll(removed);
                            if (selected.isEmpty()) selecting = false;
                            render();
                        }
                        int failed = snapshot.size() - removed.size();
                        Toast.makeText(MainActivity.this, "已取消收藏 " + removed.size() + " 项"
                                + (failed > 0 ? "，失败 " + failed + " 项" : ""), Toast.LENGTH_SHORT).show();
                    });
                });
            });
        }
    }

    private ArrayList<Media> readFavorites() {
        ArrayList<Media> items = new ArrayList<>();
        File[] files = new File(getFilesDir(), "favorites").listFiles();
        if (files == null) return items;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File file : files) {
            String name = file.getName();
            int extension = name.lastIndexOf('.');
            if (!file.isFile() || extension <= 0 || name.endsWith(".tmp")) continue;
            try (InputStream stream = new FileInputStream(file)) {
                byte[] bytes = new byte[32];
                if (stream.read(bytes) < 12) continue;
                String mime = format(bytes);
                if (mime == null) continue;
                Drawable thumbnail = decodeThumbnail(file);
                items.add(new Media(file, name.substring(0, extension), mime, "", "收藏", thumbnail, false));
            } catch (Exception ignored) { }
        }
        return items;
    }

    private void showSaveResult(String message) {
        status.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showTopNotice(String message) {
        if (noticePopup != null) noticePopup.dismiss();
        TextView notice = label(message, 13, Color.WHITE, true);
        notice.setGravity(Gravity.CENTER);
        notice.setMaxWidth(dp(320));
        notice.setPadding(dp(16), dp(11), dp(16), dp(11));
        notice.setBackground(shape(DARK, 12));
        PopupWindow popup = new PopupWindow(notice, -2, -2);
        popup.setElevation(dp(8));
        noticePopup = popup;
        popup.showAtLocation(readerRoot, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp(64));
        readerRoot.postDelayed(() -> {
            if (noticePopup == popup) {
                popup.dismiss();
                noticePopup = null;
            }
        }, 2000);
    }

    private boolean saveToDownloads(Media item) throws Exception {
        String key = "saved_media_" + item.hash;
        String previous = getPreferences(MODE_PRIVATE).getString(key, null);
        if (previous != null) {
            try (android.os.ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(Uri.parse(previous), "r")) {
                if (descriptor != null) return false;
            } catch (Exception ignored) { }
        }
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
        getPreferences(MODE_PRIVATE).edit().putString(key, target.toString()).commit();
        return true;
    }

    @Override protected void onDestroy() {
        scanning = false;
        if (noticePopup != null) noticePopup.dismiss();
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
        if (showingFavorites) favoriteScreen.back();
        else if (showingRecords) leaveRecords();
        else if (showingRecordPlatforms) showSettings();
        else if (showingReader) {
            if (viewingSavedRecord) showRecords();
            else showHome();
        }
        else if (showingSettings) showHome();
        else super.onBackPressed();
    }
}
