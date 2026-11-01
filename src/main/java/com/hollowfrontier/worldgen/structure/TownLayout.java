package com.hollowfrontier.worldgen.structure;

import static com.hollowfrontier.worldgen.structure.TownBlueprints.hangingLantern;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.log;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.s;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.slab;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.stairs;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.water;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.registry.ModBlocks;

import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * How a town is laid out, like a vanilla village grown from its centre. The start piece is a <b>town square</b>
 * (39 x 39) with streets leading off two to four of its sides; around the square stand the buildings every town has
 * exactly one of (the law or the bank, the hotel, the saloon, the gunsmith) and on a side with no street, a grand landmark
 * (courthouse or church). <b>Streets</b> (follow the terrain) have shops and homes down both sides and something at
 * their far end: a chapel, the water tower, the railroad depot, or a <b>lane</b> of homes running on out to the edge
 * of town (corrals, a windmill, Boot Hill).
 * <p>
 * Connectors: the square's street exits and each street's square end are {@link #ROAD}; every other join is a lot
 * ({@link TownBlueprints#LOT}) taking a building's front ({@link TownBlueprints#FRONT}), lanes included. Pool names come
 * from the {@link TownStyle}, so each style's pieces only ever draw that style's buildings. {@link #CENTER} marks the
 * square's middle (for /place jigsaw and the photo booth). Depth: square 0, streets 1, their buildings, lanes and farm
 * tracks 2, lane buildings, outskirts and wheat fields 3.
 */
public final class TownLayout {
    private TownLayout() {}

    public static final String ROAD = "hollowfrontier:town_road";
    public static final String CENTER = "hollowfrontier:town_center";
    public static final int SQUARE = 39;
    public static final int MID = SQUARE / 2;
    /** Positions of the two lots on a side of the square, either side of its street (or of its middle). */
    static final int LOT_A = 7;
    static final int LOT_B = 31;
    static final int EXIT_HALF = 4;
    /** Lots along a street start this far in, clear of the square's corner buildings, then every 12. */
    static final int FIRST_LOT = 24;
    static final int LANE_LENGTH = 26;

    // Pool short names; TownStyle.pool turns them into ids
    public static final String SQUARES = "squares";
    public static final String STREETS = "streets";
    public static final String CIVIC = "civic";
    public static final String COMMERCE = "commerce";
    public static final String SALOON = "saloon";
    public static final String SHOPS = "shops";
    /** The square's gunsmith lot: he alone sells guns, so every town has one (more may turn up among the shops). */
    public static final String GUNSMITH = "gunsmith";
    /** The square's post office lot: its postmaster holds the Polar Survey's letter (32d), so every town has one too. */
    public static final String POST_OFFICE = "post_office";
    public static final String HOMES = "homes";
    public static final String LANDMARKS = "square_landmarks";
    public static final String STREET_ENDS = "street_ends";
    public static final String LANE_LOTS = "lane_lots";
    public static final String OUTSKIRTS = "outskirts";
    /**
     * A street's last east lot: always a cart track back to a wheat field ({@link #FIELDS}), so every town (two streets
     * or more) has a field for crop circles. The field is too wide for a lot, so it stands behind the lots beside it.
     */
    public static final String FARMS = "farms";
    public static final String FIELDS = "fields";
    /** The track on out of town from each end of the depot's (24a): {@link #rail}. */
    public static final String RAILROAD = "railroad";
    public static final String RAIL = "hollowfrontier:town_rail";
    static final int RAIL_LENGTH = 28;
    /** The cart track's length: past the deepest street building (12), so the field behind clears its neighbours' lots. */
    static final int FARM_TRACK = 14;

    /** One side of the square: a street out with a lot either side, or no street, with two lots or one grand lot in the middle. */
    record Side(Direction dir, boolean exit, String lotA, String lotB, String grand) {
        static Side exit(Direction dir, String a, String b) {
            return new Side(dir, true, a, b, null);
        }

        static Side lots(Direction dir, String a, String b) {
            return new Side(dir, false, a, b, null);
        }

        static Side grand(Direction dir) {
            return new Side(dir, false, null, null, LANDMARKS);
        }
    }

    enum Centerpiece { BANDSTAND, WELL }

    /** Square, street and lane pieces for one style (6 squares: 3 layouts, each with a bandstand or a well). */
    static List<Blueprint> pieces(TownStyle style) {
        List<Blueprint> out = new ArrayList<>();
        for (Centerpiece piece : Centerpiece.values()) {
            String suffix = piece == Centerpiece.BANDSTAND ? "_bandstand" : "_well";
            // Crossroads: four streets out
            out.add(square(style, "square_cross" + suffix, piece,
                    Side.exit(Direction.NORTH, CIVIC, COMMERCE), Side.exit(Direction.EAST, SALOON, GUNSMITH),
                    Side.exit(Direction.SOUTH, SHOPS, POST_OFFICE), Side.exit(Direction.WEST, SHOPS, HOMES)));
            // Three streets, and a landmark looking down the square
            out.add(square(style, "square_tee" + suffix, piece,
                    Side.exit(Direction.NORTH, CIVIC, COMMERCE), Side.exit(Direction.EAST, SALOON, GUNSMITH),
                    Side.exit(Direction.SOUTH, SHOPS, POST_OFFICE), Side.grand(Direction.WEST)));
            // One street through, a landmark on one side and the law and the hotel across from it
            out.add(square(style, "square_pair" + suffix, piece,
                    Side.exit(Direction.NORTH, SALOON, GUNSMITH), Side.grand(Direction.EAST),
                    Side.exit(Direction.SOUTH, POST_OFFICE, HOMES), Side.lots(Direction.WEST, CIVIC, COMMERCE)));
        }
        out.add(street(style, "street_long", 56, new String[] {SHOPS, HOMES, SHOPS}, new String[] {HOMES, SHOPS, FARMS}));
        out.add(street(style, "street_short", 44, new String[] {SHOPS, HOMES}, new String[] {HOMES, FARMS}));
        out.add(lane(style));
        out.add(farmTrack(style));
        out.add(rail(style, RailEnd.SPUR));
        out.add(rail(style, RailEnd.BOXCAR));
        out.add(rail(style, RailEnd.TANK));
        return out;
    }

    // ---------------------------------------------------------------- the square

    // Template position of a point on a side: t along the side, `in` blocks in from the edge.
    private static int[] at(Direction side, int t, int in) {
        return switch (side) {
            case NORTH -> new int[] {t, in};
            case SOUTH -> new int[] {t, SQUARE - 1 - in};
            case WEST -> new int[] {in, t};
            default -> new int[] {SQUARE - 1 - in, t};
        };
    }

    private static FrontAndTop out(Direction side) {
        return switch (side) {
            case NORTH -> FrontAndTop.NORTH_UP;
            case SOUTH -> FrontAndTop.SOUTH_UP;
            case WEST -> FrontAndTop.WEST_UP;
            default -> FrontAndTop.EAST_UP;
        };
    }

    static Blueprint square(TownStyle style, String name, Centerpiece centerpiece, Side... sides) {
        Blueprint b = new Blueprint(style.template(name));
        int last = SQUARE - 1;
        b.air(0, 1, 0, last, 16, last);
        b.fill(0, 0, 0, last, 0, last, Blocks.COARSE_DIRT);
        // The plaza's ground, worn into patches: gravel where the wagons stand, beaten paths, the rest hard-packed
        // (round blobs about a scatter of points, ragged at the edges, and a few lone stones)
        for (int i = 0; i < 22; i++) {
            int px = hash(i, 11) % SQUARE, pz = hash(i, 23) % SQUARE, r = 2 + hash(i, 37) % 3;
            Block worn = i % 3 == 0 ? Blocks.GRAVEL : Blocks.DIRT_PATH;
            for (int x = Math.max(0, px - r); x <= Math.min(last, px + r); x++) {
                for (int z = Math.max(0, pz - r); z <= Math.min(last, pz + r); z++) {
                    double d = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));
                    if (d < r - 1 || d < r + 0.5 && hash(x, z + 31 * i) % 3 != 0) {
                        b.set(x, 0, z, worn);
                    }
                }
            }
        }
        for (int x = 0; x <= last; x++) {
            for (int z = 0; z <= last; z++) {
                if (hash(x, z + 977) % 100 < 3) {
                    b.set(x, 0, z, Blocks.GRAVEL);
                }
            }
        }
        // Wagon roads across the plaza, a paved ring round the middle
        b.fill(MID - 2, 0, 0, MID + 2, 0, last, Blocks.DIRT_PATH);
        b.fill(0, 0, MID - 2, last, 0, MID + 2, Blocks.DIRT_PATH);
        b.walls(MID - 6, 0, MID - 6, MID + 6, 0, MID + 6, s(Blocks.STONE_BRICKS));
        b.walls(MID - 5, 0, MID - 5, MID + 5, 0, MID + 5, s(Blocks.GRAVEL));
        BlockState boardwalk = s(Blocks.SPRUCE_PLANKS);
        for (Side side : sides) {
            // Boardwalk along the side (two wide), broken where its street comes in
            for (int t = 0; t < SQUARE; t++) {
                if (side.exit() && Math.abs(t - MID) <= EXIT_HALF) {
                    continue;
                }
                for (int in = 0; in < 2; in++) {
                    int[] p = at(side.dir(), t, in);
                    b.set(p[0], 0, p[1], boardwalk);
                }
            }
            if (side.exit()) {
                int[] p = at(side.dir(), MID, 0);
                b.jigsaw(p[0], 0, p[1], out(side.dir()), ROAD, ROAD, style.pool(STREETS), s(Blocks.DIRT_PATH));
                // Hitching rails either side of the way in
                for (int t : new int[] {MID - EXIT_HALF - 1, MID + EXIT_HALF + 1}) {
                    int[] r = at(side.dir(), t, 3);
                    b.set(r[0], 1, r[1], Blocks.SPRUCE_FENCE);
                }
            }
            if (side.lotA() != null) {
                int[] a = at(side.dir(), LOT_A, 0);
                b.jigsaw(a[0], 0, a[1], out(side.dir()), TownBlueprints.LOT, TownBlueprints.FRONT, style.pool(side.lotA()), boardwalk);
                int[] c = at(side.dir(), LOT_B, 0);
                b.jigsaw(c[0], 0, c[1], out(side.dir()), TownBlueprints.LOT, TownBlueprints.FRONT, style.pool(side.lotB()), boardwalk);
            }
            if (side.grand() != null) {
                int[] g = at(side.dir(), MID, 0);
                b.jigsaw(g[0], 0, g[1], out(side.dir()), TownBlueprints.LOT, TownBlueprints.FRONT, style.pool(side.grand()), s(Blocks.STONE_BRICKS));
            }
        }
        // Lamps at the four inner corners
        for (int x : new int[] {3, last - 3}) {
            for (int z : new int[] {3, last - 3}) {
                lampPost(b, x, z);
            }
        }
        // North-west quarter: the flagpole and a Bounty Board
        b.fill(8, 1, 8, 8, 9, 8, Blocks.SPRUCE_FENCE);
        b.set(8, 10, 8, Blocks.BANNER.red());
        b.set(10, 1, 8, ModBlocks.BOUNTY_BOARD.get());
        // North-east: a horse trough and hay
        b.set(last - 9, 1, 8, water(3));
        b.set(last - 8, 1, 8, water(3));
        b.set(last - 7, 1, 8, Blocks.HAY_BLOCK);
        b.fill(last - 10, 1, 10, last - 7, 1, 10, Blocks.SPRUCE_FENCE);
        // South-east: the fire pit (town cats gather at dusk)
        firePit(b, last - 8, last - 8);
        // South-west: a wagon left by the boardwalk, and some freight
        wagon(b, 6, last - 10);
        b.set(10, 1, last - 7, Blocks.BARREL);
        b.set(10, 1, last - 8, Blocks.BARREL);
        b.set(10, 2, last - 7, Blocks.BARREL);
        // 23: shade trees on the diagonals, each with a bench under it facing the middle
        for (int[] t : new int[][] {{11, 11, -1, -1}, {last - 11, 11, 1, -1}, {11, last - 11, -1, 1}, {last - 11, last - 11, 1, 1}}) {
            shadeTree(b, t[0], t[1]);
            TownDecor.bench(b, t[0] - t[2] * 2, 1, t[1], t[2] < 0 ? Direction.WEST : Direction.EAST);
        }
        // Market stalls along the north side, one by the wagon on the south; the cannon and its plaque; the stage
        stall(b, 22, 3, false, DyeColor.RED, Blocks.PUMPKIN, Blocks.MELON, TownBlueprints.openBarrel());
        stall(b, 26, 3, false, DyeColor.BLUE, Blocks.HAY_BLOCK, TownBlueprints.openBarrel().getBlock(), s(Blocks.COMPOSTER));
        stall(b, 12, last - 5, true, DyeColor.GREEN, Blocks.BARREL, Blocks.HAY_BLOCK, TownBlueprints.openBarrel());
        b.chest(13, 1, last - 3, Direction.NORTH, ModChestLoot.FREIGHT);
        b.entity(com.hollowfrontier.registry.ModEntities.TOWNSFOLK.get(), 23, 1, 4, TownBlueprints.tag("folk", "prospector"));
        cannon(b, 3, 12);
        stagecoach(b, last - 6, 23);
        BlockState middle = switch (centerpiece) {
            case BANDSTAND -> bandstand(b);
            case WELL -> well(b);
        };
        b.jigsaw(MID, 0, MID, FrontAndTop.UP_NORTH, CENTER, CENTER, "minecraft:empty", middle);
        return b;
    }

    // Octagonal bandstand: a raised platform with steps on all sides, railings, posts and a peaked roof. Returns its floor.
    private static BlockState bandstand(Blueprint b) {
        int lo = MID - 3, hi = MID + 3;
        b.fill(lo, 0, lo, hi, 0, hi, Blocks.STONE_BRICKS);
        b.fill(lo, 1, lo, hi, 1, hi, Blocks.SPRUCE_PLANKS);
        for (int[] c : new int[][] {{lo, lo}, {hi, lo}, {lo, hi}, {hi, hi}}) {
            b.set(c[0], 1, c[1], Blocks.AIR);
        }
        // Railing round the edge, open at the four steps
        for (int i = lo + 1; i < hi; i++) {
            if (i != MID) {
                b.set(i, 2, lo, Blocks.SPRUCE_FENCE);
                b.set(i, 2, hi, Blocks.SPRUCE_FENCE);
                b.set(lo, 2, i, Blocks.SPRUCE_FENCE);
                b.set(hi, 2, i, Blocks.SPRUCE_FENCE);
            }
        }
        b.set(MID, 1, lo - 1, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, Half.BOTTOM));
        b.set(MID, 1, hi + 1, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.BOTTOM));
        b.set(lo - 1, 1, MID, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, Half.BOTTOM));
        b.set(hi + 1, 1, MID, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, Half.BOTTOM));
        for (int[] c : new int[][] {{lo + 1, lo + 1}, {hi - 1, lo + 1}, {lo + 1, hi - 1}, {hi - 1, hi - 1}}) {
            b.fill(c[0], 2, c[1], c[0], 4, c[1], Blocks.SPRUCE_FENCE);
        }
        // Roof: an octagon of slabs stepping up to a finial
        b.fill(lo, 5, lo, hi, 5, hi, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        for (int[] c : new int[][] {{lo, lo}, {hi, lo}, {lo, hi}, {hi, hi}}) {
            b.set(c[0], 5, c[1], Blocks.AIR);
        }
        b.fill(lo + 1, 5, lo + 1, hi - 1, 5, hi - 1, Blocks.DARK_OAK_PLANKS);
        b.fill(lo + 1, 6, lo + 1, hi - 1, 6, hi - 1, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        b.fill(lo + 2, 6, lo + 2, hi - 2, 6, hi - 2, Blocks.DARK_OAK_PLANKS);
        b.fill(lo + 2, 7, lo + 2, hi - 2, 7, hi - 2, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        b.set(MID, 7, MID, Blocks.DARK_OAK_PLANKS);
        b.set(MID, 8, MID, Blocks.SPRUCE_FENCE);
        b.set(MID, 9, MID, Blocks.LIGHTNING_ROD.waxed().unaffected());
        b.set(MID, 4, MID, hangingLantern());
        // Music stands for the band (no piano: towns have no always-on music)
        b.set(MID + 1, 2, MID + 1, Blocks.NOTE_BLOCK);
        b.set(MID - 1, 2, MID + 1, Blocks.NOTE_BLOCK);
        return s(Blocks.STONE_BRICKS);
    }

    // Stone well with a little roof, a bucket rail and benches round it. Returns the block under the middle (water).
    private static BlockState well(Blueprint b) {
        int lo = MID - 2, hi = MID + 2;
        b.fill(lo, 0, lo, hi, 0, hi, Blocks.COBBLESTONE);
        b.walls(lo, 1, lo, hi, 1, hi, s(Blocks.COBBLESTONE));
        b.fill(lo + 1, 0, lo + 1, hi - 1, 0, hi - 1, Blocks.WATER);
        b.fill(lo + 1, 1, lo + 1, hi - 1, 1, hi - 1, Blocks.AIR);
        for (int[] c : new int[][] {{lo, lo}, {hi, lo}, {lo, hi}, {hi, hi}}) {
            b.fill(c[0], 2, c[1], c[0], 3, c[1], Blocks.SPRUCE_FENCE);
        }
        b.fill(lo, 4, lo, hi, 4, hi, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.fill(lo + 1, 4, lo + 1, hi - 1, 4, hi - 1, Blocks.SPRUCE_PLANKS);
        b.fill(lo + 1, 5, lo + 1, hi - 1, 5, hi - 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.set(MID, 3, MID, Blocks.IRON_CHAIN);
        // Benches facing the well
        b.set(MID, 1, lo - 3, stairs(Blocks.OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
        b.set(MID, 1, hi + 3, stairs(Blocks.OAK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        b.set(lo - 3, 1, MID, stairs(Blocks.OAK_STAIRS, Direction.WEST, Half.BOTTOM));
        b.set(hi + 3, 1, MID, stairs(Blocks.OAK_STAIRS, Direction.EAST, Half.BOTTOM));
        return s(Blocks.WATER);
    }

    // ---------------------------------------------------------------- streets and lanes

    /**
     * A street, 9 wide (boardwalk 2, dirt road 5, boardwalk 2) and {@code length} long, from the square (z 0) outward.
     * Lots from FIRST_LOT every 12 down each side (west lots face west at x 0, east lots east at x 8), and its far
     * end opens onto a street-end piece. Lamp posts on one side and telegraph poles on the other between the lots.
     */
    static Blueprint street(TownStyle style, String name, int length, String[] westLots, String[] eastLots) {
        Blueprint b = new Blueprint(style.template(name));
        int last = length - 1;
        b.air(0, 1, 0, 8, 12, last);
        b.fill(0, 0, 0, 1, 0, last, Blocks.SPRUCE_PLANKS);
        b.fill(7, 0, 0, 8, 0, last, Blocks.SPRUCE_PLANKS);
        b.fill(2, 0, 0, 6, 0, last, Blocks.DIRT_PATH);
        // Wheel ruts, scuffed patches and tumbleweeds
        for (int z = 0; z < length; z++) {
            if (z % 9 == 4) b.set(3, 0, z, Blocks.COARSE_DIRT);
            if (z % 11 == 7) b.set(5, 0, z, Blocks.GRAVEL);
            if (z % 13 == 2) {
                b.set(2, 0, z, Blocks.COARSE_DIRT);
                b.set(2, 1, z, Blocks.DEAD_BUSH);
            }
        }
        // Between the lots: a lamp on the west boardwalk, a telegraph pole on the east, rails and freight
        for (int z = FIRST_LOT - 6; z < length - 2; z += 12) {
            lampPost(b, 1, z);
            telegraphPole(b, 7, z);
            b.fill(2, 1, z - 2, 2, 1, z - 1, Blocks.SPRUCE_FENCE);
            b.fill(6, 1, z + 2, 6, 1, z + 3, Blocks.SPRUCE_FENCE);
            b.set(1, 1, z + 2, Blocks.BARREL);
        }
        b.set(6, 1, length / 2 + 1, water(3));
        b.set(6, 1, length / 2 + 2, water(3));
        b.set(7, 1, 5, Blocks.HAY_BLOCK);
        b.set(7, 2, 5, Blocks.HAY_BLOCK);
        b.set(1, 1, 7, Blocks.BARREL);
        welcomeArch(b, last - 2);
        BlockState boardwalk = s(Blocks.SPRUCE_PLANKS);
        for (int i = 0; i < westLots.length; i++) {
            b.jigsaw(0, 0, FIRST_LOT + 12 * i, FrontAndTop.WEST_UP, TownBlueprints.LOT, TownBlueprints.FRONT, style.pool(westLots[i]), boardwalk);
        }
        for (int i = 0; i < eastLots.length; i++) {
            b.jigsaw(8, 0, FIRST_LOT + 12 * i, FrontAndTop.EAST_UP, TownBlueprints.LOT, TownBlueprints.FRONT, style.pool(eastLots[i]), boardwalk);
        }
        b.jigsaw(4, 0, 0, FrontAndTop.NORTH_UP, ROAD, ROAD, "minecraft:empty", s(Blocks.DIRT_PATH));
        b.jigsaw(4, 0, last, FrontAndTop.SOUTH_UP, TownBlueprints.LOT, TownBlueprints.FRONT, style.pool(STREET_ENDS), s(Blocks.DIRT_PATH));
        return b;
    }

    /**
     * The welcome arch across a street at z (24a): log posts on the boardwalks, a beam, and a board hung under it, high
     * enough to ride under, with a sign each way: "WELCOME TO" the town's name and population coming in, "YOU ARE
     * LEAVING" it going out. The name lines are filled in as the town is built (TownNameProcessor).
     */
    static void welcomeArch(Blueprint b, int z) {
        for (int x : new int[] {0, 8}) {
            b.fill(x, 1, z, x, 6, z, log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        }
        b.fill(0, 7, z, 8, 7, z, log(Blocks.SPRUCE_LOG, Direction.Axis.X));
        b.fill(0, 8, z, 8, 8, z, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.fill(2, 5, z, 6, 6, z, Blocks.SPRUCE_PLANKS);
        b.set(1, 6, z, TownDecor.lamp(true));
        b.set(7, 6, z, TownDecor.lamp(true));
        b.wallSign(4, 5, z + 1, Blocks.SPRUCE_WALL_SIGN, Direction.SOUTH, DyeColor.BLACK, false,
                "sign.hollowfrontier.welcome", TownNameProcessor.NAME, TownNameProcessor.POPULATION);
        b.wallSign(4, 5, z - 1, Blocks.SPRUCE_WALL_SIGN, Direction.NORTH, DyeColor.BLACK, false,
                "sign.hollowfrontier.leaving", TownNameProcessor.NAME, "sign.hollowfrontier.come_again");
    }

    /** A back lane (5 wide, LANE_LENGTH long) off the end of a street: homes and gardens either side, the outskirts at its end. */
    static Blueprint lane(TownStyle style) {
        Blueprint b = new Blueprint(style.template("lane"));
        int last = LANE_LENGTH - 1;
        b.air(0, 1, 0, 4, 10, last);
        b.fill(0, 0, 0, 4, 0, last, Blocks.COARSE_DIRT);
        b.fill(1, 0, 0, 3, 0, last, Blocks.DIRT_PATH);
        // Split-rail fences between the yards, a lamp halfway
        for (int z : new int[] {1, 13, 23}) {
            b.fill(0, 1, z, 0, 1, z + 2, Blocks.SPRUCE_FENCE);
            b.fill(4, 1, z, 4, 1, z + 2, Blocks.SPRUCE_FENCE);
        }
        lampPost(b, 0, 14);
        b.set(4, 1, 15, Blocks.HAY_BLOCK);
        b.set(3, 1, 24, Blocks.DEAD_BUSH);
        BlockState verge = s(Blocks.COARSE_DIRT);
        for (int z : new int[] {7, 19}) {                               // full lots (TownLots) span z - 5..z + 6, clear of the outskirts
            b.jigsaw(0, 0, z, FrontAndTop.WEST_UP, TownBlueprints.LOT, TownBlueprints.FRONT, style.pool(LANE_LOTS), verge);
            b.jigsaw(4, 0, z, FrontAndTop.EAST_UP, TownBlueprints.LOT, TownBlueprints.FRONT, style.pool(LANE_LOTS), verge);
        }
        TownBlueprints.front(b, 2, s(Blocks.DIRT_PATH));
        b.jigsaw(2, 0, last, FrontAndTop.SOUTH_UP, TownBlueprints.LOT, TownBlueprints.FRONT, style.pool(OUTSKIRTS), s(Blocks.DIRT_PATH));
        return b;
    }

    /** A cart track (5 wide, FARM_TRACK long) from a street's last lot back between rail fences to the wheat field. */
    static Blueprint farmTrack(TownStyle style) {
        Blueprint b = new Blueprint(style.template("farm_track"));
        int last = FARM_TRACK - 1;
        b.air(0, 1, 0, 4, 6, last);
        b.fill(0, 0, 0, 4, 0, last, Blocks.COARSE_DIRT);
        b.fill(1, 0, 0, 1, 0, last, Blocks.DIRT_PATH);                    // wheel ruts
        b.fill(3, 0, 0, 3, 0, last, Blocks.DIRT_PATH);
        b.fill(0, 1, 2, 0, 1, last, Blocks.SPRUCE_FENCE);
        b.fill(4, 1, 2, 4, 1, last, Blocks.SPRUCE_FENCE);
        b.set(4, 1, 1, Blocks.HAY_BLOCK);
        TownBlueprints.front(b, 2, s(Blocks.DIRT_PATH));
        b.jigsaw(2, 0, last, FrontAndTop.SOUTH_UP, TownBlueprints.LOT, TownBlueprints.FRONT, style.pool(FIELDS), s(Blocks.DIRT_PATH));
        return b;
    }

    /** What stands at the far end of a stretch of track out of town. */
    enum RailEnd { SPUR, BOXCAR, TANK }

    /**
     * The line on out of town from an end of the depot's track (24a): RAIL_LENGTH of rail on sleepers and ballast, with
     * telegraph poles along it, ending at a buffer stop; or at a boxcar left on the siding (a chest of freight inside, its
     * door open), or by a water tank with its spout over the rail. Drawn heading south from its connector at (2, 0, 0);
     * the same pieces serve every town style.
     */
    static Blueprint rail(TownStyle style, RailEnd end) {
        Blueprint b = new Blueprint(style.template("rail_" + end.name().toLowerCase(java.util.Locale.ROOT)));
        int last = RAIL_LENGTH - 1;
        b.air(0, 1, 0, 6, 12, last);
        b.fill(0, 0, 0, 4, 0, last, Blocks.COARSE_DIRT);
        b.fill(1, 0, 0, 3, 0, last, Blocks.GRAVEL);
        BlockState rail = Blocks.RAIL.defaultBlockState().setValue(net.minecraft.world.level.block.RailBlock.SHAPE,
                net.minecraft.world.level.block.state.properties.RailShape.NORTH_SOUTH);
        for (int z = 0; z < last; z++) {
            if (z % 2 == 0) {
                b.fill(1, 0, z, 3, 0, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X)); // sleepers
            }
            b.set(2, 1, z, rail);
        }
        for (int z = 6; z < last - 2; z += 9) {
            if (end != RailEnd.TANK || z < last - 10) {
                telegraphPole(b, 5, z);
            }
        }
        // The buffer stop
        b.fill(1, 1, last, 3, 1, last, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
        b.fill(1, 2, last, 1, 2, last, Blocks.SPRUCE_FENCE);
        b.fill(3, 2, last, 3, 2, last, Blocks.SPRUCE_FENCE);
        switch (end) {
            case SPUR -> {
                b.fill(4, 1, last, 4, 2, last, Blocks.SPRUCE_FENCE);
                b.set(4, 3, last, Blocks.LANTERN);
            }
            case BOXCAR -> {
                // A red boxcar on the rail, its door slid open on the west side, freight inside
                int z0 = last - 9, z1 = last - 3, mid = (z0 + z1) / 2;
                b.fill(1, 1, z0, 3, 1, z1, slab(Blocks.MANGROVE_SLAB, SlabType.TOP));
                b.walls(1, 2, z0, 3, 4, z1, s(Blocks.MANGROVE_PLANKS));
                b.fill(2, 2, z0 + 1, 2, 4, z1 - 1, Blocks.AIR);
                b.fill(1, 5, z0, 3, 5, z1, slab(Blocks.MANGROVE_SLAB, SlabType.BOTTOM));
                b.fill(2, 5, z0, 2, 5, z1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));     // the roof walk
                b.air(1, 2, mid, 1, 3, mid);
                b.chest(2, 2, z1 - 1, Direction.NORTH, ModChestLoot.FREIGHT);
                b.set(2, 2, z0 + 1, Blocks.HAY_BLOCK);
                BlockState wheel = Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);
                for (int z : new int[] {z0 + 1, z1 - 1}) {
                    b.set(0, 1, z, wheel.setValue(TrapDoorBlock.FACING, Direction.WEST));
                    b.set(4, 1, z, wheel.setValue(TrapDoorBlock.FACING, Direction.EAST));
                }
            }
            case TANK -> {
                // The water tank on four legs east of the line, its spout swung out over the rail
                int t0 = last - 8;
                for (int[] c : new int[][] {{4, t0}, {6, t0}, {4, t0 + 2}, {6, t0 + 2}}) {
                    b.fill(c[0], 1, c[1], c[0], 6, c[1], log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
                }
                b.fill(4, 7, t0, 6, 7, t0 + 2, Blocks.SPRUCE_PLANKS);
                b.walls(4, 8, t0, 6, 10, t0 + 2, s(Blocks.SPRUCE_PLANKS));
                b.walls(4, 9, t0, 6, 9, t0 + 2, s(Blocks.STRIPPED_SPRUCE_LOG));
                b.fill(5, 8, t0 + 1, 5, 9, t0 + 1, Blocks.WATER);
                b.fill(4, 11, t0, 6, 11, t0 + 2, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
                b.fill(2, 8, t0 + 1, 3, 8, t0 + 1, Blocks.SPRUCE_FENCE);
                b.fill(2, 6, t0 + 1, 2, 7, t0 + 1, Blocks.IRON_CHAIN);
            }
        }
        b.jigsaw(2, 0, 0, FrontAndTop.NORTH_UP, RAIL, RAIL, "minecraft:empty", s(Blocks.GRAVEL));
        return b;
    }

    // ---------------------------------------------------------------- street furniture

    static void lampPost(Blueprint b, int x, int z) {
        b.fill(x, 1, z, x, 2, z, Blocks.SPRUCE_FENCE);
        b.set(x, 3, z, TownDecor.lamp(false));                         // lit at dusk (24c)
    }

    // A pole with a cross-arm, wires running on down the street.
    static void telegraphPole(Blueprint b, int x, int z) {
        b.fill(x, 1, z, x, 6, z, Blocks.SPRUCE_FENCE);
        b.set(x - 1, 6, z, Blocks.SPRUCE_FENCE);
        b.set(x + 1, 6, z, Blocks.SPRUCE_FENCE);
        b.set(x, 7, z, Blocks.LIGHTNING_ROD.waxed().unaffected());
    }

    // A covered wagon, 3 wide and 5 long (x..x+2, z..z+4), wheels on its sides.
    static void wagon(Blueprint b, int x, int z) {
        b.fill(x, 1, z, x + 2, 1, z + 4, slab(Blocks.OAK_SLAB, SlabType.TOP));
        BlockState wheel = Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);
        for (int dz : new int[] {0, 4}) {
            b.set(x - 1, 1, z + dz, wheel.setValue(TrapDoorBlock.FACING, Direction.WEST));
            b.set(x + 3, 1, z + dz, wheel.setValue(TrapDoorBlock.FACING, Direction.EAST));
        }
        // Canvas over hoops
        for (int dz = 1; dz <= 3; dz++) {
            b.set(x, 2, z + dz, Blocks.WOOL.white());
            b.set(x + 2, 2, z + dz, Blocks.WOOL.white());
            b.fill(x, 3, z + dz, x + 2, 3, z + dz, Blocks.WOOL.white());
        }
        b.set(x + 1, 2, z + 2, Blocks.BARREL);
        b.set(x + 1, 1, z - 1, Blocks.SPRUCE_FENCE); // the tongue
    }

    // A fixed scramble of (x, z), 0 and up: the same town piece always comes out the same.
    static int hash(int x, int z) {
        int h = x * 73_856_093 ^ z * 19_349_663;
        h = (h ^ (h >>> 13)) * 0x5bd1e995;
        return Math.floorMod(h ^ (h >>> 15), 1 << 20);
    }

    /**
     * A shade tree at (x, z): a trunk five high from a bed of podzol, a round crown of leaves that never decay. Oak as drawn;
     * the town styles make it acacia (desert) or spruce (mining).
     */
    static void shadeTree(Blueprint b, int x, int z) {
        b.set(x, 0, z, Blocks.PODZOL);
        b.fill(x, 1, z, x, 5, z, log(Blocks.OAK_LOG, Direction.Axis.Y));
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        for (int dy = 3; dy <= 6; dy++) {
            int r = dy <= 4 ? 2 : 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean corner = Math.abs(dx) == r && Math.abs(dz) == r;
                    boolean trunk = dx == 0 && dz == 0 && dy <= 5;
                    if (!trunk && !(corner && (r == 2 || dy == 6))) {
                        b.set(x + dx, dy, z + dz, leaves);
                    }
                }
            }
        }
    }

    /**
     * A market stall, 3 x 3 from (x, z): four posts under a striped awning of wool slabs, the counter in the middle of its
     * front (z + 2, or z if {@code north}), goods either side of the stallholder and one more at the back.
     */
    static void stall(Blueprint b, int x, int z, boolean north, DyeColor stripe, Block left, Block right, BlockState back) {
        int front = north ? z : z + 2, rear = north ? z + 2 : z;
        for (int[] c : new int[][] {{x, z}, {x + 2, z}, {x, z + 2}, {x + 2, z + 2}}) {
            b.fill(c[0], 1, c[1], c[0], 2, c[1], Blocks.SPRUCE_FENCE);
        }
        for (int dx = 0; dx <= 2; dx++) {
            Block wool = Blocks.WOOL_SLAB.pick(dx == 1 ? DyeColor.WHITE : stripe);
            b.fill(x + dx, 3, z, x + dx, 3, z + 2, slab(wool, SlabType.BOTTOM));
        }
        b.set(x + 1, 1, front, ModBlocks.BAR_COUNTER.get());
        b.set(x, 1, z + 1, left);
        b.set(x + 2, 1, z + 1, right);
        b.set(x + 1, 1, rear, back);
    }

    /** An old cannon on a stone plinth, 5 x 3 from (x, z), its muzzle west, a pile of shot and a plaque on the plinth's north face. */
    static void cannon(Blueprint b, int x, int z) {
        b.fill(x, 1, z, x + 4, 1, z + 2, Blocks.STONE_BRICKS);
        b.fill(x + 1, 2, z + 1, x + 3, 2, z + 1, log(Blocks.POLISHED_BASALT, Direction.Axis.X));
        b.set(x, 2, z + 1, Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.ButtonBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                .setValue(net.minecraft.world.level.block.ButtonBlock.FACING, Direction.WEST));
        BlockState wheel = Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);
        b.set(x + 2, 2, z, wheel.setValue(TrapDoorBlock.FACING, Direction.NORTH));
        b.set(x + 2, 2, z + 2, wheel.setValue(TrapDoorBlock.FACING, Direction.SOUTH));
        b.set(x + 4, 2, z + 2, slab(Blocks.POLISHED_BLACKSTONE_SLAB, SlabType.BOTTOM));
        b.set(x + 4, 2, z, slab(Blocks.POLISHED_BLACKSTONE_SLAB, SlabType.BOTTOM));
        b.wallSign(x + 2, 1, z - 1, Blocks.SPRUCE_WALL_SIGN, Direction.NORTH, DyeColor.BLACK, false,
                "sign.hollowfrontier.cannon.1", "sign.hollowfrontier.cannon.2", "sign.hollowfrontier.cannon.3", "sign.hollowfrontier.cannon.4");
    }

    /**
     * The stagecoach, waiting at its stop: 3 wide and 5 long from (x, z), a dark body with windows each side on a slab
     * bed, luggage strapped on the roof, the wheels, the driver's box and the tongue to the north, and the stop's sign.
     */
    static void stagecoach(Blueprint b, int x, int z) {
        b.fill(x, 1, z, x + 2, 1, z + 4, slab(Blocks.MANGROVE_SLAB, SlabType.TOP));
        b.walls(x, 2, z + 1, x + 2, 3, z + 4, s(Blocks.MANGROVE_PLANKS));
        b.air(x + 1, 2, z + 2, x + 1, 3, z + 3);
        b.set(x, 3, z + 2, Blocks.GLASS_PANE);
        b.set(x + 2, 3, z + 2, Blocks.GLASS_PANE);
        b.set(x, 3, z + 3, Blocks.GLASS_PANE);
        b.set(x + 2, 3, z + 3, Blocks.GLASS_PANE);
        b.fill(x, 4, z + 1, x + 2, 4, z + 4, slab(Blocks.MANGROVE_SLAB, SlabType.BOTTOM));
        b.set(x + 1, 4, z + 2, Blocks.BARREL);
        b.set(x + 1, 4, z + 3, TownDecor.barrel(Direction.EAST));
        b.set(x + 1, 2, z, stairs(Blocks.MANGROVE_STAIRS, Direction.SOUTH, Half.BOTTOM)); // the driver's box
        BlockState wheel = Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);
        for (int dz : new int[] {0, 4}) {
            b.set(x - 1, 1, z + dz, wheel.setValue(TrapDoorBlock.FACING, Direction.WEST));
            b.set(x + 3, 1, z + dz, wheel.setValue(TrapDoorBlock.FACING, Direction.EAST));
        }
        b.set(x + 1, 1, z - 1, Blocks.SPRUCE_FENCE);
        b.set(x + 1, 1, z - 2, Blocks.SPRUCE_FENCE);
        b.fill(x + 4, 1, z - 1, x + 4, 2, z - 1, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y)); // the stop's post
        b.wallSign(x + 4, 2, z - 2, Blocks.SPRUCE_WALL_SIGN, Direction.NORTH, DyeColor.BLACK, false, "",
                TownDecor.signKey("stage_stop"), TownDecor.signKey("stage_stop") + ".sub");
    }

    // A ring of stones round a lit campfire, a log bench either side (TownCats gather at it in the evening).
    static void firePit(Blueprint b, int x, int z) {
        b.fill(x - 1, 0, z - 1, x + 1, 0, z + 1, Blocks.COBBLESTONE);
        b.set(x, 1, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        b.set(x - 2, 1, z, log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Z));
        b.set(x + 2, 1, z, log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Z));
    }
}
