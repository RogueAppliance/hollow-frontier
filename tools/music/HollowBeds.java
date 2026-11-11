import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Renders the Hollow Earth's beds (45b): the sound that is always there under a biome's one-shots. Seven seamless
 * loops made from filtered noise and a few sines, no samples: one for each of the five biomes, the Still Sea heard from
 * under water, and the Steam Submersible's thrum.
 * <p>
 * How a loop is kept seamless: every slow change runs a whole number of times in the loop, every tone fits a whole
 * number of cycles, events near the end wrap round to the start, and the noise itself is rendered a few seconds long
 * and cross-faded back into its own beginning.
 * Run from the project root:  java tools/music/HollowBeds.java build/beds
 * It writes one WAV per bed; convert them with ffmpeg (ASSETS.md, Music).
 */
public class HollowBeds {
    static final int RATE = 44100;
    static final double LAP = 3.0; // seconds of noise cross-faded from the end back into the start

    interface Bed {
        /** Fills left and right; they're {@code LAP} longer than the loop, and the tail is folded in afterwards. */
        void render(double[] left, double[] right, double loop);
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/beds");
        out.mkdirs();
        write(out, "primordial_jungle", 32, HollowBeds::jungle);
        write(out, "crystal_steppes", 32, HollowBeds::steppes);
        write(out, "ember_badlands", 32, HollowBeds::badlands);
        write(out, "concavian_highlands", 36, HollowBeds::highlands);
        write(out, "still_sea", 32, HollowBeds::sea);
        write(out, "still_sea_deep", 32, HollowBeds::deep);
        write(out, "submersible_thrum", 2, HollowBeds::thrum);
    }

    // ---- the beds ----

    /** Close, wet and alive: still air under the leaves, two bands of insects shimmering, water dripping somewhere. */
    static void jungle(double[] left, double[] right, double loop) {
        int n = left.length;
        for (int ch = 0; ch < 2; ch++) {
            double[] mix = ch == 0 ? left : right;
            double[] air = lowpass(noise(n, 11 + ch), 380, 0.7);
            double[] high = bandpass(noise(n, 13 + ch), 5200, 9);
            double[] mid = bandpass(noise(n, 15 + ch), 3500, 7);
            for (int i = 0; i < n; i++) {
                double t = (double) i / RATE;
                mix[i] += air[i] * 0.55 * (0.7 + 0.3 * turn(t, loop, 1, ch));
                // Insects: each band trembles at its own rate and swells and thins over a few seconds
                double shimmer = 0.5 + 0.5 * Math.sin(2 * Math.PI * 38 * t);
                double rasp = 0.5 + 0.5 * Math.sin(2 * Math.PI * 23 * t + 1.0);
                mix[i] += high[i] * 1.6 * shimmer * (0.55 + 0.45 * turn(t, loop, 3, 0.5 + ch)) * (ch == 0 ? 1.0 : 0.6);
                mix[i] += mid[i] * 1.2 * rasp * (0.5 + 0.5 * turn(t, loop, 4, 2.0 + ch)) * (ch == 0 ? 0.6 : 1.0);
            }
        }
        Random random = new Random(45);
        for (int k = 0; k < 13; k++) {
            drip(left, right, loop, random.nextDouble() * loop, 900 + random.nextDouble() * 1100, 0.10 + random.nextDouble() * 0.10, random.nextDouble());
        }
    }

    /** Open and bright: a wide wind that rises and falls, and under it the crystals' own faint, beating hum. */
    static void steppes(double[] left, double[] right, double loop) {
        int n = left.length;
        for (int ch = 0; ch < 2; ch++) {
            double[] mix = ch == 0 ? left : right;
            final int c = ch;
            double[] wind = sweep(noise(n, 21 + ch), t -> 520 * Math.pow(2, 0.6 * turn(t, loop, 2, c * 0.7)), 1.3);
            double[] airy = highpass(noise(n, 23 + ch), 2500, 0.7);
            for (int i = 0; i < n; i++) {
                double t = (double) i / RATE;
                double gust = 0.45 + 0.55 * Math.pow(0.5 + 0.5 * turn(t, loop, 3, 1.0 + ch * 0.4), 1.5);
                mix[i] += wind[i] * 1.2 * gust + airy[i] * 0.05 * gust;
                // The hum: E, B and F sharp far up, each a pair a fraction of a hertz apart so they beat slowly
                mix[i] += 0.010 * (0.6 + 0.4 * turn(t, loop, 5, ch)) * (tone(t, loop, 1318.5, 0) + tone(t, loop, 1318.5, 7 + ch)
                        + 0.7 * tone(t, loop, 1975.5, 0) + 0.7 * tone(t, loop, 1975.5, 5 + ch)
                        + 0.4 * tone(t, loop, 2960.0, 0) + 0.4 * tone(t, loop, 2960.0, 9 + ch));
            }
        }
    }

