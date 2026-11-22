package com.stodio.arrowcubes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stodio.arrowcubes.core.Catalog;
import com.stodio.arrowcubes.core.Game;
import com.stodio.arrowcubes.core.Levels;

import java.io.File;
import java.io.FileInputStream;

/** The home screen: the next level's structure turning slowly behind the menu. Keeps the version line and the log button. */
public final class MainActivity extends Activity {
    private GameView demo;
    private FrameLayout root;
    private TextView title, subtitle, coins, play, progress, sound, crash, version;
    private Catalog.Skin shownSkin;
    private Catalog.Background shownBg;
    private int shownLevel;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        demo = new GameView(this, true);
        root.addView(demo, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int pad = Ui.dp(this, 20);
        col.setPadding(pad, Ui.dp(this, 16), pad, Ui.dp(this, 14));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        coins = Ui.coinChip(this, 0);
        top.addView(coins);
        top.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        sound = Ui.button(this, "", 0xEEFFFFFF, Ui.INK, 16);
        sound.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Store.setSound(MainActivity.this, !Store.sound(MainActivity.this));
                Sfx.on = Store.sound(MainActivity.this);
                Sfx.play(Sfx.COIN);
                refresh();
            }
        });
        top.addView(sound, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 42)));
        col.addView(top);

        title = Ui.text(this, getString(R.string.app_name), 40, Ui.INK, true);
        title.setShadowLayer(Ui.dp(this, 6), 0, Ui.dp(this, 2), 0x33000000);
        col.addView(title, Ui.lp(-1, -2, Ui.dp(this, 18)));
        subtitle = Ui.text(this, "פרקו את המבנה, קובייה אחרי קובייה", 16, Ui.MUTED, false);
        col.addView(subtitle, Ui.lp(-1, -2, Ui.dp(this, 2)));

        col.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));

        progress = Ui.text(this, "", 14, Ui.MUTED, true);
        progress.setGravity(Gravity.CENTER);
        col.addView(progress, Ui.lp(-1, -2, 0));
        play = Ui.button(this, "", Ui.ACCENT, Color.WHITE, 22);
        play.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        play.setElevation(Ui.dp(this, 4));
        play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(GameActivity.campaign(MainActivity.this, Store.level(MainActivity.this)));
            }
        });
        col.addView(play, Ui.lp(-1, -2, Ui.dp(this, 8)));

        LinearLayout row = new LinearLayout(this);
        int m = Ui.dp(this, 4);
        row.addView(menu("שלבים", LevelsActivity.class, -1), Ui.weight(1, m));
        row.addView(menu("משחק חופשי", ShopActivity.class, ShopActivity.TAB_SHAPES), Ui.weight(1, m));
        row.addView(menu("עיצובים", ShopActivity.class, ShopActivity.TAB_SKINS), Ui.weight(1, m));
        LinearLayout.LayoutParams rp = Ui.lp(-1, -2, Ui.dp(this, 10));
        rp.leftMargin = rp.rightMargin = -m;
        col.addView(row, rp);

        crash = Ui.text(this, "האפליקציה קרסה בפעם הקודמת. אפשר לשתף את הדוח בכפתור למטה.", 13, 0xFFA32D2D, true);
        crash.setGravity(Gravity.CENTER);
        crash.setBackground(Ui.round(0xEEFFFFFF, Ui.dp(this, 10)));
        crash.setPadding(m * 2, m, m * 2, m);
        col.addView(crash, Ui.lp(-1, -2, Ui.dp(this, 10)));

        LinearLayout foot = new LinearLayout(this);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        TextView share = Ui.text(this, "שיתוף יומן תקלות", 13, Ui.ACCENT, true);
        share.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        share.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String c = readCrash();
                Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, App.header() + "\n" + (c != null ? c + "\n\n" : "") + App.logText());
                startActivity(Intent.createChooser(i, "שיתוף יומן"));
            }
        });
        foot.addView(share);
        foot.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        version = Ui.text(this, "גרסה " + App.version(this), 12, Ui.MUTED, false);
        foot.addView(version);
        col.addView(foot, Ui.lp(-1, -2, Ui.dp(this, 6)));

        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    private TextView menu(String label, final Class<?> target, final int tab) {
        TextView t = Ui.button(this, label, 0xF2FFFFFF, Ui.INK, 15);
        t.setPadding(Ui.dp(this, 6), Ui.dp(this, 13), Ui.dp(this, 6), Ui.dp(this, 13));
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, target);
                if (tab >= 0) i.putExtra(ShopActivity.EXTRA_TAB, tab);
                startActivity(i);
            }
        });
        return t;
    }

    @Override protected void onResume() {
        super.onResume();
        demo.onResume();
        refresh();
    }

    @Override protected void onPause() {
        demo.onPause();
        super.onPause();
    }

    private void refresh() {
        final Catalog.Background bg = Store.background(this);
        final Catalog.Skin skin = Store.skin(this);
        Ui.bars(this, bg);
        root.setBackground(Ui.gradient(bg));
        title.setTextColor(Ui.onBg(bg));
        subtitle.setTextColor(Ui.onBgMuted(bg));
        progress.setTextColor(Ui.onBgMuted(bg));
        version.setTextColor(Ui.onBgMuted(bg));
        int level = Store.level(this);
        coins.setText(String.valueOf(Store.coins(this)));
        play.setText("שחקו · שלב " + level);
        progress.setText("★ " + Catalog.totalStars(Store.stars(this)) + "   ·   שלב " + level + " מתוך " + Levels.COUNT);
        sound.setText(Store.sound(this) ? "🔊" : "🔇");
        crash.setVisibility(App.crashFile(this).exists() ? View.VISIBLE : View.GONE);
        if (skin != shownSkin || bg != shownBg) {
            shownSkin = skin;
            shownBg = bg;
            demo.queueEvent(new Runnable() {
                @Override public void run() {
                    demo.renderer.setLook(skin, bg);
                }
            });
        }
        if (level != shownLevel) {
            shownLevel = level;
            final Levels.Spec spec = Levels.campaign(level);
            App.BG.execute(new Runnable() {
                @Override public void run() {
                    try {
                        final Game g = Levels.build(spec);
                        demo.queueEvent(new Runnable() {
                            @Override public void run() {
                                demo.renderer.setGame(g, spec.seed);
                            }
                        });
                    } catch (Throwable e) {
                        App.log("demo level failed", e);
                    }
                }
            });
        }
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
}
