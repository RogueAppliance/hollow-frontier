package com.hollowfrontier.content.frontier.raid;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every raid number in one place: the per-world synced config (ModConfig.Type.SYNCED, in the world's serverconfig folder). Before the
 * config loads (or off the server) the defaults apply. Times are in ticks, money in copper coins (a gold coin is 9).
 */
public final class RaidConfig {
    private RaidConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.IntValue CHECK_INTERVAL;
    private static final ModConfigSpec.DoubleValue CHANCE;
    private static final ModConfigSpec.IntValue COOLDOWN;
    private static final ModConfigSpec.IntValue FIRST_RAID_GRACE;
    private static final ModConfigSpec.IntValue MIN_GANG;
    private static final ModConfigSpec.IntValue MAX_GANG;
    private static final ModConfigSpec.IntValue WARNING;
    private static final ModConfigSpec.IntValue VAULT;
    private static final ModConfigSpec.IntValue TIMEOUT;
    private static final ModConfigSpec.IntValue RETREAT;
    private static final ModConfigSpec.DoubleValue MORALE;
    private static final ModConfigSpec.IntValue DYNAMITE_COOLDOWN;
    private static final ModConfigSpec.DoubleValue STOLEN;
    private static final ModConfigSpec.DoubleValue DEPOSITS;
    private static final ModConfigSpec.IntValue ROBBED;
    private static final ModConfigSpec.IntValue PAY_TAKING_PART;
    private static final ModConfigSpec.IntValue PAY_KILL;
    private static final ModConfigSpec.IntValue PAY_LEADER;
    private static final ModConfigSpec.IntValue PAY_SAFECRACKER;
    private static final ModConfigSpec.IntValue PAY_LAWMAN_SAVED;
    private static final ModConfigSpec.IntValue HEAT_OFF;
    private static final ModConfigSpec.IntValue OUTLAW_CUT;
    private static final ModConfigSpec.IntValue OUTLAW_CUT_HEAT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Outlaw raids on frontier towns").push("raids");
        ENABLED = b.comment("Whether gangs raid towns on their own (the /hollowfrontier raid command works either way)").define("enabled", true);
        CHECK_INTERVAL = b.comment("How often each occupied town rolls for a raid at night").defineInRange("checkInterval", 200, 20, 24000);
        CHANCE = b.comment("Chance per roll").defineInRange("chance", 0.025, 0.0, 1.0);
        COOLDOWN = b.comment("Quiet time after a raid before that town can be raided again").defineInRange("cooldown", 72000, 0, Integer.MAX_VALUE);
        FIRST_RAID_GRACE = b.comment("Quiet time after a town is first visited").defineInRange("firstRaidGrace", 24000, 0, Integer.MAX_VALUE);
        b.push("gang");
        MIN_GANG = b.comment("Smallest gang, leader included").defineInRange("minSize", 4, 2, 16);
        MAX_GANG = b.comment("Largest gang, leader included").defineInRange("maxSize", 8, 2, 16);
        MORALE = b.comment("The gang breaks and runs once this share of the leader's men are down").defineInRange("moraleBreak", 0.6, 0.0, 1.0);
        DYNAMITE_COOLDOWN = b.comment("Ticks between a dynamiter's throws (plus up to 2 s)").defineInRange("dynamiteCooldown", 160, 20, 6000);
        b.pop().push("timing");
        WARNING = b.comment("Warning time: the bell rings and hoofbeats come closer before the gang rides in").defineInRange("warning", 400, 20, 6000);
        VAULT = b.comment("Time the safecracker needs at the vault").defineInRange("vault", 600, 20, 12000);
        TIMEOUT = b.comment("The gang gives up and rides off this long after riding in").defineInRange("timeout", 4800, 200, 72000);
        RETREAT = b.comment("Time the gang has to ride out before it's gone for good").defineInRange("retreat", 600, 20, 6000);
        b.pop().push("aftermath");
        STOLEN = b.comment("Share of each stack in the vault the gang takes").defineInRange("stolenShare", 0.75, 0.0, 1.0);
        DEPOSITS = b.comment("Share of the coins in each safe deposit box (every player's) the gang takes when it cracks a town's vault; 0 leaves the boxes alone")
                .defineInRange("depositsAtRisk", 0.25, 0.0, 1.0);
        ROBBED = b.comment("How long a robbed town stays sore (worse prices, a grim banker)").defineInRange("robbedTime", 48000, 0, Integer.MAX_VALUE);
        PAY_TAKING_PART = b.comment("Reward for anyone who hurt a raider").defineInRange("payTakingPart", 3, 0, 1000);
        PAY_KILL = b.comment("Reward per raider killed").defineInRange("payPerKill", 3, 0, 1000);
        PAY_LEADER = b.comment("Extra for killing the leader (his badge pays its bounty on top)").defineInRange("payLeader", 9, 0, 1000);
        PAY_SAFECRACKER = b.comment("Extra for killing a safecracker").defineInRange("paySafecracker", 6, 0, 1000);
        PAY_LAWMAN_SAVED = b.comment("Extra per lawman still standing at the end").defineInRange("payPerLawmanSaved", 2, 0, 1000);
        HEAT_OFF = b.comment("Wanted heat taken off a defender (8 clears two stars)").defineInRange("heatReduction", 8, 0, 12);
        OUTLAW_CUT = b.comment("A three-star outlaw who stays out of the gang's way gets this cut of a robbery").defineInRange("outlawCut", 9, 0, 1000);
        OUTLAW_CUT_HEAT = b.comment("...and this much heat for it").defineInRange("outlawCutHeat", 2, 0, 12);
        b.pop().pop();
        SPEC = b.build();
    }

    private static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.getAsInt() : value.getDefault();
    }

    private static double get(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.getAsDouble() : value.getDefault();
    }

    public static boolean enabled() { return SPEC.isLoaded() ? ENABLED.getAsBoolean() : ENABLED.getDefault(); }
    public static int checkInterval() { return get(CHECK_INTERVAL); }
    public static double chance() { return get(CHANCE); }
    public static int cooldown() { return get(COOLDOWN); }
    public static int firstRaidGrace() { return get(FIRST_RAID_GRACE); }
    public static int minGang() { return get(MIN_GANG); }
    public static int maxGang() { return Math.max(get(MIN_GANG), get(MAX_GANG)); }
    public static double moraleBreak() { return get(MORALE); }
    public static int dynamiteCooldown() { return get(DYNAMITE_COOLDOWN); }
    public static double stolenShare() { return get(STOLEN); }
    public static double depositsAtRisk() { return get(DEPOSITS); }
    public static int robbedTime(){ return get(ROBBED); }
    public static int payTakingPart() { return get(PAY_TAKING_PART); }
    public static int payKill() { return get(PAY_KILL); }
    public static int payLeader() { return get(PAY_LEADER); }
    public static int paySafecracker() { return get(PAY_SAFECRACKER); }
    public static int payLawmanSaved() { return get(PAY_LAWMAN_SAVED); }
    public static int heatReduction() { return get(HEAT_OFF); }
    public static int outlawCut() { return get(OUTLAW_CUT); }
    public static int outlawCutHeat() { return get(OUTLAW_CUT_HEAT); }

    /** The timings a raid runs on, fixed when it starts (and saved with it). Tests pass their own. */
    public static RaidTimings timings() {
        return new RaidTimings(get(WARNING), get(VAULT), get(TIMEOUT), get(RETREAT));
    }
}
