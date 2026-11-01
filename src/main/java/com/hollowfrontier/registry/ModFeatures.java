package com.hollowfrontier.registry;

import java.util.function.Supplier;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.worldgen.feature.AshVentFeature;
import com.hollowfrontier.worldgen.feature.CrystalOutcropFeature;
import com.hollowfrontier.worldgen.feature.CycadFeature;
import com.hollowfrontier.worldgen.feature.GiantFernFeature;
import com.hollowfrontier.worldgen.feature.StandingStoneFeature;
import com.hollowfrontier.worldgen.structure.HollowSiteStructure;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredRegister;

// Feature types for the Hollow Earth's own flora and stonework (HollowFeatures places them), and the structure type of its
// lore sites (9b3), which finds the ground under the crust.
public final class ModFeatures {
    private ModFeatures() {}

    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES =
            DeferredRegister.create(Registries.FEATURE_TYPE, HollowFrontier.MODID);

    public static final Supplier<MapCodec<GiantFernFeature>> GIANT_FERN = FEATURE_TYPES.register("giant_fern", () -> GiantFernFeature.CODEC);
    public static final Supplier<MapCodec<CycadFeature>> CYCAD = FEATURE_TYPES.register("cycad", () -> CycadFeature.CODEC);
    public static final Supplier<MapCodec<CrystalOutcropFeature>> CRYSTAL_OUTCROP = FEATURE_TYPES.register("crystal_outcrop", () -> CrystalOutcropFeature.CODEC);
    public static final Supplier<MapCodec<AshVentFeature>> ASH_VENT = FEATURE_TYPES.register("ash_vent", () -> AshVentFeature.CODEC);
    public static final Supplier<MapCodec<StandingStoneFeature>> STANDING_STONE = FEATURE_TYPES.register("standing_stone", () -> StandingStoneFeature.CODEC);
    /** A Gray relay hung under the crust (27). */
    public static final Supplier<MapCodec<com.hollowfrontier.worldgen.feature.GrayRelayFeature>> GRAY_RELAY = FEATURE_TYPES.register("gray_relay", () -> com.hollowfrontier.worldgen.feature.GrayRelayFeature.CODEC);

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, HollowFrontier.MODID);

    public static final Supplier<StructureType<HollowSiteStructure>> HOLLOW_SITE = STRUCTURE_TYPES.register("hollow_site", () -> () -> HollowSiteStructure.CODEC);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, HollowFrontier.MODID);
    /** A dungeon room buried under a Hollow site (26), which leaves the land round it alone. */
    public static final Supplier<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> BURIED_PIECE =
            STRUCTURE_PIECES.register("buried", () -> (net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType) com.hollowfrontier.worldgen.structure.BuriedPiece::new);
    public static final DeferredRegister<MapCodec<? extends net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor>> STRUCTURE_PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, HollowFrontier.MODID);
    /** Writes each town's name on its welcome arches as it's built (24a). */
    public static final Supplier<MapCodec<com.hollowfrontier.worldgen.structure.TownNameProcessor>> TOWN_NAME_PROCESSOR =
            STRUCTURE_PROCESSORS.register("town_name", () -> com.hollowfrontier.worldgen.structure.TownNameProcessor.CODEC);
    /** The towns: a jigsaw town only where the ground is dry and near level (FrontierTownStructure). */
    public static final Supplier<StructureType<com.hollowfrontier.worldgen.structure.FrontierTownStructure>> FRONTIER_TOWN =
            STRUCTURE_TYPES.register("frontier_town", () -> () -> com.hollowfrontier.worldgen.structure.FrontierTownStructure.CODEC);
}
