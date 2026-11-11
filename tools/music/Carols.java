import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Writes the saloon piano's three December carols (35c) as MIDI scores for tools/music/PianoRoll.java to render:
 * "Jingle Bells" (James Lord Pierpont, published 1857 as "One Horse Open Sleigh"), "God Rest Ye Merry, Gentlemen"
 * (traditional English, in print by the 1830s) and "O Come, All Ye Faithful" ("Adeste Fideles", John Francis Wade,
 * about 1743). The tunes are public domain and are typed in here note by note as they have been sung since the nineteenth
 * century; the arrangements (a saloon pianist's left hand under the tune, the tune doubled at the octave the second time
 * through) are ours, written here, and copy no modern arrangement.
 * Run from the project root:  java tools/music/Carols.java tools/music/scores
 *
 * A tune is bars of 4/4 separated by "|": each note is NAME[:beats] (C4 is middle C, "#" sharpens, "r" rests, one beat
 * if no length is given). The harmony is one chord a bar, or two as "G/D7" (half a bar each).
 */
public class Carols {
    static final int PPQ = 480;

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "tools/music/scores");
        // Jingle Bells, in G: verse, chorus, and both again with the tune doubled an octave up
        String jingleVerse = "D4 B4 A4 G4 | D4:3 D4:.5 D4:.5 | D4 B4 A4 G4 | E4:4 | E4 C5 B4 A4 | F#4:4 | D5 D5 C5 A4 | B4:4 |"
                + " D4 B4 A4 G4 | D4:4 | D4 B4 A4 G4 | E4:3 E4 | E4 C5 B4 A4 | D5 D5 D5 D5 | E5 D5 C5 A4 | G4:2 D5:2";
        String jingleVerseChords = "G G G C C D D7 G G G G C C G D7 G";
        String jingleChorus = "B4 B4 B4:2 | B4 B4 B4:2 | B4 D5 G4:1.5 A4:.5 | B4:4 | C5 C5 C5:1.5 C5:.5 | C5 B4 B4 B4:.5 B4:.5 | B4 A4 A4 B4 | A4:2 D5:2 |"
                + " B4 B4 B4:2 | B4 B4 B4:2 | B4 D5 G4:1.5 A4:.5 | B4:4 | C5 C5 C5:1.5 C5:.5 | C5 B4 B4 B4:.5 B4:.5 | D5 D5 C5 A4 | G4:4";
        String jingleChorusChords = "G G G G C G A7 D7 G G G G C G D7 G";
        Tune jingle = new Tune(196, true);
        jingle.play(jingleVerse, jingleVerseChords, false);
        jingle.play(jingleChorus, jingleChorusChords, false);
        jingle.play(jingleVerse, jingleVerseChords, true);
        jingle.play(jingleChorus, jingleChorusChords, true);
        jingle.end("G");
        jingle.write(new File(dir, "jingle_bells.mid"));

        // God Rest Ye Merry, Gentlemen, in E minor, twice through
        String rest = "r:3 E4 | E4 B4 B4 A4 | G4 F#4 E4 D4 | E4 F#4 G4 A4 | B4:3 E4 | E4 B4 B4 A4 | G4 F#4 E4 D4 | E4 F#4 G4 A4 | B4:3 B4 |"
                + " C5 A4 B4 C5 | D5 E5 B4 A4 | G4 E4 F#4 G4 | A4:2 G4 A4 | B4:2 C5 B4 | B4 A4 G4 F#4 | E4:2 G4:.5 F#4:.5 E4 | A4:2 G4 A4 |"
                + " B4 C5 D5 E5 | B4 A4 G4 F#4 | E4:4";
        String restChords = "- Em Em Am B7 Em Em Am B7 Am G Em D G Em/B7 Em D G Em/B7 Em";
        Tune gentlemen = new Tune(126, false);
        gentlemen.play(rest, restChords, false);
        gentlemen.play(rest, restChords, true);
        gentlemen.end("Em");
        gentlemen.write(new File(dir, "god_rest_ye_merry_gentlemen.mid"));

        // O Come, All Ye Faithful, in G, twice through
        String come = "r:3 G4 | G4:2 D4 G4 | A4:2 D4:2 | B4 A4 B4 C5 | B4:2 A4 G4 | G4:2 F#4 E4 | F#4 G4 A4 B4 | F#4:2 E4:1.5 D4:.5 | D4:4 |"
                + " D5:2 C5 B4 | C5:2 B4:2 | A4 B4 G4 A4 | F#4:1.5 E4:.5 D4 G4 | G4 F#4 G4 A4 | G4:2 D4 B4 | B4 A4 B4 C5 | B4:2 A4 B4 |"
                + " C5 B4 A4 G4 | F#4:2 G4 C5 | B4:2 A4:1.5 G4:.5 | G4:4";
        String comeChords = "- G D G/D G/D Em D/G D/A7 D G C/G D/Em D G G G/D G/D C/G D/C G/D7 G";
        Tune faithful = new Tune(108, false);
        faithful.play(come, comeChords, false);
        faithful.play(come, comeChords, true);
        faithful.end("G");
        faithful.write(new File(dir, "o_come_all_ye_faithful.mid"));
    }

    /** One carol being written: the right hand's tune and the left hand under it, bar by bar. */
    static final class Tune {
        final Sequence sequence;
        final Track right, left;
        final boolean stride; // a saloon left hand (bass, chord, bass, chord) rather than a hymn's held chords
        long tick; // where the next bar starts
        int bars;

        Tune(int beatsPerMinute, boolean stride) throws Exception {
            sequence = new Sequence(Sequence.PPQ, PPQ);
            right = sequence.createTrack();
            left = sequence.createTrack();
            this.stride = stride;
            int micros = 60_000_000 / beatsPerMinute;
            right.add(new MidiEvent(new MetaMessage(0x51, new byte[] {(byte) (micros >> 16), (byte) (micros >> 8), (byte) micros}, 3), 0));
        }

        void play(String tune, String chords, boolean octave) throws Exception {
            String[] measures = tune.split("\\|");
            String[] harmony = chords.trim().split("\\s+");
            if (measures.length != harmony.length) {
                throw new IllegalArgumentException(measures.length + " bars of tune, " + harmony.length + " of harmony");
            }
            for (int m = 0; m < measures.length; m++) {
                double beat = 0;
                for (String token : measures[m].trim().split("\\s+")) {
                    String[] parts = token.split(":");
                    double beats = parts.length > 1 ? Double.parseDouble(parts[1]) : 1.0;
                    if (!parts[0].equals("r")) {
                        int key = key(parts[0]);
                        // Strong beats a little stronger, as a player leans on them
                        int velocity = (beat == 0 ? 98 : beat == 2 ? 92 : 86) + (octave ? 4 : 0);
                        note(right, key, velocity, beat, beats * 0.94);
                        if (octave) {
                            note(right, key + 12, velocity - 10, beat, beats * 0.94);
                        }
                    }
                    beat += beats;
                }
                if (Math.abs(beat - 4) > 1e-6) {
                    throw new IllegalArgumentException("bar " + (m + 1) + " of \"" + measures[m].trim() + "\" is " + beat + " beats");
                }
                if (!harmony[m].equals("-")) {
                    String[] halves = harmony[m].split("/");
                    if (halves.length == 2) {
                        accompany(halves[0], 0, 2);
                        accompany(halves[1], 2, 2);
                    } else {
                        accompany(halves[0], 0, 4);
                    }
                }
                tick += 4 * PPQ;
                bars++;
            }
        }

        // The left hand over one chord: stride is bass, chord, fifth, chord; a hymn holds bass and chord, struck again at the half bar
        void accompany(String chord, double from, double beats) throws Exception {
            int[] tones = chord(chord);
            int root = 36 + tones[0], fifth = 36 + tones[2] - (tones[2] >= 9 ? 12 : 0);
            if (stride) {
                for (double b = 0; b < beats; b += 2) {
                    note(left, b % 4 == 0 ? root : fifth + (fifth < root ? 12 : 0), 76, from + b, 0.9);
                    for (int tone : tones) {
                        note(left, 55 + ((tone + 5) % 12), 58, from + b + 1, 0.55);
                    }
                }
            } else {
                for (double b = 0; b < beats; b += 2) {
                    note(left, root, b == 0 ? 74 : 62, from + b, 1.9);
                    note(left, root + 12, b == 0 ? 66 : 56, from + b, 1.9);
                    for (int tone : tones) {
                        note(left, 52 + ((tone + 8) % 12), 54, from + b, 1.9);
                    }
                }
            }
        }

        /** A last chord, rolled and left to ring. */
        void end(String chord) throws Exception {
            int[] tones = chord(chord);
            note(left, 36 + tones[0], 80, 0, 4);
            note(left, 48 + tones[0], 72, 0, 4);
            double roll = 0;
            for (int tone : tones) {
                note(right, 60 + tone, 84, roll, 4 - roll);
                roll += 0.08;
            }
            note(right, 72 + tones[0], 88, roll, 4 - roll);
            tick += 4 * PPQ;
        }

        void note(Track track, int key, int velocity, double beat, double beats) throws Exception {
            long on = tick + Math.round(beat * PPQ);
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, key, velocity), on));
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, key, 0), on + Math.max(1, Math.round(beats * PPQ))));
        }

        void write(File file) throws Exception {
            file.getParentFile().mkdirs();
            MidiSystem.write(sequence, 1, file);
            System.out.println("Carols: " + bars + " bars -> " + file);
        }
    }

    /** The MIDI key of a note name: C4 is 60. */
    static int key(String name) {
        int at = 1;
        int semitone = "C D EF G A B".indexOf(name.charAt(0));
        if (name.charAt(at) == '#') {
            semitone++;
            at++;
        }
        return 12 * (Integer.parseInt(name.substring(at)) + 1) + semitone;
    }

    /** A chord's tones as semitones above C: major, minor (m) or a seventh (7), root first. */
    static int[] chord(String name) {
        int at = 1;
        int root = "C D EF G A B".indexOf(name.charAt(0));
        if (name.length() > at && name.charAt(at) == '#') {
            root++;
            at++;
        }
        String kind = name.substring(at);
        List<Integer> tones = new ArrayList<>(List.of(root, root + (kind.startsWith("m") ? 3 : 4), root + 7));
        if (kind.endsWith("7")) {
            tones.add(root + 10);
        }
        return tones.stream().mapToInt(t -> t % 12).toArray();
    }
}
