package com.hollowfrontier.network;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gun.GunItem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server: the reload key was pressed. The gun in hand (main hand first) starts loading, or stops if it already is.
public record ReloadGunPayload() implements CustomPacketPayload {
    public static final ReloadGunPayload INSTANCE = new ReloadGunPayload();
    public static final Type<ReloadGunPayload> TYPE = new Type<>(HollowFrontier.id("reload_gun"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadGunPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<ReloadGunPayload> type() {
        return TYPE;
    }

    public static void handle(ReloadGunPayload payload, IPayloadContext context) {
        Player player = context.player();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof GunItem gun) {
                if (GunItem.isReloading(stack)) {
                    gun.stopReload(stack);
                } else {
                    gun.startReload(player, stack);
                }
                return;
            }
        }
    }
}
