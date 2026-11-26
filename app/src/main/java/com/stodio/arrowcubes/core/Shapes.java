package com.stodio.arrowcubes.core;

/**
 * The structures the levels are built from. Each shape is a voxel grid built for a size step k (0 = smallest); the
 * campaign introduces them one by one ({@link #INTRO}) and an introduced shape is open in free play.
 */
public final class Shapes {
    private Shapes() {}

    public static final int BOX = 0, PYRAMID = 1, BALL = 2, CROSS = 3, DIAMOND = 4, CYLINDER = 5, STAIRS = 6,
            RING = 7, FRAME = 8, HEART = 9, TREE = 10, TOWER = 11, ARCH = 12, MUSHROOM = 13, HOURGLASS = 14,
            HELIX = 15, STAR = 16, HOUSE = 17, SNOWMAN = 18;

    public static final String[] NAMES = {"קובייה", "פירמידה", "כדור", "צלב", "יהלום", "גליל", "מדרגות", "טבעת",
            "שלד", "לב", "עץ", "מגדל", "קשת", "פטרייה", "שעון חול", "ספירלה", "כוכב", "בית", "איש שלג"};

    /** The campaign level where each shape first appears (and opens in free play). */
    public static final int[] INTRO = {1, 4, 8, 12, 16, 20, 24, 29, 34, 39, 45, 51, 57, 63, 70, 77, 84, 92, 100};

    public static final int COUNT = NAMES.length;
    /** Size steps per shape; larger steps get close to the cell cap. */
    public static final int MAX_K = 24;
    /** No grid side is longer than this, so a ray crosses few cells. */
    public static final int MAX_SIDE = 13;

    /** A voxel shape: on[(z*sy + y)*sx + x]. */
    public static final class Grid {
        public final int sx, sy, sz;
        public final boolean[] on;
        public int cells;

        Grid(int sx, int sy, int sz) {
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            on = new boolean[sx * sy * sz];
        }

        void put(int x, int y, int z) {
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) return;
            int i = (z * sy + y) * sx + x;
            if (!on[i]) {
                on[i] = true;
                cells++;
            }
        }

