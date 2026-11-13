import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Renders what's heard aboard the Grays' ship (50). All of it is synthesized here from sines and filtered noise, with no
 * samples, so every recording is ours:
 * <ul>
 * <li>{@code loop}: the ship's hum, a 24-second seamless bed (two low tones beating against each other, air moving in the
 *     ducts, a thin whine that comes and goes);</li>
 * <li>{@code music}: the ship's piece of the score, about two and a half minutes. It's the Hollow's chord (open E with
 *     the ninth) gone cold: the third is taken out and a note a semitone off is laid against it, over a slow pulse like a
 *     heart, with glass tones that never settle;</li>
 * <li>{@code field_drop}, {@code alarm}, {@code groan}, {@code pod_launch}, {@code pod_crash}: the short ones.</li>
 * </ul>
 * Run from the project root:  java tools/music/ShipSounds.java build/ship
 * It writes one WAV each; convert them with ffmpeg (ASSETS.md, Music).
 */
public class ShipSounds {
    static final int RATE = 44100;

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/ship");
        out.mkdirs();
        loop(out);
        music(out);
        mono(out, "field_drop", fieldDrop());
        mono(out, "alarm", alarm());
        mono(out, "groan", groan());
        mono(out, "pod_launch", podLaunch());
        mono(out, "pod_crash", podCrash());
    }

    // ---- the bed ----

    static void loop(File out) throws Exception {
        double loop = 24.0, lap = 3.0;
        int n = (int) ((loop + lap) * RATE);
        double[][] ch = new double[2][n];
        for (int c = 0; c < 2; c++) {
            double[] air = lowpass(noise(n, 501 + c), 420);
            double[] duct = bandpass(noise(n, 503 + c), 1700, 6);
            for (int i = 0; i < n; i++) {
                double t = (double) i / RATE;
                // Every tone fits the loop a whole number of times, and so does every slow change
                double hum = Math.sin(2 * Math.PI * whole(55.0, loop) * t) * 0.50
                        + Math.sin(2 * Math.PI * whole(55.0 + 0.25 + 0.125 * c, loop) * t) * 0.42
                        + Math.sin(2 * Math.PI * whole(82.5, loop) * t) * 0.20
                        + Math.sin(2 * Math.PI * whole(110.0, loop) * t) * 0.08;
                double swell = 0.75 + 0.25 * Math.sin(2 * Math.PI * t / loop * 2 + c);
                double whine = Math.sin(2 * Math.PI * whole(2310.0, loop) * t) * 0.012 * Math.pow(0.5 + 0.5 * Math.sin(2 * Math.PI * t / loop * 3 + 1.3 * c), 3);
                ch[c][i] = hum * 0.5 * swell + air[i] * 0.9 * (0.7 + 0.3 * Math.sin(2 * Math.PI * t / loop + 2.0 * c))
                        + duct[i] * 0.35 * (0.5 + 0.5 * Math.sin(2 * Math.PI * t / loop * 5 + c)) + whine;
            }
        }
        // The noise isn't periodic, so the tail is folded back over the start
        int body = (int) (loop * RATE), tail = n - body;
        double[][] folded = new double[2][body];
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < body; i++) {
                folded[c][i] = ch[c][i];
            }
            for (int i = 0; i < tail; i++) {
                double f = (double) i / tail;
                folded[c][i] = ch[c][i] * f + ch[c][body + i] * (1 - f);
            }
        }
        stereo(new File(out, "loop.wav"), folded[0], folded[1], 0.5);
    }

    static double whole(double hz, double loop) {
        return Math.round(hz * loop) / loop;
    }

    // ---- the piece ----

    static void music(File out) throws Exception {
        double length = 150.0;
        int n = (int) (length * RATE);
        double[] left = new double[n], right = new double[n];
        // E2 B2 F#3 with no third, and against it a note that doesn't belong: each chord leans a different way
        double e = 82.41, b = 123.47, fs = 185.0;
        double[][] chords = {
                {e, b, fs, fs * 2 * 1.0595},          // the ninth, and a semitone over it
                {e, b * 1.0595, fs, e * 4},           // the fifth pushed sharp
                {e * 0.9439, b, fs, fs * 2},          // the root sunk a semitone
                {e, b, fs * 1.0595, b * 4 * 0.9439},  // nothing where it should be
        };
        double span = 25.0;
        for (int k = 0; k * (span - 6) < length; k++) {
            double start = k * (span - 6);
            double[] chord = chords[k % chords.length];
            for (int v = 0; v < chord.length; v++) {
                pad(left, right, start, span, chord[v], 0.10 / (1 + v * 0.35), 0.2 + 0.2 * v);
            }
        }
        // The pulse: two soft beats and a rest, slowing nothing down and never stopping
        for (double t = 4.0; t < length - 6; t += 3.4) {
            thump(left, right, t, 46.0, 0.30);
            thump(left, right, t + 0.42, 44.0, 0.20);
        }
        // Glass: single high tones, each a little flat or sharp of the chord, far apart
        Random random = new Random(1897);
        double[] glass = {e * 8, b * 8, fs * 8, e * 16, fs * 4 * 1.0595, b * 8 * 0.9439};
        for (double t = 9.0; t < length - 12; t += 5.5 + random.nextDouble() * 6.0) {
            double hz = glass[random.nextInt(glass.length)] * (1 + (random.nextDouble() - 0.5) * 0.012);
            ping(left, right, t, hz, 0.05 + random.nextDouble() * 0.04, random.nextDouble());
        }
        hall(left);
        hall(right);
        // In and out slowly: it has no beginning to speak of
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE, env = Math.min(1, t / 6.0) * Math.min(1, (length - t) / 9.0);
            left[i] *= env;
            right[i] *= env;
        }
        stereo(new File(out, "music.wav"), left, right, 0.8);
    }

    static void pad(double[] left, double[] right, double start, double span, double hz, double level, double pan) {
        int from = (int) (start * RATE), count = (int) (span * RATE);
        for (int i = 0; i < count && from + i < left.length; i++) {
            double t = (double) i / RATE;
            double env = Math.pow(Math.sin(Math.PI * t / span), 1.5);
            double s = 0;
            for (int d = -1; d <= 1; d++) { // three copies a few cents apart, beating slowly
                double f = hz * (1 + d * 0.0028);
                s += Math.sin(2 * Math.PI * f * t + d) + 0.35 * Math.sin(2 * Math.PI * f * 2 * t) + 0.12 * Math.sin(2 * Math.PI * f * 3 * t + 1.1);
            }
            s *= level * env * (0.85 + 0.15 * Math.sin(2 * Math.PI * 0.11 * t + pan * 6));
            left[from + i] += s * (1 - pan * 0.6);
            right[from + i] += s * (0.4 + pan * 0.6);
        }
    }

    static void thump(double[] left, double[] right, double at, double hz, double level) {
        int from = (int) (at * RATE), count = (int) (0.9 * RATE);
        for (int i = 0; i < count && from + i < left.length; i++) {
            double t = (double) i / RATE;
            double s = Math.sin(2 * Math.PI * (hz + 30 * Math.exp(-t * 30)) * t) * Math.exp(-t * 7) * level;
            left[from + i] += s;
            right[from + i] += s;
        }
    }

    static void ping(double[] left, double[] right, double at, double hz, double level, double pan) {
        int from = (int) (at * RATE), count = (int) (7.0 * RATE);
        for (int i = 0; i < count && from + i < left.length; i++) {
            double t = (double) i / RATE;
            double s = (Math.sin(2 * Math.PI * hz * t) + 0.3 * Math.sin(2 * Math.PI * hz * 2.76 * t) * Math.exp(-t * 3))
                    * Math.exp(-t * 0.9) * Math.min(1, t * 200) * level;
            left[from + i] += s * (1 - pan);
            right[from + i] += s * pan;
        }
    }

    /** A plain hall: four feedback combs and two all-passes (Schroeder), mixed back in under the dry signal. */
    static void hall(double[] x) {
        int[] combs = {1557, 1617, 1491, 1422};
        double[] wet = new double[x.length];
        for (int delay : combs) {
            double[] buffer = new double[delay];
            for (int i = 0; i < x.length; i++) {
                double y = buffer[i % delay];
                buffer[i % delay] = x[i] + y * 0.86;
                wet[i] += y * 0.25;
            }
        }
        for (int delay : new int[] {225, 556}) {
            double[] buffer = new double[delay];
            for (int i = 0; i < x.length; i++) {
                double delayed = buffer[i % delay], in = wet[i] + delayed * 0.5;
                buffer[i % delay] = in;
                wet[i] = delayed - in * 0.5;
            }
        }
        for (int i = 0; i < x.length; i++) {
            x[i] = x[i] * 0.7 + wet[i] * 0.5;
        }
    }

    // ---- the short ones ----

    /** A field coming down: a bright tone falling fast through a fizz of static. */
    static double[] fieldDrop() {
        int n = (int) (0.7 * RATE);
        double[] x = new double[n], fizz = bandpass(noise(n, 61), 4200, 3);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE, hz = 180 + 1900 * Math.exp(-t * 9);
            phase += 2 * Math.PI * hz / RATE;
            x[i] = (Math.sin(phase) * 0.7 + Math.sin(phase * 1.5) * 0.2) * Math.exp(-t * 6) + fizz[i] * 2.5 * Math.exp(-t * 14);
        }
        return x;
    }

    /** The ship's alarm: two notes a tritone apart, four times, each with a ring to it that no brass bell has. */
    static double[] alarm() {
        int n = (int) (1.9 * RATE);
        double[] x = new double[n];
        for (int k = 0; k < 4; k++) {
            int from = (int) (k * 0.44 * RATE), count = (int) (0.40 * RATE);
            double hz = k % 2 == 0 ? 622.25 : 440.0;
            for (int i = 0; i < count && from + i < n; i++) {
                double t = (double) i / RATE, env = Math.min(1, t * 80) * Math.exp(-t * 3.2);
                double ring = Math.sin(2 * Math.PI * 31 * t);
                x[from + i] += (Math.sin(2 * Math.PI * hz * t) * (0.7 + 0.3 * ring) + 0.25 * Math.sin(2 * Math.PI * hz * 2.01 * t) + 0.12 * Math.sin(2 * Math.PI * hz * 3.02 * t)) * env;
            }
        }
        return x;
    }

    /** The hull giving: a low note bending under load, and metal complaining over it. */
    static double[] groan() {
        int n = (int) (2.8 * RATE);
        double[] x = new double[n], rumble = lowpass(noise(n, 71), 160), creak = bandpass(noise(n, 73), 520, 14);
        double phase = 0, phase2 = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE, env = Math.min(1, t * 6) * Math.exp(-Math.max(0, t - 0.8) * 1.6);
            double hz = 62 - 16 * t / 2.8 + 5 * Math.sin(2 * Math.PI * 1.7 * t);
            phase += 2 * Math.PI * hz / RATE;
            phase2 += 2 * Math.PI * (hz * 2.03) / RATE;
            double stick = Math.pow(0.5 + 0.5 * Math.sin(2 * Math.PI * 9 * t + 3 * Math.sin(2 * Math.PI * 0.8 * t)), 4);
            x[i] = (Math.sin(phase) * 0.8 + Math.sin(phase2) * 0.25 + rumble[i] * 3.0 + creak[i] * 5.0 * stick) * env;
        }
        return x;
    }

    /** A pod let go: a thump underfoot, then a rush that climbs out of hearing. */
    static double[] podLaunch() {
        int n = (int) (3.2 * RATE);
        double[] x = new double[n], rush = noise(n, 81);
        double[] swept = sweep(rush, 220, 3600, 2.5);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double thump = Math.sin(2 * Math.PI * (48 + 40 * Math.exp(-t * 20)) * t) * Math.exp(-t * 5) * 1.2;
            phase += 2 * Math.PI * (90 + 700 * Math.pow(t / 3.2, 2)) / RATE;
            double env = Math.min(1, t * 3) * Math.min(1, (3.2 - t) * 2.5);
            x[i] = thump + (swept[i] * 4.0 + Math.sin(phase) * 0.25) * env;
        }
        return x;
    }

    /** A pod meeting the ground: one very large blow, earth thrown up, and things still falling after. */
    static double[] podCrash() {
        int n = (int) (3.6 * RATE);
        double[] x = new double[n], earth = lowpass(noise(n, 91), 900), grit = bandpass(noise(n, 93), 2600, 2);
        Random random = new Random(95);
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double blow = Math.sin(2 * Math.PI * (38 + 70 * Math.exp(-t * 18)) * t) * Math.exp(-t * 3.2) * 1.4;
            x[i] = blow + earth[i] * 5.0 * Math.exp(-t * 4.5) + grit[i] * 1.5 * Math.exp(-t * 9);
        }
        for (int k = 0; k < 26; k++) { // what's still coming down
            int from = (int) ((0.5 + random.nextDouble() * 2.6) * RATE), count = (int) (0.05 * RATE);
            double level = 0.25 * Math.exp(-(double) from / RATE);
            for (int i = 0; i < count && from + i < n; i++) {
                x[from + i] += (random.nextDouble() * 2 - 1) * level * Math.exp(-(double) i / RATE * 90);
            }
        }
        return x;
    }

    // ---- the works ----

    static double[] noise(int n, long seed) {
        Random random = new Random(seed);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = random.nextDouble() * 2 - 1;
        }
        return x;
    }

    static double[] lowpass(double[] x, double hz) {
        return biquad(x, hz, 0.707, 0);
    }

    static double[] bandpass(double[] x, double hz, double q) {
        return biquad(x, hz, q, 1);
    }

    /** A band-pass whose middle climbs from one frequency to another over the sound. */
    static double[] sweep(double[] x, double fromHz, double toHz, double q) {
        double[] y = new double[x.length];
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < x.length; i++) {
            double hz = fromHz * Math.pow(toHz / fromHz, (double) i / x.length);
            double w = 2 * Math.PI * hz / RATE, alpha = Math.sin(w) / (2 * q), a0 = 1 + alpha;
            double b0 = alpha / a0, b2 = -alpha / a0, a1 = -2 * Math.cos(w) / a0, a2 = (1 - alpha) / a0;
            double out = b0 * x[i] + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x[i];
            y2 = y1;
            y1 = out;
            y[i] = out;
        }
        return y;
    }

    /** One RBJ biquad: kind 0 a low-pass, 1 a band-pass. */
    static double[] biquad(double[] x, double hz, double q, int kind) {
        double w = 2 * Math.PI * hz / RATE, cos = Math.cos(w), alpha = Math.sin(w) / (2 * q), a0 = 1 + alpha;
        double b0, b1, b2;
        if (kind == 0) {
            b0 = (1 - cos) / 2 / a0;
            b1 = (1 - cos) / a0;
            b2 = b0;
        } else {
            b0 = alpha / a0;
            b1 = 0;
            b2 = -alpha / a0;
        }
        double a1 = -2 * cos / a0, a2 = (1 - alpha) / a0;
        double[] y = new double[x.length];
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < x.length; i++) {
            double out = b0 * x[i] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x[i];
            y2 = y1;
            y1 = out;
            y[i] = out;
        }
        return y;
    }

    /** A short sound, mono, its loudest moment at -1 dBFS, with the last few milliseconds faded so it can't click. */
    static void mono(File out, String name, double[] x) throws Exception {
        double peak = 1e-9;
        for (double v : x) {
            peak = Math.max(peak, Math.abs(v));
        }
        int fade = RATE / 100;
        byte[] bytes = new byte[x.length * 2];
        for (int i = 0; i < x.length; i++) {
            double edge = Math.min(1.0, Math.min((double) i / 60, (double) (x.length - 1 - i) / fade));
            int s = (int) Math.round(x[i] / peak * 0.89 * edge * 32767);
            bytes[i * 2] = (byte) s;
            bytes[i * 2 + 1] = (byte) (s >> 8);
        }
        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(bytes), format, x.length), AudioFileFormat.Type.WAVE, new File(out, name + ".wav"));
    }

    static void stereo(File file, double[] left, double[] right, double peakTo) throws Exception {
        double peak = 1e-9;
        for (int i = 0; i < left.length; i++) {
            peak = Math.max(peak, Math.max(Math.abs(left[i]), Math.abs(right[i])));
        }
        byte[] bytes = new byte[left.length * 4];
        for (int i = 0; i < left.length; i++) {
            int l = (int) Math.round(left[i] / peak * peakTo * 32767), r = (int) Math.round(right[i] / peak * peakTo * 32767);
            bytes[i * 4] = (byte) l;
            bytes[i * 4 + 1] = (byte) (l >> 8);
            bytes[i * 4 + 2] = (byte) r;
            bytes[i * 4 + 3] = (byte) (r >> 8);
        }
        AudioFormat format = new AudioFormat(RATE, 16, 2, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(bytes), format, left.length), AudioFileFormat.Type.WAVE, file);
    }
}
