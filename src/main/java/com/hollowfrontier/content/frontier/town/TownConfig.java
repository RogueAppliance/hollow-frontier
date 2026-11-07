package com.hollowfrontier.content.frontier.town;

import net.neoforged.neoforge.common.ModConfigSpec;

/** How towns replace their dead, and whether new worlds start in one: the per-world synced config (hollowfrontier-towns-server.toml). Times are in ticks. */
public final class TownConfig {
    private TownConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ARRIVALS;
    private static final ModConfigSpec.IntValue ARRIVAL_TIME;
    private static final ModConfigSpec.BooleanValue SPAWN_AT_TOWN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Frontier towns").push("towns");
        ARRIVALS = b.comment("Whether towns slowly replace the townsfolk and lawmen they lose").define("arrivals", true);
        ARRIVAL_TIME = b.comment("Time from a death to the first arrival, and from one arrival to the next in that town (24000 is a Minecraft day)")
                .defineInRange("arrivalTime", 24000, 100, Integer.MAX_VALUE);
        SPAWN_AT_TOWN = b.comment("Whether a new world starts in the nearest town's square. A world chooses its spawn once, when it's created, so set this in the config folder beforehand")
                .define("spawnAtTown", true);
        b.pop();
        SPEC = b.build();
    }

    public static boolean arrivals() { return SPEC.isLoaded() ? ARRIVALS.getAsBoolean() : ARRIVALS.getDefault(); }
    public static int arrivalTime() { return SPEC.isLoaded() ? ARRIVAL_TIME.getAsInt() : ARRIVAL_TIME.getDefault(); }
    public static boolean spawnAtTown() { return SPEC.isLoaded() ? SPAWN_AT_TOWN.getAsBoolean() : SPAWN_AT_TOWN.getDefault(); }
}
