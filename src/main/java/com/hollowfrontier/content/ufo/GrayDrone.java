package com.hollowfrontier.content.ufo;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A Gray Drone (13d): a probe built to fight, launched by the Mothership in waves. It picks the nearest player within
 * {@link #SIGHT}, circles them {@link #ORBIT} out and {@link #HEIGHT} up, and with them in sight draws a violet line on
 * them for {@link #CHARGE_TICKS} (time to get behind something), then zaps them: {@link #DAMAGE} and a moment's slowness.
 * Then it cools {@link #COOLDOWN}. Small and quick but frail; while any of a wave is up, the Mothership's shield holds.
 * With its Mothership gone it blinks out.
 * <p>
 * A few keep a Gray Landing instead (15a, {@link #guard}): they hang about its saucer, take on only players within
 * {@link #LANDING_LEASH} of it, and blink out if it goes.
 */
public class GrayDrone extends GrayProbe {
    public static final double SIGHT = 40.0;
    public static final double ORBIT = 7.0, HEIGHT = 5.0;
    public static final double SPEED = 0.45;
    public static final int CHARGE_TICKS = 30, COOLDOWN = 50;
    public static final float DAMAGE = 4.0F;
    public static final double LANDING_LEASH = 28.0;
    static final int COLOUR = 0xB070FF;
    private static final DustParticleOptions LINE = new DustParticleOptions(COLOUR, 0.6F);

    private @Nullable UUID mothership;
    /** A landing's saucer, and where it's down (so it's known whether that ground is loaded when it can't be found). */
    private @Nullable UUID landing;
    private @Nullable BlockPos landingAt;
    private @Nullable Player quarry;
    private int charge, cooldown;
    private double orbit;

    public GrayDrone(EntityType<? extends GrayDrone> type, Level level) {
        super(type, level);
        xpReward = 4;
        orbit = random.nextDouble() * Math.PI * 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return GrayProbe.createAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, SIGHT);
    }

    /** A drone from {@code ship}'s underside, already its own. */
    public static GrayDrone launch(ServerLevel level, Mothership ship) {
        GrayDrone drone = ModEntities.GRAY_DRONE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        double a = level.getRandom().nextDouble() * Math.PI * 2;
        drone.snapTo(ship.getX() + Math.cos(a) * 3, ship.getY() - 1.5, ship.getZ() + Math.sin(a) * 3, (float) Math.toDegrees(a), 0);
        drone.mothership = ship.getUUID();
        level.addFreshEntity(drone);
        return drone;
    }

    /** A drone over {@code saucer}, a Gray Landing's, keeping it. */
    public static GrayDrone guard(ServerLevel level, GraySaucer saucer) {
        GrayDrone drone = ModEntities.GRAY_DRONE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        double a = level.getRandom().nextDouble() * Math.PI * 2;
        drone.snapTo(saucer.getX() + Math.cos(a) * 5, saucer.getY() + 3, saucer.getZ() + Math.sin(a) * 5, (float) Math.toDegrees(a), 0);
        drone.landing = saucer.getUUID();
        drone.landingAt = saucer.blockPosition();
        level.addFreshEntity(drone);
        return drone;
    }

    public @Nullable UUID mothership() {
        return mothership;
    }

    public @Nullable UUID landing() {
        return landing;
    }

    public int charge() {
        return charge;
    }

    /** It doesn't run. */
    @Override
    public void flee(Entity from) {}

    @Override
    protected void watch(ServerLevel level) {
        Mothership ship = mothership != null && level.getEntity(mothership) instanceof Mothership m && m.isAlive() ? m : null;
        if (mothership != null && ship == null && tickCount % 20 == 0) {
            vanish(level);
            return;
        }
        GraySaucer saucer = landing != null && level.getEntity(landing) instanceof GraySaucer s && s.isAlive() && s.isLanding() ? s : null;
        if (landing != null && saucer == null && tickCount % 20 == 0 && (landingAt == null || level.isLoaded(landingAt))) {
            vanish(level);
            return;
        }
        Entity home = ship != null ? ship : saucer;
        if (quarry == null || !canStillSee(quarry, false) || !inReach(quarry, saucer)) {
            Player seen = level.getNearestPlayer(this, SIGHT);
            quarry = seen != null && canStillSee(seen, false) && inReach(seen, saucer) ? seen : null;
            charge = 0;
        }
        fly(home, ship != null ? -3 : 3);
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (quarry == null || !canStillSee(quarry, true)) {
            charge = 0;
            return;
        }
        if (charge == 0) {
            playSound(ModSounds.SENTINEL_CHARGE.get(), 1.2F, 1.3F);
        }
        charge++;
        getLookControl().setLookAt(quarry, 30.0F, 30.0F);
        if (charge % 2 == 0) {
            line(level, quarry);
        }
        if (charge >= CHARGE_TICKS) {
            zap(level, quarry);
        }
    }

    /** Round its quarry, or with none, by its home's side (under a Mothership, over a landed saucer). */
    private void fly(@Nullable Entity home, double rise) {
        Vec3 goal;
        if (quarry != null) {
            orbit += 0.04;
            goal = quarry.position().add(Math.cos(orbit) * ORBIT, HEIGHT, Math.sin(orbit) * ORBIT);
        } else if (home != null) {
            orbit += 0.02;
            goal = home.position().add(Math.cos(orbit) * 8, rise, Math.sin(orbit) * 8);
        } else {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 to = goal.subtract(position());
        setDeltaMovement(to.lengthSqr() < 0.04 ? Vec3.ZERO : to.normalize().scale(Math.min(SPEED, to.length() * 0.25)));
    }

    private boolean canStillSee(Player player, boolean needSight) {
        return player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && distanceTo(player) <= SIGHT
                && (!needSight || hasLineOfSight(player));
    }

    /** A landing's drones don't chase anyone far from its saucer. */
    private static boolean inReach(Player player, @Nullable GraySaucer saucer) {
        return saucer == null || player.distanceTo(saucer) <= LANDING_LEASH;
    }

    private void line(ServerLevel level, Player target) {
        Vec3 from = position();
        Vec3 step = target.getEyePosition().subtract(0, 0.4, 0).subtract(from);
        int points = (int) Math.max(4, step.length() * 2);
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.add(step.scale(i / (double) points));
            level.sendParticles(LINE, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    /** The zap: harm and a moment's slowness, and a violet bolt to see it by. */
    public void zap(ServerLevel level, Player target) {
        target.hurtServer(level, ModDamageTypes.phlogistonBeam(this), DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0), this);
        PhlogistonBeam bolt = ModEntities.PHLOGISTON_BEAM.get().create(level, EntitySpawnReason.EVENT);
        bolt.aim(position(), target.getEyePosition().subtract(0, 0.4, 0), COLOUR);
        level.addFreshEntity(bolt);
        playSound(ModSounds.SENTINEL_ZAP.get(), 1.5F, 1.2F);
        charge = 0;
        cooldown = COOLDOWN;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("Mothership", UUIDUtil.CODEC, mothership);
        output.storeNullable("Landing", UUIDUtil.CODEC, landing);
        output.storeNullable("LandingAt", BlockPos.CODEC, landingAt);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        mothership = input.read("Mothership", UUIDUtil.CODEC).orElse(null);
        landing = input.read("Landing", UUIDUtil.CODEC).orElse(null);
        landingAt = input.read("LandingAt", BlockPos.CODEC).orElse(null);
    }
}
