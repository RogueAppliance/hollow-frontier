package com.hollowfrontier.gametest;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.HollowFeatures;
import com.hollowfrontier.worldgen.feature.AshVentFeature;
import com.hollowfrontier.worldgen.feature.CrystalOutcropFeature;
import com.hollowfrontier.worldgen.feature.CycadFeature;
import com.hollowfrontier.worldgen.feature.GiantFernFeature;
import com.hollowfrontier.worldgen.feature.StandingStoneFeature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Phase 9b1: the Hollow Earth's flora and ambience. Each feature is grown for real on a 16-block garden plot (grass at
 * y 0) from a fixed seed, and the biomes are checked for their features, sounds and particles.
 */
final class HollowFloraTests {
    private HollowFloraTests() {}

    /** Where the garden's features grow from: the middle of the plot, on the grass. */
    private static final BlockPos ORIGIN = new BlockPos(8, 1, 8);

    /** Each biome carries its own features, and has ambient sounds and particles of its own. */
    static void floraInBiomes(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        Map<ResourceKey<Biome>, List<ResourceKey<PlacedFeature>>> expected = Map.of(
                HollowBiomes.PRIMORDIAL_JUNGLE, List.of(HollowFeatures.JUNGLE_GIANT_FERNS, HollowFeatures.JUNGLE_CYCADS),
                HollowBiomes.CRYSTAL_STEPPES, List.of(HollowFeatures.STEPPE_CRYSTAL_OUTCROPS),
                HollowBiomes.EMBER_BADLANDS, List.of(HollowFeatures.BADLANDS_ASH_VENTS),
                HollowBiomes.CONCAVIAN_HIGHLANDS, List.of(HollowFeatures.HIGHLAND_STANDING_STONES));
        expected.forEach((key, features) -> {
            Biome biome = biomes.getOrThrow(key).value();
            List<HolderSet<PlacedFeature>> steps = biome.getGenerationSettings().features();
            for (ResourceKey<PlacedFeature> feature : features) {
                h.assertTrue(steps.stream().anyMatch(step -> step.stream().anyMatch(f -> f.is(feature))),
                        key.identifier() + " is missing " + feature.identifier());
            }
            h.assertTrue(biome.getAttributes().contains(EnvironmentAttributes.AMBIENT_SOUNDS), key.identifier() + " has no ambient sounds");
            h.assertTrue(biome.getAttributes().contains(EnvironmentAttributes.AMBIENT_PARTICLES), key.identifier() + " has no ambient particles");
        });
        // Fronds are canopy, so the crossing's landing and the return gateway's clearing see through them
        h.assertTrue(ModBlocks.FERN_FROND.get().defaultBlockState().is(BlockTags.LEAVES), "fern fronds should be leaves");
        h.succeed();
    }

    /** A tree fern: a dark trunk at least four high and a crown of persistent fronds spread round its top. */
    static void giantFernGrows(GameTestHelper h) {
        place(h, GiantFernFeature.INSTANCE, 3L);
        for (int y = 0; y < 4; y++) {
            h.assertBlockPresent(Blocks.DARK_OAK_LOG, ORIGIN.above(y));
        }
        int fronds = count(h, s -> s.is(ModBlocks.FERN_FROND.get()));
        h.assertTrue(fronds >= 12, "only " + fronds + " fronds");
        h.assertTrue(count(h, s -> s.is(ModBlocks.FERN_FROND.get()) && !s.getValue(LeavesBlock.PERSISTENT)) == 0, "fronds would decay");
        h.assertTrue(spread(h, s -> s.is(ModBlocks.FERN_FROND.get())) >= 3, "the crown should spread at least three blocks out");
        h.succeed();
    }

    /** A cycad: a short trunk of jungle wood with a crown of fronds. */
    static void cycadGrows(GameTestHelper h) {
        place(h, CycadFeature.INSTANCE, 5L);
        h.assertBlockPresent(Blocks.JUNGLE_WOOD, ORIGIN);
        h.assertTrue(count(h, s -> s.is(ModBlocks.FERN_FROND.get())) >= 8, "the cycad has too few fronds");
        h.succeed();
    }

    /** A crystal outcrop: calcite in the turf, spires of crystal, and glowing clusters. */
    static void crystalOutcropForms(GameTestHelper h) {
        place(h, CrystalOutcropFeature.INSTANCE, 7L);
        h.assertTrue(count(h, s -> s.is(Blocks.CALCITE)) >= 6, "no calcite footing");
        h.assertTrue(count(h, s -> s.is(Blocks.AMETHYST_BLOCK)) >= 4, "no crystal spires");
        h.assertTrue(count(h, s -> s.is(Blocks.AMETHYST_CLUSTER)) >= 1, "no clusters on the spires");
        h.assertTrue(count(h, s -> s.getLightEmission() > 0) >= 1, "nothing in the outcrop glows");
        h.succeed();
    }

