package com.hollowfrontier.content.frontier.weather;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Dust devils and Ash Devils (29): the per-world synced config (ModConfig.Type.SYNCED, in the world's serverconfig
 * folder), as the Grays' is. Before the config loads, the defaults apply. Times are in ticks.
 */
public final class DustDevilConfig {
    private DustDevilConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.IntValue ROLL_INTERVAL;
    private static final ModConfigSpec.DoubleValue CHANCE;
    private static final ModConfigSpec.BooleanValue LIFT_PLAYERS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Dust devils over the deserts, badlands and savannas on clear days, and Ash Devils in the Ember Badlands").push("dust_devils");
        ENABLED = b.comment("Whether they spin up on their own (the /hollowfrontier dustdevil command works either way)").define("enabled", true);
        ROLL_INTERVAL = b.comment("How often one may spin up near each player in their country").defineInRange("rollInterval", 600, 20, 24000);
        CHANCE = b.comment("Chance per roll").defineInRange("chance", 0.1, 0.0, 1.0);
        LIFT_PLAYERS = b.comment("Whether they lift players who aren't sneaking (a few blocks, and set them down gently); if not, they only push")
                .define("liftPlayers", true);
        b.pop();
        SPEC = b.build();
    }

    public static boolean enabled() { return SPEC.isLoaded() ? ENABLED.getAsBoolean() : ENABLED.getDefault(); }
    public static int rollInterval() { return SPEC.isLoaded() ? ROLL_INTERVAL.getAsInt() : ROLL_INTERVAL.getDefault(); }
    public static double chance() { return SPEC.isLoaded() ? CHANCE.getAsDouble() : CHANCE.getDefault(); }
    public static boolean liftPlayers() { return SPEC.isLoaded() ? LIFT_PLAYERS.getAsBoolean() : LIFT_PLAYERS.getDefault(); }
}