    /** Low and dry: the ground's own rumble, a hiss of heat that comes in gusts, and cinders cracking. */
    static void badlands(double[] left, double[] right, double loop) {
        int n = left.length;
        for (int ch = 0; ch < 2; ch++) {
            double[] mix = ch == 0 ? left : right;
            double[] rumble = lowpass(lowpass(noise(n, 31 + ch), 75, 0.8), 110, 0.7);
            double[] hiss = highpass(noise(n, 33 + ch), 3200, 0.7);
            for (int i = 0; i < n; i++) {
                double t = (double) i / RATE;
                mix[i] += rumble[i] * 9.0 * (0.7 + 0.3 * turn(t, loop, 2, ch));
                double gust = Math.max(0.0, turn(t, loop, 3, 0.8 + ch * 0.5));
                mix[i] += hiss[i] * 0.10 * (0.15 + 0.85 * gust * gust);
            }
        }
        Random random = new Random(46);
        for (int k = 0; k < 44; k++) {
            crackle(left, right, loop, random.nextDouble() * loop, 0.04 + random.nextDouble() * 0.10, random.nextDouble(), random.nextLong());
        }
    }

    /** Thin and high: wind finding its way round old stone, whistling a little when it gets up. */
    static void highlands(double[] left, double[] right, double loop) {
        int n = left.length;
        for (int ch = 0; ch < 2; ch++) {
            double[] mix = ch == 0 ? left : right;
            final int c = ch;
            double[] body = sweep(noise(n, 41 + ch), t -> 850 * Math.pow(2, 0.7 * turn(t, loop, 2, c * 0.9) + 0.3 * turn(t, loop, 5, 1.0)), 5);
            double[] whistle = sweep(noise(n, 43 + ch), t -> 1750 * Math.pow(2, 0.35 * turn(t, loop, 3, 2.0 + c)), 14);
            double[] air = lowpass(noise(n, 45 + ch), 300, 0.7);
            for (int i = 0; i < n; i++) {
                double t = (double) i / RATE;
                double blow = Math.pow(0.5 + 0.5 * turn(t, loop, 3, 0.3 + ch * 0.3), 2);
                mix[i] += body[i] * 1.5 * (0.30 + 0.70 * blow);
                mix[i] += whistle[i] * 0.9 * blow * blow * (ch == 0 ? 1.0 : 0.5);
                mix[i] += air[i] * 0.25;
            }
        }
    }

    /** Nothing blows down here, so nearly nothing: water moving against stone now and then, and a little air. */
    static void sea(double[] left, double[] right, double loop) {
        int n = left.length;
        Random random = new Random(47);
        double[] when = new double[9];
        double[] where = new double[9];
        for (int k = 0; k < when.length; k++) {
            when[k] = random.nextDouble() * loop;
            where[k] = random.nextDouble();
        }
        for (int ch = 0; ch < 2; ch++) {
            double[] mix = ch == 0 ? left : right;
            double[] lap = lowpass(noise(n, 51 + ch), 520, 0.8);
            double[] trickle = bandpass(noise(n, 53 + ch), 2400, 3);
            double[] air = lowpass(noise(n, 55 + ch), 180, 0.7);
            for (int i = 0; i < n; i++) {
                double t = (double) i / RATE;
                double laps = 0;
                double trickles = 0;
                for (int k = 0; k < when.length; k++) {
                    double side = ch == 0 ? Math.cos(where[k] * Math.PI / 2) : Math.sin(where[k] * Math.PI / 2);
                    double d = wrapped(t - when[k], loop);
                    laps += side * Math.exp(-d * d / (2 * 0.45 * 0.45));
                    double late = wrapped(t - when[k] - 0.5, loop);
                    trickles += side * Math.exp(-late * late / (2 * 0.30 * 0.30));
                }
                mix[i] += lap[i] * 1.6 * laps + trickle[i] * 0.35 * trickles + air[i] * 0.30;
            }
        }
    }

