package com.hollowfrontier.network;

import com.hollowfrontier.HollowFrontier;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Handlers run on the main thread (NeoForge default). Client-bound payloads get their handlers in client code.
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class ModNetwork {
    private ModNetwork() {}

    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToServer(ReloadGunPayload.TYPE, ReloadGunPayload.STREAM_CODEC, ReloadGunPayload::handle)
                .playToServer(SaucerControlPayload.TYPE, SaucerControlPayload.STREAM_CODEC, SaucerControlPayload::handle)
                .playToServer(SaucerHoldPayload.TYPE, SaucerHoldPayload.STREAM_CODEC, SaucerHoldPayload::handle)
                .playToServer(JournalRequestPayload.TYPE, JournalRequestPayload.STREAM_CODEC, JournalRequestPayload::handle)
                // The client's handler is registered client-side (GatewayOverlay), so no client class loads on a server.
                .playToClient(GatewayCrossingPayload.TYPE, GatewayCrossingPayload.STREAM_CODEC)
                .playToClient(PhantomSightingPayload.TYPE, PhantomSightingPayload.STREAM_CODEC)
                // 48a: the journal's pages, and a note of something just written in (JournalClient)
                .playToClient(JournalPagesPayload.TYPE, JournalPagesPayload.STREAM_CODEC)
                .playToClient(JournalNotePayload.TYPE, JournalNotePayload.STREAM_CODEC);
    }
}
