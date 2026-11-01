package com.hollowfrontier.content.frontier.weather;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hollowfrontier.content.hollow.creature.AshSalamander;
import com.hollowfrontier.content.hollow.creature.HollowCreatures;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModParticles;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A dust devil (phase 29): a whirl of dust {@link #HEIGHT} blocks tall that wanders over hot, dry ground on a clear day
 * for {@link #MIN_LIFE} to {@link #MAX_LIFE} ticks, spinning up at the start and dying down at the end. It has no model:
 * the client draws it in particles, tinted by the ground it's crossing. It draws what's on the ground within
 * {@link #PULL_RANGE} in toward its foot ({@link #drawIn}). What it catches at its foot ({@link #caught}):
 * <ul>
 * <li>dropped items and small creatures ({@link #carries}) go round and up the column and are flung out at the top, or
 * when it dies down, with slow falling for the creatures;</li>
 * <li>a player who isn't sneaking is lifted a few blocks, spun and let down gently, and loses a Cowboy Hat downwind
 * (Curse of Binding keeps it on); a sneaking player is only pushed;</li>
 * <li>a horse rears, and now and then throws its rider, unless it's shod.</li>
 * </ul>
 * When it's done it drops a roll on {@link DustDevils#LOOT}. Suspicious sand and gravel it crosses glints for a few
 * seconds. The Ash Devil is the same whirl in the Ember Badlands: grey, full of cinders, setting light to what it
 * carries and to the ground it crosses (where fire may spread), and it drops nothing. Never saved.
 */
public class DustDevil extends Entity {
    public static final double HEIGHT = 12.0;
    /** How far from its middle it catches things at its foot, and how wide it is at the top. */
    public static final double FOOT = 3.0, TOP = 4.6;
    /** How far out, along the ground, it draws things in toward its foot, and how hard (blocks a tick, at full strength). */
    public static final double PULL_RANGE = 7.0, PULL = 0.05;
    public static final double SPEED = 0.07;
    public static final int MIN_LIFE = 600, MAX_LIFE = 1800, SPIN_UP = 60, DIE_DOWN = 80;
    /** How long a lifted player rises (Levitation III: about six blocks), and how long they're let down for after. */
    public static final int LIFT_TICKS = 40, LIFT_LEVEL = 2, SETTLE_TICKS = 160;
    /** How long before the same thing can be caught again, once it's been flung out or set down. */
    public static final int AGAIN = 100;
    public static final float THROW_CHANCE = 0.6F;
    /** The biggest creature it carries: a sheep, a pig or a wolf, not a cow. */
    public static final float SMALL_WIDTH = 1.0F, SMALL_HEIGHT = 1.3F;
    public static final int GLINT_TICKS = 100;
    /** Motes of dust a tick at full strength, and how many bands they wind up it in (the client's drawing). */
    public static final int MOTES = 14, BANDS = 3;

    private static final EntityDataAccessor<Long> DATA_BORN = SynchedEntityData.defineId(DustDevil.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(DustDevil.class, EntityDataSerializers.INT);

    private double heading;
    private boolean fixed;
    private boolean wild = true;
    /** Everything caught lately, by entity id, and when it was last let go (or 0 while it's still in the whirl). */
    private final Map<Integer, Long> caught = new HashMap<>();
    private final Map<BlockPos, Long> glints = new HashMap<>();

    public DustDevil(EntityType<? extends DustDevil> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        heading = level.getRandom().nextDouble() * Math.PI * 2;
    }

    /** A new devil at {@code pos}, born now, lasting {@code life} ticks. */
    public static DustDevil spawn(ServerLevel level, EntityType<DustDevil> type, Vec3 pos, int life) {
        DustDevil devil = type.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        devil.setPos(pos);
        devil.entityData.set(DATA_BORN, level.getGameTime());
        devil.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(devil);
        return devil;
    }

    public boolean isAsh() {
        return getType() == ModEntities.ASH_DEVIL.get();
    }

    public int age() {
        return (int) Math.max(0, level().getGameTime() - entityData.get(DATA_BORN));
    }

    public int life() {
        return entityData.get(DATA_LIFE);
    }

    /** Stays where it is (game tests and the photo scene). */
    public DustDevil fixed() {
        fixed = true;
        return this;
    }

    /** Called up by command: it blows wherever it's put, minding only water. */
    public DustDevil tame() {
        wild = false;
        return this;
    }

    /** Starts dying down now, if it isn't already. */
    public void dieDown() {
        entityData.set(DATA_LIFE, Math.min(life(), age() + DIE_DOWN));
    }

    /** 0 to 1: how hard it's blowing, rising over its first {@link #SPIN_UP} ticks and falling over its last {@link #DIE_DOWN}. */
    public float strength() {
        int age = age(), life = life();
        return Mth.clamp(Math.min(age / (float) SPIN_UP, (life - age) / (float) DIE_DOWN), 0.0F, 1.0F);
    }

    public double heading() {
        return heading;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BORN, 0L);
        builder.define(DATA_LIFE, MIN_LIFE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            whirl();
            return;
        }
        if (age() >= life()) {
            finish(level);
            return;
        }
        if (!fixed) {
            wander(level);
        }
        float strength = strength();
        if (strength > 0.3F) {
            catchNear(level, strength);
        }
        if (tickCount % 40 == 0) {
            level.playSound(null, getX(), getY() + 2, getZ(), ModSounds.DUST_DEVIL_WIND.get(), SoundSource.WEATHER, 1.4F * strength + 0.2F, 0.9F + random.nextFloat() * 0.2F);
            if (isAsh()) {
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.WEATHER, 1.0F, 0.7F);
            }
        }
        if (isAsh() && tickCount % 20 == 0 && strength > 0.5F) {
            scorch(level);
        }
        if (tickCount % 10 == 0) {
            glint(level);
        }
    }

    // Along its way, a little to one side or the other each tick, over the ground; it dies down over water, in the rain
    // or out of its country.
    private void wander(ServerLevel level) {
        heading += random.nextGaussian() * 0.12;
        double x = getX() + Math.cos(heading) * SPEED, z = getZ() + Math.sin(heading) * SPEED;
        int ground = HollowCreatures.groundBelow(level, Mth.floor(x), Mth.floor(z), Mth.floor(getY()) + 3);
        if (ground < getY() - 8) {
            heading += Math.PI; // a drop: turn back
            return;
        }
        BlockPos foot = BlockPos.containing(x, ground + 1, z);
        setPos(x, ground + 1, z);
        boolean blows = wild ? DustDevils.canBlowAt(level, foot, isAsh()) : level.getFluidState(foot).isEmpty();
        if (tickCount % 20 == 0 && !blows) {
            dieDown();
        }
    }

    private void catchNear(ServerLevel level, float strength) {
        long now = level.getGameTime();
        if (tickCount % 20 == 0) {
            // whatever got clear of it some other way is let go too
            caught.replaceAll((id, letGo) -> {
                Entity entity = level.getEntity(id);
                return letGo == 0 && (entity == null || entity.distanceToSqr(getX(), entity.getY(), getZ()) > (TOP + 2) * (TOP + 2)) ? now : letGo;
            });
            caught.values().removeIf(letGo -> letGo > 0 && now - letGo > AGAIN);
        }
        double out = Math.max(TOP, PULL_RANGE);
        AABB reach = new AABB(getX() - out, getY() - 0.5, getZ() - out, getX() + out, getY() + HEIGHT, getZ() + out);
        for (Entity entity : level.getEntities(this, reach, e -> e.isAlive() && !e.isSpectator())) {
            Long letGo = caught.get(entity.getId());
            if (letGo != null && letGo > 0 && now - letGo < AGAIN) {
                continue;
            }
            double dx = entity.getX() - getX(), dz = entity.getZ() - getZ(), r = Math.sqrt(dx * dx + dz * dz), h = entity.getY() - getY();
            boolean inside = r <= FOOT + (TOP - FOOT) * Mth.clamp(h / HEIGHT, 0.0, 1.0);
            boolean atFoot = h < 2.5 && r <= FOOT;
            boolean held = letGo != null && letGo == 0;
            if (!held && !atFoot && h < 3.0 && r > FOOT && r <= PULL_RANGE) {
                drawIn(entity, dx, dz, r, strength);
                continue;
            }
            if (entity instanceof Player player) {
                if (letGo != null && letGo == 0) {
                    if (inside && player.hasEffect(MobEffects.LEVITATION)) {
                        spin(player, dx, dz, r, strength);
                    } else {
                        caught.put(entity.getId(), now); // up and out, or let down: done with them
                    }
                } else if (atFoot) {
                    catchPlayer(level, player, now);
                }
            } else if (entity instanceof AbstractHorse horse) {
                if (atFoot) {
                    spook(level, horse, now);
                }
            } else if (carries(entity) && (inside || letGo != null && letGo == 0)) {
                if (letGo == null && !atFoot) {
                    continue; // things are caught at its foot, then carried up
                }
                if (h >= HEIGHT - 1.0 || !inside) {
                    fling(entity, dx, dz, r, now);
                } else {
                    caught.put(entity.getId(), 0L);
                    carry(entity, dx, dz, r, strength);
                }
            }
        }
    }

    // Along the ground toward its foot, and a little round it: the nearer, the harder. A player keeping low, or in
    // creative, isn't drawn in; an Ash Salamander isn't drawn into an Ash Devil.
    private void drawIn(Entity entity, double dx, double dz, double r, float strength) {
        if (entity instanceof Player player && (player.isShiftKeyDown() || player.isCreative() || !DustDevilConfig.liftPlayers())
                || isAsh() && entity instanceof AshSalamander || entity.isNoGravity()) {
            return;
        }
        double force = PULL * strength * (1.0 - (r - FOOT) / (PULL_RANGE - FOOT) * 0.6);
        double ix = -dx / r * force - dz / r * force * 0.5, iz = -dz / r * force + dx / r * force * 0.5;
        if (entity instanceof Player player) {
            if (tickCount % 4 == 0) {
                player.push(ix * 4, 0.0, iz * 4); // their client moves them: a nudge every few ticks
                player.syncVelocity = true;
            }
        } else {
            entity.push(ix, 0.0, iz);
            entity.needsSync = true;
        }
    }

    /** Items, and creatures small enough to be blown about (an Ash Salamander minds an Ash Devil not at all). */
    public boolean carries(Entity entity) {
        if (entity instanceof ItemEntity) {
            return true;
        }
        return entity instanceof LivingEntity living && !(entity instanceof Player) && !living.isPassenger() && !living.isVehicle()
                && !living.isNoGravity() && living.getBbWidth() <= SMALL_WIDTH && living.getBbHeight() <= SMALL_HEIGHT
                && !(isAsh() && living instanceof AshSalamander) && living.getType().canSerialize();
    }

    // Round the column and up it, held toward the middle.
    private void carry(Entity entity, double dx, double dz, double r, float strength) {
        if (r < 0.05) {
            dx = 0.05;
            r = 0.05;
        }
        double tx = -dz / r, tz = dx / r, pull = Mth.clamp((r - 1.0) * 0.12, -0.1, 0.2);
        double up = entity instanceof ItemEntity ? 0.16 : 0.2;
        entity.setDeltaMovement(tx * 0.55 * strength - dx / r * pull, up * strength + 0.04, tz * 0.55 * strength - dz / r * pull);
        entity.resetFallDistance();
        entity.needsSync = true;
        if (isAsh() && entity instanceof LivingEntity living && !living.fireImmune()) {
            living.igniteForSeconds(3.0F);
        }
    }

    // Out of the top (or the side), with a soft landing for the living.
    private void fling(Entity entity, double dx, double dz, double r, long now) {
        if (r < 0.05) {
            dx = Math.cos(heading);
            dz = Math.sin(heading);
            r = 1.0;
        }
        entity.setDeltaMovement(dx / r * 1.1, 0.5, dz / r * 1.1);
        entity.needsSync = true;
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SETTLE_TICKS, 0, true, false, false));
        }
        caught.put(entity.getId(), now);
    }

    private void catchPlayer(ServerLevel level, Player player, long now) {
        if (player.isCreative()) {
            return;
        }
        if (player.getVehicle() instanceof AbstractHorse horse) {
            spook(level, horse, now);
            blowHat(level, player);
            caught.put(player.getId(), now);
            return;
        }
        double dx = player.getX() - getX(), dz = player.getZ() - getZ(), r = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
        if (player.isShiftKeyDown() || !DustDevilConfig.liftPlayers() || player.isPassenger()) {
            // keeping low: a shove round the whirl, and the hat stays on
            player.push(-dz / r * 0.3, 0.05, dx / r * 0.3);
            player.syncVelocity = true; // sent to the player's own client too
            caught.put(player.getId(), now);
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LIFT_TICKS, LIFT_LEVEL, true, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, LIFT_TICKS + SETTLE_TICKS, 0, true, false, true));
        player.resetFallDistance();
        caught.put(player.getId(), 0L);
        blowHat(level, player);
        if (isAsh() && !player.fireImmune()) {
            player.igniteForSeconds(3.0F);
        }
    }

    // A lifted player goes round with it: a shove sideways now and then (their client plays the levitation out).
    private void spin(Player player, double dx, double dz, double r, float strength) {
        player.resetFallDistance();
        if (tickCount % 3 == 0) {
            r = Math.max(0.1, r);
            double pull = Mth.clamp((r - 1.0) * 0.1, -0.1, 0.15);
            player.push(-dz / r * 0.4 * strength - dx / r * pull, 0.0, dx / r * 0.4 * strength - dz / r * pull);
            player.syncVelocity = true; // sent to the player's own client too
        }
    }

    /** Rears a horse caught in it, and now and then throws its rider; a shod horse keeps its feet. */
    public void spook(ServerLevel level, AbstractHorse horse, long now) {
        Long letGo = caught.get(horse.getId());
        if (letGo != null && now - letGo < AGAIN) {
            return;
        }
        caught.put(horse.getId(), now);
        if (horse.getData(ModAttachments.HORSESHOE_TIER.get()) > 0) {
            return;
        }
        horse.makeMad();
        if (horse.isVehicle() && random.nextFloat() < THROW_CHANCE) {
            horse.ejectPassengers();
        }
    }

    /** Takes a Cowboy Hat off the player's head and drops it a way downwind (not one with Curse of Binding). */
    public boolean blowHat(ServerLevel level, Player player) {
        ItemStack hat = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!hat.is(ModItems.COWBOY_HAT.get()) || EnchantmentHelper.has(hat, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return false;
        }
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        ItemEntity item = new ItemEntity(level, player.getX(), player.getEyeY(), player.getZ(), hat);
        item.setDeltaMovement(Math.cos(heading) * 0.6, 0.45, Math.sin(heading) * 0.6);
        item.setPickUpDelay(40);
        level.addFreshEntity(item);
        caught.put(item.getId(), level.getGameTime()); // the hat goes off downwind, not up the whirl
        player.setData(ModAttachments.HAT_LOST.get(), level.getGameTime());
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.dust_devil.hat"));
        ModCriteria.fire(player, ModCriteria.HAT_BLOWN);
        return true;
    }

    // Sets light to the ground it crosses, where fire may spread.
    private void scorch(ServerLevel level) {
        BlockPos at = BlockPos.containing(getX() + random.nextGaussian(), getY(), getZ() + random.nextGaussian());
        BlockState below = level.getBlockState(at.below());
        if (random.nextFloat() < 0.35F && level.getBlockState(at).isAir() && level.canSpreadFireAround(at)
                && below.isFlammable(level, at.below(), net.minecraft.core.Direction.UP)
                && BaseFireBlock.canBePlacedAt(level, at, net.minecraft.core.Direction.NORTH)) {
            level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
        }
    }

    // Scoured sand: suspicious sand and gravel near its foot show a glint while the wind is over them, and a while after.
    private void glint(ServerLevel level) {
        long now = level.getGameTime();
        BlockPos foot = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(foot.offset(-2, -3, -2), foot.offset(2, 0, 2))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL)) {
                glints.put(pos.immutable(), now + GLINT_TICKS);
            }
        }
        glints.values().removeIf(until -> until < now);
        glints.keySet().forEach(pos -> level.sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 2, 0.3, 0.05, 0.3, 0.0));
    }

    /** What it's still carrying goes out the sides, the loot drops, and it's gone. */
    public void finish(ServerLevel level) {
        for (Map.Entry<Integer, Long> entry : caught.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null && entry.getValue() == 0 && !(entity instanceof Player)) {
                fling(entity, entity.getX() - getX(), entity.getZ() - getZ(), Math.hypot(entity.getX() - getX(), entity.getZ() - getZ()), level.getGameTime());
            }
        }
        if (!isAsh()) {
            for (ItemStack stack : DustDevils.loot(level, position())) {
                ItemEntity item = new ItemEntity(level, getX(), getY() + 1.5, getZ(), stack);
                item.setDeltaMovement(random.nextGaussian() * 0.15, 0.3, random.nextGaussian() * 0.15);
                level.addFreshEntity(item);
            }
            DustDevils.record(level, blockPosition());
        }
        discard();
    }

    /** Whether something is in it right now (carried, or a player held up in it). */
    public boolean holds(Entity entity) {
        Long letGo = caught.get(entity.getId());
        return letGo != null && letGo == 0;
    }

    public List<Integer> held() {
        List<Integer> out = new ArrayList<>();
        caught.forEach((id, letGo) -> {
            if (letGo == 0) {
                out.add(id);
            }
        });
        return out;
    }

    // ---------------------------------------------------------------- the client: drawing it in particles

    private void whirl() {
        float strength = strength();
        if (strength <= 0.0F) {
            return;
        }
        Level level = level();
        RandomSource random = this.random;
        BlockPos below = blockPosition().below();
        BlockState ground = level.getBlockState(below);
        int colour = isAsh() ? 0x3A3633 : ground.getMapColor(level, below).col;
        if (colour == 0) {
            colour = 0xC8B48A;
        }
        float time = tickCount;
        long now = level.getGameTime();
        // Motes that go round and up it (DustDevilMote), most of them laid along BANDS bands that wind up the column
        // as they turn, the rest anywhere in it; they start low and climb.
        int count = Mth.ceil(MOTES * strength);
        for (int i = 0; i < count; i++) {
            double h = Math.pow(random.nextDouble(), 1.8) * HEIGHT * 0.8 * (0.5 + 0.5 * strength);
            double angle = random.nextFloat() < 0.65F
                    ? random.nextInt(BANDS) * Math.PI * 2 / BANDS + time * 0.25 - h * 0.55 + random.nextGaussian() * 0.15
                    : random.nextDouble() * Math.PI * 2;
            double radius = funnel(h) * (0.6 + random.nextDouble() * 0.5);
            Vec3 lean = lean(now, h);
            int tint = 0xFF000000 | shade(colour, 0.85F + random.nextFloat() * 0.3F);
            level.addAlwaysVisibleParticle(ColorParticleOption.create(ModParticles.DUST_DEVIL_MOTE.get(), tint),
                    getX() + lean.x + Math.cos(angle) * radius, getY() + h, getZ() + lean.z + Math.sin(angle) * radius, getId(), 0, 0);
        }
        // and a little loose haze at its foot that drifts off on its own
        if (random.nextFloat() < 0.5F * strength) {
            double angle = random.nextDouble() * Math.PI * 2;
            level.addParticle(new DustParticleOptions(shade(colour, 0.9F), 2.5F), getX() + Math.cos(angle) * FOOT, getY() + 0.3 + random.nextDouble(),
                    getZ() + Math.sin(angle) * FOOT, -Math.sin(angle) * 0.4, 0.1, Math.cos(angle) * 0.4);
        }
        if (random.nextFloat() < strength) {
            double angle = random.nextDouble() * Math.PI * 2;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX() + Math.cos(angle) * FOOT * 0.8, getY() + 0.1,
                    getZ() + Math.sin(angle) * FOOT * 0.8, -Math.sin(angle) * 0.3, 0.3, Math.cos(angle) * 0.3);
        }
        if (isAsh()) {
            if (random.nextFloat() < 0.6F * strength) {
                level.addAlwaysVisibleParticle(ParticleTypes.ASH, getX() + random.nextGaussian(), getY() + random.nextDouble() * HEIGHT, getZ() + random.nextGaussian(), 0, 0, 0);
            }
            if (random.nextFloat() < 0.25F * strength) {
                level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, getX() + random.nextGaussian() * 0.8, getY() + random.nextDouble() * 3, getZ() + random.nextGaussian() * 0.8, 0, 0.08, 0);
            }
            if (random.nextFloat() < 0.15F * strength) {
                level.addParticle(ParticleTypes.SMALL_FLAME, getX() + random.nextGaussian() * 0.6, getY() + 0.3, getZ() + random.nextGaussian() * 0.6, 0, 0.04, 0);
            }
        } else if (random.nextFloat() < 0.12F * strength) {
            // a tumbleweed's worth of dry brush, bowling round its foot
            double angle = time * 0.4 + random.nextDouble();
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEAD_BUSH.defaultBlockState()), getX() + Math.cos(angle) * FOOT,
                    getY() + 0.3 + random.nextDouble(), getZ() + Math.sin(angle) * FOOT, -Math.sin(angle) * 0.4, 0.15, Math.cos(angle) * 0.4);
        }
    }

    /** How wide the funnel is at height {@code h}: narrow at the foot, widening to {@link #TOP}. */
    public static double funnel(double h) {
        double f = Mth.clamp(h / HEIGHT, 0.0, 1.0);
        return FOOT * 0.3 + (TOP - FOOT * 0.3) * f * f * 0.6 + (TOP - FOOT * 0.3) * f * 0.4;
    }

    /**
     * How far the column's middle is pushed off its foot at height {@code h} and game time {@code time}: two slow waves
     * that travel up it, so a kink runs up the column and it snakes; nothing at the foot, most at the top.
     */
    public static Vec3 lean(double time, double h) {
        double f = Mth.clamp(h / HEIGHT, 0.0, 1.0);
        return new Vec3(Math.sin(time * 0.09 - h * 0.45) * 0.6 * f, 0.0, Math.cos(time * 0.065 - h * 0.35) * 0.45 * f);
    }

    private static int shade(int rgb, float by) {
        int r = Mth.clamp((int) (((rgb >> 16) & 0xFF) * by), 0, 255), g = Mth.clamp((int) (((rgb >> 8) & 0xFF) * by), 0, 255),
                b = Mth.clamp((int) ((rgb & 0xFF) * by), 0, 255);
        return r << 16 | g << 8 | b;
    }

    // ---------------------------------------------------------------- an entity nothing touches

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160.0 * 160.0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    /** For the command: one in front of the runner. */
    static Vec3 inFront(ServerPlayer player, double distance) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return player.position().add((flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize()).scale(distance));
    }
}
