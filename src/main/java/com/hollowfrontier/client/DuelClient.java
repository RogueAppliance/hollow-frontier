package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.duel.DuelText;
import com.hollowfrontier.content.frontier.duel.Duels;
import com.hollowfrontier.network.DuelAnswerPayload;
import com.hollowfrontier.network.DuelHudPayload;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * The client's end of high noon (51): the question over the hotbar and its two keys, the count, the word, and how it
 * came out, all as plain text (so shader packs leave it alone) and each read out by the narrator as it comes up. While
 * the count runs the trigger is locked here too ({@link Duels#clientHolstered}), so a pull doesn't cost the gun its
 * cooldown on this side, and the eyes are kept down from the horizon: hand over the holster, him at the top of the view.
 * At the word they're let go.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class DuelClient {
    private DuelClient() {}

    /** How far below level the eyes are held until the draw, in degrees. */
    public static final float EYES_DOWN = 18.0F;
    private static final int LINE_TICKS = 70, DRAW_TICKS = 30;
    private static final int PAPER = 0xFFF2E6C8, RED = 0xFFE03A2A, GOLD = 0xFFE8C840, DIM = 0xFFB8A888;

    private static int kind = DuelHudPayload.CLEAR;
    private static String name = "";
    // What the kind counts: ticks left to answer, which toll, ticks the gun is still locked after the draw
    private static int ticks;
    // Ticks this line has been up
    private static int shown;

    @SubscribeEvent
    static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(DuelHudPayload.TYPE, (payload, context) -> context.enqueueWork(() -> show(payload)));
    }

    @SubscribeEvent
    static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT, HollowFrontier.id("duel"), DuelClient::render);
    }

    /** What the server says of the duel now (also the photo rig's way to put a line up). */
    public static void show(DuelHudPayload payload) {
        kind = payload.kind();
        name = payload.name();
        ticks = payload.ticks();
        shown = 0;
        relock();
        Component line = line();
        if (line != null) {
            Minecraft.getInstance().getNarrator().saySystemNow(line);
        }
    }

    private static void relock() {
        Duels.clientHolstered = kind == DuelHudPayload.MARK || kind == DuelHudPayload.TOLL || kind == DuelHudPayload.DRAW && shown < ticks;
    }

    private static boolean eyesDown() {
        return kind == DuelHudPayload.MARK || kind == DuelHudPayload.TOLL;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean yes = false, no = false;
        while (ModKeyMappings.DUEL_YES.consumeClick()) {
            yes = true;
        }
        while (ModKeyMappings.DUEL_NO.consumeClick()) {
            no = true;
        }
        if (mc.player == null) {
            clear();
            return;
        }
        if (kind == DuelHudPayload.CLEAR || mc.isPaused()) {
            return;
        }
        if (kind == DuelHudPayload.ASK && (yes || no) && mc.gui.screen() == null) {
            ClientPacketDistributor.sendToServer(new DuelAnswerPayload(yes));
            clear();
            return;
        }
        shown++;
        if (kind == DuelHudPayload.ASK && shown > ticks
                || (kind == DuelHudPayload.WON || kind == DuelHudPayload.LOST) && shown > LINE_TICKS
                || kind == DuelHudPayload.DRAW && shown > Math.max(DRAW_TICKS, ticks)) {
            clear();
            return;
        }
        relock();
    }

    @SubscribeEvent
    static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        kind = DuelHudPayload.CLEAR;
        Duels.clientHolstered = false;
    }

    /** Until the word, the eyes stay down from the horizon. Held every frame, so the view never jumps. */
    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !eyesDown()) {
            return;
        }
        if (mc.player.getXRot() < EYES_DOWN) {
            mc.player.setXRot(EYES_DOWN);
            mc.player.xRotO = Math.max(mc.player.xRotO, EYES_DOWN);
        }
        if (event.getPitch() < EYES_DOWN) {
            event.setPitch(EYES_DOWN);
        }
    }

    private static Component line() {
        return switch (kind) {
            case DuelHudPayload.ASK -> Component.translatable(DuelText.ASK, name);
            case DuelHudPayload.MARK -> Component.translatable(DuelText.MARK);
            case DuelHudPayload.TOLL -> Component.translatable(DuelText.TOLLS[Math.clamp(ticks, 1, 3) - 1]);
            case DuelHudPayload.DRAW -> Component.translatable(DuelText.DRAW);
            case DuelHudPayload.WON -> Component.translatable(DuelText.WON, name);
            case DuelHudPayload.LOST -> Component.translatable(DuelText.LOST, name);
            default -> null;
        };
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Component line = line();
        if (mc.player == null || line == null) {
            return;
        }
        int width = g.guiWidth(), y = g.guiHeight() / 2 + 22; // under the crosshair, clear of the chat
        if (kind == DuelHudPayload.DRAW) {
            if (shown > DRAW_TICKS) {
                return;
            }
            float scale = 3.0F;
            g.pose().pushMatrix();
            g.pose().translate(width / 2.0F, g.guiHeight() / 2.0F - 40);
            g.pose().scale(scale, scale);
            g.text(mc.font, line, -mc.font.width(line) / 2, 0, RED, true);
            g.pose().popMatrix();
            return;
        }
        int colour = kind == DuelHudPayload.WON ? GOLD : kind == DuelHudPayload.LOST ? RED : PAPER;
        g.text(mc.font, line, (width - mc.font.width(line)) / 2, y, colour, true);
        if (kind == DuelHudPayload.ASK) {
            Component keys = Component.translatable(DuelText.ASK_KEYS, ModKeyMappings.DUEL_YES.getTranslatedKeyMessage(),
                    ModKeyMappings.DUEL_NO.getTranslatedKeyMessage(), Math.max(0, (ticks - shown + 19) / 20));
            g.text(mc.font, keys, (width - mc.font.width(keys)) / 2, y + 12, DIM, true);
        }
    }
}
