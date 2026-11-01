package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.law.Wanted;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** "WANTED ★★☆" in the top-left corner while the player has heat (synced through the wanted attachment). */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class WantedHud {
    private WantedHud() {}

    private static final int GOLD = 0xFFE8C840;
    private static final int DIM = 0xFF6A5A3A;

    @SubscribeEvent
    static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT, HollowFrontier.id("wanted"), WantedHud::render);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int stars = Wanted.stars(mc.player);
        if (stars == 0) {
            return;
        }
        Component label = Component.translatable("hud.hollowfrontier.wanted");
        int x = 4, y = 4;
        g.text(mc.font, label, x, y, 0xFFD03030, true);
        x += mc.font.width(label) + 4;
        for (int i = 0; i < 3; i++) {
            g.text(mc.font, "★", x, y, i < stars ? GOLD : DIM, true);
            x += mc.font.width("★") + 1;
        }
    }
}
