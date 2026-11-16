package com.motoroll.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
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
import android.view.MotionEvent;
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Motoroll Home 3.2.1 - smoother app drawer and clearer first-run guidance.
 * Native views only: optional on-demand weather, no analytics, no background service, and no third-party UI library.
 * App drawer opens with a downward swipe. Drawer scrolling is handled by its own container;
 * it closes only with the close button or Android Back.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MotorollHome";
    private static final int REQUEST_WALLPAPER = 3107;
    private static final String PREFS = "motoroll_home_prefs";
    private static final String PREF_WALLPAPER_URI = "launcher_wallpaper_uri";
    private static final String PREF_DOCK_PREFIX = "dock_slot_";
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
    private HorizontalScrollView drawerPager;
    private LinearLayout drawerPages;
    private TextView drawerPageIndicator;
    private int drawerPageWidth;
    private TextView weatherStatus;
    private volatile boolean weatherRefreshInFlight;
    private LinearLayout dock;
    private boolean drawerOpen = false;
    private float downX;
    private float downY;
    private boolean trackingGesture;
    private Bitmap wallpaperBitmap;

    // App icons are loaded off the UI thread so scrolling does not wait for PackageManager.
    // ConstantState keeps the cache compact and lets each ImageView receive its own Drawable.
    private final Object appIconLock = new Object();
    private final LruCache<String, Drawable.ConstantState> appIconCache = new LruCache<>(128);
    private final Set<String> appIconsLoading = new HashSet<>();
    private final Map<String, List<WeakReference<ImageView>>> pendingIconViews = new HashMap<>();
    private final Map<String, String> appLabelCache = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
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
        synchronized (appIconLock) {
            pendingIconViews.clear();
            appIconsLoading.clear();
        }
        super.onDestroy();
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
            loadApps();
            boolean welcomed = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getBoolean(PREF_WELCOME_SHOWN, false);
            if (welcomed) showHome(false);
            else showWelcome();
        } catch (RuntimeException error) {
            Log.e(TAG, "Launcher failed during startup", error);
            showStartupError(error);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            loadApps();
            if (drawerOpen && drawerSearch != null) {
                String q = drawerSearch.getText() == null ? "" : drawerSearch.getText().toString();
                refreshDrawer(q);
            } else if (homeGrid != null) {
                homeGrid.setAdapter(new AppAdapter(favoriteApps(), true));
                rebuildDock();
            }
            if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_WEATHER_ENABLED, false)) {
                refreshWeatherIfNeeded(false);
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not refresh launcher apps", error);
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
                tr("Fondos, reloj, iconos y colores personalizables", "Custom wallpapers, clock, icons and colors", "Papéis de parede, relógio, ícones e cores personalizáveis"),
                tr("Clima opcional, apagado de forma predeterminada", "Optional weather, off by default", "Clima opcional, desativado por padrão"),
                tr("Idioma automático o selección manual", "Automatic or manual language", "Idioma automático ou seleção manual"),
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
        FrameLayout root = new FrameLayout(this);
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

        root.addView(new AmbientBackground(this), new FrameLayout.LayoutParams(-1, -1));
        return root;
    }

    private Uri getSavedWallpaperUri() {
        String value = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_WALLPAPER_URI, null);
        if (value == null || value.trim().isEmpty()) return null;
        try { return Uri.parse(value); }
        catch (RuntimeException ignored) { return null; }
    }

    private Bitmap decodeWallpaper(Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int targetW = Math.max(720, getResources().getDisplayMetrics().widthPixels);
            int targetH = Math.max(1400, getResources().getDisplayMetrics().heightPixels);
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
        if (requestCode != REQUEST_WALLPAPER || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // Some document providers do not offer persistable grants. The current grant may still work.
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_WALLPAPER_URI, uri.toString()).apply();
        wallpaperBitmap = null;
        showHome(false);
        Toast.makeText(this, tr("Fondo de Motoroll actualizado", "Motoroll wallpaper updated", "Papel de parede do Motoroll atualizado"), Toast.LENGTH_SHORT).show();
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
        for (String preferred : HOME_FAVORITES) {
            ResolveInfo match = firstPackage(preferred);
            if (match != null && !isExcludedFromFavorites(match)
                    && used.add(match.activityInfo.packageName)) result.add(match);
        }
        for (ResolveInfo info : apps) {
            if (result.size() >= 8) break;
            if (!isExcludedFromFavorites(info) && used.add(info.activityInfo.packageName)) result.add(info);
        }
        return result;
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
        if (selected != null) return selected;
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
        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean weatherEnabled = prefs.getBoolean(PREF_WEATHER_ENABLED, false);
        int drawerMode = prefs.getInt(PREF_DRAWER_MODE, 0);
        String city = prefs.getString(PREF_WEATHER_CITY, "");

        keys.add("wallpaper"); labels.add(tr("Cambiar fondo de pantalla", "Change wallpaper", "Mudar papel de parede"));
        keys.add("drawer"); labels.add(drawerMode == 1
                ? tr("Bóveda: páginas horizontales ✓", "App drawer: horizontal pages ✓", "Gaveta: páginas horizontais ✓")
                : tr("Bóveda: desplazamiento vertical ✓", "App drawer: vertical scrolling ✓", "Gaveta: rolagem vertical ✓"));
        keys.add("clock_position"); labels.add(tr("Posición del reloj", "Clock position", "Posição do relógio"));
        keys.add("clock_style"); labels.add(tr("Diseño del reloj", "Clock style", "Estilo do relógio"));
        keys.add("dock"); labels.add(tr("Personalizar las 4 apps inferiores", "Customize the 4 dock apps", "Personalizar os 4 apps do dock"));
        keys.add("icons"); labels.add(tr("Tamaño de iconos", "Icon size", "Tamanho dos ícones"));
        keys.add("weather_toggle"); labels.add(weatherEnabled
                ? tr("Clima: activado", "Weather: on", "Clima: ativado")
                : tr("Clima: desactivado", "Weather: off", "Clima: desativado"));
        if (weatherEnabled) {
            keys.add("weather_city"); labels.add(tr("Configurar ciudad del clima · " + city,
                    "Set weather city · " + city, "Configurar cidade do clima · " + city));
        }
        keys.add("language"); labels.add(tr("Idioma", "Language", "Idioma"));
        keys.add("default_home"); labels.add(tr("Establecer Motoroll como launcher predeterminado",
                "Set Motoroll as default launcher", "Definir Motoroll como launcher padrão"));
        keys.add("reset"); labels.add(tr("Restablecer personalización", "Reset customization", "Redefinir personalização"));

        String[] options = labels.toArray(new String[0]);
        String title = tr("Personalizar Motoroll", "Customize Motoroll", "Personalizar Motoroll");
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    String key = keys.get(which);
                    switch (key) {
                        case "wallpaper": chooseWallpaper(); break;
                        case "drawer": chooseDrawerMode(); break;
                        case "clock_position": chooseClockPosition(); break;
                        case "clock_style": chooseClockStyle(); break;
                        case "dock": chooseDockSlot(); break;
                        case "icons": chooseIconSize(); break;
                        case "weather_toggle": toggleWeather(); break;
                        case "weather_city": chooseWeatherCity(false); break;
                        case "language": chooseLanguage(); break;
                        case "default_home": openHomeAppSettings(); break;
                        case "reset": resetCustomization(); break;
                    }
                })
                .setNegativeButton(tr("Cerrar", "Close", "Fechar"), null)
                .show();
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
                    if (weatherStatus != null) weatherStatus.setText(weatherSummary());
                });
            } catch (Exception error) {
                Log.w(TAG, "Weather refresh failed", error);
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString(PREF_WEATHER_DESCRIPTION, tr("Clima no disponible", "Weather unavailable", "Clima indisponível"))
                        .putLong(PREF_WEATHER_FETCHED, System.currentTimeMillis())
                        .apply();
                runOnUiThread(() -> {
                    weatherRefreshInFlight = false;
                    if (weatherStatus != null) weatherStatus.setText(weatherSummary());
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
        String[] choices = {tr("Izquierda", "Left", "Esquerda"), tr("Centro", "Center", "Centro"), tr("Derecha", "Right", "Direita")};
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_CLOCK_POSITION, 0);
        new AlertDialog.Builder(this)
                .setTitle(tr("Posición del reloj", "Clock position", "Posição do relógio"))
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt(PREF_CLOCK_POSITION, which).apply();
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

    private void chooseDockSlot() {
        String[] slots = {tr("Primera app", "First app", "Primeiro app"), tr("Segunda app", "Second app", "Segundo app"), tr("Tercera app", "Third app", "Terceiro app"), tr("Cuarta app", "Fourth app", "Quarto app")};
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

    private void addWeatherCard(LinearLayout content) {
        LinearLayout card = row();
        card.setPadding(dp(15), dp(12), dp(15), dp(12));
        card.setBackground(rounded(0xD9101729, 19, 0x80424A70, 1));
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
        homeGrid = null;

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
        int clockStyle = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_CLOCK_STYLE, 0);
        if (clockStyle == 1) {
            clock.setTextSize(50);
            clock.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        } else if (clockStyle == 2) {
            clock.setTextSize(53);
            clock.setTypeface(android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL));
        } else {
            clock.setTextSize(62);
            clock.setTypeface(android.graphics.Typeface.create("sans-serif-light", 0));
        }
        clock.setGravity(clockGravity());
        applyLongPressCustomization(clock);
        content.addView(clock, withTop(lp(-1, -2), 7));

        TextClock date = new TextClock(this);
        date.setFormat24Hour("en".equals(getResources().getConfiguration().getLocales().get(0).getLanguage())
                ? "EEEE, MMM d" : "EEEE, d 'de' MMMM");
        date.setFormat12Hour("en".equals(getResources().getConfiguration().getLocales().get(0).getLanguage())
                ? "EEEE, MMM d" : "EEEE, d 'de' MMMM");
        date.setTextColor(MUTED);
        date.setTextSize(13);
        date.setGravity(clockGravity());
        applyLongPressCustomization(date);
        content.addView(date, withTop(lp(-1, -2), 1));
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_WEATHER_ENABLED, false)) {
            addWeatherCard(content);
        } else {
            weatherStatus = null;
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

        shell.addView(buildSearchBar(), lp(-1, 51));
        dock = buildDock();
        shell.addView(dock, withTop(lp(-1, 70), 10));

        setContentView(root);
        configureWindow();
        shell.requestApplyInsets();
        if (animate) {
            shell.setAlpha(0.94f);
            shell.animate().alpha(1f).setDuration(150).start();
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
        search.setOnClickListener(v -> showDrawer(true, ""));
        applyLongPressCustomization(search);
        return search;
    }

    private LinearLayout buildDock() {
        LinearLayout dockView = row();
        dockView.setGravity(Gravity.CENTER);
        dockView.setPadding(dp(8), dp(7), dp(8), dp(7));
        dockView.setBackground(rounded(0xE6101423, 23, 0x80424A70, 1));
        for (int slot = 0; slot < 4; slot++) {
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
        try { icon.setImageDrawable(info.loadIcon(getPackageManager())); }
        catch (RuntimeException ignored) { icon.setImageResource(android.R.drawable.sym_def_app_icon); }
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
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
                iconFrame.setBackground(rounded(0xC51A1D32, homeStyle ? 19 : 17, 0x70434D79, 1));
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
            cell.setContentDescription(safeLabel(item));
            cell.setOnClickListener(v -> launchApp(item));
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
        refreshDrawer(initialQuery == null ? "" : initialQuery);
        drawerSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshDrawer(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        if (animate) {
            panel.setAlpha(0.94f);
            panel.setTranslationY(-dp(48));
            panel.animate().alpha(1f).translationY(0f).setDuration(180).start();
        }
    }

    private void refreshDrawer(String query) {
        if (drawerContent == null) return;
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<ResolveInfo> filtered = new ArrayList<>();
        for (ResolveInfo app : apps) {
            String label = safeLabel(app);
            if (needle.isEmpty() || label.toLowerCase(Locale.ROOT).contains(needle)) filtered.add(app);
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
        drawerPager = new HorizontalScrollView(this);
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
        drawerPager.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                drawerPager.postDelayed(() -> {
                    if (drawerPager == null || drawerPageWidth <= 0) return;
                    int target = Math.round((float) drawerPager.getScrollX() / drawerPageWidth);
                    target = Math.max(0, Math.min(pages - 1, target));
                    drawerPager.smoothScrollTo(target * drawerPageWidth, 0);
                }, 110);
            }
            return false;
        });
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
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            trackingGesture = true;
        } else if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                && trackingGesture) {
            if (action == MotionEvent.ACTION_CANCEL) {
                trackingGesture = false;
                return super.dispatchTouchEvent(event);
            }
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            trackingGesture = false;
            boolean mostlyVertical = Math.abs(dx) < Math.max(dp(100), Math.abs(dy) * 0.72f);
            if (!drawerOpen && mostlyVertical && dy > dp(82)) {
                // Pull down on the home screen to open the app drawer.
                showDrawer(true, "");
                return true;
            }
            // While the drawer is open, all grid and horizontal-page gestures are handled
            // by their own scroll containers; the × button and Android Back close it.
        }
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
