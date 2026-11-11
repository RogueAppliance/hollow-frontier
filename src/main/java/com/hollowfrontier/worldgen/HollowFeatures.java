package com.hollowfrontier.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.worldgen.feature.AshVentFeature;
import com.hollowfrontier.worldgen.feature.CrystalOutcropFeature;
import com.hollowfrontier.worldgen.feature.CycadFeature;
import com.hollowfrontier.worldgen.feature.GiantFernFeature;
import com.hollowfrontier.worldgen.feature.SeaGrassFeature;
import com.hollowfrontier.worldgen.feature.SeaKelpFeature;
import com.hollowfrontier.worldgen.feature.StandingStoneFeature;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.material.Fluids;

/**
 * What grows and pours in the Hollow Earth: 9a's base layer of vanilla plants and trees, and each biome's own (9b1): tree
 * ferns and cycads in the Jungle, crystal outcrops on the Steppes, ash vents in the Badlands and standing stones in the
 * Highlands.
 * <p>
 * <b>Why not vanilla's placed features:</b> they find the surface from the heightmap, and here the heightmap's top is the
 * crust, so every tree would grow on the roof of the world. These place vanilla's features again, finding the ground by
 * starting just under the ceiling ({@link #UNDER_CEILING}) and scanning down through air to the first solid block
 * ({@link #TO_FLOOR}): the land, or the top of a floating island, never the crust. Water stops the scan, so nothing grows
 * on a lake. Phase 9b's features and structures should place the same way.
 * <p>
 * <b>Ores</b> (9c) are underground, so they use plain height ranges: starsteel deep everywhere and again in the Badlands' red
 * rock, phlogiston scarcer everywhere and common under the crystal country.
 * <p>
 * <b>Falls from the crust:</b> water springs in the crust's underside (and lava in the Badlands) that pour all the way down,
 * found by scanning up through air to the first solid block overhead.
 */
public final class HollowFeatures {
    private HollowFeatures() {}

    /** Where surface placements start: in the open air under the lowest the crust's underside sways to (spires aside). */
    public static final int SCAN_FROM = HollowEarth.CEILING_LOWEST - 12;
    public static final PlacementModifier UNDER_CEILING = HeightRangePlacement.of(ConstantHeight.of(VerticalAnchor.absolute(SCAN_FROM)));
    public static final PlacementModifier TO_FLOOR = new OpenAirScanPlacement(Direction.DOWN, SCAN_FROM - HollowEarth.MIN_Y);
    /** Falls start from here and look up for the crust (or a spire, or an island's underside). */
    public static final PlacementModifier MID_AIR = HeightRangePlacement.of(ConstantHeight.of(VerticalAnchor.absolute(184)));
    public static final PlacementModifier TO_CEILING = new OpenAirScanPlacement(Direction.UP, HollowEarth.TOP_Y - 184);

    public static final ResourceKey<Feature> CEILING_SPRING_WATER = feature("ceiling_spring_water");
    public static final ResourceKey<Feature> CEILING_SPRING_LAVA = feature("ceiling_spring_lava");
    // 9b1: each biome's own flora and stonework (worldgen/feature)
    public static final ResourceKey<Feature> GIANT_FERN = feature("giant_fern");
    public static final ResourceKey<Feature> CYCAD = feature("cycad");
    public static final ResourceKey<Feature> CRYSTAL_OUTCROP = feature("crystal_outcrop");
    public static final ResourceKey<Feature> ASH_VENT = feature("ash_vent");
    public static final ResourceKey<Feature> STANDING_STONE = feature("standing_stone");
    /** A Gray relay hung under the crust (27), rare, where the Grays work (jungle, steppes, badlands). */
    public static final ResourceKey<Feature> GRAY_RELAY = feature("gray_relay");
    /** About one relay in this many chunks of jungle, steppe or badland (where the ceiling over them is open enough). */
    public static final int GRAY_RELAY_RARITY = 90;
    // 9c: the Hollow Earth's ores
    public static final ResourceKey<Feature> ORE_STARSTEEL = feature("ore_starsteel");
    public static final ResourceKey<Feature> ORE_PHLOGISTON = feature("ore_phlogiston");

