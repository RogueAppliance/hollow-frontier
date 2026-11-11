package com.hollowfrontier.content.frontier.gear;

import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.RaidMember;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A lit stick of dynamite. It bounces off blocks (losing most of its speed), stops dead against mobs, sputters sparks
 * and goes off when the fuse burns down. The blast breaks blocks like TNT (somewhat smaller) and is credited to the
 * thrower, so blowing up townsfolk is a crime.
 * <p>
 * Raider dynamite (thrown by a raid gang's dynamiter) never breaks blocks, whatever mobGriefing says, so a raid can't
 * wreck the town; it's a little smaller and spares townsfolk, the gang and their horses. It still knocks lawmen and
 * players about.
 */
public class DynamiteEntity extends ThrowableItemProjectile {
    public static final int FUSE_TICKS = 50;
    public static final float POWER = 2.5F;
    public static final float RAIDER_POWER = 1.75F;
    private static final double BOUNCE = 0.35;
    private static final double FRICTION = 0.55;

    private int fuse = FUSE_TICKS;
    private boolean raider;

    // Raider blasts: no blocks, and only hurts the people the gang is fighting.
    private static final ExplosionDamageCalculator RAIDER_BLAST = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
            return false;
        }

        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return super.shouldDamageEntity(explosion, entity) && !(entity instanceof AbstractVillager || entity instanceof AbstractOutlaw
                    || entity instanceof ArmorStand || RaidMember.of(entity) != null);
        }
    };

    public DynamiteEntity(EntityType<? extends DynamiteEntity> type, Level level) {
        super(type, level);
    }

    public DynamiteEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(ModEntities.DYNAMITE.get(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.DYNAMITE.get();
    }

    public int getFuse() {
        return fuse;
    }

    /** Sets the fuse (ticks left); the photo booth hangs a stick in the air with a fuse that never runs out. */
    public void setFuse(int ticks) {
        fuse = ticks;
    }

    public boolean isRaider() {
        return raider;
    }

    public void setRaider(boolean raider) {
        this.raider = raider;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.25, getZ(), 0.0, 0.02, 0.0);
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.25, getZ(), 0.0, 0.01, 0.0);
            }
        } else if (--fuse <= 0) {
            discard();
            // 46c: its own low thump under vanilla's bang, carried 64 blocks; the client shakes the view by it
            level().playSound(null, getX(), getY(), getZ(), com.hollowfrontier.registry.ModSounds.DYNAMITE_BLAST.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, 1.0F);
            if (raider) {
                level().explode(this, null, RAIDER_BLAST, getX(), getY(0.0625), getZ(), RAIDER_POWER, false, Level.ExplosionInteraction.NONE);
            } else {
                level().explode(this, getX(), getY(0.0625), getZ(), POWER, Level.ExplosionInteraction.TNT);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("fuse", fuse);
        if (raider) {
            output.putBoolean("raider", true);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        fuse = input.getIntOr("fuse", FUSE_TICKS);
        raider = input.getBooleanOr("raider", false);
    }

    // Bounce: flip the velocity across the face it hit, damped, and lose speed along the face.
    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        Vec3 v = getDeltaMovement();
        Direction.Axis axis = hit.getDirection().getAxis();
        v = new Vec3(
                axis == Direction.Axis.X ? -v.x * BOUNCE : v.x * FRICTION,
                axis == Direction.Axis.Y ? -v.y * BOUNCE : v.y * FRICTION,
                axis == Direction.Axis.Z ? -v.z * BOUNCE : v.z * FRICTION);
        setDeltaMovement(v);
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
        setPos(hit.getLocation().add(normal.scale(0.02)));
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        setDeltaMovement(getDeltaMovement().scale(-0.1));
    }
}
