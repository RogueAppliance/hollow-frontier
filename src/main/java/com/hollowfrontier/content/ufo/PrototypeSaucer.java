package com.hollowfrontier.content.ufo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModGameRules;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.registry.ModTags;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * The Prototype Saucer (13c): a craft of the player's own, crafted from crash salvage ({@link PrototypeSaucerItem}) and
 * flown by phlogiston. A pilot and one passenger sit under its glass dome. It comes with the tractor beam and heat laser;
 * use a navigation core on it to fit bearings.
 * <ul>
 * <li><b>Flight:</b> it faces where its pilot looks. Forward and back drive it ({@link #MAX_SPEED}, about three times the
 *     airship's), left and right slide it sideways, jump lifts it and sprint lowers it; with no keys held it hovers where it
 *     is. Aloft, it burns a charge of phlogiston every {@link #HOVER_TICKS} ticks. Run dry, it draws a cell's worth from the
 *     Phlogiston Cells (or failing those a phlogiston shard) its pilot carries ({@link #refuel}); with nothing to draw it can't drive or
 *     climb, sinks gently to the ground, and the gauge says why. Use a Phlogiston Cell on it to pour the cell's charge in, or a
 *     phlogiston shard for {@link PhlogistonCellItem#CHARGES}; it holds {@link #MAX_CHARGE}.</li>
 * <li><b>Heat laser</b> (standard; the laser key, held): every {@link #LASER_INTERVAL} ticks a bolt from its
 *     underside along the pilot's look, up to {@link #LASER_RANGE} blocks: {@link #LASER_DAMAGE} and fire to a creature, or
 *     a scorch and a little fire on the ground (never in a town). Each bolt builds {@link #LASER_HEAT} heat; at
 *     {@link #MAX_HEAT} it overheats and won't fire again until it's cooled to {@link #RESUME_HEAT}. A charge every
 *     {@link #SHOTS_PER_CHARGE} bolts.</li>
 * <li><b>Tractor beam</b> (standard; the beam key, held): a column of light straight down, {@link #BEAM_RADIUS}
 *     across and {@link #BEAM_DEPTH} deep, that draws up creatures, items and (unless the {@code saucer_beams_players}
 *     gamerule says no) players, and carries them along hanging just under the craft. Let the key go, or let them leave the
 *     light, and they're let down gently. A carried player struggles free by jumping, {@link #STRUGGLE} times. A charge
 *     every {@link #BEAM_TICKS_PER_CHARGE} ticks.</li>
 * <li><b>Navigation core:</b> the pilot's gauge adds the bearing and distance to the nearest Polar Gateway and to where
 *     the saucer was set down.</li>
 * <li><b>Cloaking device</b> (15b, from a Mothership rematch): at night, with a pilot aboard, nobody else sees the craft
 *     and its crew go invisible; every mob hunting them loses track of them and can't take them up again till the
 *     cloak drops. The Mothership and its drones aren't fooled: it's their own trick.</li>
 * <li><b>Cargo hold</b> (15b, from a Mothership rematch): {@link #HOLD_SLOTS} slots, opened by its crew with the hold key.
 *     What's in it goes with the saucer when it's folded, and spills out if the craft is destroyed.</li>
 * </ul>
 * Nothing harms it. Landed and empty, it folds back into its item (sneak and use it bare-handed), fittings, charge, cargo
 * and all. Movement is the server's (like the airship's), and clients interpolate.
 * <p>
 * <b>Its owner</b> (15b) is whoever set it down: only they, their team, or a creative player can fit parts to it, open
 * its hold or fold it. Anyone may ride in it. A saucer set down before 15b belongs to the first player to do one of those.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public class PrototypeSaucer extends VehicleEntity implements GeoEntity {
    public static final int MAX_CHARGE = 256;
    public static final double MAX_SPEED = 1.35, CLIMB_SPEED = 0.35, SINK_SPEED = 0.08;
    public static final int HOVER_TICKS = 100;
    public static final int LASER_INTERVAL = 5, LASER_HEAT = 6, MAX_HEAT = 100, RESUME_HEAT = 30, SHOTS_PER_CHARGE = 4;
    public static final double LASER_RANGE = 48.0;
    public static final float LASER_DAMAGE = 12.0F; // doubled after the Mothership playtest (6 barely scratched it)
    public static final double BEAM_RADIUS = 2.5, BEAM_DEPTH = 24.0;
    public static final int BEAM_TICKS_PER_CHARGE = 40, STRUGGLE = 20, FREED_TICKS = 60;
    public static final int HOLD_SLOTS = 9;
    public static final double CLOAK_RANGE = 48.0;
    static final int LASER_COLOUR = 0xFF7A30;
    // Played end to end every 2 s in flight: at 0.9 it drowned everything (playtest)
    static final float HUM_VOLUME = 0.35F;
    // A reported sight is good for this long, and only this close (the laser's reach plus the third-person camera's)
    static final int SIGHT_TICKS = 5;
    static final double SIGHT_RANGE = LASER_RANGE + 16.0;
    private static final DustParticleOptions BEAM_LIGHT = new DustParticleOptions(0xC8FFD8, 1.1F);

    private static final EntityDataAccessor<Integer> DATA_FITTINGS = SynchedEntityData.defineId(PrototypeSaucer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CHARGE = SynchedEntityData.defineId(PrototypeSaucer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(PrototypeSaucer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BEAMING = SynchedEntityData.defineId(PrototypeSaucer.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CLOAKED = SynchedEntityData.defineId(PrototypeSaucer.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation BEAMING = RawAnimation.begin().thenLoop("beam");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private SaucerFittings fittings = SaucerFittings.STANDARD;
    private @Nullable BlockPos home;
    // Server: the helm this tick, the weapons' keys, and the counters that burn charge
    private int throttle, strafe, lift;
    private int standingThrottle, standingLift;
    private boolean laserHeld, beamHeld, overheated;
    private @Nullable Vec3 sight;
    private int sightTick;
    private int aloftTicks, laserTicks, shots, beamTicks;
    private final Set<UUID> carried = new HashSet<>();
    private final Map<UUID, Integer> struggles = new HashMap<>();
    private final Map<UUID, Long> freed = new HashMap<>();
    private @Nullable BlockPos gateway;
    private long gatewaySearched = Long.MIN_VALUE;
    private @Nullable UUID owner;
    private String ownerName = "";
    private @Nullable Boolean darkForTests;
    // The hold, shared by every crewman who opens it; a window on it closes once its viewer leaves the craft
    private final SimpleContainer cargo = new SimpleContainer(HOLD_SLOTS) {
        @Override
        public boolean stillValid(Player player) {
            return isAlive() && fittings.hold() && hasPassenger(player);
        }
    };

    public PrototypeSaucer(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    // ---------------------------------------------------------------- its owner

    /** Who set it down: the one player (with their team) who can fit it, open its hold and fold it. */
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

    /**
     * Whether {@code player} may fit parts to it, open its hold or fold it: its owner, one of their team, or a creative
     * player. A saucer with no owner yet (set down before 15b) becomes theirs.
     */
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

    private void refuse(Player player) {
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.saucer.not_yours", ownerName).withStyle(ChatFormatting.RED));
    }

    // ---------------------------------------------------------------- the cargo hold

    public SimpleContainer cargo() {
        return cargo;
    }

    /** Opens the hold for one of its crew. Null if it opened, or why not ({@code no_hold}, {@code not_yours}). */
    public @Nullable String openHold(ServerPlayer player) {
        if (!fittings.hold()) {
            return "no_hold";
        }
        if (!hasPassenger(player)) {
            return "hold_outside";
        }
        if (!mayHandle(player)) {
            return "not_yours";
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inventory, cargo, 1),
                ModItems.CARGO_HOLD.get().getDefaultInstance().getHoverName()));
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.NEUTRAL, 0.8F, 1.4F);
        return null;
    }

    /** Puts {@code contents} in its hold (from its item as it's set down, or its save). */
    public void stow(ItemContainerContents contents) {
        NonNullList<ItemStack> items = NonNullList.withSize(HOLD_SLOTS, ItemStack.EMPTY);
        contents.copyInto(items);
        for (int i = 0; i < HOLD_SLOTS; i++) {
            cargo.setItem(i, items.get(i));
        }
    }

    // ---------------------------------------------------------------- the cloak

    public boolean isCloaked() {
        return entityData.get(DATA_CLOAKED);
    }

    /** For tests: night ({@code true}) or day ({@code false}) as the cloak sees it, whatever the clock says; null to go by the clock. */
    public void setDarkForTests(@Nullable Boolean dark) {
        darkForTests = dark;
    }

    private boolean dark(ServerLevel level) {
        return darkForTests != null ? darkForTests : level.isDarkOutside();
    }

    /** Cloaked, it hides its crew and shakes off whatever was hunting them. */
    private void cloak(ServerLevel level, @Nullable ServerPlayer pilot) {
        boolean on = fittings.cloak() && pilot != null && dark(level);
        if (on != isCloaked()) {
            entityData.set(DATA_CLOAKED, on);
            level.playSound(null, getX(), getY(), getZ(), on ? SoundEvents.ILLUSIONER_MIRROR_MOVE : SoundEvents.ILLUSIONER_CAST_SPELL,
                    SoundSource.NEUTRAL, 0.8F, 1.3F);
        }
        if (!on || tickCount % 10 != 0) {
            return;
        }
        for (Entity crew : getPassengers()) {
            if (crew instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false, true));
            }
        }
        for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(CLOAK_RANGE), m -> m.getTarget() != null && hasPassenger(m.getTarget()))) {
            if (!seesThroughCloaks(mob)) {
                mob.setTarget(null);
            }
        }
    }

    /** The Grays' own craft know the trick. */
    static boolean seesThroughCloaks(Entity e) {
        return e instanceof Mothership || e instanceof GrayDrone;
    }

    /** While a saucer's cloaked, no mob (but the Grays') can take up its crew as a target. */
    @SubscribeEvent
    static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && target.getVehicle() instanceof PrototypeSaucer saucer && saucer.isCloaked() && !seesThroughCloaks(event.getEntity())) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /** Its parts, its charge and where it was set down: from its item as it's placed. */
    public void outfit(SaucerFittings fittings, int charge, BlockPos home) {
        setFittings(fittings);
        this.home = home.immutable();
        setCharge(charge);
    }

    private void setFittings(SaucerFittings fittings) {
        this.fittings = fittings;
        entityData.set(DATA_FITTINGS, fittings.mask());
    }

    public SaucerFittings fittings() {
        return fittings;
    }

    public int fittingsMask() {
        return entityData.get(DATA_FITTINGS);
    }

    public int charge() {
        return entityData.get(DATA_CHARGE);
    }

    public void setCharge(int charge) {
        entityData.set(DATA_CHARGE, Mth.clamp(charge, 0, MAX_CHARGE));
    }

    public int heat() {
        return entityData.get(DATA_HEAT);
    }

    public boolean isOverheated() {
        return overheated;
    }

    public boolean isBeaming() {
        return entityData.get(DATA_BEAMING);
    }

    public Set<UUID> carried() {
        return Set.copyOf(carried);
    }

    public @Nullable BlockPos home() {
        return home;
    }

    /**
     * Where the pilot's sight points, as their client reports it each tick the laser's held (the camera's middle: in third
     * person the camera rides above the craft, off the pilot's eye line). Bolts aim at it while it's fresh; a point
     * further off than the laser could reach from here is ignored.
     */
    public void sight(Vec3 point) {
        if (point.distanceToSqr(position()) <= SIGHT_RANGE * SIGHT_RANGE) {
            this.sight = point;
            this.sightTick = tickCount;
        }
    }

    /** The weapon keys, as the pilot's client reports them (SaucerControlPayload). */
    public void setControls(boolean laser, boolean beam) {
        this.laserHeld = laser;
        this.beamHeld = beam;
    }

    /** For tests: a standing helm (throttle and lift, each -1, 0 or +1) used while nobody is aboard. */
    public void setHelm(int throttle, int lift) {
        this.standingThrottle = throttle;
        this.standingLift = lift;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FITTINGS, 0);
        builder.define(DATA_CHARGE, 0);
        builder.define(DATA_HEAT, 0);
        builder.define(DATA_BEAMING, false);
        builder.define(DATA_CLOAKED, false);
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return net.minecraft.world.entity.LinearInterpolationHandler.create(this, 3);
    }

    // ---------------------------------------------------------------- flying

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer pilot = getFirstPassenger() instanceof ServerPlayer p ? p : null;
        readHelm(pilot);
        if (pilot != null && charge() == 0) {
            refuel(pilot);
        }
        fly(level);
        laser(level, pilot);
        beam(level, pilot);
        cloak(level, pilot);
        report(level, pilot);
    }

    private void readHelm(@Nullable ServerPlayer pilot) {
        throttle = standingThrottle;
        lift = standingLift;
        strafe = 0;
        if (pilot == null) {
            laserHeld = beamHeld = false;
            return;
        }
        Input input = pilot.getLastClientInput();
        throttle = input.forward() ? 1 : input.backward() ? -1 : 0;
        strafe = input.right() ? 1 : input.left() ? -1 : 0;
        lift = input.jump() ? 1 : input.sprint() ? -1 : 0;
        setYRot(pilot.getYRot());
    }

    /**
     * Run dry, it draws on what its pilot carries: a cell's worth ({@link PhlogistonCellItem#CHARGES}) from their Phlogiston Cells, or
     * failing those a phlogiston shard. Whether it drew any.
     */
    public boolean refuel(Player pilot) {
        int want = Math.min(PhlogistonCellItem.CHARGES, MAX_CHARGE - charge());
        int carried = pilot.getAbilities().instabuild ? want : Math.min(want, PhlogistonCellItem.charges(pilot));
        boolean drew = false;
        if (carried > 0 && PhlogistonCellItem.draw(pilot, carried)) {
            setCharge(charge() + carried);
            drew = true;
        } else {
            var inventory = pilot.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize() && !drew; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.is(ModTags.Items.GEMS_PHLOGISTON) && charge() + PhlogistonCellItem.CHARGES <= MAX_CHARGE) {
                    stack.shrink(1);
                    setCharge(charge() + PhlogistonCellItem.CHARGES);
                    drew = true;
                }
            }
        }
        if (drew) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 0.8F, 1.6F);
            if (pilot instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable("message.hollowfrontier.saucer.refuelled").withStyle(ChatFormatting.AQUA));
            }
        }
        return drew;
    }

    /**
     * Whether it's down: on the ground, or resting just over it. (Set down from its item it sits exactly on the ground and
     * hovering never moves it, so onGround alone would leave a parked saucer "aloft", burning charge and refusing to fold.)
     */
    public boolean landed() {
        return onGround() || !level().noCollision(this, getBoundingBox().move(0, -0.06, 0));
    }

    private void fly(ServerLevel level) {
        boolean powered = charge() > 0;
        Vec3 v = getDeltaMovement();
        Vec3 heading = Vec3.directionFromRotation(0, getYRot());
        Vec3 right = new Vec3(-heading.z, 0, heading.x);
        Vec3 want = Vec3.ZERO;
        if (powered && (throttle != 0 || strafe != 0)) {
            want = heading.scale(throttle).add(right.scale(strafe)).normalize().scale(MAX_SPEED * (throttle < 0 ? 0.6 : 1.0));
        }
        Vec3 horizontal = v.horizontal().lerp(want, powered ? 0.12 : 0.04);
        double targetY;
        if (!powered) {
            targetY = landed() ? 0 : -SINK_SPEED;
        } else if (lift > 0 && getY() < level.getMaxY() - 8) {
            targetY = CLIMB_SPEED;
        } else if (lift < 0) {
            targetY = -CLIMB_SPEED;
        } else {
            targetY = landed() ? -0.05 : 0;
        }
        // Climbs and dives come on gently, but it brakes harder, so it stops near where the keys were let go
        double vy = Mth.lerp(Math.abs(targetY) < Math.abs(v.y) ? 0.4 : 0.2, v.y, targetY);
        setDeltaMovement(horizontal.x, vy, horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
        if (powered && !landed() && ++aloftTicks >= HOVER_TICKS) {
            aloftTicks = 0;
            setCharge(charge() - 1);
        }
        if (tickCount % 40 == 0 && powered && !landed()) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.SAUCER_HUM.get(), SoundSource.NEUTRAL, HUM_VOLUME, 1.0F);
        }
    }

    // ---------------------------------------------------------------- the heat laser

    private void laser(ServerLevel level, @Nullable ServerPlayer pilot) {
        boolean firing = pilot != null && laserHeld && fittings.lens() && !overheated && charge() > 0;
        if (firing) {
            if (laserTicks++ % LASER_INTERVAL == 0) {
                boolean fresh = sight != null && tickCount - sightTick <= SIGHT_TICKS;
                bolt(level, pilot, fresh ? sight.subtract(position().add(0, -0.3, 0)) : aim(level, pilot));
            }
        } else {
            laserTicks = 0;
            if (heat() > 0) {
                entityData.set(DATA_HEAT, heat() - 1);
            }
            if (overheated && heat() <= RESUME_HEAT) {
                overheated = false;
            }
        }
    }

    /**
     * Which way a bolt goes to land where the pilot is looking, when their client hasn't reported a sight ({@link #sight}):
     * whatever their eye's line first meets within {@link #LASER_RANGE}, a creature or a block, aimed at from the underside.
     * (In first person that's the screen's middle.) Firing straight along their look instead put every bolt a couple of
     * blocks under the crosshair (playtest).
     */
    public Vec3 aim(ServerLevel level, Player pilot) {
        Vec3 eye = pilot.getEyePosition();
        Vec3 far = eye.add(pilot.getViewVector(1.0F).scale(LASER_RANGE));
        BlockHitResult block = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pilot));
        Vec3 point = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, pilot, eye, point, new AABB(eye, point).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != this && !hasPassenger(e), 0.3F);
        if (hit != null) {
            point = hit.getLocation();
        }
        return point.subtract(position().add(0, -0.3, 0));
    }

    /** One bolt of the heat laser along {@code look} from the underside; what it struck, if a creature. */
    public @Nullable LivingEntity bolt(ServerLevel level, @Nullable Player pilot, Vec3 look) {
        Vec3 from = position().add(0, -0.3, 0);
        Vec3 to = from.add(look.normalize().scale(LASER_RANGE));
        BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, this, from, end, getBoundingBox().expandTowards(end.subtract(from)).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && !hasPassenger(e), 0.3F);
        LivingEntity struck = null;
        if (hit != null) {
            struck = (LivingEntity) hit.getEntity();
            end = hit.getLocation();
            DamageSource source = pilot != null ? ModDamageTypes.phlogistonBeam(pilot) : ModDamageTypes.phlogistonBeam(this);
            if (struck.hurtServer(level, source, LASER_DAMAGE)) {
                struck.igniteForSeconds(4.0F);
            }
        } else if (block.getType() != HitResult.Type.MISS) {
            scorch(level, block.getBlockPos(), block);
        }
        PhlogistonBeam shot = ModEntities.PHLOGISTON_BEAM.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        shot.aim(from, end, LASER_COLOUR);
        level.addFreshEntity(shot);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.PHLOGISTON_RIFLE_FIRE.get(), SoundSource.NEUTRAL, 1.0F, 1.5F);
        entityData.set(DATA_HEAT, Math.min(MAX_HEAT, heat() + LASER_HEAT));
        if (heat() >= MAX_HEAT) {
            overheated = true;
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 0.8F);
            if (pilot != null) {
                pilot.sendOverlayMessage(Component.translatable("message.hollowfrontier.saucer.overheated").withStyle(ChatFormatting.RED));
            }
        }
        if (++shots >= SHOTS_PER_CHARGE) {
            shots = 0;
            setCharge(charge() - 1);
        }
        return struck;
    }

    /** Where a bolt meets the ground: grass burned to bare earth, and a little fire on top if it'll take. Never in a town. */
    public static boolean scorch(ServerLevel level, BlockPos pos, BlockHitResult hit) {
        if (TownSite.townAt(level, pos).isPresent()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
            level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
        }
        BlockPos above = pos.relative(hit.getDirection());
        if (level.getBlockState(above).isAir() && BaseFireBlock.canBePlacedAt(level, above, hit.getDirection())) {
            level.setBlockAndUpdate(above, BaseFireBlock.getState(level, above));
        }
        return true;
    }

    // ---------------------------------------------------------------- the tractor beam

    /** The column under it that the beam fills. */
    public AABB column() {
        return new AABB(getX() - BEAM_RADIUS, getY() - BEAM_DEPTH, getZ() - BEAM_RADIUS, getX() + BEAM_RADIUS, getY(), getZ() + BEAM_RADIUS);
    }

    private void beam(ServerLevel level, @Nullable ServerPlayer pilot) {
        boolean on = beamHeld && fittings.emitter() && charge() > 0 && pilot != null;
        entityData.set(DATA_BEAMING, on);
        if (!on) {
            releaseAll();
            return;
        }
        if (++beamTicks >= BEAM_TICKS_PER_CHARGE) {
            beamTicks = 0;
            setCharge(charge() - 1);
        }
        for (int i = 0; i < 8; i++) {
            double y = getY() - level.getRandom().nextDouble() * BEAM_DEPTH;
            double a = level.getRandom().nextDouble() * Math.PI * 2, r = level.getRandom().nextDouble() * BEAM_RADIUS * 0.8;
            level.sendParticles(BEAM_LIGHT, getX() + Math.cos(a) * r, y, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        Set<UUID> inBeam = new HashSet<>();
        for (Entity e : level.getEntities(this, column(), e -> e.isAlive() && beamable(level, e))) {
            inBeam.add(e.getUUID());
            if (carried.add(e.getUUID())) {
                ModCriteria.fire(pilot, ModCriteria.SAUCER_BEAMED);
            }
            if (e instanceof ServerPlayer player && player.getLastClientInput().jump()
                    && struggles.merge(player.getUUID(), 1, Integer::sum) >= STRUGGLE) {
                struggles.remove(player.getUUID());
                freed.put(player.getUUID(), level.getGameTime() + FREED_TICKS);
                letDown(player);
                carried.remove(player.getUUID());
                inBeam.remove(player.getUUID());
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.saucer.struggled_free").withStyle(ChatFormatting.GRAY));
                continue;
            }
            draw(e);
        }
        for (UUID id : Set.copyOf(carried)) {
            if (!inBeam.contains(id)) {
                Entity e = level.getEntity(id);
                if (e != null) {
                    letDown(e);
                }
                carried.remove(id);
                struggles.remove(id);
            }
        }
    }

    /** Whether the beam can take hold of this: a creature, an item, or a player if the gamerule allows (not its own crew). */
    public boolean beamable(ServerLevel level, Entity e) {
        if (hasPassenger(e) || e.isPassenger() || e instanceof PrototypeSaucer) {
            return false;
        }
        if (e instanceof Player player) {
            Long until = freed.get(player.getUUID());
            return level.getGameRules().get(ModGameRules.SAUCER_BEAMS_PLAYERS.get()) && !player.isCreative() && !player.isSpectator()
                    && (until == null || level.getGameTime() >= until);
        }
        return e instanceof LivingEntity || e instanceof ItemEntity;
    }

    /** Up toward the craft's underside and along with it. */
    private void draw(Entity e) {
        Vec3 hold = new Vec3(getX(), getY() - 2.0 - e.getBbHeight(), getZ());
        Vec3 pull = hold.subtract(e.position()).scale(0.25);
        if (pull.length() > 0.8) {
            pull = pull.normalize().scale(0.8);
        }
        e.setDeltaMovement(pull.add(getDeltaMovement().multiply(1, 0, 1)));
        e.resetFallDistance();
        e.needsSync = true;
        if (e instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private void releaseAll() {
        if (carried.isEmpty() || !(level() instanceof ServerLevel level)) {
            return;
        }
        for (UUID id : carried) {
            Entity e = level.getEntity(id);
            if (e != null) {
                letDown(e);
            }
        }
        carried.clear();
        struggles.clear();
    }

    private static void letDown(Entity e) {
        if (e instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, true, false, true));
        }
        e.resetFallDistance();
    }

    // ---------------------------------------------------------------- the pilot's gauge

    private void report(ServerLevel level, @Nullable ServerPlayer pilot) {
        if (pilot == null || tickCount % 10 != 0) {
            return;
        }
        if (fittings.nav() && (gateway == null && level.getGameTime() - gatewaySearched > 1200 || level.getGameTime() - gatewaySearched > 6000)) {
            gatewaySearched = level.getGameTime();
            var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ModStructures.POLAR_GATEWAY);
            gateway = holder.map(h -> level.findNearestMapStructure(HolderSet.direct(h), blockPosition(), 100, false)).orElse(null);
        }
        pilot.sendOverlayMessage(gauge());
        if (tickCount % 20 == 0 && !landed()) {
            ModCriteria.fire(pilot, ModCriteria.SAUCER_LAUNCHED);
            ModCriteria.fire(pilot, ModCriteria.SAUCER_FLOWN, Mth.floor(getY()));
            if (fittings.full()) {
                ModCriteria.fire(pilot, ModCriteria.SAUCER_FULLY_FITTED);
            }
        }
    }

    public Component gauge() {
        // Dry, that's all the pilot needs to know; landed, how to get up
        if (charge() == 0) {
            return Component.translatable("message.hollowfrontier.saucer.no_phlogiston").withStyle(ChatFormatting.RED);
        }
        int bars = (int) Math.ceil(10.0 * charge() / MAX_CHARGE);
        Component line = Component.translatable("message.hollowfrontier.saucer.gauge", "▮".repeat(bars) + "▯".repeat(10 - bars), charge(),
                Mth.floor(getY()));
        var out = Component.empty().append(line);
        if (landed()) {
            out.append(Component.translatable("message.hollowfrontier.saucer.lift_off").withStyle(ChatFormatting.GREEN));
        }
        if (fittings.lens()) {
            out.append(Component.translatable(overheated ? "message.hollowfrontier.saucer.heat_over" : "message.hollowfrontier.saucer.heat", heat()));
        }
        if (fittings.nav()) {
            out.append(bearing("gateway", gateway));
            out.append(bearing("home", home));
        }
        if (isCloaked()) {
            out.append(Component.translatable("message.hollowfrontier.saucer.cloaked").withStyle(ChatFormatting.DARK_AQUA));
        }
        return out;
    }

    private Component bearing(String what, @Nullable BlockPos to) {
        if (to == null) {
            return Component.translatable("message.hollowfrontier.saucer.bearing_none", Component.translatable("message.hollowfrontier.saucer." + what));
        }
        double dx = to.getX() + 0.5 - getX(), dz = to.getZ() + 0.5 - getZ();
        String[] points = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int point = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(dx, -dz)) / 45.0), 8);
        return Component.translatable("message.hollowfrontier.saucer.bearing", Component.translatable("message.hollowfrontier.saucer." + what),
                String.format(Locale.ROOT, "%d", Math.round(Math.hypot(dx, dz))), points[point]);
    }

    // ---------------------------------------------------------------- crew, charge, fittings and folding

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        // Its crew sit inside its hit box, so their crosshair is always on it: their use goes to what they hold instead
        // (food, a gun, a cell), as it would anywhere else (playtest: no eating aboard)
        if (hasPassenger(player)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (SaucerFittings.isPart(held)) {
            if (level() instanceof ServerLevel level) {
                SaucerFittings fitted = fittings.fit(held);
                if (fitted == fittings) {
                    player.sendOverlayMessage(Component.translatable("message.hollowfrontier.saucer.already_fitted", held.getHoverName()).withStyle(ChatFormatting.GRAY));
                } else if (!mayHandle(player)) {
                    refuse(player);
                } else {
                    player.sendOverlayMessage(Component.translatable("message.hollowfrontier.saucer.fitted", held.getHoverName()).withStyle(ChatFormatting.AQUA));
                    setFittings(fitted);
                    held.consume(1, player);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.SMITHING_TABLE_USE, SoundSource.NEUTRAL, 1.0F, 1.2F);
                    if (fitted.full()) {
                        ModCriteria.fire(player, ModCriteria.SAUCER_FULLY_FITTED);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(ModItems.PHLOGISTON_CELL.get())) {
            if (!level().isClientSide()) {
                int cell = held.getMaxDamage() - held.getDamageValue();
                int poured = Math.min(cell, MAX_CHARGE - charge());
                if (poured > 0) {
                    setCharge(charge() + poured);
                    if (poured >= cell) {
                        player.setItemInHand(hand, new ItemStack(ModItems.EMPTY_PHLOGISTON_CELL.get()));
                    } else {
                        held.setDamageValue(held.getDamageValue() + poured);
                    }
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 0.8F, 1.6F);
                }
                player.sendOverlayMessage(gauge());
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(ModTags.Items.GEMS_PHLOGISTON)) {
            if (!level().isClientSide() && charge() + PhlogistonCellItem.CHARGES <= MAX_CHARGE) {
                setCharge(charge() + PhlogistonCellItem.CHARGES);
                held.consume(1, player);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 0.8F, 1.6F);
                player.sendOverlayMessage(gauge());
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() && held.isEmpty()) {
            if (level() instanceof ServerLevel) {
                String refused = fold(player);
                if (refused != null) {
                    player.sendOverlayMessage(Component.translatable("message.hollowfrontier.saucer." + refused, ownerName).withStyle(ChatFormatting.RED));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Folds it back into its item, fittings, charge and cargo kept, into {@code player}'s hands or at their feet. It must
     * be landed with nobody aboard, and theirs ({@link #mayHandle}). Null if it folded, or why not ({@code fold_aloft},
     * {@code fold_crewed}, {@code not_yours}).
     */
    public @Nullable String fold(Player player) {
        if (!landed()) {
            return "fold_aloft";
        }
        if (isVehicle()) {
            return "fold_crewed";
        }
        if (!mayHandle(player)) {
            return "not_yours";
        }
        ItemStack item = PrototypeSaucerItem.of(fittings, charge());
        if (!cargo.isEmpty()) {
            item.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(cargo.getItems()));
        }
        cargo.clearContent(); // it's in the item now, and mustn't spill as the craft goes
        if (!player.getInventory().add(item)) {
            player.drop(item, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        releaseAll();
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 1.0F, 1.2F);
        discard();
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    // Pilot forward under the dome, the passenger behind, sat low in the hull so their heads clear the glass
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double ahead = getPassengers().indexOf(passenger) == 0 ? 0.3 : -0.4;
        return new Vec3(0.0, 0.6, ahead).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
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
        return ModItems.PROTOTYPE_SAUCER.get();
    }

    @Override
    public ItemStack getPickResult() {
        return PrototypeSaucerItem.of(fittings, charge());
    }

    /** Destroyed (by /kill, the only thing that can), it spills its hold. */
    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.KILLED && level() instanceof ServerLevel level) {
            Containers.dropContents(level, this, cargo);
        }
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("Fittings", SaucerFittings.CODEC, fittings);
        output.putInt("Charge", charge());
        output.putInt("Heat", heat());
        output.storeNullable("Home", BlockPos.CODEC, home);
        output.storeNullable("Owner", UUIDUtil.CODEC, owner);
        output.putString("OwnerName", ownerName);
        if (!cargo.isEmpty()) {
            output.store("Cargo", ItemContainerContents.CODEC, ItemContainerContents.fromItems(cargo.getItems()));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setFittings(input.read("Fittings", SaucerFittings.CODEC).orElse(fittings));
        setCharge(input.getIntOr("Charge", 0));
        entityData.set(DATA_HEAT, input.getIntOr("Heat", 0));
        home = input.read("Home", BlockPos.CODEC).orElse(null);
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("OwnerName", "");
        stow(input.read("Cargo", ItemContainerContents.CODEC).orElse(ItemContainerContents.EMPTY));
    }

    // ---------------------------------------------------------------- animation

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PrototypeSaucer>("main", 5,
                state -> state.setAndContinue(state.animatable().isBeaming() ? BEAMING : HOVER)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0 * 256.0;
    }
}
