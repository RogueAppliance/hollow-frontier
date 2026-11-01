package com.hollowfrontier.network;

import com.hollowfrontier.HollowFrontier;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the player just came through a Polar Gateway. The client whites out the screen and lets the frost
 * melt off it (GatewayOverlay); {@code inside} is true going into the Hollow Earth, false coming home.
 * <p>
 * Going in, the player is at the top of Symmes' shaft: the client steers their fall (HollowDescent) down through the
 * crust (whose underside is at {@code undersideY}) to the return gateway's south stone at the target. With no fall, the
 * target y is {@link #NO_FALL}.
 */
public record GatewayCrossingPayload(boolean inside, double targetX, double targetY, double targetZ, int undersideY) implements CustomPacketPayload {
    public static final double NO_FALL = Double.NEGATIVE_INFINITY;
    public static final Type<GatewayCrossingPayload> TYPE = new Type<>(HollowFrontier.id("gateway_crossing"));
    public static final StreamCodec<ByteBuf, GatewayCrossingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GatewayCrossingPayload::inside,
            ByteBufCodecs.DOUBLE, GatewayCrossingPayload::targetX,
            ByteBufCodecs.DOUBLE, GatewayCrossingPayload::targetY,
            ByteBufCodecs.DOUBLE, GatewayCrossingPayload::targetZ,
            ByteBufCodecs.VAR_INT, GatewayCrossingPayload::undersideY,
            GatewayCrossingPayload::new);

    /** A crossing with no fall to steer (coming home, or into the Hollow Earth for a non-player). */
    public static GatewayCrossingPayload plain(boolean inside) {
        return new GatewayCrossingPayload(inside, 0.0, NO_FALL, 0.0, 0);
    }

    public boolean falling() {
        return targetY != NO_FALL;
    }

    @Override
    public Type<GatewayCrossingPayload> type() {
        return TYPE;
    }
}
