package com.hollowfrontier.content.ufo;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.furniture.BarStoolBlock;
import com.hollowfrontier.content.frontier.furniture.SeatEntity;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A Gray in disguise (21c): stranded on the frontier after the leash broke, it tries to pass for a cowpoke in a duster and
 * a hat too big for it, walking stiffly and holding a beer it never drinks. It doesn't fight. It comes to a town for a
 * while ({@link GrayVisitors}) and talks like a Gray trying to talk cowboy; the townsfolk half-notice.
 * <ul>
 * <li><b>Exposed</b> ({@link #expose}): a Sunstone Lens used on it, or a blow from a player. The hat falls off, the townsfolk
 *     near shout, and it runs for {@link #FLEE_TICKS} before it blinks away, dropping a Gray tech fragment. Not From
 *     Around Here. Exposing the town's regular costs the exposer {@link #REGULAR_HEAT} heat: the town liked him.</li>
 * <li><b>Left alone</b>, its visit ends ({@link #VISIT_TICKS}) and it leaves; after the town's
 *     {@link GrayVisitors#VISITS_TO_STAY}th it stays for good as the saloon's regular, <b>Mr. Gray</b>, on a bar stool
 *     near where it came in. He has his own lines and one trade, once a day: a Phlogiston Cell for a Sarsaparilla. Talking to
 *     him or trading earns Regular Customer.</li>
 * </ul>
 */
public class DisguisedGray extends PathfinderMob implements GeoEntity {
    public static final long NO_TOWN = Long.MIN_VALUE;
    public static final int VISIT_TICKS = 3600, FLEE_TICKS = 100;
    public static final int REGULAR_HEAT = 2;
    public static final double STOOL_REACH = 12.0;
    private static final EntityDataAccessor<Boolean> DATA_EXPOSED = SynchedEntityData.defineId(DisguisedGray.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_REGULAR = SynchedEntityData.defineId(DisguisedGray.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("misc.sit");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private long town = NO_TOWN;
    private int visitLeft;
    private int fleeLeft;
    private long lastTradeDay = -1;

    public DisguisedGray(EntityType<? extends DisguisedGray> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_EXPOSED, false);
        builder.define(DATA_REGULAR, false);
    }

    public boolean isExposed() {
        return entityData.get(DATA_EXPOSED);
    }

    public boolean isRegular() {
        return entityData.get(DATA_REGULAR);
    }

    public long town() {
        return town;
    }

    public int visitLeft() {
        return visitLeft;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** It comes into the town under {@code key} at {@code at}, beer in hand, for a visit. */
    public void startVisit(long key, BlockPos at) {
        town = key;
        visitLeft = VISIT_TICKS;
        setHomeTo(at, 8);
        setPersistenceRequired();
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.FRONTIER_BEER.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    /** It stays for good: Mr. Gray, the saloon's regular, on a stool if there's one free. */
    public void becomeRegular() {
        entityData.set(DATA_REGULAR, true);
        visitLeft = 0;
        setCustomName(Component.translatable("entity.hollowfrontier.disguised_gray.regular"));
        setPersistenceRequired();
        takeAStool();
    }

    private void takeAStool() {
        if (isPassenger() || !(level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos home = hasHome() ? getHomePosition() : blockPosition();
        int r = (int) STOOL_REACH;
        for (BlockPos pos : BlockPos.betweenClosed(home.offset(-r, -2, -r), home.offset(r, 2, r))) {
            if (level.getBlockState(pos).getBlock() instanceof BarStoolBlock && SeatEntity.sit(level, pos.immutable(), this)) {
                return;
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (isExposed()) {
            if (--fleeLeft <= 0) {
                blinkAway(level);
            } else if (fleeLeft % 10 == 0 && level.getNearestPlayer(this, 24.0) instanceof Player near) {
                Vec3 away = position().subtract(near.position()).multiply(1, 0, 1).normalize().scale(10);
                getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.6);
            }
            return;
        }
        if (isRegular()) {
            if (tickCount % 100 == 0) {
                takeAStool();
            }
        } else if (town != NO_TOWN && --visitLeft <= 0) {
            endVisit(level);
            return;
        }
        if ((tickCount + getId()) % 20 == 0) {
            Chatter.lookAround(this);
        }
    }

    /** Its visit's over: it counts towards the town's regular, and stays as him or leaves. Whether it stayed. */
    public boolean endVisit(ServerLevel level) {
        if (town != NO_TOWN && GrayVisitors.get(level).visitEnded(town, getUUID())) {
            becomeRegular();
            return true;
        }
        blinkAway(level);
        return false;
    }

    private void blinkAway(ServerLevel level) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(1.0), getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8F, 1.6F);
        discard();
    }

    /**
     * Found out ({@code by} a player, or nobody in particular): the hat comes off, the town shouts, and it runs, dropping a
     * Gray tech fragment. Whether it was news (an exposed one can't be exposed again).
     */
    public boolean expose(ServerLevel level, @Nullable ServerPlayer by) {
        if (isExposed()) {
            return false;
        }
        boolean wasRegular = isRegular();
        entityData.set(DATA_EXPOSED, true);
        entityData.set(DATA_REGULAR, false);
        stopRiding();
        fleeLeft = FLEE_TICKS;
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
        spawnAtLocation(level, new ItemStack(ModItems.GRAY_TECH_FRAGMENT.get()));
        level.sendParticles(ParticleTypes.POOF, getX(), getY(1.1), getZ(), 12, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        // The town panics a moment: those near shout and scatter
        for (Townsfolk folk : level.getEntitiesOfClass(Townsfolk.class, getBoundingBox().inflate(16), t -> t.isAlive() && !t.isPassenger())) {
            Chatter.bark(folk, Moment.EXPOSED);
            Vec3 away = folk.position().subtract(position()).multiply(1, 0, 1).normalize().scale(8);
            folk.getNavigation().moveTo(folk.getX() + away.x, folk.getY(), folk.getZ() + away.z, 1.4);
        }
        if (town != NO_TOWN) {
            GrayVisitors.get(level).exposed(town, getUUID());
        }
        if (by != null) {
            ModCriteria.fire(by, ModCriteria.GRAY_EXPOSED);
            if (wasRegular) {
                Wanted.addHeat(by, REGULAR_HEAT);
                by.sendSystemMessage(Component.translatable("message.hollowfrontier.disguised_gray.liked").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isExposed() || !(level() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
            return isExposed() ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.SUNSTONE_LENS.get())) {
            expose(level, serverPlayer);
        } else if (isRegular() && held.is(ModItems.SARSAPARILLA.get())) {
            trade(level, serverPlayer, held);
        } else {
            Chatter.talk(this, serverPlayer);
            if (isRegular()) {
                ModCriteria.fire(serverPlayer, ModCriteria.REGULAR_CUSTOMER);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Mr. Gray's one trade: a Sarsaparilla for a Phlogiston Cell, once a day. Whether it went through. */
    public boolean trade(ServerLevel level, ServerPlayer player, ItemStack sarsaparilla) {
        long today = BountyBoards.day(level);
        if (!isRegular() || lastTradeDay == today) {
            say(player, "message.hollowfrontier.disguised_gray.tomorrow");
            return false;
        }
        if (!player.getAbilities().instabuild) {
            sarsaparilla.shrink(1);
        }
        BoardUse.give(player, new ItemStack(ModItems.PHLOGISTON_CELL.get()));
        lastTradeDay = today;
        say(player, "message.hollowfrontier.disguised_gray.traded");
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.4F);
        ModCriteria.fire(player, ModCriteria.REGULAR_CUSTOMER);
        return true;
    }

    private void say(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.npc.say", getDisplayName(), Component.translatable(key)));
    }

    // A blow from a player gives it away
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!isExposed() && source.getEntity() instanceof ServerPlayer player) {
            expose(level, player);
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void die(DamageSource source) {
        if (isRegular() && town != NO_TOWN && level() instanceof ServerLevel level) {
            GrayVisitors.get(level).regularGone(town, getUUID());
        }
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("town", town);
        output.putInt("visit_left", visitLeft);
        output.putInt("flee_left", fleeLeft);
        output.putLong("last_trade", lastTradeDay);
        output.putBoolean("regular", isRegular());
        output.putBoolean("exposed", isExposed());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        town = input.getLongOr("town", NO_TOWN);
        visitLeft = input.getIntOr("visit_left", 0);
        fleeLeft = input.getIntOr("flee_left", 0);
        lastTradeDay = input.getLongOr("last_trade", -1L);
        entityData.set(DATA_REGULAR, input.getBooleanOr("regular", false));
        entityData.set(DATA_EXPOSED, input.getBooleanOr("exposed", false));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DisguisedGray>("main", 4, state -> {
            DisguisedGray gray = state.animatable();
            if (gray.isPassenger()) {
                return state.setAndContinue(SIT);
            }
            if (state.isMoving()) {
                return state.setAndContinue(gray.isExposed() ? DefaultAnimations.RUN : DefaultAnimations.WALK);
            }
            return state.setAndContinue(DefaultAnimations.IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
