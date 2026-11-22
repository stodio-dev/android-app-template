package com.stodio.arrowcubes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import com.stodio.arrowcubes.core.Catalog;
import com.stodio.arrowcubes.core.Mesh;

import java.util.Random;

/**
 * The face texture of a skin: a 4x4 grid of 256px faces. Each color channel is a mask the block shader mixes:
 * R = tile brightness (times the block color), G = glyph (the skin's glyph color), B = accent (the skin's accent).
 * Arrows point to the top of their cell; the renderer turns each face's texture so the arrow follows the block.
 */
final class Atlas {
    static final int SIZE = Mesh.ATLAS, CELL = Mesh.CELL, COLS = Mesh.COLS;
    static final int ARROW = Mesh.ARROW, FRONT = Mesh.FRONT, BACK = Mesh.BACK, STONE = Mesh.STONE, LOCK = Mesh.LOCK,
            ICE_ARROW = Mesh.ICE_ARROW, ICE_FRONT = Mesh.ICE_FRONT, ICE_BACK = Mesh.ICE_BACK, KEY_ARROW = Mesh.KEY_ARROW,
            KEY_FRONT = Mesh.KEY_FRONT, KEY_BACK = Mesh.KEY_BACK;

    private Atlas() {}

    static Bitmap build(Catalog.Skin skin) {
        Bitmap lum = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Bitmap glyph = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Bitmap accent = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas cl = new Canvas(lum), cg = new Canvas(glyph), ca = new Canvas(accent);
        cl.drawColor(Color.BLACK);
        cg.drawColor(Color.BLACK);
        ca.drawColor(Color.BLACK);
        for (int cell = 0; cell <= KEY_BACK; cell++) {
            int ox = (cell % COLS) * CELL, oy = (cell / COLS) * CELL;
            cl.save();
            cl.translate(ox, oy);
            cg.save();
            cg.translate(ox, oy);
            ca.save();
            ca.translate(ox, oy);
            drawCell(cell, skin, cl, cg, ca);
            cl.restore();
            cg.restore();
            ca.restore();
        }
        int n = SIZE * SIZE;
        int[] r = new int[n], g = new int[n], b = new int[n];
        lum.getPixels(r, 0, SIZE, 0, 0, SIZE, SIZE);
        glyph.getPixels(g, 0, SIZE, 0, 0, SIZE, SIZE);
        accent.getPixels(b, 0, SIZE, 0, 0, SIZE, SIZE);
        lum.recycle();
        glyph.recycle();
        accent.recycle();
        for (int i = 0; i < n; i++) {
            r[i] = 0xFF000000 | (((r[i] >> 16) & 255) << 16) | (((g[i] >> 16) & 255) << 8) | ((b[i] >> 16) & 255);
        }
        return Bitmap.createBitmap(r, SIZE, SIZE, Bitmap.Config.ARGB_8888);
    }

    /** One face in the skin's colors (for the shop's previews), the same masks mixed the way the shader does. */
    static Bitmap face(Catalog.Skin skin, int cell, int color) {
        Bitmap lum = Bitmap.createBitmap(CELL, CELL, Bitmap.Config.ARGB_8888);
        Bitmap glyph = Bitmap.createBitmap(CELL, CELL, Bitmap.Config.ARGB_8888);
        Bitmap accent = Bitmap.createBitmap(CELL, CELL, Bitmap.Config.ARGB_8888);
        Canvas cl = new Canvas(lum), cg = new Canvas(glyph), ca = new Canvas(accent);
        cl.drawColor(Color.BLACK);
        cg.drawColor(Color.BLACK);
        ca.drawColor(Color.BLACK);
        drawCell(cell, skin, cl, cg, ca);
        int n = CELL * CELL;
        int[] r = new int[n], g = new int[n], b = new int[n];
        lum.getPixels(r, 0, CELL, 0, 0, CELL, CELL);
        glyph.getPixels(g, 0, CELL, 0, 0, CELL, CELL);
        accent.getPixels(b, 0, CELL, 0, 0, CELL, CELL);
        lum.recycle();
        glyph.recycle();
        accent.recycle();
        int cr = (color >> 16) & 255, cg2 = (color >> 8) & 255, cb = color & 255;
        int gr = (skin.glyph >> 16) & 255, gg = (skin.glyph >> 8) & 255, gb = skin.glyph & 255;
        int ar = (skin.accent >> 16) & 255, ag = (skin.accent >> 8) & 255, ab = skin.accent & 255;
        for (int i = 0; i < n; i++) {
            float l = ((r[i] >> 16) & 255) / 255f, gm = ((g[i] >> 16) & 255) / 255f, am = ((b[i] >> 16) & 255) / 255f;
            float pr = cr * l, pg = cg2 * l, pb = cb * l;
            pr += (ar - pr) * am;
            pg += (ag - pg) * am;
            pb += (ab - pb) * am;
            pr += (gr - pr) * gm;
            pg += (gg - pg) * gm;
            pb += (gb - pb) * gm;
            r[i] = 0xFF000000 | (Math.round(pr) << 16) | (Math.round(pg) << 8) | Math.round(pb);
        }
        return Bitmap.createBitmap(r, CELL, CELL, Bitmap.Config.ARGB_8888);
    }

