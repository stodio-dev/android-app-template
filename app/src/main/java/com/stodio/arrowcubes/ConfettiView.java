package com.stodio.arrowcubes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/** A burst of falling paper squares over the win card. Draws nothing once it has fallen. */
final class ConfettiView extends View {
    private static final int N = 90;
    private static final int[] COLORS = {0xFFF2716B, 0xFF5B8DEF, 0xFF47C28F, 0xFFF5BE2E, 0xFF9B7BEA, 0xFFEC6FA9};
    private final float[] x = new float[N], y = new float[N], vx = new float[N], vy = new float[N], rot = new float[N],
            spin = new float[N], size = new float[N];
    private final int[] color = new int[N];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long start, last;

    ConfettiView(Context c) {
        super(c);
        setClickable(false);
    }

    void burst() {
        Random r = new Random();
        float w = Math.max(1, getWidth()), d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < N; i++) {
            x[i] = w / 2 + (r.nextFloat() - 0.5f) * w * 0.3f;
            y[i] = getHeight() * 0.35f;
            float a = (float) (r.nextFloat() * Math.PI * 2);
            float sp = (220 + r.nextFloat() * 520) * d;
            vx[i] = (float) Math.cos(a) * sp * 0.7f;
            vy[i] = (float) Math.sin(a) * sp - 380 * d;
            rot[i] = r.nextFloat() * 360;
            spin[i] = (r.nextFloat() - 0.5f) * 720;
            size[i] = (5 + r.nextFloat() * 6) * d;
            color[i] = COLORS[r.nextInt(COLORS.length)];
        }
        start = last = SystemClock.uptimeMillis();
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c) {
        if (start == 0) return;
        long now = SystemClock.uptimeMillis();
        float dt = Math.min(0.05f, (now - last) / 1000f);
        last = now;
        float g = 900 * getResources().getDisplayMetrics().density;
        boolean any = false;
        for (int i = 0; i < N; i++) {
            vy[i] += g * dt;
            vx[i] *= 0.99f;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            rot[i] += spin[i] * dt;
            if (y[i] > getHeight() + 40) continue;
            any = true;
            paint.setColor(color[i]);
            c.save();
            c.rotate(rot[i], x[i], y[i]);
            c.drawRect(x[i] - size[i] / 2, y[i] - size[i] / 3, x[i] + size[i] / 2, y[i] + size[i] / 3, paint);
            c.restore();
        }
        if (any && now - start < 4000) postInvalidateOnAnimation();
        else start = 0;
    }
}
