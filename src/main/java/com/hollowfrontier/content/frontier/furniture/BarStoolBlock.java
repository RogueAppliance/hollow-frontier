package com.hollowfrontier.content.frontier.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Round-ish saloon stool: a leather cushion on a seat on a single post. Right-click to sit (SeatEntity), sneak to get up.
// Shape matches the datagen model (ModModelProvider.STOOL).
public class BarStoolBlock extends Block implements SeatBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 11, 4, 12, 13, 12),
            Block.box(3, 9, 3, 13, 11, 13),
            Block.box(6.5, 0, 6.5, 9.5, 9, 9.5),
            Block.box(4, 0, 4, 12, 1, 12));

    public BarStoolBlock(Properties properties) {
        super(properties);
    }

    @Override
    public double seatHeight() {
        return SeatEntity.SEAT_HEIGHT;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
