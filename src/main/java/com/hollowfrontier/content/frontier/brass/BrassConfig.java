package com.hollowfrontier.content.frontier.brass;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Modpack settings for brass (config/hollowfrontier-startup.toml). STARTUP, so it's loaded on registration, before the first datapack load,
 * when {@link BrassAlloyingCondition} decides whether the crafting-table alloy recipe exists. Before it loads the defaults apply.
 */
public final class BrassConfig {
    private BrassConfig() {}

    public enum Alloying { AUTO, ALWAYS, NEVER }

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.EnumValue<Alloying> ALLOYING;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Brass, and sharing it with other mods. Every Hollow Frontier recipe takes any mod's brass (the c:ingots/brass,",
                "c:nuggets/brass, c:plates/brass and c:gears/brass tags).").push("brass");
        ALLOYING = b.comment("Whether brass can be alloyed at a crafting table (3 copper ingots + 1 gold nugget -> 4).",
                        "AUTO: only while no other mod adds brass (anything else in c:ingots/brass, e.g. Create's), so that mod's way of making",
                        "brass is the only one. ALWAYS / NEVER: regardless. Read at startup, so restart the game after changing it.")
                .defineEnum("alloying", Alloying.AUTO);
        b.pop();
        SPEC = b.build();
    }

    public static Alloying alloying() {
        return SPEC.isLoaded() ? ALLOYING.get() : ALLOYING.getDefault();
    }
}
