package com.hollowfrontier.content.frontier.townsfolk;

import java.util.Locale;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.gun.GunAttackGoal;
import com.hollowfrontier.content.frontier.gun.GunStats;
import com.hollowfrontier.content.frontier.gun.GunTypes;
import com.hollowfrontier.content.frontier.gun.GunWielder;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.RaidGoals;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Town law. Each lawman holds a {@link Rank} (look, name, gun) and keeps the peace around where he spawned: he hunts
 * outlaws and hostile mobs on sight, shoots back at whoever hurts him (alerting the other lawmen), and answers
 * townsfolk who get hurt. His bullets pass through townsfolk, other lawmen, horses and bystanding players. Jailed
 * outlaws are left to rot. During a raid on his town he forms up in the street (RaidGoals.Muster) and calls a truce with
 * one- and two-star players: only three-star outlaws are still shot on sight. Never despawns. Spawned by the sheriff's
 * office and bank, or spawn egg.
 */
public class Lawman extends PathfinderMob implements GeoEntity, GunWielder {
    private static final EntityDataAccessor<Integer> DATA_RANK = SynchedEntityData.defineId(Lawman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHOOT_TICKS = SynchedEntityData.defineId(Lawman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GUN_POSE = SynchedEntityData.defineId(Lawman.class, EntityDataSerializers.INT);
    private static final int SHOOT_ANIM_TICKS = 8;
    // How far he'll stray from his post when nothing needs shooting, and how far a cry for help carries.
    private static final int PATROL_RADIUS = 24;
    private static final double HELP_RANGE = 32.0;

    /** Lawman ranks: look (textures/entity/lawman_<id>.png), name, Chatter voice (the id) and the gun carried. */
    public enum Rank {
        /** Dark hat, black vest, gold star. Revolver. */
        SHERIFF(GunTypes.OUTLAW_REVOLVER, ModItems.REVOLVER),
        /** Tan hat, chambray shirt, silver star. Double-barrel. */
        DEPUTY(GunTypes.BOSS_SHOTGUN, ModItems.SHOTGUN),
        /** Black hat and long coat, silver star. Rifle. */
        MARSHAL(GunTypes.OUTLAW_RIFLE, ModItems.PLAINS_RIFLE),
        /** Cream hat, buckskin, red neckerchief. Rifle. */
        RANGER(GunTypes.OUTLAW_RIFLE, ModItems.PLAINS_RIFLE);

        private final GunStats gun;
        private final Supplier<? extends Item> gunItem;

        Rank(GunStats gun, Supplier<? extends Item> gunItem) {
            this.gun = gun;
            this.gunItem = gunItem;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String nameKey() {
            return "entity.hollowfrontier.lawman." + id();
        }

        public String texture() {
            return "lawman_" + id();
        }

        public static Rank byId(String id) {
            for (Rank r : values()) {
                if (r.id().equals(id)) {
                    return r;
                }
            }
            return DEPUTY;
        }
    }

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    // Set when the rank came from NBT (a town building chose it), so spawning doesn't reroll it.
    private boolean rankChosen;
    // Riding with a posse after a wanted player: no post to return to, and gone once the chase goes cold.
    private boolean posse;
    private int idleTicks;

    public Lawman(EntityType<? extends Lawman> type, Level level) {
        super(type, level);
        // Out through the office door when there is trouble in the street (wooden doors only; the cells stay shut).
        getNavigation().setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    /** Sends lawmen near the victim (a townsfolk) after its attacker, unless they're busy or it's one of their own. */
    public static void callForHelp(ServerLevel level, LivingEntity victim, LivingEntity attacker) {
        if (attacker instanceof Lawman || !attacker.canBeSeenAsEnemy()) {
            return;
        }
        for (Lawman lawman : level.getEntitiesOfClass(Lawman.class, victim.getBoundingBox().inflate(HELP_RANGE))) {
            if (lawman.getTarget() == null || !lawman.getTarget().isAlive()) {
                lawman.setTarget(attacker);
            }
        }
    }

    /** Sends this (not yet spawned) lawman after a wanted player as part of a posse. */
    public void joinPosse(Player wanted) {
        posse = true;
        clearHome();
        setTarget(wanted);
    }

    public boolean isPosse() {
        return posse;
    }

    /** A freed man sworn in (15c, FreedMen): a Deputy with his shotgun, keeping the peace round {@code post}. */
    public void swearIn(BlockPos post) {
        setRank(Rank.DEPUTY);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Rank.DEPUTY.gunItem.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setHomeTo(post, PATROL_RADIUS);
        setPersistenceRequired();
    }

    public Rank getRank() {
        return Rank.values()[entityData.get(DATA_RANK)];
    }

    public void setRank(Rank rank) {
        entityData.set(DATA_RANK, rank.ordinal());
        rankChosen = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RANK, Rank.DEPUTY.ordinal());
        builder.define(DATA_SHOOT_TICKS, 0);
        builder.define(DATA_GUN_POSE, GunPose.LOWERED.ordinal());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("rank", getRank().id());
        if (posse) {
            output.putBoolean("posse", true);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("rank").ifPresent(id -> setRank(Rank.byId(id)));
        posse = input.getBooleanOr("posse", false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        if (!rankChosen) {
            Rank[] all = Rank.values();
            setRank(all[level.getRandom().nextInt(all.length)]);
        }
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(getRank().gunItem.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setHomeTo(blockPosition(), PATROL_RADIUS);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(getRank().nameKey());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, new GunAttackGoal<>(this, 1.0));
        goalSelector.addGoal(3, new RaidGoals.Muster(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Lawman.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractOutlaw.class, true,
                (target, level) -> !((AbstractOutlaw) target).isJailed()));
        // Two stars and up: shoot on sight (three while a raid is on).
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (target, level) -> Wanted.stars((Player) target) >= shootOnSight(level)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                (target, level) -> target instanceof Enemy && !(target instanceof AbstractOutlaw outlaw && outlaw.isJailed())));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            int left = entityData.get(DATA_SHOOT_TICKS);
            if (left > 0) {
                entityData.set(DATA_SHOOT_TICKS, left - 1);
            }
            // Targets handed over by callForHelp skip the target goals, so drop them here once they're gone or out of reach.
            LivingEntity target = getTarget();
            if (target != null && tickCount % 20 == 0 && (!target.isAlive() || !canAttack(target)
                    || distanceToSqr(target) > Math.pow(getAttributeValue(Attributes.FOLLOW_RANGE), 2))) {
                setTarget(null);
            }
            // A player who paid up or died is no longer the law's business (unless they just shot at us), nor is a small-time
            // one while there's a raid on.
            if (getTarget() instanceof Player player && player != getLastHurtByMob() && (Wanted.stars(player) == 0
                    || tickCount % 20 == 0 && Wanted.stars(player) < shootOnSight((ServerLevel) level()))) {
                setTarget(null);
            }
            if (posse && (idleTicks = getTarget() == null ? idleTicks + 1 : 0) > 600) {
                discard();
            }
            if ((tickCount + getId()) % 20 == 0 && !posse) {
                Chatter.lookAround(this);
            }
        }
    }

    // Stars it takes to be shot on sight here.
    private int shootOnSight(ServerLevel level) {
        return TownRaids.truceAt(level, blockPosition()) ? 3 : 2;
    }

    @Override
    protected void tickHeadTurn(float yBodyRotT) {
        if (!Animations.squareUp(this, this)) {
            super.tickHeadTurn(yBodyRotT);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (getTarget() == null && player instanceof ServerPlayer serverPlayer && Chatter.talk(this, serverPlayer) != null) {
            getLookControl().setLookAt(player);
        }
        return InteractionResult.SUCCESS;
    }

    // Sights set on a player: he calls it out.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        boolean spotted = target instanceof Player && target != getTarget();
        super.setTarget(target);
        if (spotted && !level().isClientSide() && tickCount > 0) { // not a posse rider handed his quarry before he rides in
            Chatter.bark(this, Moment.SPOT);
        }
    }

    @Override
    public GunStats gunStats() {
        return getRank().gun;
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

    // Never wing a townsfolk, a fellow lawman, a bystander, a horse or a prisoner.
    @Override
    public boolean holdsFireFor(Entity entity) {
        return entity instanceof AbstractVillager || entity instanceof Lawman || entity instanceof Player || entity instanceof AbstractHorse
                || entity instanceof AbstractOutlaw outlaw && outlaw.isJailed();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return posse;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        Animations.<Lawman>register(controllers);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
