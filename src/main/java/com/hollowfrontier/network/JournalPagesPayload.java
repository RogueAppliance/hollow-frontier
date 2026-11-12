package com.hollowfrontier.network;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.journal.Journal;
import com.hollowfrontier.content.journal.JournalData;
import com.hollowfrontier.content.journal.JournalSeen;
import com.hollowfrontier.content.journal.JournalTowns;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client (48a): the journal as it reads now, in answer to {@link JournalRequestPayload} or to the item being
 * used. The client opens the journal's screen on it, or brings an open one up to date; the handler is registered
 * client-side (JournalClient).
 */
public record JournalPagesPayload(List<Journal.Thread> threads, String pinned, List<JournalSeen.Creature> creatures, List<JournalData.Strange> strange,
        int strangeCount, boolean tallyPaid, List<JournalSeen.Person> people, List<JournalTowns.Town> towns) implements CustomPacketPayload {
    public static final Type<JournalPagesPayload> TYPE = new Type<>(HollowFrontier.id("journal_pages"));
    public static final StreamCodec<ByteBuf, JournalPagesPayload> STREAM_CODEC = StreamCodec.composite(
            Journal.Thread.STREAM_CODEC.apply(ByteBufCodecs.list()), JournalPagesPayload::threads,
            ByteBufCodecs.STRING_UTF8, JournalPagesPayload::pinned,
            JournalSeen.Creature.STREAM_CODEC.apply(ByteBufCodecs.list()), JournalPagesPayload::creatures,
            JournalData.Strange.STREAM_CODEC.apply(ByteBufCodecs.list()), JournalPagesPayload::strange,
            ByteBufCodecs.VAR_INT, JournalPagesPayload::strangeCount,
            ByteBufCodecs.BOOL, JournalPagesPayload::tallyPaid,
            JournalSeen.Person.STREAM_CODEC.apply(ByteBufCodecs.list()), JournalPagesPayload::people,
            JournalTowns.Town.STREAM_CODEC.apply(ByteBufCodecs.list()), JournalPagesPayload::towns,
            JournalPagesPayload::new);

    @Override
    public Type<JournalPagesPayload> type() {
        return TYPE;
    }
}
