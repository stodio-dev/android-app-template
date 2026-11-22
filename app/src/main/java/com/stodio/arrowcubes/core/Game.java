package com.stodio.arrowcubes.core;

import java.util.ArrayList;

/** One play of a level: the board, the move count and the rules of a tap. */
public final class Game {
    /** Tap results. */
    public static final int NONE = 0, EXIT = 1, BLOCKED = 2, CRACKED = 3, LOCKED = 4, FIXED = 5;

    public final Board board;
    /** Taps a perfect play needs: every movable block once, ice twice. */
    public final int required;
    private int limit, moves, wasted, hammers;
    /** After BLOCKED: the block in the way and the free cells before it. */
    public int lastBlocker = -1, lastGap;

    /** @param limit maximum taps, 0 for no limit */
    public Game(Board board, int limit) {
        this.board = board;
        this.limit = limit;
        required = board.removable() + board.count(Board.ICE);
    }

    public int tap(int i) {
        if (i < 0 || over()) return NONE;
        byte t = board.type(i);
        if (t == Board.EMPTY) return NONE;
        if (t == Board.STONE) return FIXED;
        if (board.isLocked(i)) return LOCKED;
        moves++;
        if (t == Board.ICE) {
            board.crack(i);
            return CRACKED;
        }
        int d = board.dir(i);
        int b = board.blocker(i, d);
        if (b < 0) {
            board.clear(i);
            return EXIT;
        }
        wasted++;
        lastBlocker = b;
        lastGap = board.gap(i, d);
        return BLOCKED;
    }

    /** The hammer booster: removes any movable block without using a move. */
    public boolean hammer(int i) {
        if (i < 0 || over()) return false;
        byte t = board.type(i);
        if (t == Board.EMPTY || t == Board.STONE) return false;
        board.clear(i);
        hammers++;
        return true;
    }

    public void addMoves(int n) {
        if (limit > 0) limit += n;
    }

    public boolean won() {
        return board.removable() == 0;
    }

    public boolean lost() {
        return limit > 0 && moves >= limit && !won();
    }

    public boolean over() {
        return won() || lost();
    }

    public int moves() {
        return moves;
    }

    public int limit() {
        return limit;
    }

    /** Moves left, or -1 when the level has no limit. */
    public int movesLeft() {
        return limit > 0 ? Math.max(0, limit - moves) : -1;
    }

    public int wasted() {
        return wasted;
    }

    public int hammers() {
        return hammers;
    }

    /** 3 stars for (almost) no wasted taps; the allowance grows with the level's size. A hammer caps it at 2. */
    public int stars() {
        if (!won()) return 0;
        int s;
        if (wasted <= required / 50) s = 3;
        else if (wasted <= Math.max(3, required / 10)) s = 2;
        else s = 1;
        if (hammers > 0 && s > 2) s = 2;
        return s;
    }

    /**
     * Blocks worth tapping now, the ones that slide out first, then ice that a tap would crack (blocked ice is still
     * useful to crack, so it comes last).
     */
    public int[] hints() {
        ArrayList<Integer> free = new ArrayList<Integer>(), iceFree = new ArrayList<Integer>(),
                iceBlocked = new ArrayList<Integer>();
        for (int i = 0; i < board.size(); i++) {
            byte t = board.type(i);
            if (t == Board.ICE) {
                if (board.blocker(i, board.dir(i)) < 0) iceFree.add(i);
                else iceBlocked.add(i);
            } else if (board.canExit(i)) {
                free.add(i);
            }
        }
        free.addAll(iceFree);
        free.addAll(iceBlocked);
        int[] r = new int[free.size()];
        for (int k = 0; k < r.length; k++) r[k] = free.get(k);
        return r;
    }
}
