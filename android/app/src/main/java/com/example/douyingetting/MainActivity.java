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
import android.widget.HorizontalScrollView;
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
import java.util.List;
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
    private final BatchExtraction batch = new BatchExtraction();
    private BatchExtraction.Task batchTask;
    private boolean showingBatch;
    private boolean previewingBatchRecord;
    private TextView batchSummary;
    private LinearLayout batchRows;
    private LinearLayout batchFooter;
    private View batchDivider;
    private TextView readerRecordsButton;
    private View readerLinkInput;
    private View readerOpenButton;
    private static final android.util.SparseArray<Typeface> HOME_FONTS = new android.util.SparseArray<>();
    private static android.graphics.fonts.Font homeFontSource;
    private Typeface homeTitleFont;
    private Bitmap homeRabbit;
    private ScanRecord currentRecord;
    private String activeLink;
    private Button extractWorkButton;
    private String workMediaScript;
    private String finishedWorkPage;
    private boolean viewingSavedRecord;
    private boolean resumedFromHome;
    private static final int EXPORT_RECORDS = 7101, IMPORT_RECORDS = 7102, IMPORT_WECHAT = 7103, IMPORT_PHONE_MEDIA = 7104;
    private static final String PHONE_MEDIA_KIND = "手机导入";
    private static final int IMPORT_FAVORITE_MEDIA = 7105;
    private String favoriteImportGroup;
    private boolean importingFavoriteMedia;
    private ArrayList<Uri> pendingSharedMedia;
    private String phoneImportLink;
    private String phoneImportRecordId;
    private boolean importingPhoneMedia;
    private Button phoneImportButton;
    private boolean showingWechat;
    private boolean showingKuaishou;
    private boolean kuaishouRecords;
    private final java.util.HashMap<String, String> recordSearches = new java.util.HashMap<>();
    private final java.util.HashMap<String, Integer> recordTypes = new java.util.HashMap<>();
    private final java.util.HashMap<String, Integer> recordScrollPositions = new java.util.HashMap<>();
    private FavoritesScreen kuaishouRecordsOrigin;
    private WechatImportRecord currentWechatImport;
    private static class WechatImportRecord {
        String id, name;
        long createdAt;
        final java.util.LinkedHashSet<String> files = new java.util.LinkedHashSet<>();
    }
    private boolean wechatImportBusy;
    private String wechatPairing = "";
    private boolean recordTransferBusy;
    private int backupScope = 2; // 0 records, 1 favorites, 2 both
    private String backupGroup;
    private boolean backupMaterialsOnly;
    private RecordArchive.Prepared pendingRestore;
    private boolean savedRecordPageOpened;
    private boolean loadingSavedRecordPage;
    private boolean xhsMode;
    private TextView readerHeading;
    private TextView browserHint;
    private WebView browser;
    private EditText input;
    private TextView status;
    private TextView scanCountLine;
    private TextView scanDetails;
    private PostStatusStrip readerPostStatus;
    private ProgressBar scanProgress;
    private LinearLayout results;
    private Button scanButton;
    private PopupWindow noticePopup;
    private android.app.Dialog fullscreenPreviewDialog;
    private PopupWindow recordUndoPopup;
    private Runnable refreshRecordList = () -> {};
    private final ArrayList<PendingRecordDeletion> pendingRecordDeletions = new ArrayList<>();
    private static final long RECORD_UNDO_MS = 3000;
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
    private Button readerSelectAll;
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
    private boolean showingGroupManager;
    private boolean showingRecycleBin;
    private boolean showingPasswordSettings;
    private RecycleBin recycleBin;
    private FavoritesScreen favoriteScreen;
    private final java.util.LinkedHashSet<String> favoriteGroups = new java.util.LinkedHashSet<>();
    private final java.util.HashMap<String, String> favoriteAssignments = new java.util.HashMap<>();
    private final java.util.HashMap<String, String> privateFavoritePins = new java.util.HashMap<>();
    private JSONObject favoriteSources = new JSONObject();
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
            this(file, hash, mime, commentId, kind, thumbnail, animated, true);
        }

        Media(File file, String hash, String mime, String commentId, String kind, Drawable thumbnail,
              boolean animated, boolean inferAnimation) {
            this.file = file;
            this.hash = hash;
            this.mime = mime;
            this.commentId = commentId;
            this.kind = kind;
            this.thumbnail = thumbnail;
            this.animated = animated || (inferAnimation && ("video/mp4".equals(mime)
                    || "image/gif".equals(mime) || thumbnail instanceof Animatable));
        }
    }

    private static class ImageFingerprint {
        final int[] pixels = new int[72];
        final boolean animated;
        final float aspect;
        final long edges;

        ImageFingerprint(Media item) throws java.io.IOException {
            animated = item.animated;
            float[] ratio = {0f};
            Bitmap sample = ImageDecoder.decodeBitmap(ImageDecoder.createSource(item.file), (decoder, info, source) -> {
                ratio[0] = info.getSize().getWidth() / (float) info.getSize().getHeight();
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                decoder.setTargetSize(9, 8);
            });
            sample.getPixels(pixels, 0, 9, 0, 0, 9, 8);
            sample.recycle();
            aspect = ratio[0];
            int[] light = new int[pixels.length];
            for (int i = 0; i < pixels.length; i++) {
                int alpha = Color.alpha(pixels[i]);
                int red = 255 - alpha + Color.red(pixels[i]) * alpha / 255;
                int green = 255 - alpha + Color.green(pixels[i]) * alpha / 255;
                int blue = 255 - alpha + Color.blue(pixels[i]) * alpha / 255;
                pixels[i] = Color.rgb(red, green, blue);
                light[i] = 3 * red + 6 * green + blue;
            }
            long hash = 0;
            for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
                int pixel = y * 9 + x;
                if (light[pixel] > light[pixel + 1]) hash |= 1L << (y * 8 + x);
            }
            edges = hash;
        }

        boolean resembles(ImageFingerprint other) {
            if (animated != other.animated || Math.min(aspect, other.aspect) < Math.max(aspect, other.aspect) * 0.9f
                    || Long.bitCount(edges ^ other.edges) > 8) return false;
            int difference = 0;
            for (int i = 0; i < pixels.length; i++) {
                int first = pixels[i], second = other.pixels[i];
                difference += Math.abs(Color.red(first) - Color.red(second))
                        + Math.abs(Color.green(first) - Color.green(second))
                        + Math.abs(Color.blue(first) - Color.blue(second));
            }
            return difference <= pixels.length * 3 * 20;
        }
    }

    private static class FailedMedia {
        final String source;
        final String commentId;
        final String kind;
        final JSONArray videoUrls;

        FailedMedia(String source, String commentId, String kind, JSONArray videoUrls) {
            this.source = source;
            this.commentId = commentId;
            this.kind = kind;
            this.videoUrls = videoUrls;
        }
    }

    private static class PostFailure {
        final JSONArray urls;
        final boolean video;

        PostFailure(JSONArray urls, boolean video) {
            this.urls = urls;
            this.video = video;
        }
    }

    private static class ScanRecord {
        boolean kuaishou;
        boolean workMedia;
        int postTotal;
        final ArrayList<PostFailure> postFailures = new ArrayList<>();
        String caption = "";
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

    private static final class PendingRecordDeletion {
        final ScanRecord record;
        final RecycleBin.Entry entry;
        final long expiresAt;

        PendingRecordDeletion(ScanRecord record, RecycleBin.Entry entry) {
            this.record = record;
            this.entry = entry;
            expiresAt = android.os.SystemClock.elapsedRealtime() + RECORD_UNDO_MS;
        }
    }

    @Override public void onCreate(Bundle state) {
        applyPalette();
        setTheme(darkMode ? R.style.BlueDark : R.style.BlueLight);
        super.onCreate(state);
        recycleBin = new RecycleBin(new File(getFilesDir(), "record-trash"));
        decodeEnabled = getPreferences(MODE_PRIVATE).getBoolean("title_decode", true);
        motionEnabled = getPreferences(MODE_PRIVATE).getBoolean("page_motion", true);
        douyinPageDelayMs = getPreferences(MODE_PRIVATE).getInt("douyin_page_delay_ms", 2000);
        douyinReadReplies = getPreferences(MODE_PRIVATE).getBoolean("douyin_read_replies", false);
        updateSystemBars();
        buildReader();
        buildStage();
        loadRecords();
        if (state != null) {
            phoneImportLink = state.getString("phoneImportLink");
            phoneImportRecordId = state.getString("phoneImportRecordId");
            favoriteImportGroup = state.getString("favoriteImportGroup");
            backupScope = state.getInt("backupScope", 2);
            backupGroup = state.getString("backupGroup");
            backupMaterialsOnly = state.getBoolean("backupMaterialsOnly");
        }
        loadFavoriteGroups();
        try {
            favoriteSources = new JSONObject(getPreferences(MODE_PRIVATE).getString("favorite_sources", "{}"));
        } catch (org.json.JSONException ignored) { }
        batch.restore(getPreferences(MODE_PRIVATE).getString("extraction_queue", "[]"));
        showHome();
        updateSystemBars();
        if (getLastNonConfigurationInstance() instanceof PendingRecordDeletion[]) {
            for (PendingRecordDeletion deletion : (PendingRecordDeletion[]) getLastNonConfigurationInstance()) {
                trackRecordDeletion(deletion);
            }
            stage.post(this::refreshRecordUndoNotice);
        }
        if (state == null) handleSharedPost(getIntent());
        else {
            pendingSharedMedia = state.getParcelableArrayList("pendingSharedMedia");
            if (pendingSharedMedia != null) stage.post(this::showSharedMediaDestination);
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("phoneImportLink", phoneImportLink);
        state.putString("phoneImportRecordId", phoneImportRecordId);
        state.putString("favoriteImportGroup", favoriteImportGroup);
        state.putParcelableArrayList("pendingSharedMedia", pendingSharedMedia);
        state.putInt("backupScope", backupScope);
        state.putString("backupGroup", backupGroup);
        state.putBoolean("backupMaterialsOnly", backupMaterialsOnly);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSharedPost(intent);
    }

    private void handleSharedPost(Intent intent) {
        boolean multiple = Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction());
        if (!multiple && !Intent.ACTION_SEND.equals(intent.getAction())) return;
        java.util.LinkedHashSet<Uri> streams = new java.util.LinkedHashSet<>(selectedFiles(intent));
        if (multiple) {
            ArrayList<?> shared = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (shared != null) for (Object item : shared) if (item instanceof Uri) streams.add((Uri) item);
        } else {
            android.os.Parcelable shared = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (shared instanceof Uri) streams.add((Uri) shared);
        }
        streams.removeIf(uri -> !"content".equals(uri.getScheme()));
        if (!streams.isEmpty() && !"text/plain".equals(intent.getType())) {
            if (pendingSharedMedia != null || recordTransferBusy || importingFavoriteMedia || importingPhoneMedia) {
                showTopNotice("请先完成当前导入或备份操作");
                return;
            }
            pendingSharedMedia = new ArrayList<>(streams);
            showSharedMediaDestination();
            return;
        }
        if (multiple) return;
        CharSequence sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (sharedText == null) return;
        String shared = sharedText.toString().replace("\\:", ":");
        BatchExtraction incoming = new BatchExtraction();
        incoming.add(shared);
        if (!incoming.tasks.isEmpty() && (batchTask != null || showingBatch || incoming.tasks.size() > 1)) {
            int added = batch.add(shared);
            saveBatch();
            if (batchTask == null && extractWorkButton.isEnabled()) showBatch(sharedQueuedNotice(added));
            else showTopNotice(sharedQueuedNotice(added));
            return;
        }
        if (incoming.tasks.isEmpty()) {
            showTopNotice("分享内容中没有找到抖音、小红书或快手作品链接");
            return;
        }
        BatchExtraction.Task task = incoming.tasks.get(0);
        int platform = task.platform;
        String link = task.link;
        String name = BatchExtraction.PLATFORMS[platform];
        String caption = "作品链接";
        for (ScanRecord record : records) {
            if (link.equals(record.link) && !record.caption.isEmpty()) {
                caption = record.caption;
                break;
            }
        }
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = sheetPanel("选择提取方式", name + " · " + caption);
        addSheetChoice(panel, "提取作品正文", "保存作品图片或视频", R.drawable.extract_download, true,
                () -> dismissCardDialog(dialog, () -> openSharedPost(link, platform, false)));
        if (platform != 2) {
            addSheetChoice(panel, "提取评论素材", "打开评论区后扫描", R.drawable.extract_scan, false,
                    () -> dismissCardDialog(dialog, () -> openSharedPost(link, platform, true)));
        }
        showBottomSheet(dialog, panel);
    }

    private void showSharedMediaDestination() {
        if (pendingSharedMedia == null || pendingSharedMedia.isEmpty()) return;
        android.app.Dialog dialog = new android.app.Dialog(this);
        int count = pendingSharedMedia.size();
        LinearLayout panel = sheetPanel("导入分享素材", "已收到 " + count + " 个文件");
        addSheetChoice(panel, "加入收藏夹", "选择收藏分组", R.drawable.dialog_heart, true,
                () -> dismissCardDialog(dialog, () -> showFavoriteDestination(null, "", count,
                        this::importSharedFavorites, this::showSharedMediaDestination)));
        addSheetChoice(panel, "追加到作品记录", "选择已有作品记录", R.drawable.record_import, false,
                () -> dismissCardDialog(dialog, this::showSharedRecordPicker));
        Runnable cancel = () -> pendingSharedMedia = null;
        dialogActions(dialog, panel, "取消", null, false, cancel, null);
        dialog.setOnCancelListener(ignored -> cancel.run());
        presentBottomSheet(dialog, panel);
    }

    private void importSharedFavorites(String group) {
        ArrayList<Uri> files = pendingSharedMedia;
        pendingSharedMedia = null;
        if (files == null) return;
        openFavorites(null);
        importFavoriteMedia(group, files);
    }

    private void showSharedRecordPicker() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = sheetPanel("选择作品记录", null);
        EditText search = mediaSourceSearch("搜索作品标题、链接或平台");
        panel.addView(search, new LinearLayout.LayoutParams(-1, dp(44)));
        ScrollView scroll = dialogScroll();
        panel.addView(scroll);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(choices);
        Runnable update = () -> {
            choices.removeAllViews();
            String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            for (ScanRecord record : records) {
                if (!isFavoriteSourceLink(record.link) || !matchesMediaSource(record.link, record.caption, query)) continue;
                String title = record.caption.isEmpty() ? record.link : record.caption;
                addMenuCard(choices, R.drawable.blue_history, title, favoriteSourcePlatform(record.link), v -> {
                    if (record.processingPage || scanning && currentRecord == record) {
                        showTopNotice("请暂停扫描，等当前页处理完再导入");
                        return;
                    }
                    dismissCardDialog(dialog, () -> {
                        ArrayList<Uri> files = pendingSharedMedia;
                        pendingSharedMedia = null;
                        if (files == null) return;
                        importPhoneMedia(record.link, record.id, files, true);
                    });
                });
            }
            if (choices.getChildCount() == 0) choices.addView(homeLabel("没有符合条件的作品记录", 13, INK2, 450));
        };
        onMediaSearchChanged(search, update);
        update.run();
        dialogActions(dialog, panel, "返回", null, false, this::showSharedMediaDestination, null);
        dialog.setOnCancelListener(ignored -> showSharedMediaDestination());
        panel.setFocusableInTouchMode(true);
        panel.requestFocus();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        presentBottomSheet(dialog, panel);
    }

    private void openSharedPost(String link, int platform, boolean comments) {
        if (platform == 2) {
            showKuaishou();
            favoriteScreen.kuaishouLink.setText(link);
            favoriteScreen.kuaishouFetch.performClick();
        } else {
            if (!comments && !extractWorkButton.isEnabled()) {
                showTopNotice("有作品正在提取，请完成后再试");
                return;
            }
            stopScanForNavigation();
            resetPages();
            selectMode(platform == 1);
            input.setText(link);
            if (comments) openLink();
            else startWorkExtraction();
        }
    }

    private String sharedQueuedNotice(int added) {
        return added == 0 ? "这些链接已在队列中" : "已加入 " + added + " 条链接";
    }

    private void saveBatch() {
        getPreferences(MODE_PRIVATE).edit().putString("extraction_queue", batch.encode()).apply();
    }

    private void showBatch(String notice) {
        if (showingBatch) {
            refreshBatch();
            if (notice != null) showTopNotice(notice);
            return;
        }
        stopScanForNavigation();
        resetPages();
        showingBatch = true;
        previewingBatchRecord = false;
        currentPage = -8;
        LinearLayout root = pageRoot();
        LinearLayout top = header(root, "作品链接", true, this::showHome);
        TextView back = (TextView) top.getChildAt(0);
        back.setText("");
        readerIcon(back, R.drawable.extract_back, 20);
        TextView heading = (TextView) top.getChildAt(1);
        heading.setTypeface(homeTypeface(750));
        heading.setTextSize(20);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(16));
        scroll.addView(content);

        LinearLayout linkCard = new LinearLayout(this);
        linkCard.setOrientation(LinearLayout.VERTICAL);
        linkCard.setBackground(card(15));
        linkCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.addView(linkCard, new LinearLayout.LayoutParams(-1, -2));
        EditText links = new EditText(this);
        links.setGravity(Gravity.TOP | Gravity.START);
        links.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        links.setTextSize(14);
        links.setTypeface(homeTypeface(450));
        links.setTextColor(INK);
        links.setHintTextColor(INK3);
        links.setHint("粘贴多条作品链接或分享文案");
        links.setMinLines(3);
        links.setMaxLines(5);
        links.setPadding(dp(12), dp(10), dp(12), dp(10));
        links.setBackground(shape(TRACK, 12));
        linkCard.addView(links, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout inputActions = new LinearLayout(this);
        linkCard.addView(inputActions, batchRowParams());
        Button paste = dialogButton("粘贴", false, false, v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = clipboard == null ? null : clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                showTopNotice("剪贴板中没有可粘贴的内容");
                return;
            }
            if (links.length() > 0) links.append("\n");
            links.append(clip.getItemAt(0).coerceToText(this));
        });
        readerIcon(paste, R.drawable.extract_paste, 16);
        LinearLayout.LayoutParams pasteParams = new LinearLayout.LayoutParams(dp(84), -2);
        pasteParams.rightMargin = dp(8);
        inputActions.addView(paste, pasteParams);
        Button add = dialogButton("加入队列", false, false, v -> {
            String text = links.getText().toString();
            int added = batch.add(text);
            if (added == 0) {
                showTopNotice(text.trim().isEmpty() ? "请先粘贴作品链接" : "没有找到可加入的新链接");
                return;
            }
            saveBatch();
            links.getText().clear();
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(links.getWindowToken(), 0);
            showBatch(sharedQueuedNotice(added));
        });
        add.setTextColor(ACCENT);
        add.setBackground(shape(ACCENT_SOFT, 12));
        add.setSingleLine(true);
        add.setGravity(Gravity.CENTER);
        Drawable plus = getDrawable(R.drawable.group_add).mutate();
        plus.setTint(ACCENT);
        plus.setBounds(0, 0, dp(17), dp(17));
        add.setCompoundDrawablesRelative(plus, null, null, null);
        add.setCompoundDrawablePadding(dp(6));
        add.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            int textWidth = (int) Math.ceil(add.getPaint().measureText(add.getText().toString()));
            int padding = Math.max(dp(8), (r - l - textWidth - dp(33)) / 2);
            if (add.getPaddingLeft() != padding) add.setPadding(padding, dp(8), padding, dp(8));
        });
        inputActions.addView(add, new LinearLayout.LayoutParams(0, -2, 1));

        batchSummary = homeLabel("", 13, INK2, 650);
        LinearLayout.LayoutParams summaryParams = batchRowParams();
        summaryParams.topMargin = dp(18);
        content.addView(batchSummary, summaryParams);
        batchRows = new LinearLayout(this);
        batchRows.setOrientation(LinearLayout.VERTICAL);
        content.addView(batchRows, new LinearLayout.LayoutParams(-1, -2));
        batchDivider = new View(this);
        batchDivider.setBackgroundColor(LINE);
        root.addView(batchDivider, new LinearLayout.LayoutParams(-1, 1));
        batchFooter = new LinearLayout(this);
        batchFooter.setBackgroundColor(SURFACE);
        batchFooter.setPadding(dp(16), dp(8), dp(16), dp(12));
        root.addView(batchFooter);
        refreshBatch();
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        presentSecondary(root);
        if (notice != null) root.post(() -> { if (showingBatch && root.isShown()) showTopNotice(notice); });
    }

    private void refreshBatch() {
        int done = 0;
        int left = 0;
        for (BatchExtraction.Task task : batch.tasks) {
            if (task.state == BatchExtraction.State.QUEUED || task.state == BatchExtraction.State.ACTION) left++;
            else if (task.state == BatchExtraction.State.DONE) done++;
        }
        batchSummary.setText("队列 " + batch.tasks.size() + " 条 · 已完成 " + done + " · 待处理 " + left);
        batchRows.removeAllViews();
        if (batch.tasks.isEmpty()) {
            TextView empty = homeLabel("还没有待提取的作品", 13, INK2, 450);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(22), dp(12), dp(22));
            empty.setBackground(card(14));
            batchRows.addView(empty, batchRowParams());
        }
        for (BatchExtraction.Task task : batch.tasks) addBatchRow(batchRows, task);
        batchFooter.removeAllViews();
        BatchExtraction.Task next = batch.next();
        if (next != null) {
            Button start = dialogButton(next.state == BatchExtraction.State.ACTION ? "继续当前任务" : "开始提取",
                    true, false, v -> startBatch());
            batchFooter.addView(start, new LinearLayout.LayoutParams(-1, dp(46)));
        } else if (!batch.tasks.isEmpty()) {
            batchFooter.addView(dialogButton("清空队列", false, false, v -> {
                batch.tasks.clear();
                saveBatch();
                showBatch(null);
            }), new LinearLayout.LayoutParams(-1, dp(46)));
        }
        int footerVisibility = batchFooter.getChildCount() > 0 ? View.VISIBLE : View.GONE;
        batchDivider.setVisibility(footerVisibility);
        batchFooter.setVisibility(footerVisibility);
    }

    private LinearLayout.LayoutParams batchRowParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10);
        return params;
    }

    private void addBatchRow(LinearLayout content, BatchExtraction.Task task) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackground(card(14));
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        content.addView(row, batchRowParams());

        LinearLayout rowHeader = new LinearLayout(this);
        rowHeader.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(rowHeader, new LinearLayout.LayoutParams(-1, -2));
        int artwork = task.platform == 0 ? R.drawable.home_douyin
                : task.platform == 1 ? R.drawable.home_xiaohongshu : R.drawable.home_kuaishou;
        ImageView platformIcon = new ImageView(this);
        platformIcon.setImageResource(artwork);
        platformIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        platformIcon.setBackground(shape(SURFACE, 8));
        platformIcon.setClipToOutline(true);
        rowHeader.addView(platformIcon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wordsParams = new LinearLayout.LayoutParams(0, -2, 1);
        wordsParams.leftMargin = dp(10);
        wordsParams.rightMargin = dp(8);
        rowHeader.addView(words, wordsParams);
        words.addView(homeLabel(BatchExtraction.PLATFORMS[task.platform], 14, INK, 750));
        TextView link = homeLabel(task.link.replaceFirst("^https://", ""), 12, INK2, 450);
        link.setMaxLines(2);
        link.setEllipsize(android.text.TextUtils.TruncateAt.END);
        words.addView(link);

        boolean attention = task.state == BatchExtraction.State.ACTION || task.state == BatchExtraction.State.PARTIAL;
        boolean highlighted = task.state == BatchExtraction.State.DONE || task.state == BatchExtraction.State.RUNNING;
        TextView state = homeLabel(task.state.label, 11, attention ? DANGER : highlighted ? ACCENT : INK2, 650);
        state.setGravity(Gravity.CENTER);
        state.setPadding(dp(8), 0, dp(8), 0);
        state.setBackground(shape(attention ? (darkMode ? 0xff422b3b : 0xffffedf0) : highlighted ? ACCENT_SOFT : TRACK, 12));
        rowHeader.addView(state, new LinearLayout.LayoutParams(-2, dp(26)));
        if (!task.detail.isEmpty()) {
            TextView detail = homeLabel(task.detail, 12, attention ? DANGER : INK2, 450);
            row.addView(detail, batchRowParams());
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(actions);
        String method = task.comments ? "评论素材" : "作品正文";
        if (task.state == BatchExtraction.State.QUEUED && task.platform != 2) {
            addBatchAction(actions, method, true, () -> {
                android.app.Dialog dialog = new android.app.Dialog(this);
                LinearLayout panel = sheetPanel("选择提取方式", BatchExtraction.PLATFORMS[task.platform]);
                addCheckedSheetChoice(panel, "作品正文", "提取作品里的图片或视频", R.drawable.extract_download,
                        !task.comments, () -> setBatchMethod(dialog, task, false));
                addCheckedSheetChoice(panel, "评论素材", "打开评论区后扫描", R.drawable.extract_scan,
                        task.comments, () -> setBatchMethod(dialog, task, true));
                showBottomSheet(dialog, panel);
            });
        } else {
            addBatchAction(actions, method, true, null);
        }
        actions.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        if (task.state == BatchExtraction.State.QUEUED) {
            addBatchAction(actions, "移除", false, () -> {
                batch.tasks.remove(task);
                saveBatch();
                showBatch(null);
            });
        } else if (task.state == BatchExtraction.State.ACTION) {
            addBatchAction(actions, "跳过", false, () -> {
                task.state = BatchExtraction.State.SKIPPED;
                saveBatch();
                startBatch();
            });
        }
        ScanRecord record = batchRecord(task);
        if (record != null && task.state != BatchExtraction.State.ACTION) {
            addBatchAction(actions, "查看记录", true, () -> {
                if (!canStartBatch()) return;
                previewingBatchRecord = true;
                showRecord(record);
            });
        }
    }

    private void addBatchAction(LinearLayout actions, String title, boolean highlighted, Runnable action) {
        TextView button = homeLabel(title, 12, highlighted ? ACCENT : INK2, 650);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), 0, dp(10), 0);
        if (highlighted) {
            button.setBackground(new android.graphics.drawable.InsetDrawable(shape(ACCENT_SOFT, 12), 0, dp(7), 0, dp(7)));
        }
        if (action != null) {
            button.setOnClickListener(v -> action.run());
            pressFeedback(button);
        }
        actions.addView(button, new LinearLayout.LayoutParams(-2, dp(44)));
    }

    private void addCheckedSheetChoice(LinearLayout panel, String title, String detail, int iconRes, boolean selected, Runnable action) {
        addSheetChoice(panel, title, detail, iconRes, selected, action);
        LinearLayout row = (LinearLayout) panel.getChildAt(panel.getChildCount() - 1);
        row.setSelected(selected);
        if (selected) {
            ImageView check = (ImageView) row.getChildAt(row.getChildCount() - 1);
            check.setImageResource(R.drawable.extract_check);
            check.setImageTintList(ColorStateList.valueOf(ACCENT));
            check.setRotation(0);
        }
    }

    private void setBatchMethod(android.app.Dialog dialog, BatchExtraction.Task task, boolean comments) {
        dismissCardDialog(dialog, () -> {
            task.comments = comments;
            saveBatch();
            showBatch(null);
        });
    }

    private ScanRecord batchRecord(BatchExtraction.Task task) {
        if (!task.recordId.isEmpty()) {
            for (ScanRecord record : records) if (task.recordId.equals(record.id)) return record;
        }
        return null;
    }

    private boolean canStartBatch() {
        if (!scanning && extractWorkButton.isEnabled()
                && (currentRecord == null || !currentRecord.processingPage)
                && (!showingKuaishou || favoriteScreen == null || !favoriteScreen.busy)) return true;
        showTopNotice("请先结束或暂停正在进行的提取");
        return false;
    }

    private void startBatch() {
        if (batchTask != null || !canStartBatch()) return;
        BatchExtraction.Task task = batch.next();
        if (task == null) {
            showBatch("队列处理完毕");
            return;
        }
        ScanRecord record = batchRecord(task);
        batchTask = task;
        if (task.comments && record != null && record.complete) {
            finishBatchTask(record);
            return;
        }
        task.state = task.comments ? BatchExtraction.State.ACTION : BatchExtraction.State.RUNNING;
        task.detail = task.comments ? "请登录网页并打开评论区，再点击扫描" : "";
        saveBatch();
        if (task.comments && canResume(record)) {
            showRecord(record);
            openSavedRecordPage(record);
        } else {
            openSharedPost(task.link, task.platform, task.comments);
            if (task.comments) status.setText(task.detail);
        }
    }

    private void batchCommentRecord(ScanRecord record) {
        if (batchTask == null || !batchTask.comments || !batchTask.link.equals(record.link)) return;
        batchTask.state = BatchExtraction.State.RUNNING;
        batchTask.detail = "";
        batchTask.recordId = record.id;
        saveBatch();
    }

    private void batchNeedsAction(String detail) {
        if (batchTask == null) return;
        batchTask.state = BatchExtraction.State.ACTION;
        batchTask.detail = detail;
        saveBatch();
    }

    private void finishBatchTask(ScanRecord record) {
        if (batchTask == null) return;
        if (record == null) {
            batchNeedsAction("提取未完成，可以重试或跳过");
            return;
        }
        BatchExtraction.Task completed = batchTask;
        completed.state = record.postFailures.isEmpty() && record.emptyReplyLists == 0
                && record.failedReplies.isEmpty() && record.failedMedia.isEmpty()
                ? BatchExtraction.State.DONE : BatchExtraction.State.PARTIAL;
        completed.detail = "提取了 " + record.items.size() + " 个素材";
        completed.recordId = record.id;
        saveBatch();
        worker.execute(() -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || batchTask != completed) return;
            batchTask = null;
            if (showingReader || showingKuaishou || showingBatch) startBatch();
        }));
    }

    private boolean returnToBatch() {
        if (batchTask == null && !previewingBatchRecord) return false;
        if (!extractWorkButton.isEnabled() || showingKuaishou && favoriteScreen.busy
                || currentRecord != null && currentRecord.processingPage) {
            showTopNotice("请等待当前操作保存完成");
            return true;
        }
        if (batchTask != null) {
            if (scanning) {
                scanning = false;
                requestToken++;
                scanButton.setText("继续扫描");
                recordScanPause("已暂停批量提取", false);
            }
            if (batchTask.state == BatchExtraction.State.RUNNING) batchNeedsAction("已暂停，可继续当前任务");
            batchTask = null;
        }
        previewingBatchRecord = false;
        showBatch(null);
        return true;
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
        showingBatch = false;
        showingWechat = false;
        showingKuaishou = false;
        showingReader = false;
        showingRecords = false;
        showingRecordPlatforms = false;
        showingSettings = false;
        showingFavorites = false;
        showingGroupManager = false;
        showingRecycleBin = false;
        showingPasswordSettings = false;
        showingMy = false;
    }

    private ImageView icon(int resource, int color) {
        ImageView image = new ImageView(this);
        image.setImageResource(resource);
        if (resource != R.drawable.wechat_import) image.setImageTintList(ColorStateList.valueOf(color));
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
        presentSecondary(root, motionEnabled);
    }

    private void presentSecondary(LinearLayout root, boolean animate) {
        if (root == readerRoot) {
            readerRoot.setVisibility(View.VISIBLE);
            if (pageLayer != null) pageLayer.setVisibility(View.GONE);
        } else {
            showLayerPage(root);
        }
        if (animate) {
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
        if (!(scanning || stripPaused) || showingReader || showingSettings) {
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
        addHomeFeature(content, 0);
        addHomeFeature(content, 1);
        addHomeFeature(content, 3);
        addHomeFeature(content, R.drawable.home_batch, "批量提取", "多链接 · 自动去重", false, v -> showBatch(null));
        addHomeFeature(content, 2);
        ScanRecord latest = null;
        for (ScanRecord record : records) {
            if (record != null && !record.workMedia && extractLink(record.link, isXhsLink(record.link)) != null) {
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
        Typeface font = HOME_FONTS.get(weight);
        if (font == null) {
            try {
                if (homeFontSource == null) {
                    homeFontSource = new android.graphics.fonts.Font.Builder(getAssets(), "fonts/NotoSansSC.ttf").build();
                }
                android.graphics.fonts.Font weighted = new android.graphics.fonts.Font.Builder(homeFontSource)
                        .setFontVariationSettings("'wght' " + weight).setWeight(weight).build();
                android.graphics.fonts.FontFamily family = new android.graphics.fonts.FontFamily.Builder(weighted).build();
                font = new Typeface.CustomFallbackBuilder(family)
                        .setStyle(weighted.getStyle()).build();
            } catch (java.io.IOException error) {
                font = Typeface.create(Typeface.DEFAULT, weight, false);
            }
            HOME_FONTS.put(weight, font);
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

    private void addHomeFeature(LinearLayout content, int platform) {
        boolean xhs = platform == 1;
        boolean wechat = platform == 2;
        boolean kuaishou = platform == 3;
        int artwork = kuaishou ? R.drawable.home_kuaishou
                : wechat ? R.drawable.wechat_import : xhs ? R.drawable.home_xiaohongshu : R.drawable.home_douyin;
        String name = kuaishou ? "快手提取" : wechat ? "微信表情导入" : xhs ? "小红书提取" : "抖音提取";
        String detail = kuaishou ? "图片 · 视频 · 文案" : wechat ? "图片 · 动图 · 导入" : xhs ? "图片 · 回复 · 实况" : "图片 · 动图 · 回复";
        addHomeFeature(content, artwork, name, detail, platform == 0, v -> {
            if (kuaishou) showKuaishou();
            else if (wechat) unlockWechat();
            else selectMode(xhs);
        });
    }

    private void addHomeFeature(LinearLayout content, int artwork, String name, String detail,
                                boolean first, View.OnClickListener action) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(card(15));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(86));
        params.topMargin = dp(first ? 20 : 12);
        content.addView(row, params);
        ImageView image = new ImageView(this);
        image.setImageResource(artwork);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackground(shape(SURFACE, 13));
        image.setClipToOutline(true);
        row.addView(image, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -2, 1);
        wp.leftMargin = dp(16);
        row.addView(words, wp);
        words.addView(homeLabel(name, 20, INK, 750));
        words.addView(homeLabel(detail, 14, INK2, 400));
        row.addView(icon(R.drawable.home_arrow, ACCENT), new LinearLayout.LayoutParams(dp(26), dp(26)));
        row.setOnClickListener(action);
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
        HashSet<String> hiddenFavorites = new HashSet<>();
        favoriteAssignments.forEach((hash, group) -> {
            if (privateFavoritePins.containsKey(group)) hiddenFavorites.add(hash);
        });
        worker.execute(() -> {
            File[] files = new File(getFilesDir(), "favorites").listFiles();
            int count = 0;
            if (files != null) for (File file : files) if (file.isFile() && !file.getName().endsWith(".tmp")
                    && !hiddenFavorites.contains(favoriteFileHash(file))) count++;
            int total = count;
            runOnUiThread(() -> favoriteCount.setText("收藏素材\n" + total));
        });
        favoriteCount.setOnClickListener(v -> openFavorites(this::showMy));
        recordCount.setOnClickListener(v -> openRecordsFromMy());
        addMenuCard(page, R.drawable.blue_history, "提取记录", "按平台查看已提取的作品", v -> openRecordsFromMy());
        addMenuCard(page, R.drawable.extract_download, "导出备份", "扫描记录 · 收藏表情 · 原文件", v -> chooseBackupScope());
        addMenuCard(page, R.drawable.record_import, "导入备份", "恢复记录和收藏，重复内容自动跳过", v -> pickRecordArchive(false));
        addMenuCard(page, R.drawable.dialog_trash, "回收站", "恢复 7 天内删除的记录和收藏", v -> showRecycleBin());
        addMenuCard(page, R.drawable.blue_info, "关于咻咿", "版本信息与使用说明", v -> showAbout());
        showMainPage(root, 2);
    }

    private void showRecycleBin() {
        resetPages();
        showingRecycleBin = true;
        LinearLayout root = pageRoot();
        header(root, "回收站", true, this::showMy);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(8), dp(18), dp(20));
        scroll.addView(body);
        LinearLayout retention = new LinearLayout(this);
        retention.setGravity(Gravity.CENTER_VERTICAL);
        retention.setPadding(dp(12), dp(10), dp(12), dp(10));
        retention.setBackground(shape(ACCENT_SOFT, 14));
        retention.addView(icon(R.drawable.blue_info, ACCENT), new LinearLayout.LayoutParams(dp(17), dp(17)));
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(0, -2, 1);
        noteParams.leftMargin = dp(8);
        retention.addView(homeLabel("删除的记录和收藏保留 7 天。", 12, INK, 550), noteParams);
        body.addView(retention, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        body.addView(list);
        presentSecondary(root);
        loadRecycleBin(list);
    }

    private void loadRecycleBin(LinearLayout list) {
        list.removeAllViews();
        list.addView(homeLabel("正在读取回收站…", 14, INK2, 450));
        worker.execute(() -> {
            long now = System.currentTimeMillis();
            ArrayList<RecycleBin.Entry> entries = recycleBin.entries(now);
            runOnUiThread(() -> {
                if (!showingRecycleBin || !list.isAttachedToWindow()) return;
                list.removeAllViews();
                if (entries.isEmpty()) {
                    LinearLayout empty = new LinearLayout(this);
                    empty.setOrientation(LinearLayout.VERTICAL);
                    empty.setGravity(Gravity.CENTER_HORIZONTAL);
                    empty.setPadding(0, dp(84), 0, dp(30));
                    empty.addView(sectionIcon(R.drawable.dialog_trash, ACCENT, ACCENT_SOFT),
                            new LinearLayout.LayoutParams(dp(64), dp(64)));
                    TextView title = homeLabel("回收站为空", 18, INK, 750);
                    title.setPadding(0, dp(12), 0, dp(4));
                    empty.addView(title);
                    empty.addView(homeLabel("最近没有可恢复的记录或收藏。", 12, INK2, 450));
                    list.addView(empty, new LinearLayout.LayoutParams(-1, -2));
                    return;
                }
                addRecycleSection(list, entries, true, now);
                addRecycleSection(list, entries, false, now);
            });
        });
    }

    private void addRecycleSection(LinearLayout list, ArrayList<RecycleBin.Entry> entries, boolean records, long now) {
        LinearLayout section = null;
        for (RecycleBin.Entry entry : entries) {
            if (entry.record != records) continue;
            if (section == null) section = addSettingsSection(list, records ? "提取记录" : "收藏素材");
            boolean locked = !recyclePin(entry).isEmpty();
            String title = recycleTitle(entry, locked);
            long days = (entry.expiresAt - now + RecycleBin.DAY - 1) / RecycleBin.DAY;
            String prefix = entry.record ? entry.data.optString("platform")
                    : locked ? "需验证密码" : groupName(entry.data.optString("group"));
            android.text.SpannableStringBuilder detail = new android.text.SpannableStringBuilder();
            if (!prefix.isEmpty()) detail.append(prefix).append(" · ");
            int remaining = detail.length();
            detail.append("剩余 ").append(String.valueOf(days)).append(" 天");
            if (days <= 1) detail.setSpan(new android.text.style.ForegroundColorSpan(DANGER), remaining,
                    detail.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView restore = homeLabel("恢复", 12, INK, 750);
            restore.setGravity(Gravity.CENTER);
            restore.setPadding(dp(9), dp(5), dp(9), dp(5));
            restore.setBackground(shape(ACCENT_SOFT, 9));
            LinearLayout row = settingsSectionRow(recycleCover(entry, locked), title, detail, restore);
            row.setOnClickListener(v -> confirmRecycleRestore(entry, title, list));
            pressFeedback(row);
            if (section.getChildCount() != 0) section.addView(rowDivider());
            section.addView(row);
        }
    }

    private String recycleTitle(RecycleBin.Entry entry, boolean locked) {
        if (entry.record) return entry.data.optString("title", "提取记录");
        if (locked) return "私密收藏素材";
        JSONObject source = entry.data.optJSONObject("source");
        String caption = source == null ? "" : source.optString("caption");
        return caption.isEmpty() ? "收藏素材" : caption;
    }

    private View recycleCover(RecycleBin.Entry entry, boolean locked) {
        if (entry.record || locked) return sectionIcon(entry.record ? R.drawable.blue_history : R.drawable.blue_lock,
                ACCENT, ACCENT_SOFT);
        return recordCover(new Media(entry.payload, "", "image/*", "", "", null, false));
    }

    private String recyclePin(RecycleBin.Entry entry) {
        return privateFavoritePins.getOrDefault(entry.data.optString("group"), entry.data.optString("pin"));
    }

    private void confirmRecycleRestore(RecycleBin.Entry entry, String title, LinearLayout list) {
        if (recordTransferBusy) return;
        String group = entry.data.optString("group");
        String pin = recyclePin(entry);
        Runnable confirm = () -> {
            android.app.Dialog dialog = new android.app.Dialog(this);
            LinearLayout panel = sheetPanel(entry.record ? "恢复提取记录？" : "恢复收藏素材？", null);
            String destination = entry.record ? "恢复到提取记录，包含暂存素材"
                    : "恢复到收藏夹「" + groupName(favoriteGroups.contains(group) ? group : "") + "」";
            LinearLayout summary = settingsSectionRow(recycleCover(entry, !pin.isEmpty()), title, destination, null);
            summary.setBackground(shape(BG, 13));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = dp(12);
            panel.addView(summary, params);
            dialogActions(dialog, panel, "取消", "恢复", false, null, () -> restoreRecycleEntry(entry, list));
            presentBottomSheet(dialog, panel);
        };
        if (pin.isEmpty()) confirm.run();
        else showPin("验证私密收藏夹密码", group, "验证", "密码不正确", value -> matchesPin(value, pin), confirm);
    }

    private void restoreRecycleEntry(RecycleBin.Entry entry, LinearLayout list) {
        if (recordTransferBusy) return;
        recordTransferBusy = true;
        boolean legacy = entry.record && entry.data.optBoolean("legacy");
        File target = entry.record ? legacy ? new File(getFilesDir(), "media") : new File(recordsFolder(), entry.name)
                : new File(new File(getFilesDir(), "favorites"), entry.name);
        worker.execute(() -> {
            try {
                recycleBin.restore(entry, target);
                ArrayList<ScanRecord> restored = entry.record
                        ? readRecords(new HashSet<>(Collections.singletonList(legacy ? "legacy" : entry.name))) : null;
                if (restored != null) restored.removeIf(record -> !target.equals(record.folder));
                runOnUiThread(() -> {
                    recordTransferBusy = false;
                    if (entry.record) {
                        pendingRecordDeletions.removeIf(deletion -> entry.container.equals(deletion.entry.container));
                        refreshRecordUndoNotice();
                        for (ScanRecord record : restored) restoreRecordListEntry(record);
                    } else {
                        String hash = favoriteFileHash(target);
                        String group = entry.data.optString("group");
                        if (favoriteGroups.contains(group)) favoriteAssignments.put(hash, group);
                        else favoriteAssignments.remove(hash);
                        saveFavoriteGroups();
                        try { favoriteSources.put(hash, entry.data.optJSONObject("source")); }
                        catch (org.json.JSONException ignored) { }
                        saveFavoriteSources();
                    }
                    if (showingRecycleBin && list.isAttachedToWindow()) loadRecycleBin(list);
                    else if (showingMy) showMy();
                    else if (!entry.record && showingFavorites) showFavorites();
                    showBottomNotice(entry.record ? "已恢复提取记录" : "已恢复收藏素材");
                });
            } catch (java.io.IOException error) {
                runOnUiThread(() -> {
                    recordTransferBusy = false;
                    if (showingRecycleBin && list.isAttachedToWindow()) loadRecycleBin(list);
                    String detail = System.currentTimeMillis() >= entry.expiresAt ? "这项内容已超过 7 天的保留期限。"
                            : target.exists() ? "原位置已有同一项，回收站里的副本仍会保留。" : "请稍后重试。";
                    showMessage(R.drawable.record_import, "未能恢复", detail);
                });
            }
        });
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

    private void chooseBackupScope() {
        if (recordTransferBusy) return;
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = dialogPanel(R.drawable.extract_download, false, "选择备份内容", "收藏备份包含原文件、分组和作品来源。");
        String[] names = {"扫描记录", "收藏表情", "全部"};
        String[] details = {"记录、扫描进度和提取素材", "备份收藏夹、分组与作品来源", "扫描记录和收藏表情一起备份"};
        for (int i = 0; i < names.length; i++) {
            int scope = i;
            addMenuCard(panel, scope == 1 ? R.drawable.dialog_heart : R.drawable.dialog_file, names[i], details[i], v ->
                    dismissCardDialog(dialog, () -> {
                        backupScope = scope;
                        backupGroup = null;
                        backupMaterialsOnly = false;
                        pickRecordArchive(true);
                    }));
        }
        dialogActions(dialog, panel, "取消", null, false, null, null);
        showCardDialog(dialog, panel);
    }

    private ArrayList<File> backupFavorites() {
        ArrayList<File> files = new ArrayList<>();
        File[] saved = new File(getFilesDir(), "favorites").listFiles();
        if (saved != null) for (File file : saved) {
            String group = favoriteAssignments.get(favoriteFileHash(file));
            if (file.isFile() && !file.getName().endsWith(".tmp")
                    && (backupGroup == null ? !privateFavoritePins.containsKey(group) : backupGroup.equals(group)))
                files.add(file);
        }
        return files;
    }

    private static String favoriteFileHash(File file) {
        String name = file.getName();
        int extension = name.lastIndexOf('.');
        return extension < 0 ? name : name.substring(0, extension);
    }

    private void chooseGroupExport(String group) {
        if (recordTransferBusy) return;
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = dialogPanel(R.drawable.extract_download, false, "导出收藏夹", group);
        addMenuCard(panel, R.drawable.extract_download, "素材 ZIP", "打包此收藏夹中的原文件", v ->
                dismissCardDialog(dialog, () -> exportFavoriteGroup(group, true)));
        addMenuCard(panel, R.drawable.dialog_file, "咻咿备份", "保留收藏夹、原文件和作品来源", v ->
                dismissCardDialog(dialog, () -> exportFavoriteGroup(group, false)));
        dialogActions(dialog, panel, "取消", null, false, null, null);
        showCardDialog(dialog, panel);
    }

    private void exportFavoriteGroup(String group, boolean materialsOnly) {
        backupScope = 1;
        backupGroup = group;
        backupMaterialsOnly = materialsOnly;
        pickRecordArchive(true);
    }

    private String favoriteArchiveExtension(File file) throws Exception {
        try (InputStream input = new FileInputStream(file)) {
            byte[] header = new byte[32];
            if (input.read(header) < 12) throw new java.io.IOException("收藏素材无效");
            String mime = format(header);
            if (mime == null) throw new java.io.IOException("收藏素材格式无法识别");
            return mime.equals("image/jpeg") ? ".jpg" : mime.equals("video/mp4") ? ".mp4"
                    : "." + mime.substring(mime.indexOf('/') + 1);
        }
    }

    private void pickRecordArchive(boolean exporting) {
        if (recordTransferBusy) return;
        if (exporting && (backupScope == 0 ? records.isEmpty() : backupScope == 1 ? backupFavorites().isEmpty()
                : records.isEmpty() && backupFavorites().isEmpty())) {
            showTopNotice("暂无所选范围的记录或收藏");
            return;
        }
        if (exporting && backupScope != 1 && (scanning || (currentRecord != null && currentRecord.processingPage))) {
            showTopNotice("请先暂停扫描，等待当前页处理完成后再导出");
            return;
        }
        Intent intent = new Intent(exporting ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(exporting ? "application/zip" : "*/*");
        if (exporting) intent.putExtra(Intent.EXTRA_TITLE, (backupGroup == null ? "咻咿备份" :
                (backupMaterialsOnly ? "收藏素材-" : "咻咿备份-") + backupGroup.replaceAll("[^\\p{L}\\p{M}\\p{N} ._-]", "_")) + "-"
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
        if (request == IMPORT_FAVORITE_MEDIA) {
            String group = favoriteImportGroup;
            favoriteImportGroup = null;
            if (result == RESULT_OK && data != null && group != null) importFavoriteMedia(group, selectedFiles(data));
            return;
        }
        if (request == IMPORT_PHONE_MEDIA) {
            String link = phoneImportLink;
            String recordId = phoneImportRecordId;
            phoneImportLink = null;
            phoneImportRecordId = null;
            if (result == RESULT_OK && data != null && link != null) importPhoneMedia(link, recordId, selectedFiles(data), false);
            return;
        }
        if (request == IMPORT_WECHAT) {
            if (result == RESULT_OK && data != null && data.getData() != null) importWechat(data.getData());
            return;
        }
        if (request != EXPORT_RECORDS && request != IMPORT_RECORDS) return;
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (recordTransferBusy) return;
        Uri uri = data.getData();
        if (request == IMPORT_RECORDS) {
            prepareBackupRestore(uri);
            return;
        }
        if (backupScope != 1 && (scanning || (currentRecord != null && currentRecord.processingPage))) {
            showTopNotice("扫描仍在进行，请暂停后重新导出");
            return;
        }
        ArrayList<ScanRecord> snapshot = new ArrayList<>(records);
        if (backupScope == 1) snapshot.clear();
        List<File> favorites = backupScope != 0 ? backupFavorites() : Collections.emptyList();
        boolean materialsOnly = backupMaterialsOnly;
        byte[] favoriteMetadata = materialsOnly || favorites.isEmpty() ? null : favoriteBackupMetadata(favorites, backupGroup);
        recordTransferBusy = true;
        android.app.Dialog progress = backupProgress("正在导出…", "正在打包所选文件。");
        worker.execute(() -> {
            try {
                java.util.LinkedHashMap<String, File> folders = new java.util.LinkedHashMap<>();
                for (ScanRecord record : snapshot) {
                    if (!persistRecord(record)) throw new java.io.IOException("记录暂存失败，请重试");
                    folders.put(record.id, record.folder);
                }
                int favoriteCount;
                try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) throw new java.io.IOException("无法写入所选位置");
                    if (materialsOnly) {
                        if (favorites.isEmpty()) throw new java.io.IOException("收藏夹为空");
                        WorkExport.writeZip(output, favorites);
                        favoriteCount = favorites.size();
                    } else favoriteCount = RecordArchive.write(output, folders, favorites, favoriteMetadata,
                            this::recordArchiveFiles, this::favoriteArchiveExtension);
                }
                String report = materialsOnly ? "已导出 " + favoriteCount + " 个收藏素材"
                        : "导出成功：记录 " + snapshot.size() + " 条，收藏 " + favoriteCount + " 个";
                runOnUiThread(() -> {
                    if (finishBackupProgress(progress)) showBottomNotice(report);
                });
            } catch (Exception error) {
                try { android.provider.DocumentsContract.deleteDocument(getContentResolver(), uri); }
                catch (Exception ignored) { }
                runOnUiThread(() -> {
                    if (finishBackupProgress(progress)) showMessage(R.drawable.dialog_file, "未能导出", archiveFailureReason(error, true));
                });
            }
        });
    }

    private android.app.Dialog backupProgress(String title, String detail) {
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
        TextView progressTitle = homeLabel(title, 18, INK, 750);
        progressTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, -2);
        progressParams.topMargin = dp(16);
        panel.addView(progressTitle, progressParams);
        TextView progressDetail = homeLabel(detail, 13, INK2, 400);
        progressDetail.setGravity(Gravity.CENTER);
        progressDetail.setPadding(0, dp(8), 0, 0);
        panel.addView(progressDetail);
        showCardDialog(progress, panel);
        return progress;
    }

    private boolean finishBackupProgress(android.app.Dialog progress) {
        recordTransferBusy = false;
        if (isFinishing() || isDestroyed()) return false;
        progress.dismiss();
        return true;
    }

    private HashSet<String> knownRecordIds() {
        HashSet<String> known = new HashSet<>();
        for (ScanRecord record : records) known.add(record.id);
        return known;
    }

    private void prepareBackupRestore(Uri uri) {
        recordTransferBusy = true;
        android.app.Dialog progress = backupProgress("正在读取备份…", "正在检查备份内容和本机重复项。");
        HashSet<String> known = knownRecordIds();
        worker.execute(() -> {
            RecordArchive.Prepared staged = null;
            try {
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new java.io.IOException("无法读取所选备份");
                    staged = RecordArchive.prepare(input, new File(getCacheDir(), "record-import-" + UUID.randomUUID()),
                            this::recordArchiveFiles, this::favoriteArchiveExtension);
                }
                RecordArchive.Prepared archive = staged;
                RecordArchive.Result preview = archive.preview(recordsFolder(), new File(getFilesDir(), "favorites"), known);
                JSONObject metadata = archive.favoriteMetadata == null ? new JSONObject()
                        : new JSONObject(new String(archive.favoriteMetadata, StandardCharsets.UTF_8));
                runOnUiThread(() -> {
                    if (finishBackupProgress(progress)) showRestorePreview(archive, preview, metadata);
                    else archive.close();
                });
            } catch (Exception error) {
                if (staged != null) staged.close();
                runOnUiThread(() -> showRestoreError(progress, error));
            }
        });
    }

    private void showRestorePreview(RecordArchive.Prepared archive, RecordArchive.Result preview, JSONObject metadata) {
        pendingRestore = archive;
        recordTransferBusy = true;
        java.util.LinkedHashSet<String> groups = backupGroupNames(metadata);
        JSONObject assignments = metadata.optJSONObject("assignments");
        ArrayList<String> conflicts = new ArrayList<>();
        if (assignments != null) for (String hash : preview.duplicateFavoriteHashes) {
            if (privateFavoritePins.containsKey(favoriteAssignments.get(hash))) continue;
            String backupGroup = assignments.optString(hash);
            if ((backupGroup.isEmpty() || groups.contains(backupGroup))
                    && !backupGroup.equals(favoriteAssignments.getOrDefault(hash, ""))) conflicts.add(hash);
        }
        Collections.sort(conflicts);
        String summary = "提取记录 " + archive.recordCount() + " 条（重复 " + preview.skipped + " 条）\n"
                + "收藏素材 " + preview.favoriteHashes.size() + " 个（本机已有 " + preview.duplicateFavoriteHashes.size() + " 个）\n"
                + "收藏夹 " + groups.size() + " 个";
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = dialogPanel(R.drawable.record_import, false, "备份恢复预览", summary);
        if (!groups.isEmpty()) {
            ScrollView groupList = new ScrollView(this);
            TextView names = homeLabel(android.text.TextUtils.join("、", groups), 13, INK2, 450);
            names.setPadding(dp(8), dp(8), dp(8), dp(8));
            groupList.addView(names);
            panel.addView(groupList, new LinearLayout.LayoutParams(-1, dp(Math.min(84, 34 + 16 * (groups.size() - 1)))));
        }
        HashSet<String> useBackup = new HashSet<>();
        if (!conflicts.isEmpty()) {
            TextView status = homeLabel("", 13, INK, 700);
            status.setPadding(0, dp(10), 0, dp(8));
            panel.addView(status);
            Runnable update = () -> status.setText(conflicts.size() + " 项分组冲突 · " + useBackup.size() + " 项使用备份");
            update.run();
            panel.addView(restoreBulkActions(conflicts, useBackup, update));
            LinearLayout.LayoutParams review = new LinearLayout.LayoutParams(-1, -2);
            review.topMargin = dp(8);
            panel.addView(dialogButton("逐项处理冲突", false, false, v ->
                    reviewRestoreConflicts(conflicts, preview, assignments, useBackup, update)), review);
        }
        Runnable cancel = () -> {
            pendingRestore = null;
            recordTransferBusy = false;
            worker.execute(archive::close);
        };
        dialog.setOnCancelListener(ignored -> cancel.run());
        dialogActions(dialog, panel, "取消", "确认恢复", false, cancel, () -> {
            pendingRestore = null;
            restoreBackup(archive, metadata, new HashSet<>(useBackup));
        });
        showCardDialog(dialog, panel);
    }

    private LinearLayout restoreBulkActions(List<String> conflicts, HashSet<String> useBackup, Runnable update) {
        LinearLayout bulk = new LinearLayout(this);
        bulk.addView(dialogButton("全部保留本机", false, false, v -> {
            useBackup.clear();
            update.run();
        }), new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams allBackup = new LinearLayout.LayoutParams(0, -2, 1);
        allBackup.leftMargin = dp(8);
        bulk.addView(dialogButton("全部使用备份", false, false, v -> {
            useBackup.addAll(conflicts);
            update.run();
        }), allBackup);
        return bulk;
    }

    private void reviewRestoreConflicts(ArrayList<String> conflicts, RecordArchive.Result preview,
                                        JSONObject assignments, HashSet<String> useBackup, Runnable updateSummary) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = dialogPanel(0, false, "逐项处理冲突", "选定分组后，在恢复预览中确认恢复。");
        TextView position = homeLabel("", 12, INK2, 650);
        position.setPadding(0, dp(12), 0, dp(6));
        panel.addView(position);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackground(shape(TRACK, 12));
        image.setContentDescription("冲突素材缩略图");
        panel.addView(image, new LinearLayout.LayoutParams(-1, dp(120)));
        android.widget.RadioGroup choice = new android.widget.RadioGroup(this);
        android.widget.RadioButton local = new android.widget.RadioButton(this);
        android.widget.RadioButton backup = new android.widget.RadioButton(this);
        for (android.widget.RadioButton button : new android.widget.RadioButton[]{local, backup}) {
            button.setId(View.generateViewId());
            button.setTextColor(INK);
            choice.addView(button);
        }
        panel.addView(choice);

        int[] index = {0};
        LinearLayout.LayoutParams bulkParams = new LinearLayout.LayoutParams(-1, -2);
        bulkParams.topMargin = dp(8);
        bulkParams.bottomMargin = dp(8);
        panel.addView(restoreBulkActions(conflicts, useBackup, () -> {
            choice.check(useBackup.contains(conflicts.get(index[0])) ? backup.getId() : local.getId());
            updateSummary.run();
        }), bulkParams);
        LinearLayout navigation = new LinearLayout(this);
        panel.addView(navigation);
        Button previous = dialogButton("上一项", false, false, null);
        Button next = dialogButton("下一项", false, false, null);
        navigation.addView(previous, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(0, -2, 1);
        nextParams.leftMargin = dp(8);
        navigation.addView(next, nextParams);
        Runnable render = () -> {
            String hash = conflicts.get(index[0]);
            position.setText("第 " + (index[0] + 1) + " / " + conflicts.size() + " 项");
            local.setText("保留本机 · " + groupName(favoriteAssignments.getOrDefault(hash, "")));
            backup.setText("使用备份 · " + groupName(assignments.optString(hash)));
            choice.setOnCheckedChangeListener(null);
            choice.check(useBackup.contains(hash) ? backup.getId() : local.getId());
            choice.setOnCheckedChangeListener((group, checked) -> {
                if (checked == backup.getId()) useBackup.add(hash);
                else useBackup.remove(hash);
                updateSummary.run();
            });
            previous.setEnabled(index[0] > 0);
            next.setEnabled(index[0] + 1 < conflicts.size());
            previous.setAlpha(previous.isEnabled() ? 1f : 0.45f);
            next.setAlpha(next.isEnabled() ? 1f : 0.45f);
            image.setImageDrawable(null);
            image.setTag(hash);
            worker.execute(() -> {
                Drawable thumbnail = decodeThumbnail(preview.duplicateFavoriteFiles.get(hash));
                runOnUiThread(() -> {
                    if (dialog.isShowing() && hash.equals(image.getTag())) image.setImageDrawable(thumbnail);
                });
            });
        };
        previous.setOnClickListener(v -> { index[0]--; render.run(); });
        next.setOnClickListener(v -> { index[0]++; render.run(); });
        dialogActions(dialog, panel, null, "返回恢复预览", false, null, null);
        showCardDialog(dialog, panel);
        render.run();
    }

    private void restoreBackup(RecordArchive.Prepared archive, JSONObject metadata, HashSet<String> useBackupGroups) {
        android.app.Dialog progress = backupProgress("正在恢复备份…", "正在恢复所选记录与收藏。");
        HashSet<String> known = knownRecordIds();
        worker.execute(() -> {
            try (RecordArchive.Prepared staged = archive) {
                RecordArchive.Result result = staged.install(recordsFolder(), new File(getFilesDir(), "favorites"), known);
                ArrayList<ScanRecord> imported = readRecords(result.added);
                runOnUiThread(() -> {
                    boolean visible = finishBackupProgress(progress);
                    int changed = mergeFavoriteBackup(result, metadata, useBackupGroups);
                    if (!visible) return;
                    records.addAll(imported);
                    records.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                    if (showingMy) showMy();
                    String report = "恢复成功：记录 " + imported.size() + " 条，收藏 " + result.favoritesAdded
                            + " 个；跳过重复 " + (result.skipped + result.favoritesSkipped) + " 项";
                    if (changed > 0) report += "，更新 " + changed + " 个收藏分组";
                    if (imported.isEmpty() && !result.favoriteHashes.isEmpty())
                        showBottomNotice(report, "查看收藏", () -> openFavorites(this::showMy));
                    else showBottomNotice(report, "查看记录", this::openRecordsFromMy);
                });
            } catch (Exception error) {
                runOnUiThread(() -> showRestoreError(progress, error));
            }
        });
    }

    private void showRestoreError(android.app.Dialog progress, Exception error) {
        if (finishBackupProgress(progress)) showConfirmation(R.drawable.dialog_file, false, "无法导入这个备份",
                archiveFailureReason(error, false), "取消", "重新选择", null, () -> pickRecordArchive(false));
    }

    private void openRecordsFromMy() {
        recordsOrigin = 2;
        recordsFromSettings = true;
        showRecordPlatforms();
    }

    private File wechatFolder() {
        return new File(getFilesDir(), "wechat-stickers");
    }

    private void unlockWechat() {
        showPin("微信表情导入", "输入 6 位数字密码", "验证并进入", "密码不正确",
                this::matchesWechatPin, this::showWechat);
    }

    private boolean matchesWechatPin(String pin) {
        String stored = getPreferences(MODE_PRIVATE).getString("wechat_pin", "");
        return stored.isEmpty() ? "299852".equals(pin) : matchesPin(pin, stored);
    }

    private void showWechat() {
        stopScanForNavigation();
        resetPages();
        showingWechat = true;
        currentPage = -5;
        currentWechatImport = null;
        LinearLayout root = pageRoot();
        header(root, "微信表情导入", true, this::showHome);
        LinearLayout sources = new LinearLayout(this);
        sources.setPadding(dp(18), dp(8), dp(18), dp(8));
        root.addView(sources, new LinearLayout.LayoutParams(-1, dp(62)));
        sources.addView(shareButton("选择 ZIP 导入", v -> pickWechatArchive()), new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, 1);
        cp.leftMargin = dp(8);
        sources.addView(shareButton("连接电脑助手", v -> connectWechatComputer()), cp);
        TextView hint = homeLabel("每次导入单独保存为一条记录", 13, INK2, 400);
        hint.setPadding(dp(18), dp(8), dp(18), dp(8));
        root.addView(hint);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(18), 0, dp(18), dp(20));
        scroll.addView(list);
        list.addView(homeLabel("正在读取导入记录…", 14, INK2, 400));
        presentSecondary(root);
        worker.execute(() -> {
            ArrayList<WechatImportRecord> found = new ArrayList<>();
            String failure = null;
            try {
                migrateWechatImports();
                File[] files = wechatRecordsFolder().listFiles();
                if (files != null) for (File file : files) {
                    if (!file.getName().endsWith(".json")) continue;
                    JSONObject json = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
                    WechatImportRecord record = new WechatImportRecord();
                    record.id = file.getName().substring(0, file.getName().length() - 5);
                    record.name = json.getString("name");
                    record.createdAt = json.getLong("createdAt");
                    JSONArray names = json.getJSONArray("files");
                    for (int i = 0; i < names.length(); i++) {
                        String name = names.getString(i);
                        if (RecordArchive.leaf(name)) record.files.add(name);
                    }
                    found.add(record);
                }
                found.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
            } catch (Exception error) { failure = "导入记录暂时无法读取，请稍后重试"; }
            String message = failure;
            runOnUiThread(() -> {
                if (!showingWechat || currentWechatImport != null || !root.isAttachedToWindow()) return;
                list.removeAllViews();
                if (message != null || found.isEmpty()) {
                    TextView empty = homeLabel(message != null ? message : "还没有导入记录\n选择 ZIP 后，每批表情会分别显示在这里。", 14, INK2, 400);
                    empty.setGravity(Gravity.CENTER);
                    empty.setPadding(0, dp(32), 0, dp(32));
                    list.addView(empty, new LinearLayout.LayoutParams(-1, -2));
                }
                for (WechatImportRecord record : found) {
                    String date = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new java.util.Date(record.createdAt));
                    addMenuCard(list, R.drawable.wechat_import, record.name, date + " · " + record.files.size() + " 个表情", v -> showWechatRecord(record));
                    LinearLayout row = (LinearLayout) list.getChildAt(list.getChildCount() - 1);
                    TextView title = (TextView) ((LinearLayout) row.getChildAt(1)).getChildAt(0);
                    title.setSingleLine(true);
                    title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                }
            });
        });
    }

    private File wechatRecordsFolder() { return new File(getFilesDir(), "wechat-import-records"); }

    private WechatImportRecord writeWechatImport(String id, String name, java.util.Collection<String> files) throws Exception {
        WechatImportRecord record = new WechatImportRecord();
        record.id = id;
        record.name = name;
        record.createdAt = System.currentTimeMillis();
        record.files.addAll(files);
        File folder = wechatRecordsFolder();
        if (!folder.isDirectory() && !folder.mkdirs()) throw new java.io.IOException("无法保存导入记录");
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("createdAt", record.createdAt);
        json.put("files", new JSONArray(record.files));
        File temp = new File(folder, id + ".tmp");
        try {
            Files.write(temp.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
            Files.move(temp.toPath(), new File(folder, id + ".json").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally { temp.delete(); }
        return record;
    }

    private void migrateWechatImports() throws Exception {
        File[] records = wechatRecordsFolder().listFiles((folder, name) -> name.endsWith(".json"));
        if (records != null && records.length > 0) return;
        ArrayList<String> names = new ArrayList<>();
        File[] files = wechatFolder().listFiles();
        if (files != null) for (File file : files)
            if (file.isFile() && !file.getName().endsWith(".tmp")) names.add(file.getName());
        if (!names.isEmpty()) writeWechatImport("legacy", "历史导入", names);
    }

    private void showWechatRecord(WechatImportRecord record) {
        stopScanForNavigation();
        resetPages();
        showingWechat = true;
        currentPage = -5;
        currentWechatImport = record;
        new FavoritesScreen(true).show();
    }

    private void returnToWechatImport() {
        if (currentWechatImport == null) showWechat();
        else showWechatRecord(currentWechatImport);
    }

    private String wechatArchiveName(Uri uri) {
        String name = uri.getLastPathSegment();
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
            } catch (Exception ignored) { }
        }
        if (name == null || name.startsWith("wechat-received-")) name = "电脑助手导入.zip";
        return name.replaceAll("[\\p{Cntrl}]", " ").substring(0, Math.min(name.length(), 200));
    }

    private void pickWechatArchive() {
        if (wechatImportBusy) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try { startActivityForResult(intent, IMPORT_WECHAT); }
        catch (android.content.ActivityNotFoundException error) { showTopNotice("当前设备没有可用的系统文件选择器"); }
    }

    private void importWechat(Uri uri) {
        if (wechatImportBusy) return;
        wechatImportBusy = true;
        android.app.Dialog progress = new android.app.Dialog(this);
        LinearLayout panel = dialogPanel(R.drawable.wechat_import, false, "正在导入微信表情", "正在检查原文件，完成后可预览和保存。\n请稍候，不要关闭应用。");
        ProgressBar spinner = new ProgressBar(this);
        spinner.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(32), dp(32));
        sp.gravity = Gravity.CENTER_HORIZONTAL;
        sp.topMargin = dp(18);
        panel.addView(spinner, sp);
        progress.setCancelable(false);
        showCardDialog(progress, panel);
        worker.execute(() -> {
            String message;
            boolean success = false;
            WechatImportRecord importedRecord = null;
            StickerArchive.Result result = null;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream == null) throw new java.io.IOException("无法读取压缩包");
                migrateWechatImports();
                result = StickerArchive.read(stream,
                        new File(getCacheDir(), "wechat-import-" + UUID.randomUUID()), wechatFolder(),
                        file -> ImageDecoder.decodeDrawable(ImageDecoder.createSource(file), (decoder, info, source) -> {
                            if (info.getSize().getWidth() > 32768 || info.getSize().getHeight() > 32768)
                                throw new IllegalArgumentException("图片尺寸过大");
                            decoder.setTargetSampleSize(Math.max(1, Math.max(info.getSize().getWidth(), info.getSize().getHeight()) / 120));
                        }));
                importedRecord = writeWechatImport(UUID.randomUUID().toString(), wechatArchiveName(uri), result.files);
                message = "已创建导入记录，共 " + result.files.size() + " 个表情";
                if (result.duplicates > 0) message += "，重复 " + result.duplicates + " 个";
                if (result.failed > 0) message += "，无法读取 " + result.failed + " 个";
                success = true;
            } catch (Exception error) {
                if (importedRecord != null) new File(wechatRecordsFolder(), importedRecord.id + ".json").delete();
                if (result != null) for (String name : result.installed) new File(wechatFolder(), name).delete();
                String detail = error instanceof java.io.IOException ? error.getMessage() : null;
                message = detail != null && (detail.startsWith("压缩包") || detail.startsWith("无法创建") || detail.startsWith("无法保存"))
                        ? detail : fileFailureReason(error) + "请选择完整的表情 ZIP 压缩包。";
            }
            if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
                File received = new File(uri.getPath());
                if (getCacheDir().equals(received.getParentFile()) && received.getName().startsWith("wechat-received-"))
                    received.delete();
            }
            String report = message;
            boolean imported = success;
            WechatImportRecord addedRecord = importedRecord;
            runOnUiThread(() -> {
                wechatImportBusy = false;
                if (isFinishing() || isDestroyed()) return;
                progress.dismiss();
                if (imported) {
                    showWechatRecord(addedRecord);
                    showBottomNotice(report);
                } else showConfirmation(R.drawable.dialog_file, false, "表情未能导入", report,
                        "取消", "重新选择", null, this::pickWechatArchive, "已导入的表情不会被覆盖");
            });
        });
    }

    private void connectWechatComputer() {
        if (wechatImportBusy) return;
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = dialogPanel(R.drawable.wechat_import, false, "连接电脑助手",
                "手机与电脑连接同一局域网。\n电脑启动助手并选择自己的表情库，再复制配对内容。非官方导出仍有账号风险。");
        EditText pairing = new EditText(this);
        pairing.setTextColor(INK);
        pairing.setTextSize(13);
        pairing.setHint("粘贴电脑助手的配对内容");
        pairing.setHintTextColor(INK3);
        pairing.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        pairing.setText(wechatPairing);
        pairing.setMaxLines(4);
        panel.addView(pairing, new LinearLayout.LayoutParams(-1, dp(112)));
        dialogActions(dialog, panel, "取消", "连接并获取", false, null, () -> {
            String value = pairing.getText().toString().trim();
            receiveWechatComputer(value);
        });
        showCardDialog(dialog, panel);
    }

    private void receiveWechatComputer(String pairing) {
        if (wechatImportBusy) return;
        WechatComputer client;
        try { client = new WechatComputer(pairing); }
        catch (Exception error) { showTopNotice("配对内容无效，请从电脑助手重新复制"); return; }
        wechatPairing = pairing;
        wechatImportBusy = true;
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        android.app.Dialog progress = new android.app.Dialog(this);
        LinearLayout panel = dialogPanel(R.drawable.wechat_import, false, "正在连接电脑", null);
        TextView detail = homeLabel("等待电脑助手…", 13, INK2, 400);
        panel.addView(detail);
        dialogActions(progress, panel, null, "取消接收", false, null, () -> cancelled.set(true));
        progress.setCancelable(false);
        showCardDialog(progress, panel);
        File zip = new File(getCacheDir(), "wechat-received-" + UUID.randomUUID() + ".zip");
        worker.execute(() -> {
            String error = null;
            try { client.receive(zip, message -> runOnUiThread(() -> detail.setText(message)), cancelled::get); }
            catch (Exception failure) {
                error = failure instanceof javax.net.ssl.SSLException ? "电脑证书验证失败，请重新复制配对内容"
                        : failure instanceof java.net.ConnectException || failure instanceof java.net.SocketTimeoutException
                        ? "连接电脑失败，请检查同一局域网、助手是否启动，以及专用网络的防火墙许可"
                        : failure instanceof java.io.IOException && failure.getMessage() != null
                        && (failure.getMessage().startsWith("配对") || failure.getMessage().startsWith("电脑")
                        || failure.getMessage().startsWith("表情") || failure.getMessage().startsWith("等待"))
                        ? failure.getMessage() : "连接未完成，请检查电脑助手后重试";
            }
            String message = error;
            runOnUiThread(() -> {
                wechatImportBusy = false;
                if (isFinishing() || isDestroyed()) { zip.delete(); return; }
                progress.dismiss();
                if (cancelled.get()) { zip.delete(); showBottomNotice("已取消接收，电脑上的导出可能仍在继续"); }
                else if (message != null) showConfirmation(R.drawable.dialog_file, false, "未能获取表情", message,
                        "取消", "重新连接", null, this::connectWechatComputer, "已有表情不受影响");
                else importWechat(Uri.fromFile(zip));
            });
        });
    }

    private ArrayList<Media> readWechat(WechatImportRecord record) {
        ArrayList<Media> items = new ArrayList<>();
        File[] files = wechatFolder().listFiles();
        if (files == null) return items;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File file : files) {
            if (!file.isFile()) continue;
            if (record != null && !record.files.contains(file.getName())) continue;
            try (InputStream stream = new FileInputStream(file)) {
                byte[] header = new byte[32];
                if (stream.read(header) < 12) continue;
                String mime = format(header);
                if (mime == null || !mime.startsWith("image/")) continue;
                boolean[] animated = new boolean[1];
                Drawable thumbnail = ImageDecoder.decodeDrawable(ImageDecoder.createSource(file), (decoder, info, source) -> {
                    animated[0] = info.isAnimated();
                    decoder.setTargetSampleSize(Math.max(1, Math.max(info.getSize().getWidth(), info.getSize().getHeight()) / 120));
                });
                String name = file.getName();
                items.add(new Media(file, name.substring(0, name.lastIndexOf('.')), mime, "", "微信表情", thumbnail, animated[0], false));
            } catch (Exception ignored) { }
        }
        return items;
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
        // Consume taps in blank areas before they reach the reader behind this page.
        root.setClickable(true);
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
        page.addView(settingsGroupTitle("隐私", 22));
        addMenuCard(page, R.drawable.blue_gear, "密码设置", "微信表情导入与私密收藏夹", v -> showPasswordSettings());
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

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setGravity(Gravity.CENTER_VERTICAL);
        modeRow.setPadding(dp(14), dp(14), dp(14), dp(14));
        storage.addView(modeRow, new LinearLayout.LayoutParams(-1, -2));
        modeRow.addView(icon(R.drawable.extract_download, ACCENT), new LinearLayout.LayoutParams(dp(22), dp(22)));
        LinearLayout.LayoutParams modeLabelParams = new LinearLayout.LayoutParams(0, -2, 1);
        modeLabelParams.leftMargin = dp(12);
        modeRow.addView(homeLabel("保存方式", 14, INK, 700), modeLabelParams);
        TextView modeValue = homeLabel(mediaSaveMode().label, 12, ACCENT, 650);
        modeRow.addView(modeValue);
        ImageView modeArrow = icon(R.drawable.home_chevron, INK2);
        LinearLayout.LayoutParams modeArrowParams = new LinearLayout.LayoutParams(dp(16), dp(16));
        modeArrowParams.leftMargin = dp(6);
        modeRow.addView(modeArrow, modeArrowParams);
        modeRow.setOnClickListener(v -> showMediaSaveSettings(modeValue));
        pressFeedback(modeRow);
        storage.addView(rowDivider());

        LinearLayout importRow = new LinearLayout(this);
        importRow.setGravity(Gravity.CENTER_VERTICAL);
        importRow.setPadding(dp(14), dp(14), dp(14), dp(14));
        storage.addView(importRow, new LinearLayout.LayoutParams(-1, -2));
        importRow.addView(icon(R.drawable.record_import, ACCENT), new LinearLayout.LayoutParams(dp(22), dp(22)));
        LinearLayout.LayoutParams importLabelParams = new LinearLayout.LayoutParams(0, -2, 1);
        importLabelParams.leftMargin = dp(12);
        importRow.addView(homeLabel("导入方式", 14, INK, 700), importLabelParams);
        TextView importValue = homeLabel(importFromFiles() ? "文件" : "相册", 12, ACCENT, 650);
        importRow.addView(importValue);
        LinearLayout.LayoutParams importArrowParams = new LinearLayout.LayoutParams(dp(16), dp(16));
        importArrowParams.leftMargin = dp(6);
        importRow.addView(icon(R.drawable.home_chevron, INK2), importArrowParams);
        importRow.setOnClickListener(v -> showMediaImportSettings(importValue));
        pressFeedback(importRow);
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
        locationWords.addView(homeLabel("下载 / XiuyiGetting", 12, INK2, 400));
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

    private void showPasswordSettings() {
        resetPages();
        showingPasswordSettings = true;
        LinearLayout root = pageRoot();
        root.setClickable(true);
        header(root, "密码设置", true, this::showSettings);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        choices.setPadding(dp(18), dp(8), dp(18), dp(24));
        scroll.addView(choices);
        LinearLayout functions = addSettingsSection(choices, "功能密码");
        int wechatSurface = Color.parseColor(darkMode ? "#203B35" : "#E3F3E9");
        addPasswordRow(functions, sectionIcon(R.drawable.wechat_import, ACCENT, wechatSurface), "微信表情导入", "修改访问密码",
                () -> changePin("微信表情导入", this::matchesWechatPin, verifier ->
                        getPreferences(MODE_PRIVATE).edit().putString("wechat_pin", verifier).apply()));
        if (!privateFavoritePins.isEmpty()) {
            LinearLayout folders = addSettingsSection(choices, "私密收藏夹");
            for (String group : favoriteGroups) {
                if (!privateFavoritePins.containsKey(group)) continue;
                addPasswordRow(folders, sectionIcon(R.drawable.blue_lock, ACCENT, ACCENT_SOFT), group, "修改收藏夹密码",
                        () -> changePin("收藏夹 · " + group, pin -> matchesPin(pin, privateFavoritePins.get(group)), verifier -> {
                            privateFavoritePins.put(group, verifier);
                            saveFavoriteGroups();
                        }));
            }
        }
        presentSecondary(root);
    }

    private void addPasswordRow(LinearLayout section, View leading, String title, String detail, Runnable action) {
        if (section.getChildCount() != 0) section.addView(rowDivider());
        LinearLayout row = settingsSectionRow(leading, title, detail, icon(R.drawable.home_chevron, INK3));
        row.setOnClickListener(v -> action.run());
        pressFeedback(row);
        section.addView(row);
    }

    private LinearLayout addSettingsSection(LinearLayout parent, String title) {
        TextView heading = settingsGroupTitle(title, 16);
        heading.setTextSize(12);
        heading.setTextColor(INK2);
        parent.addView(heading);
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(card(16));
        section.setClipToOutline(true);
        parent.addView(section, new LinearLayout.LayoutParams(-1, -2));
        return section;
    }

    private View sectionIcon(int resource, int tint, int background) {
        ImageView image = icon(resource, tint);
        image.setPadding(dp(10), dp(10), dp(10), dp(10));
        image.setBackground(shape(background, 12));
        return image;
    }

    private LinearLayout settingsSectionRow(View leading, String title, CharSequence detail, View trailing) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(11), dp(12), dp(11));
        row.setMinimumHeight(dp(66));
        row.addView(leading, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wordsParams = new LinearLayout.LayoutParams(0, -2, 1);
        wordsParams.setMargins(dp(11), 0, dp(8), 0);
        row.addView(words, wordsParams);
        TextView name = homeLabel(title, 14, INK, 750);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        words.addView(name);
        TextView description = homeLabel("", 12, INK2, 450);
        description.setPadding(0, dp(3), 0, 0);
        description.setText(detail);
        words.addView(description);
        if (trailing != null) {
            int width = trailing instanceof ImageView ? dp(16) : -2;
            row.addView(trailing, new LinearLayout.LayoutParams(width, width));
        }
        return row;
    }

    private void changePin(String target, java.util.function.Predicate<String> matchesCurrent,
                           java.util.function.Consumer<String> saveVerifier) {
        showPin("验证当前密码", target, "下一步", "密码不正确", matchesCurrent, () -> {
            String[] verifier = {null};
            showPin("设置新密码", target, "下一步", "", pin -> {
                verifier[0] = newPinVerifier(pin);
                return true;
            }, () -> showPin("确认新密码", target, "保存密码", "两次输入的密码不一致",
                    pin -> matchesPin(pin, verifier[0]), () -> {
                        saveVerifier.accept(verifier[0]);
                        showBottomNotice("密码已更新");
                    }));
        });
    }

    private void openSavedFolder() {
        Uri folder = android.provider.DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents", "primary:Download/XiuyiGetting");
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
                showBottomNotice("无法打开文件管理器，请手动前往下载 / XiuyiGetting");
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
        scanButton.setText(xhsMode || douyinReadReplies ? "扫描评论" : "扫描主评论");
        status.setText(xhsMode ? "粘贴笔记链接，打开网页后进入评论区" : "粘贴作品链接，打开网页后进入评论区");
    }

    private void showRecordPlatforms() {
        resetPages();
        kuaishouRecordsOrigin = null;
        currentPage = -3;
        showingMy = false;
        showingReader = false;
        showingRecords = false;
        showingSettings = false;
        showingRecordPlatforms = true;
        showingFavorites = false;
        int douyinCount = 0;
        int xhsCount = 0;
        int kuaishouCount = 0;
        for (ScanRecord record : records) {
            if (record.kuaishou) kuaishouCount++;
            else if (isXhsLink(record.link)) xhsCount++;
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
        page.addView(label("选择要查看的提取记录", 13, INK2, false));
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        choices.setBackground(card(16));
        LinearLayout.LayoutParams choicesParams = new LinearLayout.LayoutParams(-1, -2);
        choicesParams.topMargin = dp(16);
        page.addView(choices, choicesParams);
        choices.addView(settingRow("抖音扫描记录", douyinCount + " 条", true, v -> {
            kuaishouRecords = false;
            xhsMode = false;
            showRecords();
        }));
        choices.addView(rowDivider());
        choices.addView(settingRow("小红书扫描记录", xhsCount + " 条", true, v -> {
            kuaishouRecords = false;
            xhsMode = true;
            showRecords();
        }));
        choices.addView(rowDivider());
        choices.addView(settingRow("快手正文记录", kuaishouCount + " 条", true, v -> {
            kuaishouRecords = true;
            showRecords();
        }));
        presentSecondary(root);
    }

    private void showRecords() {
        boolean fromReader = showingReader;
        resetPages();
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
        // Handle taps outside list controls so they cannot reach the reader underneath.
        root.setClickable(true);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(20), dp(6), dp(20), dp(6));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(54)));
        ImageView back = icon(R.drawable.extract_back, INK);
        back.setPadding(dp(8), dp(8), dp(8), dp(8));
        back.setContentDescription("返回");
        back.setOnClickListener(v -> leaveRecords());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView heading = homeLabel(kuaishouRecords ? "快手提取记录" : "提取记录", 18, INK, 750);
        heading.setPadding(dp(8), 0, 0, 0);
        toolbar.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView total = label("", 12, INK2, true);
        total.setGravity(Gravity.CENTER);
        toolbar.addView(total, new LinearLayout.LayoutParams(dp(66), dp(32)));

        root.setFocusableInTouchMode(true);
        String platform = kuaishouRecords ? "kuaishou" : xhsMode ? "xhs" : "douyin";
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("搜索标题或作品链接");
        search.setTextSize(13);
        search.setTextColor(INK);
        search.setHintTextColor(INK3);
        search.setBackground(shape(TRACK, 12));
        Drawable searchIcon = getDrawable(R.drawable.record_search).mutate();
        searchIcon.setTint(INK2);
        searchIcon.setBounds(0, 0, dp(18), dp(18));
        search.setCompoundDrawablesRelative(searchIcon, null, null, null);
        search.setCompoundDrawablePadding(dp(8));
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setText(recordSearches.getOrDefault(platform, ""));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(44));
        searchParams.setMargins(dp(20), dp(4), dp(20), dp(8));
        root.addView(search, searchParams);
        root.requestFocus();

        Button[] filters = new Button[kuaishouRecords ? 0 : 3];
        if (!kuaishouRecords) {
            LinearLayout filterRow = new LinearLayout(this);
            filterRow.setPadding(dp(3), dp(3), dp(3), dp(3));
            filterRow.setBackground(shape(TRACK, 12));
            LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-1, dp(38));
            filterParams.setMargins(dp(20), 0, dp(20), dp(4));
            root.addView(filterRow, filterParams);
            String[] names = {"全部", "正文", "评论"};
            for (int i = 0; i < names.length; i++) {
                filters[i] = shareButton(names[i], null);
                filterRow.addView(filters[i], new LinearLayout.LayoutParams(0, -1, 1));
            }
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(6), dp(16), dp(28));
        scroll.addView(page);
        refreshRecordList = () -> {
            if (showingRecords && page.isAttachedToWindow()) {
                int position = scroll.getScrollY();
                addRecordCards(page, total, search, recordTypes.getOrDefault(platform, 0));
                scroll.post(() -> scroll.scrollTo(0, position));
            }
        };
        Runnable renderRecords = () -> {
            int type = recordTypes.getOrDefault(platform, 0);
            for (int i = 0; i < filters.length; i++) {
                filters[i].setBackground(i == type ? shape(ACCENT, 9) : null);
                filters[i].setTextColor(i == type ? Color.WHITE : INK2);
                filters[i].setSelected(i == type);
            }
            addRecordCards(page, total, search, type);
        };
        for (int i = 0; i < filters.length; i++) {
            int target = i;
            filters[i].setOnClickListener(v -> {
                if (recordTypes.getOrDefault(platform, 0) == target) return;
                recordTypes.put(platform, target);
                renderRecords.run();
                scroll.scrollTo(0, 0);
            });
        }
        renderRecords.run();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable text) {
                recordSearches.put(platform, text.toString());
                renderRecords.run();
                scroll.scrollTo(0, 0);
            }
        });
        int position = recordScrollPositions.getOrDefault(platform, 0);
        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> recordScrollPositions.put(platform, y));
        if (position > 0) scroll.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                                 int oldLeft, int oldTop, int oldRight, int oldBottom) {
                scroll.removeOnLayoutChangeListener(this);
                scroll.scrollTo(0, position);
            }
        });
        presentSecondary(root, motionEnabled && !fromReader);
    }

    private void leaveRecords() {
        if (recordsFromSettings) showRecordPlatforms();
        else if (kuaishouRecords) {
            if (kuaishouRecordsOrigin != null) kuaishouRecordsOrigin.restoreKuaishou();
            else showKuaishou();
        }
        else if (viewingSavedRecord) selectMode(xhsMode);
        else showReader();
    }

    private boolean matchesRecordPlatform(ScanRecord record) {
        return kuaishouRecords ? record.kuaishou : !record.kuaishou && isXhsLink(record.link) == xhsMode;
    }

    private void addRecordCards(LinearLayout page, TextView total, EditText search, int filter) {
        page.removeAllViews();
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        ArrayList<ScanRecord> visibleRecords = new ArrayList<>();
        for (ScanRecord record : records) {
            boolean post = record.kuaishou || record.workMedia;
            if (!matchesRecordPlatform(record) || (filter != 0 && post != (filter == 1))) continue;
            String platform = record.kuaishou ? "快手" : isXhsLink(record.link) ? "小红书" : "抖音";
            String type = record.kuaishou || record.workMedia ? "作品正文 提取作品" : "评论扫描 扫描评论";
            String searchable = (record.caption + " " + record.link + " " + platform + " " + type).toLowerCase(Locale.ROOT);
            if (java.util.Arrays.stream(query.split("\\s+")).allMatch(searchable::contains)) visibleRecords.add(record);
        }
        total.setText("共 " + visibleRecords.size() + " 条");
        if (visibleRecords.isEmpty()) {
            TextView empty = label((!query.isEmpty() || filter != 0) ? "没有符合条件的记录。" : kuaishouRecords ? "读取过的快手作品会显示在这里。" : "扫描过的作品会暂存在这里，方便随时查看和删除。", 12, INK3, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(26), dp(18), dp(26));
            empty.setBackground(card(16));
            page.addView(empty);
            return;
        }
        SimpleDateFormat date = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        int coverWidth = Math.min(92, Math.max(68, getResources().getConfiguration().screenWidthDp - 268));
        for (ScanRecord record : visibleRecords) {
            boolean xhsRecord = isXhsLink(record.link);
            Media cover;
            synchronized (record) { cover = record.items.isEmpty() ? null : record.items.get(0); }
            LinearLayout card = new LinearLayout(this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setMinimumHeight(dp(128));
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            card.setBackground(card(16));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = dp(10);
            page.addView(card, cardParams);

            ImageView icon = new ImageView(this);
            icon.setImageResource(record.kuaishou ? R.drawable.home_kuaishou : xhsRecord ? R.drawable.home_xiaohongshu : R.drawable.home_douyin);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setBackground(shape(ACCENT_SOFT, 9));
            icon.setClipToOutline(true);
            card.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
            if (cover != null) {
                ImageView preview = recordCover(cover);
                LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(dp(coverWidth), dp(102));
                coverParams.leftMargin = dp(10);
                card.addView(preview, coverParams);
            }

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1);
            infoParams.leftMargin = dp(11);
            card.addView(info, infoParams);
            LinearLayout heading = new LinearLayout(this);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            info.addView(heading, new LinearLayout.LayoutParams(-1, -2));
            String shortLink = record.link.replaceFirst("^https?://", "").replaceFirst("\\?.*$", "").replaceFirst("/$", "");
            if (shortLink.length() > 24) shortLink = shortLink.substring(0, 23) + "…";
            TextView title = homeLabel(!record.caption.isEmpty() ? record.caption : shortLink, 14, INK, 750);
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

            int count;
            boolean hasFailures;
            synchronized (record) {
                count = record.items.size();
                hasFailures = !record.postFailures.isEmpty() || !record.failedMedia.isEmpty() || !record.failedReplies.isEmpty();
            }
            String progress = xhsRecord || record.includeReplies
                    ? "主 " + record.mainScanned + " · 回 " + record.replyScanned
                    : record.scanned + " 条评论";
            if (xhsRecord && record.scanned > 0 && record.mainScanned + record.replyScanned == 0)
                progress = record.scanned + " 条评论";
            String type = record.kuaishou || record.workMedia ? "正文" : "评论";
            String detail = ("legacy".equals(record.id) ? "旧版缓存" : type) + " · " + count + " 项";
            if ("评论".equals(type) && !"legacy".equals(record.id)) detail += " · " + progress;
            if (!"legacy".equals(record.id)) detail += " · " + date.format(record.createdAt);
            TextView sub = homeLabel(detail, 12, INK2, 450);
            sub.setSingleLine(true);
            sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
            subParams.topMargin = dp(2);
            info.addView(sub, subParams);

            TextView state = homeLabel(hasFailures ? "有失败" : record.complete ? "已完成" : "暂存中",
                    11, hasFailures ? DANGER : record.complete ? ACCENT : INK2, 650);
            state.setBackground(shape(hasFailures ? (darkMode ? 0xff422b3b : 0xffffedf0) : ACCENT_SOFT, 12));
            state.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(dp(51), dp(25));
            stateParams.leftMargin = dp(6);
            heading.addView(state, stateParams);

            LinearLayout actions = new LinearLayout(this);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            info.addView(actions);
            Button preview = shareButton("查看预览", v -> showRecord(record));
            preview.setTextSize(12);
            preview.setTypeface(homeTypeface(650));
            preview.setBackground(new android.graphics.drawable.InsetDrawable(shape(ACCENT_SOFT, 16), 0, dp(7), 0, dp(7)));
            preview.setPadding(0, 0, 0, 0);
            preview.setIncludeFontPadding(false);
            preview.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            preview.setGravity(Gravity.CENTER);
            actions.addView(preview, new LinearLayout.LayoutParams(dp(88), dp(44)));
            TextView more = homeLabel("⋯", 20, INK, 750);
            more.setGravity(Gravity.CENTER);
            more.setContentDescription("更多记录操作");
            actions.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));
            more.setOnClickListener(v -> showRecordActions(record, v));

            card.setOnClickListener(v -> showRecord(record));
        }
    }

    private PopupWindow recordPopup(LinearLayout panel, int width) {
        panel.setPadding(dp(14), dp(12), dp(14), dp(12));
        int available = getResources().getDisplayMetrics().widthPixels - dp(24);
        PopupWindow popup = new PopupWindow(panel, Math.min(dp(width), available), -2, true);
        popup.setBackgroundDrawable(card(16));
        popup.setElevation(dp(10));
        popup.setOutsideTouchable(true);
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        return popup;
    }

    private void showRecordActions(ScanRecord record, View anchor) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        PopupWindow popup = recordPopup(panel, 186);
        panel.setPadding(dp(6), dp(6), dp(6), dp(6));
        if (record.link.startsWith("http")) {
            addRecordAction(panel, "复制链接", R.drawable.extract_copy, INK, () -> {
                popup.dismiss();
                showBottomNotice(copyLink(record.link) ? "链接已复制" : "复制失败：剪贴板不可用");
            });
            View divider = new View(this);
            divider.setBackgroundColor(LINE);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
            dividerParams.setMargins(dp(10), 0, dp(10), 0);
            panel.addView(divider, dividerParams);
        }
        addRecordAction(panel, "删除记录", R.drawable.dialog_trash, DANGER, () -> {
            popup.dismiss();
            confirmDelete(record);
        });
        showRecordPopup(popup, anchor);
    }

    private void addRecordAction(LinearLayout panel, String title, int iconRes, int color, Runnable action) {
        TextView row = homeLabel(title, 14, color, 650);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(12), 0);
        Drawable icon = getDrawable(iconRes).mutate();
        icon.setTint(color == DANGER ? DANGER : ACCENT);
        icon.setBounds(0, 0, dp(19), dp(19));
        row.setCompoundDrawablesRelative(icon, null, null, null);
        row.setCompoundDrawablePadding(dp(10));
        row.setOnClickListener(v -> action.run());
        pressFeedback(row);
        panel.addView(row, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    private void showRecordPopup(PopupWindow popup, View anchor) {
        View menu = popup.getContentView();
        menu.measure(View.MeasureSpec.makeMeasureSpec(popup.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = menu.getMeasuredHeight();
        popup.setHeight(height);
        Rect visible = new Rect();
        anchor.getWindowVisibleDisplayFrame(visible);
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int gap = dp(6);
        int below = visible.bottom - location[1] - anchor.getHeight();
        int above = location[1] - visible.top;
        int y = below < height + gap && above > below ? -anchor.getHeight() - height - gap : gap;
        int left = location[0] + (anchor.getWidth() - popup.getWidth()) / 2;
        left = Math.max(visible.left, Math.min(left, visible.right - popup.getWidth()));
        popup.showAsDropDown(anchor, left - location[0], y, Gravity.LEFT);
    }

    private ImageView recordCover(Media item) {
        ImageView preview = new ImageView(this) {
            private boolean requested;

            @Override protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                if (requested || getDrawable() != null || !getLocalVisibleRect(new android.graphics.Rect())) return;
                requested = true;
                worker.execute(() -> {
                    Drawable thumbnail = decodeThumbnail(item.file);
                    runOnUiThread(() -> {
                        if (item.thumbnail == null) item.thumbnail = thumbnail;
                        if (isAttachedToWindow()) setImageDrawable(thumbnail);
                    });
                });
            }
        };
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(shape(TRACK, 8));
        preview.setClipToOutline(true);
        if (item.thumbnail != null) {
            Drawable.ConstantState state = item.thumbnail.getConstantState();
            if (state != null) preview.setImageDrawable(state.newDrawable(getResources()));
        }
        return preview;
    }

    private void showRecord(ScanRecord record) {
        if (record.kuaishou) {
            showKuaishou(record);
            return;
        }
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
            scanButton.setText(record.workMedia ? "扫描评论" : canResume(record) ? "继续扫描" : "重新扫描");
            status.setText(record.workMedia ? "已打开作品正文记录" : canResume(record) ? "已打开本机暂存记录，可继续扫描"
                    : "已打开本机暂存记录；重新扫描会生成新记录");
        }
        showReader();
        renderResults();
        updateResultLabels();
        readerScroll.post(() -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()));
    }

    private boolean canResume(ScanRecord record) {
        return record != null && !record.kuaishou && !record.workMedia && !record.complete && extractLink(record.link, isXhsLink(record.link)) != null;
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
            android.view.ViewPropertyAnimator motion = content.animate().alpha(0f);
            if (dialog.getWindow().getAttributes().gravity == Gravity.BOTTOM) motion.translationY(dp(32));
            else motion.scaleX(0.97f).scaleY(0.97f);
            motion.setDuration(140).withEndAction(() -> {
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
        showCardDialog(dialog, panel, 320, 48);
    }

    private void showCardDialog(android.app.Dialog dialog, View panel, int maxWidth, int horizontalMargin) {
        showCardDialog(dialog, panel, maxWidth, horizontalMargin, motionEnabled);
    }

    private void showCardDialog(android.app.Dialog dialog, View panel, int maxWidth, int horizontalMargin, boolean animate) {
        if (isFinishing() || isDestroyed()) return;
        dialog.setContentView(panel);
        android.view.Window window = dialog.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(darkMode ? 0.55f : 0.28f);
        window.setGravity(Gravity.CENTER);
        window.setWindowAnimations(0);
        dialog.show();
        window.setLayout(Math.min(dp(maxWidth), getResources().getDisplayMetrics().widthPixels - dp(horizontalMargin)), -2);
        if (animate) {
            panel.setAlpha(0f);
            panel.setScaleX(0.97f);
            panel.setScaleY(0.97f);
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
        }
    }

    private LinearLayout sheetPanel(String title, String detail) {
        LinearLayout panel = dialogPanel(0, false, title, detail);
        panel.setPadding(dp(18), dp(10), dp(18), dp(12));
        GradientDrawable background = shape(SURFACE, 0);
        float corner = dp(24);
        background.setCornerRadii(new float[]{corner, corner, corner, corner, 0, 0, 0, 0});
        panel.setBackground(background);
        View handle = new View(this);
        handle.setBackground(shape(INK3, 3));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(34), dp(4));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = dp(14);
        panel.addView(handle, 0, params);
        return panel;
    }

    private void addSheetChoice(LinearLayout panel, String title, String detail, int iconRes, boolean highlighted, Runnable action) {
        addRetryChoice(panel, title, detail, iconRes, action);
        LinearLayout row = (LinearLayout) panel.getChildAt(panel.getChildCount() - 1);
        row.setBackground(highlighted ? shape(ACCENT_SOFT, 13) : card(13));
        row.getChildAt(row.getChildCount() - 1).setRotation(-90);
    }

    private void showBottomSheet(android.app.Dialog dialog, LinearLayout panel) {
        TextView cancel = homeLabel("取消", 14, INK2, 650);
        cancel.setGravity(Gravity.CENTER);
        cancel.setOnClickListener(v -> dismissCardDialog(dialog, null));
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, dp(48));
        cancelParams.topMargin = dp(8);
        panel.addView(cancel, cancelParams);
        presentBottomSheet(dialog, panel);
    }

    private void presentBottomSheet(android.app.Dialog dialog, LinearLayout panel) {
        if (isFinishing() || isDestroyed()) return;
        dialog.setContentView(panel);
        android.view.Window window = dialog.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(darkMode ? 0.55f : 0.42f);
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(0);
        dialog.show();
        window.setLayout(-1, -2);
        if (motionEnabled) {
            panel.setTranslationY(dp(48));
            panel.setAlpha(0f);
            panel.animate().translationY(0).alpha(1f).setDuration(200).start();
        }
    }

    private void confirmDelete(ScanRecord record) {
        showConfirmation(0, true, "删除这条记录？", "记录和暂存素材将在回收站保留 7 天，"
                        + RECORD_UNDO_MS / 1000 + " 秒内可撤销。已保存的文件仍在下载目录。",
                "保留记录", "删除记录", null, () -> deleteWithUndo(record),
                record.caption.isEmpty() ? null : record.caption);
    }

    private void deleteWithUndo(ScanRecord record) {
        if (recordTransferBusy) {
            showTopNotice("请等备份操作完成后再删除");
            return;
        }
        if (!records.remove(record)) return;
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
        refreshRecordList.run();
        worker.execute(() -> {
            RecycleBin.Entry staged = stageRecordDeletion(record);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (staged == null) {
                    restoreRecordListEntry(record);
                    showMessage(R.drawable.dialog_trash, "记录未能删除", "暂存目录无法移动，请稍后重试。");
                    return;
                }
                rememberDeletedRecordSources(record);
                trackRecordDeletion(new PendingRecordDeletion(record, staged));
                refreshRecordUndoNotice();
            });
        });
    }

    private void restoreRecordListEntry(ScanRecord record) {
        records.add(record);
        records.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
        refreshRecordList.run();
    }

    private void trackRecordDeletion(PendingRecordDeletion pending) {
        pendingRecordDeletions.add(pending);
        long remaining = Math.max(0, pending.expiresAt - android.os.SystemClock.elapsedRealtime());
        stripHandler.postDelayed(() -> {
            if (pendingRecordDeletions.remove(pending)) refreshRecordUndoNotice();
        }, remaining);
    }

    private void refreshRecordUndoNotice() {
        if (recordUndoPopup != null) {
            recordUndoPopup.dismiss();
            recordUndoPopup = null;
        }
        int count = pendingRecordDeletions.size();
        if (count == 0) return;
        long end = pendingRecordDeletions.get(count - 1).expiresAt;
        long remaining = end - android.os.SystemClock.elapsedRealtime();
        if (remaining <= 0) return;
        showNotice(count == 1 ? "记录已删除" : "已删除 " + count + " 条记录", "撤销",
                this::undoRecordDeletions, true, remaining);
    }

    private void undoRecordDeletions() {
        ArrayList<PendingRecordDeletion> pending = new ArrayList<>(pendingRecordDeletions);
        pendingRecordDeletions.clear();
        long requestedAt = android.os.SystemClock.elapsedRealtime();
        worker.execute(() -> {
            ArrayList<ScanRecord> restored = new ArrayList<>();
            for (PendingRecordDeletion deletion : pending) {
                if (requestedAt >= deletion.expiresAt) continue;
                try {
                    recycleBin.restore(deletion.entry, deletion.record.folder);
                    restored.add(deletion.record);
                } catch (java.io.IOException ignored) { }
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                records.addAll(restored);
                records.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                refreshRecordList.run();
                if (!restored.isEmpty()) {
                    showBottomNotice("已恢复 " + restored.size() + " 条记录和暂存素材");
                } else {
                    showBottomNotice("未能撤销删除");
                }
            });
        });
    }

    private void showReader() {
        showingBatch = false;
        showingReader = true;
        showingSettings = false;
        showingFavorites = false;
        showingMy = false;
        currentPage = -1;
        showingRecords = false;
        showingRecordPlatforms = false;
        boolean batchReader = batchTask != null;
        readerRecordsButton.setText(batchReader || previewingBatchRecord ? "队列" : "记录");
        readerLinkInput.setVisibility(batchReader ? View.GONE : View.VISIBLE);
        readerOpenButton.setVisibility(batchReader ? View.GONE : View.VISIBLE);
        scanButton.setVisibility(!batchReader || batchTask.comments ? View.VISIBLE : View.GONE);
        ((View) scanButton.getParent()).setVisibility(!batchReader || batchTask.comments ? View.VISIBLE : View.GONE);
        extractWorkButton.setVisibility(batchReader && batchTask.comments ? View.GONE : View.VISIBLE);
        phoneImportButton.setVisibility(batchReader ? View.GONE : View.VISIBLE);
        presentSecondary(readerRoot);
        updateScanCard();
        for (Animatable animation : animations) animation.start();
    }

    private LinearLayout readerLinkRow(EditText input, java.util.function.Supplier<String> copyValue) {
        LinearLayout urlBox = new LinearLayout(this);
        urlBox.setGravity(Gravity.CENTER_VERTICAL);
        urlBox.setPadding(dp(10), 0, dp(6), 0);
        urlBox.setBackground(outlined(12, SURFACE));
        urlBox.addView(icon(R.drawable.extract_link, ACCENT), new LinearLayout.LayoutParams(dp(19), dp(19)));
        input.setSingleLine(true);
        input.setTextSize(12);
        input.setTypeface(homeTypeface(450));
        input.setTextColor(INK);
        input.setHintTextColor(INK3);
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
            String link = copyValue.get();
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
        return urlBox;
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
        readerRecordsButton = recordsButton;
        recordsButton.setSingleLine(true);
        recordsButton.setMinWidth(dp(72));
        readerIcon(recordsButton, R.drawable.extract_history, 17);
        recordsButton.setGravity(Gravity.CENTER);
        recordsButton.setOnClickListener(v -> {
            if (returnToBatch()) return;
            recordsFromSettings = false;
            kuaishouRecords = false;
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
        input = new EditText(this);
        input.setHint("粘贴抖音作品链接或分享文案");
        readerLinkInput = readerLinkRow(input, () -> {
            String link = extractLink(input.getText().toString(), xhsMode);
            return link == null && input.getText().length() == 0 ? activeLink : link;
        });
        inputCard.addView(readerLinkInput, new LinearLayout.LayoutParams(-1, dp(46)));
        Button open = new Button(this);
        readerOpenButton = open;
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
        scanButton = new Button(this);
        scanButton.setAllCaps(false);
        scanButton.setText("扫描评论");
        scanButton.setTextColor(ACCENT);
        scanButton.setTextSize(14);
        scanButton.setTypeface(homeTypeface(700));
        readerIcon(scanButton, R.drawable.extract_scan, 20);
        scanButton.setBackground(outlined(12, SURFACE));
        scanButton.setMinHeight(0);
        scanButton.setStateListAnimator(null);
        scanButton.setOnClickListener(v -> {
            if (!extractWorkButton.isEnabled()) {
                showTopNotice("正在提取作品，请稍候");
                return;
            }
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
                    record.caption = currentNoteTitle(page);
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
                batchCommentRecord(currentRecord);
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
        extractWorkButton = shareButton("提取作品", v -> startWorkExtraction());
        extractWorkButton.setTextSize(14);
        extractWorkButton.setTypeface(homeTypeface(700));
        extractWorkButton.setTextColor(Color.WHITE);
        extractWorkButton.setBackground(shape(ACCENT, 12));
        readerIcon(extractWorkButton, R.drawable.extract_download, 18);
        LinearLayout.LayoutParams extractParams = new LinearLayout.LayoutParams(-1, dp(40));
        extractParams.topMargin = dp(8);
        inputCard.addView(extractWorkButton, extractParams);


        resultAnchor = homeLabel("提取结果", 18, INK, 750);
        LinearLayout resultHeader = new LinearLayout(this);
        resultHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams resultHeadParams = new LinearLayout.LayoutParams(-1, -2);
        resultHeadParams.topMargin = dp(22);
        content.addView(resultHeader, resultHeadParams);
        resultHeader.addView(resultAnchor, new LinearLayout.LayoutParams(0, -2, 1));
        resultSummary = label("提取后自动显示图片与动图预览", 12, INK3, false);
        resultSummary.setVisibility(View.GONE);
        readerSelectAll = shareButton("全选", v -> {
            toggleVisibleSelection(media, selectedMedia, this::matchesFilter);
            renderResults();
            updateResultLabels();
        });
        readerSelectAll.setVisibility(View.GONE);
        LinearLayout.LayoutParams selectAllParams = new LinearLayout.LayoutParams(dp(76), dp(36));
        selectAllParams.rightMargin = dp(6);
        resultHeader.addView(readerSelectAll, selectAllParams);
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
        phoneImportButton = addPhoneImportButton(content);
        readerPostStatus = new PostStatusStrip();
        content.addView(readerPostStatus, postStatusParams());
        readerPostStatus.retry.setOnClickListener(v -> {
            ScanRecord record = currentRecord;
            if (record == null || !record.workMedia || record.processingPage) return;
            retryPost(record, () -> {
                if (currentRecord != record || !showingReader) return;
                clearMediaSelection();
                media.clear();
                synchronized (record) { media.addAll(record.items); }
                renderResults();
                updateResultLabels();
                status.setText("作品正文已读取");
            });
            readerPostStatus.update(record);
        });

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
        saveSub = homeLabel("保存到 下载 / XiuyiGetting", 10, INK2, 400);
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
            saveMedia(snapshot, currentRecord, null, null);
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
            scanButton.setTextColor(scanning ? Color.WHITE : ACCENT);
            scanButton.setBackground(scanning ? shape(ACCENT, 12) : outlined(12, SURFACE));
            drawable.setTint(scanButton.getCurrentTextColor());
            drawable.setBounds(0, 0, dp(20), dp(20));
            scanButton.setCompoundDrawablesRelative(drawable, null, null, null);
            scanButton.setTag(scanning);
        }
        ScanRecord record = currentRecord;
        boolean showCommentDetails = record != null && !record.workMedia;
        if (readerPostStatus != null) readerPostStatus.update(record != null && record.workMedia ? record : null);
        scanDetails.setVisibility(showCommentDetails ? View.VISIBLE : View.GONE);
        scanCountLine.setVisibility(showCommentDetails ? View.VISIBLE : View.GONE);
        if (!showCommentDetails) return;
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
        if (!detail.isEmpty()) words.addView(homeLabel(detail, 11, INK2, 400));
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
                    if (download(record, item.source, item.commentId, item.kind, item.videoUrls)) success++;
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
        if (batchTask != null && batchTask.comments) batchNeedsAction(reason);
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

    private String readScriptAsset(String name) throws java.io.IOException {
        try (InputStream source = getAssets().open(name); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = source.read(chunk)) != -1) buffer.write(chunk, 0, count);
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
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
        try {
            xhsCaptureScript = readScriptAsset("xhs-capture.js");
            xhsCaptureSupported = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);
            if (xhsCaptureSupported) WebViewCompat.addDocumentStartJavaScript(browser, xhsCaptureScript,
                    Collections.singleton("https://www.xiaohongshu.com"));
        } catch (Exception error) {
            xhsCaptureSupported = false;
        }
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(browser, readScriptAsset("post-media-capture.js"),
                        Collections.singleton("https://www.douyin.com"));
            }
        } catch (Exception ignored) { }
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
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                finishedWorkPage = null;
            }

            @Override public void onPageFinished(WebView view, String url) {
                if (url.equals(view.getUrl())) finishedWorkPage = url;
                if ("about:blank".equals(url)) return;
                boolean resumedPage = viewingSavedRecord && loadingSavedRecordPage && url.equals(view.getUrl())
                        && (xhsMode ? url.startsWith("https://www.xiaohongshu.com/") : url.startsWith("https://www.douyin.com/"));
                if (resumedPage) {
                    loadingSavedRecordPage = false;
                }
                if (batchTask != null && !batchTask.comments) {
                    if (batchTask.state == BatchExtraction.State.RUNNING) {
                        status.setText(url.contains("/login") ? "请在网页中完成登录" : "正在读取作品正文");
                    }
                    return;
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
                record.kuaishou = "kuaishou".equals(data.optString("platform"));
                record.workMedia = data.optBoolean("workMedia");
                record.caption = data.optString("caption");
                record.postTotal = Math.max(0, data.optInt("postTotal"));
                JSONArray postFailures = data.optJSONArray("postFailures");
                if (record.kuaishou || record.workMedia) {
                    for (int i = 0; postFailures != null && i < postFailures.length(); i++) {
                        JSONObject failure = postFailures.optJSONObject(i);
                        JSONArray urls = failure == null ? null : failure.optJSONArray("urls");
                        if (urls != null) record.postFailures.add(new PostFailure(urls, failure.optBoolean("video")));
                    }
                }
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
                    record.failedMedia.put(source, new FailedMedia(source, failure.optString("commentId"),
                            failure.optString("kind"), failure.optJSONArray("videoUrls")));
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
        if ((included == null || included.contains("legacy")) && oldFiles != null && oldFiles.length > 0) {
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
            if (record.kuaishou) {
                data.put("platform", "kuaishou");
            }
            if (record.workMedia) data.put("workMedia", true);
            if (!record.caption.isEmpty()) data.put("caption", record.caption);
            if (record.kuaishou || record.workMedia) {
                data.put("postTotal", record.postTotal);
                JSONArray pending = new JSONArray();
                synchronized (record) {
                    for (PostFailure failure : record.postFailures) {
                        pending.put(new JSONObject().put("video", failure.video).put("urls", failure.urls));
                    }
                }
                data.put("postFailures", pending);
            }
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
                    if (item.videoUrls != null) entry.put("videoUrls", item.videoUrls);
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

    private boolean isOwnedRecordFolder(ScanRecord record) throws java.io.IOException {
        File folder = record.folder.getCanonicalFile();
        return recordsFolder().getCanonicalFile().equals(folder.getParentFile())
                || ("legacy".equals(record.id) && folder.equals(new File(getFilesDir(), "media").getCanonicalFile()));
    }

    private boolean deleteFlatFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) for (File file : files) {
            if (!file.isFile() || !file.delete()) return false;
        }
        return !folder.exists() || folder.delete();
    }

    private boolean deleteRecordFiles(ScanRecord record) {
        try { return isOwnedRecordFolder(record) && deleteFlatFolder(record.folder); }
        catch (java.io.IOException error) { return false; }
    }

    private RecycleBin.Entry stageRecordDeletion(ScanRecord record) {
        try {
            if (!isOwnedRecordFolder(record)) return null;
            return recycleBin.stage(record.folder, new JSONObject()
                    .put("record", true).put("legacy", "legacy".equals(record.id))
                    .put("platform", "legacy".equals(record.id) ? "" : record.kuaishou ? "快手" : isXhsLink(record.link) ? "小红书" : "抖音")
                    .put("title", record.caption.isEmpty() ? record.link : record.caption));
        } catch (java.io.IOException | org.json.JSONException error) {
            return null;
        }
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

    private void startWorkExtraction() {
        if (scanning || currentRecord != null && currentRecord.processingPage) {
            showTopNotice("请暂停扫描并等待当前操作完成");
            return;
        }
        String link = extractLink(input.getText().toString(), xhsMode);
        if (!WorkMedia.isShareLink(link, xhsMode)) {
            batchNeedsAction("作品链接无法打开");
            showTopNotice(xhsMode ? "请粘贴小红书笔记链接" : "请粘贴抖音作品链接");
            return;
        }
        if (workMediaScript == null) {
            try {
                workMediaScript = readScriptAsset("post-media.js");
            } catch (Exception error) {
                batchNeedsAction("作品提取组件加载失败");
                showTopNotice("作品提取组件加载失败");
                return;
            }
        }
        if (!link.equals(activeLink) || WorkMedia.pageId(browser.getUrl(), xhsMode) == null) openLink();
        if (batchTask != null && !batchTask.comments && batchTask.link.equals(link)) {
            batchTask.state = BatchExtraction.State.RUNNING;
            batchTask.detail = "";
            saveBatch();
            status.setText("正在提取批量队列中的作品");
        }
        extractWorkButton.setEnabled(false);
        extractWorkButton.setText("正在读取作品…");
        pollWorkMedia(link, xhsMode, requestToken, android.os.SystemClock.uptimeMillis() + 40000);
    }

    private boolean isCurrentWork(String link, int token) {
        return showingReader && requestToken == token && link.equals(activeLink)
                && link.equals(extractLink(input.getText().toString(), xhsMode));
    }

    private String currentNoteTitle(String page) {
        String title = WorkMedia.pageId(page, xhsMode) == null ? null : browser.getTitle();
        return title == null ? "" : title.replaceFirst("\\s*-\\s*(?:抖音|小红书)\\s*$", "").trim();
    }

    private void pollWorkMedia(String link, boolean xhs, int token, long deadline) {
        if (!isCurrentWork(link, token)) {
            finishWorkExtraction();
            if (batchWorkMatches(link, token)) batchNeedsAction("作品提取已中断，可以重试");
            return;
        }
        String page = browser.getUrl();
        if (xhs && page != null && page.equals(finishedWorkPage)
                && page.startsWith("https://www.xiaohongshu.com/login")) {
            finishWorkExtraction();
            if (batchWorkMatches(link, token)) {
                batchNeedsAction("请先在网页中登录，再点「提取作品」");
                status.setText(batchTask.detail);
            }
            showTopNotice("请先在小红书网页中登录，再打开笔记提取作品");
            return;
        }
        String pageId = WorkMedia.pageId(page, xhs);
        String linkId = WorkMedia.pageId(link, xhs);
        if (pageId != null && (linkId == null || linkId.equals(pageId)) && page.equals(finishedWorkPage)) {
            browser.evaluateJavascript(workMediaScript, value -> {
                if (!isCurrentWork(link, token)) {
                    finishWorkExtraction();
                    if (batchWorkMatches(link, token)) batchNeedsAction("作品提取已中断，可以重试");
                    return;
                }
                try {
                    JSONObject data = new JSONObject(value);
                    JSONArray images = data.getJSONArray("images");
                    JSONArray videos = data.getJSONArray("videos");
                    JSONArray liveVideos = data.getJSONArray("liveVideos");
                    if (page.equals(browser.getUrl()) && pageId.equals(data.optString("id"))
                            && (images.length() > 0 || videos.length() > 0 || liveVideos.length() > 0)) {
                        if (!xhs && images.length() > 0 && (liveVideos.length() == 0 || data.optBoolean("awaitingLive"))
                                && deadline - android.os.SystemClock.uptimeMillis() > 36000) {
                            waitForWorkMedia(link, xhs, token, deadline);
                            return;
                        }
                        extractWorkButton.setText("正在保存作品…");
                        String title = data.optString("title");
                        worker.execute(() -> saveWorkMedia(link, token, title, images, videos, liveVideos));
                        return;
                    }
                } catch (org.json.JSONException ignored) { }
                waitForWorkMedia(link, xhs, token, deadline);
            });
        } else waitForWorkMedia(link, xhs, token, deadline);
    }

    private void waitForWorkMedia(String link, boolean xhs, int token, long deadline) {
        if (android.os.SystemClock.uptimeMillis() >= deadline) {
            finishWorkExtraction();
            if (batchWorkMatches(link, token)) {
                batchNeedsAction("网页未返回素材。若要求登录或验证，完成后再点「提取作品」");
                status.setText(batchTask.detail);
            }
            showTopNotice("网页未提供可读取的作品素材。若页面要求登录或验证，请完成后重试");
        } else readerRoot.postDelayed(() -> pollWorkMedia(link, xhs, token, deadline), 800);
    }

    private void finishWorkExtraction() {
        extractWorkButton.setEnabled(true);
        extractWorkButton.setText("提取作品");
    }

    private boolean batchWorkMatches(String link, int token) {
        return batchTask != null && !batchTask.comments && batchTask.platform != 2
                && batchTask.link.equals(link) && requestToken == token;
    }

    private LinearLayout.LayoutParams postStatusParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(8);
        return params;
    }

    private class PostStatusStrip extends LinearLayout {
        private final TextView summary;
        final TextView retry;

        PostStatusStrip() {
            super(MainActivity.this);
            setGravity(Gravity.CENTER_VERTICAL);
            setMinimumHeight(dp(40));
            setPadding(dp(12), 0, dp(8), 0);
            setBackground(shape(ACCENT_SOFT, 10));
            summary = homeLabel("", 11, INK, 550);
            addView(summary, new LinearLayout.LayoutParams(0, -2, 1));
            retry = homeLabel("重试失败项", 12, ACCENT, 700);
            retry.setGravity(Gravity.CENTER);
            retry.setPadding(dp(8), 0, dp(4), 0);
            retry.setMinHeight(dp(44));
            addView(retry, new LinearLayout.LayoutParams(-2, -2));
            setVisibility(View.GONE);
        }

        void update(ScanRecord record) {
            setVisibility(record == null ? View.GONE : View.VISIBLE);
            if (record == null) return;
            synchronized (record) {
                summary.setText(record.processingPage ? "正在重试失败素材…" : postProgress(record));
                retry.setVisibility(!record.processingPage && !record.postFailures.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }
    }

    private String postProgress(ScanRecord record) {
        synchronized (record) {
            if (record.postTotal == 0) return "已提取 " + record.items.size() + " 个作品素材";
            int failed = record.postFailures.size();
            return "已发现 " + record.postTotal + " 项 · 成功 " + (record.postTotal - failed) + " · 失败 " + failed;
        }
    }

    private boolean downloadPostFailures(ScanRecord record) {
        ArrayList<PostFailure> pending;
        synchronized (record) { pending = new ArrayList<>(record.postFailures); }
        ArrayList<PostFailure> remaining = new ArrayList<>();
        ArrayList<Media> downloaded = new ArrayList<>();
        for (PostFailure failure : pending) {
            Media media = null;
            for (int i = 0; i < failure.urls.length(); i++) {
                try {
                    URL url = new URL(failure.urls.getString(i));
                    media = failure.video ? readPostVideo(url, record) : readPostImage(url, record);
                    break;
                } catch (Exception ignored) { }
            }
            if (media == null) remaining.add(failure);
            else downloaded.add(media);
        }
        synchronized (record) {
            boolean previouslyComplete = record.complete;
            ArrayList<Media> added = new ArrayList<>();
            for (Media item : downloaded) {
                if (record.hashes.add(item.hash)) added.add(item);
            }
            record.items.addAll(added);
            record.postFailures.clear();
            record.postFailures.addAll(remaining);
            record.complete = remaining.isEmpty();
            if (persistRecord(record)) return true;
            record.complete = previouslyComplete;
            record.postFailures.clear();
            record.postFailures.addAll(pending);
            for (Media item : added) {
                record.items.remove(item);
                record.hashes.remove(item.hash);
                item.file.delete();
            }
            return false;
        }
    }

    private void retryPost(ScanRecord record, Runnable onFinished) {
        synchronized (record) {
            if (record.processingPage || record.postFailures.isEmpty()) return;
            record.processingPage = true;
        }
        worker.execute(() -> {
            boolean stored = downloadPostFailures(record);
            runOnUiThread(() -> {
                record.processingPage = false;
                onFinished.run();
                if (!stored) showTopNotice("无法保存重试结果，请检查存储空间");
                else if (!record.complete) showTopNotice("仍有素材下载失败。若地址已失效，请重新提取作品");
            });
        });
    }

    private void saveWorkMedia(String link, int token, String title, JSONArray images, JSONArray videos,
                               JSONArray liveVideos) {
        String id = UUID.randomUUID().toString();
        ScanRecord record = new ScanRecord(id, link, System.currentTimeMillis(), new File(recordsFolder(), id));
        record.workMedia = true;
        record.caption = title.trim();
        for (int i = 0; i < images.length(); i++) {
            JSONArray urls = images.optJSONArray(i);
            if (urls != null) record.postFailures.add(new PostFailure(urls, false));
        }
        if (videos.length() > 0) record.postFailures.add(new PostFailure(videos, true));
        for (int i = 0; i < liveVideos.length(); i++) {
            JSONArray urls = liveVideos.optJSONArray(i);
            if (urls != null && urls.length() > 0) record.postFailures.add(new PostFailure(urls, true));
        }
        record.postTotal = record.postFailures.size();
        boolean stored = downloadPostFailures(record);
        if (!stored) deleteRecordFiles(record);
        runOnUiThread(() -> {
            finishWorkExtraction();
            if (stored) records.add(0, record);
            if (!isCurrentWork(link, token)) return;
            if (!stored) {
                if (batchWorkMatches(link, token)) batchNeedsAction("无法保存作品记录，请检查存储空间");
                showTopNotice("无法保存作品记录，请检查存储空间");
                return;
            }
            boolean batched = batchWorkMatches(link, token);
            showRecord(record);
            if (batched) finishBatchTask(record);
            else showBottomNotice(postProgress(record));
        });
    }

    private Button addPhoneImportButton(LinearLayout content) {
        Button button = shareButton("导入手机素材", v -> pickPhoneMedia());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(40));
        params.topMargin = dp(8);
        content.addView(button, params);
        return button;
    }

    private void pickPhoneMedia() {
        if (importingPhoneMedia) return;
        boolean kuaishou = showingKuaishou;
        ScanRecord target = kuaishou ? favoriteScreen.kuaishouPostRecord : currentRecord;
        if (target != null && target.processingPage
                || (kuaishou ? favoriteScreen.busy : scanning || !extractWorkButton.isEnabled())) {
            showTopNotice("请暂停扫描并等待当前操作完成");
            return;
        }
        String link = displayedPhoneWorkLink();
        if (link == null) {
            showTopNotice("请先粘贴" + (kuaishou ? "快手作品" : xhsMode ? "小红书笔记" : "抖音作品") + "链接");
            return;
        }
        if (target == null || target.kuaishou != kuaishou || !samePhoneWork(link, target.link)) {
            target = null;
            for (ScanRecord record : records) {
                if (record.kuaishou == kuaishou && samePhoneWork(link, record.link)) {
                    target = record;
                    break;
                }
            }
        }
        phoneImportLink = link;
        phoneImportRecordId = target == null ? null : target.id;
        try { openLocalMediaPicker(IMPORT_PHONE_MEDIA); }
        catch (android.content.ActivityNotFoundException error) {
            phoneImportLink = null;
            phoneImportRecordId = null;
            showTopNotice("无法打开系统文件选择器");
        }
    }

    private void openLocalMediaPicker(int requestCode) {
        if (!importFromFiles() && (android.os.Build.VERSION.SDK_INT >= 33 || android.os.Build.VERSION.SDK_INT >= 30
                && android.os.ext.SdkExtensions.getExtensionVersion(30) >= 2)) {
            Intent album = new Intent(MediaStore.ACTION_PICK_IMAGES);
            album.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, MediaStore.getPickImagesMaxLimit());
            try {
                startActivityForResult(album, requestCode);
                return;
            } catch (android.content.ActivityNotFoundException ignored) { }
        }
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/mp4"});
        picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        picker.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,
                android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:"));
        startActivityForResult(picker, requestCode);
    }

    private void pickFavoriteMedia(String group) {
        if (importingFavoriteMedia || recordTransferBusy) return;
        favoriteImportGroup = group == null ? "" : group;
        try { openLocalMediaPicker(IMPORT_FAVORITE_MEDIA); }
        catch (android.content.ActivityNotFoundException error) {
            favoriteImportGroup = null;
            showTopNotice("无法打开系统文件选择器");
        }
    }

    private String displayedPhoneWorkLink() {
        if (showingKuaishou) return KuaishouPost.extractLink(favoriteScreen.kuaishouLink.getText().toString());
        if (!showingReader) return null;
        String link = extractLink(input.getText().toString(), xhsMode);
        return WorkMedia.isShareLink(link, xhsMode) ? link : null;
    }

    private static boolean samePhoneWork(String first, String second) {
        if (first.equals(second)) return true;
        boolean xhs = isXhsLink(first);
        String id = WorkMedia.pageId(first, xhs);
        return id != null && id.equals(WorkMedia.pageId(second, xhs));
    }

    private static ArrayList<Uri> selectedFiles(Intent data) {
        ArrayList<Uri> files = new ArrayList<>();
        ClipData clips = data.getClipData();
        if (clips != null) for (int i = 0; i < clips.getItemCount(); i++) {
            Uri uri = clips.getItemAt(i).getUri();
            if (uri != null) files.add(uri);
        }
        if (files.isEmpty() && data.getData() != null) files.add(data.getData());
        return files;
    }

    private void importPhoneMedia(String link, String recordId, ArrayList<Uri> files, boolean fromShare) {
        if (files.isEmpty() || importingPhoneMedia || recordTransferBusy) return;
        ScanRecord existing = null;
        if (recordId != null) {
            for (ScanRecord record : records) {
                if (record.id.equals(recordId)) {
                    existing = record;
                    break;
                }
            }
            if (existing == null) {
                showTopNotice("这条作品记录已不存在，请重新选择");
                return;
            }
        }
        String id = UUID.randomUUID().toString();
        boolean newRecord = existing == null;
        ScanRecord record = newRecord
                ? new ScanRecord(id, link, System.currentTimeMillis(), new File(recordsFolder(), id)) : existing;
        if (newRecord && link.equals(KuaishouPost.extractLink(link))) {
            record.kuaishou = true;
            record.complete = true;
        }
        boolean restorePage = !showingReader && !showingKuaishou;
        importingPhoneMedia = true;
        phoneImportButton.setEnabled(false);
        showBottomNotice("正在导入手机素材…");
        worker.execute(() -> {
            ArrayList<Media> added = new ArrayList<>();
            int duplicates = 0;
            int failed = 0;
            boolean stored = true;
            synchronized (record) {
                for (Uri uri : files) {
                    try {
                        Media item = importLocalMedia(uri, record.folder, record.hashes);
                        if (item == null) duplicates++;
                        else {
                            record.hashes.add(item.hash);
                            record.items.add(item);
                            added.add(item);
                        }
                    } catch (Exception error) { failed++; }
                }
                if (!added.isEmpty() && !persistRecord(record)) {
                    stored = false;
                    for (Media item : added) {
                        record.items.remove(item);
                        record.hashes.remove(item.hash);
                        item.file.delete();
                    }
                }
            }
            if (newRecord && (!stored || added.isEmpty())) deleteRecordFiles(record);
            if (stored && record.kuaishou && !added.isEmpty()) {
                synchronized (record) {
                    for (Media item : record.items) if (item.thumbnail == null) item.thumbnail = decodeThumbnail(item.file);
                }
            }
            boolean saved = stored;
            int duplicateCount = duplicates;
            int failureCount = failed;
            runOnUiThread(() -> {
                importingPhoneMedia = false;
                phoneImportButton.setEnabled(true);
                if (isFinishing() || isDestroyed()) return;
                if (!saved) {
                    showTopNotice("无法保存导入结果，请检查存储空间");
                    return;
                }
                if (!added.isEmpty()) {
                    if (newRecord) records.add(0, record);
                    String displayedLink = displayedPhoneWorkLink();
                    boolean sameWork = displayedLink != null && samePhoneWork(displayedLink, link);
                    boolean openRecord = fromShare || restorePage && currentPage == 1 || showingReader && sameWork;
                    boolean anotherScanActive = currentRecord != null && currentRecord != record
                            && (scanning || currentRecord.processingPage);
                    if (showingKuaishou && sameWork && !favoriteScreen.busy) {
                        favoriteScreen.showPhoneImport(record);
                    } else if (openRecord && !anotherScanActive) {
                        if (currentRecord == record && isLiveRecord(record)) {
                            media.addAll(added);
                            renderResults();
                            updateResultLabels();
                        } else {
                            if (record.kuaishou) kuaishouRecords = true;
                            showRecord(record);
                        }
                    }
                }
                String message = added.isEmpty() && failureCount == 0 ? "已在此作品记录中"
                        : "已导入 " + added.size() + " 个素材";
                if (duplicateCount > 0 && !added.isEmpty()) message += "，跳过 " + duplicateCount + " 个重复文件";
                if (failureCount > 0) message += "，" + failureCount + " 个文件无法导入";
                showBottomNotice(message);
            });
        });
    }

    private void importFavoriteMedia(String group, ArrayList<Uri> files) {
        if (files.isEmpty() || importingFavoriteMedia) return;
        importingFavoriteMedia = true;
        if (showingFavorites) favoriteScreen.updateActions();
        showBottomNotice("正在导入收藏素材…");
        worker.execute(() -> {
            File folder = new File(getFilesDir(), "favorites");
            HashSet<String> hashes = new HashSet<>();
            File[] existing = folder.listFiles();
            if (existing != null) for (File file : existing) {
                if (file.isFile() && !file.getName().endsWith(".tmp")) hashes.add(favoriteFileHash(file));
            }
            ArrayList<Media> added = new ArrayList<>();
            int failed = 0;
            for (Uri uri : files) {
                try {
                    Media item = importLocalMedia(uri, folder, hashes);
                    if (item != null) {
                        hashes.add(item.hash);
                        added.add(item);
                    }
                } catch (Exception error) { failed++; }
            }
            int failures = failed;
            runOnUiThread(() -> {
                importingFavoriteMedia = false;
                boolean changed = false;
                boolean sourcesChanged = false;
                for (Media item : added) {
                    changed |= assignFavoriteGroup(item, group);
                    sourcesChanged |= rememberFavoriteSource(item, null, false);
                }
                if (changed) saveFavoriteGroups();
                if (sourcesChanged) saveFavoriteSources();
                if (isFinishing() || isDestroyed()) return;
                if (showingFavorites) {
                    FavoritesScreen screen = favoriteScreen;
                    if (screen.loaded && !added.isEmpty()) screen.items.addAll(0, added);
                    if (!added.isEmpty() && (screen.activeGroup == null || screen.activeGroup.equals(group))) {
                        screen.category = 0;
                        screen.positionFilter(false);
                    }
                    screen.render();
                }
                int duplicates = files.size() - added.size() - failures;
                String result = "已导入 " + added.size() + " 个收藏素材";
                if (duplicates > 0) result += "，跳过 " + duplicates + " 个重复文件";
                if (failures > 0) result += "，" + failures + " 个文件无法导入";
                showBottomNotice(result);
            });
        });
    }

    private Media importLocalMedia(Uri uri, File folder, HashSet<String> hashes) throws Exception {
        Files.createDirectories(folder.toPath());
        File pending = File.createTempFile("phone-", ".tmp", folder);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String mime;
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new java.security.DigestOutputStream(new FileOutputStream(pending), digest)) {
                if (in == null) throw new java.io.IOException("无法打开所选文件");
                byte[] header = new byte[12];
                new java.io.DataInputStream(in).readFully(header);
                mime = format(header);
                if (mime == null || "qt  ".equals(new String(header, 8, 4, StandardCharsets.US_ASCII))) {
                    throw new java.io.IOException("文件格式不受支持");
                }
                out.write(header);
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            StringBuilder hash = new StringBuilder();
            for (byte b : digest.digest()) hash.append(String.format(Locale.ROOT, "%02x", b & 255));
            String key = hash.toString();
            if (hashes.contains(key)) return null;
            boolean video = "video/mp4".equals(mime);
            Drawable thumbnail = null;
            if (video) requireVideoTrack(pending);
            else {
                thumbnail = decodeThumbnail(pending);
                if (thumbnail == null) throw new java.io.IOException("无法读取图片");
            }
            String extension = "image/jpeg".equals(mime) ? ".jpg" : "." + mime.substring(mime.indexOf('/') + 1);
            File file = new File(folder, key + extension);
            Files.move(pending.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (video) thumbnail = decodeThumbnail(file);
            return new Media(file, key, mime, "", PHONE_MEDIA_KIND, thumbnail, video);
        } finally { pending.delete(); }
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
        finishedWorkPage = null;
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
            if (batchTask != null && batchTask.comments) batchNeedsAction("已暂停，可继续当前任务");
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
            record.caption = currentNoteTitle(page);
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
        batchCommentRecord(currentRecord);
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
                    browserPanel.setVisibility(View.VISIBLE);
                    updateWebToggle();
                    readerScroll.post(() -> readerScroll.smoothScrollTo(0, browserPanel.getTop()));
                    if (batchTask != null && batchTask.comments) {
                        scanning = false;
                        scanButton.setText("继续扫描");
                        status.setText("请在网页中打开或滑动评论区，再继续扫描");
                        recordScanPause("正在等待网页评论，可打开评论区后继续扫描", false);
                        return;
                    }
                    status.setText("正在等待后续评论；如网页不再加载，请在评论区向下滑动");
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
        if (batchTask != null && batchTask.comments && record.id.equals(batchTask.recordId)) {
            finishBatchTask(record);
            return;
        }
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
            if (image == null) continue;
            download(record, url(image.optJSONObject("origin_url")), id, reply ? "回复图片" : "图片");
            JSONArray clips = commentLiveVideoUrls(image);
            if (clips.length() > 0) {
                download(record, clips.optString(0), id, reply ? "回复实况" : "评论实况", clips);
            } else if (image.optInt("live_photo_type") == 1 || image.optInt("livePhotoType") == 1
                    || image.optInt("clip_type") == 5 || image.optInt("clipType") == 5) {
                record.addIssue("实况图（评论 ID " + id + "）：网页未返回视频地址");
            }
        }
        JSONObject sticker = comment.optJSONObject("sticker");
        if (sticker != null) {
            String source = url(sticker.optJSONObject("animate_url"));
            if (source == null) source = url(sticker.optJSONObject("static_url"));
            download(record, source, id, reply ? "回复表情" : "表情");
        }
    }

    private static JSONArray commentLiveVideoUrls(JSONObject image) {
        java.util.LinkedHashSet<String> urls = new java.util.LinkedHashSet<>();
        JSONObject livePhoto = image.optJSONObject("live_photo");
        if (livePhoto == null) livePhoto = image.optJSONObject("livePhoto");
        for (JSONObject video : new JSONObject[]{image.optJSONObject("video"), livePhoto,
                livePhoto == null ? null : livePhoto.optJSONObject("video")}) {
            if (video == null) continue;
            for (String key : new String[]{"play_addr_h264", "playAddrH264", "play_addr", "playAddr", "play_url",
                    "download_addr", "downloadAddr"}) {
                addCommentVideoUrls(urls, video.opt(key));
            }
        }
        addCommentVideoUrls(urls, image.opt("video_play_addr"));
        addCommentVideoUrls(urls, image.opt("videoPlayAddr"));
        return new JSONArray(urls);
    }

    private static void addCommentVideoUrls(java.util.Set<String> urls, Object address) {
        if (address instanceof JSONObject) {
            JSONObject object = (JSONObject) address;
            JSONArray alternatives = object.optJSONArray("url_list");
            if (alternatives == null) alternatives = object.optJSONArray("urlList");
            if (alternatives != null) {
                for (int i = 0; i < alternatives.length(); i++) addCommentVideoUrls(urls, alternatives.opt(i));
            }
            address = object.opt("url");
        }
        if (!(address instanceof String)) return;
        String link = (String) address;
        if (link.startsWith("//")) link = "https:" + link;
        if (link.startsWith("http://")) link = "https://" + link.substring(7);
        if (link.startsWith("https://")) urls.add(link);
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
        if (batchTask != null && batchTask.comments && record.id.equals(batchTask.recordId)) {
            finishBatchTask(record);
            return;
        }
        readerScroll.post(() -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()));
        showBottomNotice("扫描完成 · 主评论 " + record.mainScanned + " · 回复 " + record.replyScanned
                + " · 素材 " + media.size()
                + (record.emptyReplyLists > 0 ? "；" + record.emptyReplyLists + " 组回复未返回数据，可查看明细" : ""));
    }

    private boolean download(ScanRecord record, String source, String commentId, String kind) {
        return download(record, source, commentId, kind, null);
    }

    private boolean download(ScanRecord record, String source, String commentId, String kind, JSONArray videoUrls) {
        if (source == null || source.isEmpty()) return false;
        try {
            if (videoUrls != null) {
                for (int i = 0; i < videoUrls.length(); i++) {
                    try {
                        Media video = readPostVideo(new URL(videoUrls.getString(i)), record, commentId, kind);
                        return publishCommentMedia(record, source, video);
                    } catch (Exception ignored) { }
                }
                throw new java.io.IOException("实况视频未能下载或识别");
            }
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
            return publishCommentMedia(record, source, item);
        } catch (Exception error) {
            String reason = videoUrls != null ? "实况视频未能下载或识别"
                    : "媒体格式无法识别".equals(error.getMessage()) ? "媒体格式无法识别" : "下载或写入暂存失败";
            synchronized (record) {
                if (!record.failedMedia.containsKey(source)) {
                    record.failedMedia.put(source, new FailedMedia(source, commentId, kind, videoUrls));
                    record.addIssue(kind + (commentId == null || commentId.isEmpty() ? "" : "（评论 ID " + commentId + "）") + "： " + reason);
                }
            }
            return false;
        }
    }

    private boolean publishCommentMedia(ScanRecord record, String source, Media item) {
        synchronized (record) {
            record.failedMedia.remove(source);
            if (!record.hashes.add(item.hash)) return true;
            record.items.add(item);
        }
        persistRecord(record);
        runOnUiThread(() -> {
            if (currentRecord != record || !showingReader || media.contains(item)) return;
            media.add(item);
            updateResultLabels();
            if (matchesFilter(item)) addTile(item);
            if (media.size() == 1) readerScroll.postDelayed(() -> readerScroll.smoothScrollTo(0, resultAnchor.getTop()), 200);
        });
        return true;
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

    private static void toggleVisibleSelection(ArrayList<Media> items, HashSet<Media> selection,
                                               java.util.function.Predicate<Media> visible) {
        ArrayList<Media> batch = new ArrayList<>();
        for (Media item : items) if (visible.test(item)) batch.add(item);
        if (selection.containsAll(batch)) selection.removeAll(batch);
        else selection.addAll(batch);
    }

    private static void updateSelectAll(Button button, boolean selecting, boolean busy, ArrayList<Media> items,
                                        HashSet<Media> selection, java.util.function.Predicate<Media> visible) {
        button.setVisibility(selecting ? View.VISIBLE : View.GONE);
        if (!selecting) return;
        boolean found = false;
        boolean allSelected = true;
        for (Media item : items) {
            if (!visible.test(item)) continue;
            found = true;
            if (!selection.contains(item)) {
                allSelected = false;
                break;
            }
        }
        button.setText(found && allSelected ? "取消全选" : "全选");
        button.setEnabled(found && !busy);
        button.setAlpha(button.isEnabled() ? 1f : 0.45f);
    }

    private void updateResultLabels() {
        if (readerPostStatus != null) readerPostStatus.update(currentRecord != null && currentRecord.workMedia ? currentRecord : null);
        updateSelectAll(readerSelectAll, selectingMedia, false, media, selectedMedia, this::matchesFilter);
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
        saveSub.setText(media.isEmpty() ? "保存到 下载 / XiuyiGetting"
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
        boolean phoneMedia = PHONE_MEDIA_KIND.equals(item.kind);
        if (item.kind.startsWith("回复") || phoneMedia) {
            TextView replyBadge = label(phoneMedia ? PHONE_MEDIA_KIND : "回复", 9, Color.WHITE, true);
            replyBadge.setGravity(Gravity.CENTER);
            if (phoneMedia) replyBadge.setPadding(dp(6), 0, dp(6), 0);
            replyBadge.setBackground(shape(ACCENT, 5));
            art.addView(replyBadge, new FrameLayout.LayoutParams(phoneMedia ? -2 : dp(32), dp(18), Gravity.LEFT | Gravity.BOTTOM));
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
                preview(item, media, this::matchesFilter);
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
        button.setTextSize(12);
        button.setSingleLine(true);
        button.setIncludeFontPadding(false);
        button.setGravity(Gravity.CENTER);
        button.setAutoSizeTextTypeUniformWithConfiguration(10, 12, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        button.setPadding(dp(2), dp(10), dp(2), dp(8));
        Drawable drawable = getDrawable(resource).mutate();
        drawable.setTint(primary ? Color.WHITE : resource == R.drawable.dialog_heart_filled ? ACCENT : INK);
        drawable.setBounds(0, 0, dp(18), dp(18));
        if (resource == R.drawable.dialog_heart_filled) button.setTextColor(ACCENT);
        button.setCompoundDrawablesRelative(null, drawable, null, null);
        button.setCompoundDrawablePadding(dp(5));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(58), 1);
        if (row.getChildCount() > 0) params.leftMargin = dp(8);
        row.addView(button, params);
    }

    private void addPreviewLinkRow(LinearLayout panel, String title, String detail, int resource, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), 0, dp(8), 0);
        row.setBackground(shape(TRACK, 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(44));
        params.topMargin = dp(8);
        panel.addView(row, params);
        row.addView(icon(resource, ACCENT), new LinearLayout.LayoutParams(dp(18), dp(18)));
        LinearLayout.LayoutParams label = new LinearLayout.LayoutParams(0, -2, 1);
        label.leftMargin = dp(8);
        row.addView(homeLabel(title, 13, INK, 650), label);
        TextView value = homeLabel(detail, 11, INK2, 500);
        value.setMaxWidth(dp(90));
        value.setSingleLine(true);
        value.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(value);
        if (action != null) {
            row.addView(icon(R.drawable.home_chevron, INK2), new LinearLayout.LayoutParams(dp(15), dp(15)));
            row.setOnClickListener(v -> action.run());
            pressFeedback(row);
        }
    }

    private void preview(Media item, List<Media> items, java.util.function.Predicate<Media> visible) {
        ArrayList<Media> sequence = new ArrayList<>();
        for (Media candidate : items) if (visible.test(candidate)) sequence.add(candidate);
        int index = sequence.indexOf(item);
        if (index >= 0) preview(sequence, index);
    }

    private void preview(List<Media> sequence, int index) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        renderPreview(dialog, scroll, sequence, index, null);
    }

    private void renderPreview(android.app.Dialog dialog, ScrollView scroll, List<Media> sequence,
                               int index, Runnable stopPrevious) {
        Media item = sequence.get(index);
        try {
            boolean isVideo = item.mime.startsWith("video/");
            LinearLayout panel = dialogPanel(0, false, "素材预览", null);
            panel.setPadding(dp(16), dp(16), dp(16), dp(16));
            boolean favorite = isFavorite(item);
            ScanRecord work = showingReader ? currentRecord : showingKuaishou && favoriteScreen != null
                    ? favoriteScreen.kuaishouPostRecord : null;
            String formatName = item.mime.substring(item.mime.indexOf('/') + 1).toUpperCase(Locale.ROOT);
            TextView info = homeLabel(formatName, 11, INK2, 400);
            panel.addView(info);
            int limit = Math.min(dp(264), Math.round(getResources().getDisplayMetrics().heightPixels * 0.30f));
            FrameLayout frame = new FrameLayout(this);
            frame.setBackground(shape(TRACK, 15));
            frame.setClipToOutline(true);
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, limit);
            fp.topMargin = dp(16);
            panel.addView(frame, fp);
            String size = android.text.format.Formatter.formatShortFileSize(this, item.file.length());
            Runnable start;
            Runnable stop;
            if (isVideo) {
                info.setText(formatName + " · " + size);
                PreviewVideoView video = new PreviewVideoView(this);
                video.setVideoPath(item.file.getAbsolutePath());
                video.setOnPreparedListener(player -> {
                    int width = player.getVideoWidth(), height = player.getVideoHeight();
                    info.setText(formatName + (width > 0 && height > 0 ? " · " + width + " × " + height : "") + " · " + size);
                    video.onVideoPrepared(player);
                });
                video.setOnErrorListener((player, what, extra) -> {
                    if (video.isAttachedToWindow() && dialog.isShowing()) dismissCardDialog(dialog, () -> showMessage(R.drawable.extract_play, "暂时无法播放",
                            "当前设备未能播放这段视频。可以先保存文件，再用其他播放器打开。"));
                    return true;
                });
                frame.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
                addFullscreenTarget(frame, "放大视频", () -> showFullscreenPreview(item, dialog, video, null));
                start = video::start;
                stop = video::stopPlayback;
            } else {
                int[] dimensions = new int[2];
                Drawable drawable = decodePreviewImage(item.file, 1600, dimensions);
                info.setText(formatName + " · " + dimensions[0] + " × " + dimensions[1] + " · " + size);
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setImageDrawable(drawable);
                frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
                Animatable inlineAnimation = drawable instanceof Animatable ? (Animatable) drawable : null;
                start = inlineAnimation == null ? null : inlineAnimation::start;
                stop = inlineAnimation == null ? null : inlineAnimation::stop;
                addFullscreenTarget(frame, "放大图片", () -> showFullscreenPreview(item, dialog, null, inlineAnimation));
            }
            if (sequence.size() > 1) {
                addPreviewNavigation(panel, index, sequence.size(), next -> {
                    if (dialog.isShowing()) renderPreview(dialog, scroll, sequence, next, stop);
                });
            }
            if (work != null && PHONE_MEDIA_KIND.equals(item.kind) && work.folder.equals(item.file.getParentFile())) {
                addPreviewLinkRow(panel, "作品来源", PHONE_MEDIA_KIND, R.drawable.extract_link, null);
                addPreviewLinkRow(panel, "删除导入素材", "", R.drawable.dialog_trash,
                        () -> dismissCardDialog(dialog, () -> confirmDeleteImportedMedia(work, item)));
            }
            if (showingFavorites) {
                JSONObject source = favoriteSource(item);
                if (source != null) {
                    String link = source.optString("link");
                    boolean phoneImport = source.optBoolean("phoneImport");
                    addPreviewLinkRow(panel, phoneImport ? "作品来源" : "查看作品来源",
                            phoneImport ? PHONE_MEDIA_KIND : favoriteSourcePlatform(link), R.drawable.extract_link,
                            isFavoriteSourceLink(link) ? () -> dismissCardDialog(dialog, () -> showFavoriteSource(source)) : null);
                } else {
                    TextView unavailable = homeLabel("暂无作品来源", 12, INK2, 450);
                    LinearLayout.LayoutParams unavailableParams = new LinearLayout.LayoutParams(-1, -2);
                    unavailableParams.topMargin = dp(8);
                    panel.addView(unavailable, unavailableParams);
                }
                if (favorite) {
                    String current = favoriteAssignments.getOrDefault(item.hash, "");
                    addPreviewLinkRow(panel, "移到分组", groupName(current), R.drawable.blue_folder,
                            () -> dismissCardDialog(dialog, () -> showFavoriteDestination(current, current, 1,
                                    group -> movePreviewFavorite(item, group), () -> preview(sequence, index))));
                }
            }
            View divider = new View(this);
            divider.setBackgroundColor(LINE);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
            dividerParams.topMargin = dp(14);
            panel.addView(divider, dividerParams);
            LinearLayout actions = new LinearLayout(this);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
            ap.topMargin = dp(12);
            panel.addView(actions, ap);
            addPreviewAction(actions, favorite ? "取消收藏" : "收藏",
                    favorite ? R.drawable.dialog_heart_filled : R.drawable.dialog_heart, false,
                    v -> {
                        if (favorite) {
                            showConfirmation(R.drawable.dialog_heart, true, "取消收藏这项素材？",
                                    "素材将在回收站保留 7 天。提取记录和已保存的文件仍会保留。",
                                    "保留收藏", "取消收藏", null,
                                    () -> dismissCardDialog(dialog, () -> setFavorite(item, false, "", work)));
                        } else dismissCardDialog(dialog, () -> showFavoriteDestination(null, "", 1,
                                group -> setFavorite(item, true, group, work), () -> preview(sequence, index)));
                    });
            addPreviewAction(actions, "分享", R.drawable.dialog_share, false, v -> shareMedia(Collections.singletonList(item)));
            addPreviewAction(actions, "关闭", R.drawable.dialog_close, false, v -> dismissCardDialog(dialog, null));
            addPreviewAction(actions, "保存", R.drawable.extract_download, true, v -> dismissCardDialog(dialog, () -> {
                if (canOrganizeSave(work)) {
                    saveMedia(new ArrayList<>(Collections.singletonList(item)), work, null, null);
                    return;
                }
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
            if (stopPrevious != null) stopPrevious.run();
            scroll.removeAllViews();
            scroll.addView(panel, new ScrollView.LayoutParams(-1, -2));
            scroll.scrollTo(0, 0);
            dialog.setOnDismissListener(stop == null ? null : ignored -> stop.run());
            if (!dialog.isShowing()) showCardDialog(dialog, scroll, 360, 32);
            if (start != null) start.run();
        } catch (Exception error) {
            showMessage(R.drawable.extract_info, "暂时无法预览", "未能读取这项素材，请确认文件仍在本机。");
        }
    }

    private void addPreviewNavigation(LinearLayout panel, int index, int count, java.util.function.IntConsumer navigate) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(44));
        params.topMargin = dp(8);
        panel.addView(row, params);
        for (int direction : new int[]{-1, 1}) {
            if (direction == 1) {
                TextView position = homeLabel((index + 1) + " / " + count, 12, INK2, 650);
                position.setGravity(Gravity.CENTER);
                row.addView(position, new LinearLayout.LayoutParams(0, -1, 1));
            }
            ImageView arrow = icon(R.drawable.extract_chevron, ACCENT);
            arrow.setRotation(direction * -90);
            arrow.setPadding(dp(10), dp(10), dp(10), dp(10));
            arrow.setBackground(shape(ACCENT_SOFT, 12));
            arrow.setContentDescription(direction < 0 ? "上一项素材" : "下一项素材");
            row.addView(arrow, new LinearLayout.LayoutParams(dp(44), -1));
            int next = index + direction;
            arrow.setOnClickListener(v -> navigate.accept(next));
            boolean available = next >= 0 && next < count;
            arrow.setEnabled(available);
            arrow.setAlpha(available ? 1f : 0.35f);
        }
    }

    private void confirmDeleteImportedMedia(ScanRecord record, Media item) {
        showConfirmation(R.drawable.dialog_trash, true, "删除导入素材？",
                "素材将从这条记录移除。手机原文件和已收藏的副本仍会保留。",
                "取消", "删除素材", null, () -> deleteImportedMedia(record, item));
    }

    private void deleteImportedMedia(ScanRecord record, Media item) {
        if (recordTransferBusy || !records.contains(record)) {
            showTopNotice("当前无法修改这条记录");
            return;
        }
        worker.execute(() -> {
            boolean saved;
            synchronized (record) {
                int index = record.items.indexOf(item);
                if (index < 0) return;
                record.items.remove(index);
                record.hashes.remove(item.hash);
                saved = persistRecord(record);
                if (saved) item.file.delete();
                else {
                    record.items.add(index, item);
                    record.hashes.add(item.hash);
                }
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (!saved) {
                    showTopNotice("无法删除导入素材，请稍后重试");
                    return;
                }
                if (currentRecord == record && showingReader) {
                    media.remove(item);
                    selectedMedia.remove(item);
                    renderResults();
                    updateResultLabels();
                } else if (showingKuaishou && favoriteScreen.kuaishouPostRecord == record) {
                    favoriteScreen.items.remove(item);
                    favoriteScreen.selected.remove(item);
                    favoriteScreen.render();
                }
                refreshRecordList.run();
                showBottomNotice("已从记录删除导入素材");
            });
        });
    }

    private Drawable decodePreviewImage(File file, int maximumSize, int[] dimensions) throws java.io.IOException {
        return ImageDecoder.decodeDrawable(ImageDecoder.createSource(file), (decoder, info, source) -> {
            int width = info.getSize().getWidth();
            int height = info.getSize().getHeight();
            if (dimensions != null) {
                dimensions[0] = width;
                dimensions[1] = height;
            }
            int longest = Math.max(width, height);
            if (longest > maximumSize) decoder.setTargetSize(Math.max(1, width * maximumSize / longest),
                    Math.max(1, height * maximumSize / longest));
        });
    }

    private void addFullscreenTarget(FrameLayout frame, String description, Runnable open) {
        View target = new View(this);
        target.setContentDescription(description);
        target.setFocusable(true);
        target.setOnClickListener(v -> open.run());
        frame.addView(target, new FrameLayout.LayoutParams(-1, -1));

        ImageView badge = icon(R.drawable.preview_expand, Color.WHITE);
        badge.setPadding(dp(8), dp(8), dp(8), dp(8));
        badge.setBackground(shape(0xB31B2537, 10));
        FrameLayout.LayoutParams corner = new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.TOP | Gravity.END);
        corner.setMargins(dp(8), dp(8), dp(8), dp(8));
        frame.addView(badge, corner);
    }

    private void showFullscreenPreview(Media item, android.app.Dialog owner, PreviewVideoView inlineVideo,
                                       Animatable inlineAnimation) {
        if (!owner.isShowing() || fullscreenPreviewDialog != null) return;
        boolean video = inlineVideo != null;
        FullscreenPreviewDialog dialog = new FullscreenPreviewDialog(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.BLACK);
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), 0, dp(8), 0);
        page.addView(bar, new LinearLayout.LayoutParams(-1, dp(52)));
        bar.addView(homeLabel(video ? "视频预览" : "图片预览", 16, Color.WHITE, 650),
                new LinearLayout.LayoutParams(0, -2, 1));
        ImageView close = icon(R.drawable.dialog_close, Color.WHITE);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("退出全屏");
        close.setOnClickListener(v -> dialog.dismiss());
        FrameLayout content = new FrameLayout(this);
        page.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        if (video) {
            PreviewVideoView enlarged = configureFullscreenVideo(dialog, owner, content, item, inlineVideo);
            page.addView(playbackSpeedControls(enlarged), 1, new LinearLayout.LayoutParams(-1, dp(44)));
        } else {
            try {
                Drawable drawable = decodePreviewImage(item.file, 2160, null);
                ImageView image = new ImageView(this);
                image.setContentDescription("全屏图片");
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setImageDrawable(drawable);
                content.addView(image, new FrameLayout.LayoutParams(-1, -1));
                if (inlineAnimation != null) inlineAnimation.stop();
                if (drawable instanceof Animatable) {
                    Animatable enlarged = (Animatable) drawable;
                    dialog.setOnShowListener(ignored -> enlarged.start());
                    dialog.setOnDismissListener(ignored -> {
                        enlarged.stop();
                        finishFullscreenPreview(owner, inlineAnimation);
                    });
                } else dialog.setOnDismissListener(ignored -> finishFullscreenPreview(owner, inlineAnimation));
            } catch (java.io.IOException error) {
                showTopNotice("暂时无法放大图片");
                return;
            }
        }
        bar.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        fullscreenPreviewDialog = dialog;
        dialog.setContentView(page);
        android.view.Window window = dialog.getWindow();
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            page.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets safe = insets.getInsets(android.view.WindowInsets.Type.systemBars()
                        | android.view.WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
        }
        dialog.show();
        window.setLayout(-1, -1);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController controller = window.getInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(0,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            page.requestApplyInsets();
        }
    }

    private static final class FullscreenPreviewDialog extends android.app.Dialog {
        Runnable beforeDismiss;

        FullscreenPreviewDialog(Context context) {
            super(context, android.R.style.Theme_Material_NoActionBar);
        }

        @Override public void dismiss() {
            if (beforeDismiss != null) {
                Runnable capture = beforeDismiss;
                beforeDismiss = null;
                capture.run();
            }
            super.dismiss();
        }
    }

    private static final class PreviewVideoView extends VideoView {
        private android.media.MediaPlayer player;
        private int pendingPosition = -1;
        private boolean shouldPlay;
        private int speedIndex = NORMAL_SPEED;
        private Runnable speedChanged;

        PreviewVideoView(Context context) {
            super(context);
        }

        void onVideoPrepared(android.media.MediaPlayer prepared) {
            player = prepared;
            if (pendingPosition >= 0) seekTo(pendingPosition);
            if (shouldPlay) start();
        }

        @Override public void seekTo(int position) {
            pendingPosition = position;
            if (player != null) {
                try {
                    player.seekTo(position, android.media.MediaPlayer.SEEK_CLOSEST);
                    pendingPosition = -1;
                } catch (IllegalStateException ignored) {
                    player = null;
                }
            }
        }

        @Override public void start() {
            shouldPlay = true;
            super.start();
            applyPlaybackSpeed();
        }

        void setSpeedIndex(int index) {
            speedIndex = index;
            applyPlaybackSpeed();
            if (speedChanged != null) speedChanged.run();
        }

        private void applyPlaybackSpeed() {
            // Setting playback parameters can resume a paused MediaPlayer.
            if (player == null || !shouldPlay || !isPlaying()) return;
            try {
                float speed = PREVIEW_SPEEDS[speedIndex];
                android.media.PlaybackParams params = player.getPlaybackParams();
                if (params.getSpeed() != speed) player.setPlaybackParams(params.setSpeed(speed));
            } catch (IllegalStateException ignored) {
                // The surface can be released before VideoView prepares a replacement player.
            } catch (IllegalArgumentException | UnsupportedOperationException error) {
                speedIndex = NORMAL_SPEED;
                if (speedChanged != null) speedChanged.run();
                android.widget.Toast.makeText(getContext(), "当前视频不支持此倍速", android.widget.Toast.LENGTH_SHORT).show();
            }
        }

        @Override public void pause() {
            shouldPlay = false;
            super.pause();
        }

        @Override public void stopPlayback() {
            player = null;
            shouldPlay = false;
            super.stopPlayback();
        }
    }

    private static final String[] PREVIEW_SPEED_NAMES = {"0.5×", "0.75×", "1×", "1.25×", "1.5×", "2×"};
    private static final float[] PREVIEW_SPEEDS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
    private static final int NORMAL_SPEED = 2;

    private LinearLayout playbackSpeedControls(PreviewVideoView video) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(10), 0, dp(10), 0);
        for (int i = 0; i < PREVIEW_SPEED_NAMES.length; i++) {
            int index = i;
            TextView button = homeLabel(PREVIEW_SPEED_NAMES[i], 12, Color.WHITE, 650);
            button.setContentDescription("播放倍速 " + PREVIEW_SPEED_NAMES[i]);
            button.setGravity(Gravity.CENTER);
            button.setOnClickListener(v -> video.setSpeedIndex(index));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
            params.setMargins(dp(2), dp(3), dp(2), dp(3));
            row.addView(button, params);
        }
        video.speedChanged = () -> {
            for (int i = 0; i < row.getChildCount(); i++) {
                View button = row.getChildAt(i);
                boolean selected = i == video.speedIndex;
                button.setSelected(selected);
                button.setBackground(shape(selected ? 0xFF3458E8 : 0xFF252525, 10));
            }
        };
        video.speedChanged.run();
        return row;
    }

    private PreviewVideoView configureFullscreenVideo(FullscreenPreviewDialog dialog, android.app.Dialog owner,
                                                       FrameLayout content, Media item, PreviewVideoView inline) {
        int resumeAt = inline.getCurrentPosition();
        inline.pause();
        PreviewVideoView enlarged = new PreviewVideoView(this);
        enlarged.setSpeedIndex(inline.speedIndex);
        enlarged.setContentDescription("全屏视频");
        MediaController controls = new MediaController(this);
        enlarged.setMediaController(controls);
        content.addView(enlarged, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        enlarged.setOnPreparedListener(player -> {
            enlarged.onVideoPrepared(player);
            controls.show(2500);
        });
        enlarged.setOnErrorListener((player, what, extra) -> {
            dialog.dismiss();
            showTopNotice("暂时无法全屏播放这段视频");
            return true;
        });
        enlarged.setVideoPath(item.file.getAbsolutePath());
        if (resumeAt > 0) enlarged.seekTo(resumeAt);
        enlarged.start();
        int[] position = {resumeAt};
        boolean[] playing = {false};
        dialog.beforeDismiss = () -> {
            if (enlarged.getDuration() > 0) position[0] = enlarged.getCurrentPosition();
            playing[0] = enlarged.isPlaying();
            controls.hide();
            enlarged.stopPlayback();
        };
        dialog.setOnDismissListener(ignored -> {
            finishFullscreenPreview(owner, null);
            if (owner.isShowing() && !isFinishing() && !isDestroyed()) {
                inline.setSpeedIndex(enlarged.speedIndex);
                inline.seekTo(position[0]);
                if (playing[0]) inline.start();
            }
        });
        return enlarged;
    }

    private void finishFullscreenPreview(android.app.Dialog owner, Animatable inlineAnimation) {
        fullscreenPreviewDialog = null;
        if (inlineAnimation != null && owner.isShowing() && !isFinishing() && !isDestroyed()) inlineAnimation.start();
    }

    private File favoriteFile(Media item) {
        String name = item.file.getName();
        return new File(new File(getFilesDir(), "favorites"), item.hash + name.substring(name.lastIndexOf('.')));
    }

    private boolean isFavorite(Media item) {
        return favoriteFile(item).isFile();
    }

    private boolean isFavoriteSourceLink(String link) {
        return WorkMedia.isShareLink(link, false) || WorkMedia.isShareLink(link, true)
                || (!link.isEmpty() && link.equals(KuaishouPost.extractLink(link)));
    }

    private JSONObject favoriteSource(Media item) {
        JSONObject known = favoriteSources.optJSONObject(item.hash);
        String knownLink = known == null ? "" : known.optString("link");
        boolean knownWork = isFavoriteSourceLink(knownLink);
        if (knownWork && known.optBoolean("phoneImport")) return known;
        for (ScanRecord record : records) {
            if (!isFavoriteSourceLink(record.link) || knownWork && !record.link.equals(knownLink)) continue;
            synchronized (record) {
                if (!record.hashes.contains(item.hash)) continue;
                for (Media candidate : record.items) {
                    if (candidate.hash.equals(item.hash) && rememberFavoriteSource(candidate, record, true)) {
                        saveFavoriteSources();
                        return favoriteSources.optJSONObject(item.hash);
                    }
                }
            }
        }
        return knownWork || known != null && known.optBoolean("phoneImport") ? known : null;
    }

    private boolean rememberFavoriteSource(Media item, ScanRecord record, boolean matched) {
        boolean linked = record != null && isFavoriteSourceLink(record.link);
        JSONObject known = favoriteSources.optJSONObject(item.hash);
        boolean phoneImport = PHONE_MEDIA_KIND.equals(item.kind) || known != null && known.optBoolean("phoneImport");
        if (!linked && !phoneImport) return false;
        try {
            if (known != null && isFavoriteSourceLink(known.optString("link"))
                    && (!linked || matched || !known.optBoolean("matched"))) {
                if (!linked || !phoneImport || known.optBoolean("phoneImport")
                        || !record.link.equals(known.optString("link"))) return false;
                known.put("phoneImport", true);
                return true;
            }
            JSONObject source = new JSONObject();
            if (linked) source.put("link", record.link).put("caption", record.caption).put("matched", matched);
            if (phoneImport) source.put("phoneImport", true);
            favoriteSources.put(item.hash, source);
            return true;
        } catch (org.json.JSONException error) {
            return false;
        }
    }

    private void rememberDeletedRecordSources(ScanRecord record) {
        if (!isFavoriteSourceLink(record.link)) return;
        boolean changed = false;
        synchronized (record) {
            for (Media item : record.items) {
                if (isFavorite(item)) changed |= rememberFavoriteSource(item, record, true);
            }
        }
        if (changed) saveFavoriteSources();
    }

    private void saveFavoriteSources() {
        getPreferences(MODE_PRIVATE).edit().putString("favorite_sources", favoriteSources.toString()).apply();
    }

    private byte[] favoriteBackupMetadata(List<File> favorites, String group) {
        JSONObject assignments = new JSONObject();
        JSONObject sources = new JSONObject();
        try {
            for (File file : favorites) {
                String hash = favoriteFileHash(file);
                assignments.put(hash, favoriteAssignments.get(hash));
                sources.put(hash, favoriteSources.optJSONObject(hash));
            }
            ArrayList<String> groups = new ArrayList<>(favoriteGroups);
            groups.removeIf(privateFavoritePins::containsKey);
            return new JSONObject().put("groups", new JSONArray(group == null ? groups : Collections.singleton(group)))
                    .put("assignments", assignments).put("sources", sources).toString().getBytes(StandardCharsets.UTF_8);
        } catch (org.json.JSONException error) {
            throw new IllegalStateException(error);
        }
    }

    private java.util.LinkedHashSet<String> backupGroupNames(JSONObject data) {
        java.util.LinkedHashSet<String> groups = new java.util.LinkedHashSet<>();
        JSONArray names = data.optJSONArray("groups");
        for (int i = 0; names != null && i < names.length(); i++) {
            String name = names.optString(i).trim();
            if (!name.isEmpty() && name.length() <= 24 && !"全部收藏".equals(name) && !"未分组".equals(name))
                groups.add(name);
        }
        return groups;
    }

    private int mergeFavoriteBackup(RecordArchive.Result backup, JSONObject data, HashSet<String> useBackupGroups) {
        if (backup.favoriteMetadata == null) return 0;
        java.util.Set<String> groups = backupGroupNames(data);
        favoriteGroups.addAll(groups);
        JSONObject assignments = data.optJSONObject("assignments");
        JSONObject sources = data.optJSONObject("sources");
        int changed = 0;
        for (String hash : backup.favoriteHashes) {
            boolean duplicate = backup.duplicateFavoriteHashes.contains(hash);
            if (assignments != null && (!duplicate || useBackupGroups.contains(hash))) {
                String group = assignments.optString(hash);
                if (group.isEmpty() || groups.contains(group)) {
                    String previous = group.isEmpty() ? favoriteAssignments.remove(hash) : favoriteAssignments.put(hash, group);
                    if (duplicate && !group.equals(previous == null ? "" : previous)) changed++;
                }
            }
            JSONObject source = sources == null ? null : sources.optJSONObject(hash);
            if (!favoriteSources.has(hash) && source != null
                    && (source.optBoolean("phoneImport") || isFavoriteSourceLink(source.optString("link")))) {
                try { favoriteSources.put(hash, source); }
                catch (org.json.JSONException ignored) { }
            }
        }
        saveFavoriteGroups();
        saveFavoriteSources();
        return changed;
    }

    private String favoriteSourcePlatform(String link) {
        return WorkMedia.isShareLink(link, true) ? "小红书" : WorkMedia.isShareLink(link, false) ? "抖音" : "快手";
    }

    private EditText mediaSourceSearch(String hint) {
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint(hint);
        search.setTextSize(13);
        search.setTextColor(INK);
        search.setHintTextColor(INK3);
        search.setBackground(shape(TRACK, 12));
        Drawable icon = getDrawable(R.drawable.record_search).mutate();
        icon.setTint(INK2);
        icon.setBounds(0, 0, dp(18), dp(18));
        search.setCompoundDrawablesRelative(icon, null, null, null);
        search.setCompoundDrawablePadding(dp(8));
        search.setPadding(dp(12), 0, dp(12), 0);
        return search;
    }

    private void onMediaSearchChanged(EditText search, Runnable update) {
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable text) { update.run(); }
        });
    }

    private boolean matchesMediaSource(String link, String caption, String query) {
        return query.isEmpty() || (caption + " " + link + " " + favoriteSourcePlatform(link))
                .toLowerCase(Locale.ROOT).contains(query);
    }

    private void showFavoriteSource(JSONObject source) {
        String link = source.optString("link");
        if (!isFavoriteSourceLink(link)) return;
        String platform = favoriteSourcePlatform(link);
        android.app.Dialog dialog = new android.app.Dialog(this);
        String label = source.optBoolean("phoneImport") ? PHONE_MEDIA_KIND + " · 关联" + platform + "作品"
                : platform + (source.optBoolean("matched") ? " · 本机记录匹配" : "");
        LinearLayout panel = sheetPanel("作品来源", label);
        String caption = source.optString("caption");
        if (!caption.isEmpty()) {
            TextView title = homeLabel(caption, 14, INK, 650);
            title.setMaxLines(2);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            panel.addView(title, batchRowParams());
        }
        TextView address = homeLabel(link, 12, INK2, 450);
        address.setMaxLines(2);
        address.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panel.addView(address, batchRowParams());
        addSheetChoice(panel, "打开原作品", "", R.drawable.extract_open, false,
                () -> dismissCardDialog(dialog, () -> {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))); }
                    catch (android.content.ActivityNotFoundException error) { showBottomNotice("没有可打开该链接的应用"); }
                }));
        addSheetChoice(panel, "复制链接", "", R.drawable.extract_copy, false,
                () -> dismissCardDialog(dialog, () -> showBottomNotice(copyLink(link) ? "链接已复制" : "复制失败：剪贴板不可用")));
        showBottomSheet(dialog, panel);
    }

    private void loadFavoriteGroups() {
        try {
            JSONObject data = new JSONObject(getPreferences(MODE_PRIVATE).getString("favorite_groups", "{}"));
            JSONArray names = data.optJSONArray("names");
            for (int i = 0; names != null && i < names.length(); i++) favoriteGroups.add(names.getString(i));
            JSONObject pins = data.optJSONObject("privatePins");
            if (pins != null) for (String name : favoriteGroups) {
                String value = pins.optString(name);
                if (value.matches("[0-9a-f]{32}:[0-9a-f]{64}")) privateFavoritePins.put(name, value);
            }
            JSONObject assignments = data.optJSONObject("assignments");
            if (assignments != null) assignments.keys().forEachRemaining(hash -> {
                String name = assignments.optString(hash);
                if (favoriteGroups.contains(name)) favoriteAssignments.put(hash, name);
            });
        } catch (org.json.JSONException ignored) { }
    }

    private void saveFavoriteGroups() {
        try {
            JSONObject data = new JSONObject().put("names", new JSONArray(favoriteGroups))
                    .put("assignments", new JSONObject(favoriteAssignments))
                    .put("privatePins", new JSONObject(privateFavoritePins));
            getPreferences(MODE_PRIVATE).edit().putString("favorite_groups", data.toString()).apply();
        } catch (org.json.JSONException ignored) { }
    }

    private String groupName(String name) {
        return name == null ? "全部收藏" : name.isEmpty() ? "未分组" : name;
    }

    private String newPinVerifier(String pin) {
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        return pinHash(pin, salt);
    }

    private String pinHash(String pin, byte[] salt) {
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, 120000, 256);
        try {
            byte[] digest = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return String.format(Locale.ROOT, "%032x:%064x", new java.math.BigInteger(1, salt), new java.math.BigInteger(1, digest));
        } catch (java.security.GeneralSecurityException error) {
            throw new IllegalStateException(error);
        } finally {
            spec.clearPassword();
        }
    }

    private void verifyFavoriteGroup(String group, Runnable allowed) {
        String saved = privateFavoritePins.get(group);
        if (saved == null) {
            allowed.run();
            return;
        }
        showPin("解锁私密收藏夹", group, "解锁", "密码不正确", pin -> matchesPin(pin, saved), allowed);
    }

    private boolean matchesPin(String pin, String saved) {
        byte[] salt = new byte[16];
        for (int i = 0; i < salt.length; i++) salt[i] = (byte) Integer.parseInt(saved.substring(i * 2, i * 2 + 2), 16);
        return MessageDigest.isEqual(saved.getBytes(StandardCharsets.US_ASCII), pinHash(pin, salt).getBytes(StandardCharsets.US_ASCII));
    }

    private void showPin(String title, String detail, String confirmLabel, String rejectedMessage,
                         java.util.function.Predicate<String> accept, Runnable accepted) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = sheetPanel(title, detail);
        FrameLayout field = new FrameLayout(this);
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(-1, dp(46));
        fieldParams.topMargin = dp(16);
        panel.addView(field, fieldParams);
        LinearLayout boxes = new LinearLayout(this);
        boxes.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        field.addView(boxes, new FrameLayout.LayoutParams(-1, -1));
        TextView[] digits = new TextView[6];
        for (int i = 0; i < digits.length; i++) {
            TextView digit = homeLabel("", 20, INK, 700);
            digit.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams box = new LinearLayout.LayoutParams(0, -1, 1);
            if (i > 0) box.leftMargin = dp(6);
            boxes.addView(digit, box);
            digits[i] = digit;
        }
        EditText inputPin = new EditText(this);
        inputPin.setSingleLine(true);
        inputPin.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789"));
        inputPin.setRawInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        inputPin.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        inputPin.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(digits.length)});
        inputPin.setContentDescription("6 位数字密码");
        inputPin.setTextColor(Color.TRANSPARENT);
        inputPin.setHighlightColor(Color.TRANSPARENT);
        inputPin.setBackgroundColor(Color.TRANSPARENT);
        inputPin.setCursorVisible(false);
        inputPin.setOnClickListener(v -> inputPin.setSelection(inputPin.length()));
        inputPin.setSaveEnabled(false);
        inputPin.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        inputPin.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        field.addView(inputPin, new FrameLayout.LayoutParams(-1, -1));
        TextView error = homeLabel("", 12, DANGER, 550);
        error.setPadding(0, dp(6), 0, 0);
        panel.addView(error);
        Runnable update = () -> {
            int length = inputPin.length();
            error.setText("");
            for (int i = 0; i < digits.length; i++) {
                digits[i].setText(i < length ? "●" : "");
                boolean active = i == length;
                GradientDrawable border = shape(active ? ACCENT_SOFT : BG, 8);
                border.setStroke(dp(active ? 2 : 1), active ? ACCENT : LINE);
                digits[i].setBackground(border);
            }
        };
        onMediaSearchChanged(inputPin, update);
        update.run();
        Runnable submit = () -> {
            if (inputPin.length() != digits.length) {
                error.setText("请输入完整的 6 位数字密码");
                return;
            }
            if (!accept.test(inputPin.getText().toString())) {
                inputPin.setText("");
                error.setText(rejectedMessage);
                return;
            }
            inputPin.setText("");
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(inputPin.getWindowToken(), 0);
            dismissCardDialog(dialog, accepted);
        };
        LinearLayout actions = new LinearLayout(this);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
        actionParams.topMargin = dp(8);
        panel.addView(actions, actionParams);
        actions.addView(dialogButton("取消", false, false, v -> dismissCardDialog(dialog, null)),
                new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams confirm = new LinearLayout.LayoutParams(0, -2, 1.55f);
        confirm.leftMargin = dp(8);
        actions.addView(dialogButton(confirmLabel, true, false, v -> submit.run()), confirm);
        inputPin.setOnEditorActionListener((v, action, event) -> {
            if (action != android.view.inputmethod.EditorInfo.IME_ACTION_DONE) return false;
            submit.run();
            return true;
        });
        inputPin.requestFocus();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        presentBottomSheet(dialog, panel);
    }

    private boolean canChangeFavorite(String hash) {
        String group = favoriteAssignments.get(hash);
        return !privateFavoritePins.containsKey(group) || (showingFavorites && favoriteScreen != null
                && group.equals(favoriteScreen.activeGroup));
    }

    private void showFavoriteDestination(String current, String selection, int count,
                                         java.util.function.Consumer<String> onConfirm, Runnable onCancel) {
        boolean moving = current != null;
        android.app.Dialog dialog = new android.app.Dialog(this);
        String detail = moving ? "当前分组 · " + groupName(current)
                : count == 1 ? "为这张素材选择一个收藏分组" : "为 " + count + " 个素材选择收藏分组";
        LinearLayout panel = sheetPanel(moving ? "移到分组" : "收藏到分组", detail);
        ScrollView scroll = dialogScroll();
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(-1, -2);
        listParams.topMargin = dp(8);
        panel.addView(scroll, listParams);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(choices);
        String[] selected = {selection};
        TextView addGroup = homeLabel("新建分组", 12, ACCENT, 650);
        Drawable plus = getDrawable(R.drawable.group_add).mutate();
        plus.setTint(ACCENT);
        plus.setBounds(0, 0, dp(15), dp(15));
        addGroup.setCompoundDrawablesRelative(plus, null, null, null);
        addGroup.setCompoundDrawablePadding(dp(5));
        addGroup.setGravity(Gravity.CENTER_VERTICAL);
        addGroup.setPadding(dp(4), 0, dp(4), 0);
        panel.addView(addGroup, new LinearLayout.LayoutParams(-1, dp(44)));
        addGroup.setOnClickListener(v -> dismissCardDialog(dialog, () ->
                editGroupName(null, name -> showFavoriteDestination(current, name, count, onConfirm, onCancel),
                        () -> showFavoriteDestination(current, selected[0], count, onConfirm, onCancel))));
        dialogActions(dialog, panel, "取消", moving ? "确认移动" : "确认收藏", false, onCancel,
                () -> onConfirm.accept(selected[0]));
        LinearLayout buttons = (LinearLayout) panel.getChildAt(panel.getChildCount() - 1);
        Button confirm = (Button) buttons.getChildAt(1);
        ((LinearLayout.LayoutParams) confirm.getLayoutParams()).weight = 2;
        dialog.setOnCancelListener(ignored -> { if (onCancel != null) onCancel.run(); });

        ArrayList<String> names = new ArrayList<>();
        names.add("");
        names.addAll(favoriteGroups);
        Runnable renderChoices = new Runnable() {
            @Override public void run() {
                choices.removeAllViews();
                for (String name : names) {
                    boolean checked = name.equals(selected[0]);
                    addCheckedSheetChoice(choices, groupName(name), "", R.drawable.blue_folder, checked, () -> {
                        selected[0] = name;
                        run();
                    });
                    LinearLayout row = (LinearLayout) choices.getChildAt(choices.getChildCount() - 1);
                    row.setPadding(dp(10), dp(5), dp(10), dp(5));
                    row.setMinimumHeight(dp(42));
                    ((LinearLayout.LayoutParams) row.getLayoutParams()).topMargin = dp(6);
                    if (checked) {
                        GradientDrawable background = shape(ACCENT_SOFT, 10);
                        background.setStroke(dp(1), ACCENT);
                        row.setBackground(background);
                    }
                    ImageView indicator = (ImageView) row.getChildAt(row.getChildCount() - 1);
                    if (checked) {
                        indicator.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
                        indicator.setPadding(dp(4), dp(4), dp(4), dp(4));
                        indicator.setBackground(shape(ACCENT, 10));
                        indicator.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    } else indicator.setVisibility(View.GONE);
                    if (name.equals(current)) {
                        TextView tag = homeLabel("当前", 11, INK2, 600);
                        tag.setPadding(0, 0, dp(5), 0);
                        row.addView(tag, row.indexOfChild(indicator), new LinearLayout.LayoutParams(-2, -2));
                    }
                }
                boolean enabled = !moving || !selected[0].equals(current);
                confirm.setEnabled(enabled);
                confirm.setAlpha(enabled ? 1f : 0.45f);
            }
        };
        renderChoices.run();
        presentBottomSheet(dialog, panel);
        int selectedIndex = names.indexOf(selection);
        if (selectedIndex > 0) {
            scroll.post(() -> {
                View selectedRow = choices.getChildAt(selectedIndex);
                if (selectedRow != null && selectedRow.getBottom() > scroll.getHeight())
                    scroll.scrollTo(0, selectedRow.getBottom() - scroll.getHeight());
            });
        }
    }

    private void movePreviewFavorite(Media item, String group) {
        if (!isFavorite(item)) {
            showBottomNotice("素材已不在收藏夹中");
            return;
        }
        if (assignFavoriteGroup(item, group)) saveFavoriteGroups();
        if (showingFavorites && favoriteScreen != null) favoriteScreen.render();
        showBottomNotice("已移到「" + groupName(group) + "」");
    }

    private boolean assignFavoriteGroup(Media item, String group) {
        if (!canChangeFavorite(item.hash)) return false;
        if (group.isEmpty() || !favoriteGroups.contains(group)) return favoriteAssignments.remove(item.hash) != null;
        return !group.equals(favoriteAssignments.put(item.hash, group));
    }

    private void editGroupName(String oldName, java.util.function.Consumer<String> onSaved) {
        editGroupName(oldName, onSaved, null);
    }

    private void editGroupName(String oldName, java.util.function.Consumer<String> onSaved, Runnable onCancel) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        boolean creating = oldName == null;
        LinearLayout panel = sheetPanel(creating ? "新建分组" : "重命名分组", null);
        panel.setFocusableInTouchMode(true);

        TextView fieldLabel = homeLabel("分组名称", 12, ACCENT, 650);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(14);
        panel.addView(fieldLabel, labelParams);
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("例如：壁纸");
        name.setTextColor(INK);
        name.setHintTextColor(INK3);
        name.setTextSize(15);
        name.setBackground(outlined(10, SURFACE));
        name.setPadding(dp(12), 0, dp(12), 0);
        name.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
        if (!creating) name.setText(oldName);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, dp(46));
        nameParams.topMargin = dp(5);
        panel.addView(name, nameParams);
        TextView length = homeLabel("", 11, INK2, 450);
        length.setGravity(Gravity.END);
        length.setPadding(0, dp(4), dp(2), 0);
        panel.addView(length);

        dialogActions(dialog, panel, "取消", creating ? "创建" : "保存", false, onCancel, null);
        if (onCancel != null) dialog.setOnCancelListener(ignored -> onCancel.run());
        LinearLayout buttons = (LinearLayout) panel.getChildAt(panel.getChildCount() - 1);
        Button saveName = (Button) buttons.getChildAt(1);
        Runnable update = () -> {
            length.setText(name.length() + " / 24");
            saveName.setEnabled(!name.getText().toString().trim().isEmpty());
            saveName.setAlpha(saveName.isEnabled() ? 1f : 0.45f);
        };
        name.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable text) { update.run(); }
        });
        saveName.setOnClickListener(v -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) return;
            if (!value.equals(oldName) && (favoriteGroups.contains(value) || value.equals("全部收藏") || value.equals("未分组"))) {
                name.setError("分组名称已存在");
                return;
            }
            if (!value.equals(oldName)) {
                if (creating) favoriteGroups.add(value);
                else {
                    ArrayList<String> order = new ArrayList<>(favoriteGroups);
                    order.set(order.indexOf(oldName), value);
                    favoriteGroups.clear();
                    favoriteGroups.addAll(order);
                    favoriteAssignments.replaceAll((hash, group) -> oldName.equals(group) ? value : group);
                    String pin = privateFavoritePins.remove(oldName);
                    if (pin != null) privateFavoritePins.put(value, pin);
                    worker.execute(() -> recycleBin.renameGroup(oldName, value));
                    if (favoriteScreen != null && oldName.equals(favoriteScreen.activeGroup)) favoriteScreen.activeGroup = value;
                }
                saveFavoriteGroups();
            }
            dismissCardDialog(dialog, () -> onSaved.accept(value));
        });
        name.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != android.view.inputmethod.EditorInfo.IME_ACTION_DONE || !saveName.isEnabled()) return false;
            saveName.performClick();
            return true;
        });
        update.run();
        panel.requestFocus();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        presentBottomSheet(dialog, panel);
    }

    private void copyMediaToFavorites(Media item) throws java.io.IOException {
        File target = favoriteFile(item);
        if (target.isFile()) return;
        File folder = target.getParentFile();
        if (!folder.isDirectory() && !folder.mkdirs()) throw new java.io.IOException("无法创建收藏夹");
        File temp = new File(folder, target.getName() + ".tmp");
        try {
            Files.copy(item.file.toPath(), temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally { temp.delete(); }
    }

    private void setFavorite(Media item, boolean adding, String group, ScanRecord source) {
        if (!canChangeFavorite(item.hash)) {
            showTopNotice("请先从收藏页解锁此素材所在的私密收藏夹");
            return;
        }
        if (!adding) {
            trashFavorites(Collections.singletonList(item), null);
            return;
        }
        worker.execute(() -> {
            String message;
            try {
                copyMediaToFavorites(item);
                message = "已收藏到「" + groupName(group) + "」";
            } catch (Exception error) { message = "收藏操作失败：" + error.getMessage(); }
            String result = message;
            runOnUiThread(() -> {
                boolean saved = isFavorite(item);
                if (!saved) {
                    if (favoriteAssignments.remove(item.hash) != null) saveFavoriteGroups();
                    if (favoriteSources.remove(item.hash) != null) saveFavoriteSources();
                } else {
                    if (assignFavoriteGroup(item, group)) saveFavoriteGroups();
                    if (rememberFavoriteSource(item, source, false)) saveFavoriteSources();
                }
                if (saved && (showingReader || showingWechat || showingKuaishou) && !isFinishing()) {
                    if (showingKuaishou) {
                        FavoritesScreen screen = favoriteScreen;
                        showBottomNotice("已收藏", "查看收藏", () -> openFavorites(screen::restoreKuaishou));
                    } else if (showingWechat) showBottomNotice("已收藏", "查看收藏", () -> openFavorites(this::returnToWechatImport));
                    else showFavoriteNotice();
                    return;
                }
                showBottomNotice(result);
            });
        });
    }

    private void trashFavorites(List<Media> items, Runnable finished) {
        java.util.LinkedHashMap<Media, JSONObject> metadata = new java.util.LinkedHashMap<>();
        try {
            for (Media item : items) {
                if (!canChangeFavorite(item.hash)) continue;
                String group = favoriteAssignments.getOrDefault(item.hash, "");
                metadata.put(item, new JSONObject().put("group", group)
                        .put("pin", privateFavoritePins.get(group)).put("source", favoriteSources.optJSONObject(item.hash)));
            }
        } catch (org.json.JSONException error) {
            if (finished != null) finished.run();
            showMessage(R.drawable.dialog_trash, "未移入回收站", "无法读取素材信息，请稍后重试。");
            return;
        }
        worker.execute(() -> {
            HashSet<String> removed = new HashSet<>();
            metadata.forEach((item, entry) -> {
                try {
                    recycleBin.stage(favoriteFile(item), entry);
                    removed.add(item.hash);
                } catch (java.io.IOException | org.json.JSONException ignored) { }
            });
            runOnUiThread(() -> {
                boolean groupsChanged = false;
                boolean sourcesChanged = false;
                for (String hash : removed) {
                    groupsChanged |= favoriteAssignments.remove(hash) != null;
                    sourcesChanged |= favoriteSources.remove(hash) != null;
                }
                if (groupsChanged) saveFavoriteGroups();
                if (sourcesChanged) saveFavoriteSources();
                if ((showingFavorites || showingGroupManager) && favoriteScreen != null) {
                    favoriteScreen.items.removeIf(media -> removed.contains(media.hash));
                    favoriteScreen.selected.removeIf(media -> removed.contains(media.hash));
                    if (favoriteScreen.selected.isEmpty()) favoriteScreen.selecting = false;
                    favoriteScreen.render();
                }
                if (finished != null) finished.run();
                int failed = items.size() - removed.size();
                if (failed == 0) showBottomNotice("已移入回收站 " + removed.size() + " 项");
                else showMessage(R.drawable.dialog_trash, "有 " + failed + " 项未移入回收站",
                        "已移入 " + removed.size() + " 项，可重新选择未移入的素材后重试。");
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
        showingWechat = false;
        showingKuaishou = false;
        showingFavorites = true;
        showingMy = false;
        showingReader = false;
        showingRecords = false;
        showingRecordPlatforms = false;
        showingSettings = false;
        for (Animatable animation : animations) animation.stop();
        new FavoritesScreen().show();
    }

    private static final String SAVE_DIRECTORY = Environment.DIRECTORY_DOWNLOADS + "/XiuyiGetting";

    private enum MediaSaveMode {
        DIRECT("直接保存", "素材直接保存到下载目录", R.drawable.extract_download),
        WORK_FOLDER("按作品建文件夹", "按平台和作品分别存放素材", R.drawable.blue_folder),
        ZIP("打包 ZIP", "将本次保存的素材打包为 ZIP", R.drawable.dialog_file);

        final String label;
        final String detail;
        final int icon;

        MediaSaveMode(String label, String detail, int icon) {
            this.label = label;
            this.detail = detail;
            this.icon = icon;
        }
    }

    private MediaSaveMode mediaSaveMode() {
        String stored = getPreferences(MODE_PRIVATE).getString("media_save_mode", "");
        for (MediaSaveMode mode : MediaSaveMode.values()) {
            if (mode.name().equals(stored)) return mode;
        }
        return MediaSaveMode.DIRECT;
    }

    private void showMediaSaveSettings(TextView currentValue) {
        MediaSaveMode selected = mediaSaveMode();
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = sheetPanel("保存方式", "应用于有作品来源的提取素材");
        for (MediaSaveMode mode : MediaSaveMode.values()) {
            addCheckedSheetChoice(panel, mode.label, mode.detail, mode.icon, mode == selected,
                    () -> dismissCardDialog(dialog, () -> {
                        getPreferences(MODE_PRIVATE).edit().putString("media_save_mode", mode.name()).apply();
                        currentValue.setText(mode.label);
                    }));
        }
        showBottomSheet(dialog, panel);
    }

    private boolean importFromFiles() {
        return getPreferences(MODE_PRIVATE).getBoolean("media_import_files", false);
    }

    private void showMediaImportSettings(TextView currentValue) {
        boolean filesSelected = importFromFiles();
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = sheetPanel("导入方式", "应用于收藏和作品记录中的导入");
        addCheckedSheetChoice(panel, "相册", "在系统相册中选择素材", R.drawable.record_import,
                !filesSelected, () -> dismissCardDialog(dialog, () -> setMediaImportMode(false, currentValue)));
        addCheckedSheetChoice(panel, "文件", "从内部存储根目录开始浏览", R.drawable.blue_folder,
                filesSelected, () -> dismissCardDialog(dialog, () -> setMediaImportMode(true, currentValue)));
        showBottomSheet(dialog, panel);
    }

    private void setMediaImportMode(boolean files, TextView currentValue) {
        getPreferences(MODE_PRIVATE).edit().putBoolean("media_import_files", files).apply();
        currentValue.setText(files ? "文件" : "相册");
    }

    private boolean canOrganizeSave(ScanRecord record) {
        return record != null && (record.kuaishou || WorkMedia.isShareLink(record.link, isXhsLink(record.link)));
    }

    private void saveMedia(ArrayList<Media> items, ScanRecord record, Runnable before, Runnable after) {
        if (items.isEmpty()) {
            showBottomNotice("没有可保存的文件");
            return;
        }
        MediaSaveMode mode = mediaSaveMode();
        if (mode == MediaSaveMode.DIRECT || !canOrganizeSave(record)) {
            saveMediaBatch(items, SAVE_DIRECTORY, null, before, after);
            return;
        }
        String platform = record.kuaishou ? "快手" : isXhsLink(record.link) ? "小红书" : "抖音";
        String name = WorkExport.name(record.caption, record.link);
        String path = SAVE_DIRECTORY + "/" + platform;
        if (mode == MediaSaveMode.WORK_FOLDER) {
            saveMediaBatch(items, path + "/" + name, null, before, after);
        } else {
            saveMediaBatch(items, path, name + ".zip", before, after);
        }
    }

    private void saveMediaBatch(ArrayList<Media> items, String directory, String zipName, Runnable before, Runnable after) {
        if (before != null) before.run();
        worker.execute(() -> {
            if (zipName != null) {
                try {
                    ArrayList<File> files = new ArrayList<>();
                    for (Media item : items) files.add(item.file);
                    writeDownload(zipName, "application/zip", directory, out -> WorkExport.writeZip(out, files));
                    runOnUiThread(() -> {
                        if (after != null) after.run();
                        showBottomNotice("已保存 ZIP · " + items.size() + " 项素材");
                    });
                } catch (Exception error) {
                    runOnUiThread(() -> {
                        if (after != null) after.run();
                        showMessage(R.drawable.extract_download, "ZIP 未能保存", fileFailureReason(error));
                    });
                }
                return;
            }
            int saved = 0;
            for (Media item : items) {
                try { saveToDownloads(item, directory); saved++; } catch (Exception ignored) { }
            }
            int success = saved, failed = items.size() - saved;
            runOnUiThread(() -> {
                if (after != null) after.run();
                showSaveBatchResult(success, failed);
            });
        });
    }

    private void showKuaishou() {
        showKuaishou(null);
    }

    private void showKuaishou(ScanRecord record) {
        stopScanForNavigation();
        resetPages();
        showingKuaishou = true;
        currentPage = -6;
        new FavoritesScreen(false, true, record).show();
    }

    private void transferPostMedia(ScanRecord record, URL url, int limit, OutputStream output) throws Exception {
        if (record.kuaishou) KuaishouPost.transfer(url, limit, true, output);
        else WorkMedia.transfer(url, isXhsLink(record.link), limit, output);
    }

    private Media readPostImage(URL url, ScanRecord record) throws Exception {
        File folder = record.folder;
        String kind = record.kuaishou ? "快手正文图片" : "作品图片";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        transferPostMedia(record, url, (record.kuaishou ? 12 : 24) * 1024 * 1024, buffer);
        byte[] bytes = buffer.toByteArray();
        String mime = format(bytes);
        if (mime == null || !mime.startsWith("image/")) throw new java.io.IOException("图片格式无法识别");
        StringBuilder hash = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) hash.append(String.format(Locale.ROOT, "%02x", b & 255));
        String extension = mime.equals("image/jpeg") ? ".jpg" : "." + mime.substring(6);
        if (!folder.isDirectory() && !folder.mkdirs()) throw new java.io.IOException("无法创建暂存目录");
        File file = new File(folder, hash + extension);
        File temp = File.createTempFile("image-", ".tmp", folder);
        try {
            Files.write(temp.toPath(), bytes);
            Drawable preview = decodeThumbnail(temp);
            if (preview == null) throw new java.io.IOException("图片无法预览");
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new Media(file, hash.toString(), mime, "", kind, preview, preview instanceof Animatable);
        } finally { temp.delete(); }
    }

    private Media readPostVideo(URL url, ScanRecord record) throws Exception {
        return readPostVideo(url, record, "", record.kuaishou ? "快手正文视频" : "作品视频");
    }

    private Media readPostVideo(URL url, ScanRecord record, String commentId, String kind) throws Exception {
        File folder = record.folder;
        if (!folder.isDirectory() && !folder.mkdirs()) throw new java.io.IOException("无法创建暂存目录");
        File pending = File.createTempFile("video-", ".mp4", folder);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (OutputStream out = new java.security.DigestOutputStream(new FileOutputStream(pending), digest)) {
                transferPostMedia(record, url, 160 * 1024 * 1024, out);
            }
            requireVideoTrack(pending);
            StringBuilder hash = new StringBuilder();
            for (byte b : digest.digest()) hash.append(String.format(Locale.ROOT, "%02x", b & 255));
            File file = new File(folder, (commentId.isEmpty() ? hash.toString() : hash.substring(0, 20)) + ".mp4");
            Files.move(pending.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new Media(file, hash.toString(), "video/mp4", commentId, kind, decodeThumbnail(file), true);
        } finally { pending.delete(); }
    }

    private static void requireVideoTrack(File file) throws java.io.IOException {
        android.media.MediaExtractor extractor = new android.media.MediaExtractor();
        try {
            extractor.setDataSource(file.getAbsolutePath());
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                String mime = extractor.getTrackFormat(i).getString(android.media.MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) return;
            }
            throw new java.io.IOException("视频文件无效");
        } finally { extractor.release(); }
    }

    private class FavoritesScreen {
        private android.widget.GridView favoriteGrid;
        private final ArrayList<Media> visibleFavorites = new ArrayList<>();
        private final android.util.LruCache<Media, Drawable.ConstantState> favoriteThumbnails = new android.util.LruCache<>(60);
        private final boolean wechat;
        private final boolean kuaishou;
        private final boolean imported;
        private final WechatImportRecord wechatRecord;
        private final ScanRecord savedKuaishou;
        private ScanRecord kuaishouPostRecord;
        private EditText kuaishouLink;
        private Button kuaishouFetch;
        private TextView kuaishouCaption;
        private TextView kuaishouProgress;
        FavoritesScreen() { this(false); }
        FavoritesScreen(boolean wechat) { this(wechat, false, null); }
        FavoritesScreen(boolean wechat, boolean kuaishou, ScanRecord savedKuaishou) {
            this.wechat = wechat;
            this.kuaishou = kuaishou;
            imported = wechat || kuaishou;
            wechatRecord = wechat ? currentWechatImport : null;
            this.savedKuaishou = savedKuaishou;
            kuaishouPostRecord = savedKuaishou;
        }
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
        private TextView extractedCount;
        private Button select;
        private Button selectAll;
        private Button importFiles;
        private Button share;
        private Button save;
        private Button remove;
        private Button move;
        private LinearLayout groupChips;
        private PostStatusStrip kuaishouPostStatus;
        private String activeGroup;
        private String sourceQuery = "";
        private final HashSet<String> matchingSources = new HashSet<>();
        private int category;
        private boolean selecting;
        private boolean busy;
        private boolean loaded;

        private String screenTitle() {
            return kuaishou ? "快手提取" : wechat ? "微信表情导入" : "收藏";
        }

        private void restoreKuaishou() {
            resetPages();
            showingKuaishou = true;
            currentPage = -6;
            favoriteScreen = this;
            presentSecondary(root);
        }

        private void showPhoneImport(ScanRecord record) {
            kuaishouPostRecord = record;
            kuaishouCaption.setText(record.caption);
            kuaishouCaption.setVisibility(record.caption.isEmpty() ? View.GONE : View.VISIBLE);
            kuaishouProgress.setText("已打开本机提取记录");
            synchronized (record) {
                items.clear();
                items.addAll(record.items);
            }
            loaded = true;
            category = 0;
            positionFilter(false);
            setSelecting(false);
        }

        private void addKuaishouInput(LinearLayout content) {
            EditText link = new EditText(MainActivity.this);
            kuaishouLink = link;
            link.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
            link.setHint("粘贴快手作品链接或分享文案");
            LinearLayout linkRow = readerLinkRow(link, () -> KuaishouPost.extractLink(link.getText().toString()));
            if (batchTask != null) linkRow.setVisibility(View.GONE);
            content.addView(linkRow, new LinearLayout.LayoutParams(-1, dp(46)));

            Button fetch = shareButton("读取正文", v -> {});
            kuaishouFetch = fetch;
            fetch.setBackground(shape(ACCENT, 12));
            fetch.setTextSize(14);
            fetch.setTypeface(homeTypeface(700));
            fetch.setTextColor(Color.WHITE);
            readerIcon(fetch, R.drawable.extract_scan, 20);
            LinearLayout.LayoutParams fetchParams = new LinearLayout.LayoutParams(-1, dp(44));
            fetchParams.topMargin = dp(12);
            content.addView(fetch, fetchParams);

            LinearLayout statusCard = new LinearLayout(MainActivity.this);
            statusCard.setGravity(Gravity.CENTER_VERTICAL);
            statusCard.setPadding(dp(12), dp(12), dp(12), dp(12));
            statusCard.setBackground(card(14));
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
            statusParams.topMargin = dp(12);
            content.addView(statusCard, statusParams);
            ImageView info = icon(R.drawable.extract_info, ACCENT);
            statusCard.addView(info, new LinearLayout.LayoutParams(dp(20), dp(20)));
            ProgressBar spinner = new ProgressBar(MainActivity.this);
            spinner.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
            spinner.setVisibility(View.GONE);
            statusCard.addView(spinner, new LinearLayout.LayoutParams(dp(20), dp(20)));
            TextView progress = homeLabel("粘贴作品链接，读取文案、图片和视频", 12, INK2, 450);
            kuaishouProgress = progress;
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(0, -2, 1);
            progressParams.leftMargin = dp(12);
            statusCard.addView(progress, progressParams);

            kuaishouPostStatus.retry.setOnClickListener(v -> {
                ScanRecord record = kuaishouPostRecord;
                if (busy || record == null || record.processingPage) return;
                busy = true;
                fetch.setEnabled(false);
                updateActions();
                retryPost(record, () -> {
                    busy = false;
                    fetch.setEnabled(true);
                    synchronized (record) {
                        items.clear();
                        items.addAll(record.items);
                    }
                    setSelecting(false);
                });
                kuaishouPostStatus.update(record);
            });

            TextView caption = homeLabel("", 13, INK, 450);
            kuaishouCaption = caption;
            caption.setBackground(card(14));
            caption.setPadding(dp(12), dp(10), dp(12), dp(10));
            caption.setMaxLines(3);
            caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
            caption.setOnClickListener(v -> caption.setMaxLines(caption.getMaxLines() == 3 ? 12 : 3));
            caption.setVisibility(View.GONE);
            LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(-1, -2);
            captionParams.topMargin = dp(8);
            content.addView(caption, captionParams);
            if (savedKuaishou != null) {
                link.setText(savedKuaishou.link);
                caption.setText(savedKuaishou.caption);
                caption.setVisibility(savedKuaishou.caption.isEmpty() ? View.GONE : View.VISIBLE);
                progress.setText("已打开本机提取记录");
            }
            fetch.setOnClickListener(v -> {
                if (busy) return;
                String text = link.getText().toString().trim();
                if (KuaishouPost.extractLink(text) == null) {
                    showTopNotice("请粘贴快手作品链接或分享文案");
                    return;
                }
                BatchExtraction.Task queued = batchTask;
                if (queued != null) {
                    queued.state = BatchExtraction.State.RUNNING;
                    queued.detail = "";
                    saveBatch();
                }
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(link.getWindowToken(), 0);
                link.clearFocus();
                busy = true;
                fetch.setEnabled(false);
                updateActions();
                info.setVisibility(View.GONE);
                spinner.setVisibility(View.VISIBLE);
                progress.setText("正在读取快手正文…");
                worker.execute(() -> {
                    ArrayList<Media> found = new ArrayList<>();
                    String message;
                    KuaishouPost post = null;
                    ScanRecord saved = null;
                    try {
                        post = KuaishouPost.open(text);
                        String id = UUID.randomUUID().toString();
                        File folder = new File(recordsFolder(), id);
                        ScanRecord record = new ScanRecord(id, post.link, System.currentTimeMillis(), folder);
                        record.kuaishou = true;
                        record.caption = post.caption;
                        for (ArrayList<URL> variants : post.images) {
                            JSONArray urls = new JSONArray();
                            for (URL url : variants) urls.put(url.toString());
                            record.postFailures.add(new PostFailure(urls, false));
                        }
                        if (!post.videos.isEmpty()) {
                            JSONArray urls = new JSONArray();
                            for (URL url : post.videos) urls.put(url.toString());
                            record.postFailures.add(new PostFailure(urls, true));
                        }
                        record.postTotal = record.postFailures.size();
                        if (downloadPostFailures(record)) {
                            saved = record;
                            found.addAll(record.items);
                            message = record.postTotal == 0 ? "已读取作品文案" : "作品正文已读取";
                        } else {
                            deleteRecordFiles(record);
                            message = "无法保存提取记录，请检查存储空间后重试";
                        }
                    } catch (Exception error) {
                        String detail = error.getMessage();
                        message = detail != null && (detail.startsWith("请粘贴") || detail.startsWith("公开分享页"))
                                ? detail : "暂时无法读取快手作品，请检查网络或链接后重试";
                    }
                    String statusText = message;
                    ScanRecord newRecord = saved;
                    runOnUiThread(() -> {
                        if (newRecord != null) records.add(0, newRecord);
                        busy = false;
                        fetch.setEnabled(true);
                        spinner.setVisibility(View.GONE);
                        info.setVisibility(View.VISIBLE);
                        progress.setText(statusText);
                        if (newRecord != null) {
                            kuaishouPostRecord = newRecord;
                            items.clear();
                            items.addAll(found);
                            caption.setText(newRecord.caption);
                            caption.setMaxLines(3);
                            caption.setVisibility(newRecord.caption.isEmpty() ? View.GONE : View.VISIBLE);
                            category = 0;
                            positionFilter(false);
                            setSelecting(false);
                        } else updateActions();
                        if (queued != null && batchTask == queued) {
                            if (newRecord == null) batchNeedsAction(statusText);
                            else finishBatchTask(newRecord);
                        }
                    });
                });
            });
        }

        void show() {
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(BG);
            root.setFitsSystemWindows(true);
            LinearLayout header = new LinearLayout(MainActivity.this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(20), 0, dp(20), 0);
            root.addView(header, new LinearLayout.LayoutParams(-1, dp(54)));

            if (imported) {
                ImageView back = icon(kuaishou ? R.drawable.extract_back : R.drawable.home_chevron, INK);
                if (!kuaishou) back.setRotation(180);
                back.setPadding(dp(8), dp(10), dp(8), dp(10));
                back.setContentDescription(wechat ? "返回导入记录" : "返回首页");
                back.setOnClickListener(v -> back());
                header.addView(back, new LinearLayout.LayoutParams(dp(40), dp(44)));
            }
            title = homeLabel(screenTitle(), kuaishou ? 18 : imported ? 20 : 26, INK, kuaishou ? 750 : 800);
            if (kuaishou) title.setGravity(Gravity.CENTER);
            header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            selectAll = shareButton("全选", v -> {
                toggleVisibleSelection(items, selected, this::matchesCategory);
                render();
            });
            LinearLayout.LayoutParams selectAllParams = new LinearLayout.LayoutParams(dp(76), dp(44));
            selectAllParams.rightMargin = dp(6);
            if (!kuaishou) header.addView(selectAll, selectAllParams);
            select = shareButton("多选", v -> setSelecting(!selecting));
            select.setTypeface(homeTypeface(700));
            if (!kuaishou) header.addView(select, new LinearLayout.LayoutParams(dp(68), dp(44)));
            select.setEnabled(false);
            if (!imported) {
                importFiles = shareButton("导入", v -> pickFavoriteMedia(activeGroup));
                importFiles.setTypeface(homeTypeface(700));
                importFiles.setBackground(shape(ACCENT_SOFT, 12));
                LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(dp(68), dp(44));
                importParams.setMarginStart(dp(8));
                header.addView(importFiles, importParams);
                header.addView(settingsButton(), new LinearLayout.LayoutParams(dp(44), dp(44)));
            }
            if (kuaishou && savedKuaishou == null) {
                TextView history = homeLabel(batchTask != null ? "队列" : "记录", 12, INK2, 650);
                history.setGravity(Gravity.CENTER);
                history.setSingleLine(true);
                readerIcon(history, R.drawable.extract_history, 17);
                history.setOnClickListener(v -> {
                    if (returnToBatch()) return;
                    if (busy) return;
                    kuaishouRecordsOrigin = this;
                    kuaishouRecords = true;
                    recordsFromSettings = false;
                    showRecords();
                });
                header.addView(history, new LinearLayout.LayoutParams(dp(72), dp(40)));
            } else if (kuaishou) {
                header.addView(new View(MainActivity.this), new LinearLayout.LayoutParams(dp(40), dp(44)));
            }
            LinearLayout content = root;
            if (kuaishou) {
                header.setPadding(dp(16), dp(6), dp(16), dp(6));
                ScrollView scroll = new ScrollView(MainActivity.this);
                scroll.setFillViewport(true);
                root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
                content = new LinearLayout(MainActivity.this);
                content.setOrientation(LinearLayout.VERTICAL);
                content.setPadding(dp(16), dp(10), dp(16), dp(20));
                scroll.addView(content);
                kuaishouPostStatus = new PostStatusStrip();
                addKuaishouInput(content);
                LinearLayout resultHeader = new LinearLayout(MainActivity.this);
                resultHeader.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(-1, dp(36));
                resultParams.topMargin = dp(22);
                content.addView(resultHeader, resultParams);
                resultHeader.addView(homeLabel("提取结果", 18, INK, 750),
                        new LinearLayout.LayoutParams(0, -2, 1));
                selectAllParams.height = dp(36);
                resultHeader.addView(selectAll, selectAllParams);
                resultHeader.addView(select, new LinearLayout.LayoutParams(dp(70), dp(36)));
                readerIcon(select, R.drawable.extract_select, 16);
                if (batchTask == null) addPhoneImportButton(content);
                content.addView(kuaishouPostStatus, postStatusParams());
            }
            if (wechat) {
                LinearLayout introduction = new LinearLayout(MainActivity.this);
                introduction.setOrientation(LinearLayout.VERTICAL);
                introduction.setPadding(dp(20), dp(8), dp(20), dp(8));
                root.addView(introduction, new LinearLayout.LayoutParams(-1, -2));
                TextView name = homeLabel(wechatRecord == null ? "导入表情" : wechatRecord.name, 14, INK, 650);
                name.setMaxLines(2);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                introduction.addView(name);
                introduction.addView(homeLabel("仅显示本条记录中的图片与动图", 12, INK2, 400));
            }
            if (!imported) {
                LinearLayout groups = new LinearLayout(MainActivity.this);
                LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(-1, dp(42));
                groupParams.setMargins(dp(20), 0, dp(20), 0);
                root.addView(groups, groupParams);
                HorizontalScrollView chips = new HorizontalScrollView(MainActivity.this);
                chips.setHorizontalScrollBarEnabled(false);
                groups.addView(chips, new LinearLayout.LayoutParams(0, -1, 1));
                groupChips = new LinearLayout(MainActivity.this);
                groupChips.setGravity(Gravity.CENTER_VERTICAL);
                chips.addView(groupChips, new HorizontalScrollView.LayoutParams(-2, -1));
                ImageView manage = icon(R.drawable.blue_folder, INK);
                manage.setPadding(dp(10), dp(10), dp(10), dp(10));
                manage.setContentDescription("管理分组");
                manage.setOnClickListener(v -> manageGroups());
                groups.addView(manage, new LinearLayout.LayoutParams(dp(44), dp(42)));

                EditText search = mediaSourceSearch("按作品标题、链接或平台搜索");
                LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(42));
                searchParams.setMargins(dp(20), dp(8), dp(20), 0);
                root.addView(search, searchParams);
                onMediaSearchChanged(search, () -> {
                    sourceQuery = search.getText().toString().trim().toLowerCase(Locale.ROOT);
                    setSelecting(false);
                });
                root.setFocusableInTouchMode(true);
                root.requestFocus();
            }

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
            if (kuaishou) {
                tabParams.height = dp(36);
                tabParams.setMargins(0, dp(12), 0, dp(12));
            }
            content.addView(filterTrack, tabParams);
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
                    View results = imported ? list : favoriteGrid;
                    results.animate().cancel();
                    render();
                    if (motionEnabled) {
                        results.setAlpha(0f);
                        results.setTranslationX(dp(previous < target ? 12 : -12));
                        results.animate().alpha(1f).translationX(0f).setDuration(220).start();
                    } else {
                        results.setAlpha(1f);
                        results.setTranslationX(0f);
                    }
                });
                filters[i] = tab;
                tabs.addView(tab, new LinearLayout.LayoutParams(0, -1, 1));
            }

            list.setOrientation(LinearLayout.VERTICAL);
            if (kuaishou) {
                content.addView(list);
                View divider = new View(MainActivity.this);
                divider.setBackgroundColor(LINE);
                root.addView(divider, new LinearLayout.LayoutParams(-1, 1));
            } else if (wechat) {
                ScrollView scroll = new ScrollView(MainActivity.this);
                root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
                list.setPadding(dp(20), dp(12), dp(20), dp(20));
                scroll.addView(list);
            } else {
                favoriteGrid = new android.widget.GridView(MainActivity.this);
                favoriteGrid.setNumColumns(3);
                favoriteGrid.setHorizontalSpacing(dp(6));
                favoriteGrid.setVerticalSpacing(dp(8));
                favoriteGrid.setPadding(dp(20), dp(20), dp(20), dp(20));
                favoriteGrid.setClipToPadding(false);
                favoriteGrid.setStretchMode(android.widget.GridView.STRETCH_COLUMN_WIDTH);
                favoriteGrid.setSelector(android.R.color.transparent);
                FrameLayout results = new FrameLayout(MainActivity.this);
                root.addView(results, new LinearLayout.LayoutParams(-1, 0, 1));
                results.addView(favoriteGrid, new FrameLayout.LayoutParams(-1, -1));
                list.setPadding(dp(20), dp(12), dp(20), dp(20));
                results.addView(list, new FrameLayout.LayoutParams(-1, -1));
                favoriteGrid.setEmptyView(list);
                favoriteGrid.setAdapter(new android.widget.BaseAdapter() {
                    @Override public int getCount() { return visibleFavorites.size(); }
                    @Override public Media getItem(int position) { return visibleFavorites.get(position); }
                    @Override public long getItemId(int position) { return position; }
                    @Override public View getView(int position, View recycled, android.view.ViewGroup parent) {
                        FavoriteTile tile = recycled instanceof FavoriteTile ? (FavoriteTile) recycled : new FavoriteTile();
                        tile.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, favoriteGrid.getColumnWidth()));
                        tile.bind(getItem(position));
                        return tile;
                    }
                });
                favoriteGrid.setOnItemClickListener((parent, view, position, id) -> ((FavoriteTile) view).activate());
            }

            actions = new LinearLayout(MainActivity.this);
            actions.setPadding(dp(12), dp(6), dp(12), dp(6));
            root.addView(actions, new LinearLayout.LayoutParams(-1, dp(kuaishou ? 64 : 60)));
            if (kuaishou) {
                actions.setGravity(Gravity.CENTER_VERTICAL);
                actions.setPadding(dp(16), dp(8), dp(16), dp(8));
                actions.setBackgroundColor(SURFACE);
                extractedCount = homeLabel("已提取 0 个", 14, INK, 750);
                extractedCount.setSingleLine(true);
                extractedCount.setAutoSizeTextTypeUniformWithConfiguration(10, 14, 1,
                        android.util.TypedValue.COMPLEX_UNIT_SP);
                actions.addView(extractedCount, new LinearLayout.LayoutParams(0, dp(40), 1));
                extractedCount.setGravity(Gravity.CENTER_VERTICAL);
            }
            share = addAction("分享", v -> shareMedia(actionItems()));
            save = addAction(imported ? "保存全部" : "保存", v -> {
                saveMedia(actionItems(), kuaishouPostRecord, () -> {
                    busy = true;
                    updateActions();
                }, () -> {
                    busy = false;
                    updateActions();
                });
            });
            if (imported) {
                save.setBackground(shape(ACCENT, 10));
                save.setTextColor(Color.WHITE);
            }
            remove = addAction(imported ? "收藏" : "取消收藏", v -> {
                if (imported) collectImported();
                else confirmRemove();
            });
            remove.setTextColor(imported ? ACCENT : DANGER);
            if (!imported) {
                move = addAction("移到分组", v -> showGroupPicker(selection()));
                move.setBackground(shape(ACCENT, 10));
                move.setTextColor(Color.WHITE);
                move.setTypeface(homeTypeface(700));
                actions.removeView(move);
                actions.addView(move, 0);
            }
            if (kuaishou) {
                save.setSingleLine(true);
                save.setTypeface(homeTypeface(700));
                readerIcon(save, R.drawable.extract_download, 16);
                actions.removeView(save);
                actions.addView(save, new LinearLayout.LayoutParams(dp(108), dp(40)));
                for (Button action : new Button[]{share, remove}) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) action.getLayoutParams();
                    params.width = dp(46);
                    params.height = dp(40);
                    params.weight = 0;
                    params.setMargins(0, 0, dp(4), 0);
                    action.setLayoutParams(params);
                    action.setPadding(0, 0, 0, 0);
                }
            }
            favoriteScreen = this;
            if (imported) presentSecondary(root);
            else showMainPage(root, 0);
            loaded = kuaishou && savedKuaishou == null;
            render();

            if (kuaishou && savedKuaishou == null) return;
            worker.execute(() -> {
                ArrayList<Media> found;
                if (savedKuaishou != null) {
                    synchronized (savedKuaishou) { found = new ArrayList<>(savedKuaishou.items); }
                    for (Media item : found) if (item.thumbnail == null) item.thumbnail = decodeThumbnail(item.file);
                } else found = wechat ? readWechat(wechatRecord) : readFavorites();
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
            else if (kuaishou && returnToBatch()) return;
            else if (wechat) showWechat();
            else if (savedKuaishou != null) showRecords();
            else if (kuaishou) showHome();
            else leaveFavorites();
        }

        private ArrayList<Media> actionItems() {
            return imported && !selecting ? new ArrayList<>(items) : selection();
        }

        private void collectImported() {
            ArrayList<Media> snapshot = actionItems();
            if (snapshot.isEmpty()) return;
            showFavoriteDestination(null, "", snapshot.size(), group -> collectImportedInto(snapshot, group), null);
        }

        private void collectImportedInto(ArrayList<Media> snapshot, String group) {
            ScanRecord source = kuaishouPostRecord;
            busy = true;
            updateActions();
            worker.execute(() -> {
                ArrayList<Media> collected = new ArrayList<>();
                for (Media item : snapshot) {
                    try {
                        copyMediaToFavorites(item);
                        collected.add(item);
                    } catch (Exception ignored) { }
                }
                runOnUiThread(() -> {
                    boolean sourcesChanged = false;
                    boolean groupsChanged = false;
                    for (Media item : collected) {
                        groupsChanged |= assignFavoriteGroup(item, group);
                        sourcesChanged |= rememberFavoriteSource(item, source, false);
                    }
                    if (groupsChanged) saveFavoriteGroups();
                    if (sourcesChanged) saveFavoriteSources();
                    int saved = collected.size();
                    busy = false;
                    updateActions();
                    showBottomNotice("已收藏 " + saved + " 个" + (saved < snapshot.size() ? "，失败 " + (snapshot.size() - saved) + " 个" : ""),
                            "查看收藏", () -> openFavorites(kuaishou ? this::restoreKuaishou : MainActivity.this::returnToWechatImport));
                });
            });
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

        private boolean matchesCategory(Media item) {
            return matchesFavoriteView(item) && (category == 0 || category == type(item));
        }

        private boolean matchesFavoriteView(Media item) {
            return inFavoriteGroup(item) && (sourceQuery.isEmpty() || matchingSources.contains(item.hash));
        }

        private void updateSourceMatches() {
            matchingSources.clear();
            if (sourceQuery.isEmpty()) return;
            HashSet<String> missing = new HashSet<>();
            for (Media item : items) {
                JSONObject source = favoriteSources.optJSONObject(item.hash);
                String link = source == null ? "" : source.optString("link");
                if (source != null && source.optBoolean("phoneImport") && PHONE_MEDIA_KIND.contains(sourceQuery))
                    matchingSources.add(item.hash);
                if (!isFavoriteSourceLink(link)) missing.add(item.hash);
                else if (matchesMediaSource(link, source.optString("caption"), sourceQuery)) matchingSources.add(item.hash);
            }
            for (ScanRecord record : records) {
                if (missing.isEmpty()) break;
                if (!isFavoriteSourceLink(record.link)) continue;
                boolean matches = matchesMediaSource(record.link, record.caption, sourceQuery);
                synchronized (record) {
                    for (Media item : record.items) {
                        if (missing.remove(item.hash) && (matches
                                || PHONE_MEDIA_KIND.equals(item.kind) && PHONE_MEDIA_KIND.contains(sourceQuery))) {
                            matchingSources.add(item.hash);
                        }
                    }
                }
            }
        }

        private boolean inFavoriteGroup(Media item) {
            String group = favoriteAssignments.get(item.hash);
            if (activeGroup == null) return imported || !privateFavoritePins.containsKey(group);
            return activeGroup.isEmpty() ? group == null : activeGroup.equals(group);
        }

        private void selectFavoriteGroup(String name) {
            if (busy || java.util.Objects.equals(activeGroup, name)) return;
            verifyFavoriteGroup(name, () -> {
                activeGroup = name;
                category = 0;
                positionFilter(false);
                setSelecting(false);
            });
        }

        private void showGroupPicker(ArrayList<Media> moving) {
            if (busy || (moving != null && moving.isEmpty())) return;
            android.app.Dialog dialog = new android.app.Dialog(MainActivity.this);
            LinearLayout panel = dialogPanel(R.drawable.dialog_heart, false,
                    moving == null ? "查看分组" : "移到分组", moving == null ? null : "已选 " + moving.size() + " 项");
            ScrollView scroll = dialogScroll();
            panel.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
            LinearLayout choices = new LinearLayout(MainActivity.this);
            choices.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(choices);
            ArrayList<String> names = new ArrayList<>();
            if (moving == null) names.add(null);
            names.add("");
            names.addAll(favoriteGroups);
            for (String name : names) {
                addRetryChoice(choices, groupName(name), "", R.drawable.dialog_heart,
                        () -> dismissCardDialog(dialog, () -> {
                            if (moving == null) {
                                selectFavoriteGroup(name);
                            } else moveToGroup(moving, name);
                        }));
            }
            dialogActions(dialog, panel, "取消", moving == null ? null : "新建分组", false, null,
                    () -> editGroupName(null, name -> moveToGroup(moving, name)));
            showCardDialog(dialog, panel);
        }

        private void moveToGroup(ArrayList<Media> moving, String name) {
            for (Media item : moving) {
                if (name.isEmpty()) favoriteAssignments.remove(item.hash);
                else if (item.file.isFile()) favoriteAssignments.put(item.hash, name);
            }
            saveFavoriteGroups();
            setSelecting(false);
            showBottomNotice("已移动 " + moving.size() + " 项到「" + groupName(name) + "」");
        }

        private void manageGroups() {
            if (busy || !loaded) return;
            if (privateFavoritePins.containsKey(activeGroup)) {
                activeGroup = null;
                category = 0;
                setSelecting(false);
            }
            resetPages();
            showingGroupManager = true;
            currentPage = -7;
            LinearLayout page = pageRoot();
            header(page, "管理分组", true, this::leaveGroupManager);

            ScrollView scroll = new ScrollView(MainActivity.this);
            page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            LinearLayout groups = new LinearLayout(MainActivity.this);
            groups.setOrientation(LinearLayout.VERTICAL);
            groups.setPadding(dp(16), dp(6), dp(16), dp(12));
            scroll.addView(groups);
            renderManagedGroups(groups);

            Button similar = dialogButton("相似素材整理", false, false,
                    v -> findSimilarFavorites(() -> renderManagedGroups(groups)));
            LinearLayout.LayoutParams similarParams = new LinearLayout.LayoutParams(-1, dp(48));
            similarParams.setMargins(dp(16), dp(8), dp(16), 0);
            page.addView(similar, similarParams);
            Button create = dialogButton("新建分组", true, false, v -> editGroupName(null, name -> {
                activeGroup = name;
                category = 0;
                positionFilter(false);
                setSelecting(false);
                renderManagedGroups(groups);
            }));
            readerIcon(create, R.drawable.group_add, 19);
            LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(-1, dp(48));
            createParams.setMargins(dp(16), dp(8), dp(16), dp(16));
            page.addView(create, createParams);
            presentSecondary(page);
        }

        private void findSimilarFavorites(Runnable refreshGroups) {
            if (busy || recordTransferBusy || importingFavoriteMedia) return;
            ArrayList<Media> snapshot = new ArrayList<>();
            for (Media item : items) {
                if (!privateFavoritePins.containsKey(favoriteAssignments.get(item.hash))) snapshot.add(item);
            }
            android.app.Dialog progress = new android.app.Dialog(MainActivity.this);
            progress.setCancelable(false);
            showCardDialog(progress, dialogPanel(R.drawable.record_search, false, "正在查找相似素材…",
                    "比较图片与动图封面。"));
            worker.execute(() -> {
                java.util.LinkedHashMap<ImageFingerprint, ArrayList<Media>> groups = new java.util.LinkedHashMap<>();
                for (Media item : snapshot) {
                    if (!item.mime.startsWith("image/")) continue;
                    try {
                        ImageFingerprint fingerprint = new ImageFingerprint(item);
                        ImageFingerprint anchor = fingerprint;
                        for (ImageFingerprint candidate : groups.keySet()) {
                            if (!candidate.resembles(fingerprint)) continue;
                            anchor = candidate;
                            break;
                        }
                        groups.computeIfAbsent(anchor, key -> new ArrayList<>()).add(item);
                    } catch (Exception ignored) { }
                }
                ArrayList<ArrayList<Media>> matches = new ArrayList<>(groups.values());
                matches.removeIf(group -> group.size() < 2);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    progress.dismiss();
                    if (matches.isEmpty()) showMessage(R.drawable.record_search, "未找到相似素材", "已检查收藏中的图片和动图封面。");
                    else reviewSimilarFavorites(matches, refreshGroups);
                });
            });
        }

        private void reviewSimilarFavorites(ArrayList<ArrayList<Media>> groups, Runnable refreshGroups) {
            android.app.Dialog dialog = new android.app.Dialog(MainActivity.this);
            LinearLayout panel = dialogPanel(0, false, "相似素材整理", "比较图片和动图封面。勾选要取消收藏的素材。");
            TextView position = homeLabel("", 13, INK, 700);
            position.setPadding(0, dp(12), 0, dp(6));
            panel.addView(position);
            ScrollView scroll = dialogScroll();
            panel.addView(scroll);
            LinearLayout candidates = new LinearLayout(MainActivity.this);
            candidates.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(candidates);
            LinearLayout navigation = new LinearLayout(MainActivity.this);
            panel.addView(navigation);
            Button previous = dialogButton("上一组", false, false, null);
            Button next = dialogButton("下一组", false, false, null);
            navigation.addView(previous, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(0, -2, 1);
            nextParams.leftMargin = dp(8);
            navigation.addView(next, nextParams);
            HashSet<Media> marked = new HashSet<>();
            int[] index = {0};
            Button removeMarked = dialogButton("取消收藏选中项", true, true, v ->
                    confirmRemove(new ArrayList<>(marked), () -> {
                        dismissCardDialog(dialog, null);
                        refreshGroups.run();
                    }));
            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(-1, -2);
            removeParams.topMargin = dp(10);
            panel.addView(removeMarked, removeParams);
            Runnable updateSelection = () -> {
                removeMarked.setText("取消收藏选中项（" + marked.size() + "）");
                removeMarked.setEnabled(!marked.isEmpty());
                removeMarked.setAlpha(marked.isEmpty() ? 0.45f : 1f);
            };
            Runnable render = () -> {
                ArrayList<Media> group = groups.get(index[0]);
                position.setText("第 " + (index[0] + 1) + " / " + groups.size() + " 组 · " + group.size() + " 项");
                candidates.removeAllViews();
                LinearLayout row = null;
                for (int i = 0; i < group.size(); i++) {
                    Media item = group.get(i);
                    if (i % 2 == 0) {
                        row = new LinearLayout(MainActivity.this);
                        candidates.addView(row, new LinearLayout.LayoutParams(-1, -2));
                    }
                    LinearLayout cell = new LinearLayout(MainActivity.this);
                    cell.setOrientation(LinearLayout.VERTICAL);
                    cell.setPadding(dp(3), 0, dp(3), dp(8));
                    row.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
                    ImageView image = recordCover(item);
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    cell.addView(image, new LinearLayout.LayoutParams(-1, dp(104)));
                    android.widget.CheckBox check = new android.widget.CheckBox(MainActivity.this);
                    check.setText(groupName(favoriteAssignments.getOrDefault(item.hash, "")));
                    check.setTextColor(INK);
                    check.setTextSize(12);
                    check.setSingleLine(true);
                    check.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    check.setContentDescription("选择素材 " + (i + 1) + " · " + check.getText());
                    check.setChecked(marked.contains(item));
                    check.setOnCheckedChangeListener((button, checked) -> {
                        if (checked) marked.add(item);
                        else marked.remove(item);
                        updateSelection.run();
                    });
                    image.setOnClickListener(v -> check.toggle());
                    cell.addView(check, new LinearLayout.LayoutParams(-1, dp(40)));
                }
                if (group.size() % 2 != 0) row.addView(new View(MainActivity.this), new LinearLayout.LayoutParams(0, 0, 1));
                scroll.scrollTo(0, 0);
                previous.setEnabled(index[0] > 0);
                next.setEnabled(index[0] + 1 < groups.size());
                previous.setAlpha(previous.isEnabled() ? 1f : 0.45f);
                next.setAlpha(next.isEnabled() ? 1f : 0.45f);
            };
            previous.setOnClickListener(v -> { index[0]--; render.run(); });
            next.setOnClickListener(v -> { index[0]++; render.run(); });
            dialogActions(dialog, panel, "关闭", null, false, null, null);
            render.run();
            updateSelection.run();
            showCardDialog(dialog, panel, 380, 24);
        }

        private void leaveGroupManager() {
            showingGroupManager = false;
            showingFavorites = true;
            render();
            showMainPage(root, 0);
        }

        private void renderManagedGroups(LinearLayout groups) {
            groups.removeAllViews();
            if (favoriteGroups.isEmpty()) {
                TextView empty = homeLabel("还没有自定义分组", 13, INK2, 450);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(dp(16), dp(30), dp(16), dp(30));
                empty.setBackground(card(14));
                groups.addView(empty, new LinearLayout.LayoutParams(-1, -2));
                return;
            }
            java.util.HashMap<String, Integer> counts = favoriteGroupCounts();
            for (String group : favoriteGroups) {
                int count = counts.getOrDefault(group, 0);
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(10), dp(8), dp(10));
                row.setBackground(card(14));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.bottomMargin = dp(10);
                groups.addView(row, params);

                ImageView folder = icon(R.drawable.blue_folder, ACCENT);
                folder.setPadding(dp(11), dp(11), dp(11), dp(11));
                folder.setBackground(shape(ACCENT_SOFT, 12));
                row.addView(folder, new LinearLayout.LayoutParams(dp(50), dp(50)));

                LinearLayout info = new LinearLayout(MainActivity.this);
                info.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1);
                infoParams.leftMargin = dp(12);
                row.addView(info, infoParams);
                TextView title = homeLabel(group, 16, INK, 750);
                title.setSingleLine(true);
                title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                info.addView(title);
                info.addView(homeLabel(privateFavoritePins.containsKey(group) ? "私密" : "公开 · " + count + " 项", 12, INK2, 450));

                TextView more = homeLabel("⋯", 22, INK, 750);
                more.setGravity(Gravity.CENTER);
                more.setContentDescription(group + "的分组操作");
                more.setOnClickListener(v -> editGroup(group, count, v, groups));
                row.addView(more, new LinearLayout.LayoutParams(dp(44), dp(48)));
            }
        }

        private void editGroup(String group, int count, View anchor, LinearLayout groups) {
            LinearLayout options = new LinearLayout(MainActivity.this);
            options.setOrientation(LinearLayout.VERTICAL);
            PopupWindow popup = recordPopup(options, 154);
            options.setPadding(dp(5), dp(5), dp(5), dp(5));
            addRecordAction(options, "重命名", R.drawable.group_edit, INK, () -> {
                popup.dismiss();
                verifyFavoriteGroup(group, () -> editGroupName(group, name -> renderManagedGroups(groups)));
            });
            boolean isPrivate = privateFavoritePins.containsKey(group);
            addRecordAction(options, isPrivate ? "设为公开" : "设为私密", R.drawable.blue_folder, INK, () -> {
                popup.dismiss();
                if (isPrivate) {
                    verifyFavoriteGroup(group, () -> {
                        privateFavoritePins.remove(group);
                        saveFavoriteGroups();
                        renderManagedGroups(groups);
                    });
                } else {
                    showPin("设置私密收藏夹密码", "设置 6 位数字密码，打开「" + group + "」时需要验证。",
                            "设为私密", "", pin -> {
                                privateFavoritePins.put(group, newPinVerifier(pin));
                                return true;
                            }, () -> {
                                if (group.equals(activeGroup)) activeGroup = null;
                                saveFavoriteGroups();
                                setSelecting(false);
                                renderManagedGroups(groups);
                            });
                }
            });
            if (count > 0) addRecordAction(options, "导出收藏夹", R.drawable.extract_download, INK, () -> {
                popup.dismiss();
                verifyFavoriteGroup(group, () -> chooseGroupExport(group));
            });
            addRecordAction(options, "删除分组", R.drawable.dialog_trash, DANGER, () -> {
                popup.dismiss();
                verifyFavoriteGroup(group, () -> confirmDeleteFavoriteGroup(group, count, groups));
            });
            popup.showAsDropDown(anchor, 0, dp(4), Gravity.END);
        }

        private void confirmDeleteFavoriteGroup(String group, int count, LinearLayout groups) {
            android.app.Dialog dialog = new android.app.Dialog(MainActivity.this);
            LinearLayout panel = sheetPanel("删除分组？", null);
            TextView summary = homeLabel(group + " · " + count + " 项", 14, INK, 650);
            summary.setGravity(Gravity.CENTER_VERTICAL);
            summary.setPadding(dp(12), dp(10), dp(12), dp(10));
            Drawable folder = getDrawable(R.drawable.blue_folder).mutate();
            folder.setTint(DANGER);
            folder.setBounds(0, 0, dp(20), dp(20));
            summary.setCompoundDrawablesRelative(folder, null, null, null);
            summary.setCompoundDrawablePadding(dp(10));
            summary.setBackground(shape(ACCENT_SOFT, 10));
            LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
            summaryParams.topMargin = dp(12);
            panel.addView(summary, summaryParams);
            TextView detail = homeLabel("组内素材会移到「未分组」。", 14, INK2, 450);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
            detailParams.topMargin = dp(12);
            panel.addView(detail, detailParams);
            dialogActions(dialog, panel, "保留分组", "删除分组", true, null, () -> {
                favoriteGroups.remove(group);
                favoriteAssignments.values().removeIf(group::equals);
                privateFavoritePins.remove(group);
                if (group.equals(activeGroup)) activeGroup = null;
                saveFavoriteGroups();
                setSelecting(false);
                renderManagedGroups(groups);
            });
            presentBottomSheet(dialog, panel);
        }

        private java.util.HashMap<String, Integer> favoriteGroupCounts() {
            java.util.HashMap<String, Integer> counts = new java.util.HashMap<>();
            for (Media item : items) counts.merge(favoriteAssignments.get(item.hash), 1, Integer::sum);
            return counts;
        }

        private void renderGroupChips() {
            groupChips.removeAllViews();
            java.util.HashMap<String, Integer> counts = favoriteGroupCounts();
            ArrayList<String> names = new ArrayList<>();
            names.add(null);
            names.addAll(favoriteGroups);
            names.add("");
            int publicCount = items.size();
            for (String group : privateFavoritePins.keySet()) publicCount -= counts.getOrDefault(group, 0);
            for (String name : names) {
                boolean active = java.util.Objects.equals(activeGroup, name);
                int count = name == null ? publicCount : counts.getOrDefault(name.isEmpty() ? null : name, 0);
                boolean locked = privateFavoritePins.containsKey(name) && !active;
                String suffix = locked ? " · 私密" : loaded ? " " + count : "";
                Button chip = shareButton(groupName(name) + suffix, v -> selectFavoriteGroup(name));
                chip.setSingleLine(true);
                chip.setMinimumWidth(0);
                chip.setMinWidth(0);
                chip.setMaxWidth(dp(156));
                chip.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
                chip.setPadding(dp(13), 0, dp(13), 0);
                chip.setTextColor(active ? Color.WHITE : INK2);
                chip.setBackground(shape(active ? ACCENT : TRACK, 18));
                chip.setSelected(active);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(34));
                params.rightMargin = dp(6);
                groupChips.addView(chip, params);
            }
        }

        private void render() {
            updateSourceMatches();
            visibleFavorites.clear();
            int[] counts = new int[categories.length];
            for (Media item : items) {
                if (matchesFavoriteView(item)) {
                    counts[0]++;
                    counts[type(item)]++;
                    if (category == 0 || category == type(item)) visibleFavorites.add(item);
                }
            }
            if (groupChips != null) renderGroupChips();
            if (kuaishouPostStatus != null) kuaishouPostStatus.update(kuaishouPostRecord);
            for (int i = 0; i < filters.length; i++) {
                boolean active = category == i;
                filters[i].setSelected(active);
                filters[i].setText(categories[i] + " " + counts[i]);
                filters[i].setTextColor(active ? Color.WHITE : INK2);
                filters[i].setBackground(null);
            }
            list.removeAllViews();
            if (favoriteGrid != null) {
                ((android.widget.BaseAdapter) favoriteGrid.getAdapter()).notifyDataSetChanged();
            } else {
                LinearLayout row = null;
                int size = (getResources().getDisplayMetrics().widthPixels - dp(kuaishou ? 44 : 52)) / 3;
                for (int i = 0; i < visibleFavorites.size(); i++) {
                    if (i % 3 == 0) {
                        row = new LinearLayout(MainActivity.this);
                        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                        rowParams.topMargin = dp(8);
                        list.addView(row, rowParams);
                    }
                    FavoriteTile tile = new FavoriteTile();
                    tile.bind(visibleFavorites.get(i));
                    LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(size, size);
                    if (i % 3 != 0) tileParams.leftMargin = dp(6);
                    row.addView(tile, tileParams);
                }
            }
            if (visibleFavorites.isEmpty()) {
                String message = !loaded ? kuaishou ? "正在打开提取记录…" : wechat ? "正在读取表情…" : "正在读取收藏…"
                        : items.isEmpty() ? wechat ? "还没有导入表情\n选择电脑导出的 ZIP 后，图片和动图会显示在这里。"
                        : kuaishou ? "等待提取素材\n读取作品后，图片和视频会显示在这里" : "还没有收藏。在素材预览里点「收藏」即可添加。"
                        : imported ? "这个分类没有素材" : sourceQuery.isEmpty()
                        ? "此分组或分类中没有收藏" : "没有符合该作品来源的收藏";
                TextView empty = label(message, kuaishou ? 12 : 14, INK2, false);
                if (kuaishou) {
                    empty.setBackground(card(16));
                    empty.setLineSpacing(dp(5), 1f);
                }
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, dp(24), 0, dp(24));
                list.addView(empty, new LinearLayout.LayoutParams(-1, -2));
            }
            updateActions();
        }

        private class FavoriteTile extends FrameLayout {
            private final ImageView image;
            private final TextView badge;
            private final TextView check;
            private Media item;
            private volatile int binding;
            private boolean requested;

            FavoriteTile() {
                super(MainActivity.this);
                setBackground(card(10));
                setClipToOutline(true);
                image = new ImageView(MainActivity.this) {
                    @Override protected void onDraw(android.graphics.Canvas canvas) {
                        super.onDraw(canvas);
                        if (!imported && !requested && getLocalVisibleRect(new Rect())) loadThumbnail();
                    }
                };
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                addView(image, new FrameLayout.LayoutParams(-1, -1));
                badge = label("", 10, Color.WHITE, true);
                badge.setPadding(dp(6), dp(2), dp(6), dp(2));
                FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
                badgeParams.setMargins(dp(6), 0, 0, dp(6));
                addView(badge, badgeParams);
                check = label("", 16, Color.WHITE, true);
                check.setGravity(Gravity.CENTER);
                FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.END | Gravity.TOP);
                checkParams.setMargins(0, dp(6), dp(6), 0);
                addView(check, checkParams);
                if (imported) setOnClickListener(v -> activate());
            }

            void activate() {
                if (!selecting) {
                    preview(item, items, FavoritesScreen.this::matchesCategory);
                    return;
                }
                if (!selected.add(item)) selected.remove(item);
                updateTile(this, check, item);
                updateActions();
            }

            void bind(Media next) {
                binding++;
                item = next;
                Drawable.ConstantState cached = favoriteThumbnails.get(item);
                Drawable thumbnail = cached == null ? item.thumbnail : cached.newDrawable(getResources());
                image.setImageDrawable(thumbnail);
                requested = thumbnail != null;
                int type = type(item);
                badge.setVisibility(type == 1 ? GONE : VISIBLE);
                badge.setText(kuaishou && PHONE_MEDIA_KIND.equals(item.kind) ? PHONE_MEDIA_KIND : categories[type]);
                badge.setBackground(shape(type == 2 ? ACCENT : DARK, 6));
                check.setVisibility(selecting ? VISIBLE : GONE);
                updateTile(this, check, item);
            }

            private void loadThumbnail() {
                requested = true;
                Media target = item;
                int request = binding;
                worker.execute(() -> {
                    if (request != binding) return;
                    Drawable thumbnail = decodeThumbnail(target.file);
                    runOnUiThread(() -> {
                        if (request != binding || favoriteScreen != FavoritesScreen.this || isDestroyed()) return;
                        Drawable.ConstantState state = thumbnail == null ? null : thumbnail.getConstantState();
                        if (state != null) favoriteThumbnails.put(target, state);
                        image.setImageDrawable(thumbnail);
                    });
                });
            }
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
            updateSelectAll(selectAll, selecting, busy, items, selected, this::matchesCategory);
            if (importFiles != null) {
                importFiles.setVisibility(selecting ? View.GONE : View.VISIBLE);
                importFiles.setEnabled(!busy && !importingFavoriteMedia);
            }
            title.setText(selecting && !kuaishou ? "已选 " + selected.size() + " 项" : screenTitle());
            if (extractedCount != null) extractedCount.setText(selecting
                    ? "已选 " + selected.size() + " 项" : "已提取 " + items.size() + " 个");
            select.setText(selecting ? "完成" : "多选");
            select.setSelected(selecting);
            select.setBackground(kuaishou && !selecting ? null : shape(selecting ? ACCENT : ACCENT_SOFT, 12));
            select.setTextColor(selecting ? Color.WHITE : ACCENT);
            if (kuaishou) select.getCompoundDrawablesRelative()[0].setTint(select.getCurrentTextColor());
            select.setEnabled(!busy && (selecting || !items.isEmpty()));
            actions.setVisibility(selecting || imported ? View.VISIBLE : View.GONE);
            if (imported) save.setText(selecting ? "保存选中" : "保存全部");
            for (Button button : new Button[]{share, save, remove}) {
                button.setEnabled((imported && !selecting ? !items.isEmpty() : !selected.isEmpty()) && !busy);
                button.setAlpha(button.isEnabled() ? 1f : 0.45f);
            }
            if (move != null) {
                move.setEnabled(!busy && !selected.isEmpty());
                move.setAlpha(move.isEnabled() ? 1f : 0.45f);
            }
        }

        private void confirmRemove() {
            confirmRemove(selection(), null);
        }

        private void confirmRemove(ArrayList<Media> snapshot, Runnable afterRemoval) {
            showConfirmation(R.drawable.dialog_heart, true, "取消收藏这 " + snapshot.size() + " 项？",
                    "选中素材将在回收站保留 7 天。\n扫描记录和已保存的文件仍会保留。",
                    "保留收藏", "取消收藏", null, () -> {
                busy = true;
                updateActions();
                trashFavorites(snapshot, () -> {
                    busy = false;
                    if (favoriteScreen != this) return;
                    updateActions();
                    if (afterRemoval != null) afterRemoval.run();
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
                boolean animated = "image/webp".equals(mime) && bytes[12] == 'V' && bytes[13] == 'P'
                        && bytes[14] == '8' && bytes[15] == 'X' && (bytes[20] & 2) != 0
                        || "image/avif".equals(mime) && bytes[11] == 's';
                items.add(new Media(file, name.substring(0, extension), mime, "", "收藏", null, animated));
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
        if (message.contains("过大")) return "备份过大或文件无效，请重新选择受支持的备份。";
        if (error instanceof SecurityException || error instanceof java.io.FileNotFoundException)
            return fileFailureReason(error);
        if (message.contains("收藏素材")) return "备份中的收藏文件不完整或格式无法识别，请重新选择完整备份。";
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
        showNotice(message, actionLabel, action, bottom && recordUndoPopup == null, 0);
    }

    private void showNotice(String message, String actionLabel, Runnable action, boolean bottom, long undoDuration) {
        if (isFinishing() || isDestroyed()) return;
        boolean undo = undoDuration > 0;
        PopupWindow previous = undo ? recordUndoPopup : noticePopup;
        if (previous != null) previous.dismiss();
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
                if (recordUndoPopup == popup) recordUndoPopup = null;
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
        if (undo) recordUndoPopup = popup;
        else noticePopup = popup;
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
            if (recordUndoPopup == popup) {
                popup.dismiss();
                recordUndoPopup = null;
            }
        }, undo ? undoDuration : action == null ? 2000 : 4000);
    }

    private void saveToDownloads(Media item) throws Exception {
        saveToDownloads(item, SAVE_DIRECTORY);
    }

    private void saveToDownloads(Media item, String directory) throws Exception {
        writeDownload(item.file.getName(), item.mime, directory, out -> Files.copy(item.file.toPath(), out));
    }

    private interface DownloadWriter {
        void write(OutputStream output) throws Exception;
    }

    private void writeDownload(String name, String mime, String directory, DownloadWriter writer) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, name);
        values.put(MediaStore.Downloads.MIME_TYPE, mime);
        values.put(MediaStore.Downloads.RELATIVE_PATH, directory);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri target = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (target == null) throw new Exception("无法创建下载文件");
        try {
            try (OutputStream out = getContentResolver().openOutputStream(target)) {
                if (out == null) throw new Exception("无法写入下载文件");
                writer.write(out);
            }
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (getContentResolver().update(target, values, null, null) == 0) throw new Exception("无法完成保存");
        } catch (Exception error) {
            try { getContentResolver().delete(target, null, null); }
            catch (Exception cleanupError) { error.addSuppressed(cleanupError); }
            throw error;
        }
    }

    @Override public Object onRetainNonConfigurationInstance() {
        return pendingRecordDeletions.toArray(new PendingRecordDeletion[0]);
    }

    @Override protected void onDestroy() {
        scanning = false;
        stripHandler.removeCallbacksAndMessages(null);
        if (themeAnimation != null) themeAnimation.cancel();
        if (noticePopup != null) noticePopup.dismiss();
        if (recordUndoPopup != null) recordUndoPopup.dismiss();
        if (fullscreenPreviewDialog != null) fullscreenPreviewDialog.dismiss();
        if (pendingRestore != null) {
            pendingRestore.close();
            pendingRestore = null;
        }
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
        worker.execute(() -> recycleBin.entries(System.currentTimeMillis()));
        if (showingReader) for (Animatable animation : animations) animation.start();
    }

    private void leaveReader() {
        if (returnToBatch()) return;
        if (viewingSavedRecord && !resumedFromHome) showRecords();
        else showHome();
    }

    @Override public void onBackPressed() {
        if (themeAnimation != null) themeAnimation.end();
        if (showingBatch) showHome();
        else if (showingWechat && currentWechatImport == null) showHome();
        else if (showingWechat) favoriteScreen.back();
        else if (showingKuaishou) favoriteScreen.back();
        else if (showingGroupManager) favoriteScreen.leaveGroupManager();
        else if (showingRecycleBin) showMy();
        else if (showingPasswordSettings) showSettings();
        else if (showingFavorites) favoriteScreen.back();
        else if (showingRecords) leaveRecords();
        else if (showingRecordPlatforms) returnToRecordsOrigin();
        else if (showingReader) leaveReader();
        else if (showingSettings) returnFromSettings();
        else if (showingMy) showHome();
        else super.onBackPressed();
    }
}
