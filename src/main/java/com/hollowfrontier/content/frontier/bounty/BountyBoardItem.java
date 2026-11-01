package com.hollowfrontier.content.frontier.bounty;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Hangs a Bounty Board (18) on the side of a block, like a painting: three across if there's room, else two. */
public class BountyBoardItem extends Item {
    public BountyBoardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction face = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(face);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (face.getAxis().isVertical() || player != null && !player.mayUseItemAt(pos, face, stack)) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        Optional<BountyBoardEntity> board = BountyBoardEntity.create(level, pos, face);
        if (board.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide()) {
            board.get().playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, board.get().position());
            level.addFreshEntity(board.get());
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
