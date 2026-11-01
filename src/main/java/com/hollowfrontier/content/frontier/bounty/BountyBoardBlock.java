package com.hollowfrontier.content.frontier.bounty;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.registry.ModBlockEntities;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Small Bounty Board (18): one block with four posters on each open side; the postings are {@link BountyBoards}', the
 * face {@link BountyBoardBlockEntity}'s, and using it {@link BoardUse}'s. The hanging Bounty Board ({@link BountyBoardEntity})
 * is the bigger one. Boards from before 18 had no block entity: one is made the first time the board is used or gets a
 * random tick.
 */
public class BountyBoardBlock extends Block implements EntityBlock {
    public BountyBoardBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BountyBoardBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != ModBlockEntities.BOUNTY_BOARD.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<BountyBoardBlockEntity>) BountyBoardBlockEntity::serverTick;
    }

    // An old board (no block entity yet) gets one, and puts up its posters
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BountyBoardBlockEntity board && board.face().shown().isEmpty()) {
            board.sync(level);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            BountyBoardBlockEntity board = level.getBlockEntity(pos) instanceof BountyBoardBlockEntity b ? b : null;
            if (board != null) {
                BoardUse.emptyHand(server, serverPlayer, board, BountyBoardBlockEntity.faceSpot(hit).orElse(null), Vec3.atCenterOf(pos));
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.LETTER_HOME.get())) {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
                Captives.deliverLetter(server, serverPlayer, stack, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (!stack.is(ModItems.OUTLAW_BADGE.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        BountyData bounty = stack.get(ModDataComponents.BOUNTY.get());
        if (bounty == null) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            BoardUse.turnIn(server, player, stack, bounty, level.getBlockEntity(pos) instanceof BountyBoardBlockEntity b ? b : null, Vec3.atCenterOf(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
