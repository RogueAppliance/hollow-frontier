package com.hollowfrontier.content.ufo;

import java.util.HashSet;
import java.util.List;
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
import com.hollowfrontier.content.concavia.KindledStanding;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Mothership (13d): the Grays' great craft, the thing that holds their leash on the men they've implanted. A Phlogiston
 * Beacon calls it down out of the night ({@link #summon}); it comes to hang {@link #HOVER} over the beacon, circling it
 * slowly, a boss bar for everyone within {@link #BOSS_RANGE}, and fights in three phases:
 * <ol>
 * <li><b>Drones</b> (full health to two thirds): every so often it launches Gray Drones ({@link GrayDrone}, up to
 *     {@link #MAX_DRONES} at once), and calls its <b>death ray</b> down on one of its foes: a red ring on the ground where
 *     they stood and a thin red line to it for {@link #RAY_CHARGE} ticks (time to get clear), then a column of red fire,
 *     {@link #RAY_DAMAGE} and burning to anything within {@link #RAY_RADIUS}.</li>
 * <li><b>Abduction</b> (two thirds to one third): at the turn it raises its shield and launches a wave of
 *     {@link #SHIELD_WAVE} drones; nothing harms it till those four are down (no other waves come meanwhile, and drones
 *     already up don't hold it). Then, besides the drones and the ray, every so often it swoops over a foe and lets down its <b>beam</b>: anyone in the column who isn't sneaking (or riding) is drawn up
 *     toward the hull for {@link #BEAM_TICKS} ticks and dropped from the height when it lets go.</li>
 * <li><b>Exposed</b> (the last third): shield and wave again, then it comes down to {@link #LOW_HOVER}, its belly opened
 *     on its glowing core, faster with everything and harmed {@link #EXPOSED} times as hard.</li>
 * </ol>
 * It's fought from the ground with bows and phlogiston weapons, or in the air from a Prototype Saucer (its laser, and the
 * drones make it a dogfight). If no one is left to fight within {@link #BOSS_RANGE} for {@link #RESET_TICKS}, it rises
 * and goes (the beacon can call it again). Falling, it burns for {@link #DEATH_TICKS} ticks, sinking, and breaks apart;
 * its loot table gives salvage, and the first one to fall in a world breaks the leash ({@link Leash#breakIt}). The
 * Kindled count everyone who fought it a step nearer their alliance.
 * <p>
 * <b>Rematches</b> (15b): each one called after the first comes a step harder ({@link Leash#rematch}, taken when it's
 * called): {@link #REMATCH_HEALTH} more health, a drone more in every wave (the shield's too) and room for one more up,
 * and a death ray {@link #REMATCH_RAY} sooner each time (its warning stays as long). A rematch also gives up a saucer
 * fitting: the Cloaking Device and the Cargo Hold by turns, starting with the cloak, so two rematches give both.
 */
public class Mothership extends Monster implements GeoEntity {
    public static final double BOSS_RANGE = 80.0;
    public static final double HOVER = 22.0, LOW_HOVER = 14.0, ORBIT = 10.0, ARRIVE_FROM = 70.0;
    public static final int RESET_TICKS = 400;
    public static final float PHASE_TWO = 2.0F / 3.0F, PHASE_THREE = 1.0F / 3.0F;
    public static final float EXPOSED = 1.5F;
    public static final int[] DRONE_INTERVAL = {0, 240, 200, 160};
    public static final int[] DRONE_WAVE = {0, 2, 2, 3};
    public static final int MAX_DRONES = 6, SHIELD_WAVE = 4, SHIELD_FAILSAFE = 1200;
    public static final int[] RAY_INTERVAL = {0, 120, 90, 60};
    public static final int RAY_CHARGE = 40;
    public static final float RAY_DAMAGE = 14.0F;
    public static final double RAY_RADIUS = 2.5;
    public static final int BEAM_INTERVAL = 260, BEAM_TICKS = 60;
    public static final double BEAM_RADIUS = 3.0, BEAM_LIFT = 0.32;
    public static final int DEATH_TICKS = 100;
    public static final int KINDLED_THANKS = 6;
    public static final double REMATCH_HEALTH = 0.15; // at the cap, 960: vanilla holds max health to 1024
    public static final double REMATCH_RAY = 0.125;
    private static final Identifier REMATCH_ID = HollowFrontier.id("rematch");
    public static final int ARRIVING = 0, DRONES = 1, ABDUCTION = 2, EXPOSED_CORE = 3, LEAVING = 4;
    static final int RAY_COLOUR = 0xFF2A20, BEAM_COLOUR = 0xC8B0FF;
    private static final DustParticleOptions RING = new DustParticleOptions(0xFF3020, 1.4F);
    private static final DustParticleOptions BEAM_LIGHT = new DustParticleOptions(0xD8B8FF, 1.2F);

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(Mothership.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SHIELDED = SynchedEntityData.defineId(Mothership.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BEAMING = SynchedEntityData.defineId(Mothership.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation BEAM = RawAnimation.begin().thenLoop("beam");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(getUUID(), getDisplayName(), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_6);
    private BlockPos anchor = BlockPos.ZERO;
    private final Set<UUID> drones = new HashSet<>();
    private final Set<UUID> shieldWave = new HashSet<>();
    private int droneCooldown = 60, shieldTicks;
    private int rayCooldown = 80, rayCharge;
    private @Nullable Vec3 raySpot;
    private int beamCooldown = 120, beamTicks;
    private @Nullable UUID beamTarget;
    private int alone, leavingTicks;
    private double orbit;
    private boolean endsTheLeash = true;
    // How many had fallen in this world when it was called (0: the first)
    private int encore;

    public Mothership(EntityType<? extends Mothership> type, Level level) {
        super(type, level);
        xpReward = 1000;
        bossEvent.setPlayBossMusic(true); // 45c: how the client knows to play its theme (client/BossMusic)
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, BOSS_RANGE);
    }

    /** Calls one down from {@link #ARRIVE_FROM} over the beacon at {@code beacon}. */
    public static Mothership summon(ServerLevel level, BlockPos beacon) {
        Mothership ship = ModEntities.MOTHERSHIP.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        ship.anchor = beacon.immutable();
        ship.snapTo(beacon.getX() + 0.5, beacon.getY() + ARRIVE_FROM, beacon.getZ() + 0.5, 0, 0);
        ship.setEncore(Leash.fallen(level));
        level.addFreshEntity(ship);
        level.playSound(null, ship.getX(), ship.getY(), ship.getZ(), ModSounds.MOTHERSHIP_HUM.get(), SoundSource.HOSTILE, 6.0F, 0.7F);
        return ship;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, ARRIVING);
        builder.define(DATA_SHIELDED, false);
        builder.define(DATA_BEAMING, false);
    }

    public int phase() {
        return entityData.get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        entityData.set(DATA_PHASE, phase);
    }

    public boolean isShielded() {
        return entityData.get(DATA_SHIELDED);
    }

    public boolean isBeaming() {
        return entityData.get(DATA_BEAMING);
    }

    public boolean isExposed() {
        return phase() == EXPOSED_CORE;
    }

    public BlockPos anchor() {
        return anchor;
    }

    public ServerBossEvent bossEvent() {
        return bossEvent;
    }

    /** How many Motherships had fallen in this world when this one was called: above 0, it's a rematch. */
    public int encore() {
        return encore;
    }

    /** How much harder than the first it comes, 0 to {@link Leash#MAX_REMATCH}. */
    public int rematch() {
        return Math.min(encore, Leash.MAX_REMATCH);
    }

    /** Sets it as a rematch after {@code fallen} Motherships, with the health that comes with it (at full). */
    public void setEncore(int fallen) {
        encore = Math.max(0, fallen);
        var health = getAttribute(Attributes.MAX_HEALTH);
        health.removeModifier(REMATCH_ID);
        if (rematch() > 0) {
            health.addPermanentModifier(new AttributeModifier(REMATCH_ID, REMATCH_HEALTH * rematch(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        setHealth(getMaxHealth());
    }

    /** How many drones an ordinary wave of {@code phase} launches. */
    public int droneWave(int phase) {
        return DRONE_WAVE[phase] + rematch();
    }

    /** How many drones its shield waits behind. */
    public int shieldWaveSize() {
        return SHIELD_WAVE + rematch();
    }

    /** How many drones it keeps up at most. */
    public int maxDrones() {
        return MAX_DRONES + rematch();
    }

    /** Ticks between one death ray and the next one in {@code phase}. */
    public int rayInterval(int phase) {
        return (int) Math.round(RAY_INTERVAL[phase] * (1.0 - REMATCH_RAY * rematch()));
    }

    @Override
    public Component getDisplayName() {
        Component name = super.getDisplayName();
        return encore > 0 ? Component.translatable("entity.hollowfrontier.mothership.rematch", name, encore) : name;
    }

    /** How many of its drones are still up. */
    public int dronesUp(ServerLevel level) {
        drones.removeIf(id -> !(level.getEntity(id) instanceof GrayDrone drone) || !drone.isAlive());
        shieldWave.retainAll(drones);
        return drones.size();
    }

    /** The drones of the wave its shield holds behind (the shield falls with the last of them, whatever else is up). */
    public Set<UUID> shieldWave() {
        return Set.copyOf(shieldWave);
    }

    // ---------------------------------------------------------------- the fight

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setName(getDisplayName());
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 20 == 0) {
            updateBossBarPlayers(level);
        }
        if (tickCount % 40 == 0) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.MOTHERSHIP_HUM.get(), SoundSource.HOSTILE, 4.0F, 1.0F);
        }
        switch (phase()) {
            case ARRIVING -> arrive(level);
            case LEAVING -> leave(level);
            default -> fight(level);
        }
    }

    private void arrive(ServerLevel level) {
        double goal = anchor.getY() + HOVER;
        if (getY() - goal < 0.5) {
            setPhase(DRONES);
            announce(level, encore > 0 ? "message.hollowfrontier.mothership.returns" : "message.hollowfrontier.mothership.arrives", ChatFormatting.DARK_PURPLE);
            return;
        }
        setDeltaMovement(0, -Math.min(0.5, (getY() - goal) * 0.05 + 0.08), 0);
    }

    private void leave(ServerLevel level) {
        setDeltaMovement(0, 0.5, 0);
        if (++leavingTicks >= 100) {
            PhlogistonBeaconBlock.putOut(level, anchor);
            dropDrones(level);
            discard();
        }
    }

    /** Off it goes, up and away (no one left to fight): the beacon can call it again. */
    public void departs(ServerLevel level) {
        setPhase(LEAVING);
        entityData.set(DATA_BEAMING, false);
        announce(level, "message.hollowfrontier.mothership.leaves", ChatFormatting.GRAY);
        bossEvent.removeAllPlayers();
    }

    private void fight(ServerLevel level) {
        updatePhase(level);
        if (isShielded()) {
            shieldTicks++;
            dronesUp(level);
            if (shieldWave.isEmpty() || shieldTicks >= SHIELD_FAILSAFE) {
                entityData.set(DATA_SHIELDED, false);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 4.0F, 0.6F);
                announce(level, "message.hollowfrontier.mothership.shield_down", ChatFormatting.LIGHT_PURPLE);
            } else if (tickCount % 4 == 0) {
                double a = level.getRandom().nextDouble() * Math.PI * 2;
                level.sendParticles(ParticleTypes.WITCH, getX() + Math.cos(a) * 6.5, getY() + 0.8, getZ() + Math.sin(a) * 6.5, 3, 0.2, 0.3, 0.2, 0);
            }
        }
        List<Player> foes = foes(level);
        if (foes.isEmpty()) {
            if (++alone >= RESET_TICKS) {
                departs(level);
            }
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        alone = 0;
        int phase = phase();
        // No new waves while the shield's up: bring down the wave that holds it, and it falls (playtest: with waves still
        // coming, it never fell to the drones' loss, only to the failsafe)
        if (!isShielded() && --droneCooldown <= 0) {
            droneCooldown = DRONE_INTERVAL[phase];
            launch(level, Math.min(droneWave(phase), maxDrones() - dronesUp(level)));
        }
        deathRay(level, foes);
        if (phase >= ABDUCTION) {
            abduction(level, foes);
        }
        drift(level);
    }

    private List<Player> foes(ServerLevel level) {
        return level.getEntitiesOfClass(Player.class, new AABB(anchor).inflate(BOSS_RANGE).expandTowards(0, HOVER, 0),
                p -> p.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p));
    }

    private void updatePhase(ServerLevel level) {
        float health = getHealth() / getMaxHealth();
        if (phase() == DRONES && health <= PHASE_TWO) {
            enter(level, ABDUCTION, "message.hollowfrontier.mothership.abduction");
        } else if (phase() == ABDUCTION && health <= PHASE_THREE) {
            enter(level, EXPOSED_CORE, "message.hollowfrontier.mothership.exposed");
        }
    }

    /** Into the next phase: shield up behind a wave of drones. */
    private void enter(ServerLevel level, int phase, String message) {
        setPhase(phase);
        entityData.set(DATA_SHIELDED, true);
        shieldTicks = 0;
        shieldWave.clear();
        shieldWave.addAll(launch(level, shieldWaveSize()));
        droneCooldown = DRONE_INTERVAL[phase];
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4.0F, 0.5F);
        announce(level, message, ChatFormatting.LIGHT_PURPLE);
    }

    private Set<UUID> launch(ServerLevel level, int count) {
        Set<UUID> launched = new HashSet<>();
        for (int i = 0; i < count; i++) {
            launched.add(GrayDrone.launch(level, this).getUUID());
        }
        drones.addAll(launched);
        if (count > 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.DISPENSER_LAUNCH, SoundSource.HOSTILE, 3.0F, 0.6F);
        }
        return launched;
    }

    /** For tests: a drone of an ordinary wave (not the shield's). */
    public UUID launchDrone(ServerLevel level) {
        return launch(level, 1).iterator().next();
    }

    /** Round the beacon, slowly; over its quarry while it beams; low over it once its core's exposed. */
    private void drift(ServerLevel level) {
        double height = anchor.getY() + (isExposed() ? LOW_HOVER : HOVER);
        Vec3 goal;
        Entity quarry = beamTarget != null ? level.getEntity(beamTarget) : null;
        if (quarry != null) {
            goal = new Vec3(quarry.getX(), Math.max(height, quarry.getY() + LOW_HOVER), quarry.getZ());
        } else {
            orbit += isExposed() ? 0.01 : 0.006;
            goal = new Vec3(anchor.getX() + 0.5 + Math.cos(orbit) * ORBIT, height, anchor.getZ() + 0.5 + Math.sin(orbit) * ORBIT);
        }
        Vec3 to = goal.subtract(position());
        double speed = quarry != null ? 0.35 : 0.2;
        setDeltaMovement(to.length() < 0.05 ? Vec3.ZERO : to.scale(0.06).normalize().scale(Math.min(speed, to.length() * 0.06)));
        setYRot(getYRot() + 0.4F);
    }

    // ---------------------------------------------------------------- the death ray

    private void deathRay(ServerLevel level, List<Player> foes) {
        if (raySpot == null) {
            if (--rayCooldown <= 0) {
                Player target = foes.get(level.getRandom().nextInt(foes.size()));
                startRay(ground(level, target.position()));
            }
            return;
        }
        rayCharge++;
        if (rayCharge % 4 == 1) {
            PhlogistonBeam line = ModEntities.PHLOGISTON_BEAM.get().create(level, EntitySpawnReason.EVENT);
            line.aim(underside(), raySpot, RAY_COLOUR, 0.6F, 6);
            level.addFreshEntity(line);
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24;
                level.sendParticles(RING, raySpot.x + Math.cos(a) * RAY_RADIUS, raySpot.y + 0.1, raySpot.z + Math.sin(a) * RAY_RADIUS, 1, 0, 0, 0, 0);
            }
        }
        if (rayCharge >= RAY_CHARGE) {
            fireRay(level);
        }
    }

    /** Marks {@code spot} for the death ray, which falls on it {@link #RAY_CHARGE} ticks on. */
    public void startRay(Vec3 spot) {
        raySpot = spot;
        rayCharge = 0;
        playSound(ModSounds.DEATH_RAY_CHARGE.get(), 5.0F, 1.0F);
    }

    private void fireRay(ServerLevel level) {
        Vec3 spot = raySpot;
        raySpot = null;
        rayCharge = 0;
        rayCooldown = rayInterval(Math.max(DRONES, Math.min(EXPOSED_CORE, phase())));
        PhlogistonBeam ray = ModEntities.PHLOGISTON_BEAM.get().create(level, EntitySpawnReason.EVENT);
        ray.aim(underside(), spot, RAY_COLOUR, 20.0F, 16); // as wide as the spot it burns (narrower, it read as a thread from the ground)
        level.addFreshEntity(ray);
        level.playSound(null, spot.x, spot.y, spot.z, ModSounds.DEATH_RAY_FIRE.get(), SoundSource.HOSTILE, 6.0F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y + 0.5, spot.z, 3, 1.0, 0.3, 1.0, 0);
        level.sendParticles(ParticleTypes.FLAME, spot.x, spot.y + 0.2, spot.z, 40, RAY_RADIUS * 0.6, 0.2, RAY_RADIUS * 0.6, 0.05);
        AABB column = new AABB(spot, spot).inflate(RAY_RADIUS, 0, RAY_RADIUS).expandTowards(0, 4, 0).move(0, -1, 0);
        for (LivingEntity struck : level.getEntitiesOfClass(LivingEntity.class, column, e -> e.isAlive() && !(e instanceof GrayDrone) && e != this
                && e.position().subtract(spot).horizontalDistance() <= RAY_RADIUS)) {
            if (struck.hurtServer(level, ModDamageTypes.phlogistonBeam(this), RAY_DAMAGE)) {
                struck.igniteForSeconds(3.0F);
            }
        }
        BlockPos ground = BlockPos.containing(spot.x, spot.y - 0.5, spot.z);
        PrototypeSaucer.scorch(level, ground, new BlockHitResult(spot, net.minecraft.core.Direction.UP, ground, false));
    }

    private Vec3 underside() {
        return position().add(0, -0.5, 0);
    }

    /** The ground under {@code at} (or {@code at}, if there's none within 16). */
    private Vec3 ground(ServerLevel level, Vec3 at) {
        BlockHitResult hit = level.clip(new ClipContext(at.add(0, 0.5, 0), at.add(0, -16, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        return hit.getType() == HitResult.Type.MISS ? at : hit.getLocation();
    }

    // ---------------------------------------------------------------- the abduction beam

    private void abduction(ServerLevel level, List<Player> foes) {
        if (beamTarget == null) {
            if (--beamCooldown <= 0 && !isShielded()) {
                List<Player> takeable = foes.stream().filter(p -> !p.isPassenger()).toList();
                if (!takeable.isEmpty()) {
                    startBeam(takeable.get(level.getRandom().nextInt(takeable.size())));
                }
            }
            return;
        }
        Entity target = level.getEntity(beamTarget);
        if (!(target instanceof Player player) || !player.isAlive()) {
            endBeam();
            return;
        }
        // Over its quarry first; then the light comes down
        if (!isBeaming()) {
            if (position().subtract(player.position()).horizontalDistance() < BEAM_RADIUS) {
                entityData.set(DATA_BEAMING, true);
                beamTicks = 0;
                playSound(SoundEvents.BEACON_ACTIVATE, 5.0F, 0.5F);
            } else if (++beamTicks > 200) {
                endBeam(); // it couldn't get over them
            }
            return;
        }
        // A column of pale light, as wide as the beam's hold, laid fresh before the last fades (motes alone read as nothing)
        if (beamTicks % 6 == 0) {
            PhlogistonBeam light = ModEntities.PHLOGISTON_BEAM.get().create(level, EntitySpawnReason.EVENT);
            light.aim(underside(), ground(level, position().subtract(0, HOVER - 2, 0)), BEAM_COLOUR, (float) BEAM_RADIUS / PhlogistonBeam.GLOW_WIDTH, 12);
            light.soften();
            level.addFreshEntity(light);
        }
        for (int i = 0; i < 10; i++) {
            double y = getY() - level.getRandom().nextDouble() * (HOVER + 4);
            double a = level.getRandom().nextDouble() * Math.PI * 2, r = level.getRandom().nextDouble() * BEAM_RADIUS * 0.8;
            level.sendParticles(BEAM_LIGHT, getX() + Math.cos(a) * r, y, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        AABB column = new AABB(getX() - BEAM_RADIUS, getY() - HOVER - 8, getZ() - BEAM_RADIUS, getX() + BEAM_RADIUS, getY() - 2, getZ() + BEAM_RADIUS);
        for (Player taken : level.getEntitiesOfClass(Player.class, column, p -> p.isAlive() && !p.isShiftKeyDown() && !p.isPassenger()
                && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p))) {
            lift(taken);
        }
        if (++beamTicks >= BEAM_TICKS) {
            endBeam();
        }
    }

    /** Swoops over {@code player} and lets its beam down on them. */
    public void startBeam(Player player) {
        beamTarget = player.getUUID();
        beamTicks = 0;
        entityData.set(DATA_BEAMING, false);
    }

    private void endBeam() {
        if (isBeaming()) {
            playSound(SoundEvents.BEACON_DEACTIVATE, 5.0F, 0.5F);
        }
        beamTarget = null;
        beamTicks = 0;
        beamCooldown = BEAM_INTERVAL;
        entityData.set(DATA_BEAMING, false);
    }

    /** Up toward the hull and along under it (held there once close); dropped when the beam lets go. */
    private void lift(Player player) {
        double below = getY() - player.getY();
        Vec3 centre = new Vec3(getX() - player.getX(), 0, getZ() - player.getZ()).scale(0.15);
        player.setDeltaMovement(centre.x, below > 4 ? BEAM_LIFT : 0.02, centre.z);
        player.needsSync = true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }

    // ---------------------------------------------------------------- harm and its fall

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }
        if (phase() == ARRIVING || phase() == LEAVING || source.getEntity() instanceof GrayDrone || source.getEntity() == this) {
            return false;
        }
        if (isShielded()) {
            playSound(SoundEvents.AMETHYST_BLOCK_HIT, 3.0F, 0.5F);
            level.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 12, 4, 0.5, 4, 0);
            return false;
        }
        return super.hurtServer(level, source, isExposed() ? damage * EXPOSED : damage);
    }

    @Override
    public void die(DamageSource source) {
        if (!dead && level() instanceof ServerLevel level) {
            announce(level, "message.hollowfrontier.mothership.falls", ChatFormatting.GOLD);
            for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(BOSS_RANGE))) {
                if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
                    KindledStanding.add(serverPlayer, KINDLED_THANKS);
                    if (encore > 0) {
                        ModCriteria.fire(serverPlayer, ModCriteria.MOTHERSHIP_REMATCH);
                    }
                }
            }
            dropDrones(level);
            PhlogistonBeaconBlock.putOut(level, anchor);
            // Counted before the leash breaks: a broken leash with no count reads as one fallen (worlds from before 15b)
            int number = endsTheLeash ? Leash.countFallen(level) : encore + 1;
            if (encore > 0) {
                spawnAtLocation(level, fitting(number));
            }
            if (endsTheLeash) {
                Leash.breakIt(level, position());
            }
        }
        entityData.set(DATA_BEAMING, false);
        bossEvent.removeAllPlayers();
        super.die(source);
    }

    /** The saucer fitting the {@code number}th Mothership to fall gives up (a rematch's): the cloak, then the hold, by turns. */
    public static ItemStack fitting(int number) {
        return new ItemStack(number % 2 == 0 ? ModItems.CLOAKING_DEVICE.get() : ModItems.CARGO_HOLD.get());
    }

    /** Its drones fall with it (or go with it). */
    private void dropDrones(ServerLevel level) {
        for (UUID id : drones) {
            if (level.getEntity(id) instanceof GrayDrone drone && drone.isAlive()) {
                level.sendParticles(ParticleTypes.SMOKE, drone.getX(), drone.getY(), drone.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                drone.discard();
            }
        }
        drones.clear();
        shieldWave.clear();
    }

    /** It burns and sinks for {@link #DEATH_TICKS} ticks, explosions bursting across the hull, then breaks apart. */
    @Override
    protected void tickDeath() {
        deathTime++;
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        setDeltaMovement(0, -0.06, 0);
        if (deathTime % 3 == 0) {
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * 5.5;
            level.sendParticles(ParticleTypes.EXPLOSION, getX() + Math.cos(a) * r, getY() + random.nextDouble() * 2, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX() + Math.cos(a) * r, getY() + 1, getZ() + Math.sin(a) * r, 4, 0.5, 0.3, 0.5, 0.02);
        }
        if (deathTime % 10 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.6F + random.nextFloat() * 0.3F);
        }
        if (deathTime >= DEATH_TICKS && !isRemoved()) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 3, 3, 0.5, 3, 0);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 8.0F, 0.5F);
            level.broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    private void announce(ServerLevel level, String key, ChatFormatting colour) {
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(BOSS_RANGE))) {
            player.sendSystemMessage(Component.translatable(key).withStyle(colour));
        }
    }

    private void updateBossBarPlayers(ServerLevel level) {
        boolean shown = phase() != LEAVING && isAlive();
        for (ServerPlayer player : level.players()) {
            boolean near = shown && player.distanceTo(this) <= BOSS_RANGE + HOVER;
            if (near && !bossEvent.getPlayers().contains(player)) {
                bossEvent.addPlayer(player);
            } else if (!near && bossEvent.getPlayers().contains(player)) {
                bossEvent.removePlayer(player);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 320.0 * 320.0;
    }

    // ---------------------------------------------------------------- for the tests

    /** Where it circles, as if a beacon there had called it. */
    public void setAnchor(BlockPos pos) {
        anchor = pos.immutable();
    }

    /** Skips the arrival: it's in the fight at once, where it is. */
    public void engage() {
        setPhase(DRONES);
    }

    /** Whether its fall breaks the leash and is counted (the tests keep the world's leash whole and its count as it was). */
    public void setEndsTheLeash(boolean ends) {
        endsTheLeash = ends;
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("Anchor", BlockPos.CODEC, anchor);
        output.putInt("Phase", phase());
        output.putBoolean("Shielded", isShielded());
        output.putInt("Encore", encore);
        output.store("Drones", UUIDUtil.CODEC.listOf(), List.copyOf(drones));
        output.store("ShieldWave", UUIDUtil.CODEC.listOf(), List.copyOf(shieldWave));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        anchor = input.read("Anchor", BlockPos.CODEC).orElse(blockPosition());
        setPhase(input.getIntOr("Phase", DRONES));
        entityData.set(DATA_SHIELDED, input.getBooleanOr("Shielded", false));
        encore = input.getIntOr("Encore", 0); // its health was saved with its attributes
        drones.clear();
        input.read("Drones", UUIDUtil.CODEC.listOf()).ifPresent(drones::addAll);
        shieldWave.clear();
        input.read("ShieldWave", UUIDUtil.CODEC.listOf()).ifPresent(shieldWave::addAll);
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    // ---------------------------------------------------------------- animation

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Mothership>("main", 5,
                state -> state.setAndContinue(state.animatable().isBeaming() ? BEAM : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
