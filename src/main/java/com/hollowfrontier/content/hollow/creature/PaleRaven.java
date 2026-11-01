package com.hollowfrontier.content.hollow.creature;

import java.util.EnumSet;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Pale Raven (Concavian Highlands, 9b2): a white raven that perches on the Highlands' standing stones. Come near without
 * sneaking and it takes off, croaking, and flies away: toward the nearest Kindled site if one is in range (the waystones, sealed doors
 * and murals in {@link ModStructures#KINDLED_SITES}), so a player who follows the ravens finds the ruins; otherwise just away. Drops a
 * feather now and then.
 */
public class PaleRaven extends PathfinderMob implements GeoEntity {
    /** How close you can come, not sneaking, before it flies. */
    public static final double FLUSH_RANGE = 6.0;
    /** How far it looks for a stone to perch on. */
    private static final int PERCH_RANGE = 16;
    /** How far (in chunks) it looks for a Kindled site to fly toward. */
    private static final int GUIDE_CHUNKS = 24;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public PaleRaven(EntityType<? extends PaleRaven> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl<>(this, 10, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public static boolean checkSpawnRules(EntityType<PaleRaven> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return HollowCreatures.onLitGround(level, pos);
    }

    /**
     * A perch: the top of standing stonework (calcite, dressed diorite or quartz, with more of it under it, so not plain
     * calcite ground: it must stand clear on two sides) with open air over it.
     */
    public static boolean isPerch(Level level, BlockPos pos) {
        if (!isStone(level.getBlockState(pos)) || !isStone(level.getBlockState(pos.below())) || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        // It has to stand up out of the ground: open air on at least two sides
        int open = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(side)).isAir()) {
                open++;
            }
        }
        return open >= 2;
    }

    private static boolean isStone(BlockState state) {
        return state.is(Blocks.CALCITE) || state.is(Blocks.POLISHED_DIORITE) || state.is(Blocks.CHISELED_QUARTZ_BLOCK) || state.is(Blocks.QUARTZ_PILLAR);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        goalSelector.addGoal(2, new TakeOffGoal(this));
        goalSelector.addGoal(3, new PerchGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PALE_RAVEN_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PALE_RAVEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PALE_RAVEN_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkFlyIdleController(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Where a flushed raven makes for: the nearest Kindled site in range, else straight away from whoever flushed it. */
    public Vec3 flightDestination(Player from) {
        if (level() instanceof ServerLevel level) {
            BlockPos site = level.findNearestMapStructure(ModStructures.KINDLED_SITES, blockPosition(), GUIDE_CHUNKS, false);
            if (site != null) {
                Vec3 toward = Vec3.atCenterOf(site).subtract(position()).multiply(1, 0, 1);
                if (toward.lengthSqr() > 1) {
                    return position().add(toward.normalize().scale(Math.min(40, toward.length())));
                }
            }
        }
        Vec3 away = position().subtract(from.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 0.01) {
            away = new Vec3(getRandom().nextDouble() - 0.5, 0, getRandom().nextDouble() - 0.5);
        }
        return position().add(away.normalize().scale(24 + getRandom().nextInt(16)));
    }

    /** Flushed by someone close who isn't sneaking: up and away (see {@link #flightDestination}), then down again. */
    static final class TakeOffGoal extends Goal {
        private final PaleRaven raven;
        private Vec3 destination;
        private int flying;

        TakeOffGoal(PaleRaven raven) {
            this.raven = raven;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (raven.tickCount % 5 != 0) {
                return false;
            }
            Player near = raven.level().getNearestPlayer(raven.getX(), raven.getY(), raven.getZ(), FLUSH_RANGE,
                    p -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p) && !p.isShiftKeyDown());
            if (near == null) {
                return false;
            }
            destination = raven.flightDestination(near);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return flying < 200 && raven.position().multiply(1, 0, 1).distanceToSqr(destination.multiply(1, 0, 1)) > 9;
        }

        @Override
        public void start() {
            flying = 0;
            raven.playSound(ModSounds.PALE_RAVEN_AMBIENT.get(), 1.0F, 1.0F);
            raven.getNavigation().stop();
        }

        @Override
        public void tick() {
            flying++;
            // Eight blocks over the ground under its destination, so it clears the hills between (found by looking down, since
            // in the Hollow Earth the heightmap's top is the crust)
            int ground = HollowCreatures.groundBelow(raven.level(), (int) Math.floor(destination.x), (int) Math.floor(destination.z), (int) raven.getY() + 24);
            double y = Math.min(ground, raven.getY() + 12) + 8;
            raven.getMoveControl().setWantedPosition(destination.x, Math.max(y, raven.getY() + (flying < 20 ? 2 : 0)), destination.z, 1.6);
            raven.getLookControl().setLookAt(destination.x, y, destination.z);
        }

        @Override
        public void stop() {
            destination = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** Now and then, flies to the top of a standing stone nearby and settles there. */
    static final class PerchGoal extends Goal {
        // After a search that finds no stone, the raven leaves off looking for a while (each search reads ~14,000 blocks)
        private static final int BARREN_RETRY = 200;

        private final PaleRaven raven;
        private BlockPos perch;
        private int tries;
        private int nextSearch;

        PerchGoal(PaleRaven raven) {
            this.raven = raven;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (raven.tickCount < nextSearch || isPerch(raven.level(), raven.blockPosition().below()) || raven.getRandom().nextInt(40) != 0) {
                return false;
            }
            perch = findPerch();
            if (perch == null) {
                nextSearch = raven.tickCount + BARREN_RETRY;
            }
            return perch != null;
        }

        @Override
        public boolean canContinueToUse() {
            return perch != null && tries < 300 && !isPerchedOn(perch) && isPerch(raven.level(), perch);
        }

        @Override
        public void start() {
            tries = 0;
        }

        @Override
        public void tick() {
            tries++;
            raven.getMoveControl().setWantedPosition(perch.getX() + 0.5, perch.getY() + 1.05, perch.getZ() + 0.5, 1.0);
        }

        @Override
        public void stop() {
            perch = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private boolean isPerchedOn(BlockPos pos) {
            return raven.onGround() && raven.blockPosition().below().equals(pos);
        }

        private BlockPos findPerch() {
            BlockPos origin = raven.blockPosition();
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-PERCH_RANGE, -6, -PERCH_RANGE), origin.offset(PERCH_RANGE, 6, PERCH_RANGE))) {
                if (isPerch(raven.level(), pos)) {
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
