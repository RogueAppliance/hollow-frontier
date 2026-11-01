package com.hollowfrontier.client;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyBoardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/**
 * The hanging Bounty Board (18): a cork board in a wooden frame, a sixteenth of a block deep, with its posters on the front
 * ({@link PosterPainter}), one to a block. The texture (64x48) is a 3x3 grid of 16x16 front tiles, corners, edges and
 * middle, picked per block by where it sits on the board, and a wood tile (48..64, 0..16) for the back and the edges.
 */
public class HangingBountyBoardRenderer extends EntityRenderer<BountyBoardEntity, HangingBountyBoardRenderer.State> {
    static final Identifier TEXTURE = HollowFrontier.id("textures/entity/bounty_board.png");
    private static final float TEX_W = 64.0F, TEX_H = 48.0F;
    private static final float SCALE = 1.0F / PosterPainter.CELL; // a cell to a block

    private final PosterPainter painter;

    public static class State extends EntityRenderState {
        Direction direction = Direction.SOUTH;
        int columns = BountyBoardEntity.NARROW;
        int light = LightCoordsUtil.FULL_BRIGHT;
        List<PosterPainter.Poster> posters = List.of();
    }

    public HangingBountyBoardRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.painter = new PosterPainter(context.getFont());
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BountyBoardEntity board, State state, float partialTicks) {
        super.extractRenderState(board, state, partialTicks);
        state.direction = board.getDirection();
        state.columns = board.columns();
        state.light = LightCoordsUtil.getLightCoords(board.level(), board.getPos());
        state.posters = painter.posters(board.face(), board.columns(), board.rows(), board.getPos());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int columns = state.columns, rows = BountyBoardEntity.ROWS;
        float depth = (float) BountyBoardEntity.DEPTH / 2;
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, -state.direction.toYRot()); // +z out of the wall, +x to the right as you face it
        collector.submitCustomGeometry(pose, RenderTypes.entitySolid(TEXTURE), (at, buffer) -> {
            float left = -columns / 2.0F, top = rows / 2.0F;
            for (int cx = 0; cx < columns; cx++) {
                for (int cy = 0; cy < rows; cy++) {
                    float x0 = left + cx, x1 = x0 + 1, y1 = top - cy, y0 = y1 - 1;
                    int tileU = cx == 0 ? 0 : cx == columns - 1 ? 2 : 1, tileV = cy == 0 ? 0 : cy == rows - 1 ? 2 : 1;
                    float u0 = tileU * 16 / TEX_W, u1 = (tileU + 1) * 16 / TEX_W, v0 = tileV * 16 / TEX_H, v1 = (tileV + 1) * 16 / TEX_H;
                    quad(at, buffer, state.light, 0, 0, 1, x0, y0, depth, u0, v1, x1, y0, depth, u1, v1, x1, y1, depth, u1, v0, x0, y1, depth, u0, v0);
                }
            }
            float right = -left, bottom = -top;
            float wu0 = 48 / TEX_W, wu1 = 1.0F, wv0 = 0, wv1 = 16 / TEX_H, edge = 1 / TEX_H;
            quad(at, buffer, state.light, 0, 0, -1, right, bottom, -depth, wu0, wv1, left, bottom, -depth, wu1, wv1, left, top, -depth, wu1, wv0, right, top, -depth, wu0, wv0);
            quad(at, buffer, state.light, 0, 1, 0, left, top, -depth, wu0, wv0, left, top, depth, wu0, wv0 + edge, right, top, depth, wu1, wv0 + edge, right, top, -depth, wu1, wv0);
            quad(at, buffer, state.light, 0, -1, 0, left, bottom, depth, wu0, wv0, left, bottom, -depth, wu0, wv0 + edge, right, bottom, -depth, wu1, wv0 + edge, right, bottom, depth, wu1, wv0);
            quad(at, buffer, state.light, 1, 0, 0, right, bottom, depth, wu0, wv1, right, bottom, -depth, wu0 + 1 / TEX_W, wv1, right, top, -depth, wu0 + 1 / TEX_W, wv0, right, top, depth, wu0, wv0);
            quad(at, buffer, state.light, -1, 0, 0, left, bottom, -depth, wu0, wv1, left, bottom, depth, wu0 + 1 / TEX_W, wv1, left, top, depth, wu0 + 1 / TEX_W, wv0, left, top, -depth, wu0, wv0);
        });
        pose.translate(0.0F, 0.0F, depth + 0.02F); // the paper sits 0.8 units (1/80 block) behind the ink: keep it off the cork
        pose.scale(SCALE, -SCALE, SCALE);
        pose.translate(-columns * PosterPainter.CELL / 2.0F, -rows * PosterPainter.CELL / 2.0F, 0.0F);
        painter.paint(pose, collector, state.posters, columns, rows, state.light);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    // One face, corners counter-clockwise as seen from outside
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int light, int nx, int ny, int nz, float... corners) {
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, corners[i * 5], corners[i * 5 + 1], corners[i * 5 + 2]).setColor(-1).setUv(corners[i * 5 + 3], corners[i * 5 + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }
}
