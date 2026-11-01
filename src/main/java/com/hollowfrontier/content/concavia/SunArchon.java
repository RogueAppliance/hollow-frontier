package com.hollowfrontier.content.concavia;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModDamageTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Sun Archon (12b): the last king of Concavia, who went into the inner sun to hold it against the Grays and came out
 * as this. It sits on its throne atop the Sun Temple, its head bowed, until a player comes within {@link #WAKE_RANGE} or
 * strikes it; then it rises, a boss bar shows to everyone within {@link #BOSS_RANGE}, and it fights in three phases:
 * <ol>
 * <li><b>The King</b> (full health to two thirds): it walks its enemy down with its staff ({@link #MELEE}) and every
 *     {@link #BEAM_INTERVAL} ticks, if it can see them, draws a solar beam on them for {@link #BEAM_CHARGE} ticks and
 *     burns them ({@link #BEAM_DAMAGE}, and fire).</li>
 * <li><b>Corona</b> (two thirds to one third): it rises into the air over the arena and rains fire (three small fireballs
 *     every {@link #FLARE_INTERVAL} ticks), still beaming; while any of the temple's Sun Pylons stands, their light shields
 *     it and nothing harms it. Break them, and it comes down.</li>
 * <li><b>Eclipse</b> (the last third): it lands, faster and harder-hitting, and every {@link #BURST_INTERVAL} ticks gathers
 *     a sunburst for {@link #BURST_CHARGE} ticks and lets it go: {@link #BURST_DAMAGE}, fire and a hard shove to everyone
 *     within {@link #BURST_RANGE}.</li>
 * </ol>
 * If no one is left to fight within {@link #BOSS_RANGE} for {@link #RESET_TICKS}, it goes back to its throne, healed, and
 * the pylons are made whole again. It drops the Phlogiston Core, the Starsteel Crown and sunstone shards (its loot table), and
 * the Kindled count everyone who stood against it a step nearer their alliance ({@link #KINDLED_THANKS}).
 */
public class SunArchon extends Monster implements GeoEntity {
    public static final double WAKE_RANGE = 10.0;
    public static final double BOSS_RANGE = 48.0;
    public static final double PYLON_RANGE = 24.0;
    public static final int RESET_TICKS = 200;
    public static final float PHASE_TWO = 2.0F / 3.0F, PHASE_THREE = 1.0F / 3.0F;
    public static final float MELEE = 10.0F;
    public static final int BEAM_INTERVAL = 80, BEAM_CHARGE = 25;
    public static final float BEAM_DAMAGE = 9.0F;
    public static final int FLARE_INTERVAL = 50;
    public static final int BURST_INTERVAL = 100, BURST_CHARGE = 20;
    public static final float BURST_DAMAGE = 8.0F;
    public static final double BURST_RANGE = 8.0;
    public static final double HOVER = 5.0;
    public static final int KINDLED_THANKS = 6;
    public static final int DORMANT = 0, KING = 1, CORONA = 2, ECLIPSE = 3;

    private static final DustParticleOptions SUNBEAM = new DustParticleOptions(0xFFD040, 0.8F);
    private static final DustParticleOptions PYLON_LIGHT = new DustParticleOptions(0xFFF0A0, 0.6F);
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(SunArchon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SHIELDED = SynchedEntityData.defineId(SunArchon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CASTING = SynchedEntityData.defineId(SunArchon.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier ECLIPSE_SPEED = HollowFrontier.id("eclipse_speed");
    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("dormant");
    private static final RawAnimation CAST = RawAnimation.begin().thenLoop("cast");
    private static final RawAnimation HOVERING = RawAnimation.begin().thenLoop("hover");
    private static final RawAnimation SWING = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(getUUID(), getDisplayName(), BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_6);
    private @Nullable BlockPos throne;
    private final List<BlockPos> pylons = new ArrayList<>();
    private int beamCooldown = BEAM_INTERVAL, beamCharge;
    private int flareCooldown = FLARE_INTERVAL;
    private int burstCooldown = BURST_INTERVAL, burstCharge;
    private int alone;

    public SunArchon(EntityType<? extends SunArchon> type, Level level) {
        super(type, level);
        xpReward = 500;
        bossEvent.setVisible(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, MELEE)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, BOSS_RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, DORMANT);
        builder.define(DATA_SHIELDED, false);
        builder.define(DATA_CASTING, false);
    }

    public int phase() {
        return entityData.get(DATA_PHASE);
    }

    public boolean isAwake() {
        return phase() != DORMANT;
    }

    public boolean isShielded() {
        return entityData.get(DATA_SHIELDED);
    }

    public boolean isCasting() {
        return entityData.get(DATA_CASTING);
    }

    public @Nullable BlockPos throne() {
        return throne;
    }

    public List<BlockPos> pylons() {
        return List.copyOf(pylons);
    }

    public ServerBossEvent bossEvent() {
        return bossEvent;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        throne = blockPosition();
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return onFoot() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return onFoot() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (target, level) -> isAwake()));
    }

    /** Fighting on its feet: awake, and not up in the light under the pylons' shield. */
    private boolean onFoot() {
        return isAwake() && !(phase() == CORONA && isShielded());
    }

    // ---------------------------------------------------------------- waking, phases and the reset

    /** Rises from the throne to face {@code foe}: the boss bar shows, and it marks the pylons round it. */
    public void wake(@Nullable LivingEntity foe) {
        if (isAwake() || !(level() instanceof ServerLevel level)) {
            return;
        }
        if (throne == null) {
            throne = blockPosition();
        }
        if (pylons.isEmpty()) {
            findPylons(level);
        }
        setPhase(KING);
        bossEvent.setVisible(true);
        if (foe != null) {
            setTarget(foe);
        }
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 0.7F);
        announce(level, "message.hollowfrontier.archon.wakes", ChatFormatting.GOLD);
    }

    private void findPylons(ServerLevel level) {
        BlockPos at = throne != null ? throne : blockPosition();
        int r = (int) PYLON_RANGE;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -8, -r), at.offset(r, 12, r))) {
            if (level.getBlockState(pos).is(ModBlocks.SUN_PYLON.get())) {
                pylons.add(pos.immutable());
            }
        }
    }

    private int pylonsStanding() {
        int standing = 0;
        for (BlockPos pos : pylons) {
            if (level().getBlockState(pos).is(ModBlocks.SUN_PYLON.get())) {
                standing++;
            }
        }
        return standing;
    }

    private void setPhase(int phase) {
        entityData.set(DATA_PHASE, phase);
    }

    private void updatePhase(ServerLevel level) {
        float health = getHealth() / getMaxHealth();
        int phase = phase();
        if (phase == KING && health <= PHASE_TWO) {
            setPhase(CORONA);
            boolean shielded = pylonsStanding() > 0;
            entityData.set(DATA_SHIELDED, shielded);
            setNoGravity(shielded);
            getNavigation().stop();
            playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 0.6F);
            announce(level, shielded ? "message.hollowfrontier.archon.corona" : "message.hollowfrontier.archon.corona_bare", ChatFormatting.GOLD);
        } else if (phase == CORONA && isShielded() && pylonsStanding() == 0) {
            entityData.set(DATA_SHIELDED, false);
            setNoGravity(false);
            playSound(SoundEvents.BEACON_DEACTIVATE, 3.0F, 0.6F);
            announce(level, "message.hollowfrontier.archon.shield_broken", ChatFormatting.YELLOW);
        } else if (phase == CORONA && !isShielded() && health <= PHASE_THREE) {
            eclipse(level);
        } else if (phase == KING && health <= PHASE_THREE) {
            eclipse(level);
        }
    }

    private void eclipse(ServerLevel level) {
        setPhase(ECLIPSE);
        setNoGravity(false);
        entityData.set(DATA_SHIELDED, false);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(ECLIPSE_SPEED)) {
            speed.addTransientModifier(new AttributeModifier(ECLIPSE_SPEED, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        playSound(SoundEvents.WITHER_SPAWN, 2.0F, 1.4F);
        announce(level, "message.hollowfrontier.archon.eclipse", ChatFormatting.DARK_RED);
    }

    /** Back to the throne, healed, with the pylons made whole: no one is left to fight. */
    public void reset() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        setPhase(DORMANT);
        entityData.set(DATA_SHIELDED, false);
        entityData.set(DATA_CASTING, false);
        setNoGravity(false);
        setTarget(null);
        getNavigation().stop();
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ECLIPSE_SPEED);
        }
        setHealth(getMaxHealth());
        clearFire();
        if (throne != null) {
            teleportTo(throne.getX() + 0.5, throne.getY(), throne.getZ() + 0.5);
        }
        for (BlockPos pos : pylons) {
            if (level.getBlockState(pos).isAir()) {
                level.setBlockAndUpdate(pos, ModBlocks.SUN_PYLON.get().defaultBlockState());
            }
        }
        beamCharge = burstCharge = alone = 0;
        bossEvent.setVisible(false);
        bossEvent.removeAllPlayers();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (!isAwake()) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            if (tickCount % 10 == 0) {
                Player near = level.getNearestPlayer(getX(), getY(), getZ(), WAKE_RANGE, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
                if (near != null) {
                    wake(near);
                }
            }
            return;
        }
        updatePhase(level);
        bossEvent.setName(getDisplayName());
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 20 == 0) {
            updateBossBarPlayers(level);
        }
        if (!anyoneToFight(level)) {
            if (++alone >= RESET_TICKS) {
                reset();
            }
            return;
        }
        alone = 0;
        if (phase() == CORONA && isShielded()) {
            hover(level);
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            beam(level, target);
            if (phase() == CORONA) {
                flares(level, target);
            }
            if (phase() == ECLIPSE) {
                sunburst(level);
            }
        }
    }

    private boolean anyoneToFight(ServerLevel level) {
        return !level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(BOSS_RANGE),
                p -> p.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p)).isEmpty();
    }

    private void updateBossBarPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            boolean near = player.distanceTo(this) <= BOSS_RANGE;
            if (near && !bossEvent.getPlayers().contains(player)) {
                bossEvent.addPlayer(player);
            } else if (!near && bossEvent.getPlayers().contains(player)) {
                bossEvent.removePlayer(player);
            }
        }
    }

    /** Up in the light over the arena: rises to {@link #HOVER} over its throne's level, keeping over the arena. */
    private void hover(ServerLevel level) {
        BlockPos home = throne != null ? throne : blockPosition();
        Vec3 centre = pylons.isEmpty() ? Vec3.atBottomCenterOf(home) : pylonCentre();
        Vec3 goal = new Vec3(centre.x, home.getY() + HOVER, centre.z);
        Vec3 to = goal.subtract(position());
        setDeltaMovement(to.lengthSqr() > 0.25 ? to.normalize().scale(0.12) : Vec3.ZERO);
        if (getTarget() != null) {
            getLookControl().setLookAt(getTarget(), 30.0F, 30.0F);
        }
        if (isShielded() && tickCount % 10 == 0) {
            for (BlockPos pylon : pylons) {
                if (level.getBlockState(pylon).is(ModBlocks.SUN_PYLON.get())) {
                    line(level, PYLON_LIGHT, Vec3.atCenterOf(pylon), position().add(0, getBbHeight() * 0.5, 0), 1.5);
                }
            }
        }
    }

    private Vec3 pylonCentre() {
        double x = 0, z = 0;
        for (BlockPos pos : pylons) {
            x += pos.getX() + 0.5;
            z += pos.getZ() + 0.5;
        }
        return new Vec3(x / pylons.size(), 0, z / pylons.size());
    }

    // ---------------------------------------------------------------- its attacks

    /** The solar beam: drawn on a target in sight for {@link #BEAM_CHARGE} ticks, then it burns. */
    private void beam(ServerLevel level, LivingEntity target) {
        if (beamCharge == 0 && --beamCooldown > 0) {
            return;
        }
        if (!hasLineOfSight(target) || distanceTo(target) > PYLON_RANGE) {
            beamCharge = 0;
            beamCooldown = 20;
            entityData.set(DATA_CASTING, false);
            return;
        }
        if (beamCharge == 0) {
            playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.6F);
            entityData.set(DATA_CASTING, true);
        }
        beamCharge++;
        if (beamCharge % 2 == 0) {
            line(level, SUNBEAM, getEyePosition(), target.position().add(0, target.getBbHeight() * 0.6, 0), 2.5);
        }
        if (beamCharge >= BEAM_CHARGE) {
            if (target.hurtServer(level, ModDamageTypes.phlogistonBeam(this), BEAM_DAMAGE)) {
                target.igniteForSeconds(4.0F);
            }
            playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.6F);
            beamCharge = 0;
            beamCooldown = phase() == ECLIPSE ? BEAM_INTERVAL * 3 / 4 : BEAM_INTERVAL;
            entityData.set(DATA_CASTING, false);
        }
    }

    /** Three small fireballs at the target, spread a little. */
    private void flares(ServerLevel level, LivingEntity target) {
        if (--flareCooldown > 0) {
            return;
        }
        flareCooldown = FLARE_INTERVAL;
        playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.9F);
        for (int i = 0; i < 3; i++) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(getEyePosition())
                    .add(getRandom().triangle(0, 2.0), 0, getRandom().triangle(0, 2.0));
            SmallFireball flare = new SmallFireball(level, this, to.normalize());
            flare.setPos(getX(), getEyeY() - 0.3, getZ());
            level.addFreshEntity(flare);
        }
    }

    /** The eclipse's sunburst: a ring of fire gathers for {@link #BURST_CHARGE} ticks, then everyone near is burned and thrown. */
    private void sunburst(ServerLevel level) {
        if (burstCharge == 0 && --burstCooldown > 0) {
            return;
        }
        if (burstCharge == 0) {
            playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.0F, 1.2F);
        }
        burstCharge++;
        if (burstCharge % 4 == 0) {
            double r = BURST_RANGE * burstCharge / BURST_CHARGE;
            for (int k = 0; k < 24; k++) {
                double a = k * Math.PI / 12;
                level.sendParticles(ParticleTypes.FLAME, getX() + Math.cos(a) * r, getY() + 0.3, getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0.01);
            }
        }
        if (burstCharge >= BURST_CHARGE) {
            burst(level);
            burstCharge = 0;
            burstCooldown = BURST_INTERVAL;
        }
    }

    /** Burns and throws everyone (but its own kind) within {@link #BURST_RANGE}; how many it caught. */
    public int burst(ServerLevel level) {
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 1.3F);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        int caught = 0;
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BURST_RANGE),
                e -> e != this && e.isAlive() && !(e instanceof SunArchon) && distanceTo(e) <= BURST_RANGE)) {
            if (near instanceof Player p && !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p)) {
                continue;
            }
            if (near.hurtServer(level, ModDamageTypes.phlogistonBeam(this), BURST_DAMAGE)) {
                near.igniteForSeconds(4.0F);
            }
            Vec3 away = near.position().subtract(position()).multiply(1, 0, 1).normalize();
            near.push(away.x * 1.4, 0.6, away.z * 1.4);
            if (near instanceof ServerPlayer pushed) {
                pushed.connection.send(new ClientboundSetEntityMotionPacket(pushed));
            }
            caught++;
        }
        return caught;
    }

    private static void line(ServerLevel level, DustParticleOptions dust, Vec3 from, Vec3 to, double perBlock) {
        Vec3 step = to.subtract(from);
        int points = (int) Math.max(4, step.length() * perBlock);
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.add(step.scale(i / (double) points));
            level.sendParticles(dust, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    private void announce(ServerLevel level, String key, ChatFormatting colour) {
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(BOSS_RANGE))) {
            player.sendSystemMessage(Component.translatable(key).withStyle(colour));
        }
    }

    // ---------------------------------------------------------------- harm

    /** Woken by a blow; untouchable while the pylons shield it (save by a /kill). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!isAwake() && source.getEntity() instanceof LivingEntity attacker) {
            wake(attacker);
        }
        if (isShielded() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (source.getEntity() instanceof Player player) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.archon.shielded").withStyle(ChatFormatting.YELLOW));
            }
            playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.5F, 0.6F);
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel level) {
            announce(level, "message.hollowfrontier.archon.falls", ChatFormatting.GOLD);
            for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(BOSS_RANGE))) {
                if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
                    KindledStanding.add(serverPlayer, KINDLED_THANKS);
                }
            }
        }
        bossEvent.removeAllPlayers();
        super.die(source);
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
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && phase() == ECLIPSE) {
            target.igniteForSeconds(3.0F);
        }
        return hit;
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
        return false; // a king of the sun shrugs off poisons and potions alike
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("Throne", BlockPos.CODEC, throne);
        output.store("Pylons", BlockPos.CODEC.listOf(), List.copyOf(pylons));
        output.putInt("Phase", phase());
        output.putBoolean("Shielded", isShielded());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        throne = input.read("Throne", BlockPos.CODEC).orElse(null);
        pylons.clear();
        input.read("Pylons", BlockPos.CODEC.listOf()).ifPresent(pylons::addAll);
        setPhase(input.getIntOr("Phase", DORMANT));
        entityData.set(DATA_SHIELDED, input.getBooleanOr("Shielded", false));
        setNoGravity(phase() == CORONA && isShielded());
        if (isAwake()) {
            bossEvent.setVisible(true);
        }
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    // ---------------------------------------------------------------- animation

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SunArchon>("main", 5, state -> {
            SunArchon archon = state.animatable();
            if (!archon.isAwake()) {
                return state.setAndContinue(DORMANT_ANIM);
            }
            if (archon.isCasting()) {
                return state.setAndContinue(CAST);
            }
            if (archon.phase() == CORONA && archon.isShielded()) {
                return state.setAndContinue(HOVERING);
            }
            return state.setAndContinue(state.isMoving() ? DefaultAnimations.WALK : DefaultAnimations.IDLE);
        }));
        controllers.add(DefaultAnimations.genericAttackAnimation(SWING));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** For the tests: the Archon's corner of the world, without a structure to place it. */
    public void setThrone(BlockPos pos) {
        throne = pos.immutable();
    }

    /** For the tests: a pylon it counts, without a structure to find it in. */
    public void addPylon(BlockPos pos) {
        pylons.add(pos.immutable());
    }
}
