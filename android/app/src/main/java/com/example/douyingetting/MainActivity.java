package com.example.douyingetting;

import android.app.Activity;
import android.Manifest;
import android.content.ContentValues;
import android.content.ClipData;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageDecoder;
import android.graphics.Bitmap;
import android.graphics.Rect;
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
    private final android.util.SparseArray<Typeface> homeFonts = new android.util.SparseArray<>();
    private Typeface homeTitleFont;
    private Bitmap homeRabbit;
    private ScanRecord currentRecord;
    private String activeLink;
    private boolean viewingSavedRecord;
    private boolean resumedFromHome;
    private static final int EXPORT_RECORDS = 7101, IMPORT_RECORDS = 7102;
    private boolean recordTransferBusy;
    private boolean savedRecordPageOpened;
    private boolean loadingSavedRecordPage;
    private boolean xhsMode;
    private TextView readerHeading;
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
    private LinearLayout readerRoot;
    private FrameLayout stage;
    private FrameLayout pageLayer;
    private FrameLayout stripHost;
    private LinearLayout stripBar;
    private LinearLayout stripPausedBar;
    private LinearLayout stripCard;
    private TextView stripTime;
    private TextView stripCount;
    private TextView stripPausedText;
    private TextView stripCardTitle;
    private TextView stripCardTime;
    private TextView stripCardCount;
    private View stripDot;
    private View stripCardFill;
    private View stripCardTrack;
    private final android.os.Handler stripHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean stripExpanded;
    private boolean stripPaused;
    private boolean stripWasRunning;
    private long stripAccum;
    private long stripResumeAt;
    private String stripRecordId;
    private String stripLastClock;
    private String stripLastCount;
    private String stripLastPaused;
    private String stripLastCard;
    private Runnable stripCollapse;
    private float stripPhase;
    private final Runnable stripTick = new Runnable() {
        @Override public void run() {
            updateScanStrip();
            stripHandler.postDelayed(this, 500);
        }
    };
    private LinearLayout browserPanel;
    private TextView webToggle;
    private ImageView webToggleArrow;
    private ImageView scanStatusIcon;
    private TextView scanStateLabel;
    private FrameLayout readerFilterTrack;
    private View readerFilterIndicator;
    private android.animation.ValueAnimator readerFilterAnimation;
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
    private int BG, DARK, GREEN, MUTED, SURFACE, LINE, INK, INK2, INK3, ACCENT, ACCENT_SOFT, TRACK, DANGER;
    private boolean darkMode;
    private boolean decodeEnabled = true;
    private boolean motionEnabled = true;
    private android.animation.ValueAnimator themeAnimation;
    private boolean titleDecoded;
    private boolean showingMy;
    private int currentPage = 1;
    private int settingsOrigin = 1;
    private int recordsOrigin = 1;
    private LinearLayout mainShell;
    private FrameLayout mainHost;
    private FrameLayout navTrack;
    private View navIndicator;
    private final ArrayList<TextView> navLabels = new ArrayList<>();
    private final ArrayList<ImageView> navIcons = new ArrayList<>();
    private int mainTab = 1;
    private boolean returningTransition;
    private android.animation.ValueAnimator navAnimation;

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
        applyPalette();
        setTheme(darkMode ? R.style.BlueDark : R.style.BlueLight);
        super.onCreate(state);
        decodeEnabled = getPreferences(MODE_PRIVATE).getBoolean("title_decode", true);
        motionEnabled = getPreferences(MODE_PRIVATE).getBoolean("page_motion", true);
        douyinPageDelayMs = getPreferences(MODE_PRIVATE).getInt("douyin_page_delay_ms", 2000);
        douyinReadReplies = getPreferences(MODE_PRIVATE).getBoolean("douyin_read_replies", false);
        updateSystemBars();
        buildReader();
        buildStage();
        loadRecords();
        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            String shared = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null) input.setText(shared);
        }
        showHome();
        updateSystemBars();
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
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
        GradientDrawable drawable = new OutlinedCard();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(1, LINE);
        return drawable;
    }

    private static class OutlinedCard extends GradientDrawable { }

    private TextView segment(String text, String type, LinearLayout parent) {
        TextView button = homeLabel(text, 12, INK2, 650);
        button.setGravity(Gravity.CENTER);
        parent.addView(button, new LinearLayout.LayoutParams(0, -1, 1));
        button.setOnClickListener(v -> {
            if (selectedFilter.equals(type)) return;
            int previous = readerFilterIndex();
            selectedFilter = type;
            positionReaderFilter(motionEnabled);
            results.animate().cancel();
            renderResults();
            updateResultLabels();
            if (motionEnabled) {
                results.setAlpha(0f);
                results.setTranslationX(dp(previous < readerFilterIndex() ? 9 : -9));
                results.animate().alpha(1f).translationX(0f).setDuration(220).start();
            } else {
                results.setAlpha(1f);
                results.setTranslationX(0f);
            }
        });
        pressFeedback(button);
        return button;
    }

    private int readerFilterIndex() {
        return "all".equals(selectedFilter) ? 0 : "gif".equals(selectedFilter) ? 1 : 2;
    }

    private void positionReaderFilter(boolean animate) {
        if (readerFilterTrack == null) return;
        int width = readerFilterTrack.getWidth() - readerFilterTrack.getPaddingLeft() - readerFilterTrack.getPaddingRight();
        if (width <= 0) return;
        android.view.ViewGroup.LayoutParams params = readerFilterIndicator.getLayoutParams();
        params.width = width / 3;
        readerFilterIndicator.setLayoutParams(params);
        float target = (float) params.width * readerFilterIndex();
        if (readerFilterAnimation != null) readerFilterAnimation.cancel();
        if (animate) {
            readerFilterAnimation = android.animation.ValueAnimator.ofFloat(readerFilterIndicator.getTranslationX(), target);
            readerFilterAnimation.setDuration(220);
            readerFilterAnimation.addUpdateListener(value -> readerFilterIndicator.setTranslationX((float) value.getAnimatedValue()));
            readerFilterAnimation.start();
        } else readerFilterIndicator.setTranslationX(target);
    }

    private void pressFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && motionEnabled)
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
            else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL)
                v.animate().scaleX(1f).scaleY(1f).setDuration(motionEnabled ? 120 : 0).start();
            return false;
        });
    }

    private void readerIcon(TextView view, int resource, int size) {
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER);
        view.setPadding(view.getPaddingLeft(), 0, view.getPaddingRight(), 0);
        Drawable drawable = getDrawable(resource).mutate();
        drawable.setTint(view.getCurrentTextColor());
        drawable.setBounds(0, 0, dp(size), dp(size));
        view.setCompoundDrawablesRelative(drawable, null, null, null);
        view.setCompoundDrawablePadding(dp(7));
        Runnable align = () -> {
            int padding = Math.max(dp(4), (view.getWidth() - dp(size + 7)
                    - (int) view.getPaint().measureText(view.getText().toString())) / 2);
            if (view.getWidth() > 0 && view.getPaddingLeft() != padding)
                view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
        };
        view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> align.run());
        view.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable text) { view.post(align); }
        });
        pressFeedback(view);
    }


    private void applyPalette() {
        String mode = getPreferences(MODE_PRIVATE).getString("appearance", "system");
        darkMode = "dark".equals(mode) || ("system".equals(mode)
                && (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        BG = Color.parseColor(darkMode ? "#0D1320" : "#F4F6FA");
        SURFACE = Color.parseColor(darkMode ? "#1A2436" : "#FFFFFF");
        LINE = Color.parseColor(darkMode ? "#303E55" : "#DCE2ED");
        INK = Color.parseColor(darkMode ? "#F3F6FF" : "#0F172A");
        INK2 = Color.parseColor(darkMode ? "#BEC9DC" : "#536176");
        INK3 = Color.parseColor(darkMode ? "#95A6C1" : "#738198");
        ACCENT = Color.parseColor(darkMode ? "#708CFF" : "#456BFF");
        ACCENT_SOFT = Color.parseColor(darkMode ? "#24345C" : "#E7EDFF");
        TRACK = Color.parseColor(darkMode ? "#222E42" : "#E9EDF4");
        DANGER = Color.parseColor(darkMode ? "#F09791" : "#AD4741");
        DARK = INK;
        GREEN = ACCENT;
        MUTED = INK3;
    }

    private void updateSystemBars() {
        View decor = getWindow().getDecorView();
        android.view.WindowInsetsController controller = android.os.Build.VERSION.SDK_INT >= 30
                ? decor.getWindowInsetsController() : null;
        if (controller != null) {
            int mask = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(darkMode ? 0 : mask, mask);
        } else {
            int mask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decor.setSystemUiVisibility((decor.getSystemUiVisibility() & ~mask) | (darkMode ? 0 : mask));
        }
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
    }

    private int[] palette() {
        return new int[]{BG, SURFACE, LINE, INK, INK2, INK3, ACCENT, ACCENT_SOFT, TRACK, DANGER,
                darkMode ? 0xFFFFB571 : 0xFFE46A10};
    }

    private int mappedColor(int color, int[] old, int[] updated) {
        for (int i = 0; i < old.length; i++) if (old[i] == color) return updated[i];
        return color;
    }

    private void retheme(View view, int[] old, int[] updated) {
        if (view instanceof WebView) return;
        Drawable background = view.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable gradient = (GradientDrawable) background;
            if (gradient.getColor() != null)
                gradient.setColor(mappedColor(gradient.getColor().getDefaultColor(), old, updated));
            if (gradient instanceof OutlinedCard) gradient.setStroke(
                    "reader-primary-outline".equals(view.getTag()) ? dp(1) : 1,
                    "reader-primary-outline".equals(view.getTag()) ? ACCENT : LINE);
        } else if (background instanceof android.graphics.drawable.ColorDrawable) {
            view.setBackgroundColor(mappedColor(((android.graphics.drawable.ColorDrawable) background).getColor(), old, updated));
        }
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            // White text on action buttons and image overlays stays white.
            if (text.getCurrentTextColor() != Color.WHITE)
                text.setTextColor(mappedColor(text.getCurrentTextColor(), old, updated));
            for (Drawable drawable : text.getCompoundDrawablesRelative())
                if (drawable != null) drawable.setTint(text.getCurrentTextColor());
            text.setHintTextColor(INK3);
        }
        if (view instanceof Switch) {
            Switch toggle = (Switch) view;
            if (toggle.getTrackTintList() != null)
                toggle.setTrackTintList(ColorStateList.valueOf(mappedColor(
                        toggle.getTrackTintList().getDefaultColor(), old, updated)));
        }
        if (view instanceof ImageView) {
            ImageView image = (ImageView) view;
            if (image.getImageTintList() != null)
                image.setImageTintList(ColorStateList.valueOf(mappedColor(image.getImageTintList().getDefaultColor(), old, updated)));
        }
        if (view instanceof ProgressBar) ((ProgressBar) view).setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) retheme(group.getChildAt(i), old, updated);
        }
    }

    private LinearLayout pageRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);
        return root;
    }

    private void stopScanForNavigation() {
        for (Animatable animation : animations) animation.stop();
        updateScanStrip();
    }

    private void resetPages() {
        showingReader = false;
        showingRecords = false;
        showingRecordPlatforms = false;
        showingSettings = false;
        showingFavorites = false;
        showingMy = false;
    }

    private ImageView icon(int resource, int color) {
        ImageView image = new ImageView(this);
        image.setImageResource(resource);
        image.setImageTintList(ColorStateList.valueOf(color));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return image;
    }

    private View settingsButton() {
        ImageView button = icon(R.drawable.blue_gear, INK);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setContentDescription("设置");
        button.setOnClickListener(v -> {
            settingsOrigin = currentPage;
            stopScanForNavigation();
            showSettings();
        });
        return button;
    }

    private LinearLayout header(LinearLayout root, String title, boolean back, Runnable goBack) {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(18), dp(6), dp(12), dp(6));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(60)));
        if (back) {
            TextView button = label("‹", 28, INK, false);
            button.setGravity(Gravity.CENTER);
            button.setContentDescription("返回");
            button.setOnClickListener(v -> goBack.run());
            bar.addView(button, new LinearLayout.LayoutParams(dp(36), dp(44)));
        }
        bar.addView(label(title, back ? 19 : 24, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        if (!back) bar.addView(settingsButton(), new LinearLayout.LayoutParams(dp(44), dp(44)));
        return bar;
    }

    private void ensureMainShell() {
        if (mainShell != null) return;
        mainShell = pageRoot();
        mainHost = new FrameLayout(this);
        mainShell.addView(mainHost, new LinearLayout.LayoutParams(-1, 0, 1));
        navTrack = new FrameLayout(this);
        navTrack.setBackground(outlined(0, SURFACE));
        mainShell.addView(navTrack, new LinearLayout.LayoutParams(-1, dp(68)));
        navIndicator = new View(this);
        navIndicator.setBackground(shape(ACCENT_SOFT, 16));
        FrameLayout.LayoutParams indicatorParams = new FrameLayout.LayoutParams(dp(94), dp(54));
        indicatorParams.topMargin = dp(7);
        navTrack.addView(navIndicator, indicatorParams);
        LinearLayout tabs = new LinearLayout(this);
        navTrack.addView(tabs, new FrameLayout.LayoutParams(-1, -1));
        String[] names = {"收藏", "首页", "我的"};
        int[] icons = {R.drawable.blue_star, R.drawable.blue_home, R.drawable.blue_person};
        navLabels.clear();
        navIcons.clear();
        for (int i = 0; i < names.length; i++) {
            int target = i;
            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            tab.setContentDescription(names[i] + "导航");
            ImageView image = icon(icons[i], INK2);
            tab.addView(image, new LinearLayout.LayoutParams(dp(24), dp(24)));
            TextView text = homeLabel(names[i], 12, INK2, 550);
            text.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
            tp.topMargin = dp(3);
            tab.addView(text, tp);
            tabs.addView(tab, new LinearLayout.LayoutParams(0, -1, 1));
            navLabels.add(text);
            navIcons.add(image);
            tab.setOnClickListener(v -> {
                if (currentPage == target) return;
                if (target == 0) openFavorites(this::showHome);
                else if (target == 1) showHome();
                else showMy();
            });
        }
        navTrack.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            if (r - l != or - ol) positionNav(false);
        });
    }

    private void positionNav(boolean animate) {
        if (navTrack == null || navTrack.getWidth() == 0) return;
        float target = navTrack.getWidth() * (mainTab + 0.5f) / 3f - dp(47);
        if (navAnimation != null) navAnimation.cancel();
        if (animate && motionEnabled) {
            navAnimation = android.animation.ValueAnimator.ofFloat(navIndicator.getTranslationX(), target);
            navAnimation.setDuration(220);
            navAnimation.addUpdateListener(value -> navIndicator.setTranslationX((float) value.getAnimatedValue()));
            navAnimation.start();
        } else navIndicator.setTranslationX(target);
        navIcons.get(0).setImageResource(mainTab == 0 ? R.drawable.blue_star_filled : R.drawable.blue_star);
        navIcons.get(1).setImageResource(mainTab == 1 ? R.drawable.blue_home_filled : R.drawable.blue_home);
        for (int i = 0; i < 3; i++) {
            navLabels.get(i).setTextColor(i == mainTab ? ACCENT : INK2);
            navIcons.get(i).setImageTintList(ColorStateList.valueOf(i == mainTab ? ACCENT : INK2));
        }
    }

    private void showMainPage(LinearLayout root, int tab) {
        int previous = currentPage;
        returningTransition = false;
        currentPage = tab;
        ensureMainShell();
        root.setFitsSystemWindows(false);
        if (root.getParent() instanceof android.view.ViewGroup)
            ((android.view.ViewGroup) root.getParent()).removeView(root);
        mainHost.removeAllViews();
        mainHost.addView(root, new FrameLayout.LayoutParams(-1, -1));
        showLayerPage(mainShell);
        mainTab = tab;
        positionNav(previous >= 0);
        if (motionEnabled && previous != tab) {
            root.setAlpha(0f);
            root.setTranslationX(dp(previous <= tab ? 16 : -16));
            root.animate().alpha(1).translationX(0).setDuration(220).start();
        }
    }

    private void presentSecondary(LinearLayout root) {
        if (root == readerRoot) {
            readerRoot.setVisibility(View.VISIBLE);
            if (pageLayer != null) pageLayer.setVisibility(View.GONE);
        } else {
            showLayerPage(root);
        }
        if (motionEnabled) {
            root.setAlpha(0f);
            root.setTranslationX(dp(returningTransition ? -12 : 12));
            root.animate().alpha(1).translationX(0).setDuration(220).start();
        }
        returningTransition = false;
        updateScanStrip();
    }

    private void showLayerPage(View root) {
        if (root.getParent() instanceof android.view.ViewGroup)
            ((android.view.ViewGroup) root.getParent()).removeView(root);
        pageLayer.removeAllViews();
        pageLayer.addView(root, new FrameLayout.LayoutParams(-1, -1));
        root.requestApplyInsets();
        pageLayer.setVisibility(View.VISIBLE);
        pageLayer.bringToFront();
        if (stripHost != null) stripHost.bringToFront();
        updateScanStrip();
    }

    private int statusBarHeight() {
        int resource = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resource > 0 ? getResources().getDimensionPixelSize(resource) : dp(24);
    }

    private void buildStage() {
        stage = new FrameLayout(this);
        stage.setBackgroundColor(BG);
        stage.addView(readerRoot, new FrameLayout.LayoutParams(-1, -1));
        pageLayer = new FrameLayout(this);
        pageLayer.setBackgroundColor(BG);
        stage.addView(pageLayer, new FrameLayout.LayoutParams(-1, -1));
        buildScanStrip();
        setContentView(stage);
        stripHandler.postDelayed(stripTick, 500);
    }

    private void buildScanStrip() {
        stripHost = new FrameLayout(this);
        stripHost.setClipChildren(false);
        stage.addView(stripHost, new FrameLayout.LayoutParams(-1, -1));
        stripHost.setVisibility(View.GONE);
        int top = statusBarHeight() + dp(8);

        stripBar = new LinearLayout(this);
        stripBar.setOrientation(LinearLayout.HORIZONTAL);
        stripBar.setGravity(Gravity.CENTER_VERTICAL);
        stripBar.setPadding(dp(12), 0, dp(10), 0);
        stripBar.setBackground(outlined(15, SURFACE));
        stripBar.setElevation(dp(2));
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(-1, dp(30));
        barParams.setMargins(dp(16), top, dp(16), 0);
        stripHost.addView(stripBar, barParams);
        stripDot = new View(this);
        stripDot.setBackground(shape(ACCENT, 3));
        stripBar.addView(stripDot, new LinearLayout.LayoutParams(dp(6), dp(6)));
        stripTime = stripClock(12);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(-2, -2);
        timeParams.leftMargin = dp(8);
        stripBar.addView(stripTime, timeParams);
        stripCount = label("", 11, INK2, false);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(0, -2, 1);
        countParams.leftMargin = dp(6);
        stripBar.addView(stripCount, countParams);
        TextView chevron = label("⌄", 13, INK3, true);
        chevron.setGravity(Gravity.CENTER);
        stripBar.addView(chevron, new LinearLayout.LayoutParams(dp(22), -1));
        stripBar.setOnClickListener(v -> expandScanStrip());

        stripPausedBar = new LinearLayout(this);
        stripPausedBar.setOrientation(LinearLayout.HORIZONTAL);
        stripPausedBar.setGravity(Gravity.CENTER_VERTICAL);
        stripPausedBar.setPadding(dp(12), 0, dp(6), 0);
        stripPausedBar.setBackground(outlined(15, SURFACE));
        stripPausedBar.setElevation(dp(2));
        FrameLayout.LayoutParams pausedParams = new FrameLayout.LayoutParams(-1, dp(30));
        pausedParams.setMargins(dp(16), top, dp(16), 0);
        stripHost.addView(stripPausedBar, pausedParams);
        View pausedDot = new View(this);
        pausedDot.setBackground(shape(INK3, 3));
        stripPausedBar.addView(pausedDot, new LinearLayout.LayoutParams(dp(6), dp(6)));
        stripPausedText = label("已暂停", 11, INK2, false);
        LinearLayout.LayoutParams pausedTextParams = new LinearLayout.LayoutParams(0, -2, 1);
        pausedTextParams.leftMargin = dp(8);
        stripPausedBar.addView(stripPausedText, pausedTextParams);
        TextView resume = label("继续", 11, ACCENT, true);
        resume.setGravity(Gravity.CENTER);
        resume.setBackground(shape(ACCENT_SOFT, 9));
        resume.setOnClickListener(v -> resumeScanFromStrip());
        stripPausedBar.addView(resume, new LinearLayout.LayoutParams(dp(44), dp(22)));
        stripPausedBar.setVisibility(View.GONE);

        stripCard = new LinearLayout(this);
        stripCard.setOrientation(LinearLayout.VERTICAL);
        stripCard.setPadding(dp(12), dp(9), dp(10), dp(9));
        stripCard.setBackground(card(16));
        stripCard.setElevation(dp(4));
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(-1, dp(112));
        cardParams.setMargins(dp(16), top, dp(16), 0);
        stripHost.addView(stripCard, cardParams);
        stripCard.setVisibility(View.GONE);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        stripCard.addView(head);
        stripCardTitle = label("正在扫描", 13, INK, true);
        head.addView(stripCardTitle, new LinearLayout.LayoutParams(0, -2, 1));
        stripCardTime = stripClock(11);
        stripCardTime.setTextColor(INK3);
        LinearLayout.LayoutParams cardTimeParams = new LinearLayout.LayoutParams(-2, -2);
        cardTimeParams.leftMargin = dp(8);
        head.addView(stripCardTime, cardTimeParams);
        TextView fold = label("⌃", 13, INK3, true);
        fold.setGravity(Gravity.CENTER);
        fold.setOnClickListener(v -> collapseScanStrip());
        head.addView(fold, new LinearLayout.LayoutParams(dp(24), dp(24)));

        stripCardCount = label("", 12, INK2, false);
        LinearLayout.LayoutParams cardCountParams = new LinearLayout.LayoutParams(-1, -2);
        cardCountParams.topMargin = dp(5);
        stripCard.addView(stripCardCount, cardCountParams);

        FrameLayout track = new FrameLayout(this);
        track.setBackground(shape(TRACK, 2));
        stripCardTrack = track;
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(-1, dp(3));
        trackParams.topMargin = dp(9);
        stripCard.addView(track, trackParams);
        stripCardFill = new View(this);
        stripCardFill.setBackground(shape(ACCENT, 2));
        track.addView(stripCardFill, new FrameLayout.LayoutParams(0, -1));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(-1, dp(28));
        buttonsParams.topMargin = dp(9);
        stripCard.addView(buttons, buttonsParams);
        TextView pause = label("暂停", 12, INK2, true);
        pause.setGravity(Gravity.CENTER);
        pause.setBackground(outlined(9, SURFACE));
        pause.setOnClickListener(v -> pauseScanFromStrip());
        buttons.addView(pause, new LinearLayout.LayoutParams(0, -1, 1));
        TextView end = label("结束", 12, ACCENT, true);
        end.setGravity(Gravity.CENTER);
        end.setBackground(shape(ACCENT_SOFT, 9));
        end.setOnClickListener(v -> endScanFromStrip());
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(0, -1, 1);
        endParams.leftMargin = dp(8);
        buttons.addView(end, endParams);
    }

    private TextView stripClock(int size) {
        TextView view = label("00:00", size, INK, true);
        view.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        view.setIncludeFontPadding(false);
        return view;
    }

    private void updatePageInset(boolean inset) {
        if (pageLayer == null || pageLayer.getChildCount() == 0 || stage == null) return;
        int stageHeight = stage.getHeight();
        if (stageHeight <= 0) return;
        View child = pageLayer.getChildAt(0);
        if (!(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) child.getLayoutParams();
        int shift = inset ? dp(38) : 0;
        int target = stageHeight - shift;
        if (params.height != target) {
            params.height = target;
            child.setLayoutParams(params);
        }
        if (child.getTranslationY() != shift) child.setTranslationY(shift);
    }

    private void updateScanStrip() {
        if (stripHost == null) return;
        ScanRecord record = currentRecord;
        long now = android.os.SystemClock.elapsedRealtime();
        if (record == null) {
            stripHost.setVisibility(View.GONE);
            stripExpanded = false;
            stripPaused = false;
            stripRecordId = null;
            stripAccum = 0;
            stripWasRunning = false;
            updatePageInset(false);
            return;
        }
        if (!record.id.equals(stripRecordId)) {
            stripRecordId = record.id;
            stripAccum = 0;
            stripResumeAt = now;
            stripWasRunning = scanning;
            stripExpanded = false;
            stripPaused = false;
            stripLastClock = null;
            stripLastCount = null;
            stripLastPaused = null;
            stripLastCard = null;
        }
        if (scanning && !stripWasRunning) {
            stripResumeAt = now;
            stripWasRunning = true;
        } else if (!scanning && stripWasRunning) {
            stripAccum += now - stripResumeAt;
            stripResumeAt = now;
            stripWasRunning = false;
        }
        if (!(scanning || stripPaused) || showingReader) {
            stripHost.setVisibility(View.GONE);
            stripExpanded = false;
            updatePageInset(false);
            return;
        }
        stripHost.setVisibility(View.VISIBLE);
        updatePageInset(pageLayer.getVisibility() == View.VISIBLE);
        long total = stripAccum + (scanning ? now - stripResumeAt : 0);
        String clock = String.format(Locale.US, "%02d:%02d", total / 60000, (total / 1000) % 60);
        if (!clock.equals(stripLastClock)) {
            stripLastClock = clock;
            stripTime.setText(clock);
            stripCardTime.setText(clock);
        }
        String countText = "已读 " + record.scanned + " 条";
        if (!countText.equals(stripLastCount)) {
            stripLastCount = countText;
            stripCount.setText(countText);
        }
        String pausedText = "已暂停 · 已读 " + record.scanned + " 条";
        if (!pausedText.equals(stripLastPaused)) {
            stripLastPaused = pausedText;
            stripPausedText.setText(pausedText);
        }
        String cardCountText = scanCounts(record);
        if (!cardCountText.equals(stripLastCard)) {
            stripLastCard = cardCountText;
            stripCardCount.setText(cardCountText);
        }
        if (scanning) stripDot.setAlpha(0.45f + 0.55f * (float) (0.5 + 0.5 * Math.sin(now / 320.0)));
        else if (stripDot.getAlpha() != 1f) stripDot.setAlpha(1f);
        boolean determinate = record.mainDone && record.includeReplies && !record.replyParents.isEmpty();
        int trackWidth = stripCardTrack.getWidth();
        if (trackWidth > 0) {
            int fill;
            if (determinate) {
                float part = (float) record.replyIndex / record.replyParents.size();
                stripCardFill.setTranslationX(0);
                fill = Math.max(dp(4), (int) (trackWidth * part));
            } else {
                stripPhase += 0.06f;
                if (stripPhase > 1f) stripPhase -= 1f;
                fill = Math.max(dp(44), trackWidth / 4);
                stripCardFill.setTranslationX((trackWidth - fill) * stripPhase);
            }
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) stripCardFill.getLayoutParams();
            if (params.width != fill) {
                params.width = fill;
                stripCardFill.setLayoutParams(params);
            }
        }
        if (stripPaused) {
            stripPausedBar.setVisibility(View.VISIBLE);
            stripBar.setVisibility(View.GONE);
            stripCard.setVisibility(View.GONE);
        } else if (stripExpanded) {
            stripPausedBar.setVisibility(View.GONE);
            stripBar.setVisibility(View.GONE);
            stripCard.setVisibility(View.VISIBLE);
        } else {
            stripPausedBar.setVisibility(View.GONE);
            stripBar.setVisibility(View.VISIBLE);
            stripCard.setVisibility(View.GONE);
        }
    }

    private void expandScanStrip() {
        if (currentRecord == null) return;
        stripExpanded = true;
        updateScanStrip();
        scheduleStripCollapse();
    }

    private void collapseScanStrip() {
        stripExpanded = false;
        if (stripCollapse != null) stripHandler.removeCallbacks(stripCollapse);
        updateScanStrip();
    }

    private void scheduleStripCollapse() {
        if (stripCollapse != null) stripHandler.removeCallbacks(stripCollapse);
        stripCollapse = () -> {
            stripExpanded = false;
            updateScanStrip();
        };
        stripHandler.postDelayed(stripCollapse, 4000);
    }

    private void pauseScanFromStrip() {
        ScanRecord record = currentRecord;
        if (!scanning || record == null) return;
        scanning = false;
        requestToken++;
        stripPaused = true;
        stripExpanded = false;
        if (stripCollapse != null) stripHandler.removeCallbacks(stripCollapse);
        if (scanButton != null) scanButton.setText("继续扫描");
        if (status != null) status.setText("扫描已暂停，已提取素材保留在暂存记录中");
        recordScanPause("已手动暂停扫描", false);
        updateScanCard();
        updateScanStrip();
    }

    private void resumeScanFromStrip() {
        ScanRecord record = currentRecord;
        if (record == null || scanning || record.complete) return;
        stripPaused = false;
        stripExpanded = false;
        if (isXhsLink(record.link)) {
            toggleXhsScan();
        } else {
            scanning = true;
            requestWaits = 0;
            pageRetries = 0;
            if (scanButton != null) scanButton.setText("暂停扫描");
            if (status != null) status.setText("正在继续扫描…");
            startPage();
        }
        updateScanStrip();
    }

    private void endScanFromStrip() {
        ScanRecord record = currentRecord;
        scanning = false;
        requestToken++;
        stripPaused = false;
        stripExpanded = false;
        if (stripCollapse != null) stripHandler.removeCallbacks(stripCollapse);
        if (record != null) {
            recordScanPause("已手动结束扫描", false);
            final ScanRecord done = record;
            worker.execute(() -> persistRecord(done));
            if (scanButton != null) scanButton.setText(done.complete ? "扫描完成" : "继续扫描");
            if (status != null) status.setText(done.complete ? "扫描完成" : "扫描已结束，已提取的素材保留在暂存记录中");
        }
        updateScanCard();
        updateScanStrip();
        showBottomNotice(record != null && record.complete ? "扫描完成" : "已结束扫描，素材已保留");
    }

    private void showHome() {
        stopScanForNavigation();
        resumedFromHome = false;
        resetPages();
        LinearLayout root = pageRoot();
        LinearLayout top = header(root, "咻咿", false, null);
        top.getLayoutParams().height = dp(68);
        TextView brand = (TextView) top.getChildAt(0);
        brand.setTextSize(26);
        brand.setTypeface(homeTypeface(800));
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(20));
        scroll.addView(content);

        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(shape(darkMode ? ACCENT_SOFT : 0xFFEDF2FF, 16));
        hero.setClipToOutline(true);
        content.addView(hero, new LinearLayout.LayoutParams(-1, dp(176)));
        if (homeRabbit == null)
            homeRabbit = android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.home_rabbit);
        hero.addView(new BlueHeroView(this, darkMode, homeRabbit), new FrameLayout.LayoutParams(-1, -1));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(18), dp(23), 0, 0);
        hero.addView(words, new FrameLayout.LayoutParams(-1, -1));
        TextView title = label("Xiuyi\nextraction", 35, ACCENT, true);
        if (homeTitleFont == null)
            homeTitleFont = new Typeface.Builder(getAssets(), "fonts/Manrope.ttf")
                    .setFontVariationSettings("'wght' 800").setWeight(800).build();
        title.setTypeface(homeTitleFont);
        title.setLetterSpacing(-0.037f);
        title.setIncludeFontPadding(false);
        title.setMaxLines(2);
        title.setAutoSizeTextTypeUniformWithConfiguration(24, 35, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        int titleWidth = Math.min(dp(222), (int) ((getResources().getDisplayMetrics().widthPixels - dp(32)) * 0.65f) - dp(18));
        words.addView(title, new LinearLayout.LayoutParams(titleWidth, dp(88)));
        words.addView(homeLabel("发现评论里的好表情", 14, INK, 650));
        if (decodeEnabled && !titleDecoded) {
            titleDecoded = true;
            android.animation.ValueAnimator decode = android.animation.ValueAnimator.ofFloat(0, 1);
            String finalText = "Xiuyi\nextraction";
            String glyphs = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#@$%";
            decode.setDuration(800);
            decode.addUpdateListener(value -> {
                if (!title.isAttachedToWindow()) return;
                int settled = (int) ((float) value.getAnimatedValue() * finalText.length());
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < finalText.length(); i++) {
                    char actual = finalText.charAt(i);
                    text.append(i < settled || actual == '\n' ? actual : glyphs.charAt((i * 7 + (int) value.getCurrentPlayTime() / 35) % glyphs.length()));
                }
                title.setText(text);
            });
            decode.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) { title.setText(finalText); }
            });
            title.post(decode::start);
        }
        addHomeFeature(content, false);
        addHomeFeature(content, true);
        ScanRecord latest = null;
        for (ScanRecord record : records) {
            if (record != null && extractLink(record.link, isXhsLink(record.link)) != null) {
                latest = record;
                break;
            }
        }
        if (latest != null) {
            ScanRecord record = latest;
            boolean resumable = canResume(record);
            addHomeResume(content, resumable ? "继续上次扫描" : "查看上次扫描",
                    resumable ? "已读 " + record.scanned + " 条评论"
                            : "已完成 · " + record.items.size() + " 个素材", v -> {
                        recordsFromSettings = false;
                        boolean live = isLiveRecord(record);
                        showRecord(record);
                        resumedFromHome = true;
                        if (resumable && !live) openSavedRecordPage(record);
                    });
        }
        showMainPage(root, 1);
    }

    private Typeface homeTypeface(int weight) {
        Typeface font = homeFonts.get(weight);
        if (font == null) {
            font = new Typeface.Builder(getAssets(), "fonts/NotoSansSC.ttf")
                    .setFontVariationSettings("'wght' " + weight).setWeight(weight).build();
            homeFonts.put(weight, font);
        }
        return font;
    }

    private TextView homeLabel(String text, int size, int color, int weight) {
        TextView view = label(text, size, color, false);
        view.setTypeface(homeTypeface(weight));
        view.setIncludeFontPadding(false);
        view.setPadding(0, dp(2), 0, dp(2));
        return view;
    }

    private void addHomeResume(LinearLayout parent, String name, String detail, View.OnClickListener action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(10), dp(16), dp(10));
        row.setBackground(card(15));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(72));
        rp.topMargin = dp(20);
        parent.addView(row, rp);
        row.addView(icon(R.drawable.home_resume, ACCENT), new LinearLayout.LayoutParams(dp(28), dp(28)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -2, 1);
        wp.leftMargin = dp(16);
        row.addView(words, wp);
        words.addView(homeLabel(name, 17, INK, 700));
        words.addView(homeLabel(detail, 13, INK2, 400));
        row.addView(icon(R.drawable.home_chevron, INK2), new LinearLayout.LayoutParams(dp(20), dp(20)));
        row.setOnClickListener(action);
    }

    private void addHomeFeature(LinearLayout content, boolean xhs) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(card(15));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(86));
        params.topMargin = dp(xhs ? 12 : 20);
        content.addView(row, params);
        ImageView image = new ImageView(this);
        image.setImageResource(xhs ? R.drawable.home_xiaohongshu : R.drawable.home_douyin);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackground(shape(SURFACE, 13));
        image.setClipToOutline(true);
        row.addView(image, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -2, 1);
        wp.leftMargin = dp(16);
        row.addView(words, wp);
        words.addView(homeLabel(xhs ? "小红书提取" : "抖音提取", 20, INK, 750));
        words.addView(homeLabel(xhs ? "图片 · 回复 · 实况" : "图片 · 动图 · 回复", 14, INK2, 400));
        row.addView(icon(R.drawable.home_arrow, ACCENT), new LinearLayout.LayoutParams(dp(26), dp(26)));
        row.setOnClickListener(v -> selectMode(xhs));
    }

    private void addMenuCard(LinearLayout parent, int resource, String name, String detail, View.OnClickListener action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setBackground(card(16));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(12);
        parent.addView(row, rp);
        row.addView(icon(resource, INK), new LinearLayout.LayoutParams(dp(27), dp(27)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -2, 1);
        wp.leftMargin = dp(14);
        row.addView(words, wp);
        words.addView(label(name, 16, INK, true));
        if (!detail.isEmpty()) words.addView(label(detail, 12, INK2, false));
        row.addView(label("›", 23, INK3, false));
        row.setOnClickListener(action);
    }

    private void showMy() {
        stopScanForNavigation();
        resetPages();
        showingMy = true;
        LinearLayout root = pageRoot();
        header(root, "我的", false, null);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(12), dp(18), dp(20));
        scroll.addView(page);
        page.addView(label("管理本机素材与记录", 13, INK2, false));
        LinearLayout stats = new LinearLayout(this);
        stats.setBackground(card(16));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(88));
        sp.topMargin = dp(18);
        page.addView(stats, sp);
        TextView favoriteCount = label("收藏素材\n…", 15, ACCENT, true);
        favoriteCount.setGravity(Gravity.CENTER);
        stats.addView(favoriteCount, new LinearLayout.LayoutParams(0, -1, 1));
        TextView recordCount = label("扫描记录\n" + records.size(), 15, ACCENT, true);
        recordCount.setGravity(Gravity.CENTER);
        stats.addView(recordCount, new LinearLayout.LayoutParams(0, -1, 1));
        worker.execute(() -> {
            File[] files = new File(getFilesDir(), "favorites").listFiles();
            int count = 0;
            if (files != null) for (File file : files) if (file.isFile() && !file.getName().endsWith(".tmp")) count++;
            int total = count;
            runOnUiThread(() -> favoriteCount.setText("收藏素材\n" + total));
        });
        favoriteCount.setOnClickListener(v -> openFavorites(this::showMy));
        recordCount.setOnClickListener(v -> openRecordsFromMy());
        addMenuCard(page, R.drawable.blue_history, "扫描记录", "按平台查看与继续扫描", v -> openRecordsFromMy());
        addMenuCard(page, R.drawable.extract_download, "导出扫描记录", "备份两平台记录、进度及素材原文件", v -> pickRecordArchive(true));
        addMenuCard(page, R.drawable.record_import, "导入扫描记录", "从 ZIP 备份恢复，重复记录自动跳过", v -> pickRecordArchive(false));
        addMenuCard(page, R.drawable.blue_info, "关于咻咿", "版本信息与使用说明", v -> showAbout());
        showMainPage(root, 2);
    }

    private void showAbout() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = dialogPanel(0, false, "咻咿提取", "从评论中发现、预览和保存素材。");
        ImageView brand = new ImageView(this);
        brand.setImageResource(R.mipmap.ic_launcher);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(52), dp(52));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.bottomMargin = dp(13);
        panel.addView(brand, 0, logoParams);
        ((TextView) panel.getChildAt(1)).setGravity(Gravity.CENTER);
        ((TextView) panel.getChildAt(2)).setGravity(Gravity.CENTER);
        TextView version = homeLabel("版本 " + versionName(), 12, INK2, 450);
        version.setGravity(Gravity.CENTER);
        panel.addView(version, 2);
        dialogNote(panel, "登录与验证\n在抖音或小红书网页内完成");
        dialogNote(panel, "素材保存\n先暂存到本机，保存后写入下载目录");
        dialogActions(dialog, panel, null, "知道了", false, null, null);
        showCardDialog(dialog, panel);
    }

    private void pickRecordArchive(boolean exporting) {
        if (recordTransferBusy) return;
        if (exporting && records.isEmpty()) {
            showTopNotice("暂无可导出的扫描记录");
            return;
        }
        if (exporting && (scanning || (currentRecord != null && currentRecord.processingPage))) {
            showTopNotice("请先暂停扫描，等待当前页处理完成后再导出");
            return;
        }
        Intent intent = new Intent(exporting ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(exporting ? "application/zip" : "*/*");
        if (exporting) intent.putExtra(Intent.EXTRA_TITLE, "咻咿扫描记录-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new java.util.Date()) + ".zip");
        try { startActivityForResult(intent, exporting ? EXPORT_RECORDS : IMPORT_RECORDS); }
        catch (android.content.ActivityNotFoundException error) {
            showTopNotice("当前设备没有可用的系统文件选择器");
        }
    }

    private java.util.List<String> recordArchiveFiles(File folder) throws Exception {
        File manifest = new File(folder, "manifest.json");
        if (!manifest.isFile() || manifest.length() > 4 * 1024 * 1024) throw new java.io.IOException("记录清单缺失或过大");
        JSONObject data = new JSONObject(new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8));
        data.getString("link");
        if (data.getLong("createdAt") < 0) throw new java.io.IOException("记录时间无效");
        JSONArray media = data.getJSONArray("media");
        ArrayList<String> files = new ArrayList<>();
        files.add("manifest.json");
        for (int i = 0; i < media.length(); i++) {
            JSONObject item = media.getJSONObject(i);
            String name = item.getString("file"), mime = item.getString("mime");
            if (!RecordArchive.leaf(name) || name.equals("manifest.json")
                    || !(mime.startsWith("image/") || mime.startsWith("video/")))
                throw new java.io.IOException("记录中的素材信息无效");
            File file = new File(folder, name);
            if (!file.isFile() || file.length() == 0) throw new java.io.IOException("记录素材缺失：" + name);
            files.add(name);
        }
        return files;
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != EXPORT_RECORDS && request != IMPORT_RECORDS) return;
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (recordTransferBusy) return;
        boolean exporting = request == EXPORT_RECORDS;
        if (exporting && (scanning || (currentRecord != null && currentRecord.processingPage))) {
            showTopNotice("扫描仍在进行，请暂停后重新导出");
            return;
        }
        Uri uri = data.getData();
        ArrayList<ScanRecord> snapshot = new ArrayList<>(records);
        HashSet<String> known = new HashSet<>();
        for (ScanRecord record : snapshot) known.add(record.id);
        recordTransferBusy = true;
        android.app.Dialog progress = new android.app.Dialog(this);
        progress.setCancelable(false);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(24), dp(24), dp(24), dp(24));
        panel.setBackground(card(24));
        ProgressBar spinner = new ProgressBar(this);
        spinner.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        panel.addView(spinner, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView progressTitle = homeLabel(exporting ? "正在导出备份…" : "正在导入备份…", 18, INK, 750);
        progressTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, -2);
        progressParams.topMargin = dp(16);
        panel.addView(progressTitle, progressParams);
        TextView progressDetail = homeLabel(exporting ? "正在打包扫描记录与素材原文件。\n文件较多时，请稍候。" : "正在检查备份并恢复记录与素材。", 13, INK2, 400);
        progressDetail.setGravity(Gravity.CENTER);
        progressDetail.setPadding(0, dp(8), 0, 0);
        panel.addView(progressDetail);
        showCardDialog(progress, panel);
        worker.execute(() -> {
            String message;
            boolean succeeded = false;
            ArrayList<ScanRecord> imported = new ArrayList<>();
            try {
                if (exporting) {
                    java.util.LinkedHashMap<String, File> folders = new java.util.LinkedHashMap<>();
                    for (ScanRecord record : snapshot) {
                        if (!persistRecord(record)) throw new java.io.IOException("记录暂存失败，请重试");
                        recordArchiveFiles(record.folder);
                        folders.put(record.id, record.folder);
                    }
                    try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                        if (output == null) throw new java.io.IOException("无法写入所选位置");
                        RecordArchive.write(output, folders, this::recordArchiveFiles);
                    }
                    message = "导出成功：" + snapshot.size() + " 条记录，包含素材原文件";
                } else {
                    RecordArchive.Result restored;
                    try (InputStream input = getContentResolver().openInputStream(uri)) {
                        if (input == null) throw new java.io.IOException("无法读取所选备份");
                        restored = RecordArchive.read(input, new File(getCacheDir(), "record-import-" + UUID.randomUUID()),
                                recordsFolder(), known, this::recordArchiveFiles);
                    }
                    imported = readRecords(restored.added);
                    message = "导入成功：" + imported.size() + " 条；跳过重复记录 " + restored.skipped + " 条";
                }
                succeeded = true;
            } catch (Exception error) {
                if (exporting) {
                    try { android.provider.DocumentsContract.deleteDocument(getContentResolver(), uri); }
                    catch (Exception ignored) { }
                }
                message = archiveFailureReason(error, exporting);
            }
            String report = message;
            boolean success = succeeded;
            ArrayList<ScanRecord> added = imported;
            runOnUiThread(() -> {
                recordTransferBusy = false;
                if (isFinishing() || isDestroyed()) return;
                progress.dismiss();
                records.addAll(added);
                records.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                if (showingMy) showMy();
                if (success) {
                    if (exporting) showBottomNotice(report);
                    else showBottomNotice(report, "查看记录", this::openRecordsFromMy);
                } else showConfirmation(R.drawable.dialog_file, false, exporting ? "备份未能导出" : "无法导入这个备份",
                        report, "取消", "重新选择", null, () -> pickRecordArchive(exporting),
                        exporting ? "扫描记录和素材仍保留在本机" : "本机现有记录未被覆盖");
            });
        });
    }

    private void openRecordsFromMy() {
        recordsOrigin = 2;
        recordsFromSettings = true;
        showRecordPlatforms();
    }

    private void returnToRecordsOrigin() {
        if (recordsOrigin == 2) showMy();
        else showSettings();
    }

    private void returnFromSettings() {
        returningTransition = true;
        switch (settingsOrigin) {
            case 0: showFavorites(); break;
            case 2: showMy(); break;
            case -1: showReader(); break;
            case -2: showRecords(); break;
            case -3: showRecordPlatforms(); break;
            default: showHome(); break;
        }
    }

    private void showSettings() {
        resetPages();
        showingSettings = true;
        currentPage = -4;
        LinearLayout root = pageRoot();
        LinearLayout heading = header(root, "设置", true, this::returnFromSettings);
        TextView headingText = (TextView) heading.getChildAt(1);
        headingText.setTypeface(homeTypeface(750));
        headingText.setTextSize(20);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(8), dp(18), dp(24));
        scroll.addView(page);
        page.addView(settingsGroupTitle("外观", 8));
        LinearLayout modes = new LinearLayout(this);
        modes.setPadding(dp(3), dp(3), dp(3), dp(3));
        modes.setBackground(shape(TRACK, 12));
        page.addView(modes, new LinearLayout.LayoutParams(-1, dp(44)));
        String selected = getPreferences(MODE_PRIVATE).getString("appearance", "system");
        String[] names = {"浅色", "深色", "跟随系统"};
        String[] keys = {"light", "dark", "system"};
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            TextView button = homeLabel(names[i], 13, selected.equals(key) ? Color.WHITE : INK2, 650);
            button.setGravity(Gravity.CENTER);
            if (selected.equals(key)) button.setBackground(shape(ACCENT, 10));
            modes.addView(button, new LinearLayout.LayoutParams(0, -1, 1));
            button.setOnClickListener(v -> changeAppearance(key, v));
        }
        page.addView(settingsGroupTitle("扫描偏好", 22));
        LinearLayout scanningCards = new LinearLayout(this);
        page.addView(scanningCards, new LinearLayout.LayoutParams(-1, -2));
        addToggleCard(scanningCards, R.drawable.settings_reply, "读取回复", "同时读取抖音作品的\n评论与回复", "", douyinReadReplies, enabled -> {
            douyinReadReplies = enabled;
            getPreferences(MODE_PRIVATE).edit().putBoolean("douyin_read_replies", enabled).apply();
            if (currentRecord == null) configureReaderMode();
        });
        addToggleCard(scanningCards, R.drawable.settings_fast, "快速扫描", "缩短抖音评论\n翻页等待", "可能频繁暂停", douyinPageDelayMs == 350, enabled -> {
            douyinPageDelayMs = enabled ? 350 : 2000;
            getPreferences(MODE_PRIVATE).edit().putInt("douyin_page_delay_ms", douyinPageDelayMs).apply();
        });
        page.addView(settingsGroupTitle("动效", 22));
        LinearLayout motionCards = new LinearLayout(this);
        page.addView(motionCards, new LinearLayout.LayoutParams(-1, -2));
        addToggleCard(motionCards, R.drawable.settings_decode, "标题解码", "进入首页时播放\n标题逐字解码", "", decodeEnabled, enabled -> {
            decodeEnabled = enabled;
            if (enabled) titleDecoded = false;
            getPreferences(MODE_PRIVATE).edit().putBoolean("title_decode", enabled).apply();
        });
        addToggleCard(motionCards, R.drawable.settings_motion, "页面动效", "页面切换与操作\n使用平滑过渡", "", motionEnabled, enabled -> {
            motionEnabled = enabled;
            getPreferences(MODE_PRIVATE).edit().putBoolean("page_motion", enabled).apply();
        });
        page.addView(settingsGroupTitle("存储", 22));
        LinearLayout storage = new LinearLayout(this);
        storage.setOrientation(LinearLayout.VERTICAL);
        storage.setBackground(card(16));
        page.addView(storage, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout usage = new LinearLayout(this);
        usage.setGravity(Gravity.CENTER_VERTICAL);
        usage.setPadding(dp(14), dp(14), dp(14), dp(14));
        storage.addView(usage, new LinearLayout.LayoutParams(-1, -2));
        usage.addView(icon(R.drawable.blue_storage, ACCENT), new LinearLayout.LayoutParams(dp(22), dp(22)));
        LinearLayout usageWords = new LinearLayout(this);
        usageWords.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams usageParams = new LinearLayout.LayoutParams(0, -2, 1);
        usageParams.leftMargin = dp(12);
        usage.addView(usageWords, usageParams);
        usageWords.addView(homeLabel("本机存储", 14, INK, 700));
        usageWords.addView(homeLabel("暂存记录与收藏", 12, INK2, 400));
        TextView storageValue = homeLabel("计算中…", 14, ACCENT, 700);
        usage.addView(storageValue, new LinearLayout.LayoutParams(-2, -2));
        storage.addView(rowDivider());

        LinearLayout location = new LinearLayout(this);
        location.setGravity(Gravity.CENTER_VERTICAL);
        location.setPadding(dp(14), dp(14), dp(14), dp(14));
        storage.addView(location, new LinearLayout.LayoutParams(-1, -2));
        location.addView(icon(R.drawable.blue_folder, ACCENT), new LinearLayout.LayoutParams(dp(22), dp(22)));
        LinearLayout locationWords = new LinearLayout(this);
        locationWords.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(0, -2, 1);
        locationParams.leftMargin = dp(12);
        location.addView(locationWords, locationParams);
        locationWords.addView(homeLabel("保存位置", 14, INK, 700));
        locationWords.addView(homeLabel("下载 / DouYinGetting", 12, INK2, 400));
        TextView openFolder = homeLabel("打开文件夹", 12, ACCENT, 700);
        openFolder.setGravity(Gravity.CENTER);
        openFolder.setSingleLine(true);
        openFolder.setPadding(dp(12), 0, dp(12), 0);
        openFolder.setBackground(shape(ACCENT_SOFT, 10));
        openFolder.setOnClickListener(v -> openSavedFolder());
        pressFeedback(openFolder);
        LinearLayout.LayoutParams openFolderParams = new LinearLayout.LayoutParams(-2, dp(40));
        openFolderParams.leftMargin = dp(8);
        location.addView(openFolder, openFolderParams);
        ArrayList<ScanRecord> storageRecords = new ArrayList<>(records);
        worker.execute(() -> {
            String size = storageSize(storageRecords);
            runOnUiThread(() -> storageValue.setText(size));
        });
        presentSecondary(root);
    }

    private void openSavedFolder() {
        Uri folder = android.provider.DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents", "primary:Download/DouYinGetting");
        Intent browse = new Intent(Intent.ACTION_VIEW).setDataAndType(folder,
                android.provider.DocumentsContract.Document.MIME_TYPE_DIR);
        try {
            startActivity(browse);
        } catch (android.content.ActivityNotFoundException | SecurityException error) {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("*/*");
            picker.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, folder);
            try {
                startActivity(picker);
                showBottomNotice("已打开系统文件选择器，可浏览保存文件夹");
            } catch (android.content.ActivityNotFoundException | SecurityException unavailable) {
                showBottomNotice("无法打开文件管理器，请手动前往下载 / DouYinGetting");
            }
        }
    }

    private void changeAppearance(String key, View control) {
        if (themeAnimation != null) return;
        String selected = getPreferences(MODE_PRIVATE).getString("appearance", "system");
        if (selected.equals(key)) return;
        boolean nextDark = "dark".equals(key) || ("system".equals(key)
                && (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        FrameLayout content = stage;
        int width = content.getWidth(), height = content.getHeight();
        boolean animate = motionEnabled && darkMode != nextDark && width > 0 && height > 0;
        Bitmap before = null;
        int[] center = new int[2];
        if (animate) {
            control.getLocationInWindow(center);
            int[] position = new int[2];
            content.getLocationInWindow(position);
            center[0] += control.getWidth() / 2 - position[0];
            center[1] += control.getHeight() / 2 - position[1];
            before = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            content.draw(new android.graphics.Canvas(before));
        }
        getPreferences(MODE_PRIVATE).edit().putString("appearance", key).apply();
        int[] old = palette();
        applyPalette();
        setTheme(darkMode ? R.style.BlueDark : R.style.BlueLight);
        retheme(readerRoot, old, palette());
        retheme(pageLayer, old, palette());
        if (stripHost != null) retheme(stripHost, old, palette());
        content.setBackgroundColor(BG);
        mainShell = null;
        LinearLayout modes = (LinearLayout) control.getParent();
        String[] keys = {"light", "dark", "system"};
        for (int i = 0; i < keys.length; i++) {
            TextView button = (TextView) modes.getChildAt(i);
            boolean active = keys[i].equals(key);
            button.setTextColor(active ? Color.WHITE : INK2);
            button.setBackground(active ? shape(ACCENT, 10) : null);
        }
        if (!animate) {
            updateSystemBars();
            return;
        }
        ThemeRevealView cover = new ThemeRevealView(this, before, center[0], center[1], nextDark);
        cover.layout(0, 0, width, height);
        content.getOverlay().add(cover);
        float radius = (float) Math.hypot(Math.max(center[0], width - center[0]),
                Math.max(center[1], height - center[1])) + 1;
        android.animation.ValueAnimator reveal = android.animation.ValueAnimator.ofFloat(
                nextDark ? 0 : radius, nextDark ? radius : 0);
        themeAnimation = reveal;
        reveal.setDuration(500);
        reveal.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        reveal.addUpdateListener(value -> cover.setRadius((float) value.getAnimatedValue()));
        reveal.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                content.getOverlay().remove(cover);
                themeAnimation = null;
                updateSystemBars();
            }
        });
        content.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                content.getViewTreeObserver().removeOnPreDrawListener(this);
                if (themeAnimation != reveal) return true;
                Bitmap after = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                cover.setVisibility(View.INVISIBLE);
                content.draw(new android.graphics.Canvas(after));
                cover.setVisibility(View.VISIBLE);
                cover.setAfter(after);
                cover.setRadius(nextDark ? 0 : radius);
                reveal.start();
                return true;
            }
        });
    }

    private TextView settingsGroupTitle(String text, int topMargin) {
        TextView view = homeLabel(text, 14, INK, 750);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(topMargin);
        params.bottomMargin = dp(10);
        view.setLayoutParams(params);
        return view;
    }

    private void addToggleCard(LinearLayout row, int resource, String title, String detail, String warning, boolean enabled,
                               java.util.function.Consumer<Boolean> changed) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        card.setMinimumHeight(dp(174));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, 1);
        if (row.getChildCount() > 0) cp.leftMargin = dp(12);
        row.addView(card, cp);
        FrameLayout head = new FrameLayout(this);
        card.addView(head, new LinearLayout.LayoutParams(-1, dp(34)));
        ImageView image = icon(resource, enabled ? Color.WHITE : ACCENT);
        head.addView(image, new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.START | Gravity.CENTER_VERTICAL));
        Switch toggle = new Switch(this);
        toggle.setChecked(enabled);
        toggle.setClickable(false);
        toggle.setEnabled(false);
        toggle.setFocusable(false);
        toggle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        toggle.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        head.addView(toggle, new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.CENTER_VERTICAL));
        TextView name = homeLabel(title, 16, enabled ? Color.WHITE : INK, 750);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(12);
        card.addView(name, np);
        TextView description = homeLabel(detail, 12, enabled ? Color.WHITE : INK2, 400);
        description.setLineSpacing(dp(2), 1f);
        description.setMinLines(2);
        card.addView(description);
        TextView caution = homeLabel(warning, 11, 0xFFFF8A22, 600);
        if (!warning.isEmpty()) {
            LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2);
            wp.topMargin = dp(8);
            card.addView(caution, wp);
        }
        boolean[] state = {enabled};
        Runnable paint = () -> {
            card.setBackground(outlined(16, state[0] ? ACCENT : SURFACE));
            name.setTextColor(state[0] ? Color.WHITE : INK);
            description.setTextColor(state[0] ? Color.WHITE : INK2);
            image.setImageTintList(ColorStateList.valueOf(state[0] ? Color.WHITE : ACCENT));
            caution.setTextColor(state[0] ? 0xFFFFD080 : darkMode ? 0xFFFFB571 : 0xFFE46A10);
            toggle.setChecked(state[0]);
            toggle.setTrackTintList(ColorStateList.valueOf(state[0] ? Color.argb(100, 255, 255, 255) : INK3));
            card.setSelected(state[0]);
            card.setContentDescription(title + (state[0] ? "，已开启" : "，已关闭"));
        };
        paint.run();
        card.setOnClickListener(v -> {
            state[0] = !state[0];
            changed.accept(state[0]);
            paint.run();
            if (motionEnabled) {
                card.setScaleX(0.97f);
                card.setScaleY(0.97f);
                card.animate().scaleX(1).scaleY(1).setDuration(150).start();
            }
        });
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

    private String storageSize(ArrayList<ScanRecord> snapshot) {
        long bytes = 0;
        for (ScanRecord record : snapshot) {
            File[] files = record.folder.listFiles();
            if (files == null) continue;
            for (File file : files) if (file.isFile()) bytes += file.length();
        }
        File[] favorites = new File(getFilesDir(), "favorites").listFiles();
        if (favorites != null) for (File file : favorites) if (file.isFile() && !file.getName().endsWith(".tmp")) bytes += file.length();
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

    private void selectMode(boolean xhs) {
        scanning = false;
        requestToken++;
        xhsMode = xhs;
        currentRecord = null;
        viewingSavedRecord = false;
        resumedFromHome = false;
        activeLink = null;
        input.setText("");
        clearMediaSelection();
        media.clear();
        for (Animatable animation : animations) animation.stop();
        animations.clear();
        selectedFilter = "all";
        positionReaderFilter(false);
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
        readerHeading.setText(xhsMode ? "小红书提取" : "抖音提取");
        input.setHint(xhsMode ? "粘贴小红书笔记链接或分享文案" : "粘贴抖音作品链接或分享文案");
        browserHint.setText(xhsMode ? "在网页中登录并打开笔记评论，扫描时会读取评论素材" : douyinReadReplies
                ? "在网页中登录并打开评论区，然后扫描主评论与回复" : "在网页中登录并打开评论区，然后扫描主评论");
        codeLogin.setVisibility(xhsMode ? View.GONE : View.VISIBLE);
        scanButton.setText(xhsMode || douyinReadReplies ? "扫描评论" : "扫描主评论");
        status.setText(xhsMode ? "粘贴笔记链接，打开网页后进入评论区" : "粘贴作品链接，打开网页后进入评论区");
    }

    private void showRecordPlatforms() {
        currentPage = -3;
        showingMy = false;
        showingReader = false;
        showingRecords = false;
        showingSettings = false;
        showingRecordPlatforms = true;
        showingFavorites = false;
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
        back.setOnClickListener(v -> returnToRecordsOrigin());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView heading = label("选择平台", 16, INK, true);
        heading.setGravity(Gravity.CENTER);
        toolbar.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));

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
        presentSecondary(root);
    }

    private void showRecords() {
        currentPage = -2;
        showingSettings = false;
        showingMy = false;
        showingReader = false;
        showingRecords = true;
        showingFavorites = false;
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
        presentSecondary(root);
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
            icon.setBackground(shape(ACCENT_SOFT, 11));
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
                    boolean live = isLiveRecord(record);
                    showRecord(record);
                    if (!live) openSavedRecordPage(record);
                });
            }
            if (record.link.startsWith("http")) {
                TextView copy = label("复制链接", 12, INK2, true);
                LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(-2, -2);
                copyParams.leftMargin = dp(12);
                copy.setOnClickListener(v -> {
                    if (copyLink(record.link)) flashCopied(copy, "复制链接");
                    else showTopNotice("复制失败：剪贴板不可用");
                });
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
        boolean live = isLiveRecord(record);
        resumedFromHome = false;
        xhsMode = isXhsLink(record.link);
        configureReaderMode();
        currentRecord = record;
        viewingSavedRecord = !live;
        savedRecordPageOpened = live;
        loadingSavedRecordPage = false;
        activeLink = record.link.startsWith("http") ? record.link : null;
        input.setText(activeLink == null ? "" : activeLink);
        if (!live) {
            cursor = record.cursor;
            scanned = xhsMode ? record.scanned : record.mainScanned > 0 ? record.mainScanned : record.scanned;
            clearMediaSelection();
            media.clear();
            synchronized (record) { media.addAll(record.items); }
            selectedFilter = "all";
        positionReaderFilter(false);
            browserPanel.setVisibility(View.GONE);
        }
        updateWebToggle();
        if (live) {
            scanButton.setText(scanning ? "暂停扫描" : "继续扫描");
            status.setText(scanning ? "正在扫描，离开本页不会中断" : "扫描已暂停，已提取素材保留在暂存记录中");
        } else {
            scanButton.setText(canResume(record) ? "继续扫描" : "重新扫描");
            status.setText(canResume(record) ? "已打开本机暂存记录，可继续扫描"
                    : "已打开本机暂存记录；重新扫描会生成新记录");
        }
        showReader();
        renderResults();
        updateResultLabels();
        readerScroll.post(() -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()));
    }

    private boolean canResume(ScanRecord record) {
        return record != null && !record.complete && extractLink(record.link, isXhsLink(record.link)) != null;
    }

    private boolean isLiveRecord(ScanRecord record) {
        return record != null && currentRecord != null && record.id.equals(currentRecord.id)
                && (scanning || stripPaused);
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

    private LinearLayout dialogPanel(int iconRes, boolean destructive, String title, String message) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(22), dp(22), dp(22));
        panel.setBackground(card(24));
        if (iconRes != 0) {
            ImageView badge = icon(iconRes, destructive ? Color.parseColor(darkMode ? "#FF8290" : "#D94855") : ACCENT);
            badge.setPadding(dp(9), dp(9), dp(9), dp(9));
            badge.setBackground(shape(destructive ? Color.parseColor(darkMode ? "#422B3B" : "#FFF0F1") : ACCENT_SOFT, 14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
            params.bottomMargin = dp(13);
            panel.addView(badge, params);
        }
        panel.addView(homeLabel(title, 19, INK, 750));
        if (message != null && !message.isEmpty()) {
            TextView detail = homeLabel(message, 14, INK2, 400);
            detail.setLineSpacing(dp(4), 1f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = dp(8);
            panel.addView(detail, params);
        }
        return panel;
    }

    private void dialogNote(LinearLayout panel, String message) {
        TextView note = homeLabel(message, 12, INK2, 450);
        note.setPadding(dp(12), dp(10), dp(12), dp(10));
        note.setBackground(shape(TRACK, 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(12);
        panel.addView(note, params);
    }

    private void dismissCardDialog(android.app.Dialog dialog, Runnable after) {
        if (motionEnabled && dialog.isShowing()) {
            View content = dialog.findViewById(android.R.id.content);
            content.animate().alpha(0f).scaleX(0.97f).scaleY(0.97f).setDuration(140).withEndAction(() -> {
                dialog.dismiss();
                if (after != null) after.run();
            }).start();
        } else {
            dialog.dismiss();
            if (after != null) after.run();
        }
    }

    private void dialogActions(android.app.Dialog dialog, LinearLayout panel, String cancelLabel,
                               String confirmLabel, boolean destructive, Runnable onCancel, Runnable onConfirm) {
        LinearLayout buttons = new LinearLayout(this);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(22);
        panel.addView(buttons, rowParams);
        if (cancelLabel != null) {
            Button cancel = dialogButton(cancelLabel, false, false,
                    v -> dismissCardDialog(dialog, onCancel));
            buttons.addView(cancel, new LinearLayout.LayoutParams(0, -2, 1));
        }
        if (confirmLabel != null) {
            Button confirm = dialogButton(confirmLabel, true, destructive,
                    v -> dismissCardDialog(dialog, onConfirm));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
            if (cancelLabel != null) params.leftMargin = dp(10);
            buttons.addView(confirm, params);
        }
    }

    private Button dialogButton(String text, boolean primary, boolean destructive, View.OnClickListener action) {
        Button button = shareButton(text, action);
        button.setAllCaps(false);
        button.setTypeface(homeTypeface(650));
        button.setTextSize(13);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(44));
        button.setMinimumHeight(dp(44));
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        button.setTextColor(primary ? Color.WHITE : INK);
        button.setBackground(shape(primary ? (destructive ? Color.parseColor(darkMode ? "#DF5869" : "#D94855") : ACCENT) : TRACK, 12));
        pressFeedback(button);
        return button;
    }

    private void showConfirmation(int iconRes, boolean destructive, String title, String message,
                                  String cancelLabel, String confirmLabel, Runnable onCancel, Runnable onConfirm) {
        showConfirmation(iconRes, destructive, title, message, cancelLabel, confirmLabel, onCancel, onConfirm, null);
    }

    private void showConfirmation(int iconRes, boolean destructive, String title, String message,
                                  String cancelLabel, String confirmLabel, Runnable onCancel, Runnable onConfirm, String note) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout panel = dialogPanel(iconRes, destructive, title, message);
        if (note != null) dialogNote(panel, note);
        dialogActions(dialog, panel, cancelLabel, confirmLabel, destructive, onCancel, onConfirm);
        if (onCancel != null) dialog.setOnCancelListener(ignored -> onCancel.run());
        showCardDialog(dialog, panel);
    }

    private void showMessage(int icon, String title, String message) {
        showConfirmation(icon, false, title, message, null, "知道了", null, null);
    }

    private void showCardDialog(android.app.Dialog dialog, View panel) {
        if (isFinishing() || isDestroyed()) return;
        dialog.setContentView(panel);
        android.view.Window window = dialog.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(darkMode ? 0.55f : 0.28f);
        window.setWindowAnimations(0);
        dialog.show();
        window.setLayout(Math.min(dp(320), getResources().getDisplayMetrics().widthPixels - dp(48)), -2);
        if (motionEnabled) {
            panel.setAlpha(0f);
            panel.setScaleX(0.97f);
            panel.setScaleY(0.97f);
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
        }
    }

    private void confirmDelete(ScanRecord record) {
        showConfirmation(R.drawable.dialog_trash, true, "删除这条扫描记录？",
                "这条记录及暂存素材将被删除。\n已保存到下载目录的文件不受影响。",
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
                        showBottomNotice("记录已删除");
                    } else {
                        showMessage(R.drawable.dialog_trash, "记录未能完全删除",
                                "部分暂存文件未能删除，请稍后重试。已保存到下载目录的文件不受影响。");
                    }
                });
            });
        });
    }

    private void showReader() {
        showingReader = true;
        showingSettings = false;
        showingFavorites = false;
        showingMy = false;
        currentPage = -1;
        showingRecords = false;
        showingRecordPlatforms = false;
        presentSecondary(readerRoot);
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
        TextView back = label("", 22, INK, false);
        readerIcon(back, R.drawable.extract_back, 20);
        back.setContentDescription("返回");
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> leaveReader());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(34), dp(34)));
        readerHeading = homeLabel("抖音提取", 18, INK, 750);
        readerHeading.setGravity(Gravity.CENTER);
        toolbar.addView(readerHeading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView recordsButton = homeLabel("记录", 12, INK2, 650);
        recordsButton.setSingleLine(true);
        recordsButton.setMinWidth(dp(72));
        readerIcon(recordsButton, R.drawable.extract_history, 17);
        recordsButton.setGravity(Gravity.CENTER);
        recordsButton.setOnClickListener(v -> {
            recordsFromSettings = false;
            showRecords();
        });
        toolbar.addView(recordsButton, new LinearLayout.LayoutParams(-2, dp(40)));

        FrameLayout scrollArea = new FrameLayout(this);
        readerRoot.addView(scrollArea, new LinearLayout.LayoutParams(-1, 0, 1));
        readerScroll = new ScrollView(this);
        readerScroll.setFillViewport(true);
        scrollArea.addView(readerScroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(10), dp(16), dp(64));
        readerScroll.addView(content);
        TextView scrollTop = homeLabel("置顶", 12, ACCENT, 650);
        readerIcon(scrollTop, R.drawable.extract_up, 16);
        scrollTop.setGravity(Gravity.CENTER);
        scrollTop.setBackground(shape(SURFACE, 22));
        scrollTop.setElevation(dp(4));
        scrollTop.setOnClickListener(v -> { if (motionEnabled) readerScroll.smoothScrollTo(0, 0); else readerScroll.scrollTo(0, 0); });
        FrameLayout.LayoutParams scrollTopParams = new FrameLayout.LayoutParams(dp(76), dp(36), Gravity.RIGHT | Gravity.BOTTOM);
        scrollTopParams.setMargins(0, 0, dp(18), dp(10));
        scrollArea.addView(scrollTop, scrollTopParams);

        LinearLayout inputCard = new LinearLayout(this);
        inputCard.setOrientation(LinearLayout.VERTICAL);
        content.addView(inputCard);
        LinearLayout urlBox = new LinearLayout(this);
        urlBox.setGravity(Gravity.CENTER_VERTICAL);
        urlBox.setPadding(dp(10), 0, dp(6), 0);
        urlBox.setBackground(outlined(12, SURFACE));
        urlBox.addView(icon(R.drawable.extract_link, ACCENT), new LinearLayout.LayoutParams(dp(19), dp(19)));
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(-1, dp(46));
        inputCard.addView(urlBox, urlParams);
        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(12);
        input.setTypeface(homeTypeface(450));
        input.setTextColor(INK);
        input.setHintTextColor(INK3);
        input.setHint("粘贴抖音作品链接或分享文案");
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(dp(8), 0, 0, 0);
        urlBox.addView(input, new LinearLayout.LayoutParams(0, -1, 1));
        ImageView paste = new ImageView(this);
        paste.setImageResource(R.drawable.extract_paste);
        paste.setScaleType(ImageView.ScaleType.FIT_CENTER);
        paste.setPadding(dp(8), dp(8), dp(8), dp(8));
        paste.setImageTintList(ColorStateList.valueOf(ACCENT));
        paste.setContentDescription("粘贴");
        pressFeedback(paste);
        paste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                input.setText(clipboard.getPrimaryClip().getItemAt(0).coerceToText(this));
                showBottomNotice("链接已粘贴");
            } else showTopNotice("剪贴板中没有可粘贴的内容");
        });
        LinearLayout.LayoutParams pasteParams = new LinearLayout.LayoutParams(dp(36), dp(40));
        pasteParams.leftMargin = dp(6);
        urlBox.addView(paste, pasteParams);
        ImageView copy = new ImageView(this);
        copy.setImageResource(R.drawable.extract_copy);
        copy.setScaleType(ImageView.ScaleType.FIT_CENTER);
        copy.setPadding(dp(8), dp(8), dp(8), dp(8));
        copy.setImageTintList(ColorStateList.valueOf(ACCENT));
        copy.setContentDescription("复制");
        pressFeedback(copy);
        copy.setOnClickListener(v -> {
            String link = extractLink(input.getText().toString(), xhsMode);
            if (link == null && input.getText().length() == 0) link = activeLink;
            if (link == null) {
                showTopNotice("没有可复制的链接");
            } else if (copyLink(link)) {
                flashCopied(copy);
            } else {
                showTopNotice("复制失败：剪贴板不可用");
            }
        });
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(dp(36), dp(40));
        copyParams.leftMargin = dp(6);
        urlBox.addView(copy, copyParams);
        Button open = new Button(this);
        open.setAllCaps(false);
        open.setText("打开作品");
        open.setTextColor(ACCENT);
        open.setTextSize(14);
        open.setTypeface(homeTypeface(700));
        GradientDrawable openShape = outlined(12, SURFACE);
        openShape.setStroke(dp(1), ACCENT);
        open.setBackground(openShape);
        open.setTag("reader-primary-outline");
        readerIcon(open, R.drawable.extract_open, 20);
        open.setMinHeight(0);
        open.setStateListAnimator(null);
        open.setOnClickListener(v -> openLink());
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(44));
        actionParams.topMargin = dp(12);
        inputCard.addView(actionRow, actionParams);
        actionRow.addView(open, new LinearLayout.LayoutParams(0, -1, 1));

        LinearLayout scanCard = new LinearLayout(this);
        scanCard.setOrientation(LinearLayout.HORIZONTAL);
        scanCard.setGravity(Gravity.TOP);
        scanCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        scanCard.setBackground(card(16));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(12);
        content.addView(scanCard, statusParams);
        scanStatusIcon = icon(R.drawable.extract_info, ACCENT);
        LinearLayout.LayoutParams scanIconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        scanIconParams.topMargin = dp(2);
        scanCard.addView(scanStatusIcon, scanIconParams);
        LinearLayout scanBody = new LinearLayout(this);
        scanBody.setOrientation(LinearLayout.VERTICAL);
        scanBody.setGravity(Gravity.CENTER_VERTICAL);
        scanBody.setPadding(dp(12), 0, 0, 0);
        scanCard.addView(scanBody, new LinearLayout.LayoutParams(0, -2, 1));
        scanStateLabel = homeLabel("扫描中", 16, INK, 750);
        scanStateLabel.setVisibility(View.GONE);
        scanBody.addView(scanStateLabel);
        status = homeLabel("粘贴作品链接，打开网页后进入评论区", 12, INK2, 450);
        scanBody.addView(status);
        scanCountLine = homeLabel("", 12, INK2, 400);
        scanCountLine.setLineSpacing(dp(3), 1f);
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
        scanDetails = homeLabel("查看扫描明细", 12, ACCENT, 650);
        scanDetails.setGravity(Gravity.CENTER_VERTICAL);
        scanDetails.setMinHeight(dp(32));
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
        webToggleRow.setPadding(dp(12), 0, dp(12), 0);
        webToggleRow.addView(icon(R.drawable.extract_globe, INK), new LinearLayout.LayoutParams(dp(20), dp(20)));
        webToggleRow.setBackground(card(14));
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(-1, dp(44));
        toggleParams.topMargin = dp(12);
        content.addView(webToggleRow, toggleParams);
        webToggle = homeLabel("网页操作", 13, INK, 650);
        LinearLayout.LayoutParams webLabelParams = new LinearLayout.LayoutParams(0, -2, 1);
        webLabelParams.leftMargin = dp(9);
        webToggleRow.addView(webToggle, webLabelParams);
        webToggleArrow = icon(R.drawable.extract_chevron, INK2);
        webToggleRow.addView(webToggleArrow, new LinearLayout.LayoutParams(dp(18), dp(18)));
        pressFeedback(webToggleRow);
        webToggleRow.setOnClickListener(v -> {
            if (motionEnabled) android.transition.TransitionManager.beginDelayedTransition(
                    (android.view.ViewGroup) browserPanel.getParent(), new android.transition.ChangeBounds().setDuration(180));
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
        scanButton.setText("扫描评论");
        scanButton.setTextColor(Color.WHITE);
        scanButton.setTextSize(14);
        scanButton.setTypeface(homeTypeface(700));
        readerIcon(scanButton, R.drawable.extract_scan, 20);
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
                stripPaused = true;
                scanButton.setText("继续扫描");
                status.setText("扫描已暂停，已提取素材保留在暂存记录中");
                recordScanPause("已手动暂停扫描", false);
                updateScanStrip();
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
                stripPaused = false;
                scanButton.setText("暂停扫描");
                startPage();
            }
        });
        LinearLayout.LayoutParams scanButtonParams = new LinearLayout.LayoutParams(0, -1, 1);
        scanButtonParams.leftMargin = dp(10);
        actionRow.addView(scanButton, scanButtonParams);

        resultAnchor = homeLabel("提取结果", 18, INK, 750);
        LinearLayout resultHeader = new LinearLayout(this);
        resultHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams resultHeadParams = new LinearLayout.LayoutParams(-1, -2);
        resultHeadParams.topMargin = dp(22);
        content.addView(resultHeader, resultHeadParams);
        resultHeader.addView(resultAnchor, new LinearLayout.LayoutParams(0, -2, 1));
        resultSummary = label("提取后自动显示图片与动图预览", 12, INK3, false);
        resultSummary.setVisibility(View.GONE);
        selectionAction = homeLabel("多选", 12, ACCENT, 650);
        readerIcon(selectionAction, R.drawable.extract_select, 18);
        selectionAction.setGravity(Gravity.CENTER);
        selectionAction.setPadding(dp(12), dp(6), dp(12), dp(6));
        selectionAction.setBackgroundColor(Color.TRANSPARENT);
        selectionAction.setOnClickListener(v -> {
            selectingMedia = !selectingMedia;
            selectedMedia.clear();
            renderResults();
            updateResultLabels();
        });
        resultHeader.addView(selectionAction, new LinearLayout.LayoutParams(dp(70), dp(36)));
        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        readerFilterTrack = new FrameLayout(this);
        readerFilterTrack.setPadding(dp(3), dp(3), dp(3), dp(3));
        readerFilterTrack.setBackground(shape(TRACK, 10));
        readerFilterIndicator = new View(this);
        readerFilterIndicator.setBackground(shape(ACCENT, 8));
        readerFilterTrack.addView(readerFilterIndicator, new FrameLayout.LayoutParams(0, -1));
        readerFilterTrack.addView(filters, new FrameLayout.LayoutParams(-1, -1));
        readerFilterTrack.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            if (r - l != or - ol) positionReaderFilter(false);
        });
        LinearLayout.LayoutParams filtersParams = new LinearLayout.LayoutParams(-1, dp(36));
        filtersParams.topMargin = dp(12);
        filtersParams.bottomMargin = dp(12);
        content.addView(readerFilterTrack, filtersParams);
        allFilter = segment("全部 0", "all", filters);
        gifFilter = segment("动图 0", "gif", filters);
        imageFilter = segment("图片 0", "image", filters);
        emptyResults = homeLabel("等待提取素材\n打开作品并扫描评论后，素材会显示在这里", 12, INK2, 400);
        emptyResults.setLineSpacing(dp(5), 1f);
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
        saveBar.setPadding(dp(16), dp(8), dp(16), dp(8));
        saveBar.setBackgroundColor(SURFACE);
        readerRoot.addView(saveBar, new LinearLayout.LayoutParams(-1, dp(64)));
        LinearLayout saveInfo = new LinearLayout(this);
        saveInfo.setOrientation(LinearLayout.VERTICAL);
        saveBar.addView(saveInfo, new LinearLayout.LayoutParams(0, -2, 1));
        countLabel = homeLabel("已提取 0 个", 14, INK, 750);
        saveInfo.addView(countLabel);
        saveSub = homeLabel("保存到 下载 / DouYinGetting", 10, INK2, 400);
        LinearLayout.LayoutParams saveSubParams = new LinearLayout.LayoutParams(-1, -2);
        saveSubParams.topMargin = dp(3);
        saveInfo.addView(saveSub, saveSubParams);
        saveButton = new Button(this);
        saveButton.setAllCaps(false);
        saveButton.setText("保存全部");
        saveButton.setTextColor(Color.WHITE);
        saveButton.setTextSize(13);
        saveButton.setTypeface(homeTypeface(700));
        readerIcon(saveButton, R.drawable.extract_download, 18);
        saveButton.setBackground(shape(ACCENT, 10));
        saveButton.setMinHeight(0);
        saveButton.setStateListAnimator(null);
        saveButton.setOnClickListener(v -> {
            ArrayList<Media> snapshot = new ArrayList<>();
            for (Media item : media) if (!selectingMedia || selectedMedia.contains(item)) snapshot.add(item);
            saveMediaBatch(snapshot, this::showSaveBatchResult);
        });
        saveBar.addView(saveButton, new LinearLayout.LayoutParams(dp(118), dp(40)));
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
        scanStateLabel.setVisibility(scanning ? View.VISIBLE : View.GONE);
        status.setVisibility(scanning ? View.GONE : View.VISIBLE);
        int stateIcon = scanning ? R.drawable.extract_spinner
                : currentRecord != null && currentRecord.complete ? R.drawable.extract_check : R.drawable.extract_info;
        if (!Integer.valueOf(stateIcon).equals(scanStatusIcon.getTag())) {
            scanStatusIcon.setImageResource(stateIcon);
            scanStatusIcon.setTag(stateIcon);
        }
        if (scanning && showingReader && motionEnabled) {
            if (scanStatusIcon.getAnimation() == null) {
                android.view.animation.RotateAnimation rotation = new android.view.animation.RotateAnimation(0, 360,
                        android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                        android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
                rotation.setDuration(900);
                rotation.setRepeatCount(android.view.animation.Animation.INFINITE);
                rotation.setInterpolator(new android.view.animation.LinearInterpolator());
                scanStatusIcon.startAnimation(rotation);
            }
        } else scanStatusIcon.clearAnimation();
        if (scanButton != null && !Boolean.valueOf(scanning).equals(scanButton.getTag())) {
            Drawable drawable = getDrawable(scanning ? R.drawable.extract_pause : R.drawable.extract_scan).mutate();
            drawable.setTint(Color.WHITE);
            drawable.setBounds(0, 0, dp(20), dp(20));
            scanButton.setCompoundDrawablesRelative(drawable, null, null, null);
            scanButton.setTag(scanning);
        }
        ScanRecord record = currentRecord;
        scanDetails.setVisibility(record == null ? View.GONE : View.VISIBLE);
        scanCountLine.setVisibility(record == null ? View.GONE : View.VISIBLE);
        if (record == null) return;
        scanCountLine.setText(scanCounts(record) + (scanning ? "\n已找到 " + media.size() + " 个素材" : ""));
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
        LinearLayout panel = dialogPanel(R.drawable.extract_history, false, "扫描明细", "查看已读取的评论与本次异常。");
        ScrollView scroll = dialogScroll();
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, -2, 1));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(12), 0, dp(4));
        scroll.addView(body);
        String state = record.complete ? "扫描已完成" : scanning && currentRecord == record ? "扫描进行中" : "扫描未完成";
        body.addView(homeLabel(state, 12, INK2, 450));
        if (record.mainScanned + record.replyScanned == 0 && record.scanned > 0) {
            dialogStats(body, new String[]{"已读评论", "素材"},
                    new String[]{String.valueOf(record.scanned), String.valueOf(mediaCount)}, false);
        } else dialogStats(body, new String[]{"主评论", "回复", "素材"},
                new String[]{String.valueOf(record.mainScanned), String.valueOf(record.replyScanned), String.valueOf(mediaCount)}, false);
        boolean canRetry = mediaFailures + replyFailures > 0;
        if (canRetry) {
            body.addView(groupTitle("待处理", 14));
            if (mediaFailures > 0) dialogNote(body, mediaFailures + " 个素材下载失败\n可重新下载未成功的文件");
            if (replyFailures > 0) dialogNote(body, replyFailures + " 组回复读取中断\n此前读取的结果已保留");
        }
        body.addView(groupTitle("异常记录 · " + (issueCount + olderEmpty) + " 条", 14));
        if (issueCount > issues.size()) body.addView(homeLabel("显示最近 " + issues.size() + " 条", 11, INK2, 400));
        if (olderEmpty > 0) issues.add(0, "旧记录中有 " + olderEmpty + " 组回复未返回数据");
        if (issues.isEmpty()) body.addView(homeLabel("本次未记录异常", 13, INK2, 400));
        else for (String issue : issues) dialogNote(body, issue);
        dialogActions(dialog, panel, "关闭", canRetry ? "重试失败项" : null, false, null,
                () -> showRetryOptions(record));
        showCardDialog(dialog, panel);
    }

    private ScrollView dialogScroll() {
        return new ScrollView(this) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int limit = getResources().getDisplayMetrics().heightPixels / 2;
                if (View.MeasureSpec.getMode(heightSpec) != View.MeasureSpec.UNSPECIFIED)
                    limit = Math.min(limit, View.MeasureSpec.getSize(heightSpec));
                super.onMeasure(widthSpec, View.MeasureSpec.makeMeasureSpec(limit, View.MeasureSpec.AT_MOST));
            }
        };
    }

    private void showRetryOptions(ScanRecord record) {
        if (currentRecord != record || scanning || record.processingPage) {
            showTopNotice("请暂停扫描并等待当前页保存完成");
            return;
        }
        ArrayList<String> parents;
        int mediaFailures;
        synchronized (record) {
            mediaFailures = record.failedMedia.size();
            parents = new ArrayList<>(record.failedReplies.keySet());
        }
        if (mediaFailures == 0 && parents.isEmpty()) return;
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = dialogPanel(R.drawable.dialog_retry, false, "选择重试内容", "只处理失败项，保留已有结果。");
        ScrollView scroll = dialogScroll();
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, -2, 1);
        scrollParams.topMargin = dp(12);
        panel.addView(scroll, scrollParams);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        if (mediaFailures > 0) addRetryChoice(list, "失败素材 · " + mediaFailures + " 个", "重新下载未成功的文件",
                R.drawable.extract_download, () -> dismissCardDialog(dialog, () -> retryFailedMedia(record)));
        for (int i = 0; i < parents.size(); i++) {
            String parent = parents.get(i);
            addRetryChoice(list, "回复组 " + (i + 1), "从上次中断的位置继续", R.drawable.dialog_retry,
                    () -> dismissCardDialog(dialog, () -> retryFailedReply(record, parent)));
        }
        dialogNote(panel, "点击一项即可开始重试");
        dialogActions(dialog, panel, "关闭", null, false, null, null);
        showCardDialog(dialog, panel);
    }

    private void addRetryChoice(LinearLayout list, String title, String detail, int resource, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setMinimumHeight(dp(62));
        row.setBackground(shape(TRACK, 13));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        if (list.getChildCount() > 0) params.topMargin = dp(10);
        list.addView(row, params);
        row.addView(icon(resource, ACCENT), new LinearLayout.LayoutParams(dp(20), dp(20)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -2, 1);
        wp.leftMargin = dp(10);
        wp.rightMargin = dp(8);
        row.addView(words, wp);
        words.addView(homeLabel(title, 13, INK, 650));
        words.addView(homeLabel(detail, 11, INK2, 400));
        row.addView(icon(R.drawable.extract_chevron, INK2), new LinearLayout.LayoutParams(dp(14), dp(14)));
        row.setOnClickListener(v -> action.run());
        pressFeedback(row);
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
        recordScanPause(reason, true);
    }

    private void recordScanPause(String reason, boolean abnormal) {
        ScanRecord record = currentRecord;
        if (record == null) return;
        if (abnormal) {
            String location = isXhsLink(record.link) ? "网页评论"
                    : record.mainDone && record.replyIndex < record.replyParents.size()
                    ? "回复组 " + (record.replyIndex + 1) + "（主评论 ID " + record.replyParents.get(record.replyIndex) + "）"
                    : "主评论分页（已读取 " + scanned + " 条）";
            record.addIssue(location + "： " + reason);
        }
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
        records.addAll(readRecords(null));
    }

    private ArrayList<ScanRecord> readRecords(HashSet<String> included) {
        ArrayList<ScanRecord> records = new ArrayList<>();
        File[] folders = recordsFolder().listFiles();
        if (folders == null) folders = new File[0];
        for (File folder : folders) {
            if (!folder.isDirectory() || (included != null && !included.contains(folder.getName()))) continue;
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
        if (included == null && oldFiles != null && oldFiles.length > 0) {
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
        return records;
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
        showConfirmation(R.drawable.dialog_camera, false, "允许网页使用相机？",
                "当前网页请求使用相机进行身份验证。",
                "暂不允许", "允许使用", request::deny, () -> {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    } else {
                        pendingCameraRequest = request;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1001);
                    }
                }, "请求方：" + request.getOrigin().getHost());
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
        webToggleArrow.animate().cancel();
        if (motionEnabled) webToggleArrow.animate().rotation(expanded ? 180f : 0f).setDuration(180).start();
        else webToggleArrow.setRotation(expanded ? 180f : 0f);
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
        positionReaderFilter(false);
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

    private boolean copyLink(String link) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) return false;
        clipboard.setPrimaryClip(ClipData.newPlainText("作品链接", link));
        return true;
    }

    private void showCopiedBanner() {
        showBottomNotice("链接已复制");
    }

    private void flashCopied(ImageView button) {
        button.setImageResource(R.drawable.extract_check);
        button.setBackground(shape(ACCENT_SOFT, 8));
        showCopiedBanner();
        Object pending = button.getTag();
        if (pending instanceof Runnable) button.removeCallbacks((Runnable) pending);
        Runnable restore = () -> {
            button.setImageResource(R.drawable.extract_copy);
            button.setBackgroundColor(Color.TRANSPARENT);
        };
        button.setTag(restore);
        button.postDelayed(restore, 1000);
    }

    private void flashCopied(TextView button, String original) {
        button.setText("✓ 已复制");
        button.setTextColor(ACCENT);
        showCopiedBanner();
        Object pending = button.getTag();
        if (pending instanceof Runnable) button.removeCallbacks((Runnable) pending);
        Runnable restore = () -> {
            button.setText(original);
            button.setTextColor(INK2);
        };
        button.setTag(restore);
        button.postDelayed(restore, 1000);
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
        showBottomNotice("扫描完成 · 主评论 " + record.mainScanned + " · 回复 " + record.replyScanned
                + " · 素材 " + media.size());
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
        if (showingReader) showConfirmation(R.drawable.extract_pause, false, "扫描暂时中断",
                "当前页多次读取失败。\n已提取素材已保留，可从当前页继续。",
                "稍后再试", "继续扫描", null, () -> scanButton.performClick(),
                "已读 " + currentRecord.scanned + " 条评论 · 已提取 " + currentRecord.items.size() + " 个素材");
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
        showBottomNotice("扫描完成 · 主评论 " + record.mainScanned + " · 回复 " + record.replyScanned
                + " · 素材 " + media.size()
                + (record.emptyReplyLists > 0 ? "；" + record.emptyReplyLists + " 组回复未返回数据，可查看明细" : ""));
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
        selectionAction.setTextColor(selectingMedia ? Color.WHITE : ACCENT);
        selectionAction.setBackground(selectingMedia ? shape(ACCENT, 8) : null);
        for (Drawable drawable : selectionAction.getCompoundDrawablesRelative())
            if (drawable != null) drawable.setTint(selectionAction.getCurrentTextColor());
        countLabel.setText(selectingMedia ? "已选 " + selectedMedia.size() + " / " + media.size() + " 个" : "已提取 " + media.size() + " 个");
        saveButton.setText(selectingMedia ? "保存选中" : "保存全部");
        saveButton.setEnabled(!selectingMedia || !selectedMedia.isEmpty());
        saveButton.setAlpha(saveButton.isEnabled() ? 1f : 0.45f);
        shareSelectedButton.setVisibility(selectingMedia ? View.VISIBLE : View.GONE);
        shareSelectedButton.setEnabled(!selectedMedia.isEmpty());
        shareSelectedButton.setAlpha(shareSelectedButton.isEnabled() ? 1f : 0.45f);
        saveSub.setText(media.isEmpty() ? "保存到 下载 / DouYinGetting"
                : gifCount + " 个动图 · " + (media.size() - gifCount) + " 张图片");
        emptyResults.setText(media.isEmpty() ? "等待提取素材\n打开作品并扫描评论后，素材会显示在这里" : "当前分类没有结果");
        emptyResults.setVisibility(displayedCount == 0 ? View.VISIBLE : View.GONE);
    }

    private void renderResults() {
        for (Animatable animation : animations) animation.stop();
        animations.clear();
        results.removeAllViews();
        resultRow = null;
        displayedCount = 0;
        for (Media item : media) if (matchesFilter(item)) addTile(item);
        emptyResults.setText(media.isEmpty() ? "等待提取素材\n打开作品并扫描评论后，素材会显示在这里" : "当前分类没有结果");
        emptyResults.setVisibility(displayedCount == 0 ? View.VISIBLE : View.GONE);
        styleSegment(allFilter, "all");
        styleSegment(gifFilter, "gif");
        styleSegment(imageFilter, "image");
    }

    private void styleSegment(TextView button, String type) {
        boolean active = type.equals(selectedFilter);
        button.setTextColor(active ? Color.WHITE : INK2);
        button.setBackground(null);
    }

    private void addTile(Media item) {
        if (displayedCount % 3 == 0) {
            resultRow = new LinearLayout(this);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(8);
            results.addView(resultRow, rowParams);
        }
        int tileWidth = (getResources().getDisplayMetrics().widthPixels - dp(48)) / 3;
        LinearLayout row = resultRow;
        FrameLayout art = new FrameLayout(this);
        art.setBackground(outlined(12, SURFACE));
        pressFeedback(art);
        art.setClipToOutline(true);
        LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(tileWidth, tileWidth);
        if (displayedCount % 3 != 0) artParams.leftMargin = dp(8);
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
            ImageView play = icon(R.drawable.extract_play, Color.WHITE);
            play.setPadding(dp(5), dp(5), dp(5), dp(5));
            play.setBackground(shape(Color.argb(110, 0, 0, 0), 17));
            FrameLayout.LayoutParams playParams = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.RIGHT | Gravity.BOTTOM);
            playParams.setMargins(0, 0, dp(6), dp(6));
            art.addView(play, playParams);
        }
        if (item.animated || "video/mp4".equals(item.mime)) {
            String tag = "video/mp4".equals(item.mime) ? "视频" : ("image/gif".equals(item.mime) ? "GIF" : "动图");
            TextView badge = label(tag, 9, Color.WHITE, true);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(6), 0, dp(6), 0);
            badge.setBackground(shape("video/mp4".equals(item.mime) ? Color.argb(190, 23, 37, 43) : ACCENT, 5));
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, dp(17), Gravity.RIGHT | Gravity.TOP);
            badgeParams.setMargins(0, dp(6), dp(6), 0);
            art.addView(badge, badgeParams);
        }
        if (item.kind.startsWith("回复")) {
            TextView replyBadge = label("回复", 9, Color.WHITE, true);
            replyBadge.setGravity(Gravity.CENTER);
            replyBadge.setBackground(shape(ACCENT, 5));
            art.addView(replyBadge, new FrameLayout.LayoutParams(dp(32), dp(18), Gravity.LEFT | Gravity.BOTTOM));
        }
        TextView check = label(selectedMedia.contains(item) ? "✓" : "", 15, selectedMedia.contains(item) ? Color.WHITE : ACCENT, true);
        check.setGravity(Gravity.CENTER);
        check.setBackground(shape(selectedMedia.contains(item) ? ACCENT : Color.argb(225, 255, 255, 255), 13));
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.LEFT | Gravity.TOP);
        checkParams.setMargins(dp(6), dp(6), 0, 0);
        art.addView(check, checkParams);
        check.setVisibility(selectingMedia ? View.VISIBLE : View.GONE);
        if (selectingMedia && selectedMedia.contains(item)) {
            GradientDrawable border = shape(Color.TRANSPARENT, 12);
            border.setStroke(dp(2), ACCENT);
            art.setForeground(border);
        }
        art.setContentDescription("素材 " + item.file.getName());
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
            GradientDrawable border = shape(Color.TRANSPARENT, 12);
            if (selected) border.setStroke(dp(2), ACCENT);
            art.setForeground(border);
            if (motionEnabled) {
                check.setScaleX(0.7f);
                check.setScaleY(0.7f);
                check.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
            }
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
            showTopNotice("无法分享，请确认素材仍在本机");
        }
    }

    private void addPreviewAction(LinearLayout row, String text, int resource, boolean primary, View.OnClickListener action) {
        Button button = dialogButton(text, primary, false, action);
        button.setTextSize(11);
        button.setSingleLine(true);
        button.setAutoSizeTextTypeUniformWithConfiguration(7, 11, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        button.setPadding(dp(2), dp(8), dp(2), dp(8));
        if (text.length() <= 2) {
            Drawable drawable = getDrawable(resource).mutate();
            drawable.setTint(primary ? Color.WHITE : INK2);
            drawable.setBounds(0, 0, dp(14), dp(14));
            button.setCompoundDrawablesRelative(drawable, null, null, null);
            button.setCompoundDrawablePadding(dp(3));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        if (row.getChildCount() > 0) params.leftMargin = dp(5);
        row.addView(button, params);
    }

    private void preview(Media item) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        try {
            boolean isVideo = item.mime.startsWith("video/");
            LinearLayout panel = dialogPanel(0, false, "素材预览", null);
            String formatName = item.mime.substring(item.mime.indexOf('/') + 1).toUpperCase(Locale.ROOT);
            TextView info = homeLabel(formatName, 11, INK2, 400);
            panel.addView(info);
            int limit = Math.min(dp(320), getResources().getDisplayMetrics().heightPixels / 3);
            FrameLayout frame = new FrameLayout(this);
            frame.setBackground(shape(ACCENT_SOFT, 16));
            frame.setClipToOutline(true);
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, limit);
            fp.topMargin = dp(16);
            panel.addView(frame, fp);
            String size = android.text.format.Formatter.formatShortFileSize(this, item.file.length());
            Animatable animation = null;
            if (isVideo) {
                info.setText(formatName + " · " + size);
                VideoView video = new VideoView(this);
                video.setVideoPath(item.file.getAbsolutePath());
                video.setOnPreparedListener(player -> {
                    int width = player.getVideoWidth(), height = player.getVideoHeight();
                    info.setText(formatName + (width > 0 && height > 0 ? " · " + width + " × " + height : "") + " · " + size);
                    video.start();
                });
                video.setOnErrorListener((player, what, extra) -> {
                    dismissCardDialog(dialog, () -> showMessage(R.drawable.extract_play, "暂时无法播放",
                            "当前设备未能播放这段视频。可以先保存文件，再用其他播放器打开。"));
                    return true;
                });
                video.setMediaController(new MediaController(this));
                frame.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
                dialog.setOnDismissListener(ignored -> video.stopPlayback());
            } else {
                int[] dimensions = new int[2];
                Drawable drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(item.file), (decoder, imageInfo, source) -> {
                    dimensions[0] = imageInfo.getSize().getWidth();
                    dimensions[1] = imageInfo.getSize().getHeight();
                    int longest = Math.max(dimensions[0], dimensions[1]);
                    if (longest > 1600) decoder.setTargetSize(Math.max(1, dimensions[0] * 1600 / longest),
                            Math.max(1, dimensions[1] * 1600 / longest));
                });
                info.setText(formatName + " · " + dimensions[0] + " × " + dimensions[1] + " · " + size);
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setImageDrawable(drawable);
                frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
                if (drawable instanceof Animatable) {
                    Animatable animated = (Animatable) drawable;
                    animation = animated;
                    dialog.setOnDismissListener(ignored -> animated.stop());
                }
            }
            LinearLayout actions = new LinearLayout(this);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
            ap.topMargin = dp(16);
            panel.addView(actions, ap);
            addPreviewAction(actions, isFavorite(item) ? "取消收藏" : "收藏", R.drawable.dialog_heart, false,
                    v -> dismissCardDialog(dialog, () -> toggleFavorite(item)));
            addPreviewAction(actions, "分享", R.drawable.dialog_share, false, v -> shareMedia(Collections.singletonList(item)));
            addPreviewAction(actions, "关闭", R.drawable.dialog_close, false, v -> dismissCardDialog(dialog, null));
            addPreviewAction(actions, "保存", R.drawable.extract_download, true, v -> dismissCardDialog(dialog, () -> {
                worker.execute(() -> {
                    try {
                        saveToDownloads(item);
                        runOnUiThread(() -> {
                            status.setText("已保存 " + item.file.getName());
                            showBottomNotice("保存成功");
                        });
                    } catch (Exception error) {
                        runOnUiThread(() -> showMessage(R.drawable.extract_download, "这项素材未保存",
                                fileFailureReason(error) + "素材仍在本机，可稍后重新保存。"));
                    }
                });
            }));
            showCardDialog(dialog, panel);
            if (animation != null) animation.start();
        } catch (Exception error) {
            dialog.dismiss();
            showMessage(R.drawable.extract_info, "暂时无法预览", "未能读取这项素材，请确认文件仍在本机。");
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
                if (showingFavorites) showFavorites();
                showBottomNotice(result);
            });
        });
    }

    private void showFavoriteNotice() {
        showBottomNotice("已收藏", "查看收藏", () -> openFavorites(this::returnToReaderFromFavorites));
    }

    private void openFavorites(Runnable onBack) {
        returnFromFavorites = onBack;
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
        showingMy = false;
        showingReader = false;
        showingRecords = false;
        showingRecordPlatforms = false;
        showingSettings = false;
        for (Animatable animation : animations) animation.stop();
        new FavoritesScreen().show();
    }

    private void saveMediaBatch(ArrayList<Media> items, java.util.function.BiConsumer<Integer, Integer> onResult) {
        worker.execute(() -> {
            int saved = 0;
            for (Media item : items) {
                try { saveToDownloads(item); saved++; } catch (Exception ignored) { }
            }
            int success = saved, failed = items.size() - saved;
            runOnUiThread(() -> onResult.accept(success, failed));
        });
    }

    private class FavoritesScreen {
        private final LinearLayout root = new LinearLayout(MainActivity.this);
        private final LinearLayout list = new LinearLayout(MainActivity.this);
        private final ArrayList<Media> items = new ArrayList<>();
        private final HashSet<Media> selected = new HashSet<>();
        private final String[] categories = {"全部", "图片", "动图", "视频"};
        private final TextView[] filters = new TextView[categories.length];
        private FrameLayout filterTrack;
        private View filterIndicator;
        private android.animation.ValueAnimator filterAnimation;
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

            title = homeLabel("收藏", 26, INK, 800);
            header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            select = shareButton("多选", v -> setSelecting(!selecting));
            select.setTypeface(homeTypeface(700));
            header.addView(select, new LinearLayout.LayoutParams(dp(68), dp(44)));
            select.setEnabled(false);
            header.addView(settingsButton(), new LinearLayout.LayoutParams(dp(44), dp(44)));

            filterTrack = new FrameLayout(MainActivity.this);
            filterTrack.setPadding(dp(3), dp(3), dp(3), dp(3));
            filterTrack.setBackground(shape(TRACK, 10));
            filterIndicator = new View(MainActivity.this);
            filterIndicator.setBackground(shape(ACCENT, 8));
            filterTrack.addView(filterIndicator, new FrameLayout.LayoutParams(0, -1));
            LinearLayout tabs = new LinearLayout(MainActivity.this);
            filterTrack.addView(tabs, new FrameLayout.LayoutParams(-1, -1));
            filterTrack.addOnLayoutChangeListener((v, l, t, r, bottom, ol, ot, or, ob) -> {
                if (r - l != or - ol) positionFilter(false);
            });
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(-1, dp(44));
            tabParams.setMargins(dp(20), dp(8), dp(20), 0);
            root.addView(filterTrack, tabParams);
            for (int i = 0; i < categories.length; i++) {
                int target = i;
                TextView tab = homeLabel(categories[i], 13, INK2, 700);
                tab.setSingleLine(true);
                tab.setAutoSizeTextTypeUniformWithConfiguration(10, 13, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
                tab.setGravity(Gravity.CENTER);
                tab.setOnClickListener(v -> {
                    if (category == target) return;
                    int previous = category;
                    category = target;
                    positionFilter(motionEnabled);
                    list.animate().cancel();
                    render();
                    if (motionEnabled) {
                        list.setAlpha(0f);
                        list.setTranslationX(dp(previous < target ? 12 : -12));
                        list.animate().alpha(1f).translationX(0f).setDuration(220).start();
                    } else {
                        list.setAlpha(1f);
                        list.setTranslationX(0f);
                    }
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
                saveMediaBatch(selection(), (saved, failed) -> {
                    busy = false;
                    updateActions();
                    showSaveBatchResult(saved, failed);
                });
            });
            remove = addAction("取消收藏", v -> confirmRemove());
            remove.setTextColor(DANGER);
            favoriteScreen = this;
            showMainPage(root, 0);
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

        private void positionFilter(boolean animate) {
            int width = filterTrack.getWidth() - filterTrack.getPaddingLeft() - filterTrack.getPaddingRight();
            if (width <= 0) return;
            int slot = width / categories.length;
            android.view.ViewGroup.LayoutParams params = filterIndicator.getLayoutParams();
            params.width = slot;
            filterIndicator.setLayoutParams(params);
            float target = (float) slot * category;
            if (filterAnimation != null) filterAnimation.cancel();
            if (animate) {
                filterAnimation = android.animation.ValueAnimator.ofFloat(filterIndicator.getTranslationX(), target);
                filterAnimation.setDuration(220);
                filterAnimation.addUpdateListener(value -> filterIndicator.setTranslationX((float) value.getAnimatedValue()));
                filterAnimation.start();
            } else filterIndicator.setTranslationX(target);
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
            int[] counts = new int[categories.length];
            counts[0] = items.size();
            for (Media item : items) counts[type(item)]++;
            for (int i = 0; i < filters.length; i++) {
                boolean active = category == i;
                filters[i].setSelected(active);
                filters[i].setText(categories[i] + " " + counts[i]);
                filters[i].setTextColor(active ? Color.WHITE : INK2);
                filters[i].setBackground(null);
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
                    badge.setBackground(shape(type(item) == 2 ? ACCENT : DARK, 6));
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
            GradientDrawable border = shape(Color.TRANSPARENT, 10);
            if (checked) border.setStroke(dp(2), ACCENT);
            tile.setForeground(border);
            tile.setContentDescription((selecting ? checked ? "已选 " : "选择 " : "预览 ")
                    + categories[type(item)] + " " + item.file.getName());
            check.setText(checked ? "✓" : "");
            check.setBackground(shape(checked ? ACCENT : Color.argb(165, 23, 37, 43), 14));
        }

        private void updateActions() {
            title.setText(selecting ? "已选 " + selected.size() + " 项" : "收藏");
            select.setText(selecting ? "完成" : "多选");
            select.setSelected(selecting);
            select.setBackground(shape(selecting ? ACCENT : ACCENT_SOFT, 12));
            select.setTextColor(selecting ? Color.WHITE : ACCENT);
            select.setEnabled(selecting || !items.isEmpty());
            actions.setVisibility(selecting ? View.VISIBLE : View.GONE);
            for (Button button : new Button[]{share, save, remove}) {
                button.setEnabled(!selected.isEmpty() && !busy);
                button.setAlpha(button.isEnabled() ? 1f : 0.45f);
            }
        }

        private void confirmRemove() {
            ArrayList<Media> snapshot = selection();
            showConfirmation(R.drawable.dialog_heart, true, "取消收藏这 " + snapshot.size() + " 项？",
                    "选中素材将从收藏夹移除。\n扫描记录和已保存的文件仍会保留。",
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
                        String result = "已取消收藏 " + removed.size() + " 项";
                        if (failed == 0) showBottomNotice(result);
                        else showMessage(R.drawable.dialog_heart, "有 " + failed + " 项未移除",
                                result + "，剩余 " + failed + " 项暂未移除，可重新选择后重试。");
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

    private String fileFailureReason(Exception error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof android.system.ErrnoException
                    && ((android.system.ErrnoException) cause).errno == android.system.OsConstants.ENOSPC)
                return "设备可用空间不足，请清理后重试。";
        }
        if (error instanceof java.io.FileNotFoundException) return "文件或保存位置暂时无法访问。";
        if (error instanceof SecurityException) return "无法访问所选文件或位置，请重新选择。";
        return "当前无法完成文件操作，请稍后重试。";
    }

    private String archiveFailureReason(Exception error, boolean exporting) {
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (exporting) {
            if (message.contains("超过 2 GB")) return "备份超过 2 GB，请清理不需要的记录后重试。";
            if (message.contains("记录素材缺失")) return "部分暂存素材已不存在，请检查扫描记录后重试。";
            if (message.contains("暂存失败")) return "扫描记录暂存失败，请稍后重试。";
            return fileFailureReason(error) + "可更换保存位置后重试。";
        }
        if (message.contains("素材缺失") || message.contains("清单缺失"))
            return "备份文件不完整，缺少记录或素材。请重新选择完整的备份文件。";
        if (message.contains("过大")) return "备份过大或文件无效，请重新选择受支持的记录备份。";
        if (error instanceof SecurityException || error instanceof java.io.FileNotFoundException)
            return fileFailureReason(error);
        return "备份文件无法读取或格式不受支持。请选择完整的咻咿提取备份文件。";
    }

    private void dialogStats(LinearLayout panel, String[] names, String[] values, boolean lastFailed) {
        LinearLayout stats = new LinearLayout(this);
        stats.setBackground(shape(ACCENT_SOFT, 12));
        stats.setPadding(dp(6), dp(12), dp(6), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(14);
        panel.addView(stats, params);
        for (int i = 0; i < names.length; i++) {
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER);
            stats.addView(column, new LinearLayout.LayoutParams(0, -2, 1));
            TextView value = homeLabel(values[i], 20, lastFailed && i == names.length - 1
                    ? Color.parseColor(darkMode ? "#FF8290" : "#D94855") : ACCENT, 750);
            value.setGravity(Gravity.CENTER);
            column.addView(value);
            TextView name = homeLabel(names[i], 11, INK2, 450);
            name.setGravity(Gravity.CENTER);
            column.addView(name);
        }
    }

    private void showSaveBatchResult(int saved, int failed) {
        String result = saved + failed == 0 ? "没有可保存的文件" : "已保存 " + saved + " 项";
        status.setText(failed == 0 ? result : result + "，另有 " + failed + " 项未保存");
        if (failed == 0) {
            showBottomNotice(result);
            return;
        }
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = dialogPanel(R.drawable.extract_download, false,
                saved == 0 ? "本次保存未成功" : "有 " + failed + " 项未保存",
                (saved == 0 ? "文件未能写入下载目录。" : "部分文件未能写入下载目录。")
                        + "\n素材仍在本机，可稍后重新保存。");
        dialogStats(panel, new String[]{"保存成功", "保存失败"}, new String[]{saved + " 项", failed + " 项"}, true);
        dialogActions(dialog, panel, null, "知道了", false, null, null);
        showCardDialog(dialog, panel);
    }

    private void showTopNotice(String message) {
        showTopNotice(message, null, null);
    }

    private void showTopNotice(String message, String actionLabel, Runnable action) {
        showNotice(message, actionLabel, action, false);
    }

    private void showBottomNotice(String message) {
        showBottomNotice(message, null, null);
    }

    private void showBottomNotice(String message, String actionLabel, Runnable action) {
        showNotice(message, actionLabel, action, true);
    }

    private void showNotice(String message, String actionLabel, Runnable action, boolean bottom) {
        if (isFinishing() || isDestroyed()) return;
        if (noticePopup != null) noticePopup.dismiss();
        View decor = getWindow().getDecorView();
        LinearLayout notice = new LinearLayout(this);
        notice.setGravity(Gravity.CENTER_VERTICAL);
        notice.setPadding(dp(16), dp(6), dp(action == null ? 16 : 8), dp(6));
        notice.setMinimumHeight(dp(44));
        notice.setBackground(card(14));
        TextView text = label(message, 13, INK, true);
        text.setGravity(Gravity.CENTER_VERTICAL);
        notice.addView(text, new LinearLayout.LayoutParams(-2, -2));
        int maxWidth = Math.min(dp(360), (decor.getWidth() > 0 ? decor.getWidth()
                : getResources().getDisplayMetrics().widthPixels) - dp(40));
        int actionWidth = 0;
        PopupWindow popup = new PopupWindow(notice, -2, -2);
        popup.setElevation(dp(8));
        popup.setFocusable(false);
        popup.setTouchable(action != null);
        popup.setOutsideTouchable(false);
        popup.setTouchModal(false);
        if (action != null) {
            TextView button = label(actionLabel, 12, ACCENT, true);
            button.setGravity(Gravity.CENTER);
            button.setPadding(dp(12), 0, dp(12), 0);
            button.setBackground(shape(ACCENT_SOFT, 10));
            button.setOnClickListener(v -> {
                popup.dismiss();
                if (noticePopup == popup) noticePopup = null;
                action.run();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(40));
            params.leftMargin = dp(12);
            button.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(dp(40), View.MeasureSpec.EXACTLY));
            actionWidth = button.getMeasuredWidth() + params.leftMargin;
            notice.addView(button, params);
        }
        text.setMaxWidth(Math.max(dp(40), maxWidth - notice.getPaddingLeft()
                - notice.getPaddingRight() - actionWidth));
        notice.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        popup.setWidth(notice.getMeasuredWidth());
        noticePopup = popup;
        int[] location = new int[2];
        decor.getLocationOnScreen(location);
        Rect frame = new Rect();
        decor.getWindowVisibleDisplayFrame(frame);
        int offset = bottom ? Math.max(dp(86), decor.getHeight() + location[1] - frame.bottom + dp(86))
                : Math.max(dp(12), frame.top - location[1] + dp(12));
        popup.showAtLocation(decor, (bottom ? Gravity.BOTTOM : Gravity.TOP) | Gravity.CENTER_HORIZONTAL, 0, offset);
        if (motionEnabled) {
            notice.setAlpha(0f);
            notice.setTranslationY(dp(bottom ? 8 : -8));
            notice.animate().alpha(1f).translationY(0).setDuration(150).start();
        }
        decor.postDelayed(() -> {
            if (noticePopup == popup) {
                popup.dismiss();
                noticePopup = null;
            }
        }, action == null ? 2000 : 4000);
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
        stripHandler.removeCallbacksAndMessages(null);
        if (themeAnimation != null) themeAnimation.cancel();
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

    private void leaveReader() {
        if (viewingSavedRecord && !resumedFromHome) showRecords();
        else showHome();
    }

    @Override public void onBackPressed() {
        if (themeAnimation != null) themeAnimation.end();
        if (showingFavorites) favoriteScreen.back();
        else if (showingRecords) leaveRecords();
        else if (showingRecordPlatforms) returnToRecordsOrigin();
        else if (showingReader) leaveReader();
        else if (showingSettings) returnFromSettings();
        else if (showingMy) showHome();
        else super.onBackPressed();
    }
}
