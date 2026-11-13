package com.hollowfrontier.content.ufo.taken;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Where everything is aboard the Grays' ship (50), in the template's own coordinates: one source for the blueprint that
 * draws it ({@code ShipBlueprints}) and the code that runs it ({@link Ship}).
 * <p>
 * The ship is a disc {@link #SIZE} across, its middle at ({@link #C}, {@link #C}), its deck at {@link #DECK} (the
 * walking height) under a ceiling four blocks up. From the middle out: the <b>engine room</b> (to {@link #ENGINE_R}),
 * a wall, the <b>vents</b> (a duct two wide and two high, {@link #VENT_IN} to {@link #VENT_OUT}, shut at the south where
 * the engine room's door runs through), a wall, the <b>ring</b> corridor ({@link #RING_IN} to {@link #RING_OUT}), a wall,
 * and four <b>rooms</b> ({@link #ROOM_IN} to {@link #ROOM_OUT}) parted along the diagonals: the pens north, the hold east,
 * the surgery south, the pod bay west. Then the hull.
 * <p>
 * A room is drawn in its own frame, {@code (u, v)}: {@code v} out from the middle along the room's axis, {@code u} across
 * it (to the right as you look out), so one drawing serves any of the four ({@link Side#at}).
 */
public final class ShipLayout {
    private ShipLayout() {}

    public static final int SIZE = 59, HEIGHT = 12, C = 29;
    /** The walking height: feet at this y, on the floor block under it. */
    public static final int DECK = 2;
    /** The ceiling's block over the rooms and the ring. */
    public static final int CEILING = 6;
    public static final double ENGINE_R = 7.0, VENT_IN = 8.5, VENT_OUT = 10.5, RING_IN = 12.0, RING_OUT = 16.0, ROOM_IN = 18.0, ROOM_OUT = 26.0,
            HULL_R = 27.0;

    public enum Side {
        NORTH, EAST, SOUTH, WEST;

        /** The template column at {@code u} across and {@code v} out on this side. */
        public BlockPos at(int u, int v, int y) {
            return switch (this) {
                case NORTH -> new BlockPos(C + u, y, C - v);
                case EAST -> new BlockPos(C + v, y, C + u);
                case SOUTH -> new BlockPos(C - u, y, C + v);
                case WEST -> new BlockPos(C - v, y, C - u);
            };
        }

        public BlockPos deck(int u, int v) {
            return at(u, v, DECK);
        }
    }

    public static double radius(int x, int z) {
        return Math.hypot(x - C, z - C);
    }

    /** Along a diagonal: where the walls between the rooms stand. */
    public static boolean onDiagonal(int x, int z) {
        return Math.abs(Math.abs(x - C) - Math.abs(z - C)) <= 1;
    }

    /** In the duct between the engine room and the ring: the crew don't look in there, and can't follow. */
    public static boolean inVent(double localX, double localZ) {
        double r = Math.hypot(localX - (C + 0.5), localZ - (C + 0.5));
        return r > VENT_IN - 0.5 && r <= VENT_OUT + 0.5;
    }

    // ---------------------------------------------------------------- the pens (north)

    /** Each pen's middle across the room; pens are three wide and run from {@link #PEN_FRONT} + 1 back to the hull. */
    public static final int[] PENS = {-8, -4, 0, 4, 8};
    /** The pen the taken wake in. */
    public static final int PLAYER_PEN = 2;
    public static final int PEN_FRONT = 22;
    public static final BlockPos WAKE = Side.NORTH.deck(0, 24);
    public static final BlockPos PENS_CONSOLE = Side.NORTH.deck(3, 19);
    /** Who's held, by pen (the player's is empty, and so is the last). */
    public static final int[] CAPTIVE_PENS = {0, 1, 3};

    /** The field across a pen's front: three wide and three high. */
    public static List<BlockPos> penField(int pen) {
        List<BlockPos> field = new ArrayList<>();
        for (int u = PENS[pen] - 1; u <= PENS[pen] + 1; u++) {
            for (int y = DECK; y <= DECK + 2; y++) {
                field.add(Side.NORTH.at(u, PEN_FRONT, y));
            }
        }
        return field;
    }

    public static BlockPos penMiddle(int pen) {
        return Side.NORTH.deck(PENS[pen], 24);
    }

    /** Where a surgeon stands to drop the player's field. */
    public static final BlockPos PEN_CALL = Side.NORTH.deck(0, 20);

    // ---------------------------------------------------------------- the hold (east)

    public static final BlockPos KIT_POD = Side.EAST.deck(0, 24);
    public static final BlockPos GADGET_POD = Side.EAST.deck(1, 25);
    public static final BlockPos HOLD_CONSOLE = Side.EAST.deck(-2, 22);
    public static final BlockPos BEAST = Side.EAST.deck(-6, 24);
    public static final BlockPos[] CATTLE = {Side.EAST.deck(5, 24), Side.EAST.deck(7, 24)};

    /** The field across the beast's pen. */
    public static List<BlockPos> beastField() {
        List<BlockPos> field = new ArrayList<>();
        for (int u = -9; u <= -3; u++) {
            for (int y = DECK; y <= DECK + 2; y++) {
                field.add(Side.EAST.at(u, 22, y));
            }
        }
        return field;
    }

    // ---------------------------------------------------------------- the surgery (south)

    /** The rig hangs here; the table is under it and the patient stands on the table. */
    public static final BlockPos RIG = Side.SOUTH.at(0, 22, DECK + 3);
    public static final BlockPos PATIENT = Side.SOUTH.at(0, 22, DECK + 1);
    public static final BlockPos TRAY_POD = Side.SOUTH.deck(4, 24);
    public static final BlockPos[] SURGEONS = {Side.SOUTH.deck(-2, 21), Side.SOUTH.deck(2, 23)};

    // ---------------------------------------------------------------- the pod bay (west)

    /** Each pod's middle across the bay; a pod's cabin is three by three from v 23 to 25, its console at the back. */
    public static final int[] PODS = {-5, 5};

    public static BlockPos podConsole(int pod) {
        return Side.WEST.deck(PODS[pod], 25);
    }

    /** Inside a pod, and the step in front of its hatch: who goes when it's launched. */
    public static AABB podCabin(int pod) {
        BlockPos a = Side.WEST.at(PODS[pod] - 1, 22, DECK), b = Side.WEST.at(PODS[pod] + 1, 25, DECK + 3);
        return new AABB(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, b.getY(), Math.max(a.getZ(), b.getZ()) + 1);
    }

    /** Which pod a console belongs to, or -1. */
    public static int podAt(BlockPos local) {
        for (int pod = 0; pod < PODS.length; pod++) {
            if (podConsole(pod).equals(local)) {
                return pod;
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------- the engine room (middle)

    public static final BlockPos CORE = new BlockPos(C, DECK, C);
    public static final int CORE_TOP = 8;
    /** The three cells' sockets, round the core. */
    public static final BlockPos[] SOCKETS = {new BlockPos(C, DECK, C - 3), new BlockPos(C + 3, DECK, C), new BlockPos(C - 3, DECK, C)};

    // ---------------------------------------------------------------- doors, vents, rounds

    /** A doorway's field: three wide and three high across {@code side}'s axis at {@code v}. */
    public static List<BlockPos> door(Side side, int v) {
        List<BlockPos> field = new ArrayList<>();
        for (int u = -1; u <= 1; u++) {
            for (int y = DECK; y <= DECK + 2; y++) {
                field.add(side.at(u, v, y));
            }
        }
        return field;
    }

    public static final int ROOM_DOOR_V = 17, ENGINE_DOOR_V = 11;

    /** Every door the alarm shuts: the four rooms' and the engine room's. */
    public static List<List<BlockPos>> doors() {
        List<List<BlockPos>> doors = new ArrayList<>();
        for (Side side : Side.values()) {
            doors.add(door(side, ROOM_DOOR_V));
        }
        doors.add(door(Side.SOUTH, ENGINE_DOOR_V));
        return doors;
    }

    /** The grilles: off the ring north and west, and into the engine room east. Each is one wide and two high. */
    public static final BlockPos[] GRILLES = {Side.NORTH.deck(0, 12), Side.WEST.deck(0, 12), Side.EAST.deck(0, 8)};

    /** The Warden's round: the ring, a point every eighth of the way, clockwise from the north. */
    public static List<BlockPos> round() {
        List<BlockPos> points = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            points.add(new BlockPos(C + (int) Math.round(Math.sin(a) * 14), DECK, C - (int) Math.round(Math.cos(a) * 14)));
        }
        return points;
    }

    /**
     * A way along the ring from one side's door to another's, the short way round: the points of {@link #round} between
     * them, both ends included. (The crew find a path a quarter of the ring at a time, not across the whole ship.)
     */
    public static List<BlockPos> along(Side from, Side to) {
        List<BlockPos> ring = round();
        int a = from.ordinal() * 2, b = to.ordinal() * 2, n = ring.size();
        int step = Math.floorMod(b - a, n) <= n / 2 ? 1 : -1;
        List<BlockPos> way = new ArrayList<>();
        for (int i = a; ; i = Math.floorMod(i + step, n)) {
            way.add(ring.get(i));
            if (i == b) {
                return way;
            }
        }
    }

    // ---------------------------------------------------------------- template to world

    /** Whether a spot (in the template's coordinates) is inside the hull. */
    public static boolean inside(BlockPos local) {
        return local.getY() >= 0 && local.getY() < HEIGHT && radius(local.getX(), local.getZ()) <= HULL_R + 2;
    }

    public static Vec3 feet(BlockPos origin, BlockPos local) {
        return new Vec3(origin.getX() + local.getX() + 0.5, origin.getY() + local.getY(), origin.getZ() + local.getZ() + 0.5);
    }
}
