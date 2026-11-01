package com.hollowfrontier.content.frontier.townsfolk;

import java.util.EnumSet;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.saloon.Barmaid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

/**
 * A townsperson's day (24c). By day they keep to where they work (their home: where they first stood, their shop or
 * house); in the evening they go down to the saloon, if their town has one within {@link #SALOON_REACH} of home; at night
 * they go home and to bed, if there's a free bed within {@link #BED_REACH} of home, or stand at home if not. They wake
 * at dawn, or when hurt or spoken to. A raid, outlaws or a fright come first (their goals rank higher), and the barmaid,
 * whose day is the saloon, has none of this.
 */
public final class Routine {
    private Routine() {}

    public enum Phase { WORK, EVENING, NIGHT }

    public static final int SALOON_REACH = 64;
    public static final int BED_REACH = 10;
    /** How near the place for the phase a townsperson keeps (blocks). */
    public static final int WORK_RANGE = 10, SALOON_RANGE = 6, HOME_RANGE = 3;

    /** The part of the day: work until 11500, the evening until 14500, the night until 23500, then work again. */
    public static Phase phase(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t < 11500 ? Phase.WORK : t < 14500 ? Phase.EVENING : t < 23500 ? Phase.NIGHT : Phase.WORK;
    }

    /** The saloon for a home: where the nearest barmaid within reach stands. */
    public static Optional<BlockPos> saloon(Level level, BlockPos home) {
        return level.getEntitiesOfClass(Barmaid.class, new AABB(home).inflate(SALOON_REACH), Barmaid::isAlive).stream()
                .min(java.util.Comparator.comparingDouble(b -> b.distanceToSqr(home.getX(), home.getY(), home.getZ())))
                .map(Barmaid::blockPosition);
    }

    /** A free bed (its head) near a home, the nearest first. */
    public static Optional<BlockPos> bed(Level level, BlockPos home) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(home.offset(-BED_REACH, -3, -BED_REACH), home.offset(BED_REACH, 6, BED_REACH))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD && !state.getValue(BedBlock.OCCUPIED)) {
                double distance = pos.distSqr(home);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** Where a townsperson should be in a phase, and how near it. */
    public record Place(BlockPos pos, int range, boolean bed) {}

    public static Place place(Level level, BlockPos home, Phase phase) {
        return switch (phase) {
            case WORK -> new Place(home, WORK_RANGE, false);
            case EVENING -> saloon(level, home).map(saloon -> new Place(saloon, SALOON_RANGE, false)).orElse(new Place(home, WORK_RANGE, false));
            case NIGHT -> bed(level, home).map(bed -> new Place(bed, 1, true)).orElse(new Place(home, HOME_RANGE, false));
        };
    }

    /** The goal: walks to the place for the time of day (looked up again when the phase turns), and sleeps there at night. */
    public static final class RoutineGoal extends Goal {
        private final Townsfolk folk;
        private @Nullable Phase phase;
        private @Nullable Place place;

        public RoutineGoal(Townsfolk folk) {
            this.folk = folk;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (folk.isTrading() || folk.getWorkplace() == null || (folk.tickCount + folk.getId()) % 20 != 0) {
                return false;
            }
            Phase now = phase(folk.level().getOverworldClockTime());
            if (now != phase || place == null) {
                phase = now;
                place = place(folk.level(), folk.getWorkplace(), now);
                folk.setHomeTo(place.bed() ? folk.getWorkplace() : place.pos(), place.bed() ? BED_REACH : place.range());
                if (now != Phase.NIGHT && folk.isSleeping()) {
                    folk.stopSleeping();
                }
            }
            if (folk.isSleeping()) {
                return false;
            }
            if (place.bed() && folk.blockPosition().closerThan(place.pos(), 2.5)) {
                if (folk.level().getBlockState(place.pos()).getBlock() instanceof BedBlock) {
                    folk.startSleeping(place.pos());
                }
                return false;
            }
            return !folk.blockPosition().closerThan(place.pos(), place.range() + 1);
        }

        @Override
        public void start() {
            folk.getNavigation().moveTo(place.pos().getX() + 0.5, place.pos().getY(), place.pos().getZ() + 0.5, 0.5);
        }

        @Override
        public boolean canContinueToUse() {
            return !folk.getNavigation().isDone() && place != null && !folk.blockPosition().closerThan(place.pos(), place.bed() ? 1.5 : place.range());
        }
    }
}
