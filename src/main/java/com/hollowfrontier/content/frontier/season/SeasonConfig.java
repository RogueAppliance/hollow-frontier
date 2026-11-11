package com.hollowfrontier.content.frontier.season;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The frontier's December: the per-world synced config (hollowfrontier-season-server.toml). */
public final class SeasonConfig {
    private SeasonConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.EnumValue<Season.Mode> MODE;
    private static final ModConfigSpec.BooleanValue TRUCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("A frontier December").push("season");
        MODE = b.comment("When December's decorations, drinks, carols and talk are on: AUTO goes by the server's own date, ALWAYS keeps them all year, NEVER turns them off. Christmas itself (December 24th to 26th) always goes by the date, unless this is NEVER")
                .defineEnum("mode", Season.Mode.AUTO);
        TRUCE = b.comment("Whether no outlaw raid starts from December 24th to 26th, and a sheriff lets one small fine go")
                .define("truce", true);
        b.pop();
        SPEC = b.build();
    }

    public static Season.Mode mode() { return SPEC.isLoaded() ? MODE.get() : MODE.getDefault(); }
    public static boolean truce() { return SPEC.isLoaded() ? TRUCE.getAsBoolean() : TRUCE.getDefault(); }
}
