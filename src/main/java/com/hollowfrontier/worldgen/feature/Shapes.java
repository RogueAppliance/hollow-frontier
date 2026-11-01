package com.hollowfrontier.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/** What the Hollow Earth's flora and stonework features share: where they may build, and where the ground is. */
final class Shapes {
    private Shapes() {}

    /** Air, or something a plant could grow through (grass, flowers, snow), but never water or lava. */
    static boolean free(BlockState state) {
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    static boolean free(WorldGenLevel level, BlockPos pos) {
        return free(level.getBlockState(pos));
    }

    /** Sets the block only where it's {@link #free}. */
    static boolean put(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!free(level, pos)) {
            return false;
        }
        level.setBlock(pos, state, 2);
        return true;
    }

    /** Solid ground: a full, dry block (not leaves, not a plant). */
    static boolean ground(BlockState state) {
        return state.isSolid() && state.getFluidState().isEmpty() && !state.canBeReplaced();
    }

    /**
     * The ground under a column near a feature's origin: scanning down from {@code fromY} to {@code toY}, the first solid
     * block with open air (or a plant) over it. {@link Integer#MIN_VALUE} if there's none (a cliff, water, a cave).
     */
    static int surface(WorldGenLevel level, int x, int z, int fromY, int toY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, fromY, z);
        boolean open = free(level.getBlockState(pos));
        for (int y = fromY - 1; y >= toY; y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (open && ground(state)) {
                return y;
            }
            if (!state.getFluidState().isEmpty()) {
                return Integer.MIN_VALUE;
            }
            open = free(state);
        }
        return Integer.MIN_VALUE;
    }
}
