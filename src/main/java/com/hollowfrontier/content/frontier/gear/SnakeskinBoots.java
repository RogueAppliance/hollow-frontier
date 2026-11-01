package com.hollowfrontier.content.frontier.gear;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Snakeskin Boots (Cowboy Boots + 2 Snakeskin): rattlesnakes won't strike the wearer unless provoked, their bite
 * doesn't poison, and the wearer walks 15% faster on sand. They count as cowboy boots for the set bonus.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class SnakeskinBoots {
    private SnakeskinBoots() {}

    private static final Identifier SAND_SPEED_ID = HollowFrontier.id("snakeskin_sand_speed");
    private static final AttributeModifier SAND_SPEED = new AttributeModifier(SAND_SPEED_ID, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    public static boolean isWornBy(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SNAKESKIN_BOOTS.get());
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.tickCount % 5 == 0) {
            update(player);
        }
    }

    /** Adds or removes the sand speed bonus (public so game tests can drive it; fake players don't tick). */
    public static void update(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (isWornBy(player) && player.onGround() && player.getBlockStateOn().is(BlockTags.SAND)) {
            speed.addOrUpdateTransientModifier(SAND_SPEED);
        } else {
            speed.removeModifier(SAND_SPEED_ID);
        }
    }
}
