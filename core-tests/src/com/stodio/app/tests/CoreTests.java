package com.stodio.app.tests;

import com.stodio.app.core.Greeting;

/** Checks of the core package on a plain JDK: bash core-tests/run.sh. The workflow runs them before every build. */
public final class CoreTests {
    private static int passed, failed;

    static void check(String name, boolean ok, String detail) {
        if (ok) passed++;
        else failed++;
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok || detail == null ? "" : ": " + detail));
    }

    public static void main(String[] args) {
        String h = Greeting.hello("X");
        check("greeting names the app", h.contains("X"), h);
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
