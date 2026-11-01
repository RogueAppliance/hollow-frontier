package com.hollowfrontier.content.frontier.saloon;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Saloon nights (21b): the per-world synced config, as the raids' and the Grays' are. Before the config loads, the
 * defaults apply.
 */
public final class SaloonConfig {
    private SaloonConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue MISSING_TIME;
    private static final ModConfigSpec.DoubleValue MISSING_TIME_CHANCE;
    private static final ModConfigSpec.BooleanValue BRAWLS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("A night at the saloon").push("saloon");
        MISSING_TIME = b.comment("Whether a player whose Tipsy III wears off in a town may wake up somewhere else in it (never outside a town, never anywhere unsafe)")
                .define("missingTime", true);
        MISSING_TIME_CHANCE = b.comment("Chance it happens, each time Tipsy III or worse wears off in a town").defineInRange("missingTimeChance", 0.15, 0.0, 1.0);
        BRAWLS = b.comment("Whether tipsy townsfolk in a saloon now and then come to blows (fists only; nobody dies)").define("brawls", true);
        b.pop();
        SPEC = b.build();
    }

    public static boolean missingTime() { return SPEC.isLoaded() ? MISSING_TIME.getAsBoolean() : MISSING_TIME.getDefault(); }
    public static double missingTimeChance() { return SPEC.isLoaded() ? MISSING_TIME_CHANCE.getAsDouble() : MISSING_TIME_CHANCE.getDefault(); }
    public static boolean brawls() { return SPEC.isLoaded() ? BRAWLS.getAsBoolean() : BRAWLS.getDefault(); }
}
