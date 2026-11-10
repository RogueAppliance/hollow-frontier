package com.hollowfrontier.registry;

import java.util.function.Supplier;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.worldgen.ClearOfSitesFilter;
import com.hollowfrontier.worldgen.OpenAirScanPlacement;
import com.hollowfrontier.worldgen.SeabedScanPlacement;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.neoforged.neoforge.registries.DeferredRegister;

// Placement modifier types for the Hollow Earth's features (HollowFeatures).
public final class ModPlacements {
    private ModPlacements() {}

    public static final DeferredRegister<MapCodec<? extends PlacementModifier>> PLACEMENTS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, HollowFrontier.MODID);

    public static final Supplier<MapCodec<OpenAirScanPlacement>> OPEN_AIR_SCAN = PLACEMENTS.register("open_air_scan", () -> OpenAirScanPlacement.CODEC);
    public static final Supplier<MapCodec<ClearOfSitesFilter>> CLEAR_OF_SITES = PLACEMENTS.register("clear_of_sites", () -> ClearOfSitesFilter.CODEC);
    /** Down through water to the bed of the Still Sea (43a). */
    public static final Supplier<MapCodec<SeabedScanPlacement>> SEABED_SCAN = PLACEMENTS.register("seabed_scan", () -> SeabedScanPlacement.CODEC);
}
