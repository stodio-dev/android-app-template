package com.stodio.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.stodio.app.core.Greeting;

import java.io.File;
import java.io.FileInputStream;

/** The first screen. Replace its body with the app; keep the version line and the log button. */
public final class MainActivity extends Activity {
    static final int INK = 0xFF2C2C2A, MUTED = 0xFF5F5E5A, ACCENT = 0xFF185FA5, BG = 0xFFF7F6F2;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int pad = dp(20);
        root.setPadding(pad, dp(28), pad, pad);
        scroll.addView(root);

        root.addView(text(getString(R.string.app_name), 26, INK, true));
        root.addView(text(Greeting.hello(getString(R.string.app_name)), 16, MUTED, false), lp(dp(8)));

        String crash = readCrash();
        if (crash != null) {
            TextView t = text("האפליקציה קרסה בפעם הקודמת. אפשר לשתף את הדוח בכפתור למטה.", 14, 0xFFA32D2D, false);
            root.addView(t, lp(dp(16)));
        }

        Button share = new Button(this);
        share.setText("שיתוף יומן תקלות");
        share.setAllCaps(false);
        share.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ACCENT);
        bg.setCornerRadius(dp(10));
        share.setBackground(bg);
        share.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String c = readCrash();
                Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, App.header() + "\n" + (c != null ? c + "\n\n" : "") + App.logText());
                startActivity(Intent.createChooser(i, "שיתוף יומן"));
            }
        });
        root.addView(share, lp(dp(28)));

        TextView ver = text("גרסה " + App.version(this), 12, MUTED, false);
        ver.setGravity(Gravity.CENTER);
        root.addView(ver, lp(dp(16)));
        setContentView(scroll);
    }

    private String readCrash() {
        File f = App.crashFile(this);
        if (!f.exists()) return null;
        try {
            FileInputStream in = new FileInputStream(f);
            byte[] b = new byte[(int) Math.min(f.length(), 64000)];
            int n = in.read(b);
            in.close();
            return new String(b, 0, Math.max(n, 0), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams lp(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = top;
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
