package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.worldgen.HollowEarth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * The Hollow Earth's inner sun: a white-gold disc with a broad halo, always straight overhead and always the same size.
 * <p>
 * A sky-box sun can't work here: the sky is drawn behind the world, and the crust overhead hides all of it. So the sun
 * is drawn in the world, as a pair of flat emissive quads facing down, hung a little under the crust's underside above
 * the camera ({@link #MAX_DISTANCE} up at most) and scaled with that distance so it always looks the same size. It
 * follows the camera sideways, as the real sun seems to. Being in the world, anything between you and it (a spire, a
 * floating island, a tree, a roof) hides it naturally, and the fog softens it like everything else.
 * <p>
 * Vanilla's {@code eyes} render type (emissive, translucent), so shader packs treat it like any glowing mob part. Inside the
 * crust (falling down Symmes' shaft) there's no sun: it fades out within {@link #FADE} blocks of the underside.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class InnerSun {
    private InnerSun() {}

    static final Identifier SUN = HollowFrontier.id("textures/environment/inner_sun.png");
    static final Identifier HALO = HollowFrontier.id("textures/environment/inner_sun_halo.png");
    /** Angular size: the disc's half-width per block of distance (about 9 degrees across, a big, near sun). */
    static final float SUN_SIZE = 0.08F;
    static final float HALO_SIZE = 0.34F;
    static final float MAX_DISTANCE = 130.0F;
    static final float MIN_DISTANCE = 12.0F;
    static final float FADE = 16.0F;
    /** Gap kept between the sun and the rock above it. */
    static final float UNDER_ROCK = 10.0F;

    /** Where the crust's underside is above the camera's column (measured each tick; scanning per frame is waste). */
    private static int rockAbove = Integer.MAX_VALUE;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || level.dimension() != HollowEarth.LEVEL || mc.gameRenderer.mainCamera() == null) {
            rockAbove = Integer.MAX_VALUE;
            return;
        }
        Vec3 eye = mc.gameRenderer.mainCamera().position();
        BlockPos.MutableBlockPos pos = BlockPos.containing(eye).mutable();
        int top = level.getMaxY();
        rockAbove = Integer.MAX_VALUE;
        // The first crust block straight up (trees, roofs and the like are in front of the sun, not above it)
        for (int y = pos.getY() + 1; y <= top; y++) {
            if (HollowEarth.isCrust(level.getBlockState(pos.setY(y)))) {
                rockAbove = y;
                return;
            }
        }
    }

    @SubscribeEvent
    static void onSubmit(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != HollowEarth.LEVEL) {
            return;
        }
        Vec3 eye = event.getLevelRenderState().cameraRenderState.pos;
        // Never higher than the crust's lowest underside: the crust over you may be higher, but looking up Symmes' shaft
        // the first crust block is the shell at the top of the world, and a sun up there would be inside the rock
        float roof = Math.min(rockAbove, HollowEarth.CEILING_LOWEST);
        float room = roof - (float) eye.y - UNDER_ROCK;
        // Out of the crust and into the open, the sun comes up out of the haze above you
        float alpha = Mth.clamp((room - MIN_DISTANCE) / FADE, 0.0F, 1.0F);
        if (alpha <= 0.0F) {
            return;
        }
        float distance = Math.min(MAX_DISTANCE, Math.max(MIN_DISTANCE, room));
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0F, distance, 0.0F);
        // The halo first, behind the disc (both face down, toward the camera below)
        quad(event, poseStack, RenderTypes.eyes(HALO), distance * HALO_SIZE, ARGB.white(alpha * 0.85F), 0.05F);
        quad(event, poseStack, RenderTypes.eyes(SUN), distance * SUN_SIZE, ARGB.white(alpha), 0.0F);
        poseStack.popPose();
    }

    private static void quad(SubmitCustomGeometryEvent event, PoseStack poseStack, RenderType type, float half, int color, float lift) {
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            // Wound as a block's bottom face, so it's the side seen from below
            vertex(buffer, pose, -half, lift, half, 0, 1, color);
            vertex(buffer, pose, -half, lift, -half, 0, 0, color);
            vertex(buffer, pose, half, lift, -half, 1, 0, color);
            vertex(buffer, pose, half, lift, half, 1, 1, color);
        });
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, -1.0F, 0.0F);
    }
}
