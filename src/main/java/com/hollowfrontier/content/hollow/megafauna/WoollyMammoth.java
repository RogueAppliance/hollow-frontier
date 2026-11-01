package com.hollowfrontier.content.hollow.megafauna;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
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
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Woolly Mammoth (Concavian Highlands and Crystal Steppes, 10a): a shaggy herd beast taller than a house door, with
 * curling tusks. Neutral: leave it be and it grazes; hurt one and it trumpets and charges, tusks first (a heavy blow that
 * throws you back), and the whole herd with it. Herds keep together. Terror birds give mammoths a wide berth, so a herd
 * is a safe place on the steppe. Drops its tusks (the Oliphant), mammoth meat and leather. One the Grays have tagged (10b,
 * {@link Specimens}) goes for players on sight.
 */
public class WoollyMammoth extends PathfinderMob implements GeoEntity, Specimen {
    private static final EntityDataAccessor<Boolean> DATA_TAGGED = SynchedEntityData.defineId(WoollyMammoth.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation SWING = RawAnimation.begin().thenPlay("attack.swing");
    private static final RawAnimation TRUMPET = RawAnimation.begin().thenPlay("misc.trumpet");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public WoollyMammoth(EntityType<? extends WoollyMammoth> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean checkSpawnRules(EntityType<WoollyMammoth> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return Megafauna.onOpenGround(level, pos, 4);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(3, new Megafauna.FollowHerdGoal<>(this, WoollyMammoth.class, 12.0, 32.0, 0.8));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, Specimens.huntWhenTagged(this));
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

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean(Specimens.SAVE_KEY, isTagged());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setTagged(input.getBooleanOr(Specimens.SAVE_KEY, false));
    }

    /** Trumpets as it takes up a quarrel (the herd's too, as each is alerted). */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        boolean newQuarrel = target != null && getTarget() == null;
        super.setTarget(target);
        if (newQuarrel && level() instanceof ServerLevel) {
            playSound(ModSounds.MAMMOTH_TRUMPET.get(), 2.5F, 0.9F + random.nextFloat() * 0.2F);
            triggerAnim("trumpet", "trumpet");
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MAMMOTH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MAMMOTH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MAMMOTH_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    public int getMaxHeadYRot() {
        return 30;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
        controllers.add(DefaultAnimations.genericAttackAnimation(SWING));
        controllers.add(new AnimationController<WoollyMammoth>("trumpet", 4, test -> PlayState.STOP)
                .triggerableAnim("trumpet", TRUMPET));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
