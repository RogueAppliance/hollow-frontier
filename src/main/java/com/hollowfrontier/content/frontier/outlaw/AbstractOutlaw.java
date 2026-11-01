package com.hollowfrontier.content.frontier.outlaw;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.gun.GunAttackGoal;
import com.hollowfrontier.content.frontier.gun.GunStats;
import com.hollowfrontier.content.frontier.gun.GunWielder;
import com.hollowfrontier.content.frontier.raid.RaidMember;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.hollow.gray.Captives;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Gun-toting humanoid outlaw. Subclasses pick the gun (stats + displayed item). Outlaws gang up:
 * hurting one alerts nearby outlaws, and they hunt players, lawmen, townsfolk and villagers.
 * A jailed outlaw (NBT "jailed", e.g. in the sheriff's cells) is unarmed and harmless: it only jeers at passers-by,
 * lawmen leave it alone and townsfolk don't flee it.
 * A freed outlaw (13d, NBT "freed": the Grays' leash broken, {@code content/ufo/Leash}) has thrown his gun down and only
 * wanders, harmless, a little lost; the Kindled Sigil no longer finds an implant in him. In a town he can be hired as a
 * deputy or sent home ({@link FreedMen}, 15c).
 * Rendered with GeckoLib's shared "humanoid" model; the gun is drawn by ItemInHandGeoLayer.
 */
public abstract class AbstractOutlaw extends Monster implements GeoEntity, GunWielder {
    private static final int SHOOT_ANIM_TICKS = 8;
    // Ticks left of the shoot animation, and the gun pose (GunPose ordinal). Synced entity data rather than GeckoLib
    // trigger packets, so they need no custom payload channel and players who start tracking mid-fight still see them.
    private static final EntityDataAccessor<Integer> DATA_SHOOT_TICKS = SynchedEntityData.defineId(AbstractOutlaw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GUN_POSE = SynchedEntityData.defineId(AbstractOutlaw.class, EntityDataSerializers.INT);
    // A jailbird heckles a nearby player at most this often (ticks).
    private static final int JEER_COOLDOWN = 600;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean jailed;
    private boolean freed;
    private long lastJeer = Long.MIN_VALUE / 2;

    protected AbstractOutlaw(EntityType<? extends AbstractOutlaw> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createOutlawAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHOOT_TICKS, 0);
        builder.define(DATA_GUN_POSE, GunPose.LOWERED.ordinal());
    }

    @Override
    public void onShot() {
        entityData.set(DATA_SHOOT_TICKS, SHOOT_ANIM_TICKS);
    }

    @Override
    public int shootTicks() {
        return entityData.get(DATA_SHOOT_TICKS);
    }

    @Override
    public GunPose getGunPose() {
        return GunPose.values()[entityData.get(DATA_GUN_POSE)];
    }

    @Override
    public void setGunPose(GunPose pose) {
        entityData.set(DATA_GUN_POSE, pose.ordinal());
    }

    public boolean isJailed() {
        return jailed;
    }

    /** Locks the outlaw up: disarmed, no targets, no gunfight. Permanent. */
    public void setJailed() {
        jailed = true;
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setTarget(null);
        targetSelector.removeAllGoals(goal -> true);
    }

    public boolean isFreed() {
        return freed;
    }

    /**
     * The leash is broken (13d): he throws his gun down at his feet, stops fighting for good, and says so to whoever's
     * near. Permanent.
     */
    public void setFreed() {
        if (freed) {
            return;
        }
        freed = true;
        ItemStack gun = getMainHandItem();
        if (!gun.isEmpty() && level() instanceof ServerLevel level) {
            spawnAtLocation(level, gun.copy());
            if (level.getNearestPlayer(this, 24.0) instanceof ServerPlayer near) {
                Chatter.say(this, near, Moment.FREED);
            }
        }
        disarm();
    }

    // Freed (15c): in a town he'll take a deputy's star or go home (FreedMen)
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!freed || !isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (level() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
            FreedMen.talk(level, serverPlayer, this, hand);
        }
        return InteractionResult.SUCCESS;
    }

    // Sights set on a player: he lets them know. (Raiders take their targets from the raid, so this covers them too.)
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        boolean spotted = target instanceof Player && target != getTarget();
        super.setTarget(target);
        if (spotted && !level().isClientSide()) {
            Chatter.bark(this, Moment.SPOT);
        }
    }

    @Override
    public void onReload() {
        if (getRandom().nextInt(2) == 0) {
            Chatter.bark(this, Moment.RELOAD);
        }
    }

    /** One in this many of a player's chambers loaded under fire draws a taunt (21a), on top of the bark cooldowns. */
    public static final int TAUNT_CHANCE = 3;

    /** A player loading a chamber: now and then the nearest outlaw after them jeers (GunItem.startReload). */
    public static void onPlayerReload(Player player) {
        if (player.getRandom().nextInt(TAUNT_CHANCE) == 0) {
            taunt(player);
        }
    }

    /** The nearest outlaw fighting this player taunts them for reloading: the key shouted, or null. */
    public static @Nullable String taunt(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getEntitiesOfClass(AbstractOutlaw.class, player.getBoundingBox().inflate(Chatter.BARK_RANGE),
                        o -> o.isAlive() && o.getTarget() == player && !o.isJailed() && !o.isFreed()).stream()
                .min(java.util.Comparator.comparingDouble(o -> o.distanceToSqr(player)))
                .map(o -> Chatter.bark(o, Moment.TAUNT)).orElse(null);
    }

    // Down to a third of his health: he cries out, once.
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean wasFine = getHealth() > getMaxHealth() / 3.0F;
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && wasFine && isAlive() && getHealth() <= getMaxHealth() / 3.0F) {
            Chatter.bark(this, Moment.HURT);
        }
        return hurt;
    }

    private void disarm() {
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setTarget(null);
        targetSelector.removeAllGoals(goal -> true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (jailed) {
            output.putBoolean("jailed", true);
        }
        if (freed) {
            output.putBoolean("freed", true);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.getBooleanOr("jailed", false)) {
            setJailed();
        }
        if (input.getBooleanOr("freed", false)) {
            freed = true;
            disarm();
        }
    }

    @Override
    protected void tickHeadTurn(float yBodyRotT) {
        if (!Animations.squareUp(this, this)) {
            super.tickHeadTurn(yBodyRotT);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            int left = entityData.get(DATA_SHOOT_TICKS);
            if (left > 0) {
                entityData.set(DATA_SHOOT_TICKS, left - 1);
            }
            if (jailed && tickCount % 40 == 0) {
                jeer();
            }
            if (freed && (tickCount + getId()) % 20 == 0) {
                Chatter.lookAround(this);
            }
        }
    }

    // Heckle the nearest player who comes by the cell.
    private void jeer() {
        long now = level().getGameTime();
        if (!(level().getNearestPlayer(this, 6.0) instanceof ServerPlayer player) || player.isSpectator() || now - lastJeer < JEER_COOLDOWN
                || getRandom().nextInt(3) != 0 || !hasLineOfSight(player)) {
            return;
        }
        lastJeer = now;
        Chatter.say(this, player, Moment.JEER);
    }

    // Prisoners carry nothing worth looting.
    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return !jailed && super.shouldDropLoot(level);
    }

    // Gangs don't shoot their own. Raiders came for the money: their shots pass hiding townsfolk and horses by.
    @Override
    public boolean holdsFireFor(Entity entity) {
        return entity instanceof AbstractOutlaw
                || RaidMember.of(this) != null && (entity instanceof AbstractVillager || entity instanceof AbstractHorse);
    }

    @Override
    public abstract GunStats gunStats();

    protected abstract ItemStack createGun();

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Captives.FollowCaptorGoal(this)); // a prisoner walked in from a Gray pen (20)
        goalSelector.addGoal(2, new GunAttackGoal<>(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, AbstractOutlaw.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Lawman.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Townsfolk.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (jailed || Captives.of(this) != null) {
            return;
        }
        arm();
    }

    /** Puts his gun in his hand (a new outlaw, or a captive the Grays have just finished with, 20). */
    public void arm() {
        setItemSlot(EquipmentSlot.MAINHAND, createGun());
        // Gun drops are handled by the loot table, not equipment drop chance.
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return data;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        Animations.<AbstractOutlaw>register(controllers);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
