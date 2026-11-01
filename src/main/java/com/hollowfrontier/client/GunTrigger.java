package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gun.GunItem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Holding right-click with a gun. Vanilla repeats a held use every 4 ticks, and every use that goes through drops the
 * first-person hand to raise it again, so a held gun bobbed all through a reload; and each chamber, once loaded, was
 * fired by the next repeat, so the reload never got past one chamber. Now a held click fans the gun until it's empty
 * and starts the reload, and from the moment the gun is seen loading until the button is let go, held repeats are
 * dropped: no use, no bob. A fresh click still fires mid-reload, as before.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class GunTrigger {
    private GunTrigger() {}

    // Whether use was down at the end of the last tick (so a use now is a held repeat, not a fresh click), and whether
    // a gun has been loading during this hold
    private static boolean heldLastTick;
    private static boolean loadedThisHold;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean down = minecraft.options.keyUse.isDown();
        Player player = minecraft.player;
        if (!down) {
            loadedThisHold = false;
        } else if (player != null && (GunItem.isReloading(player.getMainHandItem()) || GunItem.isReloading(player.getOffhandItem()))) {
            loadedThisHold = true;
        }
        heldLastTick = down;
    }

    @SubscribeEvent
    static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Player player = Minecraft.getInstance().player;
        if (!event.isUseItem() || !heldLastTick || player == null) {
            return;
        }
        ItemStack held = player.getItemInHand(event.getHand());
        if (held.getItem() instanceof GunItem && (loadedThisHold || GunItem.isReloading(held))) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
