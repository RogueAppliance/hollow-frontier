package com.hollowfrontier.content.hollow.sea;

import java.util.Collection;
import java.util.List;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A thrown Harpoon (43b; a line weapon since the 2026-11-09 playtest, which found the first one a trident by another
 * name). It flies as true through water as through air, and what it hits it <b>holds</b>: a jab of {@link #JAB}
 * ({@link #TOOTHED_JAB} with a Basilosaur's teeth on it), and then the harpoon stays in its catch on the end of its line.
 * <ul>
 * <li><b>Something small</b> (under {@link #BIG} blocks square, side on: a fish, a jelly, a zombie, another player), or an
 *     item afloat that the harpoon passes within reach of, is <b>reeled in</b> to the thrower, and let go at arm's length.</li>
 * <li><b>Something big</b> (a sea cow, a Plesiosaur, a Basilosaurus) doesn't come: it <b>tows</b> the thrower along behind
 *     it once the line's {@link #SLACK} is taken up, boat and all if they're in one.</li>
 * </ul>
 * The line lets go when the thrower sneaks, when the catch dies, after {@link #holdTicks()} of holding, or when it's
 * stretched past {@link #lineLength()} and snaps. Then, as after a miss or {@link #LINE_TICKS} of flight, the harpoon is
 * hauled back to the thrower through anything in the way and goes into their pack. The line itself is drawn by its
 * renderer, a rope from the thrower's hand.
 */
public class HarpoonEntity extends AbstractArrow implements GeoEntity {
    public static final float JAB = 3.0F, TOOTHED_JAB = 5.0F;
    public static final int LINE_TICKS = 50;
    /** A catch at least this big (width times height) tows the thrower; anything smaller is reeled in. */
    public static final float BIG = 2.0F;
    public static final double SLACK = 5.0, REEL_SPEED = 0.3, ARMS_LENGTH = 2.0;
    /** How hard a towing catch pulls per block of stretch past the slack, the most it pulls in a tick, and the speed it won't pull past. */
    public static final double TOW_PER_BLOCK = 0.06, TOW_MAX = 0.3, TOW_SPEED = 0.9;
    public static final int HOLD_TICKS = 200, TOOTHED_HOLD_TICKS = 400;
    public static final double LINE_LENGTH = 32.0, TOOTHED_LINE_LENGTH = 48.0;
    /** How hard it's hauled home, as a trident's Loyalty II. */
    private static final int HAUL = 2;

    private static final EntityDataAccessor<Integer> DATA_HOOKED = SynchedEntityData.defineId(HarpoonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_TOOTHED = SynchedEntityData.defineId(HarpoonEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean dealtDamage;
    private int flown;
    private int held;
    private boolean returnSounded;

    public HarpoonEntity(EntityType<? extends HarpoonEntity> type, Level level) {
        super(type, level);
    }

    public HarpoonEntity(Level level, LivingEntity owner, ItemStack harpoon) {
        super(ModEntities.HARPOON.get(), owner, level, harpoon, null);
        entityData.set(DATA_TOOTHED, harpoon.is(ModItems.TOOTHED_HARPOON.get()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HOOKED, 0);
        builder.define(DATA_TOOTHED, false);
    }

    /** What a hit with this harpoon does before it holds. */
    public static float jab(ItemStack harpoon) {
        return harpoon.is(ModItems.TOOTHED_HARPOON.get()) ? TOOTHED_JAB : JAB;
    }

    public boolean isToothed() {
        return entityData.get(DATA_TOOTHED);
    }

    public int holdTicks() {
        return isToothed() ? TOOTHED_HOLD_TICKS : HOLD_TICKS;
    }

    public double lineLength() {
        return isToothed() ? TOOTHED_LINE_LENGTH : LINE_LENGTH;
    }

    /** What it has hold of, if anything (and it's still there). */
    public @Nullable Entity hooked() {
        int id = entityData.get(DATA_HOOKED);
        Entity entity = id == 0 ? null : level().getEntity(id - 1);
        return entity != null && entity.isAlive() ? entity : null;
    }

    public boolean isHooked() {
        return entityData.get(DATA_HOOKED) != 0;
    }

    /** Whether {@code catch_} is big enough to tow the thrower rather than be reeled in. */
    public static boolean tows(Entity catch_) {
        return catch_.getBbWidth() * catch_.getBbHeight() >= BIG;
    }

    public boolean isReturning() {
        return !isHooked() && (dealtDamage || isNoPhysics());
    }

    /** Takes hold of {@code catch_}: from here the line does the work. */
    public void hook(Entity catch_) {
        entityData.set(DATA_HOOKED, catch_.getId() + 1);
        held = 0;
        setNoPhysics(true);
        setDeltaMovement(Vec3.ZERO);
        playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.6F, 0.8F);
    }

    /** Lets go of whatever it holds; it comes home from here. */
    public void release() {
        if (isHooked()) {
            entityData.set(DATA_HOOKED, 0);
        }
        dealtDamage = true;
    }

    @Override
    public void tick() {
        Entity owner = getOwner();
        if (level() instanceof ServerLevel level) {
            if (isHooked()) {
                holdLine(level, owner);
            } else if (!dealtDamage) {
                if (inGroundTime > 4 || ++flown > LINE_TICKS) {
                    dealtDamage = true;
                } else {
                    // Flotsam: an item within reach of the head is caught as it passes
                    List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(0.75), ItemEntity::isAlive);
                    if (!items.isEmpty() && owner != null) {
                        hook(items.get(0));
                    }
                }
            }
        }
        if (isReturning() && owner != null) {
            if (!owner.isAlive() || owner instanceof ServerPlayer player && player.isSpectator()) {
                if (level() instanceof ServerLevel level && pickup == AbstractArrow.Pickup.ALLOWED) {
                    spawnAtLocation(level, getPickupItem(), 0.1F);
                }
                discard();
                return;
            }
            setNoPhysics(true);
            Vec3 line = owner.getEyePosition().subtract(position());
            setPosRaw(getX(), getY() + line.y * 0.015 * HAUL, getZ());
            setDeltaMovement(getDeltaMovement().scale(0.95).add(line.normalize().scale(0.05 * HAUL)));
            if (!returnSounded) {
                playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0F, 0.6F);
                returnSounded = true;
            }
        }
        super.tick();
    }

    /** One tick of a held line: the harpoon rides its catch, which is reeled in or tows, until something lets go. */
    private void holdLine(ServerLevel level, @Nullable Entity owner) {
        Entity catch_ = hooked();
        if (catch_ == null || owner == null || !owner.isAlive() || owner.level() != level) {
            release();
            return;
        }
        Vec3 at = catch_.getBoundingBox().getCenter();
        setPos(at.x, at.y, at.z);
        setDeltaMovement(Vec3.ZERO);
        held++;
        Vec3 line = owner.getEyePosition().subtract(at);
        double length = line.length();
        if (length > lineLength()) {
            playSound(SoundEvents.LEAD_BREAK, 1.0F, 0.8F);
            release();
            return;
        }
        if (held > holdTicks() || held > 10 && owner.isShiftKeyDown()) {
            release();
            return;
        }
        if (tows(catch_)) {
            Entity towed = owner.getRootVehicle();
            if (towed == catch_ || length <= SLACK) {
                return;
            }
            Vec3 pull = line.normalize().scale(-Math.min((length - SLACK) * TOW_PER_BLOCK, TOW_MAX));
            Vec3 motion = towed.getDeltaMovement().add(pull);
            if (motion.length() > TOW_SPEED) {
                motion = motion.normalize().scale(TOW_SPEED);
            }
            move(towed, motion);
        } else {
            if (length <= ARMS_LENGTH) {
                release();
                return;
            }
            move(catch_, line.normalize().scale(REEL_SPEED).add(0, 0.04, 0));
        }
    }

    // A player's own client decides how they move, so it has to be told.
    private static void move(Entity entity, Vec3 motion) {
        entity.setDeltaMovement(motion);
        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        for (Entity rider : entity.getPassengers()) {
            if (rider instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetEntityMotionPacket(entity));
            }
        }
    }


    @Override
    protected @Nullable EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return dealtDamage || isHooked() ? null : super.findHitEntity(from, to);
    }

    @Override
    protected Collection<EntityHitResult> findHitEntities(Vec3 from, Vec3 to) {
        EntityHitResult hit = findHitEntity(from, to);
        return hit != null ? List.of(hit) : List.of();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        Entity owner = getOwner();
        DamageSource source = damageSources().trident(this, owner == null ? this : owner);
        entity.hurtOrSimulate(source, jab(getPickupItemStackOrigin()));
        playSound(SoundEvents.TRIDENT_HIT, 1.0F, 0.8F);
        if (entity.isAlive() && owner != null && !level().isClientSide()) {
            hook(entity);
        } else {
            dealtDamage = true;
            setDeltaMovement(getDeltaMovement().scale(-0.05));
        }
    }

    @Override
    protected boolean tryPickup(Player player) {
        return !isHooked() && (super.tryPickup(player) || isNoPhysics() && ownedBy(player) && player.getInventory().add(getPickupItem()));
    }

    @Override
    public void playerTouch(Player player) {
        if (ownedBy(player) || getOwner() == null) {
            super.playerTouch(player);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.HARPOON.get());
    }

    /** The harpoon that was thrown (the server's copy; clients know only {@link #isToothed}). */
    public ItemStack getItem() {
        return getPickupItemStackOrigin();
    }

    @Override
    public ItemStack getWeaponItem() {
        return getPickupItemStackOrigin();
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    public void tickDespawn() {
        if (pickup != AbstractArrow.Pickup.ALLOWED || getOwner() == null) {
            super.tickDespawn();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DealtDamage", dealtDamage || isHooked()); // a held line isn't saved: it comes home
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        dealtDamage = input.getBooleanOr("DealtDamage", false);
        entityData.set(DATA_TOOTHED, getPickupItemStackOrigin().is(ModItems.TOOTHED_HARPOON.get()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
