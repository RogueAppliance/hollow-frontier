package com.hollowfrontier.content.frontier.mystery;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * An airship-wave sighting (11a): a great dark cigar strung with lights, high over the frontier at night, as ranchers
 * swore they saw across the West in 1896-97. It crosses the sky in a straight line at an unhurried pace ({@link #SPEED})
 * and is gone in under half a minute; nothing touches it and it touches nothing. One sent for the Boot Hill pilot's skull
 * (a {@link StrangeSkullItem} held up to the night sky) comes over the player and hangs there a while first, its light
 * on them. Everyone in the Overworld within {@link #WITNESS_RANGE} blocks of it sees it (the sighting advancement).
 * Never saved: a sighting that would outlast its chunks just isn't there next time.
 */
public class AirshipSighting extends Entity implements GeoEntity {
    public static final double SPEED = 0.5;
    /** Half its run: it appears this far before its nearest point to the one it was sent over, and goes this far past. */
    public static final double HALF_RUN = 96.0;
    public static final double WITNESS_RANGE = 160.0;
    private static final EntityDataAccessor<Boolean> DATA_HOVERING = SynchedEntityData.defineId(AirshipSighting.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation DRIFT = RawAnimation.begin().thenLoop("misc.drift");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> witnesses = new HashSet<>();
    private int ticksLeft = (int) (2 * HALF_RUN / SPEED);
    private int hoverTicks;
    private Vec3 heading = new Vec3(SPEED, 0, 0);

    public AirshipSighting(EntityType<? extends AirshipSighting> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Sets it on its way along {@code direction} (flattened); hovering first for {@code hover} ticks if that's above 0. */
    public void launch(Vec3 direction, int hover) {
        Vec3 flat = new Vec3(direction.x, 0, direction.z);
        heading = (flat.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : flat.normalize()).scale(SPEED);
        setYRot((float) (Mth.atan2(-heading.x, heading.z) * Mth.RAD_TO_DEG));
        hoverTicks = hover;
        entityData.set(DATA_HOVERING, hover > 0);
        setDeltaMovement(hover > 0 ? Vec3.ZERO : heading);
    }

    public boolean isHovering() {
        return entityData.get(DATA_HOVERING);
    }

    public int ticksLeft() {
        return ticksLeft;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HOVERING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            setPos(position().add(getDeltaMovement())); // carried along between the server's updates
            return;
        }
        if (hoverTicks > 0) {
            setDeltaMovement(new Vec3(0, Math.sin(tickCount * 0.1) * 0.02, 0));
            if (--hoverTicks == 0) {
                entityData.set(DATA_HOVERING, false);
            }
        } else {
            setDeltaMovement(heading);
            if (--ticksLeft <= 0) {
                discard();
                return;
            }
        }
        setPos(position().add(getDeltaMovement()));
        if (tickCount % 60 == 0) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.SIGHTING_HUM.get(), SoundSource.AMBIENT, 8.0F, 0.6F);
        }
        if (tickCount % 20 == 0) {
            for (ServerPlayer player : level.players()) {
                if (Math.hypot(player.getX() - getX(), player.getZ() - getZ()) < WITNESS_RANGE && witnesses.add(player.getUUID())) {
                    ModCriteria.fire(player, ModCriteria.SIGHTING_SEEN);
                }
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 384.0 * 384.0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<AirshipSighting>("drift", 0, test -> test.setAndContinue(DRIFT)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
