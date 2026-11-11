package com.hollowfrontier.content.frontier.music;

import com.hollowfrontier.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Upright saloon piano. While PLAYING it plays Joplin rags in turn (our own renderings, see ASSETS.md), heard by anyone
 * within SaloonPianoBlockEntity.AUDIBLE_RANGE; near a playing piano the background music falls quiet. Right-click to start
 * or stop it.
 */
public class SaloonPianoBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty PLAYING = BooleanProperty.create("playing");
    /** Trouble near (21b): a brawl or a gunfight. The pianist stops mid-tune, and starts again once it's over. */
    public static final BooleanProperty HUSHED = BooleanProperty.create("hushed");
    /** December (35c): the server sets it, and the pianist works a carol in every other tune. */
    public static final BooleanProperty CAROLS = BooleanProperty.create("carols");
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);

    public SaloonPianoBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PLAYING, true).setValue(HUSHED, false).setValue(CAROLS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PLAYING, HUSHED, CAROLS);
    }

    // The keyboard faces whoever placed it.
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockState next = state.setValue(PLAYING, !state.getValue(PLAYING));
            level.setBlock(pos, next, Block.UPDATE_ALL);
            player.sendOverlayMessage(next.getValue(PLAYING)
                    ? Component.translatable("message.hollowfrontier.piano.playing")
                    : Component.translatable("message.hollowfrontier.piano.stopped"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SaloonPianoBlockEntity(pos, state);
    }

    // Music is client-side only: each client plays the piano for itself. The server only watches for trouble (HUSHED).
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SALOON_PIANO.get(),
                level.isClientSide() ? SaloonPianoBlockEntity::clientTick : SaloonPianoBlockEntity::serverTick);
    }
}
