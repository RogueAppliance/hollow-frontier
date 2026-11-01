package com.hollowfrontier.content.hollow.gateway;

import com.hollowfrontier.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds nothing: every block of an open well has one so the client can draw the gateway's beam of light from the middle
 * one (PolarPortalRenderer), the way a beacon draws its beam.
 */
public class PolarPortalBlockEntity extends BlockEntity {
    public PolarPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POLAR_PORTAL.get(), pos, state);
    }
}
