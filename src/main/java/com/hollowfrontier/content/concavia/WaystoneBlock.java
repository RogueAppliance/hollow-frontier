package com.hollowfrontier.content.concavia;

import java.util.List;

import com.hollowfrontier.content.hollow.gateway.GatewayLinks;
import com.hollowfrontier.content.hollow.gateway.PolarGateways;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Concavian waystone (15c): carved pale stone with runes that glow, standing on the dais of a fully restored Crystal
 * Ruins. Unbreakable, like the gateways' stones. Used, it wakes (if it hasn't: one set down by hand wakes the first time
 * it's used) and shows where the network can take you ({@link Waystones}).
 */
public class WaystoneBlock extends Block {
    public WaystoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer traveller) {
            Waystones network = Waystones.get(server);
            Waystones.Stone stone = network.at(level.dimension(), pos).orElseGet(() -> network.wake(level.dimension(), pos, pos.south(2)));
            open(traveller, network.fromStone(server, stone), pos, stone.label());
        }
        return InteractionResult.SUCCESS;
    }

    /** Opens the list of {@code destinations} from {@code from}, or says there's nowhere to go yet. */
    public static void open(ServerPlayer player, List<Waystones.Destination> destinations, BlockPos from, Component title) {
        if (destinations.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.waystone.alone").withStyle(ChatFormatting.GRAY));
            return;
        }
        List<Component> labels = destinations.stream().map(Waystones.Destination::label).toList();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new WaystoneMenu(id, inventory, destinations, from), title),
                buf -> WaystoneMenu.LABELS.encode(buf, labels));
    }

    /**
     * A phlogiston shard used on the ring of a gateway that's been crossed: the woken waystones it can take you to. Whether it was
     * such a ring.
     */
    public static boolean fromGateway(ServerLevel level, ServerPlayer player, BlockPos frame) {
        var center = PolarGateways.ringCenter(level, frame);
        if (center.isEmpty() || level.dimension() != Level.OVERWORLD || GatewayLinks.get(level).fromOverworld(center.get()).isEmpty()) {
            return false;
        }
        open(player, Waystones.get(level).fromGateway(), frame, Component.translatable("block.hollowfrontier.gateway_frame"));
        return true;
    }
}
