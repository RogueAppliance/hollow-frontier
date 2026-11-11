package com.hollowfrontier.gametest;

import java.util.HashMap;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.gateway.HollowLanding;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.HollowEarth;
import com.hollowfrontier.worldgen.HollowFeatures;
import com.hollowfrontier.worldgen.HollowTerrain;
import com.hollowfrontier.worldgen.SeabedScanPlacement;
import com.hollowfrontier.worldgen.feature.SeaGrassFeature;
import com.hollowfrontier.worldgen.feature.SeaKelpFeature;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Phase 43a: the Still Sea. Like {@link HollowEarthTests}, the terrain tests sample the dimension's generator directly for
 * several seeds (the game test server loads no datapack dimensions). What they hold: the sea takes only the ground under
 * its continentalness threshold; everywhere else the biome is what the table gave before the sea and the land's height is
 * exactly what it was; out past the shelf there's a bed well under the water; and no land site may stand in it.
 */
final class StillSeaTests {
    private StillSeaTests() {}

    /** How close to the threshold a sample is left out: the biome source reads the climate rounded to four places. */
    private static final float EDGE = 0.002F;

    /**
     * A 32 x 32 grid 128 blocks apart for each seed (about 4 km square). Under the threshold: the Still Sea, never higher
     * than the land was, never over the beach. At or over it: the biome the old table gives and the old height, exactly.
     */
    static void seaTakesOnlyTheLowGround(GameTestHelper h) {
        HollowEarthTests.World world = HollowEarthTests.world(h);
        var access = h.getLevel().registryAccess();
        var functions = access.lookupOrThrow(Registries.DENSITY_FUNCTION);
        DensityFunction continents = functions.getOrThrow(HollowTerrain.F_CONTINENTS).value();
        DensityFunction before = functions.getOrThrow(HollowTerrain.LAND_HEIGHT).value();
        DensityFunction now = functions.getOrThrow(HollowTerrain.HEIGHT).value();
        var old = MultiNoiseBiomeSource.createFromList(HollowBiomes.parametersBeforeTheSea(access.lookupOrThrow(Registries.BIOME)));
        int samples = 0;
        int sea = 0;
        int deep = 0;
        int wetBefore = 0;
        for (long seed : HollowEarthTests.SEEDS) {
            RandomState random = world.random(seed);
            BiomeResolver biomes = world.generator().getBiomeSource().createUncachedResolver(random);
            BiomeResolver biomesBefore = old.createUncachedResolver(random);
            int seedSea = 0;
            for (int i = -16; i < 16; i++) {
                for (int j = -16; j < 16; j++) {
                    int x = i * 128;
                    int z = j * 128;
                    float c = random.sampleBlockValueUncached(continents, x, 0, z);
                    if (Math.abs(c - HollowBiomes.SEA_CONTINENTS) < EDGE) {
                        continue;
                    }
                    samples++;
                    float was = random.sampleBlockValueUncached(before, x, 0, z);
                    float is = random.sampleBlockValueUncached(now, x, 0, z);
                    Holder<Biome> biome = biomes.getNoiseBiome(x >> 2, 80 >> 2, z >> 2);
                    String where = "seed " + seed + " " + x + "," + z + " (continents " + c + ")";
                    if (c < HollowBiomes.SEA_CONTINENTS) {
                        sea++;
                        seedSea++;
                        if (was < HollowEarth.SEA_LEVEL) {
                            wetBefore++;
                        }
                        h.assertTrue(biome.is(HollowBiomes.STILL_SEA), where + ": low ground that isn't sea: " + biome.unwrapKey());
                        h.assertTrue(is <= was, where + ": the sea raised the land, " + was + " -> " + is);
                        if (c < HollowBiomes.SEA_CONTINENTS - HollowTerrain.SEA_SHORE) {
                            h.assertTrue(is <= HollowEarth.SEA_LEVEL + 1.5F, where + ": land past the shore stands at " + is);
                        }
                        if (c < HollowBiomes.SEA_CONTINENTS - HollowTerrain.SEA_SHELF) {
                            deep++;
                            // the bed is 15 to 35 under the surface, or wherever the land already lay if that was lower still
                            h.assertTrue(is <= HollowEarth.SEA_LEVEL - 15.0F && is >= Math.min(was, HollowEarth.SEA_LEVEL - 35.0F), where + ": the deep bed is at " + is + " (the land was at " + was + ")");
                        }
                    } else {
                        h.assertFalse(biome.is(HollowBiomes.STILL_SEA), where + ": sea over the threshold");
                        Holder<Biome> wasBiome = biomesBefore.getNoiseBiome(x >> 2, 80 >> 2, z >> 2);
                        h.assertTrue(biome.unwrapKey().equals(wasBiome.unwrapKey()), where + ": the land's biome changed, " + wasBiome.unwrapKey() + " -> " + biome.unwrapKey());
                        h.assertTrue(is == was, where + ": the land moved, " + was + " -> " + is);
                    }
                }
            }
            h.assertTrue(seedSea > 0, "seed " + seed + " has no sea within 2 km of the origin");
        }
        double share = sea / (double) samples;
        LogUtils.getLogger().info("[{}] The Still Sea: {} of {} samples ({}%), {} past the shelf; {} of them were already under sea level before the sea",
                HollowFrontier.MODID, sea, samples, Math.round(share * 100), deep, wetBefore);
        h.assertTrue(share >= 0.12 && share <= 0.28, "the sea should be 15-25% of the Hollow, give or take: " + Math.round(share * 100) + "%");
        h.assertTrue(deep > 0, "no sample reached deep water");
        h.succeed();
    }

