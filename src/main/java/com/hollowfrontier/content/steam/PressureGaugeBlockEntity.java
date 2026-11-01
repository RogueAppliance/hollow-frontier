package com.hollowfrontier.content.steam;

import com.hollowfrontier.registry.ModBlockEntities;
import com.hollowfrontier.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public class PressureGaugeBlockEntity extends BlockEntity {
    private int signal;

    public PressureGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESSURE_GAUGE.get(), pos, state);
    }

    public int signal() {
        return signal;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PressureGaugeBlockEntity be) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        SteamNetwork.Pressure reading = read(level, pos);
        int newSignal = reading.capacity() <= 0 ? 0 : Mth.ceil(reading.fraction() * 15);
        if (newSignal != be.signal) {
            be.signal = newSignal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    /** The neighbor with the highest steam fill fraction. */
    public static SteamNetwork.Pressure read(Level level, BlockPos pos) {
        FluidResource steam = ModFluids.steam();
        SteamNetwork.Pressure best = new SteamNetwork.Pressure(0, 0);
        for (Direction dir : Direction.values()) {
            ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, pos.relative(dir), dir.getOpposite());
            if (handler != null) {
                SteamNetwork.Pressure p = SteamNetwork.pressure(handler, steam);
                if (p.capacity() > 0 && (best.capacity() == 0 || p.fraction() > best.fraction())) {
                    best = p;
                }
            }
        }
        return best;
    }
}
