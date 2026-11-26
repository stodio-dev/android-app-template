package com.stodio.arrowcubes.core;

/** Skins, backgrounds, prices and rewards, plus the block colors a skin gives. Colors are 0xRRGGBB. */
public final class Catalog {
    private Catalog() {}

    public static final int HINT_PRICE = 15, HAMMER_PRICE = 30, MOVES_PRICE = 40, MOVES_BONUS = 5;
    public static final int START_COINS = 100;

    /** How a skin colors the blocks. */
    public static final int BY_LEVEL = 0, BY_DIRECTION = 1, BY_HEIGHT = 2, BY_RANDOM = 3;
    /** Face patterns. */
    public static final int PLAIN = 0, WOOD = 1, MARBLE = 2, GLASS = 3;
    /** Arrow glyphs. */
    public static final int ARROW = 0, CHEVRON = 1, PIXEL = 2, THIN = 3;

    public static final int KEY_COLOR = 0xF5BE2E, LOCK_COLOR = 0x4F5967, ICE_COLOR = 0xC4ECFA, STONE_COLOR = 0x8A8580;

    public static final class Skin {
        public final String id, name;
        public final int price, mode, pattern, arrow;
        public final int[] palette;
        public final int glyph, accent;
        /** Corner radius and border width of the face tile, as shares of the face. */
        public final float corner, border, shine;