    /**
     * Bare noise columns out past the shelf: water just under the surface, a solid bed 8 to 64 blocks down (15 to 35 before the 3D noise roughens it, deeper where the land already lay lower), and the crust
     * still overhead. A column a spire stands in is solid at the surface, and is left out.
     */
    static void deepWaterHasABed(GameTestHelper h) {
        HollowEarthTests.World world = HollowEarthTests.world(h);
        DensityFunction continents = h.getLevel().registryAccess().lookupOrThrow(Registries.DENSITY_FUNCTION).getOrThrow(HollowTerrain.F_CONTINENTS).value();
        int open = 0;
        long depths = 0;
        for (long seed : HollowEarthTests.SEEDS) {
            RandomState random = world.random(seed);
            HollowLanding.Terrain terrain = HollowEarthTests.terrain(world, random);
            int found = 0;
            for (int i = -24; i < 24 && found < 12; i++) {
                for (int j = -24; j < 24 && found < 12; j++) {
                    int x = i * 96;
                    int z = j * 96;
                    if (random.sampleBlockValueUncached(continents, x, 0, z) >= HollowBiomes.SEA_CONTINENTS - HollowTerrain.SEA_SHELF) {
                        continue;
                    }
                    found++;
                    String where = "seed " + seed + " " + x + "," + z;
                    BlockState surface = terrain.get(x, HollowEarth.SEA_LEVEL - 1, z);
                    if (surface.isSolid()) {
                        continue; // a sea stack
                    }
                    h.assertTrue(surface.is(Blocks.WATER), where + ": no water under the surface: " + surface);
                    h.assertTrue(terrain.get(x, HollowEarth.SEA_LEVEL + 1, z).isAir(), where + ": no air over the water");
                    int bed = HollowEarth.SEA_LEVEL - 1;
                    while (bed > HollowEarth.MIN_Y && !terrain.get(x, bed, z).isSolid()) {
                        bed--;
                    }
                    int depth = HollowEarth.SEA_LEVEL - 1 - bed;
                    h.assertTrue(depth >= 8 && depth <= 64, where + ": the bed is " + depth + " blocks down");
                    h.assertTrue(HollowLanding.ceilingAbove(terrain, new BlockPos(x, HollowEarth.SEA_LEVEL, z)).isPresent(), where + ": no crust over the sea");
                    open++;
                    depths += depth;
                }
            }
        }
        LogUtils.getLogger().info("[{}] The Still Sea: {} deep columns, {} blocks deep on average", HollowFrontier.MODID, open, depths / Math.max(1, open));
        h.assertTrue(open >= 20, "only " + open + " open deep-water columns found");
        h.succeed();
    }

