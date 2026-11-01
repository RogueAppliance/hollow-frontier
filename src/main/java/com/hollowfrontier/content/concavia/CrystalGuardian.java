package com.hollowfrontier.content.concavia;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.mystery.ManInBlack;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.content.hollow.gray.Specimens;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Crystal Guardian (12a): a Kindled construct, a crystal body on stone legs, keeping its post in a crystal ruin (home
 * {@link #HOME_RADIUS}). It stands still until something it guards against comes within {@link #SIGHT}: anything of the
 * Grays' (probes, sentinels, Wardens, tagged specimens, men in black), a player who has wronged the Kindled, or a player who isn't
 * yet their friend carrying the Grays' things (its first shot at such a player is a warning). Then it plants itself,
 * draws a pale beam on its target for {@link #CHARGE_TICKS} (getting behind something breaks it off) and fires:
 * {@link #DAMAGE}, and {@link #COOLDOWN} ticks to charge again. Anyone who strikes it, or a Kindled near it, has the guardians round about on them. It doesn't leave its
 * post to give chase. Structure-placed only; never despawns.
 */
public class CrystalGuardian extends PathfinderMob implements GeoEntity {
    public static final double SIGHT = 20.0;
    public static final int CHARGE_TICKS = 30;
    public static final int COOLDOWN = 40;
    public static final float DAMAGE = 6.0F;
    public static final int HOME_RADIUS = 6;
    public static final double RALLY_RANGE = 24.0;
    private static final DustParticleOptions BEAM = new DustParticleOptions(0x8FE8FF, 0.6F);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING = SynchedEntityData.defineId(CrystalGuardian.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation CHARGE = RawAnimation.begin().thenLoop("charge");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    // The players it has fired a warning at (not saved: a guardian reloaded warns again)
    private final Set<UUID> warned = new HashSet<>();

    public CrystalGuardian(EntityType<? extends CrystalGuardian> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, SIGHT + 4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGING, false);
    }

    public boolean isCharging() {
        return entityData.get(DATA_CHARGING);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        setHomeTo(blockPosition(), HOME_RADIUS);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BeamGoal(this));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, CrystalGuardian.class, Kindled.class).setAlertOthers(CrystalGuardian.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target, level) -> guardsAgainst(target)));
    }

    /** Whether it fires on this on sight. */
    public static boolean guardsAgainst(LivingEntity target) {
        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator()
                    && (KindledStanding.wronged(player) || !KindledStanding.friend(player) && carriesGrayThings(player));
        }
        return target instanceof GrayProbe || target instanceof GrayWarden || target instanceof ManInBlack || Specimens.isTagged(target);
    }

    public static boolean carriesGrayThings(Player player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ModTags.Items.STRANGE_GOODS)) {
                return true;
            }
        }
        return false;
    }

    /** Sets the guardians round {@code at} on whoever hurt a Kindled there. */
    public static void rally(ServerLevel level, Vec3 at, LivingEntity attacker) {
        for (CrystalGuardian guardian : level.getEntitiesOfClass(CrystalGuardian.class, new AABB(at, at).inflate(RALLY_RANGE), CrystalGuardian::isAlive)) {
            guardian.setTarget(attacker);
        }
    }

    /** A player it turns on hears why. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity old = getTarget();
        super.setTarget(target);
        if (target instanceof ServerPlayer player && target != old) {
            String why = KindledStanding.wronged(player) ? "wronged" : carriesGrayThings(player) && !KindledStanding.friend(player) ? "gray" : "struck";
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.guardian." + why).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // Its own kind's beams pass through it (they're built of the same crystal)
        if (source.getEntity() instanceof CrystalGuardian) {
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void die(DamageSource source) {
        ModCriteria.fire(source.getEntity(), ModCriteria.GUARDIAN_KILLED);
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected int calculateFallDamage(double fallDistance, float damageModifier) {
        return 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CrystalGuardian>("main", 4, state -> {
            if (state.animatable().isCharging()) {
                return state.setAndContinue(CHARGE);
            }
            return state.setAndContinue(state.isMoving() ? DefaultAnimations.WALK : DefaultAnimations.IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /**
     * The beam: with a target in sight and range, it stops, turns to it and charges, drawing the beam every other tick; at
     * full charge it fires. Out of sight, the charge is lost.
     */
    static class BeamGoal extends Goal {
        private final CrystalGuardian guardian;
        private int charge;
        private int cooldown;

        BeamGoal(CrystalGuardian guardian) {
            this.guardian = guardian;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = guardian.getTarget();
            return target != null && target.isAlive() && guardian.distanceTo(target) <= SIGHT;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            guardian.getNavigation().stop();
        }

        @Override
        public void stop() {
            charge = 0;
            guardian.entityData.set(DATA_CHARGING, false);
        }

        @Override
        public void tick() {
            LivingEntity target = guardian.getTarget();
            if (target == null || !(guardian.level() instanceof ServerLevel level)) {
                return;
            }
            guardian.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (!guardian.hasLineOfSight(target)) {
                charge = 0;
                guardian.entityData.set(DATA_CHARGING, false);
                return;
            }
            if (charge == 0) {
                guardian.playSound(SoundEvents.BEACON_ACTIVATE, 1.5F, 1.6F);
                guardian.entityData.set(DATA_CHARGING, true);
            }
            charge++;
            if (charge % 2 == 0) {
                beam(level, guardian, target);
            }
            if (charge >= CHARGE_TICKS) {
                fire(level, guardian, target);
                charge = 0;
                cooldown = COOLDOWN;
                guardian.entityData.set(DATA_CHARGING, false);
            }
        }
    }

    /** The pale line it draws from its eye to its target while it charges. */
    static void beam(ServerLevel level, CrystalGuardian guardian, LivingEntity target) {
        Vec3 from = guardian.getEyePosition();
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Vec3 step = to.subtract(from);
        int points = (int) Math.max(4, step.length() * 2.5);
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.add(step.scale(i / (double) points));
            level.sendParticles(BEAM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Fires on the target: {@link #DAMAGE}, except that each guardian's first shot at a player whose only fault is carrying
     * the Grays' things (not one who's wronged the Kindled, nor one who struck it) goes wide, as a warning.
     */
    public static void fire(ServerLevel level, CrystalGuardian guardian, LivingEntity target) {
        guardian.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.7F);
        if (target instanceof ServerPlayer player && !KindledStanding.wronged(player) && guardian.getLastHurtByMob() != player
                && guardian.warned.add(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.guardian.warning").withStyle(ChatFormatting.AQUA));
            return;
        }
        target.hurtServer(level, ModDamageTypes.phlogistonBeam(guardian), DAMAGE);
        guardian.playSound(SoundEvents.GUARDIAN_ATTACK, 1.0F, 1.6F);
    }

    /** Whether this guardian has fired its warning shot at the player. */
    public boolean hasWarned(Player player) {
        return warned.contains(player.getUUID());
    }
}
