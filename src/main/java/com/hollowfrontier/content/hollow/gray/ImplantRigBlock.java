package com.hollowfrontier.content.hollow.gray;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The implant rig (20): the Grays' surgical machine, hung over the operating table in a Gray pen. It runs the procedure
 * ({@link ImplantRigBlockEntity}); breaking it stops one under way.
 */
public class ImplantRigBlock extends BaseEntityBlock {
    public ImplantRigBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImplantRigBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.IMPLANT_RIG.get(), ImplantRigBlockEntity::serverTick);
    }
}
