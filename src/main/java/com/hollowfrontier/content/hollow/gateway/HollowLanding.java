package com.hollowfrontier.content.hollow.gateway;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where a return gateway goes inside: on the land, under the crust, never on top of it, never in lava, never in a tree.
 * <p>
 * A column is read from the top down. Leaves, logs and plants count as air (a canopy is not a floor), so the scan sees the
 * crust, the open air under it, perhaps a floating island and more air, then the land. The <b>floor</b> is the highest
 * solid top below {@link #ISLAND_FLOOR} with at least {@link #HEADROOM} blocks of open air over it: floating islands sit
 * higher than any land, and caves lie under the land, so neither is chosen. Lava rules a column out; water only makes it
 * a last resort (the gateway then stands on a platform at the surface, as phase 8 did at sea).
 * <p>
 * Around the requested x/z, columns are tried in a widening square out to {@link #SEARCH_RADIUS} for dry, level ground
 * (no more than {@link #MAX_STEP} blocks of rise across the gateway's 7x7 floor). The first good one wins; failing that,
 * the levellest dry one; failing that, water; failing that, a platform in the open air at x/z itself.
 * <p>
 * Written against {@link Terrain} so the game tests can run it on bare noise columns for many seeds.
 */
public final class HollowLanding {
    private HollowLanding() {}

    /** Open air a floor needs above it to count as the land (caves and hollows under overhangs don't have it). */
    public static final int HEADROOM = 24;
    /** Floors at or above this are floating islands (the land tops out lower, around 182). */
    public static final int ISLAND_FLOOR = 188;
    public static final int SEARCH_RADIUS = 32;
    static final int SEARCH_STEP = 4;
    /** Most the ground may rise or fall across the gateway's floor for a spot to count as level. */
    public static final int MAX_STEP = 3;

    /** Reads blocks for the search: a level, or (in tests) bare noise columns. */
    public interface Terrain {
        BlockState get(int x, int y, int z);

        int minY();

        /** The highest y to start reading from (the top of the crust). */
        int maxY();
    }

    /** What the scan found in one column. */
    public record Column(int floorY, Kind kind) {}

    public enum Kind { LAND, WATER, LAVA, NONE }

    /** The chosen spot: the block the return gateway's well sits in (its floor is at y), and what it stands on. */
    public record Landing(BlockPos pos, Kind kind, boolean level) {}

    public static Terrain of(ServerLevel level) {
        return new Terrain() {
            private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            @Override
            public BlockState get(int x, int y, int z) {
                return level.getBlockState(pos.set(x, y, z));
            }

            @Override
            public int minY() {
                return level.getMinY();
            }

            @Override
            public int maxY() {
                return level.getMaxY() - 1;
            }
        };
    }

    /** Air, or something you'd fall through or cut down: plants, leaves, logs, snow layers. */
    static boolean open(BlockState state) {
        return state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    public static Column column(Terrain terrain, int x, int z) {
        int air = 0;
        boolean roofed = false; // open air only counts under something (the crust), not above the top of the world
        for (int y = terrain.maxY(); y > terrain.minY(); y--) {
            BlockState state = terrain.get(x, y, z);
            boolean fluid = !state.getFluidState().isEmpty();
            if (!fluid && open(state)) {
                air++;
                continue;
            }
            if (roofed && air >= HEADROOM && y < ISLAND_FLOOR) {
                if (fluid) {
                    return new Column(y, state.getFluidState().is(FluidTags.LAVA) ? Kind.LAVA : Kind.WATER);
                }
                return new Column(y, Kind.LAND);
            }
            air = 0;
            roofed = true;
        }
        return new Column(Integer.MIN_VALUE, Kind.NONE);
    }

    public static Landing find(Terrain terrain, int x, int z) {
        Map<Long, Column> seen = new HashMap<>();
        Landing bestDry = null;
        int bestDryStep = Integer.MAX_VALUE;
        Landing water = null;
        for (int ring = 0; ring <= SEARCH_RADIUS; ring += SEARCH_STEP) {
            for (int dx = -ring; dx <= ring; dx += SEARCH_STEP) {
                for (int dz = -ring; dz <= ring; dz += SEARCH_STEP) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue; // only this ring's edge; the inside was tried already
                    }
                    int cx = x + dx;
                    int cz = z + dz;
                    Column center = cached(terrain, seen, cx, cz);
                    if (center.kind() == Kind.WATER && water == null) {
                        water = new Landing(new BlockPos(cx, center.floorY(), cz), Kind.WATER, true);
                    }
                    if (center.kind() != Kind.LAND) {
                        continue;
                    }
                    int step = step(terrain, seen, cx, cz, center.floorY());
                    if (step < 0) {
                        continue; // lava or a drop at the edge
                    }
                    Landing here = new Landing(new BlockPos(cx, center.floorY(), cz), Kind.LAND, step <= MAX_STEP);
                    if (here.level()) {
                        return here;
                    }
                    if (step < bestDryStep) {
                        bestDry = here;
                        bestDryStep = step;
                    }
                }
            }
        }
        if (bestDry != null) {
            return bestDry;
        }
        if (water != null) {
            return water;
        }
        // Nothing at all to stand on: a platform in the open air at x/z, under the crust
        return new Landing(new BlockPos(x, terrain.minY() + 128, z), Kind.NONE, false);
    }

    /** Most rise or fall across a 7x7 floor (corners and edge middles); -1 if any of it is lava, water or a cliff. */
    static int step(Terrain terrain, Map<Long, Column> seen, int x, int z, int floorY) {
        int worst = 0;
        for (int dx = -3; dx <= 3; dx += 3) {
            for (int dz = -3; dz <= 3; dz += 3) {
                Column c = cached(terrain, seen, x + dx, z + dz);
                if (c.kind() != Kind.LAND) {
                    return -1;
                }
                worst = Math.max(worst, Math.abs(c.floorY() - floorY));
            }
        }
        return worst > 12 ? -1 : worst;
    }

    private static Column cached(Terrain terrain, Map<Long, Column> seen, int x, int z) {
        return seen.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> column(terrain, x, z));
    }

    /** The underside of whatever is over pos (the crust, or a spire): the lowest solid block above the open air. */
    public static Optional<Integer> ceilingAbove(Terrain terrain, BlockPos pos) {
        for (int y = pos.getY() + 1; y <= terrain.maxY(); y++) {
            BlockState state = terrain.get(pos.getX(), y, pos.getZ());
            if (!open(state) || !state.getFluidState().isEmpty()) {
                return Optional.of(y);
            }
        }
        return Optional.empty();
    }
}
