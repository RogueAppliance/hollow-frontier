package com.hollowfrontier.network;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.journal.Journal;
import com.hollowfrontier.content.journal.JournalText;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server (48a): the journal key was pressed, or a thread was pinned or unpinned on the open screen. The
 * server answers with {@link JournalPagesPayload}, to a player who carries a journal. {@code pin}: {@link #OPEN} to
 * change nothing, a thread's name to pin it, "" to unpin.
 */
public record JournalRequestPayload(String pin) implements CustomPacketPayload {
    public static final String OPEN = "?";
    public static final Type<JournalRequestPayload> TYPE = new Type<>(HollowFrontier.id("journal_request"));
    public static final StreamCodec<ByteBuf, JournalRequestPayload> STREAM_CODEC = ByteBufCodecs.stringUtf8(32).map(JournalRequestPayload::new, JournalRequestPayload::pin);

    @Override
    public Type<JournalRequestPayload> type() {
        return TYPE;
    }

    public static void handle(JournalRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (!Journal.carried(player)) {
                player.sendOverlayMessage(Component.translatable(JournalText.NOT_CARRIED));
                return;
            }
            if (!payload.pin().equals(OPEN)) {
                Journal.pin(player, payload.pin());
            }
            Journal.open(player);
        }
    }
}
