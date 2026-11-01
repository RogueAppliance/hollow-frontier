package com.hollowfrontier.worldgen.feature;

import com.hollowfrontier.registry.ModBlocks;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * An ash vent (Ember Badlands): a field of grey ash 3-6 blocks round, thinning out at its edges, with glowing magma
 * showing through here and there, and in the middle a knee-high cone of basalt and blackstone with a smoking vent at its
 * top over a magma core. The origin is the block above the ground.
 */
public record AshVentFeature() implements Feature {
    public static final AshVentFeature INSTANCE = new AshVentFeature();
    public static final MapCodec<AshVentFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<AshVentFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!Shapes.ground(level.getBlockState(origin.below())) || !Shapes.free(level, origin)) {
            return false;
        }
        // The ash field
        int radius = 3 + random.nextInt(4);
        BlockState ash = ModBlocks.ASH.get().defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / radius;
                if (d > 1.0 || random.nextDouble() < d * d * 0.7) {
                    continue;
                }
                int y = Shapes.surface(level, origin.getX() + dx, origin.getZ() + dz, origin.getY() + 3, origin.getY() - 6);
                if (y != Integer.MIN_VALUE) {
                    BlockPos ground = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    level.setBlock(ground, d < 0.6 && random.nextInt(14) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : ash, 2);
                    // Anything growing there is buried
                    if (!level.getBlockState(ground.above()).isAir() && Shapes.free(level, ground.above())) {
                        level.setBlock(ground.above(), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // The cone: a ring of rock round a magma core, then a cross round the vent
        level.setBlock(origin.below(), Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
        level.setBlock(origin, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean corner = dx != 0 && dz != 0;
                if ((dx != 0 || dz != 0) && (!corner || random.nextBoolean())) {
                    Shapes.put(level, origin.offset(dx, 0, dz), rock(random));
                }
            }
        }
        for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            if (random.nextInt(10) < 7) {
                Shapes.put(level, origin.offset(d[0], 1, d[1]), rock(random));
            }
        }
        level.setBlock(origin.above(), ModBlocks.ASH_VENT.get().defaultBlockState(), 2);
        return true;
    }

    private static BlockState rock(RandomSource random) {
        return random.nextInt(3) == 0 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
    }
}
