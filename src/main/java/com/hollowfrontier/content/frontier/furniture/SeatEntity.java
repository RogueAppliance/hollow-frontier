package com.hollowfrontier.content.frontier.furniture;

import com.hollowfrontier.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible mount a player rides while sitting on a bar stool. It sits on the cushion, holds one rider and
 * removes itself once the rider gets up or the stool is gone (also when a world loads with nobody on it).
 */
public class SeatEntity extends Entity {
    /** Height of the cushion top above the stool's block (BarStoolBlock.SHAPE). */
    public static final double SEAT_HEIGHT = 13.0 / 16.0;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Sits the player on the stool at pos, unless someone is already sitting there. */
    public static boolean sit(Level level, BlockPos pos, LivingEntity player) {
        if (!level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty()) {
            return false;
        }
        SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), level);
        double height = level.getBlockState(pos).getBlock() instanceof SeatBlock block ? block.seatHeight() : SEAT_HEIGHT;
        seat.setPos(pos.getX() + 0.5, pos.getY() + height, pos.getZ() + 0.5);
        seat.setYRot(player.getYRot());
        level.addFreshEntity(seat);
        if (!player.startRiding(seat)) {
            seat.discard();
            return false;
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (getPassengers().isEmpty() || !(level().getBlockState(blockPosition()).getBlock() instanceof SeatBlock))) {
            ejectPassengers();
            discard();
        }
    }

    // The rider's hips sit right on the cushion.
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return Vec3.ZERO;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    // Step off beside the stool, facing-side first, rather than onto the cushion.
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockPos stool = blockPosition();
        Direction facing = passenger.getDirection();
        for (Direction d : new Direction[] {facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite()}) {
            Vec3 spot = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), stool.relative(d), true);
            if (spot != null) {
                return spot;
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
