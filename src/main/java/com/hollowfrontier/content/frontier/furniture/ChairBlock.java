package com.hollowfrontier.content.frontier.furniture;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wooden chair (24c): four legs, a seat and a ladder back, facing the way its sitter faces (placed facing you).
 * Right-click to sit (SeatEntity), sneak to get up. The towns' chairs, in place of the stairs they used before. The
 * shape matches the datagen model (ModModelProvider.CHAIR), drawn facing north with the back to the south.
 */
public class ChairBlock extends HorizontalDirectionalBlock implements SeatBlock {
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
            Block.box(2, 7, 2, 14, 9, 14),
            Block.box(2, 9, 12, 14, 16, 14),
            Block.box(2, 0, 2, 4, 7, 4), Block.box(12, 0, 2, 14, 7, 4), Block.box(2, 0, 12, 4, 7, 14), Block.box(12, 0, 12, 14, 7, 14)));
    public static final double SEAT = 9.0 / 16.0;

    public ChairBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public double seatHeight() {
        return SEAT;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSecondaryUseActive() || player.isPassenger() || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            SeatEntity.sit(level, pos, player);
        }
        return InteractionResult.SUCCESS;
    }
}
