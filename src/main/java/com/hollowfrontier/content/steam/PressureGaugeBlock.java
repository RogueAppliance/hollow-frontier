package com.hollowfrontier.content.steam;

import com.hollowfrontier.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Reads the highest steam pressure among its six neighbors. Comparators get 0-15;
 * right-click shows the reading. Its block entity re-checks twice a second and pings comparators on change.
 */
public class PressureGaugeBlock extends BaseEntityBlock {
    public PressureGaugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            SteamNetwork.Pressure reading = PressureGaugeBlockEntity.read(level, pos);
            player.sendOverlayMessage(reading.capacity() <= 0
                    ? Component.translatable("message.hollowfrontier.gauge.none")
                    : Component.translatable("message.hollowfrontier.gauge.reading",
                            Math.round(reading.fraction() * 100), reading.amount(), reading.capacity()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof PressureGaugeBlockEntity gauge ? gauge.signal() : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PressureGaugeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.PRESSURE_GAUGE.get(), PressureGaugeBlockEntity::serverTick);
    }
}