        public boolean get(int x, int y, int z) {
            return on[(z * sy + y) * sx + x];
        }
    }

    /** The number of shapes open once the player has reached the given campaign level. */
    public static int unlockedCount(int level) {
        int n = 0;
        for (int i = 0; i < COUNT; i++) if (INTRO[i] <= level) n++;
        return n;
    }

    /** The shape at size step k, or null when that step does not exist (too big). */
    public static Grid build(int shape, int k) {
        Grid g = make(shape, k);
        if (g == null || g.sx > MAX_SIDE || g.sy > MAX_SIDE || g.sz > MAX_SIDE || g.cells == 0) return null;
        return g;
    }

    /** The size step whose cell count is closest to the target (within the shape's steps). */
    public static int stepFor(int shape, int target) {
        int best = 0, bestDiff = Integer.MAX_VALUE;
        for (int k = 0; k <= MAX_K; k++) {
            Grid g = build(shape, k);
            if (g == null) break;
            int diff = Math.abs(g.cells - target);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = k;
            }
        }
        return best;
    }

    private static Grid make(int shape, int k) {
        switch (shape) {
            case BOX: {
                int a = 2 + k / 3;
                return fill(new Grid(a + (k % 3 >= 1 ? 1 : 0), a, a + (k % 3 >= 2 ? 1 : 0)), shape);
            }
            case PYRAMID: {
                int n = 3 + 2 * (k / 2) + (k % 2); // 3,4,5,6,...
                return fill(new Grid(n, (n + 1) / 2, n), shape);
            }
            case BALL:
            case DIAMOND:
            case CROSS: {
                int n = 3 + k;
                return fill(new Grid(n, n, n), shape);
            }
            case CYLINDER: {
                int n = 3 + k;
                return fill(new Grid(n, n + 1, n), shape);
            }
            case STAIRS: {
                int n = 3 + k;
                return fill(new Grid(n, n, Math.max(2, (n + 1) / 2)), shape);
            }
            case RING: {
                int n = 6 + k;
                return fill(new Grid(n, n, Math.max(2, n / 4 + 1)), shape);
            }
            case FRAME: {
                int n = 4 + k;
                return fill(new Grid(n, n, n), shape);
            }
            case HEART: {
                int n = 6 + k;
                return fill(new Grid(n, n, Math.max(3, n * 2 / 3)), shape);
            }
            case TREE: {
                int n = 3 + 2 * (k / 2) + (k % 2);
                return fill(new Grid(n, n + 2, n), shape);
            }
            case TOWER: {
                int n = 4 + k / 2, h = 5 + k;
                return fill(new Grid(n, h, n), shape);
            }
            case ARCH: {
                int n = 6 + k;
                return fill(new Grid(n, n / 2 + 1, Math.max(2, n / 3)), shape);
            }
            case MUSHROOM: {
                int n = 5 + k;
                return fill(new Grid(n, n, n), shape);
            }
            case HOURGLASS: {
                int n = 4 + k;
                return fill(new Grid(n, 2 * ((n + 1) / 2) + 1, n), shape);
            }
            case HELIX: {
                int n = 5 + 2 * (k / 3), h = 6 + k;
                return fill(new Grid(n, h, n), shape);
            }
            case STAR: {
                int n = 7 + k;
                return fill(new Grid(n, n, Math.max(2, n / 4 + 1)), shape);
            }
            case HOUSE: {
                int n = 4 + k;
                return fill(new Grid(n, n + n / 2, Math.max(3, n * 3 / 4)), shape);
            }
            case SNOWMAN: {
                int n = 5 + k;
                return fill(new Grid(n, n * 2 + 1, n), shape);
            }
            default:
                return null;
        }
    }

    private static Grid fill(Grid g, int shape) {
        int sx = g.sx, sy = g.sy, sz = g.sz;
        double hx = (sx - 1) / 2.0, hz = (sz - 1) / 2.0;
        if (shape == HELIX) {
            helix(g);
            return g;
        }
        for (int z = 0; z < sz; z++) {
            for (int y = 0; y < sy; y++) {
                for (int x = 0; x < sx; x++) {
                    double cx = x - hx, cy = y - (sy - 1) / 2.0, cz = z - hz;
                    if (inShape(shape, g, x, y, z, cx, cy, cz)) g.put(x, y, z);
                }
            }
        }
        return g;
    }

    private static boolean inShape(int shape, Grid g, int x, int y, int z, double cx, double cy, double cz) {
        int sx = g.sx, sy = g.sy, sz = g.sz;
        switch (shape) {
            case BOX:
                return true;
            case PYRAMID: {
                double half = (sx - 1) / 2.0 - y;
                return Math.abs(cx) <= half + 0.01 && Math.abs(cz) <= half + 0.01;
            }
            case BALL: {
                double r = sx / 2.0;
                return cx * cx + cy * cy + cz * cz <= r * r - 0.2;
            }
            case CROSS: {
                int t = (sx + 1) / 3, m = (sx - t) / 2, hi = m + t;
                int in = 0;
                if (x >= m && x < hi) in++;
                if (y >= m && y < hi) in++;
                if (z >= m && z < hi) in++;
                return in >= 2;
            }
            case DIAMOND: {
                double r = sx / 2.0;
                return Math.abs(cx) + Math.abs(cy) + Math.abs(cz) <= r - 0.3;
            }
            case CYLINDER: {
                double r = sx / 2.0;
                return cx * cx + cz * cz <= r * r - 0.2;
            }
            case STAIRS:
                return y <= x * (sy - 1) / Math.max(1, sx - 1);
            case RING: {
                double outer = sx / 2.0, thick = Math.max(1.2, sx / 5.0), inner = outer - thick;
                double r2 = cx * cx + cy * cy;
                return r2 <= outer * outer - 0.2 && r2 >= inner * inner;
            }
            case FRAME: {
                int edges = 0;
                if (x == 0 || x == sx - 1) edges++;
                if (y == 0 || y == sy - 1) edges++;
                if (z == 0 || z == sz - 1) edges++;
                return edges >= 2;
            }
            case HEART: {
                // two round lobes on a cone that narrows to the tip
                double X = cx / (sx / 2.0), Y = cy / (sy / 2.0), Z = cz / (sz / 2.0) * 0.55;
                double l = 0.5, ly = 0.42, lr = 0.52;
                if ((X - l) * (X - l) + (Y - ly) * (Y - ly) + Z * Z <= lr * lr) return true;
                if ((X + l) * (X + l) + (Y - ly) * (Y - ly) + Z * Z <= lr * lr) return true;
                return Y <= ly && Math.sqrt(X * X + Z * Z * 2) <= (Y + 1.0) * 0.68;
            }
            case TREE: {
                if (y < 2) return Math.abs(cx) <= 0.6 && Math.abs(cz) <= 0.6;
                double r = (sx / 2.0) * (1.0 - (y - 2) / (double) (sy - 2)) + 0.25;
                return Math.abs(cx) + Math.abs(cz) * 0.6 <= r && Math.abs(cz) + Math.abs(cx) * 0.6 <= r;
            }
            case TOWER: {
                boolean wall = x == 0 || z == 0 || x == sx - 1 || z == sz - 1;
                if (y == sy - 1) return wall && (x + z) % 2 == 0;
                if (y == sy - 2) return true;
                return wall && !(y >= 1 && y <= 2 && (Math.abs(cx) < 0.6 && z == 0));
            }
            case ARCH: {
                double outer = sx / 2.0, inner = Math.max(1.0, outer - Math.max(1.5, sx / 4.0));
                double r2 = cx * cx + y * y;
                return r2 <= outer * outer - 0.2 && r2 >= inner * inner;
            }
            case MUSHROOM: {
                int stem = sy / 2;
                if (y < stem) return cx * cx + cz * cz <= Math.max(0.6, sx / 6.0) * Math.max(0.6, sx / 6.0);
                double r = sx / 2.0, h = sy - stem;
                double yy = (y - stem) / h * r;
                return cx * cx + cz * cz + yy * yy <= r * r - 0.2;
            }
            case HOURGLASS: {
                double r = sx / 2.0, half = (sy - 1) / 2.0;
                double rr = 0.7 + (r - 0.7) * Math.abs(cy) / half;
                return cx * cx + cz * cz <= rr * rr;
            }
            case STAR: {
                double r = sx / 2.0;
                return inStar(cx / r, -cy / r);
            }
            case HOUSE: {
                int wallH = sy - (sx + 1) / 2;
                if (y < wallH) return true;
                double half = (sx - 1) / 2.0 - (y - wallH);
                return Math.abs(cx) <= half + 0.01;
            }
            case SNOWMAN: {
                double r1 = sx / 2.0, r2 = sx / 2.0 * 0.72, r3 = sx / 2.0 * 0.5;
                double y1 = r1 - 0.5, y2 = y1 + r1 + r2 - 0.8, y3 = y2 + r2 + r3 - 0.6;
                return ball(cx, y - y1, cz, r1) || ball(cx, y - y2, cz, r2) || ball(cx, y - y3, cz, r3);
            }
            default:
                return false;
        }
    }

    private static boolean ball(double x, double y, double z, double r) {
        return x * x + y * y + z * z <= r * r - 0.15;
    }

    /** A five-pointed star in [-1, 1]^2, point up. */
    private static boolean inStar(double px, double py) {
        double[] xs = new double[10], ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 2 + i * Math.PI / 5;
            double r = i % 2 == 0 ? 1.05 : 0.48;
            xs[i] = Math.cos(a) * r;
            ys[i] = -Math.sin(a) * r + 0.08;
        }
        boolean in = false;
        for (int i = 0, j = 9; i < 10; j = i++) {
            if ((ys[i] > py) != (ys[j] > py) && px < (xs[j] - xs[i]) * (py - ys[i]) / (ys[j] - ys[i]) + xs[i]) in = !in;
        }
        return in;
    }

    /** A spiral staircase: a central pillar and one step per layer, turning 45 degrees each layer. */
    private static void helix(Grid g) {
        int c = g.sx / 2;
        double r = (g.sx - 1) / 2.0;
        for (int y = 0; y < g.sy; y++) {
            g.put(c, y, c);
            double a = y * Math.PI / 4;
            for (double t = 0.5; t <= r + 0.01; t += 0.25) {
                int x = (int) Math.round(c + Math.cos(a) * t), z = (int) Math.round(c + Math.sin(a) * t);
                g.put(x, y, z);
            }
            if (g.sx >= 7) { // wider steps on bigger spirals
                double a2 = a + Math.PI / 8;
                for (double t = 1.0; t <= r + 0.01; t += 0.25) {
                    g.put((int) Math.round(c + Math.cos(a2) * t), y, (int) Math.round(c + Math.sin(a2) * t));
                }
            }
        }
    }
}
