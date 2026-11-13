import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Renders the Hollow Earth's score (45a): five slow pieces, one per biome, all grown from the reveal swell
 * ({@code Swell.java}): its open E chord with the ninth, its string-and-choir voices, and its rising figure
 * E, F sharp, G sharp, B, which every piece states once. Long chords cross-fade under a sparse lead; there's no
 * percussion and no pulse, so a piece can sit under hours of play. Additive synthesis only (no samples), so the
 * recordings are entirely ours. Stereo, as background music isn't played from a position.
 * Run from the project root:  java tools/music/HollowScore.java build/score
 * It writes one WAV per piece; convert them with ffmpeg (ASSETS.md, Music).
 */
public class HollowScore {
    static final int RATE = 44100;

    enum Lead { REED, BELL, VOICE }

    /** One piece. A chord is its notes low to high (MIDI); a tune is {chord index, beat (8 to a chord), note, beats long}. */
    record Piece(String name, double chordSeconds, double brightness, double padGain, Lead lead, double leadGain,
                 double wet, double breathe, int chimes, int[][] chords, double[][] tune,
                 double fadeIn, double tail, boolean loop, int pulse) {}

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/score");
        out.mkdirs();
        for (Piece piece : pieces()) {
            if (args.length > 1 && !piece.name().startsWith(args[1])) {
                continue;
            }
            render(piece, new File(out, piece.name() + ".wav"));
            System.out.println(piece.name());
        }
    }

    static List<Piece> pieces() {
        List<Piece> all = new ArrayList<>();
        // Primordial Jungle, "Green Noon": warm and close. E major, a soft reed over full chords.
        all.add(new Piece("primordial_jungle", 16, 0.55, 1.0, Lead.REED, 0.50, 0.50, 0.0, 0, new int[][] {
                {40, 47, 52, 56, 59, 66}, // E add9
                {45, 52, 57, 59, 61, 64}, // A add9
                {37, 44, 52, 56, 59, 64}, // C#m7
                {35, 47, 54, 59, 64, 66}, // B sus4
                {40, 47, 52, 56, 59, 66}, // E add9
                {42, 49, 57, 61, 64, 69}, // F#m7
                {45, 52, 57, 59, 61, 64}, // A add9
                {35, 47, 54, 59, 64, 66}, // B sus4
                {40, 47, 52, 56, 59, 66}, // E add9
        }, new double[][] {
                {1, 1, 64, 2}, {1, 3, 66, 1}, {1, 4, 68, 3},
                {2, 1, 71, 3}, {2, 5, 68, 2},
                {3, 2, 66, 4},
                {4, 0, 64, 1}, {4, 1, 66, 1}, {4, 2, 68, 1}, {4, 3, 71, 4}, // the figure
                {5, 1, 73, 2}, {5, 3, 71, 1}, {5, 4, 69, 3},
                {6, 0, 68, 2}, {6, 3, 64, 3},
                {7, 1, 66, 5},
                {8, 2, 64, 5},
        }, 4.0, 10.0, false, 0));
        // Crystal Steppes, "Glass Wind": glassy and bright. E lydian, thin high chords and bells like wind chimes.
        all.add(new Piece("crystal_steppes", 14, 0.85, 0.62, Lead.BELL, 0.30, 0.62, 0.0, 4, new int[][] {
                {52, 59, 64, 66, 68, 71}, // E add9
                {52, 58, 61, 66, 70, 73}, // F#/E (the lydian glint)
                {56, 59, 63, 66, 71},     // G#m7
                {47, 54, 59, 63, 66, 71}, // B
                {52, 59, 64, 66, 68, 71}, // E add9
                {49, 56, 59, 63, 64, 68}, // C#m9
                {52, 58, 61, 66, 70, 73}, // F#/E
                {47, 54, 59, 63, 66, 71}, // B
                {52, 59, 63, 64, 68, 71}, // E maj7
                {52, 59, 64, 66, 68, 71}, // E add9
        }, new double[][] {
                {4, 0, 76, 1}, {4, 1, 78, 1}, {4, 2, 80, 1}, {4, 3, 83, 4}, // the figure, an octave up
                {8, 1, 83, 2}, {8, 3, 80, 2}, {8, 5, 78, 2},
                {9, 2, 76, 5},
        }, 4.0, 10.0, false, 0));
        // Ember Badlands, "Cinder Country": low and dry. E minor with a phrygian turn, a lone low reed, little hall.
        all.add(new Piece("ember_badlands", 18, 0.30, 1.0, Lead.REED, 0.55, 0.30, 0.0, 0, new int[][] {
                {28, 40, 47, 52, 55, 59}, // Em
                {28, 40, 48, 52, 55, 59}, // C maj7 / E
                {33, 45, 52, 57, 59, 60}, // Am add9
                {35, 47, 52, 54, 59},     // B sus4
                {28, 40, 47, 52, 55, 59}, // Em
                {28, 41, 48, 53, 57, 60}, // F / E
                {33, 45, 52, 57, 59, 60}, // Am add9
                {28, 40, 47, 52, 55, 59, 66}, // Em add9
        }, new double[][] {
                {1, 1, 64, 3}, {1, 5, 67, 2},
                {2, 0, 69, 2}, {2, 2, 67, 1}, {2, 3, 64, 4},
                {3, 2, 66, 2}, {3, 4, 64, 3},
                {4, 0, 64, 1}, {4, 1, 66, 1}, {4, 2, 67, 1}, {4, 3, 71, 4}, // the figure, in the minor
                {5, 1, 72, 2}, {5, 3, 69, 1}, {5, 4, 65, 3},
                {6, 1, 64, 2}, {6, 3, 60, 1}, {6, 4, 59, 3},
                {7, 2, 64, 5},
        }, 4.0, 10.0, false, 0));
        // Concavian Highlands, "The High Stones": thin and high. Open fifths, E mixolydian, one wordless voice.
        all.add(new Piece("concavian_highlands", 20, 0.60, 0.70, Lead.VOICE, 0.42, 0.68, 0.0, 0, new int[][] {
                {52, 59, 64, 71},     // E5
                {52, 57, 62, 66, 76}, // D add9 / E
                {52, 57, 64, 69},     // A5 / E
                {52, 59, 64, 71},     // E5
                {52, 59, 62, 66, 69}, // Bm7 / E
                {45, 57, 59, 64, 73}, // A add9
                {52, 59, 66, 71},     // E5 add9
        }, new double[][] {
                {0, 3, 76, 4},
                {1, 0, 78, 2}, {1, 3, 74, 4},
                {2, 1, 73, 3}, {2, 5, 76, 3},
                {3, 1, 76, 1}, {3, 2, 78, 1}, {3, 3, 80, 1}, {3, 4, 83, 4}, // the figure
                {4, 0, 81, 3}, {4, 4, 78, 3},
                {5, 1, 76, 2}, {5, 4, 73, 3},
                {6, 1, 71, 2}, {6, 4, 76, 4},
        }, 4.0, 10.0, false, 0));
        // Still Sea, "Still Water": slow and deep. C sharp minor, chords that breathe like a swell, arriving on the
        // overture's E chord at the end.
        all.add(new Piece("still_sea", 22, 0.25, 1.0, Lead.VOICE, 0.34, 0.60, 0.35, 0, new int[][] {
                {37, 44, 52, 56, 63},     // C#m add9
                {33, 45, 52, 56, 61, 64}, // A maj7
                {32, 44, 52, 56, 59, 64}, // E / G#
                {30, 42, 49, 56, 57, 61}, // F#m9
                {37, 44, 52, 56, 63},     // C#m add9
                {33, 45, 52, 57, 59, 61}, // A add9
                {28, 40, 47, 52, 56, 59, 66}, // E add9
        }, new double[][] {
                {1, 2, 68, 4},
                {2, 1, 71, 2}, {2, 4, 68, 3},
                {3, 0, 69, 3}, {3, 4, 66, 3},
                {4, 2, 64, 2}, {4, 5, 68, 3},
                {5, 1, 64, 1}, {5, 2, 66, 1}, {5, 3, 68, 1}, {5, 4, 71, 4}, // the figure
                {6, 2, 66, 5},
        }, 4.0, 10.0, false, 0));
        moments(all);
        return all;
    }

    /**
     * 45c: the two boss themes and the five stings. A theme is a loop (it's rendered three times round and the middle
     * taken, so its start already carries the end's tail) with a pulse in the bass, which the biome pieces never have. A
     * sting is two or three chords that swell and go, with a few notes over them.
     */
    static void moments(List<Piece> all) {
        // The Sun Archon: E minor with the phrygian F, bright and hard, a choir over a steady pulse
        all.add(new Piece("boss_sun_archon", 8, 0.95, 0.85, Lead.VOICE, 0.50, 0.45, 0.0, 0, new int[][] {
                {28, 40, 47, 52, 55, 59, 64}, // Em
                {28, 41, 48, 53, 57, 60, 65}, // F / E
                {31, 43, 50, 55, 59, 62, 67}, // G
                {35, 47, 51, 54, 57, 59, 63}, // B7
                {28, 40, 47, 52, 55, 59, 64}, // Em
                {33, 45, 52, 57, 60, 64, 69}, // Am
                {28, 41, 48, 53, 57, 60, 65}, // F / E
                {35, 47, 51, 54, 57, 59, 66}, // B7
        }, new double[][] {
                {0, 0, 76, 3}, {0, 4, 79, 3},
                {1, 0, 77, 4}, {1, 5, 76, 2},
                {2, 0, 74, 3}, {2, 4, 79, 3},
                {3, 0, 78, 5},
                {4, 0, 76, 1}, {4, 1, 78, 1}, {4, 2, 79, 1}, {4, 3, 83, 4}, // the figure, in the minor
                {5, 0, 84, 3}, {5, 4, 81, 3},
                {6, 0, 81, 2}, {6, 2, 77, 2}, {6, 4, 76, 3},
                {7, 0, 75, 3}, {7, 4, 78, 3},
        }, 0.0, 0.0, true, 16));
        // The Mothership: B flat minor, a tritone from everything the Hollow sings in. Cold bells over a machine's pulse.
        all.add(new Piece("boss_mothership", 8, 0.75, 0.80, Lead.BELL, 0.42, 0.50, 0.0, 2, new int[][] {
                {34, 46, 53, 58, 61, 65}, // Bbm
                {34, 46, 54, 58, 61, 66}, // Gb / Bb
                {34, 46, 51, 58, 63, 66}, // Ebm / Bb
                {29, 41, 48, 53, 57, 63}, // F7
                {34, 46, 53, 58, 61, 65}, // Bbm
                {30, 42, 49, 54, 58, 61}, // Gb
                {28, 40, 47, 52, 58, 64}, // E with a B flat in it: the tritone itself
                {29, 41, 48, 53, 57, 63}, // F7
        }, new double[][] {
                {0, 0, 70, 1}, {0, 3, 73, 1}, {0, 6, 77, 1},
                {1, 0, 78, 1}, {1, 4, 73, 1},
                {2, 0, 75, 1}, {2, 3, 78, 1}, {2, 6, 82, 1},
                {3, 0, 81, 1}, {3, 4, 77, 1},
                {4, 0, 70, 1}, {4, 2, 73, 1}, {4, 4, 77, 1}, {4, 6, 82, 1},
                {5, 0, 85, 1}, {5, 4, 82, 1},
                {6, 0, 76, 1}, {6, 3, 82, 1}, {6, 6, 76, 1},
                {7, 0, 81, 1}, {7, 4, 75, 1},
        }, 0.0, 0.0, true, 16));
        // Well met: the first of the Kindled. Warm, and the figure whole, in bells.
        all.add(new Piece("sting_well_met", 3.5, 0.7, 0.9, Lead.BELL, 0.40, 0.60, 0.0, 0, new int[][] {
                {40, 47, 52, 56, 59, 66}, {45, 52, 57, 59, 61, 64},
        }, new double[][] {
                {0, 2, 76, 1}, {0, 3, 78, 1}, {0, 4, 80, 1}, {0, 5, 83, 1}, {1, 2, 85, 1}, {1, 4, 83, 1},
        }, 0.3, 5.0, false, 0));
        // The Sun Temple: low and grave, a voice that rises a fourth and stays
        all.add(new Piece("sting_sun_temple", 4.0, 0.5, 1.0, Lead.VOICE, 0.45, 0.60, 0.0, 0, new int[][] {
                {28, 40, 47, 52, 55, 59}, {35, 47, 52, 54, 59, 64},
        }, new double[][] {
                {0, 2, 64, 3}, {0, 6, 67, 2}, {1, 1, 71, 5},
        }, 0.3, 5.0, false, 0));
        // A Gray station, still running: the Mothership's key, and nothing resolves
        all.add(new Piece("sting_still_running", 4.0, 0.6, 0.9, Lead.BELL, 0.34, 0.55, 0.0, 0, new int[][] {
                {34, 46, 53, 58, 61, 65}, {28, 40, 47, 52, 58, 64},
        }, new double[][] {
                {0, 2, 70, 1}, {0, 5, 77, 1}, {1, 1, 76, 1}, {1, 4, 82, 1},
        }, 0.3, 5.0, false, 0));
        // The Drowned Quarter: the sea's key, coming up into the overture's chord as you come up into air
        all.add(new Piece("sting_drowned_quarter", 4.0, 0.4, 1.0, Lead.VOICE, 0.40, 0.65, 0.0, 0, new int[][] {
                {37, 44, 52, 56, 63}, {28, 40, 47, 52, 56, 59, 66},
        }, new double[][] {
                {0, 2, 68, 3}, {0, 6, 71, 2}, {1, 1, 78, 5},
        }, 0.3, 5.0, false, 0));
        // The leash broken: the figure at last in full voice, A to B to home
        all.add(new Piece("sting_cut_loose", 3.5, 0.9, 1.0, Lead.VOICE, 0.50, 0.60, 0.0, 3, new int[][] {
                {45, 52, 57, 59, 61, 64}, {35, 47, 54, 59, 64, 66}, {40, 47, 52, 56, 59, 66, 71},
        }, new double[][] {
                {0, 1, 76, 2}, {0, 4, 78, 2}, {1, 0, 80, 3}, {1, 4, 78, 3}, {2, 0, 83, 6},
        }, 0.3, 6.0, false, 0));
    }

    static void render(Piece p, File file) throws Exception {
        double tail = p.tail();
        int rounds = p.loop() ? 3 : 1;
        int[][] chords = new int[p.chords().length * rounds][];
        double[][] tune = new double[p.tune().length * rounds][];
        for (int r = 0; r < rounds; r++) {
            for (int c = 0; c < p.chords().length; c++) {
                chords[r * p.chords().length + c] = p.chords()[c];
            }
            for (int k = 0; k < p.tune().length; k++) {
                double[] note = p.tune()[k].clone();
                note[0] += r * p.chords().length;
                tune[r * p.tune().length + k] = note;
            }
        }
        double span = p.chords().length * p.chordSeconds();
        double length = chords.length * p.chordSeconds() + tail;
        int n = (int) (length * RATE);
        double[] left = new double[n];
        double[] right = new double[n];
        // The chords: each comes up over the last quarter of the one before and falls away under the next
        double lap = p.chordSeconds() * 0.30;
        for (int c = 0; c < chords.length; c++) {
            int[] chord = chords[c];
            double start = Math.max(0.0, c * p.chordSeconds() - lap);
            double end = (c + 1) * p.chordSeconds() + lap + (c == chords.length - 1 ? tail * 0.4 : 0.0);
            for (int v = 0; v < chord.length; v++) {
                // The bass is kept light (and lighter still under 65 Hz, which small speakers lose and big ones boom on); the top
                // notes are a haze; each enters a little after the one below
                double gain = p.padGain() * (v == 0 ? 0.15 : 0.24 - 0.012 * v) * (chord[v] < 36 ? 0.6 : 1.0);
                double brightness = p.brightness() * (0.5 + 0.5 * v / (double) chord.length);
                pad(left, right, chord[v], gain, start + v * 0.35, end, lap, brightness, p.breathe(), c * 7 + v);
            }
        }
        // The pulse (the boss themes only): the chord's root, plucked, the first of every four a little harder
        for (int c = 0; c < chords.length && p.pulse() > 0; c++) {
            int root = chords[c][0] < 36 ? chords[c][0] + 12 : chords[c][0];
            for (int j = 0; j < p.pulse(); j++) {
                pluck(left, right, root + (j % 8 == 6 ? 7 : 0), j % 4 == 0 ? 0.30 : 0.17, (c + j / (double) p.pulse()) * p.chordSeconds());
            }
        }
        // The lead
        double beat = p.chordSeconds() / 8.0;
        for (double[] note : tune) {
            double at = note[0] * p.chordSeconds() + note[1] * beat;
            double held = note[3] * beat;
            double pan = 0.5 + 0.18 * Math.sin(note[0] * 1.7 + note[1]);
            switch (p.lead()) {
                case REED -> reed(left, right, note[2], p.leadGain(), at, held, pan);
                case BELL -> bell(left, right, note[2], p.leadGain(), at, pan);
                case VOICE -> voice(left, right, note[2], p.leadGain(), at, held, pan);
            }
        }
        // Wind chimes: a few high chord tones a chord, when and where a fixed seed says
        if (p.chimes() > 0) {
            Random random = new Random(p.name().hashCode());
            for (int c = 0; c < chords.length; c++) {
                if (c % p.chords().length == 0) { // the same chimes each time round, so a loop's end rings into its start
                    random = new Random(p.name().hashCode());
                }
                int[] chord = chords[c];
                for (int k = 0; k < p.chimes(); k++) {
                    int note = chord[chord.length - 1 - random.nextInt(Math.min(4, chord.length))] + 12 * (1 + random.nextInt(2));
                    double at = (c + random.nextDouble()) * p.chordSeconds();
                    bell(left, right, note, p.leadGain() * (0.35 + 0.4 * random.nextDouble()), at, random.nextDouble());
                }
            }
        }
        double[] wetLeft = reverb(left, 0);
        double[] wetRight = reverb(right, 23);
        double peak = 0;
        for (int i = 0; i < n; i++) {
            left[i] = left[i] * (1.0 - p.wet() * 0.5) + wetLeft[i] * p.wet();
            right[i] = right[i] * (1.0 - p.wet() * 0.5) + wetRight[i] * p.wet();
            peak = Math.max(peak, Math.max(Math.abs(left[i]), Math.abs(right[i])));
        }
        // A loop is the middle round of the three, as it stands. Anything else comes up from nothing and goes out over
        // its tail, so a piece never starts or stops: it arrives and leaves.
        int from = p.loop() ? (int) (span * RATE) : 0;
        int total = n;
        n = p.loop() ? (int) (span * RATE) : n;
        byte[] pcm = new byte[n * 4];
        for (int i = 0; i < n; i++) {
            double edge = p.loop() ? 1.0
                    : smooth(Math.min(1.0, i / (p.fadeIn() * RATE))) * smooth(Math.min(1.0, (total - i) / (tail * 0.8 * RATE)));
            double sl = left[from + i];
            double sr = right[from + i];
            int blend = RATE / 10;
            if (p.loop() && i < blend) {
                // The first tenth of a second eases over from what really follows the loop's end (the third round's start),
                // so the join is the same sound on both sides whatever the voices' vibrato was doing
                double a = smooth((double) i / blend);
                sl = sl * a + left[from + n + i] * (1 - a);
                sr = sr * a + right[from + n + i] * (1 - a);
            }
            int l = (int) Math.round(sl / peak * 0.85 * edge * 32767);
            int r = (int) Math.round(sr / peak * 0.85 * edge * 32767);
            pcm[i * 4] = (byte) l;
            pcm[i * 4 + 1] = (byte) (l >> 8);
            pcm[i * 4 + 2] = (byte) r;
            pcm[i * 4 + 3] = (byte) (r >> 8);
        }
        AudioFormat format = new AudioFormat(RATE, 16, 2, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), format, n), AudioFileFormat.Type.WAVE, file);
    }

    static double hz(double midi) {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }

    static double smooth(double x) {
        x = Math.max(0.0, Math.min(1.0, x));
        return x * x * (3 - 2 * x);
    }

    /**
     * One note of a chord: the swell's tone (three slightly detuned copies of a bowed, sung partial series), spread left,
     * middle and right, rising and falling over {@code lap} seconds. {@code breathe} is how deep a slow swell rides on it.
     */
    static void pad(double[] left, double[] right, double midi, double gain, double start, double end, double lap,
                    double brightness, double breathe, int seed) {
        double f = hz(midi);
        // Fewer partials on high notes: nothing above about 5 kHz, where it would only hiss
        int partials = (int) Math.max(2, Math.min(8, 5000.0 / f));
        double[] detune = {-0.0035, 0.0, 0.0031};
        double[] pans = {0.15, 0.5, 0.85};
        double[] phase = new double[detune.length * partials];
        int from = Math.max(0, (int) (start * RATE));
        int to = Math.min(left.length, (int) (end * RATE));
        for (int i = from; i < to; i++) {
            double t = (double) i / RATE;
            double env = smooth((t - start) / (lap * 2.0)) * smooth((end - t) / (lap * 2.0));
            if (breathe > 0) {
                env *= 1.0 - breathe * (0.5 + 0.5 * Math.sin(2 * Math.PI * 0.085 * t + seed * 0.9));
            }
            double vibrato = 1.0 + 0.002 * Math.sin(2 * Math.PI * 4.3 * t + seed);
            for (int c = 0; c < detune.length; c++) {
                double s = 0;
                for (int h = 1; h <= partials; h++) {
                    double amp = Math.pow(h, -1.5 + 0.6 * brightness * env) * (h == 3 || h == 4 ? 1.2 : 1.0);
                    int k = c * partials + h - 1;
                    phase[k] += 2 * Math.PI * f * h * (1 + detune[c]) * vibrato / RATE;
                    s += amp * Math.sin(phase[k]);
                }
                s *= gain * env / detune.length;
                left[i] += s * Math.cos(pans[c] * Math.PI / 2);
                right[i] += s * Math.sin(pans[c] * Math.PI / 2);
            }
        }
    }

    /** One beat of the pulse: a low plucked string, a handful of partials that die away in a third of a second. */
    static void pluck(double[] left, double[] right, double midi, double gain, double at) {
        double f = hz(midi);
        for (int i = Math.max(0, (int) (at * RATE)); i < Math.min(left.length, (int) ((at + 0.9) * RATE)); i++) {
            double t = (double) i / RATE - at;
            double s = 0;
            for (int h = 1; h <= 6; h++) {
                s += Math.pow(h, -1.2) * Math.exp(-t * (2.5 + h * 1.5)) * Math.sin(2 * Math.PI * f * h * t);
            }
            s *= gain * Math.min(1.0, t / 0.004);
            left[i] += s * 0.7;
            right[i] += s * 0.7;
        }
    }

    /** A soft reed (a low clarinet, or a harmonica heard from a long way off): odd partials, a slow start, a breath of vibrato. */
    static void reed(double[] left, double[] right, double midi, double gain, double at, double held, double pan) {
        double f = hz(midi);
        double end = at + held + 1.6;
        double[] phase = new double[6];
        for (int i = Math.max(0, (int) (at * RATE)); i < Math.min(left.length, (int) (end * RATE)); i++) {
            double t = (double) i / RATE - at;
            double env = smooth(t / 0.45) * (t < held ? 1.0 : smooth(1.0 - (t - held) / 1.6));
            // The vibrato comes in once the note has settled
            double vibrato = 1.0 + 0.004 * smooth((t - 0.6) / 1.2) * Math.sin(2 * Math.PI * 4.9 * t);
            double s = 0;
            for (int h = 0; h < phase.length; h++) {
                int partial = h * 2 + 1;
                phase[h] += 2 * Math.PI * f * partial * vibrato / RATE;
                s += Math.pow(partial, -1.7) * Math.sin(phase[h]);
                if (h < 2) { // a little of the even partials, so it isn't a hollow square
                    s += 0.12 * Math.pow(partial + 1, -1.7) * Math.sin(phase[h] * (partial + 1) / partial);
                }
            }
            s *= gain * 0.45 * env;
            left[i] += s * Math.cos(pan * Math.PI / 2);
            right[i] += s * Math.sin(pan * Math.PI / 2);
        }
    }

    /** A glass bell: a struck tone with partials a little off the harmonic series, ringing down over six seconds. */
    static void bell(double[] left, double[] right, double midi, double gain, double at, double pan) {
        double f = hz(midi);
        double[] ratio = {1.0, 2.01, 2.76, 4.07, 5.4};
        double[] amp = {1.0, 0.45, 0.22, 0.12, 0.05};
        double[] decay = {1.7, 1.2, 0.8, 0.5, 0.3};
        for (int i = Math.max(0, (int) (at * RATE)); i < Math.min(left.length, (int) ((at + 6.0) * RATE)); i++) {
            double t = (double) i / RATE - at;
            double s = 0;
            for (int h = 0; h < ratio.length; h++) {
                if (f * ratio[h] < 9000) {
                    s += amp[h] * Math.exp(-t / decay[h]) * Math.sin(2 * Math.PI * f * ratio[h] * t);
                }
            }
            // A soft strike (5 ms), not a click
            s *= gain * 0.40 * Math.min(1.0, t / 0.005) * smooth((6.0 - t) / 1.0);
            left[i] += s * Math.cos(pan * Math.PI / 2);
            right[i] += s * Math.sin(pan * Math.PI / 2);
        }
    }

    /** One wordless voice ("ah"): a partial series shaped by two vowel formants, a slow start, and a singer's vibrato. */
    static void voice(double[] left, double[] right, double midi, double gain, double at, double held, double pan) {
        double f = hz(midi);
        int partials = (int) Math.max(3, Math.min(12, 4500.0 / f));
        double end = at + held + 2.4;
        double[] phase = new double[partials];
        for (int i = Math.max(0, (int) (at * RATE)); i < Math.min(left.length, (int) (end * RATE)); i++) {
            double t = (double) i / RATE - at;
            double env = smooth(t / 0.9) * (t < held ? 1.0 : smooth(1.0 - (t - held) / 2.4));
            double vibrato = 1.0 + 0.005 * smooth((t - 0.5) / 1.5) * Math.sin(2 * Math.PI * 5.1 * t);
            double s = 0;
            for (int h = 1; h <= partials; h++) {
                double hzNow = f * h;
                // "Ah": formants near 750 and 1150 Hz over a falling series
                double formant = 1.0 + 2.2 * Math.exp(-Math.pow((hzNow - 750) / 220, 2)) + 1.4 * Math.exp(-Math.pow((hzNow - 1150) / 260, 2));
                phase[h - 1] += 2 * Math.PI * hzNow * vibrato / RATE;
                s += Math.pow(h, -1.6) * formant * Math.sin(phase[h - 1]);
            }
            s *= gain * 0.33 * env;
            left[i] += s * Math.cos(pan * Math.PI / 2);
            right[i] += s * Math.sin(pan * Math.PI / 2);
        }
    }

    /** The swell's hall (four combs into two allpasses), a little longer; {@code spread} offsets one ear's from the other's. */
    static double[] reverb(double[] dry, int spread) {
        int[] combs = {1557 + spread, 1617 + spread, 1491 + spread, 1422 + spread};
        double[] wet = new double[dry.length];
        for (int len : combs) {
            double[] buf = new double[len * 3];
            int at = 0;
            double damp = 0;
            for (int i = 0; i < dry.length; i++) {
                double out = buf[at];
                damp = out * 0.55 + damp * 0.45;
                buf[at] = dry[i] + damp * 0.89;
                at = (at + 1) % buf.length;
                wet[i] += out * 0.25;
            }
        }
        for (int len : new int[] {556 + spread, 441 + spread}) {
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
