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
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Glyptodon (Primordial Jungle and Crystal Steppes, 10a): a giant armadillo the size of a small car, under a domed shell
 * of bony scutes, with a spiked club of a tail. Passive and slow. Frighten it (hurt it, sprint up close, or let a terror
 * bird near) and it hunkers down: head and legs in, it won't budge and takes a quarter of the damage, for a few seconds.
 * Its scutes are the shell armor's: it sheds one now and then, a brush takes one off it (once every five minutes, as an
 * armadillo's does), and it drops a few when killed. One the Grays have tagged (10b, {@link Specimens}) never hunkers.
 */
public class Glyptodon extends PathfinderMob implements GeoEntity, Specimen {
    private static final EntityDataAccessor<Boolean> DATA_TAGGED = SynchedEntityData.defineId(Glyptodon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HUNKERED = SynchedEntityData.defineId(Glyptodon.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation HUNKER = RawAnimation.begin().thenPlay("misc.hunker").thenLoop("misc.hunkered");
    /** How long a fright keeps it hunkered, and the share of damage that gets through its shell meanwhile. */
    public static final int HUNKER_TICKS = 120;
    public static final float HUNKERED_DAMAGE = 0.25F;
    /** Between brushings, and between scutes shed of its own accord. */
    public static final int BRUSH_COOLDOWN = 6000;
    public static final int SHED_MIN = 12000, SHED_MAX = 24000;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int hunkerTicks;
    private int brushCooldown;
    private int shedTicks;

    public Glyptodon(EntityType<? extends Glyptodon> type, Level level) {
        super(type, level);
        xpReward = 5;
        shedTicks = SHED_MIN + random.nextInt(SHED_MAX - SHED_MIN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    public static boolean checkSpawnRules(EntityType<Glyptodon> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return Megafauna.onOpenGround(level, pos, 2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUNKERED, false);
        builder.define(DATA_TAGGED, false);
    }

    public boolean isHunkered() {
        return entityData.get(DATA_HUNKERED);
    }

    /** Pulls in under its shell for {@link #HUNKER_TICKS} (or keeps on doing so). */
    public void hunker() {
        if (isTagged()) {
            return; // the implant won't let it
        }
        if (!isHunkered()) {
            playSound(ModSounds.GLYPTODON_HUNKER.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
        }
        hunkerTicks = HUNKER_TICKS;
        entityData.set(DATA_HUNKERED, true);
        getNavigation().stop();
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

    public boolean canBeBrushed() {
        return brushCooldown <= 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HunkerGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (brushCooldown > 0) {
            brushCooldown--;
        }
        if (--shedTicks <= 0) {
            shedTicks = SHED_MIN + random.nextInt(SHED_MAX - SHED_MIN);
            spawnAtLocation(level, new ItemStack(ModItems.GLYPTODON_SCUTE.get()));
            gameEvent(GameEvent.ENTITY_PLACE);
        }
        if (tickCount % 10 == 0 && threatened(level)) {
            hunker();
        }
        if (hunkerTicks > 0 && --hunkerTicks == 0) {
            entityData.set(DATA_HUNKERED, false);
        }
    }

    /** A terror bird within 8 blocks, or a player sprinting within 5. */
    boolean threatened(ServerLevel level) {
        return !level.getEntitiesOfClass(TerrorBird.class, getBoundingBox().inflate(8.0)).isEmpty()
                || !level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(5.0),
                        p -> p.isSprinting() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p)).isEmpty();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isHunkered() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= HUNKERED_DAMAGE;
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive()) {
            hunker();
        }
        return hurt;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isHunkered();
    }

    /** A brush takes a scute off its shell, once every five minutes. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.BRUSH)) {
            return super.mobInteract(player, hand);
        }
        if (!canBeBrushed()) {
            return InteractionResult.FAIL;
        }
        if (level() instanceof ServerLevel level) {
            spawnAtLocation(level, new ItemStack(ModItems.GLYPTODON_SCUTE.get()));
            playSound(ModSounds.GLYPTODON_BRUSH.get(), 1.0F, 1.0F);
            gameEvent(GameEvent.ENTITY_INTERACT);
            stack.hurtAndBreak(16, player, hand.asEquipmentSlot());
            brushCooldown = BRUSH_COOLDOWN;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("BrushCooldown", brushCooldown);
        output.putInt("ShedTime", shedTicks);
        output.putBoolean(Specimens.SAVE_KEY, isTagged());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        brushCooldown = input.getIntOr("BrushCooldown", 0);
        shedTicks = input.getIntOr("ShedTime", shedTicks);
        setTagged(input.getBooleanOr(Specimens.SAVE_KEY, false));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isHunkered() ? null : ModSounds.GLYPTODON_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GLYPTODON_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GLYPTODON_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
        controllers.add(new AnimationController<Glyptodon>("hunker", 4,
                test -> test.animatable().isHunkered() ? test.setAndContinue(HUNKER) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Holds it still (and its gaze too) while it's hunkered under its shell. */
    static final class HunkerGoal extends Goal {
        private final Glyptodon glyptodon;

        HunkerGoal(Glyptodon glyptodon) {
            this.glyptodon = glyptodon;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return glyptodon.isHunkered();
        }

        @Override
        public void start() {
            glyptodon.getNavigation().stop();
        }
    }
}
