package com.hollowfrontier.content.frontier.stake;

import java.util.Optional;

import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Claim Stake by an Old Place's gate (47b): a post that says whose the place is. A deed used on it takes the place
 * ({@link OldPlace#claim}); an empty hand reads it. Only its owner can pull it up, which gives the place up. It has no
 * item: stakes come with the place.
 */
public class ClaimStakeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

    public ClaimStakeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.DEED.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        TownRef deed = stack.get(ModDataComponents.TOWN.get());
        if (deed != null && level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            OldPlace.claim(server, serverPlayer, pos, stack, deed);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            Optional<StakeData.Claim> claim = StakeData.get(server).at(pos);
            if (claim.isPresent()) {
                serverPlayer.sendOverlayMessage(Component.translatable(StakeText.STAKE_OWNED, claim.get().ownerName(), Herds.count(server, claim.get())));
            } else {
                Optional<Long> town = Towns.near(server, pos, StakeConfig.claimReach()).map(TownSite::keyOf);
                serverPlayer.sendOverlayMessage(town.isPresent() ? Component.translatable(StakeText.FOR_SALE, Towns.name(server, town.get()))
                        : Component.translatable(StakeText.FOR_SALE_NOWHERE));
            }
        }
        return InteractionResult.SUCCESS;
    }

    // Nobody but the owner pulls up a stake that's been taken
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter getter, BlockPos pos) {
        if (player.level() instanceof ServerLevel server) {
            Optional<StakeData.Claim> claim = StakeData.get(server).at(pos);
            if (claim.isPresent() && !claim.get().owner().equals(player.getUUID()) && !player.isCreative()) {
                return 0.0F;
            }
        }
        return super.getDestroyProgress(state, player, getter, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        StakeData.get(level).remove(pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
