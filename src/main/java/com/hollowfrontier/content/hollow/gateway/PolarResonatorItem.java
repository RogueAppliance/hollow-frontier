package com.hollowfrontier.content.hollow.gateway;

import java.util.Optional;
import java.util.function.Consumer;

import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.registry.ModCriteria;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A steam-driven tuning fork the size of a coffee grinder. Use it on any stone of a Polar Gateway's ring: it spends
 * {@link #OPEN_STEAM} of steam from the canisters you carry, and the ring wakes stone by stone and opens the well a couple
 * of seconds later (GatewayEffects). Sneak-use on an open
 * gateway to shut it again (free). This is what keeps the Hollow Earth behind the steam age.
 */
public class PolarResonatorItem extends Item {
    public static final int OPEN_STEAM = 2_000;

    public PolarResonatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockState(context.getClickedPos()).getBlock() instanceof GatewayFrameBlock)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel server) || player == null) {
            return InteractionResult.SUCCESS;
        }
        Optional<BlockPos> ring = PolarGateways.ringCenter(level, context.getClickedPos());
        if (ring.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.broken_ring"));
            return InteractionResult.FAIL;
        }
        BlockPos center = ring.get();
        if (GatewayEffects.isWaking(level, center)) {
            return InteractionResult.FAIL; // already ringing
        }
        if (PolarGateways.isOpen(level, center)) {
            if (player.isSecondaryUseActive()) {
                PolarGateways.close(server, center);
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.closed"));
                return InteractionResult.SUCCESS;
            }
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.already_open"));
            return InteractionResult.FAIL;
        }
        if (!PolarGateways.wellClear(level, center)) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.blocked"));
            return InteractionResult.FAIL;
        }
        if (!SteamCanisterItem.hasSteam(player, OPEN_STEAM)) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.no_steam", OPEN_STEAM));
            return InteractionResult.FAIL;
        }
        SteamCanisterItem.drawFromInventory(player, OPEN_STEAM);
        GatewayEffects.awaken(server, center);
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.opened"));
        ModCriteria.fire(player, ModCriteria.GATEWAY_OPENED);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.polar_resonator", OPEN_STEAM).withStyle(ChatFormatting.GRAY));
    }
}
