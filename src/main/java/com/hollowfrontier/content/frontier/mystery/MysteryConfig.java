package com.hollowfrontier.content.frontier.mystery;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The frontier's mysteries (11a) and the men in black (11b), switched on and off (30): the per-world synced config
 * (ModConfig.Type.SYNCED, in the world's serverconfig folder), as the raids' is. Before the config loads, the defaults apply.
 * The {@code /hollowfrontier mystery} commands work whatever it says.
 */
public final class MysteryConfig {
    private MysteryConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.DoubleValue CHANCE;
    private static final ModConfigSpec.DoubleValue OVERNIGHT_CHANCE;
    private static final ModConfigSpec.BooleanValue MUTILATIONS;
    private static final ModConfigSpec.BooleanValue CROP_CIRCLES;
    private static final ModConfigSpec.BooleanValue VISITS;
    private static final ModConfigSpec.BooleanValue HUNTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Airship sightings, cattle mutilations and crop circles near players at night in the Overworld").push("mysteries");
        ENABLED = b.comment("Whether they happen on their own at all").define("enabled", true);
        CHANCE = b.comment("Chance per roll (one roll a minute at night, at most one mystery a night)").defineInRange("chance", (double) Mysteries.ROLL_CHANCE, 0.0, 1.0);
        OVERNIGHT_CHANCE = b.comment("Chance a night slept through turns up a cut steer or a crop circle by morning").defineInRange("overnightChance", (double) Mysteries.OVERNIGHT_CHANCE, 0.0, 1.0);
        MUTILATIONS = b.comment("Whether cattle are found cut (the steer dies; off, a sighting happens instead)").define("cattleMutilations", true);
        CROP_CIRCLES = b.comment("Whether ripe wheat is flattened into circles (off, a sighting happens instead)").define("cropCircles", true);
        b.pop();
        b.comment("The men in black dusters").push("menInBlack");
        VISITS = b.comment("Whether they turn up half a day after a mystery to lean on the town").define("visits", true);
        HUNTS = b.comment("Whether they hunt a player who has made them hostile, at night").define("hunts", true);
        b.pop();
        SPEC = b.build();
    }

    public static boolean enabled() { return SPEC.isLoaded() ? ENABLED.getAsBoolean() : ENABLED.getDefault(); }
    public static double chance() { return SPEC.isLoaded() ? CHANCE.getAsDouble() : CHANCE.getDefault(); }
    public static double overnightChance() { return SPEC.isLoaded() ? OVERNIGHT_CHANCE.getAsDouble() : OVERNIGHT_CHANCE.getDefault(); }
    public static boolean mutilations() { return SPEC.isLoaded() ? MUTILATIONS.getAsBoolean() : MUTILATIONS.getDefault(); }
    public static boolean cropCircles() { return SPEC.isLoaded() ? CROP_CIRCLES.getAsBoolean() : CROP_CIRCLES.getDefault(); }
    public static boolean visits() { return SPEC.isLoaded() ? VISITS.getAsBoolean() : VISITS.getDefault(); }
    public static boolean hunts() { return SPEC.isLoaded() ? HUNTS.getAsBoolean() : HUNTS.getDefault(); }

    /** Whether this kind of mystery may happen on its own (sightings always may, while mysteries are on). */
    public static boolean allows(MysteryLog.Kind kind) {
        return switch (kind) {
            case SIGHTING, LANDING -> true; // landings answer to the UFO config
            case MUTILATION -> mutilations();
            case CROP_CIRCLE -> cropCircles();
        };
    }
}
