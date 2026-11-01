package com.hollowfrontier.content.hollow.creature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** What the Hollow Earth's small creatures (9b2) share. */
public final class HollowCreatures {
    private HollowCreatures() {}

    /**
     * Spawn rule: out on open ground in the light (the land under the crust is in full daylight, so this keeps them out of
     * caves), on any solid top that isn't water.
     */
    public static boolean onLitGround(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos.below(), Direction.UP) && below.getFluidState().isEmpty() && level.getRawBrightness(pos, 0) > 8;
    }

    /**
     * The ground's height under a column, looking down from {@code fromY} through air, leaves and plants for up to 96
     * blocks (in the Hollow Earth the heightmap's top is the crust, so heightmaps can't be used). {@code fromY - 96} if
     * nothing's found.
     */
    public static int groundBelow(LevelReader level, int x, int z, int fromY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, fromY, z);
        for (int i = 0; i < 96 && !level.isOutsideBuildHeight(pos.getY()); i++, pos.move(Direction.DOWN)) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.canBeReplaced() && !state.is(net.minecraft.tags.BlockTags.LEAVES)) {
                return pos.getY();
            }
        }
        return fromY - 96;
    }
}
