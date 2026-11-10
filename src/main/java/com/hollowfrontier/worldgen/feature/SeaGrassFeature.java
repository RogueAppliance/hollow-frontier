package com.hollowfrontier.worldgen.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Seagrass on the Still Sea's bed (43a): short, or tall where there are two blocks of water for it, in the shallows only
 * (no deeper than {@link #MAX_DEPTH}). The origin is the water on the bed.
 */
public record SeaGrassFeature() implements Feature {
    public static final SeaGrassFeature INSTANCE = new SeaGrassFeature();
    public static final MapCodec<SeaGrassFeature> CODEC = MapCodec.unit(INSTANCE);
    public static final int MAX_DEPTH = 14;

    @Override
    public MapCodec<SeaGrassFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int depth = depth(level, origin, MAX_DEPTH + 1);
        if (depth < 1 || depth > MAX_DEPTH || !Blocks.SEAGRASS.defaultBlockState().canSurvive(level, origin) || onGlass(level, origin)) {
            return false;
        }
        if (depth >= 3 && random.nextInt(4) == 0) {
            BlockState tall = Blocks.TALL_SEAGRASS.defaultBlockState();
            level.setBlock(origin, tall.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.LOWER), 2);
            level.setBlock(origin.above(), tall.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER), 2);
        } else {
            level.setBlock(origin, Blocks.SEAGRASS.defaultBlockState(), 2);
        }
        return true;
    }

    /**
     * Whether {@code origin} stands on a Drowned Quarter's dome (43d): its glass, its crystal ribs or a lamp. Weed grows
     * over the Quarter's paving and walls, but a dome furred with it reads as a rock.
     */
    static boolean onGlass(WorldGenLevel level, BlockPos origin) {
        BlockState below = level.getBlockState(origin.below());
        return below.is(Blocks.GLASS) || below.is(com.hollowfrontier.registry.ModBlocks.CRYSTAL_BRICKS.get())
                || below.is(com.hollowfrontier.registry.ModBlocks.JELLY_LAMP.get());
    }

    /** How many blocks of open water stand at and above {@code origin}, counted up to {@code limit}; 0 if it isn't open water. */
    static int depth(WorldGenLevel level, BlockPos origin, int limit) {
        BlockPos.MutableBlockPos pos = origin.mutable();
        int depth = 0;
        while (depth < limit && level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource()) {
            depth++;
            pos.move(0, 1, 0);
        }
        return depth;
    }
}
