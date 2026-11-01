import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntToDoubleFunction;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Renders the trailer's score and its synthesized sound design in one stereo track, cut to trailer/edit.csv: no samples,
 * so the recording is ours. cut.ps1 lays a western recording over the frontier part (Kevin MacLeod's "Still Pickin'",
 * CC BY, stretched to the cut's tempo) and the game's own sounds over everything (trailer/sfx.csv). 120 bpm throughout (a
 * bar is two seconds, so the shots cut on the bar lines).
 *
 * <ul>
 * <li><b>The frontier</b> (0 to {@code pass}): sound design only under the western recording: wind, the revolver and
 * the shootout, the dynamite, the boiler's anvil and hiss, the airship's steam whistle and propeller, the raid's gunfire,
 * and crickets over the quiet night until the saucer comes over.</li>
 * <li><b>The abduction</b> ({@code pass} to {@code drop}): everything western stops dead as the saucer passes over (a
 * doppler whoosh); a drone, the beam coming on, a theremin bending the theme, a riser to the moment the cow is taken,
 * and a beat of silence.</li>
 * <li><b>The Grays and the Hollow</b> ({@code drop} to {@code title}): a braam and a hit, the theme on a saw lead over a
 * pulsing synth bass and four-on-the-floor drums; the gateway's charge; the fall, all wind and a falling rush; a choir
 * over F and G for the Hollow vista; then everything at once in half time over its ruins, whistle and lead together,
 * and a snare build into the title.</li>
 * <li><b>The title</b>: the last hit, the band's last strum and the whistle's last note together, ringing out.</li>
 * </ul>
 *
 * Run from the project root:  java tools/music/TrailerScore.java trailer/music/score.wav [cue=seconds ...]
 * The cues (seconds into the cut) default to the current edit; trailer/cut.ps1 passes them in.
 */
public class TrailerScore {
    static final int RATE = 48000;
    static final double BEAT = 0.5;
    static final double BAR = 2.0;
    static final Map<String, Double> CUE = new LinkedHashMap<>();

    static {
        CUE.put("saloon", 6.0);    // the saloon doors
        CUE.put("ride", 12.0);     // the gallop: the band back in
        CUE.put("load", 16.0);     // revolver loading
        CUE.put("shootout", 17.5); // its shots come 0.2, 0.9, 1.6 and 2.4 s later
        CUE.put("dynamite", 20.0); // fuse lit; the bang a little over a second later
        CUE.put("bang", 21.7);
        CUE.put("steam", 22.0);    // the boiler
        CUE.put("airship", 24.0);
        CUE.put("raid", 28.0);
        CUE.put("stop", 32.0);     // the raid over; the quiet night and the cows until the saucer
        CUE.put("abduct", 34.0);   // the abduction shot starts
        CUE.put("pass", 35.75);    // the saucer passes over the camera: the western music stops dead
        CUE.put("beam", 36.1);     // the beam comes on
        CUE.put("take", 39.25);    // the cow is taken
        CUE.put("drop", 42.0);     // the Grays' part
        CUE.put("gate", 45.6);     // the polar gateway wakes
        CUE.put("fall", 48.0);     // down Symmes' Hole
        CUE.put("vista", 52.0);    // the Hollow opening up
        CUE.put("sites", 56.0);    // its ruins and temples
        CUE.put("title", 62.0);
        CUE.put("end", 68.0);
    }

    static double cue(String name) {
        return CUE.get(name);
    }

    static double[] L, R, SL, SR;
    static final Random RANDOM = new Random(1849);

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        for (int i = 1; i < args.length; i++) {
            String[] kv = args[i].split("=");
            CUE.put(kv[0], Double.parseDouble(kv[1]));
        }
        int length = n(cue("end") + 1.0);
        L = new double[length];
        R = new double[length];
        SL = new double[length];
        SR = new double[length];

        frontier();
        abduction();
        grays();
        title();
        stinger();

        double[][] wet = reverb(SL, SR);
        double peak = 1e-9;
        for (int i = 0; i < length; i++) {
            L[i] += wet[0][i];
            R[i] += wet[1][i];
        }
        // Master: into a soft saturating limiter at a level set by the loud parts (the 99.9th percentile), so only the
        // shots and hits are rounded off; then normalise to just under full scale, fading the last half second
        double[] level = new double[length / 16];
        for (int k = 0; k < level.length; k++) {
            level[k] = Math.max(Math.abs(L[k * 16]), Math.abs(R[k * 16]));
        }
        java.util.Arrays.sort(level);
        double drive = 0.9 / Math.max(1e-9, level[(int) (level.length * 0.999)]);
        for (int i = 0; i < length; i++) {
            L[i] = Math.tanh(L[i] * drive);
            R[i] = Math.tanh(R[i] * drive);
            peak = Math.max(peak, Math.max(Math.abs(L[i]), Math.abs(R[i])));
        }
        byte[] pcm = new byte[length * 4];
        int fade = n(0.5);
        for (int i = 0; i < length; i++) {
            double edge = Math.min(1.0, (length - i) / (double) fade);
            int l = (int) Math.round(L[i] / peak * 0.93 * edge * 32767);
            int r = (int) Math.round(R[i] / peak * 0.93 * edge * 32767);
            pcm[i * 4] = (byte) l;
            pcm[i * 4 + 1] = (byte) (l >> 8);
            pcm[i * 4 + 2] = (byte) r;
            pcm[i * 4 + 3] = (byte) (r >> 8);
        }
        out.getAbsoluteFile().getParentFile().mkdirs();
        AudioFormat format = new AudioFormat(RATE, 16, 2, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), format, length), AudioFileFormat.Type.WAVE, out);
        System.out.printf("TrailerScore: %.1f s -> %s%n", length / (double) RATE, out);
    }

    // ================================================================ the score

    /** The theme, as {beat in the phrase, length in beats, midi note}: A minor, phrase A ends open, phrase B home. */
    static final double[][] PHRASE_A = {
            {0, 2, 69}, {2, 1, 76}, {3, 0.5, 74}, {3.5, 0.5, 72},
            {4, 1.5, 71}, {5.5, 0.5, 72}, {6, 2, 69},
            {8, 1, 67}, {9, 0.5, 69}, {9.5, 0.5, 72}, {10, 2, 76},
            {12, 1.5, 74}, {13.5, 0.5, 72}, {14, 2, 71}};
    static final double[][] PHRASE_B = {
            {0, 2, 69}, {2, 1, 76}, {3, 0.5, 79}, {3.5, 0.5, 77},
            {4, 1.5, 76}, {5.5, 0.5, 74}, {6, 2, 72},
            {8, 1, 74}, {9, 0.5, 72}, {9.5, 0.5, 71}, {10, 1, 68}, {11, 1, 71},
            {12, 4, 69}};

    /** A chord: the bass's root and fifth, the guitar's voicing, the banjo's five strings. */
    record Chord(int root, int fifth, int[] guitar, int[] banjo) {}

    static Chord chord(String name) {
        return switch (name) {
            case "Am" -> new Chord(45, 52, new int[] {57, 60, 64, 69}, new int[] {64, 69, 72, 76, 81});
            case "E" -> new Chord(40, 47, new int[] {52, 56, 59, 64}, new int[] {64, 68, 71, 76, 80});
            case "E7" -> new Chord(40, 47, new int[] {52, 56, 59, 62}, new int[] {62, 64, 68, 71, 74});
            case "C" -> new Chord(48, 43, new int[] {55, 60, 64, 67}, new int[] {64, 67, 72, 76, 79});
            case "G" -> new Chord(43, 50, new int[] {55, 59, 62, 67}, new int[] {62, 67, 71, 74, 79});
            case "Dm" -> new Chord(50, 45, new int[] {57, 62, 65, 69}, new int[] {62, 65, 69, 74, 77});
            case "F" -> new Chord(41, 48, new int[] {53, 57, 60, 65}, new int[] {65, 69, 72, 77, 81});
            default -> throw new IllegalArgumentException(name);
        };
    }

    /**
     * The frontier's sound design only: its music is a recording that trailer/cut.ps1 lays in from the top to {@code pass}
     * (a real western band, stopping dead as the saucer comes over).
     */
    static void frontier() {
        double saloon = cue("saloon");
        double stop = cue("stop");
        double pass = cue("pass");
        // The wind, under the open and the quiet night
        wind(0, saloon + 1, 0.10);
        wind(stop, pass, 0.08);

        // Revolver loading and the shootout
        for (int k = 0; k < 3; k++) {
            click(cue("load") + 0.2 + k * 0.25, 0.25);
        }
        cock(cue("load") + 1.2, 0.35);
        double s = cue("shootout");
        for (double t : new double[] {s + 0.2, s + 0.9, s + 1.6, s + 2.4}) { // the shots in the clip
            gunshot(t, 0.75, RANDOM.nextDouble() - 0.5, false);
        }
        ricochet(s + 1.0, 0.25, -0.5);
        // Dynamite: the fuse, then the bang
        fuse(cue("dynamite") + 0.1, cue("bang"), 0.12);
        explosion(cue("bang"), 1.0);
        // Steam: the anvil on the boiler and a hiss; the steam whistle as the airship climbs, its engine thrumming
        put(anvil(), cue("steam"), 0.35, 0.3, 0.2);
        put(anvil(), cue("steam") + 0.75, 0.25, 0.3, 0.2);
        hiss(cue("steam") + 0.4, 0.9, 0.12);
        steamWhistle(cue("airship") + 0.3, 1.4, 0.28);
        propeller(cue("airship"), cue("raid"), 0.18);
        // The raid: scattered shots
        for (double t : new double[] {0.6, 1.3, 1.9, 2.4, 3.0, 3.3}) {
            gunshot(cue("raid") + t, 0.45, RANDOM.nextDouble() - 0.5, false);
        }
    }

    static void abduction() {
        double stop = cue("stop");
        double pass = cue("pass");
        double beam = cue("beam");
        double take = cue("take");
        double drop = cue("drop");
        // Crickets under the quiet night, cut off dead as the saucer comes over
        crickets(stop + 0.6, pass - 0.25, 0.10);
        // From the saucer on, the Grays' sound: a low drone, growing to the take
        drone(pass - 0.2, take, 0.12);
        // The saucer over the camera, the beam, its hum
        saucerPass(pass, 0.6);
        beamOn(beam, take, 0.45);
        // The theremin bends the theme: up from below to A, to E, slipping to D#, and away up at the take
        List<double[]> theremin = new ArrayList<>();
        theremin.add(new double[] {beam + 0.1, 1.2, 69, 57});
        theremin.add(new double[] {beam + 1.3, 1.0, 76});
        theremin.add(new double[] {beam + 2.3, take - beam - 2.5, 75});
        theremin.add(new double[] {take - 0.2, 0.35, 93});
        line(theremin, Voice.THEREMIN, 0.24, -0.2, 0.5);
        // The riser into the take, the take itself, and the craft gone up and away
        riser(beam + 0.4, take, 0.25);
        zap(take, 0.5);
        whoosh(take + 0.1, 0.8, 0.3, true);
        // A beat of nothing; a reversed cymbal pulling into the drop
        reverseCymbal(drop - 1.3, drop, 0.30);
    }

    static void grays() {
        double drop = cue("drop");
        double gate = cue("gate");
        double fall = cue("fall");
        double vista = cue("vista");
        double sites = cue("sites");
        double title = cue("title");
        braam(drop, new int[] {33, 45, 52, 57}, 3.0, 0.55);
        impact(drop, 1.0);
        // The drive, from the drop to the fall: the theme on a saw lead (the whistle's octave) over Am | F | C ...
        synth(drop, new String[] {"Am", "F", "C", "G", "Am", "F"}, (int) Math.round((fall - drop) / BAR), false);
        line(notes(PHRASE_A, drop, 12, 0, fall), Voice.LEAD, 0.20, 0.1, 0.3);
        line(notes(PHRASE_A, drop, 0, 0, fall), Voice.LEAD, 0.08, -0.1, 0.3);
        // The gateway waking: a charge swelling into its beam
        riser(gate - 1.0, gate + 0.2, 0.20);
        impact(gate + 0.2, 0.45);
        // The fall: the drums gone; the wind tearing past, a long falling rush, and the deep rising to meet the vista
        wind(fall, vista + 0.6, 0.22);
        whoosh(fall, vista - fall, 0.35, false);
        put(pad(new int[] {43, 50, 55, 59, 62}, vista - fall, 900), fall, 0.10, 0.0, 0.5);
        riser(fall + 1.0, vista, 0.24);
        // The vista: the choir opening on F, then G (the ninths in them), the whistle answering with phrase B's start
        put(timpani(41), vista, 0.5, 0.0, 0.4);
        choir(new int[] {41, 53, 57, 60, 65, 67, 72}, vista, BAR + 0.3, 0.16);
        choir(new int[] {43, 55, 59, 62, 67, 69, 74}, vista + BAR, BAR, 0.18);
        double[][] answer = {{0, 2, 69}, {2, 1, 76}, {3, 0.5, 79}, {3.5, 0.5, 77}, {4, 1.5, 76}, {5.5, 0.5, 74}, {6, 1.6, 74}};
        line(notes(answer, vista, 12, 0, sites), Voice.WHISTLE, 0.28, 0.1, 0.45);
        line(notes(answer, vista, 12, 0, sites), Voice.LEAD, 0.10, -0.1, 0.45);
        // The sites: everything at once, half time: phrase B on the whistle and the lead together over Am | C | Dm E,
        // the choir under it, into the title
        put(crash(), sites, 0.4, 0.0, 0.4);
        impact(sites, 0.6);
        // The progression and phrase B (four bars each) come round again for as long as the Hollow's shots run
        String[] round = {"Am", "C", "Dm|E", "Am"};
        int bars = (int) Math.round((title - sites) / BAR);
        String[] epic = new String[bars];
        for (int b = 0; b < bars; b++) {
            epic[b] = round[b % round.length];
        }
        synth(sites, epic, bars, true);
        for (int b = 0; b < bars; b++) {
            Chord ch = chord(epic[b].split("\\|")[0]);
            choir(new int[] {ch.root() - 12, ch.root(), ch.guitar()[0], ch.guitar()[1], ch.guitar()[2], ch.guitar()[3]}, sites + b * BAR, BAR, 0.10);
        }
        for (double at = sites; at < title - BEAT; at += 4 * BAR) {
            line(notes(PHRASE_B, at, 12, 0, title - BEAT), Voice.WHISTLE, 0.26, 0.15, 0.4);
            line(notes(PHRASE_B, at, 12, 0, title - BEAT), Voice.LEAD, 0.16, -0.15, 0.4);
            line(notes(PHRASE_B, at, 0, 0, title - BEAT), Voice.LEAD, 0.08, 0.0, 0.4);
        }
        // The Archon waking: a hit and a timpani under the band; the saucer tearing past the lens
        if (CUE.containsKey("wake")) {
            impact(cue("wake"), 0.7);
            put(timpani(33), cue("wake"), 0.5, 0.0, 0.4);
        }
        if (CUE.containsKey("zoom")) {
            whoosh(cue("zoom"), 1.4, 0.3, false);
        }
        // The build into the title: a snare roll and a riser, then a sixteenth of silence
        for (double t = title - BAR; t < title - BEAT / 4; ) {
            double k = (t - (title - BAR)) / BAR;
            put(snare(0.4), t, 0.10 + 0.32 * k, 0.0, 0.15);
            t += k < 0.5 ? BEAT / 4 : BEAT / 8;
        }
        riser(title - 2 * BAR, title - BEAT / 4, 0.22);
    }

    /**
     * The synth band from {@code start}, {@code count} bars of the chords (the last held if they run out): a saw bass
     * pulsing sixteenths, an arpeggio panned wide, a pad of detuned saws, and drums: four on the floor with a big snare on
     * two and four, or for {@code epic} half time (kick and timpani on one, a clap and a tom on three) under eighth hats.
     */
    static void synth(double start, String[] chords, int count, boolean epic) {
        for (int b = 0; b < count; b++) {
            double t = start + b * BAR;
            String[] halves = chords[Math.min(b, chords.length - 1)].split("\\|");
            for (int beat = 0; beat < 4; beat++) {
                Chord ch = chord(halves.length > 1 && beat >= 2 ? halves[1] : halves[0]);
                double at = t + beat * BEAT;
                for (int k = 0; k < 4; k++) {
                    int note = ch.root() - 12 + (k == 2 ? 12 : 0);
                    put(synthBass(mtof(note), BEAT / 4 * 0.9), at + k * BEAT / 4, epic ? 0.22 : 0.28, 0.0, 0.03);
                    int arp = ch.guitar()[new int[] {0, 1, 2, 3, 2, 1, 2, 3}[(beat * 4 + k) % 8]] + 12;
                    put(pulse(mtof(arp), 0.11), at + k * BEAT / 4, 0.07, ((beat * 4 + k) % 2 == 0 ? -0.6 : 0.6), 0.25);
                }
                if (beat == 0 || halves.length > 1 && beat == 2) {
                    put(pad(ch.guitar(), halves.length > 1 ? BAR / 2 : BAR, epic ? 1800 : 1200), at, 0.10, 0.0, 0.35);
                }
                if (!epic) {
                    put(synthKick(), at, 0.65, 0.0, 0.02);
                    if (beat % 2 == 1) {
                        put(clap(), at, 0.38, 0.0, 0.3);
                    }
                    put(hat(0.05), at + BEAT / 2, 0.10, 0.3, 0.05);
                    put(hat(0.02), at + BEAT / 4, 0.05, 0.3, 0.02);
                    put(hat(0.02), at + 3 * BEAT / 4, 0.05, 0.3, 0.02);
                } else {
                    if (beat == 0) {
                        put(synthKick(), at, 0.75, 0.0, 0.05);
                        put(timpani(ch.root() - 12), at, 0.45, 0.0, 0.3);
                    }
                    if (beat == 2) {
                        put(clap(), at, 0.45, 0.0, 0.45);
                        put(tom(70), at, 0.4, 0.0, 0.3);
                    }
                    put(hat(0.04), at, 0.07, 0.3, 0.05);
                    put(hat(0.04), at + BEAT / 2, 0.09, -0.3, 0.05);
                }
            }
        }
    }

    static void title() {
        double title = cue("title");
        double end = CUE.containsKey("stinger") ? cue("stinger") : cue("end");
        double ring = end - title + 0.5;
        braam(title, new int[] {33, 45, 52, 57, 60}, ring, 0.6);
        impact(title, 1.2);
        put(gong(), title, 0.4, 0.0, 0.5);
        put(crash(), title, 0.35, 0.0, 0.4);
        // The frontier answers: the band's last strum, the bass, the whistle's last A
        strum(chord("Am").guitar(), title + 0.02, 0.40, ring, -0.25, true);
        for (int k = 0; k < 5; k++) {
            put(banjo(mtof(chord("Am").banjo()[k])), title + k * 0.015, 0.22, 0.35, 0.3);
        }
        put(pluck(mtof(33), ring, 0.35, ring, 0.0), title, 0.6, 0.0, 0.1);
        List<double[]> last = new ArrayList<>();
        last.add(new double[] {title + 0.4, ring - 1.0, 81, 76});
        line(last, Voice.WHISTLE, 0.24, 0.0, 0.6);
        choir(new int[] {45, 57, 60, 64, 69, 71, 76}, title, ring, 0.12);
    }

    /** After the title, Mr. Gray at the bar: the saloon gone quiet but for the crickets, and one plucked banjo note to end on. */
    static void stinger() {
        if (!CUE.containsKey("stinger")) {
            return;
        }
        double at = cue("stinger");
        double end = cue("end");
        crickets(at, end, 0.06);
        put(banjo(mtof(57)), end - 0.75, 0.32, 0.0, 0.3);
    }

    // ================================================================ melody lines

    enum Voice { WHISTLE, THEREMIN, LEAD }

    /** Notes from a phrase: {start s, length s, midi(, glide from midi)}. */
    static List<double[]> notes(double[][] phrase, double start, int transpose, double gap) {
        return notes(phrase, start, transpose, gap, Double.MAX_VALUE);
    }

    /** The same, stopping at {@code until} (a note running past it is cut short there). */
    static List<double[]> notes(double[][] phrase, double start, int transpose, double gap, double until) {
        List<double[]> out = new ArrayList<>();
        for (double[] p : phrase) {
            double on = start + p[0] * BEAT;
            if (on < until - 0.05) {
                out.add(new double[] {on, Math.min(p[1] * BEAT - gap, until - on), p[2] + transpose});
            }
        }
        return out;
    }

    /**
     * One continuous monophonic voice through the notes, gliding between them: a whistle (a near-sine with breath and a
     * vibrato that blooms on held notes), a theremin (pure, slow glides, a wide wobble), or a saw lead (two detuned saws
     * through a low-pass). A note's optional fourth value is a pitch to glide up or down from.
     */
    static void line(List<double[]> notes, Voice voice, double gain, double pan, double send) {
        if (notes.isEmpty()) {
            return;
        }
        double start = notes.get(0)[0] - 0.05;
        double end = 0;
        for (double[] note : notes) {
            end = Math.max(end, note[0] + note[1]);
        }
        end += 0.6;
        int len = n(end - start);
        double[] out = new double[len];
        double glide = voice == Voice.THEREMIN ? 0.12 : voice == Voice.LEAD ? 0.03 : 0.035;
        double vibRate = voice == Voice.THEREMIN ? 5.8 : voice == Voice.LEAD ? 5.2 : 5.5;
        double vibDepth = voice == Voice.THEREMIN ? 0.45 : voice == Voice.LEAD ? 0.15 : 0.22; // semitones
        double attack = voice == Voice.THEREMIN ? 0.15 : 0.025;
        double pitch = notes.get(0).length > 3 ? notes.get(0)[3] : notes.get(0)[2];
        double amp = 0, phase = 0, phase2 = 0, low = 0, band = 0;
        int current = 0;
        double onset = notes.get(0)[0];
        for (int i = 0; i < len; i++) {
            double t = start + i / (double) RATE;
            while (current + 1 < notes.size() && t >= notes.get(current + 1)[0]) {
                current++;
                onset = notes.get(current)[0];
                if (notes.get(current).length > 3) {
                    pitch = notes.get(current)[3];
                }
            }
            double[] note = notes.get(current);
            boolean on = t >= note[0] && t < note[0] + note[1] - 0.02;
            double target = note[2];
            double localGlide = note.length > 3 ? Math.max(glide, note[1] * 0.35) : glide;
            pitch += (target - pitch) * (1 - Math.exp(-1.0 / (localGlide * RATE)));
            double tau = on ? attack : 0.07;
            amp += ((on ? 1 : 0) - amp) * (1 - Math.exp(-1.0 / (tau * RATE)));
            double held = Math.max(0, Math.min(1, (t - onset - 0.18) / 0.35));
            double vib = vibDepth * (voice == Voice.THEREMIN ? 1 : held) * Math.sin(2 * Math.PI * vibRate * t);
            double f = mtof(pitch + vib);
            phase += f / RATE;
            phase -= Math.floor(phase);
            double s;
            if (voice == Voice.WHISTLE) {
                double breath = (RANDOM.nextDouble() * 2 - 1) * 0.12 * Math.sin(2 * Math.PI * phase);
                s = Math.sin(2 * Math.PI * phase) + 0.035 * Math.sin(4 * Math.PI * phase) + breath + (RANDOM.nextDouble() * 2 - 1) * 0.015;
            } else if (voice == Voice.THEREMIN) {
                s = Math.sin(2 * Math.PI * phase) + 0.12 * Math.sin(4 * Math.PI * phase) + 0.06 * Math.sin(6 * Math.PI * phase);
            } else {
                phase2 += f * 1.006 / RATE;
                phase2 -= Math.floor(phase2);
                double raw = (2 * phase - 1) + (2 * phase2 - 1);
                double fc = 2 * Math.sin(Math.PI * Math.min(2600 + 1400 * amp, RATE / 6.0) / RATE);
                low += fc * band;
                double high = raw - low - 0.7 * band;
                band += fc * high;
                s = low * 0.6;
            }
            out[i] = s * amp;
        }
        put(out, start, gain, pan, send);
    }

    // ================================================================ instruments

    /** Karplus-Strong plucked string: a burst of filtered noise round a tuned delay loop with a slow loss. */
    static double[] pluck(double freq, double length, double bright, double ring, double pick) {
        int len = n(length + 0.05);
        double[] y = new double[len];
        double period = RATE / freq;
        int exc = (int) period;
        double lp = 0;
        double[] burst = new double[exc];
        for (int i = 0; i < exc; i++) {
            lp += bright * ((RANDOM.nextDouble() * 2 - 1) - lp);
            burst[i] = lp;
        }
        if (pick > 0) {
            int off = Math.max(1, (int) (period * pick));
            for (int i = exc - 1; i >= off; i--) {
                burst[i] -= burst[i - off];
            }
        }
        double loss = Math.pow(0.001, 1.0 / (freq * Math.max(0.05, ring)));
        double delay = period - 0.5;
        for (int i = 0; i < len; i++) {
            double x = i < exc ? burst[i] : 0;
            double fb = 0;
            double d = i - delay;
            if (d >= 1) {
                int k = (int) d;
                double frac = d - k;
                double a = y[k] * (1 - frac) + y[Math.min(k + 1, i - 1)] * frac;
                double b = y[k - 1] * (1 - frac) + y[k] * frac;
                fb = loss * 0.5 * (a + b);
            }
            y[i] = x + fb;
        }
        // A short release at the end of the note
        int rel = Math.min(len, n(0.05));
        for (int i = 0; i < rel; i++) {
            y[len - 1 - i] *= i / (double) rel;
        }
        return y;
    }

    /** A banjo string: bright, plucked near the bridge, short, with the head's ring (a resonance near 1 kHz). */
    static double[] banjo(double freq) {
        double[] y = pluck(freq, 0.7, 0.85, 0.55, 0.12);
        double[] body = y.clone();
        svf(body, i -> 1100, 0.25, 1);
        for (int i = 0; i < y.length; i++) {
            y[i] = y[i] * 0.8 + body[i] * 0.5;
        }
        return y;
    }

    /** A strum: the strings a few milliseconds apart, down or up. */
    static void strum(int[] notes, double t, double gain, double length, double pan, boolean full) {
        for (int k = 0; k < notes.length; k++) {
            double ring = full ? Math.min(length, 2.5) : 0.12;
            double[] s = pluck(mtof(notes[k]), length, full ? 0.55 : 0.7, ring, 0.2);
            if (!full) {
                // Palm-muted chick: damped fast
                for (int i = 0; i < s.length; i++) {
                    s[i] *= Math.exp(-i / (0.05 * RATE));
                }
            }
            put(s, t + k * 0.011, gain / Math.sqrt(notes.length), pan + 0.08 * k, 0.12);
        }
    }

    static double[] synthKick() {
        int len = n(0.6);
        double[] y = new double[len];
        double phase = 0;
        for (int i = 0; i < len; i++) {
            double t = i / (double) RATE;
            double f = 42 + 160 * Math.exp(-t / 0.03);
            phase += 2 * Math.PI * f / RATE;
            y[i] = Math.tanh(1.8 * Math.sin(phase) * Math.exp(-t / 0.22)) + (i < 90 ? (RANDOM.nextDouble() - 0.5) * 0.6 : 0);
        }
        return y;
    }

    static double[] snare(double snap) {
        int len = n(0.3);
        double[] noise = noise(len);
        svf(noise, i -> 4500, 0.9, 2);
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            double t = i / (double) RATE;
            y[i] = noise[i] * Math.exp(-t / (0.06 + 0.08 * snap)) + 0.5 * Math.sin(2 * Math.PI * 185 * t) * Math.exp(-t / 0.05);
        }
        return y;
    }

    static double[] clap() {
        int len = n(0.4);
        double[] noise = noise(len);
        svf(noise, i -> 1300, 0.6, 1);
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            double t = i / (double) RATE;
            double env = 0;
            for (int k = 0; k < 3; k++) {
                double tk = t - k * 0.011;
                if (tk >= 0) {
                    env = Math.max(env, Math.exp(-tk / (k == 2 ? 0.12 : 0.008)));
                }
            }
            y[i] = noise[i] * env * 1.6;
        }
        return y;
    }

    static double[] hat(double decay) {
        int len = n(decay * 6);
        double[] y = noise(len);
        svf(y, i -> 7500, 0.8, 2);
        for (int i = 0; i < len; i++) {
            y[i] *= Math.exp(-i / (decay * RATE));
        }
        return y;
    }

    static double[] tom(double freq) {
        int len = n(0.6);
        double[] y = new double[len];
        double phase = 0;
        for (int i = 0; i < len; i++) {
            double t = i / (double) RATE;
            phase += 2 * Math.PI * freq * (1 + 0.5 * Math.exp(-t / 0.04)) / RATE;
            y[i] = Math.sin(phase) * Math.exp(-t / 0.2) + (RANDOM.nextDouble() - 0.5) * 0.3 * Math.exp(-t / 0.02);
        }
        return y;
    }

    static double[] timpani(int midi) {
        int len = n(2.5);
        double f = mtof(midi);
        double[] ratios = {1, 1.5, 1.99, 2.44};
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            double t = i / (double) RATE;
            double s = 0;
            for (int k = 0; k < ratios.length; k++) {
                s += Math.sin(2 * Math.PI * f * ratios[k] * (1 + 0.03 * Math.exp(-t / 0.1)) * t) / (k + 1) * Math.exp(-t / (0.9 / (k + 1)));
            }
            y[i] = s + (RANDOM.nextDouble() - 0.5) * 0.4 * Math.exp(-t / 0.015);
        }
        return y;
    }

    static double[] crash() {
        int len = n(3.0);
        double[] y = noise(len);
        svf(y, i -> 5000, 0.9, 2);
        for (int i = 0; i < len; i++) {
            y[i] *= Math.exp(-i / (0.9 * RATE));
        }
        return y;
    }

    /** A gong: inharmonic partials, the high ones blooming a little after the strike. */
    static double[] gong() {
        int len = n(5.0);
        double[] ratios = {1, 1.52, 2.04, 2.71, 3.38, 4.15, 5.6};
        double f = 82;
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            double t = i / (double) RATE;
            double s = 0;
            for (int k = 0; k < ratios.length; k++) {
                double bloom = k < 2 ? 1 : 1 - Math.exp(-t / (0.08 * k));
                s += Math.sin(2 * Math.PI * f * ratios[k] * t + k) * bloom * Math.exp(-t / (2.5 / (1 + 0.3 * k))) / (1 + 0.4 * k);
            }
            y[i] = s * 0.5;
        }
        return y;
    }

    static double[] anvil() {
        int len = n(1.5);
        double[] ratios = {1, 2.76, 5.4, 8.93};
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            double t = i / (double) RATE;
            double s = 0;
            for (int k = 0; k < ratios.length; k++) {
                s += Math.sin(2 * Math.PI * 880 * ratios[k] * t) * Math.exp(-t / (0.7 / (1 + k))) / (1 + k * 0.5);
            }
            y[i] = s * 0.6 + (RANDOM.nextDouble() - 0.5) * Math.exp(-t / 0.004);
        }
        return y;
    }

    static double[] synthBass(double freq, double length) {
        int len = n(length);
        double[] y = new double[len];
        double phase = 0, phase2 = 0;
        for (int i = 0; i < len; i++) {
            phase += freq / RATE;
            phase -= Math.floor(phase);
            phase2 += freq * 0.995 / RATE;
            phase2 -= Math.floor(phase2);
            y[i] = (2 * phase - 1) + (phase2 < 0.5 ? 0.6 : -0.6);
        }
        double[] env = new double[len];
        for (int i = 0; i < len; i++) {
            env[i] = Math.exp(-i / (0.04 * RATE));
        }
        svf(y, i -> 180 + 1600 * env[i], 0.5, 0);
        for (int i = 0; i < len; i++) {
            y[i] *= Math.min(1, i / 60.0) * Math.min(1, (len - i) / 200.0);
        }
        return y;
    }

    static double[] pulse(double freq, double length) {
        int len = n(length);
        double[] y = new double[len];
        double phase = 0;
        for (int i = 0; i < len; i++) {
            phase += freq / RATE;
            phase -= Math.floor(phase);
            y[i] = (phase < 0.3 ? 1 : -1) * Math.exp(-i / (0.06 * RATE)) * Math.min(1, i / 30.0);
        }
        svf(y, i -> 3500, 0.7, 0);
        return y;
    }

    static double[] pad(int[] chord, double length, double cutoff) {
        int len = n(length + 0.3);
        double[] y = new double[len];
        double[] det = {-0.006, 0, 0.0055};
        for (int note : chord) {
            double f = mtof(note);
            for (double d : det) {
                double phase = RANDOM.nextDouble();
                for (int i = 0; i < len; i++) {
                    phase += f * (1 + d) / RATE;
                    phase -= Math.floor(phase);
                    y[i] += (2 * phase - 1);
                }
            }
        }
        svf(y, i -> cutoff, 0.8, 0);
        for (int i = 0; i < len; i++) {
            double t = i / (double) RATE;
            y[i] *= Math.min(1, t / 0.25) * Math.min(1, (len - i) / (0.3 * RATE)) / chord.length;
        }
        return y;
    }

    /** Wordless voices on "ah": stacked partials with the vowel's formants lifted, slowly swelling. */
    static void choir(int[] chord, double t, double length, double gain) {
        int len = n(length + 1.0);
        double[] y = new double[len];
        double[] formants = {700, 1150, 2800};
        for (int note : chord) {
            double f = mtof(note);
            for (int voice = 0; voice < 3; voice++) {
                double detune = 1 + (voice - 1) * 0.004;
                double vibPhase = RANDOM.nextDouble() * 6;
                for (int h = 1; h <= 12; h++) {
                    double fh = f * h;
                    if (fh > 6000) {
                        break;
                    }
                    double amp = 0;
                    for (double fm : formants) {
                        amp += Math.exp(-Math.pow((fh - fm) / 260, 2));
                    }
                    amp = (amp + 0.15) / h;
                    double phase = RANDOM.nextDouble() * 6;
                    for (int i = 0; i < len; i++) {
                        double tt = i / (double) RATE;
                        double vib = 1 + 0.004 * Math.sin(2 * Math.PI * 5.0 * tt + vibPhase);
                        phase += 2 * Math.PI * fh * detune * vib / RATE;
                        y[i] += Math.sin(phase) * amp;
                    }
                }
            }
        }
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            y[i] *= Math.min(1, tt / 0.6) * Math.min(1, Math.max(0, (length + 0.8 - tt) / 0.8)) / (chord.length * 3);
        }
        put(y, t, gain * 4, 0.0, 0.6);
    }

    /** The trailer braam: low saws stacked, driven, the filter opening and closing. */
    static void braam(double t, int[] notes, double length, double gain) {
        int len = n(length);
        double[] y = new double[len];
        for (int note : notes) {
            double f = mtof(note);
            for (double d : new double[] {-0.008, 0, 0.007}) {
                double phase = RANDOM.nextDouble();
                for (int i = 0; i < len; i++) {
                    phase += f * (1 + d) / RATE;
                    phase -= Math.floor(phase);
                    y[i] += 2 * phase - 1;
                }
            }
        }
        svf(y, i -> 200 + 2200 * Math.exp(-i / (0.5 * RATE)) * Math.min(1, i / (0.08 * RATE)), 0.6, 0);
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            y[i] = Math.tanh(y[i] * 0.35) * Math.min(1, tt / 0.03) * Math.exp(-tt / (length * 0.45));
        }
        put(y, t, gain, 0.0, 0.3);
    }

    // ================================================================ sound design

    static void gunshot(double t, double gain, double pan, boolean echo) {
        int len = n(0.6);
        double[] crack = noise(len);
        svf(crack, i -> 2500, 0.9, 2);
        double[] body = noise(len);
        svf(body, i -> 700, 0.8, 0);
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            y[i] = crack[i] * Math.exp(-tt / 0.012) * 1.5 + body[i] * Math.exp(-tt / 0.09) * 2.5 + Math.sin(2 * Math.PI * 58 * tt) * Math.exp(-tt / 0.08) * 0.8;
        }
        put(y, t, gain, pan, 0.35);
        if (echo) {
            double[] far = y.clone();
            svf(far, i -> 1200, 0.9, 0);
            put(far, t + 0.42, gain * 0.28, -0.6, 0.6);
            put(far, t + 0.95, gain * 0.12, 0.6, 0.7);
        }
    }

    /** The western ricochet: a whine falling and wavering. */
    static void ricochet(double t, double gain, double pan) {
        int len = n(0.55);
        double[] y = new double[len];
        double phase = 0;
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            double f = 2900 * Math.exp(-tt / 0.6) * (1 + 0.03 * Math.sin(2 * Math.PI * 38 * tt));
            phase += 2 * Math.PI * f / RATE;
            y[i] = Math.sin(phase) * Math.min(1, tt / 0.01) * Math.exp(-tt / 0.22);
        }
        put(y, t, gain, pan, 0.4);
    }

    static void cock(double t, double gain) {
        click(t, gain);
        click(t + 0.09, gain * 1.3);
    }

    static void click(double t, double gain) {
        int len = n(0.03);
        double[] y = noise(len);
        svf(y, i -> 3500, 0.5, 1);
        for (int i = 0; i < len; i++) {
            y[i] *= Math.exp(-i / (0.004 * RATE)) * 2;
        }
        put(y, t, gain, 0.1, 0.1);
    }

    static void explosion(double t, double gain) {
        int len = n(2.5);
        double[] y = noise(len);
        svf(y, i -> 120 + 1500 * Math.exp(-i / (0.15 * RATE)), 0.7, 0);
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            y[i] = y[i] * 3 * Math.exp(-tt / 0.6) + Math.sin(2 * Math.PI * (40 + 30 * Math.exp(-tt / 0.1)) * tt) * Math.exp(-tt / 0.4);
        }
        put(y, t, gain, 0.0, 0.3);
    }

    static void fuse(double t, double until, double gain) {
        int len = n(until - t);
        double[] y = noise(len);
        svf(y, i -> 6000, 0.5, 1);
        for (int i = 0; i < len; i++) {
            y[i] *= 0.6 + 0.4 * RANDOM.nextDouble();
        }
        put(y, t, gain, 0.2, 0.05);
    }

    static void hiss(double t, double length, double gain) {
        int len = n(length);
        double[] y = noise(len);
        svf(y, i -> 5000, 0.8, 2);
        for (int i = 0; i < len; i++) {
            double k = i / (double) len;
            y[i] *= Math.sin(Math.PI * k);
        }
        put(y, t, gain, -0.3, 0.2);
    }

    /** The airship's engine: a slow-beating propeller drone over the hiss of its boiler. */
    static void propeller(double t, double until, double gain) {
        int len = n(until - t);
        double[] y = new double[len];
        double[] air = noise(len);
        svf(air, i -> 800, 0.5, 1);
        double phase = 0;
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            phase += 2 * Math.PI * 55 / RATE;
            double blade = 0.5 + 0.5 * Math.sin(2 * Math.PI * 6.5 * tt); // the blades beating past
            double env = Math.min(1, tt / 0.8) * Math.min(1, (len - i) / (0.5 * RATE));
            y[i] = ((Math.sin(phase) + 0.4 * Math.sin(2 * phase) + 0.2 * Math.sin(3 * phase)) * 0.4 + air[i] * 1.2 * blade) * env;
        }
        put(y, t, gain, 0.3, 0.2);
    }

    /** A three-pipe steam whistle (a minor-ish chord), bending up as the steam reaches it. */
    static void steamWhistle(double t, double length, double gain) {
        int len = n(length);
        double[] y = new double[len];
        double[] pipes = {mtof(69), mtof(72), mtof(76)};
        double[] phase = new double[3];
        double[] breath = noise(len);
        svf(breath, i -> 1500, 0.3, 1);
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            double bend = 1 - 0.04 * Math.exp(-tt / 0.12);
            double env = Math.min(1, tt / 0.08) * Math.min(1, (length - tt) / 0.35);
            double s = 0;
            for (int k = 0; k < 3; k++) {
                phase[k] += 2 * Math.PI * pipes[k] * bend / RATE;
                s += Math.sin(phase[k]) + 0.3 * Math.sin(2 * phase[k]) + 0.15 * Math.sin(3 * phase[k]);
            }
            y[i] = (s / 3 + breath[i] * 0.4) * env;
        }
        put(y, t, gain, 0.2, 0.45);
    }

    static void wind(double t, double until, double gain) {
        int len = n(until - t);
        double[] y = noise(len);
        double slow = RANDOM.nextDouble() * 6;
        svf(y, i -> 300 + 250 * Math.sin(2 * Math.PI * 0.23 * i / RATE + slow) + 150 * Math.sin(2 * Math.PI * 0.61 * i / RATE), 0.3, 1);
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            y[i] *= (0.6 + 0.4 * Math.sin(2 * Math.PI * 0.17 * tt + slow)) * Math.min(1, tt / 0.8) * Math.min(1, (until - t - tt) / 1.5);
        }
        put(y, t, gain * 3, -0.2, 0.1);
    }

    static void crickets(double t, double until, double gain) {
        int len = n(until - t);
        double[] y = new double[len];
        for (double at = 0; at < until - t; at += 0.35 + RANDOM.nextDouble() * 0.4) {
            double f = 4300 + RANDOM.nextDouble() * 500;
            for (int k = 0; k < 3; k++) {
                int s = n(at + k * 0.045);
                for (int i = 0; i < n(0.03) && s + i < len; i++) {
                    double tt = i / (double) RATE;
                    y[s + i] += Math.sin(2 * Math.PI * f * tt) * Math.sin(Math.PI * tt / 0.03);
                }
            }
        }
        put(y, t, gain, 0.5, 0.2);
        put(y, t + 0.17, gain * 0.6, -0.5, 0.2);
    }

    static void drone(double t, double until, double gain) {
        int len = n(until - t + 0.5);
        double[] y = new double[len];
        double[] f = {mtof(33), mtof(33) * 1.003, mtof(40), mtof(45) * 1.06};
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            double s = 0;
            for (int k = 0; k < f.length; k++) {
                s += Math.sin(2 * Math.PI * f[k] * tt) * (k == 3 ? 0.15 : 0.5);
            }
            double grow = Math.min(1, tt / (until - t));
            y[i] = s * grow * grow * Math.min(1, (len - i) / (0.4 * RATE));
        }
        put(y, t, gain, 0.0, 0.2);
    }

    /** The saucer over the camera: a hum and a rush of air, the pitch falling as it passes, front to back. */
    static void saucerPass(double t, double gain) {
        double before = 1.2, after = 1.0;
        int len = n(before + after);
        double[] hum = new double[len];
        double[] air = noise(len);
        double phase = 0;
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE - before;
            double doppler = 1 + 0.12 * Math.tanh(-tt * 3);
            phase += 2 * Math.PI * 70 * doppler / RATE;
            hum[i] = Math.sin(phase) + 0.5 * Math.sin(2 * phase) + 0.3 * Math.sin(3.01 * phase) + 0.2 * Math.sin(7 * phase);
        }
        svf(air, i -> 600 + 3000 * Math.exp(-Math.pow((i / (double) RATE - before) / 0.35, 2)), 0.6, 1);
        double[] l = new double[len], r = new double[len];
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE - before;
            double near = 1 / (1 + Math.pow(tt / 0.35, 2));
            double s = (hum[i] * 0.5 + air[i] * 2.0) * near;
            double p = Math.tanh(tt * 2); // behind and centre, then away to the right
            l[i] = s * Math.cos((p + 1) * Math.PI / 4);
            r[i] = s * Math.sin((p + 1) * Math.PI / 4);
        }
        putStereo(l, r, t - before, gain, 0.3);
    }

    /** The beam: a rising "vwoom" as it comes on, then a pulsing hum with a shimmer until the take. */
    static void beamOn(double t, double until, double gain) {
        int len = n(until - t + 0.2);
        double[] y = new double[len];
        double p1 = 0, p2 = 0, p3 = 0;
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            double sweep = 1 + 2 * (1 - Math.exp(-tt / 0.25));
            p1 += 2 * Math.PI * 110 * sweep / RATE;
            p2 += 2 * Math.PI * 110 * sweep * 1.5 * 1.004 / RATE;
            p3 += 2 * Math.PI * (1760 + 40 * Math.sin(2 * Math.PI * 7 * tt)) / RATE;
            double pulse = 0.75 + 0.25 * Math.sin(2 * Math.PI * 4 * tt);
            double hum = (Math.sin(p1) + 0.6 * Math.sin(p2) + 0.25 * Math.sin(3 * p1)) * pulse;
            double shimmer = Math.sin(p3) * 0.15 * Math.min(1, tt / 0.6);
            y[i] = (hum * 0.5 + shimmer) * Math.min(1, tt / 0.05) * Math.min(1, (len - i) / (0.1 * RATE));
        }
        put(y, t, gain, 0.0, 0.4);
    }

    static void riser(double t, double until, double gain) {
        int len = n(until - t);
        double[] y = noise(len);
        svf(y, i -> 300 * Math.pow(25, i / (double) len), 0.3, 1);
        double phase = 0;
        for (int i = 0; i < len; i++) {
            double k = i / (double) len;
            phase += 2 * Math.PI * (110 * Math.pow(8, k)) / RATE;
            y[i] = (y[i] * 1.5 + 0.3 * Math.sin(phase)) * k * k;
        }
        put(y, t, gain, 0.0, 0.3);
    }

    /** The take: a bright upward sweep, sucked in. */
    static void zap(double t, double gain) {
        int len = n(0.5);
        double[] y = new double[len];
        double phase = 0;
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            phase += 2 * Math.PI * (200 * Math.pow(15, Math.min(1, tt / 0.25))) / RATE;
            y[i] = (Math.sin(phase) + 0.3 * Math.sin(3 * phase)) * Math.min(1, tt / 0.01) * Math.exp(-tt / 0.15);
        }
        put(y, t, gain, 0.0, 0.5);
    }

    static void whoosh(double t, double length, double gain, boolean up) {
        int len = n(length);
        double[] y = noise(len);
        svf(y, i -> up ? 400 * Math.pow(10, i / (double) len) : 4000 * Math.pow(0.1, i / (double) len), 0.4, 1);
        for (int i = 0; i < len; i++) {
            double k = i / (double) len;
            y[i] *= Math.sin(Math.PI * Math.pow(k, 0.6));
        }
        put(y, t, gain * 2, 0.4, 0.3);
    }

    static void reverseCymbal(double t, double until, double gain) {
        int len = n(until - t);
        double[] y = noise(len);
        svf(y, i -> 6000, 0.8, 2);
        for (int i = 0; i < len; i++) {
            double k = i / (double) len;
            y[i] *= Math.pow(k, 3);
        }
        put(y, t, gain, 0.0, 0.2);
    }

    /** A trailer impact: a deep boom, a crack and a sub drop. */
    static void impact(double t, double gain) {
        int len = n(2.5);
        double[] y = new double[len];
        double[] crack = noise(len);
        svf(crack, i -> 1800, 0.7, 1);
        double phase = 0;
        for (int i = 0; i < len; i++) {
            double tt = i / (double) RATE;
            phase += 2 * Math.PI * (30 + 70 * Math.exp(-tt / 0.08)) / RATE;
            y[i] = Math.tanh(2 * Math.sin(phase)) * Math.exp(-tt / 0.7) + crack[i] * Math.exp(-tt / 0.05) * 1.5;
        }
        put(y, t, gain, 0.0, 0.35);
    }

    // ================================================================ plumbing

    static int n(double seconds) {
        return (int) Math.round(seconds * RATE);
    }

    static double mtof(double midi) {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }

    static double[] noise(int len) {
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            y[i] = RANDOM.nextDouble() * 2 - 1;
        }
        return y;
    }

    /** Mixes a mono sound in at {@code t} seconds: equal-power pan (-1 left to 1 right), some of it sent to the reverb. */
    static void put(double[] x, double t, double gain, double pan, double send) {
        double gl = gain * Math.cos((pan + 1) * Math.PI / 4);
        double gr = gain * Math.sin((pan + 1) * Math.PI / 4);
        int start = n(t);
        for (int i = 0; i < x.length; i++) {
            int k = start + i;
            if (k < 0) {
                continue;
            }
            if (k >= L.length) {
                break;
            }
            L[k] += x[i] * gl;
            R[k] += x[i] * gr;
            SL[k] += x[i] * gl * send;
            SR[k] += x[i] * gr * send;
        }
    }

    static void putStereo(double[] l, double[] r, double t, double gain, double send) {
        int start = n(t);
        for (int i = 0; i < l.length; i++) {
            int k = start + i;
            if (k < 0) {
                continue;
            }
            if (k >= L.length) {
                break;
            }
            L[k] += l[i] * gain;
            R[k] += r[i] * gain;
            SL[k] += l[i] * gain * send;
            SR[k] += r[i] * gain * send;
        }
    }

    /** A state-variable filter, in place, its cutoff (Hz) per sample; damping 0.1 (ringing) to 1.4; mode 0 low, 1 band, 2 high. */
    static void svf(double[] x, IntToDoubleFunction cutoff, double damping, int mode) {
        double low = 0, band = 0;
        for (int i = 0; i < x.length; i++) {
            double f = 2 * Math.sin(Math.PI * Math.max(20, Math.min(cutoff.applyAsDouble(i), RATE / 6.5)) / RATE);
            low += f * band;
            double high = x[i] - low - damping * band;
            band += f * high;
            x[i] = mode == 0 ? low : mode == 1 ? band : high;
        }
    }

    /** A big hall (Freeverb's eight combs and four allpasses a side, the right side's a little longer). */
    static double[][] reverb(double[] inL, double[] inR) {
        int[] combs = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
        int[] allpasses = {556, 441, 341, 225};
        double scale = RATE / 44100.0;
        double[][] out = new double[2][inL.length];
        for (int side = 0; side < 2; side++) {
            double[] in = side == 0 ? inL : inR;
            double[] wet = out[side];
            int spread = side == 0 ? 0 : 23;
            for (int c : combs) {
                int len = (int) ((c + spread) * scale);
                double[] buf = new double[len];
                int at = 0;
                double damp = 0;
                for (int i = 0; i < in.length; i++) {
                    double o = buf[at];
                    damp = o * 0.75 + damp * 0.25;
                    buf[at] = in[i] * 0.015 + damp * 0.86;
                    at = (at + 1) % len;
                    wet[i] += o;
                }
            }
            for (int a : allpasses) {
                int len = (int) ((a + spread) * scale);
                double[] buf = new double[len];
                int at = 0;
                for (int i = 0; i < wet.length; i++) {
                    double o = buf[at];
                    double x = wet[i];
                    buf[at] = x + o * 0.5;
                    wet[i] = o - x;
                    at = (at + 1) % len;
                }
            }
            for (int i = 0; i < wet.length; i++) {
                wet[i] *= 3.0;
            }
        }
        return out;
    }
}
