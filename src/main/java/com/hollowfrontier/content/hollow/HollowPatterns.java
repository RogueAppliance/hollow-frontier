package com.hollowfrontier.content.hollow;

import java.util.List;

import com.hollowfrontier.HollowFrontier;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;

/**
 * The Hollow Earth's own patterns (28), data-driven like vanilla's: three pottery sherds brushed out of its ruins (the
 * inner sun, a crystal, the Gray watcher) and the Concavian armor trim. Each sherd's pattern is drawn from
 * {@code textures/entity/decorated_pot/<name>_pottery_pattern.png}; the trim from {@code textures/trims/entity/...}.
 */
public final class HollowPatterns {
    private HollowPatterns() {}

    public static final ResourceKey<DecoratedPotPattern> SUN = pot("sun");
    public static final ResourceKey<DecoratedPotPattern> CRYSTAL = pot("crystal");
    public static final ResourceKey<DecoratedPotPattern> WATCHER = pot("watcher");
    public static final List<ResourceKey<DecoratedPotPattern>> POTS = List.of(SUN, CRYSTAL, WATCHER);
    public static final ResourceKey<TrimPattern> CONCAVIAN = ResourceKey.create(Registries.TRIM_PATTERN, HollowFrontier.id("concavian"));

    private static ResourceKey<DecoratedPotPattern> pot(String name) {
        return ResourceKey.create(Registries.DECORATED_POT_PATTERN, HollowFrontier.id(name));
    }

    public static void bootstrapPots(BootstrapContext<DecoratedPotPattern> context) {
        for (ResourceKey<DecoratedPotPattern> key : POTS) {
            context.register(key, new DecoratedPotPattern(key.identifier().withSuffix("_pottery_pattern")));
        }
    }

    public static void bootstrapTrims(BootstrapContext<TrimPattern> context) {
        context.register(CONCAVIAN, new TrimPattern(CONCAVIAN.identifier(),
                Component.translatable(Util.makeDescriptionId("trim_pattern", CONCAVIAN.identifier())), false));
    }
}
