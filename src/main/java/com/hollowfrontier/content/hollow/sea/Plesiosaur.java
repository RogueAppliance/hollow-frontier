package com.hollowfrontier.content.hollow.sea;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Plesiosaur (the Still Sea's middle water, 43b): the long neck seen from the shore. A fish-eater that keeps ten blocks
 * off anyone who isn't holding a fish, and bites back if hurt. Give it a fish ({@link #mobInteract}) and it dives to the
 * bed under it, noses about, and comes back up to whoever fed it with something off the bottom
 * ({@link SeaLife#DIVE_LOOT}: mostly shells and kelp, now and then a sherd or a relic), and looks toward the Drowned
 * Quarter as it does ({@link SeaLife#quarterBearing}, once 43d builds one). Then it wants {@link #DIVE_COOLDOWN} before
 * it will go again.
 * <p>
 * The dive is run from {@link #customServerAiStep}, not a goal (a goal only holds the other goals off while it lasts), so
 * it can't be left half-cleared between ticks.
 */
public class Plesiosaur extends SeaBeast {
    private static final EntityDataAccessor<Boolean> DATA_DIVING = SynchedEntityData.defineId(Plesiosaur.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation DIVE = RawAnimation.begin().thenLoop("misc.dive");
    public static final int DIVE_COOLDOWN = 2400;
    /** The longest each leg of a dive may take before it moves on regardless, and how long it noses about on the bed. */
    public static final int LEG_TICKS = 300, BOTTOM_TICKS = 50;

    /** Where a dive is. */
    public enum Dive { NONE, DOWN, BOTTOM, UP }

    private Dive dive = Dive.NONE;
    private int diveTicks;
    private int diveCooldown;
    private @Nullable UUID fedBy;
    private BlockPos diveTo = BlockPos.ZERO;
    private Vec3 diveFrom = Vec3.ZERO;

    public Plesiosaur(EntityType<? extends Plesiosaur> type, Level level) {
        super(type, level, 5);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.TEMPT_RANGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.9)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DIVING, false);
    }

    public boolean isDiving() {
        return entityData.get(DATA_DIVING);
    }

    public Dive dive() {
        return dive;
    }

    public boolean willDive() {
        return dive == Dive.NONE && diveCooldown <= 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new DiveHoldGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
        goalSelector.addGoal(2, new TemptGoal(this, 1.0, stack -> stack.is(ItemTags.FISHES), false));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.0, 1.3,
                living -> living instanceof Player player && !holdsFish(player)));
        goalSelector.addGoal(5, new SwimAtDepthGoal(this, 0.8, 30, 4, -1, 0.15F)); // the middle water, and up for a look now and then
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    static boolean holdsFish(Player player) {
        return player.getMainHandItem().is(ItemTags.FISHES) || player.getOffhandItem().is(ItemTags.FISHES);
    }

    /** A fish sends it down, if it's in the water and willing. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ItemTags.FISHES)) {
            return super.mobInteract(player, hand);
        }
        if (!willDive() || !isInWater() || getTarget() != null) {
            return InteractionResult.FAIL;
        }
        if (level() instanceof ServerLevel level) {
            stack.consume(1, player);
            playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 0.7F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getEyeY(), getZ(), 5, 0.4, 0.3, 0.4, 0.0);
            gameEvent(GameEvent.EAT);
            startDive(player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Down to the bed under it, for {@code player}. */
    public void startDive(Player player) {
        fedBy = player.getUUID();
        diveFrom = position();
        diveTo = SeaLife.bedBelow(level(), blockPosition());
        leg(Dive.DOWN);
        entityData.set(DATA_DIVING, true);
        getNavigation().moveTo(diveTo.getX() + 0.5, diveTo.getY(), diveTo.getZ() + 0.5, 1.3);
    }

    private void leg(Dive next) {
        dive = next;
        diveTicks = 0;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (diveCooldown > 0) {
            diveCooldown--;
        }
        if (dive == Dive.NONE) {
            return;
        }
        diveTicks++;
        switch (dive) {
            case DOWN -> {
                boolean there = distanceToSqr(diveTo.getX() + 0.5, diveTo.getY(), diveTo.getZ() + 0.5) < 12.0;
                if (there || diveTicks > LEG_TICKS) {
                    getNavigation().stop();
                    leg(Dive.BOTTOM);
                } else if (getNavigation().isDone()) {
                    getNavigation().moveTo(diveTo.getX() + 0.5, diveTo.getY(), diveTo.getZ() + 0.5, 1.3);
                }
            }
            case BOTTOM -> {
                if (diveTicks % 10 == 0) {
                    level.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 0.5, getZ(), 6, 0.6, 0.3, 0.6, 0.02);
                }
                if (diveTicks > BOTTOM_TICKS) {
                    leg(Dive.UP);
                }
            }
            case UP -> {
                Player player = fedBy == null ? null : level.getPlayerByUUID(fedBy);
                Vec3 to = player != null && player.distanceToSqr(this) < 64 * 64 ? player.position() : diveFrom;
                boolean there = distanceToSqr(to) < 16.0;
                if (there || diveTicks > LEG_TICKS) {
                    surface(level, player);
                } else if (diveTicks % 10 == 1) {
                    getNavigation().moveTo(to.x, Math.min(to.y, level.getSeaLevel() - 1), to.z, 1.3);
                }
            }
            default -> {}
        }
    }

    /** Up with it: what it found goes to whoever fed it (or floats where it is), and it looks the Quarter's way. */
    private void surface(ServerLevel level, @Nullable Player player) {
        List<ItemStack> found = bringUp(level);
        Vec3 mouth = getEyePosition();
        for (ItemStack stack : found) {
            ItemEntity item = new ItemEntity(level, mouth.x, mouth.y, mouth.z, stack);
            if (player != null) {
                item.setDeltaMovement(player.getEyePosition().subtract(mouth).normalize().scale(0.3).add(0, 0.15, 0));
            }
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        playSound(ModSounds.PLESIOSAUR_AMBIENT.get(), 1.2F, 1.1F);
        BlockPos look = SeaLife.quarterBearing(level, blockPosition()).orElse(player == null ? null : player.blockPosition());
        if (look != null) {
            getLookControl().setLookAt(look.getX() + 0.5, getEyeY(), look.getZ() + 0.5, 60.0F, 30.0F);
        }
        if (player != null && !found.isEmpty()) {
            ModCriteria.fire(player, ModCriteria.PLESIOSAUR_FETCHED);
        }
        getNavigation().stop();
        dive = Dive.NONE;
        diveCooldown = DIVE_COOLDOWN;
        fedBy = null;
        entityData.set(DATA_DIVING, false);
    }

    /** One roll of the dive table. */
    public List<ItemStack> bringUp(ServerLevel level) {
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, position()).create(LootContextParamSets.CHEST);
        return level.getServer().reloadableRegistries().getLootTable(SeaLife.DIVE_LOOT).getRandomItems(params);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("DiveCooldown", diveCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // A dive under way isn't saved: it comes back up with nothing, and may be asked again
        diveCooldown = input.getIntOr("DiveCooldown", 0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDiving() ? null : ModSounds.PLESIOSAUR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PLESIOSAUR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PLESIOSAUR_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<Plesiosaur>("dive", 6,
                test -> test.animatable().isDiving() ? test.setAndContinue(DIVE) : PlayState.STOP));
    }

    /** Holds its other goals off while it dives (the dive itself runs in the entity's own step). */
    static final class DiveHoldGoal extends Goal {
        private final Plesiosaur plesiosaur;

        DiveHoldGoal(Plesiosaur plesiosaur) {
            this.plesiosaur = plesiosaur;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return plesiosaur.dive != Dive.NONE;
        }
    }
}
