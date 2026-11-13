package com.hollowfrontier.network;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.duel.Duels;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server (51): the answer to a boss's call to the street. Ignored unless that player has been asked. */
public record DuelAnswerPayload(boolean yes) implements CustomPacketPayload {
    public static final Type<DuelAnswerPayload> TYPE = new Type<>(HollowFrontier.id("duel_answer"));
    public static final StreamCodec<ByteBuf, DuelAnswerPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(DuelAnswerPayload::new, DuelAnswerPayload::yes);

    @Override
    public Type<DuelAnswerPayload> type() {
        return TYPE;
    }

    public static void handle(DuelAnswerPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Duels.answer(player, payload.yes());
        }
    }
}
