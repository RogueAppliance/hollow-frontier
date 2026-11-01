package com.hollowfrontier.content.steam;

import com.hollowfrontier.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Pressure-equalizing steam transfer. Each tick a steam holder (boiler, pipe, tank) pushes to every
 * neighbor with a fluid capability whose fill fraction ("pressure") is lower than its own, moving at
 * most half the pressure difference so pipes settle instead of oscillating. Consumers drain their own
 * tanks, which lowers their pressure and draws steam down the line. Each holder keeps its six
 * neighbor lookups cached ({@link Neighbours}) rather than repeating them every tick.
 */
public final class SteamNetwork {
    private SteamNetwork() {}

    public static void push(Neighbours neighbours, FluidTank source, int maxPerSide) {
        FluidResource steam = ModFluids.steam();
        for (Direction dir : Direction.values()) {
            int available = source.amount();
            if (available <= 0) {
                return;
            }
            ResourceHandler<FluidResource> target = neighbours.get(dir);
            if (target == null || target == source) {
                continue;
            }
            Pressure theirs = pressure(target, steam);
            if (theirs.capacity <= 0) {
                continue;
            }
            double myPressure = (double) available / source.capacity();
            double gap = myPressure - theirs.fraction();
            if (gap <= 0) {
                continue;
            }
            int amount = (int) Math.min(maxPerSide, Math.ceil(gap * Math.min(source.capacity(), theirs.capacity) / 2.0));
            amount = Math.min(amount, available);
            if (amount <= 0) {
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int accepted = target.insert(steam, amount, tx);
                if (accepted > 0 && source.extract(0, steam, accepted, tx) == accepted) {
                    tx.commit();
                }
            }
        }
    }

    /** Steam held and steam capacity across all slots of a handler that can hold steam. */
    public static Pressure pressure(ResourceHandler<FluidResource> handler, FluidResource steam) {
        long amount = 0;
        long capacity = 0;
        for (int i = 0; i < handler.size(); i++) {
            FluidResource held = handler.getResource(i);
            if (held.isEmpty() ? handler.isValid(i, steam) : ModFluids.isSteam(held.getFluid())) {
                amount += handler.getAmountAsLong(i);
                capacity += handler.getCapacityAsLong(i, steam);
            }
        }
        return new Pressure((int) Math.min(Integer.MAX_VALUE, amount), (int) Math.min(Integer.MAX_VALUE, capacity));
    }

    /**
     * The fluid handlers on a holder's six sides, each a NeoForge capability cache (dropped by NeoForge when the block
     * there changes or its chunk unloads). Made on the holder's first server tick, since a cache needs the ServerLevel.
     */
    public static final class Neighbours {
        private final ServerLevel level;
        private final BlockPos pos;
        @SuppressWarnings("unchecked")
        private final BlockCapabilityCache<ResourceHandler<FluidResource>, Direction>[] caches = new BlockCapabilityCache[6];

        public Neighbours(ServerLevel level, BlockPos pos) {
            this.level = level;
            this.pos = pos.immutable();
        }

        public ResourceHandler<FluidResource> get(Direction dir) {
            BlockCapabilityCache<ResourceHandler<FluidResource>, Direction> cache = caches[dir.ordinal()];
            if (cache == null) {
                cache = caches[dir.ordinal()] = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, pos.relative(dir), dir.getOpposite());
            }
            return cache.getCapability();
        }
    }

    public record Pressure(int amount, int capacity) {
        public double fraction() {
            return capacity <= 0 ? 0 : (double) amount / capacity;
        }
    }
}
