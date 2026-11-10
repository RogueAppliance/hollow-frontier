package com.hollowfrontier.datagen;

import java.util.concurrent.CompletableFuture;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.weather.DustDevils;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.ModSpawns;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.hollowfrontier.worldgen.structure.TownStyle;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;

public class ModBiomeTagsProvider extends TagsProvider<Biome> {
    public ModBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.BIOME, lookupProvider, HollowFrontier.MODID);
    }

    /** A biome of Terralith's, for optional entries: ignored when Terralith isn't installed. */
    private static ResourceKey<Biome> terralith(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("terralith", name));
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ModStructures.IS_FRONTIER)
                .addTag(Tags.Biomes.IS_DESERT)
                .addTag(Tags.Biomes.IS_PLAINS)
                .addTag(BiomeTags.IS_SAVANNA)
                .addTag(BiomeTags.IS_BADLANDS)
                .addOptional(terralith("hot_shrubland"))
                .addOptional(terralith("shrubland"))
                .addOptional(terralith("yosemite_lowlands"));
        tag(ModSpawns.HAS_OUTLAWS).addTag(ModStructures.IS_FRONTIER);
        // Town styles (TownStyle): clapboard on the grass, missions in the sand, mining camps up in the pines
        // Terralith's level dry country: its tags leave these out, and its deserts proper are mostly too rugged for a town
        tag(TownStyle.FRONTIER.biomes())
                .addTag(Tags.Biomes.IS_PLAINS)
                .addTag(BiomeTags.IS_SAVANNA)
                .addOptional(terralith("shrubland"))
                .addOptional(terralith("yosemite_lowlands"));
        tag(TownStyle.DESERT.biomes())
                .addTag(Tags.Biomes.IS_DESERT)
                .addTag(BiomeTags.IS_BADLANDS)
                .addOptional(terralith("hot_shrubland"));
        tag(TownStyle.MINING.biomes())
                .addTag(BiomeTags.IS_TAIGA)
                .addTag(BiomeTags.IS_HILL)
                .addTag(Tags.Biomes.IS_HILL)
                .add(Biomes.MEADOW);
        // Polar Gateways: frozen country only (the Overworld has no poles, so the snows stand in for them). Any mod's snowy
        // biomes count (c:is_snowy), since the way into the Hollow Earth mustn't vanish in a modded world; beaches, oceans,
        // rivers and bare peaks are taken out again (NeoForge's tag removal).
        tag(ModStructures.HAS_POLAR_GATEWAY)
                .add(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA, Biomes.GROVE, Biomes.SNOWY_SLOPES)
                .addTag(Tags.Biomes.IS_SNOWY)
                .remove(Tags.Biomes.IS_BEACH, BiomeTags.IS_OCEAN, BiomeTags.IS_RIVER, Tags.Biomes.IS_MOUNTAIN_PEAK);
        // The Hollow Earth, and where its megafauna (10a) and the Grays' labs (10b) will turn up
        // 43a: the Still Sea is Hollow Earth but not land, so the sites that stand anywhere go by is_hollow_land. It stays out
        // of minecraft:is_ocean and every vanilla has_structure tag: no monuments, shipwrecks or drowned under the crust.
        tag(HollowBiomes.IS_HOLLOW_LAND).add(HollowBiomes.PRIMORDIAL_JUNGLE, HollowBiomes.CRYSTAL_STEPPES, HollowBiomes.EMBER_BADLANDS,
                HollowBiomes.CONCAVIAN_HIGHLANDS);
        tag(HollowBiomes.IS_HOLLOW_EARTH).add(HollowBiomes.STILL_SEA);
        tag(HollowBiomes.IS_HOLLOW_EARTH).add(HollowBiomes.PRIMORDIAL_JUNGLE, HollowBiomes.CRYSTAL_STEPPES, HollowBiomes.EMBER_BADLANDS,
                HollowBiomes.CONCAVIAN_HIGHLANDS);
        tag(HollowBiomes.HAS_MAMMOTHS).add(HollowBiomes.CONCAVIAN_HIGHLANDS, HollowBiomes.CRYSTAL_STEPPES);
        tag(HollowBiomes.HAS_TERROR_BIRDS).add(HollowBiomes.CRYSTAL_STEPPES, HollowBiomes.EMBER_BADLANDS);
        tag(HollowBiomes.HAS_MEGATHERIUM).add(HollowBiomes.PRIMORDIAL_JUNGLE);
        tag(HollowBiomes.HAS_GLYPTODONS).add(HollowBiomes.PRIMORDIAL_JUNGLE, HollowBiomes.CRYSTAL_STEPPES);
        tag(HollowBiomes.HAS_GRAY_LABS).add(HollowBiomes.PRIMORDIAL_JUNGLE, HollowBiomes.CRYSTAL_STEPPES, HollowBiomes.EMBER_BADLANDS);
        tag(ModStructures.HAS_KINDLED_SITES).add(HollowBiomes.CONCAVIAN_HIGHLANDS);
        tag(ModStructures.HAS_CRYSTAL_RUINS).add(HollowBiomes.CONCAVIAN_HIGHLANDS);
        tag(ModStructures.HAS_SUN_TEMPLE).add(HollowBiomes.EMBER_BADLANDS);
        tag(ModStructures.HAS_CRASH_SITE).addTag(ModStructures.IS_FRONTIER).addTag(BiomeTags.IS_FOREST).addTag(BiomeTags.IS_TAIGA);
        // 29: dust devils over the dry country, Ash Devils in the Ember Badlands
        tag(DustDevils.HAS_DUST_DEVILS)
                .addTag(Tags.Biomes.IS_DESERT)
                .addTag(BiomeTags.IS_SAVANNA)
                .addTag(BiomeTags.IS_BADLANDS);
        tag(DustDevils.HAS_ASH_DEVILS).add(HollowBiomes.EMBER_BADLANDS);
        tag(com.hollowfrontier.content.hollow.sea.SeaLife.HAS_SEA_LIFE).add(HollowBiomes.STILL_SEA); // 43b
        tag(ModSpawns.HAS_RATTLESNAKES)
                .addTag(Tags.Biomes.IS_DESERT)
                .addTag(BiomeTags.IS_SAVANNA)
                .addTag(BiomeTags.IS_BADLANDS);
    }
}
