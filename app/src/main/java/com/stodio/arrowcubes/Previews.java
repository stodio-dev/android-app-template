package com.stodio.arrowcubes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import com.stodio.arrowcubes.core.Catalog;
import com.stodio.arrowcubes.core.Mesh;
import com.stodio.arrowcubes.core.Shapes;

/** Small isometric pictures for the shop: a skin's block, and a shape built of plain cubes. Run off the UI thread. */
final class Previews {
    private Previews() {}

    private static final float COS30 = 0.8660254f;

    /** One block of the skin, three faces: arrows on top and right, the "coming at you" dot on the left. */
    static Bitmap skin(Catalog.Skin s, int size) {
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        int color = Catalog.blockColor(s, 7L, 0, 0, 0, 1, 2);
        Bitmap arrow = Atlas.face(s, Mesh.ARROW, color), dot = Atlas.face(s, Mesh.FRONT, color);
        float e = size * 0.45f, w = e * COS30, h = e / 2f, cx = size / 2f, cy = size * 0.5f;
        float[] T = {cx, cy - 2 * h}, R = {cx + w, cy - h}, C = {cx, cy}, L = {cx - w, cy - h};
        float[] Ld = {cx - w, cy - h + e}, Cd = {cx, cy + e}, Rd = {cx + w, cy - h + e};
        face(c, arrow, T, R, L, 1f);
        face(c, dot, L, C, Ld, 0.86f);
        face(c, arrow, C, R, Cd, 0.68f);
        arrow.recycle();
        dot.recycle();
        return out;
    }

    /** Maps the square face bitmap onto the parallelogram (topLeft, topRight, bottomLeft), darkened by shade. */
    private static void face(Canvas c, Bitmap b, float[] tl, float[] tr, float[] bl, float shade) {
        Matrix m = new Matrix();
        float n = b.getWidth();
        m.setPolyToPoly(new float[]{0, 0, n, 0, 0, n}, 0, new float[]{tl[0], tl[1], tr[0], tr[1], bl[0], bl[1]}, 0, 3);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        int k = Math.round(255 * shade);
        p.setColorFilter(new LightingColorFilter(0xFF000000 | (k << 16) | (k << 8) | k, 0));
        c.drawBitmap(b, m, p);
    }

    /** The shape at a medium size as isometric cubes, in one color with three shades. */
    static Bitmap shape(int shape, int size, int color) {
        Shapes.Grid g = Shapes.build(shape, Shapes.stepFor(shape, 140));
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int z = 0; z < g.sz; z++) {
            for (int y = 0; y < g.sy; y++) {
                for (int x = 0; x < g.sx; x++) {
                    if (!g.get(x, y, z)) continue;
                    for (int k = 0; k < 8; k++) {
                        float[] p = iso(x + (k & 1), y + ((k >> 1) & 1), z + ((k >> 2) & 1), 1, 0, 0);
                        minX = Math.min(minX, p[0]);
                        maxX = Math.max(maxX, p[0]);
                        minY = Math.min(minY, p[1]);
                        maxY = Math.max(maxY, p[1]);
                    }
                }
            }
        }
        float scale = size * 0.9f / Math.max(maxX - minX, maxY - minY);
        float ox = size / 2f - (minX + maxX) / 2f * scale, oy = size / 2f - (minY + maxY) / 2f * scale;
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint top = fill(Catalog.mix(color, 0xFFFFFF, 0.15f)), right = fill(color), left = fill(Catalog.mix(color, 0x000000, 0.25f));
        Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(Math.max(1f, scale * 0.06f));
        edge.setColor(0xFF000000 | Catalog.mix(color, 0x000000, 0.45f));
        // far to near: smaller x+y+z first
        int maxSum = g.sx + g.sy + g.sz;
        for (int sum = 0; sum <= maxSum; sum++) {
            for (int y = 0; y < g.sy; y++) {
                for (int x = 0; x < g.sx; x++) {
                    int z = sum - x - y;
                    if (z < 0 || z >= g.sz || !g.get(x, y, z)) continue;
                    if (y + 1 >= g.sy || !g.get(x, y + 1, z)) {
                        quad(c, top, edge, scale, ox, oy, x, y + 1, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, x, y + 1, z + 1);
                    }
                    if (x + 1 >= g.sx || !g.get(x + 1, y, z)) {
                        quad(c, right, edge, scale, ox, oy, x + 1, y, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, x + 1, y, z + 1);
                    }
                    if (z + 1 >= g.sz || !g.get(x, y, z + 1)) {
                        quad(c, left, edge, scale, ox, oy, x, y, z + 1, x + 1, y, z + 1, x + 1, y + 1, z + 1, x, y + 1, z + 1);
                    }
                }
            }
        }
        return out;
    }

    private static Paint fill(int rgb) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF000000 | rgb);
        return p;
    }

    private static float[] iso(float x, float y, float z, float s, float ox, float oy) {
        return new float[]{ox + (x - z) * COS30 * s, oy + (x + z) * 0.5f * s - y * s};
    }

    private static void quad(Canvas c, Paint fill, Paint edge, float s, float ox, float oy, float... v) {
        Path p = new Path();
        for (int i = 0; i < 4; i++) {
            float[] q = iso(v[i * 3], v[i * 3 + 1], v[i * 3 + 2], s, ox, oy);
            if (i == 0) p.moveTo(q[0], q[1]);
            else p.lineTo(q[0], q[1]);
        }
        p.close();
        c.drawPath(p, fill);
        c.drawPath(p, edge);
    }
}