    /** An ash vent: the vent on its cone over a magma core, in a field of ash. */
    static void ashVentForms(GameTestHelper h) {
        place(h, AshVentFeature.INSTANCE, 11L);
        h.assertBlockPresent(ModBlocks.ASH_VENT.get(), ORIGIN.above());
        h.assertBlockPresent(Blocks.MAGMA_BLOCK, ORIGIN);
        int ash = count(h, s -> s.is(ModBlocks.ASH.get()));
        h.assertTrue(ash >= 10, "only " + ash + " ash");
        h.assertTrue(count(h, s -> s.is(Blocks.BASALT) || s.is(Blocks.BLACKSTONE)) >= 4, "no cone round the vent");
        h.succeed();
    }

    /** Kindled stonework, every kind (stone, toppled pillar, ring) over a run of seeds: pale stone always stands. */
    static void standingStonesRaise(GameTestHelper h) {
        Predicate<BlockState> pale = s -> s.is(Blocks.CALCITE) || s.is(Blocks.POLISHED_DIORITE) || s.is(Blocks.QUARTZ_PILLAR)
                || s.is(Blocks.CHISELED_QUARTZ_BLOCK);
        boolean pillar = false, ring = false;
        for (long seed = 0; seed < 24; seed++) {
            clear(h);
            place(h, StandingStoneFeature.INSTANCE, seed);
            h.assertTrue(count(h, pale) >= 2, "seed " + seed + ": no stonework");
            pillar |= count(h, s -> s.is(Blocks.QUARTZ_PILLAR)) > 0;
            ring |= h.getBlockState(ORIGIN.below()).is(Blocks.CHISELED_QUARTZ_BLOCK);
        }
        h.assertTrue(pillar && ring, "24 seeds should give a toppled pillar and a ring (pillar " + pillar + ", ring " + ring + ")");
        h.succeed();
    }

    /** An ash vent is hot underfoot, like magma, unless you step carefully. */
    static void ashVentBurns(GameTestHelper h) {
        BlockPos vent = new BlockPos(2, 0, 2);
        h.setBlock(vent, ModBlocks.ASH_VENT.get());
        // A pig, not a player: a fresh player has spawn protection, which shrugs off the burn
        Pig pig = h.spawnWithNoFreeWill(EntityTypes.PIG, vent.above());
        BlockState state = h.getBlockState(vent);
        float health = pig.getHealth();
        pig.setShiftKeyDown(true);
        state.getBlock().stepOn(h.getLevel(), h.absolutePos(vent), state, pig);
        h.assertTrue(pig.getHealth() == health, "sneaking across the vent burned");
        pig.setShiftKeyDown(false);
        state.getBlock().stepOn(h.getLevel(), h.absolutePos(vent), state, pig);
        h.assertTrue(pig.getHealth() < health, "the vent didn't burn");
        h.succeed();
    }

    /** Ash falls like sand. */
    static void ashFalls(GameTestHelper h) {
        h.setBlock(new BlockPos(5, 4, 5), ModBlocks.ASH.get());
        h.succeedWhen(() -> h.assertBlockPresent(ModBlocks.ASH.get(), new BlockPos(5, 1, 5)));
    }

    // ---------------------------------------------------------------- helpers

    private static void place(GameTestHelper h, Feature feature, long seed) {
        boolean placed = feature.place(h.getLevel(), h.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed), h.absolutePos(ORIGIN));
        h.assertTrue(placed, feature + " didn't place (seed " + seed + ")");
    }

    /** Back to bare grass. */
    private static void clear(GameTestHelper h) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
                for (int y = 1; y < 16; y++) {
                    h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static int count(GameTestHelper h, Predicate<BlockState> what) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 15, 15, 15)) {
            if (what.test(h.getBlockState(pos))) {
                n++;
            }
        }
        return n;
    }

    /** How far out from the origin's column the matching blocks reach (Chebyshev distance). */
    private static int spread(GameTestHelper h, Predicate<BlockState> what) {
        int reach = 0;
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 15, 15, 15)) {
            if (what.test(h.getBlockState(pos))) {
                reach = Math.max(reach, Math.max(Math.abs(pos.getX() - ORIGIN.getX()), Math.abs(pos.getZ() - ORIGIN.getZ())));
            }
        }
        return reach;
    }
}
