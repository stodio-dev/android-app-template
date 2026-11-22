package com.stodio.arrowcubes;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.Random;

/** Short sounds synthesized at start (no audio files): one static AudioTrack per sound, replayed from the start. */
final class Sfx {
    static final int POP = 0, BUMP = 1, CRACK = 2, DENY = 3, WIN = 4, LOSE = 5, HINT = 6, COIN = 7;
    private static final int RATE = 22050;
    private static final AudioTrack[] TRACKS = new AudioTrack[8];
    static volatile boolean on = true;

    private Sfx() {}

    /** Builds the sounds in the background; play() is silent until then. */
    static void init() {
        App.BG.execute(new Runnable() {
            @Override public void run() {
                try {
                    make(POP, pop());
                    make(BUMP, bump());
                    make(CRACK, crack());
                    make(DENY, deny());
                    make(WIN, notes(new float[]{523.25f, 659.25f, 783.99f, 1046.5f}, 0.11f, 0.35f));
                    make(LOSE, notes(new float[]{392f, 329.63f, 261.63f}, 0.16f, 0.3f));
                    make(HINT, notes(new float[]{1318.5f, 1760f}, 0.09f, 0.18f));
                    make(COIN, notes(new float[]{1567.98f, 2093f}, 0.06f, 0.2f));
                } catch (Throwable e) {
                    App.log("sound setup failed", e);
                }
            }
        });
    }

    static void play(int id) {
        if (!on) return;
        AudioTrack t;
        synchronized (TRACKS) {
            t = TRACKS[id];
        }
        if (t == null) return;
        try {
            if (t.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) t.stop();
            t.reloadStaticData();
            t.play();
        } catch (Exception e) {
            App.log("sound " + id + " failed", e);
        }
    }

    private static void make(int id, short[] pcm) {
        AudioTrack t = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.length * 2)
                .build();
        t.write(pcm, 0, pcm.length);
        synchronized (TRACKS) {
            TRACKS[id] = t;
        }
    }

    /** A quick rising "pop" for a block that leaves. */
    private static short[] pop() {
        int n = (int) (RATE * 0.13f);
        short[] s = new short[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double f = 520 + 900 * t;
            ph += 2 * Math.PI * f / RATE;
            double env = Math.min(1, i / 60.0) * Math.exp(-5 * t);
            s[i] = (short) (Math.sin(ph) * env * 0.42 * 32767);
        }
        return s;
    }

    /** A low thud for a blocked block. */
    private static short[] bump() {
        int n = (int) (RATE * 0.12f);
        short[] s = new short[n];
        double ph = 0;
        Random r = new Random(3);
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            ph += 2 * Math.PI * (170 - 90 * t) / RATE;
            double env = Math.min(1, i / 40.0) * Math.exp(-6 * t);
            s[i] = (short) ((Math.sin(ph) * 0.8 + (r.nextDouble() - 0.5) * 0.25) * env * 0.5 * 32767);
        }
        return s;
    }

    /** Breaking ice: a bright noise burst. */
    private static short[] crack() {
        int n = (int) (RATE * 0.1f);
        short[] s = new short[n];
        Random r = new Random(5);
        double prev = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double w = r.nextDouble() * 2 - 1;
            double hp = w - prev; // crude high-pass: crisper
            prev = w;
            s[i] = (short) (hp * Math.exp(-7 * t) * 0.3 * 32767);
        }
        return s;
    }

    /** Two dull clicks: locked, or a stone. */
    private static short[] deny() {
        int n = (int) (RATE * 0.16f);
        short[] s = new short[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double local = t < 0.07 ? t : t - 0.08;
            if (local < 0) continue;
            double env = Math.exp(-local * 60);
            s[i] = (short) (Math.sin(2 * Math.PI * 220 * t) * env * 0.4 * 32767);
        }
        return s;
    }

    private static short[] notes(float[] freqs, float each, float tail) {
        int step = (int) (RATE * each), n = step * (freqs.length - 1) + (int) (RATE * tail);
        double[] mix = new double[n];
        for (int k = 0; k < freqs.length; k++) {
            int start = k * step, len = n - start;
            for (int i = 0; i < len; i++) {
                double t = i / (double) RATE;
                double env = Math.min(1, i / 80.0) * Math.exp(-t * 9);
                mix[start + i] += (Math.sin(2 * Math.PI * freqs[k] * t) + 0.3 * Math.sin(4 * Math.PI * freqs[k] * t)) * env;
            }
        }
        short[] s = new short[n];
        for (int i = 0; i < n; i++) s[i] = (short) (Math.max(-1, Math.min(1, mix[i] * 0.3)) * 32767);
        return s;
    }
}
