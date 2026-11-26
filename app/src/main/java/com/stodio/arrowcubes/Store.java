package com.stodio.arrowcubes;

import android.content.Context;
import android.content.SharedPreferences;

import com.stodio.arrowcubes.core.Catalog;
import com.stodio.arrowcubes.core.Levels;

/** Progress, coins and the shop, kept in the app's private preferences. */
final class Store {
    private Store() {}

    static final String SKIN = "skin", BG = "bg";

    private static SharedPreferences p(Context c) {
        return App.prefs(c);
    }

    /** The highest level the player may play (the first one not yet finished). */
    static int level(Context c) {
        return Math.max(1, Math.min(Levels.COUNT, p(c).getInt("level", 1)));
    }

    static String stars(Context c) {
        return p(c).getString("stars", "");
    }

    static int stars(Context c, int level) {
        return Catalog.starsOf(stars(c), level);
    }

    static int coins(Context c) {
        return p(c).getInt("coins", Catalog.START_COINS);
    }

    static void addCoins(Context c, int n) {
        p(c).edit().putInt("coins", Math.max(0, coins(c) + n)).apply();
    }

    static boolean spend(Context c, int n) {
        int have = coins(c);
        if (have < n) return false;
        p(c).edit().putInt("coins", have - n).apply();
        return true;
    }

    /** Records a finished campaign level and returns the coins it paid. */
    static int finish(Context c, int level, int stars) {
        int before = stars(c, level);
        int coins = Catalog.reward(level, stars, before);
        SharedPreferences.Editor e = p(c).edit();
        e.putString("stars", Catalog.withStars(stars(c), level, stars));
        if (level >= level(c)) e.putInt("level", Math.min(Levels.COUNT, level + 1));
        e.putInt("coins", coins(c) + coins);
        e.apply();
        return coins;
    }

    static boolean owns(Context c, String kind, String id, int price) {
        return price == 0 || p(c).getBoolean("own_" + kind + "_" + id, false);
    }

    static boolean buy(Context c, String kind, String id, int price) {
        if (owns(c, kind, id, price)) return true;
        if (!spend(c, price)) return false;
        p(c).edit().putBoolean("own_" + kind + "_" + id, true).apply();
        return true;
    }

    static void select(Context c, String kind, String id) {
        p(c).edit().putString("sel_" + kind, id).apply();
    }

    static Catalog.Skin skin(Context c) {
        return Catalog.skin(p(c).getString("sel_" + SKIN, Catalog.SKINS[0].id));
    }

    static Catalog.Background background(Context c) {
        return Catalog.background(p(c).getString("sel_" + BG, Catalog.BACKGROUNDS[0].id));
    }

    static boolean sound(Context c) {
        return p(c).getBoolean("sound", true);
    }

    static void setSound(Context c, boolean on) {
        p(c).edit().putBoolean("sound", on).apply();
    }

    /** True the first time a level's tip is asked for. */
    static boolean firstTip(Context c, int level) {
        String k = "tip_" + level;
        if (p(c).getBoolean(k, false)) return false;
        p(c).edit().putBoolean(k, true).apply();
        return true;
    }
}
