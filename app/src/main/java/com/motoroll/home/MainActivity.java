package com.motoroll.home;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ClipData;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Debug;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.provider.Settings;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Arrays;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Motoroll Home 3.3.19 - SafeSave, Motoroll Lab and adaptive memory profiles.
 * Native views only: optional on-demand weather, no analytics, no background service, and no third-party UI library.
 * App drawer opens with a downward swipe. Drawer scrolling is handled by its own container;
 * it closes only with the close button or Android Back.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MotorollHome";
    private static final int REQUEST_WALLPAPER = 3107;
    private static final int REQUEST_BACKUP_EXPORT = 3110;
    private static final int REQUEST_BACKUP_IMPORT = 3111;
    private static final long MAX_BACKUP_WALLPAPER_BYTES = 32L * 1024L * 1024L;
    private static final int MAX_BACKUP_JSON_BYTES = 2 * 1024 * 1024;
    private static final String PREFS = "motoroll_home_prefs";
    private static final String PREF_WALLPAPER_URI = "launcher_wallpaper_uri";
    private static final String PREF_FORCE_MOTOROLL_BG = "force_motoroll_default_background";
    private static final String PREF_DOCK_PREFIX = "dock_slot_";
    private static final String PREF_DOCK_COUNT = "dock_count"; // 4, 5 or 6
    private static final String PREF_FAVORITE_PACKAGES = "home_favorite_packages";
    private static final String PREF_DRAWER_SWIPE_DIRECTION = "drawer_swipe_direction"; // 0 = down, 1 = up
    private static final String PREF_CLOCK_POSITION = "clock_position";
    private static final String PREF_CLOCK_STYLE = "clock_style";
    private static final String PREF_ICON_SIZE = "icon_size";
    private static final String PREF_WELCOME_SHOWN = "welcome_shown";
    private static final String PREF_DRAWER_MODE = "drawer_mode"; // 0 = vertical, 1 = horizontal pages
    private static final String PREF_LANGUAGE = "app_language"; // system, es, en, pt
    private static final String PREF_WEATHER_ENABLED = "weather_enabled";
    private static final String PREF_WEATHER_CITY = "weather_city";
    private static final String PREF_WEATHER_TEMP = "weather_temperature";
    private static final String PREF_WEATHER_DESCRIPTION = "weather_description";
    private static final String PREF_WEATHER_PLACE = "weather_place";
    private static final String PREF_WEATHER_FETCHED = "weather_fetched_at";
    private static final String PREF_WEATHER_STYLE = "weather_style"; // 0 Motoroll, 1 One UI inspired
    private static final String PREF_WEATHER_POSITION = "weather_position"; // 0 below clock, 1 beside clock
    private static final String PREF_WEATHER_SHAPE = "weather_shape"; // 0 rounded, 1 square, 2 soft-square
    private static final String PREF_HOME_NOTE_COLOR = "home_note_color";
    private static final String PREF_HOME_NOTE_SIZE = "home_note_size";
    private static final String PREF_ICON_SHAPE = "icon_shape"; // 0 rounded, 1 circle, 2 square
    private static final String PREF_ANIMATION_MODE = "animation_mode"; // 0 reduced, 1 smooth, 2 relaxed
    private static final String PREF_DATE_VISIBLE = "date_visible";
    private static final String PREF_BATTERY_WIDGET = "battery_widget";
    private static final String PREF_PULSE_WIDGET = "pulse_widget";
    private static final String PREF_MEMORY_MODE = "memory_profile"; // 0 Auto, 1 RAM saver, 2 balanced
    private static final String PREF_HIDDEN_APPS = "hidden_apps";
    private static final String PREF_PROFILES = "appearance_profiles";
    private static final String PREF_HOME_NOTE = "home_note";
    private static final String PREF_HOME_SEARCH_VISIBLE = "home_search_visible";
    private static final String PREF_HOME_NOTE_X = "home_note_x";
    private static final String PREF_HOME_NOTE_Y = "home_note_y";
    private static final String PREF_PENDING_OEM_WALLPAPER = "pending_oem_wallpaper";
    private static final String PREF_PENDING_OEM_WALLPAPER_ID = "pending_oem_wallpaper_id";
    private static final int WEATHER_REFRESH_MS = 30 * 60 * 1000;
    private static final int HORIZONTAL_PAGE_SIZE = 24; // 4 columns x 6 rows

    private static final int BG = Color.rgb(5, 7, 13);
    private static final int PANEL = Color.rgb(15, 18, 31);
    private static final int PANEL_ALT = Color.rgb(22, 25, 43);
    private static final int TEXT = Color.rgb(246, 247, 255);
    private static final int MUTED = Color.rgb(149, 158, 181);
    private static final int BLUE = Color.rgb(65, 137, 255);
    private static final int CYAN = Color.rgb(94, 193, 255);
    private static final int PURPLE = Color.rgb(145, 94, 255);
    private static final int STROKE = Color.rgb(45, 49, 76);

    // Preferred home shortcuts. Missing packages are simply skipped.
    private static final String[] HOME_FAVORITES = {
            "com.google.android.apps.photos",
            "com.android.vending",
            "com.google.android.apps.maps",
            "com.google.android.gm",
            "com.android.chrome",
            "com.google.android.apps.messaging",
            "com.google.android.dialer",
            "com.android.settings"
    };

    // The camera and GameTime are intentionally not pinned as launcher menu shortcuts.
    private static final String[][] DOCK_PACKAGES = {
            {"com.google.android.dialer", "com.android.dialer"},
            {"com.google.android.apps.messaging", "com.android.mms"},
            {"com.android.chrome"},
            {"com.android.settings"}
    };

    private final List<ResolveInfo> apps = new ArrayList<>();
    private GridView homeGrid;
    private GridView drawerGrid;
    private EditText drawerSearch;
    private TextView drawerCount;
    private LinearLayout drawerContent;
    private SnapHorizontalScrollView drawerPager;
    private LinearLayout drawerPages;
    private TextView drawerPageIndicator;
    private int drawerPageWidth;
    private TextView weatherStatus;
    private TextView weatherTemperatureView;
    private TextView weatherPlaceView;
    private TextView weatherConditionView;
    private TextView weatherIconView;
    private LinearLayout batteryWidget;
    private TextView batteryWidgetText;
    private PulseBatteryRing pulseBatteryRing;
    private TextView pulseGreetingText;
    private TextView pulseStatusText;
    private TextView pulseBatteryText;
    private volatile boolean weatherRefreshInFlight;
    private LinearLayout dock;
    private boolean drawerOpen = false;
    private float downX;
    private float downY;
    private boolean trackingGesture;
    private Bitmap wallpaperBitmap;
    private ImageView systemWallpaperView;
    private Drawable cachedSystemWallpaper;
    private int cachedSystemWallpaperId = -1;

    // App icons are loaded off the UI thread so scrolling does not wait for PackageManager.
    // ConstantState keeps the cache compact and lets each ImageView receive its own Drawable.
    private final Object appIconLock = new Object();
    private final LruCache<String, Drawable.ConstantState> appIconCache = new LruCache<>(128);
    private final Set<String> appIconsLoading = new HashSet<>();
    private final Map<String, List<WeakReference<ImageView>>> pendingIconViews = new HashMap<>();
    private final Map<String, String> appLabelCache = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService wallpaperExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread worker = new Thread(runnable, "Motoroll-WallpaperWorker");
        worker.setDaemon(true);
        return worker;
    });
    private final ExecutorService appIconLoader = Executors.newSingleThreadExecutor(runnable -> {
        Thread worker = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            runnable.run();
        }, "Motoroll-AppIconLoader");
        worker.setDaemon(true);
        return worker;
    });

    @Override
    protected void onDestroy() {
        appIconLoader.shutdownNow();
        wallpaperExecutor.shutdownNow();
        synchronized (appIconLock) {
            pendingIconViews.clear();
            appIconsLoading.clear();
        }
        super.onDestroy();
        if (updateReceiverRegistered) {
            try { unregisterReceiver(updateDownloadReceiver); } catch (Exception ignored) { }
            updateReceiverRegistered = false;
        }
        if (packageReceiverRegistered) {
            try { unregisterReceiver(packageChangeReceiver); } catch (Exception ignored) { }
            packageReceiverRegistered = false;
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String language = prefs.getString(PREF_LANGUAGE, "system");
        if (language != null && !"system".equals(language)) {
            Configuration configuration = new Configuration(base.getResources().getConfiguration());
            configuration.setLocale(new Locale(language));
            super.attachBaseContext(base.createConfigurationContext(configuration));
        } else {
            super.attachBaseContext(base);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            applyMemoryProfile();
            loadApps();
            boolean welcomed = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getBoolean(PREF_WELCOME_SHOWN, false);
            if (welcomed) showHome(false);
            else showWelcome();
        } catch (RuntimeException error) {
            Log.e(TAG, "Launcher failed during startup", error);
            showStartupError(error);
        }
        IntentFilter updateFilter = new IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(updateDownloadReceiver, updateFilter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(updateDownloadReceiver, updateFilter);
        updateReceiverRegistered = true;

        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageChangeReceiver, packageFilter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(packageChangeReceiver, packageFilter);
        packageReceiverRegistered = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Avoid a full PackageManager scan and icon/dock rebind on every app exit.
            // Package broadcasts above keep the cached app list current when it changes.
            completePendingOemWallpaperIfChanged();
            if (homeGrid != null && getSavedWallpaperUri() == null && systemWallpaperView != null) {
                systemWallpaperView.setImageDrawable(copySystemWallpaperDrawable());
            }
            if (batteryWidgetText != null) updateBatteryWidget();
            if (pulseBatteryRing != null) updatePulseWidget();
            if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_WEATHER_ENABLED, false)) {
                refreshWeatherIfNeeded(false);
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not refresh launcher apps", error);
        }
        if (awaitingInstallPermission && Build.VERSION.SDK_INT >= 26 && getPackageManager().canRequestPackageInstalls()) {
            awaitingInstallPermission = false;
            long id = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(UPDATE_DOWNLOAD_ID, -1L);
            if (id != -1L) installDownloadedUpdate(id);
        }
    }

    /**
     * Apply system-bar styling only after setContentView() has created the DecorView.
     * Calling Window.getInsetsController() before that point can crash on some Android 16 builds
     * because PhoneWindow has not created its DecorView yet.
     */
    private void configureWindow() {
        Window window = getWindow();
        if (window == null) return;

        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        final View decor = window.getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            // Post until the decor is attached and its WindowInsetsController is available.
            decor.post(() -> {
                WindowInsetsController controller = decor.getWindowInsetsController();
                if (controller != null) {
                    controller.setSystemBarsAppearance(0,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                } else {
                    decor.setSystemUiVisibility(0);
                }
            });
        } else {
            decor.setSystemUiVisibility(0);
        }
    }

    private String tr(String es, String en, String pt) {
        String lang = getResources().getConfiguration().getLocales().get(0).getLanguage();
        if ("en".equalsIgnoreCase(lang)) return en;
        if ("pt".equalsIgnoreCase(lang)) return pt;
        return es;
    }

    private String tr(String es, String en) {
        return tr(es, en, es);
    }

    private void showWelcome() {
        drawerOpen = false;
        FrameLayout root = createRoot();
        LinearLayout shell = column();
        shell.setGravity(Gravity.CENTER_HORIZONTAL);
        shell.setPadding(dp(26), dp(28), dp(26), dp(24));
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));

        View spacerTop = new View(this);
        shell.addView(spacerTop, new LinearLayout.LayoutParams(1, 0, 0.55f));

        MotorollSymbol symbol = new MotorollSymbol(this);
        shell.addView(symbol, lp(82, 82));

        TextView title = text(tr("¡Bienvenido a Motoroll Launcher!", "Welcome to Motoroll Launcher!", "Bem-vindo ao Motoroll Launcher!"),
                25, TEXT, true);
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        title.setPadding(0, dp(22), 0, dp(5));
        shell.addView(title, lp(-1, -2));

        TextView credit = text("Creado por: perrunoGT", 13, CYAN, true);
        credit.setGravity(Gravity.CENTER);
        shell.addView(credit, withTop(lp(-1, -2), 3));

        LinearLayout card = column();
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(rounded(PANEL, 22, STROKE, 1));
        TextView featureTitle = text(tr("LO QUE INCLUYE", "WHAT'S INCLUDED", "O QUE INCLUI"), 10, CYAN, true);
        featureTitle.setLetterSpacing(0.13f);
        card.addView(featureTitle, lp(-1, -2));
        String[] features = {
                tr("Bóveda de aplicaciones vertical u horizontal", "Vertical or horizontal app drawer", "Gaveta de apps vertical ou horizontal"),
                tr("Dock inferior con cuatro accesos editables", "Four customizable dock shortcuts", "Dock inferior com quatro atalhos personalizáveis"),
                tr("Fondos, reloj, widgets e iconos personalizables", "Custom wallpapers, clock, widgets and icons", "Papéis de parede, relógio, widgets e ícones personalizáveis"),
                tr("Clima opcional, apagado de forma predeterminada", "Optional weather, off by default", "Clima opcional, desativado por padrão"),
                tr("Idioma automático o selección manual", "Automatic or manual language", "Idioma automático ou seleção manual"),
                tr("Perfiles visuales, apps ocultas y animaciones ajustables", "Appearance profiles, hidden apps and adjustable animations", "Perfis visuais, apps ocultos e animações ajustáveis"),
                tr("Diseño ligero, sin servicios permanentes", "Lightweight design, no always-on services", "Design leve, sem serviços permanentes")
        };
        for (String feature : features) {
            TextView line = text("•  " + feature, 12, MUTED, false);
            line.setPadding(0, dp(9), 0, 0);
            card.addView(line, lp(-1, -2));
        }
        shell.addView(card, withTop(lp(-1, -2), 26));

        TextView customizationTip = text(tr(
                "TIP: En la pantalla principal, mantén presionada la barra superior (MOTOROLL) para abrir Personalización.",
                "TIP: On the home screen, press and hold the top bar (MOTOROLL) to open Personalization.",
                "DICA: Na tela inicial, mantenha pressionada a barra superior (MOTOROLL) para abrir Personalização."),
                11, CYAN, true);
        customizationTip.setGravity(Gravity.CENTER);
        customizationTip.setPadding(dp(8), dp(5), dp(8), dp(5));
        shell.addView(customizationTip, withTop(lp(-1, -2), 12));

        TextView start = text(tr("COMENZAR", "GET STARTED", "COMEÇAR"), 13, Color.WHITE, true);
        start.setGravity(Gravity.CENTER);
        start.setBackground(gradient(BLUE, PURPLE, 19));
        shell.addView(start, withTop(lp(-1, 54), 22));
        start.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(PREF_WELCOME_SHOWN, true).apply();
            showHome(true);
        });

        View spacerBottom = new View(this);
        shell.addView(spacerBottom, new LinearLayout.LayoutParams(1, 0, 0.45f));
        setContentView(root);
        configureWindow();
        shell.requestApplyInsets();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private LinearLayout.LayoutParams lp(int widthDp, int heightDp) {
        int w = widthDp < 0 ? widthDp : dp(widthDp);
        int h = heightDp < 0 ? heightDp : dp(heightDp);
        return new LinearLayout.LayoutParams(w, h);
    }

    private LinearLayout.LayoutParams withTop(LinearLayout.LayoutParams params, int marginDp) {
        params.topMargin = dp(marginDp);
        return params;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        view.setTypeface(android.graphics.Typeface.create(
                "sans-serif", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        return view;
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeWidthDp > 0) drawable.setStroke(dp(strokeWidthDp), strokeColor);
        return drawable;
    }

    private GradientDrawable gradient(int start, int end, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{start, end});
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private void configureInsets(View view, int horizontalDp, int verticalTopDp, int verticalBottomDp) {
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            int left = 0, top = 0, right = 0, bottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            target.setPadding(dp(horizontalDp) + left, dp(verticalTopDp) + top,
                    dp(horizontalDp) + right, dp(verticalBottomDp) + bottom);
            return insets;
        });
    }

    private FrameLayout createRoot() {
        FrameLayout root = new SwipeRootLayout(this);
        systemWallpaperView = null;
        root.setBackgroundColor(BG);

        Uri saved = getSavedWallpaperUri();
        if (saved != null) {
            if (wallpaperBitmap == null) wallpaperBitmap = decodeWallpaper(saved);
            if (wallpaperBitmap != null) {
                ImageView wallpaper = new ImageView(this);
                wallpaper.setImageBitmap(wallpaperBitmap);
                wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
                root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));
                View scrim = new View(this);
                scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0x76030712, 0x4205070D, 0x99030610}));
                root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
                return root;
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(PREF_WALLPAPER_URI).apply();
        }

        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_FORCE_MOTOROLL_BG, false)) {
            root.addView(new AmbientBackground(this), new FrameLayout.LayoutParams(-1, -1));
            return root;
        }

        // When Motoroll has no custom wallpaper selected, mirror the phone's current
        // system home wallpaper. This uses Android's public WallpaperManager API and
        // therefore works across stock Samsung, Motorola and other Android builds.
        try {
            Drawable systemWallpaper = copySystemWallpaperDrawable();
            if (systemWallpaper != null) {
                ImageView wallpaper = new ImageView(this);
                systemWallpaperView = wallpaper;
                wallpaper.setImageDrawable(systemWallpaper);
                wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
                root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));
                View scrim = new View(this);
                scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0x65030712, 0x2605070D, 0x85030610}));
                root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
                return root;
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Could not mirror system wallpaper; using Motoroll background", error);
        }
        root.addView(new AmbientBackground(this), new FrameLayout.LayoutParams(-1, -1));
        return root;
    }

    private void refreshSystemWallpaperCache() {
        try {
            WallpaperManager manager = WallpaperManager.getInstance(this);
            int wallpaperId = manager.getWallpaperId(WallpaperManager.FLAG_SYSTEM);
            if (cachedSystemWallpaper == null || wallpaperId != cachedSystemWallpaperId) {
                cachedSystemWallpaper = manager.getDrawable();
                cachedSystemWallpaperId = wallpaperId;
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Could not refresh cached system wallpaper", error);
        }
    }

    private Drawable copySystemWallpaperDrawable() {
        refreshSystemWallpaperCache();
        if (cachedSystemWallpaper == null) return null;
        Drawable.ConstantState state = cachedSystemWallpaper.getConstantState();
        return state != null ? state.newDrawable(getResources()).mutate() : cachedSystemWallpaper;
    }

    private void completePendingOemWallpaperIfChanged() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_PENDING_OEM_WALLPAPER, false)) return;
        int oldId = prefs.getInt(PREF_PENDING_OEM_WALLPAPER_ID, -1);
        try {
            int newId = WallpaperManager.getInstance(this).getWallpaperId(WallpaperManager.FLAG_SYSTEM);
            if (newId != oldId) {
                prefs.edit().remove(PREF_WALLPAPER_URI)
                        .putBoolean(PREF_FORCE_MOTOROLL_BG, false).apply();
                wallpaperBitmap = null;
                cachedSystemWallpaper = null;
                cachedSystemWallpaperId = -1;
                Toast.makeText(this, tr("Fondo de inicio sincronizado con el teléfono",
                        "Home wallpaper synced with the phone", "Papel de parede inicial sincronizado"), Toast.LENGTH_SHORT).show();
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Could not verify OEM wallpaper change", error);
        } finally {
            prefs.edit().remove(PREF_PENDING_OEM_WALLPAPER)
                    .remove(PREF_PENDING_OEM_WALLPAPER_ID).apply();
        }
    }

    private Uri getSavedWallpaperUri() {
        String value = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_WALLPAPER_URI, null);
        if (value == null || value.trim().isEmpty()) return null;
        try { return Uri.parse(value); }
        catch (RuntimeException ignored) { return null; }
    }

    // Copy selected images into Motoroll's private storage. Some gallery/cloud providers
    // return temporary content:// grants that stop working after the picker closes or a reboot.
    // An app-owned file keeps the wallpaper available reliably across launcher restarts.
    private Uri persistWallpaperImage(Uri source) throws Exception {
        File target = new File(getFilesDir(), "motoroll_custom_wallpaper.img");
        File temp = new File(getFilesDir(), "motoroll_custom_wallpaper.tmp");
        try (InputStream input = getContentResolver().openInputStream(source);
             FileOutputStream output = new FileOutputStream(temp, false)) {
            if (input == null) throw new IllegalStateException("Selected image could not be opened");
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count > 0) output.write(buffer, 0, count);
            }
            output.flush();
            output.getFD().sync();
        }
        if (target.exists() && !target.delete()) {
            temp.delete();
            throw new IllegalStateException("Could not replace previous wallpaper");
        }
        if (!temp.renameTo(target)) {
            temp.delete();
            throw new IllegalStateException("Could not save wallpaper in Motoroll storage");
        }
        return Uri.fromFile(target);
    }

    private Bitmap decodeWallpaper(Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int memoryMode = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_MEMORY_MODE, 0);
            boolean economy = memoryMode == 1 || (memoryMode == 0 && isLowMemoryDevice());
            int targetW = Math.max(720, Math.min(economy ? 900 : 1120, getResources().getDisplayMetrics().widthPixels));
            int targetH = Math.max(1400, Math.min(economy ? 1950 : 2420, getResources().getDisplayMetrics().heightPixels));
            int sample = 1;
            while ((bounds.outWidth / (sample * 2)) >= targetW
                    && (bounds.outHeight / (sample * 2)) >= targetH) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                return BitmapFactory.decodeStream(in, null, options);
            }
        } catch (Exception error) {
            Log.w(TAG, "Could not load selected wallpaper", error);
            return null;
        }
    }

    private void chooseWallpaper() {
        new AlertDialog.Builder(this)
                .setTitle(tr("Fondo de Motoroll", "Motoroll wallpaper", "Papel de parede do Motoroll"))
                .setItems(new String[]{
                        tr("Usar el fondo actual del teléfono", "Use the phone's current wallpaper", "Usar o papel de parede atual do telefone"),
                        tr("Usar el fondo original de Motoroll", "Use the original Motoroll background", "Usar o fundo original do Motoroll"),
                        tr("Elegir una imagen", "Choose an image", "Escolher uma imagem")
                }, (dialog, which) -> {
                    SharedPreferences.Editor edit = getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(PREF_WALLPAPER_URI);
                    if (which == 0) {
                        edit.putBoolean(PREF_FORCE_MOTOROLL_BG, false).apply();
                        wallpaperBitmap = null;
                        showHome(true);
                        Toast.makeText(this, tr("Motoroll seguirá el fondo de inicio del sistema", "Motoroll will follow the system home wallpaper", "Motoroll seguirá o papel de parede inicial do sistema"), Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        edit.putBoolean(PREF_FORCE_MOTOROLL_BG, true).apply();
                        wallpaperBitmap = null;
                        showHome(true);
                    } else chooseWallpaperImage();
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void chooseWallpaperImage() {
        try {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("image/*");
            picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(picker, REQUEST_WALLPAPER);
        } catch (Exception error) {
            Toast.makeText(this, tr("No se pudo abrir el selector de fondos", "Could not open the wallpaper picker", "Não foi possível abrir o seletor de papel de parede"), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Wallpaper picker unavailable", error);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BACKUP_EXPORT) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) exportSafeSave(data.getData());
            return;
        }
        if (requestCode == REQUEST_BACKUP_IMPORT) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) importSafeSave(data.getData());
            return;
        }
        if (requestCode != REQUEST_WALLPAPER || resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri selectedUri = data.getData();
        Toast.makeText(this, tr("Guardando imagen…", "Saving image…", "Salvando imagem…"), Toast.LENGTH_SHORT).show();
        try {
            wallpaperExecutor.execute(() -> {
                try {
                    Uri savedUri = persistWallpaperImage(selectedUri);
                    mainHandler.post(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putString(PREF_WALLPAPER_URI, savedUri.toString())
                                .putBoolean(PREF_FORCE_MOTOROLL_BG, false).apply();
                        wallpaperBitmap = null;
                        cachedSystemWallpaper = null;
                        cachedSystemWallpaperId = -1;
                        showHome(false);
                        showPhoneWallpaperChoices(savedUri);
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not persist selected Motoroll wallpaper", error);
                    mainHandler.post(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(this, tr("No se pudo guardar esa foto. Prueba con otra imagen.",
                                    "Could not save that photo. Try another image.",
                                    "Não foi possível salvar essa foto. Tente outra imagem."), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } catch (RuntimeException rejected) {
            Log.e(TAG, "Wallpaper storage worker unavailable", rejected);
            Toast.makeText(this, tr("No se pudo guardar la imagen.", "Could not save the image.", "Não foi possível salvar a imagem."), Toast.LENGTH_LONG).show();
        }
    }

    private void showSafeSaveMenu() {
        new AlertDialog.Builder(this)
                .setTitle("MOTOROLL SAFESAVE")
                .setItems(new String[]{
                        tr("Crear copia de seguridad", "Create backup", "Criar backup"),
                        tr("Restaurar copia de seguridad", "Restore backup", "Restaurar backup")
                }, (dialog, which) -> {
                    if (which == 0) createSafeSaveDocument();
                    else openSafeSaveDocument();
                })
                .setNegativeButton(tr("Cerrar", "Close", "Fechar"), null)
                .show();
    }

    private void createSafeSaveDocument() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE, "Motoroll_SafeSave_" + System.currentTimeMillis() + ".zip");
            startActivityForResult(intent, REQUEST_BACKUP_EXPORT);
        } catch (Exception error) {
            Toast.makeText(this, tr("No se pudo abrir el selector para guardar", "Could not open the save picker", "Não foi possível abrir o seletor para salvar"), Toast.LENGTH_LONG).show();
        }
    }

    private void openSafeSaveDocument() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed"});
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_BACKUP_IMPORT);
        } catch (Exception error) {
            Toast.makeText(this, tr("No se pudo abrir el selector de copias", "Could not open the backup picker", "Não foi possível abrir o seletor de backups"), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isBackupExcludedKey(String key) {
        return key == null || key.startsWith("update_")
                || key.startsWith("pending_")
                || key.equals(PREF_PENDING_OEM_WALLPAPER)
                || key.equals(PREF_PENDING_OEM_WALLPAPER_ID);
    }

    private JSONObject createSafeSaveManifest() throws Exception {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        File wallpaper = new File(getFilesDir(), "motoroll_custom_wallpaper.img");
        boolean includeWallpaper = wallpaper.isFile() && wallpaper.length() > 0
                && wallpaper.length() <= MAX_BACKUP_WALLPAPER_BYTES;
        JSONObject values = new JSONObject();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (isBackupExcludedKey(key)) continue;
            Object value = entry.getValue();
            JSONObject item = new JSONObject();
            if (value instanceof Boolean) {
                item.put("type", "boolean"); item.put("value", value);
            } else if (value instanceof Integer) {
                item.put("type", "int"); item.put("value", value);
            } else if (value instanceof Long) {
                item.put("type", "long"); item.put("value", value);
            } else if (value instanceof Float) {
                item.put("type", "float"); item.put("value", value);
            } else if (value instanceof String) {
                item.put("type", "string");
                String stringValue = (String) value;
                if (PREF_WALLPAPER_URI.equals(key) && !includeWallpaper) stringValue = "";
                if (stringValue.length() > 262144) continue;
                item.put("value", stringValue);
            } else if (value instanceof Set) {
                item.put("type", "string_set");
                JSONArray array = new JSONArray();
                for (Object member : (Set<?>) value) {
                    if (member instanceof String) array.put(member);
                }
                item.put("value", array);
            } else {
                continue;
            }
            values.put(key, item);
        }
        JSONObject root = new JSONObject();
        root.put("format", "motoroll-home-safesave");
        root.put("schemaVersion", 1);
        root.put("appVersion", "3.3.19");
        root.put("createdAt", System.currentTimeMillis());
        root.put("wallpaperIncluded", includeWallpaper);
        root.put("preferences", values);
        return root;
    }

    private void exportSafeSave(Uri destination) {
        final JSONObject manifest;
        try {
            manifest = createSafeSaveManifest();
        } catch (Exception error) {
            Toast.makeText(this, tr("No se pudo preparar la copia", "Could not prepare backup", "Não foi possível preparar o backup"), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, tr("Creando copia de seguridad…", "Creating backup…", "Criando backup…"), Toast.LENGTH_SHORT).show();
        try {
            wallpaperExecutor.execute(() -> {
                String failure = null;
                try (OutputStream raw = getContentResolver().openOutputStream(destination);
                     ZipOutputStream zip = new ZipOutputStream(raw)) {
                    if (raw == null) throw new IllegalStateException("Backup destination unavailable");
                    byte[] json = manifest.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    if (json.length > MAX_BACKUP_JSON_BYTES) throw new IllegalStateException("Backup settings are too large");
                    zip.putNextEntry(new ZipEntry("motoroll_backup.json"));
                    zip.write(json);
                    zip.closeEntry();

                    if (manifest.optBoolean("wallpaperIncluded", false)) {
                        File wallpaper = new File(getFilesDir(), "motoroll_custom_wallpaper.img");
                        if (!wallpaper.isFile() || wallpaper.length() > MAX_BACKUP_WALLPAPER_BYTES) {
                            throw new IllegalStateException("Wallpaper changed while backup was being created");
                        }
                        zip.putNextEntry(new ZipEntry("motoroll_custom_wallpaper.img"));
                        try (InputStream input = new FileInputStream(wallpaper)) {
                            copyStreamLimited(input, zip, MAX_BACKUP_WALLPAPER_BYTES);
                        }
                        zip.closeEntry();
                    }
                    zip.finish();
                } catch (Exception error) {
                    Log.e(TAG, "SafeSave export failed", error);
                    failure = error.getMessage();
                }
                final String result = failure;
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, result == null
                            ? tr("Copia de Motoroll guardada", "Motoroll backup saved", "Backup do Motoroll salvo")
                            : tr("No se pudo guardar la copia", "Could not save backup", "Não foi possível salvar o backup"),
                            Toast.LENGTH_LONG).show();
                });
            });
        } catch (RuntimeException rejected) {
            Toast.makeText(this, tr("El servicio de copia no está disponible", "Backup worker unavailable", "Serviço de backup indisponível"), Toast.LENGTH_LONG).show();
        }
    }

    private void importSafeSave(Uri source) {
        Toast.makeText(this, tr("Comprobando copia de seguridad…", "Checking backup…", "Verificando backup…"), Toast.LENGTH_SHORT).show();
        try {
            wallpaperExecutor.execute(() -> {
                File wallpaperTemp = null;
                try {
                    JSONObject manifest = null;
                    boolean wallpaperFound = false;
                    boolean settingsFound = false;
                    try (InputStream raw = getContentResolver().openInputStream(source);
                         ZipInputStream zip = new ZipInputStream(raw)) {
                        if (raw == null) throw new IllegalStateException("Backup file unavailable");
                        ZipEntry entry;
                        while ((entry = zip.getNextEntry()) != null) {
                            if ("motoroll_backup.json".equals(entry.getName())) {
                                if (settingsFound) throw new IllegalArgumentException("Duplicate settings entry");
                                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                copyStreamLimited(zip, bytes, MAX_BACKUP_JSON_BYTES);
                                manifest = new JSONObject(new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
                                settingsFound = true;
                            } else if ("motoroll_custom_wallpaper.img".equals(entry.getName())) {
                                if (wallpaperFound) throw new IllegalArgumentException("Duplicate wallpaper entry");
                                wallpaperTemp = File.createTempFile("motoroll_restore_", ".img", getFilesDir());
                                try (FileOutputStream output = new FileOutputStream(wallpaperTemp, false)) {
                                    copyStreamLimited(zip, output, MAX_BACKUP_WALLPAPER_BYTES);
                                    output.flush();
                                    output.getFD().sync();
                                }
                                wallpaperFound = wallpaperTemp.length() > 0;
                            }
                            zip.closeEntry();
                        }
                    }
                    validateSafeSaveManifest(manifest, wallpaperFound);
                    final JSONObject checkedManifest = manifest;
                    final File checkedWallpaper = wallpaperFound ? wallpaperTemp : null;
                    mainHandler.post(() -> {
                        if (isFinishing() || isDestroyed()) {
                            if (checkedWallpaper != null) checkedWallpaper.delete();
                            return;
                        }
                        try {
                            restoreSafeSavePreferences(checkedManifest, checkedWallpaper);
                            wallpaperBitmap = null;
                            cachedSystemWallpaper = null;
                            cachedSystemWallpaperId = -1;
                            applyMemoryProfile();
                            showHome(false);
                            Toast.makeText(this, tr("Copia restaurada correctamente", "Backup restored successfully", "Cópia restaurada com sucesso"), Toast.LENGTH_LONG).show();
                        } catch (Exception error) {
                            Log.e(TAG, "SafeSave restore failed", error);
                            Toast.makeText(this, tr("No se pudo restaurar la copia", "Could not restore backup", "Não foi possível restaurar o backup"), Toast.LENGTH_LONG).show();
                        }
                    });
                    wallpaperTemp = null;
                } catch (Exception error) {
                    Log.e(TAG, "SafeSave import rejected", error);
                    if (wallpaperTemp != null) wallpaperTemp.delete();
                    mainHandler.post(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(this, tr("La copia no es válida o está dañada", "Backup is invalid or damaged", "O backup é inválido ou está danificado"), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } catch (RuntimeException rejected) {
            Toast.makeText(this, tr("El servicio de restauración no está disponible", "Restore worker unavailable", "Serviço de restauração indisponível"), Toast.LENGTH_LONG).show();
        }
    }

    private void validateSafeSaveManifest(JSONObject manifest, boolean wallpaperFound) throws Exception {
        if (manifest == null || !"motoroll-home-safesave".equals(manifest.optString("format"))
                || manifest.optInt("schemaVersion", -1) != 1) {
            throw new IllegalArgumentException("Unsupported SafeSave format");
        }
        if (manifest.optBoolean("wallpaperIncluded", false) != wallpaperFound) {
            throw new IllegalArgumentException("Backup wallpaper data is incomplete");
        }
        JSONObject values = manifest.optJSONObject("preferences");
        if (values == null || values.length() > 512) throw new IllegalArgumentException("Invalid settings list");
        JSONArray names = values.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            String key = names.getString(i);
            if (isBackupExcludedKey(key)) continue;
            JSONObject item = values.optJSONObject(key);
            if (item == null) throw new IllegalArgumentException("Invalid preference item");
            String type = item.optString("type");
            if (!Arrays.asList("boolean", "int", "long", "float", "string", "string_set").contains(type)) {
                throw new IllegalArgumentException("Invalid preference type");
            }
            if ("string".equals(type) && item.optString("value", "").length() > 262144) {
                throw new IllegalArgumentException("Preference string too large");
            }
            if ("string_set".equals(type) && item.optJSONArray("value") == null) {
                throw new IllegalArgumentException("Invalid preference set");
            }
        }
    }

    private void restoreSafeSavePreferences(JSONObject manifest, File importedWallpaper) throws Exception {
        File target = new File(getFilesDir(), "motoroll_custom_wallpaper.img");
        File previous = new File(getFilesDir(), "motoroll_custom_wallpaper.restore_old");
        boolean hadPrevious = target.isFile();
        if (previous.exists()) previous.delete();
        if (hadPrevious && !target.renameTo(previous)) throw new IllegalStateException("Could not preserve current wallpaper");
        if (importedWallpaper != null && !importedWallpaper.renameTo(target)) {
            if (hadPrevious) previous.renameTo(target);
            throw new IllegalStateException("Could not install restored wallpaper");
        }

        JSONObject values = manifest.getJSONObject("preferences");
        SharedPreferences.Editor edit = getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear();
        JSONArray names = values.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String key = names.getString(i);
                if (isBackupExcludedKey(key) || PREF_WALLPAPER_URI.equals(key)) continue;
                JSONObject item = values.getJSONObject(key);
                String type = item.getString("type");
                if ("boolean".equals(type)) edit.putBoolean(key, item.getBoolean("value"));
                else if ("int".equals(type)) edit.putInt(key, item.getInt("value"));
                else if ("long".equals(type)) edit.putLong(key, item.getLong("value"));
                else if ("float".equals(type)) edit.putFloat(key, (float) item.getDouble("value"));
                else if ("string".equals(type)) edit.putString(key, item.getString("value"));
                else if ("string_set".equals(type)) {
                    JSONArray array = item.getJSONArray("value");
                    Set<String> set = new HashSet<>();
                    for (int j = 0; j < array.length(); j++) set.add(array.getString(j));
                    edit.putStringSet(key, set);
                }
            }
        }
        edit.putBoolean(PREF_WELCOME_SHOWN, true);
        if (importedWallpaper != null || target.isFile()) edit.putString(PREF_WALLPAPER_URI, Uri.fromFile(target).toString());
        else edit.remove(PREF_WALLPAPER_URI);
        edit.remove(PREF_PENDING_OEM_WALLPAPER).remove(PREF_PENDING_OEM_WALLPAPER_ID);
        if (!edit.commit()) {
            if (target.exists()) target.delete();
            if (hadPrevious) previous.renameTo(target);
            throw new IllegalStateException("Could not save restored preferences");
        }
        if (previous.exists()) previous.delete();
    }

    private void copyStreamLimited(InputStream input, OutputStream output, long maxBytes) throws Exception {
        byte[] buffer = new byte[16 * 1024];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > maxBytes) throw new IllegalArgumentException("Archive entry exceeds allowed size");
            output.write(buffer, 0, count);
        }
    }

    private void showSmartDrawerMenu() {
        new AlertDialog.Builder(this)
                .setTitle("SMART DRAWER")
                .setItems(new String[]{
                        tr("Diseño vertical u horizontal", "Vertical or horizontal layout", "Layout vertical ou horizontal"),
                        tr("Aplicaciones ocultas", "Hidden apps", "Aplicativos ocultos"),
                        tr("Gesto para abrir la bóveda", "Drawer opening gesture", "Gesto para abrir a gaveta")
                }, (dialog, which) -> {
                    if (which == 0) chooseDrawerMode();
                    else if (which == 1) chooseHiddenApps();
                    else chooseDrawerSwipeDirection();
                })
                .setNegativeButton(tr("Cerrar", "Close", "Fechar"), null)
                .show();
    }

    private void showMotorollStudioMenu() {
        new AlertDialog.Builder(this)
                .setTitle("MOTOROLL STUDIO")
                .setItems(new String[]{
                        tr("Perfiles de apariencia", "Appearance profiles", "Perfis de aparência"),
                        tr("Fondo y ambiente", "Wallpaper and ambience", "Papel de parede e ambiente"),
                        tr("Forma y tamaño de iconos", "Icon shape and size", "Forma e tamanho dos ícones"),
                        tr("Estilo de notas", "Note styling", "Estilo das notas")
                }, (dialog, which) -> {
                    if (which == 0) manageAppearanceProfiles();
                    else if (which == 1) chooseWallpaper();
                    else if (which == 2) chooseStudioIconOptions();
                    else chooseHomeNoteStyle();
                })
                .setNegativeButton(tr("Cerrar", "Close", "Fechar"), null)
                .show();
    }

    private void chooseStudioIconOptions() {
        new AlertDialog.Builder(this)
                .setTitle(tr("Iconos de Motoroll Studio", "Motoroll Studio icons", "Ícones do Motoroll Studio"))
                .setItems(new String[]{
                        tr("Forma de iconos", "Icon shape", "Forma dos ícones"),
                        tr("Tamaño de iconos", "Icon size", "Tamanho dos ícones")
                }, (dialog, which) -> {
                    if (which == 0) chooseIconShape();
                    else chooseIconSize();
                })
                .setNegativeButton(tr("Cerrar", "Close", "Fechar"), null)
                .show();
    }

    private String memoryProfileLabel(int mode) {
        if (mode == 1) return tr("Memoria: ahorro RAM ✓", "Memory: RAM saver ✓", "Memória: economia de RAM ✓");
        if (mode == 2) return tr("Memoria: equilibrado ✓", "Memory: balanced ✓", "Memória: equilibrada ✓");
        return tr("Memoria: automático ✓", "Memory: automatic ✓", "Memória: automática ✓");
    }

    private boolean isLowMemoryDevice() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        return manager != null && (manager.isLowRamDevice() || manager.getMemoryClass() <= 256);
    }

    private void applyMemoryProfile() {
        int mode = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_MEMORY_MODE, 0);
        int limit;
        if (mode == 1) limit = 32;
        else if (mode == 2) limit = 112;
        else limit = isLowMemoryDevice() ? 40 : 72;
        synchronized (appIconLock) {
            appIconCache.resize(limit);
        }
    }

    private void chooseMemoryProfile() {
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_MEMORY_MODE, 0);
        String[] choices = {
                tr("Automático · se adapta al teléfono", "Automatic · adapts to phone", "Automático · adapta-se ao telefone"),
                tr("Ahorro RAM · caché pequeña y fondos ligeros", "RAM saver · smaller cache and lighter wallpapers", "Economia de RAM · cache menor e fundos leves"),
                tr("Equilibrado · caché amplia", "Balanced · larger cache", "Equilibrado · cache maior")
        };
        new AlertDialog.Builder(this)
                .setTitle(tr("Perfil de memoria", "Memory profile", "Perfil de memória"))
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(PREF_MEMORY_MODE, which).apply();
                    applyMemoryProfile();
                    wallpaperBitmap = null;
                    dialog.dismiss();
                    showHome(false);
                    Toast.makeText(this, memoryProfileLabel(which), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private String megabytes(long bytes) {
        return Math.max(0L, bytes / (1024L * 1024L)) + " MB";
    }

    private void showMemoryStatus() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        if (manager != null) manager.getMemoryInfo(info);
        Runtime runtime = Runtime.getRuntime();
        long javaUsed = runtime.totalMemory() - runtime.freeMemory();
        long processPss = Debug.getPss() * 1024L;
        int cacheSize;
        int cacheMax;
        synchronized (appIconLock) {
            cacheSize = appIconCache.size();
            cacheMax = appIconCache.maxSize();
        }
        int mode = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_MEMORY_MODE, 0);
        String report = tr("RAM del sistema disponible: ", "Available system RAM: ", "RAM do sistema disponível: ")
                + megabytes(info.availMem) + "
"
                + tr("RAM total del sistema: ", "Total system RAM: ", "RAM total do sistema: ") + megabytes(info.totalMem) + "
"
                + tr("Motoroll (PSS aproximado): ", "Motoroll (approx. PSS): ", "Motoroll (PSS aproximado): ") + megabytes(processPss) + "
"
                + tr("Heap Java usado / máximo: ", "Java heap used / max: ", "Heap Java usado / máximo: ")
                + megabytes(javaUsed) + " / " + megabytes(runtime.maxMemory()) + "
"
                + tr("Caché de iconos: ", "Icon cache: ", "Cache de ícones: ") + cacheSize + " / " + cacheMax + "
"
                + tr("Aplicaciones detectadas: ", "Detected apps: ", "Aplicativos detectados: ") + apps.size() + "
"
                + memoryProfileLabel(mode);
        new AlertDialog.Builder(this)
                .setTitle(tr("Estado de RAM de Motoroll", "Motoroll RAM status", "Estado de RAM do Motoroll"))
                .setMessage(report)
                .setPositiveButton(tr("Cerrar", "Close", "Fechar"), null)
                .show();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            synchronized (appIconLock) {
                appIconCache.trimToSize(Math.max(16, appIconCache.maxSize() / 2));
                pendingIconViews.clear();
            }
        }
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            cachedSystemWallpaper = null;
            cachedSystemWallpaperId = -1;
        }
    }

    private void showPhoneWallpaperChoices(Uri uri) {
        LinearLayout choices = column();
        choices.setPadding(dp(4), dp(4), dp(4), dp(2));
        TextView message = text(tr("Elige dónde aplicar la imagen en el teléfono. Motoroll también seguirá el fondo de inicio del sistema cuando lo cambies.",
                "Choose where to apply this image on the phone. Motoroll will also follow the system home wallpaper when it changes.",
                "Escolha onde aplicar a imagem no telefone. Motoroll também seguirá o papel de parede inicial do sistema."), 13, MUTED, false);
        message.setPadding(dp(4), dp(4), dp(4), dp(12));
        choices.addView(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(tr("Aplicar fondo del teléfono", "Apply phone wallpaper", "Aplicar papel de parede do telefone"))
                .setView(choices)
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), (d, w) -> { })
                .create();

        addWallpaperChoice(choices, tr("Pantalla de inicio", "Home screen", "Tela inicial"),
                tr("Cambia el fondo del sistema y sincroniza Motoroll", "Changes the system wallpaper and syncs Motoroll", "Altera o fundo do sistema e sincroniza Motoroll"),
                () -> { dialog.dismiss(); applyPhoneWallpaper(uri, WallpaperManager.FLAG_SYSTEM); });
        addWallpaperChoice(choices, tr("Pantalla de bloqueo", "Lock screen", "Tela de bloqueio"),
                tr("Cambia solo el fondo de bloqueo", "Changes only the lock-screen wallpaper", "Altera apenas o fundo da tela de bloqueio"),
                () -> { dialog.dismiss(); applyPhoneWallpaper(uri, WallpaperManager.FLAG_LOCK); });
        addWallpaperChoice(choices, tr("Inicio y bloqueo", "Home and lock screens", "Tela inicial e bloqueio"),
                tr("Usa la misma imagen en ambas pantallas", "Uses the same image on both screens", "Usa a mesma imagem nas duas telas"),
                () -> { dialog.dismiss(); applyPhoneWallpaper(uri, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK); });
        addWallpaperChoice(choices, tr("Abrir selector de Samsung / Motorola", "Open Samsung / Motorola wallpaper picker", "Abrir seletor Samsung / Motorola"),
                tr("Usa el selector propio del teléfono", "Uses the phone's own wallpaper picker", "Usa o seletor próprio do telefone"),
                () -> { dialog.dismiss(); launchOemWallpaperPicker(uri); });
        dialog.show();
    }

    private void addWallpaperChoice(LinearLayout parent, String title, String subtitle, Runnable action) {
        LinearLayout item = column();
        item.setPadding(dp(14), dp(11), dp(14), dp(11));
        item.setBackground(rounded(PANEL_ALT, 15, STROKE, 1));
        TextView heading = text(title, 14, TEXT, true);
        TextView detail = text(subtitle, 11, MUTED, false);
        item.addView(heading);
        item.addView(detail, withTop(lp(-1, -2), 3));
        LinearLayout.LayoutParams params = withTop(lp(-1, -2), 7);
        parent.addView(item, params);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(v -> action.run());
    }

    private void applyPhoneWallpaper(Uri uri, int flags) {
        Toast.makeText(this, tr("Aplicando fondo…", "Applying wallpaper…", "Aplicando papel de parede…"), Toast.LENGTH_SHORT).show();
        try {
            wallpaperExecutor.execute(() -> {
                Exception failure = null;
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new IllegalStateException("Image stream unavailable");
                    WallpaperManager.getInstance(this).setStream(input, null, true, flags);
                } catch (Exception error) {
                    failure = error;
                    Log.e(TAG, "WallpaperManager.setStream failed; will use OEM picker", error);
                }
                final boolean success = failure == null;
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (success) {
                        if ((flags & WallpaperManager.FLAG_SYSTEM) != 0) {
                            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                    .remove(PREF_WALLPAPER_URI)
                                    .putBoolean(PREF_FORCE_MOTOROLL_BG, false).apply();
                            wallpaperBitmap = null;
                            cachedSystemWallpaper = null;
                            cachedSystemWallpaperId = -1;
                            showHome(false);
                        }
                        Toast.makeText(this, tr("Fondo del teléfono actualizado", "Phone wallpaper updated", "Papel de parede do telefone atualizado"), Toast.LENGTH_LONG).show();
                    } else {
                        launchOemWallpaperPicker(uri);
                    }
                });
            });
        } catch (RuntimeException rejected) {
            Log.e(TAG, "Wallpaper worker unavailable", rejected);
            launchOemWallpaperPicker(uri);
        }
    }

    private void launchOemWallpaperPicker(Uri uri) {
        int oldId = -1;
        try { oldId = WallpaperManager.getInstance(this).getWallpaperId(WallpaperManager.FLAG_SYSTEM); }
        catch (RuntimeException ignored) { }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(PREF_PENDING_OEM_WALLPAPER, true)
                .putInt(PREF_PENDING_OEM_WALLPAPER_ID, oldId).apply();

        Intent crop = new Intent("android.service.wallpaper.CROP_AND_SET_WALLPAPER");
        crop.setDataAndType(uri, "image/*");
        crop.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        crop.setClipData(ClipData.newRawUri("Motoroll wallpaper", uri));
        try {
            startActivity(crop);
            return;
        } catch (Exception error) {
            Log.w(TAG, "OEM crop wallpaper activity unavailable", error);
        }

        Intent attach = new Intent(Intent.ACTION_ATTACH_DATA);
        attach.setDataAndType(uri, "image/*");
        attach.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        attach.setClipData(ClipData.newRawUri("Motoroll wallpaper", uri));
        attach.putExtra("mimeType", "image/*");
        try {
            startActivity(Intent.createChooser(attach, tr("Aplicar fondo con el teléfono", "Set wallpaper with phone", "Definir papel de parede com o telefone")));
            return;
        } catch (Exception fallbackError) {
            Log.w(TAG, "Generic attach-data wallpaper handler unavailable", fallbackError);
        }
        try {
            startActivity(new Intent(Intent.ACTION_SET_WALLPAPER));
        } catch (Exception finalError) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(PREF_PENDING_OEM_WALLPAPER).remove(PREF_PENDING_OEM_WALLPAPER_ID).apply();
            Log.e(TAG, "No OEM wallpaper handler available", finalError);
            Toast.makeText(this, tr("No se pudo abrir el selector del teléfono. Prueba desde Ajustes > Fondo de pantalla.",
                    "Could not open the phone wallpaper picker. Try Settings > Wallpaper.",
                    "Não foi possível abrir o seletor. Tente Configurações > Papel de parede."), Toast.LENGTH_LONG).show();
        }
    }

    private void loadApps() {
        apps.clear();
        appLabelCache.clear();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> found;
        try {
            found = pm.queryIntentActivities(query, 0);
        } catch (RuntimeException error) {
            Log.e(TAG, "PackageManager could not enumerate launcher apps", error);
            found = Collections.emptyList();
        }
        Set<String> seen = new HashSet<>();
        if (found != null) {
            for (ResolveInfo info : found) {
                if (info == null || info.activityInfo == null) continue;
                String packageName = info.activityInfo.packageName;
                if (packageName == null || getPackageName().equals(packageName)) continue;
                String key = packageName + "/" + info.activityInfo.name;
                if (seen.add(key)) apps.add(info);
            }
        }
        final Collator collator = Collator.getInstance();
        Collections.sort(apps, (a, b) -> collator.compare(safeLabel(a), safeLabel(b)));
    }

    private String appKey(ResolveInfo info) {
        if (info == null || info.activityInfo == null) return "unknown";
        return info.activityInfo.packageName + "/" + info.activityInfo.name;
    }

    private String safeLabel(ResolveInfo info) {
        String key = appKey(info);
        String cached = appLabelCache.get(key);
        if (cached != null) return cached;
        String label;
        try {
            CharSequence loaded = info.loadLabel(getPackageManager());
            label = loaded == null ? info.activityInfo.packageName : loaded.toString();
        } catch (RuntimeException error) {
            label = info == null || info.activityInfo == null
                    ? "Aplicación" : info.activityInfo.packageName;
        }
        appLabelCache.put(key, label);
        return label;
    }

    private Drawable cachedAppIcon(String key) {
        synchronized (appIconLock) {
            Drawable.ConstantState state = appIconCache.get(key);
            return state == null ? null : state.newDrawable(getResources()).mutate();
        }
    }

    private void bindAppIcon(ImageView target, ResolveInfo info) {
        String key = appKey(info);
        target.setTag(key);
        Drawable cached = cachedAppIcon(key);
        if (cached != null) {
            target.setImageDrawable(cached);
            return;
        }

        target.setImageResource(android.R.drawable.sym_def_app_icon);
        boolean startLoad = false;
        synchronized (appIconLock) {
            Drawable.ConstantState state = appIconCache.get(key);
            if (state != null) {
                target.setImageDrawable(state.newDrawable(getResources()).mutate());
                return;
            }
            List<WeakReference<ImageView>> waiting = pendingIconViews.get(key);
            if (waiting == null) {
                waiting = new ArrayList<>();
                pendingIconViews.put(key, waiting);
            }
            waiting.add(new WeakReference<>(target));
            startLoad = appIconsLoading.add(key);
        }
        if (!startLoad) return;

        try {
            appIconLoader.execute(() -> {
                Drawable loaded = null;
                Drawable.ConstantState state = null;
                try {
                    loaded = info.loadIcon(getPackageManager());
                    if (loaded != null) state = loaded.getConstantState();
                } catch (RuntimeException error) {
                    Log.d(TAG, "Could not load icon for " + key, error);
                }

                List<WeakReference<ImageView>> waiting;
                synchronized (appIconLock) {
                    if (state != null) appIconCache.put(key, state);
                    appIconsLoading.remove(key);
                    waiting = pendingIconViews.remove(key);
                }

                final Drawable.ConstantState readyState = state;
                final Drawable fallback = loaded;
                if (waiting == null || waiting.isEmpty()) return;
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    for (WeakReference<ImageView> reference : waiting) {
                        ImageView image = reference.get();
                        if (image == null || !key.equals(image.getTag())) continue;
                        Drawable icon = readyState != null
                                ? readyState.newDrawable(getResources()).mutate() : fallback;
                        if (icon != null) image.setImageDrawable(icon);
                    }
                });
            });
        } catch (RuntimeException rejected) {
            synchronized (appIconLock) {
                appIconsLoading.remove(key);
                pendingIconViews.remove(key);
            }
        }
    }

    private String compactLabel(ResolveInfo info) {
        String value = safeLabel(info);
        return value.length() > 14 ? value.substring(0, 13).trim() + "…" : value;
    }

    private List<ResolveInfo> favoriteApps() {
        List<ResolveInfo> result = new ArrayList<>();
        Set<String> used = new HashSet<>();
        Set<String> hidden = hiddenPackages();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(PREF_FAVORITE_PACKAGES, null);
        if (saved != null) {
            for (String packageName : saved.split("\\n")) {
                if (result.size() >= 8) break;
                ResolveInfo match = firstPackage(packageName.trim());
                if (match != null && !hidden.contains(packageName.trim())
                        && !isExcludedFromFavorites(match) && used.add(match.activityInfo.packageName)) {
                    result.add(match);
                }
            }
            return result;
        }
        for (String preferred : HOME_FAVORITES) {
            ResolveInfo match = firstPackage(preferred);
            if (match != null && !hidden.contains(match.activityInfo.packageName)
                    && !isExcludedFromFavorites(match) && used.add(match.activityInfo.packageName)) result.add(match);
        }
        for (ResolveInfo info : apps) {
            if (result.size() >= 8) break;
            if (!hidden.contains(info.activityInfo.packageName) && !isExcludedFromFavorites(info)
                    && used.add(info.activityInfo.packageName)) result.add(info);
        }
        return result;
    }

    private List<String> favoritePackageNames() {
        List<String> names = new ArrayList<>();
        for (ResolveInfo info : favoriteApps()) names.add(info.activityInfo.packageName);
        return names;
    }

    private void saveFavoritePackageNames(List<String> names) {
        StringBuilder value = new StringBuilder();
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) continue;
            if (value.length() > 0) value.append('\n');
            value.append(name.trim());
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_FAVORITE_PACKAGES, value.toString()).apply();
        if (homeGrid != null) homeGrid.setAdapter(new AppAdapter(favoriteApps(), true));
    }

    private void manageHomeFavorites() {
        List<String> packages = favoritePackageNames();
        String[] actions = new String[packages.size() + 1];
        for (int i = 0; i < packages.size(); i++) {
            ResolveInfo info = firstPackage(packages.get(i));
            actions[i] = (i + 1) + ". " + (info == null ? packages.get(i) : safeLabel(info));
        }
        actions[packages.size()] = tr("＋ Agregar aplicación", "＋ Add application", "＋ Adicionar aplicativo");
        new AlertDialog.Builder(this)
                .setTitle(tr("Editar aplicaciones favoritas", "Edit favorite apps", "Editar aplicativos favoritos"))
                .setItems(actions, (dialog, which) -> {
                    if (which == packages.size()) {
                        chooseFavoriteApp(packages, -1);
                    } else {
                        String packageName = packages.get(which);
                        ResolveInfo info = firstPackage(packageName);
                        String label = info == null ? packageName : safeLabel(info);
                        new AlertDialog.Builder(this)
                                .setTitle(label)
                                .setItems(new String[]{tr("Reemplazar", "Replace", "Substituir"), tr("Eliminar de favoritas", "Remove from favorites", "Remover dos favoritos")}, (d, action) -> {
                                    if (action == 0) chooseFavoriteApp(packages, which);
                                    else {
                                        packages.remove(which);
                                        saveFavoritePackageNames(packages);
                                        manageHomeFavorites();
                                    }
                                }).setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
                    }
                })
                .setPositiveButton(tr("Listo", "Done", "Concluir"), null)
                .setNegativeButton(tr("Restaurar sugeridas", "Restore suggested", "Restaurar sugeridos"), (d, w) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(PREF_FAVORITE_PACKAGES).apply();
                    if (homeGrid != null) homeGrid.setAdapter(new AppAdapter(favoriteApps(), true));
                }).show();
    }

    private void manageFavoriteAt(ResolveInfo selected) {
        if (selected == null || selected.activityInfo == null) return;
        List<String> packages = favoritePackageNames();
        int index = packages.indexOf(selected.activityInfo.packageName);
        if (index < 0) return;
        new AlertDialog.Builder(this)
                .setTitle(safeLabel(selected))
                .setItems(new String[]{
                        tr("Cambiar aplicación", "Replace app", "Trocar aplicativo"),
                        tr("Quitar de favoritas", "Remove from favorites", "Remover dos favoritos")
                }, (dialog, action) -> {
                    if (action == 0) chooseFavoriteApp(packages, index);
                    else {
                        packages.remove(index);
                        saveFavoritePackageNames(packages);
                        Toast.makeText(this, tr("Favorita eliminada", "Favorite removed", "Favorito removido"), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void chooseFavoriteApp(List<String> current, int replaceIndex) {
        List<ResolveInfo> choices = new ArrayList<>();
        for (ResolveInfo info : apps) {
            if (info.activityInfo == null || info.activityInfo.packageName == null
                    || isExcludedFromFavorites(info)) continue;
            String pkg = info.activityInfo.packageName;
            if (hiddenPackages().contains(pkg)) continue;
            if (current.contains(pkg) && (replaceIndex < 0 || !pkg.equals(current.get(replaceIndex)))) continue;
            choices.add(info);
        }
        String[] labels = new String[choices.size()];
        for (int i = 0; i < choices.size(); i++) labels[i] = safeLabel(choices.get(i));
        new AlertDialog.Builder(this)
                .setTitle(replaceIndex < 0
                        ? tr("Agregar a favoritas", "Add to favorites", "Adicionar aos favoritos")
                        : tr("Reemplazar aplicación", "Replace app", "Substituir aplicativo"))
                .setItems(labels, (dialog, which) -> {
                    String pkg = choices.get(which).activityInfo.packageName;
                    if (replaceIndex >= 0 && replaceIndex < current.size()) current.set(replaceIndex, pkg);
                    else if (current.size() < 8) current.add(pkg);
                    else {
                        Toast.makeText(this, tr("Puedes tener hasta 8 favoritas", "You can have up to 8 favorites", "Você pode ter até 8 favoritos"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveFavoritePackageNames(current);
                    Toast.makeText(this, tr("Favoritas actualizadas", "Favorites updated", "Favoritos atualizados"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private String defaultDockPackage(int slot) {
        if (slot >= 0 && slot < DOCK_PACKAGES.length) {
            for (String candidate : DOCK_PACKAGES[slot]) {
                if (firstPackage(candidate) != null) return candidate;
            }
        }
        List<ResolveInfo> favorites = favoriteApps();
        int index = Math.min(slot, favorites.size() - 1);
        return index >= 0 && index < favorites.size()
                ? favorites.get(index).activityInfo.packageName : null;
    }

    private ResolveInfo dockAppForSlot(int slot) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(PREF_DOCK_PREFIX + slot, null);
        ResolveInfo selected = saved == null ? null : firstPackage(saved);
        if (selected != null && !hiddenPackages().contains(selected.activityInfo.packageName)) return selected;
        String fallback = defaultDockPackage(slot);
        return fallback == null ? null : firstPackage(fallback);
    }

    private int clockGravity() {
        int position = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_CLOCK_POSITION, 0);
        if (position == 1) return Gravity.CENTER;
        if (position == 2) return Gravity.RIGHT;
        return Gravity.LEFT;
    }

    private void applyLongPressCustomization(View view) {
        if (view == null) return;
        view.setLongClickable(true);
        view.setOnLongClickListener(v -> {
            showCustomizationMenu();
            return true;
        });
    }

    private void showCustomizationMenu() {
        String[] categories = {
                tr("Pantalla y fondos", "Home screen and wallpapers", "Tela e papéis de parede"),
                tr("Reloj y clima", "Clock and weather", "Relógio e clima"),
                tr("Notas y widgets", "Notes and widgets", "Notas e widgets"),
                tr("Aplicaciones e iconos", "Apps and icons", "Aplicativos e ícones"),
                tr("Bóveda y navegación", "App drawer and navigation", "Gaveta e navegação"),
                tr("Sistema y ajustes", "System and settings", "Sistema e ajustes"),
                tr("Motoroll Lab", "Motoroll Lab", "Motoroll Lab")
        };
        new AlertDialog.Builder(this)
                .setTitle(tr("Personalización", "Personalization", "Personalização"))
                .setItems(categories, (dialog, which) -> showCustomizationCategory(which))
                .setNegativeButton(tr("Cerrar", "Close", "Fechar"), null)
                .show();
    }

    private void showCustomizationCategory(int category) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean weatherEnabled = prefs.getBoolean(PREF_WEATHER_ENABLED, false);
        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        switch (category) {
            case 0: // Home screen and wallpapers
                keys.add("wallpaper"); labels.add(tr("Fondos de Motoroll y del teléfono", "Motoroll and phone wallpapers", "Papéis de parede do Motoroll e do telefone"));
                keys.add("home_search"); labels.add(prefs.getBoolean(PREF_HOME_SEARCH_VISIBLE, false)
                        ? tr("Búsqueda inferior: visible", "Home search bar: visible", "Busca inferior: visível")
                        : tr("Búsqueda inferior: oculta", "Home search bar: hidden", "Busca inferior: oculta"));
                keys.add("date"); labels.add(tr("Mostrar u ocultar la fecha", "Show or hide date", "Mostrar ou ocultar a data"));
                break;
            case 1: // Clock and weather
                keys.add("clock_position"); labels.add(tr("Posición del reloj", "Clock position", "Posição do relógio"));
                keys.add("clock_style"); labels.add(tr("Diseño del reloj", "Clock style", "Estilo do relógio"));
                keys.add("weather_toggle"); labels.add(weatherEnabled
                        ? tr("Clima: activado", "Weather: on", "Clima: ativado")
                        : tr("Clima: desactivado", "Weather: off", "Clima: desativado"));
                if (weatherEnabled) {
                    int position = prefs.getInt(PREF_WEATHER_POSITION, 0);
                    keys.add("weather_position"); labels.add(tr("Posición del clima: " + (position == 0 ? "debajo" : position == 2 ? "izquierda" : "derecha"),
                            "Weather position: " + (position == 0 ? "below" : position == 2 ? "left" : "right"),
                            "Posição do clima: " + (position == 0 ? "abaixo" : position == 2 ? "esquerda" : "direita")));
                    keys.add("weather_city"); labels.add(tr("Ciudad del clima · " + prefs.getString(PREF_WEATHER_CITY, ""),
                            "Weather city · " + prefs.getString(PREF_WEATHER_CITY, ""), "Cidade do clima · " + prefs.getString(PREF_WEATHER_CITY, "")));
                    keys.add("weather_style"); labels.add(tr("Diseño del clima: " + (prefs.getInt(PREF_WEATHER_STYLE, 0) == 0 ? "Motoroll" : "Samsung One UI"),
                            "Weather design: " + (prefs.getInt(PREF_WEATHER_STYLE, 0) == 0 ? "Motoroll" : "Samsung One UI"),
                            "Design do clima: " + (prefs.getInt(PREF_WEATHER_STYLE, 0) == 0 ? "Motoroll" : "Samsung One UI")));
                    keys.add("weather_shape"); labels.add(tr("Forma del widget del clima", "Weather widget shape", "Formato do widget de clima"));
                }
                break;
            case 2: // Notes and widgets
                boolean pulseEnabled = prefs.getBoolean(PREF_PULSE_WIDGET, true);
                keys.add("pulse_toggle"); labels.add(pulseEnabled
                        ? tr("Motoroll Pulse: activado ✓", "Motoroll Pulse: enabled ✓", "Motoroll Pulse: ativado ✓")
                        : tr("Motoroll Pulse: desactivado", "Motoroll Pulse: disabled", "Motoroll Pulse: desativado"));
                keys.add("home_note"); labels.add(tr("Editar nota o bienvenida", "Edit note or greeting", "Editar nota ou saudação"));
                keys.add("home_note_style"); labels.add(tr("Color y tamaño de la nota", "Note color and size", "Cor e tamanho da nota"));
                keys.add("widgets"); labels.add(tr("Widgets y elementos de pantalla", "Widgets and home elements", "Widgets e elementos da tela"));
                keys.add("profiles"); labels.add(tr("Perfiles de apariencia", "Appearance profiles", "Perfis de aparência"));
                break;
            case 3: // Apps and icons
                keys.add("favorites"); labels.add(tr("Editar apps favoritas", "Edit favorite apps", "Editar apps favoritos"));
                keys.add("dock_count"); labels.add(tr("Cantidad de apps inferiores: " + prefs.getInt(PREF_DOCK_COUNT, 4),
                        "Bottom app count: " + prefs.getInt(PREF_DOCK_COUNT, 4), "Quantidade de apps inferiores: " + prefs.getInt(PREF_DOCK_COUNT, 4)));
                keys.add("dock"); labels.add(tr("Editar aplicaciones inferiores", "Edit bottom apps", "Editar aplicativos inferiores"));
                keys.add("icons"); labels.add(tr("Tamaño de iconos", "Icon size", "Tamanho dos ícones"));
                keys.add("icon_shape"); labels.add(tr("Forma de los iconos", "Icon shape", "Formato dos ícones"));
                keys.add("hidden_apps"); labels.add(tr("Aplicaciones ocultas", "Hidden apps", "Aplicativos ocultos"));
                break;
            case 4: // Drawer and navigation
                keys.add("drawer"); labels.add(prefs.getInt(PREF_DRAWER_MODE, 0) == 1
                        ? tr("Bóveda: páginas horizontales ✓", "App drawer: horizontal pages ✓", "Gaveta: páginas horizontais ✓")
                        : tr("Bóveda: desplazamiento vertical ✓", "App drawer: vertical scrolling ✓", "Gaveta: rolagem vertical ✓"));
                int swipeMode = prefs.getInt(PREF_DRAWER_SWIPE_DIRECTION, 0);
                keys.add("swipe_direction"); labels.add(tr("Gesto de bóveda: " + (swipeMode == 0 ? "arriba → abajo" : "abajo → arriba"),
                        "Drawer gesture: " + (swipeMode == 0 ? "top → bottom" : "bottom → top"),
                        "Gesto da gaveta: " + (swipeMode == 0 ? "cima → baixo" : "baixo → cima")));
                break;
            case 5: // System
                keys.add("animations"); labels.add(tr("Velocidad de animaciones", "Animation speed", "Velocidade das animações"));
                keys.add("language"); labels.add(tr("Idioma", "Language", "Idioma"));
                keys.add("updates"); labels.add(tr("Actualizaciones de Motoroll", "Motoroll updates", "Atualizações do Motoroll"));
                keys.add("default_home"); labels.add(tr("Establecer Motoroll como launcher predeterminado",
                        "Set Motoroll as default launcher", "Definir Motoroll como launcher padrão"));
                keys.add("memory_mode"); labels.add(memoryProfileLabel(prefs.getInt(PREF_MEMORY_MODE, 0)));
                keys.add("memory_status"); labels.add(tr("Estado de RAM de Motoroll", "Motoroll RAM status", "Estado de RAM do Motoroll"));
                keys.add("updates"); labels.add(tr("Actualizaciones de Motoroll", "Motoroll updates", "Atualizações do Motoroll"));
                keys.add("default_home"); labels.add(tr("Establecer Motoroll como launcher predeterminado",
                        "Set Motoroll as default launcher", "Definir Motoroll como launcher padrão"));
                keys.add("reset"); labels.add(tr("Restablecer personalización", "Reset customization", "Redefinir personalização"));
                break;
            case 6: // Motoroll Lab tools
                keys.add("safesave"); labels.add(tr("SafeSave · copia y restauración", "SafeSave · backup and restore", "SafeSave · backup e restauração"));
                keys.add("smart_drawer"); labels.add(tr("Smart Drawer · opciones de bóveda", "Smart Drawer · drawer options", "Smart Drawer · opções da gaveta"));
                keys.add("studio"); labels.add(tr("Motoroll Studio · perfiles y aspecto", "Motoroll Studio · profiles and look", "Motoroll Studio · perfis e aparência"));
                break;
            default:
                return;
        }

        String[] options = labels.toArray(new String[0]);
        String[] categoryNames = {
                tr("Pantalla y fondos", "Home screen and wallpapers", "Tela e papéis de parede"),
                tr("Reloj y clima", "Clock and weather", "Relógio e clima"),
                tr("Notas y widgets", "Notes and widgets", "Notas e widgets"),
                tr("Aplicaciones e iconos", "Apps and icons", "Aplicativos e ícones"),
                tr("Bóveda y navegación", "App drawer and navigation", "Gaveta e navegação"),
                tr("Sistema y ajustes", "System and settings", "Sistema e ajustes"),
                tr("Motoroll Lab", "Motoroll Lab", "Motoroll Lab")
        };
        new AlertDialog.Builder(this)
                .setTitle(categoryNames[category])
                .setItems(options, (dialog, which) -> runCustomizationAction(keys.get(which)))
                .setNeutralButton(tr("Volver", "Back", "Voltar"), (dialog, which) -> showCustomizationMenu())
                .setNegativeButton(tr("Cerrar", "Close", "Fechar"), null)
                .show();
    }

    private void runCustomizationAction(String key) {
        switch (key) {
            case "wallpaper": chooseWallpaper(); break;
            case "drawer": chooseDrawerMode(); break;
            case "clock_position": chooseClockPosition(); break;
            case "clock_style": chooseClockStyle(); break;
            case "favorites": manageHomeFavorites(); break;
            case "dock": chooseDockSlot(); break;
            case "dock_count": chooseDockCount(); break;
            case "swipe_direction": chooseDrawerSwipeDirection(); break;
            case "updates": checkForUpdates(true); break;
            case "icons": chooseIconSize(); break;
            case "weather_toggle": toggleWeather(); break;
            case "weather_city": chooseWeatherCity(false); break;
            case "weather_style": chooseWeatherStyle(); break;
            case "weather_position": chooseWeatherPosition(); break;
            case "weather_shape": chooseWeatherShape(); break;
            case "home_search": toggleHomeSearch(); break;
            case "date": toggleDateVisibility(); break;
            case "widgets": chooseWidgets(); break;
            case "pulse_toggle": togglePulseWidget(); break;
            case "home_note": chooseHomeNote(); break;
            case "home_note_style": chooseHomeNoteStyle(); break;
            case "icon_shape": chooseIconShape(); break;
            case "animations": chooseAnimationMode(); break;
            case "hidden_apps": chooseHiddenApps(); break;
            case "profiles": manageAppearanceProfiles(); break;
            case "language": chooseLanguage(); break;
            case "default_home": openHomeAppSettings(); break;
            case "reset": resetCustomization(); break;
            case "safesave": showSafeSaveMenu(); break;
            case "smart_drawer": showSmartDrawerMenu(); break;
            case "studio": showMotorollStudioMenu(); break;
            case "memory_mode": chooseMemoryProfile(); break;
            case "memory_status": showMemoryStatus(); break;
        }
    }

    private void addDraggableHomeNote(FrameLayout area, String value) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        TextView note = text(value, 15, TEXT, true);
        note.setMaxLines(2);
        note.setEllipsize(android.text.TextUtils.TruncateAt.END);
        note.setPadding(dp(10), dp(7), dp(10), dp(7));
        note.setBackground(rounded(0xD91A2540, 12, 0xA04B6DAA, 1));
        note.setElevation(dp(3));
        note.setMaxWidth(getResources().getDisplayMetrics().widthPixels - dp(80));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.LEFT);
        area.addView(note, params);
        area.post(() -> {
            int maxX = Math.max(0, area.getWidth() - note.getWidth());
            int maxY = Math.max(0, area.getHeight() - note.getHeight());
            params.leftMargin = Math.round(maxX * (prefs.getInt(PREF_HOME_NOTE_X, 0) / 1000f));
            params.topMargin = Math.round(maxY * (prefs.getInt(PREF_HOME_NOTE_Y, 500) / 1000f));
            note.setLayoutParams(params);
        });
        note.setOnTouchListener((view, event) -> {
            int maxX = Math.max(0, area.getWidth() - view.getWidth());
            int maxY = Math.max(0, area.getHeight() - view.getHeight());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.setTag(new float[]{event.getRawX(), event.getRawY(), params.leftMargin, params.topMargin});
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float[] start = (float[]) view.getTag();
                if (start == null) return true;
                int[] location = new int[2]; area.getLocationOnScreen(location);
                params.leftMargin = Math.max(0, Math.min(maxX, Math.round(start[2] + event.getRawX() - start[0])));
                params.topMargin = Math.max(0, Math.min(maxY, Math.round(start[3] + event.getRawY() - start[1])));
                view.setLayoutParams(params);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                prefs.edit()
                        .putInt(PREF_HOME_NOTE_X, maxX == 0 ? 0 : Math.round(params.leftMargin * 1000f / maxX))
                        .putInt(PREF_HOME_NOTE_Y, maxY == 0 ? 500 : Math.round(params.topMargin * 1000f / maxY))
                        .apply();
                view.performClick();
                return true;
            }
            return true;
        });
        note.setOnClickListener(v -> { });
        note.setContentDescription(tr("Nota personal. Mantén y arrastra para moverla", "Personal note. Touch and drag to move", "Nota pessoal. Toque e arraste para mover"));
    }

    private void toggleDateVisibility() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean visible = prefs.getBoolean(PREF_DATE_VISIBLE, true);
        prefs.edit().putBoolean(PREF_DATE_VISIBLE, !visible).apply();
        showHome(true);
    }

    private void chooseHomeNote() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(3);
        input.setText(prefs.getString(PREF_HOME_NOTE, ""));
        input.setHint(tr("Ej.: ¡Hola! / ¡Bienvenido, José!", "E.g. Hello! / Welcome, Alex!", "Ex.: Olá! / Bem-vindo, José!"));
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setBackground(rounded(0xE6101423, 14, 0x90424A70, 1));
        LinearLayout wrapper = column();
        wrapper.setPadding(dp(4), dp(8), dp(4), dp(2));
        wrapper.addView(input, lp(-1, -2));
        new AlertDialog.Builder(this)
                .setTitle(tr("Texto de bienvenida", "Welcome message", "Texto de boas-vindas"))
                .setMessage(tr("Escribe una nota breve para mostrar arriba del reloj. Déjalo vacío para ocultarla.",
                        "Write a short note to show above the clock. Leave it empty to hide it.",
                        "Escreva uma nota curta para mostrar acima do relógio. Deixe vazio para ocultar."))
                .setView(wrapper)
                .setPositiveButton(tr("Guardar", "Save", "Salvar"), (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.length() > 80) value = value.substring(0, 80);
                    prefs.edit().putString(PREF_HOME_NOTE, value).apply();
                    showHome(true);
                })
                .setNeutralButton(tr("Quitar nota", "Clear note", "Limpar nota"), (dialog, which) -> {
                    prefs.edit().remove(PREF_HOME_NOTE).remove(PREF_HOME_NOTE_X).remove(PREF_HOME_NOTE_Y).apply();
                    showHome(true);
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void chooseHomeNoteStyle() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String[] colors = {
                tr("Cian Motoroll", "Motoroll cyan", "Ciano Motoroll"),
                tr("Blanco", "White", "Branco"),
                tr("Azul eléctrico", "Electric blue", "Azul elétrico"),
                tr("Violeta", "Violet", "Violeta"),
                tr("Verde", "Green", "Verde"),
                tr("Naranja", "Orange", "Laranja"),
                tr("Rosa", "Pink", "Rosa")
        };
        int[] colorValues = {CYAN, Color.WHITE, 0xFF579BFF, 0xFFB18CFF, 0xFF75D6A0, 0xFFFFB36B, 0xFFFF8FC7};
        int selectedColor = 0;
        int currentColor = prefs.getInt(PREF_HOME_NOTE_COLOR, CYAN);
        for (int i = 0; i < colorValues.length; i++) if (colorValues[i] == currentColor) selectedColor = i;
        final int[] chosenColor = {selectedColor};
        new AlertDialog.Builder(this)
                .setTitle(tr("Color de la nota", "Note color", "Cor da nota"))
                .setSingleChoiceItems(colors, selectedColor, (d, which) -> chosenColor[0] = which)
                .setPositiveButton(tr("Siguiente", "Next", "Próximo"), (d, w) -> {
                    String[] sizes = {"10", "12", "14", "16", "18"};
                    int currentSize = prefs.getInt(PREF_HOME_NOTE_SIZE, 12);
                    int selectedSize = Math.max(0, Math.min(4, (currentSize - 10) / 2));
                    new AlertDialog.Builder(this)
                            .setTitle(tr("Tamaño de la nota", "Note size", "Tamanho da nota"))
                            .setSingleChoiceItems(sizes, selectedSize, (dialog, which) -> {
                                prefs.edit().putInt(PREF_HOME_NOTE_COLOR, colorValues[chosenColor[0]])
                                        .putInt(PREF_HOME_NOTE_SIZE, 10 + which * 2).apply();
                                dialog.dismiss();
                                showHome(true);
                            })
                            .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                            .show();
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void chooseWidgets() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String[] options = {
                tr("Mostrar fecha debajo del reloj", "Show date below clock", "Mostrar data abaixo do relógio"),
                tr("Mostrar widget compacto de batería", "Show compact battery widget", "Mostrar widget compacto de bateria"),
                tr("Mostrar Motoroll Pulse", "Show Motoroll Pulse", "Mostrar Motoroll Pulse")
        };
        boolean[] checked = {
                prefs.getBoolean(PREF_DATE_VISIBLE, true),
                prefs.getBoolean(PREF_BATTERY_WIDGET, false),
                prefs.getBoolean(PREF_PULSE_WIDGET, true)
        };
        new AlertDialog.Builder(this).setTitle(tr("Widgets de Motoroll", "Motoroll widgets", "Widgets do Motoroll"))
                .setMultiChoiceItems(options, checked, (dialog, which, value) -> checked[which] = value)
                .setPositiveButton(tr("Aplicar", "Apply", "Aplicar"), (d, w) -> {
                    prefs.edit().putBoolean(PREF_DATE_VISIBLE, checked[0])
                            .putBoolean(PREF_BATTERY_WIDGET, checked[1])
                            .putBoolean(PREF_PULSE_WIDGET, checked[2]).apply();
                    showHome(true);
                }).setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private void chooseIconShape() {
        String[] choices = {tr("Redondeados", "Rounded", "Arredondados"), tr("Circulares", "Circular", "Circulares"), tr("Cuadrados", "Square", "Quadrados")};
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_ICON_SHAPE, 0);
        new AlertDialog.Builder(this).setTitle(tr("Forma de los iconos", "Icon shape", "Formato dos ícones"))
                .setSingleChoiceItems(choices, selected, (d, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(PREF_ICON_SHAPE, which).apply();
                    d.dismiss(); showHome(true);
                }).setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private void chooseAnimationMode() {
        String[] choices = {tr("Reducidas · máxima respuesta", "Reduced · fastest response", "Reduzidas · resposta rápida"),
                tr("Suaves · recomendado", "Smooth · recommended", "Suaves · recomendado"),
                tr("Relajadas · transiciones lentas", "Relaxed · slower transitions", "Suaves · transições lentas")};
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_ANIMATION_MODE, 0);
        new AlertDialog.Builder(this).setTitle(tr("Animaciones", "Animations", "Animações"))
                .setSingleChoiceItems(choices, selected, (d, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(PREF_ANIMATION_MODE, which).apply();
                    d.dismiss(); showHome(true);
                }).setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private long animationDuration() {
        int mode = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_ANIMATION_MODE, 0);
        return mode == 0 ? 90L : mode == 2 ? 240L : 150L;
    }

    private long drawerAnimationDuration() {
        int mode = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_ANIMATION_MODE, 0);
        return mode == 0 ? 145L : mode == 2 ? 225L : 185L;
    }

    private GradientDrawable iconFrameBackground() {
        int shape = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_ICON_SHAPE, 0);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xC51A1D32);
        drawable.setStroke(dp(1), 0x70434D79);
        if (shape == 1) drawable.setShape(GradientDrawable.OVAL);
        else drawable.setCornerRadius(dp(shape == 2 ? 10 : 18));
        return drawable;
    }

    private Set<String> hiddenPackages() {
        Set<String> saved = getSharedPreferences(PREFS, MODE_PRIVATE).getStringSet(PREF_HIDDEN_APPS, Collections.emptySet());
        return saved == null ? new HashSet<>() : new HashSet<>(saved);
    }

    private void chooseHiddenApps() {
        Set<String> hidden = hiddenPackages();
        String[] labels = new String[apps.size()];
        boolean[] checked = new boolean[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo info = apps.get(i);
            labels[i] = safeLabel(info);
            checked[i] = hidden.contains(info.activityInfo.packageName);
        }
        new AlertDialog.Builder(this).setTitle(tr("Ocultar apps de la bóveda (no es un bloqueo)", "Hide apps from drawer (not a security lock)", "Ocultar apps da gaveta (não é bloqueio de segurança)"))
                .setMultiChoiceItems(labels, checked, (dialog, which, value) -> {
                    String pkg = apps.get(which).activityInfo.packageName;
                    if (value) hidden.add(pkg); else hidden.remove(pkg);
                }).setPositiveButton(tr("Guardar", "Save", "Salvar"), (d, w) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putStringSet(PREF_HIDDEN_APPS, hidden).apply();
                    showHome(true);
                }).setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private JSONObject currentAppearance() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        JSONObject data = new JSONObject();
        try {
            data.put(PREF_ICON_SIZE, p.getInt(PREF_ICON_SIZE, 1));
            data.put(PREF_CLOCK_STYLE, p.getInt(PREF_CLOCK_STYLE, 0));
            data.put(PREF_CLOCK_POSITION, p.getInt(PREF_CLOCK_POSITION, 0));
            data.put(PREF_DRAWER_MODE, p.getInt(PREF_DRAWER_MODE, 0));
            data.put(PREF_DOCK_COUNT, p.getInt(PREF_DOCK_COUNT, 4));
            data.put(PREF_WEATHER_STYLE, p.getInt(PREF_WEATHER_STYLE, 0));
            data.put(PREF_WEATHER_POSITION, p.getInt(PREF_WEATHER_POSITION, 0));
            data.put(PREF_ICON_SHAPE, p.getInt(PREF_ICON_SHAPE, 0));
            data.put(PREF_ANIMATION_MODE, p.getInt(PREF_ANIMATION_MODE, 0));
            data.put(PREF_DATE_VISIBLE, p.getBoolean(PREF_DATE_VISIBLE, true));
            data.put(PREF_BATTERY_WIDGET, p.getBoolean(PREF_BATTERY_WIDGET, false));
            data.put(PREF_WALLPAPER_URI, p.getString(PREF_WALLPAPER_URI, ""));
            data.put(PREF_FORCE_MOTOROLL_BG, p.getBoolean(PREF_FORCE_MOTOROLL_BG, false));
            data.put(PREF_HOME_NOTE, p.getString(PREF_HOME_NOTE, ""));
            data.put(PREF_HOME_NOTE_COLOR, p.getInt(PREF_HOME_NOTE_COLOR, CYAN));
            data.put(PREF_HOME_NOTE_SIZE, p.getInt(PREF_HOME_NOTE_SIZE, 12));
            data.put(PREF_WEATHER_SHAPE, p.getInt(PREF_WEATHER_SHAPE, 0));
            data.put(PREF_HOME_SEARCH_VISIBLE, p.getBoolean(PREF_HOME_SEARCH_VISIBLE, false));
            data.put(PREF_HOME_NOTE_X, p.getInt(PREF_HOME_NOTE_X, 0));
            data.put(PREF_HOME_NOTE_Y, p.getInt(PREF_HOME_NOTE_Y, 500));
            data.put(PREF_DRAWER_SWIPE_DIRECTION, p.getInt(PREF_DRAWER_SWIPE_DIRECTION, 0));
            data.put(PREF_FAVORITE_PACKAGES, p.getString(PREF_FAVORITE_PACKAGES, ""));
        } catch (Exception ignored) { }
        return data;
    }

    private void manageAppearanceProfiles() {
        String[] actions = {tr("Guardar apariencia actual como perfil", "Save current appearance as profile", "Salvar aparência atual como perfil"),
                tr("Aplicar un perfil guardado", "Apply a saved profile", "Aplicar um perfil salvo"),
                tr("Eliminar un perfil", "Delete a profile", "Excluir um perfil")};
        new AlertDialog.Builder(this).setTitle(tr("Perfiles de apariencia", "Appearance profiles", "Perfis de aparência"))
                .setItems(actions, (d, which) -> {
                    if (which == 0) saveAppearanceProfile();
                    else if (which == 1) selectAppearanceProfile(false);
                    else selectAppearanceProfile(true);
                }).setNegativeButton(tr("Cerrar", "Close", "Fechar"), null).show();
    }

    private void saveAppearanceProfile() {
        EditText name = new EditText(this);
        name.setSingleLine(true); name.setHint(tr("Nombre del perfil", "Profile name", "Nome do perfil"));
        new AlertDialog.Builder(this).setTitle(tr("Guardar perfil", "Save profile", "Salvar perfil")).setView(name)
                .setPositiveButton(tr("Guardar", "Save", "Salvar"), (d, w) -> {
                    String value = name.getText().toString().trim();
                    if (value.isEmpty()) return;
                    SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
                    Set<String> profiles = new HashSet<>(p.getStringSet(PREF_PROFILES, Collections.emptySet()));
                    profiles.add(value);
                    p.edit().putString("profile_" + value, currentAppearance().toString()).putStringSet(PREF_PROFILES, profiles).apply();
                    Toast.makeText(this, tr("Perfil guardado", "Profile saved", "Perfil salvo"), Toast.LENGTH_SHORT).show();
                }).setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private void selectAppearanceProfile(boolean delete) {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> names = new HashSet<>(p.getStringSet(PREF_PROFILES, Collections.emptySet()));
        if (names.isEmpty()) { Toast.makeText(this, tr("Todavía no hay perfiles guardados", "No saved profiles yet", "Ainda não há perfis salvos"), Toast.LENGTH_SHORT).show(); return; }
        List<String> ordered = new ArrayList<>(names); Collections.sort(ordered);
        new AlertDialog.Builder(this).setTitle(delete ? tr("Eliminar perfil", "Delete profile", "Excluir perfil") : tr("Aplicar perfil", "Apply profile", "Aplicar perfil"))
                .setItems(ordered.toArray(new String[0]), (d, which) -> {
                    String name = ordered.get(which);
                    if (delete) {
                        names.remove(name); p.edit().remove("profile_" + name).putStringSet(PREF_PROFILES, names).apply();
                        Toast.makeText(this, tr("Perfil eliminado", "Profile deleted", "Perfil excluído"), Toast.LENGTH_SHORT).show();
                    } else {
                        try {
                            JSONObject data = new JSONObject(p.getString("profile_" + name, "{}"));
                            SharedPreferences.Editor edit = p.edit();
                            edit.putInt(PREF_ICON_SIZE, data.optInt(PREF_ICON_SIZE, 1));
                            edit.putInt(PREF_CLOCK_STYLE, data.optInt(PREF_CLOCK_STYLE, 0));
                            edit.putInt(PREF_CLOCK_POSITION, data.optInt(PREF_CLOCK_POSITION, 0));
                            edit.putInt(PREF_DRAWER_MODE, data.optInt(PREF_DRAWER_MODE, 0));
                            edit.putInt(PREF_DOCK_COUNT, data.optInt(PREF_DOCK_COUNT, 4));
                            edit.putInt(PREF_WEATHER_STYLE, data.optInt(PREF_WEATHER_STYLE, 0));
                            edit.putInt(PREF_WEATHER_POSITION, data.optInt(PREF_WEATHER_POSITION, 0));
                            edit.putInt(PREF_ICON_SHAPE, data.optInt(PREF_ICON_SHAPE, 0));
                            edit.putInt(PREF_ANIMATION_MODE, data.optInt(PREF_ANIMATION_MODE, 0));
                            edit.putBoolean(PREF_DATE_VISIBLE, data.optBoolean(PREF_DATE_VISIBLE, true));
                            edit.putBoolean(PREF_BATTERY_WIDGET, data.optBoolean(PREF_BATTERY_WIDGET, false));
                            edit.putString(PREF_HOME_NOTE, data.optString(PREF_HOME_NOTE, ""));
                            edit.putInt(PREF_HOME_NOTE_COLOR, data.optInt(PREF_HOME_NOTE_COLOR, CYAN));
                            edit.putInt(PREF_HOME_NOTE_SIZE, data.optInt(PREF_HOME_NOTE_SIZE, 12));
                            edit.putInt(PREF_WEATHER_SHAPE, data.optInt(PREF_WEATHER_SHAPE, 0));
                            edit.putBoolean(PREF_HOME_SEARCH_VISIBLE, data.optBoolean(PREF_HOME_SEARCH_VISIBLE, false));
                            edit.putInt(PREF_HOME_NOTE_X, data.optInt(PREF_HOME_NOTE_X, 0));
                            edit.putInt(PREF_HOME_NOTE_Y, data.optInt(PREF_HOME_NOTE_Y, 500));
                            edit.putInt(PREF_DRAWER_SWIPE_DIRECTION, data.optInt(PREF_DRAWER_SWIPE_DIRECTION, 0));
                            String savedFavorites = data.optString(PREF_FAVORITE_PACKAGES, "");
                            if (savedFavorites.isEmpty()) edit.remove(PREF_FAVORITE_PACKAGES);
                            else edit.putString(PREF_FAVORITE_PACKAGES, savedFavorites);
                            String wallpaper = data.optString(PREF_WALLPAPER_URI, "");
                            if (wallpaper.isEmpty()) edit.remove(PREF_WALLPAPER_URI);
                            else edit.putString(PREF_WALLPAPER_URI, wallpaper);
                            edit.putBoolean(PREF_FORCE_MOTOROLL_BG, data.optBoolean(PREF_FORCE_MOTOROLL_BG, false));
                            edit.apply(); wallpaperBitmap = null; showHome(true);
                        } catch (Exception error) { Toast.makeText(this, tr("No se pudo cargar el perfil", "Could not load profile", "Não foi possível carregar o perfil"), Toast.LENGTH_LONG).show(); }
                    }
                }).setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private String pulseGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return tr("Buenos días", "Good morning", "Bom dia");
        if (hour >= 12 && hour < 19) return tr("Buenas tardes", "Good afternoon", "Boa tarde");
        return tr("Buenas noches", "Good evening", "Boa noite");
    }

    private void addPulseWidget(LinearLayout content) {
        LinearLayout card = row();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(14), dp(12));
        GradientDrawable cardBackground = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xE91D3153, 0xEF11182B});
        cardBackground.setCornerRadius(dp(20));
        cardBackground.setStroke(dp(1), 0x90465F91);
        card.setBackground(cardBackground);
        card.setElevation(dp(2));

        LinearLayout details = column();
        details.setGravity(Gravity.CENTER_VERTICAL);
        TextView eyebrow = text("MOTOROLL PULSE", 9, CYAN, true);
        eyebrow.setLetterSpacing(0.16f);
        details.addView(eyebrow);

        pulseGreetingText = text(pulseGreeting(), 20, TEXT, true);
        details.addView(pulseGreetingText, withTop(lp(-1, -2), 3));

        pulseStatusText = text(tr("Tu energía, de un vistazo", "Your energy at a glance", "Sua energia em um olhar"),
                10, MUTED, false);
        pulseStatusText.setSingleLine(true);
        pulseStatusText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        details.addView(pulseStatusText, withTop(lp(-1, -2), 3));
        card.addView(details, new LinearLayout.LayoutParams(0, -2, 1f));

        FrameLayout batteryGauge = new FrameLayout(this);
        pulseBatteryRing = new PulseBatteryRing(this);
        batteryGauge.addView(pulseBatteryRing,
                new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER));
        pulseBatteryText = text("--", 13, TEXT, true);
        pulseBatteryText.setGravity(Gravity.CENTER);
        batteryGauge.addView(pulseBatteryText,
                new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        card.addView(batteryGauge, lp(58, 58));

        card.setContentDescription(tr(
                "Motoroll Pulse. Toca para abrir los ajustes de batería. Mantén pulsado para personalizar.",
                "Motoroll Pulse. Tap to open battery settings. Long press to personalize.",
                "Motoroll Pulse. Toque para abrir as configurações de bateria. Pressione para personalizar."));
        card.setOnClickListener(v -> openBatterySettings());
        card.setOnLongClickListener(v -> {
            showPulseOptions();
            return true;
        });
        content.addView(card, withTop(lp(-1, 94), 20));
        updatePulseWidget();
    }

    private void showPulseOptions() {
        boolean enabled = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_PULSE_WIDGET, true);
        String toggleLabel = enabled
                ? tr("Desactivar Motoroll Pulse", "Turn off Motoroll Pulse", "Desativar Motoroll Pulse")
                : tr("Activar Motoroll Pulse", "Turn on Motoroll Pulse", "Ativar Motoroll Pulse");
        new AlertDialog.Builder(this)
                .setTitle("MOTOROLL PULSE")
                .setItems(new String[]{
                        toggleLabel,
                        tr("Personalización general", "General personalization", "Personalização geral")
                }, (dialog, which) -> {
                    if (which == 0) setPulseWidgetEnabled(!enabled);
                    else showCustomizationMenu();
                })
                .setNegativeButton(tr("Cerrar", "Close", "Fechar"), null)
                .show();
    }

    private void togglePulseWidget() {
        boolean enabled = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_PULSE_WIDGET, true);
        setPulseWidgetEnabled(!enabled);
    }

    private void setPulseWidgetEnabled(boolean enabled) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(PREF_PULSE_WIDGET, enabled).apply();
        Toast.makeText(this, enabled
                ? tr("Motoroll Pulse activado", "Motoroll Pulse enabled", "Motoroll Pulse ativado")
                : tr("Motoroll Pulse desactivado", "Motoroll Pulse disabled", "Motoroll Pulse desativado"),
                Toast.LENGTH_SHORT).show();
        showHome(true);
    }

    private void updatePulseWidget() {
        if (pulseBatteryRing == null) return;
        if (pulseGreetingText != null) pulseGreetingText.setText(pulseGreeting());

        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            pulseBatteryRing.setBatteryLevel(-1);
            if (pulseBatteryText != null) pulseBatteryText.setText("--");
            if (pulseStatusText != null) pulseStatusText.setText(tr(
                    "Estado de batería no disponible", "Battery status unavailable", "Status da bateria indisponível"));
            return;
        }

        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int percent = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        boolean full = status == BatteryManager.BATTERY_STATUS_FULL;
        pulseBatteryRing.setBatteryLevel(percent);
        if (pulseBatteryText != null) pulseBatteryText.setText(percent >= 0 ? percent + "%" : "--");

        String state;
        if (full) state = tr("Carga completa", "Fully charged", "Carga completa");
        else if (charging) state = tr("Cargando ahora", "Charging now", "Carregando agora");
        else if (percent >= 0 && percent <= 15) state = tr("Batería baja", "Low battery", "Bateria baixa");
        else if (percent >= 0 && percent >= 85) state = tr("Energía lista para el día", "Power ready for the day", "Energia pronta para o dia");
        else state = tr("Energía estable", "Power is steady", "Energia estável");
        if (pulseStatusText != null) pulseStatusText.setText(state);
    }

    private void openBatterySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        } catch (Exception firstError) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception secondError) {
                Toast.makeText(this, tr("No se pudieron abrir los ajustes", "Could not open settings", "Não foi possível abrir as configurações"),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addBatteryWidget(LinearLayout content) {
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return;
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int percent = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        LinearLayout chip = row(); chip.setGravity(Gravity.CENTER_VERTICAL); chip.setPadding(dp(13), dp(8), dp(13), dp(8));
        chip.setBackground(rounded(PANEL, 15, STROKE, 1));
        TextView symbol = text(charging ? "ϟ" : "▰", 15, CYAN, true); chip.addView(symbol, lp(22, -2));
        TextView value = text(percent < 0 ? tr("Batería", "Battery", "Bateria") : percent + "% " + (charging ? tr("cargando", "charging", "carregando") : tr("de batería", "battery", "de bateria")), 11, TEXT, true);
        batteryWidgetText = value;
        chip.addView(value, withTop(lp(-2, -2), 0));
        content.addView(chip, withTop(lp(-2, -2), 12));
        batteryWidget = chip;
    }

    private void updateBatteryWidget() {
        if (batteryWidgetText == null) return;
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return;
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int percent = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        batteryWidgetText.setText(percent < 0 ? tr("Batería", "Battery", "Bateria") : percent + "% " + (charging ? tr("cargando", "charging", "carregando") : tr("de batería", "battery", "de bateria")));
    }

    private void toggleHomeSearch() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean visible = !prefs.getBoolean(PREF_HOME_SEARCH_VISIBLE, false);
        prefs.edit().putBoolean(PREF_HOME_SEARCH_VISIBLE, visible).apply();
        Toast.makeText(this, visible
                ? tr("Búsqueda inferior activada", "Home search bar enabled", "Busca inferior ativada")
                : tr("Búsqueda inferior ocultada", "Home search bar hidden", "Busca inferior ocultada"), Toast.LENGTH_SHORT).show();
        showHome(true);
    }

    private void chooseDrawerMode() {
        String[] choices = {
                tr("Vertical · lista continua", "Vertical · continuous list", "Vertical · lista contínua"),
                tr("Horizontal · páginas tipo Samsung", "Horizontal · Samsung-style pages", "Horizontal · páginas estilo Samsung")
        };
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_DRAWER_MODE, 0);
        new AlertDialog.Builder(this)
                .setTitle(tr("Estilo de la bóveda", "App drawer style", "Estilo da gaveta"))
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(PREF_DRAWER_MODE, which).apply();
                    dialog.dismiss();
                    showHome(true);
                    Toast.makeText(this, tr("Estilo de bóveda actualizado", "App drawer style updated", "Estilo da gaveta atualizado"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void openHomeAppSettings() {
        try {
            startActivity(new Intent("android.settings.HOME_SETTINGS"));
        } catch (Exception first) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
            } catch (Exception second) {
                Toast.makeText(this, tr("No se pudieron abrir los ajustes de launcher", "Could not open Home app settings", "Não foi possível abrir as configurações do launcher"), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Could not open default Home settings", second);
            }
        }
    }

    private void chooseLanguage() {
        String[] choices = {
                tr("Automático · idioma del teléfono", "Automatic · phone language", "Automático · idioma do telefone"),
                "Español", "English", "Português"
        };
        String current = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_LANGUAGE, "system");
        int selected = "es".equals(current) ? 1 : "en".equals(current) ? 2 : "pt".equals(current) ? 3 : 0;
        new AlertDialog.Builder(this)
                .setTitle(tr("Idioma de Motoroll", "Motoroll language", "Idioma do Motoroll"))
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    String value = which == 1 ? "es" : which == 2 ? "en" : which == 3 ? "pt" : "system";
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_LANGUAGE, value).apply();
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void toggleWeather() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(PREF_WEATHER_ENABLED, false);
        if (enabled) {
            prefs.edit().putBoolean(PREF_WEATHER_ENABLED, false).apply();
            weatherStatus = null;
            showHome(true);
            Toast.makeText(this, tr("Clima desactivado", "Weather disabled", "Clima desativado"), Toast.LENGTH_SHORT).show();
        } else {
            chooseWeatherCity(true);
        }
    }

    private void chooseWeatherCity(boolean enableAfterSave) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setHint(tr("Ejemplo: Quintero", "Example: London", "Exemplo: Lisboa"));
        String existing = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_WEATHER_CITY, "");
        input.setText(existing);
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this)
                .setTitle(tr("Ciudad para el clima", "Weather location", "Cidade para o clima"))
                .setMessage(tr("Escribe una ciudad. Motoroll no necesita acceder a tu ubicación.",
                        "Enter a city. Motoroll does not need access to your location.",
                        "Digite uma cidade. Motoroll não precisa acessar sua localização."))
                .setView(input)
                .setPositiveButton(tr("Guardar", "Save", "Salvar"), (dialog, which) -> {
                    String city = input.getText().toString().trim();
                    if (city.isEmpty()) {
                        Toast.makeText(this, tr("Escribe una ciudad", "Enter a city", "Digite uma cidade"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(PREF_WEATHER_CITY, city).putLong(PREF_WEATHER_FETCHED, 0L);
                    if (enableAfterSave) editor.putBoolean(PREF_WEATHER_ENABLED, true);
                    editor.apply();
                    showHome(true);
                    refreshWeatherIfNeeded(true);
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void updateWeatherViews() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String place = prefs.getString(PREF_WEATHER_PLACE, prefs.getString(PREF_WEATHER_CITY, ""));
        String temperature = prefs.getString(PREF_WEATHER_TEMP, "--°C");
        String description = prefs.getString(PREF_WEATHER_DESCRIPTION, tr("Clima no disponible", "Weather unavailable", "Clima indisponível"));
        if (weatherStatus != null) weatherStatus.setText(weatherSummary());
        if (weatherPlaceView != null) weatherPlaceView.setText(place);
        if (weatherTemperatureView != null) weatherTemperatureView.setText(temperature);
        if (weatherConditionView != null) weatherConditionView.setText(description);
        if (weatherIconView != null) weatherIconView.setText(weatherSymbol(description));
    }

    private void refreshWeatherIfNeeded(boolean force) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_WEATHER_ENABLED, false)) return;
        String city = prefs.getString(PREF_WEATHER_CITY, "").trim();
        if (city.isEmpty() || weatherRefreshInFlight) return;
        long fetched = prefs.getLong(PREF_WEATHER_FETCHED, 0L);
        if (!force && System.currentTimeMillis() - fetched < WEATHER_REFRESH_MS) return;
        weatherRefreshInFlight = true;
        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(city, "UTF-8");
                JSONObject geo = requestJson("https://geocoding-api.open-meteo.com/v1/search?name="
                        + encoded + "&count=1&language=es&format=json");
                JSONArray results = geo.optJSONArray("results");
                if (results == null || results.length() == 0) throw new Exception("Ciudad no encontrada");
                JSONObject place = results.getJSONObject(0);
                double latitude = place.getDouble("latitude");
                double longitude = place.getDouble("longitude");
                String placeName = place.optString("name", city);
                String admin = place.optString("admin1", "");
                JSONObject forecast = requestJson("https://api.open-meteo.com/v1/forecast?latitude="
                        + latitude + "&longitude=" + longitude
                        + "&current=temperature_2m,weather_code&timezone=auto");
                JSONObject current = forecast.getJSONObject("current");
                String temperature = Math.round(current.getDouble("temperature_2m")) + "°C";
                String description = weatherDescription(current.optInt("weather_code", -1));
                String placeLabel = admin.isEmpty() ? placeName : placeName + ", " + admin;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString(PREF_WEATHER_TEMP, temperature)
                        .putString(PREF_WEATHER_DESCRIPTION, description)
                        .putString(PREF_WEATHER_PLACE, placeLabel)
                        .putLong(PREF_WEATHER_FETCHED, System.currentTimeMillis())
                        .apply();
                runOnUiThread(() -> {
                    weatherRefreshInFlight = false;
                    updateWeatherViews();
                });
            } catch (Exception error) {
                Log.w(TAG, "Weather refresh failed", error);
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString(PREF_WEATHER_DESCRIPTION, tr("Clima no disponible", "Weather unavailable", "Clima indisponível"))
                        .putLong(PREF_WEATHER_FETCHED, System.currentTimeMillis())
                        .apply();
                runOnUiThread(() -> {
                    weatherRefreshInFlight = false;
                    updateWeatherViews();
                });
            }
        }, "MotorollWeather").start();
    }

    private JSONObject requestJson(String address) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5500);
            connection.setReadTimeout(5500);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "MotorollHome/3.2.0");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new Exception("HTTP " + status);
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            return new JSONObject(body.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String weatherDescription(int code) {
        if (code == 0) return tr("Despejado", "Clear", "Céu limpo");
        if (code <= 3 && code >= 1) return tr("Parcialmente nublado", "Partly cloudy", "Parcialmente nublado");
        if (code == 45 || code == 48) return tr("Niebla", "Fog", "Nevoeiro");
        if (code >= 51 && code <= 67) return tr("Llovizna o lluvia", "Drizzle or rain", "Garoa ou chuva");
        if (code >= 71 && code <= 77) return tr("Nieve", "Snow", "Neve");
        if (code >= 80 && code <= 82) return tr("Chubascos", "Rain showers", "Pancadas de chuva");
        if (code >= 95) return tr("Tormenta", "Thunderstorm", "Tempestade");
        return tr("Condición no disponible", "Condition unavailable", "Condição indisponível");
    }

    private String weatherSummary() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String city = prefs.getString(PREF_WEATHER_PLACE, prefs.getString(PREF_WEATHER_CITY, ""));
        String temperature = prefs.getString(PREF_WEATHER_TEMP, "--°C");
        String description = prefs.getString(PREF_WEATHER_DESCRIPTION, tr("Consultando clima…", "Loading weather…", "Carregando clima…"));
        return city + "  ·  " + temperature + "  ·  " + description;
    }

    private void openWeatherProviderInfo() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/")));
        } catch (Exception error) {
            Toast.makeText(this, tr("Datos meteorológicos de Open-Meteo", "Weather data by Open-Meteo", "Dados meteorológicos da Open-Meteo"), Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseClockPosition() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String[] choices = {
                tr("Izquierda", "Left", "Esquerda"),
                tr("Centro", "Center", "Centro"),
                tr("Derecha", "Right", "Direita")
        };
        int selected = prefs.getInt(PREF_CLOCK_POSITION, 0);
        new AlertDialog.Builder(this)
                .setTitle(tr("Posición del reloj", "Clock position", "Posição do relógio"))
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    SharedPreferences.Editor editor = prefs.edit().putInt(PREF_CLOCK_POSITION, which);
                    int weatherPosition = prefs.getInt(PREF_WEATHER_POSITION, 0);
                    // If clock and weather would occupy the same edge, move weather
                    // automatically to the opposite edge. Center never conflicts.
                    if (which == 0 && weatherPosition == 2) editor.putInt(PREF_WEATHER_POSITION, 1);
                    else if (which == 2 && weatherPosition == 1) editor.putInt(PREF_WEATHER_POSITION, 2);
                    editor.apply();
                    dialog.dismiss();
                    showHome(true);
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void chooseClockStyle() {
        String[] choices = {tr("Grande y fino", "Large and thin", "Grande e fino"), tr("Compacto", "Compact", "Compacto"), tr("Digital", "Digital", "Digital")};
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_CLOCK_STYLE, 0);
        new AlertDialog.Builder(this)
                .setTitle(tr("Diseño del reloj", "Clock style", "Estilo do relógio"))
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt(PREF_CLOCK_STYLE, which).apply();
                    dialog.dismiss();
                    showHome(true);
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void chooseIconSize() {
        String[] choices = {tr("Compactos", "Small", "Compactos"), tr("Normales", "Medium", "Normais"), tr("Grandes", "Large", "Grandes")};
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_ICON_SIZE, 1);
        new AlertDialog.Builder(this)
                .setTitle(tr("Tamaño de iconos", "Icon size", "Tamanho dos ícones"))
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt(PREF_ICON_SIZE, which).apply();
                    dialog.dismiss();
                    showHome(true);
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }


    private static final String UPDATE_MANIFEST_URL =
            "https://raw.githubusercontent.com/perrunofortnite209-max/motoroll-L2/main/update.json";
    private static final String UPDATE_DOWNLOAD_ID = "motoroll_pending_update_download";
    private boolean awaitingInstallPermission;

    private void checkForUpdates(boolean showUpToDate) {
        Toast.makeText(this, tr("Buscando actualizaciones…", "Checking for updates…", "Procurando atualizações…"), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(UPDATE_MANIFEST_URL).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Cache-Control", "no-cache");
                JSONObject manifest;
                try (InputStream input = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))) {
                    StringBuilder json = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                    manifest = new JSONObject(json.toString());
                }
                int latestCode = manifest.getInt("versionCode");
                String latestName = manifest.getString("versionName");
                String apkUrl = manifest.getString("apkUrl");
                String notes = manifest.optString("releaseNotes", "");
                int currentCode;
                try {
                    android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                    currentCode = Build.VERSION.SDK_INT >= 28 ? (int) info.getLongVersionCode() : info.versionCode;
                } catch (Exception e) { currentCode = 0; }
                final boolean updateAvailable = latestCode > currentCode;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (!updateAvailable) {
                        if (showUpToDate) new AlertDialog.Builder(this).setTitle("Motoroll Update Center")
                                .setMessage(tr("Ya tienes la última versión instalada.", "You already have the latest version installed.", "Você já tem a versão mais recente instalada."))
                                .setPositiveButton("OK", null).show();
                        return;
                    }
                    new AlertDialog.Builder(this).setTitle(tr("Actualización disponible", "Update available", "Atualização disponível"))
                            .setMessage("Motoroll Home " + latestName + " · " + tr("versión", "version", "versão") + " " + latestCode
                                    + (notes.isEmpty() ? "" : "\n\n" + notes))
                            .setNegativeButton(tr("Más tarde", "Later", "Mais tarde"), null)
                            .setPositiveButton(tr("Descargar", "Download", "Baixar"), (dialog, which) -> downloadUpdate(apkUrl, latestName))
                            .show();
                });
            } catch (Exception error) {
                Log.e(TAG, "Update check failed", error);
                runOnUiThread(() -> {
                    if (!isFinishing()) new AlertDialog.Builder(this).setTitle("Motoroll Update Center")
                            .setMessage(tr("No se pudo comprobar si hay actualizaciones. Revisa tu conexión e inténtalo de nuevo.",
                                    "Could not check for updates. Check your connection and try again.",
                                    "Não foi possível verificar atualizações. Verifique sua conexão e tente novamente."))
                            .setPositiveButton("OK", null).show();
                });
            } finally { if (connection != null) connection.disconnect(); }
        }, "Motoroll-UpdateCheck").start();
    }

    private void downloadUpdate(String apkUrl, String versionName) {
        try {
            android.app.DownloadManager manager = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("Motoroll Home " + versionName);
            request.setDescription(tr("Descargando actualización", "Downloading update", "Baixando atualização"));
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "MotorollHome-update.apk");
            long id = manager.enqueue(request);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong(UPDATE_DOWNLOAD_ID, id).apply();
            Toast.makeText(this, tr("Descarga iniciada. Te avisaremos al terminar.", "Download started. We’ll notify you when it finishes.", "Download iniciado. Avisaremos quando terminar."), Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Log.e(TAG, "Update download failed", error);
            Toast.makeText(this, tr("No se pudo iniciar la descarga.", "Could not start download.", "Não foi possível iniciar o download."), Toast.LENGTH_LONG).show();
        }
    }

    private void installDownloadedUpdate(long downloadId) {
        try {
            android.app.DownloadManager manager = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            Uri apkUri = manager.getUriForDownloadedFile(downloadId);
            if (apkUri == null) {
                Toast.makeText(this, tr("La descarga aún no está lista.", "The download is not ready yet.", "O download ainda não está pronto."), Toast.LENGTH_SHORT).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong(UPDATE_DOWNLOAD_ID, downloadId).apply();
                new AlertDialog.Builder(this).setTitle(tr("Permitir instalación", "Allow installation", "Permitir instalação"))
                        .setMessage(tr("Activa “Permitir desde esta fuente” para instalar las actualizaciones de Motoroll.",
                                "Enable “Allow from this source” to install Motoroll updates.",
                                "Ative “Permitir desta fonte” para instalar atualizações do Motoroll."))
                        .setPositiveButton(tr("Abrir ajustes", "Open settings", "Abrir configurações"), (d, w) -> {
                            awaitingInstallPermission = true;
                            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
                        }).setNegativeButton(tr("Ahora no", "Not now", "Agora não"), null).show();
                return;
            }
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(UPDATE_DOWNLOAD_ID).apply();
        } catch (Exception error) {
            Log.e(TAG, "Could not open update installer", error);
            Toast.makeText(this, tr("No se pudo abrir el instalador de Android.", "Could not open Android installer.", "Não foi possível abrir o instalador do Android."), Toast.LENGTH_LONG).show();
        }
    }

    private final android.content.BroadcastReceiver updateDownloadReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
            long id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            long expected = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(UPDATE_DOWNLOAD_ID, -2L);
            if (id == expected && id >= 0) {
                Toast.makeText(MainActivity.this, tr("Actualización descargada.", "Update downloaded.", "Atualização baixada."), Toast.LENGTH_SHORT).show();
                installDownloadedUpdate(id);
            }
        }
    };
    private boolean updateReceiverRegistered;

    private final android.content.BroadcastReceiver packageChangeReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            // Refresh the app list only when Android actually reports an install,
            // removal or package update, never on every return from another app.
            mainHandler.post(() -> {
                try {
                    loadApps();
                    if (drawerOpen && drawerSearch != null) {
                        String query = drawerSearch.getText() == null ? "" : drawerSearch.getText().toString();
                        refreshDrawer(query);
                    } else if (homeGrid != null) {
                        homeGrid.setAdapter(new AppAdapter(favoriteApps(), true));
                        rebuildDock();
                    }
                } catch (RuntimeException error) {
                    Log.w(TAG, "Could not refresh apps after package change", error);
                }
            });
        }
    };
    private boolean packageReceiverRegistered;

    private void chooseDrawerSwipeDirection() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int selected = prefs.getInt(PREF_DRAWER_SWIPE_DIRECTION, 0);
        new AlertDialog.Builder(this)
                .setTitle(tr("Gesto para abrir la bóveda", "Swipe to open app drawer", "Gesto para abrir a gaveta"))
                .setSingleChoiceItems(new String[]{
                        tr("Arriba hacia abajo (predeterminado)", "Top to bottom (default)", "De cima para baixo (padrão)"),
                        tr("Abajo hacia arriba", "Bottom to top", "De baixo para cima")
                }, selected, (dialog, which) -> {
                    prefs.edit().putInt(PREF_DRAWER_SWIPE_DIRECTION, which).apply();
                    dialog.dismiss();
                    Toast.makeText(this, tr("Gesto actualizado", "Swipe updated", "Gesto atualizado"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private void chooseDockCount() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int current = prefs.getInt(PREF_DOCK_COUNT, 4);
        int selected = current == 5 ? 1 : current == 6 ? 2 : 0;
        new AlertDialog.Builder(this)
                .setTitle(tr("Cantidad de apps inferiores", "Bottom app count", "Quantidade de apps inferiores"))
                .setSingleChoiceItems(new String[]{"4", "5", "6"}, selected, (dialog, which) -> {
                    prefs.edit().putInt(PREF_DOCK_COUNT, 4 + which).apply();
                    dialog.dismiss();
                    rebuildDock();
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private void chooseDockSlot() {
        int count = Math.max(4, Math.min(6, getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_DOCK_COUNT, 4)));
        String[] slots = new String[count];
        for (int i = 0; i < count; i++) slots[i] = tr("Posición ", "Slot ", "Posição ") + (i + 1);
        new AlertDialog.Builder(this)
                .setTitle(tr("¿Qué posición quieres cambiar?", "Which slot do you want to change?", "Qual posição você quer mudar?"))
                .setItems(slots, (dialog, which) -> showDockPicker(which))
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void showDockPicker(int slot) {
        // One entry per package; apps are already sorted alphabetically by label.
        List<ResolveInfo> choices = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : apps) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) continue;
            if (seen.add(info.activityInfo.packageName)) choices.add(info);
        }
        String[] labels = new String[choices.size()];
        for (int i = 0; i < choices.size(); i++) labels[i] = safeLabel(choices.get(i));
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(tr("Elegir app · posición ", "Choose app · slot ", "Escolher app · posição ") + (slot + 1))
                .setItems(labels, (dialog, which) -> {
                    String packageName = choices.get(which).activityInfo.packageName;
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(PREF_DOCK_PREFIX + slot, packageName).apply();
                    rebuildDock();
                    Toast.makeText(this, tr("Acceso del dock actualizado", "Dock shortcut updated", "Atalho do dock atualizado"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .setNeutralButton(tr("Restaurar", "Reset", "Restaurar"), (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .remove(PREF_DOCK_PREFIX + slot).apply();
                    rebuildDock();
                });
        builder.show();
    }

    private void resetCustomization() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean welcomed = prefs.getBoolean(PREF_WELCOME_SHOWN, false);
        prefs.edit().clear().putBoolean(PREF_WELCOME_SHOWN, welcomed).apply();
        wallpaperBitmap = null;
        weatherStatus = null;
        Toast.makeText(this, tr("Personalización restaurada", "Customization reset", "Personalização restaurada"), Toast.LENGTH_SHORT).show();
        recreate();
    }

    private boolean isExcludedFromFavorites(ResolveInfo info) {
        String p = info.activityInfo.packageName.toLowerCase(Locale.ROOT);
        return p.contains("gamemode") || p.contains("gameassistant") || p.contains("camera5")
                || p.contains("camera");
    }

    private ResolveInfo firstPackage(String packageName) {
        for (ResolveInfo info : apps) {
            if (packageName.equals(info.activityInfo.packageName)) return info;
        }
        return null;
    }

    private ResolveInfo firstPackage(String[] candidates) {
        for (String candidate : candidates) {
            ResolveInfo found = firstPackage(candidate);
            if (found != null) return found;
        }
        return null;
    }

    private void chooseWeatherPosition() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int selected = prefs.getInt(PREF_WEATHER_POSITION, 0);
        int clockPosition = prefs.getInt(PREF_CLOCK_POSITION, 0);
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        labels.add(tr("Debajo del reloj", "Below the clock", "Abaixo do relógio"));
        values.add(0);
        // Hide the edge already occupied by the clock. Keep the stored values
        // (1 = right, 2 = left) so existing preferences remain compatible.
        if (clockPosition != 2) {
            labels.add(tr("A la derecha", "Right side", "À direita"));
            values.add(1);
        }
        if (clockPosition != 0) {
            labels.add(tr("A la izquierda", "Left side", "À esquerda"));
            values.add(2);
        }
        String[] positions = labels.toArray(new String[0]);
        int selectedIndex = values.indexOf(selected);
        if (selectedIndex < 0) selectedIndex = 0;
        new AlertDialog.Builder(this)
                .setTitle(tr("Posición independiente del clima", "Independent weather position", "Posição independente do clima"))
                .setSingleChoiceItems(positions, selectedIndex, (dialog, which) -> {
                    prefs.edit().putInt(PREF_WEATHER_POSITION, values.get(which)).apply();
                    dialog.dismiss();
                    showHome(true);
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private int weatherCornerRadiusDp() {
        int shape = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_WEATHER_SHAPE, 0);
        return shape == 1 ? 8 : shape == 2 ? 19 : 42;
    }

    private void chooseWeatherShape() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String[] shapes = {
                tr("Circular", "Circular", "Circular"),
                tr("Cuadrado", "Square", "Quadrado"),
                tr("Medio cuadrado · esquinas suaves", "Soft square · rounded corners", "Meio quadrado · cantos suaves")
        };
        int selected = prefs.getInt(PREF_WEATHER_SHAPE, 0);
        new AlertDialog.Builder(this)
                .setTitle(tr("Forma del widget del clima", "Weather widget shape", "Formato do widget de clima"))
                .setSingleChoiceItems(shapes, selected, (dialog, which) -> {
                    prefs.edit().putInt(PREF_WEATHER_SHAPE, which).apply();
                    dialog.dismiss();
                    showHome(true);
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null)
                .show();
    }

    private void addSquareWeatherWidget(LinearLayout parent) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        LinearLayout card = column();
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(7), dp(7), dp(7), dp(7));
        GradientDrawable weatherBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF31598C, 0xFF172943, 0xFF121D31});
        int weatherShape = prefs.getInt(PREF_WEATHER_SHAPE, 0);
        weatherBackground.setCornerRadius(dp(weatherCornerRadiusDp()));
        weatherBackground.setStroke(dp(1), 0x704F83C8);
        card.setBackground(weatherBackground);
        card.setElevation(dp(3));

        TextView place = text(prefs.getString(PREF_WEATHER_PLACE, prefs.getString(PREF_WEATHER_CITY, "")), 8, 0xFFDCEBFF, true);
        place.setGravity(Gravity.CENTER);
        place.setMaxLines(1);
        place.setEllipsize(android.text.TextUtils.TruncateAt.END);
        weatherPlaceView = place;
        card.addView(place, lp(-1, -2));

        LinearLayout middle = row();
        middle.setGravity(Gravity.CENTER);
        TextView icon = text(weatherSymbol(prefs.getString(PREF_WEATHER_DESCRIPTION, "")), 20, 0xFFFFE5A8, false);
        weatherIconView = icon;
        middle.addView(icon, lp(25, 29));
        TextView temperature = text(prefs.getString(PREF_WEATHER_TEMP, "--°C"), 20, Color.WHITE, true);
        temperature.setTypeface(android.graphics.Typeface.create("sans-serif-light", 0));
        weatherTemperatureView = temperature;
        middle.addView(temperature, lp(-2, -2));
        card.addView(middle, withTop(lp(-1, -2), 2));

        TextView condition = text(prefs.getString(PREF_WEATHER_DESCRIPTION, tr("Consultando…", "Loading…", "Consultando…")), 8, 0xFFD3E0F4, false);
        condition.setGravity(Gravity.CENTER);
        condition.setMaxLines(1);
        condition.setEllipsize(android.text.TextUtils.TruncateAt.END);
        weatherConditionView = condition;
        card.addView(condition, withTop(lp(-1, -2), 1));

        card.setOnClickListener(v -> chooseWeatherCity(false));
        card.setOnLongClickListener(v -> { chooseWeatherPosition(); return true; });
        card.setContentDescription(tr("Widget del clima. Toca para cambiar ciudad", "Weather widget. Tap to change city", "Widget do clima. Toque para mudar cidade"));
        // Keep the tile's full 84dp footprint inside its holder. The previous
        // extra margins could push its edge/shadow outside the small side container.
        LinearLayout.LayoutParams params = lp(84, 84);
        parent.addView(card, params);
    }

    private void addWeatherCard(LinearLayout content) {
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_WEATHER_STYLE, 0) == 1) {
            addOneUiWeatherCard(content);
            return;
        }
        LinearLayout card = row();
        card.setPadding(dp(15), dp(12), dp(15), dp(12));
        card.setBackground(rounded(0xD9101729, weatherCornerRadiusDp(), 0x80424A70, 1));
        TextView icon = text("◌", 23, CYAN, false);
        card.addView(icon, lp(34, -2));
        weatherStatus = text(weatherSummary(), 11, TEXT, false);
        weatherStatus.setMaxLines(2);
        weatherStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1f);
        infoParams.leftMargin = dp(10);
        card.addView(weatherStatus, infoParams);
        TextView source = text(tr("Datos: Open-Meteo", "Data: Open-Meteo", "Dados: Open-Meteo"), 8, MUTED, false);
        source.setGravity(Gravity.CENTER);
        card.addView(source, lp(-2, 30));
        source.setOnClickListener(v -> openWeatherProviderInfo());
        TextView refresh = text("↻", 20, PURPLE, true);
        card.addView(refresh, lp(25, -2));
        card.setOnClickListener(v -> chooseWeatherCity(false));
        refresh.setOnClickListener(v -> refreshWeatherIfNeeded(true));
        card.setContentDescription(tr("Clima. Toca para cambiar ciudad", "Weather. Tap to change city", "Clima. Toque para mudar cidade"));
        content.addView(card, withTop(lp(-1, -2), 19));
        applyLongPressCustomization(card);
    }

    private void addOneUiWeatherCard(LinearLayout content) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        // Compact blue One UI-inspired card: one-line location and a short horizontal forecast row.
        LinearLayout card = column();
        card.setPadding(dp(13), dp(9), dp(13), dp(9));
        GradientDrawable oneUiBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF385C91, 0xFF1B2B49, 0xFF172238});
        oneUiBackground.setCornerRadius(dp(weatherCornerRadiusDp()));
        card.setBackground(oneUiBackground);
        TextView place = text(prefs.getString(PREF_WEATHER_PLACE, prefs.getString(PREF_WEATHER_CITY, "")), 10, 0xFFE3EEFF, true);
        place.setMaxLines(1);
        place.setEllipsize(android.text.TextUtils.TruncateAt.END);
        weatherPlaceView = place;
        card.addView(place, lp(-1, -2));
        LinearLayout main = row();
        main.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = text(weatherSymbol(prefs.getString(PREF_WEATHER_DESCRIPTION, "")), 30, 0xFFFFE5A8, false);
        weatherIconView = icon;
        main.addView(icon, lp(42, 39));
        TextView temperature = text(prefs.getString(PREF_WEATHER_TEMP, "--°C"), 29, Color.WHITE, false);
        weatherTemperatureView = temperature;
        temperature.setTypeface(android.graphics.Typeface.create("sans-serif-light", 0));
        main.addView(temperature, lp(-2, -2));
        TextView condition = text(prefs.getString(PREF_WEATHER_DESCRIPTION, tr("Consultando clima…", "Loading weather…", "Carregando clima…")), 10, 0xFFD3E0F4, false);
        weatherConditionView = condition;
        condition.setMaxLines(2);
        condition.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams conditionParams = new LinearLayout.LayoutParams(0, -2, 1f);
        conditionParams.leftMargin = dp(9);
        main.addView(condition, conditionParams);
        card.addView(main, withTop(lp(-1, -2), 2));
        card.setOnClickListener(v -> chooseWeatherCity(false));
        card.setOnLongClickListener(v -> { chooseWeatherStyle(); return true; });
        content.addView(card, withTop(lp(-1, -2), 13));
    }

    private String weatherSymbol(String description) {
        String value = description == null ? "" : description.toLowerCase(Locale.ROOT);
        if (value.contains("lluv") || value.contains("rain") || value.contains("chubasc")) return "☂";
        if (value.contains("nieve") || value.contains("snow")) return "❄";
        if (value.contains("niebla") || value.contains("fog")) return "☁";
        if (value.contains("nublado") || value.contains("cloud")) return "☁";
        if (value.contains("torment") || value.contains("thunder")) return "⚡";
        return "☀";
    }

    private void chooseWeatherStyle() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int selected = prefs.getInt(PREF_WEATHER_STYLE, 0);
        new AlertDialog.Builder(this)
                .setTitle(tr("Diseño del widget del clima", "Weather widget design", "Design do widget de clima"))
                .setSingleChoiceItems(new String[]{
                        tr("Motoroll · compacto", "Motoroll · compact", "Motoroll · compacto"),
                        tr("One UI · estilo Samsung", "One UI · Samsung-inspired", "One UI · estilo Samsung")
                }, selected, (dialog, which) -> {
                    prefs.edit().putInt(PREF_WEATHER_STYLE, which).apply();
                    dialog.dismiss();
                    showHome(true);
                })
                .setNegativeButton(tr("Cancelar", "Cancel", "Cancelar"), null).show();
    }

    private void showHome(boolean animate) {
        drawerOpen = false;
        drawerSearch = null;
        drawerGrid = null;
        drawerCount = null;
        drawerContent = null;
        drawerPager = null;
        drawerPages = null;
        drawerPageIndicator = null;
        weatherStatus = null;
        weatherTemperatureView = null;
        weatherPlaceView = null;
        weatherConditionView = null;
        weatherIconView = null;
        homeGrid = null;
        batteryWidget = null;
        batteryWidgetText = null;
        pulseBatteryRing = null;
        pulseGreetingText = null;
        pulseStatusText = null;
        pulseBatteryText = null;

        FrameLayout root = createRoot();
        applyLongPressCustomization(root);
        LinearLayout shell = column();
        applyLongPressCustomization(shell);
        configureInsets(shell, 20, 12, 10);
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));

        shell.addView(buildTopBar(), lp(-1, 48));

        ScrollView scroll = new ScrollView(this);
        applyLongPressCustomization(scroll);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout content = column();
        applyLongPressCustomization(content);
        content.setPadding(0, dp(23), 0, dp(12));
        scroll.addView(content, new FrameLayout.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView label = text(tr("TU ESPACIO", "YOUR SPACE", "SEU ESPAÇO"), 10, CYAN, true);
        label.setLetterSpacing(0.17f);
        label.setGravity(clockGravity());
        applyLongPressCustomization(label);
        content.addView(label, lp(-1, -2));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("h:mm");
        clock.setTextColor(TEXT);
        String homeNote = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_HOME_NOTE, "").trim();
        boolean hasHomeNote = !homeNote.isEmpty();
        int clockStyle = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_CLOCK_STYLE, 0);
        if (clockStyle == 1) {
            clock.setTextSize(hasHomeNote ? 42 : 50);
            clock.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        } else if (clockStyle == 2) {
            clock.setTextSize(hasHomeNote ? 44 : 53);
            clock.setTypeface(android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL));
        } else {
            clock.setTextSize(hasHomeNote ? 48 : 62);
            clock.setTypeface(android.graphics.Typeface.create("sans-serif-light", 0));
        }
        clock.setGravity(clockGravity());
        applyLongPressCustomization(clock);
        boolean weatherBesideClock = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_WEATHER_ENABLED, false)
                && getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_WEATHER_POSITION, 0) != 0;
        if (weatherBesideClock) {
            // Keep the clock aligned against the full screen width.
            // The weather widget is overlaid at the selected edge and never changes clock gravity.
            int weatherPosition = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getInt(PREF_WEATHER_POSITION, 1);
            FrameLayout clockWeatherFrame = new FrameLayout(this);
            clockWeatherFrame.setClipChildren(false);
            clockWeatherFrame.setClipToPadding(false);
            FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                    -1, -1, Gravity.CENTER_VERTICAL);
            clockParams.topMargin = dp(7);
            clock.setGravity(clockGravity() | Gravity.CENTER_VERTICAL);
            clock.setSingleLine(true);
            clockWeatherFrame.addView(clock, clockParams);

            // A 96dp holder gives the 84dp tile room for its rounded corners and
            // elevation shadow, preventing the outer edge from being clipped.
            LinearLayout weatherHolder = column();
            weatherHolder.setGravity(Gravity.CENTER);
            weatherHolder.setClipChildren(false);
            weatherHolder.setClipToPadding(false);
            addSquareWeatherWidget(weatherHolder);
            FrameLayout.LayoutParams weatherParams = new FrameLayout.LayoutParams(
                    dp(96), dp(96), Gravity.CENTER_VERTICAL
                    | (weatherPosition == 2 ? Gravity.START : Gravity.END));
            weatherParams.setMarginStart(dp(5));
            weatherParams.setMarginEnd(dp(5));
            clockWeatherFrame.addView(weatherHolder, weatherParams);
            content.addView(clockWeatherFrame, lp(-1, 104));
        } else {
            content.addView(clock, withTop(lp(-1, -2), 7));
        }
        if (hasHomeNote) {
            SharedPreferences notePrefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            int noteColor = notePrefs.getInt(PREF_HOME_NOTE_COLOR, CYAN);
            int noteSize = notePrefs.getInt(PREF_HOME_NOTE_SIZE, 12);
            TextView note = text(homeNote, noteSize, noteColor, true);
            note.setMaxLines(2);
            note.setEllipsize(android.text.TextUtils.TruncateAt.END);
            note.setGravity(clockGravity() | Gravity.CENTER_VERTICAL);
            note.setContentDescription(tr("Nota personal. Mantén pulsado para editar", "Personal note. Long-press to edit", "Nota pessoal. Pressione para editar"));
            note.setOnLongClickListener(v -> { chooseHomeNote(); return true; });
            content.addView(note, withTop(lp(-1, -2), 3));
        }

        TextClock date = new TextClock(this);
        date.setFormat24Hour("en".equals(getResources().getConfiguration().getLocales().get(0).getLanguage())
                ? "EEEE, MMM d" : "EEEE, d 'de' MMMM");
        date.setFormat12Hour("en".equals(getResources().getConfiguration().getLocales().get(0).getLanguage())
                ? "EEEE, MMM d" : "EEEE, d 'de' MMMM");
        date.setTextColor(MUTED);
        date.setTextSize(13);
        date.setGravity(clockGravity());
        applyLongPressCustomization(date);
        date.setVisibility(getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_DATE_VISIBLE, true) ? View.VISIBLE : View.GONE);
        content.addView(date, withTop(lp(-1, -2), 1));
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_BATTERY_WIDGET, false)) addBatteryWidget(content);
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_WEATHER_ENABLED, false)) {
            if (getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_WEATHER_POSITION, 0) == 0) {
                addWeatherCard(content);
            }
        } else {
            weatherStatus = null;
        }

        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_PULSE_WIDGET, true)) {
            addPulseWidget(content);
        }

        LinearLayout favoritesHeader = row();
        TextView favoritesTitle = text(tr("FAVORITAS", "FAVORITES", "FAVORITOS"), 11, TEXT, true);
        favoritesTitle.setLetterSpacing(0.1f);
        favoritesHeader.addView(favoritesTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView all = text(tr("TODAS  ↓", "ALL APPS  ↓", "TODOS  ↓"), 10, CYAN, true);
        all.setLetterSpacing(0.04f);
        favoritesHeader.addView(all);
        all.setOnClickListener(v -> showDrawer(true, ""));
        applyLongPressCustomization(favoritesHeader);
        content.addView(favoritesHeader, withTop(lp(-1, -2), 29));

        homeGrid = new GridView(this);
        configureGrid(homeGrid, 4);
        homeGrid.setVerticalSpacing(dp(8));
        homeGrid.setAdapter(new AppAdapter(favoriteApps(), true));
        content.addView(homeGrid, withTop(lp(-1, 218), 12));

        View spacer = new View(this);
        content.addView(spacer, lp(-1, 5));

        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_HOME_SEARCH_VISIBLE, false)) {
            shell.addView(buildSearchBar(), lp(-1, 51));
        }
        dock = buildDock();
        shell.addView(dock, withTop(lp(-1, 70), 10));

        setContentView(root);
        configureWindow();
        shell.requestApplyInsets();
        if (animate) {
            shell.setAlpha(0.94f);
            shell.animate().alpha(1f).setDuration(animationDuration()).start();
        }
    }

    private LinearLayout buildTopBar() {
        LinearLayout bar = row();
        MotorollSymbol symbol = new MotorollSymbol(this);
        bar.addView(symbol, lp(37, 37));
        LinearLayout identity = column();
        identity.setGravity(Gravity.CENTER_VERTICAL);
        identity.setPadding(dp(10), 0, 0, 0);
        TextView name = text("MOTOROLL", 14, TEXT, true);
        name.setLetterSpacing(0.12f);
        identity.addView(name);
        bar.addView(identity, new LinearLayout.LayoutParams(0, -2, 1f));
        // Keep the top bar clean. Long-press the home screen to open all customization options.
        applyLongPressCustomization(bar);
        return bar;
    }

    private View buildSearchBar() {
        LinearLayout search = row();
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(15), 0, dp(15), 0);
        search.setBackground(rounded(0xE6101423, 19, 0x90424A70, 1));
        TextView icon = text("⌕", 25, CYAN, false);
        search.addView(icon, lp(29, -2));
        TextView hint = text(tr("Buscar en tus aplicaciones", "Search your apps", "Buscar aplicativos"), 12, MUTED, false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(0, -2, 1f);
        hintParams.leftMargin = dp(8);
        search.addView(hint, hintParams);
        TextView arrow = text("↓", 17, PURPLE, true);
        search.addView(arrow);
        search.setOnClickListener(v -> {
            showDrawer(true, "");
            if (drawerSearch != null) {
                drawerSearch.requestFocus();
                mainHandler.postDelayed(() -> {
                    if (drawerSearch != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) imm.showSoftInput(drawerSearch, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 140);
            }
        });
        applyLongPressCustomization(search);
        return search;
    }

    private LinearLayout buildDock() {
        LinearLayout dockView = row();
        dockView.setGravity(Gravity.CENTER);
        dockView.setPadding(dp(8), dp(7), dp(8), dp(7));
        dockView.setBackground(rounded(0xE6101423, 23, 0x80424A70, 1));
        int dockCount = Math.max(4, Math.min(6, getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_DOCK_COUNT, 4)));
        for (int slot = 0; slot < dockCount; slot++) {
            ResolveInfo info = dockAppForSlot(slot);
            View item = info == null ? createEmptyDockSlot(slot) : createDockIcon(info, slot);
            dockView.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
        }
        return dockView;
    }

    private void rebuildDock() {
        if (dock == null || !(dock.getParent() instanceof LinearLayout)) return;
        LinearLayout parent = (LinearLayout) dock.getParent();
        int index = parent.indexOfChild(dock);
        if (index < 0) return;
        LinearLayout replacement = buildDock();
        parent.removeViewAt(index);
        parent.addView(replacement, index, withTop(lp(-1, 70), 10));
        dock = replacement;
    }

    private View createDockIcon(ResolveInfo info, int slot) {
        FrameLayout holder = new FrameLayout(this);
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bindAppIcon(icon, info);
        int iconDp = selectedDockIconSize();
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(iconDp), dp(iconDp), Gravity.CENTER);
        holder.addView(icon, iconParams);
        holder.setContentDescription(safeLabel(info) + ". Mantén pulsado para cambiar esta app");
        holder.setOnClickListener(v -> launchApp(info));
        holder.setOnLongClickListener(v -> {
            showDockPicker(slot);
            return true;
        });
        return holder;
    }

    private View createEmptyDockSlot(int slot) {
        TextView empty = text("+", 25, CYAN, false);
        empty.setGravity(Gravity.CENTER);
        empty.setBackground(rounded(PANEL_ALT, 16, STROKE, 1));
        empty.setContentDescription("Añadir aplicación al dock, posición " + (slot + 1));
        empty.setOnClickListener(v -> showDockPicker(slot));
        empty.setOnLongClickListener(v -> {
            showDockPicker(slot);
            return true;
        });
        return empty;
    }

    private int selectedDockIconSize() {
        int size = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_ICON_SIZE, 1);
        int count = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_DOCK_COUNT, 4);
        if (count >= 6) return size == 0 ? 23 : size == 2 ? 29 : 26;
        if (count == 5) return size == 0 ? 26 : size == 2 ? 33 : 30;
        if (size == 0) return 29;
        if (size == 2) return 38;
        return 34;
    }

    private void configureGrid(GridView grid, int columns) {
        grid.setNumColumns(columns);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(dp(2));
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setVerticalScrollBarEnabled(false);
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        grid.setSmoothScrollbarEnabled(true);
        grid.setFadingEdgeLength(0);
        grid.setVerticalFadingEdgeEnabled(false);
        grid.setHorizontalFadingEdgeEnabled(false);
        grid.setCacheColorHint(Color.TRANSPARENT);
        grid.setClipToPadding(false);
    }

    private class AppAdapter extends BaseAdapter {
        private final List<ResolveInfo> items;
        private final boolean homeStyle;

        AppAdapter(List<ResolveInfo> source, boolean home) {
            items = new ArrayList<>(source);
            homeStyle = home;
        }

        @Override public int getCount() { return items.size(); }
        @Override public ResolveInfo getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder;
            LinearLayout cell;
            if (convertView == null) {
                cell = column();
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(2), dp(4), dp(2), dp(4));
                cell.setMinimumHeight(dp(homeStyle ? 99 : 91));

                FrameLayout iconFrame = new FrameLayout(MainActivity.this);
                iconFrame.setBackground(iconFrameBackground());
                iconFrame.setClipToOutline(true);
                ImageView image = new ImageView(MainActivity.this);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int sizeChoice = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_ICON_SIZE, 1);
                int iconDp = homeStyle ? (sizeChoice == 0 ? 37 : sizeChoice == 2 ? 48 : 43)
                        : (sizeChoice == 0 ? 33 : sizeChoice == 2 ? 43 : 38);
                int frameDp = iconDp + (homeStyle ? 18 : 18);
                image.setTag("motoroll_icon");
                iconFrame.addView(image, new FrameLayout.LayoutParams(dp(iconDp),
                        dp(iconDp), Gravity.CENTER));
                cell.addView(iconFrame, lp(frameDp, homeStyle ? 57 : 53));

                TextView label = text("", homeStyle ? 10 : 10, TEXT, false);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(1);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                cell.addView(label, withTop(lp(-1, -2), 7));
                holder = new AppViewHolder(image, label);
                cell.setTag(holder);
            } else {
                cell = (LinearLayout) convertView;
                holder = (AppViewHolder) cell.getTag();
            }

            ResolveInfo item = getItem(position);
            holder.label.setText(compactLabel(item));
            bindAppIcon(holder.icon, item);
            cell.setContentDescription(safeLabel(item) + (homeStyle
                    ? tr(". Mantén pulsado para editar favorita", ". Long-press to edit favorite", ". Mantenha pressionado para editar favorito") : ""));
            cell.setOnClickListener(v -> launchApp(item));
            if (homeStyle) {
                cell.setOnLongClickListener(v -> {
                    manageFavoriteAt(item);
                    return true;
                });
            } else {
                cell.setOnLongClickListener(null);
            }
            return cell;
        }
    }

    private static final class AppViewHolder {
        final ImageView icon;
        final TextView label;
        AppViewHolder(ImageView icon, TextView label) { this.icon = icon; this.label = label; }
    }

    private void showDrawer(boolean animate, String initialQuery) {
        drawerOpen = true;
        homeGrid = null;
        drawerSearch = null;
        drawerGrid = null;
        drawerPager = null;
        drawerPages = null;
        drawerPageIndicator = null;
        drawerContent = null;
        batteryWidget = null;
        batteryWidgetText = null;
        pulseBatteryRing = null;
        pulseGreetingText = null;
        pulseStatusText = null;
        pulseBatteryText = null;

        FrameLayout root = createRoot();
        LinearLayout shell = column();
        configureInsets(shell, 10, 7, 8);
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout panel = column();
        panel.setPadding(dp(17), dp(17), dp(17), dp(12));
        panel.setBackground(rounded(0xF40A0D19, 30, 0xA03D456B, 1));
        shell.addView(panel, new LinearLayout.LayoutParams(-1, -1));

        LinearLayout header = row();
        LinearLayout titles = column();
        TextView title = text(tr("APLICACIONES", "APPS", "APLICATIVOS"), 20, TEXT, true);
        title.setLetterSpacing(0.045f);
        titles.addView(title);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView close = text("×", 29, MUTED, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(rounded(PANEL_ALT, 18, STROKE, 1));
        header.addView(close, lp(42, 42));
        close.setOnClickListener(v -> showHome(true));
        panel.addView(header);

        drawerSearch = new EditText(this);
        drawerSearch.setSingleLine(true);
        drawerSearch.setTextSize(14);
        drawerSearch.setTextColor(TEXT);
        drawerSearch.setHintTextColor(MUTED);
        drawerSearch.setHint(tr("Buscar una aplicación", "Search an app", "Buscar um aplicativo"));
        drawerSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        drawerSearch.setPadding(dp(16), 0, dp(16), 0);
        drawerSearch.setBackground(rounded(PANEL_ALT, 18, STROKE, 1));
        panel.addView(drawerSearch, withTop(lp(-1, 52), 17));

        drawerCount = text("", 10, MUTED, false);
        panel.addView(drawerCount, withTop(lp(-1, 18), 14));

        drawerContent = column();
        panel.addView(drawerContent, withTop(new LinearLayout.LayoutParams(-1, 0, 1f), 7));

        TextView footer = text(tr("Pulsa × o Atrás para volver", "Tap × or Back to return", "Toque em × ou Voltar para retornar"), 10, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        panel.addView(footer, withTop(lp(-1, 22), 5));

        setContentView(root);
        configureWindow();
        shell.requestApplyInsets();
        drawerSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshDrawer(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        TextView loading = text(tr("Cargando aplicaciones…", "Loading apps…", "Carregando aplicativos…"), 12, MUTED, false);
        loading.setGravity(Gravity.CENTER);
        drawerContent.addView(loading, new LinearLayout.LayoutParams(-1, -1));

        if (animate) {
            panel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            panel.setAlpha(0.97f);
            panel.setScaleX(0.985f);
            panel.setScaleY(0.985f);
            panel.setTranslationY(-dp(18));
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .setDuration(drawerAnimationDuration())
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .withEndAction(() -> panel.setLayerType(View.LAYER_TYPE_NONE, null)).start();
        }
        root.postDelayed(() -> {
            if (!drawerOpen || drawerContent == null) return;
            refreshDrawer(initialQuery == null ? "" : initialQuery);
        }, 24L);
    }

    private void refreshDrawer(String query) {
        if (drawerContent == null) return;
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<ResolveInfo> filtered = new ArrayList<>();
        Set<String> hidden = hiddenPackages();
        for (ResolveInfo app : apps) {
            String label = safeLabel(app);
            if (!hidden.contains(app.activityInfo.packageName)
                    && (needle.isEmpty() || label.toLowerCase(Locale.ROOT).contains(needle))) filtered.add(app);
        }
        if (drawerCount != null) drawerCount.setText(filtered.size() + " "
                + tr("aplicaciones", "apps", "aplicativos"));

        drawerContent.removeAllViews();
        drawerGrid = null;
        drawerPager = null;
        drawerPages = null;
        drawerPageIndicator = null;

        int mode = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_DRAWER_MODE, 0);
        if (mode == 1) {
            buildHorizontalDrawerPages(filtered);
        } else {
            drawerGrid = new GridView(this);
            configureGrid(drawerGrid, 4);
            drawerGrid.setVerticalSpacing(dp(6));
            drawerGrid.setAdapter(new AppAdapter(filtered, false));
            drawerContent.addView(drawerGrid, new LinearLayout.LayoutParams(-1, -1));
        }
    }

    private void buildHorizontalDrawerPages(List<ResolveInfo> filtered) {
        int pageCount = Math.max(1, (filtered.size() + HORIZONTAL_PAGE_SIZE - 1) / HORIZONTAL_PAGE_SIZE);
        drawerPageWidth = Math.max(dp(260), getResources().getDisplayMetrics().widthPixels - dp(54));
        drawerPager = new SnapHorizontalScrollView(this);
        drawerPager.setPageMetrics(drawerPageWidth, pageCount);
        drawerPager.setFillViewport(true);
        drawerPager.setHorizontalScrollBarEnabled(false);
        drawerPager.setVerticalScrollBarEnabled(false);
        drawerPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        drawerPager.setSmoothScrollingEnabled(true);
        drawerPages = new LinearLayout(this);
        drawerPages.setOrientation(LinearLayout.HORIZONTAL);
        drawerPager.addView(drawerPages, new HorizontalScrollView.LayoutParams(-2, -1));

        for (int page = 0; page < pageCount; page++) {
            int from = page * HORIZONTAL_PAGE_SIZE;
            int to = Math.min(filtered.size(), from + HORIZONTAL_PAGE_SIZE);
            List<ResolveInfo> pageApps = new ArrayList<>(filtered.subList(from, to));
            GridView pageGrid = new GridView(this);
            configureGrid(pageGrid, 4);
            pageGrid.setVerticalSpacing(dp(6));
            pageGrid.setAdapter(new AppAdapter(pageApps, false));
            drawerPages.addView(pageGrid, new LinearLayout.LayoutParams(drawerPageWidth, -1));
        }

        drawerPageIndicator = text("1 / " + pageCount + "  ·  "
                + tr("Desliza ← →", "Swipe ← →", "Deslize ← →"), 10, MUTED, false);
        drawerPageIndicator.setGravity(Gravity.CENTER);
        drawerContent.addView(drawerPager, new LinearLayout.LayoutParams(-1, 0, 1f));
        drawerContent.addView(drawerPageIndicator, lp(-1, 25));
        final int pages = pageCount;
        drawerPager.setOnScrollChangeListener((View v, int scrollX, int scrollY, int oldX, int oldY) -> {
            if (drawerPageIndicator != null && drawerPageWidth > 0) {
                int active = Math.min(pages, Math.max(1, Math.round((float) scrollX / drawerPageWidth) + 1));
                drawerPageIndicator.setText(active + " / " + pages + "  ·  "
                        + tr("Desliza ← →", "Swipe ← →", "Deslize ← →"));
            }
        });
        drawerPager.post(() -> {
            if (drawerPager == null || drawerPages == null) return;
            int actualWidth = drawerPager.getWidth();
            if (actualWidth <= 0) return;
            drawerPageWidth = actualWidth;
            drawerPager.setPageMetrics(actualWidth, pages);
            for (int i = 0; i < drawerPages.getChildCount(); i++) {
                View page = drawerPages.getChildAt(i);
                ViewGroup.LayoutParams params = page.getLayoutParams();
                params.width = actualWidth;
                page.setLayoutParams(params);
            }
            drawerPages.requestLayout();
            drawerPager.requestLayout();
        });

    }

    /**
     * One-page-per-gesture horizontal drawer. The platform fling is disabled so a quick
     * swipe cannot skip several pages; a small drag still settles to the nearest page.
     */
    private final class SnapHorizontalScrollView extends HorizontalScrollView {
        private int pageWidth;
        private int pageCount = 1;
        private float downTouchX;
        private float downTouchY;
        private int startScrollX;
        private VelocityTracker velocityTracker;
        private boolean draggingHorizontally;
        private final int touchSlop = ViewConfiguration.get(MainActivity.this).getScaledTouchSlop();
        private final int minimumFlingVelocity = ViewConfiguration.get(MainActivity.this).getScaledMinimumFlingVelocity();

        SnapHorizontalScrollView(Context context) {
            super(context);
            setClickable(true);
        }

        void setPageMetrics(int width, int count) {
            pageWidth = Math.max(1, width);
            pageCount = Math.max(1, count);
        }

        private int effectivePageWidth() {
            return pageWidth > 0 ? pageWidth : Math.max(1, getWidth());
        }

        @Override public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            // Child GridViews request disallow-intercept for clicks; keep the parent
            // pager eligible to take over once a clearly horizontal drag begins.
            super.requestDisallowInterceptTouchEvent(false);
        }

        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downTouchX = event.getX();
                downTouchY = event.getY();
                startScrollX = getScrollX();
                draggingHorizontally = false;
                recycleVelocityTracker();
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getX() - downTouchX;
                float dy = event.getY() - downTouchY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.05f) {
                    draggingHorizontally = true;
                    return true;
                }
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                draggingHorizontally = false;
            }
            return false;
        }

        @Override public void fling(int velocityX) {
            // The gesture is settled manually to exactly one adjacent page.
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downTouchX = event.getX();
                downTouchY = event.getY();
                startScrollX = getScrollX();
                draggingHorizontally = false;
                if (velocityTracker != null) velocityTracker.recycle();
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
            } else if (velocityTracker != null) {
                velocityTracker.addMovement(event);
            }

            boolean handled = super.onTouchEvent(event);

            if (action == MotionEvent.ACTION_UP) {
                float velocityX = 0f;
                if (velocityTracker != null) {
                    velocityTracker.computeCurrentVelocity(1000);
                    velocityX = velocityTracker.getXVelocity();
                    velocityTracker.recycle();
                    velocityTracker = null;
                }

                float distance = event.getX() - downTouchX;
                int width = effectivePageWidth();
                int startPage = Math.round((float) startScrollX / width);
                int targetPage;
                boolean clearFling = Math.abs(velocityX) >= minimumFlingVelocity * 1.35f
                        && Math.abs(distance) >= dp(18);
                if (clearFling) {
                    targetPage = startPage + (velocityX < 0f ? 1 : -1);
                } else if (Math.abs(distance) >= width * 0.18f) {
                    targetPage = startPage + (distance < 0f ? 1 : -1);
                } else {
                    targetPage = Math.round((float) getScrollX() / width);
                }

                targetPage = Math.max(0, Math.min(pageCount - 1, targetPage));
                smoothScrollTo(targetPage * width, 0);
                draggingHorizontally = false;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                recycleVelocityTracker();
                int width = effectivePageWidth();
                int nearestPage = Math.max(0, Math.min(pageCount - 1,
                        Math.round((float) getScrollX() / width)));
                smoothScrollTo(nearestPage * width, 0);
                draggingHorizontally = false;
            }
            return handled;
        }

        private void recycleVelocityTracker() {
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }
        }

        @Override protected void onDetachedFromWindow() {
            recycleVelocityTracker();
            super.onDetachedFromWindow();
        }
    }

    /**
     * Detects the drawer gesture before GridView or other home widgets consume it.
     * This makes both configured directions reliable and prevents accidental clicks.
     */
    private final class SwipeRootLayout extends FrameLayout {
        private float startX;
        private float startY;
        private boolean openingGesture;
        private int activePointerId = MotionEvent.INVALID_POINTER_ID;
        private final int slop = ViewConfiguration.get(MainActivity.this).getScaledTouchSlop();

        SwipeRootLayout(Context context) {
            super(context);
            setClickable(true);
        }

        private float triggerDistance() {
            return Math.max(dp(12), slop * 0.85f);
        }

        // Android ScrollView and GridView normally disallow parent interception as
        // soon as scrolling starts. Keep this root listening so a deliberate
        // launcher swipe can win, while ordinary/opposite-direction scrolling passes through.
        @Override
        public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            // Intentionally do not propagate the disallow flag to FrameLayout.
        }

        private boolean isOpeningDirection(float dx, float dy) {
            int direction = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getInt(PREF_DRAWER_SWIPE_DIRECTION, 0);
            boolean correctDirection = direction == 0 ? dy > 0f : dy < 0f;
            return correctDirection
                    && Math.abs(dy) >= triggerDistance()
                    && Math.abs(dy) > Math.abs(dx) * 1.22f;
        }

        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            if (drawerOpen) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                startX = event.getX();
                startY = event.getY();
                activePointerId = event.getPointerId(0);
                openingGesture = false;
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                int index = event.findPointerIndex(activePointerId);
                if (index < 0) return false;
                float dx = event.getX(index) - startX;
                float dy = event.getY(index) - startY;
                if (isOpeningDirection(dx, dy)) {
                    openingGesture = true;
                    return true;
                }
            }
            if (action == MotionEvent.ACTION_POINTER_UP
                    && event.getPointerId(event.getActionIndex()) == activePointerId) {
                int replacement = event.getActionIndex() == 0 ? 1 : 0;
                if (replacement < event.getPointerCount()) {
                    startX = event.getX(replacement);
                    startY = event.getY(replacement);
                    activePointerId = event.getPointerId(replacement);
                } else {
                    openingGesture = false;
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                openingGesture = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
            }
            return false;
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP) {
                int index = activePointerId == MotionEvent.INVALID_POINTER_ID
                        ? event.getActionIndex() : event.findPointerIndex(activePointerId);
                if (index < 0) index = event.getActionIndex();
                float dx = event.getX(index) - startX;
                float dy = event.getY(index) - startY;
                boolean open = openingGesture && isOpeningDirection(dx, dy);
                openingGesture = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                if (open && !drawerOpen) showDrawer(true, "");
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                openingGesture = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                return true;
            }
            return true;
        }
    }

    private void launchApp(ResolveInfo info) {
        try {
            if (info == null || info.activityInfo == null || info.activityInfo.name == null) return;
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception error) {
            Log.e(TAG, "Could not launch application", error);
            Toast.makeText(this, tr("No se pudo abrir esa aplicación", "Could not open this app", "Não foi possível abrir este aplicativo"), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // SwipeRootLayout intercepts the configured vertical gesture before child views.
        // Keep Activity dispatch untouched so clicks and horizontal paging remain independent.
        return super.dispatchTouchEvent(event);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (drawerOpen) showHome(true);
        else super.onBackPressed();
    }

    private void showStartupError(RuntimeException error) {
        LinearLayout screen = column();
        screen.setGravity(Gravity.CENTER);
        screen.setPadding(dp(28), dp(28), dp(28), dp(28));
        screen.setBackgroundColor(BG);
        TextView title = text(tr("Motoroll Home no pudo iniciarse", "Motoroll Home could not start", "Motoroll Home não pôde iniciar"), 20, TEXT, true);
        TextView detail = text(error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage()),
                12, MUTED, false);
        detail.setPadding(0, dp(14), 0, dp(20));
        screen.addView(title);
        screen.addView(detail);
        TextView retry = text(tr("Volver a intentar", "Try again", "Tentar novamente"), 13, CYAN, true);
        retry.setGravity(Gravity.CENTER);
        retry.setBackground(rounded(PANEL_ALT, 16, STROKE, 1));
        screen.addView(retry, lp(210, 48));
        retry.setOnClickListener(v -> {
            try { loadApps(); showHome(true); }
            catch (RuntimeException again) { Log.e(TAG, "Retry failed", again); }
        });
        setContentView(screen);
        configureWindow();
    }

    private class AmbientBackground extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private RadialGradient blueGlow;
        private RadialGradient purpleGlow;

        AmbientBackground(Context context) { super(context); }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            float main = Math.max(width, height) * 0.54f;
            blueGlow = new RadialGradient(width * 0.92f, height * 0.18f, main,
                    new int[]{0x30335FA8, 0x15142143, 0x0005070D},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
            purpleGlow = new RadialGradient(width * 0.04f, height * 0.78f, main * 0.9f,
                    new int[]{0x2C57318D, 0x141A1038, 0x0005070D},
                    new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP);
        }

        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(BG);
            if (blueGlow != null) {
                paint.setShader(blueGlow);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
            if (purpleGlow != null) {
                paint.setShader(purpleGlow);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
            paint.setShader(null);
        }
    }

    private class PulseBatteryRing extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int batteryLevel = -1;

        PulseBatteryRing(Context context) {
            super(context);
        }

        void setBatteryLevel(int level) {
            batteryLevel = level;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float stroke = dp(4);
            float radius = (Math.min(getWidth(), getHeight()) - stroke * 2f) / 2f;
            if (radius <= 0f) return;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            android.graphics.RectF circle = new android.graphics.RectF(
                    cx - radius, cy - radius, cx + radius, cy + radius);

            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeWidth(stroke);
            trackPaint.setStrokeCap(Paint.Cap.ROUND);
            trackPaint.setColor(0x665C78A8);
            trackPaint.setShader(null);
            canvas.drawArc(circle, -90f, 360f, false, trackPaint);

            if (batteryLevel >= 0) {
                progressPaint.setStyle(Paint.Style.STROKE);
                progressPaint.setStrokeWidth(stroke);
                progressPaint.setStrokeCap(Paint.Cap.ROUND);
                progressPaint.setColor(batteryLevel <= 15 ? 0xFFFFB36B : BLUE);
                progressPaint.setShader(null);
                canvas.drawArc(circle, -90f, 360f * Math.max(0, Math.min(100, batteryLevel)) / 100f,
                        false, progressPaint);
            }
        }
    }

    private class MotorollSymbol extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        MotorollSymbol(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(getWidth(), getHeight()) * 0.43f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.5f));
            paint.setShader(new android.graphics.LinearGradient(cx-r, cy-r, cx+r, cy+r,
                    new int[]{CYAN, BLUE, PURPLE}, null, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, r, paint);
            paint.setShader(null);
            paint.setStrokeWidth(dp(2.5f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setShader(new android.graphics.LinearGradient(cx-r, cy, cx+r, cy,
                    CYAN, PURPLE, Shader.TileMode.CLAMP));
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(cx-r*0.57f, cy-r*0.20f);
            path.lineTo(cx-r*0.27f, cy+r*0.29f);
            path.lineTo(cx, cy-r*0.02f);
            path.lineTo(cx+r*0.27f, cy+r*0.29f);
            path.lineTo(cx+r*0.57f, cy-r*0.20f);
            canvas.drawPath(path, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
        }
    }
}
