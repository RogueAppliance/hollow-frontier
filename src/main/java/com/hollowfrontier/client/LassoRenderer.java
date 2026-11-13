package com.hollowfrontier.client;

import com.hollowfrontier.content.frontier.gear.LassoEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A lasso in the air (46b): a loop of rope that whirls as it flies, on a line back to the thrower's hand. It was the
 * item's flat picture until the user asked to see it whirl. Drawn by {@link RopeDraw}; the hand is found as the
 * Harpoon's line finds it.
 */
public class LassoRenderer extends EntityRenderer<LassoEntity, LassoRenderer.State> {
    /** The loop's radius, how far its plane leans, and how fast the lean goes round (radians a tick: three turns a second). */
    static final double RADIUS = 0.5, TILT = 0.6, SPIN = 0.95;

    public static class State extends EntityRenderState {
        /** From the lasso to its thrower's hand, or null with no thrower in this world. */
        Vec3 line;
    }

    public LassoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(LassoEntity lasso, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        return lasso.getOwner() != null || super.shouldRender(lasso, culler, camX, camY, camZ, partialTicks);
    }

    @Override
    protected AABB getBoundingBoxForCulling(LassoEntity lasso, float partialTicks) {
        AABB box = super.getBoundingBoxForCulling(lasso, partialTicks).inflate(RADIUS);
        Entity owner = lasso.getOwner();
        return owner == null ? box : box.minmax(owner.getBoundingBox());
    }

    @Override
    public void extractRenderState(LassoEntity lasso, State state, float partialTicks) {
        super.extractRenderState(lasso, state, partialTicks);
        Entity owner = lasso.getOwner();
        state.line = owner != null && owner.isAlive() ? handOf(owner, partialTicks).subtract(lasso.getPosition(partialTicks)) : null;
    }

    /** Where the line leaves its thrower: a player's hand as vanilla places it for a fishing line, anyone else's chest. */
    private Vec3 handOf(Entity owner, float partialTicks) {
        if (!(owner instanceof Player player)) {
            return owner.getEyePosition(partialTicks).add(0, -0.4, 0);
        }
        int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
            float fov = entityRenderDispatcher.options.fov().get().intValue();
            Vec3 corner = entityRenderDispatcher.camera.getNearPlane(fov).getPointOnPlane(side * 0.525F, -0.4F).scale(960.0 / fov);
            return player.getEyePosition(partialTicks).add(corner);
        }
        float yaw = Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
        float scale = player.getScale();
        double right = side * 0.35 * scale, forward = 0.6 * scale;
        return player.getEyePosition(partialTicks).add(-cos * right - sin * forward, (player.isCrouching() ? -0.1875 : 0.0) - 0.55 * scale, -sin * right + cos * forward);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        int light = state.lightCoords;
        double turn = state.ageInTicks * SPIN;
        Vec3 line = state.line;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(RopeDraw.ROPE), (pose, buffer) -> {
            RopeDraw.loop(pose, buffer, light, Vec3.ZERO, RADIUS, TILT, turn);
            if (line != null && line.lengthSqr() > 0.25) {
                RopeDraw.line(pose, buffer, light, RopeDraw.knot(Vec3.ZERO, RADIUS, TILT, turn), line, Math.min(0.6, line.length() * 0.04));
            }
        });
    }
}
