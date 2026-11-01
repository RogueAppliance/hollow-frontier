package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.ufo.PrototypeSaucer;
import com.hollowfrontier.content.ufo.SaucerFittings;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The Prototype Saucer pilot's laser sight (13c, after the playtest found the laser hard to aim): four orange corner
 * brackets round the middle of the screen, which is where a bolt lands ({@link PrototypeSaucer#aim}), and the laser's
 * heat as a bar under them, going from green through orange to red. It shows in third person too, where vanilla hides its
 * crosshair (the camera rides above the craft there, and the client sends where its middle points: {@link SaucerAim}).
 * Brighter while the laser key is held.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class SaucerHud {
    private SaucerHud() {}

    private static final int SIGHT = 0xFFFF8A40, SIGHT_DIM = 0xA0FF8A40, BAR_BACK = 0x80202020;

    @SubscribeEvent
    static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, HollowFrontier.id("saucer_sight"), SaucerHud::render);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof PrototypeSaucer saucer)
                || saucer.getFirstPassenger() != mc.player || (saucer.fittingsMask() & SaucerFittings.LENS) == 0) {
            return;
        }
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        int c = ModKeyMappings.SAUCER_LASER.isDown() ? SIGHT : SIGHT_DIM;
        int r = 9, arm = 4;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                int x = cx + sx * r, y = cy + sy * r;
                // Each corner: a short arm along x and one along y, pointing back toward the middle
                g.fill(Math.min(x, x - sx * arm), y, Math.max(x, x - sx * arm) + 1, y + 1, c);
                g.fill(x, Math.min(y, y - sy * arm), x + 1, Math.max(y, y - sy * arm) + 1, c);
            }
        }
        g.fill(cx, cy, cx + 1, cy + 1, c);
        // The heat bar
        int heat = Math.min(PrototypeSaucer.MAX_HEAT, saucer.heat());
        int w = 2 * r + 1, x0 = cx - r, y0 = cy + r + 4;
        g.fill(x0, y0, x0 + w, y0 + 2, BAR_BACK);
        int filled = Math.round((float) w * heat / PrototypeSaucer.MAX_HEAT);
        if (filled > 0) {
            int hot = heat >= 70 ? 0xFFE03020 : heat >= 35 ? 0xFFFF9030 : 0xFF70E080;
            g.fill(x0, y0, x0 + filled, y0 + 2, hot);
        }
    }
}