    private static void drawCell(int cell, Catalog.Skin s, Canvas lum, Canvas glyph, Canvas accent) {
        boolean stone = cell == STONE, ice = cell >= ICE_ARROW && cell <= ICE_BACK;
        tile(lum, s, stone ? Catalog.PLAIN : s.pattern, cell);
        if (stone) {
            stoneTexture(lum);
            return;
        }
        if (Color.red(s.accent) + Color.green(s.accent) + Color.blue(s.accent) > 0) {
            Paint p = paint(255);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(CELL * 0.035f);
            float in = CELL * (s.border + 0.035f);
            float rad = CELL * s.corner;
            accent.drawRoundRect(new RectF(in, in, CELL - in, CELL - in), rad, rad, p);
        }
        if (ice) frost(lum);
        int a = ice ? 150 : 255;
        switch (cell) {
            case ARROW:
            case ICE_ARROW:
            case KEY_ARROW:
                arrow(glyph, s.arrow, a);
                break;
            case FRONT:
            case ICE_FRONT:
            case KEY_FRONT:
                dot(glyph, s.arrow == Catalog.PIXEL, a);
                break;
            case LOCK:
                padlock(glyph);
                break;
            default:
                break;
        }
        if (cell >= KEY_ARROW) {
            sparkle(glyph, CELL * 0.2f, CELL * 0.2f, CELL * 0.09f);
            sparkle(glyph, CELL * 0.8f, CELL * 0.8f, CELL * 0.07f);
            if (cell == KEY_BACK) sparkle(glyph, CELL * 0.5f, CELL * 0.5f, CELL * 0.16f);
        }
    }

