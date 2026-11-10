package com.hollowfrontier.content.hollow.sea;

import java.util.function.Consumer;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Harpoon and the Toothed Harpoon (43b). Use it to throw it (no drawing back, as with the lasso): a
 * {@link HarpoonEntity} that flies true underwater, holds what it hits on the end of its line, and comes back. Each throw
 * wears it by one; a harpoon on its last use won't be thrown. Held, it's a GeckoLib model (the renderer is made on the
 * client only, as the guns' is); in the inventory, in frames and on the ground it's its flat icon.
 */
public class HarpoonItem extends Item implements GeoItem {
    private static final float THROW_POWER = 2.2F;
    private static final int COOLDOWN = 15;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public HarpoonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.nextDamageWillBreak()) {
            return InteractionResult.FAIL;
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        if (level instanceof ServerLevel serverLevel) {
            stack.hurtWithoutBreaking(1, player);
            ItemStack thrown = stack.consumeAndReturn(1, player);
            HarpoonEntity harpoon = Projectile.spawnProjectileFromRotation(HarpoonEntity::new, serverLevel, thrown, player, 0.0F, THROW_POWER, 1.0F);
            if (player.hasInfiniteMaterials()) {
                harpoon.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.playSound(null, harpoon, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 0.85F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.hollowfrontier.harpoon.damage", (int) HarpoonEntity.jab(stack)).withStyle(ChatFormatting.DARK_GREEN));
        lines.accept(Component.translatable("item.hollowfrontier.harpoon.hint").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.hollowfrontier.harpoon.release").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private com.hollowfrontier.client.HarpoonItemRenderer renderer;

            @Override
            public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new com.hollowfrontier.client.HarpoonItemRenderer(HarpoonItem.this);
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
