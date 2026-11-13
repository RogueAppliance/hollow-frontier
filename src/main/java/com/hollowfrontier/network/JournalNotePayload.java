package com.hollowfrontier.network;

import com.hollowfrontier.HollowFrontier;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client (48a): something was just written into the journal the player carries ({@code key}: the translation key of
 * what it was: a milestone's title, a creature's or a person's name, a strange doing). The client says "noted" with a pencil scratch, if its {@code journalNotes} option is
 * on; the handler is registered client-side (JournalClient).
 */
public record JournalNotePayload(String key) implements CustomPacketPayload {
    public static final Type<JournalNotePayload> TYPE = new Type<>(HollowFrontier.id("journal_note"));
    public static final StreamCodec<ByteBuf, JournalNotePayload> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(JournalNotePayload::new, JournalNotePayload::key);

    @Override
    public Type<JournalNotePayload> type() {
        return TYPE;
    }
}