        Skin(String id, String name, int price, int mode, int[] palette, int glyph, int accent, int pattern, int arrow,
             float corner, float border, float shine) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.mode = mode;
            this.palette = palette;
            this.glyph = glyph;
            this.accent = accent;
            this.pattern = pattern;
            this.arrow = arrow;
            this.corner = corner;
            this.border = border;
            this.shine = shine;
        }
    }

    public static final class Background {
        public final String id, name;
        public final int price, top, bottom;
        public final boolean stars, dark;

        Background(String id, String name, int price, int top, int bottom, boolean stars, boolean dark) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.top = top;
            this.bottom = bottom;
            this.stars = stars;
            this.dark = dark;
        }
    }

    public static final Skin[] SKINS = {
            new Skin("classic", "קלאסי", 0, BY_LEVEL,
                    new int[]{0x5B8DEF, 0xF2716B, 0x47C28F, 0xF5A742, 0x9B7BEA, 0x3DBAD1, 0xEC6FA9},
                    0xFFFFFF, 0x000000, PLAIN, ARROW, 0.16f, 0.05f, 0.25f),
            new Skin("compass", "מצפן", 120, BY_DIRECTION,
                    new int[]{0xE8574F, 0x1FB3A5, 0x79BF3F, 0x2F7FD8, 0x8D6BE0, 0xE45BA8},
                    0xFFFFFF, 0x000000, PLAIN, ARROW, 0.16f, 0.05f, 0.25f),
            new Skin("rainbow", "קשת", 150, BY_HEIGHT,
                    new int[]{0x8E5CE6, 0x4A7FF0, 0x2FB8C9, 0x46C46E, 0xF2C433, 0xF58A3A, 0xEC4F5A},
                    0xFFFFFF, 0x000000, PLAIN, CHEVRON, 0.2f, 0.05f, 0.3f),
            new Skin("candy", "ממתקים", 200, BY_RANDOM,
                    new int[]{0xFFB3C7, 0xB5E8D5, 0xFFE3A3, 0xC9C1FF, 0xA8DBFF, 0xFFC8A8},
                    0x6B4A5E, 0x000000, PLAIN, CHEVRON, 0.32f, 0.03f, 0.45f),
            new Skin("neon", "ניאון", 250, BY_LEVEL,
                    new int[]{0x1C2340, 0x231C40, 0x1C3340},
                    0x5CF2FF, 0xFF4FD8, GLASS, THIN, 0.14f, 0.06f, 0.6f),
            new Skin("wood", "עץ", 250, BY_RANDOM,
                    new int[]{0xC08A55, 0xB27D4A, 0xCC9966, 0xA8733F},
                    0x4A2E17, 0x000000, WOOD, ARROW, 0.08f, 0.05f, 0.1f),
            new Skin("pixel", "פיקסלים", 200, BY_DIRECTION,
                    new int[]{0xE04848, 0xF0A030, 0x40B048, 0x3088E0, 0x9050D0, 0xE050A0},
                    0xFFFFFF, 0x000000, PLAIN, PIXEL, 0.0f, 0.08f, 0.0f),
            new Skin("marble", "שיש", 350, BY_LEVEL,
                    new int[]{0xF2F0EB, 0xE9EEF2, 0xF2ECEC},
                    0x2E2B29, 0x000000, MARBLE, THIN, 0.1f, 0.04f, 0.55f),
            new Skin("gold", "זהב", 500, BY_LEVEL,
                    new int[]{0xE8B53A},
                    0x5A3A0C, 0x000000, PLAIN, ARROW, 0.12f, 0.05f, 0.9f),
    };

    public static final Background[] BACKGROUNDS = {
            new Background("sky", "שמיים", 0, 0xB9DDFB, 0xF2F8FF, false, false),
            new Background("mint", "מנטה", 80, 0x9FE0C6, 0xEDFBF5, false, false),
            new Background("sunset", "שקיעה", 100, 0xFF9473, 0xFFE0A8, false, false),
            new Background("lavender", "לבנדר", 100, 0xBFAEE3, 0xF3EEFF, false, false),
            new Background("rose", "ורד", 120, 0xF5A9BE, 0xFFEAF0, false, false),
            new Background("ocean", "אוקיינוס", 150, 0x0F5C86, 0x46B7D8, false, true),
            new Background("forest", "יער", 150, 0x1E4A2D, 0x76AE6A, false, true),
            new Background("night", "לילה", 200, 0x07142E, 0x2A4673, true, true),
            new Background("space", "חלל", 300, 0x03020A, 0x221338, true, true),
    };

    public static Skin skin(String id) {
        for (Skin s : SKINS) if (s.id.equals(id)) return s;
        return SKINS[0];
    }

    public static Background background(String id) {
        for (Background b : BACKGROUNDS) if (b.id.equals(id)) return b;
        return BACKGROUNDS[0];
    }

    /** The color of a block at (x, y, z) in a grid sy high, pointing dir, for a level identified by seed. */
    public static int blockColor(Skin s, long seed, int x, int y, int z, int sy, int dir) {
        int[] p = s.palette;
        int c;
        switch (s.mode) {
            case BY_DIRECTION:
                c = p[dir % p.length];
                break;
            case BY_HEIGHT: {
                float t = sy <= 1 ? 0 : y / (float) (sy - 1) * (p.length - 1);
                int a = Math.min(p.length - 1, (int) t);
                int b = Math.min(p.length - 1, a + 1);
                c = mix(p[a], p[b], t - a);
                break;
            }
            case BY_RANDOM: {
                int h = (int) (seed ^ (seed >>> 32)) * 31 + x * 73856093 ^ y * 19349663 ^ z * 83492791;
                h ^= h >>> 13;
                h *= 0x5bd1e995;
                h ^= h >>> 15;
                c = p[(h & 0x7fffffff) % p.length];
                break;
            }
            default: {
                int idx = (int) ((seed ^ (seed >>> 29)) & 0x7fffffff) % p.length;
                c = p[idx];
            }
        }
        // a light checker so neighbouring faces read as separate blocks
        if (((x + y + z) & 1) == 1) c = mix(c, 0x000000, 0.06f);
        return c;
    }

    public static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Coins for finishing a campaign level, given the stars it had before (0 = first time). */
    public static int reward(int level, int stars, int before) {
        if (before == 0) return 10 + 5 * stars + level / 10;
        if (stars > before) return 5 * (stars - before);
        return 2;
    }

    /** Coins for a free-play structure. */
    public static int freeReward(int cells) {
        return 3 + cells / 30;
    }

    /** Stars per level packed as digits, level 1 first: "3312". */
    public static int starsOf(String packed, int level) {
        if (packed == null || level < 1 || level > packed.length()) return 0;
        int c = packed.charAt(level - 1) - '0';
        return c >= 0 && c <= 3 ? c : 0;
    }

    public static String withStars(String packed, int level, int stars) {
        StringBuilder b = new StringBuilder(packed == null ? "" : packed);
        while (b.length() < level) b.append('0');
        if (stars > starsOf(packed, level)) b.setCharAt(level - 1, (char) ('0' + stars));
        return b.toString();
    }

    public static int totalStars(String packed) {
        int n = 0;
        if (packed != null) for (int i = 0; i < packed.length(); i++) n += Math.max(0, Math.min(3, packed.charAt(i) - '0'));
        return n;
    }
}
