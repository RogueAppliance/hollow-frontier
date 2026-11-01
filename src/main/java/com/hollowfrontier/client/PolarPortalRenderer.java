package com.hollowfrontier.client;

import com.hollowfrontier.content.hollow.gateway.PolarPortalBlockEntity;
import com.hollowfrontier.registry.ModBlocks;
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
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An open Polar Gateway's beam: a pale shaft of light standing up out of the middle of the well into the sky, drawn with
 * vanilla's beacon-beam code and render types (so shader packs handle it like a beacon), wider than a beacon's to fill the
 * well. Only the well's middle block draws it; seen from far off it marks where the gateway is.
 */
public class PolarPortalRenderer implements BlockEntityRenderer<PolarPortalBlockEntity, PolarPortalRenderer.State> {
    static final int COLOR = 0xFFC8F4FF;
    static final float SOLID_RADIUS = 0.55F;
    static final float GLOW_RADIUS = 0.75F;

    public static class State extends BlockEntityRenderState {
        boolean middle;
        float animationTime;
        float radiusScale;
    }

    public PolarPortalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PolarPortalBlockEntity portal, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(portal, state, partialTicks, cameraPosition, breakProgress);
        Level level = portal.getLevel();
        state.middle = level != null && isMiddle(level, portal.getBlockPos());
        state.animationTime = level != null ? Math.floorMod(level.getGameTime(), 40) + partialTicks : 0.0F;
        // Like a beacon's: thicker with distance so it doesn't thin to nothing far away (unless you're looking through a spyglass)
        float distance = (float) cameraPosition.subtract(Vec3.atCenterOf(state.blockPos)).horizontalDistance();
        var player = Minecraft.getInstance().player;
        state.radiusScale = player != null && player.isScoping() ? 1.0F : Math.max(1.0F, distance / 96.0F);
    }

    static boolean isMiddle(Level level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(dir)).is(ModBlocks.POLAR_PORTAL.get())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.middle) {
            BeaconRenderer.submitBeaconBeam(poseStack, collector, BeaconRenderer.BEAM_LOCATION, 1.0F, state.animationTime, 0,
                    BeaconRenderer.MAX_RENDER_Y, COLOR, SOLID_RADIUS * state.radiusScale, GLOW_RADIUS * state.radiusScale);
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
    public boolean shouldRender(PolarPortalBlockEntity portal, Vec3 cameraPosition) {
        return Vec3.atCenterOf(portal.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(cameraPosition.multiply(1.0, 0.0, 1.0), getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(PolarPortalBlockEntity portal) {
        BlockPos pos = portal.getBlockPos();
        return new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, BeaconRenderer.MAX_RENDER_Y, pos.getZ() + 2);
    }
}
