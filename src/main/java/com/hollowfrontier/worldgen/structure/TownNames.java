package com.hollowfrontier.worldgen.structure;

import java.util.Locale;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Every town's own name (24a), "Dry Gulch" or "Coyote Springs": two parts drawn from the world seed and the town's key
 * (its structure start chunk, the key TownSite saves it under), so a town always has the same name, on its welcome
 * arches (TownNameProcessor writes it there as the town is built), in the Gazette, on letters and bank notices. Its
 * population, on the arch, comes the same way. The words are invented frontier place-words, no real town's name whole.
 */
public final class TownNames {
    private TownNames() {}

    static final String[] FIRST = {"Dry", "Dusty", "Red", "Copper", "Silver", "Lonesome", "Rattlesnake", "Buzzard", "Coyote", "Broken",
            "Twin", "Black", "Bitter", "Cottonwood", "Mesquite", "Juniper", "Sagebrush", "Whiskey", "Powder", "Lucky", "Hangman's",
            "Dead Horse", "Sundown", "Tin Cup", "Painted", "Bluebonnet", "Iron", "Saddle", "Gold", "Antelope", "Thunder", "Blind Mule"};
    static final String[] SECOND = {"Gulch", "Creek", "Springs", "Flats", "Bend", "Ridge", "Gap", "Wells", "Butte", "Junction", "Crossing",
            "Canyon", "Mesa", "Fork", "Station", "Bluff", "Draw", "Wash", "Rock", "Hollow", "Pass", "Hill"};

    private static RandomSource random(long seed, long key) {
        return RandomSource.create(seed ^ key * 0x9E3779B97F4A7C15L ^ 0x5DEECE66DL);
    }

    /** The town's name, title case ("Dry Gulch"). */
    public static String name(long seed, long key) {
        RandomSource random = random(seed, key);
        return FIRST[random.nextInt(FIRST.length)] + " " + SECOND[random.nextInt(SECOND.length)];
    }

    /** The name in the town's own world (its overworld seed). */
    public static String name(ServerLevel level, long key) {
        return name(level.getServer().overworld().getSeed(), key);
    }

    /** The name painted on a sign. */
    public static String signName(long seed, long key) {
        return name(seed, key).toUpperCase(Locale.ROOT);
    }

    /** "the town of Dry Gulch" in a sentence, or "the town" for a name not known (saved before towns had names). */
    public static net.minecraft.network.chat.Component phrase(String name) {
        return name.isBlank() ? net.minecraft.network.chat.Component.translatable("text.hollowfrontier.the_town")
                : net.minecraft.network.chat.Component.translatable("text.hollowfrontier.town_of", name);
    }

    /** The population on the arch: 40 to 339. */
    public static int population(long seed, long key) {
        RandomSource random = random(seed, key);
        random.nextInt(FIRST.length);
        random.nextInt(SECOND.length);
        return 40 + random.nextInt(300);
    }
}
