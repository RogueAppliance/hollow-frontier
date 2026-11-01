package com.hollowfrontier.content.hollow.creature;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Glassback Hare (Crystal Steppes, 9b2): a big buff hare with little crystals grown out along its back, which glow (they're
 * drawn emissive). Shy: it bolts from anyone within eight blocks who isn't sneaking, but a carrot held out will bring it
 * in. Phase 9c's phlogiston is the same stuff as its crystals. Drops rabbit hide, and now and then an amethyst shard.
 * <p>
 * The one penned at the abandoned Gray field lab (9b3) wears an ear tag: a yellow tab clipped to its left ear, drawn only
 * when {@link #hasEarTag()} (saved as {@code EarTag}).
 */
public class GlassbackHare extends PathfinderMob implements GeoEntity {
    public static final double SHY_RANGE = 8.0;

    private static final EntityDataAccessor<Boolean> DATA_EAR_TAG = SynchedEntityData.defineId(GlassbackHare.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public GlassbackHare(EntityType<? extends GlassbackHare> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.TEMPT_RANGE, 10.0);
    }

    public static boolean checkSpawnRules(EntityType<GlassbackHare> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return HollowCreatures.onLitGround(level, pos);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_EAR_TAG, false);
    }

    public boolean hasEarTag() {
        return entityData.get(DATA_EAR_TAG);
    }

    public void setEarTag(boolean tagged) {
        entityData.set(DATA_EAR_TAG, tagged);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (hasEarTag()) {
            output.putBoolean("EarTag", true);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setEarTag(input.getBooleanOr("EarTag", false));
    }

    public static boolean isTreat(ItemStack stack) {
        return stack.is(Items.CARROT) || stack.is(Items.GOLDEN_CARROT);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 2.0));
        goalSelector.addGoal(2, new TemptGoal(this, 1.0, GlassbackHare::isTreat, false));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, p -> !p.isShiftKeyDown() && !isTreat(p.getMainHandItem()),
                (float) SHY_RANGE, 1.4, 2.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GLASSBACK_HARE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GLASSBACK_HARE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GLASSBACK_HARE_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
