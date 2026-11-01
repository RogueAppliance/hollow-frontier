package com.hollowfrontier.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.bounty.BountyBoardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The Small Bounty Board's posters (18): four to a side, on every side with open air in front ({@link PosterPainter}). */
public class BountyBoardRenderer implements BlockEntityRenderer<BountyBoardBlockEntity, BountyBoardRenderer.State> {
    private static final float SCALE = 1.0F / (2 * PosterPainter.CELL); // two cells across the block

    private final PosterPainter painter;

    public static class State extends BlockEntityRenderState {
        final List<Side> sides = new ArrayList<>();
        List<PosterPainter.Poster> posters = List.of();
    }

    record Side(Direction direction, int light) {}

    public BountyBoardRenderer(BlockEntityRendererProvider.Context context) {
        this.painter = new PosterPainter(context.font());
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BountyBoardBlockEntity board, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(board, state, partialTicks, cameraPosition, breakProgress);
        state.sides.clear();
        state.posters = List.of();
        Level level = board.getLevel();
        BlockPos pos = board.getBlockPos();
        if (level == null) {
            return;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos front = pos.relative(side);
            if (!level.getBlockState(front).isSolidRender()) {
                state.sides.add(new Side(side, LightCoordsUtil.getLightCoords(level, front)));
            }
        }
        if (!state.sides.isEmpty()) {
            state.posters = painter.posters(board.face(), board.columns(), board.rows(), pos);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (Side side : state.sides) {
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.rotateDegrees(Axis.YP, -side.direction().toYRot());
            pose.translate(0.0F, 0.0F, 0.512F);
            pose.scale(SCALE, -SCALE, SCALE);
            pose.translate(-PosterPainter.CELL, -PosterPainter.CELL, 0.0F);
            painter.paint(pose, collector, state.posters, 2, 2, side.light());
            pose.popPose();
        }
    }

    @Override
    public int getViewDistance() {
        return 32;
    }
}
