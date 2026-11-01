package com.hollowfrontier.content.frontier.gear;

import com.hollowfrontier.registry.ModSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Lights the fuse and throws a DynamiteEntity (lobbed, not as far as a snowball). Handy for blast mining.
public class DynamiteItem extends Item {
    private static final float THROW_POWER = 1.0F;
    private static final int COOLDOWN = 15;

    public DynamiteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DYNAMITE_FUSE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            DynamiteEntity dynamite = new DynamiteEntity(serverLevel, player, stack.copyWithCount(1));
            dynamite.shootFromRotation(player, player.getXRot(), player.getYRot(), -10.0F, THROW_POWER, 1.0F);
            serverLevel.addFreshEntity(dynamite);
        }
        stack.consume(1, player);
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        return InteractionResult.SUCCESS;
    }
}
