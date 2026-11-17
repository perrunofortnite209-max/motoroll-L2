package com.motoroll.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Motoroll TV: remote-first launcher screen. No background services or third-party UI libraries. */
public class TvActivity extends Activity {
    private static final int BG=Color.rgb(5,7,13), PANEL=Color.rgb(18,22,38);
    private static final int BLUE=Color.rgb(65,137,255), TEXT=Color.rgb(246,247,255), MUTED=Color.rgb(149,158,181);
    private GridLayout grid;
    private TextView status;

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int color,int stroke){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(18));
        if(stroke!=0)d.setStroke(dp(2),stroke); return d;
    }
    private TextView label(String s,float size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
        build(); loadApps();
    }

    private void build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(1);
        root.setPadding(dp(54),dp(30),dp(54),dp(22)); root.setBackgroundColor(BG);
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=label("MOTOROLL",26,TEXT,true); brand.setLetterSpacing(.16f);
        header.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));
        TextClock clock=new TextClock(this); clock.setFormat12Hour("h:mm a"); clock.setFormat24Hour("HH:mm");
        clock.setTextColor(TEXT); clock.setTextSize(22); header.addView(clock); root.addView(header);
        TextView sub=label("Tu entretenimiento. Tu espacio.",16,MUTED,false);
        root.addView(sub,new LinearLayout.LayoutParams(-1,dp(38)));
        LinearLayout section=new LinearLayout(this); section.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("APLICACIONES",13,BLUE,true); title.setLetterSpacing(.12f);
        section.addView(title,new LinearLayout.LayoutParams(0,dp(46),1));
        TextView settings=label("⚙  Ajustes",14,TEXT,true); settings.setGravity(Gravity.CENTER);
        settings.setPadding(dp(18),0,dp(18),0); settings.setBackground(shape(PANEL,0)); settings.setFocusable(true);
        settings.setOnFocusChangeListener((v,f)->v.setBackground(shape(f?Color.rgb(28,48,82):PANEL,f?BLUE:0)));
        settings.setOnClickListener(v->{try{startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}catch(Exception ignored){}});
        section.addView(settings,new LinearLayout.LayoutParams(-2,dp(42))); root.addView(section);
        ScrollView scroll=new ScrollView(this); scroll.setClipToPadding(false);
        grid=new GridLayout(this); grid.setColumnCount(5); grid.setUseDefaultMargins(false);
        scroll.addView(grid,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        status=label("Cargando aplicaciones…",14,MUTED,false);
        root.addView(status,new LinearLayout.LayoutParams(-1,dp(30))); setContentView(root);
    }

    private void loadApps(){
        PackageManager pm=getPackageManager(); Map<String,ResolveInfo> map=new LinkedHashMap<>();
        Intent normal=new Intent(Intent.ACTION_MAIN); normal.addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo r:pm.queryIntentActivities(normal,0)) if(r.activityInfo!=null&&!getPackageName().equals(r.activityInfo.packageName))map.put(r.activityInfo.packageName,r);
        Intent tv=new Intent(Intent.ACTION_MAIN); tv.addCategory("android.intent.category.LEANBACK_LAUNCHER");
        for(ResolveInfo r:pm.queryIntentActivities(tv,0)) if(r.activityInfo!=null&&!getPackageName().equals(r.activityInfo.packageName))map.put(r.activityInfo.packageName,r);
        List<ResolveInfo> list=new ArrayList<>(map.values());
        Collections.sort(list,new Comparator<ResolveInfo>(){public int compare(ResolveInfo a,ResolveInfo b){return String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm)));}});
        int width=getResources().getDisplayMetrics().widthPixels; int cardWidth=Math.max(dp(142),(width-dp(150))/5);
        for(ResolveInfo r:list)addCard(r,cardWidth);
        status.setText(list.size()+" aplicaciones  •  Navega con las flechas y pulsa OK");
        if(grid.getChildCount()>0)grid.getChildAt(0).requestFocus();
    }

    private void addCard(ResolveInfo app,int width){
        LinearLayout card=new LinearLayout(this); card.setOrientation(1); card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10),dp(12),dp(10),dp(10)); card.setBackground(shape(PANEL,0));
        card.setFocusable(true); card.setClickable(true);
        card.setOnFocusChangeListener((v,f)->{
            v.setBackground(shape(f?Color.rgb(28,48,82):PANEL,f?BLUE:0));
            v.setScaleX(f?1.06f:1f); v.setScaleY(f?1.06f:1f); v.setElevation(f?dp(9):0);
        });
        ImageView icon=new ImageView(this);
        try{icon.setImageDrawable(app.loadIcon(getPackageManager()));}catch(Exception ignored){}
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));
        TextView name=label(String.valueOf(app.loadLabel(getPackageManager())),14,TEXT,true);
        name.setGravity(Gravity.CENTER); name.setMaxLines(2);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(42)); lp.topMargin=dp(8); card.addView(name,lp);
        card.setOnClickListener(v->{
            try{
                Intent launch=getPackageManager().getLaunchIntentForPackage(app.activityInfo.packageName);
                if(launch==null){launch=new Intent(Intent.ACTION_MAIN);launch.addCategory("android.intent.category.LEANBACK_LAUNCHER");launch.setPackage(app.activityInfo.packageName);}
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(launch);
            }catch(Exception e){android.widget.Toast.makeText(this,"No se pudo abrir la aplicación",0).show();}
        });
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=width;p.height=dp(148);
        p.setMargins(dp(8),dp(8),dp(8),dp(8));grid.addView(card,p);
    }
}