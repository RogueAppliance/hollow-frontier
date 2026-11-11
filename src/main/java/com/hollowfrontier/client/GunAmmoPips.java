package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/**
 * What's in a black powder gun, shown on its slot: a row of small pips along the top, one for each chamber or barrel,
 * brass for a loaded one and dark for an empty one. A loaded gun and an empty one were the same picture before (the
 * creative tab lists both, side by side), and the only way to tell was the tooltip. The phlogiston guns have no chambers
 * (they draw on cells), so they get none.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class GunAmmoPips {
    private GunAmmoPips() {}

    private static final int LOADED = 0xFFE8B830, LOADED_SHADE = 0xFFA87A18, EMPTY = 0xFF2A2A2E;

    @SubscribeEvent
    static void register(RegisterItemDecorationsEvent event) {
        event.register(ModItems.REVOLVER.get(), GunAmmoPips::render);
        event.register(ModItems.PLAINS_RIFLE.get(), GunAmmoPips::render);
        event.register(ModItems.SHOTGUN.get(), GunAmmoPips::render);
    }

    private static boolean render(GuiGraphicsExtractor g, Font font, ItemStack stack, int x, int y) {
        if (!(stack.getItem() instanceof GunItem gun) || gun.stats().magazine() <= 0) {
            return false;
        }
        int chambers = gun.stats().magazine(), loaded = GunItem.getAmmo(stack);
        for (int i = 0; i < chambers; i++) {
            int left = x + 1 + i * 2, top = y + 1;
            if (i < loaded) {
                g.fill(left, top, left + 1, top + 2, LOADED);
                g.fill(left, top + 2, left + 1, top + 3, LOADED_SHADE);
            } else {
                g.fill(left, top, left + 1, top + 3, EMPTY);
            }
        }
        return false;
    }
}
