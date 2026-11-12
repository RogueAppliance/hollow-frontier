package com.hollowfrontier.content.ufo;

import java.util.EnumSet;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.hollow.megafauna.Megafauna;
import com.hollowfrontier.content.concavia.Kindled;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Gray Warden (13a): a Gray of the caste that keeps their sites, tall and thin with a great head and black eyes, armed
 * with a ray pistol. It keeps {@link #NEAR}-{@link #FAR} blocks from its quarry, backing off when they close; it raises
 * the pistol for {@link #AIM_TICKS} (losing sight spoils the shot) and fires a green ray ({@link #DAMAGE}, a phlogiston beam),
 * then {@link #COOLDOWN} ticks to the next. Hurt, it may blink away a few blocks ({@link #BLINK_COOLDOWN} between). It
 * goes for players and the Kindled. Two keep every crash site; a few roam the Hollow Earth's Gray-lab biomes
 * ({@link #checkSpawnRules}).
 */
public class GrayWarden extends Monster implements GeoEntity {
    public static final double NEAR = 6.0, FAR = 14.0, RANGE = 24.0;
    public static final int AIM_TICKS = 12, COOLDOWN = 30;
    public static final float DAMAGE = 5.0F;
    public static final int BLINK_COOLDOWN = 80;
    private static final DustParticleOptions RAY = new DustParticleOptions(0x60FF80, 0.7F);
    private static final EntityDataAccessor<Boolean> DATA_AIMING = SynchedEntityData.defineId(GrayWarden.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation AIM = RawAnimation.begin().thenLoop("aim");

    /** A Warden with no pistol (the surgeons aboard a ship, 50): it closes and lays on with a rod instead. */
    public static final String UNARMED = "hollowfrontier.unarmed";
    public static final float PROD_DAMAGE = 3.0F;
    public static final int PROD_COOLDOWN = 30;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int blinkCooldown;
    private int stunned;
    private java.util.List<BlockPos> round = java.util.List.of();
    private int roundAt;
    private boolean roundLoops;
    private double roundPace = 0.75;

    public GrayWarden(EntityType<? extends GrayWarden> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /**
     * Natural spawns (the Hollow Earth's Gray-lab biomes): on open ground, under the inner sun's light as well as in the dark,
     * one roll in three, and never more than two within 32 blocks.
     */
    public static boolean checkSpawnRules(EntityType<GrayWarden> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !Megafauna.onOpenGround(level, pos, 2)) {
            return false;
        }
        return reason != EntitySpawnReason.NATURAL
                || (random.nextInt(3) == 0 && level.getEntitiesOfClass(GrayWarden.class, new AABB(pos).inflate(32)).size() < 2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AIMING, false);
    }

    public boolean isAiming() {
        return entityData.get(DATA_AIMING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new StunnedGoal(this));
        goalSelector.addGoal(2, new RayGoal(this));
        goalSelector.addGoal(2, new ProdGoal(this));
        goalSelector.addGoal(4, new RoundGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, GrayWarden.class).setAlertOthers(GrayWarden.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Kindled.class, true));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isAlive() && blinkCooldown <= 0 && getRandom().nextInt(2) == 0) {
            blink();
        }
        return hurt;
    }

    public boolean isUnarmed() {
        return entityTags().contains(UNARMED);
    }

    /** Out of the fight for {@code ticks} (a Shock Rod, 50): it drops its quarry, stands where it is and can't blink. */
    public void stun(int ticks) {
        stunned = Math.max(stunned, ticks);
        setTarget(null);
        getNavigation().stop();
        entityData.set(DATA_AIMING, false);
    }

    public boolean isStunned() {
        return stunned > 0;
    }

    /**
     * Where it walks when it has nobody to go after (50): each point in turn, round again if {@code loops}, else it stops at
     * the last. Not saved: whoever posts it posts it again.
     */
    public void setRound(java.util.List<BlockPos> points, boolean loops) {
        setRound(points, loops, 0.75);
    }

    /** As above, at {@code pace} (a speed modifier: 0.75 is a watchman's walk, 1.1 a run). */
    public void setRound(java.util.List<BlockPos> points, boolean loops, double pace) {
        round = java.util.List.copyOf(points);
        roundAt = 0;
        roundLoops = loops;
        roundPace = pace;
    }

    public boolean hasRound() {
        return !round.isEmpty();
    }

    /** Whether it has walked its round to the end (one that loops never has). */
    public boolean roundDone() {
        return !round.isEmpty() && !roundLoops && roundAt >= round.size() - 1 && blockPosition().closerThan(round.get(round.size() - 1), 2.5);
    }

    /** A few blocks off in a flash, if there's footing there; whether it went. */
    public boolean blink() {
        if (isStunned()) {
            return false;
        }
        for (int i = 0; i < 12; i++) {
            double a = getRandom().nextDouble() * Math.PI * 2, d = 4 + getRandom().nextDouble() * 4;
            if (randomTeleport(getX() + Math.cos(a) * d, getY() + getRandom().nextInt(3) - 1, getZ() + Math.sin(a) * d, true, BlockTags.ENDERMAN_DOES_NOT_TELEPORT_TO)) {
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.6F);
                blinkCooldown = BLINK_COOLDOWN;
                return true;
            }
        }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (blinkCooldown > 0) {
            blinkCooldown--;
        }
        if (stunned > 0 && !level().isClientSide()) {
            stunned--;
            if (stunned % 5 == 0 && level() instanceof ServerLevel server) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, getX(), getY(1.0), getZ(), 3, 0.25, 0.4, 0.25, 0.02);
            }
        }
    }

    /** The ray: a green line from the pistol, and the harm. */
    public void fire(ServerLevel level, LivingEntity target) {
        Vec3 from = getEyePosition().subtract(0, 0.4, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Vec3 step = to.subtract(from);
        int points = (int) Math.max(4, step.length() * 3);
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.add(step.scale(i / (double) points));
            level.sendParticles(RAY, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        PhlogistonGunItem.fireSound(level, this, 1.1F, SoundSource.HOSTILE);
        target.hurtServer(level, ModDamageTypes.phlogistonBeam(this), DAMAGE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GrayWarden>("main", 4, state -> {
            if (state.animatable().isAiming()) {
                return state.setAndContinue(AIM);
            }
            return state.setAndContinue(state.isMoving() ? DefaultAnimations.WALK : DefaultAnimations.IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Keeps its distance from its quarry, aims and fires. */
    static class RayGoal extends Goal {
        private final GrayWarden warden;
        private int aim;
        private int cooldown;

        RayGoal(GrayWarden warden) {
            this.warden = warden;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = warden.getTarget();
            return target != null && target.isAlive() && !warden.isUnarmed() && !warden.isStunned();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            aim = 0;
            warden.entityData.set(DATA_AIMING, false);
            warden.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = warden.getTarget();
            if (target == null || !(warden.level() instanceof ServerLevel level)) {
                return;
            }
            warden.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = warden.distanceTo(target);
            boolean sees = warden.getSensing().hasLineOfSight(target);
            if (distance > FAR || !sees) {
                warden.getNavigation().moveTo(target, 1.0);
            } else if (distance < NEAR) {
                Vec3 away = warden.position().subtract(target.position()).normalize().scale(4);
                warden.getNavigation().moveTo(warden.getX() + away.x, warden.getY(), warden.getZ() + away.z, 1.1);
            } else {
                warden.getNavigation().stop();
            }
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (!sees || distance > RANGE) {
                aim = 0;
                warden.entityData.set(DATA_AIMING, false);
                return;
            }
            warden.entityData.set(DATA_AIMING, true);
            if (++aim >= AIM_TICKS) {
                warden.fire(level, target);
                aim = 0;
                cooldown = COOLDOWN;
                warden.entityData.set(DATA_AIMING, false);
            }
        }
    }

    /** Stunned: it stands where it is and does nothing else. */
    static class StunnedGoal extends Goal {
        private final GrayWarden warden;

        StunnedGoal(GrayWarden warden) {
            this.warden = warden;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return warden.isStunned();
        }

        @Override
        public void start() {
            warden.getNavigation().stop();
        }
    }

    /** An unarmed Warden closes with its quarry and lays on with a rod: a jolt, and a moment of slowness. */
    static class ProdGoal extends Goal {
        private final GrayWarden warden;
        private int cooldown;

        ProdGoal(GrayWarden warden) {
            this.warden = warden;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = warden.getTarget();
            return target != null && target.isAlive() && warden.isUnarmed() && !warden.isStunned();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            warden.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = warden.getTarget();
            if (target == null || !(warden.level() instanceof ServerLevel level)) {
                return;
            }
            warden.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (warden.tickCount % 5 == 0) {
                warden.getNavigation().moveTo(target, 1.15);
            }
            if (cooldown > 0) {
                cooldown--;
            } else if (warden.distanceToSqr(target) <= 4.0 && warden.getSensing().hasLineOfSight(target)) {
                target.hurtServer(level, ModDamageTypes.phlogistonBeam(warden), PROD_DAMAGE);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 30, 1), warden);
                warden.playSound(SoundEvents.BEE_STING, 1.0F, 0.6F);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 8, 0.3, 0.4, 0.3, 0.1);
                cooldown = PROD_COOLDOWN;
            }
        }
    }

    /** With nobody to go after, it walks the round it was posted to. */
    static class RoundGoal extends Goal {
        private final GrayWarden warden;

        RoundGoal(GrayWarden warden) {
            this.warden = warden;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return warden.hasRound() && warden.getTarget() == null && !warden.isStunned() && !warden.roundDone();
        }

        @Override
        public void stop() {
            warden.getNavigation().stop();
        }

        @Override
        public void tick() {
            BlockPos to = warden.round.get(Math.min(warden.roundAt, warden.round.size() - 1));
            if (warden.blockPosition().closerThan(to, 2.5)) {
                if (warden.roundAt < warden.round.size() - 1) {
                    warden.roundAt++;
                } else if (warden.roundLoops) {
                    warden.roundAt = 0;
                }
            } else if (warden.tickCount % 20 == 0 || (warden.getNavigation().isDone() && warden.tickCount % 5 == 0)) {
                // (Not every tick: a way that can't be found yet would be looked for twenty times a second)
                warden.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, warden.roundPace);
            }
        }
    }
}
