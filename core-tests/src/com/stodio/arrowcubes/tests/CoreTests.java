package com.stodio.arrowcubes.tests;

import com.stodio.arrowcubes.core.Board;
import com.stodio.arrowcubes.core.Catalog;
import com.stodio.arrowcubes.core.Game;
import com.stodio.arrowcubes.core.LevelGen;
import com.stodio.arrowcubes.core.Levels;
import com.stodio.arrowcubes.core.Mesh;
import com.stodio.arrowcubes.core.Shapes;

import java.util.HashSet;
import java.util.Random;

/** Checks of the core package on a plain JDK: bash core-tests/run.sh. The workflow runs them before every build. */
public final class CoreTests {
    private static int passed, failed;

    static void check(String name, boolean ok, String detail) {
        if (ok) passed++;
        else failed++;
        if (!ok || name.startsWith("*")) System.out.println((ok ? "PASS " : "FAIL ") + name + (ok || detail == null ? "" : ": " + detail));
    }

    public static void main(String[] args) {
        board();
        raycast();
        game();
        shapes();
        generator();
        campaign();
        catalog();
        mesh();
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static void board() {
        Board b = new Board(3, 1, 1);
        b.set(0, Board.NORMAL, 0); // points +x into cell 1
        b.set(1, Board.NORMAL, 0);
        b.set(2, Board.NORMAL, 0);
        check("blocked by neighbour", b.blocker(0, 0) == 1, "" + b.blocker(0, 0));
        check("edge block is free", b.canExit(2), null);
        check("gap 0 next to blocker", b.gap(0, 0) == 0, "" + b.gap(0, 0));
        b.clear(1);
        check("gap counts empty cells", b.gap(0, 0) == 1, "" + b.gap(0, 0));
        check("removable count", b.removable() == 2, "" + b.removable());
        b.set(1, Board.STONE, 0);
        check("stones are not removable", b.removable() == 2, "" + b.removable());
        Board k = new Board(2, 1, 1);
        k.set(0, Board.KEY, 1);
        k.set(1, Board.LOCK, 0);
        check("lock closed while a key is left", k.isLocked(1) && !k.canExit(1), null);
        k.clear(0);
        check("lock opens when keys are gone", !k.isLocked(1) && k.canExit(1), null);
        Board c = new Board(4, 3, 2);
        int i = c.index(3, 2, 1);
        check("index round trip", c.x(i) == 3 && c.y(i) == 2 && c.z(i) == 1, null);
    }

    static void raycast() {
        Board b = new Board(3, 3, 3);
        b.set(b.index(1, 1, 1), Board.NORMAL, 0);
        int hit = b.raycast(1.5f, 1.5f, 10f, 0, 0, -1);
        check("ray down the z axis hits the centre", hit == b.index(1, 1, 1), "" + hit);
        b.set(b.index(1, 1, 2), Board.NORMAL, 0);
        hit = b.raycast(1.5f, 1.5f, 10f, 0, 0, -1);
        check("ray hits the nearest block", hit == b.index(1, 1, 2), "" + hit);
        check("ray that misses", b.raycast(10f, 10f, 10f, 1, 0, 0) == -1, null);
        hit = b.raycast(-5f, 1.5f, 2.5f, 1, 0, 0);
        check("ray along x", hit == b.index(1, 1, 2), "" + hit);
        hit = b.raycast(-3f, -2.4f, 2.5f, 1, 1, 0);
        check("diagonal ray", hit == b.index(1, 1, 2), "" + hit);
    }

    static void game() {
        Board b = new Board(3, 1, 1);
        b.set(0, Board.NORMAL, 0);
        b.set(1, Board.ICE, 0);
        b.set(2, Board.NORMAL, 0); // free: nothing beyond it
        Game g = new Game(b, 10);
        check("required counts ice twice", g.required == 4, "" + g.required);
        check("blocked tap", g.tap(0) == Game.BLOCKED && g.wasted() == 1 && g.lastBlocker == 1, null);
        check("blocked tap uses a move", g.moves() == 1, "" + g.moves());
        check("ice cracks first", g.tap(1) == Game.CRACKED, null);
        check("cracked ice is still blocked by 2", g.tap(1) == Game.BLOCKED, null);
        check("free block exits", g.tap(2) == Game.EXIT, null);
        check("now 1 exits", g.tap(1) == Game.EXIT, null);
        check("and 0", g.tap(0) == Game.EXIT, null);
        check("won", g.won() && !g.lost(), null);
        check("stars after 2 wasted", g.stars() >= 1, "" + g.stars());
        Board s = new Board(2, 1, 1);
        s.set(0, Board.NORMAL, 0);
        s.set(1, Board.NORMAL, 0);
        Game l = new Game(s, 1);
        l.tap(0);
        check("lost when out of moves", l.lost(), null);
        l.addMoves(2);
        check("extra moves continue the game", !l.lost() && l.tap(1) == Game.EXIT && l.tap(0) == Game.EXIT && l.won(), null);
        Board h = new Board(2, 1, 1);
        h.set(0, Board.NORMAL, 0);
        h.set(1, Board.STONE, 0);
        Game hg = new Game(h, 0);
        check("stone tap is not a move", hg.tap(1) == Game.FIXED && hg.moves() == 0, null);
        check("hammer removes", hg.hammer(0) && hg.won() && hg.stars() <= 2, null);
    }

    static void shapes() {
        for (int s = 0; s < Shapes.COUNT; s++) {
            Shapes.Grid g0 = Shapes.build(s, 0);
            check("shape " + Shapes.NAMES[s] + " builds", g0 != null && g0.cells >= 6, g0 == null ? "null" : "" + g0.cells);
            int prev = 0, max = 0;
            boolean grows = true;
            for (int k = 0; k <= Shapes.MAX_K; k++) {
                Shapes.Grid g = Shapes.build(s, k);
                if (g == null) break;
                if (g.cells < prev) grows = false;
                prev = g.cells;
                max = g.cells;
            }
            check("shape " + Shapes.NAMES[s] + " grows with its step", grows, null);
            check("shape " + Shapes.NAMES[s] + " reaches 100 blocks", max >= 100, "" + max);
        }
        check("intro list matches names", Shapes.INTRO.length == Shapes.COUNT && Shapes.INTRO[0] == 1, null);
    }

    static void generator() {
        Random r = new Random(1);
        int bad = 0;
        for (int t = 0; t < 120; t++) {
            int shape = r.nextInt(Shapes.COUNT), k = r.nextInt(6);
            Shapes.Grid g = Shapes.build(shape, k);
            if (g == null) continue;
            LevelGen.Result res = LevelGen.generate(g, r.nextInt(6), r.nextInt(6), r.nextInt(3), r.nextInt(5),
                    r.nextFloat(), new Random(t));
            if (!LevelGen.solvable(res.board) || !followsOrder(res)) bad++;
        }
        check("random boards are solvable along their order", bad == 0, bad + " bad");
        Levels.Spec s = Levels.campaign(77);
        Game a = Levels.build(s), b = Levels.build(Levels.campaign(77));
        boolean same = a.board.size() == b.board.size();
        for (int i = 0; same && i < a.board.size(); i++) {
            same = a.board.type(i) == b.board.type(i) && a.board.dir(i) == b.board.dir(i);
        }
        check("a level is the same every time", same, null);
        Board easy = LevelGen.generate(Shapes.build(Shapes.BOX, 9), 0, 0, 0, 0, 0f, new Random(5)).board;
        Board hard = LevelGen.generate(Shapes.build(Shapes.BOX, 9), 0, 0, 0, 0, 1f, new Random(5)).board;
        check("*difficulty lengthens the chains (" + LevelGen.depth(easy) + " -> " + LevelGen.depth(hard) + ")",
                LevelGen.depth(hard) >= LevelGen.depth(easy), null);
    }

    /** Plays the generator's order: each block must be free (and unlocked) when its turn comes. */
    static boolean followsOrder(LevelGen.Result res) {
        Board b = res.board.copy();
        for (int c : res.order) {
            if (b.type(c) == Board.EMPTY) return false;
            if (b.type(c) == Board.ICE) b.crack(c);
            if (!b.canExit(c)) return false;
            b.clear(c);
        }
        return b.removable() == 0;
    }

    static void campaign() {
        int unsolvable = 0, maxCells = 0, maxMs = 0;
        HashSet<Integer> shapesSeen = new HashSet<Integer>();
        for (int level = 1; level <= Levels.COUNT; level += (level < 120 ? 1 : 7)) {
            long t0 = System.nanoTime();
            Levels.Spec s = Levels.campaign(level);
            Game g = Levels.build(s);
            int ms = (int) ((System.nanoTime() - t0) / 1000000);
            maxMs = Math.max(maxMs, ms);
            if (!LevelGen.solvable(g.board)) unsolvable++;
            maxCells = Math.max(maxCells, g.board.removable());
            shapesSeen.add(s.shape);
            if (level < Levels.LIMIT_FROM && g.limit() != 0) check("no limit before level " + Levels.LIMIT_FROM, false, "" + level);
            if (level >= Levels.LIMIT_FROM && g.limit() < g.required + 3) check("limit leaves room at " + level, false, g.limit() + "/" + g.required);
        }
        check("every campaign level is solvable", unsolvable == 0, "" + unsolvable);
        check("*biggest level " + maxCells + " blocks, slowest build " + maxMs + " ms", maxCells >= 300 && maxCells <= 800, "" + maxCells);
        check("all shapes appear", shapesSeen.size() == Shapes.COUNT, "" + shapesSeen.size());
        check("level 1 is tiny", Levels.build(Levels.campaign(1)).board.removable() == 8, null);
        check("ice arrives at its level", Levels.build(Levels.campaign(Levels.ICE_FROM)).board.count(Board.ICE) > 0, null);
        check("stones arrive at their level", Levels.build(Levels.campaign(Levels.STONE_FROM)).board.count(Board.STONE) > 0, null);
        Game lk = Levels.build(Levels.campaign(Levels.LOCK_FROM));
        check("locks arrive with keys", lk.board.count(Board.LOCK) > 0 && lk.board.keys() > 0, null);
        check("tips for new things", Levels.tip(1) != null && Levels.tip(Levels.ICE_FROM) != null && Levels.tip(Shapes.INTRO[3]) != null, null);
        for (int sh = 0; sh < Shapes.COUNT; sh++) {
            for (int size = 0; size < 3; size++) {
                Game f = Levels.build(Levels.free(sh, size, 1234L + sh));
                if (!LevelGen.solvable(f.board) || f.limit() != 0) check("free play " + sh + "/" + size, false, null);
            }
        }
        check("free play builds", true, null);
    }

    static void catalog() {
        HashSet<String> ids = new HashSet<String>();
        for (Catalog.Skin s : Catalog.SKINS) ids.add(s.id);
        check("skin ids unique", ids.size() == Catalog.SKINS.length, null);
        check("first skin and background are free", Catalog.SKINS[0].price == 0 && Catalog.BACKGROUNDS[0].price == 0, null);
        check("unknown id falls back", Catalog.skin("nope") == Catalog.SKINS[0], null);
        String p = Catalog.withStars("", 3, 2);
        check("stars packed", p.equals("002") && Catalog.starsOf(p, 3) == 2 && Catalog.starsOf(p, 9) == 0, p);
        check("stars only go up", Catalog.withStars("3", 1, 1).equals("3"), null);
        check("total stars", Catalog.totalStars("312") == 6, null);
        check("first clear pays more than a replay", Catalog.reward(5, 3, 0) > Catalog.reward(5, 3, 3), null);
        for (Catalog.Skin s : Catalog.SKINS) {
            int c = Catalog.blockColor(s, 42L, 1, 2, 3, 5, 4);
            if (c < 0 || c > 0xFFFFFF) check("color in range for " + s.id, false, Integer.toHexString(c));
        }
    }

    static void mesh() {
        Catalog.Skin skin = Catalog.SKINS[0];
        int wrongWinding = 0, wrongArrow = 0;
        for (int d = 0; d < 6; d++) {
            Board b = new Board(3, 3, 3);
            int c = b.index(1, 1, 1);
            b.set(c, Board.NORMAL, d);
            float[] v = Mesh.block(b, c, skin, 1L);
            for (int t = 0; t < 12; t++) {
                int o = t * 3 * Mesh.FLOATS;
                float[] p0 = at(v, o), p1 = at(v, o + Mesh.FLOATS), p2 = at(v, o + 2 * Mesh.FLOATS);
                float[] e = {p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]}, f = {p2[0] - p0[0], p2[1] - p0[1], p2[2] - p0[2]};
                float cx = e[1] * f[2] - e[2] * f[1], cy = e[2] * f[0] - e[0] * f[2], cz = e[0] * f[1] - e[1] * f[0];
                if (cx * v[o + 3] + cy * v[o + 4] + cz * v[o + 5] <= 0) wrongWinding++;
            }
            for (int face = 0; face < 6; face++) {
                int o = face * 6 * Mesh.FLOATS;
                int nf = dirOf(v[o + 3], v[o + 4], v[o + 5]);
                if (nf == d || nf == (d ^ 1)) continue;
                float[] uv = Mesh.uv(Mesh.ARROW);
                for (int k = 0; k < 6; k++) {
                    int q = o + k * Mesh.FLOATS;
                    float along = (v[q] - 1.5f) * Board.DX[d] + (v[q + 1] - 1.5f) * Board.DY[d] + (v[q + 2] - 1.5f) * Board.DZ[d];
                    boolean top = Math.abs(v[q + 7] - uv[1]) < 1e-6f;
                    if (top != (along > 0)) wrongArrow++;
                }
            }
        }
        check("faces wind counter-clockwise from outside", wrongWinding == 0, "" + wrongWinding);
        check("arrows point along the block's direction", wrongArrow == 0, "" + wrongArrow);
        Board two = new Board(2, 1, 1);
        two.set(0, Board.NORMAL, 0);
        two.set(1, Board.NORMAL, 0);
        float[][] buf = {new float[16]};
        int n = Mesh.build(two, null, skin, 1L, buf);
        check("faces between neighbours are left out", n == 10 * 6 * Mesh.FLOATS, "" + n / (6 * Mesh.FLOATS));
        boolean[] hidden = {false, true};
        n = Mesh.build(two, hidden, skin, 1L, buf);
        check("a moving neighbour uncovers the face", n == 6 * 6 * Mesh.FLOATS, "" + n / (6 * Mesh.FLOATS));
        check("front face shows the dot", Mesh.faceCell(two, 0, 0) == Mesh.FRONT && Mesh.faceCell(two, 0, 1) == Mesh.BACK
                && Mesh.faceCell(two, 0, 2) == Mesh.ARROW, null);
    }

    static float[] at(float[] v, int o) {
        return new float[]{v[o], v[o + 1], v[o + 2]};
    }

    static int dirOf(float x, float y, float z) {
        for (int d = 0; d < 6; d++) if (Board.DX[d] == x && Board.DY[d] == y && Board.DZ[d] == z) return d;
        return -1;
    }
}
