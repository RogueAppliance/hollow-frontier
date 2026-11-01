package com.hollowfrontier.network;

import com.hollowfrontier.HollowFrontier;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client (21b): the moonshine is talking. The client puts an airship sighting (11a) in its own sky, seen by
 * nobody else, crossing on {@code angle} (radians); the handler is registered client-side (TipsyClient).
 */
public record PhantomSightingPayload(float angle) implements CustomPacketPayload {
    public static final Type<PhantomSightingPayload> TYPE = new Type<>(HollowFrontier.id("phantom_sighting"));
    public static final StreamCodec<ByteBuf, PhantomSightingPayload> STREAM_CODEC = ByteBufCodecs.FLOAT.map(PhantomSightingPayload::new, PhantomSightingPayload::angle);

    @Override
    public Type<PhantomSightingPayload> type() {
        return TYPE;
    }
}
