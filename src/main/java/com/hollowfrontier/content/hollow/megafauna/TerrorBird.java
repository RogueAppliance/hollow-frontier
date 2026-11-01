package com.hollowfrontier.content.hollow.megafauna;

import java.util.EnumSet;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.hollow.creature.FernSkitter;
import com.hollowfrontier.content.hollow.creature.GlassbackHare;
import com.hollowfrontier.content.hollow.gray.Specimen;
import com.hollowfrontier.content.hollow.gray.Specimens;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Terror Bird (Crystal Steppes and Ember Badlands, 10a): a flightless hunter half again as tall as a man, with a hooked
 * axe of a beak. Hostile, and it hunts by day (the Hollow Earth has no night): it screeches when it picks you out, runs
 * you down a little faster than you can sprint, and leaps the last few blocks. It hunts the small game too (rabbits,
 * chickens, fern skitters, glassback hares). On a horse you can outrun it; among a mammoth herd you're safe, as it won't
 * go near mammoths. The Oliphant's blast puts it to flight ({@link #scare}). It roams singly or in pairs and comes and
 * goes like any monster, but only a few at a time ({@link #checkSpawnRules}). Drops its talon (the Talon Knife),
 * feathers and raw chicken. One the Grays have tagged (10b, {@link Specimens}) is faster still, fears no mammoth and no
 * Oliphant.
 */
public class TerrorBird extends Monster implements GeoEntity, Specimen {
    private static final EntityDataAccessor<Boolean> DATA_TAGGED = SynchedEntityData.defineId(TerrorBird.class, EntityDataSerializers.BOOLEAN);
    /** How far a terror bird keeps from mammoths. */
    public static final float MAMMOTH_BERTH = 10.0F;
    /** Natural spawning gives up if this many are already within {@link #CROWD_RANGE}. */
    public static final int CROWD = 2;
    public static final double CROWD_RANGE = 96.0;

    private static final RawAnimation SWING = RawAnimation.begin().thenPlay("attack.swing");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("move.run");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int scaredTicks;
    private @Nullable Vec3 scaredFrom;

    public TerrorBird(EntityType<? extends TerrorBird> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    /** Hunts by day on open ground, but only a few at a time, or the steppe would fill up with them. */
    public static boolean checkSpawnRules(EntityType<TerrorBird> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !Megafauna.onOpenGround(level, pos, 3)) {
            return false;
        }
        return reason != EntitySpawnReason.NATURAL
                || (random.nextInt(4) == 0 && !crowded(level, pos));
    }

    /** Whether {@link #CROWD} terror birds already roam within {@link #CROWD_RANGE} of {@code pos}. */
    public static boolean crowded(EntityGetter level, BlockPos pos) {
        return crowded(level, pos, CROWD_RANGE);
    }

    public static boolean crowded(EntityGetter level, BlockPos pos, double range) {
        return level.getEntitiesOfClass(TerrorBird.class, new AABB(pos).inflate(range)).size() >= CROWD;
    }

    /** What it hunts besides players. */
    public static boolean isPrey(LivingEntity entity) {
        return entity instanceof Rabbit || entity instanceof Chicken || entity instanceof FernSkitter || entity instanceof GlassbackHare;
    }

    /** Puts it to flight from {@code from} for {@code ticks}, dropping whatever it was after (the Oliphant). */
    public void scare(Vec3 from, int ticks) {
        if (isTagged()) {
            return; // the implant overrides its fear
        }
        scaredTicks = ticks;
        scaredFrom = from;
        setTarget(null);
        setLastHurtByMob(null);
        getNavigation().stop();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TAGGED, false);
    }

    @Override
    public boolean isTagged() {
        return entityData.get(DATA_TAGGED);
    }

    @Override
    public void setTagged(boolean tagged) {
        entityData.set(DATA_TAGGED, tagged);
        if (!level().isClientSide()) {
            Specimens.applyBoosts(this, tagged);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        Specimens.onSpawn(this, reason, level.getRandom());
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    public boolean isScared() {
        return scaredTicks > 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FleeGoal(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, WoollyMammoth.class, mammoth -> !isTagged(), MAMMOTH_BERTH, 1.0, 1.2, EntitySelector.NO_SPECTATORS::test));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.45F));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (player, level) -> !isScared() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 40, true, false,
                (prey, level) -> !isScared() && isPrey(prey)));
    }

    /** Screeches as it picks out a quarry. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        boolean newQuarry = target != null && target != getTarget();
        super.setTarget(target);
        if (newQuarry && level() instanceof ServerLevel) {
            playSound(ModSounds.TERROR_BIRD_SCREECH.get(), 2.0F, 0.9F + random.nextFloat() * 0.2F);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (scaredTicks > 0 && --scaredTicks == 0) {
            scaredFrom = null;
        }
    }

    // A daylight hunter: no liking for the dark, as other monsters have
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (scaredTicks > 0) {
            output.putInt("Scared", scaredTicks);
        }
        output.putBoolean(Specimens.SAVE_KEY, isTagged());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        scaredTicks = input.getIntOr("Scared", 0);
        setTagged(input.getBooleanOr(Specimens.SAVE_KEY, false));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.TERROR_BIRD_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.TERROR_BIRD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TERROR_BIRD_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Runs while it's after something (MeleeAttackGoal marks it aggressive), walks otherwise
        controllers.add(new AnimationController<TerrorBird>("move", 4, test -> {
            if (!test.isMoving()) {
                return test.setAndContinue(DefaultAnimations.IDLE);
            }
            return test.setAndContinue(test.animatable().isAggressive() ? RUN : DefaultAnimations.WALK);
        }));
        controllers.add(DefaultAnimations.genericAttackAnimation(SWING));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** While scared (the Oliphant), it runs off away from where the blast came from. */
    static final class FleeGoal extends Goal {
        private final TerrorBird bird;

        FleeGoal(TerrorBird bird) {
            this.bird = bird;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return bird.isScared() && bird.scaredFrom != null;
        }

        @Override
        public void tick() {
            if (bird.getNavigation().isDone() && bird.scaredFrom != null) {
                Vec3 away = DefaultRandomPos.getPosAway(bird, 20, 7, bird.scaredFrom);
                if (away != null) {
                    bird.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
                }
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            bird.getNavigation().stop();
        }
    }
}
