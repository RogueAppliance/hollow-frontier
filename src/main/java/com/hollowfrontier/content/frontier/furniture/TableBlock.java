package com.hollowfrontier.content.frontier.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wooden table (24c): a plank top on four legs, things set on it as on any block. The towns' tables, in place of a
 * fence post with a pressure plate. The shape matches the datagen model (ModModelProvider.TABLE).
 */
public class TableBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 13, 0, 16, 16, 16),
            Block.box(1, 0, 1, 3, 13, 3), Block.box(13, 0, 1, 15, 13, 3), Block.box(1, 0, 13, 3, 13, 15), Block.box(13, 0, 13, 15, 13, 15));

    public TableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
