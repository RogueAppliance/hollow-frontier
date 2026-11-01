package com.hollowfrontier.content.frontier.gun;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.hollowfrontier.content.frontier.saloon.TipsyEffect;
import com.hollowfrontier.registry.ModDamageTypes;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side hitscan shooting, shared by player guns and gun-wielding mobs.
 * Each pellet raycasts blocks then entities along the shooter's look vector; damage from all
 * pellets is summed per target and applied once so invulnerability frames don't eat shotgun pellets.
 */
public final class GunMechanics {
    private GunMechanics() {}

    private static final double TRACER_STEP = 1.5;
    // How far under the eyes a shot still counts as a headshot (a player's head is 0.5 tall, eyes near its middle)
    private static final double HEAD_BAND = 0.25;
    // Where the shooter sees their own muzzle smoke start: ahead of the eyes, right and down to the first-person barrel's end
    private static final double OWN_MUZZLE_AHEAD = 1.3;
    private static final double OWN_MUZZLE_RIGHT = 0.45;
    private static final double OWN_MUZZLE_DROP = 0.4;
    private static final Holder<SoundEvent> HEADSHOT_SOUND = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ARROW_HIT_PLAYER);

    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats) {
        fire(level, shooter, stats, e -> false);
    }

    /** As fire, but bullets pass through anything {@code passThrough} accepts (a lawman never hits townsfolk). */
    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats, Predicate<Entity> passThrough) {
        Vec3 start = shooter.getEyePosition();
        Vec3 look = shooter.getViewVector(1.0F);
        RandomSource random = shooter.getRandom();
        Map<Entity, Float> damageByTarget = new HashMap<>();
        Map<Entity, Vec3> headshots = new HashMap<>();
        // Liquor spoils your aim: each Tipsy level widens the cone, even for a rifle.
        float spreadDegrees = stats.spreadDegrees() + TipsyEffect.extraSpread(shooter);

        for (int i = 0; i < stats.pellets(); i++) {
            Vec3 dir = spread(look, spreadDegrees, random);
            Vec3 end = start.add(dir.scale(stats.range()));

            BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            if (blockHit.getType() != HitResult.Type.MISS) {
                end = blockHit.getLocation();
            }

            AABB searchBox = shooter.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, shooter, start, end, searchBox,
                    target -> target != shooter && target.isPickable() && !target.isSpectator() && !shooter.isPassengerOfSameVehicle(target)
                            && !passThrough.test(target),
                    0.3F);

            if (entityHit != null) {
                end = entityHit.getLocation();
                float headshot = headshot(shooter, entityHit.getEntity(), end, stats);
                if (headshot > 1.0F) {
                    headshots.putIfAbsent(entityHit.getEntity(), end);
                }
                damageByTarget.merge(entityHit.getEntity(), stats.damage() * headshot, Float::sum);
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 3, 0.05, 0.05, 0.05, 0.01);
            }
            tracer(level, start.add(dir.scale(0.8)), end);
        }

        DamageSource source = new DamageSource(ModDamageTypes.holder(level, ModDamageTypes.BULLET), shooter, shooter);
        damageByTarget.forEach((target, damage) -> target.hurtServer(level, source, damage));
        headshots.forEach((target, at) -> headshotFeedback(level, shooter, at));

        // Black powder: a flash and a white cloud drifting out of the muzzle, sent to each player on their own. Everyone else
        // sees it just ahead of the shooter's face, where the muzzle is from outside. The shooter gets a thinner one where
        // their first-person barrel ends, low and to the right of the sights, so it doesn't hide what they shot at.
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 drift = look.scale(0.08);
        for (ServerPlayer viewer : level.players()) {
            boolean own = viewer == shooter;
            Vec3 muzzle = own
                    ? start.add(look.scale(OWN_MUZZLE_AHEAD)).add(right.scale(OWN_MUZZLE_RIGHT)).add(0, -OWN_MUZZLE_DROP, 0)
                    : start.add(look.scale(0.9)).add(0, -0.1, 0);
            level.sendParticles(viewer, ParticleTypes.FLAME, false, false, muzzle.x, muzzle.y, muzzle.z, 2, 0.02, 0.02, 0.02, 0.01);
            int puffs = own ? 2 + stats.pellets() / 4 : 4 + stats.pellets();
            for (int i = 0; i < puffs; i++) {
                double ahead = random.nextDouble() * 0.6;
                level.sendParticles(viewer, ParticleTypes.POOF, false, false, muzzle.x + look.x * ahead, muzzle.y + look.y * ahead,
                        muzzle.z + look.z * ahead, 0, drift.x, drift.y + 0.02, drift.z, 1.0);
            }
            if (!own) {
                level.sendParticles(viewer, ParticleTypes.WHITE_SMOKE, false, false, muzzle.x, muzzle.y, muzzle.z, 4, 0.15, 0.1, 0.15, 0.01);
            }
        }
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), stats.fireSound(), SoundSource.PLAYERS,
                1.0F, stats.firePitch() + (random.nextFloat() - 0.5F) * 0.1F);
    }

    /**
     * The damage multiplier for a shot landing at {@code hit}: the gun's headshot multiplier if a player put it in a
     * creature's head (from a little under the eyes up; the band shrinks for small creatures), else 1. Mobs never get it.
     */
    public static float headshot(LivingEntity shooter, Entity target, Vec3 hit, GunStats stats) {
        if (!(shooter instanceof Player) || !(target instanceof LivingEntity)) {
            return 1.0F;
        }
        return hit.y >= target.getEyeY() - Math.min(HEAD_BAND, target.getBbHeight() * 0.15) ? stats.headshot() : 1.0F;
    }

    /** A spray of sparks at the head for everyone, and a "ding" only the shooter hears. */
    public static void headshotFeedback(ServerLevel level, LivingEntity shooter, Vec3 at) {
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, 10, 0.15, 0.15, 0.15, 0.2);
        if (shooter instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSoundPacket(HEADSHOT_SOUND, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(),
                    0.6F, 1.3F, level.getRandom().nextLong()));
        }
    }

    // Random direction inside a cone of the given half-angle around dir.
    private static Vec3 spread(Vec3 dir, float degrees, RandomSource random) {
        if (degrees <= 0) {
            return dir;
        }
        float max = degrees * Mth.DEG_TO_RAD;
        Vec3 up = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 realUp = right.cross(dir).normalize();
        float angle = random.nextFloat() * Mth.TWO_PI;
        float radius = Mth.sqrt(random.nextFloat()) * Mth.sin(max);
        return dir.add(right.scale(Mth.cos(angle) * radius)).add(realUp.scale(Mth.sin(angle) * radius)).normalize();
    }

    private static void tracer(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = (int) (delta.length() / TRACER_STEP);
        for (int i = 1; i <= steps; i++) {
            Vec3 p = from.add(delta.scale((double) i / steps));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }
}
