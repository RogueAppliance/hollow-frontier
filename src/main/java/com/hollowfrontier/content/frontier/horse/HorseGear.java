package com.hollowfrontier.content.frontier.horse;

import java.util.stream.Stream;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Horseshoes and saddlebags on tamed horses (main hand only):
 * - right-click with horseshoes: fit them (old pair drops),
 * - right-click with saddlebags: attach 18 slots of storage,
 * - sneak + right-click empty-handed: open the saddlebags,
 * - sneak + right-click with shears: strip all gear (drops shoes, bags and contents).
 * Gear also drops when the horse dies.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class HorseGear {
    private HorseGear() {}

    private static final Identifier SHOE_MODIFIER = HollowFrontier.id("horseshoes");

    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getTarget() instanceof AbstractHorse horse) || !horse.isTamed()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        boolean handled;
        if (stack.getItem() instanceof HorseshoeItem shoes) {
            handled = fitShoes(horse, player, stack, shoes);
        } else if (stack.is(ModItems.SADDLEBAGS.get())) {
            handled = attachSaddlebags(horse, player, stack);
        } else if (player.isSecondaryUseActive() && stack.is(Items.SHEARS)) {
            handled = stripGear(horse, player);
        } else if (player.isSecondaryUseActive() && stack.isEmpty() && horse.hasData(ModAttachments.SADDLEBAGS.get())) {
            if (!player.level().isClientSide()) {
                SaddlebagContainer bags = SaddlebagContainer.of(horse);
                player.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x2, id, inventory, bags, 2),
                        Component.translatable("container.hollowfrontier.saddlebags")));
            }
            handled = true;
        } else {
            handled = false;
        }
        if (handled) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean fitShoes(AbstractHorse horse, Player player, ItemStack stack, HorseshoeItem shoes) {
        int current = horse.getData(ModAttachments.HORSESHOE_TIER.get());
        if (current == shoes.tier()) {
            return false;
        }
        if (player.level() instanceof ServerLevel level) {
            dropShoes(horse, level);
            horse.setData(ModAttachments.HORSESHOE_TIER.get(), shoes.tier());
            applyShoeModifiers(horse, shoes);
            horse.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.0F);
            ModCriteria.fire(player, ModCriteria.HORSE_SHOD, shoes.tier());
        }
        stack.consume(1, player);
        return true;
    }

    private static boolean attachSaddlebags(AbstractHorse horse, Player player, ItemStack stack) {
        if (horse.hasData(ModAttachments.SADDLEBAGS.get())) {
            return false;
        }
        if (!player.level().isClientSide()) {
            horse.setData(ModAttachments.SADDLEBAGS.get(), ItemContainerContents.EMPTY);
            horse.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
        }
        stack.consume(1, player);
        return true;
    }

    private static boolean stripGear(AbstractHorse horse, Player player) {
        boolean hasGear = horse.getData(ModAttachments.HORSESHOE_TIER.get()) > 0 || horse.hasData(ModAttachments.SADDLEBAGS.get());
        if (hasGear && player.level() instanceof ServerLevel level) {
            dropAllGear(horse, level);
            horse.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
        }
        return hasGear;
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse && horse.level() instanceof ServerLevel level) {
            dropAllGear(horse, level);
        }
    }

    private static void dropAllGear(AbstractHorse horse, ServerLevel level) {
        dropShoes(horse, level);
        if (horse.hasData(ModAttachments.SADDLEBAGS.get())) {
            SaddlebagContainer.forget(horse);
            horse.getData(ModAttachments.SADDLEBAGS.get()).nonEmptyItemCopyStream().forEach(item -> horse.spawnAtLocation(level, item));
            horse.removeData(ModAttachments.SADDLEBAGS.get());
            horse.spawnAtLocation(level, ModItems.SADDLEBAGS.get());
        }
    }

    private static void dropShoes(AbstractHorse horse, ServerLevel level) {
        int tier = horse.getData(ModAttachments.HORSESHOE_TIER.get());
        if (tier <= 0) {
            return;
        }
        Item shoes = shoesForTier(tier);
        if (shoes != null) {
            horse.spawnAtLocation(level, shoes);
        }
        horse.setData(ModAttachments.HORSESHOE_TIER.get(), 0);
        removeShoeModifiers(horse);
    }

    private static Item shoesForTier(int tier) {
        return Stream.of(ModItems.IRON_HORSESHOES.get(), ModItems.BRASS_HORSESHOES.get(), ModItems.STARSTEEL_HORSESHOES.get())
                .filter(item -> item.tier() == tier).findFirst().orElse(null);
    }

    // Permanent modifiers are saved with the horse, matching the saved HORSESHOE_TIER attachment.
    private static void applyShoeModifiers(AbstractHorse horse, HorseshoeItem shoes) {
        removeShoeModifiers(horse);
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jump = horse.getAttribute(Attributes.JUMP_STRENGTH);
        if (speed != null) {
            speed.addPermanentModifier(new AttributeModifier(SHOE_MODIFIER, shoes.speedBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (jump != null) {
            jump.addPermanentModifier(new AttributeModifier(SHOE_MODIFIER, shoes.jumpBonus(), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void removeShoeModifiers(AbstractHorse horse) {
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jump = horse.getAttribute(Attributes.JUMP_STRENGTH);
        if (speed != null) {
            speed.removeModifier(SHOE_MODIFIER);
        }
        if (jump != null) {
            jump.removeModifier(SHOE_MODIFIER);
        }
    }
}
