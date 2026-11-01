package com.hollowfrontier.content.steam;

import com.hollowfrontier.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Steam pipe. Arms connect to any neighbor exposing a fluid capability on the facing side
 * (other pipes, boilers, tanks, crushers, other mods' tanks). Vanilla PipeBlock provides the shapes.
 */
public class CopperPipeBlock extends PipeBlock implements EntityBlock {
    public CopperPipeBlock(Properties properties) {
        super(6.0F, properties);
        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), connects(context.getLevel(), context.getClickedPos(), dir));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects(level, pos, direction));
    }

    private static boolean connects(LevelReader level, BlockPos pos, Direction dir) {
        return level instanceof Level realLevel
                && realLevel.getCapability(Capabilities.Fluid.BLOCK, pos.relative(dir), dir.getOpposite()) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return SteamStorageBlockEntity.pipe(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != ModBlockEntities.COPPER_PIPE.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<SteamStorageBlockEntity>) SteamStorageBlockEntity::serverTick;
    }
}
