package com.stodio.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Settings, a worker thread, an in-memory log the user can share from the app, and a crash report kept for the next
 * start (there is no adb from a cloud session, so the log is how problems on the phone get back to Claude).
 */
public final class App extends Application {
    static final String TAG = "App";
    private static final ArrayDeque<String> LOG = new ArrayDeque<String>();
    private static final int LOG_MAX = 400;
    /** Background work (files, network). */
    static final ExecutorService BG = Executors.newFixedThreadPool(2);
    static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static App app;

    @Override public void onCreate() {
        super.onCreate();
        app = this;
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                try {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String txt = header() + "crashed " + stamp() + ", thread " + t.getName() + "\n\n" + sw + "\n\nlog:\n" + logText();
                    FileOutputStream o = new FileOutputStream(crashFile(App.this));
                    o.write(txt.getBytes("UTF-8"));
                    o.close();
                } catch (Throwable ignored) {
                    // nothing more to do while crashing
                }
                if (prev != null) prev.uncaughtException(t, e);
            }
        });
        log("start " + version(this));
    }

    static App get() {
        return app;
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("app", MODE_PRIVATE);
    }

    static void ui(Runnable r) {
        MAIN.post(r);
    }

    /** versionName (versionCode), e.g. "0.1.0 (12)": the build number is the commit count. */
    static String version(Context c) {
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return p.versionName + " (" + p.getLongVersionCode() + ")";
        } catch (Exception e) {
            return "?";
        }
    }

    static String header() {
        return app.getString(R.string.app_name) + " " + version(app) + "\n" + Build.MANUFACTURER + " " + Build.MODEL
                + ", Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n";
    }

    static File crashFile(Context c) {
        return new File(c.getFilesDir(), "crash.txt");
    }

    static String stamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    static void log(String msg) {
        Log.i(TAG, msg);
        synchronized (LOG) {
            LOG.addLast(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + " " + msg);
            while (LOG.size() > LOG_MAX) LOG.removeFirst();
        }
    }

    static void log(String msg, Throwable e) {
        Log.w(TAG, msg, e);
        log(msg + ": " + e);
    }

    static String logText() {
        StringBuilder b = new StringBuilder();
        synchronized (LOG) {
            for (String s : LOG) b.append(s).append('\n');
        }
        return b.toString();
    }
}
