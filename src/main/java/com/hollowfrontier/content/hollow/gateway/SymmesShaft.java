package com.hollowfrontier.content.hollow.gateway;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Symmes' Hole, the way down: a round shaft seven blocks across, bored straight down through the crust above a return
 * gateway, where a traveller from the Overworld comes out and falls into the Hollow Earth.
 * <ul>
 * <li>Its mouth, at the top, is rimed with packed ice (the polar cold coming through with you).</li>
 * <li>Below that the wall is dark polished deepslate with a ring of sea lanterns every {@link #RING_EVERY} blocks: old
 *     Concavian work, and lights that rush past as you fall.</li>
 * <li>A cap of ordinary bedrock seals the top, so sky light (which crust rock lets through) can't pour down it: the shaft
 *     is dark but for its rings and the daylight at the bottom.</li>
 * <li>Anything solid between the crust and the gateway (a spire, a floating island) is cut through too.</li>
 * </ul>
 */
public final class SymmesShaft {
    private SymmesShaft() {}

    static final double RADIUS = 3.2;
    static final double WALL = 4.3;
    static final int RING_EVERY = 9;
    static final int ICE_DEPTH = 10;
    /** How tall the shaft is at most (the crust is about 80 thick; elsewhere, as much of the roof as there is). */
    static final int MAX_DEPTH = 80;

    /** Where a shaft starts and where it opens out of the crust's underside. */
    public record Mouth(int arrivalY, int undersideY) {}

    /** The underside of the crust over center: scanning down from the top, the first stretch of open air. */
    static int crustUnderside(ServerLevel level, BlockPos center, int from) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int open = 0;
        for (int y = from; y > center.getY() + 4; y--) {
            open = HollowLanding.open(level.getBlockState(pos.set(center.getX(), y, center.getZ()))) ? open + 1 : 0;
            if (open >= 4) {
                return y + 4;
            }
        }
        return center.getY() + 8;
    }

    /** The top of the shaft: near the top of the world, or a short way above the underside where the roof is thin. */
    static int top(ServerLevel level, int underside) {
        return Math.min(level.getMaxY() - 7, underside + MAX_DEPTH);
    }

    /** Bores the shaft over the gateway whose well is at center, and returns where a traveller comes out. */
    public static Mouth carve(ServerLevel level, BlockPos center) {
        int underside = crustUnderside(level, center, level.getMaxY() - 7);
        int top = top(level, underside);
        BlockState lining = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        BlockState ring = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState cap = Blocks.BEDROCK.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = (int) Math.ceil(WALL);
        for (int y = top + 1; y >= center.getY() + 6; y--) {
            boolean inCrust = y >= underside;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > WALL) {
                        continue;
                    }
                    pos.set(center.getX() + dx, y, center.getZ() + dz);
                    BlockState state;
                    if (y == top + 1) {
                        state = cap;
                    } else if (d <= RADIUS) {
                        if (level.getBlockState(pos).isAir()) {
                            continue;
                        }
                        state = Blocks.AIR.defaultBlockState();
                    } else if (inCrust) {
                        state = top - y < ICE_DEPTH ? ice : (top - y) % RING_EVERY == 0 ? ring : lining;
                    } else {
                        continue; // below the crust the shaft is open air; leave the land alone
                    }
                    level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                }
            }
        }
        return new Mouth(top - 1, underside);
    }

    /** For a gateway that already has its shaft: where a traveller comes out, and where the shaft leaves the crust. */
    public static Mouth mouth(ServerLevel level, BlockPos center) {
        // Up the middle of the shaft to its cap (or, with no shaft, to the crust's underside)
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(center.getX(), center.getY() + 5, center.getZ());
        while (pos.getY() < level.getMaxY() && level.getBlockState(pos).isAir()) {
            pos.move(0, 1, 0);
        }
        int arrival = pos.getY() - 2;
        // Down the wall, from the mouth, to where the lining stops: the underside
        BlockPos.MutableBlockPos wall = new BlockPos.MutableBlockPos(center.getX() + 4, arrival, center.getZ());
        while (wall.getY() > center.getY() + 6 && !HollowLanding.open(level.getBlockState(wall))) {
            wall.move(0, -1, 0);
        }
        int underside = wall.getY() + 1;
        return new Mouth(arrival, Math.min(arrival, underside));
    }
}
