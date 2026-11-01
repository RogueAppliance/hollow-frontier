package com.hollowfrontier.network;

import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.ufo.PrototypeSaucer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: a saucer pilot's weapon keys (the heat laser and the tractor beam, each held or not), sent when they
 * change, and while the laser's held, every tick, with where their sight points (the camera's middle, which in third
 * person isn't their eye line), so the bolts land in it.
 */
public record SaucerControlPayload(boolean laser, boolean beam, Optional<Vec3> aim) implements CustomPacketPayload {
    public static final Type<SaucerControlPayload> TYPE = new Type<>(HollowFrontier.id("saucer_control"));
    public static final StreamCodec<ByteBuf, SaucerControlPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SaucerControlPayload::laser, ByteBufCodecs.BOOL, SaucerControlPayload::beam,
            ByteBufCodecs.optional(Vec3.STREAM_CODEC), SaucerControlPayload::aim, SaucerControlPayload::new);

    @Override
    public Type<SaucerControlPayload> type() {
        return TYPE;
    }

    public static void handle(SaucerControlPayload payload, IPayloadContext context) {
        if (context.player().getVehicle() instanceof PrototypeSaucer saucer && saucer.getFirstPassenger() == context.player()) {
            saucer.setControls(payload.laser(), payload.beam());
            payload.aim().ifPresent(saucer::sight);
        }
    }
}
