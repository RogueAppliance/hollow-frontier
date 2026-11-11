package com.hollowfrontier.content.hollow.sea;

import java.util.EnumSet;
import java.util.Optional;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.hollow.megafauna.Megafauna;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Great Sea Cow (the Still Sea's kelp shallows, 43b): eight blocks of slow, gentle grey bulk with a whale's tail, grazing
 * the kelp in herds. It keeps to its herd, flees (slowly) when hurt, and never fights back. Every few minutes it swims to
 * a stalk of kelp and eats the top off it ({@link #graze}); the stalk grows back. Kelp in hand leads it and breeds it.
 * It drops Blubber, which renders down to Tallow.
 */
public class GreatSeaCow extends Animal implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_GRAZING = SynchedEntityData.defineId(GreatSeaCow.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation GRAZE = RawAnimation.begin().thenLoop("misc.graze");
    /** Between meals, and how long one takes. */
    public static final int GRAZE_MIN = 1200, GRAZE_MAX = 2400, GRAZE_TICKS = 60;
    /** How far it looks for kelp, across and up or down. */
    public static final int KELP_RANGE = 10, KELP_HEIGHT = 8;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int grazeCooldown;

    public GreatSeaCow(EntityType<? extends GreatSeaCow> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 4, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 4);
        grazeCooldown = random.nextInt(GRAZE_MAX);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GRAZING, false);
    }

    public boolean isGrazing() {
        return entityData.get(DATA_GRAZING);
    }

    void setGrazing(boolean grazing) {
        entityData.set(DATA_GRAZING, grazing);
    }

    public boolean hungry() {
        return grazeCooldown <= 0;
    }

    /** For the tests: hungry now, or fed for the usual while. */
    public void setHungry(boolean hungry) {
        grazeCooldown = hungry ? 0 : GRAZE_MIN;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new PanicGoal(this, 1.5));
        goalSelector.addGoal(1, new BreedGoal(this, 1.0));
        goalSelector.addGoal(2, new TemptGoal(this, 1.1, stack -> stack.is(Items.KELP), false));
        goalSelector.addGoal(3, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(4, new GrazeGoal(this));
        goalSelector.addGoal(5, new Megafauna.FollowHerdGoal<>(this, GreatSeaCow.class, 8.0, 28.0, 1.0));
        goalSelector.addGoal(6, new SwimAtDepthGoal(this, 0.8, 40, 2, 5, 0.0F)); // down among the kelp
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (grazeCooldown > 0) {
            grazeCooldown--;
        }
    }

    /** The top of a stalk of kelp within reach that has stalk under it (it never eats a plant down to nothing). */
    public Optional<BlockPos> findKelp() {
        BlockPos here = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(here.offset(-KELP_RANGE, -KELP_HEIGHT, -KELP_RANGE), here.offset(KELP_RANGE, KELP_HEIGHT, KELP_RANGE))) {
            double distance = pos.distSqr(here);
            if (distance < bestDistance && level().getBlockState(pos).is(Blocks.KELP) && level().getBlockState(pos.below()).is(Blocks.KELP_PLANT)) {
                best = pos.immutable();
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Eats the top off the stalk at {@code kelp}: the block goes, the stalk under it becomes the new tip (young enough to
     * grow back), and the cow is fed for a while and a little healed. False if there's no such stalk there any more.
     */
    public boolean graze(BlockPos kelp) {
        BlockState top = level().getBlockState(kelp);
        if (!top.is(Blocks.KELP) || !level().getBlockState(kelp.below()).is(Blocks.KELP_PLANT)) {
            return false;
        }
        level().setBlock(kelp, Blocks.WATER.defaultBlockState(), 3);
        level().setBlock(kelp.below(), Blocks.KELP.defaultBlockState().setValue(KelpBlock.AGE, 10 + random.nextInt(6)), 3);
        playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 0.6F);
        gameEvent(GameEvent.EAT);
        heal(2.0F);
        grazeCooldown = GRAZE_MIN + random.nextInt(GRAZE_MAX - GRAZE_MIN);
        return true;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.KELP);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.GREAT_SEA_COW.get().create(level, EntitySpawnReason.BREEDING);
    }

    // ---- a swimmer (as SeaBeast, which it can't extend)

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        moveRelative(getSpeed(), input);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.9));
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return SeaLife.isWater(level, pos) ? 10.0F : 0.0F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("GrazeCooldown", grazeCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        grazeCooldown = input.getIntOr("GrazeCooldown", grazeCooldown);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SEA_COW_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SEA_COW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SEA_COW_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 320;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericSwimIdleController());
        controllers.add(new AnimationController<GreatSeaCow>("graze", 6,
                test -> test.animatable().isGrazing() ? test.setAndContinue(GRAZE) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /**
     * When hungry: swim to the nearest stalk of kelp, chew at it for {@link #GRAZE_TICKS}, and eat its top. Its fields are
     * cleared only in {@link #stop} (phase 34's rule), and it doesn't ask to be ticked every tick.
     */
    public static final class GrazeGoal extends Goal {
        private final GreatSeaCow cow;
        private BlockPos kelp;
        private int chewing;
        private int giveUp;

        public GrazeGoal(GreatSeaCow cow) {
            this.cow = cow;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!cow.hungry() || cow.isBaby() || cow.getRandom().nextInt(reducedTickDelay(20)) != 0) {
                return false;
            }
            kelp = cow.findKelp().orElse(null);
            if (kelp == null) {
                cow.grazeCooldown = 200; // none about: look again in ten seconds
            }
            return kelp != null;
        }

        @Override
        public boolean canContinueToUse() {
            return kelp != null && giveUp > 0 && cow.level().getBlockState(kelp).is(Blocks.KELP);
        }

        @Override
        public void start() {
            chewing = 0;
            giveUp = 400;
            cow.getNavigation().moveTo(kelp.getX() + 0.5, kelp.getY(), kelp.getZ() + 0.5, 1.0);
        }

        @Override
        public void tick() {
            if (kelp == null) {
                return;
            }
            giveUp -= 2; // ticked every other tick
            cow.getLookControl().setLookAt(kelp.getX() + 0.5, kelp.getY() + 0.5, kelp.getZ() + 0.5);
            if (cow.distanceToSqr(kelp.getX() + 0.5, kelp.getY() + 0.5, kelp.getZ() + 0.5) > 9.0) {
                if (cow.getNavigation().isDone()) {
                    cow.getNavigation().moveTo(kelp.getX() + 0.5, kelp.getY(), kelp.getZ() + 0.5, 1.0);
                }
                return;
            }
            cow.getNavigation().stop();
            cow.setGrazing(true);
            chewing += 2;
            if (chewing >= GRAZE_TICKS) {
                cow.graze(kelp);
                giveUp = 0;
            }
        }

        @Override
        public void stop() {
            kelp = null;
            cow.setGrazing(false);
        }
    }
}
