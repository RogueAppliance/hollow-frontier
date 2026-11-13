package com.hollowfrontier.content.frontier.stake;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * A stake in town (47), per world ({@code hollowfrontier-stake-server.toml}): the three parts can each be switched off,
 * and the distances and the deed's price set. Before the config loads the defaults apply.
 */
public final class StakeConfig {
    private StakeConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue STANDING;
    private static final ModConfigSpec.BooleanValue CLAIMS;
    private static final ModConfigSpec.BooleanValue DRIVES;
    private static final ModConfigSpec.IntValue CLAIM_REACH;
    private static final ModConfigSpec.IntValue DRIVE_REACH;
    private static final ModConfigSpec.IntValue WARRANT_REACH;
    private static final ModConfigSpec.IntValue DEED_PRICE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("A stake in town: standing, the Old Place and cattle drives").push("stake");
        STANDING = b.comment("Whether towns remember what a player has done for them (off: nobody earns standing, a badge or a deed)").define("standing", true);
        CLAIMS = b.comment("Whether bankers sell deeds").define("claims", true);
        DRIVES = b.comment("Whether wranglers post cattle drives and buy branded cattle").define("drives", true);
        CLAIM_REACH = b.comment("How far outside a town's bounds a deed can have a place raised, in blocks").defineInRange("claimReach", 96, 16, 512);
        DRIVE_REACH = b.comment("How far off another town may be for a herd to be driven to it, in blocks (farther, and the herd goes to a buyer on the trail)").defineInRange("driveReach", 3000, 500, 20000);
        WARRANT_REACH = b.comment("How far off another town may be for a warrant to be carried to it, in blocks (towns stand some 3000 to 6000 apart)").defineInRange("warrantReach", 6000, 500, 20000);
        DEED_PRICE = b.comment("What a deed costs, in gold coins").defineInRange("deedPrice", 24, 1, 64);
        b.pop();
        SPEC = b.build();
    }

    public static boolean standing() { return SPEC.isLoaded() ? STANDING.getAsBoolean() : STANDING.getDefault(); }
    public static boolean claims() { return standing() && (SPEC.isLoaded() ? CLAIMS.getAsBoolean() : CLAIMS.getDefault()); }
    public static boolean drives() { return SPEC.isLoaded() ? DRIVES.getAsBoolean() : DRIVES.getDefault(); }
    public static int claimReach() { return SPEC.isLoaded() ? CLAIM_REACH.getAsInt() : CLAIM_REACH.getDefault(); }
    public static int driveReach() { return SPEC.isLoaded() ? DRIVE_REACH.getAsInt() : DRIVE_REACH.getDefault(); }
    public static int warrantReach() { return SPEC.isLoaded() ? WARRANT_REACH.getAsInt() : WARRANT_REACH.getDefault(); }
    public static int deedPrice() { return SPEC.isLoaded() ? DEED_PRICE.getAsInt() : DEED_PRICE.getDefault(); }
}
