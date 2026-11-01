package com.hollowfrontier.content.frontier.gear;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Throws a LassoEntity. The thrown lasso is consumed on a catch (it becomes the leash) and dropped where it lands on a miss.
public class LassoItem extends Item {
    private static final float THROW_POWER = 1.6F;
    private static final int COOLDOWN = 20;

    public LassoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6F, 0.8F);
        if (level instanceof ServerLevel serverLevel) {
            LassoEntity lasso = new LassoEntity(serverLevel, player, stack.copyWithCount(1));
            lasso.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_POWER, 1.0F);
            serverLevel.addFreshEntity(lasso);
        }
        stack.consume(1, player);
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        return InteractionResult.SUCCESS;
    }
}
