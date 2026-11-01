import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Renders a MIDI score as a saloon upright: an additive piano synthesizer (no samples, so the recording is entirely ours).
 * Each key has one to three strings with inharmonic partials, tuned a little apart (the honky-tonk beat) and a little off
 * pitch overall, struck by a felt hammer, damped on key release, then put in a small wooden room.
 * Run from the project root:  java tools/music/PianoRoll.java tools/music/scores/the_entertainer.mid out.wav [tempoScale]
 * Then convert with ffmpeg like the other music (ASSETS.md, Music).
 */
public class PianoRoll {
    static final int RATE = 44100;
    static final double MAX_SECONDS = 7.0;

    public static void main(String[] args) throws Exception {
        File in = new File(args[0]);
        File out = new File(args[1]);
        double tempoScale = args.length > 2 ? Double.parseDouble(args[2]) : 1.0;
        List<Note> notes = read(in, tempoScale);
        double end = 0;
        for (Note n : notes) end = Math.max(end, n.off);
        int length = (int) ((end + 3.0) * RATE);
        double[] mix = new double[length];
        Random humanize = new Random(in.getName().hashCode());
        Map<Integer, double[]> cache = new HashMap<>();
        for (Note n : notes) {
            // A human player: a few ms early or late, never quite the same strength twice.
            double on = Math.max(0, n.on + (humanize.nextDouble() - 0.5) * 0.012);
            int vel = Math.max(20, Math.min(127, n.velocity + humanize.nextInt(13) - 6));
            int bucket = vel / 16;
            double[] tone = cache.computeIfAbsent(n.key * 16 + bucket, k -> strike(n.key, (bucket * 16 + 8) / 127.0));
            int start = (int) (on * RATE);
            int held = (int) ((n.off - n.on) * RATE);
            boolean damped = n.key < 89; // the top keys have no dampers
            double releaseTau = n.key < 48 ? 0.14 : 0.07;
            double amp = Math.pow(vel / 127.0, 1.5);
            for (int i = 0; i < tone.length && start + i < length; i++) {
                double env = 1;
                if (damped && i > held) {
                    env = Math.exp(-(i - held) / (releaseTau * RATE));
                    if (env < 1e-4) break;
                }
                mix[start + i] += tone[i] * env * amp;
            }
        }
        double[] room = room(mix);
        double peak = 0;
        for (double v : room) peak = Math.max(peak, Math.abs(v));
        byte[] pcm = new byte[room.length * 2];
        for (int i = 0; i < room.length; i++) {
            int s = (int) Math.round(room[i] / peak * 0.89 * 32767);
            pcm[2 * i] = (byte) s;
            pcm[2 * i + 1] = (byte) (s >> 8);
        }
        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), format, room.length), AudioFileFormat.Type.WAVE, out);
        System.out.printf("PianoRoll: %d notes, %.1f s -> %s%n", notes.size(), end, out);
    }

    record Note(int key, int velocity, double on, double off) {}

    /** Notes in seconds, using the score's tempo map. Both hands may be on separate tracks. */
    static List<Note> read(File file, double tempoScale) throws Exception {
        Sequence seq = MidiSystem.getSequence(file);
        TreeMap<Long, Double> tempo = new TreeMap<>(); // tick -> microseconds per quarter
        tempo.put(0L, 500000.0);
        for (Track t : seq.getTracks()) {
            for (int i = 0; i < t.size(); i++) {
                MidiEvent e = t.get(i);
                if (e.getMessage() instanceof MetaMessage m && m.getType() == 0x51) {
                    byte[] d = m.getData();
                    tempo.put(e.getTick(), (double) (((d[0] & 0xFF) << 16) | ((d[1] & 0xFF) << 8) | (d[2] & 0xFF)));
                }
            }
        }
        double ppq = seq.getResolution();
        List<Note> notes = new ArrayList<>();
        for (Track t : seq.getTracks()) {
            Map<Integer, long[]> down = new HashMap<>(); // key -> {tick, velocity}
            for (int i = 0; i < t.size(); i++) {
                MidiEvent e = t.get(i);
                if (!(e.getMessage() instanceof ShortMessage m)) continue;
                int cmd = m.getCommand();
                int key = m.getData1();
                boolean on = cmd == ShortMessage.NOTE_ON && m.getData2() > 0;
                boolean off = cmd == ShortMessage.NOTE_OFF || (cmd == ShortMessage.NOTE_ON && m.getData2() == 0);
                if (on || off) {
                    long[] prev = down.remove(key);
                    if (prev != null) notes.add(new Note(key, (int) prev[1], seconds(prev[0], tempo, ppq) * tempoScale, seconds(e.getTick(), tempo, ppq) * tempoScale));
                    if (on) down.put(key, new long[] {e.getTick(), m.getData2()});
                }
            }
        }
        return notes;
    }

    static double seconds(long tick, TreeMap<Long, Double> tempo, double ppq) {
        double s = 0;
        long last = 0;
        double us = tempo.firstEntry().getValue();
        for (Map.Entry<Long, Double> e : tempo.entrySet()) {
            if (e.getKey() >= tick) break;
            s += (e.getKey() - last) * us / ppq / 1e6;
            last = e.getKey();
            us = e.getValue();
        }
        return s + (tick - last) * us / ppq / 1e6;
    }

    /** One hammer strike on one key at a velocity (0-1), left to ring out. */
    static double[] strike(int key, double velocity) {
        Random rnd = new Random(key * 7919L);
        // An old upright: every key a few cents off, and its strings a few more cents apart from each other.
        double keyCents = (rnd.nextDouble() - 0.5) * 9 + (key - 60) * 0.35; // + a little stretch tuning
        double f0 = 440 * Math.pow(2, (key - 69) / 12.0) * Math.pow(2, keyCents / 1200);
        int strings = key < 34 ? 1 : key < 46 ? 2 : 3;
        double spread = 5 + rnd.nextDouble() * 6; // cents
        double inharm = Math.min(0.02, Math.max(0.00008, 0.0006 * Math.pow(2, (key - 60) / 14.0)));
        double tau = 2.6 * Math.pow(2, -(key - 21) / 17.0) + 0.25; // seconds to fall by 1/e: bass rings, treble is short
        int n = (int) (Math.min(MAX_SECONDS, tau * 7 + 0.3) * RATE);
        double[] out = new double[n];
        double bright = 1.9 - 0.8 * velocity; // a harder blow brings out the upper partials
        double hammerAt = 1 / 7.3; // struck about a seventh of the way along, which tames the seventh partial
        double total = 0;
        for (int s = 0; s < strings; s++) {
            double detune = strings == 1 ? 0 : (s - (strings - 1) / 2.0) * spread / ((strings - 1) / 2.0) * 0.5;
            double fs = f0 * Math.pow(2, detune / 1200);
            for (int k = 1; k <= 28; k++) {
                double fk = k * fs * Math.sqrt(1 + inharm * k * k);
                if (fk > 9000) break;
                double a = Math.abs(Math.sin(Math.PI * k * hammerAt)) / Math.pow(k, bright);
                double tk = tau / (1 + 0.35 * (k - 1) + fk / 3000);
                double phase = rnd.nextDouble() * Math.PI * 2;
                // Rotate a phasor instead of calling sin per sample.
                double c = Math.cos(2 * Math.PI * fk / RATE), sn = Math.sin(2 * Math.PI * fk / RATE);
                double x = Math.cos(phase), y = Math.sin(phase);
                double fast = Math.exp(-1 / (tk * 0.22 * RATE)), slow = Math.exp(-1 / (tk * RATE));
                double ef = 0.65, es = 0.35;
                for (int i = 0; i < n; i++) {
                    out[i] += a * y * (ef + es);
                    double nx = x * c - y * sn;
                    y = x * sn + y * c;
                    x = nx;
                    ef *= fast;
                    es *= slow;
                    if (ef + es < 1e-5) break;
                }
                total += a;
            }
        }
        // The felt thump: a short burst of filtered noise, brighter when struck harder.
        Random noise = new Random(key);
        double lp = 0, cut = 1 - Math.exp(-2 * Math.PI * (800 + 2500 * velocity) / RATE);
        for (int i = 0; i < Math.min(n, RATE / 20); i++) {
            lp += cut * ((noise.nextDouble() * 2 - 1) - lp);
            out[i] += lp * 0.35 * Math.exp(-i / (0.006 * RATE)) * total / 6;
        }
        for (int i = 0; i < n; i++) {
            double attack = Math.min(1, i / (0.0015 * RATE));
            out[i] = out[i] * attack / total;
        }
        return out;
    }

    /** A small wooden room: a soft treble roll-off, then a little Schroeder reverb mixed under the dry piano. */
    static double[] room(double[] dry) {
        double[] tone = new double[dry.length];
        double lp = 0, cut = 1 - Math.exp(-2 * Math.PI * 6500 / RATE);
        for (int i = 0; i < dry.length; i++) {
            lp += cut * (dry[i] - lp);
            tone[i] = lp;
        }
        int[] combs = {1116, 1188, 1277, 1356};
        double[] wet = new double[dry.length];
        for (int d : combs) {
            int delay = d * 3 / 4; // a small room
            double[] buf = new double[delay];
            double damp = 0;
            for (int i = 0, p = 0; i < dry.length; i++, p = (p + 1) % delay) {
                double y = buf[p];
                damp = y * 0.6 + damp * 0.4;
                buf[p] = tone[i] + damp * 0.72;
                wet[i] += y;
            }
        }
        for (int d : new int[] {556, 441}) {
            double[] buf = new double[d];
            for (int i = 0, p = 0; i < dry.length; i++, p = (p + 1) % d) {
                double b = buf[p];
                double y = -wet[i] + b;
                buf[p] = wet[i] + b * 0.5;
                wet[i] = y;
            }
        }
        double[] out = new double[dry.length];
        for (int i = 0; i < dry.length; i++) out[i] = tone[i] + wet[i] * 0.045;
        return out;
    }
}
