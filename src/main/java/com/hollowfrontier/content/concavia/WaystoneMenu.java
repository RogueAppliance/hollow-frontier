package com.hollowfrontier.content.concavia;

import java.util.List;

import com.hollowfrontier.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Where a waystone (or a crossed gateway's ring) can take a player (15c): a list of destinations, one button each. The
 * names go to the client with the menu; the screen's buttons come back through vanilla's {@code clickMenuButton}, and
 * the server carries the player to the one picked ({@link Waystones#travel}). It stays open only near where it was
 * opened.
 */
public class WaystoneMenu extends AbstractContainerMenu {
    public static final StreamCodec<RegistryFriendlyByteBuf, List<Component>> LABELS = ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list());
    public static final double REACH = 8.0;

    private final List<Component> labels;
    private final List<Waystones.Destination> destinations;
    private final BlockPos from;

    /** Client side: the names, as the server sent them. */
    public WaystoneMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        super(ModMenus.WAYSTONE.get(), containerId);
        this.labels = LABELS.decode(buf);
        this.destinations = List.of();
        this.from = BlockPos.ZERO;
    }

    /** Server side: these destinations, from the stone (or ring) at {@code from}. */
    public WaystoneMenu(int containerId, Inventory inventory, List<Waystones.Destination> destinations, BlockPos from) {
        super(ModMenus.WAYSTONE.get(), containerId);
        this.labels = destinations.stream().map(Waystones.Destination::label).toList();
        this.destinations = List.copyOf(destinations);
        this.from = from.immutable();
    }

    public List<Component> labels() {
        return labels;
    }

    public List<Waystones.Destination> destinations() {
        return destinations;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer traveller) || id < 0 || id >= destinations.size()) {
            return false;
        }
        Waystones.Destination to = destinations.get(id);
        traveller.closeContainer();
        return Waystones.travel(traveller, to);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && player.distanceToSqr(Vec3.atCenterOf(from)) <= REACH * REACH;
    }
}
