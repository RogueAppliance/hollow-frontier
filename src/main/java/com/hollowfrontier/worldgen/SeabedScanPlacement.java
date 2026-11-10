package com.hollowfrontier.worldgen;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

/**
 * Finds the Still Sea's bed (43a). The ocean-floor heightmap is no use under the crust (its top is the roof of the world),
 * and {@link OpenAirScanPlacement} stops at water. This starts in the water just under the surface and goes down through
 * water (and whatever grows in it) to the first solid block, and places in the water on top of it. Nothing comes of a start
 * that isn't water (land, a sea stack) or of a bed that isn't solid.
 */
public record SeabedScanPlacement(int maxSteps) implements PlacementModifier {
    public static final MapCodec<SeabedScanPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.intRange(1, 4064).fieldOf("max_steps").forGetter(SeabedScanPlacement::maxSteps))
            .apply(i, SeabedScanPlacement::new));

    @Override
    public void modify(PlacementContext context, RandomSource random, BlockPos origin, Consumer<BlockPos> output) {
        WorldGenLevel level = context.getLevel();
        BlockPos.MutableBlockPos pos = origin.mutable();
        if (!wet(level.getBlockState(pos))) {
            return;
        }
        for (int i = 0; i < maxSteps; i++) {
            pos.move(Direction.DOWN);
            if (level.isOutsideBuildHeight(pos.getY())) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (!wet(state)) {
                if (state.isSolid()) {
                    output.accept(pos.above());
                }
                return;
            }
        }
    }

    /** Water, or something standing in water (kelp, seagrass). */
    public static boolean wet(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER) && !state.isSolid();
    }

    @Override
    public MapCodec<SeabedScanPlacement> codec() {
        return CODEC;
    }
}
