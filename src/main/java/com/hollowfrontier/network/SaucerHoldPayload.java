package com.hollowfrontier.network;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.ufo.PrototypeSaucer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server (15b): the hold key was pressed aboard a saucer. Its cargo hold opens, or the gauge line says why not.
public record SaucerHoldPayload() implements CustomPacketPayload {
    public static final SaucerHoldPayload INSTANCE = new SaucerHoldPayload();
    public static final Type<SaucerHoldPayload> TYPE = new Type<>(HollowFrontier.id("saucer_hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaucerHoldPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<SaucerHoldPayload> type() {
        return TYPE;
    }

    public static void handle(SaucerHoldPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof PrototypeSaucer saucer) {
            String refused = saucer.openHold(player);
            if (refused != null) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.saucer." + refused, saucer.ownerName()).withStyle(ChatFormatting.RED));
            }
        }
    }
}