    public static final ResourceKey<PlacedFeature> JUNGLE_TREES = placed("jungle_trees");
    public static final ResourceKey<PlacedFeature> JUNGLE_BAMBOO = placed("jungle_bamboo");
    public static final ResourceKey<PlacedFeature> JUNGLE_GRASS = placed("jungle_grass");
    public static final ResourceKey<PlacedFeature> JUNGLE_FERNS = placed("jungle_ferns");
    public static final ResourceKey<PlacedFeature> JUNGLE_FLOWERS = placed("jungle_flowers");
    public static final ResourceKey<PlacedFeature> STEPPE_TALL_GRASS = placed("steppe_tall_grass");
    public static final ResourceKey<PlacedFeature> STEPPE_GRASS = placed("steppe_grass");
    public static final ResourceKey<PlacedFeature> STEPPE_BUSHES = placed("steppe_bushes");
    public static final ResourceKey<PlacedFeature> STEPPE_FLOWERS = placed("steppe_flowers");
    public static final ResourceKey<PlacedFeature> STEPPE_TREES = placed("steppe_trees");
    public static final ResourceKey<PlacedFeature> BADLANDS_DEAD_BUSHES = placed("badlands_dead_bushes");
    public static final ResourceKey<PlacedFeature> BADLANDS_DRY_GRASS = placed("badlands_dry_grass");
    public static final ResourceKey<PlacedFeature> BADLANDS_CACTUS = placed("badlands_cactus");
    public static final ResourceKey<PlacedFeature> HIGHLAND_TREES = placed("highland_trees");
    public static final ResourceKey<PlacedFeature> HIGHLAND_GRASS = placed("highland_grass");
    public static final ResourceKey<PlacedFeature> WATERFALLS_COMMON = placed("waterfalls_common");
    public static final ResourceKey<PlacedFeature> WATERFALLS_RARE = placed("waterfalls_rare");
    public static final ResourceKey<PlacedFeature> LAVAFALLS = placed("lavafalls");
    public static final ResourceKey<PlacedFeature> JUNGLE_GIANT_FERNS = placed("jungle_giant_ferns");
    public static final ResourceKey<PlacedFeature> JUNGLE_CYCADS = placed("jungle_cycads");
    public static final ResourceKey<PlacedFeature> STEPPE_CRYSTAL_OUTCROPS = placed("steppe_crystal_outcrops");
    public static final ResourceKey<PlacedFeature> BADLANDS_ASH_VENTS = placed("badlands_ash_vents");
    public static final ResourceKey<PlacedFeature> HIGHLAND_STANDING_STONES = placed("highland_standing_stones");
    public static final ResourceKey<PlacedFeature> CEILING_GRAY_RELAYS = placed("ceiling_gray_relays");
    public static final ResourceKey<PlacedFeature> STARSTEEL_ORE = placed("ore_starsteel");
    public static final ResourceKey<PlacedFeature> STARSTEEL_ORE_BADLANDS = placed("ore_starsteel_badlands");
    public static final ResourceKey<PlacedFeature> PHLOGISTON_ORE = placed("ore_phlogiston");
    public static final ResourceKey<PlacedFeature> PHLOGISTON_ORE_CRYSTAL = placed("ore_phlogiston_crystal");

