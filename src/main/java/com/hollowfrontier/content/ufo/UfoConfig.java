package com.hollowfrontier.content.ufo;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Grays' flyovers and abductions (13a), and the landings that take their place once the leash is broken (15a): the
 * per-world synced config (ModConfig.Type.SYNCED, in the world's serverconfig folder), as the raids' is. Before the config
 * loads, the defaults apply. Times are in ticks.
 */
public final class UfoConfig {
    private UfoConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue FLYOVERS;
    private static final ModConfigSpec.IntValue ROLL_INTERVAL;
    private static final ModConfigSpec.DoubleValue CHANCE;
    private static final ModConfigSpec.DoubleValue ABDUCTION_CHANCE;
    private static final ModConfigSpec.BooleanValue ABDUCT_PLAYERS;
    private static final ModConfigSpec.BooleanValue TAKEN;
    private static final ModConfigSpec.IntValue TAKEN_SUSPICION;
    private static final ModConfigSpec.DoubleValue TAKEN_AGAIN;
    private static final ModConfigSpec.BooleanValue LANDINGS;
    private static final ModConfigSpec.IntValue LANDING_INTERVAL;
    private static final ModConfigSpec.DoubleValue LANDING_CHANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("The Grays' saucers over the Overworld at night").push("flyovers");
        FLYOVERS = b.comment("Whether saucers fly over on their own (the /hollowfrontier ufo command works either way)").define("enabled", true);
        ROLL_INTERVAL = b.comment("How often, at night, a saucer may come over one of the players").defineInRange("rollInterval", 1200, 20, 24000);
        CHANCE = b.comment("Chance per roll").defineInRange("chance", 0.05, 0.0, 1.0);
        ABDUCTION_CHANCE = b.comment("Chance a saucer that comes over stops to beam something up").defineInRange("abductionChance", 0.4, 0.0, 1.0);
        ABDUCT_PLAYERS = b.comment("Whether a saucer's beam takes players who aren't sneaking (animals either way)").define("abductPlayers", true);
        b.pop();
        b.comment("Taken: a player the beam draws up may be kept, and wake aboard the ship with nothing on them (their things are always given back)").push("taken");
        TAKEN = b.comment("Whether the Grays ever keep a player (off: every abduction is missing time, as before; abductPlayers off turns this off too)").define("enabled", true);
        TAKEN_SUSPICION = b.comment("How much the men in black must mind a player before the Grays keep them the first time (they warn at 25, hunt at 60)").defineInRange("suspicion", 30, 0, 100);
        TAKEN_AGAIN = b.comment("Chance that a later abduction of the same player keeps them again").defineInRange("again", 0.25, 0.0, 1.0);
        b.pop();
        b.comment("Gray Landings, once the leash is broken: a saucer set down near a player for the night, in the Overworld or the Hollow Earth").push("landings");
        LANDINGS = b.comment("Whether saucers land on their own (the /hollowfrontier ufo landing command works either way)").define("enabled", true);
        LANDING_INTERVAL = b.comment("Fewest nights from one landing to the next, in each world").defineInRange("interval", 2, 1, 30);
        LANDING_CHANCE = b.comment("Chance each night, once the interval has passed, that one comes down").defineInRange("chance", 0.5, 0.0, 1.0);
        b.pop();
        SPEC = b.build();
    }

    public static boolean flyovers() { return SPEC.isLoaded() ? FLYOVERS.getAsBoolean() : FLYOVERS.getDefault(); }
    public static int rollInterval() { return SPEC.isLoaded() ? ROLL_INTERVAL.getAsInt() : ROLL_INTERVAL.getDefault(); }
    public static double chance() { return SPEC.isLoaded() ? CHANCE.getAsDouble() : CHANCE.getDefault(); }
    public static double abductionChance() { return SPEC.isLoaded() ? ABDUCTION_CHANCE.getAsDouble() : ABDUCTION_CHANCE.getDefault(); }
    public static boolean abductPlayers() { return SPEC.isLoaded() ? ABDUCT_PLAYERS.getAsBoolean() : ABDUCT_PLAYERS.getDefault(); }
    public static boolean taken() { return SPEC.isLoaded() ? TAKEN.getAsBoolean() : TAKEN.getDefault(); }
    public static int takenSuspicion() { return SPEC.isLoaded() ? TAKEN_SUSPICION.getAsInt() : TAKEN_SUSPICION.getDefault(); }
    public static double takenAgain() { return SPEC.isLoaded() ? TAKEN_AGAIN.getAsDouble() : TAKEN_AGAIN.getDefault(); }
    public static boolean landings() { return SPEC.isLoaded() ? LANDINGS.getAsBoolean() : LANDINGS.getDefault(); }
    public static int landingInterval() { return SPEC.isLoaded() ? LANDING_INTERVAL.getAsInt() : LANDING_INTERVAL.getDefault(); }
    public static double landingChance() { return SPEC.isLoaded() ? LANDING_CHANCE.getAsDouble() : LANDING_CHANCE.getDefault(); }
}
