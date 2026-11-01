import java.io.ByteArrayInputStream;
import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Renders the Hollow Earth's reveal: the swell that plays as you fall out of the crust and the land opens up below. A slow
 * crescendo of soft strings and wordless voices on an open E major chord (with the ninth), over a deep drone, a bright
 * shimmer coming in on top, then a long fade. Additive synthesis only (no samples), so the recording is entirely ours.
 * Run from the project root:  java tools/music/Swell.java out.wav
 * Then convert with ffmpeg like the other music (ASSETS.md, Music).
 */
public class Swell {
    static final int RATE = 44100;
    static final double LENGTH = 11.0;

    public static void main(String[] args) throws Exception {
        int n = (int) (LENGTH * RATE);
        double[] mix = new double[n];
        // The chord, low to high: drone, then the voices, each entering a little after the one below
        double[][] voices = {
                // midi note, gain, entry (s), brightness
                {28, 0.16, 0.0, 0.3},  // E1 drone
                {40, 0.22, 0.2, 0.5},  // E2
                {47, 0.26, 0.5, 0.6},  // B2
                {52, 0.30, 0.8, 0.7},  // E3
                {56, 0.30, 1.1, 0.8},  // G#3
                {59, 0.30, 1.4, 0.8},  // B3
                {66, 0.30, 1.9, 0.9},  // F#4 (the ninth: the sense of wonder)
                {68, 0.27, 2.3, 0.9},  // G#4
                {71, 0.25, 2.7, 1.0},  // B4
        };
        for (double[] v : voices) {
            voice(mix, v[0], v[1], v[2], v[3]);
        }
        // Shimmer: high bells, slowly trembling, from about two and a half seconds in
        shimmer(mix, 76, 0.07, 2.6);
        shimmer(mix, 83, 0.05, 3.2);
        shimmer(mix, 88, 0.035, 3.8);
        double[] wet = reverb(mix);
        double peak = 0;
        for (int i = 0; i < n; i++) {
            mix[i] = mix[i] * 0.6 + wet[i] * 0.55;
            peak = Math.max(peak, Math.abs(mix[i]));
        }
        byte[] pcm = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            // Fade in over the first 80 ms and out over the last second, so nothing clicks
            double edge = Math.min(1.0, i / (0.08 * RATE)) * Math.min(1.0, (n - i) / (1.0 * RATE));
            int s = (int) Math.round(mix[i] / peak * 0.9 * edge * 32767);
            pcm[i * 2] = (byte) s;
            pcm[i * 2 + 1] = (byte) (s >> 8);
        }
        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), format, n), AudioFileFormat.Type.WAVE, new File(args[0]));
    }

    static double hz(double midi) {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }

    /** The swell's shape: a slow rise to full at 4 s after entry, holding, then a long fall from 6.5 s. */
    static double envelope(double t, double entry) {
        double rise = Math.max(0.0, Math.min(1.0, (t - entry) / (4.0 - entry * 0.4)));
        rise = rise * rise * (3 - 2 * rise);
        double fall = t < 6.5 ? 1.0 : Math.max(0.0, 1.0 - (t - 6.5) / (LENGTH - 6.5));
        // and the whole chord grows from a whisper: a crescendo to full at 5 s
        double grow = Math.min(1.0, t / 5.0);
        grow = 0.12 + 0.88 * grow * grow * (3 - 2 * grow);
        return rise * fall * fall * grow;
    }

    /** One voice: three slightly detuned copies of a soft, string-and-choir tone, with a slow vibrato. */
    static void voice(double[] mix, double midi, double gain, double entry, double brightness) {
        double f = hz(midi);
        double[] detune = {-0.004, 0.0, 0.0037};
        double[] phase = new double[detune.length * 8];
        for (int i = 0; i < mix.length; i++) {
            double t = (double) i / RATE;
            double env = envelope(t, entry);
            if (env <= 0) {
                continue;
            }
            double vibrato = 1.0 + 0.0025 * Math.sin(2 * Math.PI * 4.7 * t + midi);
            double s = 0;
            for (int c = 0; c < detune.length; c++) {
                for (int h = 1; h <= 8; h++) {
                    // Partials falling away like a bowed string, a little more of them as it swells ("ah")
                    double amp = Math.pow(h, -1.3 + 0.5 * brightness * env) * (h == 3 || h == 4 ? 1.25 : 1.0);
                    int k = c * 8 + h - 1;
                    phase[k] += 2 * Math.PI * f * h * (1 + detune[c]) * vibrato / RATE;
                    s += amp * Math.sin(phase[k]);
                }
            }
            mix[i] += s * gain * env / detune.length;
        }
    }

    static void shimmer(double[] mix, double midi, double gain, double entry) {
        double f = hz(midi);
        for (int i = 0; i < mix.length; i++) {
            double t = (double) i / RATE;
            double env = envelope(t, entry);
            double tremble = 0.75 + 0.25 * Math.sin(2 * Math.PI * 5.5 * t);
            mix[i] += gain * env * tremble * (Math.sin(2 * Math.PI * f * t) + 0.3 * Math.sin(2 * Math.PI * f * 2.01 * t));
        }
    }

    /** A big, soft hall: four combs into two allpasses (Schroeder). */
    static double[] reverb(double[] dry) {
        int[] combs = {1557, 1617, 1491, 1422};
        double[] wet = new double[dry.length];
        for (int len : combs) {
            double[] buf = new double[len * 3];
            int at = 0;
            double damp = 0;
            for (int i = 0; i < dry.length; i++) {
                double out = buf[at];
                damp = out * 0.6 + damp * 0.4;
                buf[at] = dry[i] + damp * 0.86;
                at = (at + 1) % buf.length;
                wet[i] += out * 0.25;
            }
        }
        for (int len : new int[] {556, 441}) {
            double[] buf = new double[len];
            int at = 0;
            for (int i = 0; i < wet.length; i++) {
                double delayed = buf[at];
                double in = wet[i];
                buf[at] = in + delayed * 0.5;
                wet[i] = delayed - in * 0.5;
                at = (at + 1) % len;
            }
        }
        return wet;
    }
}
