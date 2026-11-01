package com.hollowfrontier.content.frontier.wildlife;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.gear.SnakeskinBoots;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Desert and badlands rattlesnake. Leaves you be at a distance, but coils up and rattles when you come within
 * RATTLE_RANGE (a warning) and strikes if you step within STRIKE_RANGE or hit it. The bite poisons. It gives up
 * once you back off past GIVE_UP_RANGE. Drops snakeskin. Snakeskin Boots wearers are only bitten if they provoke it,
 * and the bite doesn't poison them. It rattles at cats and backs away from them, so towns with cats are safer at the edges.
 */
public class Rattlesnake extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_RATTLING = SynchedEntityData.defineId(Rattlesnake.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_STRIKE_TICKS = SynchedEntityData.defineId(Rattlesnake.class, EntityDataSerializers.INT);
    private static final RawAnimation RATTLE = RawAnimation.begin().thenLoop("misc.rattle");
    private static final RawAnimation STRIKE = RawAnimation.begin().thenPlay("attack.strike");
    public static final double RATTLE_RANGE = 6.0;
    public static final double STRIKE_RANGE = 2.5;
    private static final double GIVE_UP_RANGE = 7.0;
    public static final double CAT_RANGE = 6.0;
    private static final int STRIKE_TICKS = 7;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public Rattlesnake(EntityType<? extends Rattlesnake> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    /** Spawns on sand, terracotta or the usual grass and dirt (savanna edges), in any light. */
    public static boolean checkSpawnRules(EntityType<Rattlesnake> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(BlockTags.SAND) || below.is(BlockTags.TERRACOTTA) || below.is(BlockTags.ANIMALS_SPAWNABLE_ON);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RATTLING, false);
        builder.define(DATA_STRIKE_TICKS, 0);
    }

    public boolean isRattling() {
        return entityData.get(DATA_RATTLING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4, false));
        // Slinks away from cats the way creepers do (but one already striking, or hurt by a cat, still bites back).
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, (float) CAT_RANGE, 1.0, 1.3));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, (float) RATTLE_RANGE));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, true, false,
                (target, level) -> target.distanceToSqr(this) < STRIKE_RANGE * STRIKE_RANGE && !SnakeskinBoots.isWornBy(target)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        int strike = entityData.get(DATA_STRIKE_TICKS);
        if (strike > 0) {
            entityData.set(DATA_STRIKE_TICKS, strike - 1);
        }
        // Back off far enough and it lets you go (unless you hurt it).
        LivingEntity target = getTarget();
        if (target != null && target != getLastHurtByMob() && distanceToSqr(target) > GIVE_UP_RANGE * GIVE_UP_RANGE) {
            setTarget(null);
        }
        if (tickCount % 5 == 0) {
            Player near = level().getNearestPlayer(getX(), getY(), getZ(), RATTLE_RANGE, p -> !p.isSpectator() && !((Player) p).isCreative());
            boolean catNear = !level().getEntitiesOfClass(Cat.class, getBoundingBox().inflate(CAT_RANGE)).isEmpty();
            boolean rattling = getTarget() != null || near != null || catNear;
            entityData.set(DATA_RATTLING, rattling);
            if (rattling && getTarget() == null && !catNear) {
                getNavigation().stop(); // coiled, holding its ground (unless it's backing away from a cat)
            }
            if (rattling && tickCount % 20 == 0) {
                playSound(ModSounds.RATTLESNAKE_RATTLE.get(), 1.0F, 1.0F);
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        entityData.set(DATA_STRIKE_TICKS, STRIKE_TICKS);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living && !SnakeskinBoots.isWornBy(living)) {
            int seconds = level.getDifficulty() == Difficulty.HARD ? 8 : level.getDifficulty() == Difficulty.NORMAL ? 5 : 3;
            living.addEffect(new MobEffectInstance(MobEffects.POISON, seconds * 20), this);
        }
        return hit;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RATTLESNAKE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RATTLESNAKE_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
        controllers.add(new AnimationController<Rattlesnake>("rattle", 4,
                test -> test.animatable().isRattling() ? test.setAndContinue(RATTLE) : PlayState.STOP));
        controllers.add(new AnimationController<Rattlesnake>("strike",
                test -> test.animatable().entityData.get(DATA_STRIKE_TICKS) > 0 ? test.setAndContinue(STRIKE) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
