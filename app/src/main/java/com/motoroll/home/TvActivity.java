package com.motoroll.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Canvas;
import android.widget.FrameLayout;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Motoroll TV: lightweight, remote-first Android TV launcher. */
public class TvActivity extends Activity {
    private static final int BG=Color.rgb(7,10,18), PANEL=Color.rgb(20,27,43);
    private static final int PANEL_FOCUS=Color.rgb(29,48,78), BLUE=Color.rgb(73,145,255);
    private static final int TEXT=Color.rgb(246,248,255), MUTED=Color.rgb(151,164,190);
    private static final String PREFS="motoroll_tv", PREF_WELCOME="welcome_done";
    private LinearLayout root;
    private GridLayout grid;
    private TextView status;
    private final List<ResolveInfo> allApps=new ArrayList<>();
    private String selectedCategory="Todo";
    private View pendingFocus;
    private Uri downloadedUpdateUri;
    private long updateDownloadId = -1L;
    private boolean updateReceiverRegistered;
    private boolean showingSettings;
    private boolean packageReceiverRegistered;
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            // Refresh the visible library after installs, updates, replacements or removals.
            loadApps();
            if (!showingSettings) buildHome();
        }
    };
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            if (id != updateDownloadId) return;
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (manager == null) return;
            downloadedUpdateUri = manager.getUriForDownloadedFile(id);
            if (downloadedUpdateUri == null) {
                Toast.makeText(TvActivity.this, "No se pudo descargar la actualización.", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(TvActivity.this, "Actualización descargada. Preparando instalación…", Toast.LENGTH_LONG).show();
            installDownloadedUpdate();
        }
    };

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int color,int stroke,float radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(2),stroke); return d;
    }
    private TextView text(String s,float size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL); t.setIncludeFontPadding(true);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private void focusStyle(View v,boolean focused){
        v.setBackground(shape(focused?PANEL_FOCUS:PANEL,focused?BLUE:0,18));
        v.setScaleX(focused?1.045f:1f); v.setScaleY(focused?1.045f:1f);
        v.setElevation(focused?dp(8):0);
    }
    private void makeFocusable(View v){
        v.setFocusable(true); v.setFocusableInTouchMode(true);
        v.setOnFocusChangeListener((view,focused)->focusStyle(view,focused));
    }
    private void focusWhenReady(View v){
        pendingFocus=v;
        v.post(()->{ if(v.isAttachedToWindow() && v.isFocusable()) v.requestFocus(); });
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        IntentFilter updateFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(updateReceiver, updateFilter, Context.RECEIVER_EXPORTED);
        else registerReceiver(updateReceiver, updateFilter);
        updateReceiverRegistered = true;
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageReceiver, packageFilter, Context.RECEIVER_EXPORTED);
        else registerReceiver(packageReceiver, packageFilter);
        packageReceiverRegistered = true;
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
        loadApps();
        if(!getSharedPreferences(PREFS,0).getBoolean(PREF_WELCOME,false)) buildWelcome();
        else buildHome();
    }
    @Override protected void onResume(){
        super.onResume();
        if (downloadedUpdateUri != null && Build.VERSION.SDK_INT >= 26 && getPackageManager().canRequestPackageInstalls()) {
            Uri ready = downloadedUpdateUri;
            downloadedUpdateUri = null;
            launchPackageInstaller(ready);
        }
    }
    @Override protected void onDestroy(){
        if (updateReceiverRegistered) {
            try { unregisterReceiver(updateReceiver); } catch (Exception ignored) { }
            updateReceiverRegistered = false;
        }
        if (packageReceiverRegistered) {
            try { unregisterReceiver(packageReceiver); } catch (Exception ignored) { }
            packageReceiverRegistered = false;
        }
        super.onDestroy();
    }
    @Override public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus && pendingFocus!=null) pendingFocus.post(()->{
            if(pendingFocus!=null && pendingFocus.isAttachedToWindow()) pendingFocus.requestFocus();
        });
    }
    private void prepareRoot(){
        FrameLayout screen=new FrameLayout(this);
        screen.addView(new TvAmbientBackground(this),new FrameLayout.LayoutParams(-1,-1));
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(54),dp(28),dp(54),dp(22)); root.setBackgroundColor(Color.TRANSPARENT);
        root.setFocusable(false);
        screen.addView(root,new FrameLayout.LayoutParams(-1,-1));
        setContentView(screen);
    }
    private TextView button(String caption){
        TextView b=text(caption,16,TEXT,true); b.setGravity(Gravity.CENTER);
        b.setPadding(dp(22),dp(8),dp(22),dp(8)); makeFocusable(b); return b;
    }
    private void addBrandHeader(String rightLabel){
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brandGroup=new LinearLayout(this); brandGroup.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.ic_motoroll);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brandGroup.addView(logo,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout wordmark=new LinearLayout(this); wordmark.setOrientation(LinearLayout.VERTICAL);
        TextView brand=text("MOTOROLL",22,TEXT,true); brand.setLetterSpacing(.18f);
        TextView edition=text("TV  •  MOTOROLL OS",10,BLUE,true); edition.setLetterSpacing(.16f);
        wordmark.addView(brand); wordmark.addView(edition);
        LinearLayout.LayoutParams wordmarkLp=new LinearLayout.LayoutParams(-2,-2); wordmarkLp.leftMargin=dp(12);
        brandGroup.addView(wordmark,wordmarkLp);
        header.addView(brandGroup,new LinearLayout.LayoutParams(0,dp(52),1));
        TextClock clock=new TextClock(this); clock.setFormat12Hour("h:mm a"); clock.setFormat24Hour("HH:mm");
        clock.setTextColor(TEXT); clock.setTextSize(20); header.addView(clock);
        root.addView(header);
        TextView sub=text(rightLabel,14,MUTED,false);
        root.addView(sub,new LinearLayout.LayoutParams(-1,dp(32)));
    }

    private void buildWelcome(){
        prepareRoot(); addBrandHeader("TU TELEVISOR, TU ESPACIO");
        View spacer=new View(this); root.addView(spacer,new LinearLayout.LayoutParams(1,0,1));
        TextView eyebrow=text("MOTOROLL TV  /  HOME",13,BLUE,true); eyebrow.setLetterSpacing(.14f);
        root.addView(eyebrow,new LinearLayout.LayoutParams(-1,dp(30)));
        TextView title=text("Todo tu entretenimiento.\nEn un solo lugar.",36,TEXT,true);
        title.setLineSpacing(dp(5),1f); root.addView(title,new LinearLayout.LayoutParams(dp(850),dp(112)));
        TextView desc=text("Tus aplicaciones, organizadas y listas para abrir con tu control remoto.",18,MUTED,false);
        root.addView(desc,new LinearLayout.LayoutParams(dp(760),dp(58)));
        TextView start=button("EMPEZAR  〉");
        start.setTextSize(17); start.setBackground(shape(BLUE,BLUE,16));
        start.setOnFocusChangeListener((v,f)->{
            v.setBackground(shape(f?Color.rgb(91,158,255):BLUE,f?TEXT:BLUE,16));
            v.setScaleX(f?1.06f:1f); v.setScaleY(f?1.06f:1f);
        });
        start.setOnClickListener(v->{
            getSharedPreferences(PREFS,0).edit().putBoolean(PREF_WELCOME,true).apply();
            buildHome();
        });
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(250),dp(58)); bp.topMargin=dp(20);
        root.addView(start,bp);
        TextView hint=text("Usa las flechas para moverte y OK para seleccionar.",13,MUTED,false);
        root.addView(hint,new LinearLayout.LayoutParams(-1,dp(44)));
        View bottom=new View(this); root.addView(bottom,new LinearLayout.LayoutParams(1,0,1));
        focusWhenReady(start);
    }

    private void loadApps(){
        PackageManager pm=getPackageManager(); Map<String,ResolveInfo> map=new LinkedHashMap<>();
        Intent normal=new Intent(Intent.ACTION_MAIN); normal.addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo r:pm.queryIntentActivities(normal,0))
            if(r.activityInfo!=null&&!getPackageName().equals(r.activityInfo.packageName))map.put(r.activityInfo.packageName,r);
        Intent tv=new Intent(Intent.ACTION_MAIN); tv.addCategory("android.intent.category.LEANBACK_LAUNCHER");
        for(ResolveInfo r:pm.queryIntentActivities(tv,0))
            if(r.activityInfo!=null&&!getPackageName().equals(r.activityInfo.packageName))map.put(r.activityInfo.packageName,r);
        allApps.clear(); allApps.addAll(map.values());
        Collections.sort(allApps,(a,b)->String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));
    }

    private void buildHome(){
        showingSettings=false;
        prepareRoot();
        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brandGroup=new LinearLayout(this);
        brandGroup.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_motoroll);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brandGroup.addView(logo,new LinearLayout.LayoutParams(dp(56),dp(56)));
        LinearLayout brandText=new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        TextView brand=text("MOTOROLL",24,TEXT,true); brand.setLetterSpacing(.20f);
        TextView edition=text("TV  /  MOTOROLL OS",10,BLUE,true); edition.setLetterSpacing(.18f);
        brandText.addView(brand); brandText.addView(edition);
        LinearLayout.LayoutParams brandTextLp=new LinearLayout.LayoutParams(-2,-2);
        brandTextLp.leftMargin=dp(13); brandGroup.addView(brandText,brandTextLp);
        header.addView(brandGroup,new LinearLayout.LayoutParams(0,dp(60),1));
        TextClock clock=new TextClock(this);
        clock.setFormat12Hour("h:mm a"); clock.setFormat24Hour("HH:mm");
        clock.setTextColor(TEXT); clock.setTextSize(20); clock.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        header.addView(clock);
        TextView androidSettings=button("⚙");
        androidSettings.setContentDescription("Configuración de Android TV");
        androidSettings.setTextSize(20); androidSettings.setBackground(shape(0xCC17243A,0,14));
        LinearLayout.LayoutParams sysLp=new LinearLayout.LayoutParams(dp(52),dp(48));
        sysLp.leftMargin=dp(18); header.addView(androidSettings,sysLp);
        androidSettings.setOnClickListener(v->openAndroidTvSettings());
        TextView menu=button("☰  MENÚ"); menu.setTextSize(13);
        menu.setBackground(shape(0xCC17243A,BLUE,14));
        LinearLayout.LayoutParams menuLp=new LinearLayout.LayoutParams(dp(136),dp(48));
        menuLp.leftMargin=dp(10); header.addView(menu,menuLp);
        menu.setOnClickListener(v->buildSettings());
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(68)));
        LinearLayout sectionHeader=new LinearLayout(this); sectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView subtitle=text("TU ESPACIO DIGITAL",12,BLUE,true); subtitle.setLetterSpacing(.20f);
        sectionHeader.addView(subtitle,new LinearLayout.LayoutParams(0,dp(34),1));
        TextView count=text(allApps.size()+" APPS",10,MUTED,true); count.setLetterSpacing(.12f);
        sectionHeader.addView(count); root.addView(sectionHeader,new LinearLayout.LayoutParams(-1,dp(38)));
        ScrollView contentScroll=new ScrollView(this);
        contentScroll.setVerticalScrollBarEnabled(false); contentScroll.setClipToPadding(false);
        contentScroll.setFocusable(false);
        contentScroll.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        LinearLayout sections=new LinearLayout(this); sections.setOrientation(LinearLayout.VERTICAL);
        contentScroll.addView(sections,new ScrollView.LayoutParams(-1,-2));
        root.addView(contentScroll,new LinearLayout.LayoutParams(-1,0,1));
        View firstApp=addFlauncherSection(sections,"APLICACIONES",false);
        addFlauncherSection(sections,"ENTRETENIMIENTO",true);
        status=text("LISTO PARA DISFRUTAR",10,MUTED,true); status.setLetterSpacing(.12f);
        root.addView(status,new LinearLayout.LayoutParams(-1,dp(22)));
        TextView tip=text("OK ABRIR   •   MANTENER OK: OPCIONES   •   MENÚ: AJUSTES",9,0xFF8295B8,false);
        tip.setLetterSpacing(.06f); root.addView(tip,new LinearLayout.LayoutParams(-1,dp(20)));
        if(firstApp!=null)focusWhenReady(firstApp); else focusWhenReady(menu);
    }

    private View addFlauncherSection(LinearLayout parent,String heading,boolean entertainment){
        List<ResolveInfo> items=new ArrayList<>();
        for(ResolveInfo app:allApps){
            boolean media=matchesCategory(app,"Vídeo")||matchesCategory(app,"Música");
            if(media==entertainment)items.add(app);
        }
        if(items.isEmpty())return null;

        TextView label=text(heading,13,TEXT,true);
        label.setLetterSpacing(.12f);
        LinearLayout.LayoutParams labelLp=new LinearLayout.LayoutParams(-1,dp(34));
        labelLp.topMargin=dp(12);
        parent.addView(label,labelLp);

        GridLayout appGrid=new GridLayout(this);
        appGrid.setUseDefaultMargins(false);
        appGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        appGrid.setColumnCount(6);
        appGrid.setRowCount((int)Math.ceil(items.size()/6.0));
        parent.addView(appGrid,new LinearLayout.LayoutParams(-1,-2));

        for(ResolveInfo app:items)addFlauncherAppCard(app,appGrid);
        return appGrid.getChildCount()>0?appGrid.getChildAt(0):null;
    }

    private void addFlauncherAppCard(ResolveInfo app,GridLayout target){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8),dp(9),dp(8),dp(7));
        card.setBackground(shape(0x99141D2E,0,12));
        makeFocusable(card);
        card.setClickable(true);

        ImageView icon=new ImageView(this);
        try{icon.setImageDrawable(app.loadIcon(getPackageManager()));}catch(Exception ignored){}
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54)));

        TextView name=text(String.valueOf(app.loadLabel(getPackageManager())),12,TEXT,true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameLp=new LinearLayout.LayoutParams(-1,dp(34));
        nameLp.topMargin=dp(5);
        card.addView(name,nameLp);

        card.setOnClickListener(v->launchApp(app));
        card.setLongClickable(true);
        card.setOnLongClickListener(v->{showAppActions(app,card);return true;});

        GridLayout.LayoutParams p=new GridLayout.LayoutParams();
        p.width=dp(174);p.height=dp(124);
        p.setMargins(0,dp(4),dp(10),dp(5));
        target.addView(card,p);
    }

    private void addViewAllCard(String category,int width){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10),dp(10),dp(10),dp(8));
        card.setBackground(shape(0xAA17243A,BLUE,18));
        makeFocusable(card);
        card.setClickable(true);
        TextView symbol=text("＋",34,BLUE,true);
        symbol.setGravity(Gravity.CENTER);
        card.addView(symbol,new LinearLayout.LayoutParams(-1,dp(64)));
        TextView label=text("VER TODAS",11,TEXT,true);
        label.setGravity(Gravity.CENTER);
        card.addView(label,new LinearLayout.LayoutParams(-1,dp(38)));
        card.setOnClickListener(v->buildCategoryCatalog(category));
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();
        p.width=width;p.height=dp(142);
        p.setMargins(0,dp(5),dp(12),dp(5));
        grid.addView(card,p);
    }

    private void buildCategoryCatalog(String category){
        showingSettings=false;
        prepareRoot();
        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=button("←  INICIO");
        back.setTextSize(13);
        back.setBackground(shape(PANEL_FOCUS,BLUE,14));
        back.setOnClickListener(v->buildHome());
        header.addView(back,new LinearLayout.LayoutParams(dp(142),dp(46)));
        TextView title=text("  "+("Todo".equals(category)?"Todas las aplicaciones":category),24,TEXT,true);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));
        TextClock clock=new TextClock(this);
        clock.setFormat12Hour("h:mm a");clock.setFormat24Hour("HH:mm");
        clock.setTextColor(TEXT);clock.setTextSize(18);
        header.addView(clock);
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(58)));

        status=text("Cargando aplicaciones…",12,MUTED,false);
        root.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
        ScrollView scroll=new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFocusable(false);
        scroll.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        grid=new GridLayout(this);
        grid.setUseDefaultMargins(false);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        scroll.addView(grid,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        selectedCategory=category;
        showFilteredApps();
        if(grid.getChildCount()>0)focusWhenReady(grid.getChildAt(0));
        else focusWhenReady(back);
    }

    private int categoryOf(ResolveInfo app){
        try{return getPackageManager().getApplicationInfo(app.activityInfo.packageName,0).category;}
        catch(Exception e){return ApplicationInfo.CATEGORY_UNDEFINED;}
    }
    private boolean matches(ResolveInfo app){ return matchesCategory(app,selectedCategory); }
    private boolean matchesCategory(ResolveInfo app,String category){
        int c=categoryOf(app);
        String pkg=app.activityInfo.packageName.toLowerCase();
        boolean video=c==ApplicationInfo.CATEGORY_VIDEO || pkg.matches(".*(youtube|netflix|disney|primevideo|plex|twitch|video).*");
        boolean music=c==ApplicationInfo.CATEGORY_AUDIO || pkg.matches(".*(spotify|music|deezer|tidal).*");
        boolean game=c==ApplicationInfo.CATEGORY_GAME;
        switch(category){
            case "Juegos": return game;
            case "Vídeo": return video;
            case "Música": return music;
            case "Otros": return !game&&!video&&!music;
            default:return true;
        }
    }
    private void showFilteredApps(){
        grid.removeAllViews();
        List<ResolveInfo> visible=new ArrayList<>();
        for(ResolveInfo app:allApps)if(matches(app))visible.add(app);
        int screenWidth=getResources().getDisplayMetrics().widthPixels;
        int availableWidth=screenWidth-dp(54*2)-dp(194)-dp(30)-dp(30);
        int cardWidth=dp(176);
        int columns=Math.max(3,Math.min(7,(availableWidth+dp(14))/(cardWidth+dp(14))));
        grid.setColumnCount(columns);
        grid.setRowCount(Math.max(1,(int)Math.ceil(visible.size()/(double)columns)));
        for(ResolveInfo app:visible)addAppCard(app,cardWidth);
        status.setText(visible.size()+" aplicaciones  ·  "+columns+" por fila");
        if(visible.isEmpty()){
            TextView empty=text("No hay aplicaciones en esta categoría.",16,MUTED,false);
            GridLayout.LayoutParams ep=new GridLayout.LayoutParams();
            ep.width=dp(420); ep.height=dp(150);
            grid.addView(empty,ep);
        }
    }
    private void addAppCard(ResolveInfo app,int width){
        LinearLayout card=new LinearLayout(this); card.setOrientation(1); card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10),dp(10),dp(10),dp(8)); card.setBackground(shape(0xCC151F32,0,18));
        makeFocusable(card); card.setClickable(true);
        ImageView icon=new ImageView(this);
        try{icon.setImageDrawable(app.loadIcon(getPackageManager()));}catch(Exception ignored){}
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));
        TextView name=text(String.valueOf(app.loadLabel(getPackageManager())),12,TEXT,true);
        name.setGravity(Gravity.CENTER); name.setMaxLines(2);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(38)); lp.topMargin=dp(7); card.addView(name,lp);
        card.setOnClickListener(v->launchApp(app));
        // Holding OK on an Android TV remote opens per-app actions.
        card.setLongClickable(true);
        card.setOnLongClickListener(v->{
            showAppActions(app,card);
            return true;
        });
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();
        p.width=width; p.height=dp(142);
        p.setMargins(0,dp(5),dp(12),dp(5));
        grid.addView(card,p);
    }
    private void showAppActions(ResolveInfo app,View appCard){
        if(app==null||app.activityInfo==null)return;
        final String packageName=app.activityInfo.packageName;
        final String appName=String.valueOf(app.loadLabel(getPackageManager()));
        final String[] actions={"Desinstalar / Borrar aplicación","Información de la aplicación"};
        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle(appName)
                .setItems(actions,(d,which)->{
                    // Close the menu first: some Android TV package installers refuse
                    // to open another system activity while an AlertDialog is attached.
                    d.dismiss();
                    if(appCard!=null){
                        appCard.postDelayed(()->{
                            if(which==0)uninstallApp(packageName);
                            else if(which==1)openAppInfo(packageName);
                        },250);
                    }else{
                        if(which==0)uninstallApp(packageName);
                        else if(which==1)openAppInfo(packageName);
                    }
                })
                .setOnDismissListener(d->{
                    if(appCard!=null&&appCard.isAttachedToWindow())appCard.requestFocus();
                })
                .create();
        dialog.setOnShowListener(d->{
            if(dialog.getListView()!=null)dialog.getListView().setSelector(android.R.drawable.list_selector_background);
        });
        dialog.show();
    }
    private void uninstallApp(String packageName){
        if(packageName==null||packageName.isEmpty()||getPackageName().equals(packageName)){
            Toast.makeText(this,"Esta aplicación no se puede desinstalar desde aquí.",Toast.LENGTH_LONG).show();
            return;
        }
        Uri packageUri=Uri.parse("package:"+packageName);
        try{
            // Preferred uninstall flow on Android 8+ (including Android TV).
            Intent uninstall=new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            uninstall.setData(packageUri);
            uninstall.putExtra(Intent.EXTRA_RETURN_RESULT,true);
            startActivity(uninstall);
            return;
        }catch(Exception firstError){
            // Compatibility fallback for TV builds with an older package installer.
            try{
                Intent legacy=new Intent(Intent.ACTION_DELETE);
                legacy.setData(packageUri);
                startActivity(legacy);
                return;
            }catch(Exception secondError){
                // At least leave the user on the system app page, where uninstall
                // may still be available even if the TV image omits the uninstall UI.
                try{
                    openAppInfo(packageName);
                    Toast.makeText(this,"No se abrió el desinstalador. Revisa la opción Desinstalar en Información de la aplicación.",Toast.LENGTH_LONG).show();
                }catch(Exception ignored){
                    Toast.makeText(this,"Este televisor no ofrece una pantalla para desinstalar esta aplicación.",Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    private void openAppInfo(String packageName){
        try{
            Intent info=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+packageName));
            startActivity(info);
        }catch(Exception e){
            Toast.makeText(this,"No se pudo abrir la información de la aplicación.",Toast.LENGTH_LONG).show();
        }
    }

    private void launchApp(ResolveInfo app){
        try{
            Intent launch=getPackageManager().getLaunchIntentForPackage(app.activityInfo.packageName);
            if(launch==null){launch=new Intent(Intent.ACTION_MAIN);launch.addCategory("android.intent.category.LEANBACK_LAUNCHER");launch.setPackage(app.activityInfo.packageName);}
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(launch);
        }catch(Exception e){Toast.makeText(this,"No se pudo abrir la aplicación",Toast.LENGTH_SHORT).show();}
    }

    private void buildSettings(){
        showingSettings=true;
        prepareRoot(); addBrandHeader("MENÚ MOTOROLL TV");
        TextView title=text("Ajustes del launcher",29,TEXT,true);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(66)));
        TextView description=text("Configura Motoroll TV como pantalla de inicio y administra su comportamiento.",16,MUTED,false);
        root.addView(description,new LinearLayout.LayoutParams(dp(850),dp(58)));
        LinearLayout options=new LinearLayout(this); options.setOrientation(LinearLayout.VERTICAL);
        TextView androidSettings=button("⚙   Abrir configuración de Android TV");
        androidSettings.setGravity(Gravity.CENTER_VERTICAL); androidSettings.setPadding(dp(22),0,dp(22),0);
        androidSettings.setOnClickListener(v->openAndroidTvSettings());
        options.addView(androidSettings,new LinearLayout.LayoutParams(dp(760),dp(64)));
        TextView home=button("⌂   Establecer Motoroll TV como launcher predeterminado");
        home.setGravity(Gravity.CENTER_VERTICAL); home.setPadding(dp(22),0,dp(22),0);
        home.setOnClickListener(v->requestDefaultLauncher());
        options.addView(home,new LinearLayout.LayoutParams(dp(760),dp(64)));
        TextView refresh=button("↻   Actualizar lista de aplicaciones");
        refresh.setGravity(Gravity.CENTER_VERTICAL); refresh.setPadding(dp(22),0,dp(22),0);
        refresh.setOnClickListener(v->{loadApps();selectedCategory="Todo";buildHome();});
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(760),dp(64));rp.topMargin=dp(14);options.addView(refresh,rp);
        TextView updates=button("⇧   Buscar actualizaciones de Motoroll TV");
        updates.setGravity(Gravity.CENTER_VERTICAL); updates.setPadding(dp(22),0,dp(22),0);
        updates.setOnClickListener(v->checkForTvUpdates());
        LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(dp(760),dp(64));up.topMargin=dp(14);options.addView(updates,up);
        TextView channel=text("Canal independiente de GitHub  ·  Motoroll TV  ·  com.motoroll.tv",13,BLUE,false);
        root.addView(options);
        root.addView(channel,new LinearLayout.LayoutParams(-1,dp(38)));
        TextView note=text("Las actualizaciones de Motoroll TV se descargan desde su propio canal de GitHub. Motoroll Home del teléfono conserva su canal y sus versiones independientes.",14,MUTED,false);
        root.addView(note,new LinearLayout.LayoutParams(dp(820),dp(64)));
        TextView back=button("←   VOLVER A MIS APLICACIONES");
        back.setBackground(shape(PANEL_FOCUS,BLUE,16));
        back.setOnClickListener(v->buildHome());
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(390),dp(56));bp.topMargin=dp(22);root.addView(back,bp);
        focusWhenReady(home);
    }
    private void openAndroidTvSettings(){
        String[] actions={Settings.ACTION_SETTINGS,Settings.ACTION_WIRELESS_SETTINGS,
                Settings.ACTION_APPLICATION_SETTINGS,Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS};
        for(String action:actions){
            try{
                Intent intent=new Intent(action);
                if(intent.resolveActivity(getPackageManager())!=null){
                    startActivity(intent);
                    return;
                }
            }catch(Exception ignored){}
        }
        try{
            Intent tvSettings=getPackageManager().getLaunchIntentForPackage("com.android.tv.settings");
            if(tvSettings!=null){startActivity(tvSettings);return;}
        }catch(Exception ignored){}
        Toast.makeText(this,"Este dispositivo no permite abrir la configuración de Android TV.",Toast.LENGTH_LONG).show();
    }

    private void requestDefaultLauncher(){
        try{
            if(Build.VERSION.SDK_INT>=29){
                RoleManager rm=getSystemService(RoleManager.class);
                if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_HOME)){
                    if(rm.isRoleHeld(RoleManager.ROLE_HOME)){
                        Toast.makeText(this,"Motoroll TV ya es tu launcher predeterminado",Toast.LENGTH_LONG).show();
                    }else startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),410);
                    return;
                }
            }
            Intent i=new Intent("android.settings.HOME_SETTINGS");
            if(i.resolveActivity(getPackageManager())==null)i=new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            startActivity(i);
        }catch(Exception e){
            try{startActivity(new Intent(Settings.ACTION_SETTINGS));}
            catch(Exception ignored){Toast.makeText(this,"Abre Ajustes del sistema para elegir la aplicación de inicio.",Toast.LENGTH_LONG).show();}
        }
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==410){
            String message=resultCode==RESULT_OK?"Motoroll TV quedó como launcher predeterminado":"No se cambió el launcher predeterminado";
            Toast.makeText(this,message,Toast.LENGTH_LONG).show();
            buildHome();
        }
    }


    /** Checks only the Motoroll TV release channel; mobile Motoroll Home releases are ignored. */
    private void checkForTvUpdates(){
        Toast.makeText(this,"Conectando con GitHub…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            HttpURLConnection connection=null;
            try{
                URL url=new URL("https://api.github.com/repos/perrunofortnite209-max/motoroll-L2/releases?per_page=100");
                connection=(HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000); connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept","application/vnd.github+json");
                connection.setRequestProperty("User-Agent","Motoroll-TV-Updater");
                int code=connection.getResponseCode();
                if(code<200||code>=300)throw new Exception("GitHub respondió "+code);
                BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"));
                StringBuilder body=new StringBuilder(); String line;
                while((line=reader.readLine())!=null)body.append(line);
                reader.close();
                JSONArray releases=new JSONArray(body.toString());
                JSONObject newest=null; String newestVersion=null;
                for(int i=0;i<releases.length();i++){
                    JSONObject release=releases.getJSONObject(i);
                    String tag=release.optString("tag_name","");
                    if(!tag.startsWith("motoroll-tv-v")||release.optBoolean("draft",false)||release.optBoolean("prerelease",false))continue;
                    String version=tag.substring("motoroll-tv-v".length());
                    if(newest==null||compareVersions(version,newestVersion)>0){newest=release;newestVersion=version;}
                }
                if(newest==null)throw new Exception("Todavía no hay versiones publicadas de Motoroll TV.");
                if(compareVersions(newestVersion,BuildConfig.VERSION_NAME)<=0){
                    runOnUiThread(()->Toast.makeText(this,"Motoroll TV ya está actualizado.",Toast.LENGTH_LONG).show());
                    return;
                }
                JSONArray assets=newest.optJSONArray("assets");
                String apkUrl=null;
                if(assets!=null)for(int i=0;i<assets.length();i++){
                    JSONObject asset=assets.getJSONObject(i);
                    if("MotorollTV.apk".equals(asset.optString("name"))){apkUrl=asset.optString("browser_download_url");break;}
                }
                if(apkUrl==null||apkUrl.isEmpty())throw new Exception("La versión de GitHub no contiene MotorollTV.apk.");
                final String downloadUrl=apkUrl;
                final String versionToDownload=newestVersion;
                runOnUiThread(()->downloadTvUpdate(downloadUrl,versionToDownload));
            }catch(Exception error){
                String message=error.getMessage()==null?"Error de conexión":error.getMessage();
                runOnUiThread(()->Toast.makeText(this,"No se pudo buscar: "+message,Toast.LENGTH_LONG).show());
            }finally{if(connection!=null)connection.disconnect();}
        }).start();
    }
    private int compareVersions(String a,String b){
        if(b==null)return 1;
        String[] aa=a.split("\\."),bb=b.split("\\.");
        for(int i=0;i<Math.max(aa.length,bb.length);i++){
            int x=i<aa.length?parseVersionPart(aa[i]):0;
            int y=i<bb.length?parseVersionPart(bb[i]):0;
            if(x!=y)return Integer.compare(x,y);
        }
        return 0;
    }
    private int parseVersionPart(String value){
        try{return Integer.parseInt(value.replaceAll("[^0-9].*$",""));}catch(Exception ignored){return 0;}
    }
    private void downloadTvUpdate(String url,String version){
        try{
            DownloadManager manager=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            if(manager==null)throw new Exception("Descargas no está disponible en este dispositivo.");
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Motoroll TV "+version);
            request.setDescription("Descargando actualización desde GitHub");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this,Environment.DIRECTORY_DOWNLOADS,"MotorollTV-update.apk");
            updateDownloadId=manager.enqueue(request);
            Toast.makeText(this,"Descargando Motoroll TV "+version+"…",Toast.LENGTH_LONG).show();
        }catch(Exception error){
            Toast.makeText(this,"No se pudo iniciar la descarga: "+error.getMessage(),Toast.LENGTH_LONG).show();
        }
    }
    private void installDownloadedUpdate(){
        if(downloadedUpdateUri==null)return;
        if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
            Toast.makeText(this,"Permite instalar aplicaciones desconocidas para completar la actualización.",Toast.LENGTH_LONG).show();
            try{
                Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));
                settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(settings);
            }catch(Exception ignored){
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            }
            return;
        }
        Uri ready=downloadedUpdateUri; downloadedUpdateUri=null;
        launchPackageInstaller(ready);
    }
    private void launchPackageInstaller(Uri apk){
        try{
            Intent install=new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apk,"application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
        }catch(Exception error){
            Toast.makeText(this,"No se pudo abrir el instalador: "+error.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    /** Same AMOLED blue/purple ambient wallpaper language as Motoroll Home, composed for widescreen TV. */
    private final class TvAmbientBackground extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private RadialGradient blueGlow,purpleGlow;
        TvAmbientBackground(android.content.Context context){super(context);setLayerType(View.LAYER_TYPE_HARDWARE,null);}
        @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
            super.onSizeChanged(w,h,oldW,oldH);
            float radius=Math.max(w,h)*0.66f;
            blueGlow=new RadialGradient(w*0.91f,h*0.12f,radius,
                    new int[]{0x59365FAF,0x20192349,0x00070A12},
                    new float[]{0f,0.56f,1f},Shader.TileMode.CLAMP);
            purpleGlow=new RadialGradient(w*0.08f,h*0.88f,radius*0.88f,
                    new int[]{0x4D693AAB,0x1A24163F,0x00070A12},
                    new float[]{0f,0.58f,1f},Shader.TileMode.CLAMP);
        }
        @Override protected void onDraw(Canvas canvas){
            canvas.drawColor(BG);
            if(blueGlow!=null){paint.setShader(blueGlow);canvas.drawRect(0,0,getWidth(),getHeight(),paint);}
            if(purpleGlow!=null){paint.setShader(purpleGlow);canvas.drawRect(0,0,getWidth(),getHeight(),paint);}
            paint.setShader(null);
            // Motoroll signature: luminous diagonal ribbons and a quiet M watermark.
            paint.setShader(null); paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3)); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(0x123F9EFF);
            float w=getWidth(), h=getHeight();
            canvas.drawLine(w*0.70f,h*0.02f,w*0.98f,h*0.30f,paint);
            canvas.drawLine(w*0.76f,0,w,h*0.24f,paint);
            paint.setStrokeWidth(dp(7)); paint.setColor(0x0D65B8FF);
            android.graphics.Path mark=new android.graphics.Path();
            mark.moveTo(w*0.73f,h*0.39f); mark.lineTo(w*0.81f,h*0.57f);
            mark.lineTo(w*0.89f,h*0.43f); mark.lineTo(w*0.97f,h*0.57f);
            mark.lineTo(w*1.05f,h*0.39f); canvas.drawPath(mark,paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(0x125B8DFF);
            canvas.drawRect(0,h*0.82f,w,h,paint);
        }
    }
}
