package com.hollowfrontier.worldgen;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModEntities;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Natural mob spawns. Outlaws roam open frontier biomes at night (monster light rules, see ModEntities spawn placements);
// rattlesnakes live in deserts, badlands and savannas.
public final class ModSpawns {
    private ModSpawns() {}

    public static final TagKey<Biome> HAS_OUTLAWS = TagKey.create(Registries.BIOME, HollowFrontier.id("has_outlaws"));
    public static final ResourceKey<BiomeModifier> ADD_OUTLAW_SPAWNS =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, HollowFrontier.id("add_outlaw_spawns"));
    public static final TagKey<Biome> HAS_RATTLESNAKES = TagKey.create(Registries.BIOME, HollowFrontier.id("has_rattlesnakes"));
    public static final ResourceKey<BiomeModifier> ADD_RATTLESNAKE_SPAWNS =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, HollowFrontier.id("add_rattlesnake_spawns"));

    // The Hollow Earth's wild megafauna (10a), on the biome tags 9a set up (so other biomes can join by tag)
    public static final ResourceKey<BiomeModifier> ADD_MAMMOTH_SPAWNS = modifier("add_mammoth_spawns");
    public static final ResourceKey<BiomeModifier> ADD_TERROR_BIRD_SPAWNS = modifier("add_terror_bird_spawns");
    public static final ResourceKey<BiomeModifier> ADD_MEGATHERIUM_SPAWNS = modifier("add_megatherium_spawns");
    public static final ResourceKey<BiomeModifier> ADD_GLYPTODON_SPAWNS = modifier("add_glyptodon_spawns");
    // 13a: Gray Wardens about the Gray labs' biomes
    public static final ResourceKey<BiomeModifier> ADD_GRAY_WARDEN_SPAWNS = modifier("add_gray_warden_spawns");

    private static ResourceKey<BiomeModifier> modifier(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, HollowFrontier.id(name));
    }

    public static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        context.register(ADD_OUTLAW_SPAWNS, new BiomeModifiers.AddSpawnsBiomeModifier(
                biomes.getOrThrow(HAS_OUTLAWS),
                WeightedList.<MobSpawnSettings.SpawnerData>builder()
                        .add(new MobSpawnSettings.SpawnerData(ModEntities.OUTLAW.get(), UniformInt.of(1, 3)), 20)
                        .add(new MobSpawnSettings.SpawnerData(ModEntities.OUTLAW_GUNSLINGER.get(), UniformInt.of(1, 1)), 6)
                        .build()));
        // Rattlesnakes are creatures (daylight, mostly placed as chunks generate), on sand, terracotta and dry grass.
        context.register(ADD_RATTLESNAKE_SPAWNS, new BiomeModifiers.AddSpawnsBiomeModifier(
                biomes.getOrThrow(HAS_RATTLESNAKES),
                WeightedList.<MobSpawnSettings.SpawnerData>builder()
                        .add(new MobSpawnSettings.SpawnerData(ModEntities.RATTLESNAKE.get(), UniformInt.of(1, 2)), 12)
                        .build()));
        // Herds of mammoths, a megatherium or two, glyptodons in ones and twos: creatures, placed as chunks generate. Terror
        // birds are monsters that come and go by day, kept few by their own spawn rule (TerrorBird.checkSpawnRules).
        spawns(context, ADD_MAMMOTH_SPAWNS, biomes.getOrThrow(HollowBiomes.HAS_MAMMOTHS), ModEntities.WOOLLY_MAMMOTH.get(), 4, 2, 4);
        spawns(context, ADD_TERROR_BIRD_SPAWNS, biomes.getOrThrow(HollowBiomes.HAS_TERROR_BIRDS), ModEntities.TERROR_BIRD.get(), 25, 1, 2);
        spawns(context, ADD_MEGATHERIUM_SPAWNS, biomes.getOrThrow(HollowBiomes.HAS_MEGATHERIUM), ModEntities.MEGATHERIUM.get(), 4, 1, 2);
        spawns(context, ADD_GLYPTODON_SPAWNS, biomes.getOrThrow(HollowBiomes.HAS_GLYPTODONS), ModEntities.GLYPTODON.get(), 5, 1, 2);
        spawns(context, ADD_GRAY_WARDEN_SPAWNS, biomes.getOrThrow(HollowBiomes.HAS_GRAY_LABS), ModEntities.GRAY_WARDEN.get(), 4, 1, 2);
    }

    private static void spawns(BootstrapContext<BiomeModifier> context, ResourceKey<BiomeModifier> key, HolderSet<Biome> biomes, EntityType<?> type,
                               int weight, int min, int max) {
        context.register(key, new BiomeModifiers.AddSpawnsBiomeModifier(biomes,
                WeightedList.<MobSpawnSettings.SpawnerData>builder().add(new MobSpawnSettings.SpawnerData(type, UniformInt.of(min, max)), weight).build()));
    }
}
