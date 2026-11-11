package com.hollowfrontier.content.frontier.gear;

import com.hollowfrontier.registry.ModSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Throws a LassoEntity. Hold use to wind it up (the loop whirls overhead, and is heard), let go to throw; a plain click
 * throws at once, as it always did, since the throw is the same however long it was wound. The thrown lasso is consumed
 * on a catch (it becomes the leash) and dropped where it lands on a miss.
 */
public class LassoItem extends Item {
    private static final float THROW_POWER = 1.6F;
    private static final int COOLDOWN = 20;
    /** Ticks to a turn of the loop: one swish each. */
    private static final int TURN = 7;

    public LassoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TRIDENT; // the arm up and back
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    /** 46b: a swish each time the loop comes round, a little higher as it gets up to speed. */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        int wound = getUseDuration(stack, user) - ticksRemaining;
        if (!level.isClientSide() && wound % TURN == 0) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.LASSO_WHIRL.get(), SoundSource.PLAYERS,
                    0.7F, Math.min(1.3F, 0.8F + wound * 0.012F));
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int ticksRemaining) {
        if (!(user instanceof Player player)) {
            return false;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6F, 0.8F);
        if (level instanceof ServerLevel serverLevel) {
            LassoEntity lasso = new LassoEntity(serverLevel, player, stack.copyWithCount(1));
            lasso.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_POWER, 1.0F);
            serverLevel.addFreshEntity(lasso);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        stack.consume(1, player);
        return true;
    }
}
