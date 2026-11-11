package com.hollowfrontier.content.frontier.stake;

import java.util.Optional;

import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.season.ChristmasParcelItem;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A mailbox (47b): a small barrel on a post. On a claim it's the owner's alone to open, and the post rider leaves the
 * day's Gazette in it each morning (and, in December, the owner's Christmas Parcel, 35b). Anywhere else it's a box.
 * It keeps a barrel's block entity (ModBlockEntities adds it to the barrel's blocks), so hoppers and other mods see a
 * plain container.
 */
public class MailboxBlock extends BarrelBlock {
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);
    /** How far from a claim's stake its mailbox is looked for. */
    public static final int SEARCH = 8;

    public MailboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && !player.isCreative()) {
            Optional<StakeData.Claim> claim = StakeData.get(server).containing(pos);
            if (claim.isPresent() && !claim.get().owner().equals(player.getUUID())) {
                player.sendOverlayMessage(Component.translatable(StakeText.MAIL_NOT_YOURS, claim.get().ownerName()));
                return InteractionResult.SUCCESS;
            }
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    // Its door faces whoever sets it down, never the sky
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** The mailbox of a claim, if it has one within {@link #SEARCH} of its stake and that ground is loaded. */
    public static Optional<Container> of(ServerLevel level, StakeData.Claim claim) {
        BlockPos stake = claim.stake();
        if (!level.isLoaded(stake)) {
            return Optional.empty();
        }
        for (BlockPos pos : BlockPos.betweenClosed(stake.offset(-SEARCH, -1, -SEARCH), stake.offset(SEARCH, 2, SEARCH))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(ModBlocks.MAILBOX.get()) && level.getBlockEntity(pos) instanceof Container box) {
                return Optional.of(box);
            }
        }
        return Optional.empty();
    }

    /** Puts a stack in the first free slot. Whether there was room. */
    public static boolean post(Container box, ItemStack stack) {
        for (int slot = 0; slot < box.getContainerSize(); slot++) {
            if (box.getItem(slot).isEmpty()) {
                box.setItem(slot, stack);
                box.setChanged();
                return true;
            }
        }
        return false;
    }

    /** The morning's delivery to every claim whose mailbox is loaded: one Gazette a day, if yesterday's has been taken in. */
    public static void deliver(ServerLevel level) {
        StakeData data = StakeData.get(level);
        long today = BountyBoards.day(level);
        for (StakeData.Claim claim : data.all()) {
            if (claim.mailDay() == today) {
                continue;
            }
            Optional<Container> box = of(level, claim);
            if (box.isEmpty()) {
                continue;
            }
            if (!box.get().hasAnyOf(java.util.Set.of(ModItems.FRONTIER_GAZETTE.get()))) {
                post(box.get(), new ItemStack(ModItems.FRONTIER_GAZETTE.get()));
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(claim.owner());
            if (owner != null && ChristmasParcelItem.due(level, owner) && post(box.get(), new ItemStack(ModItems.CHRISTMAS_PARCEL.get()))) {
                owner.setData(com.hollowfrontier.registry.ModAttachments.PARCEL_YEAR.get(), com.hollowfrontier.content.frontier.season.Season.year());
            }
            data.put(claim.mailed(today));
        }
    }
}
