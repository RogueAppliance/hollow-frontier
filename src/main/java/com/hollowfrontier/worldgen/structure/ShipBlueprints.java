package com.hollowfrontier.worldgen.structure;

import static com.hollowfrontier.content.ufo.taken.ShipLayout.C;
import static com.hollowfrontier.content.ufo.taken.ShipLayout.CEILING;
import static com.hollowfrontier.content.ufo.taken.ShipLayout.DECK;
import static com.hollowfrontier.content.ufo.taken.ShipLayout.SIZE;

import java.util.List;
import java.util.Random;

import com.hollowfrontier.content.ufo.taken.CoreSocketBlock;
import com.hollowfrontier.content.ufo.taken.ShipConsoleBlock;
import com.hollowfrontier.content.ufo.taken.ShipLayout;
import com.hollowfrontier.content.ufo.taken.ShipLayout.Side;
import com.hollowfrontier.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * What the taken see (50). Neither is worldgen: {@code Taken} sets each down when it's needed.
 * <ul>
 * <li>The <b>ship</b>, drawn to {@link ShipLayout}: a hull of saucer plate round white rooms lit from the ceiling, as the
 *     Grays' pens in the Hollow are. No crew, captives or closed doors are drawn: {@code Ship} brings those aboard, so a
 *     ship can be raised with nobody on it.</li>
 * <li>The <b>pod wreck</b>, 7 x 7: what's left of an escape pod where it came down, on a scorched patch still smoking, with
 *     one block of wreckage worth prying at.</li>
 * </ul>
 */
public final class ShipBlueprints {
    private ShipBlueprints() {}

    public static final String SHIP = "taken/ship", POD_WRECK = "taken/pod_wreck";
    /** Where someone set down in the wreck stands (the template's own coordinates). */
    public static final BlockPos WRECK_SEAT = new BlockPos(3, 1, 3);

    public static List<Blueprint> all() {
        return List.of(ship(), podWreck());
    }

    private static final BlockState HULL = ModBlocks.SAUCER_HULL.get().defaultBlockState();
    private static final BlockState WALL = Blocks.CONCRETE.white().defaultBlockState();
    private static final BlockState FLOOR = Blocks.CONCRETE.lightGray().defaultBlockState();
    private static final BlockState CELL_FLOOR = Blocks.CONCRETE.gray().defaultBlockState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState GLASS = Blocks.TINTED_GLASS.defaultBlockState();

    /** The grille over a vent: the one thing aboard that comes away in the hands. */
    public static BlockState grille() {
        return Blocks.COPPER_GRATE.waxed().oxidized().defaultBlockState();
    }

