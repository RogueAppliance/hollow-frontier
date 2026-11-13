package com.hollowfrontier.worldgen.structure;

import static com.hollowfrontier.worldgen.structure.TownBlueprints.candles;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.facing;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.front;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.gableRoof;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.hangingLantern;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.log;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.s;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.shopfront;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.slab;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.stairs;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.tag;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.water;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import net.minecraft.world.item.Items;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The grand buildings and the edge of town: a courthouse with a clock tower and a church with a steeple (both stand on
 * the town square), a grand hotel and a two-storey saloon (alternatives to the plain ones), the railroad depot with its
 * track and water tank (at the end of a street), and out past the lanes a windpump, a corral and Boot Hill. Homes'
 * lanes also get vegetable gardens. Wheat fields stand back from every street (a cart track leads to each from the
 * street's last lot) and out past some lanes, ripe and big enough for a whole crop circle. Same rules as TownBlueprints:
 * drawn facing north, front connector mid z 0, floor y 0.
 */
public final class TownLandmarks {
    private TownLandmarks() {}

    static List<Blueprint> all() {
        return List.of(courthouse(), church(), grandHotel(), grandSaloon(), depot(), windpump(), corral(), bootHill(), bootHillB(), bootHillC(), bootHillStrange(),
                TownLots.inLot(garden()), townLimits(), wheatField(), ranch(), OldPlaceBlueprint.oldPlace());
    }

    // ---------------------------------------------------------------- on the square

    // 17 x 16: brick courthouse on a stone plinth, a portico of four columns under a pediment, a clock tower with a bell.
    static Blueprint courthouse() {
        Blueprint b = new Blueprint("town/courthouse");
        int w = 17, d = 16, cx = 8;
        b.air(0, 1, 0, w - 1, 22, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.STONE_BRICKS);
        // Steps up to the portico
        b.fill(3, 1, 0, w - 4, 1, 0, stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        b.fill(2, 1, 1, w - 3, 1, 4, Blocks.STONE_BRICKS);
        // The hall: two storeys of brick with sandstone string courses and corner quoins
        b.walls(0, 1, 5, w - 1, 11, d - 1, s(Blocks.BRICKS));
        b.walls(0, 6, 5, w - 1, 6, d - 1, s(Blocks.SMOOTH_SANDSTONE));
        b.walls(0, 11, 5, w - 1, 11, d - 1, s(Blocks.SMOOTH_SANDSTONE));
        for (int[] c : new int[][] {{0, 5}, {w - 1, 5}, {0, d - 1}, {w - 1, d - 1}}) {
            b.fill(c[0], 1, c[1], c[0], 11, c[1], Blocks.CUT_SANDSTONE);
        }
        b.fill(1, 12, 6, w - 2, 12, d - 2, slab(Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
        b.walls(0, 12, 5, w - 1, 12, d - 1, s(Blocks.SMOOTH_SANDSTONE));    // parapet
        // Portico: columns, entablature and a stepped pediment
        for (int x : new int[] {3, 6, 10, 13}) {
            b.fill(x, 2, 2, x, 9, 2, Blocks.QUARTZ_PILLAR);
        }
        b.fill(2, 10, 1, w - 3, 10, 4, Blocks.SMOOTH_SANDSTONE);
        for (int i = 0; i < 4; i++) {
            b.fill(2 + 2 * i, 11 + i, 1, w - 3 - 2 * i, 11 + i, 1, s(Blocks.SMOOTH_SANDSTONE));
            b.set(2 + 2 * i, 11 + i, 1, stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.EAST, Half.BOTTOM));
            b.set(w - 3 - 2 * i, 11 + i, 1, stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.WEST, Half.BOTTOM));
        }
        b.set(cx, 13, 0, Blocks.GLAZED_TERRACOTTA.white());   // a clock in the pediment too
        // Great doors (standing open) and tall windows
        b.air(cx - 1, 2, 5, cx + 1, 4, 5);
        b.door(cx - 1, 2, 5, (DoorBlock) Blocks.DARK_OAK_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.door(cx + 1, 2, 5, (DoorBlock) Blocks.DARK_OAK_DOOR, Direction.NORTH, DoorHingeSide.RIGHT);
        // A column between the two doors (an open gap there read as a missing door), the keystone over it
        b.fill(cx, 2, 5, cx, 3, 5, Blocks.QUARTZ_PILLAR);
        b.set(cx, 4, 5, Blocks.CHISELED_SANDSTONE);
        b.set(cx - 1, 4, 5, Blocks.SMOOTH_SANDSTONE);                     // lintels over the doors (the opening was a block too tall)
        b.set(cx + 1, 4, 5, Blocks.SMOOTH_SANDSTONE);
        b.fill(1, 1, 6, w - 2, 1, d - 2, Blocks.DARK_OAK_PLANKS);          // floor, a step up from the plinth
        for (int x : new int[] {2, 4, 12, 14}) {
            b.fill(x, 3, 5, x, 4, 5, Blocks.GLASS_PANE);
            b.fill(x, 8, 5, x, 9, 5, Blocks.GLASS_PANE);
        }
        for (int z : new int[] {8, 11, 13}) {
            b.fill(0, 3, z, 0, 4, z, Blocks.GLASS_PANE);
            b.fill(w - 1, 3, z, w - 1, 4, z, Blocks.GLASS_PANE);
            b.fill(0, 8, z, 0, 9, z, Blocks.GLASS_PANE);
            b.fill(w - 1, 8, z, w - 1, 9, z, Blocks.GLASS_PANE);
        }
        // Courtroom: the judge's bench raised at the back, a bar rail, the jury box and pews for the public
        b.fill(5, 2, d - 3, w - 6, 2, d - 2, Blocks.DARK_OAK_PLANKS);
        b.fill(6, 3, d - 4, w - 7, 3, d - 4, ModBlocks.BAR_COUNTER.get());
        b.set(cx, 3, d - 2, stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        b.set(cx - 2, 4, d - 2, Blocks.BANNER.red());
        b.set(cx + 2, 4, d - 2, Blocks.BANNER.blue());
        b.fill(2, 2, d - 6, w - 3, 2, d - 6, Blocks.DARK_OAK_FENCE);
        b.set(cx, 2, d - 6, Blocks.DARK_OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        b.fill(1, 2, d - 5, 3, 2, d - 3, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, Half.BOTTOM)); // jury
        for (int z = 6; z <= d - 8; z += 2) {
            b.fill(2, 2, z, cx - 2, 2, z, stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
            b.fill(cx + 2, 2, z, w - 3, 2, z, stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
        }
        b.set(cx, 2, d - 5, facing(Blocks.LECTERN, Direction.NORTH));
        b.set(cx, 5, 9, hangingLantern());
        b.set(cx, 5, d - 4, hangingLantern());
        b.entity(ModEntities.TOWNSFOLK.get(), cx + 3, 2, d - 3, tag("folk", "banker"));   // the clerk of the court
        // Upstairs: the land office (a floor over the front half, reached by stairs along the east wall)
        b.fill(1, 6, 6, w - 2, 6, 9, Blocks.DARK_OAK_PLANKS);
        for (int i = 0; i < 5; i++) {                                     // the last step set in the gallery floor
            b.set(w - 2, 2 + i, 13 - i, stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
        }
        b.fill(2, 7, 9, 5, 8, 9, Blocks.BOOKSHELF);                       // shelves along the gallery edge
        b.chest(cx, 7, 6, Direction.SOUTH, ModChestLoot.TOWN_HOUSE);
        b.set(cx, 10, 8, hangingLantern());
        // Clock tower over the middle of the roof: brick shaft, a clock on each face, open belfry, bell, cupola
        int tx0 = cx - 2, tx1 = cx + 2, tz0 = 8, tz1 = 12;
        b.walls(tx0, 13, tz0, tx1, 17, tz1, s(Blocks.BRICKS));
        b.walls(tx0, 15, tz0, tx1, 15, tz1, s(Blocks.SMOOTH_SANDSTONE));
        b.set(cx, 16, tz0, Blocks.GLAZED_TERRACOTTA.white());
        b.set(cx, 16, tz1, Blocks.GLAZED_TERRACOTTA.white());
        b.set(tx0, 16, 10, Blocks.GLAZED_TERRACOTTA.white());
        b.set(tx1, 16, 10, Blocks.GLAZED_TERRACOTTA.white());
        b.fill(tx0, 18, tz0, tx1, 18, tz1, Blocks.SMOOTH_SANDSTONE);
        b.air(tx0 + 1, 19, tz0, tx1 - 1, 20, tz1);
        b.air(tx0, 19, tz0 + 1, tx1, 20, tz1 - 1);
        for (int[] c : new int[][] {{tx0, tz0}, {tx1, tz0}, {tx0, tz1}, {tx1, tz1}}) {
            b.fill(c[0], 19, c[1], c[0], 20, c[1], Blocks.QUARTZ_PILLAR);
        }
        b.fill(tx0, 21, tz0, tx1, 21, tz1, Blocks.SMOOTH_SANDSTONE);
        b.set(cx, 20, 10, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.NORTH));
        b.fill(tx0 + 1, 22, tz0 + 1, tx1 - 1, 22, tz1 - 1, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        b.set(cx, 22, 10, Blocks.DARK_OAK_PLANKS);
        b.set(cx, 23, 10, Blocks.DARK_OAK_FENCE);
        b.set(cx, 24, 10, Blocks.BANNER.red());
        // Lamps either side of the steps
        for (int x : new int[] {1, w - 2}) {
            b.fill(x, 1, 0, x, 2, 0, Blocks.DARK_OAK_FENCE);
            b.set(x, 3, 0, TownDecor.lamp(false));
        }
        front(b, cx, s(Blocks.STONE_BRICKS));
        return b;
    }

    // 11 x 22 white clapboard church: an entrance tower carrying an open belfry and a tall spire, pews, an altar.
    static Blueprint church() {
        Blueprint b = new Blueprint("town/church");
        int w = 11, d = 22, cx = 5;
        b.air(0, 1, 0, w - 1, 26, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.SPRUCE_PLANKS);
        b.fill(cx - 1, 0, 0, cx + 1, 0, 0, Blocks.STONE_BRICKS);
        // Nave
        b.walls(0, 1, 5, w - 1, 7, d - 1, s(Blocks.BIRCH_PLANKS));
        for (int[] c : new int[][] {{0, 5}, {w - 1, 5}, {0, d - 1}, {w - 1, d - 1}}) {
            b.fill(c[0], 1, c[1], c[0], 7, c[1], log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y));
        }
        gableRoof(b, w, 5, d - 1, 8, Blocks.DARK_OAK_STAIRS, s(Blocks.DARK_OAK_PLANKS), s(Blocks.BIRCH_PLANKS));
        for (int z = 8; z < d - 2; z += 3) {
            b.fill(0, 2, z, 0, 5, z, Blocks.STAINED_GLASS_PANE.yellow());
            b.fill(w - 1, 2, z, w - 1, 5, z, Blocks.STAINED_GLASS_PANE.yellow());
        }
        b.fill(cx - 1, 9, d - 1, cx + 1, 11, d - 1, Blocks.STAINED_GLASS_PANE.red()); // rose window over the altar
        // Entrance tower (z 0..5), the door in its front, into the nave through an arch
        int tx0 = cx - 2, tx1 = cx + 2;
        b.walls(tx0, 1, 1, tx1, 15, 5, s(Blocks.BIRCH_PLANKS));
        for (int[] c : new int[][] {{tx0, 1}, {tx1, 1}, {tx0, 5}, {tx1, 5}}) {
            b.fill(c[0], 1, c[1], c[0], 15, c[1], log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y));
        }
        b.door(cx, 1, 1, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.set(cx, 3, 1, Blocks.STAINED_GLASS_PANE.white());
        b.set(cx, 8, 1, Blocks.GLAZED_TERRACOTTA.white());  // the town clock
        b.air(cx - 1, 1, 5, cx + 1, 3, 5);
        b.fill(tx0 + 1, 10, 2, tx1 - 1, 10, 4, Blocks.SPRUCE_PLANKS);         // belfry floor
        b.fill(tx0 + 1, 15, 2, tx1 - 1, 15, 4, Blocks.SPRUCE_PLANKS);         // belfry ceiling, the bell hangs from it
        // Open belfry with the bell, then the spire
        b.air(tx0 + 1, 12, 1, tx1 - 1, 14, 5);
        b.air(tx0, 12, 2, tx1, 14, 4);
        b.set(cx, 14, 3, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.NORTH));
        b.fill(tx0, 16, 1, tx1, 16, 5, slab(Blocks.DARK_OAK_SLAB, SlabType.TOP));
        b.fill(tx0 + 1, 17, 2, tx1 - 1, 20, 4, Blocks.DARK_OAK_PLANKS);
        b.fill(tx0 + 1, 21, 2, tx1 - 1, 21, 4, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        b.fill(cx, 21, 3, cx, 23, 3, Blocks.DARK_OAK_FENCE);
        b.set(cx - 1, 24, 3, Blocks.DARK_OAK_FENCE);
        b.set(cx + 1, 24, 3, Blocks.DARK_OAK_FENCE);
        b.set(cx, 24, 3, Blocks.DARK_OAK_FENCE);
        b.set(cx, 25, 3, Blocks.DARK_OAK_FENCE);
        // Inside: an aisle, pews, the altar with candles, an organ of note blocks and bookshelves
        b.fill(cx, 1, 6, cx, 1, d - 4, Blocks.CARPET.red());
        for (int z = 7; z <= d - 6; z += 2) {
            b.fill(1, 1, z, cx - 2, 1, z, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.BOTTOM));
            b.fill(cx + 2, 1, z, w - 2, 1, z, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.BOTTOM));
        }
        b.fill(2, 1, d - 3, w - 3, 1, d - 3, Blocks.SPRUCE_PLANKS);
        b.set(cx, 1, d - 4, facing(Blocks.LECTERN, Direction.NORTH));
        b.entity(ModEntities.TOWNSFOLK.get(), cx + 1, 1, d - 4, tag("folk", "preacher")); // the preacher by his lectern (24c)
        b.set(2, 2, d - 3, candles(4));
        b.set(w - 3, 2, d - 3, candles(4));
        b.fill(1, 1, d - 2, 2, 3, d - 2, Blocks.NOTE_BLOCK);
        b.fill(w - 3, 1, d - 2, w - 2, 2, d - 2, Blocks.BOOKSHELF);
        b.set(cx, 7, 10, hangingLantern());
        b.set(cx, 7, 16, hangingLantern());
        front(b, cx, s(Blocks.STONE_BRICKS));
        return b;
    }

    // ---------------------------------------------------------------- grand versions

    // 13 x 13, three storeys: a balcony on each upper floor, lobby with a jukebox, eight rooms.
    static Blueprint grandHotel() {
        Blueprint b = new Blueprint("town/hotel_grand");
        int w = 13, d = 13, cx = 6;
        shopfront(b, w, d, 13, 2, 5, s(Blocks.BIRCH_PLANKS), log(Blocks.DARK_OAK_LOG, Direction.Axis.Y),
                s(Blocks.BIRCH_PLANKS), Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_SLAB, (DoorBlock) Blocks.DARK_OAK_DOOR);
        for (int y : new int[] {5, 9}) {
            b.fill(0, y, 0, w - 1, y, 1, Blocks.SPRUCE_PLANKS);
            b.fill(0, y + 1, 0, w - 1, y + 1, 0, Blocks.DARK_OAK_FENCE);
            b.fill(0, y + 1, 1, 0, y + 1, 1, Blocks.DARK_OAK_FENCE);
            b.fill(w - 1, y + 1, 1, w - 1, y + 1, 1, Blocks.DARK_OAK_FENCE);
            b.door(cx, y + 1, 2, (DoorBlock) Blocks.DARK_OAK_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
            b.fill(2, y + 2, 2, 3, y + 3, 2, Blocks.GLASS_PANE);
            b.fill(w - 4, y + 2, 2, w - 3, y + 3, 2, Blocks.GLASS_PANE);
            b.set(0, y + 2, 7, Blocks.GLASS_PANE);
            b.set(w - 1, y + 2, 7, Blocks.GLASS_PANE);
            b.set(cx, y + 2, 0, TownDecor.lamp(false));                    // a lamp stood on the balcony rail (lit at dusk)
            // A floor of rooms
            b.fill(1, y, 3, w - 2, y, d - 2, Blocks.SPRUCE_PLANKS);
            b.fill(cx - 1, y + 1, 7, cx - 1, y + 3, d - 2, Blocks.BIRCH_PLANKS);
            b.fill(cx + 1, y + 1, 7, cx + 1, y + 3, d - 2, Blocks.BIRCH_PLANKS);
            // The rooms' front walls (23): without them both rooms lay open to the landing, their doors for nothing.
            // The east one stops short of the flight up at x 10.
            b.fill(1, y + 1, 6, cx - 1, y + 3, 6, Blocks.BIRCH_PLANKS);
            b.fill(cx + 1, y + 1, 6, w - 4, y + 3, 6, Blocks.BIRCH_PLANKS);
            b.door(cx - 1, y + 1, 9, (DoorBlock) Blocks.DARK_OAK_DOOR, Direction.EAST, DoorHingeSide.LEFT);
            b.door(cx + 1, y + 1, 9, (DoorBlock) Blocks.DARK_OAK_DOOR, Direction.WEST, DoorHingeSide.RIGHT);
            b.bed(2, y + 1, 9, Blocks.BED.red(), Direction.WEST);
            if (y == 9) { // the flight up lands in this room: its bed against the partition, out of the way
                b.bed(cx + 2, y + 1, d - 3, Blocks.BED.blue(), Direction.SOUTH);
            } else {
                b.bed(w - 3, y + 1, d - 2, Blocks.BED.blue(), Direction.EAST);
            }
            b.chest(2, y + 1, d - 2, Direction.NORTH, ModChestLoot.HOTEL_ROOM);
        }
        // Stairs up the east side, each flight walked onto straight from a landing and its last step set in the floor
        // above: ground to first floor along the wall from the back of the lobby (northward, railed off from the east room
        // it rises through), then first to second floor just inside it from the front of the hall (southward).
        for (int i = 0; i < 5; i++) {
            b.set(w - 2, 1 + i, d - 3 - i, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.BOTTOM));
        }
        b.air(w - 2, 5, d - 6, w - 2, 5, d - 3);
        b.fill(w - 3, 6, d - 6, w - 3, 6, d - 3, Blocks.SPRUCE_FENCE);
        for (int i = 0; i < 4; i++) {
            b.set(w - 3, 6 + i, 4 + i, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, Half.BOTTOM));
        }
        b.air(w - 3, 9, 4, w - 3, 9, 6);
        // Lobby: front desk, a jukebox, a rug, armchairs and a potted palm
        b.fill(1, 0, 3, w - 3, 0, d - 2, Blocks.DARK_OAK_PLANKS);
        b.fill(1, 1, d - 3, 5, 1, d - 3, ModBlocks.BAR_COUNTER.get());
        b.set(cx, 1, d - 2, Blocks.JUKEBOX);                            // an empty jukebox for the lobby
        b.fill(3, 1, 4, 8, 1, 7, Blocks.CARPET.red());
        b.set(2, 1, 5, stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST, Half.BOTTOM));
        b.set(9, 1, 5, stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST, Half.BOTTOM));
        b.set(1, 1, 3, Blocks.POTTED_BAMBOO);
        b.set(2, 2, d - 3, candles(3));
        b.set(cx, 4, 6, hangingLantern());
        // 23: its name over the door, shutters and a cornice, the key rack behind the desk, the guest book
        TownDecor.shopSign(b, cx, 3, "hotel_grand");
        TownDecor.shutters(b, w, Blocks.DARK_OAK_TRAPDOOR);
        TownDecor.cornice(b, w, 13, 2, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y), Blocks.DARK_OAK_STAIRS);
        b.fill(1, 3, d - 2, 5, 3, d - 2, TownBlueprints.pigeonholes(Direction.NORTH));
        b.set(w - 3, 1, 3, Blocks.POTTED_FERN);
        b.entity(ModEntities.TOWNSFOLK.get(), 3, 1, d - 2);
        return b;
    }

    // 15 x 14 two-storey saloon: false front, balcony, a long bar, tables, the piano, rooms upstairs.
    static Blueprint grandSaloon() {
        Blueprint b = new Blueprint("town/saloon_grand");
        int w = 15, d = 14, cx = 7;
        shopfront(b, w, d, 9, 3, 5, s(Blocks.SPRUCE_PLANKS), log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y),
                s(Blocks.SPRUCE_PLANKS), Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_SLAB, (DoorBlock) ModBlocks.SALOON_DOORS.get());
        b.door(cx + 1, 1, 2, (DoorBlock) ModBlocks.SALOON_DOORS.get(), Direction.NORTH, DoorHingeSide.RIGHT);
        // The second door took the wall between the doors and the east window: the window moves a block east
        b.fill(cx + 2, 2, 2, cx + 2, 3, 2, Blocks.SPRUCE_PLANKS);
        b.fill(cx + 4, 2, 2, cx + 4, 3, 2, Blocks.GLASS_PANE);
        b.fill(0, 0, 3, w - 1, 0, d - 1, Blocks.DARK_OAK_PLANKS);
        // Balcony with a door from the upstairs hall
        b.fill(0, 5, 0, w - 1, 5, 1, Blocks.SPRUCE_PLANKS);
        b.fill(0, 6, 0, w - 1, 6, 0, Blocks.DARK_OAK_FENCE);
        b.door(cx, 6, 2, (DoorBlock) Blocks.DARK_OAK_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.fill(2, 7, 2, 4, 8, 2, Blocks.GLASS_PANE);
        b.fill(w - 5, 7, 2, w - 3, 8, 2, Blocks.GLASS_PANE);
        b.fill(4, 8, 1, w - 5, 8, 1, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP)); // sign board
        // The bar along the back, barrels and stills behind it
        b.fill(2, 1, d - 4, w - 5, 1, d - 4, ModBlocks.BAR_COUNTER.get());
        b.fill(1, 1, d - 2, w - 5, 2, d - 2, Blocks.BARREL);
        b.set(4, 3, d - 2, Blocks.BREWING_STAND);
        b.set(9, 3, d - 2, Blocks.BREWING_STAND);
        FrontierBlueprints.backBar(b, d - 2, new int[] {1, 2, 3, 5, 6, 7, 8, 10});
        b.wallSign(cx, 3, 1, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, DyeColor.WHITE, false, "", TownDecor.signKey("saloon"),
                TownDecor.signKey("saloon") + ".sub");
        b.wallSign(cx + 1, 3, 1, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, DyeColor.WHITE, false, "", TownDecor.signKey("saloon_cards"),
                TownDecor.signKey("saloon_cards") + ".sub");
        TownDecor.cornice(b, w, 9, 3, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y), Blocks.DARK_OAK_STAIRS);
        for (int x = 3; x <= w - 6; x += 2) {
            b.set(x, 1, d - 5, ModBlocks.BAR_STOOL.get());
        }
        b.set(2, 2, d - 4, Blocks.LANTERN);
        b.set(w - 5, 2, d - 4, Blocks.LANTERN);
        b.chest(w - 4, 1, d - 2, Direction.NORTH, ModChestLoot.SALOON);
        // Tables: the middle one a card table, in reach of the barmaid who deals (17)
        for (int[] t : new int[][] {{3, 5}, {7, 6}, {11, 5}}) {
            if (t[0] == 7) {
                b.set(t[0], 1, t[1], ModBlocks.CARD_TABLE.get());
            } else {
                b.set(t[0], 1, t[1], ModBlocks.DARK_OAK_TABLE.get());
            }
            b.set(t[0] - 1, 1, t[1], ModBlocks.BAR_STOOL.get());
            b.set(t[0] + 1, 1, t[1], ModBlocks.BAR_STOOL.get());
        }
        b.bountyBoard(1, 1, 4, Direction.EAST, 3); // three across the west wall: six posters
        b.set(w - 2, 1, 8, ModBlocks.SALOON_PIANO.get().defaultBlockState().setValue(SaloonPianoBlock.FACING, Direction.WEST));
        b.set(w - 3, 1, 8, ModBlocks.BAR_STOOL.get());
        // Stairs up the east wall to the rooms, walked onto straight from inside the door, the last step set in the floor
        b.fill(1, 5, 3, w - 2, 5, d - 2, Blocks.SPRUCE_PLANKS);
        b.air(w - 2, 5, 4, w - 2, 5, 7);
        for (int i = 0; i < 5; i++) {
            b.set(w - 2, 1 + i, 4 + i, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, Half.BOTTOM));
        }
        b.fill(1, 6, 9, w - 3, 8, 9, Blocks.SPRUCE_PLANKS);                 // rooms along the back, the hall in front
        for (int x : new int[] {3, 7, 11}) {
            b.door(x, 6, 9, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        }
        b.fill(5, 6, 10, 5, 8, d - 2, Blocks.SPRUCE_PLANKS);
        b.fill(9, 6, 10, 9, 8, d - 2, Blocks.SPRUCE_PLANKS);
        b.fill(w - 3, 6, 10, w - 3, 8, d - 2, Blocks.SPRUCE_PLANKS);       // the east room walled off from the stairhead (it landed you in it)
        b.bed(2, 6, 11, Blocks.BED.red(), Direction.SOUTH);
        b.bed(7, 6, 11, Blocks.BED.purple(), Direction.SOUTH);
        b.bed(11, 6, 11, Blocks.BED.red(), Direction.SOUTH);
        for (int x : new int[] {3, 11}) {
            b.set(x, 4, 6, hangingLantern());
        }
        b.set(cx, 4, 9, hangingLantern());
        b.set(cx, 9, 5, hangingLantern());
        // Hitching rail and a water trough out front
        b.fill(1, 1, 0, 3, 1, 0, Blocks.SPRUCE_FENCE);
        b.set(w - 3, 1, 0, water(3));
        b.set(w - 2, 1, 0, water(3));
        b.entity(ModEntities.BARMAID.get(), cx, 1, d - 3);
        b.cat(3, 2, d - 4, "saloon");                                     // the saloon cat, up on the bar
        b.cat(2, 1, 1, "porch");
        return b;
    }

    // ---------------------------------------------------------------- street ends

    // 25 x 16 railroad depot: the station with a ticket window, a covered platform, the track, a water tank, and the steam
    // standpipe by the track (a boiler over a well, piping into a steam tank) where a handcar takes on steam. A handcar waits
    // on the siding and the station agent is the railroad engineer.
    static Blueprint depot() {
        Blueprint b = new Blueprint("town/depot");
        int w = 25, d = 16, cx = 12;
        b.air(0, 1, 0, w - 1, 14, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.COARSE_DIRT);
        // Station: 13 wide, deep eaves on both sides, the waiting room and ticket office
        int x0 = 6, x1 = 18;
        b.fill(x0 - 1, 0, 0, x1 + 1, 0, 10, Blocks.SPRUCE_PLANKS);
        b.walls(x0, 1, 2, x1, 5, 8, s(Blocks.OAK_PLANKS));
        for (int[] c : new int[][] {{x0, 2}, {x1, 2}, {x0, 8}, {x1, 8}}) {
            b.fill(c[0], 1, c[1], c[0], 5, c[1], log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Y));
        }
        for (int i = 0; i <= 3; i++) {                                     // hipped roof, eaves over the front and the platform
            b.fill(x0 - 1 + i, 6 + i, -0 + i, x1 + 1 - i, 6 + i, 10 - i, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
            if (i < 3) {
                b.fill(x0 + i, 6 + i, 1 + i, x1 - i, 6 + i, 9 - i, Blocks.DARK_OAK_PLANKS);
            }
        }
        for (int x : new int[] {x0 - 1, x1 + 1}) {
            b.fill(x, 1, 0, x, 5, 0, Blocks.SPRUCE_FENCE);
            b.fill(x, 1, 10, x, 5, 10, Blocks.SPRUCE_FENCE);
        }
        b.door(cx, 1, 2, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.door(cx, 1, 8, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.SOUTH, DoorHingeSide.LEFT);
        b.fill(x0 + 2, 2, 2, x0 + 3, 3, 2, Blocks.GLASS_PANE);
        b.fill(x1 - 3, 2, 2, x1 - 2, 3, 2, Blocks.GLASS_PANE);
        b.fill(x0 + 2, 2, 8, x0 + 3, 3, 8, Blocks.GLASS_PANE);
        b.set(x1 - 2, 2, 8, Blocks.IRON_BARS);                             // ticket window onto the platform
        b.fill(x1 - 4, 1, 6, x1 - 1, 1, 6, ModBlocks.BAR_COUNTER.get());
        b.fill(x0 + 1, 1, 4, x0 + 1, 1, 6, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, Half.BOTTOM)); // waiting benches
        b.set(x1 - 1, 1, 3, Blocks.BARREL);
        b.chest(x1 - 1, 1, 7, Direction.WEST, ModChestLoot.FREIGHT);
        b.set(cx, 4, 5, hangingLantern());
        b.entity(ModEntities.TOWNSFOLK.get(), x1 - 2, 1, 7, tag("folk", "engineer")); // the station agent
        // Platform (raised), freight on it, then the track on its gravel bed
        b.fill(0, 1, 9, w - 1, 1, 11, Blocks.SPRUCE_PLANKS);
        b.fill(0, 2, 9, w - 1, 2, 11, Blocks.AIR);
        // Out of the back door at floor level into a notch in the platform, then a step up: a step right at the door
        // would lift your head into the wall over it
        b.set(cx, 1, 9, Blocks.AIR);
        b.set(cx, 1, 10, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, Half.BOTTOM));
        b.set(2, 2, 10, Blocks.BARREL);
        b.set(3, 2, 10, Blocks.BARREL);
        b.set(2, 3, 10, Blocks.BARREL);
        b.set(w - 4, 2, 10, Blocks.HAY_BLOCK);
        b.fill(0, 0, 12, w - 1, 0, 14, Blocks.GRAVEL);
        for (int x = 0; x < w; x++) {
            b.set(x, 0, 13, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z)); // sleepers
            b.set(x, 1, 13, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        b.set(0, 1, 14, Blocks.SPRUCE_FENCE);                              // buffer stops
        b.set(w - 1, 1, 14, Blocks.SPRUCE_FENCE);
        // 24a: the line runs on out of town both ways (TownLayout.rail), joined under the track's ends
        // (one pool for every style: the track is the same in all of them)
        b.jigsaw(0, 0, 13, net.minecraft.core.FrontAndTop.WEST_UP, TownLayout.RAIL, TownLayout.RAIL, TownStyle.FRONTIER.pool(TownLayout.RAILROAD), s(Blocks.GRAVEL));
        b.jigsaw(w - 1, 0, 13, net.minecraft.core.FrontAndTop.EAST_UP, TownLayout.RAIL, TownLayout.RAIL, TownStyle.FRONTIER.pool(TownLayout.RAILROAD), s(Blocks.GRAVEL));
        // Water tank for the locomotives, on four legs by the track, with its spout
        int tx = 20;
        for (int[] c : new int[][] {{tx, 1}, {tx + 3, 1}, {tx, 4}, {tx + 3, 4}}) {
            b.fill(c[0], 1, c[1], c[0], 7, c[1], log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        }
        b.fill(tx, 8, 1, tx + 3, 8, 4, Blocks.SPRUCE_PLANKS);
        b.walls(tx, 9, 1, tx + 3, 11, 4, s(Blocks.SPRUCE_PLANKS));
        b.fill(tx + 1, 9, 2, tx + 2, 10, 3, Blocks.WATER);
        b.fill(tx, 12, 1, tx + 3, 12, 4, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        b.set(tx + 1, 9, 5, Blocks.SPRUCE_FENCE);
        b.set(tx + 1, 8, 5, Blocks.IRON_CHAIN);
        // The standpipe, between the platform and the rail: a boiler (two coal to get it going) fed from a well under it,
        // pushing steam into the tank beside it. A handcar stopped on the rail next to either fills up (SteamHandcar).
        b.set(tx + 2, 0, 12, Blocks.WATER);
        CompoundTag boiler = new CompoundTag();
        boiler.putString("id", HollowFrontier.MODID + ":boiler");
        CompoundTag coal = new CompoundTag();
        coal.putByte("Slot", (byte) 0);
        coal.putString("id", "minecraft:coal");
        coal.putInt("count", 2);
        ListTag items = new ListTag();
        items.add(coal);
        boiler.put("Items", items);
        b.blockEntity(tx + 2, 1, 12, ModBlocks.BOILER.get().defaultBlockState(), boiler);
        b.set(tx + 1, 1, 12, ModBlocks.STEAM_TANK.get());
        b.set(tx + 1, 2, 12, ModBlocks.PRESSURE_GAUGE.get());
        b.entity(ModEntities.STEAM_HANDCAR.get(), 8, 1, 13);             // a handcar on the siding, steam down
        // Lamps and a telegraph pole along the front
        TownLayout.lampPost(b, 2, 1);
        TownLayout.telegraphPole(b, 3, 6);
        front(b, cx, s(Blocks.SPRUCE_PLANKS));
        return b;
    }

    // 9 x 3: where a street just stops: a split-rail fence with a gap, hitching posts, a cactus and some freight.
    static Blueprint townLimits() {
        Blueprint b = new Blueprint("town/town_limits");
        int w = 9;
        b.air(0, 1, 0, w - 1, 4, 2);
        b.fill(0, 0, 0, w - 1, 0, 2, Blocks.COARSE_DIRT);
        b.fill(0, 1, 2, 2, 1, 2, Blocks.SPRUCE_FENCE);
        b.fill(w - 3, 1, 2, w - 1, 1, 2, Blocks.SPRUCE_FENCE);
        b.fill(0, 2, 2, 0, 2, 2, Blocks.SPRUCE_FENCE);
        b.fill(w - 1, 2, 2, w - 1, 2, 2, Blocks.SPRUCE_FENCE);
        b.set(1, 1, 0, Blocks.BARREL);
        b.set(w - 2, 1, 0, Blocks.HAY_BLOCK);
        b.set(w - 2, 1, 1, Blocks.CACTUS);
        front(b, 4, s(Blocks.DIRT_PATH));
        return b;
    }

    // ---------------------------------------------------------------- the edge of town

    // 7 x 11 windpump: a lattice tower with its wheel and tail vane, pumping into a stock tank.
    static Blueprint windpump() {
        Blueprint b = new Blueprint("town/windpump");
        int w = 7, cx = 3;
        b.air(0, 1, 0, w - 1, 17, 10);
        b.fill(0, 0, 0, w - 1, 0, 10, Blocks.COARSE_DIRT);
        for (int[] c : new int[][] {{1, 2}, {5, 2}, {1, 6}, {5, 6}}) {
            b.fill(c[0], 1, c[1], c[0], 11, c[1], Blocks.SPRUCE_FENCE);
        }
        for (int y : new int[] {4, 8}) {
            b.walls(1, y, 2, 5, y, 6, s(Blocks.SPRUCE_FENCE));
        }
        b.fill(2, 12, 3, 4, 12, 5, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.fill(cx, 1, 4, cx, 13, 4, Blocks.SPRUCE_FENCE);                  // the pump rod
        // The wheel, facing the front: a hub and eight blades
        b.set(cx, 14, 3, Blocks.DARK_OAK_PLANKS);
        BlockState blade = Blocks.BIRCH_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true);
        for (int[] o : new int[][] {{0, 1}, {0, 2}, {0, -1}, {0, -2}, {1, 0}, {2, 0}, {-1, 0}, {-2, 0}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
            b.set(cx + o[0], 14 + o[1], 2, blade);
        }
        b.fill(cx, 14, 4, cx, 14, 6, Blocks.SPRUCE_FENCE);                 // tail boom and vane
        b.fill(cx, 13, 7, cx, 15, 7, Blocks.SPRUCE_PLANKS);
        // Stock tank
        b.walls(1, 1, 8, 5, 1, 10, s(Blocks.STONE_BRICKS));
        b.fill(2, 0, 9, 4, 0, 9, Blocks.STONE_BRICKS);
        b.fill(2, 1, 9, 4, 1, 9, Blocks.WATER);
        b.set(cx, 2, 7, Blocks.IRON_CHAIN);
        front(b, cx, s(Blocks.COARSE_DIRT));
        return b;
    }

    // 13 x 11 corral: a split-rail pen with a gate, a lean-to, hay and water, cattle and a horse.
    static Blueprint corral() {
        Blueprint b = new Blueprint("town/corral");
        int w = 13, d = 11, cx = 6;
        b.air(0, 1, 0, w - 1, 5, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.COARSE_DIRT);
        b.fill(2, 0, 2, w - 3, 0, d - 3, Blocks.DIRT);
        b.walls(0, 1, 0, w - 1, 1, d - 1, s(Blocks.SPRUCE_FENCE));
        b.set(cx, 1, 0, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        for (int[] c : new int[][] {{0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}}) {
            b.set(c[0], 2, c[1], Blocks.SPRUCE_FENCE);
        }
        // Lean-to in the back corner
        for (int[] c : new int[][] {{8, 7}, {11, 7}}) {
            b.fill(c[0], 1, c[1], c[0], 3, c[1], Blocks.SPRUCE_FENCE);
        }
        b.fill(8, 4, 7, 11, 4, 9, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.fill(9, 1, 9, 11, 2, 9, Blocks.HAY_BLOCK);
        b.set(2, 1, 8, water(3));
        b.set(3, 1, 8, water(3));
        b.set(2, 1, 2, Blocks.HAY_BLOCK);
        b.entity(EntityTypes.COW, 4, 1, 4);
        b.entity(EntityTypes.COW, 7, 1, 5);
        b.entity(EntityTypes.HORSE, 5, 1, 7);
        front(b, cx, s(Blocks.COARSE_DIRT));
        return b;
    }

    /**
     * Joke epitaphs (21a), four sign lines each, sign.hollowfrontier.epitaph.<n>.<line>. Each Boot Hill has three of them
     * on standing signs at the foot of its crosses ({@link #EPITAPH_SPOTS}); the strange grave stays the serious one.
     */
    public static final List<List<String>> EPITAPHS = List.of(
            List.of("JEB HOLLIS.", "CALLED RUBY'S", "COFFEE WEAK.", "ONCE."),
            List.of("HANK. QUICK", "ON THE DRAW.", "NOT QUICK", "ENOUGH."),
            List.of("HERE LIES", "A HORSE THIEF.", "THE HORSE", "IS FINE."),
            List.of("HE BET ON", "SIXTEEN.", "THE DEALER", "HAD TWENTY."),
            List.of("LEFTY McGRAW.", "HE WAS", "RIGHT-HANDED.", "NOBODY ASKED."),
            List.of("IKE DUNN.", "STOOD ON THE", "DYNAMITE TO", "SEE BETTER."),
            List.of("ELIAS. TRIED", "DOC'S TONIC.", "THE TONIC", "WON."),
            List.of("HERE LIES", "A LAWYER.", "WE DON'T HAVE", "ONE NOW."),
            List.of("TOM BRISCOE.", "PETTED THE", "SHERIFF'S", "CAT."),
            List.of("OL' PETE.", "TOLD THE MULE", "TO MOVE.", "IT DIDN'T."),
            List.of("MAE ORDERED", "WHISKEY.", "GOT", "MOONSHINE."),
            List.of("HE SAID IT", "WAS ONLY A", "WEATHER", "BALLOON."));

    /** The crosses (x, z) that get the epitaphs, per Boot Hill; each one's three, in turn, are the next three of EPITAPHS. */
    static final List<List<int[]>> EPITAPH_SPOTS = List.of(
            List.of(new int[] {4, 3}, new int[] {8, 3}, new int[] {4, 6}),   // boot_hill
            List.of(new int[] {2, 3}, new int[] {6, 3}, new int[] {8, 6}),   // boot_hill_b
            List.of(new int[] {2, 6}, new int[] {4, 3}, new int[] {8, 3}),   // boot_hill_c
            List.of(new int[] {6, 3}, new int[] {4, 6}, new int[] {8, 6}));  // boot_hill_strange (its stone is at 2, 3)

    static Blueprint bootHill() {
        return epitaphs(bootHill("town/boot_hill"), 0);
    }

    static Blueprint bootHillB() {
        return epitaphs(bootHill("town/boot_hill_b"), 1);
    }

    static Blueprint bootHillC() {
        return epitaphs(bootHill("town/boot_hill_c"), 2);
    }

    // A standing sign at the foot of each chosen cross, its text towards the gate.
    private static Blueprint epitaphs(Blueprint b, int set) {
        List<int[]> spots = EPITAPH_SPOTS.get(set);
        for (int i = 0; i < spots.size(); i++) {
            int n = set * spots.size() + i + 1;
            List<Component> lines = new java.util.ArrayList<>();
            for (int line = 1; line <= 4; line++) {
                lines.add(Component.translatable("sign.hollowfrontier.epitaph." + n + "." + line));
            }
            b.blockEntity(spots.get(i)[0], 1, spots.get(i)[1] - 1,
                    Blocks.SPRUCE_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8), sign(lines));
        }
        return b;
    }

    private static CompoundTag sign(List<Component> lines) {
        CompoundTag sign = new CompoundTag();
        sign.putString("id", "minecraft:sign");
        SignText text = new SignText(lines, lines, DyeColor.BLACK, false);
        sign.put("front_text", SignText.CODEC.encodeStart(NbtOps.INSTANCE, text).getOrThrow());
        return sign;
    }

    /**
     * The rare Boot Hill (9c, phase 11a pays it off) with one strange grave among the crosses: a stone where the first
     * cross stood, its epitaph on a sign ("the airship's pilot, not of this world, April 1897", after the Aurora story),
     * and buried in the mound a chest of what was found with him, a phlogiston shard among it.
     */
    static Blueprint bootHillStrange() {
        Blueprint b = bootHill("town/boot_hill_strange");
        b.air(2, 1, 3, 2, 3, 3);
        b.set(2, 1, 3, Blocks.CHISELED_STONE_BRICKS);
        b.set(2, 2, 3, Blocks.STONE_BRICK_WALL);
        List<Component> lines = new java.util.ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            lines.add(Component.translatable("sign.hollowfrontier.strange_grave." + i));
        }
        b.blockEntity(2, 1, 2, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH), sign(lines));
        b.chest(2, 0, 4, Direction.NORTH, ModChestLoot.STRANGE_GRAVE);
        return epitaphs(b, 3);
    }

    // 11 x 11 Boot Hill: a fenced graveyard of wooden crosses and mounds, a dead tree, a fresh grave dug.
    private static Blueprint bootHill(String name) {
        Blueprint b = new Blueprint(name);
        int w = 11, d = 11, cx = 5;
        b.air(0, 1, 0, w - 1, 7, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.COARSE_DIRT);
        b.walls(0, 1, 0, w - 1, 1, d - 1, s(Blocks.SPRUCE_FENCE));
        b.set(cx, 1, 0, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        b.fill(cx - 1, 1, 0, cx - 1, 3, 0, Blocks.SPRUCE_FENCE);           // gate posts and a cross-bar
        b.fill(cx + 1, 1, 0, cx + 1, 3, 0, Blocks.SPRUCE_FENCE);
        b.set(cx, 3, 0, Blocks.SPRUCE_FENCE);
        for (int z : new int[] {3, 6}) {
            for (int x : new int[] {2, 4, 6, 8}) {
                if (x == 6 && z == 6) {
                    continue; // the open grave
                }
                b.fill(x, 1, z, x, 2, z, Blocks.SPRUCE_FENCE);                    // a cross: post...
                b.set(x, 2, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));      // ...and its crosspiece
                b.set(x, 3, z, Blocks.SPRUCE_FENCE);
                b.set(x, 0, z + 1, Blocks.PODZOL);                                 // the mound
            }
        }
        b.fill(6, 0, 6, 6, 0, 7, Blocks.AIR);
        b.set(7, 1, 7, Blocks.COARSE_DIRT);
        b.set(7, 1, 6, Blocks.DIRT);
        // A dead tree in the corner, a lantern on a post
        b.fill(9, 1, 9, 9, 4, 9, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        b.set(8, 4, 9, Blocks.DARK_OAK_FENCE);
        b.set(9, 5, 8, Blocks.DARK_OAK_FENCE);
        b.fill(1, 1, 9, 1, 2, 9, Blocks.SPRUCE_FENCE);
        b.set(1, 3, 9, Blocks.LANTERN);
        b.cat(2, 1, 8, "ghost");                                          // sometimes a pale cat keeps watch
        front(b, cx, s(Blocks.COARSE_DIRT));
        return b;
    }

    // 21 x 21 ranch at the edge of town (24b): a gate arch with the brand on it; the ranch house on the left with its porch,
    // a red barn on the right (hay loft, two horses, tack), and behind both a corral of cattle and horses with a trough and
    // hay. The wrangler on the porch.
    static Blueprint ranch() {
        Blueprint b = new Blueprint("town/ranch");
        int w = 21, d = 21, cx = 10;
        b.air(0, 1, 0, w - 1, 14, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.COARSE_DIRT);
        // The fence round it, the gate arch with the brand
        b.walls(0, 1, 1, w - 1, 1, d - 1, s(Blocks.SPRUCE_FENCE));
        b.air(cx - 1, 1, 1, cx + 1, 1, 1);
        for (int x : new int[] {cx - 2, cx + 2}) {
            b.fill(x, 1, 1, x, 5, 1, log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        }
        b.fill(cx - 2, 6, 1, cx + 2, 6, 1, log(Blocks.SPRUCE_LOG, Direction.Axis.X));
        b.wallSign(cx, 6, 0, Blocks.SPRUCE_WALL_SIGN, Direction.NORTH, DyeColor.BLACK, false, "", TownDecor.signKey("ranch"),
                TownDecor.signKey("ranch") + ".sub");
        b.set(cx - 2, 7, 1, Blocks.CARVED_PUMPKIN.defaultBlockState());   // a steer's skull would do; a pumpkin's what there is
        b.fill(cx, 0, 0, cx, 0, 12, Blocks.DIRT_PATH);
        b.fill(4, 0, 6, cx, 0, 6, Blocks.DIRT_PATH);
        b.fill(cx, 0, 6, 15, 0, 6, Blocks.DIRT_PATH);
        // The ranch house (x 1..8, z 3..11): porch to the path, a room with a bed, the stove, a table, a chest
        int hx0 = 1, hx1 = 7, hz0 = 6, hz1 = 11;
        b.fill(hx0, 0, hz0 - 2, hx1, 0, hz1, Blocks.SPRUCE_PLANKS);
        b.walls(hx0, 1, hz0, hx1, 4, hz1, s(Blocks.OAK_PLANKS));
        for (int[] c : new int[][] {{hx0, hz0}, {hx1, hz0}, {hx0, hz1}, {hx1, hz1}}) {
            b.fill(c[0], 1, c[1], c[0], 4, c[1], log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Y));
        }
        for (int i = 0; i <= 3; i++) {                                    // a gable over the long sides, ridge along x
            b.fill(hx0 - 1, 5 + i, hz0 - 1 + i, hx1 + 1, 5 + i, hz0 - 1 + i, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, Half.BOTTOM));
            b.fill(hx0 - 1, 5 + i, hz1 + 1 - i, hx1 + 1, 5 + i, hz1 + 1 - i, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.BOTTOM));
            if (hz1 - hz0 - 2 * i > 0) {
                b.fill(hx0, 5 + i, hz0 + i, hx0, 5 + i, hz1 - i, Blocks.OAK_PLANKS);
                b.fill(hx1, 5 + i, hz0 + i, hx1, 5 + i, hz1 - i, Blocks.OAK_PLANKS);
            }
        }
        b.door(4, 1, hz0, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.fill(2, 2, hz0, 2, 3, hz0, Blocks.GLASS_PANE);
        b.fill(6, 2, hz0, 6, 3, hz0, Blocks.GLASS_PANE);
        b.set(hx0, 2, 9, Blocks.GLASS_PANE);
        b.fill(hx0 - 1, 1, hz0 - 2, hx0 - 1, 3, hz0 - 2, Blocks.SPRUCE_FENCE);
        b.fill(hx1 + 1, 1, hz0 - 2, hx1 + 1, 3, hz0 - 2, Blocks.SPRUCE_FENCE);
        b.fill(hx0 - 1, 4, hz0 - 2, hx1 + 1, 4, hz0 - 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.set(2, 1, hz0 - 1, stairs(Blocks.OAK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        b.set(6, 1, hz0 - 1, stairs(Blocks.OAK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        b.bed(3, 1, 10, Blocks.BED.brown(), Direction.WEST);
        b.chest(4, 1, 10, Direction.NORTH, ModChestLoot.RANCH);
        TownDecor.stove(b, 6, 1, 10, Direction.NORTH, 8);
        TownDecor.table(b, 5, 1, 8, Blocks.OAK_FENCE, Blocks.OAK_PRESSURE_PLATE);
        chairAt(b, 6, 8, Direction.EAST);
        b.shelf(6, 2, 7, Blocks.SPRUCE_SHELF, Direction.WEST, Items.LEAD, ModItems.LASSO.get(), Items.BOWL);
        TownDecor.rug(b, 2, 1, 7, 4, 9, DyeColor.RED, DyeColor.BROWN);
        b.set(4, 7, 8, hangingLantern());
        // The barn (x 12..20, z 3..11): mangrove red, a wide door to the path, the hay loft door over it
        int bx0 = 12, bx1 = 20, bz0 = 3, bz1 = 11;
        b.fill(bx0, 0, bz0, bx1, 0, bz1, Blocks.COARSE_DIRT);
        b.walls(bx0, 1, bz0, bx1, 5, bz1, s(Blocks.MANGROVE_PLANKS));
        for (int[] c : new int[][] {{bx0, bz0}, {bx1, bz0}, {bx0, bz1}, {bx1, bz1}}) {
            b.fill(c[0], 1, c[1], c[0], 5, c[1], log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        }
        for (int i = 0; i <= 4; i++) {                                    // a gable roof, ridge along z, over the door end
            int l = bx0 + i, r = bx1 - i, y = 6 + i;
            if (l == r) {
                b.fill(l, y, bz0 - 1, l, y, bz1 + 1, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
            } else {
                b.fill(l, y, bz0 - 1, l, y, bz1 + 1, stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST, Half.BOTTOM));
                b.fill(r, y, bz0 - 1, r, y, bz1 + 1, stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST, Half.BOTTOM));
                if (r - l > 1) {
                    b.fill(l + 1, y, bz0, r - 1, y, bz0, Blocks.MANGROVE_PLANKS);
                    b.fill(l + 1, y, bz1, r - 1, y, bz1, Blocks.MANGROVE_PLANKS);
                }
            }
        }
        b.air(15, 1, bz0, 17, 3, bz0);                                    // the barn door, open
        b.set(16, 6, bz0, Blocks.HAY_BLOCK);
        b.set(16, 7, bz0, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true));
        for (int z : new int[] {6, 9}) {
            b.fill(bx0 + 1, 1, z, bx0 + 2, 1, z, Blocks.SPRUCE_FENCE);
        }
        b.set(bx0 + 3, 1, 7, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST));
        b.set(bx0 + 3, 1, 8, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST));
        b.set(bx0 + 1, 1, 7, Blocks.HAY_BLOCK);
        b.entity(EntityTypes.HORSE, bx0 + 1, 1, 8);
        b.fill(bx1 - 2, 1, bz1 - 2, bx1 - 1, 2, bz1 - 1, Blocks.HAY_BLOCK);
        b.set(bx1 - 1, 1, 5, water(3));
        b.shelf(bx1 - 1, 2, 6, Blocks.SPRUCE_SHELF, Direction.WEST, Items.SADDLE, Items.LEAD, Items.LEAD);
        b.fill(bx0 + 1, 6, 7, bx1 - 1, 6, 7, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X)); // a tie beam, the lamp hung from it
        b.set(16, 5, 7, hangingLantern());
        // The corral behind (z 13..19): cattle and a horse, the trough, hay, and a gate from the yard
        b.walls(1, 1, 13, w - 2, 1, d - 2, s(Blocks.SPRUCE_FENCE));
        b.set(cx, 1, 13, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        b.fill(2, 0, 14, w - 3, 0, d - 3, Blocks.DIRT);
        b.set(3, 1, 15, water(3));
        b.set(4, 1, 15, water(3));
        b.fill(w - 4, 1, d - 4, w - 3, 1, d - 3, Blocks.HAY_BLOCK);
        b.entity(EntityTypes.COW, 6, 1, 16);
        b.entity(EntityTypes.COW, 11, 1, 17);
        b.entity(EntityTypes.COW, 14, 1, 15);
        b.entity(EntityTypes.HORSE, 8, 1, 18);
        b.entity(ModEntities.TOWNSFOLK.get(), 4, 1, 4, tag("folk", "wrangler"));
        b.cat(6, 1, 4, "home");
        front(b, cx, s(Blocks.DIRT_PATH));
        return b;
    }

    private static void chairAt(Blueprint b, int x, int z, Direction back) {
        b.set(x, 1, z, stairs(Blocks.OAK_STAIRS, back, Half.BOTTOM));
    }

    // 7 x 7 vegetable garden on a lane: fenced rows of crops round a water channel, a scarecrow.
    static Blueprint garden() {
        Blueprint b = new Blueprint("town/garden");
        int w = 7, cx = 3;
        b.air(0, 1, 0, w - 1, 4, 6);
        b.fill(0, 0, 0, w - 1, 0, 6, Blocks.COARSE_DIRT);
        b.walls(0, 1, 0, w - 1, 1, 6, s(Blocks.OAK_FENCE));
        b.set(cx, 1, 0, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7);
        b.fill(1, 0, 1, w - 2, 0, 5, farmland);
        b.fill(cx, 0, 1, cx, 0, 5, Blocks.WATER);
        for (int z = 1; z <= 5; z++) {
            b.set(1, 1, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
            b.set(2, 1, z, Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 5 + z % 3));
            b.set(4, 1, z, Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, 7));
            b.set(5, 1, z, Blocks.BEETROOTS.defaultBlockState().setValue(net.minecraft.world.level.block.BeetrootBlock.AGE, 3));
        }
        // Scarecrow over the water channel
        b.set(cx, 1, 3, Blocks.SPRUCE_FENCE);
        b.set(cx, 2, 3, Blocks.HAY_BLOCK);
        b.set(cx, 3, 3, Blocks.CARVED_PUMPKIN.defaultBlockState());
        b.set(cx, 0, 3, Blocks.COARSE_DIRT);
        b.set(w - 1, 2, 6, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        front(b, cx, s(Blocks.COARSE_DIRT));
        return b;
    }

    // ---------------------------------------------------------------- wheat (ripe, for the Grays' crop circles)

    /** Where a field's crop circle goes: the middle of its wheat, which the whole pattern (7 out) fits round. */
    public static final int FIELD_MIDDLE_X = 9;
    public static final int FIELD_MIDDLE_Z = 11;

    // 19 x 21 wheat field: a rail fence and gate, a headland with a scarecrow, hay, a hand cart and a composter, and a
    // 17 x 17 square of ripe wheat watered the way farmers do it, four 9 x 9 plots each round a water hole (lily-padded so
    // nobody steps in). The holes sit in the gaps of the crop circle pattern, so a circle pressed in the middle is whole.
    static Blueprint wheatField() {
        Blueprint b = new Blueprint("town/wheat_field");
        int w = 19, d = 21, cx = 9;
        b.air(0, 1, 0, w - 1, 4, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.COARSE_DIRT);
        b.walls(0, 1, 1, w - 1, 1, d - 1, s(Blocks.OAK_FENCE));
        b.set(cx, 1, 1, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        b.fill(1, 0, 3, w - 2, 0, d - 2, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        b.fill(1, 1, 3, w - 2, 1, d - 2, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        for (int dx : new int[] {-4, 4}) {
            for (int dz : new int[] {-4, 4}) {
                b.set(FIELD_MIDDLE_X + dx, 0, FIELD_MIDDLE_Z + dz, Blocks.WATER);
                b.set(FIELD_MIDDLE_X + dx, 1, FIELD_MIDDLE_Z + dz, Blocks.LILY_PAD);
            }
        }
        // The headland: hay stacked by the fence, the scarecrow, the hand cart (slabs on a wheel, shafts to the gate), a composter
        b.fill(1, 1, 2, 3, 1, 2, Blocks.HAY_BLOCK);
        b.set(2, 2, 2, Blocks.HAY_BLOCK);
        b.set(6, 1, 2, Blocks.SPRUCE_FENCE);
        b.set(6, 2, 2, Blocks.HAY_BLOCK);
        b.set(6, 3, 2, Blocks.CARVED_PUMPKIN.defaultBlockState());
        b.fill(w - 5, 1, 2, w - 4, 1, 2, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        b.set(w - 6, 1, 2, Blocks.SPRUCE_FENCE);
        b.set(w - 5, 2, 2, Blocks.HAY_BLOCK);
        b.set(w - 2, 1, 2, Blocks.COMPOSTER);
        front(b, cx, s(Blocks.COARSE_DIRT));
        return b;
    }
}
