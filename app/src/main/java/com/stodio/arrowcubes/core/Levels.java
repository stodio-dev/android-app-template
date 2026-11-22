package com.stodio.arrowcubes.core;

import java.util.Random;

/**
 * The campaign: every level is generated from its number, so level n is the same on every phone and every replay.
 * Structures grow from 8 blocks to about 600, move limits start at level 6 and tighten, and ice (15), stones (25) and
 * locks with gold keys (35) join in.
 */
public final class Levels {
    private Levels() {}

    public static final int COUNT = 600;
    public static final int LIMIT_FROM = 6, ICE_FROM = 15, STONE_FROM = 25, LOCK_FROM = 35;

    public static final class Spec {
        /** Campaign level, or 0 in free play. */
        public int level;
        public int shape, step;
        public int stones, ice, keys, locks;
        public float difficulty;
        /** Spare taps as a share of the required ones; negative for no move limit. */
        public float slack = -1;
        public long seed;
    }

    /** Roughly how many blocks level n has. */
    public static int targetCells(int level) {
        if (level <= 1) return 8;
        if (level == 2) return 12;
        if (level == 3) return 18;
        if (level <= 10) return 8 + (level - 1) * 6;
        return (int) Math.round(600 - 545 * Math.exp(-(level - 10) / 110.0));
    }

    public static Spec campaign(int level) {
        Random r = new Random(level * 7919L + 104729L);
        Spec s = new Spec();
        s.level = level;
        s.shape = -1;
        for (int i = 0; i < Shapes.COUNT; i++) if (Shapes.INTRO[i] == level) s.shape = i;
        if (s.shape < 0) s.shape = r.nextInt(Shapes.unlockedCount(level));
        float target = targetCells(level);
        if (level > 10) {
            if (level % 5 == 0) target *= 0.7f; // a lighter level every five
            target *= 0.88f + r.nextFloat() * 0.24f;
        }
        s.step = Shapes.stepFor(s.shape, Math.round(target));
        int cells = Shapes.build(s.shape, s.step).cells;
        s.difficulty = Math.min(0.95f, 0.08f + level / 90f + (r.nextFloat() - 0.5f) * 0.1f);
        if (level >= LIMIT_FROM) s.slack = (float) (0.05 + 0.5 * Math.exp(-(level - LIMIT_FROM) / 40.0));
        boolean ice = level == ICE_FROM || (level > ICE_FROM && r.nextFloat() < 0.55f);
        boolean stone = level == STONE_FROM || (level > STONE_FROM && r.nextFloat() < 0.5f);
        boolean lock = level == LOCK_FROM || (level > LOCK_FROM && r.nextFloat() < 0.4f);
        if (ice) s.ice = Math.max(2, Math.round(cells * (0.04f + Math.min(0.08f, (level - ICE_FROM) * 0.0015f))));
        if (stone) s.stones = Math.max(2, Math.round(cells * (0.03f + Math.min(0.05f, (level - STONE_FROM) * 0.001f))));
        if (lock) {
            s.keys = 1 + Math.min(3, (level - LOCK_FROM) / 40);
            s.locks = 2 + Math.min(6, (level - LOCK_FROM) / 25);
        }
        s.seed = r.nextLong();
        return s;
    }

    /** Free play sizes: 0 small, 1 medium, 2 large. */
    public static Spec free(int shape, int size, long seed) {
        Spec s = new Spec();
        s.shape = shape;
        int[] targets = {40, 130, 300};
        s.step = Shapes.stepFor(shape, targets[Math.max(0, Math.min(2, size))]);
        s.difficulty = 0.3f + size * 0.25f;
        s.seed = seed;
        Random r = new Random(seed);
        int cells = Shapes.build(shape, s.step).cells;
        if (r.nextBoolean()) s.ice = Math.max(1, cells / 20);
        if (size > 0 && r.nextBoolean()) s.stones = Math.max(1, cells / 30);
        return s;
    }

    public static Game build(Spec s) {
        Shapes.Grid g = Shapes.build(s.shape, s.step);
        LevelGen.Result res = LevelGen.generate(g, s.stones, s.ice, s.keys, s.locks, s.difficulty, new Random(s.seed));
        Board b = res.board;
        int required = b.removable() + b.count(Board.ICE);
        int limit = s.slack < 0 ? 0 : required + Math.max(3, Math.round(required * s.slack));
        return new Game(b, limit);
    }

    /** A line shown when a level brings something new, or null. */
    public static String tip(int level) {
        switch (level) {
            case 1:
                return "הקישו על קובייה: היא תחליק החוצה בכיוון החץ — אבל רק אם הדרך פנויה.";
            case 2:
                return "גררו אצבע כדי לסובב את המבנה, וצבטו כדי להתקרב או להתרחק.";
            case 3:
                return "העיגול על הפאה אומר שהקובייה תצא לכיוונכם.";
            case LIMIT_FROM:
                return "מעכשיו יש מגבלת מהלכים: הקשה על קובייה חסומה מבזבזת מהלך.";
            case ICE_FROM:
                return "קוביות קרח: ההקשה הראשונה שוברת את הקרח, השנייה משחררת.";
            case STONE_FROM:
                return "אבנים לא זזות ולא צריך להוציא אותן — אבל הן חוסמות את הדרך.";
            case LOCK_FROM:
                return "קוביות נעולות נפתחות רק אחרי שכל הקוביות המוזהבות יצאו.";
            default:
                for (int i = 1; i < Shapes.COUNT; i++) {
                    if (Shapes.INTRO[i] == level) return "צורה חדשה: " + Shapes.NAMES[i] + "! היא נפתחה גם במשחק החופשי.";
                }
                return null;
        }
    }
}
