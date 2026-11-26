package com.stodio.arrowcubes;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;

import com.stodio.arrowcubes.core.Board;
import com.stodio.arrowcubes.core.Catalog;
import com.stodio.arrowcubes.core.Game;
import com.stodio.arrowcubes.core.Mesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Draws the structure with OpenGL ES 2.0 and runs the taps. Threads: the camera (rotation, zoom, pan, spin) is set
 * from the UI thread under {@link #cam}; the game, the animations and every GL call live on the GL thread, so taps,
 * hints and the hammer arrive through GLSurfaceView.queueEvent and results go back through {@link Listener}.
 *
 * Static blocks are one vertex buffer with the faces hidden between neighbours left out; it is rebuilt when a block
 * leaves or starts moving. Moving blocks (leaving, bumping, shaking, the hint) are drawn one by one.
 */
final class CubeRenderer implements GLSurfaceView.Renderer {
    /** Called on the GL thread. */
    interface Listener {
        void onTap(State s);

        void onHint(boolean found, boolean visible);

        void onHammer(State s);
    }

    /** What the screen needs after a tap, copied on the GL thread. */
    static final class State {
        int result, remaining, moves, movesLeft, keys, stars, wasted;
        boolean won, lost;
    }

    private static final int FLOATS = Mesh.FLOATS, STRIDE = FLOATS * 4;
    private static final float FOV = 38f;
    private static final float[] LIGHT = norm(-0.35f, 0.8f, 0.5f);

    private final GLSurfaceView view;
    private final boolean demo;
    private Listener listener;

    // ---- camera, shared with the UI thread ----
    private final Object cam = new Object();
    private final float[] rot = new float[16];
    private float zoom = 1f, panX, panY, spinX, spinY;
    private int rotations;

    // ---- GL thread ----
    private Game game;
    private long seed;
    private Catalog.Skin skin = Catalog.SKINS[0];
    private Catalog.Background bg = Catalog.BACKGROUNDS[0];
    private boolean skinChanged = true, dirty = true, hammerMode;
    private int width = 1, height = 1;
    private int blockProg, bgProg, tex, vbo, staticCount;
    private int uMvp, uRot, uOffset, uCenter, uScale, uFlash, uFlashCol, uAlpha, uGlyph, uAccent, uLight, uSpec, uTex;
    private int bTop, bBottom, bStars;
    private FloatBuffer staticBuf, bgQuad;
    private final float[][] scratch = {new float[Mesh.BLOCK_FLOATS * 64]};
    private boolean[] dynamic = new boolean[0];
    private final ArrayList<Anim> anims = new ArrayList<Anim>();
    private final float[] proj = new float[16], viewM = new float[16], model = new float[16], mv = new float[16],
            mvp = new float[16], tmp = new float[16], rotNow = new float[16];
    private long lastFrame;
    private float fitMargin = 1.0f;

    CubeRenderer(GLSurfaceView view, boolean demo) {
        this.view = view;
        this.demo = demo;
        if (demo) {
            fitMargin = 1.05f;
            panY = 0.04f; // a little above the buttons
        }
        resetRotation();
    }

    void setListener(Listener l) {
        listener = l;
    }

    // ================= UI thread: camera =================

    void rotate(float degX, float degY) {
        synchronized (cam) {
            applyRotation(degX, degY);
        }
        view.requestRender();
    }

    void fling(float degPerSecX, float degPerSecY) {
        synchronized (cam) {
            spinX = degPerSecX;
            spinY = degPerSecY;
        }
        view.requestRender();
    }

    void stopSpin() {
        synchronized (cam) {
            spinX = spinY = 0;
        }
    }

    void zoomBy(float f) {
        synchronized (cam) {
            zoom = Math.max(0.55f, Math.min(3.5f, zoom * f));
        }
        view.requestRender();
    }

    /** Pan by a share of the view height. */
    void pan(float dx, float dy) {
        synchronized (cam) {
            panX = Math.max(-0.6f, Math.min(0.6f, panX + dx));
            panY = Math.max(-0.6f, Math.min(0.6f, panY - dy));
        }
        view.requestRender();
    }

    void resetView() {
        synchronized (cam) {
            resetRotation();
            zoom = 1f;
            panX = 0;
            panY = demo ? 0.04f : 0;
            spinX = spinY = 0;
        }
        view.requestRender();
    }

    private void resetRotation() {
        float[] a = new float[16], b = new float[16];
        Matrix.setRotateM(a, 0, 24f, 1, 0, 0);
        Matrix.setRotateM(b, 0, -32f, 0, 1, 0);
        Matrix.multiplyMM(rot, 0, a, 0, b, 0);
    }

    /** Turns the model about the screen's axes (pre-multiplied), so dragging always turns it the way the finger goes. */
    private void applyRotation(float degX, float degY) {
        float[] r = new float[16], out = new float[16];
        if (degX != 0) {
            Matrix.setRotateM(r, 0, degX, 0, 1, 0);
            Matrix.multiplyMM(out, 0, r, 0, rot, 0);
            System.arraycopy(out, 0, rot, 0, 16);
        }
        if (degY != 0) {
            Matrix.setRotateM(r, 0, degY, 1, 0, 0);
            Matrix.multiplyMM(out, 0, r, 0, rot, 0);
            System.arraycopy(out, 0, rot, 0, 16);
        }
        if (++rotations % 30 == 0) orthonormalize(rot);
    }

    /** Gram-Schmidt on the rotation's columns, so many small turns do not skew it. */
    private static void orthonormalize(float[] m) {
        float[] x = {m[0], m[1], m[2]}, y = {m[4], m[5], m[6]};
        normalize(x);
        float d = x[0] * y[0] + x[1] * y[1] + x[2] * y[2];
        y[0] -= d * x[0];
        y[1] -= d * x[1];
        y[2] -= d * x[2];
        normalize(y);
        float[] z = {x[1] * y[2] - x[2] * y[1], x[2] * y[0] - x[0] * y[2], x[0] * y[1] - x[1] * y[0]};
        m[0] = x[0];
        m[1] = x[1];
        m[2] = x[2];
        m[4] = y[0];
        m[5] = y[1];
        m[6] = y[2];
        m[8] = z[0];
        m[9] = z[1];
        m[10] = z[2];
    }

    // ================= GL thread: game =================

    void setGame(Game g, long levelSeed) {
        game = g;
        seed = levelSeed;
        anims.clear();
        dynamic = new boolean[g.board.size()];
        hammerMode = false;
        dirty = true;
        view.requestRender();
    }

    void setLook(Catalog.Skin s, Catalog.Background b) {
        if (s != skin) skinChanged = true;
        skin = s;
        bg = b;
        dirty = true;
        view.requestRender();
    }

    void setHammerMode(boolean on) {
        hammerMode = on;
    }

    void tap(float x, float y) {
        if (game == null || demo) return;
        int cell = pick(x, y);
        if (cell < 0) return;
        Board b = game.board;
        long now = SystemClock.uptimeMillis();
        if (hammerMode) {
            float[] verts = blockVerts(cell);
            if (game.hammer(cell)) {
                hammerMode = false;
                dropAnims(cell);
                Anim a = new Anim(Anim.POP, cell, verts, now, 380);
                anims.add(a);
                dirty = true;
                if (listener != null) listener.onHammer(state(Game.EXIT));
            }
            view.requestRender();
            return;
        }
        int dir = b.dir(cell);
        float[] before = blockVerts(cell);
        int r = game.tap(cell);
        switch (r) {
            case Game.EXIT: {
                dropAnims(cell);
                Anim a = new Anim(Anim.EXIT, cell, before, now, 620);
                a.dir = dir;
                a.travel = 4f + radius() * 2.2f;
                anims.add(a);
                dirty = true;
                break;
            }
            case Game.BLOCKED: {
                dropAnims(cell);
                Anim a = new Anim(Anim.BUMP, cell, before, now, 240 + 60 * game.lastGap);
                a.dir = dir;
                a.travel = game.lastGap == 0 ? 0.16f : game.lastGap;
                addOnBoard(a);
                int blocker = game.lastBlocker;
                if (blocker >= 0) {
                    dropAnims(blocker);
                    Anim s = new Anim(Anim.SHAKE, blocker, blockVerts(blocker), now + (long) (a.dur * 0.45f), 260);
                    s.dir = dir;
                    s.color = 0xFF5A4F;
                    addOnBoard(s);
                }
                break;
            }
            case Game.CRACKED: {
                dropAnims(cell);
                Anim a = new Anim(Anim.FLASH, cell, blockVerts(cell), now, 320);
                a.color = 0xFFFFFF;
                addOnBoard(a);
                break;
            }
            case Game.LOCKED:
            case Game.FIXED: {
                dropAnims(cell);
                Anim a = new Anim(Anim.SHAKE, cell, before, now, 300);
                a.dir = (dir + 2) % 6;
                a.color = r == Game.LOCKED ? 0xF5BE2E : 0x777777;
                addOnBoard(a);
                break;
            }
            default:
                return;
        }
        if (listener != null) listener.onTap(state(r));
        view.requestRender();
    }

    /** The "+5 moves" booster after running out. */
    void addMoves(int n) {
        if (game == null) return;
        game.addMoves(n);
        if (listener != null) listener.onTap(state(Game.NONE));
    }

    /** Marks a block that can go now, preferring one the camera sees. */
    void hint() {
        if (game == null || game.over()) return;
        int[] cands = game.hints();
        if (cands.length == 0) {
            if (listener != null) listener.onHint(false, false);
            return;
        }
        int pickCell = -1;
        for (int c : cands) {
            if (visible(c)) {
                pickCell = c;
                break;
            }
        }
        boolean vis = pickCell >= 0;
        if (!vis) pickCell = cands[0];
        dropAnims(pickCell);
        Anim a = new Anim(Anim.HINT, pickCell, blockVerts(pickCell), SystemClock.uptimeMillis(), 9000);
        a.color = 0xFFF27A;
        addOnBoard(a);
        if (listener != null) listener.onHint(true, vis);
        view.requestRender();
    }

    State state(int result) {
        State s = new State();
        s.result = result;
        s.remaining = game.board.removable();
        s.moves = game.moves();
        s.movesLeft = game.movesLeft();
        s.keys = game.board.keys();
        s.won = game.won();
        s.lost = game.lost();
        s.stars = game.stars();
        s.wasted = game.wasted();
        return s;
    }

    private void addOnBoard(Anim a) {
        a.onBoard = true;
        anims.add(a);
        if (!dynamic[a.cell]) {
            dynamic[a.cell] = true;
            dirty = true;
        }
    }

    private void dropAnims(int cell) {
        for (int i = anims.size() - 1; i >= 0; i--) {
            Anim a = anims.get(i);
            if (a.cell == cell && a.onBoard) anims.remove(i);
        }
        if (cell < dynamic.length && dynamic[cell]) {
            dynamic[cell] = false;
            dirty = true;
        }
    }

    private float radius() {
        Board b = game.board;
        return 0.5f * (float) Math.sqrt(b.sx * b.sx + b.sy * b.sy + b.sz * b.sz);
    }

    // ================= GL thread: picking =================

    /** The block under a screen point: unproject the touch into grid space and walk the voxels. */
    private int pick(float x, float y) {
        float[] inv = new float[16];
        if (!Matrix.invertM(inv, 0, mvp, 0)) return -1;
        float nx = 2f * x / width - 1f, ny = 1f - 2f * y / height;
        float[] near = unproject(inv, nx, ny, -1f), far = unproject(inv, nx, ny, 1f);
        return game.board.raycast(near[0], near[1], near[2], far[0] - near[0], far[1] - near[1], far[2] - near[2]);
    }

    private static float[] unproject(float[] inv, float x, float y, float z) {
        float[] out = new float[4];
        Matrix.multiplyMV(out, 0, inv, 0, new float[]{x, y, z, 1f}, 0);
        return new float[]{out[0] / out[3], out[1] / out[3], out[2] / out[3]};
    }

    private boolean visible(int cell) {
        float[] inv = new float[16];
        if (!Matrix.invertM(inv, 0, mv, 0)) return true;
        float[] eye = new float[4];
        Matrix.multiplyMV(eye, 0, inv, 0, new float[]{0, 0, 0, 1}, 0);
        Board b = game.board;
        float cx = b.x(cell) + 0.5f, cy = b.y(cell) + 0.5f, cz = b.z(cell) + 0.5f;
        float ex = eye[0] / eye[3], ey = eye[1] / eye[3], ez = eye[2] / eye[3];
        return b.raycast(ex, ey, ez, cx - ex, cy - ey, cz - ez) == cell;
    }

    // ================= GL thread: drawing =================

    @Override public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        App.log("GL " + GLES20.glGetString(GLES20.GL_RENDERER) + " / " + GLES20.glGetString(GLES20.GL_VERSION));
        blockProg = program(Shaders.BLOCK_VS, Shaders.BLOCK_FS, new String[]{"a_pos", "a_nrm", "a_uv", "a_col"});
        bgProg = program(Shaders.BG_VS, Shaders.BG_FS, new String[]{"a_pos"});
        uMvp = GLES20.glGetUniformLocation(blockProg, "u_mvp");
        uRot = GLES20.glGetUniformLocation(blockProg, "u_rot");
        uOffset = GLES20.glGetUniformLocation(blockProg, "u_offset");
        uCenter = GLES20.glGetUniformLocation(blockProg, "u_center");
        uScale = GLES20.glGetUniformLocation(blockProg, "u_scale");
        uFlash = GLES20.glGetUniformLocation(blockProg, "u_flash");
        uFlashCol = GLES20.glGetUniformLocation(blockProg, "u_flashCol");
        uAlpha = GLES20.glGetUniformLocation(blockProg, "u_alpha");
        uGlyph = GLES20.glGetUniformLocation(blockProg, "u_glyph");
        uAccent = GLES20.glGetUniformLocation(blockProg, "u_accent");
        uLight = GLES20.glGetUniformLocation(blockProg, "u_light");
        uSpec = GLES20.glGetUniformLocation(blockProg, "u_spec");
        uTex = GLES20.glGetUniformLocation(blockProg, "u_tex");
        bTop = GLES20.glGetUniformLocation(bgProg, "u_top");
        bBottom = GLES20.glGetUniformLocation(bgProg, "u_bottom");
        bStars = GLES20.glGetUniformLocation(bgProg, "u_stars");
        int[] ids = new int[1];
        GLES20.glGenBuffers(1, ids, 0);
        vbo = ids[0];
        GLES20.glGenTextures(1, ids, 0);
        tex = ids[0];
        bgQuad = floats(new float[]{-1, -1, 1, -1, -1, 1, 1, 1});
        skinChanged = true;
        dirty = true;
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        lastFrame = SystemClock.uptimeMillis();
    }

    @Override public void onSurfaceChanged(GL10 unused, int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override public void onDrawFrame(GL10 unused) {
        long now = SystemClock.uptimeMillis();
        float dt = Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (skinChanged) uploadAtlas();
        boolean spinning;
        float zoomNow, px, py;
        synchronized (cam) {
            if (demo) {
                float[] r = new float[16], out = new float[16];
                Matrix.setRotateM(r, 0, 14f * dt, 0, 1, 0);
                Matrix.multiplyMM(out, 0, rot, 0, r, 0);
                System.arraycopy(out, 0, rot, 0, 16);
            }
            spinning = Math.abs(spinX) + Math.abs(spinY) > 2f;
            if (spinning) {
                applyRotation(spinX * dt, spinY * dt);
                float decay = (float) Math.exp(-3.5 * dt);
                spinX *= decay;
                spinY *= decay;
            }
            System.arraycopy(rot, 0, rotNow, 0, 16);
            zoomNow = zoom;
            px = panX;
            py = panY;
        }

        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        drawBackground();
        if (game == null) {
            if (demo) view.requestRender();
            return;
        }
        Board b = game.board;

        float aspect = width / (float) height;
        float half = (float) Math.toRadians(FOV / 2);
        float halfX = (float) Math.atan(Math.tan(half) * aspect);
        float r = radius();
        float fit = r / (float) Math.sin(Math.min(half, halfX)) * fitMargin;
        float dist = fit / zoomNow;
        float visibleH = 2f * dist * (float) Math.tan(half);
        Matrix.perspectiveM(proj, 0, FOV, aspect, 0.1f, dist + r * 6f + 60f);
        Matrix.setIdentityM(viewM, 0);
        Matrix.translateM(viewM, 0, px * visibleH, py * visibleH, -dist);
        Matrix.setIdentityM(tmp, 0);
        Matrix.translateM(tmp, 0, -b.sx / 2f, -b.sy / 2f, -b.sz / 2f);
        Matrix.multiplyMM(model, 0, rotNow, 0, tmp, 0);
        Matrix.multiplyMM(mv, 0, viewM, 0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0);

        if (dirty) rebuildStatic();

        GLES20.glUseProgram(blockProg);
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
        GLES20.glUniformMatrix4fv(uRot, 1, false, rotNow, 0);
        GLES20.glUniform3f(uLight, LIGHT[0], LIGHT[1], LIGHT[2]);
        GLES20.glUniform3f(uGlyph, red(skin.glyph), green(skin.glyph), blue(skin.glyph));
        GLES20.glUniform3f(uAccent, red(skin.accent), green(skin.accent), blue(skin.accent));
        GLES20.glUniform1f(uSpec, skin.shine);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
        GLES20.glUniform1i(uTex, 0);
        for (int i = 1; i < 4; i++) GLES20.glEnableVertexAttribArray(i);
        GLES20.glEnableVertexAttribArray(0);

        setPose(0, 0, 0, 0, 0, 0, 1f, 0f, 0, 1f);
        if (staticCount > 0) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
            GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, STRIDE, 0);
            GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, STRIDE, 12);
            GLES20.glVertexAttribPointer(2, 2, GLES20.GL_FLOAT, false, STRIDE, 24);
            GLES20.glVertexAttribPointer(3, 3, GLES20.GL_FLOAT, false, STRIDE, 32);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, staticCount);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }

        boolean blending = false;
        for (int i = anims.size() - 1; i >= 0; i--) {
            Anim a = anims.get(i);
            float t = (now - a.start) / a.dur;
            if (t >= 1f) {
                anims.remove(i);
                if (a.onBoard && !hasOnBoardAnim(a.cell)) {
                    dynamic[a.cell] = false;
                    dirty = true;
                }
                continue;
            }
            if (!a.onBoard && !blending) {
                GLES20.glEnable(GLES20.GL_BLEND);
                blending = true;
            }
            drawAnim(a, Math.max(0f, t), now);
        }
        if (blending) GLES20.glDisable(GLES20.GL_BLEND);
        if (!anims.isEmpty() || spinning || demo || dirty) view.requestRender();
    }

    private boolean hasOnBoardAnim(int cell) {
        for (Anim a : anims) if (a.cell == cell && a.onBoard) return true;
        return false;
    }

    private void drawAnim(Anim a, float t, long now) {
        Board b = game.board;
        float cx = b.x(a.cell) + 0.5f, cy = b.y(a.cell) + 0.5f, cz = b.z(a.cell) + 0.5f;
        float ox = 0, oy = 0, oz = 0, scale = 1f, flash = 0f, alpha = 1f;
        float dx = a.dir >= 0 ? Board.DX[a.dir] : 0, dy = a.dir >= 0 ? Board.DY[a.dir] : 0,
                dz = a.dir >= 0 ? Board.DZ[a.dir] : 0;
        boolean started = now >= a.start;
        switch (a.kind) {
            case Anim.EXIT: {
                float s = 0.25f * t + a.travel * t * t;
                ox = dx * s;
                oy = dy * s;
                oz = dz * s;
                alpha = t < 0.55f ? 1f : 1f - (t - 0.55f) / 0.45f;
                break;
            }
            case Anim.BUMP: {
                float out = 0.45f, s;
                if (t < out) {
                    float u = t / out;
                    s = a.travel * u * u;
                } else {
                    float u = (t - out) / (1f - out);
                    s = a.travel * (1f - u) * (1f - u);
                }
                ox = dx * s;
                oy = dy * s;
                oz = dz * s;
                break;
            }
            case Anim.SHAKE: {
                if (!started) break;
                float s = (float) Math.sin(t * Math.PI * 6) * 0.07f * (1f - t);
                ox = dx * s;
                oy = dy * s;
                oz = dz * s;
                flash = 0.55f * (1f - t);
                break;
            }
            case Anim.FLASH:
                flash = 0.85f * (1f - t);
                scale = 1f + 0.08f * (float) Math.sin(t * Math.PI);
                break;
            case Anim.HINT:
                flash = 0.25f + 0.25f * (float) Math.sin((now - a.start) / 1000f * Math.PI * 3);
                scale = 1.04f + 0.03f * (float) Math.sin((now - a.start) / 1000f * Math.PI * 3);
                break;
            case Anim.POP:
                scale = t < 0.3f ? 1f + t : Math.max(0f, 1.3f * (1f - (t - 0.3f) / 0.7f));
                flash = 0.7f;
                break;
            default:
                break;
        }
        setPose(ox, oy, oz, cx, cy, cz, scale, flash, a.color, alpha);
        FloatBuffer fb = a.buffer();
        fb.position(0);
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, STRIDE, fb);
        fb.position(3);
        GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, STRIDE, fb);
        fb.position(6);
        GLES20.glVertexAttribPointer(2, 2, GLES20.GL_FLOAT, false, STRIDE, fb);
        fb.position(8);
        GLES20.glVertexAttribPointer(3, 3, GLES20.GL_FLOAT, false, STRIDE, fb);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, a.verts.length / FLOATS);
    }

    private void setPose(float ox, float oy, float oz, float cx, float cy, float cz, float scale, float flash,
                         int color, float alpha) {
        GLES20.glUniform3f(uOffset, ox, oy, oz);
        GLES20.glUniform3f(uCenter, cx, cy, cz);
        GLES20.glUniform1f(uScale, scale);
        GLES20.glUniform1f(uFlash, flash);
        GLES20.glUniform3f(uFlashCol, red(color), green(color), blue(color));
        GLES20.glUniform1f(uAlpha, alpha);
    }

    private void drawBackground() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);
        GLES20.glUseProgram(bgProg);
        for (int i = 1; i < 4; i++) GLES20.glDisableVertexAttribArray(i);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        bgQuad.position(0);
        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 8, bgQuad);
        GLES20.glUniform3f(bTop, red(bg.top), green(bg.top), blue(bg.top));
        GLES20.glUniform3f(bBottom, red(bg.bottom), green(bg.bottom), blue(bg.bottom));
        GLES20.glUniform1f(bStars, bg.stars ? 1f : 0f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void uploadAtlas() {
        skinChanged = false;
        long t0 = SystemClock.uptimeMillis();
        Bitmap bmp = Atlas.build(skin);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        App.log("atlas " + skin.id + " " + (SystemClock.uptimeMillis() - t0) + " ms");
    }

    // ================= geometry =================

    private void rebuildStatic() {
        dirty = false;
        if (game == null) return;
        int n = Mesh.build(game.board, dynamic, skin, seed, scratch);
        if (staticBuf == null || staticBuf.capacity() < n) {
            staticBuf = ByteBuffer.allocateDirect(Math.max(n, 1024) * 4 * 3 / 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        }
        staticBuf.clear();
        staticBuf.put(scratch[0], 0, n);
        staticBuf.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, n * 4, staticBuf, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        staticCount = n / FLOATS;
    }

    private float[] blockVerts(int cell) {
        return Mesh.block(game.board, cell, skin, seed);
    }

    // ================= helpers =================

    private static int program(String vs, String fs, String[] attribs) {
        int v = shader(GLES20.GL_VERTEX_SHADER, vs), f = shader(GLES20.GL_FRAGMENT_SHADER, fs);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        for (int i = 0; i < attribs.length; i++) GLES20.glBindAttribLocation(p, i, attribs[i]);
        GLES20.glLinkProgram(p);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) App.log("GL link failed: " + GLES20.glGetProgramInfoLog(p));
        return p;
    }

    private static int shader(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) App.log("GL shader failed: " + GLES20.glGetShaderInfoLog(s));
        return s;
    }

    private static FloatBuffer floats(float[] a) {
        FloatBuffer b = ByteBuffer.allocateDirect(a.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        b.put(a).position(0);
        return b;
    }

    private static float red(int c) {
        return ((c >> 16) & 255) / 255f;
    }

    private static float green(int c) {
        return ((c >> 8) & 255) / 255f;
    }

    private static float blue(int c) {
        return (c & 255) / 255f;
    }

    private static float[] norm(float x, float y, float z) {
        float[] v = {x, y, z};
        normalize(v);
        return v;
    }

    private static void normalize(float[] v) {
        float l = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (l > 0) {
            v[0] /= l;
            v[1] /= l;
            v[2] /= l;
        }
    }

    /** One animated block, drawn on its own. */
    static final class Anim {
        static final int EXIT = 0, BUMP = 1, SHAKE = 2, FLASH = 3, HINT = 4, POP = 5;
        final int kind, cell;
        final float[] verts;
        final long start;
        final float dur;
        int dir = -1, color;
        float travel;
        /** Still on the board (drawn here instead of in the static buffer) or already gone from it. */
        boolean onBoard;
        private FloatBuffer buf;

        Anim(int kind, int cell, float[] verts, long start, float dur) {
            this.kind = kind;
            this.cell = cell;
            this.verts = verts;
            this.start = start;
            this.dur = dur;
        }

        FloatBuffer buffer() {
            if (buf == null) buf = floats(verts);
            return buf;
        }
    }
}
