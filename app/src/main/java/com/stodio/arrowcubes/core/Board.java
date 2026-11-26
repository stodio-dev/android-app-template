package com.stodio.arrowcubes.core;

/**
 * The puzzle grid: each cell is empty or holds a block with a type and an arrow direction (0..5, see DX/DY/DZ).
 * A block leaves the grid in its arrow's direction only when every cell between it and the edge is empty.
 * Cell (x, y, z) spans [x, x+1] x [y, y+1] x [z, z+1] in grid units; y is up.
 */
public final class Board {
    public static final byte EMPTY = 0, NORMAL = 1, STONE = 2, ICE = 3, KEY = 4, LOCK = 5;
    /** Directions: +x, -x, +y, -y, +z, -z. The opposite of d is d ^ 1. */
    public static final int[] DX = {1, -1, 0, 0, 0, 0};
    public static final int[] DY = {0, 0, 1, -1, 0, 0};
    public static final int[] DZ = {0, 0, 0, 0, 1, -1};

    public final int sx, sy, sz;
    private final byte[] type;
    private final byte[] dir;
    private int removable, keys;

    public Board(int sx, int sy, int sz) {
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        type = new byte[sx * sy * sz];
        dir = new byte[sx * sy * sz];
    }

    public Board copy() {
        Board b = new Board(sx, sy, sz);
        System.arraycopy(type, 0, b.type, 0, type.length);
        System.arraycopy(dir, 0, b.dir, 0, dir.length);
        b.removable = removable;
        b.keys = keys;
        return b;
    }

    public int size() {
        return type.length;
    }

    public int index(int x, int y, int z) {
        return (z * sy + y) * sx + x;
    }

    public int x(int i) {
        return i % sx;
    }

    public int y(int i) {
        return (i / sx) % sy;
    }

    public int z(int i) {
        return i / (sx * sy);
    }

    public boolean inside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz;
    }

    public byte type(int i) {
        return type[i];
    }

    public int dir(int i) {
        return dir[i];
    }

    public boolean occupied(int i) {
        return type[i] != EMPTY;
    }

    /** Blocks that still have to leave (everything but stones). */
    public int removable() {
        return removable;
    }

    /** Gold blocks left; locks open when this reaches 0. */
    public int keys() {
        return keys;
    }

    public int count(byte t) {
        int n = 0;
        for (byte b : type) if (b == t) n++;
        return n;
    }

    public void set(int i, byte t, int d) {
        clear(i);
        type[i] = t;
        dir[i] = (byte) d;
        if (t != EMPTY && t != STONE) removable++;
        if (t == KEY) keys++;
    }

    public void clear(int i) {
        byte t = type[i];
        if (t != EMPTY && t != STONE) removable--;
        if (t == KEY) keys--;
        type[i] = EMPTY;
        dir[i] = 0;
    }

    public void setDir(int i, int d) {
        dir[i] = (byte) d;
    }

    /** Ice becomes a normal block (keeps its direction). */
    public void crack(int i) {
        if (type[i] == ICE) type[i] = NORMAL;
    }

    public boolean isLocked(int i) {
        return type[i] == LOCK && keys > 0;
    }

    /** The first occupied cell from i in direction d (not counting i), or -1 when the way to the edge is clear. */
    public int blocker(int i, int d) {
        int x = x(i) + DX[d], y = y(i) + DY[d], z = z(i) + DZ[d];
        while (inside(x, y, z)) {
            int j = index(x, y, z);
            if (type[j] != EMPTY) return j;
            x += DX[d];
            y += DY[d];
            z += DZ[d];
        }
        return -1;
    }

    /** Empty cells between i and its blocker in direction d, or -1 when the way out is clear. */
    public int gap(int i, int d) {
        int b = blocker(i, d);
        if (b < 0) return -1;
        return Math.abs(x(b) - x(i)) + Math.abs(y(b) - y(i)) + Math.abs(z(b) - z(i)) - 1;
    }

    /** True when the block at i can slide out right now: movable, unlocked and nothing in its way. */
    public boolean canExit(int i) {
        byte t = type[i];
        if (t == EMPTY || t == STONE || isLocked(i)) return false;
        return blocker(i, dir[i]) < 0;
    }

    /**
     * The first occupied cell hit by the ray o + t*d (grid units), or -1. Amanatides-Woo voxel walk, so it costs one
     * step per cell crossed.
     */
    public int raycast(float ox, float oy, float oz, float dx, float dy, float dz) {
        float tmin = 0f, tmax = Float.MAX_VALUE;
        float[] o = {ox, oy, oz}, d = {dx, dy, dz};
        int[] s = {sx, sy, sz};
        for (int a = 0; a < 3; a++) {
            if (Math.abs(d[a]) < 1e-9f) {
                if (o[a] < 0 || o[a] > s[a]) return -1;
            } else {
                float t1 = (0 - o[a]) / d[a], t2 = (s[a] - o[a]) / d[a];
                if (t1 > t2) {
                    float t = t1;
                    t1 = t2;
                    t2 = t;
                }
                if (t1 > tmin) tmin = t1;
                if (t2 < tmax) tmax = t2;
                if (tmin > tmax) return -1;
            }
        }
        float t = tmin + 1e-4f;
        int[] c = new int[3], step = new int[3];
        float[] tMax = new float[3], tDelta = new float[3];
        for (int a = 0; a < 3; a++) {
            float p = o[a] + d[a] * t;
            c[a] = Math.max(0, Math.min(s[a] - 1, (int) Math.floor(p)));
            if (d[a] > 0) {
                step[a] = 1;
                tMax[a] = (c[a] + 1 - o[a]) / d[a];
                tDelta[a] = 1f / d[a];
            } else if (d[a] < 0) {
                step[a] = -1;
                tMax[a] = (c[a] - o[a]) / d[a];
                tDelta[a] = -1f / d[a];
            } else {
                step[a] = 0;
                tMax[a] = Float.MAX_VALUE;
                tDelta[a] = Float.MAX_VALUE;
            }
        }
        for (int guard = 0; guard < sx + sy + sz + 3; guard++) {
            if (c[0] < 0 || c[1] < 0 || c[2] < 0 || c[0] >= sx || c[1] >= sy || c[2] >= sz) return -1;
            int i = index(c[0], c[1], c[2]);
            if (type[i] != EMPTY) return i;
            int a = tMax[0] < tMax[1] ? (tMax[0] < tMax[2] ? 0 : 2) : (tMax[1] < tMax[2] ? 1 : 2);
            c[a] += step[a];
            tMax[a] += tDelta[a];
        }
        return -1;
    }
}