    public static Blueprint ship() {
        Blueprint b = new Blueprint(SHIP);
        // The disc: hull under and over, and between them each ring of the plan
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double r = ShipLayout.radius(x, z);
                if (r > ShipLayout.HULL_R + 1.5) {
                    continue;
                }
                if (r > ShipLayout.HULL_R) { // the rim
                    b.set(x, 3, z, HULL);
                    b.set(x, 4, z, (x + z) % 3 == 0 ? LIGHT : HULL);
                    continue;
                }
                b.set(x, 0, z, HULL);
                boolean dome = r <= ShipLayout.VENT_IN;
                b.set(x, dome ? 9 : CEILING + 1, z, HULL);
                if (r > ShipLayout.ROOM_OUT) {
                    b.fill(x, 1, z, x, CEILING, z, HULL);
                    continue;
                }
                b.set(x, 1, z, r <= ShipLayout.ENGINE_R ? HULL : FLOOR);
                if (!dome) {
                    b.set(x, CEILING, z, Blocks.SMOOTH_QUARTZ);
                }
                int top = dome ? 8 : CEILING - 1;
                boolean south = z > C && Math.abs(x - C) <= 2;
                BlockState fill;
                if (r <= ShipLayout.ENGINE_R) {
                    fill = AIR;
                } else if (r <= ShipLayout.VENT_IN) {
                    fill = WALL;
                } else if (r <= ShipLayout.VENT_OUT) {
                    fill = south ? WALL : null; // the duct: two high, shut where the engine room's door runs through
                } else if (r <= ShipLayout.RING_IN) {
                    fill = WALL;
                } else if (r <= ShipLayout.RING_OUT) {
                    fill = AIR;
                } else if (r <= ShipLayout.ROOM_IN) {
                    fill = WALL;
                } else {
                    fill = ShipLayout.onDiagonal(x, z) ? WALL : AIR;
                }
                if (fill == null) {
                    b.set(x, 1, z, HULL);
                    b.fill(x, DECK, z, x, DECK + 1, z, AIR);
                    b.fill(x, DECK + 2, z, x, top, z, WALL);
                } else {
                    b.fill(x, DECK, z, x, top, z, fill);
                }
            }
        }
        // The ring's lights: sixteen in the ceiling, and a strip let into the floor between them
        for (int i = 0; i < 32; i++) {
            double a = Math.PI / 16 * i;
            int x = C + (int) Math.round(Math.sin(a) * 14), z = C - (int) Math.round(Math.cos(a) * 14);
            b.set(x, i % 2 == 0 ? CEILING : 1, z, LIGHT);
        }
        // Cover: crates of plate against the ring's outer wall, between the doors, to stand behind
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 8 + Math.PI / 4 * i;
            for (double along : new double[] {-0.04, 0.04}) {
                int x = C + (int) Math.round(Math.sin(a + along) * 15.4), z = C - (int) Math.round(Math.cos(a + along) * 15.4);
                if (ShipLayout.radius(x, z) > ShipLayout.RING_IN + 2.5 && ShipLayout.radius(x, z) <= ShipLayout.RING_OUT) {
                    b.fill(x, DECK, z, x, DECK + 1, z, HULL);
                }
            }
        }
        for (Side side : Side.values()) {
            // The doorway off the ring, three wide and three high under a lintel; its field is the ship's to raise
            carve(b, side, -1, 1, 16, 18, DECK, DECK + 2);
            for (int u = -12; u <= 12; u += 4) {
                light(b, side, u, 20);
                light(b, side, u, 24);
            }
        }
        // The engine room's door, south: a short passage through both walls and the duct between them
        carve(b, Side.SOUTH, -1, 1, 7, 12, DECK, DECK + 2);
        // The grilles: two off the ring and one into the engine room, each one wide and two high
        for (BlockPos grille : ShipLayout.GRILLES) {
            b.fill(grille.getX(), DECK, grille.getZ(), grille.getX(), DECK + 1, grille.getZ(), grille());
        }
        carve(b, Side.NORTH, 0, 0, 11, 11, DECK, DECK + 1);
        carve(b, Side.WEST, 0, 0, 11, 11, DECK, DECK + 1);
        // The walls are banded: a grey foot, and a pale course under the ceiling
        b.map((pos, state) -> state != WALL || ShipLayout.radius(pos.getX(), pos.getZ()) <= ShipLayout.VENT_OUT ? state
                : pos.getY() == DECK ? Blocks.CONCRETE.lightGray().defaultBlockState()
                : pos.getY() == CEILING - 1 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : state);
        pens(b);
        hold(b);
        surgery(b);
        podBay(b);
        engineRoom(b);
        return b;
    }

    /** A lamp let into a room's ceiling, where there is ceiling (not over a wall, and not out in the hull). */
    private static void light(Blueprint b, Side side, int u, int v) {
        BlockPos at = side.at(u, v, CEILING);
        if (ShipLayout.radius(at.getX(), at.getZ()) <= ShipLayout.ROOM_OUT - 0.5 && !ShipLayout.onDiagonal(at.getX(), at.getZ())) {
            b.set(at.getX(), at.getY(), at.getZ(), LIGHT);
        }
    }

    private static void set(Blueprint b, Side side, int u, int v, int y, BlockState state) {
        BlockPos at = side.at(u, v, y);
        b.set(at.getX(), at.getY(), at.getZ(), state);
    }

    private static void fill(Blueprint b, Side side, int u1, int u2, int v1, int v2, int y1, int y2, BlockState state) {
        for (int u = u1; u <= u2; u++) {
            for (int v = v1; v <= v2; v++) {
                for (int y = y1; y <= y2; y++) {
                    BlockPos at = side.at(u, v, y);
                    if (ShipLayout.radius(at.getX(), at.getZ()) <= ShipLayout.ROOM_OUT) { // never through the hull
                        b.set(at.getX(), at.getY(), at.getZ(), state);
                    }
                }
            }
        }
    }

    private static void carve(Blueprint b, Side side, int u1, int u2, int v1, int v2, int y1, int y2) {
        fill(b, side, u1, u2, v1, v2, y1, y2, AIR);
    }

    /** North: five pens along the hull behind fields, a walk in front of them, and the console that drops every field. */
    private static void pens(Blueprint b) {
        Side s = Side.NORTH;
        int front = ShipLayout.PEN_FRONT;
        fill(b, s, -10, 10, front, front, CEILING - 1, CEILING - 1, WALL);
        for (int u = -10; u <= 10; u += 4) {
            fill(b, s, u, u, front, 25, DECK, CEILING - 1, WALL);
        }
        for (int pen = 0; pen < ShipLayout.PENS.length; pen++) {
            fill(b, s, ShipLayout.PENS[pen] - 1, ShipLayout.PENS[pen] + 1, front + 1, 25, 1, 1, CELL_FLOOR);
            for (BlockPos at : ShipLayout.penField(pen)) {
                b.set(at.getX(), at.getY(), at.getZ(), ModBlocks.GRAY_FIELD.get());
            }
        }
        // A strip of light down the walk, and the console by the door
        for (int u = -8; u <= 8; u += 4) {
            set(b, s, u, 20, 1, LIGHT);
        }
        console(b, ShipLayout.PENS_CONSOLE, ShipConsoleBlock.Kind.PENS);
        // Two stacks of crates on the walk, either side of the door: somewhere to be when the door opens
        for (int u : new int[] {-7, 7}) {
            fill(b, s, u, u + (u < 0 ? -1 : 1), 19, 19, DECK, DECK + 1, HULL);
        }
    }

    /** East: the beast's pen and the cattle's either side of the cargo, where what was on the taken is stacked. */
    private static void hold(Blueprint b) {
        Side s = Side.EAST;
        for (int u : new int[] {-10, -2, 2, 10}) {
            fill(b, s, u, u, 22, 25, DECK, CEILING - 1, WALL);
        }
        fill(b, s, -10, -2, 22, 22, CEILING - 1, CEILING - 1, WALL);
        fill(b, s, 2, 10, 22, 22, CEILING - 1, CEILING - 1, WALL);
        for (BlockPos at : ShipLayout.beastField()) {
            b.set(at.getX(), at.getY(), at.getZ(), ModBlocks.GRAY_FIELD.get());
        }
        fill(b, s, 3, 9, 22, 22, DECK, DECK + 2, ModBlocks.GRAY_FIELD.get().defaultBlockState());
        fill(b, s, -9, -3, 23, 25, 1, 1, CELL_FLOOR);
        fill(b, s, 3, 9, 23, 25, 1, 1, Blocks.HAY_BLOCK.defaultBlockState());
        console(b, ShipLayout.HOLD_CONSOLE, ShipConsoleBlock.Kind.HOLD);
        // The cargo: two pods, and plate crates stacked behind them under a lamp
        pod(b, ShipLayout.KIT_POD);
        pod(b, ShipLayout.GADGET_POD);
        fill(b, s, -1, -1, 24, 25, DECK, DECK + 1, HULL);
        set(b, s, 0, 25, DECK, HULL);
        set(b, s, 0, 25, DECK + 1, Blocks.COPPER_BULB.waxed().oxidized().defaultBlockState().setValue(CopperBulbBlock.LIT, true));
    }

    /** South: the table under the rig, its arms either side, a ring of light in the floor, and the tray. */
    private static void surgery(Blueprint b) {
        Side s = Side.SOUTH;
        BlockState slab = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        fill(b, s, -1, 1, 22, 22, DECK, DECK, slab);
        set(b, s, 0, 21, DECK, Blocks.IRON_BLOCK.defaultBlockState());
        set(b, s, 0, 23, DECK, Blocks.IRON_BLOCK.defaultBlockState());
        BlockPos rig = ShipLayout.RIG;
        b.set(rig.getX(), rig.getY(), rig.getZ(), ModBlocks.IMPLANT_RIG.get());
        for (int u : new int[] {-2, 2}) {
            set(b, s, u, 22, CEILING - 1, Blocks.IRON_CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y));
            set(b, s, u, 22, CEILING - 2, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN));
        }
        for (int du = -3; du <= 3; du++) {
            for (int dv = -3; dv <= 3; dv++) {
                if ((Math.abs(du) == 3 || Math.abs(dv) == 3) && (du + dv) % 2 == 0) {
                    set(b, s, du, 22 + dv, 1, LIGHT);
                }
            }
        }
        pod(b, ShipLayout.TRAY_POD);
        set(b, s, -4, 24, DECK, Blocks.BREWING_STAND.defaultBlockState());
        set(b, s, -5, 24, DECK, Blocks.ANVIL.defaultBlockState());
        set(b, s, -6, 23, DECK, Blocks.BREWING_STAND.defaultBlockState());
    }

    /** West: two pods in their cradles, glass round each and a hatch in front, and a desk of lit glass between them. */
    private static void podBay(Blueprint b) {
        Side s = Side.WEST;
        for (int pod = 0; pod < ShipLayout.PODS.length; pod++) {
            int u = ShipLayout.PODS[pod];
            fill(b, s, u - 2, u + 2, 22, 25, DECK, DECK + 2, Blocks.STAINED_GLASS.cyan().defaultBlockState());
            fill(b, s, u - 2, u + 2, 22, 25, DECK + 3, DECK + 3, HULL);
            carve(b, s, u - 1, u + 1, 23, 25, DECK, DECK + 2);
            carve(b, s, u, u, 22, 22, DECK, DECK + 1);
            fill(b, s, u - 1, u + 1, 23, 25, 1, 1, HULL);
            set(b, s, u, 24, 1, LIGHT);
            console(b, ShipLayout.podConsole(pod), ShipConsoleBlock.Kind.POD);
        }
        fill(b, s, -1, 1, 24, 24, DECK, DECK, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        BlockState bulb = Blocks.COPPER_BULB.waxed().oxidized().defaultBlockState().setValue(CopperBulbBlock.LIT, true);
        set(b, s, -1, 25, DECK, bulb);
        set(b, s, 1, 25, DECK, bulb);
        set(b, s, 0, 25, DECK + 1, GLASS);
    }

    /**
     * The middle: the Concavians' engine, taken with everything else. A pillar of phlogiston in crystal up into the dome, and
     * round it the three sockets its cells sit in.
     */
    private static void engineRoom(Blueprint b) {
        BlockPos core = ShipLayout.CORE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                b.fill(core.getX() + dx, DECK, core.getZ() + dz, core.getX() + dx, ShipLayout.CORE_TOP, core.getZ() + dz,
                        dx == 0 && dz == 0 ? ModBlocks.PHLOGISTON_LAMP.get().defaultBlockState() : ModBlocks.CONCAVIAN_CRYSTAL.get().defaultBlockState());
            }
        }
        for (int[] at : new int[][] {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            b.set(core.getX() + at[0], 1, core.getZ() + at[1], ModBlocks.PHLOGISTON_LAMP.get());
        }
        for (BlockPos socket : ShipLayout.SOCKETS) {
            b.set(socket.getX(), socket.getY(), socket.getZ(), ModBlocks.CORE_SOCKET.get().defaultBlockState().setValue(CoreSocketBlock.CELL, true));
            int dx = Integer.signum(core.getX() - socket.getX()), dz = Integer.signum(core.getZ() - socket.getZ());
            Direction toCore = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
            b.set(socket.getX() + dx, DECK, socket.getZ() + dz, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, toCore));
        }
    }

    private static void console(Blueprint b, BlockPos at, ShipConsoleBlock.Kind kind) {
        b.set(at.getX(), at.getY(), at.getZ(), ModBlocks.SHIP_CONSOLE.get().defaultBlockState().setValue(ShipConsoleBlock.KIND, kind));
    }

    private static void pod(Blueprint b, BlockPos at) {
        b.set(at.getX(), at.getY(), at.getZ(), ModBlocks.GRAY_CARGO_POD.get().defaultBlockState());
    }

    /** An escape pod where it came down: its floor row replaces the ground's top block. */
    public static Blueprint podWreck() {
        Blueprint b = new Blueprint(POD_WRECK);
        Random r = new Random(50);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                double d = Math.hypot(x - 3, z - 3);
                if (d <= 3.3) {
                    b.set(x, 0, z, switch (r.nextInt(4)) {
                        case 0 -> Blocks.BASALT.defaultBlockState();
                        case 1 -> Blocks.BLACKSTONE.defaultBlockState();
                        default -> Blocks.COARSE_DIRT.defaultBlockState();
                    });
                    b.fill(x, 1, z, x, 3, z, AIR);
                }
            }
        }
        b.set(1, 0, 3, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        b.set(5, 0, 4, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        // What's left of the cabin: two corners of plate, a pane that held, and the part worth prying at
        b.fill(2, 1, 2, 2, 2, 2, HULL);
        b.set(4, 1, 2, HULL);
        b.set(4, 2, 2, GLASS);
        b.set(2, 1, 4, HULL);
        b.set(3, 1, 5, GLASS);
        b.set(4, 1, 4, ModBlocks.SAUCER_WRECKAGE.get());
        return b;
    }
}
