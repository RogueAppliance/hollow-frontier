package com.hollowfrontier.content.hollow.sea;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Idle swimming that keeps to a depth. Vanilla's random swimming picks a spot up to seven blocks up or down and the
 * surface turns every upward pick back, so in practice everything ends up idling just under it (the 2026-11-09 playtest).
 * This takes vanilla's spot for where to go and sets how deep itself: somewhere in the creature's own band of the water
 * column there, at least {@code minBelowSurface} under the surface and, if {@code maxAboveBed} isn't negative, no more
 * than that over the bed. {@code surfaceChance} of the time it comes right up instead (a Plesiosaur's neck out of the
 * water). Water too shallow for the band puts it as low as it can go.
 */
public class SwimAtDepthGoal extends RandomSwimmingGoal {
    private final int minBelowSurface;
    private final int maxAboveBed;
    private final float surfaceChance;

    public SwimAtDepthGoal(PathfinderMob mob, double speed, int interval, int minBelowSurface, int maxAboveBed, float surfaceChance) {
        super(mob, speed, interval);
        this.minBelowSurface = minBelowSurface;
        this.maxAboveBed = maxAboveBed;
        this.surfaceChance = surfaceChance;
    }

    @Override
    protected @Nullable Vec3 getPosition() {
        Vec3 spot = super.getPosition();
        if (spot == null) {
            spot = mob.position(); // nowhere to go sideways: at least go to the right depth here
        }
        return atDepth(mob.level(), BlockPos.containing(spot), minBelowSurface, maxAboveBed, surfaceChance, mob.getRandom());
    }

    /**
     * A point in the water column through {@code at}, in the band described above; null if there's no water within twelve
     * blocks above or below {@code at}.
     */
    public static @Nullable Vec3 atDepth(LevelReader level, BlockPos at, int minBelowSurface, int maxAboveBed, float surfaceChance, RandomSource random) {
        BlockPos.MutableBlockPos pos = at.mutable();
        for (int i = 0; i < 12 && !SeaLife.isWater(level, pos); i++) {
            pos.move(0, -1, 0);
        }
        if (!SeaLife.isWater(level, pos)) {
            pos.set(at);
            for (int i = 0; i < 12 && !SeaLife.isWater(level, pos); i++) {
                pos.move(0, 1, 0);
            }
            if (!SeaLife.isWater(level, pos)) {
                return null;
            }
        }
        int bed = SeaLife.bedBelow(level, pos).getY();
        int surface = pos.getY();
        while (surface - bed < 96 && SeaLife.isWater(level, pos.setY(surface + 1))) {
            surface++;
        }
        int low = Math.min(bed + 1, surface);
        int high = surface - minBelowSurface;
        if (maxAboveBed >= 0) {
            high = Math.min(high, bed + maxAboveBed);
        }
        high = Math.max(high, low);
        int y = random.nextFloat() < surfaceChance ? surface : low + random.nextInt(high - low + 1);
        return new Vec3(at.getX() + 0.5, y, at.getZ() + 0.5);
    }
}