    private static Paint paint(int gray) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(gray, gray, gray));
        return p;
    }

    /** The face: a darker seam around a lighter rounded tile with a soft top-left light. */
    private static void tile(Canvas c, Catalog.Skin s, int pattern, int cell) {
        c.drawColor(Color.rgb(150, 150, 150));
        float in = CELL * s.border, rad = CELL * s.corner;
        RectF r = new RectF(in, in, CELL - in, CELL - in);
        Paint p = paint(255);
        p.setShader(new LinearGradient(0, 0, CELL, CELL, Color.rgb(255, 255, 255), Color.rgb(222, 222, 222),
                Shader.TileMode.CLAMP));
        if (rad < 1) c.drawRect(r, p);
        else c.drawRoundRect(r, rad, rad, p);
        Random rnd = new Random(cell * 31 + 7);
        if (pattern == Catalog.WOOD) {
            Paint g = paint(200);
            g.setStyle(Paint.Style.STROKE);
            for (int i = 0; i < 9; i++) {
                g.setStrokeWidth(3 + rnd.nextInt(6));
                int gray = 185 + rnd.nextInt(40);
                g.setColor(Color.rgb(gray, gray, gray));
                Path path = new Path();
                float y = in + (CELL - 2 * in) * (i + 0.5f) / 9f;
                path.moveTo(in, y);
                for (int k = 1; k <= 4; k++) {
                    float x = in + (CELL - 2 * in) * k / 4f;
                    path.quadTo(x - 30, y + rnd.nextInt(16) - 8, x, y + rnd.nextInt(10) - 5);
                }
                c.drawPath(path, g);
            }
        } else if (pattern == Catalog.MARBLE) {
            Paint g = paint(185);
            g.setStyle(Paint.Style.STROKE);
            for (int i = 0; i < 4; i++) {
                g.setStrokeWidth(1.5f + rnd.nextFloat() * 3);
                Path path = new Path();
                float x = rnd.nextInt(CELL), y = in;
                path.moveTo(x, y);
                while (y < CELL - in) {
                    x += rnd.nextInt(60) - 30;
                    y += 20 + rnd.nextInt(30);
                    path.lineTo(x, Math.min(y, CELL - in));
                }
                c.drawPath(path, g);
            }
        } else if (pattern == Catalog.GLASS) {
            Paint g = paint(255);
            g.setShader(new LinearGradient(0, 0, 0, CELL, Color.rgb(255, 255, 255), Color.rgb(190, 190, 190),
                    Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(in * 2.5f, in * 2.5f, CELL - in * 2.5f, CELL - in * 2.5f), rad, rad, g);
        }
    }

    private static void stoneTexture(Canvas c) {
        Random rnd = new Random(99);
        Paint p = paint(200);
        for (int i = 0; i < 70; i++) {
            int gray = 170 + rnd.nextInt(80);
            p.setColor(Color.rgb(gray, gray, gray));
            c.drawCircle(rnd.nextInt(CELL), rnd.nextInt(CELL), 4 + rnd.nextInt(14), p);
        }
        Paint crack = paint(110);
        crack.setStyle(Paint.Style.STROKE);
        crack.setStrokeWidth(4);
        Path path = new Path();
        path.moveTo(40, 70);
        path.lineTo(95, 110);
        path.lineTo(120, 100);
        path.lineTo(170, 160);
        path.lineTo(215, 175);
        path.moveTo(120, 100);
        path.lineTo(130, 50);
        c.drawPath(path, crack);
    }

    private static void frost(Canvas c) {
        Paint p = paint(205);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(40, 60, 100, 40, p);
        c.drawLine(170, 220, 225, 170, p);
        c.drawLine(30, 200, 70, 230, p);
        Paint shine = paint(255);
        shine.setStrokeWidth(10);
        shine.setStrokeCap(Paint.Cap.ROUND);
        shine.setAlpha(255);
        c.drawLine(196, 40, 222, 66, shine);
    }

    private static void arrow(Canvas c, int style, int alpha) {
        Paint p = paint(alpha);
        float m = CELL / 2f;
        switch (style) {
            case Catalog.CHEVRON: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(30);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeJoin(Paint.Join.ROUND);
                Path a = new Path();
                a.moveTo(m - 62, 118);
                a.lineTo(m, 56);
                a.lineTo(m + 62, 118);
                a.moveTo(m - 62, 192);
                a.lineTo(m, 130);
                a.lineTo(m + 62, 192);
                c.drawPath(a, p);
                break;
            }
            case Catalog.PIXEL: {
                p.setAntiAlias(false);
                String[] rows = {"...##...", "..####..", ".######.", "########", "...##...", "...##...", "...##...",
                        "...##..."};
                float px = 22, x0 = m - 4 * px, y0 = m - 4 * px;
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        if (rows[y].charAt(x) == '#') c.drawRect(x0 + x * px, y0 + y * px, x0 + (x + 1) * px, y0 + (y + 1) * px, p);
                    }
                }
                break;
            }
            case Catalog.THIN: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(18);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeJoin(Paint.Join.ROUND);
                Path a = new Path();
                a.moveTo(m, 200);
                a.lineTo(m, 60);
                a.moveTo(m - 52, 112);
                a.lineTo(m, 60);
                a.lineTo(m + 52, 112);
                c.drawPath(a, p);
                break;
            }
            default: {
                p.setPathEffect(new CornerPathEffect(12));
                Path a = new Path();
                a.moveTo(m, 44);
                a.lineTo(m + 74, 124);
                a.lineTo(m + 26, 124);
                a.lineTo(m + 26, 208);
                a.lineTo(m - 26, 208);
                a.lineTo(m - 26, 124);
                a.lineTo(m - 74, 124);
                a.close();
                c.drawPath(a, p);
            }
        }
    }

    /** "Coming at you": a ring with a dot, on the face the block leaves through. */
    private static void dot(Canvas c, boolean square, int alpha) {
        Paint p = paint(alpha);
        float m = CELL / 2f;
        if (square) {
            p.setAntiAlias(false);
            c.drawRect(m - 30, m - 30, m + 30, m + 30, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(18);
            c.drawRect(m - 74, m - 74, m + 74, m + 74, p);
            return;
        }
        c.drawCircle(m, m, 30, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(17);
        c.drawCircle(m, m, 72, p);
    }

    private static void padlock(Canvas c) {
        Paint p = paint(255);
        float m = CELL / 2f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(20);
        c.drawArc(new RectF(m - 40, 62, m + 40, 142), 180, 180, false, p);
        c.drawLine(m - 40, 102, m - 40, 124, p);
        c.drawLine(m + 40, 102, m + 40, 124, p);
        p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF(m - 62, 118, m + 62, 206), 16, 16, p);
        Paint hole = paint(0);
        c.drawCircle(m, 152, 13, hole);
        c.drawRect(m - 6, 152, m + 6, 182, hole);
    }

    private static void sparkle(Canvas c, float x, float y, float r) {
        Paint p = paint(255);
        Path s = new Path();
        s.moveTo(x, y - r);
        s.quadTo(x, y, x + r, y);
        s.quadTo(x, y, x, y + r);
        s.quadTo(x, y, x - r, y);
        s.quadTo(x, y, x, y - r);
        s.close();
        c.drawPath(s, p);
    }
}
