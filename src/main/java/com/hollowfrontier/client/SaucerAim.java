package com.hollowfrontier.client;

import org.joml.Vector3fc;

import com.hollowfrontier.content.ufo.PrototypeSaucer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Where the Prototype Saucer pilot's sight is (13c): what the middle of the screen points at, from the camera, whatever
 * the camera is doing. In third person the camera rides above and behind the craft ({@link AirshipCamera}), so the
 * pilot's own eye line is no longer the screen's middle; the client works out the point and sends it with the laser key
 * (SaucerControlPayload), and the server aims bolts at it from the underside. The first creature or block along the
 * camera's line, out to the laser's range past the craft; never the craft or its crew.
 */
final class SaucerAim {
    private SaucerAim() {}

    static Vec3 target(Minecraft mc, PrototypeSaucer saucer) {
        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 from = camera.position();
        Vector3fc f = camera.forwardVector();
        double reach = PrototypeSaucer.LASER_RANGE + from.distanceTo(saucer.position());
        Vec3 far = from.add(new Vec3(f.x(), f.y(), f.z()).scale(reach));
        BlockHitResult block = mc.level.clip(new ClipContext(from, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, saucer));
        Vec3 point = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(mc.level, saucer, from, point, new AABB(from, point).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != saucer && !saucer.hasPassenger(e), 0.3F);
        return hit != null ? hit.getLocation() : point;
    }
}
