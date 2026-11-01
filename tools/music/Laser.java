import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Renders the phlogiston guns' shots (13b): the classic ray-gun "pew", synthesized, no samples. Each shot is a tone whose pitch
 * falls away fast (an exponential sweep), voiced as two slightly detuned oscillators (a sine with a little of its odd
 * harmonics, so it buzzes like a square without the harshness) for a chorused, electric edge, with a fast vibrato on the
 * pitch for shimmer, a sharp attack, a quick exponential decay and one soft slap of echo. The rifle's is lower, longer and
 * heavier, with a sub-bass thump under it and a crackle at the start. Three takes of each, differing a little in pitch and
 * length, so repeated shots sound alike but not identical.
 * Also the Prototype Saucer's hum (13c, {@link #hum}).
 * Run from the project root:  java tools/music/Laser.java <out dir>   (writes pistol_1..3.wav, rifle_1..3.wav, saucer_hum.wav)
 * Then convert each to mono Ogg Vorbis with ffmpeg (ASSETS.md, Sounds).
 */
public class Laser {
    static final int RATE = 44100;

    record Shot(double from, double to, double length, double detune, double vibrato, double sub, double crackle) {}

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "build/laser");
        dir.mkdirs();
        Random random = new Random(1897);
        for (int take = 1; take <= 3; take++) {
            double v = (take - 2) * 0.06; // takes a few percent apart
            write(new File(dir, "pistol_" + take + ".wav"), render(new Shot(2600 * (1 + v), 380 * (1 + v), 0.20 + take * 0.01, 1.012, 38, 0.0, 0.15), random));
            write(new File(dir, "rifle_" + take + ".wav"), render(new Shot(1500 * (1 + v), 110 * (1 + v), 0.46 + take * 0.02, 1.018, 22, 0.55, 0.45), random));
        }
        // 13c: the Prototype Saucer's hum, two seconds that play back to back while it flies
        write(new File(dir, "saucer_hum.wav"), hum(2.0));
        // 13d: the Mothership's hum, its death ray charging (as long as the charge, two seconds) and falling
        write(new File(dir, "mothership_hum.wav"), throb(2.0));
        write(new File(dir, "death_ray_charge.wav"), charge(2.0, random));
        write(new File(dir, "death_ray_fire.wav"), render(new Shot(900, 45, 1.1, 1.03, 12, 1.0, 0.9), random));
        System.out.println("Laser: wrote 6 shots, the saucer hum, the Mothership's hum and its death ray to " + dir);
    }

    /**
     * The Mothership's hum (13d): a deep, slow throb under everything. A 41 Hz fundamental and two detuned voices on its
     * fourth (55 and 55.8 Hz, beating a little under once a second) and octave, soft squares, swelling and ebbing at
     * 1.5 Hz, with a faint high whine on top. Short fades at the ends so plays laid end to end don't click.
     */
    static double[] throb(double length) {
        int n = (int) (length * RATE);
        double[] out = new double[n];
        double[] phase = new double[5];
        double[] freq = {41, 55, 55.8, 82, 1240};
        double[] gain = {0.55, 0.3, 0.3, 0.2, 0.04};
        double peak = 1e-9;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double sample = 0;
            for (int k = 0; k < freq.length; k++) {
                phase[k] += 2 * Math.PI * freq[k] / RATE;
                sample += gain[k] * (k == 4 ? Math.sin(phase[k]) * (1 + 0.5 * Math.sin(2 * Math.PI * 7 * t)) : voice(phase[k]));
            }
            double swell = 0.7 + 0.3 * Math.sin(2 * Math.PI * 1.5 * t);
            double edge = Math.min(1, Math.min(t, length - t) / 0.1);
            out[i] = sample * swell * edge;
            peak = Math.max(peak, Math.abs(out[i]));
        }
        double scale = Math.pow(10, -3 / 20.0) / peak;
        for (int i = 0; i < n; i++) {
            out[i] *= scale;
        }
        return out;
    }

    /**
     * The death ray charging (13d): a whine climbing from 180 Hz to 1600 (an exponential sweep), soft-square voiced, its
     * tremolo quickening from 4 to 18 Hz and a hiss of noise rising under it, louder all the way, cut off clean at the end
     * (where the ray falls).
     */
    static double[] charge(double length, Random random) {
        int n = (int) (length * RATE);
        double[] out = new double[n];
        double phase = 0, tremPhase = 0, y = 0;
        double rate = Math.log(1600 / 180.0) / length;
        double peak = 1e-9;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double f = 180 * Math.exp(rate * t);
            phase += 2 * Math.PI * f / RATE;
            tremPhase += 2 * Math.PI * (4 + 14 * t / length) / RATE;
            double tremolo = 0.6 + 0.4 * Math.sin(tremPhase);
            y += 0.3 * ((random.nextDouble() * 2 - 1) - y);
            double rise = Math.min(1, t / 0.05) * (0.25 + 0.75 * t / length);
            double end = Math.min(1, (length - t) / 0.01);
            out[i] = (0.6 * voice(phase) * tremolo + 0.35 * y * t / length) * rise * end;
            peak = Math.max(peak, Math.abs(out[i]));
        }
        double scale = Math.pow(10, -1 / 20.0) / peak;
        for (int i = 0; i < n; i++) {
            out[i] *= scale;
        }
        return out;
    }

    static double[] render(Shot s, Random random) {
        double echo = 0.11; // the slap's delay (s)
        int n = (int) ((s.length + echo * 3 + 0.05) * RATE);
        double[] dry = new double[n];
        double phaseA = 0, phaseB = 0, subPhase = 0;
        int shotSamples = (int) (s.length * RATE);
        double rate = Math.log(s.to / s.from) / s.length; // exponential sweep
        for (int i = 0; i < shotSamples; i++) {
            double t = i / (double) RATE;
            double f = s.from * Math.exp(rate * t) * (1 + 0.025 * Math.sin(2 * Math.PI * s.vibrato * t));
            phaseA += 2 * Math.PI * f / RATE;
            phaseB += 2 * Math.PI * f * s.detune / RATE;
            double tone = voice(phaseA) + voice(phaseB);
            // Sharp attack (2 ms), then an exponential decay that's mostly gone by the end
            double env = Math.min(1, t / 0.002) * Math.exp(-4.2 * t / s.length);
            double sample = 0.5 * tone * env;
            if (s.sub > 0) {
                // A thump an octave or two down that falls faster still: the rifle's weight
                double fs = 90 * Math.exp(-6 * t);
                subPhase += 2 * Math.PI * (fs + 40) / RATE;
                sample += s.sub * Math.sin(subPhase) * Math.exp(-9 * t);
            }
            if (t < 0.03) {
                // A crackle at the start: noise, bright and gone in thirty milliseconds
                sample += s.crackle * (random.nextDouble() * 2 - 1) * (1 - t / 0.03);
            }
            dry[i] = sample;
        }
        // One soft slap of echo, late enough not to blur the sweep
        double[] out = dry.clone();
        for (int tap = 1; tap <= 1; tap++) {
            int delay = (int) (echo * tap * RATE);
            double gain = 0.2;
            for (int i = 0; i + delay < n; i++) {
                out[i + delay] += dry[i] * gain;
            }
        }
        // A gentle one-pole low-pass to round off the top, then peak-normalise to -1 dBFS
        double y = 0, a = 0.55;
        double peak = 1e-9;
        for (int i = 0; i < n; i++) {
            y += a * (out[i] - y);
            out[i] = y;
            peak = Math.max(peak, Math.abs(y));
        }
        double scale = Math.pow(10, -1 / 20.0) / peak;
        for (int i = 0; i < n; i++) {
            out[i] *= scale;
        }
        return out;
    }


    /**
     * The saucer's hum (13c): a theremin-ish warble, the classic flying-saucer sound. A low fundamental (110 Hz) and its
     * fifth and octave, each a soft square, with a slow, wide vibrato (5.5 Hz, about a quarter-tone) and a gentle tremolo, a
     * high sine floating an octave and a half up for the eerie part, and short fades at each end so plays laid end to end
     * don't click. Peak-normalised to -3 dBFS (it's a bed under everything else).
     */
    static double[] hum(double length) {
        int n = (int) (length * RATE);
        double[] out = new double[n];
        double[] phase = new double[4];
        double[] freq = {110, 165, 220, 622};
        double[] gain = {0.5, 0.22, 0.25, 0.12};
        double peak = 1e-9;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double wobble = 1 + 0.015 * Math.sin(2 * Math.PI * 5.5 * t);
            double sample = 0;
            for (int k = 0; k < freq.length; k++) {
                phase[k] += 2 * Math.PI * freq[k] * wobble / RATE;
                sample += gain[k] * (k == 3 ? Math.sin(phase[k]) : voice(phase[k]));
            }
            double tremolo = 0.85 + 0.15 * Math.sin(2 * Math.PI * 3.0 * t);
            double edge = Math.min(1, Math.min(t, length - t) / 0.08);
            out[i] = sample * tremolo * edge;
            peak = Math.max(peak, Math.abs(out[i]));
        }
        double scale = Math.pow(10, -3 / 20.0) / peak;
        for (int i = 0; i < n; i++) {
            out[i] *= scale;
        }
        return out;
    }
    /** A sine with a little of its third and fifth harmonics: a soft square, buzzy but not harsh. */
    static double voice(double phase) {
        return Math.sin(phase) + 0.28 * Math.sin(3 * phase) + 0.12 * Math.sin(5 * phase);
    }

    static void write(File file, double[] samples) throws Exception {
        byte[] pcm = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int v = (int) Math.round(Math.max(-1, Math.min(1, samples[i])) * 32767);
            pcm[2 * i] = (byte) v;
            pcm[2 * i + 1] = (byte) (v >> 8);
        }
        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        try (AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(pcm), format, samples.length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, file);
        }
    }
}
