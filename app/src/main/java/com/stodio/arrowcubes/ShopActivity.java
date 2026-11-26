package com.stodio.arrowcubes;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.stodio.arrowcubes.core.Catalog;
import com.stodio.arrowcubes.core.Shapes;

/** Block skins and backgrounds to buy with coins, and the shapes (free play) the campaign has opened. */
public final class ShopActivity extends Activity {
    static final String EXTRA_TAB = "tab";
    static final int TAB_SKINS = 0, TAB_BACKGROUNDS = 1, TAB_SHAPES = 2;
    private static final String[] TABS = {"קוביות", "רקעים", "צורות"};

    private int tab;
    private LinearLayout list, tabs;
    private TextView coins;
    private Catalog.Background bg;
    private LinearLayout col;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        tab = getIntent().getIntExtra(EXTRA_TAB, TAB_SKINS);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int pad = Ui.dp(this, 16);
        col.setPadding(pad, pad, pad, 0);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView head = Ui.text(this, "עיצובים וצורות", 24, Ui.INK, true);
        head.setTag("head");
        top.addView(head, new LinearLayout.LayoutParams(0, -2, 1));
        coins = Ui.coinChip(this, Store.coins(this));
        top.addView(coins);
        TextView close = Ui.button(this, "✕", 0xEEFFFFFF, Ui.INK, 16);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 40));
        cp.setMarginStart(Ui.dp(this, 8));
        top.addView(close, cp);
        col.addView(top);

        tabs = new LinearLayout(this);
        col.addView(tabs, Ui.lp(-1, -2, Ui.dp(this, 12)));

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 20));
        scroll.addView(list);
        col.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(col);
    }

    @Override protected void onResume() {
        super.onResume();
        show();
    }

    private void show() {
        bg = Store.background(this);
        Ui.bars(this, bg);
        col.setBackground(Ui.gradient(bg));
        ((TextView) col.findViewWithTag("head")).setTextColor(Ui.onBg(bg));
        coins.setText(String.valueOf(Store.coins(this)));
        tabs.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            TextView b = Ui.button(this, TABS[i], i == tab ? Ui.ACCENT : 0xEEFFFFFF, i == tab ? Color.WHITE : Ui.INK, 15);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    tab = t;
                    show();
                }
            });
            tabs.addView(b, Ui.weight(1, Ui.dp(this, 3)));
        }
        list.removeAllViews();
        if (tab == TAB_SKINS) {
            Catalog.Skin cur = Store.skin(this);
            for (final Catalog.Skin s : Catalog.SKINS) {
                ImageView pic = picture();
                final ImageView target = pic;
                App.BG.execute(new Runnable() {
                    @Override public void run() {
                        final Bitmap bmp = Previews.skin(s, Ui.dp(ShopActivity.this, 72));
                        App.ui(new Runnable() {
                            @Override public void run() {
                                target.setImageBitmap(bmp);
                            }
                        });
                    }
                });
                item(pic, s.name, describe(s), Store.SKIN, s.id, s.price, s == cur);
            }
        } else if (tab == TAB_BACKGROUNDS) {
            Catalog.Background cur = Store.background(this);
            for (Catalog.Background g : Catalog.BACKGROUNDS) {
                ImageView pic = picture();
                android.graphics.drawable.GradientDrawable d = Ui.gradient(g);
                d.setCornerRadius(Ui.dp(this, 14));
                pic.setImageDrawable(d);
                item(pic, g.name, g.stars ? "עם כוכבים" : "", Store.BG, g.id, g.price, g == cur);
            }
        } else {
            shapes();
        }
    }

    private String describe(Catalog.Skin s) {
        switch (s.mode) {
            case Catalog.BY_DIRECTION:
                return "צבע לכל כיוון: קל לזהות לאן כל קובייה תזוז";
            case Catalog.BY_HEIGHT:
                return "צבעים לפי גובה";
            case Catalog.BY_RANDOM:
                return "צבעים מעורבים";
            default:
                return "צבע אחד לכל שלב";
        }
    }

    private ImageView picture() {
        ImageView v = new ImageView(this);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return v;
    }

    /** A row: picture, name and note, and the buy / use button. */
    private void item(View pic, String name, String note, final String kind, final String id, final int price,
                      boolean selected) {
        LinearLayout row = row();
        row.addView(pic, new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 72)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        texts.addView(Ui.text(this, name, 18, Ui.INK, true));
        if (note.length() > 0) texts.addView(Ui.text(this, note, 13, Ui.MUTED, false));
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        boolean owned = Store.owns(this, kind, id, price);
        TextView btn;
        if (selected) {
            btn = Ui.button(this, "בשימוש", 0xFFE3F4EA, Ui.GOOD, 14);
        } else if (owned) {
            btn = Ui.button(this, "בחירה", Ui.ACCENT, Color.WHITE, 14);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Store.select(ShopActivity.this, kind, id);
                    Sfx.play(Sfx.HINT);
                    show();
                }
            });
        } else {
            btn = Ui.button(this, "קנייה · " + price, Store.coins(this) >= price ? Ui.GOLD : 0xFFDADDE3, Ui.INK, 14);
            btn.setCompoundDrawablesRelative(null, null, Ui.coinIcon(this, 14), null);
            btn.setCompoundDrawablePadding(Ui.dp(this, 4));
            btn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (Store.buy(ShopActivity.this, kind, id, price)) {
                        Store.select(ShopActivity.this, kind, id);
                        Sfx.play(Sfx.COIN);
                        App.log("bought " + kind + " " + id);
                        show();
                    } else {
                        Toast.makeText(ShopActivity.this, "חסרים " + (price - Store.coins(ShopActivity.this))
                                + " מטבעות. מרוויחים אותם בשלבים.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        row.addView(btn);
        list.addView(row, Ui.lp(-1, -2, Ui.dp(this, 10)));
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Ui.round(0xF2FFFFFF, Ui.dp(this, 18)));
        int p = Ui.dp(this, 12);
        row.setPadding(p, p, p, p);
        return row;
    }

    /** Free play: every shape the campaign has opened, in three sizes. */
    private void shapes() {
        int reached = Store.level(this);
        TextView intro = Ui.text(this, "משחק חופשי: מבנה חדש בכל פעם, בלי מגבלת מהלכים. צורות נפתחות כשמגיעים אליהן בשלבים.",
                14, Ui.onBgMuted(bg), false);
        list.addView(intro);
        final int[] palette = Catalog.SKINS[0].palette;
        for (int i = 0; i < Shapes.COUNT; i++) {
            final int shape = i;
            boolean open = Shapes.INTRO[i] <= reached;
            LinearLayout row = row();
            final ImageView pic = picture();
            if (!open) pic.setAlpha(0.35f);
            App.BG.execute(new Runnable() {
                @Override public void run() {
                    final Bitmap bmp = Previews.shape(shape, Ui.dp(ShopActivity.this, 72), palette[shape % palette.length]);
                    App.ui(new Runnable() {
                        @Override public void run() {
                            pic.setImageBitmap(bmp);
                        }
                    });
                }
            });
            row.addView(pic, new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 72)));
            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(Ui.dp(this, 12), 0, 0, 0);
            texts.addView(Ui.text(this, Shapes.NAMES[i], 18, open ? Ui.INK : Ui.MUTED, true));
            if (open) {
                LinearLayout sizes = new LinearLayout(this);
                String[] labels = {"קטן", "בינוני", "גדול"};
                for (int s = 0; s < 3; s++) {
                    final int size = s;
                    TextView b = Ui.button(this, labels[s], s == 1 ? Ui.ACCENT : 0xFFEEF1F7, s == 1 ? Color.WHITE : Ui.INK, 14);
                    b.setPadding(Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8));
                    b.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            startActivity(GameActivity.free(ShopActivity.this, shape, size));
                        }
                    });
                    sizes.addView(b, Ui.weight(1, Ui.dp(this, 3)));
                }
                texts.addView(sizes, Ui.lp(-1, -2, Ui.dp(this, 8)));
            } else {
                texts.addView(Ui.text(this, "🔒 נפתח בשלב " + Shapes.INTRO[i], 14, Ui.MUTED, false));
            }
            row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
            list.addView(row, Ui.lp(-1, -2, Ui.dp(this, 10)));
        }
    }
}
