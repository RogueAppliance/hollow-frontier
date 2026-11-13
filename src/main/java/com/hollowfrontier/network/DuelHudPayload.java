package com.hollowfrontier.network;

import com.hollowfrontier.HollowFrontier;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client (51): where a duel stands, for the lines over the hotbar and the locked trigger. {@code kind} is one
 * of the constants; {@code name} the boss's; {@code ticks} what the kind needs (ASK: how long there is to answer; TOLL:
 * which of the three; DRAW: how much longer the gun stays locked, for a flinch). The handler is registered client-side
 * (DuelClient).
 */
public record DuelHudPayload(int kind, String name, int ticks) implements CustomPacketPayload {
    public static final int CLEAR = 0, ASK = 1, MARK = 2, TOLL = 3, DRAW = 4, WON = 5, LOST = 6;

    public static final Type<DuelHudPayload> TYPE = new Type<>(HollowFrontier.id("duel_hud"));
    public static final StreamCodec<ByteBuf, DuelHudPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DuelHudPayload::kind,
            ByteBufCodecs.STRING_UTF8, DuelHudPayload::name,
            ByteBufCodecs.VAR_INT, DuelHudPayload::ticks,
            DuelHudPayload::new);

    @Override
    public Type<DuelHudPayload> type() {
        return TYPE;
    }
}
