package com.hollowfrontier.content.frontier.gear;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Full cowboy set (Snakeskin Boots count as the boots): the horse you ride gets +20% movement speed. Applied as a transient
 * (unsaved) modifier on the mount, refreshed every half second and removed on dismount.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class CowboySetBonus {
    private CowboySetBonus() {}

    private static final Identifier MODIFIER_ID = HollowFrontier.id("cowboy_set_mount_speed");
    private static final AttributeModifier MOUNT_SPEED = new AttributeModifier(MODIFIER_ID, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    public static boolean hasFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.COWBOY_HAT.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.DUSTER.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.CHAPS.get())
                && (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.COWBOY_BOOTS.get()) || SnakeskinBoots.isWornBy(player));
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.tickCount % 10 == 0) {
            update(player);
        }
    }

    /** Applies or removes the mount bonus for the player's current horse. */
    public static void update(Player player) {
        if (player.getVehicle() instanceof AbstractHorse horse) {
            AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null) {
                return;
            }
            if (hasFullSet(player)) {
                speed.addOrUpdateTransientModifier(MOUNT_SPEED);
            } else {
                speed.removeModifier(MODIFIER_ID);
            }
        }
    }

    @SubscribeEvent
    static void onDismount(EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityBeingMounted() instanceof AbstractHorse horse) {
            AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(MODIFIER_ID);
            }
        }
    }
}
