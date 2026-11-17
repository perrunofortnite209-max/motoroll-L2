package com.motoroll.home;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
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
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
        loadApps();
        if(!getSharedPreferences(PREFS,0).getBoolean(PREF_WELCOME,false)) buildWelcome();
        else buildHome();
    }
    @Override public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus && pendingFocus!=null) pendingFocus.post(()->{
            if(pendingFocus!=null && pendingFocus.isAttachedToWindow()) pendingFocus.requestFocus();
        });
    }
    private void prepareRoot(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(54),dp(28),dp(54),dp(22)); root.setBackgroundColor(BG);
        root.setFocusable(false); setContentView(root);
    }
    private TextView button(String caption){
        TextView b=text(caption,16,TEXT,true); b.setGravity(Gravity.CENTER);
        b.setPadding(dp(22),dp(8),dp(22),dp(8)); makeFocusable(b); return b;
    }
    private void addBrandHeader(String rightLabel){
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("MOTOROLL",25,TEXT,true); brand.setLetterSpacing(.18f);
        header.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));
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
        prepareRoot(); addBrandHeader("TU CENTRO DE APLICACIONES");
        LinearLayout titleRow=new LinearLayout(this); titleRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heading=new LinearLayout(this); heading.setOrientation(1);
        TextView title=text("Mis aplicaciones",27,TEXT,true);
        TextView subtitle=text("Elige qué quieres ver hoy",14,MUTED,false);
        heading.addView(title); heading.addView(subtitle);
        titleRow.addView(heading,new LinearLayout.LayoutParams(0,dp(66),1));
        TextView settings=button("⚙  AJUSTES DEL LAUNCHER");
        settings.setTextSize(13); settings.setBackground(shape(PANEL,0,14));
        settings.setOnClickListener(v->buildSettings());
        titleRow.addView(settings,new LinearLayout.LayoutParams(-2,dp(48)));
        root.addView(titleRow,new LinearLayout.LayoutParams(-1,dp(82)));

        HorizontalScrollView categoriesScroll=new HorizontalScrollView(this);
        categoriesScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout categories=new LinearLayout(this); categories.setGravity(Gravity.CENTER_VERTICAL);
        String[] names={"Todo","Juegos","Vídeo","Música","Otros"};
        for(String name:names){
            TextView chip=button(name); chip.setTextSize(14);
            boolean active=name.equals(selectedCategory);
            chip.setBackground(shape(active?PANEL_FOCUS:PANEL,active?BLUE:0,22));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(42)); cp.rightMargin=dp(10);
            categories.addView(chip,cp);
            chip.setOnClickListener(v->{selectedCategory=name;buildHome();});
        }
        categoriesScroll.addView(categories); root.addView(categoriesScroll,new LinearLayout.LayoutParams(-1,dp(56)));

        ScrollView scroll=new ScrollView(this); scroll.setClipToPadding(false);
        scroll.setFocusable(false); scroll.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        grid=new GridLayout(this); grid.setColumnCount(5); grid.setUseDefaultMargins(false);
        scroll.addView(grid,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        status=text("",13,MUTED,false); root.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
        showFilteredApps();
        if(grid.getChildCount()>0)focusWhenReady(grid.getChildAt(0));
        else focusWhenReady(settings);
    }

    private int categoryOf(ResolveInfo app){
        try{return getPackageManager().getApplicationInfo(app.activityInfo.packageName,0).category;}
        catch(Exception e){return ApplicationInfo.CATEGORY_UNDEFINED;}
    }
    private boolean matches(ResolveInfo app){
        int c=categoryOf(app);
        switch(selectedCategory){
            case "Juegos": return c==ApplicationInfo.CATEGORY_GAME;
            case "Vídeo": return c==ApplicationInfo.CATEGORY_VIDEO || app.activityInfo.packageName.toLowerCase().matches(".*(youtube|netflix|disney|primevideo|plex|twitch|video).*");
            case "Música": return c==ApplicationInfo.CATEGORY_AUDIO || app.activityInfo.packageName.toLowerCase().matches(".*(spotify|music|deezer|tidal).*");
            case "Otros": return c!=ApplicationInfo.CATEGORY_GAME && c!=ApplicationInfo.CATEGORY_VIDEO && c!=ApplicationInfo.CATEGORY_AUDIO
                    && !app.activityInfo.packageName.toLowerCase().matches(".*(youtube|netflix|disney|primevideo|plex|twitch|video|spotify|music|deezer|tidal).*");
            default:return true;
        }
    }
    private void showFilteredApps(){
        grid.removeAllViews(); PackageManager pm=getPackageManager();
        int width=getResources().getDisplayMetrics().widthPixels;
        int cardWidth=Math.max(dp(142),(width-dp(150))/5); int count=0;
        for(ResolveInfo app:allApps){if(matches(app)){addAppCard(app,cardWidth);count++;}}
        status.setText(count+" aplicaciones  •  Flechas para navegar  •  OK para abrir");
        if(count==0){
            TextView empty=text("No hay aplicaciones en esta categoría.",16,MUTED,false);
            grid.addView(empty,new GridLayout.LayoutParams());
        }
    }
    private void addAppCard(ResolveInfo app,int width){
        LinearLayout card=new LinearLayout(this); card.setOrientation(1); card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10),dp(12),dp(10),dp(10)); card.setBackground(shape(PANEL,0,18));
        makeFocusable(card); card.setClickable(true);
        ImageView icon=new ImageView(this);
        try{icon.setImageDrawable(app.loadIcon(getPackageManager()));}catch(Exception ignored){}
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(icon,new LinearLayout.LayoutParams(dp(66),dp(66)));
        TextView name=text(String.valueOf(app.loadLabel(getPackageManager())),14,TEXT,true);
        name.setGravity(Gravity.CENTER); name.setMaxLines(2);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(42)); lp.topMargin=dp(8); card.addView(name,lp);
        card.setOnClickListener(v->launchApp(app));
        GridLayout.LayoutParams p=new GridLayout.LayoutParams(); p.width=width; p.height=dp(150);
        p.setMargins(dp(8),dp(8),dp(8),dp(8)); grid.addView(card,p);
    }
    private void launchApp(ResolveInfo app){
        try{
            Intent launch=getPackageManager().getLaunchIntentForPackage(app.activityInfo.packageName);
            if(launch==null){launch=new Intent(Intent.ACTION_MAIN);launch.addCategory("android.intent.category.LEANBACK_LAUNCHER");launch.setPackage(app.activityInfo.packageName);}
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(launch);
        }catch(Exception e){Toast.makeText(this,"No se pudo abrir la aplicación",Toast.LENGTH_SHORT).show();}
    }

    private void buildSettings(){
        prepareRoot(); addBrandHeader("PERSONALIZA TU EXPERIENCIA");
        TextView title=text("Ajustes del launcher",29,TEXT,true);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(66)));
        TextView description=text("Configura Motoroll TV como pantalla de inicio y administra su comportamiento.",16,MUTED,false);
        root.addView(description,new LinearLayout.LayoutParams(dp(850),dp(58)));
        LinearLayout options=new LinearLayout(this); options.setOrientation(LinearLayout.VERTICAL);
        TextView home=button("⌂   Establecer Motoroll TV como launcher predeterminado");
        home.setGravity(Gravity.CENTER_VERTICAL); home.setPadding(dp(22),0,dp(22),0);
        home.setOnClickListener(v->requestDefaultLauncher());
        options.addView(home,new LinearLayout.LayoutParams(dp(760),dp(64)));
        TextView refresh=button("↻   Actualizar lista de aplicaciones");
        refresh.setGravity(Gravity.CENTER_VERTICAL); refresh.setPadding(dp(22),0,dp(22),0);
        refresh.setOnClickListener(v->{loadApps();selectedCategory="Todo";buildHome();});
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(760),dp(64));rp.topMargin=dp(14);options.addView(refresh,rp);
        root.addView(options);
        TextView note=text("Puedes cambiar el launcher predeterminado desde los ajustes del sistema cuando quieras.",14,MUTED,false);
        root.addView(note,new LinearLayout.LayoutParams(dp(820),dp(64)));
        TextView back=button("←   VOLVER A MIS APLICACIONES");
        back.setBackground(shape(PANEL_FOCUS,BLUE,16));
        back.setOnClickListener(v->buildHome());
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(390),dp(56));bp.topMargin=dp(22);root.addView(back,bp);
        focusWhenReady(home);
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
}
