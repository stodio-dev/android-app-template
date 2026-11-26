package com.stodio.arrowcubes;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stodio.arrowcubes.core.Catalog;

/** Shared look of the screens: Hebrew right-to-left text, rounded buttons and cards, the coin chip. */
final class Ui {
    static final int INK = 0xFF23262E, MUTED = 0xFF5E6472, ACCENT = 0xFF3D6BE0, GOLD = 0xFFF5BE2E, GOOD = 0xFF2E9E5B,
            BAD = 0xFFD9534F;

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    static Drawable pressable(Context c, int color, float radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(0x33000000), round(color, dp(c, radiusDp)), null);
    }

    /** A rounded button; light text on dark fills. */
    static TextView button(Context c, String label, int fill, int textColor, float sp) {
        TextView b = text(c, label, sp, textColor, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(pressable(c, fill, 14));
        b.setPadding(dp(c, 14), dp(c, 11), dp(c, 14), dp(c, 11));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    static Drawable coinIcon(Context c, int sizeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(GOLD);
        g.setStroke(dp(c, 2), 0xFFC98F0F);
        int s = dp(c, sizeDp);
        g.setBounds(0, 0, s, s);
        return g;
    }

    /** "● 120" with a gold coin. */
    static TextView coinChip(Context c, int coins) {
        TextView t = text(c, String.valueOf(coins), 16, INK, true);
        t.setCompoundDrawablesRelative(coinIcon(c, 16), null, null, null);
        t.setCompoundDrawablePadding(dp(c, 6));
        t.setBackground(round(0xEEFFFFFF, dp(c, 18)));
        t.setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    static GradientDrawable gradient(Catalog.Background bg) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF000000 | bg.top, 0xFF000000 | bg.bottom});
    }

    /** Status and navigation bars in the background's colors. */
    @SuppressWarnings("deprecation")
    static void bars(Activity a, Catalog.Background bg) {
        Window w = a.getWindow();
        w.setStatusBarColor(0xFF000000 | bg.top);
        w.setNavigationBarColor(0xFF000000 | bg.bottom);
        int flags = 0;
        if (!bg.dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        w.getDecorView().setSystemUiVisibility(flags);
    }

    static int onBg(Catalog.Background bg) {
        return bg.dark ? Color.WHITE : INK;
    }

    static int onBgMuted(Catalog.Background bg) {
        return bg.dark ? 0xCCFFFFFF : MUTED;
    }

    static LinearLayout.LayoutParams lp(int w, int h, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = top;
        return p;
    }

    static LinearLayout.LayoutParams weight(float w, int sideMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
        p.leftMargin = sideMargin;
        p.rightMargin = sideMargin;
        return p;
    }

    static String stars(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 3; i++) b.append(i < n ? '★' : '☆');
        return b.toString();
    }
}
