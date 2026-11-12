package com.hollowfrontier.content.ufo.taken;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Gray field (50): the pane of light across a pen's front and across a doorway once the ship's alarm is up. It's seen
 * through, it stops anything that walks, it can't be broken, and it's gone when the ship lets it go ({@link Ship}).
 */
public class GrayFieldBlock extends TransparentBlock {
    public GrayFieldBlock(Properties properties) {
        super(properties);
    }

    // Nothing to see of it from inside another pane, like glass
    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbour, Direction direction) {
        return neighbour.is(this) || super.skipRendering(state, neighbour, direction);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
