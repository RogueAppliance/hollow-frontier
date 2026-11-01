package com.hollowfrontier.content.ufo;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A phlogiston gun's beam as it's seen (13b): a shaft of light from the muzzle to where the shot stopped, drawn by
 * PhlogistonBeamRenderer with vanilla's beacon-beam code (full-bright, and shader packs treat it as a beacon's), thinning away
 * over {@link #LIFETIME} ticks (or as long as it's given). It can be drawn wider: the Mothership's death ray is (13d).
 * It's only the look of the shot: the harm is done the moment the gun fires. Never saved,
 * touches nothing.
 */
public class PhlogistonBeam extends Entity {
    public static final int LIFETIME = 8;
    // The glow's radius at width 1, in blocks (PhlogistonBeamRenderer's): a beam of width w glows w * GLOW_WIDTH out from its line
    public static final float GLOW_WIDTH = 0.08F;
    private static final EntityDataAccessor<Vector3fc> DATA_REACH = SynchedEntityData.defineId(PhlogistonBeam.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_COLOUR = SynchedEntityData.defineId(PhlogistonBeam.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(PhlogistonBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(PhlogistonBeam.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SOFT = SynchedEntityData.defineId(PhlogistonBeam.class, EntityDataSerializers.BOOLEAN);

    public PhlogistonBeam(EntityType<? extends PhlogistonBeam> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** A beam from {@code from} to {@code to} in {@code colour} (RGB). */
    public void aim(Vec3 from, Vec3 to, int colour) {
        setPos(from);
        Vec3 reach = to.subtract(from);
        entityData.set(DATA_REACH, new Vector3f((float) reach.x, (float) reach.y, (float) reach.z));
        entityData.set(DATA_COLOUR, 0xFF000000 | colour);
    }

    /** As {@link #aim(Vec3, Vec3, int)}, {@code width} times as wide, lasting {@code life} ticks. */
    public void aim(Vec3 from, Vec3 to, int colour, float width, int life) {
        aim(from, to, colour);
        entityData.set(DATA_WIDTH, width);
        entityData.set(DATA_LIFE, life);
    }

    /** Only the glow, no bright core: a column of light to see through (the Mothership's abduction beam). */
    public void soften() {
        entityData.set(DATA_SOFT, true);
    }

    public boolean soft() {
        return entityData.get(DATA_SOFT);
    }

    public float width() {
        return entityData.get(DATA_WIDTH);
    }

    public int life() {
        return entityData.get(DATA_LIFE);
    }

    /** From its start to its end. */
    public Vector3fc reach() {
        return entityData.get(DATA_REACH);
    }

    public int colour() {
        return entityData.get(DATA_COLOUR);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_REACH, new Vector3f());
        builder.define(DATA_COLOUR, 0xFFFFFFFF);
        builder.define(DATA_WIDTH, 1.0F);
        builder.define(DATA_LIFE, LIFETIME);
        builder.define(DATA_SOFT, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount >= life()) {
            discard();
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
        return distance < 128.0 * 128.0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
