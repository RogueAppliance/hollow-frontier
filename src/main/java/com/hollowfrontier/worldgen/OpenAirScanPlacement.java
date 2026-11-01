package com.hollowfrontier.worldgen;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

/**
 * Like vanilla's environment scan, but for the Hollow Earth's long drops (vanilla's stops after 32 blocks): from a start
 * in open air, go up or down through air to the first solid block and place there. Stops with nothing on any other
 * block (water, plants), or if the start isn't air (inside a spire).
 */
public record OpenAirScanPlacement(Direction direction, int maxSteps) implements PlacementModifier {
    public static final MapCodec<OpenAirScanPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Direction.CODEC.fieldOf("direction").forGetter(OpenAirScanPlacement::direction),
                    Codec.intRange(1, 4064).fieldOf("max_steps").forGetter(OpenAirScanPlacement::maxSteps))
            .apply(i, OpenAirScanPlacement::new));

    @Override
    public void modify(PlacementContext context, RandomSource random, BlockPos origin, Consumer<BlockPos> output) {
        WorldGenLevel level = context.getLevel();
        BlockPos.MutableBlockPos pos = origin.mutable();
        if (!level.getBlockState(pos).isAir()) {
            return;
        }
        for (int i = 0; i < maxSteps; i++) {
            pos.move(direction);
            if (level.isOutsideBuildHeight(pos.getY())) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                if (state.isSolid()) {
                    output.accept(pos.immutable());
                }
                return;
            }
        }
    }

    @Override
    public MapCodec<OpenAirScanPlacement> codec() {
        return CODEC;
    }
}
