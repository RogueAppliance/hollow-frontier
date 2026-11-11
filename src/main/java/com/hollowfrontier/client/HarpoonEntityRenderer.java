package com.hollowfrontier.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.sea.HarpoonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Harpoon in flight (GeckoLib model "harpoon", head toward -Z): turned to the way it's going, as an arrow is, by
 * turning its one bone (the renderer itself applies no yaw: {@link #calculateYRot} is zero). The teeth show only on a
 * Toothed Harpoon.
 * <p>
 * <b>Its line</b> is drawn here too: a rope from the thrower's hand to the harpoon, a square-section tube of
 * {@link #SEGMENTS} short lengths in textures/entity/harpoon_line.png, hanging in a shallow curve that pulls straight as
 * it's stretched. The hand is found as vanilla finds it for a fishing line (the corner of the screen in first person, by
 * the hip otherwise). The harpoon is kept drawn whenever its thrower is in view, since the rope reaches all the way back.
 */
public class HarpoonEntityRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<HarpoonEntity, R> {
    static final DataTicket<Float> YAW = DataTicket.create("hollowfrontier_harpoon_yaw", Float.class);
    static final DataTicket<Float> PITCH = DataTicket.create("hollowfrontier_harpoon_pitch", Float.class);
    static final DataTicket<Boolean> TOOTHED = DataTicket.create("hollowfrontier_harpoon_toothed", Boolean.class);
    /** From the harpoon to its thrower's hand, in blocks; absent when it has no thrower in this world. */
    static final DataTicket<Vec3> LINE = DataTicket.create("hollowfrontier_harpoon_line", Vec3.class);
    /** Set by eye in runPhoto: which way the bone turns for a positive yaw and pitch. */
    static final float YAW_SIGN = 1.0F, PITCH_SIGN = 1.0F;
    static final Identifier ROPE = HollowFrontier.id("textures/entity/harpoon_line.png");
    static final int SEGMENTS = 20;
    /** Half the rope's thickness, and how far it hangs below a straight line per block of its length when slack. */
    static final float HALF_WIDTH = 0.02F, SAG = 0.06F;

    public HarpoonEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<HarpoonEntity>(HollowFrontier.id("harpoon")));
        this.shadowRadius = 0.0F;
    }

    @Override
    protected float calculateYRot(HarpoonEntity harpoon, float yHeadRot, float partialTick) {
        return 0.0F;
    }

    @Override
    public boolean shouldRender(HarpoonEntity harpoon, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        return harpoon.getOwner() != null || super.shouldRender(harpoon, culler, camX, camY, camZ, partialTicks);
    }

    @Override
    protected AABB getBoundingBoxForCulling(HarpoonEntity harpoon, float partialTicks) {
        AABB box = super.getBoundingBoxForCulling(harpoon, partialTicks);
        Entity owner = harpoon.getOwner();
        return owner == null ? box : box.minmax(owner.getBoundingBox());
    }

    @Override
    public void captureDefaultRenderState(HarpoonEntity harpoon, Void relatedObject, R state, float partialTick) {
        super.captureDefaultRenderState(harpoon, relatedObject, state, partialTick);
        state.addGeckolibData(YAW, Mth.rotLerp(partialTick, harpoon.yRotO, harpoon.getYRot()));
        state.addGeckolibData(PITCH, Mth.lerp(partialTick, harpoon.xRotO, harpoon.getXRot()));
        state.addGeckolibData(TOOTHED, harpoon.isToothed());
        Entity owner = harpoon.getOwner();
        if (owner != null && owner.isAlive()) {
            state.addGeckolibData(LINE, handOf(owner, partialTick).subtract(harpoon.getPosition(partialTick)));
        }
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
    public void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        float yaw = info.renderState().getOrDefaultGeckolibData(YAW, 0.0F) * Mth.DEG_TO_RAD;
        float pitch = info.renderState().getOrDefaultGeckolibData(PITCH, 0.0F) * Mth.DEG_TO_RAD;
        boolean toothed = info.renderState().getOrDefaultGeckolibData(TOOTHED, false);
        snapshots.ifPresent("harpoon", bone -> bone.setRotY(YAW_SIGN * yaw).setRotX(PITCH_SIGN * pitch));
        snapshots.ifPresent("teeth", bone -> bone.skipRender(!toothed));
    }

    @Override
    public void submit(R state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        Vec3 line = state.getOrDefaultGeckolibData(LINE, (Vec3) null);
        if (line == null || line.lengthSqr() < 0.25) {
            return;
        }
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(ROPE), (pose, buffer) -> rope(pose, buffer, line, light));
    }

    /** The rope from the origin (the harpoon) to {@code line} (the hand): {@link #SEGMENTS} four-sided lengths along a sagging curve. */
    private static void rope(PoseStack.Pose pose, VertexConsumer buffer, Vec3 line, int light) {
        double length = line.length();
        // Slack hangs; a long line is pulled tight (it sags less per block the further it's stretched)
        double sag = Math.min(1.5, length * SAG) * Mth.clamp(1.4 - length / 24.0, 0.15, 1.0);
        Vec3 previous = Vec3.ZERO;
        for (int i = 1; i <= SEGMENTS; i++) {
            double a = i / (double) SEGMENTS;
            Vec3 next = line.scale(a).add(0, -sag * 4 * a * (1 - a), 0);
            Vec3 along = next.subtract(previous);
            if (along.lengthSqr() > 1.0E-8) {
                Vec3 dir = along.normalize();
                Vec3 side = dir.cross(Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize().scale(HALF_WIDTH);
                Vec3 up = side.cross(dir).normalize().scale(HALF_WIDTH);
                // Four faces round the length; the strands run the length of the texture, one quarter of it to a face
                face(pose, buffer, light, previous, next, side, up, 0.00F);
                face(pose, buffer, light, previous, next, up, side.scale(-1), 0.25F);
                face(pose, buffer, light, previous, next, side.scale(-1), up.scale(-1), 0.50F);
                face(pose, buffer, light, previous, next, up.scale(-1), side, 0.75F);
            }
            previous = next;
        }
    }

    /** One face of a length of rope: the side that faces along {@code out}, spanning {@code across} either way. */
    private static void face(PoseStack.Pose pose, VertexConsumer buffer, int light, Vec3 from, Vec3 to, Vec3 out, Vec3 across, float v) {
        Vec3 n = out.normalize();
        vertex(pose, buffer, light, from.add(out).subtract(across), 0.0F, v, n);
        vertex(pose, buffer, light, from.add(out).add(across), 0.0F, v + 0.25F, n);
        vertex(pose, buffer, light, to.add(out).add(across), 1.0F, v + 0.25F, n);
        vertex(pose, buffer, light, to.add(out).subtract(across), 1.0F, v, n);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int light, Vec3 at, float u, float v, Vec3 normal) {
        buffer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
