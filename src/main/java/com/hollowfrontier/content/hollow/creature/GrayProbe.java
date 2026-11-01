package com.hollowfrontier.content.hollow.creature;

import java.util.UUID;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.frontier.mystery.Mysteries;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Gray Probe (9b3): a palm-sized saucer of dull metal with a glowing band, left hovering over the abandoned Gray field
 * lab. It hangs in the air and turns to watch you, and the moment you come close (or look straight at it, or hit it) it
 * shoots up and away, faster and faster, and blinks out. It never comes back; phase 10b's live stations have sentinels
 * instead. Only structures place it (no natural spawns, no drops), except that (11a) now and then one is sent down to
 * mutilate a steer or press a crop circle: it does the job from just over it, by a beam of light, and minds being looked
 * at no longer, only someone coming close ({@link #giveJob}). Brought down mid-job, it drops Gray tech fragments
 * (MysteryEvents).
 */
public class GrayProbe extends PathfinderMob implements GeoEntity {
    /** How long a job takes, once it's over the spot; and how near a player can come before it gives up (sneaking, nearer). */
    public static final int JOB_TICKS = 200;
    public static final double JOB_SPOOK = 12.0, JOB_SPOOK_SNEAKING = 5.0;
    /** A player this close scares it off, sneaking or not. */
    public static final double NOTICE_RANGE = 10.0;
    /** A player looking straight at it from this far scares it off too. */
    public static final double STARE_RANGE = 32.0;
    /** How long it flees before it blinks out (sooner if it hits something). */
    public static final int FLEE_TICKS = 50;
    private static final EntityDataAccessor<Boolean> DATA_FLEEING = SynchedEntityData.defineId(GrayProbe.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation FLEE = RawAnimation.begin().thenLoop("move.fly");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private Vec3 fleeDirection = Vec3.ZERO;
    private int fleeTicks;
    // 11a: sent down to do a job (a mutilation or a crop circle; Mysteries.visit)
    private MysteryLog.@Nullable Kind job;
    private BlockPos jobPos = BlockPos.ZERO;
    private @Nullable UUID jobTarget;
    private int jobTicks;

    public GrayProbe(EntityType<? extends GrayProbe> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, STARE_RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLEEING, false);
    }

    public boolean isFleeing() {
        return entityData.get(DATA_FLEEING);
    }

    @Override
    protected void registerGoals() {
        // Watching: always turned toward the nearest player in sight
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, (float) STARE_RANGE, 1.0F));
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // It has no neck: the whole probe turns to face what it's watching (its eye is on the dome's front)
        yBodyRot = yHeadRot;
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (!isFleeing()) {
            // Hover in place, bobbing a little
            if (job != null) {
                work(server);
                return;
            }
            setDeltaMovement(0, Math.sin(tickCount * 0.08) * 0.01, 0);
            watch(server);
            return;
        }
        fleeTicks++;
        double speed = Math.min(1.8, 0.15 + fleeTicks * 0.05);
        setDeltaMovement(fleeDirection.scale(speed));
        if (fleeTicks > FLEE_TICKS || (fleeTicks > 5 && (horizontalCollision || verticalCollision))) {
            vanish(server);
        }
    }

    /** Sends it to work: mutilate {@code target} (a steer), or press a crop circle at {@code pos}. */
    public void giveJob(MysteryLog.Kind kind, BlockPos pos, @Nullable Entity target) {
        job = kind;
        jobPos = pos.immutable();
        jobTarget = target == null ? null : target.getUUID();
        jobTicks = 0;
    }

    public boolean hasJob() {
        return job != null;
    }

    public int jobTicks() {
        return jobTicks;
    }

    /**
     * At work: down to hang over the spot (over the steer, wherever it is), then a beam of light on it for {@link #JOB_TICKS};
     * done, it flies off. Intent on the job it doesn't mind being looked at, only a player coming close.
     */
    private void work(ServerLevel level) {
        Entity target = jobTarget == null ? null : level.getEntity(jobTarget);
        if (job == MysteryLog.Kind.MUTILATION && (target == null || !target.isAlive())) {
            flee(this);
            return;
        }
        Player spotter = level.getNearestPlayer(this, JOB_SPOOK);
        if (spotter != null && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(spotter)
                && distanceTo(spotter) < (spotter.isShiftKeyDown() ? JOB_SPOOK_SNEAKING : JOB_SPOOK)) {
            flee(spotter);
            return;
        }
        Vec3 ground = target != null ? target.position() : Vec3.atBottomCenterOf(jobPos);
        Vec3 to = ground.add(0, Mysteries.VISIT_HEIGHT, 0).subtract(position());
        if (to.length() > 1.0) {
            setDeltaMovement(to.normalize().scale(Math.min(0.5, to.length() * 0.2)));
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        if (target instanceof Mob held) {
            held.getNavigation().stop();
            held.setDeltaMovement(Vec3.ZERO);
        }
        if (jobTicks % 3 == 0) {
            for (double y = 0; y < Mysteries.VISIT_HEIGHT; y += 0.5) {
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() - y, getZ(), 1, 0.05, 0, 0.05, 0);
            }
        }
        if (++jobTicks >= JOB_TICKS) {
            if (job == MysteryLog.Kind.MUTILATION && target instanceof Cow cow) {
                Mysteries.mutilate(level, cow);
            } else if (job == MysteryLog.Kind.CROP_CIRCLE) {
                Mysteries.cropCircle(level, jobPos);
            }
            flee(this);
        }
    }

    /** While it hovers: a player who comes close or stares at it scares it off. (A sentinel keeps watch differently.) */
    protected void watch(ServerLevel level) {
        Player spotter = level.getNearestPlayer(this, STARE_RANGE);
        if (spotter != null && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(spotter)
                && (distanceTo(spotter) < NOTICE_RANGE || staredAtBy(spotter))) {
            flee(spotter);
        }
    }

    /** As an Enderman knows it's being stared at: the player's eyes on it, with a clear line of sight. */
    public boolean staredAtBy(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toProbe = new Vec3(getX() - player.getX(), getEyeY() - player.getEyeY(), getZ() - player.getZ());
        double distance = toProbe.length();
        return distance < STARE_RANGE && view.dot(toProbe.normalize()) > 1.0 - 0.025 / distance && player.hasLineOfSight(this);
    }

    /** Off it goes: up and away from {@code from}. */
    public void flee(Entity from) {
        job = null;
        if (isFleeing()) {
            return;
        }
        Vec3 away = position().subtract(from.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
        fleeDirection = away.scale(0.55).add(0, 0.85, 0).normalize();
        entityData.set(DATA_FLEEING, true);
        playSound(ModSounds.GRAY_PROBE_FLEE.get(), 1.0F, 1.0F);
    }

    protected void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.5), getZ(), 12, 0.2, 0.2, 0.2, 0.08);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.GRAY_PROBE_VANISH.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (isAlive()) {
            Entity attacker = source.getEntity();
            flee(attacker != null ? attacker : this);
        }
        return hurt;
    }

    // A job in hand is kept over a save; the steer it was after is found again by its id
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (job != null) {
            output.store("Job", MysteryLog.Kind.CODEC, job);
            output.putLong("JobPos", jobPos.asLong());
            output.putInt("JobTicks", jobTicks);
            if (jobTarget != null) {
                output.store("JobTarget", UUIDUtil.CODEC, jobTarget);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        job = input.read("Job", MysteryLog.Kind.CODEC).orElse(null);
        jobPos = BlockPos.of(input.getLongOr("JobPos", 0L));
        jobTicks = input.getIntOr("JobTicks", 0);
        jobTarget = input.read("JobTarget", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {}

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GRAY_PROBE_HUM.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GRAY_PROBE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GRAY_PROBE_VANISH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GrayProbe>("hover", 5, test -> test.setAndContinue(isFleeing() ? FLEE : HOVER)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
