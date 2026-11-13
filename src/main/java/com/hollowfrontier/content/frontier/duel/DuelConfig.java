package com.hollowfrontier.content.frontier.duel;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * High noon (51), per world ({@code hollowfrontier-duel-server.toml}): whether bosses call anyone out, how often, how far
 * from town a player can be and still be asked, and whether losing kills. Before the config loads the defaults apply.
 */
public final class DuelConfig {
    private DuelConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue DUELS;
    private static final ModConfigSpec.DoubleValue CALL_OUT_CHANCE;
    private static final ModConfigSpec.IntValue DUEL_REACH;
    private static final ModConfigSpec.BooleanValue LETHAL_DUELS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("High noon: an Outlaw Boss calling a player out to the street").push("duel");
        DUELS = b.comment("Whether a Wanted Poster's boss ever calls its holder out").define("duels", true);
        CALL_OUT_CHANCE = b.comment("The share of posters whose boss means to meet their holder in the street").defineInRange("callOutChance", 0.33, 0.0, 1.0);
        DUEL_REACH = b.comment("How far from the town's street a player may be near noon and still be asked, in blocks").defineInRange("duelReach", 200, 16, 512);
        LETHAL_DUELS = b.comment("Whether the boss's shot kills (off: the loser is left at one heart, short a purse)").define("lethalDuels", false);
        b.pop();
        SPEC = b.build();
    }

    public static boolean duels() { return SPEC.isLoaded() ? DUELS.getAsBoolean() : DUELS.getDefault(); }
    public static double callOutChance() { return SPEC.isLoaded() ? CALL_OUT_CHANCE.getAsDouble() : CALL_OUT_CHANCE.getDefault(); }
    public static int duelReach() { return SPEC.isLoaded() ? DUEL_REACH.getAsInt() : DUEL_REACH.getDefault(); }
    public static boolean lethalDuels() { return SPEC.isLoaded() ? LETHAL_DUELS.getAsBoolean() : LETHAL_DUELS.getDefault(); }
}
