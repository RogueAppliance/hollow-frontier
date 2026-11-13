package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.journal.Journal;
import com.hollowfrontier.content.journal.JournalText;
import com.hollowfrontier.network.JournalNotePayload;
import com.hollowfrontier.network.JournalPagesPayload;
import com.hollowfrontier.network.JournalRequestPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * The client's end of the Prospector's Journal (48a): the key asks the server for the book while one is carried, the
 * answer opens {@link JournalScreen} (or brings an open one up to date), and a note of something just written in is a
 * line over the hotbar and a pencil scratch, unless {@code journalNotes} is off.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class JournalClient {
    private JournalClient() {}

    @SubscribeEvent
    static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(JournalPagesPayload.TYPE, (payload, context) -> context.enqueueWork(() -> show(payload)));
        event.register(JournalNotePayload.TYPE, (payload, context) -> context.enqueueWork(() -> noted(payload.key())));
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ModKeyMappings.JOURNAL.consumeClick()) {
            if (mc.player == null || mc.gui.screen() != null) {
                continue;
            }
            if (Journal.carried(mc.player)) {
                ClientPacketDistributor.sendToServer(new JournalRequestPayload(JournalRequestPayload.OPEN));
            } else {
                mc.player.sendOverlayMessage(Component.translatable(JournalText.NOT_CARRIED));
            }
        }
    }

    static void show(JournalPagesPayload pages) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof JournalScreen open) {
            open.update(pages);
        } else if (mc.gui.screen() == null) {
            mc.gui.setScreen(new JournalScreen(pages));
        }
    }

    static void noted(String key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && ClientConfig.journalNotes()) {
            mc.player.sendOverlayMessage(Component.translatable(JournalText.NOTED, Component.translatable(key)));
            mc.player.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.5F, 1.5F);
        }
    }
}