    // 43a: the Still Sea's bed. Vanilla's kelp and seagrass find the bed by heightmap, which here is the crust, so these are ours.
    public static final ResourceKey<Feature> SEA_KELP = feature("sea_kelp");
    public static final ResourceKey<Feature> SEA_GRASS = feature("sea_grass");
    public static final ResourceKey<PlacedFeature> SEA_KELP_FORESTS = placed("sea_kelp_forests");
    public static final ResourceKey<PlacedFeature> SEA_GRASS_MEADOWS = placed("sea_grass_meadows");
    /** Seabed placements start just under the surface and go down through the water ({@link SeabedScanPlacement}). */
    public static final PlacementModifier UNDER_THE_SURFACE = HeightRangePlacement.of(ConstantHeight.of(VerticalAnchor.absolute(HollowEarth.SEA_LEVEL - 1)));
    public static final PlacementModifier TO_SEABED = new SeabedScanPlacement(HollowEarth.SEA_LEVEL - HollowEarth.MIN_Y);

    private static ResourceKey<Feature> feature(String name) {
        return ResourceKey.create(Registries.FEATURE, HollowFrontier.id("hollow_earth/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, HollowFrontier.id("hollow_earth/" + name));
    }

    // ---------------------------------------------------------------- biome hookup

    // Stonework and crystal go in before the plants (LOCAL_MODIFICATIONS), so grass grows up round it, not under it. Tree
    // ferns and cycads grow before the jungle trees, so they take their ground first and stand in the light.
    static void jungle(BiomeGenerationSettings.Builder gen) {
        ores(gen, STARSTEEL_ORE, PHLOGISTON_ORE);
        vegetation(gen, JUNGLE_GIANT_FERNS, JUNGLE_CYCADS, JUNGLE_TREES, JUNGLE_BAMBOO, JUNGLE_FERNS, JUNGLE_GRASS, JUNGLE_FLOWERS);
        gen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, CEILING_GRAY_RELAYS);
        falls(gen, WATERFALLS_COMMON);
    }

    static void steppes(BiomeGenerationSettings.Builder gen) {
        ores(gen, STARSTEEL_ORE, PHLOGISTON_ORE, PHLOGISTON_ORE_CRYSTAL);
        gen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, STEPPE_CRYSTAL_OUTCROPS);
        vegetation(gen, STEPPE_TREES, STEPPE_BUSHES, STEPPE_TALL_GRASS, STEPPE_GRASS, STEPPE_FLOWERS);
        gen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, CEILING_GRAY_RELAYS);
        falls(gen, WATERFALLS_RARE);
    }

    static void badlands(BiomeGenerationSettings.Builder gen) {
        ores(gen, STARSTEEL_ORE, STARSTEEL_ORE_BADLANDS, PHLOGISTON_ORE);
        gen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, BADLANDS_ASH_VENTS);
        vegetation(gen, BADLANDS_CACTUS, BADLANDS_DEAD_BUSHES, BADLANDS_DRY_GRASS);
        gen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, CEILING_GRAY_RELAYS);
        falls(gen, LAVAFALLS);
    }

    static void highlands(BiomeGenerationSettings.Builder gen) {
        ores(gen, STARSTEEL_ORE, PHLOGISTON_ORE, PHLOGISTON_ORE_CRYSTAL);
        gen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, HIGHLAND_STANDING_STONES);
        vegetation(gen, HIGHLAND_TREES, HIGHLAND_GRASS);
        falls(gen, WATERFALLS_RARE);
    }

    // The Still Sea (43a): kelp forests and seagrass on the bed, the usual ores under it, and falls pouring in from the crust
    static void sea(BiomeGenerationSettings.Builder gen) {
        ores(gen, STARSTEEL_ORE, PHLOGISTON_ORE);
        vegetation(gen, SEA_KELP_FORESTS, SEA_GRASS_MEADOWS);
        falls(gen, WATERFALLS_COMMON);
    }

    @SafeVarargs
    private static void ores(BiomeGenerationSettings.Builder gen, ResourceKey<PlacedFeature>... ores) {
        for (ResourceKey<PlacedFeature> ore : ores) {
            gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ore);
        }
    }

    @SafeVarargs
    private static void vegetation(BiomeGenerationSettings.Builder gen, ResourceKey<PlacedFeature>... features) {
        for (ResourceKey<PlacedFeature> feature : features) {
            gen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature);
        }
    }

    private static void falls(BiomeGenerationSettings.Builder gen, ResourceKey<PlacedFeature> feature) {
        gen.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, feature);
    }

    // ---------------------------------------------------------------- bootstraps

    public static void bootstrapFeatures(BootstrapContext<Feature> context) {
        // A spring in the underside of the crust: rock above and on all four sides, open air below
        HolderSet<Block> crust = HolderSet.direct(Block::builtInRegistryHolder, ModBlocks.CRUST_ROCK.get(), Blocks.STONE, Blocks.CALCITE);
        context.register(CEILING_SPRING_WATER, new SpringFeature(Fluids.WATER.defaultFluidState(), false, 4, 1, crust));
        context.register(CEILING_SPRING_LAVA, new SpringFeature(Fluids.LAVA.defaultFluidState(), false, 4, 1, crust));
        context.register(GIANT_FERN, GiantFernFeature.INSTANCE);
        context.register(CYCAD, CycadFeature.INSTANCE);
        context.register(CRYSTAL_OUTCROP, CrystalOutcropFeature.INSTANCE);
        context.register(ASH_VENT, AshVentFeature.INSTANCE);
        context.register(STANDING_STONE, StandingStoneFeature.INSTANCE);
        context.register(GRAY_RELAY, com.hollowfrontier.worldgen.feature.GrayRelayFeature.INSTANCE);
        context.register(SEA_KELP, SeaKelpFeature.INSTANCE);
        context.register(SEA_GRASS, SeaGrassFeature.INSTANCE);
        RuleTest stone = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslate = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        context.register(ORE_STARSTEEL, new OreFeature(List.of(
                BlockReplacement.replace(stone, ModBlocks.STARSTEEL_ORE.get().defaultBlockState()),
                BlockReplacement.replace(deepslate, ModBlocks.DEEPSLATE_STARSTEEL_ORE.get().defaultBlockState())), 6));
        context.register(ORE_PHLOGISTON, new OreFeature(List.of(
                BlockReplacement.replace(stone, ModBlocks.PHLOGISTON_ORE.get().defaultBlockState()),
                BlockReplacement.replace(deepslate, ModBlocks.DEEPSLATE_PHLOGISTON_ORE.get().defaultBlockState())), 4));
    }

    public static void bootstrapPlacements(BootstrapContext<PlacedFeature> context) {
        HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
        // Primordial Jungle: jungle trees, mega trees among them (thinned from 30 a chunk to 18 in 9b1, so the understory
        // isn't black and the tree ferns show), bamboo thickets, ferns and grass, a few flowers
        context.register(JUNGLE_TREES, tree(features.getOrThrow(VegetationFeatures.TREES_JUNGLE), PlacementUtils.countExtra(18, 0.1F, 1), Blocks.JUNGLE_SAPLING));
        context.register(JUNGLE_BAMBOO, clear(patch(features.getOrThrow(VegetationFeatures.BAMBOO_NO_PODZOL), 4, 1, 0)));
        context.register(JUNGLE_FERNS, patch(features.getOrThrow(VegetationFeatures.LARGE_FERN), 2, 1, 64));
        context.register(JUNGLE_GRASS, patch(features.getOrThrow(VegetationFeatures.GRASS_JUNGLE), 1, 12, 32));
        context.register(JUNGLE_FLOWERS, patch(features.getOrThrow(VegetationFeatures.FLOWER_DEFAULT), 12, 1, 48));
        // Crystal Steppes: knee-high grass to the horizon, a lone acacia now and then
        context.register(STEPPE_TREES, tree(features.getOrThrow(VegetationFeatures.TREES_SAVANNA), PlacementUtils.countExtra(0, 0.05F, 1), Blocks.ACACIA_SAPLING));
        context.register(STEPPE_BUSHES, clear(patch(features.getOrThrow(VegetationFeatures.BUSH), 6, 1, 16)));
        context.register(STEPPE_TALL_GRASS, patch(features.getOrThrow(VegetationFeatures.TALL_GRASS), 2, 1, 96));
        context.register(STEPPE_GRASS, patch(features.getOrThrow(VegetationFeatures.GRASS), 1, 6, 32));
        context.register(STEPPE_FLOWERS, patch(features.getOrThrow(VegetationFeatures.FLOWER_DEFAULT), 24, 1, 48));
        // Ember Badlands: scrub
        context.register(BADLANDS_CACTUS, clear(patch(features.getOrThrow(VegetationFeatures.CACTUS), 13, 1, 10)));
        context.register(BADLANDS_DEAD_BUSHES, patch(features.getOrThrow(VegetationFeatures.DEAD_BUSH), 1, 4, 4));
        context.register(BADLANDS_DRY_GRASS, patch(features.getOrThrow(VegetationFeatures.DRY_GRASS), 6, 1, 48));
        // Concavian Highlands: scattered wind-bent spruce and oak, short grass
        context.register(HIGHLAND_TREES, tree(features.getOrThrow(VegetationFeatures.TREES_WINDSWEPT_HILLS), PlacementUtils.countExtra(0, 0.25F, 2), Blocks.SPRUCE_SAPLING));
        context.register(HIGHLAND_GRASS, patch(features.getOrThrow(VegetationFeatures.TAIGA_GRASS), 1, 6, 32));
        // Falls from the crust
        context.register(WATERFALLS_COMMON, fall(features.getOrThrow(CEILING_SPRING_WATER), 24));
        context.register(WATERFALLS_RARE, fall(features.getOrThrow(CEILING_SPRING_WATER), 48));
        context.register(LAVAFALLS, fall(features.getOrThrow(CEILING_SPRING_LAVA), 40));
        // Each biome's own (9b1): tree ferns and cycads among the jungle trees; an outcrop, a vent or a standing stone
        // every few chunks
        context.register(JUNGLE_GIANT_FERNS, onGround(features.getOrThrow(GIANT_FERN), PlacementUtils.countExtra(2, 0.5F, 2)));
        context.register(JUNGLE_CYCADS, onGround(features.getOrThrow(CYCAD), PlacementUtils.countExtra(1, 0.5F, 2)));
        context.register(STEPPE_CRYSTAL_OUTCROPS, onGround(features.getOrThrow(CRYSTAL_OUTCROP), RarityFilter.onAverageOnceEvery(5)));
        context.register(BADLANDS_ASH_VENTS, onGround(features.getOrThrow(ASH_VENT), RarityFilter.onAverageOnceEvery(4)));
        context.register(HIGHLAND_STANDING_STONES, onGround(features.getOrThrow(STANDING_STONE), RarityFilter.onAverageOnceEvery(5)));
        context.register(CEILING_GRAY_RELAYS, new PlacedFeature(features.getOrThrow(GRAY_RELAY), List.of(RarityFilter.onAverageOnceEvery(GRAY_RELAY_RARITY),
                InSquarePlacement.spread(), MID_AIR, TO_CEILING, BiomeFilter.biome(), ClearOfSitesFilter.INSTANCE)));
        // The Still Sea's bed (43a): kelp in forests (thick in places and absent in others, by noise, as vanilla's), seagrass
        // all through the shallows. Each feature keeps to its own depth.
        context.register(SEA_KELP_FORESTS, new PlacedFeature(features.getOrThrow(SEA_KELP), List.of(NoiseBasedCountPlacement.of(90, 80.0, 0.0),
                InSquarePlacement.spread(), UNDER_THE_SURFACE, TO_SEABED, BiomeFilter.biome())));
        context.register(SEA_GRASS_MEADOWS, new PlacedFeature(features.getOrThrow(SEA_GRASS), List.of(CountPlacement.of(40),
                InSquarePlacement.spread(), UNDER_THE_SURFACE, TO_SEABED, BiomeFilter.biome())));
        // Starsteel deep everywhere (a little richer than diamond), and again in the red rock of the Badlands; phlogiston scarcer
        // everywhere, and common under the crystal country (Steppes and Highlands), where it comes up nearer the surface
        context.register(STARSTEEL_ORE, ore(features.getOrThrow(ORE_STARSTEEL), 8, HeightRangePlacement.triangle(VerticalAnchor.absolute(-56), VerticalAnchor.absolute(56))));
        context.register(STARSTEEL_ORE_BADLANDS, ore(features.getOrThrow(ORE_STARSTEEL), 6, HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(128))));
        context.register(PHLOGISTON_ORE, ore(features.getOrThrow(ORE_PHLOGISTON), 3, HeightRangePlacement.triangle(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(40))));
        context.register(PHLOGISTON_ORE_CRYSTAL, ore(features.getOrThrow(ORE_PHLOGISTON), 7, HeightRangePlacement.uniform(VerticalAnchor.absolute(16), VerticalAnchor.absolute(160))));
    }

    /** An ore, {@code count} veins a chunk at the heights given (underground, so the plain height range does). */
    private static PlacedFeature ore(Holder<Feature> ore, int count, PlacementModifier height) {
        return new PlacedFeature(ore, List.of(CountPlacement.of(count), InSquarePlacement.spread(), height, BiomeFilter.biome()));
    }

    /** One of our own features on the ground (or an island's top), not in a lore site's chunks; the feature checks what it stands on. */
    private static PlacedFeature onGround(Holder<Feature> feature, PlacementModifier count) {
        return new PlacedFeature(feature, List.of(count, InSquarePlacement.spread(), UNDER_CEILING, TO_FLOOR, OffsetPlacement.above(), BiomeFilter.biome(),
                ClearOfSitesFilter.INSTANCE));
    }

    /** Trees: on the ground (or an island), where their sapling could grow, not in a lore site's chunks. */
    private static PlacedFeature tree(Holder<Feature> feature, PlacementModifier count, Block sapling) {
        return new PlacedFeature(feature, List.of(count, InSquarePlacement.spread(), UNDER_CEILING, TO_FLOOR, OffsetPlacement.above(),
                BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(sapling)), BiomeFilter.biome(), ClearOfSitesFilter.INSTANCE));
    }

    /**
     * A patch of plants, as vanilla's: one spot on the ground (once every {@code rarity} chunks, {@code spots} times), then
     * {@code tries} plants scattered round it in the air. With tries 0 the feature is placed once at the spot.
     */
    private static PlacedFeature patch(Holder<Feature> feature, int rarity, int spots, int tries) {
        List<PlacementModifier> modifiers = new ArrayList<>();
        if (rarity > 1) {
            modifiers.add(RarityFilter.onAverageOnceEvery(rarity));
        }
        if (spots > 1) {
            modifiers.add(CountPlacement.of(spots));
        }
        modifiers.addAll(List.of(InSquarePlacement.spread(), UNDER_CEILING, TO_FLOOR, OffsetPlacement.above(), BiomeFilter.biome()));
        if (tries > 0) {
            modifiers.add(CountPlacement.of(tries));
            modifiers.add(OffsetPlacement.ofTriangle(7, 3));
            modifiers.add(BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE));
        }
        return new PlacedFeature(feature, modifiers);
    }

    /** A patch kept out of a lore site's chunks (bamboo, bushes, cactus: what would stand in the way). */
    private static PlacedFeature clear(PlacedFeature patch) {
        List<PlacementModifier> modifiers = new ArrayList<>(patch.placement());
        modifiers.add(ClearOfSitesFilter.INSTANCE);
        return new PlacedFeature(patch.feature(), modifiers);
    }

    /** A spring in whatever's overhead, once every {@code rarity} chunks. */
    private static PlacedFeature fall(Holder<Feature> spring, int rarity) {
        return new PlacedFeature(spring, List.of(RarityFilter.onAverageOnceEvery(rarity), InSquarePlacement.spread(), MID_AIR,
                TO_CEILING, BiomeFilter.biome()));
    }
}
