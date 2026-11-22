package com.stodio.arrowcubes;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * The 3D view. One finger turns the structure (and flings it), two fingers pinch to zoom and drag to pan, and a short
 * touch that did not move is a tap on a block. A demo view (the home screen) only turns by itself.
 */
final class GameView extends GLSurfaceView {
    private static final float DEG_PER_DP = 0.5f;
    final CubeRenderer renderer;
    private final boolean demo;
    private final ScaleGestureDetector scaler;
    private final float slop, density;
    private VelocityTracker velocity;
    private float downX, downY, lastX, lastY, focusX, focusY;
    private long downTime;
    private boolean dragging, multi;

    GameView(Context c, boolean demo) {
        super(c);
        this.demo = demo;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(new Chooser());
        setPreserveEGLContextOnPause(true);
        renderer = new CubeRenderer(this, demo);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        density = c.getResources().getDisplayMetrics().density;
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        scaler = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector d) {
                renderer.zoomBy(d.getScaleFactor());
                return true;
            }
        });
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (demo) return false;
        scaler.onTouchEvent(e);
        if (velocity == null) velocity = VelocityTracker.obtain();
        velocity.addMovement(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX();
                downY = lastY = e.getY();
                downTime = e.getEventTime();
                dragging = false;
                multi = false;
                renderer.stopSpin();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                multi = true;
                focus(e);
                break;
            case MotionEvent.ACTION_MOVE:
                if (e.getPointerCount() > 1) {
                    float fx = focusX, fy = focusY;
                    focus(e);
                    renderer.pan((focusX - fx) / getHeight(), (focusY - fy) / getHeight());
                } else if (!multi) {
                    float x = e.getX(), y = e.getY();
                    if (!dragging && Math.hypot(x - downX, y - downY) > slop) {
                        dragging = true;
                        lastX = x;
                        lastY = y;
                    }
                    if (dragging) {
                        renderer.rotate((x - lastX) / density * DEG_PER_DP, (y - lastY) / density * DEG_PER_DP);
                        lastX = x;
                        lastY = y;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!dragging && !multi && e.getEventTime() - downTime < 450) {
                    final float x = e.getX(), y = e.getY();
                    queueEvent(new Runnable() {
                        @Override public void run() {
                            renderer.tap(x, y);
                        }
                    });
                } else if (dragging && !multi) {
                    velocity.computeCurrentVelocity(1000);
                    renderer.fling(velocity.getXVelocity() / density * DEG_PER_DP,
                            velocity.getYVelocity() / density * DEG_PER_DP);
                }
                recycle();
                break;
            case MotionEvent.ACTION_CANCEL:
                recycle();
                break;
            default:
                break;
        }
        return true;
    }

    /** The middle of the fingers still down (one lifting in a POINTER_UP is left out). */
    private void focus(MotionEvent e) {
        int skip = e.getActionMasked() == MotionEvent.ACTION_POINTER_UP ? e.getActionIndex() : -1;
        float sx = 0, sy = 0;
        int n = 0;
        for (int i = 0; i < e.getPointerCount(); i++) {
            if (i == skip) continue;
            sx += e.getX(i);
            sy += e.getY(i);
            n++;
        }
        if (n > 0) {
            focusX = sx / n;
            focusY = sy / n;
        }
    }

    private void recycle() {
        if (velocity != null) {
            velocity.recycle();
            velocity = null;
        }
    }

    /** RGB 888 with depth, 4x multisampling when the phone has it (smoother cube edges), else without. */
    private static final class Chooser implements GLSurfaceView.EGLConfigChooser {
        private static final int EGL_OPENGL_ES2_BIT = 4;

        @Override public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[][] tries = {
                    {EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_DEPTH_SIZE, 16,
                            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_SAMPLE_BUFFERS, 1,
                            EGL10.EGL_SAMPLES, 4, EGL10.EGL_NONE},
                    {EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_DEPTH_SIZE, 16,
                            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE},
                    {EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE},
            };
            for (int t = 0; t < tries.length; t++) {
                EGLConfig[] configs = new EGLConfig[1];
                int[] num = new int[1];
                if (egl.eglChooseConfig(display, tries[t], configs, 1, num) && num[0] > 0 && configs[0] != null) {
                    App.log("EGL config " + (t == 0 ? "4x MSAA" : "no MSAA (" + t + ")"));
                    return configs[0];
                }
            }
            throw new IllegalArgumentException("no OpenGL ES 2 config");
        }
    }
}