    /** The same sea from under it: weight, a slow dark drone, and every sound of the surface gone. */
    static void deep(double[] left, double[] right, double loop) {
        int n = left.length;
        for (int ch = 0; ch < 2; ch++) {
            double[] mix = ch == 0 ? left : right;
            double[] weight = lowpass(lowpass(noise(n, 61 + ch), 110, 0.8), 160, 0.7);
            double[] murk = bandpass(noise(n, 63 + ch), 320, 2);
            for (int i = 0; i < n; i++) {
                double t = (double) i / RATE;
                mix[i] += weight[i] * 7.0 * (0.7 + 0.3 * turn(t, loop, 2, ch * 1.3));
                mix[i] += murk[i] * 0.25 * (0.5 + 0.5 * turn(t, loop, 3, 1.0 + ch));
                mix[i] += 0.11 * (tone(t, loop, 55.0, 0) + tone(t, loop, 55.0, 6 + ch) + 0.5 * tone(t, loop, 82.4, 3 * ch));
            }
        }
    }

    /** The submersible's works: four soft beats a second of steam through a valve, over a low hum from the hull. */
    static void thrum(double[] left, double[] right, double loop) {
        int n = left.length;
        double[] chuff = lowpass(noise(n, 71), 420, 0.9);
        double[] hiss = highpass(noise(n, 73), 4200, 0.7);
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double beat = (t * 4.0) % 1.0;
            double pulse = Math.exp(-beat * 5.0) * Math.min(1.0, beat / 0.02);
            double s = chuff[i] * 2.2 * (0.25 + 0.75 * pulse) + hiss[i] * 0.05 * pulse
                    + 0.20 * (0.6 + 0.4 * pulse) * (tone(t, loop, 48.0, 0) + 0.5 * tone(t, loop, 96.0, 0));
            left[i] += s;
            right[i] += s;
        }
    }

    // ---- the parts ----

    /** A slow turn between -1 and 1 that comes round exactly {@code cycles} times in the loop. */
    static double turn(double t, double loop, int cycles, double phase) {
        return Math.sin(2 * Math.PI * cycles * t / loop + phase);
    }

    /** A sine near {@code hz}, nudged so it fits the loop exactly; {@code extra} whole cycles more make a beating twin. */
    static double tone(double t, double loop, double hz, int extra) {
        return Math.sin(2 * Math.PI * (Math.round(hz * loop) + extra) * t / loop);
    }

    /** The shortest way round the loop from an event to now, in seconds. */
    static double wrapped(double d, double loop) {
        d = ((d % loop) + loop) % loop;
        return d > loop / 2 ? d - loop : d;
    }

    static double[] noise(int n, long seed) {
        Random random = new Random(seed);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = random.nextDouble() * 2 - 1;
        }
        return x;
    }

    /** One drip: a short sine that falls a little in pitch, written at {@code at} and wrapped round the loop's end. */
    static void drip(double[] left, double[] right, double loop, double at, double hz, double gain, double pan) {
        int n = (int) (loop * RATE);
        int start = (int) (at * RATE);
        for (int j = 0; j < (int) (0.35 * RATE); j++) {
            double t = (double) j / RATE;
            double s = gain * Math.exp(-t / 0.05) * Math.min(1.0, t / 0.002) * Math.sin(2 * Math.PI * hz * (t + 1.5 * t * t) );
            int i = (start + j) % n;
            left[i] += s * Math.cos(pan * Math.PI / 2);
            right[i] += s * Math.sin(pan * Math.PI / 2);
            if (i + n < left.length) { // and in the tail that gets folded back, so the fold leaves it whole
                left[i + n] += s * Math.cos(pan * Math.PI / 2);
                right[i + n] += s * Math.sin(pan * Math.PI / 2);
            }
        }
    }

    /** One cinder cracking: a few milliseconds of bright noise. */
    static void crackle(double[] left, double[] right, double loop, double at, double gain, double pan, long seed) {
        int n = (int) (loop * RATE);
        int start = (int) (at * RATE);
        int length = (int) (0.012 * RATE);
        double[] burst = bandpass(noise(length, seed), 2200, 1.5);
        for (int j = 0; j < length; j++) {
            double s = gain * 4.0 * burst[j] * Math.exp(-j / (0.003 * RATE));
            int i = (start + j) % n;
            left[i] += s * Math.cos(pan * Math.PI / 2);
            right[i] += s * Math.sin(pan * Math.PI / 2);
            if (i + n < left.length) { // and in the tail that gets folded back, so the fold leaves it whole
                left[i + n] += s * Math.cos(pan * Math.PI / 2);
                right[i + n] += s * Math.sin(pan * Math.PI / 2);
            }
        }
    }

    interface Curve {
        double at(double t);
    }

    static double[] lowpass(double[] x, double hz, double q) {
        return filter(x, t -> hz, q, 0);
    }

    static double[] bandpass(double[] x, double hz, double q) {
        return filter(x, t -> hz, q, 1);
    }

    static double[] highpass(double[] x, double hz, double q) {
        return filter(x, t -> hz, q, 2);
    }

    /** A band of noise whose centre moves. */
    static double[] sweep(double[] x, Curve hz, double q) {
        return filter(x, hz, q, 1);
    }

    /**
     * A two-pole state-variable filter, its frequency read from the curve every 32 samples. This form stays steady while
     * its frequency moves; a plain biquad swept like this threw out low thumps.
     */
    static double[] filter(double[] x, Curve hz, double q, int kind) {
        double[] y = new double[x.length];
        double k = 1.0 / q;
        double a1 = 0, a2 = 0, a3 = 0;
        double s1 = 0, s2 = 0;
        for (int i = 0; i < x.length; i++) {
            if (i % 32 == 0) {
                double g = Math.tan(Math.PI * hz.at((double) i / RATE) / RATE);
                a1 = 1.0 / (1.0 + g * (g + k));
                a2 = g * a1;
                a3 = g * a2;
            }
            double v3 = x[i] - s2;
            double v1 = a1 * s1 + a2 * v3;
            double v2 = s2 + a2 * s1 + a3 * v3;
            s1 = 2 * v1 - s1;
            s2 = 2 * v2 - s2;
            y[i] = kind == 0 ? v2 : kind == 1 ? k * v1 : x[i] - k * v1 - v2;
        }
        return y;
    }

    static void write(File dir, String name, double loop, Bed bed) throws Exception {
        int n = (int) (loop * RATE);
        int lap = (int) (Math.min(LAP, loop / 4) * RATE);
        double[] left = new double[n + lap];
        double[] right = new double[n + lap];
        bed.render(left, right, loop);
        // Fold the tail back into the start. Everything that isn't noise is the same in both, so only the noise changes.
        double peak = 0;
        for (int i = 0; i < n; i++) {
            if (i < lap) {
                double a = (double) i / lap;
                left[i] = left[i] * a + left[i + n] * (1 - a);
                right[i] = right[i] * a + right[i + n] * (1 - a);
            }
            peak = Math.max(peak, Math.max(Math.abs(left[i]), Math.abs(right[i])));
        }
        byte[] pcm = new byte[n * 4];
        for (int i = 0; i < n; i++) {
            int l = (int) Math.round(left[i] / peak * 0.7 * 32767);
            int r = (int) Math.round(right[i] / peak * 0.7 * 32767);
            pcm[i * 4] = (byte) l;
            pcm[i * 4 + 1] = (byte) (l >> 8);
            pcm[i * 4 + 2] = (byte) r;
            pcm[i * 4 + 3] = (byte) (r >> 8);
        }
        AudioFormat format = new AudioFormat(RATE, 16, 2, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), format, n), AudioFileFormat.Type.WAVE, new File(dir, name + ".wav"));
        System.out.println(name + " peak " + peak);
    }
}
