package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Rope, drawn as the Harpoon's line is (square-section lengths in textures/entity/harpoon_line.png), for the lasso
 * (46b): a line that hangs between two points, and a loop that whirls.
 * <p>
 * A circle spinning in its own plane looks like a circle standing still, so the loop is tilted and the tilt goes
 * round: the rim rises and dips as it turns, and the knot where the line joins it travels the rim.
 */
final class RopeDraw {
    private RopeDraw() {}

    static final Identifier ROPE = HollowFrontier.id("textures/entity/harpoon_line.png");
    static final float HALF_WIDTH = 0.022F;
    private static final int LOOP_SEGMENTS = 18, LINE_SEGMENTS = 12;

    /** Where the knot is on a loop of this centre and radius, {@code turn} radians round, its plane tilted {@code tilt} radians. */
    static Vec3 knot(Vec3 centre, double radius, double tilt, double turn) {
        return rim(centre, radius, tilt, turn, 0.0);
    }

    private static Vec3 rim(Vec3 centre, double radius, double tilt, double turn, double round) {
        // The loop's plane leans toward the direction "turn" points in; a and b lie in it
        Vec3 normal = new Vec3(Math.sin(tilt) * Math.cos(turn), Math.cos(tilt), Math.sin(tilt) * Math.sin(turn));
        Vec3 a = normal.cross(new Vec3(-Math.sin(turn), 0, Math.cos(turn))).normalize();
        Vec3 b = normal.cross(a);
        double at = round + turn * 1.3;
        return centre.add(a.scale(radius * Math.cos(at))).add(b.scale(radius * Math.sin(at)));
    }

    /** The whirling loop. */
    static void loop(PoseStack.Pose pose, VertexConsumer buffer, int light, Vec3 centre, double radius, double tilt, double turn) {
        Vec3 previous = rim(centre, radius, tilt, turn, 0.0);
        for (int i = 1; i <= LOOP_SEGMENTS; i++) {
            Vec3 next = rim(centre, radius, tilt, turn, Mth.TWO_PI * i / LOOP_SEGMENTS);
            length(pose, buffer, light, previous, next, HALF_WIDTH);
            previous = next;
        }
        // The knot: a short fat length where the line joins
        Vec3 knot = rim(centre, radius, tilt, turn, 0.0);
        length(pose, buffer, light, knot.add(0, -0.03, 0), knot.add(0, 0.03, 0), HALF_WIDTH * 2.2F);
    }

    /** A line from one point to another, hanging a little. */
    static void line(PoseStack.Pose pose, VertexConsumer buffer, int light, Vec3 from, Vec3 to, double sag) {
        Vec3 span = to.subtract(from);
        Vec3 previous = from;
        for (int i = 1; i <= LINE_SEGMENTS; i++) {
            double t = i / (double) LINE_SEGMENTS;
            Vec3 next = from.add(span.scale(t)).add(0, -sag * 4 * t * (1 - t), 0);
            length(pose, buffer, light, previous, next, HALF_WIDTH);
            previous = next;
        }
    }

    /** One straight length of rope, four faces round. */
    private static void length(PoseStack.Pose pose, VertexConsumer buffer, int light, Vec3 from, Vec3 to, float half) {
        Vec3 along = to.subtract(from);
        if (along.lengthSqr() < 1.0E-8) {
            return;
        }
        Vec3 dir = along.normalize();
        Vec3 side = dir.cross(Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize().scale(half);
        Vec3 up = side.cross(dir).normalize().scale(half);
        face(pose, buffer, light, from, to, side, up, 0.00F);
        face(pose, buffer, light, from, to, up, side.scale(-1), 0.25F);
        face(pose, buffer, light, from, to, side.scale(-1), up.scale(-1), 0.50F);
        face(pose, buffer, light, from, to, up.scale(-1), side, 0.75F);
    }

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
