package com.stodio.app.core;

/**
 * An example of the core package: plain Java, no android.* imports, so core-tests checks it on a plain JDK in the
 * cloud before every build. Put the app's logic here (parsing, calculations, formats) and keep the screens thin.
 */
public final class Greeting {
    private Greeting() {}

    public static String hello(String app) {
        return "שלום מ-" + app + ". התבנית עובדת: האפליקציה נבנתה בענן והותקנה מהלינק.";
    }
}
