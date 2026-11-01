package com.hollowfrontier.content.steam.airship;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.registry.ModCriteria;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
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
 * A steam dirigible: a wicker-and-brass gondola for two hung under a canvas envelope, with a coal-fired boiler at the stern
 * driving a propeller. The boiler turns fuel into steam ({@link #STEAM_PER_TICK} a tick while it burns, a coal lasting
 * {@link #COAL_TICKS}, the same as a Boiler); steam keeps the envelope up and drives the screw.
 * <p>
 * The pilot (first aboard) flies it from their own keys, read on the server like the handcar's: forward/back for the
 * propeller, left/right to turn, jump to climb (venting steam into the envelope), sprint to descend. With nothing to burn
 * and the tank dry it can't climb or drive, and it sinks gently to the ground: it never falls. It takes on coal or charcoal
 * by hand, steam from a canister, or steam from any steam holder it is moored over (a tank, pipe or boiler under its keel).
 * <p>
 * <b>Its owner</b> (31a) is whoever set it down, as with the saucer: other players can ride and stoke it, but their blows
 * (and their shots and dynamite) don't harm it. Only the owner, their team or a creative player can break it, and it
 * drops with the coal and steam it still had. An airship set down before 31a belongs to the first player to pilot or hit it.
 * <p>
 * Movement is worked out on the server only (it has no controlling passenger), and clients interpolate, as with minecarts.
 */
public class Airship extends VehicleEntity implements GeoEntity {
    public static final int CAPACITY = 16_000;
    public static final int MAX_FUEL = 8_000;
    public static final int COAL_TICKS = 1_600;
    public static final int STEAM_PER_TICK = 10;
    /** Steam use: keeping the envelope hot while aloft, climbing, and driving the propeller (they add up). */
    public static final int HOVER_STEAM = 1, CLIMB_STEAM = 3, DRIVE_STEAM = 2;
    public static final double MAX_SPEED = 0.45;
    static final double CLIMB_SPEED = 0.16;
    static final double SINK_SPEED = 0.06;
    static final float TURN_RATE = 3.0F;
    static final int MOOR_RATE = 100;
    /** Above this many blocks over the ground, getting off takes a second sneak within {@link #BAIL_WINDOW} ticks of the first. */
    public static final int BAIL_HEIGHT = 4, BAIL_WINDOW = 30;
    /** With nobody aboard this long (ticks), the ship lets its envelope cool and sinks to the ground. */
    public static final int UNATTENDED_TICKS = 600;

    private static final EntityDataAccessor<Integer> DATA_STEAM = SynchedEntityData.defineId(Airship.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FUEL = SynchedEntityData.defineId(Airship.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DRIVING = SynchedEntityData.defineId(Airship.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("misc.propeller");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Server: the helm this tick (+1 ahead, -1 astern; +1 climb, -1 descend; +1 turn right, -1 left).
    private int throttle;
    private int lift;
    private int rudder;
    private int engineBeat;
    // Helm for a pilotless airship (tests); a pilot's keys replace it.
    private int standingThrottle;
    private int standingLift;
    // Server: ticks with nobody aboard, and when each rider was last warned that the next sneak bails out.
    private int unattended;
    private final java.util.Map<UUID, Long> bailWarnings = new java.util.HashMap<>();
    // Client: how far the ship turned this tick, to turn the local rider with it (as boats do).
    private float turned;
    private @Nullable UUID owner;
    private String ownerName = "";

    public Airship(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STEAM, 0);
        builder.define(DATA_FUEL, 0);
        builder.define(DATA_DRIVING, false);
    }

    public int steam() {
        return entityData.get(DATA_STEAM);
    }

    public void setSteam(int amount) {
        entityData.set(DATA_STEAM, Mth.clamp(amount, 0, CAPACITY));
    }

    public int fuel() {
        return entityData.get(DATA_FUEL);
    }

    public void setFuel(int ticks) {
        entityData.set(DATA_FUEL, Mth.clamp(ticks, 0, MAX_FUEL));
    }

    /** The propeller is turning (driving or climbing under steam). */
    public boolean isDriving() {
        return entityData.get(DATA_DRIVING);
    }

    public int fill(int amount) {
        int taken = Math.min(amount, CAPACITY - steam());
        setSteam(steam() + taken);
        return taken;
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
    }

    // ---------------------------------------------------------------- flying

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
            watchForBailing(level);
            readHelm();
            boil();
            fly(level);
            if (onGround() && getDeltaMovement().horizontalDistanceSqr() < 1.0E-3) {
                moor(level);
            }
            report(level);
        } else {
            turned = Mth.wrapDegrees(getYRot() - yawBefore);
            if (fuel() > 0 && random.nextInt(3) == 0) {
                Vec3 stack = local(0, 2.6, -1.75);
                level().addParticle(ParticleTypes.LARGE_SMOKE, stack.x, stack.y, stack.z, 0, 0.05, 0);
            }
            if (isDriving() && random.nextInt(4) == 0) {
                Vec3 vent = local(0, 0.6, -2.4);
                level().addParticle(ParticleTypes.CLOUD, vent.x, vent.y, vent.z, 0, 0.02, 0);
            }
        }
    }

    private void readHelm() {
        throttle = standingThrottle;
        lift = standingLift;
        rudder = 0;
        unattended = isVehicle() ? 0 : unattended + 1;
        if (getFirstPassenger() instanceof ServerPlayer pilot) {
            Input input = pilot.getLastClientInput();
            throttle = input.forward() ? 1 : input.backward() ? -1 : 0;
            lift = input.jump() ? 1 : input.sprint() ? -1 : 0;
            rudder = input.right() ? 1 : input.left() ? -1 : 0;
        }
    }

    // The boiler: burns fuel while there's room in the tank, like a Boiler block.
    private void boil() {
        if (fuel() > 0 && steam() < CAPACITY) {
            setFuel(fuel() - 1);
            fill(STEAM_PER_TICK);
        }
    }

    private void fly(ServerLevel level) {
        boolean aloft = !onGround();
        int use = aloft ? HOVER_STEAM : 0;
        boolean climbing = lift > 0 && steam() >= HOVER_STEAM + CLIMB_STEAM && getY() < level.getMaxY() - 6;
        boolean driving = throttle != 0 && steam() >= use + DRIVE_STEAM + (climbing ? CLIMB_STEAM : 0);
        if (climbing) {
            use += CLIMB_STEAM;
        }
        if (driving) {
            use += DRIVE_STEAM;
        }
        boolean buoyant = steam() >= use && steam() > 0;
        if (buoyant) {
            setSteam(steam() - use);
        }
        entityData.set(DATA_DRIVING, driving || climbing);

        if (rudder != 0) {
            setYRot(getYRot() + rudder * TURN_RATE * (driving ? 1.0F : 0.6F));
        }
        Vec3 v = getDeltaMovement();
        // Ahead at full speed, astern at half; the hull takes a while to answer the screw.
        double targetSpeed = driving ? (throttle > 0 ? MAX_SPEED : -MAX_SPEED * 0.5) : 0;
        Vec3 heading = Vec3.directionFromRotation(0, getYRot());
        Vec3 horizontal = v.horizontal().lerp(heading.scale(targetSpeed), driving ? 0.05 : (onGround() ? 0.5 : 0.02));
        double targetY;
        if (!buoyant || isAdrift()) {
            targetY = -SINK_SPEED; // a cold envelope sinks, slowly (and one left to itself comes down to wait)
        } else if (climbing) {
            targetY = CLIMB_SPEED;
        } else if (lift < 0) {
            targetY = -CLIMB_SPEED;
        } else if (onGround()) {
            targetY = -0.08; // stay put on the ground (moored) rather than drift up off it
        } else {
            targetY = isInWater() ? 0.05 : 0; // hold altitude; bob up off water
        }
        double vy = Mth.lerp(0.12, v.y, targetY);
        setDeltaMovement(horizontal.x, vy, horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
        if (driving && --engineBeat <= 0) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.AIRSHIP_ENGINE.get(), SoundSource.NEUTRAL, 0.7F, 0.9F + random.nextFloat() * 0.15F);
            engineBeat = 10;
        }
        if (climbing && tickCount % 15 == 0) {
            level.playSound(null, getX(), getY() + 3, getZ(), ModSounds.AIRSHIP_VENT.get(), SoundSource.NEUTRAL, 0.5F, 1.0F);
        }
    }

    /** Left with nobody aboard for {@link #UNATTENDED_TICKS}: it sinks to the ground rather than hang where its crew left it. */
    public boolean isAdrift() {
        return unattended > UNATTENDED_TICKS;
    }

    // ---------------------------------------------------------------- bailing out

    /**
     * Near the ground a rider's sneak gets them off as usual. Aloft, the first sneak only warns them how far down it is;
     * a second within {@link #BAIL_WINDOW} ticks lets them go. The ship ticks before its riders, so clearing the sneak here
     * keeps the rider's own tick from getting them off; the client sends its keys only when they change, so the next
     * sneak is a fresh press.
     */
    private void watchForBailing(ServerLevel level) {
        long now = level.getGameTime();
        bailWarnings.values().removeIf(warned -> now - warned > BAIL_WINDOW);
        for (Entity passenger : getPassengers()) {
            if (!(passenger instanceof ServerPlayer rider) || !rider.isShiftKeyDown()) {
                continue;
            }
            int height = heightAboveGround(level);
            if (height <= BAIL_HEIGHT || bailWarnings.remove(rider.getUUID()) != null) {
                continue; // near the ground, or the second sneak: off they go
            }
            bailWarnings.put(rider.getUUID(), now);
            rider.setShiftKeyDown(false);
            rider.sendOverlayMessage(Component.translatable("message.hollowfrontier.airship.bail", height).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** How many blocks of clear air are under the keel (counted down to the bottom of the world). */
    public int heightAboveGround(Level level) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(getX(), getY() - 0.01, getZ()).mutable();
        int height = 0;
        while (pos.getY() > level.getMinY() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getFluidState(pos).isEmpty()) {
            pos.move(Direction.DOWN);
            height++;
        }
        return height;
    }

    // Whoever leaves the ship aloft, by choice or not (it broke up under them), gets slow falling to see them down.
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (level() instanceof ServerLevel level && passenger instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive()) {
            int height = heightAboveGround(level);
            if (height > BAIL_HEIGHT) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING,
                        20 * (3 + height / 8)));
            }
        }
    }

    /** Moored over a steam holder (a tank, pipe or boiler right under the keel, or beside the block under it): fill up. */
    void moor(ServerLevel level) {
        if (steam() >= CAPACITY) {
            return;
        }
        BlockPos under = BlockPos.containing(getX(), getY() - 0.5, getZ());
        int before = steam();
        for (BlockPos pos : new BlockPos[] {under, under.north(), under.south(), under.east(), under.west()}) {
            int want = Math.min(MOOR_RATE - (steam() - before), CAPACITY - steam());
            if (want <= 0) {
                break;
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
        if (steam() > before && tickCount % 20 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 0.4F, 1.4F);
        }
    }

    private void report(ServerLevel level) {
        if (getFirstPassenger() instanceof ServerPlayer pilot) {
            if (tickCount % 10 == 0 && !bailWarnings.containsKey(pilot.getUUID())) { // the bail warning stays up while it counts
                // A Polar Compass in hand reads out on the gauge line, which would otherwise write over it
                Component gauge = gauge();
                pilot.sendOverlayMessage(com.hollowfrontier.content.hollow.gateway.PolarCompassItem.heldReading(pilot)
                        .<Component>map(reading -> Component.translatable("message.hollowfrontier.airship.gauge_bearing", gauge, reading))
                        .orElse(gauge));
            }
            if (tickCount % 20 == 0 && !onGround()) {
                ModCriteria.AIRSHIP_ALTITUDE.get().trigger(pilot, Mth.floor(getY()));
            }
        }
    }

    public Component gauge() {
        int bars = (int) Math.ceil(10.0 * steam() / CAPACITY);
        String coal = String.format(java.util.Locale.ROOT, "%.1f", fuel() / (double) COAL_TICKS);
        return Component.translatable("message.hollowfrontier.airship.gauge", "▮".repeat(bars) + "▯".repeat(10 - bars),
                steam() * 100 / CAPACITY, coal, Mth.floor(getY()));
    }

    /** A point given in the ship's own frame (blocks to starboard, up, and toward the bow) in world space. */
    private Vec3 local(double right, double up, double ahead) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
        return new Vec3(getX() - right * cos - ahead * sin, getY() + up, getZ() - right * sin + ahead * cos);
    }

    // ---------------------------------------------------------------- crew

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ItemTags.FURNACE_MINECART_FUEL)) {
            if (!level().isClientSide()) {
                if (fuel() + COAL_TICKS <= MAX_FUEL) {
                    setFuel(fuel() + COAL_TICKS);
                    held.consume(1, player);
                    level().playSound(null, getX(), getY(), getZ(), ModSounds.AIRSHIP_STOKE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                player.sendOverlayMessage(gauge());
            }
            return InteractionResult.SUCCESS;
        }
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
        // A rider's eyes are inside the hull's box, so every click aboard lands here: let it through to what they hold
        // (a compass, a gun, a sandwich) rather than eat it.
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
                setOwner(player); // an airship from before 31a: its first pilot claims it
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- its owner

    /** Who set it down: the one player (with their team) who can break it. */
    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().name();
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    /** Whether {@code player} may break it: its owner, one of their team, or a creative player. An unowned one becomes theirs. */
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

    // Another player's blows, shots and blasts glance off; the weather, fire, lava and mobs still count. So do the crew's
    // own: a rider's swing lands on the hull around them, and nobody means to break up their ship in mid-air.
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() != null && hasPassenger(source.getEntity())) {
            return false;
        }
        if (source.getEntity() instanceof Player player && !mayHandle(player)) {
            if (source.getDirectEntity() == player) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.airship.not_yours", ownerName).withStyle(ChatFormatting.RED));
            }
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    /** Broken up, it folds into an airship item that keeps its coal and steam (and its name). */
    @Override
    protected void destroy(ServerLevel level, DamageSource source) {
        kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            ItemStack item = AirshipItem.of(fuel(), steam());
            item.set(DataComponents.CUSTOM_NAME, getCustomName());
            spawnAtLocation(level, item);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    // Pilot at the helm forward, passenger aft by the boiler.
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double ahead = getPassengers().indexOf(passenger) == 0 ? 0.7 : -0.6;
        return new Vec3(0.0, 0.3, ahead).yRot(-getYRot() * Mth.DEG_TO_RAD);
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
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        resetFallDistance();
    }

    @Override
    protected Item getDropItem() {
        return ModItems.AIRSHIP.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.AIRSHIP.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("steam", steam());
        output.putInt("fuel", fuel());
        output.storeNullable("Owner", UUIDUtil.CODEC, owner);
        output.putString("OwnerName", ownerName);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setSteam(input.getIntOr("steam", 0));
        setFuel(input.getIntOr("fuel", 0));
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("OwnerName", "");
    }

    /** For tests: as if nobody had been aboard for {@code ticks}. */
    public void leaveUnattended(int ticks) {
        unattended = ticks;
    }

    /** For tests: a standing helm (throttle and lift, each -1, 0 or +1) used while nobody is aboard. */
    public void setHelm(int throttle, int lift) {
        this.standingThrottle = throttle;
        this.standingLift = lift;
    }

    // ---------------------------------------------------------------- animation

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Airship>("propeller", 5,
                test -> test.animatable().isDriving() ? test.setAndContinue(SPIN) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
