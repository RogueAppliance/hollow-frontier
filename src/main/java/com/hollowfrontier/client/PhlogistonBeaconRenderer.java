package com.hollowfrontier.client;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.ufo.PhlogistonBeaconBlock;
import com.hollowfrontier.content.ufo.PhlogistonBeaconBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A lit Phlogiston Beacon's beam (13d): violet, up into the sky with vanilla's beacon-beam code, thickening with distance like a
 * beacon's so it can be seen from far off while the Mothership is called and fought.
 */
public class PhlogistonBeaconRenderer implements BlockEntityRenderer<PhlogistonBeaconBlockEntity, PhlogistonBeaconRenderer.State> {
    static final int COLOR = 0xFFB070FF;

    public static class State extends BlockEntityRenderState {
        boolean lit;
        float animationTime;
        float radiusScale;
    }

    public PhlogistonBeaconRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PhlogistonBeaconBlockEntity beacon, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(beacon, state, partialTicks, cameraPosition, breakProgress);
        Level level = beacon.getLevel();
        state.lit = beacon.getBlockState().getValue(PhlogistonBeaconBlock.LIT);
        state.animationTime = level != null ? Math.floorMod(level.getGameTime(), 40) + partialTicks : 0.0F;
        float distance = (float) cameraPosition.subtract(Vec3.atCenterOf(state.blockPos)).horizontalDistance();
        var player = Minecraft.getInstance().player;
        state.radiusScale = player != null && player.isScoping() ? 1.0F : Math.max(1.0F, distance / 96.0F);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.lit) {
            BeaconRenderer.submitBeaconBeam(poseStack, collector, BeaconRenderer.BEAM_LOCATION, 1.0F, state.animationTime, 1,
                    BeaconRenderer.MAX_RENDER_Y, COLOR, 0.2F * state.radiusScale, 0.3F * state.radiusScale);
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }

    @Override
    public AABB getRenderBoundingBox(PhlogistonBeaconBlockEntity beacon) {
        BlockPos pos = beacon.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, BeaconRenderer.MAX_RENDER_Y, pos.getZ() + 1);
    }
}
