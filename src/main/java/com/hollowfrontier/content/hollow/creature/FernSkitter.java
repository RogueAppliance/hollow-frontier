package com.hollowfrontier.content.hollow.creature;

import java.util.EnumSet;
import java.util.List;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.event.EventHooks;

import org.jspecify.annotations.Nullable;

/**
 * Fern Skitter (Primordial Jungle, 9b2): a chicken-sized feathered dinosaur that runs in little flocks. It keeps away from
 * anyone who isn't creeping up (sneak to get close), and when one is hurt the whole flock scatters. It has a thief's eye
 * for food: anything edible dropped nearby, it darts in, snatches and runs off with, and eats a while later unless you
 * catch it (it drops what it's carrying). Drops a feather now and then. Hand it any meat and one bite in three tames it:
 * a tame skitter isn't shy of its owner, follows them and sits when they right-click it, but strangers still spook it
 * and it's still a thief.
 */
public class FernSkitter extends TamableAnimal implements GeoEntity {
    public static final double SHY_RANGE = 7.0;
    private static final double SNATCH_RANGE = 10.0;
    /** How long it carries food before it eats it. */
    public static final int EAT_AFTER = 400;
    /** One bite of meat in this many tames it. */
    public static final int TAME_CHANCE = 3;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int carriedSince = -1;

    public FernSkitter(EntityType<? extends FernSkitter> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    public static boolean checkSpawnRules(EntityType<FernSkitter> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return HollowCreatures.onLitGround(level, pos);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.7));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        // With food in its jaws it runs from anyone; otherwise only from those not creeping up. Never from its owner.
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, p -> !isOwnedBy(p) && (isCarryingFood() || !p.isShiftKeyDown()), (float) SHY_RANGE,
                1.2, 1.6, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test));
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.2, 6.0F, 2.0F));
        goalSelector.addGoal(4, new SnatchFoodGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Any meat tames it (the vanilla meat tag, which the mod's mammoth meat joins). */
    public static boolean isTamingFood(ItemStack stack) {
        return stack.is(ItemTags.MEAT);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isTamingFood(stack) && !isTame()) {
            usePlayerItem(player, hand, stack);
            playSound(SoundEvents.GENERIC_EAT.value(), 0.5F, 1.5F);
            if (level() instanceof ServerLevel) {
                tryToTame(player);
            }
            return InteractionResult.SUCCESS;
        }
        if (isTame() && isOwnedBy(player) && isTamingFood(stack) && getHealth() < getMaxHealth()) {
            feed(player, hand, stack, 1.0F, 2.0F);
            return InteractionResult.SUCCESS;
        }
        InteractionResult result = super.mobInteract(player, hand);
        if (!result.consumesAction() && isTame() && isOwnedBy(player)) {
            if (level() instanceof ServerLevel) {
                setOrderedToSit(!isOrderedToSit());
                getNavigation().stop();
            }
            return InteractionResult.SUCCESS;
        }
        return result;
    }

    private void tryToTame(Player player) {
        if (random.nextInt(TAME_CHANCE) == 0 && !EventHooks.onAnimalTame(this, player)) {
            tame(player);
            getNavigation().stop();
            setOrderedToSit(true);
            level().broadcastEntityEvent(this, (byte) 7);
        } else {
            level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    // Tamed for company, not bred: no breeding food, no young, and no babies in a spawned flock
    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal partner) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        return super.finalizeSpawn(level, difficulty, reason, groupData == null ? new AgeableMobGroupData(false) : groupData);
    }

    /** Wild ones still come and go (animals normally stay put); a tame one stays. */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !isTame();
    }

    public boolean isCarryingFood() {
        return !getMainHandItem().isEmpty();
    }

    /** Takes one of what's in the item entity into its jaws. */
    public void snatch(ItemEntity item) {
        ItemStack stack = item.getItem();
        setItemInHand(InteractionHand.MAIN_HAND, stack.split(1));
        if (stack.isEmpty()) {
            item.discard();
        }
        carriedSince = tickCount;
        playSound(SoundEvents.ITEM_PICKUP, 0.4F, 1.6F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel level && isCarryingFood() && carriedSince >= 0 && tickCount - carriedSince > EAT_AFTER) {
            eat(level);
        }
    }

    /** Gulps down what it's carrying: crumbs fly, and it's the better for it. */
    public void eat(ServerLevel level) {
        ItemStack food = getMainHandItem();
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food.getItem()), getX(), getY() + 0.4, getZ(), 6, 0.1, 0.1, 0.1, 0.05);
        playSound(SoundEvents.GENERIC_EAT.value(), 0.5F, 1.5F);
        setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        heal(2.0F);
        carriedSince = -1;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker) {
            // The flock scatters
            for (FernSkitter other : level.getEntitiesOfClass(FernSkitter.class, getBoundingBox().inflate(10.0), s -> s != this)) {
                other.setLastHurtByMob(attacker);
            }
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (isCarryingFood()) {
            spawnAtLocation(level, getMainHandItem());
            setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.FERN_SKITTER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.FERN_SKITTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.FERN_SKITTER_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.1F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Darts to dropped food it can see within reach and snatches it. */
    public static final class SnatchFoodGoal extends Goal {
        private final FernSkitter skitter;
        private ItemEntity target;

        public SnatchFoodGoal(FernSkitter skitter) {
            this.skitter = skitter;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (skitter.isCarryingFood() || skitter.getRandom().nextInt(4) != 0) {
                return false;
            }
            List<ItemEntity> food = skitter.level().getEntitiesOfClass(ItemEntity.class, skitter.getBoundingBox().inflate(SNATCH_RANGE, 3.0, SNATCH_RANGE),
                    item -> item.isAlive() && item.getItem().has(DataComponents.FOOD) && skitter.hasLineOfSight(item));
            target = food.stream().min((a, b) -> Double.compare(a.distanceToSqr(skitter), b.distanceToSqr(skitter))).orElse(null);
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !skitter.isCarryingFood() && target.distanceToSqr(skitter) < SNATCH_RANGE * SNATCH_RANGE * 2;
        }

        @Override
        public void start() {
            skitter.getNavigation().moveTo(target, 1.5);
        }

        @Override
        public void tick() {
            // Snatched it already: vanilla only asks canContinueToUse every other tick, and between times just ticks an every-tick goal
            if (target == null) {
                return;
            }
            skitter.getLookControl().setLookAt(target);
            if (skitter.distanceToSqr(target) < 1.5 * 1.5) {
                skitter.snatch(target);
                target = null;
            } else if (skitter.getNavigation().isDone()) {
                skitter.getNavigation().moveTo(target, 1.5);
            }
        }

        @Override
        public void stop() {
            target = null;
            skitter.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }
}
