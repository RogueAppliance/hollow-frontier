package com.hollowfrontier.content.hollow.creature;

import java.util.EnumSet;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ash Salamander (Ember Badlands, 9b2): a black, orange-spotted lizard as long as your arm, at home in fire. Fireproof, it
 * walks on lava as on stone and is hurt by water like a strider. It likes heat: every so often it goes and lies on the
 * nearest ash vent or magma it can find and basks there, flat out, for a while. Its spots glow. Harmless; drops magma cream
 * now and then.
 */
public class AshSalamander extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_BASKING = SynchedEntityData.defineId(AshSalamander.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation BASK = RawAnimation.begin().thenLoop("misc.bask");
    /** How far it looks for somewhere hot to lie. */
    public static final int HEAT_RANGE = 10;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public AshSalamander(EntityType<? extends AshSalamander> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, -1.0F);
        setPathfindingMalus(PathType.LAVA, 0.0F);
        setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0F);
        setPathfindingMalus(PathType.FIRE, 0.0F);
        setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, 0.0F);
        setPathfindingMalus(PathType.DAMAGING, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    public static boolean checkSpawnRules(EntityType<AshSalamander> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return HollowCreatures.onLitGround(level, pos);
    }

    /** Somewhere hot to lie: an ash vent or magma. */
    public static boolean isHot(BlockState state) {
        return state.is(ModBlocks.ASH_VENT.get()) || state.is(Blocks.MAGMA_BLOCK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BASKING, false);
    }

    public boolean isBasking() {
        return entityData.get(DATA_BASKING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(3, new BaskGoal(this));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidTags.LAVA);
    }

    // Walking on lava as a strider does: lava has a half-block-deep floor for it, and in lava it bobs up onto that floor.
    @Override
    public VoxelShape getLiquidCollisionShape() {
        return Block.column(16.0, 0.0, 8.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (isInLava()) {
            if (CollisionContext.of(this).isAbove(getLiquidCollisionShape(), blockPosition(), true)
                    && !level().getFluidState(blockPosition().above()).is(FluidTags.LAVA)) {
                setOnGround(true);
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.5).add(0.0, 0.05, 0.0));
            }
        }
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ASH_SALAMANDER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ASH_SALAMANDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ASH_SALAMANDER_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
        controllers.add(new AnimationController<AshSalamander>("bask", 6,
                test -> test.animatable().isBasking() ? test.setAndContinue(BASK) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Goes and lies on the nearest hot block (a vent, magma) and basks there a while. */
    public static final class BaskGoal extends Goal {
        private final AshSalamander salamander;
        private BlockPos spot;
        private int basked;
        private int length;

        BaskGoal(AshSalamander salamander) {
            this.salamander = salamander;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (salamander.getRandom().nextInt(60) != 0) {
                return false;
            }
            spot = findHeat();
            return spot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return spot != null && basked < length && isHot(salamander.level().getBlockState(spot)) && salamander.getLastHurtByMob() == null;
        }

        @Override
        public void start() {
            basked = 0;
            length = 200 + salamander.getRandom().nextInt(400);
            walk();
        }

        /** Onto the block itself (vanilla's moveTo stops a block short, which leaves it lying beside the vent). */
        private void walk() {
            var path = salamander.getNavigation().createPath(spot.above(), 0);
            if (path != null) {
                salamander.getNavigation().moveTo(path, 1.0);
            }
        }

        @Override
        public void tick() {
            if (salamander.onGround() && salamander.blockPosition().below().equals(spot)) {
                salamander.getNavigation().stop();
                salamander.entityData.set(DATA_BASKING, true);
                basked++;
            } else if (salamander.getNavigation().isDone()) {
                if (salamander.distanceToSqr(spot.getX() + 0.5, spot.getY() + 1, spot.getZ() + 0.5) < 4) {
                    salamander.getMoveControl().setWantedPosition(spot.getX() + 0.5, spot.getY() + 1, spot.getZ() + 0.5, 1.0); // the last step
                } else {
                    walk();
                }
                basked++; // it can't try to get there forever
            }
        }

        @Override
        public void stop() {
            salamander.entityData.set(DATA_BASKING, false);
            spot = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private BlockPos findHeat() {
            BlockPos origin = salamander.blockPosition();
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-HEAT_RANGE, -3, -HEAT_RANGE), origin.offset(HEAT_RANGE, 3, HEAT_RANGE))) {
                if (isHot(salamander.level().getBlockState(pos)) && salamander.level().getBlockState(pos.above()).isAir()) {
                    double d = pos.distSqr(origin);
                    if (d < bestDistance) {
                        bestDistance = d;
                        best = pos.immutable();
                    }
                }
            }
            return best;
        }
    }
}