    /**
     * The biome: its bed's features and the falls; Hollow Earth but not Hollow land; out of vanilla's ocean tags; and no
     * the land's sites keep out of it (which structures may stand in it is SeaSiteTests' business, since 43d).
     */
    static void seaBiomeIsItsOwn(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        Holder<Biome> sea = access.lookupOrThrow(Registries.BIOME).getOrThrow(HollowBiomes.STILL_SEA);
        Map<ResourceKey<PlacedFeature>, Boolean> wanted = new HashMap<>();
        for (var key : java.util.List.of(HollowFeatures.SEA_KELP_FORESTS, HollowFeatures.SEA_GRASS_MEADOWS, HollowFeatures.WATERFALLS_COMMON)) {
            wanted.put(key, false);
        }
        for (HolderSet<PlacedFeature> step : sea.value().getGenerationSettings().features()) {
            step.forEach(f -> f.unwrapKey().ifPresent(k -> wanted.computeIfPresent(k, (key, v) -> true)));
        }
        wanted.forEach((key, present) -> h.assertTrue(present, "the Still Sea is missing " + key.identifier()));
        h.assertTrue(sea.is(HollowBiomes.IS_HOLLOW_EARTH), "the sea should be Hollow Earth");
        h.assertFalse(sea.is(HollowBiomes.IS_HOLLOW_LAND), "the sea isn't land");
        for (ResourceKey<Biome> land : HollowBiomes.LANDS) {
            Holder<Biome> biome = access.lookupOrThrow(Registries.BIOME).getOrThrow(land);
            h.assertTrue(biome.is(HollowBiomes.IS_HOLLOW_EARTH) && biome.is(HollowBiomes.IS_HOLLOW_LAND), land.identifier() + " should be Hollow land");
        }
        h.assertFalse(sea.is(BiomeTags.IS_OCEAN) || sea.is(BiomeTags.IS_DEEP_OCEAN) || sea.is(BiomeTags.IS_BEACH) || sea.is(BiomeTags.IS_RIVER),
                "the sea is in a vanilla water tag, which would bring vanilla's ocean structures and spawns");
        h.succeed();
    }

    /** Kelp on the shelf: a stalk from the bed that stops short of the surface; none in the shallows. */
    static void kelpGrowsOnTheShelf(GameTestHelper h) {
        BlockPos bed = pool(h, 10);
        place(h, SeaKelpFeature.INSTANCE, bed, 3L, true);
        h.assertBlockPresent(Blocks.KELP_PLANT, bed);
        int top = 0;
        while (h.getBlockState(bed.above(top)).is(Blocks.KELP_PLANT)) {
            top++;
        }
        h.assertBlockPresent(Blocks.KELP, bed.above(top));
        h.assertTrue(top + 1 <= 9, "the kelp reaches the surface: " + (top + 1) + " tall in 10 of water");
        h.assertBlockPresent(Blocks.WATER, bed.above(9));
        BlockPos shallow = pool(h, SeaKelpFeature.MIN_DEPTH - 1);
        place(h, SeaKelpFeature.INSTANCE, shallow, 3L, false);
        h.assertBlockPresent(Blocks.WATER, shallow);
        h.succeed();
    }

    /** Seagrass in the shallows, none on dry ground. */
    static void seagrassGrowsInTheShallows(GameTestHelper h) {
        BlockPos bed = pool(h, 4);
        int grown = 0;
        for (long seed = 1; seed <= 8; seed++) {
            BlockPos spot = bed.offset((int) seed - 4, 0, 2);
            if (SeaGrassFeature.INSTANCE.place(h.getLevel(), h.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed), h.absolutePos(spot))) {
                grown++;
                BlockState state = h.getBlockState(spot);
                h.assertTrue(state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS), "not seagrass: " + state);
            }
        }
        h.assertTrue(grown == 8, "seagrass grew at only " + grown + " of 8 spots");
        BlockPos dry = pool(h, 0);
        place(h, SeaGrassFeature.INSTANCE, dry, 1L, false);
        h.assertTrue(SeabedScanPlacement.wet(Blocks.WATER.defaultBlockState()) && SeabedScanPlacement.wet(Blocks.KELP_PLANT.defaultBlockState())
                && !SeabedScanPlacement.wet(Blocks.AIR.defaultBlockState()) && !SeabedScanPlacement.wet(Blocks.SAND.defaultBlockState()),
                "the seabed scan should pass through water and kelp and stop at anything else");
        h.succeed();
    }

    /** A walled pool on the garden plot with {@code depth} blocks of water over a sand bed; returns the water on the bed at its middle. */
    private static BlockPos pool(GameTestHelper h, int depth) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                boolean wall = x == 0 || z == 0 || x == 15 || z == 15;
                h.setBlock(new BlockPos(x, 0, z), Blocks.SAND);
                for (int y = 1; y < 14; y++) {
                    h.setBlock(new BlockPos(x, y, z), y <= depth ? (wall ? Blocks.SANDSTONE : Blocks.WATER) : Blocks.AIR);
                }
            }
        }
        return new BlockPos(8, 1, 8);
    }

    private static void place(GameTestHelper h, Feature feature, BlockPos at, long seed, boolean expected) {
        boolean placed = feature.place(h.getLevel(), h.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed), h.absolutePos(at));
        h.assertTrue(placed == expected, feature + (expected ? " didn't place" : " placed where it shouldn't") + " (seed " + seed + ")");
    }
}
