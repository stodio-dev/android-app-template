package com.stodio.arrowcubes;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stodio.arrowcubes.core.Catalog;
import com.stodio.arrowcubes.core.Levels;

/** All campaign levels in a grid with their stars; the ones not reached yet are locked. */
public final class LevelsActivity extends Activity {
    private BaseAdapter adapter;
    private TextView total;
    private String stars = "";
    private int reached = 1;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Catalog.Background bg = Store.background(this);
        Ui.bars(this, bg);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        col.setBackground(Ui.gradient(bg));
        int pad = Ui.dp(this, 16);
        col.setPadding(pad, pad, pad, 0);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(Ui.text(this, "שלבים", 26, Ui.onBg(bg), true), new LinearLayout.LayoutParams(0, -2, 1));
        total = Ui.text(this, "", 16, Ui.INK, true);
        total.setBackground(Ui.round(0xEEFFFFFF, Ui.dp(this, 16)));
        total.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        top.addView(total);
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

        final GridView grid = new GridView(this);
        grid.setNumColumns(5);
        grid.setVerticalSpacing(Ui.dp(this, 8));
        grid.setHorizontalSpacing(Ui.dp(this, 8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        grid.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 16));
        grid.setClipToPadding(false);
        final int tileH = Ui.dp(this, 66);
        adapter = new BaseAdapter() {
            @Override public int getCount() {
                return Levels.COUNT;
            }

            @Override public Object getItem(int i) {
                return i + 1;
            }

            @Override public long getItemId(int i) {
                return i + 1;
            }

            @Override public View getView(int i, View old, ViewGroup parent) {
                TextView t = old instanceof TextView ? (TextView) old : new TextView(LevelsActivity.this);
                t.setLayoutParams(new AbsListView.LayoutParams(-1, tileH));
                t.setGravity(Gravity.CENTER);
                int level = i + 1, s = Catalog.starsOf(stars, level);
                boolean open = level <= reached, current = level == reached;
                t.setTextSize(16);
                t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                if (!open) {
                    t.setText(level + "\n🔒");
                    t.setTextColor(0x99000000);
                    t.setBackground(Ui.round(0x66FFFFFF, Ui.dp(LevelsActivity.this, 14)));
                } else if (current) {
                    t.setText(level + "\nשחקו");
                    t.setTextColor(Color.WHITE);
                    t.setBackground(Ui.pressable(LevelsActivity.this, Ui.ACCENT, 14));
                } else {
                    t.setText(level + "\n" + Ui.stars(s));
                    t.setTextColor(Ui.INK);
                    t.setBackground(Ui.pressable(LevelsActivity.this, 0xF2FFFFFF, 14));
                }
                return t;
            }
        };
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                if (pos + 1 <= reached) startActivity(GameActivity.campaign(LevelsActivity.this, pos + 1));
            }
        });
        col.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(col);
        load();
        grid.setSelection(Math.max(0, reached - 1 - 10));
    }

    @Override protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        stars = Store.stars(this);
        reached = Store.level(this);
        total.setText("★ " + Catalog.totalStars(stars));
        adapter.notifyDataSetChanged();
    }
}
