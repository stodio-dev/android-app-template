package com.stodio.arrowcubes.core;

import java.util.ArrayList;
import java.util.Random;

/**
 * Turns a shape into a puzzle that always has a solution. It plays the game forward on a working copy: at every step
 * it picks a block with at least one clear way out, gives it that direction and removes it. Played in that order the
 * real board empties completely, and since removing a block never blocks another, any order the player finds works.
 */
public final class LevelGen {
    private LevelGen() {}

    public static final class Result {
        public final Board board;
        /** One removal order that clears the board (key blocks all come before locks). */
        public final int[] order;
        /** Cells taken out of the shape because stones walled them in. */
        public final int dropped;

        Result(Board board, int[] order, int dropped) {
            this.board = board;
            this.order = order;
            this.dropped = dropped;
        }
    }

    /**
     * @param difficulty 0..1: higher prefers directions that cross more blocks, so fewer blocks are free at the start
     *                   and the chains are longer
     */
    public static Result generate(Shapes.Grid g, int stones, int ice, int keys, int locks, float difficulty,
                                  Random rnd) {
        Board b = new Board(g.sx, g.sy, g.sz);
        ArrayList<Integer> cells = new ArrayList<Integer>();
        for (int i = 0; i < g.on.length; i++) {
            if (g.on[i]) {
                b.set(i, Board.NORMAL, 0);
                cells.add(i);
            }
        }
        // stones never on cells whose loss would leave too few blocks
        stones = Math.min(stones, cells.size() / 6);
        ArrayList<Integer> pool = new ArrayList<Integer>(cells);
        for (int k = 0; k < stones && !pool.isEmpty(); k++) {
            int c = pool.remove(rnd.nextInt(pool.size()));
            b.set(c, Board.STONE, 0);
        }
        Board full = b.copy();
        Board work = b.copy();
        ArrayList<Integer> alive = new ArrayList<Integer>();
        for (int c : cells) if (b.type(c) == Board.NORMAL) alive.add(c);

        int[] order = new int[alive.size()];
        int n = 0, dropped = 0;
        int tournament = 1 + Math.round(Math.max(0f, Math.min(1f, difficulty)) * 5);
        int[] candCell = new int[alive.size() * 6], candDir = new int[alive.size() * 6];
        while (!alive.isEmpty()) {
            int cands = 0;
            for (int a = 0; a < alive.size(); a++) {
                int c = alive.get(a);
                for (int d = 0; d < 6; d++) {
                    if (work.blocker(c, d) < 0) {
                        candCell[cands] = a;
                        candDir[cands] = d;
                        cands++;
                    }
                }
            }
            if (cands == 0) { // walled in by stones: take a block out of the shape
                int a = rnd.nextInt(alive.size());
                int c = alive.remove(a);
                work.clear(c);
                b.clear(c);
                dropped++;
                continue;
            }
            int bestA = -1, bestD = -1;
            float bestScore = -1;
            for (int t = 0; t < tournament; t++) {
                int k = rnd.nextInt(cands);
                int c = alive.get(candCell[k]);
                float s = crossed(full, c, candDir[k]) + rnd.nextFloat() * 0.5f;
                if (s > bestScore) {
                    bestScore = s;
                    bestA = candCell[k];
                    bestD = candDir[k];
                }
            }
            int c = alive.remove(bestA);
            b.setDir(c, bestD);
            work.clear(c);
            order[n++] = c;
        }
        if (n < order.length) {
            int[] o = new int[n];
            System.arraycopy(order, 0, o, 0, n);
            order = o;
        }

        // keys from the first part of the order, locks from the last, so all keys can leave before any lock must
        int len = order.length;
        if (len >= 12) {
            keys = Math.min(keys, len / 6);
            locks = Math.min(locks, len / 5);
            ArrayList<Integer> early = new ArrayList<Integer>(), late = new ArrayList<Integer>();
            for (int i = 0; i < len * 2 / 5; i++) early.add(order[i]);
            for (int i = len - len * 2 / 5; i < len; i++) late.add(order[i]);
            if (keys > 0 && locks > 0) {
                for (int k = 0; k < keys && !early.isEmpty(); k++) {
                    int c = early.remove(rnd.nextInt(early.size()));
                    b.set(c, Board.KEY, b.dir(c));
                }
                for (int k = 0; k < locks && !late.isEmpty(); k++) {
                    int c = late.remove(rnd.nextInt(late.size()));
                    b.set(c, Board.LOCK, b.dir(c));
                }
            }
        }
        ArrayList<Integer> plain = new ArrayList<Integer>();
        for (int i = 0; i < len; i++) if (b.type(order[i]) == Board.NORMAL) plain.add(order[i]);
        ice = Math.min(ice, plain.size() / 3);
        for (int k = 0; k < ice; k++) {
            int c = plain.remove(rnd.nextInt(plain.size()));
            b.set(c, Board.ICE, b.dir(c));
        }
        return new Result(b, order, dropped);
    }

    /** Blocks of the full shape on the way out from c in direction d: how many must leave before c can. */
    private static int crossed(Board full, int c, int d) {
        int x = full.x(c) + Board.DX[d], y = full.y(c) + Board.DY[d], z = full.z(c) + Board.DZ[d];
        int n = 0;
        while (full.inside(x, y, z)) {
            if (full.occupied(full.index(x, y, z))) n++;
            x += Board.DX[d];
            y += Board.DY[d];
            z += Board.DZ[d];
        }
        return n;
    }

    /** True when some order clears every movable block (greedy is exact: removing a block never blocks another). */
    public static boolean solvable(Board start) {
        Board b = start.copy();
        boolean moved = true;
        while (moved && b.removable() > 0) {
            moved = false;
            for (int i = 0; i < b.size(); i++) {
                if (b.type(i) == Board.ICE) b.crack(i);
                if (b.canExit(i)) {
                    b.clear(i);
                    moved = true;
                }
            }
        }
        return b.removable() == 0;
    }

    /** Blocks that can leave at the start (a rough difficulty measure: fewer is harder). */
    public static int freeAtStart(Board b) {
        int n = 0;
        for (int i = 0; i < b.size(); i++) if (b.canExit(i)) n++;
        return n;
    }

    /** Rounds of "remove everything free" needed to clear the board: the length of the longest chain. */
    public static int depth(Board start) {
        Board b = start.copy();
        int rounds = 0;
        while (b.removable() > 0) {
            ArrayList<Integer> free = new ArrayList<Integer>();
            for (int i = 0; i < b.size(); i++) {
                if (b.type(i) == Board.ICE) b.crack(i);
                if (b.canExit(i)) free.add(i);
            }
            if (free.isEmpty()) return -1;
            for (int i : free) b.clear(i);
            rounds++;
        }
        return rounds;
    }
}
