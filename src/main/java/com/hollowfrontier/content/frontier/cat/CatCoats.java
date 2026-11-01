package com.hollowfrontier.content.frontier.cat;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.worldgen.ModSpawns;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;

/**
 * The frontier's two cat coats, as data-driven cat_variant entries (datagen). Desert (sandy, faint stripes) also turns up
 * among ordinary village cats in desert, badlands and savanna biomes; Saloon (scruffy, patchy, a notched ear) never
 * spawns on its own. Both are rolled for town cats by TownCatOdds, and kittens inherit them like any coat.
 */
public final class CatCoats {
    private CatCoats() {}

    public static final ResourceKey<CatVariant> DESERT = key("desert");
    public static final ResourceKey<CatVariant> SALOON = key("saloon");

    private static ResourceKey<CatVariant> key(String name) {
        return ResourceKey.create(Registries.CAT_VARIANT, HollowFrontier.id(name));
    }

    public static void bootstrap(BootstrapContext<CatVariant> context) {
        // Priority 0 matches vanilla's fallback coats, so in those biomes it's one more coat in the draw.
        context.register(DESERT, coat("desert", new SpawnPrioritySelectors(List.of(
                new PriorityProvider.Selector<>(new BiomeCheck(context.lookup(Registries.BIOME).getOrThrow(ModSpawns.HAS_RATTLESNAKES)), 0)))));
        context.register(SALOON, coat("saloon", SpawnPrioritySelectors.EMPTY));
    }

    private static CatVariant coat(String name, SpawnPrioritySelectors spawns) {
        return new CatVariant(new ClientAsset.ResourceTexture(HollowFrontier.id("entity/cat/" + name)),
                new ClientAsset.ResourceTexture(HollowFrontier.id("entity/cat/" + name + "_baby")), spawns);
    }
}
