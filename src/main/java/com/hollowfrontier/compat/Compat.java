package com.hollowfrontier.compat;

import java.util.List;
import java.util.function.Supplier;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.compat.curios.CuriosCompat;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModRecipes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/**
 * Optional compat (phase 14). JEI and Jade find their plugins themselves ({@code compat/jei}, {@code compat/jade});
 * Curios is wired up here. Each mod's classes are only touched once {@link #isLoaded} says it's there.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Compat {
    public static final String JEI = "jei";
    public static final String JADE = "jade";
    public static final String CURIOS = "curios";
    public static final String IRIS = "iris";

    /** Items JEI gives an info page ({@link #infoKey}): steam, which no recipe explains, and what's only found in the world. */
    public static final List<Supplier<? extends Item>> INFO_PAGES = List.of(
            ModItems.BOILER, ModItems.COPPER_PIPE, ModItems.STEAM_TANK, ModItems.PRESSURE_GAUGE, ModItems.STEAM_CANISTER,
            ModItems.OUTLAW_BADGE, ModItems.BRASS_STAR, ModItems.STRANGE_SKULL, ModItems.GRAY_TECH_FRAGMENT, ModItems.KINDLED_SIGIL,
            ModItems.SUNSTONE_SHARD, ModItems.PHLOGISTON_CORE,
            ModItems.ALIEN_ALLOY_PLATING, ModItems.DAMAGED_GRAVITY_DRIVE, ModItems.BEAM_EMITTER, ModItems.LENS_ARRAY, ModItems.NAVIGATION_CORE,
            ModItems.CLOAKING_DEVICE, ModItems.CARGO_HOLD, ModItems.LETTER_HOME, ModItems.SEALED_HULL);

    private Compat() {}

    public static String infoKey(Item item) {
        return "jei." + HollowFrontier.MODID + ".info." + BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @SubscribeEvent
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (isLoaded(CURIOS)) {
            CuriosCompat.registerCapabilities(event);
        }
    }

    /** Vanilla only sends clients the recipe-book types; recipe viewers need the crusher's too. */
    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(ModRecipes.CRUSHING.get());
    }
}
