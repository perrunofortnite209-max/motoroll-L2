package com.motoroll.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Motoroll Home 3.1.0 - lightweight, customizable HOME launcher.
 * Native views only: no network, analytics, background service, or third-party UI library.
 * App drawer opens with a downward swipe. Drawer scrolling is never treated as a close gesture; swipe up from its header to close.
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
    private LinearLayout dock;
    private boolean drawerOpen = false;
    private float downX;
    private float downY;
    private boolean trackingGesture;
    private Bitmap wallpaperBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            loadApps();
            showHome(false);
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
            Toast.makeText(this, "No se pudo abrir el selector de fondos", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Fondo de Motoroll actualizado", Toast.LENGTH_SHORT).show();
    }

    private void loadApps() {
        apps.clear();
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

    private String safeLabel(ResolveInfo info) {
        try {
            CharSequence label = info.loadLabel(getPackageManager());
            return label == null ? info.activityInfo.packageName : label.toString();
        } catch (RuntimeException error) {
            return info.activityInfo == null ? "Aplicación" : info.activityInfo.packageName;
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
        final String[] options = {
                "Cambiar fondo de pantalla",
                "Posición del reloj",
                "Diseño del reloj",
                "Personalizar las 4 apps inferiores",
                "Tamaño de iconos",
                "Restablecer personalización"
        };
        new AlertDialog.Builder(this)
                .setTitle("Personalizar Motoroll")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: chooseWallpaper(); break;
                        case 1: chooseClockPosition(); break;
                        case 2: chooseClockStyle(); break;
                        case 3: chooseDockSlot(); break;
                        case 4: chooseIconSize(); break;
                        case 5: resetCustomization(); break;
                    }
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void chooseClockPosition() {
        String[] choices = {"Izquierda", "Centro", "Derecha"};
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_CLOCK_POSITION, 0);
        new AlertDialog.Builder(this)
                .setTitle("Posición del reloj")
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt(PREF_CLOCK_POSITION, which).apply();
                    dialog.dismiss();
                    showHome(true);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void chooseClockStyle() {
        String[] choices = {"Grande y fino", "Compacto", "Digital"};
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_CLOCK_STYLE, 0);
        new AlertDialog.Builder(this)
                .setTitle("Diseño del reloj")
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt(PREF_CLOCK_STYLE, which).apply();
                    dialog.dismiss();
                    showHome(true);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void chooseIconSize() {
        String[] choices = {"Compactos", "Normales", "Grandes"};
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_ICON_SIZE, 1);
        new AlertDialog.Builder(this)
                .setTitle("Tamaño de iconos")
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt(PREF_ICON_SIZE, which).apply();
                    dialog.dismiss();
                    showHome(true);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void chooseDockSlot() {
        String[] slots = {"Primera app", "Segunda app", "Tercera app", "Cuarta app"};
        new AlertDialog.Builder(this)
                .setTitle("¿Qué posición quieres cambiar?")
                .setItems(slots, (dialog, which) -> showDockPicker(which))
                .setNegativeButton("Cancelar", null)
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
                .setTitle("Elegir app · posición " + (slot + 1))
                .setItems(labels, (dialog, which) -> {
                    String packageName = choices.get(which).activityInfo.packageName;
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(PREF_DOCK_PREFIX + slot, packageName).apply();
                    rebuildDock();
                    Toast.makeText(this, "Acceso del dock actualizado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Restaurar", (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .remove(PREF_DOCK_PREFIX + slot).apply();
                    rebuildDock();
                });
        builder.show();
    }

    private void resetCustomization() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
        wallpaperBitmap = null;
        showHome(true);
        Toast.makeText(this, "Personalización restaurada", Toast.LENGTH_SHORT).show();
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

    private void showHome(boolean animate) {
        drawerOpen = false;
        drawerSearch = null;
        drawerGrid = null;
        drawerCount = null;
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

        TextView label = text("TU ESPACIO", 10, CYAN, true);
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
        date.setFormat24Hour("EEEE, d 'de' MMMM");
        date.setFormat12Hour("EEEE, d 'de' MMMM");
        date.setTextColor(MUTED);
        date.setTextSize(13);
        date.setGravity(clockGravity());
        applyLongPressCustomization(date);
        content.addView(date, withTop(lp(-1, -2), 1));

        LinearLayout favoritesHeader = row();
        TextView favoritesTitle = text("FAVORITAS", 11, TEXT, true);
        favoritesTitle.setLetterSpacing(0.1f);
        favoritesHeader.addView(favoritesTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView all = text("TODAS  ↓", 10, CYAN, true);
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
        TextView hint = text("Buscar en tus aplicaciones", 12, MUTED, false);
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
            try { holder.icon.setImageDrawable(item.loadIcon(getPackageManager())); }
            catch (RuntimeException error) { holder.icon.setImageResource(android.R.drawable.sym_def_app_icon); }
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
        TextView title = text("APLICACIONES", 20, TEXT, true);
        title.setLetterSpacing(0.045f);
        TextView sub = text("TU BIBLIOTECA MOTOROLL", 8, CYAN, true);
        sub.setLetterSpacing(0.13f);
        titles.addView(title);
        titles.addView(sub, withTop(lp(-2, -2), 4));
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
        drawerSearch.setHint("Buscar una aplicación");
        drawerSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        drawerSearch.setPadding(dp(16), 0, dp(16), 0);
        drawerSearch.setBackground(rounded(PANEL_ALT, 18, STROKE, 1));
        panel.addView(drawerSearch, withTop(lp(-1, 52), 17));

        drawerCount = text("", 10, MUTED, false);
        panel.addView(drawerCount, withTop(lp(-1, 18), 14));

        drawerGrid = new GridView(this);
        configureGrid(drawerGrid, 4);
        drawerGrid.setVerticalSpacing(dp(6));
        panel.addView(drawerGrid, withTop(new LinearLayout.LayoutParams(-1, 0, 1f), 7));

        TextView footer = text("Desliza hacia arriba para volver al inicio", 10, MUTED, false);
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
            panel.setTranslationY(-dp(62));
            panel.animate().alpha(1f).translationY(0f).setDuration(190).start();
        }
    }

    private void refreshDrawer(String query) {
        if (drawerGrid == null) return;
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<ResolveInfo> filtered = new ArrayList<>();
        for (ResolveInfo app : apps) {
            String label = safeLabel(app);
            if (needle.isEmpty() || label.toLowerCase(Locale.ROOT).contains(needle)) filtered.add(app);
        }
        if (drawerCount != null) drawerCount.setText(filtered.size() + " aplicaciones");
        drawerGrid.setAdapter(new AppAdapter(filtered, false));
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
            Toast.makeText(this, "No se pudo abrir esa aplicación", Toast.LENGTH_SHORT).show();
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
                // The home screen keeps the familiar pull-down gesture to open the app drawer.
                showDrawer(true, "");
                return true;
            }

            // IMPORTANT: only a swipe that begins in the drawer's header closes it.
            // Gestures that start in the app grid are left entirely to GridView scrolling.
            boolean startedInDrawerHeader = downY <= dp(122);
            if (drawerOpen && startedInDrawerHeader && mostlyVertical && dy < -dp(82)) {
                showHome(true);
                return true;
            }
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
        TextView title = text("Motoroll Home no pudo iniciarse", 20, TEXT, true);
        TextView detail = text(error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage()),
                12, MUTED, false);
        detail.setPadding(0, dp(14), 0, dp(20));
        screen.addView(title);
        screen.addView(detail);
        TextView retry = text("Volver a intentar", 13, CYAN, true);
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
