package com.hollowfrontier.content.frontier.mystery;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.gun.GunAttackGoal;
import com.hollowfrontier.content.frontier.gun.GunStats;
import com.hollowfrontier.content.frontier.gun.GunTypes;
import com.hollowfrontier.content.frontier.gun.GunWielder;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A man in a black duster (11b): one of the Grays' implanted collaborators, the outlaws' finished product, sent back to
 * tidy up after their masters. Pale, soft-spoken, a little too still; a faint light behind one ear.
 * <ul>
 * <li>They come to town the day after a sighting, a mutilation or a crop circle ({@link MenInBlack}), stay about half a
 *     day ({@link #VISIT_TICKS}) and are gone.</li>
 * <li>They buy the Grays' things back: hold one out ({@code #hollowfrontier:strange_goods}) and they pay in gold, and
 *     mind you less ({@link Suspicion#SELL}).</li>
 * <li>A player they're wary of ({@link Suspicion#WARN}) gets a quiet word, once a visit; one they've marked
 *     ({@link Suspicion#HOSTILE}) gets a revolver, on sight, and at night they come hunting in pairs. Hurt one and the
 *     others take it up.</li>
 * <li>Seeing a player carry the Grays' things (within {@link #NOTICE_RANGE}) raises their interest.</li>
 * </ul>
 */
public class ManInBlack extends PathfinderMob implements GeoEntity, GunWielder {
    public static final int VISIT_TICKS = 12000;
    public static final double NOTICE_RANGE = 16.0;
    public static final double WARN_RANGE = 8.0;
    private static final EntityDataAccessor<Integer> DATA_SHOOT_TICKS = SynchedEntityData.defineId(ManInBlack.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GUN_POSE = SynchedEntityData.defineId(ManInBlack.class, EntityDataSerializers.INT);
    private static final int SHOOT_ANIM_TICKS = 8;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> warned = new HashSet<>();
    private int visitLeft = VISIT_TICKS;
    private boolean hunting;

    public ManInBlack(EntityType<? extends ManInBlack> type, Level level) {
        super(type, level);
        getNavigation().setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    /** Sent after a marked player: no visit to end, and gone once the trail goes cold. */
    public void hunt(Player player) {
        hunting = true;
        clearHome();
        setTarget(player);
    }

    public boolean isHunting() {
        return hunting;
    }

    public int visitLeft() {
        return visitLeft;
    }

    public boolean hasWarned(Player player) {
        return warned.contains(player.getUUID());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHOOT_TICKS, 0);
        builder.define(DATA_GUN_POSE, GunPose.LOWERED.ordinal());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.REVOLVER.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setHomeTo(blockPosition(), 24);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, new GunAttackGoal<>(this, 1.0));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F, 0.6F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, ManInBlack.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (player, level) -> Suspicion.hunted((Player) player)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int left = entityData.get(DATA_SHOOT_TICKS);
        if (left > 0) {
            entityData.set(DATA_SHOOT_TICKS, left - 1);
        }
        if (getTarget() instanceof Player player && (!player.isAlive() || player != getLastHurtByMob() && !Suspicion.hunted(player))) {
            setTarget(null);
        }
        if (tickCount % 40 == 0) {
            watch(level);
        }
        if (hunting && getTarget() == null && visitLeft > 600) {
            visitLeft = 600; // a hunt with no quarry in sight goes cold in half a minute
        }
        // A visit ends (or a hunt with no quarry in sight goes cold): they're simply gone
        if (getTarget() == null && --visitLeft <= 0 && tickCount % 20 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY(1.0), getZ(), 8, 0.2, 0.4, 0.2, 0.01);
            discard();
        }
    }

    /** Notice who's carrying the Grays' things, and have a quiet word with anyone they're wary of. */
    private void watch(ServerLevel level) {
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(NOTICE_RANGE), p -> !p.isCreative() && !p.isSpectator())) {
            if (!(player instanceof ServerPlayer serverPlayer) || !hasLineOfSight(player)) {
                continue;
            }
            if (carriesGoods(player)) {
                Suspicion.add(serverPlayer, Suspicion.SEEN_CARRYING);
            }
            if (Suspicion.warned(player) && !Suspicion.hunted(player) && distanceTo(player) <= WARN_RANGE && warned.add(player.getUUID())) {
                getLookControl().setLookAt(player);
                Chatter.say(this, serverPlayer, Moment.WARN);
                ModCriteria.fire(serverPlayer, ModCriteria.WARNED_OFF);
            }
        }
    }

    public static boolean carriesGoods(Player player) {
        return player.getInventory().hasAnyMatching(stack -> stack.is(ModTags.Items.STRANGE_GOODS));
    }

    /** Offered something of the Grays', he buys it (the whole stack); otherwise a word about the weather, or the sky. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide() || getTarget() != null) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModTags.Items.STRANGE_GOODS) && player instanceof ServerPlayer serverPlayer) {
            int price = MenInBlack.price(held) * held.getCount();
            int count = held.getCount();
            player.setItemInHand(hand, ItemStack.EMPTY);
            MenInBlack.pay(serverPlayer, price);
            Suspicion.add(serverPlayer, Suspicion.SELL * count);
            playSound(SoundEvents.VILLAGER_TRADE, 0.6F, 0.6F);
            Chatter.say(this, serverPlayer, Moment.BOUGHT);
            ModCriteria.fire(serverPlayer, ModCriteria.SOLD_TO_THEM);
            return InteractionResult.SUCCESS;
        }
        getLookControl().setLookAt(player);
        if (player instanceof ServerPlayer serverPlayer) {
            Chatter.talk(this, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("VisitLeft", visitLeft);
        if (hunting) {
            output.putBoolean("Hunting", true);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        visitLeft = input.getIntOr("VisitLeft", VISIT_TICKS);
        hunting = input.getBooleanOr("Hunting", false);
    }

    @Override
    protected void tickHeadTurn(float yBodyRotT) {
        if (!Animations.squareUp(this, this)) {
            super.tickHeadTurn(yBodyRotT);
        }
    }

    @Override
    public GunStats gunStats() {
        return GunTypes.OUTLAW_REVOLVER;
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

    // Their bullets don't touch each other
    @Override
    public boolean holdsFireFor(Entity entity) {
        return entity instanceof ManInBlack;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        Animations.<ManInBlack>register(controllers);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
