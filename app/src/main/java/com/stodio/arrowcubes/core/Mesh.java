package com.stodio.arrowcubes.core;

/**
 * Block geometry for the renderer, as plain floats so it can be tested off the phone. Each vertex is 11 floats:
 * position (grid units), normal, texture coordinates, color (0..1). Faces are two counter-clockwise triangles seen
 * from outside, and the texture's "up" on a side face points along the block's arrow.
 */
public final class Mesh {
    private Mesh() {}

    public static final int FLOATS = 11, BLOCK_FLOATS = FLOATS * 36;

    /** Atlas cells: a 4x4 grid of square faces. */
    public static final int ARROW = 0, FRONT = 1, BACK = 2, STONE = 3, LOCK = 4, ICE_ARROW = 5, ICE_FRONT = 6,
            ICE_BACK = 7, KEY_ARROW = 8, KEY_FRONT = 9, KEY_BACK = 10, CELLS = 11;
    public static final int ATLAS = 1024, CELL = 256, COLS = 4;

    private static final float[] AS = {-1, 1, 1, -1}, BS = {-1, -1, 1, 1};
    private static final int[] TRI = {0, 1, 2, 0, 2, 3};
    private static final float[][] UV = new float[16][];

    static {
        for (int i = 0; i < UV.length; i++) UV[i] = uv(i);
    }

    /** Texture coordinates of a cell, inset so mipmaps bleed less: {s0, t0, s1, t1}; t0 is the top of the cell. */
    public static float[] uv(int cell) {
        float in = 3f / ATLAS;
        float s0 = (cell % COLS) * CELL / (float) ATLAS, t0 = (cell / COLS) * CELL / (float) ATLAS;
        return new float[]{s0 + in, t0 + in, s0 + CELL / (float) ATLAS - in, t0 + CELL / (float) ATLAS - in};
    }

    /** The atlas cell for face f (a direction) of block i. */
    public static int faceCell(Board b, int i, int f) {
        byte type = b.type(i);
        int d = b.dir(i);
        if (type == Board.STONE) return STONE;
        if (b.isLocked(i)) return LOCK;
        if (type == Board.ICE) return f == d ? ICE_FRONT : f == (d ^ 1) ? ICE_BACK : ICE_ARROW;
        if (type == Board.KEY) return f == d ? KEY_FRONT : f == (d ^ 1) ? KEY_BACK : KEY_ARROW;
        return f == d ? FRONT : f == (d ^ 1) ? BACK : ARROW;
    }

    public static int color(Board b, int i, Catalog.Skin skin, long seed) {
        byte type = b.type(i);
        if (type == Board.STONE) return Catalog.STONE_COLOR;
        if (type == Board.KEY) return Catalog.KEY_COLOR;
        if (b.isLocked(i)) return Catalog.LOCK_COLOR;
        int c = Catalog.blockColor(skin, seed, b.x(i), b.y(i), b.z(i), b.sy, b.dir(i));
        if (type == Board.ICE) c = Catalog.mix(c, Catalog.ICE_COLOR, 0.72f);
        return c;
    }

    /**
     * Appends block i's faces to out at n and returns the new length. With hidden != null, faces against a neighbour
     * that is on the board and not hidden (not drawn separately) are left out.
     */
    public static int appendBlock(float[] out, int n, Board b, int i, boolean[] hidden, Catalog.Skin skin, long seed) {
        int x = b.x(i), y = b.y(i), z = b.z(i);
        int d = b.dir(i);
        int color = color(b, i, skin, seed);
        float r = ((color >> 16) & 255) / 255f, g = ((color >> 8) & 255) / 255f, bl = (color & 255) / 255f;
        float cx = x + 0.5f, cy = y + 0.5f, cz = z + 0.5f;
        boolean turns = b.type(i) != Board.STONE && !b.isLocked(i);
        for (int f = 0; f < 6; f++) {
            if (hidden != null) {
                int nx = x + Board.DX[f], ny = y + Board.DY[f], nz = z + Board.DZ[f];
                if (b.inside(nx, ny, nz)) {
                    int j = b.index(nx, ny, nz);
                    if (b.occupied(j) && !hidden[j]) continue;
                }
            }
            int cell = faceCell(b, i, f);
            boolean side = f != d && f != (d ^ 1);
            float nxf = Board.DX[f], nyf = Board.DY[f], nzf = Board.DZ[f];
            // e1 is the texture's "up" (where the arrow points), e2 = n x e1, so (e1, e2) runs counter-clockwise
            float e1x, e1y, e1z;
            if (side && turns) {
                e1x = Board.DX[d];
                e1y = Board.DY[d];
                e1z = Board.DZ[d];
            } else if (f == 2 || f == 3) { // top and bottom: up is -z
                e1x = 0;
                e1y = 0;
                e1z = -1;
            } else {
                e1x = 0;
                e1y = 1;
                e1z = 0;
            }
            float e2x = nyf * e1z - nzf * e1y, e2y = nzf * e1x - nxf * e1z, e2z = nxf * e1y - nyf * e1x;
            float[] uv = UV[cell];
            float fx = cx + nxf * 0.5f, fy = cy + nyf * 0.5f, fz = cz + nzf * 0.5f;
            for (int k : TRI) {
                float a = AS[k] * 0.5f, bb = BS[k] * 0.5f;
                out[n++] = fx + e1x * a + e2x * bb;
                out[n++] = fy + e1y * a + e2y * bb;
                out[n++] = fz + e1z * a + e2z * bb;
                out[n++] = nxf;
                out[n++] = nyf;
                out[n++] = nzf;
                out[n++] = BS[k] < 0 ? uv[0] : uv[2];
                out[n++] = AS[k] > 0 ? uv[1] : uv[3];
                out[n++] = r;
                out[n++] = g;
                out[n++] = bl;
            }
        }
        return n;
    }

    /**
     * Every block on the board that is not hidden, without the faces between neighbours. Writes into buf[0], growing
     * it when needed, and returns the float count.
     */
    public static int build(Board b, boolean[] hidden, Catalog.Skin skin, long seed, float[][] buf) {
        if (hidden == null) hidden = new boolean[b.size()];
        float[] out = buf[0];
        int n = 0;
        for (int i = 0; i < b.size(); i++) {
            if (!b.occupied(i) || hidden[i]) continue;
            if (out.length < n + BLOCK_FLOATS) {
                float[] bigger = new float[Math.max(out.length * 2, n + BLOCK_FLOATS)];
                System.arraycopy(out, 0, bigger, 0, n);
                out = bigger;
            }
            n = appendBlock(out, n, b, i, hidden, skin, seed);
        }
        buf[0] = out;
        return n;
    }

    /** All six faces of one block, for drawing it on its own. */
    public static float[] block(Board b, int i, Catalog.Skin skin, long seed) {
        float[] out = new float[BLOCK_FLOATS];
        appendBlock(out, 0, b, i, null, skin, seed);
        return out;
    }
}
