package com.hollowfrontier.content.hollow.megafauna;

import java.util.EnumSet;

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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Megatherium (Primordial Jungle, 10a): a giant ground sloth the size of an elephant, shaggy and slow, with claws like
 * sickles. Neutral and very hard to kill (thick hide, nothing knocks it back). Now and then it rears up on its hind legs
 * at a tree and browses: it strips one block of the tree's own leaves (not leaves a player placed, and only with
 * mobGriefing on) and eats it. Hurt it and it rears and swipes, slow but crushing. Drops its thick hide (the Hide
 * Barding) and leather. One the Grays have tagged (10b, {@link Specimens}) goes for players on sight.
 */
public class Megatherium extends PathfinderMob implements GeoEntity, Specimen {
    private static final EntityDataAccessor<Boolean> DATA_TAGGED = SynchedEntityData.defineId(Megatherium.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_REARING = SynchedEntityData.defineId(Megatherium.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation SWING = RawAnimation.begin().thenPlay("attack.swing");
    private static final RawAnimation BROWSE = RawAnimation.begin().thenLoop("misc.browse");
    /** How high above its feet leaves can be for it to reach them standing. */
    public static final int REACH_MIN = 2, REACH_MAX = 4;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public Megatherium(EntityType<? extends Megatherium> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 15.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean checkSpawnRules(EntityType<Megatherium> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return Megafauna.onOpenGround(level, pos, 3);
    }

    /** Leaves it may browse: a tree's own (not placed by a player). */
    public static boolean isBrowse(BlockState state) {
        return state.is(BlockTags.LEAVES) && (!state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_REARING, false);
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

    /** Up on its hind legs, browsing. */
    public boolean isRearing() {
        return entityData.get(DATA_REARING);
    }

    void setRearing(boolean rearing) {
        entityData.set(DATA_REARING, rearing);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(3, new BrowseGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, Specimens.huntWhenTagged(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MEGATHERIUM_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MEGATHERIUM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MEGATHERIUM_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.4F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 320;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
        controllers.add(new AnimationController<Megatherium>("browse", 8,
                test -> test.animatable().isRearing() ? test.setAndContinue(BROWSE) : PlayState.STOP));
        controllers.add(DefaultAnimations.genericAttackAnimation(SWING));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /**
     * Every so often, finds a tree's leaves within reach of its standing height nearby, walks up to them, rears and
     * browses a few seconds, then strips and eats that block.
     */
    public static final class BrowseGoal extends Goal {
        static final int BROWSE_TICKS = 100;
        private final Megatherium sloth;
        private @Nullable BlockPos leaves;
        private int ticks;

        public BrowseGoal(Megatherium sloth) {
            this.sloth = sloth;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (sloth.getTarget() != null || sloth.getRandom().nextInt(reducedTickDelay(160)) != 0) {
                return false;
            }
            leaves = findLeaves();
            return leaves != null;
        }

        /** The nearest browse leaves within 8 blocks across, 2 to 4 above the ground beside them. */
        @Nullable BlockPos findLeaves() {
            BlockPos feet = sloth.blockPosition();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-8, REACH_MIN, -8), feet.offset(8, REACH_MAX + 1, 8))) {
                if (isBrowse(sloth.level().getBlockState(pos))) {
                    double d = pos.distSqr(feet);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos.immutable();
                    }
                }
            }
            return best;
        }

        @Override
        public boolean canContinueToUse() {
            return leaves != null && sloth.getTarget() == null && ticks < BROWSE_TICKS + 200 && isBrowse(sloth.level().getBlockState(leaves));
        }

        @Override
        public void start() {
            ticks = 0;
            sloth.getNavigation().moveTo(leaves.getX() + 0.5, leaves.getY() - REACH_MIN, leaves.getZ() + 0.5, 0.7);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            sloth.getLookControl().setLookAt(leaves.getX() + 0.5, leaves.getY() + 0.5, leaves.getZ() + 0.5);
            double across = Math.hypot(leaves.getX() + 0.5 - sloth.getX(), leaves.getZ() + 0.5 - sloth.getZ());
            if (!sloth.isRearing()) {
                if (across < 2.6) {
                    sloth.getNavigation().stop();
                    sloth.setRearing(true);
                    ticks = 0;
                } else if (sloth.getNavigation().isDone()) {
                    leaves = null; // couldn't get there
                }
                return;
            }
            if (sloth.level() instanceof ServerLevel level) {
                BlockState state = level.getBlockState(leaves);
                if (ticks % 20 == 10) {
                    sloth.playSound(ModSounds.MEGATHERIUM_BROWSE.get(), 1.0F, 0.8F + sloth.getRandom().nextFloat() * 0.3F);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), leaves.getX() + 0.5, leaves.getY() + 0.3, leaves.getZ() + 0.5,
                            6, 0.3, 0.2, 0.3, 0.05);
                }
                if (ticks >= BROWSE_TICKS) {
                    if (level.getGameRules().get(GameRules.MOB_GRIEFING)) {
                        level.destroyBlock(leaves, false, sloth);
                    }
                    sloth.heal(6.0F);
                    leaves = null;
                }
            }
        }

        @Override
        public void stop() {
            sloth.setRearing(false);
            leaves = null;
        }
    }
}
