package com.hollowfrontier.content.steam;

import com.hollowfrontier.registry.ModBlockEntities;
import com.hollowfrontier.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Copper pipe (small buffer, fast) and steam tank (big buffer): hold steam and push it to lower-pressure neighbors. */
public class SteamStorageBlockEntity extends BlockEntity {
    public static final int PIPE_CAPACITY = 500;
    public static final int TANK_CAPACITY = 16_000;

    private final FluidTank tank;
    private final int rate;
    private SteamNetwork.Neighbours neighbours;

    public SteamStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int rate) {
        super(type, pos, state);
        this.tank = FluidTank.steam(capacity, this::setChanged);
        this.rate = rate;
    }

    public static SteamStorageBlockEntity pipe(BlockPos pos, BlockState state) {
        return new SteamStorageBlockEntity(ModBlockEntities.COPPER_PIPE.get(), pos, state, PIPE_CAPACITY, 200);
    }

    public static SteamStorageBlockEntity tank(BlockPos pos, BlockState state) {
        return new SteamStorageBlockEntity(ModBlockEntities.STEAM_TANK.get(), pos, state, TANK_CAPACITY, 400);
    }

    public FluidTank tank() {
        return tank;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamStorageBlockEntity be) {
        if (be.neighbours == null) {
            be.neighbours = new SteamNetwork.Neighbours((ServerLevel) level, pos);
        }
        SteamNetwork.push(be.neighbours, be.tank, be.rate);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("steam"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("steam"));
    }
}
