package com.hollowfrontier.worldgen;

import com.hollowfrontier.registry.ModFeatures;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;

/**
 * Keeps trees and the bigger flora and stonework out of any chunk a Hollow Earth lore site touches (9b3), so the sites
 * stand in a clearing instead of with a jungle tree through the roof. Features come after structures, which have already
 * marked every chunk they reach with a reference by then. Grass and flowers still grow there.
 */
public final class ClearOfSitesFilter implements PlacementFilter {
    public static final ClearOfSitesFilter INSTANCE = new ClearOfSitesFilter();
    public static final MapCodec<ClearOfSitesFilter> CODEC = MapCodec.unit(INSTANCE);

    private ClearOfSitesFilter() {}

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
        return context.getLevel().getChunk(origin).getAllReferences().entrySet().stream()
                .noneMatch(entry -> !entry.getValue().isEmpty() && entry.getKey().type() == ModFeatures.HOLLOW_SITE.get());
    }

    @Override
    public MapCodec<ClearOfSitesFilter> codec() {
        return CODEC;
    }
}
