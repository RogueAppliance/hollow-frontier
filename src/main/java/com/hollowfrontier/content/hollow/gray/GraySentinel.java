package com.hollowfrontier.content.hollow.gray;

import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.registry.ModDamageTypes;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Gray Sentinel (10b): a probe that keeps watch over a live Gray field station and doesn't run. When it sees a player
 * (not creative or spectating) within {@link #SIGHT}, it draws a red beam on them for {@link #CHARGE_TICKS} (warning
 * enough to get behind something, which breaks it off) and fires: a jolt of {@link #DAMAGE}, Slowness III and Weakness
 * for five seconds, and an alarm that sets every tagged specimen within {@link #ALARM_RANGE} on them. Then it cools down
 * {@link #COOLDOWN} ticks. Tougher than a probe; drops Gray tech fragments. Structure-placed only.
 */
public class GraySentinel extends GrayProbe {
    public static final double SIGHT = 20.0;
    public static final int CHARGE_TICKS = 40;
    public static final int COOLDOWN = 80;
    public static final float DAMAGE = 3.0F;
    public static final int STUN_TICKS = 100;
    public static final double ALARM_RANGE = 48.0;
    private static final DustParticleOptions BEAM = new DustParticleOptions(0xFF3030, 0.5F);

    private @Nullable Player quarry;
    private int charge;
    private int cooldown;

    public GraySentinel(EntityType<? extends GraySentinel> type, Level level) {
        super(type, level);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return GrayProbe.createAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.ARMOR, 4.0);
    }

    /** It holds its post: no fleeing. */
    @Override
    public void flee(Entity from) {}

    /** How far through its charge it is (0 when idle). */
    public int charge() {
        return charge;
    }

    @Override
    protected void watch(ServerLevel level) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (quarry == null || !canStillSee(quarry)) {
            Player seen = level.getNearestPlayer(this, SIGHT);
            quarry = seen != null && canStillSee(seen) ? seen : null;
            if (quarry == null) {
                charge = 0;
                return;
            }
            playSound(ModSounds.SENTINEL_CHARGE.get(), 1.5F, 1.0F);
        }
        charge++;
        getLookControl().setLookAt(quarry, 30.0F, 30.0F);
        if (charge % 2 == 0) {
            beam(level, quarry);
        }
        if (charge >= CHARGE_TICKS) {
            fire(level, quarry);
        }
    }

    private boolean canStillSee(Player player) {
        return player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && distanceTo(player) <= SIGHT && hasLineOfSight(player);
    }

    /** The red line it draws on its quarry while it charges. */
    private void beam(ServerLevel level, Player target) {
        Vec3 from = position().add(0, getBbHeight() * 0.3, 0);
        Vec3 to = target.getEyePosition().subtract(0, 0.4, 0);
        Vec3 step = to.subtract(from);
        int points = (int) Math.max(4, step.length() * 2);
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.add(step.scale(i / (double) points));
            level.sendParticles(BEAM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    /** Jolts and slows the quarry, and rouses the specimens against them. */
    public void fire(ServerLevel level, Player target) {
        target.hurtServer(level, ModDamageTypes.phlogistonBeam(this), DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STUN_TICKS, 2), this);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STUN_TICKS, 0), this);
        playSound(ModSounds.SENTINEL_ZAP.get(), 2.0F, 1.0F);
        if (Specimens.alarm(level, target, position(), ALARM_RANGE) > 0) {
            playSound(ModSounds.SENTINEL_ALARM.get(), 3.0F, 1.0F);
        }
        charge = 0;
        cooldown = COOLDOWN;
        quarry = null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
