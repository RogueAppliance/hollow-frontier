package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.steam.BoilerBlockEntity;
import com.hollowfrontier.content.steam.BoilerMenu;
import com.hollowfrontier.content.steam.CrusherBlockEntity;
import com.hollowfrontier.content.steam.CrusherMenu;
import com.hollowfrontier.registry.ModMenus;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Boiler and Crusher screens. Drawn entirely with flat fills (panel, slot wells, gauges), so they need
 * no GUI textures; swap to a texture blit once real art exists.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class SteamMachineScreens {
    private SteamMachineScreens() {}

    private static final int PANEL = 0xFFC6C6C6;
    private static final int EDGE = 0xFF555555;
    private static final int WELL = 0xFF8B8B8B;
    private static final int WATER = 0xFF3F76E4;
    private static final int STEAM = 0xFFE8E8F0;
    private static final int FIRE = 0xFFE07020;
    private static final int PROGRESS = 0xFFB08D57;

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.BOILER.get(), BoilerScreen::new);
        event.register(ModMenus.CRUSHER.get(), CrusherScreen::new);
    }

    private static void panel(GuiGraphicsExtractor g, AbstractContainerScreen<?> screen, AbstractContainerMenu menu, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        for (Slot slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, WELL);
        }
    }

    /** Vertical gauge filling from the bottom. */
    private static void gauge(GuiGraphicsExtractor g, int x, int y, int w, int h, float fraction, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        g.fill(x, y, x + w, y + h, WELL);
        int filled = Math.round(h * Math.max(0, Math.min(1, fraction)));
        g.fill(x, y + h - filled, x + w, y + h, color);
    }

    static class BoilerScreen extends AbstractContainerScreen<BoilerMenu> {
        BoilerScreen(BoilerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 176, 166);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            super.extractBackground(g, mouseX, mouseY, a);
            int x = leftPos;
            int y = topPos;
            panel(g, this, menu, x, y, imageWidth, imageHeight);
            gauge(g, x + 30, y + 18, 12, 52, (float) menu.water() / BoilerBlockEntity.WATER_CAPACITY, WATER);
            gauge(g, x + 134, y + 18, 12, 52, (float) menu.steam() / BoilerBlockEntity.STEAM_CAPACITY, STEAM);
            gauge(g, x + 81, y + 34, 14, 14, menu.burnFraction(), FIRE);
        }
    }

    static class CrusherScreen extends AbstractContainerScreen<CrusherMenu> {
        CrusherScreen(CrusherMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 176, 166);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            super.extractBackground(g, mouseX, mouseY, a);
            int x = leftPos;
            int y = topPos;
            panel(g, this, menu, x, y, imageWidth, imageHeight);
            gauge(g, x + 152, y + 18, 12, 52, (float) menu.steam() / CrusherBlockEntity.STEAM_CAPACITY, STEAM);
            int arrow = Math.round(24 * menu.progress());
            g.fill(x + 79, y + 41, x + 103, y + 45, WELL);
            g.fill(x + 79, y + 41, x + 79 + arrow, y + 45, PROGRESS);
        }
    }
}
