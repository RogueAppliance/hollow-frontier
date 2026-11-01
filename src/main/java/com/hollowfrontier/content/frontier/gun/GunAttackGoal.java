package com.hollowfrontier.content.frontier.gun;

import java.util.EnumSet;

import com.geckolib.animatable.GeoItem;
import com.hollowfrontier.content.frontier.gun.GunWielder.GunPose;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/**
 * Ranged gun AI for outlaws and lawmen: close to ~70% of gun range with line of sight, then stop, raise the gun
 * (AIM_TICKS) and shoot; reload when the gun runs dry. The mob's GunPose drives its arm animations. Mobs have infinite
 * ammo; reloading is a pause of magazine * loadTicks. The held gun's RELOAD_START/LAST_FIRED components are set so its
 * GeckoLib model plays the same animations as a player's.
 */
public class GunAttackGoal<T extends PathfinderMob & GunWielder> extends Goal {
    private final T mob;
    private final double speed;
    private int cooldown;
    private int rounds;
    private int seeTime;
    private boolean reloading;
    private boolean inPosition;
    private int aimTime;
    // Ticks the gun is held up before the first shot, so players see it coming.
    private static final int AIM_TICKS = 10;

    public GunAttackGoal(T mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.getMainHandItem().getItem() instanceof GunItem; // not a dynamiter's stick or a safecracker's pick
    }

    @Override
    public void start() {
        rounds = mob.gunStats().magazine();
        cooldown = 20;
        seeTime = 0;
        GeoItem.getOrAssignId(mob.getMainHandItem(), (ServerLevel) mob.level());
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        mob.setGunPose(GunPose.LOWERED);
        inPosition = false;
        aimTime = 0;
        if (reloading) {
            // Interrupted mid-reload: finish it off-screen so the next fight starts loaded.
            reloading = false;
            GunItem.setReloadStart(mob.getMainHandItem(), null);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        GunStats stats = mob.gunStats();
        double range = stats.range();
        double distSqr = mob.distanceToSqr(target);
        boolean canSee = mob.getSensing().hasLineOfSight(target);
        seeTime = canSee ? seeTime + 1 : 0;

        // Close to ~70% of range, then plant feet; hold the spot until the target leaves range or sight.
        inPosition = canSee && distSqr <= range * range * (inPosition ? 1.0 : 0.5);
        if (inPosition) {
            mob.getNavigation().stop();
        } else {
            mob.getNavigation().moveTo(target, speed);
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        aimTime = inPosition && !reloading ? aimTime + 1 : 0;
        mob.setGunPose(reloading ? GunPose.RELOADING : inPosition ? GunPose.AIMING : GunPose.LOWERED);

        if (--cooldown > 0) {
            return;
        }
        ItemStack gun = mob.getMainHandItem();
        if (reloading) {
            reloading = false;
            GunItem.setReloadStart(gun, null);
        }
        // Only shoot standing still with the gun raised for a moment.
        if (aimTime < AIM_TICKS || seeTime < 5) {
            return;
        }
        // Snap aim straight at the target; GunMechanics fires along the shooter's view vector.
        mob.lookAt(target, 360.0F, 360.0F);
        GunMechanics.fire((ServerLevel) mob.level(), mob, stats, e -> e != target && mob.holdsFireFor(e));
        GunItem.markFired(gun, mob.level().getGameTime());
        mob.onShot();
        if (--rounds <= 0) {
            rounds = stats.magazine();
            cooldown = stats.magazine() * stats.loadTicks() + mob.getRandom().nextInt(10);
            reloading = true;
            GunItem.setReloadStart(gun, mob.level().getGameTime());
            mob.onReload();
        } else {
            cooldown = stats.fireCooldown() + mob.getRandom().nextInt(10);
        }
    }
}
