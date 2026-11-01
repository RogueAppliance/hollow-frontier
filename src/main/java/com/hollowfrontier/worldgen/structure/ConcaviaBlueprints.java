package com.hollowfrontier.worldgen.structure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Concavia's ruins (12a), each one piece of a {@link HollowSiteStructure} with its floor ({@link HollowBlueprints#FLOOR})
 * set on the ground.
 * <ul>
 * <li>The <b>Sun Temple</b> (Ember Badlands, 12b), 41 x 41: the Sun Archon's ziggurat, its arena, throne and pylons
 *     ({@link #sunTemple}).</li>
 * <li>The <b>Crystal Ruins</b> (Concavian Highlands), 41 x 41: a round plaza of crystal brick and pale stone, broken up
 *     and mossed over; four crystal towers at the quarters, one still whole with its spire, three fallen to stumps with
 *     their rubble round them; crystal growing up through the paving; a phlogiston fountain before an avenue of lamp posts; and
 *     the <b>sanctum</b>, the one building still whole, with a skylight of crystal, a band of crystal windows and phlogiston
 *     lamps in its roof. Three Kindled live in it, with an altar and a chest of their stores at the back; two Crystal
 *     Guardians stand at its door and a third on the plaza.</li>
 * <li>Its <b>restoration</b> (15c), three stages, each a template of only what changes from the stage before
 *     ({@link #stageName}): 1, the north-east tower raised whole; 2, the south-west tower raised and the sanctum lit
 *     (a band of lamps in its windows, lamps on its corner pillars and in its skylight); 3, the last tower raised, the
 *     paving mended, the moss cleared, and the waystone on its dais at the plaza's north edge ({@link #WAYSTONE}).
 *     A stage is drawn from the same random draws as the ruin, so it matches the ruin already standing.</li>
 * </ul>
 */
public final class ConcaviaBlueprints {
    private ConcaviaBlueprints() {}

    public static final String CRYSTAL_RUINS = "crystal_ruins";
    /** The sanctum's box, and where its door is (on the south side, z = SANCTUM_Z0, three wide round x = 20). */
    public static final int SANCTUM_X0 = 13, SANCTUM_X1 = 27, SANCTUM_Z0 = 16, SANCTUM_Z1 = 30, SANCTUM_HEIGHT = 8;
    public static final int CENTRE = 20;
    public static final String SUN_TEMPLE = "sun_temple";
    /** The Sun Temple's arena floor, above the temple's own floor; and where its four pylons stand. */
    public static final int ARENA_Y = 8;
    public static final int[][] PYLONS = {{10, 10 + ConcaviaBlueprints.APPROACH}, {30, 10 + ConcaviaBlueprints.APPROACH}, {10, 30 + ConcaviaBlueprints.APPROACH}, {30, 30 + ConcaviaBlueprints.APPROACH}};
    /** How far the Sun Temple's approach runs north of its ziggurat (25): the ziggurat's z is its old z plus this. */
    public static final int APPROACH = 18;

    /** Restoration (15c): how many stages; the fountain's middle, the waystone and where a traveller arrives by it (template-local). */
    public static final int STAGES = 3;
    public static final BlockPos FOUNTAIN = new BlockPos(CENTRE, 1, 8);
    public static final BlockPos WAYSTONE = new BlockPos(CENTRE, 1, 2);
    public static final BlockPos WAYSTONE_ARRIVAL = new BlockPos(CENTRE, 1, 4);
    // Each tower's x, z and height; the fallen ones in the order they're raised, and the height they're raised to
    private static final int[][] TOWERS = {{8, 8, 17}, {32, 8, 6}, {8, 32, 10}, {32, 32, 4}};
    private static final int WHOLE = 17;

    public static List<Blueprint> all() {
        List<Blueprint> all = new ArrayList<>(List.of(crystalRuins(), sunTemple()));
        for (int stage = 1; stage <= STAGES; stage++) {
            all.add(delta(stageName(stage), crystalRuins(stage - 1, "before"), crystalRuins(stage, "after")));
        }
        return all;
    }

    /** The template that takes a ruin from the stage before to {@code stage}. */
    public static String stageName(int stage) {
        return CRYSTAL_RUINS + "_stage_" + stage;
    }

    /** What changes from {@code from} to {@code to}: what's drawn differently, and air where {@code to} has nothing. */
    static Blueprint delta(String name, Blueprint from, Blueprint to) {
        Blueprint delta = new Blueprint(name);
        Set<BlockPos> positions = new LinkedHashSet<>(from.positions());
        positions.addAll(to.positions());
        for (BlockPos pos : positions) {
            BlockState after = to.get(pos) == null ? Blocks.AIR.defaultBlockState() : to.get(pos);
            if (!after.equals(from.get(pos))) {
                delta.set(pos.getX(), pos.getY(), pos.getZ(), after);
            }
        }
        return delta;
    }

    static Blueprint crystalRuins() {
        return crystalRuins(0, CRYSTAL_RUINS);
    }

    /** The ruin as it stands at {@code stage} of its restoration (0: as it's found). */
    static Blueprint crystalRuins(int stage, String name) {
        Blueprint b = new Blueprint(name);
        Random r = new Random(1912);
        int c = CENTRE, s = HollowBlueprints.FLOOR;
        BlockState bricks = ModBlocks.CRYSTAL_BRICKS.get().defaultBlockState();
        BlockState crystal = ModBlocks.CONCAVIAN_CRYSTAL.get().defaultBlockState();
        BlockState chiseled = ModBlocks.CHISELED_CRYSTAL_BRICKS.get().defaultBlockState();
        BlockState lamp = ModBlocks.PHLOGISTON_LAMP.get().defaultBlockState();
        // The plaza: cleared, paved in crystal brick and pale stone, with gaps where the ground shows through
        for (int x = 0; x <= 40; x++) {
            for (int z = 0; z <= 40; z++) {
                double d = Math.hypot(x - c, z - c);
                if (d > 20.4) {
                    continue;
                }
                b.air(x, s + 1, z, x, s + 24, z);
                // (Every stage makes the same draws in the same order, so a stage matches the ruin already standing)
                if (d <= 18.6) {
                    if (r.nextInt(6) != 0) {
                        b.set(x, s, z, paving(r));
                        if (r.nextInt(16) == 0 && stage < 3) {
                            b.set(x, s + 1, z, Blocks.PALE_MOSS_CARPET);
                        }
                    } else if (stage >= 3) {
                        b.set(x, s, z, bricks); // the gaps mended
                    }
                }
            }
        }
        // The towers at the quarters: one whole, three fallen (raised again one a stage)
        int[][] towers = TOWERS;
        for (int i = 0; i < towers.length; i++) {
            int[] t = towers[i];
            tower(b, r, t[0], t[1], s, t[2], t[2] >= WHOLE, i > 0 && i <= stage);
        }
        // Crystal grown up through the paving, here and there
        for (int i = 0; i < 9; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 11 + r.nextDouble() * 7;
            int x = c + (int) Math.round(Math.cos(a) * d), z = c + (int) Math.round(Math.sin(a) * d);
            if (inSanctum(x, z, 2) || nearTower(towers, x, z) || !clear(x, z)) {
                continue;
            }
            int h = 1 + r.nextInt(3);
            b.fill(x, s, z, x, s + h, z, crystal);
            b.set(x, s + h + 1, z, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
        }
        // The phlogiston fountain on the plaza before the sanctum: a basin of crystal brick round a lit pillar, chiseled at the rim's
        // four points
        int fz = 8;
        for (int x = c - 4; x <= c + 4; x++) {
            for (int z = fz - 4; z <= fz + 4; z++) {
                double d = Math.hypot(x - c, z - fz);
                if (d <= 3.6) {
                    b.set(x, s, z, bricks);
                }
                if (d > 2.5 && d <= 3.6) {
                    b.set(x, s + 1, z, bricks); // full blocks: a slab rim would be waterlogged as the template places, and spill
                } else if (d <= 2.5) {
                    b.set(x, s + 1, z, Blocks.WATER);
                }
            }
        }
        for (int[] point : new int[][] {{c - 3, fz}, {c + 3, fz}, {c, fz - 3}, {c, fz + 3}}) {
            b.set(point[0], s + 1, point[1], chiseled);
        }
        b.fill(c, s + 1, fz, c, s + 2, fz, chiseled);
        b.set(c, s + 3, fz, lamp);
        b.set(c, s + 4, fz, crystal);
        b.set(c, s + 5, fz, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
        // The avenue from the fountain to the sanctum door: lamp posts either side
        for (int x : new int[] {c - 3, c + 3}) {
            b.fill(x, s + 1, 13, x, s + 2, 13, ModBlocks.CRYSTAL_BRICK_WALL.get());
            b.set(x, s + 3, 13, lamp);
        }
        for (int z = 12; z < SANCTUM_Z0; z++) {
            b.fill(c - 1, s, z, c + 1, s, z, bricks);
        }
        sanctum(b, s, bricks, crystal, chiseled, lamp, stage >= 2);
        // The waystone (stage 3), on a chiseled dais at the plaza's north edge, a crystal and its cluster above it
        if (stage >= 3) {
            int wx = WAYSTONE.getX(), wz = WAYSTONE.getZ();
            b.fill(wx - 1, s, wz - 1, wx + 1, s, wz + 1, chiseled);
            b.set(wx, s + 1, wz, ModBlocks.WAYSTONE.get());
            b.set(wx, s + 2, wz, crystal);
            b.set(wx, s + 3, wz, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
        }
        districts(b, s);
        // Who keeps it: the guardians at the door and on the plaza, the Kindled within
        b.entity(ModEntities.CRYSTAL_GUARDIAN.get(), c - 3, s + 1, 15);
        b.entity(ModEntities.CRYSTAL_GUARDIAN.get(), c + 3, s + 1, 15);
        b.entity(ModEntities.CRYSTAL_GUARDIAN.get(), 7, s + 1, 21);
        b.entity(ModEntities.KINDLED.get(), c - 3, s + 1, 22);
        b.entity(ModEntities.KINDLED.get(), c + 3, s + 1, 22);
        b.entity(ModEntities.KINDLED.get(), c, s + 1, 25);
        return b;
    }

    /**
     * The city round the plaza (25), the same at every stage of its restoration (drawn from its own random draws, so the
     * stages' deltas never touch it): two houses on the west side, roofless now, a bed, a table and a hearth in each and a
     * chest in one; a market row of three stalls on the east under crystal awnings, their goods still on the counters; a
     * library tower south of the sanctum, three floors of shelves up a ladder, its top broken off; and a spire that fell
     * across the north-east of the plaza, lying in three pieces.
     */
    private static void districts(Blueprint b, int s) {
        Random r = new Random(1913);
        BlockState bricks = ModBlocks.CRYSTAL_BRICKS.get().defaultBlockState();
        BlockState chiseled = ModBlocks.CHISELED_CRYSTAL_BRICKS.get().defaultBlockState();
        BlockState crystal = ModBlocks.CONCAVIAN_CRYSTAL.get().defaultBlockState();
        BlockState lamp = ModBlocks.PHLOGISTON_LAMP.get().defaultBlockState();
        // The houses (x 2..8; z 12..17 and 24..28), doors east onto the plaza
        for (int[] house : new int[][] {{2, 12, 8, 17}, {2, 24, 8, 28}}) {
            int x0 = house[0], z0 = house[1], x1 = house[2], z1 = house[3];
            b.fill(x0, s, z0, x1, s, z1, Blocks.POLISHED_DIORITE);
            for (int y = s + 1; y <= s + 5; y++) {
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        boolean wall = x == x0 || x == x1 || z == z0 || z == z1, corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                        b.set(x, y, z, !wall ? Blocks.AIR.defaultBlockState() : corner ? chiseled : y == s + 3 && (x + z) % 2 == 0 ? crystal : bricks);
                    }
                }
            }
            int dz = (z0 + z1) / 2;
            b.air(x1, s + 1, dz, x1, s + 3, dz);
            b.set(x1, s + 4, dz, chiseled);
            b.bed(x0 + 1, s + 1, z0 + 1, Blocks.BED.lightBlue(), Direction.EAST);
            b.set(x0 + 1, s + 1, z1 - 1, ModBlocks.CRYSTAL_BRICK_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            b.set(x0 + 1, s + 2, z1 - 1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2));
            b.set(x0 + 3, s + 1, z1 - 1, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            b.pot(x1 - 1, s + 1, z0 + 1, Direction.WEST, null);
            Masonry.crumble(b, r, x0, z0, x1, z1, 3, 0.55);
        }
        b.chest(3, s + 1, 15, Direction.EAST, ModChestLoot.CRYSTAL_HOME);
        b.shelf(3, s + 2, 26, Blocks.PALE_OAK_SHELF, Direction.EAST, net.minecraft.world.item.Items.AMETHYST_SHARD, null, net.minecraft.world.item.Items.BOOK);
        // The market row (east): three stalls, a counter of slabs, walls at the corners carrying an awning of crystal slabs
        int[] stalls = {13, 18, 23};
        for (int i = 0; i < stalls.length; i++) {
            int z0 = stalls[i], x0 = 31, x1 = 35, z1 = z0 + 3;
            b.fill(x0, s, z0, x1, s, z1, ModBlocks.CRYSTAL_BRICKS.get());
            for (int[] post : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
                b.fill(post[0], s + 1, post[1], post[0], s + 3, post[1], ModBlocks.CRYSTAL_BRICK_WALL.get());
            }
            boolean torn = i == 1;
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    if (!torn || r.nextInt(3) == 0) {
                        b.set(x, s + 4, z, ModBlocks.CRYSTAL_BRICK_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                    }
                }
            }
            b.fill(x0, s + 1, z0 + 1, x0, s + 1, z1 - 1, ModBlocks.CRYSTAL_BRICK_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            b.set(x0 + 3, s + 1, z0 + 1, Blocks.BARREL);
            b.pot(x0 + 3, s + 1, z1 - 1, Direction.WEST, ModChestLoot.KINDLED_OFFERING);
            b.set(x0, s + 2, z0 + 1, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
            b.set(x0, s + 2, z1 - 1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + i));
        }
        b.chest(34, s + 1, 20, Direction.WEST, ModChestLoot.CRYSTAL_MARKET);
        // The library tower (x 16..24, z 32..38): three floors of shelves up a ladder, the top broken away
        int lx0 = 16, lx1 = 24, lz0 = 32, lz1 = 38;
        for (int y = s; y <= s + 13; y++) {
            for (int x = lx0; x <= lx1; x++) {
                for (int z = lz0; z <= lz1; z++) {
                    boolean wall = x == lx0 || x == lx1 || z == lz0 || z == lz1, corner = (x == lx0 || x == lx1) && (z == lz0 || z == lz1);
                    if (y == s || y == s + 5 || y == s + 10) {
                        b.set(x, y, z, wall ? chiseled : y == s ? Blocks.POLISHED_DIORITE.defaultBlockState() : bricks);
                    } else {
                        b.set(x, y, z, !wall ? Blocks.AIR.defaultBlockState() : corner ? chiseled
                                : (y % 5 == 3) && (x + z) % 3 == 0 ? crystal : bricks);
                    }
                }
            }
        }
        b.air(20, s + 1, lz0, 20, s + 3, lz0); // the door, north toward the sanctum
        b.set(20, s + 4, lz0, lamp);
        for (int floor = 0; floor < 3; floor++) {
            int y = s + 1 + floor * 5;
            b.fill(lx0 + 1, y, lz1 - 1, lx1 - 1, y + 2, lz1 - 1, Blocks.BOOKSHELF);
            b.fill(lx0 + 1, y, lz0 + 2, lx0 + 1, y + 1, lz1 - 2, Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
            b.set(22, y, 34, Blocks.LECTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LecternBlock.FACING, Direction.WEST));
            if (floor < 2) {
                b.set(20, y + 4, 35, lamp); // in the floor above
            }
            if (floor > 0) {
                b.air(23, y - 1, 33, 23, y - 1, 33); // the hatch the ladder goes up through
            }
        }
        b.fill(23, s + 1, 33, 23, s + 12, 33, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.WEST));
        b.chest(17, s + 6, 37, Direction.EAST, ModChestLoot.CRYSTAL_LIBRARY);
        Masonry.crumble(b, r, lx0, lz0, lx1, lz1, 4, 0.6);
        Masonry.rubble(b, r, 26, s + 1, 37, 2.5, 6, bricks, ModBlocks.CRYSTAL_BRICK_SLAB.get().defaultBlockState(), ModBlocks.CRYSTAL_BRICK_STAIRS.get().defaultBlockState());
        // Suspicious gravel where the houses' floors and the library's rubble have settled (28: for a brush)
        for (int[] p : new int[][] {{5, 14}, {5, 26}, {26, 35}, {28, 3}, {33, 24}}) {
            b.suspicious(p[0], s, p[1], Blocks.SUSPICIOUS_GRAVEL, ModChestLoot.DIG_CRYSTAL_RUINS);
        }
        // The fallen spire, north-east, in three pieces with crystal grown along it
        int[][] spire = {{26, 2}, {27, 4}, {27, 5}, {28, 7}, {28, 8}, {29, 11}, {29, 12}};
        for (int i = 0; i < spire.length; i++) {
            int x = spire[i][0], z = spire[i][1];
            b.fill(x, s + 1, z, x + 1, s + 1, z, crystal);
            if (i % 3 != 2) {
                b.set(x, s + 2, z, i == 0 ? chiseled : crystal);
            }
            if (i % 2 == 0) {
                b.set(x + 1, s + 2, z, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
            }
        }
    }

    /**
     * The sanctum: crystal brick walls on a chiseled course, chiseled pillars at the corners, a band of crystal windows,
     * the roof crystal brick round a crystal skylight with four phlogiston lamps set in it. Benches along the sides, the altar
     * at the back with a lamp on it, and the stores in a chest beside it. Lit (restoration stage 2), the windows' middle
     * row, the corner pillars' caps and the skylight's middle are phlogiston lamps.
     */
    private static void sanctum(Blueprint b, int s, BlockState bricks, BlockState crystal, BlockState chiseled, BlockState lamp, boolean lit) {
        int x0 = SANCTUM_X0, x1 = SANCTUM_X1, z0 = SANCTUM_Z0, z1 = SANCTUM_Z1, top = s + SANCTUM_HEIGHT, c = CENTRE;
        b.fill(x0, s, z0, x1, s, z1, bricks);
        b.fill(x0 + 2, s, z0 + 2, x1 - 2, s, z1 - 2, Blocks.CALCITE);
        b.walls(x0, s + 1, z0, x1, top - 1, z1, bricks);
        b.walls(x0, s + 1, z0, x1, s + 1, z1, chiseled);
        for (int y = s + 3; y <= s + 5; y++) {
            BlockState pane = lit && y == s + 4 ? lamp : crystal;
            for (int k = 2; k <= 12; k += 3) {
                b.set(x0, y, z0 + k, pane);
                b.set(x1, y, z0 + k, pane);
                b.set(x0 + k, y, z1, pane);
            }
        }
        for (int[] corner : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
            b.fill(corner[0], s + 1, corner[1], corner[0], top, corner[1], chiseled);
            b.set(corner[0], top + 1, corner[1], lit ? lamp : crystal);
        }
        // The roof
        b.fill(x0, top, z0, x1, top, z1, bricks);
        b.fill(x0 + 3, top, z0 + 3, x1 - 3, top, z1 - 3, crystal);
        for (int[] p : new int[][] {{x0 + 3, z0 + 3}, {x1 - 3, z0 + 3}, {x0 + 3, z1 - 3}, {x1 - 3, z1 - 3}}) {
            b.set(p[0], top, p[1], lamp);
        }
        if (lit) {
            b.set(c, top, (z0 + z1) / 2, lamp);
        }
        // The door, three wide and four high, under a chiseled lintel
        b.air(c - 1, s + 1, z0, c + 1, s + 4, z0);
        b.fill(c - 2, s + 5, z0, c + 2, s + 5, z0, chiseled);
        // Benches along the side walls
        BlockState east = ModBlocks.CRYSTAL_BRICK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState west = ModBlocks.CRYSTAL_BRICK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = z0 + 3; z <= z1 - 5; z += 4) {
            b.fill(x0 + 1, s + 1, z, x0 + 1, s + 1, z + 1, east);
            b.fill(x1 - 1, s + 1, z, x1 - 1, s + 1, z + 1, west);
        }
        // The altar and the stores
        b.fill(c - 1, s + 1, z1 - 2, c + 1, s + 1, z1 - 2, chiseled);
        b.set(c, s + 2, z1 - 2, lamp);
        b.set(c - 1, s + 2, z1 - 2, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
        b.set(c + 1, s + 2, z1 - 2, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
        b.chest(c + 4, s + 1, z1 - 1, Direction.NORTH, ModChestLoot.CRYSTAL_RUINS);
    }

    /**
     * A round tower of crystal brick, radius 2.6, {@code height} high. A whole one has a ring of chiseled brick at the top
     * and a crystal spire; a fallen one is broken off ragged and its rubble lies round it. A fallen one {@code raised}
     * (restoration) is drawn whole at {@link #WHOLE}, with no rubble, from the same random draws.
     */
    private static void tower(Blueprint b, Random r, int tx, int tz, int s, int height, boolean whole, boolean raised) {
        BlockState bricks = ModBlocks.CRYSTAL_BRICKS.get().defaultBlockState();
        int h = raised ? WHOLE : height;
        for (int x = tx - 3; x <= tx + 3; x++) {
            for (int z = tz - 3; z <= tz + 3; z++) {
                double d = Math.hypot(x - tx, z - tz);
                if (d > 2.7) {
                    continue;
                }
                b.set(x, s, z, bricks);
                if (d <= 1.7) {
                    continue;
                }
                int top = whole ? height : height - r.nextInt(Math.max(1, height / 2));
                b.fill(x, s + 1, z, x, s + (raised ? h : top), z, bricks);
                if (whole || raised) {
                    b.set(x, s + h + 1, z, ModBlocks.CHISELED_CRYSTAL_BRICKS.get());
                }
            }
        }
        if (whole || raised) {
            b.fill(tx, s + h - 1, tz, tx, s + h + 4, tz, ModBlocks.CONCAVIAN_CRYSTAL.get());
            b.set(tx, s + h + 5, tz, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
            b.set(tx, s + h - 2, tz, ModBlocks.PHLOGISTON_LAMP.get());
            if (whole) {
                return;
            }
        }
        // Rubble: bricks, slabs and stairs strewn out from the stump
        for (int i = 0; i < 12; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 3 + r.nextDouble() * 4;
            int x = tx + (int) Math.round(Math.cos(a) * d), z = tz + (int) Math.round(Math.sin(a) * d);
            if (Math.hypot(x - CENTRE, z - CENTRE) > 19 || inSanctum(x, z, 1) || !clear(x, z)) {
                continue;
            }
            Block block = switch (r.nextInt(3)) {
                case 0 -> ModBlocks.CRYSTAL_BRICKS.get();
                case 1 -> ModBlocks.CRYSTAL_BRICK_SLAB.get();
                default -> ModBlocks.CRYSTAL_BRICK_STAIRS.get();
            };
            BlockState state = block.defaultBlockState();
            if (block instanceof StairBlock) {
                state = state.setValue(StairBlock.FACING, Direction.from2DDataValue(r.nextInt(4)));
            }
            if (!raised) { // raised, its rubble's gone back into it (the draws are still made)
                b.set(x, s + 1, z, state);
            }
        }
    }

    /**
     * The Sun Temple (12b; rebuilt in 25), 41 x 59. North, its approach ({@link #APPROACH} deep): a sundial plaza (a
     * starsteel gnomon in a ring of hour stones) and an avenue of resin-brick pillars (one in four broken, its drums
     * lying by it) to two sun priests in stone either side of the stair's foot. Then the ziggurat: three tiers of crystal
     * brick, each topped with smooth quartz and banded in resin brick, balustrades along their ledges, glowstone at the
     * corners and a fire at each corner of the base. A stair climbs the north face to the arena on top (29 x 29,
     * {@link #ARENA_Y} its floor): a sun laid in its paving, a parapet round it, a Sun Pylon on a pedestal at each corner.
     * At its south end the throne hall: walls of crystal and resin to the arena's height and more, a roof with a light
     * well over the throne, braziers, an offering chest; inside, a stepped dais and the throne, the sun disc set into the
     * back wall behind it. The Archon stands before it facing the stair; the hoard is in a chest beside it.
     */
    static Blueprint sunTemple() {
        Blueprint b = new Blueprint(SUN_TEMPLE);
        Random r = new Random(1212);
        int c = CENTRE, s = HollowBlueprints.FLOOR, top = s + ARENA_Y, o = APPROACH;
        BlockState bricks = ModBlocks.CRYSTAL_BRICKS.get().defaultBlockState();
        BlockState chiseled = ModBlocks.CHISELED_CRYSTAL_BRICKS.get().defaultBlockState();
        BlockState wall = ModBlocks.CRYSTAL_BRICK_WALL.get().defaultBlockState();
        BlockState resin = Blocks.RESIN_BRICKS.defaultBlockState(), carved = Blocks.CHISELED_RESIN_BRICKS.defaultBlockState();
        b.air(0, s + 1, 0, 40, s + 30, 40 + o);
        // The three tiers, balustrades along the two lower ledges (open where the stair goes up)
        for (int tier = 0; tier < 3; tier++) {
            int m = tier * 3, y0 = s + tier * 3, y1 = y0 + 2;
            b.fill(m, y0, m + o, 40 - m, y1, 40 - m + o, bricks);
            b.fill(m, y1, m + o, 40 - m, y1, 40 - m + o, Blocks.SMOOTH_QUARTZ);
            b.walls(m, y1 - 1, m + o, 40 - m, y1 - 1, 40 - m + o, resin);
            for (int[] corner : new int[][] {{m, m}, {40 - m, m}, {m, 40 - m}, {40 - m, 40 - m}}) {
                b.set(corner[0], y1, corner[1] + o, Blocks.GLOWSTONE);
            }
            if (tier < 2) {
                for (int k = m + 1; k < 40 - m; k++) {
                    for (int[] p : new int[][] {{k, m}, {k, 40 - m}, {m, k}, {40 - m, k}}) {
                        boolean stair = p[1] == m && Math.abs(p[0] - c) <= 3;
                        if (!stair && k % 6 != 0) {
                            b.set(p[0], y1 + 1, p[1] + o, wall);
                        } else if (!stair) {
                            b.set(p[0], y1 + 1, p[1] + o, carved);
                        }
                    }
                }
            }
        }
        for (int[] corner : new int[][] {{1, 1}, {39, 1}, {1, 39}, {39, 39}}) {
            b.set(corner[0], s + 3, corner[1] + o, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        }
        // The arena's paving: a sun
        for (int x = 7; x <= 33; x++) {
            for (int z = 7; z <= 33; z++) {
                double d = Math.hypot(x - c, z - c);
                double a = Math.toDegrees(Math.atan2(z - c, x - c)) + 360;
                boolean ray = d > 3 && d < 10 && Math.abs(((a + 22.5) % 45) - 22.5) < 6;
                Block block = d <= 2.5 ? Blocks.SHROOMLIGHT
                        : d > 5.5 && d <= 6.5 ? Blocks.DYED_TERRACOTTA.yellow()
                        : d > 10.5 && d <= 11.5 ? Blocks.HONEYCOMB_BLOCK
                        : ray ? Blocks.DYED_TERRACOTTA.yellow()
                        : Blocks.SMOOTH_QUARTZ;
                b.set(x, top, z + o, block);
            }
        }
        // The parapet, and the pylons on their pedestals
        b.walls(6, top + 1, 6 + o, 34, top + 1, 34 + o, wall);
        for (int[] p : PYLONS) {
            b.fill(p[0], top + 1, p[1], p[0], top + 2, p[1], chiseled);
            b.set(p[0], top + 3, p[1], ModBlocks.SUN_PYLON.get());
        }
        // The stair up the north face, with a balustrade
        for (int k = 0; k <= 7; k++) {
            int y = s + 1 + k;
            for (int x = c - 2; x <= c + 2; x++) {
                b.fill(x, s, k + o, x, y - 1, k + o, bricks);
                b.set(x, y, k + o, ModBlocks.CRYSTAL_BRICK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
                b.air(x, y + 1, k + o, x, y + 4, k + o);
            }
            b.set(c - 3, y + 1, k + o, wall);
            b.set(c + 3, y + 1, k + o, wall);
        }
        // The throne hall (x 12..28, z 27..34 over the arena): walls banded in resin, a roof with a light well
        int h0 = 27 + o, h1 = 34 + o, roof = top + 12;
        for (int y = top + 1; y < roof; y++) {
            for (int z = h0; z <= h1; z++) {
                for (int x : new int[] {12, 28}) {
                    b.set(x, y, z, y % 4 == 0 ? resin : z == h0 ? chiseled : bricks);
                }
            }
            for (int x = 12; x <= 28; x++) {
                b.set(x, y, h1, y % 4 == 0 ? resin : bricks);
            }
        }
        for (int x = 12; x <= 28; x++) {
            for (int z = h0; z <= h1; z++) {
                boolean well = Math.abs(x - c) <= 1 && z >= 30 + o && z <= 32 + o;
                if (!well) {
                    b.set(x, roof, z, x == 12 || x == 28 || z == h0 || z == h1 ? carved : bricks);
                }
            }
        }
        b.fill(12, roof - 1, h0, 28, roof - 1, h0, resin); // the lintel along the open front
        for (int x = 16; x <= 24; x += 8) { // two columns under it
            b.set(x, top + 1, h0, carved);
            b.fill(x, top + 2, h0, x, roof - 2, h0, Blocks.QUARTZ_PILLAR);
        }
        // Braziers at the hall's corners, and an offering chest in its west aisle
        for (int[] p : new int[][] {{13, h0}, {27, h0}, {13, h1 - 1}, {27, h1 - 1}}) {
            b.set(p[0], top + 1, p[1], carved);
            b.set(p[0], top + 2, p[1], Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        }
        b.chest(13, top + 1, 31 + o, Direction.EAST, ModChestLoot.SUN_TEMPLE_OFFERING);
        b.pot(13, top + 1, 30 + o, Direction.EAST, ModChestLoot.SUN_TEMPLE_OFFERING);
        b.pot(27, top + 1, 30 + o, Direction.WEST, ModChestLoot.SUN_TEMPLE_OFFERING);
        // The dais and the throne, the sun disc behind it
        b.fill(c - 3, top + 1, 29 + o, c + 3, top + 1, 33 + o, chiseled);
        b.fill(c - 2, top + 2, 31 + o, c + 2, top + 2, 33 + o, bricks);
        b.set(c, top + 3, 32 + o, ModBlocks.CRYSTAL_BRICK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
        b.set(c - 1, top + 3, 32 + o, chiseled);
        b.set(c + 1, top + 3, 32 + o, chiseled);
        b.fill(c, top + 3, 33 + o, c, top + 6, 33 + o, chiseled);
        b.fill(c - 1, top + 3, 33 + o, c + 1, top + 4, 33 + o, chiseled);
        for (int x = c - 4; x <= c + 4; x++) {
            for (int y = top + 5; y <= top + 11; y++) {
                double d = Math.hypot(x - c, y - (top + 8));
                if (d <= 2.6) {
                    b.set(x, y, h1, Blocks.GLOWSTONE);
                } else if (d <= 3.7) {
                    b.set(x, y, h1, Blocks.SHROOMLIGHT);
                }
            }
        }
        // Behind the dais, a shaft down through the ziggurat to the undercroft (26), where the hoard is now
        b.fill(25, s + 1, 33 + o, 25, top, 33 + o, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.NORTH));
        DungeonBlueprints.shaftEntry(b, DungeonBlueprints.UNDERCROFT, 25, s, 33 + o, Direction.NORTH);
        // The approach: the sundial plaza (centre (20, 7)), its hour stones and gnomon
        int pz = 7;
        for (int x = c - 7; x <= c + 7; x++) {
            for (int z = pz - 7; z <= pz + 7; z++) {
                double d = Math.hypot(x - c, z - pz);
                if (d > 6.6) {
                    continue;
                }
                double a = Math.toDegrees(Math.atan2(z - pz, x - c)) + 360;
                boolean hour = d > 3 && d <= 5.6 && Math.abs(((a + 15) % 30) - 15) < 2.5;
                b.set(x, s, z, d > 5.6 ? resin : hour ? Blocks.DYED_TERRACOTTA.orange().defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState());
                if (d > 5.6 && Math.abs(((a + 15) % 30) - 15) < 4) {
                    b.set(x, s + 1, z, carved); // the hour stones round the rim
                }
            }
        }
        b.fill(c, s + 1, pz, c, s + 3, pz, Masonry.starsteel());
        b.set(c, s + 4, pz, carved);
        for (int z = pz + 1; z <= pz + 4; z++) { // the gnomon's shadow, laid in dark stone
            b.set(c, s, z, Blocks.DYED_TERRACOTTA.black());
        }
        // The avenue: paving from the plaza to the stair, pillars either side, one in four broken
        for (int z = 13; z < o; z++) {
            b.fill(c - 2, s, z, c + 2, s, z, Blocks.SMOOTH_SANDSTONE);
            b.set(c - 3, s, z, resin);
            b.set(c + 3, s, z, resin);
        }
        for (int z = 1; z <= 13; z += 4) {
            for (int x : new int[] {13, 27}) {
                b.set(x, s, z, carved);
                if (r.nextInt(4) == 0) {
                    b.fill(x, s + 1, z, x, s + 1 + r.nextInt(3), z, resin);
                    int dir = x < c ? -1 : 1;
                    for (int k = 2; k <= 4; k++) {
                        b.set(x + dir * k, s + 1, z, Blocks.RESIN_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                    }
                } else {
                    b.fill(x, s + 1, z, x, s + 6, z, resin);
                    b.set(x, s + 7, z, chiseled);
                    b.set(x, s + 8, z, Blocks.SHROOMLIGHT);
                }
            }
        }
        priest(b, c - 6, s, o - 2);
        priest(b, c + 6, s, o - 2);
        // The Archon, before its throne, facing the stair
        CompoundTag facing = new CompoundTag();
        ListTag rotation = new ListTag();
        rotation.add(FloatTag.valueOf(180.0F));
        rotation.add(FloatTag.valueOf(0.0F));
        facing.put("Rotation", rotation);
        b.entity(ModEntities.SUN_ARCHON.get(), c, top + 2, 30 + o, facing);
        return b;
    }

    /** A sun priest in stone, facing north, holding a sun disc up over its head. */
    private static void priest(Blueprint b, int x, int y, int z) {
        b.fill(x - 1, y + 1, z - 1, x + 1, y + 1, z + 1, Blocks.CHISELED_RESIN_BRICKS);
        b.fill(x, y + 2, z, x, y + 4, z, Blocks.SMOOTH_QUARTZ); // the robe
        b.set(x, y + 5, z, ModBlocks.CHISELED_CRYSTAL_BRICKS.get()); // the head
        b.set(x - 1, y + 4, z, Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST)); // arms raised
        b.set(x + 1, y + 4, z, Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        b.fill(x - 1, y + 6, z, x + 1, y + 6, z, Blocks.HONEYCOMB_BLOCK); // the disc
        b.set(x, y + 7, z, Blocks.SHROOMLIGHT);
        b.set(x, y + 6, z, Blocks.SHROOMLIGHT);
    }

    private static boolean inSanctum(int x, int z, int margin) {
        return x >= SANCTUM_X0 - margin && x <= SANCTUM_X1 + margin && z >= SANCTUM_Z0 - margin && z <= SANCTUM_Z1 + margin;
    }

    /** Clear of the fountain, the avenue and the plaza guardian's post, where nothing may fall or grow. */
    private static boolean clear(int x, int z) {
        boolean fountain = Math.hypot(x - CENTRE, z - 8) <= 5, avenue = Math.abs(x - CENTRE) <= 4 && z >= 10 && z < SANCTUM_Z0;
        return !fountain && !avenue && Math.hypot(x - 7, z - 21) > 2;
    }

    private static boolean nearTower(int[][] towers, int x, int z) {
        for (int[] t : towers) {
            if (Math.hypot(x - t[0], z - t[1]) <= 4) {
                return true;
            }
        }
        return false;
    }

    private static BlockState paving(Random r) {
        return switch (r.nextInt(7)) {
            case 0, 1, 2 -> ModBlocks.CRYSTAL_BRICKS.get().defaultBlockState();
            case 3, 4 -> Blocks.CALCITE.defaultBlockState();
            case 5 -> Blocks.POLISHED_DIORITE.defaultBlockState();
            default -> Blocks.SMOOTH_QUARTZ.defaultBlockState();
        };
    }
}
