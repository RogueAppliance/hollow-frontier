package com.hollowfrontier.content.hollow.sea;

import java.util.EnumSet;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Basilosaurus (the Still Sea's deep water, 43b): a whale as long as a street and as thin as an eel, all jaw at the front.
 * The sea's hunter. It goes for anyone swimming or boating over deep water ({@link #isPrey}): it circles first, a slow
 * ring ten blocks out, then comes in fast and bites ({@link HuntGoal}), and circles again. Hit a boat's rider and the boat
 * takes it too. It leaves the shallows alone, and it gives up on prey that has got ashore or into shallow water for
 * {@link #GIVE_UP_TICKS}. Hurt, it turns on whoever did it wherever they are. Drops Blubber, and a tooth to a hunter.
 */
public class Basilosaurus extends SeaBeast {
    private static final EntityDataAccessor<Boolean> DATA_STRIKING = SynchedEntityData.defineId(Basilosaurus.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation BITE = RawAnimation.begin().thenLoop("attack.bite");
    /** Water this deep (bed to surface) under its prey, or it isn't prey. */
    public static final int DEEP = 6;
    public static final double CIRCLE_RADIUS = 10.0;
    public static final int CIRCLE_MIN = 60, CIRCLE_MAX = 110, STRIKE_TICKS = 70, GIVE_UP_TICKS = 60;
    /** What a boat (or anything else it's ridden in) takes when its rider is bitten. */
    public static final float BOAT_DAMAGE = 6.0F;
    /** What a Steam Submersible's hull takes from a ram (of its 40): five in a row break it up. */
    public static final float RAM_DAMAGE = 8.0F;

    public Basilosaurus(EntityType<? extends Basilosaurus> type, Level level) {
        super(type, level, 3);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 1.1)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STRIKING, false);
    }

    public boolean isStriking() {
        return entityData.get(DATA_STRIKING);
    }

    void setStriking(boolean striking) {
        entityData.set(DATA_STRIKING, striking);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new HuntGoal(this));
        goalSelector.addGoal(5, new SwimAtDepthGoal(this, 0.7, 30, 8, 10, 0.0F)); // the deep: well under the surface, near the bed
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 20, true, false, (target, level) -> isPrey(target)));
    }

    /** Something worth hunting: alive, not in creative, in the water or riding something that is, over deep water. */
    public static boolean isPrey(LivingEntity target) {
        if (!target.isAlive() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            return false;
        }
        Entity afloat = target.getVehicle() != null ? target.getVehicle() : target;
        return SeaLife.columnDepth(target.level(), afloat.blockPosition()) >= DEEP
                || SeaLife.columnDepth(target.level(), afloat.blockPosition().below()) >= DEEP; // a boat floats on top of it
    }

    /** Bites: the target is hurt as by any mob, and what it rides is hurt with it. */
    public boolean bite(ServerLevel level, LivingEntity target) {
        // A submersible's crew is behind brass: the hull takes it (43c), and they feel it
        if (target.getVehicle() instanceof com.hollowfrontier.content.steam.submersible.SteamSubmersible sub) {
            playSound(ModSounds.BASILOSAURUS_BITE.get(), 1.4F, 0.7F + random.nextFloat() * 0.2F);
            return sub.hurtServer(level, damageSources().mobAttack(this), RAM_DAMAGE);
        }
        boolean hurt = doHurtTarget(level, target);
        Entity vehicle = target.getVehicle();
        if (vehicle != null) {
            vehicle.hurtServer(level, damageSources().mobAttack(this), BOAT_DAMAGE);
        }
        playSound(ModSounds.BASILOSAURUS_BITE.get(), 1.4F, 0.9F + random.nextFloat() * 0.2F);
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BASILOSAURUS_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BASILOSAURUS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BASILOSAURUS_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<Basilosaurus>("bite", 4,
                test -> test.animatable().isStriking() ? test.setAndContinue(BITE) : PlayState.STOP));
    }

    /** Where the hunt is. */
    public enum Phase { CIRCLE, STRIKE }

    /**
     * The hunt: circle, strike, circle. Ticked every tick, so by phase 34's rule nothing it reads in {@link #tick} is
     * cleared there: the target is read fresh from the mob each tick and checked, and the goal's own state is reset only
     * in {@link #start} and {@link #stop}.
     */
    public static final class HuntGoal extends Goal {
        private final Basilosaurus beast;
        private Phase phase = Phase.CIRCLE;
        private int phaseTicks;
        private int lostTicks;
        private double angle;
        private int turn = 1;

        public HuntGoal(Basilosaurus beast) {
            this.beast = beast;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        public Phase phase() {
            return phase;
        }

        /** Ticks the prey has been out of reach (ashore, or in the shallows). */
        public int lostTicks() {
            return lostTicks;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = beast.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            LivingEntity target = beast.getTarget();
            Vec3 from = target == null ? Vec3.ZERO : beast.position().subtract(target.position());
            angle = Math.atan2(from.z, from.x);
            turn = beast.getRandom().nextBoolean() ? 1 : -1;
            lostTicks = 0;
            circle();
        }

        private void circle() {
            phase = Phase.CIRCLE;
            phaseTicks = CIRCLE_MIN + beast.getRandom().nextInt(CIRCLE_MAX - CIRCLE_MIN);
            beast.setStriking(false);
        }

        private void strike() {
            phase = Phase.STRIKE;
            phaseTicks = STRIKE_TICKS;
            beast.setStriking(true);
        }

        @Override
        public void tick() {
            LivingEntity target = beast.getTarget();
            if (target == null || !target.isAlive() || !(beast.level() instanceof ServerLevel level)) {
                return; // stopped on the next full tick
            }
            // Prey out of the deep water: wait about for it a little, then let it go (unless it's the one that hurt us)
            if (!isPrey(target) && beast.getLastHurtByMob() != target) {
                if (++lostTicks >= GIVE_UP_TICKS) {
                    beast.setTarget(null);
                    return;
                }
            } else {
                lostTicks = 0;
            }
            beast.getLookControl().setLookAt(target, 30.0F, 30.0F);
            phaseTicks--;
            if (phase == Phase.CIRCLE) {
                angle += turn * 0.045;
                if (beast.tickCount % 5 == 0) {
                    double x = target.getX() + Math.cos(angle) * CIRCLE_RADIUS, z = target.getZ() + Math.sin(angle) * CIRCLE_RADIUS;
                    beast.getNavigation().moveTo(x, Math.min(target.getY(), level.getSeaLevel() - 2), z, 0.9);
                }
                if (phaseTicks <= 0 && lostTicks == 0) {
                    strike();
                }
            } else {
                if (beast.tickCount % 3 == 0) {
                    beast.getNavigation().moveTo(target, 2.2);
                }
                double reach = beast.getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + 1.0;
                if (beast.distanceToSqr(target) <= reach * reach) {
                    beast.bite(level, target);
                    circle();
                } else if (phaseTicks <= 0) {
                    circle();
                }
            }
        }

        @Override
        public void stop() {
            beast.setStriking(false);
            beast.getNavigation().stop();
            phase = Phase.CIRCLE;
            lostTicks = 0;
        }
    }
}
