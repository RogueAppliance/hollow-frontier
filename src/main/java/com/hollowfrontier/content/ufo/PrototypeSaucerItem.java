package com.hollowfrontier.content.ufo;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * A Prototype Saucer, folded (13c): crafted from eight alien alloy plates round a mended gravity drive, and set down on
 * the ground like the airship, facing the way you face. It keeps its fittings and its phlogiston on it
 * ({@link ModDataComponents#SAUCER_FITTINGS}, {@link ModDataComponents#PHLOGISTON_CHARGE}); a new one comes with the tractor beam and heat laser ({@link SaucerFittings#STANDARD}) and primed with
 * {@link #PRIMED_CHARGE}. Its cargo hold's contents ride on it too (vanilla's {@code container}), and whoever sets it down owns it
 * (15b). A landed, empty saucer folds back into this item (sneak and use it bare-handed).
 */
public class PrototypeSaucerItem extends Item {
    public static final int PRIMED_CHARGE = PhlogistonCellItem.CHARGES;

    public PrototypeSaucerItem(Properties properties) {
        super(properties);
    }

    public static SaucerFittings fittings(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SAUCER_FITTINGS.get(), SaucerFittings.STANDARD);
    }

    public static int charge(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PHLOGISTON_CHARGE.get(), PRIMED_CHARGE);
    }

    /** The saucer as an item, with its fittings and charge. */
    public static ItemStack of(SaucerFittings fittings, int charge) {
        ItemStack stack = new ItemStack(com.hollowfrontier.registry.ModItems.PROTOTYPE_SAUCER.get());
        stack.set(ModDataComponents.SAUCER_FITTINGS.get(), fittings);
        stack.set(ModDataComponents.PHLOGISTON_CHARGE.get(), charge);
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos at = context.getClickedFace() == Direction.UP ? clicked.above() : clicked.relative(context.getClickedFace());
        Player player = context.getPlayer();
        PrototypeSaucer saucer = place(level, at, player != null ? player.getYRot() : 0.0F, context.getItemInHand());
        if (saucer == null) {
            if (player != null && !level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.saucer.no_room").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            if (player != null) {
                saucer.setOwner(player);
            }
            level.addFreshEntity(saucer);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            level.playSound(null, at, SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8F, 1.4F);
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** A saucer from {@code stack} standing at {@code at}, not yet added to the level; null if there's no room for it there. */
    public static @Nullable PrototypeSaucer place(Level level, BlockPos at, float yaw, ItemStack stack) {
        PrototypeSaucer saucer = ModEntities.PROTOTYPE_SAUCER.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (saucer == null) {
            return null;
        }
        Vec3 pos = Vec3.atBottomCenterOf(at);
        saucer.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        if (!level.noCollision(saucer, saucer.getBoundingBox())) {
            return null;
        }
        saucer.outfit(fittings(stack), charge(stack), at);
        saucer.stow(stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
        return saucer;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.prototype_saucer.charge", charge(stack), PrototypeSaucer.MAX_CHARGE)
                .withStyle(ChatFormatting.AQUA));
        fittings(stack).describe(line -> builder.accept(line.copy().withStyle(ChatFormatting.GRAY)));
        builder.accept(Component.translatable("tooltip.hollowfrontier.prototype_saucer.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
