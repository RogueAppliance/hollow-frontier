package com.hollowfrontier.content.frontier.saloon;

import java.util.Optional;
import java.util.Random;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * How the world reads to someone at Tipsy II and up (21b): here and there in a word of four letters or more, a letter
 * comes out twice or two letters swap places. It works on the text as translated, so it needs no lang of its own and
 * slurs every language alike; the client applies it to what NPCs say and to its own Gazette. The same text slurs the
 * same way each time (the seed is the text), so a page doesn't shimmer as it's redrawn.
 */
public final class Slur {
    private Slur() {}

    /** The lowest Tipsy level that slurs. */
    public static final int FROM_LEVEL = 2;
    /** Chance a long enough word is touched, per Tipsy level from {@link #FROM_LEVEL}. */
    public static final float PER_LEVEL = 0.15F;

    /** {@code text} as a drinker at Tipsy {@code level} reads it. */
    public static String slur(String text, int level) {
        if (level < FROM_LEVEL || text.isEmpty()) {
            return text;
        }
        float chance = (level - FROM_LEVEL + 1) * PER_LEVEL;
        Random random = new Random(text.hashCode() * 31L + level);
        StringBuilder out = new StringBuilder(text.length() + 8);
        int start = -1;
        for (int i = 0; i <= text.length(); i++) {
            boolean letter = i < text.length() && Character.isLetter(text.charAt(i));
            if (letter && start < 0) {
                start = i;
            } else if (!letter && start >= 0) {
                out.append(word(text.substring(start, i), chance, random));
                start = -1;
            }
            if (!letter && i < text.length()) {
                out.append(text.charAt(i));
            }
        }
        return out.toString();
    }

    private static String word(String word, float chance, Random random) {
        if (word.length() < 4 || random.nextFloat() >= chance) {
            return word;
        }
        int at = 1 + random.nextInt(word.length() - 2);
        if (random.nextBoolean()) {
            return word.substring(0, at + 1) + word.charAt(at) + word.substring(at + 1);
        }
        char[] chars = word.toCharArray();
        char c = chars[at];
        chars[at] = chars[at + 1];
        chars[at + 1] = c;
        return new String(chars);
    }

    /** A component slurred piece by piece, each piece keeping its style. */
    public static Component slur(Component text, int level) {
        if (level < FROM_LEVEL) {
            return text;
        }
        MutableComponent out = Component.empty();
        text.visit((Style style, String piece) -> {
            out.append(Component.literal(slur(piece, level)).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
