package com.hollowfrontier.content.ufo;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Gray saucer (13a): one of the Concavian craft the Grays took, a disc with a lit rim and a glass dome, over the
 * Overworld at night. Most just cross the sky, fast, low enough to see well ({@link #flyover}). Some come to a stop over a
 * spot ({@link #abduction}), {@link #BEAM_HEIGHT} up, and let down a beam of pale light for up to {@link #BEAM_TICKS}:
 * the first animal in its column (or a player standing in it who isn't sneaking, when the config allows) is drawn up
 * into the craft and taken ({@link Abductions#take}). Sneaking or stepping out of the column breaks the beam's hold, and
 * the light lets its catch down gently (slow falling). Then it goes, up and away. Nothing harms it.
 * Everyone within {@link #WITNESS_RANGE} sees it.
 * <p>
 * Once the leash is broken (15a), some come down instead ({@link #land}): straight down over a clearing, slowing to
 * {@link #HOVER} above the ground, and there it waits out the night as a {@link GrayLandings Gray Landing}, which says
 * when it lifts off. Only a landing saucer is saved; the rest come and go in one sitting.
 */
public class GraySaucer extends Entity implements GeoEntity {
    public static final double SPEED = 0.9;
    public static final double HALF_RUN = 96.0;
    public static final double BEAM_HEIGHT = 18.0;
    public static final double BEAM_RADIUS = 2.5;
    public static final int BEAM_TICKS = 240;
    public static final double LIFT = 0.14;
    public static final double WITNESS_RANGE = 128.0;
    private static final DustParticleOptions LIGHT = new DustParticleOptions(0xC8FFD8, 1.2F);
    private static final EntityDataAccessor<Boolean> DATA_BEAMING = SynchedEntityData.defineId(GraySaucer.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation BEAMING = RawAnimation.begin().thenLoop("beam");

    /** How far up a landing saucer hangs over the ground, how high it starts down from, and how fast it comes. */
    public static final double HOVER = 1.5, DESCENT_HEIGHT = 40.0, DESCENT_SPEED = 0.35;

    public enum Run { CROSS, ARRIVE, BEAM, LEAVE, DESCEND, LANDED }

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> witnesses = new HashSet<>();
    private Run run = Run.CROSS;
    /** A landing's: the ground it sets down on, the day it came (it goes at the next dawn), and its cache. */
    private @Nullable BlockPos ground;
    private long landedDay;
    private @Nullable BlockPos cache;
    private Vec3 heading = new Vec3(SPEED, 0, 0);
    private @Nullable Vec3 stopOver;
    private int ticksLeft = (int) (2 * HALF_RUN / SPEED);
    private int beamTicks;
    private @Nullable LivingEntity caught;
    private @Nullable LivingEntity taken;

    public GraySaucer(EntityType<? extends GraySaucer> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Across the sky along {@code direction}, and gone. */
    public void flyover(Vec3 direction) {
        run = Run.CROSS;
        setHeading(direction);
    }

    /** In along {@code direction} to stop over {@code spot} and beam; it must start {@link #BEAM_HEIGHT} above it. */
    public void abduction(Vec3 direction, Vec3 spot) {
        run = Run.ARRIVE;
        stopOver = spot;
        setHeading(direction);
        ticksLeft = (int) (3 * HALF_RUN / SPEED);
    }

    /**
     * Straight down onto {@code ground} (the top block of the clearing), to land there on {@code day}; it must start over
     * it. {@link GrayLandings#touchdown} runs when it gets there.
     */
    public void land(BlockPos ground, long day) {
        run = Run.DESCEND;
        this.ground = ground.immutable();
        landedDay = day;
    }

    /** Lifts off, along {@code direction}, from wherever it is (a landing's end). */
    public void liftOff(ServerLevel level, Vec3 direction) {
        setHeading(direction);
        leave(level);
    }

    public @Nullable BlockPos ground() {
        return ground;
    }

    public long landedDay() {
        return landedDay;
    }

    /** For the tests: when it came down. */
    public void setLandedDay(long day) {
        landedDay = day;
    }

    public @Nullable BlockPos cache() {
        return cache;
    }

    public void setCache(@Nullable BlockPos cache) {
        this.cache = cache;
    }

    /** Where it hangs once it's down. */
    public Vec3 restingPlace() {
        return Vec3.atBottomCenterOf(ground).add(0, 1 + HOVER, 0);
    }

    private void setHeading(Vec3 direction) {
        Vec3 flat = new Vec3(direction.x, 0, direction.z);
        heading = (flat.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : flat.normalize()).scale(SPEED);
        setYRot((float) (Mth.atan2(-heading.x, heading.z) * Mth.RAD_TO_DEG));
    }

    public Run run() {
        return run;
    }

    public boolean isBeaming() {
        return entityData.get(DATA_BEAMING);
    }

    public @Nullable LivingEntity caught() {
        return caught;
    }

    /** What it took, if it has. */
    public @Nullable LivingEntity taken() {
        return taken;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BEAMING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            setPos(position().add(getDeltaMovement()));
            return;
        }
        switch (run) {
            case CROSS -> setDeltaMovement(heading);
            case ARRIVE -> {
                Vec3 to = new Vec3(stopOver.x - getX(), 0, stopOver.z - getZ());
                if (to.length() <= SPEED) {
                    setPos(stopOver.x, getY(), stopOver.z);
                    setDeltaMovement(Vec3.ZERO);
                    run = Run.BEAM;
                    entityData.set(DATA_BEAMING, true);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 6.0F, 0.5F);
                } else {
                    setDeltaMovement(to.normalize().scale(SPEED));
                }
            }
            case BEAM -> beam(level);
            case LEAVE -> setDeltaMovement(heading.scale(1.6).add(0, 0.4, 0));
            case DESCEND -> {
                if (ground == null) {
                    discard();
                    return;
                }
                double drop = getY() - restingPlace().y;
                if (drop <= DESCENT_SPEED) {
                    setPos(restingPlace());
                    setDeltaMovement(Vec3.ZERO);
                    run = Run.LANDED;
                    GrayLandings.touchdown(level, this);
                } else {
                    // easing in over the last few blocks
                    setDeltaMovement(0, -Math.max(0.06, Math.min(DESCENT_SPEED, drop * 0.08)), 0);
                }
            }
            case LANDED -> {
                setDeltaMovement(Vec3.ZERO);
                if (ground == null) {
                    discard();
                    return;
                }
                if (tickCount % 20 == 0) {
                    GrayLandings.watch(level, this);
                }
            }
        }
        if ((run == Run.CROSS || run == Run.ARRIVE || run == Run.LEAVE) && --ticksLeft <= 0) {
            release();
            discard();
            return;
        }
        setPos(position().add(getDeltaMovement()));
        if (tickCount % 50 == 0) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.SIGHTING_HUM.get(), SoundSource.AMBIENT, 6.0F, 1.1F);
        }
        if (tickCount % 20 == 0) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceTo(this) < WITNESS_RANGE && witnesses.add(player.getUUID())) {
                    ModCriteria.fire(player, ModCriteria.SAUCER_SEEN);
                }
            }
        }
    }

    /** The column under it that the beam fills. */
    public AABB column() {
        return new AABB(getX() - BEAM_RADIUS, getY() - BEAM_HEIGHT - 3, getZ() - BEAM_RADIUS, getX() + BEAM_RADIUS, getY(), getZ() + BEAM_RADIUS);
    }

    /** Whether the beam can take hold of this. */
    public static boolean beamable(LivingEntity entity) {
        if (entity instanceof Player player) {
            return UfoConfig.abductPlayers() && !player.isCreative() && !player.isSpectator() && !player.isShiftKeyDown();
        }
        return entity instanceof Animal && !entity.isPassenger();
    }

    private void beam(ServerLevel level) {
        setDeltaMovement(new Vec3(0, Math.sin(tickCount * 0.1) * 0.01, 0));
        for (int i = 0; i < 14; i++) {
            double y = getY() - getRandom().nextDouble() * BEAM_HEIGHT;
            double a = getRandom().nextDouble() * Math.PI * 2, r = getRandom().nextDouble() * BEAM_RADIUS * 0.8;
            level.sendParticles(LIGHT, getX() + Math.cos(a) * r, y, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        if (caught == null || !caught.isAlive()) {
            caught = level.getEntitiesOfClass(LivingEntity.class, column(), e -> e.isAlive() && beamable(e)).stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(getX(), e.getY(), getZ()))).orElse(null);
        } else if (!column().contains(caught.position()) || !beamable(caught)) {
            if (caught instanceof ServerPlayer player && player.isShiftKeyDown()) {
                ModCriteria.fire(player, ModCriteria.BEAM_ESCAPED);
            }
            release();
        }
        if (caught != null) {
            lift(caught);
            if (caught.getY() + caught.getBbHeight() >= getY() - 1.0) {
                taken = caught;
                caught = null;
                Abductions.take(level, this, taken);
                leave(level);
                return;
            }
        }
        if (++beamTicks >= BEAM_TICKS) {
            release();
            leave(level);
        }
    }

    /** Up the beam, and in toward its middle. */
    private void lift(LivingEntity target) {
        Vec3 in = new Vec3(getX() - target.getX(), 0, getZ() - target.getZ()).scale(0.08);
        target.resetFallDistance();
        if (target instanceof ServerPlayer player) {
            // A player's movement is their own client's: lift them with levitation, which their client plays out
            if (tickCount % 5 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 12, 4, true, false, false));
            }
        } else {
            target.setDeltaMovement(in.x, LIFT, in.z);
            target.needsSync = true;
        }
    }

    /** Lets its catch go, down gently. */
    private void release() {
        if (caught != null) {
            caught.removeEffect(MobEffects.LEVITATION);
            caught.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, true, false, true));
            caught = null;
        }
    }

    private void leave(ServerLevel level) {
        run = Run.LEAVE;
        entityData.set(DATA_BEAMING, false);
        ticksLeft = 100;
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 6.0F, 0.5F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /** Only a landing is saved (it waits out the night where it set down); the rest are gone in a minute. */
    @Override
    public boolean shouldBeSaved() {
        return isLanding() && super.shouldBeSaved();
    }

    /** Coming down or down, as a Gray Landing. */
    public boolean isLanding() {
        return run == Run.DESCEND || run == Run.LANDED;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 320.0 * 320.0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        ground = input.read("Ground", BlockPos.CODEC).orElse(null);
        if (ground != null) {
            run = input.getStringOr("Run", "").equals("DESCEND") ? Run.DESCEND : Run.LANDED;
            landedDay = input.getLongOr("Day", 0L);
            cache = input.read("Cache", BlockPos.CODEC).orElse(null);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (isLanding() && ground != null) {
            output.putString("Run", run.name());
            output.store("Ground", BlockPos.CODEC, ground);
            output.putLong("Day", landedDay);
            output.storeNullable("Cache", BlockPos.CODEC, cache);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GraySaucer>("main", 5, state ->
                state.setAndContinue(state.animatable().isBeaming() ? BEAMING : SPIN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** For the tests: a saucer already stopped over its spot. */
    public void beamHere() {
        run = Run.BEAM;
        stopOver = position();
        entityData.set(DATA_BEAMING, true);
    }
}
