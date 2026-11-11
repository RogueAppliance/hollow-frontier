package com.hollowfrontier.content.steam.submersible;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.hollow.sea.SeaLife;
import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.registry.ModFluids;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Steam Submersible (43c): a brass and copper boat for two, with its crew under a caged dome amidships and a screw
 * at the stern. It is <b>fireless</b>: nothing burns underwater, so there's no firebox and no coal, only a reservoir of
 * {@link #CAPACITY} mB of steam charged before it leaves: from canisters by hand, or by itself from any steam holder
 * (a tank, pipe or boiler) within a block of its hull, which is what a boiler on the shore is for.
 * <p>
 * The pilot (first aboard) drives it from their own keys, read on the server as the airship's are: forward and back for
 * the screw, left and right to turn, jump to rise, sprint to dive. Holding still costs nothing. With the reservoir dry
 * it can't drive or dive and it rises, slowly, until it's awash: it never sinks for good (the airship's rule turned
 * over). Out of the water it sits where it is and won't drive.
 * <p>
 * Its riders don't drown and see by its lamp ({@link #LAMP_TICKS} of night vision, kept topped up). Its owner is whoever
 * launched it, as with the airship: others can ride and charge it, but only the owner, their team or a creative player
 * can break it up, and it folds into an item that keeps its steam. The sea's own dangers are another matter: a
 * Basilosaurus rams the hull ({@link #ram}), and {@link #HULL} of that breaks it up under its crew.
 * <p>
 * Movement is worked out on the server only, and clients interpolate, as with the airship.
 */
public class SteamSubmersible extends VehicleEntity implements GeoEntity {
    public static final int CAPACITY = 16_000;
    /** Steam use a tick: turning the screw, and pumping ballast to rise or dive (they add up). Holding still is free. */
    public static final int DRIVE_STEAM = 2, BALLAST_STEAM = 1;
    public static final double MAX_SPEED = 0.32;
    static final double BALLAST_SPEED = 0.12;
    /** How fast it comes up by itself with a dry reservoir. */
    static final double DRY_RISE = 0.04;
    static final float TURN_RATE = 3.0F;
    /** Charging from steam holders beside it: every {@link #CHARGE_INTERVAL} ticks, up to this much. */
    public static final int CHARGE_INTERVAL = 5, CHARGE_AMOUNT = 500;
    /** What the hull takes from the sea (a Basilosaurus's ram, a Drowned's trident) before it breaks up; it mends one point every {@link #MEND_TICKS}. */
    public static final float HULL = 40.0F;
    public static final int MEND_TICKS = 100;
    public static final int LAMP_TICKS = 300;

    private static final EntityDataAccessor<Integer> DATA_STEAM = SynchedEntityData.defineId(SteamSubmersible.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DRIVING = SynchedEntityData.defineId(SteamSubmersible.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_HULL = SynchedEntityData.defineId(SteamSubmersible.class, EntityDataSerializers.FLOAT);

    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("misc.propeller");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Server: the helm this tick (+1 ahead, -1 astern; +1 rise, -1 dive; +1 turn right, -1 left).
    private int throttle;
    private int ballast;
    private int rudder;
    private int engineBeat;
    // Helm for a pilotless submersible (tests); a pilot's keys replace it.
    private int standingThrottle;
    private int standingBallast;
    // Client: how far it turned this tick, to turn the local rider with it (as boats do).
    private float turned;
    private @Nullable UUID owner;
    private String ownerName = "";

    public SteamSubmersible(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STEAM, 0);
        builder.define(DATA_DRIVING, false);
        builder.define(DATA_HULL, HULL);
    }

    public int steam() {
        return entityData.get(DATA_STEAM);
    }

    public void setSteam(int amount) {
        entityData.set(DATA_STEAM, Mth.clamp(amount, 0, CAPACITY));
    }

    public int fill(int amount) {
        int taken = Math.min(amount, CAPACITY - steam());
        setSteam(steam() + taken);
        return taken;
    }

    /** The screw is turning. */
    public boolean isDriving() {
        return entityData.get(DATA_DRIVING);
    }

    /** What the hull has left, of {@link #HULL}. */
    public float hull() {
        return entityData.get(DATA_HULL);
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
    }

    // ---------------------------------------------------------------- under way

    /** Whether the hull is in the water (its middle is), and whether it's awash (in the water with its dome out). */
    public boolean afloat() {
        return SeaLife.isWater(level(), BlockPos.containing(getX(), getY() + 0.4, getZ()));
    }

    public boolean awash() {
        return afloat() && !SeaLife.isWater(level(), BlockPos.containing(getX(), getY() + 1.3, getZ()));
    }

    /** Blocks of water over the hull (0 when awash or out of the water). */
    public int depth() {
        BlockPos.MutableBlockPos pos = BlockPos.containing(getX(), getY() + 1.3, getZ()).mutable();
        int depth = 0;
        while (depth < 128 && SeaLife.isWater(level(), pos)) {
            depth++;
            pos.move(Direction.UP);
        }
        return depth;
    }

    @Override
    public void tick() {
        if (getHurtTime() > 0) {
            setHurtTime(getHurtTime() - 1);
        }
        if (getDamage() > 0.0F) {
            setDamage(getDamage() - 1.0F);
        }
        float yawBefore = getYRot();
        super.tick();
        if (level() instanceof ServerLevel level) {
            readHelm();
            run(level);
            if (tickCount % CHARGE_INTERVAL == 0 && getDeltaMovement().horizontalDistanceSqr() < 1.0E-3) {
                charge(level);
            }
            if (hull() < HULL && tickCount % MEND_TICKS == 0) {
                entityData.set(DATA_HULL, Math.min(HULL, hull() + 1.0F));
            }
            keepCrew();
            report();
        } else {
            turned = Mth.wrapDegrees(getYRot() - yawBefore);
            if (isDriving() && afloat() && random.nextInt(2) == 0) {
                Vec3 screw = local(0, 0.5, -1.7);
                level().addParticle(ParticleTypes.BUBBLE, screw.x, screw.y, screw.z, 0, 0.04, 0);
            }
        }
    }

    private void readHelm() {
        throttle = standingThrottle;
        ballast = standingBallast;
        rudder = 0;
        if (getFirstPassenger() instanceof ServerPlayer pilot) {
            Input input = pilot.getLastClientInput();
            throttle = input.forward() ? 1 : input.backward() ? -1 : 0;
            ballast = input.jump() ? 1 : input.sprint() ? -1 : 0;
            rudder = input.right() ? 1 : input.left() ? -1 : 0;
        }
    }

    private void run(ServerLevel level) {
        boolean afloat = afloat(), awash = awash();
        boolean pumping = afloat && ballast != 0 && steam() >= BALLAST_STEAM && !(ballast > 0 && awash);
        boolean driving = afloat && throttle != 0 && steam() >= DRIVE_STEAM + (pumping ? BALLAST_STEAM : 0);
        setSteam(steam() - (pumping ? BALLAST_STEAM : 0) - (driving ? DRIVE_STEAM : 0));
        entityData.set(DATA_DRIVING, driving);

        if (rudder != 0 && afloat) {
            setYRot(getYRot() + rudder * TURN_RATE * (driving ? 1.0F : 0.6F));
        }
        Vec3 v = getDeltaMovement();
        // Ahead at full speed, astern at half; water takes the way off a hull quickly.
        double targetSpeed = driving ? (throttle > 0 ? MAX_SPEED : -MAX_SPEED * 0.5) : 0;
        Vec3 heading = Vec3.directionFromRotation(0, getYRot());
        Vec3 horizontal = v.horizontal().lerp(heading.scale(targetSpeed), driving ? 0.06 : (afloat ? 0.05 : 0.5));
        double targetY;
        if (!afloat) {
            targetY = -0.3; // out of the water it's a ton of brass: it drops, and sits
        } else if (pumping) {
            targetY = ballast * BALLAST_SPEED;
        } else if (steam() < BALLAST_STEAM && !awash) {
            targetY = DRY_RISE; // a dry reservoir: up she comes
        } else {
            targetY = 0; // trimmed: it holds its depth
        }
        setDeltaMovement(horizontal.x, Mth.lerp(0.15, v.y, targetY), horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
        if (driving && --engineBeat <= 0) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.AIRSHIP_ENGINE.get(), SoundSource.NEUTRAL, 0.5F, 0.6F + random.nextFloat() * 0.1F);
            engineBeat = 12;
        }
    }

    /**
     * Takes steam from any holder within a block of the hull (a tank on a jetty, a pipe run down to the waterline, a
     * boiler on the shore). Returns what it took.
     */
    public int charge(ServerLevel level) {
        if (steam() >= CAPACITY) {
            return 0;
        }
        int before = steam();
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(getBoundingBox().getMinPosition().subtract(1, 1, 1)),
                BlockPos.containing(getBoundingBox().getMaxPosition().add(1, 1, 1)))) {
            int want = Math.min(CHARGE_AMOUNT - (steam() - before), CAPACITY - steam());
            if (want <= 0) {
                break;
            }
            if (level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) {
                continue;
            }
            ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP);
            if (handler == null) {
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int got = ModFluids.extractSteam(handler, want, tx);
                if (got > 0) {
                    tx.commit();
                    fill(got);
                }
            }
        }
        int took = steam() - before;
        if (took > 0 && tickCount % 20 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 0.4F, 1.4F);
        }
        return took;
    }

    /** Air for everyone aboard, and the lamp to see by. */
    private void keepCrew() {
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof LivingEntity rider) {
                rider.setAirSupply(rider.getMaxAirSupply());
                if (tickCount % 20 == 0) {
                    rider.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, LAMP_TICKS, 0, true, false, false));
                }
            }
        }
    }

    // The lamp goes out for whoever gets out (their own potion, if they had one, shows its particles and is left alone).
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && passenger instanceof LivingEntity rider) {
            MobEffectInstance lamp = rider.getEffect(MobEffects.NIGHT_VISION);
            if (lamp != null && lamp.isAmbient() && !lamp.isVisible()) {
                rider.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    private void report() {
        if (getFirstPassenger() instanceof ServerPlayer pilot && tickCount % 10 == 0) {
            pilot.sendOverlayMessage(gauge());
        }
    }

    public Component gauge() {
        int bars = (int) Math.ceil(10.0 * steam() / CAPACITY);
        return Component.translatable("message.hollowfrontier.submersible.gauge", "▮".repeat(bars) + "▯".repeat(10 - bars),
                steam() * 100 / CAPACITY, depth(), Math.round(hull() * 100 / HULL));
    }

    /** A point given in its own frame (blocks to starboard, up, and toward the bow) in world space. */
    private Vec3 local(double right, double up, double ahead) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
        return new Vec3(getX() - right * cos - ahead * sin, getY() + up, getZ() - right * sin + ahead * cos);
    }

    // ---------------------------------------------------------------- crew

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof SteamCanisterItem) {
            if (!level().isClientSide()) {
                int poured = fill(SteamCanisterItem.steam(held));
                if (poured > 0) {
                    SteamCanisterItem.setSteam(held, SteamCanisterItem.steam(held) - poured);
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.6F, 1.5F);
                }
                player.sendOverlayMessage(gauge());
            }
            return InteractionResult.SUCCESS;
        }
        // A rider's eyes are inside the hull's box, so every click aboard lands here: let it through to what they hold.
        if (hasPassenger(player)) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide()) {
                player.sendOverlayMessage(gauge());
            }
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide()) {
            if (!player.startRiding(this)) {
                return InteractionResult.PASS;
            }
            if (owner == null && getFirstPassenger() == player) {
                setOwner(player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- its owner, and what can break it

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().name();
    }

    public @Nullable UUID owner() {
        return owner;
    }

    /** Whether {@code player} may break it up: its owner, one of their team, or a creative player. An unowned one becomes theirs. */
    public boolean mayHandle(Player player) {
        if (owner == null) {
            setOwner(player);
            return true;
        }
        if (owner.equals(player.getUUID()) || player.getAbilities().instabuild) {
            return true;
        }
        var team = player.getTeam();
        return team != null && team == level().getScoreboard().getPlayersTeam(ownerName);
    }

    /**
     * Its crew's own swings and another player's blows glance off. A player who may handle it breaks it up as any boat
     * breaks. Anything else alive (the sea's own) wears the hull instead, so one bump doesn't put its crew in the water.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null && hasPassenger(attacker)) {
            return false;
        }
        if (attacker instanceof Player player) {
            if (!mayHandle(player)) {
                if (source.getDirectEntity() == player) {
                    player.sendOverlayMessage(Component.translatable("message.hollowfrontier.submersible.not_yours", ownerName).withStyle(ChatFormatting.RED));
                }
                return false;
            }
            return super.hurtServer(level, source, amount);
        }
        if (attacker instanceof LivingEntity) {
            return ram(level, source, amount);
        }
        return super.hurtServer(level, source, amount);
    }

    /** A blow on the hull from something in the sea: it rings, it holds, and when {@link #HULL} is used up it doesn't. */
    public boolean ram(ServerLevel level, DamageSource source, float amount) {
        if (isRemoved() || amount <= 0) {
            return false;
        }
        setHurtTime(10);
        entityData.set(DATA_HULL, hull() - amount);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.5F, 0.5F);
        if (source.getEntity() != null) {
            Vec3 shove = position().subtract(source.getEntity().position()).multiply(1, 0, 1);
            if (shove.lengthSqr() > 1.0E-4) {
                setDeltaMovement(getDeltaMovement().add(shove.normalize().scale(0.35)));
            }
        }
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof ServerPlayer rider) {
                rider.sendOverlayMessage(Component.translatable("message.hollowfrontier.submersible.rammed", Math.max(0, Math.round(hull() * 100 / HULL)))
                        .withStyle(ChatFormatting.RED));
            }
        }
        if (hull() <= 0) {
            destroy(level, source);
        }
        return true;
    }

    /** Broken up, it folds into an item that keeps its steam (and its name). */
    @Override
    protected void destroy(ServerLevel level, DamageSource source) {
        kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            ItemStack item = SubmersibleItem.of(steam());
            item.set(DataComponents.CUSTOM_NAME, getCustomName());
            spawnAtLocation(level, item);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    // Pilot forward under the dome, passenger behind.
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double ahead = getPassengers().indexOf(passenger) == 0 ? 0.22 : -0.42; // well back from the glass
        return new Vec3(0.0, 0.35, ahead).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (turned != 0 && passenger.isLocalInstanceAuthoritative()) {
            passenger.setYRot(passenger.getYRot() + turned);
            passenger.setYHeadRot(passenger.getYHeadRot() + turned);
        }
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        resetFallDistance();
    }

    @Override
    protected Item getDropItem() {
        return ModItems.STEAM_SUBMERSIBLE.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.STEAM_SUBMERSIBLE.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("steam", steam());
        output.putFloat("hull", hull());
        output.storeNullable("Owner", UUIDUtil.CODEC, owner);
        output.putString("OwnerName", ownerName);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setSteam(input.getIntOr("steam", 0));
        entityData.set(DATA_HULL, input.getFloatOr("hull", HULL));
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("OwnerName", "");
    }

    /** For tests: a standing helm (throttle and ballast, each -1, 0 or +1) used while nobody is aboard. */
    public void setHelm(int throttle, int ballast) {
        this.standingThrottle = throttle;
        this.standingBallast = ballast;
    }

    // ---------------------------------------------------------------- animation

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SteamSubmersible>("propeller", 5,
                test -> test.animatable().isDriving() ? test.setAndContinue(SPIN) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
