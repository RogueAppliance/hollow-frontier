package com.hollowfrontier.worldgen.structure;

import static com.hollowfrontier.worldgen.structure.TownBlueprints.facing;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.front;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.hangingLantern;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.log;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.s;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.slab;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.stairs;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.water;

import java.util.List;

import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.content.frontier.stake.StakeText;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The Old Place (47b): the homestead a town has standing empty on its outskirts, for sale. A 35 x 31 lot behind a rail
 * fence and a gate arch. The house is two storeys of clapboard on a stone footing, a veranda the width of its front with
 * a balcony over it and a gable over that, a brick stack up one end: a hall and the stairs, the parlour (the hearth, a
 * piano, books), the kitchen laid for supper, and upstairs two bedrooms off a landing with a door to the balcony. Across
 * the yard a red barn with stalls, tack and a hay loft under a cupola; behind, a corral of cattle under a shade tree, a
 * kitchen garden, an outhouse; in the front corner the well and its windmill.
 * Whoever lived here left it as it stood. A town's outskirts and street ends draw it like any other piece; where a town
 * has none, a deed raises the same template ({@code content/frontier/stake/OldPlace}), which is why its {@link #STAKE}
 * and size are given here.
 */
public final class OldPlaceBlueprint {
    private OldPlaceBlueprint() {}

    public static final String NAME = "old_place";
    public static final int WIDTH = 35, DEPTH = 31;
    /** The gate, and the building's connector: the middle of the front row. */
    public static final int GATE_X = 17;
    /** Where the Claim Stake stands, by the gate. The for-sale board is beside it. */
    public static final BlockPos STAKE = new BlockPos(20, 1, 0);
    /** The parlour piano, and how many head the corral holds (the game test looks for both). */
    public static final BlockPos PIANO = new BlockPos(7, 1, 17);
    public static final int CATTLE = 4;

    static Blueprint oldPlace() {
        Blueprint b = new Blueprint("town/" + NAME);
        int w = WIDTH, d = DEPTH, cx = GATE_X;
        b.air(0, 1, 0, w - 1, 20, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.COARSE_DIRT);
        grounds(b, w, d, cx);
        house(b);
        barn(b);
        corral(b);
        garden(b);
        yard(b);
        front(b, cx, s(Blocks.DIRT_PATH));
        return b;
    }

    // The fence round the lot, the gate arch with its board, the stake, the for-sale sign, the mailbox, the paths and
    // the flower beds.
    private static void grounds(Blueprint b, int w, int d, int cx) {
        b.walls(0, 1, 1, w - 1, 1, d - 1, s(Blocks.SPRUCE_FENCE));
        b.air(cx - 1, 1, 1, cx + 1, 1, 1);
        for (int x : new int[] {cx - 2, cx + 2}) {
            b.fill(x, 1, 1, x, 6, 1, log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
            b.set(x, 7, 1, Blocks.SPRUCE_FENCE);
        }
        b.fill(cx - 3, 6, 1, cx + 3, 6, 1, log(Blocks.SPRUCE_LOG, Direction.Axis.X));
        b.set(cx - 1, 5, 1, hangingLantern());
        b.set(cx + 1, 5, 1, hangingLantern());
        b.wallSign(cx, 6, 0, Blocks.SPRUCE_WALL_SIGN, Direction.NORTH, DyeColor.BLACK, false, "", StakeText.SIGN_OLD, "");
        b.set(STAKE.getX(), STAKE.getY(), STAKE.getZ(), ModBlocks.CLAIM_STAKE.get());
        b.blockEntity(STAKE.getX() + 1, 1, 0, Blocks.SPRUCE_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8),
                Blueprint.signNbt(List.of(Component.empty(), Component.translatable(StakeText.SIGN_FOR_SALE), Component.translatable(StakeText.SIGN_ASK),
                        Component.empty()), DyeColor.BLACK, false));
        b.set(cx - 3, 1, 0, Blocks.SPRUCE_FENCE);
        b.set(cx - 3, 2, 0, ModBlocks.MAILBOX.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.NORTH));
        // In from the gate, west to the veranda, east to the barn door, and down between house and barn to the back
        b.fill(cx, 0, 0, cx, 0, 3, Blocks.DIRT_PATH);
        b.fill(8, 0, 3, 26, 0, 3, Blocks.DIRT_PATH);
        b.fill(8, 0, 4, 10, 0, 4, Blocks.DIRT_PATH);
        b.fill(26, 0, 3, 26, 0, 5, Blocks.DIRT_PATH);
        b.fill(18, 0, 3, 18, 0, 20, Blocks.DIRT_PATH);
        b.fill(9, 0, 20, 26, 0, 20, Blocks.DIRT_PATH);
        TownLayout.lampPost(b, 12, 2);
        TownLayout.lampPost(b, 22, 2);
        Block[] flowers = {Blocks.POPPY, Blocks.OXEYE_DAISY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.AZURE_BLUET};
        for (int x = 2; x <= 15; x++) {              // flower beds under the veranda rail, either side of the steps
            if (x < 8 || x > 10) {
                b.set(x, 0, 4, Blocks.GRASS_BLOCK);
                b.set(x, 1, 4, flowers[x % flowers.length]);
            }
        }
    }

    // x 3..15, z 8..18, two storeys under a gable whose ridge runs along x; the veranda and balcony along the front
    // (z 5..7) under their own roof, with a gable over the door.
    private static void house(Blueprint b) {
        int x0 = 3, x1 = 15, z0 = 8, z1 = 18;
        BlockState wall = s(Blocks.BIRCH_PLANKS);
        BlockState post = log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Y);
        Block roof = Blocks.DARK_OAK_STAIRS;
        DoorBlock door = (DoorBlock) Blocks.SPRUCE_DOOR;
        b.fill(x0, 0, 5, x1, 0, z1, Blocks.SPRUCE_PLANKS);
        b.walls(x0, 1, z0, x1, 8, z1, wall);
        b.walls(x0, 1, z0, x1, 1, z1, s(Blocks.COBBLESTONE));                       // the footing
        for (int[] c : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
            b.fill(c[0], 1, c[1], c[0], 8, c[1], post);
        }
        b.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, Blocks.SPRUCE_PLANKS);          // the upper floor
        // The roof: six courses up each long side to a ridge of slabs, the gable ends filled in
        for (int i = 0; i <= 5; i++) {
            b.fill(x0 - 1, 9 + i, z0 - 1 + i, x1 + 1, 9 + i, z0 - 1 + i, stairs(roof, Direction.SOUTH, Half.BOTTOM));
            b.fill(x0 - 1, 9 + i, z1 + 1 - i, x1 + 1, 9 + i, z1 + 1 - i, stairs(roof, Direction.NORTH, Half.BOTTOM));
            b.fill(x0, 9 + i, z0 + i, x0, 9 + i, z1 - i, wall);
            b.fill(x1, 9 + i, z0 + i, x1, 9 + i, z1 - i, wall);
        }
        b.fill(x0 - 1, 15, 13, x1 + 1, 15, 13, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        for (int z : new int[] {11, 15}) {                                            // tie beams the lamps hang from
            b.fill(x0 + 1, 9, z, x1 - 1, 9, z, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X));
        }
        // The veranda: a balcony floor on four posts, rails below and above, its roof, and the gable over the door
        b.fill(x0, 5, 5, x1, 5, 7, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        b.fill(x0 - 1, 9, 4, x1 + 1, 9, 6, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        for (int x = x0; x <= x1; x++) {
            b.set(x, 6, 5, Blocks.SPRUCE_FENCE);
            if (x < 8 || x > 10) {
                b.set(x, 1, 5, Blocks.SPRUCE_FENCE);
            }
        }
        for (int x : new int[] {x0, x1}) {
            b.fill(x, 1, 6, x, 1, 7, Blocks.SPRUCE_FENCE);
            b.fill(x, 6, 6, x, 6, 7, Blocks.SPRUCE_FENCE);
        }
        for (int x : new int[] {x0, 7, 11, x1}) {
            b.fill(x, 1, 5, x, 8, 5, post);
        }
        for (int j = 0; j <= 2; j++) {
            b.fill(6 + j, 9 + j, 4, 6 + j, 9 + j, 6 + j, stairs(roof, Direction.EAST, Half.BOTTOM));
            b.fill(12 - j, 9 + j, 4, 12 - j, 9 + j, 6 + j, stairs(roof, Direction.WEST, Half.BOTTOM));
            b.fill(7 + j, 9 + j, 5, 11 - j, 9 + j, 5, wall);
        }
        b.fill(9, 12, 4, 9, 12, 9, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        b.set(9, 10, 5, Blocks.GLASS_PANE);
        TownDecor.chair(b, 4, 1, 6, Blocks.OAK_STAIRS, Direction.SOUTH);
        TownDecor.table(b, 5, 1, 6, Blocks.OAK_FENCE, Blocks.OAK_PRESSURE_PLATE);
        TownDecor.chair(b, 6, 1, 6, Blocks.OAK_STAIRS, Direction.SOUTH);
        TownDecor.bench(b, 13, 1, 7, Direction.SOUTH);
        b.set(14, 1, 7, Blocks.BARREL);
        b.set(14, 2, 7, Blocks.POTTED_DEAD_BUSH);
        b.set(8, 4, 6, hangingLantern());
        b.set(10, 4, 6, hangingLantern());
        TownDecor.chair(b, 5, 6, 6, Blocks.SPRUCE_STAIRS, Direction.SOUTH);
        TownDecor.table(b, 6, 6, 6, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_PRESSURE_PLATE);
        TownDecor.chair(b, 13, 6, 6, Blocks.SPRUCE_STAIRS, Direction.SOUTH);
        b.set(12, 6, 7, Blocks.POTTED_FERN);
        // Doors and windows
        b.door(9, 1, z0, door, Direction.NORTH, DoorHingeSide.LEFT);
        b.door(9, 6, z0, door, Direction.NORTH, DoorHingeSide.LEFT);
        for (int x : new int[] {5, 12}) {
            for (int z : new int[] {z0, z1}) {
                b.fill(x, 2, z, x + 1, 3, z, Blocks.GLASS_PANE);
                b.fill(x, 7, z, x + 1, 7, z, Blocks.GLASS_PANE);
            }
        }
        for (int z : new int[] {10, 16}) {
            b.fill(x0, 2, z, x0, 3, z, Blocks.GLASS_PANE);
            b.set(x0, 7, z, Blocks.GLASS_PANE);
        }
        b.fill(x1, 2, 10, x1, 3, 11, Blocks.GLASS_PANE);
        b.fill(x1, 7, 10, x1, 7, 11, Blocks.GLASS_PANE);
        b.fill(x1, 11, 13, x1, 12, 13, Blocks.GLASS_PANE);                             // the gable light
        for (int x : new int[] {4, 7, 11, 14}) {                                      // shutters on the back windows, both floors
            for (int y : new int[] {2, 3, 7}) {
                b.set(x, y, z1 + 1, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.SOUTH).setValue(TrapDoorBlock.OPEN, true));
            }
        }
        TownDecor.backDoor(b, 10, z1 + 1, door);
        // The hearth in the west wall: a grate over the fire, a brick breast and the stack up the gable outside
        b.fill(x0, 1, 12, x0, 3, 14, Blocks.BRICKS);
        b.set(x0, 0, 13, Blocks.CAMPFIRE);
        b.set(x0, 1, 13, Blocks.IRON_BARS);
        b.fill(x0 - 1, 1, 12, x0 - 1, 3, 14, Blocks.BRICKS);
        b.fill(x0 - 1, 1, 13, x0 - 1, 17, 13, Blocks.BRICKS);
        b.set(x0 - 1, 18, 13, Blocks.BRICK_SLAB);
        // The stairs: up the middle from the back of the hall to the landing, books packed under them
        b.air(9, 5, 13, 9, 5, 16);
        for (int i = 0; i < 5; i++) {
            b.set(9, 1 + i, 16 - i, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.BOTTOM));
            if (i > 0) {
                b.fill(9, 1, 16 - i, 9, i, 16 - i, Blocks.BOOKSHELF);
            }
        }
        // The parlour (west): rug, two chairs to the fire, the mantel, books, the piano against the back wall
        TownDecor.rug(b, 5, 1, 11, 7, 15, DyeColor.RED, DyeColor.BROWN);
        TownDecor.chair(b, 6, 1, 12, Blocks.DARK_OAK_STAIRS, Direction.EAST);
        TownDecor.chair(b, 6, 1, 14, Blocks.DARK_OAK_STAIRS, Direction.EAST);
        b.shelf(4, 3, 13, Blocks.SPRUCE_SHELF, Direction.EAST, Items.SPYGLASS, Items.CLOCK, Items.WRITABLE_BOOK);
        b.fill(7, 1, 9, 8, 3, 9, Blocks.BOOKSHELF);
        b.set(4, 1, 9, TownDecor.barrel(Direction.UP));
        b.set(4, 2, 9, Blocks.POTTED_FERN);
        b.set(PIANO.getX(), PIANO.getY(), PIANO.getZ(), ModBlocks.SALOON_PIANO.get().defaultBlockState().setValue(SaloonPianoBlock.FACING, Direction.NORTH));
        b.set(7, 1, 16, TownDecor.stool());
        TownDecor.table(b, 4, 1, 17, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_PRESSURE_PLATE);
        b.set(4, 2, 17, TownDecor.candle(DyeColor.WHITE));
        b.cat(6, 1, 13, "home");
        // The kitchen (east): the stove, a counter and shelves of food, the table laid for supper, the pantry
        TownDecor.stove(b, 14, 1, 15, Direction.WEST, 15);
        b.set(14, 1, 14, water(3));
        b.set(14, 1, 16, Blocks.BARREL);
        b.chest(14, 1, 17, Direction.NORTH, ModChestLoot.OLD_PLACE_PANTRY);
        b.set(13, 1, 17, Blocks.CRAFTING_TABLE);
        b.set(12, 1, 17, facing(Blocks.SMOKER, Direction.NORTH));
        b.set(11, 1, 17, TownDecor.barrel(Direction.NORTH));
        b.shelf(14, 3, 13, Blocks.SPRUCE_SHELF, Direction.WEST, Items.BREAD, Items.COOKED_BEEF, Items.APPLE);
        b.shelf(14, 3, 14, Blocks.SPRUCE_SHELF, Direction.WEST, ModItems.COWBOY_COFFEE.get(), Items.SUGAR, Items.EGG);
        b.shelf(14, 3, 16, Blocks.SPRUCE_SHELF, Direction.WEST, Items.PUMPKIN_PIE, Items.HONEY_BOTTLE, Items.BOWL);
        TownDecor.table(b, 12, 1, 10, Blocks.OAK_FENCE, Blocks.OAK_PRESSURE_PLATE);
        TownDecor.table(b, 12, 1, 11, Blocks.OAK_FENCE, Blocks.OAK_PRESSURE_PLATE);
        b.set(12, 2, 10, TownDecor.candle(DyeColor.WHITE));
        b.set(12, 2, 11, Blocks.CAKE);
        for (int z : new int[] {10, 11}) {
            TownDecor.chair(b, 11, 1, z, Blocks.OAK_STAIRS, Direction.WEST);
            TownDecor.chair(b, 13, 1, z, Blocks.OAK_STAIRS, Direction.EAST);
        }
        TownDecor.chair(b, 12, 1, 9, Blocks.OAK_STAIRS, Direction.NORTH);
        TownDecor.chair(b, 12, 1, 12, Blocks.OAK_STAIRS, Direction.SOUTH);
        for (int[] lamp : new int[][] {{6, 13}, {12, 11}, {9, 10}, {12, 15}}) {
            b.set(lamp[0], 4, lamp[1], hangingLantern());
        }
        // Upstairs: the landing between two bedroom walls, a door to each room and one out to the balcony
        for (int x : new int[] {8, 10}) {
            b.fill(x, 6, 9, x, 8, 17, wall);
        }
        b.door(8, 6, 10, door, Direction.EAST, DoorHingeSide.LEFT);
        b.door(10, 6, 10, door, Direction.WEST, DoorHingeSide.LEFT);
        // The big bedroom (west)
        b.bed(5, 6, 12, Blocks.BED.red(), Direction.WEST);
        b.bed(5, 6, 13, Blocks.BED.red(), Direction.WEST);
        for (int z : new int[] {11, 14}) {
            b.set(4, 6, z, TownDecor.barrel(Direction.UP));
            b.set(4, 7, z, TownDecor.candle(DyeColor.WHITE));
        }
        b.chest(4, 6, 17, Direction.NORTH, ModChestLoot.OLD_PLACE_TRUNK);
        b.fill(7, 6, 16, 7, 7, 17, Blocks.BOOKSHELF);
        b.armorStand(6, 6, 17, 180, ModItems.COWBOY_HAT.get(), ModItems.DUSTER.get());
        TownDecor.rug(b, 6, 6, 11, 7, 14, DyeColor.BLUE, DyeColor.BLUE);
        TownDecor.table(b, 4, 6, 9, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_PRESSURE_PLATE);
        b.set(4, 7, 9, Blocks.FLOWER_POT);
        // The small bedroom and the writing desk (east)
        b.bed(13, 6, 9, Blocks.BED.blue(), Direction.EAST);
        b.bed(13, 6, 11, Blocks.BED.blue(), Direction.EAST);
        b.set(14, 6, 10, TownDecor.barrel(Direction.UP));
        b.set(14, 7, 10, TownDecor.candle(DyeColor.WHITE));
        TownDecor.rug(b, 11, 6, 13, 13, 15, DyeColor.GREEN, DyeColor.WHITE);
        TownDecor.table(b, 13, 6, 17, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_PRESSURE_PLATE);
        b.set(13, 7, 17, TownDecor.candle(DyeColor.WHITE));
        TownDecor.chair(b, 13, 6, 16, Blocks.SPRUCE_STAIRS, Direction.NORTH);
        b.set(14, 6, 17, Blocks.CARTOGRAPHY_TABLE);
        b.chest(12, 6, 17, Direction.NORTH, ModChestLoot.OLD_PLACE_DESK);
        b.shelf(11, 7, 17, Blocks.SPRUCE_SHELF, Direction.NORTH, Items.MAP, Items.COMPASS, Items.SPYGLASS);
        for (int[] lamp : new int[][] {{6, 11}, {6, 15}, {12, 11}, {12, 15}, {9, 11}}) {
            b.set(lamp[0], 8, lamp[1], hangingLantern());
        }
    }

    // x 21..31, z 6..18: mangrove red with pale door posts, a wide door to the path and a hoist over it, stalls and tack
    // below, a hay loft over the back half, a cupola on the ridge.
    private static void barn(Blueprint b) {
        int x0 = 21, x1 = 31, z0 = 6, z1 = 18, mid = 26;
        b.walls(x0, 1, z0, x1, 6, z1, s(Blocks.MANGROVE_PLANKS));
        for (int[] c : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
            b.fill(c[0], 1, c[1], c[0], 6, c[1], log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        }
        for (int i = 0; i <= 5; i++) {                                                // a gable roof, ridge along z, over the door end
            int l = x0 - 1 + i, r = x1 + 1 - i, y = 7 + i;
            b.fill(l, y, z0 - 1, l, y, z1 + 1, stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST, Half.BOTTOM));
            b.fill(r, y, z0 - 1, r, y, z1 + 1, stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST, Half.BOTTOM));
            b.fill(l + 1, y, z0, r - 1, y, z0, Blocks.MANGROVE_PLANKS);
            b.fill(l + 1, y, z1, r - 1, y, z1, Blocks.MANGROVE_PLANKS);
        }
        b.fill(mid, 13, z0 - 1, mid, 13, z1 + 1, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        // The cupola and its rod
        b.set(mid, 13, 12, Blocks.BIRCH_PLANKS);
        b.set(mid, 14, 12, Blocks.BIRCH_PLANKS);
        b.fill(mid - 1, 15, 11, mid + 1, 15, 13, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        b.set(mid, 16, 12, Blocks.LIGHTNING_ROD.waxed().unaffected());
        // The barn door, open, between pale posts; the hay door and the hoist beam over it
        b.air(mid - 1, 1, z0, mid + 1, 4, z0);
        for (int x : new int[] {mid - 2, mid + 2}) {
            b.fill(x, 1, z0, x, 5, z0, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y));
        }
        b.fill(mid - 2, 5, z0, mid + 2, 5, z0, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.X));
        b.set(mid, 8, z0, Blocks.HAY_BLOCK);
        for (int x : new int[] {mid - 1, mid + 1}) {
            b.set(x, 8, z0 - 1, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true));
        }
        b.fill(mid, 11, z0 - 2, mid, 11, z0, log(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
        b.set(mid, 10, z0 - 2, hangingLantern());
        b.air(mid, 1, z1, mid, 2, z1);                                                // and a way out the back to the corral
        for (int z : new int[] {10, 14}) {
            b.set(x0, 3, z, Blocks.GLASS_PANE);
            b.set(x1, 3, z, Blocks.GLASS_PANE);
        }
        // Three stalls down the west side: the horse, the donkey, and one empty
        for (int z : new int[] {10, 13, 16}) {
            b.fill(x0 + 1, 1, z, x0 + 4, 1, z, Blocks.SPRUCE_FENCE);
        }
        for (int z : new int[] {7, 11, 14}) {
            b.set(x0 + 4, 1, z, Blocks.SPRUCE_FENCE);
            b.set(x0 + 4, 1, z + 1, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST));
        }
        b.set(x0 + 1, 1, 7, Blocks.HAY_BLOCK);                                         // against the wall, not a stall rail (a way over it)
        b.set(x0 + 4, 1, 9, Blocks.SPRUCE_FENCE);
        b.entity(EntityTypes.HORSE, x0 + 2, 1, 8);
        b.entity(EntityTypes.DONKEY, x0 + 2, 1, 12);
        // The east side: trough, tack on shelves, the anvil, the chest, feed
        b.set(x1 - 1, 1, 7, water(3));
        b.set(x1 - 1, 1, 8, water(3));
        b.shelf(x1 - 1, 3, 7, Blocks.SPRUCE_SHELF, Direction.WEST, Items.SADDLE, Items.LEAD, ModItems.LASSO.get());
        b.shelf(x1 - 1, 3, 8, Blocks.SPRUCE_SHELF, Direction.WEST, Items.WHEAT, Items.SHEARS, Items.BUCKET);
        b.set(x1 - 1, 1, 10, Blocks.ANVIL);
        b.chest(x1 - 1, 1, 11, Direction.WEST, ModChestLoot.OLD_PLACE_BARN);
        b.set(x1 - 1, 1, 13, Blocks.BARREL);
        b.set(x1 - 1, 1, 14, TownDecor.barrel(Direction.WEST));
        b.fill(x1 - 2, 1, 16, x1 - 1, 2, 17, Blocks.HAY_BLOCK);
        // The loft over the back half, up a ladder on the east wall
        b.fill(x0 + 1, 4, 13, x1 - 1, 4, z1 - 1, Blocks.SPRUCE_PLANKS);
        b.fill(x1 - 1, 1, 12, x1 - 1, 4, 12, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
        b.fill(x0 + 1, 5, 13, x1 - 3, 5, 13, Blocks.SPRUCE_FENCE);
        b.fill(x0 + 1, 5, 15, x1 - 1, 5, z1 - 1, Blocks.HAY_BLOCK);
        b.fill(x0 + 1, 6, 16, x0 + 5, 6, z1 - 1, Blocks.HAY_BLOCK);
        b.fill(x0 + 1, 7, 10, x1 - 1, 7, 10, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X));
        b.set(mid, 6, 10, hangingLantern());
        b.set(x0 + 3, 6, 10, hangingLantern());
        b.set(x1 - 3, 6, 10, hangingLantern());
    }

    // z 20..29 behind the barn: the lot's own fence on two sides, cattle, the trough and hay, a shade tree.
    private static void corral(Blueprint b) {
        b.fill(20, 1, 20, 33, 1, 20, Blocks.SPRUCE_FENCE);
        b.fill(20, 1, 20, 20, 1, 29, Blocks.SPRUCE_FENCE);
        b.set(26, 1, 20, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        b.fill(21, 0, 21, 33, 0, 29, Blocks.DIRT);
        // The trough and the hay stand clear of the fence: a cow gets up on either, and from there it's a step over the rail
        b.set(23, 1, 26, water(3));
        b.set(23, 1, 27, water(3));
        b.fill(24, 1, 27, 25, 1, 27, Blocks.HAY_BLOCK);
        TownLayout.shadeTree(b, 30, 26);
        int[][] cattle = {{24, 22}, {28, 24}, {22, 24}, {30, 22}};
        for (int i = 0; i < CATTLE; i++) {
            b.entity(EntityTypes.COW, cattle[i][0], 1, cattle[i][1]);
        }
    }

    // x 1..10, z 22..29 behind the house: rows of crops between water channels, a scarecrow, a pumpkin patch.
    private static void garden(Blueprint b) {
        b.fill(1, 1, 21, 11, 1, 21, Blocks.SPRUCE_FENCE);
        b.fill(11, 1, 21, 11, 1, 29, Blocks.SPRUCE_FENCE);
        b.set(9, 1, 21, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        b.fill(1, 0, 22, 10, 0, 28, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        b.fill(1, 0, 24, 10, 0, 24, Blocks.WATER);
        b.fill(1, 0, 27, 10, 0, 27, Blocks.WATER);
        for (int x = 1; x <= 10; x++) {
            b.set(x, 1, 22, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
            b.set(x, 1, 23, Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 5 + x % 3));
            b.set(x, 1, 25, Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, 7));
            b.set(x, 1, 26, Blocks.BEETROOTS.defaultBlockState().setValue(net.minecraft.world.level.block.BeetrootBlock.AGE, 3));
            b.set(x, 1, 28, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 4 + x % 4));
            if (x % 3 == 0) {
                b.set(x, 1, 29, x % 2 == 0 ? Blocks.MELON : Blocks.PUMPKIN);
            }
        }
        b.set(5, 0, 24, Blocks.COARSE_DIRT);
        b.set(5, 1, 24, Blocks.SPRUCE_FENCE);
        b.set(5, 2, 24, Blocks.HAY_BLOCK);
        b.set(5, 3, 24, Blocks.CARVED_PUMPKIN.defaultBlockState());
    }

    // What stands about the yard: the well and its windmill, the woodpile, a rain barrel, the outhouse under a tree, a
    // hitching rail, hens.
    private static void yard(Blueprint b) {
        // The well in the front corner: a stone ring, posts and a little roof
        b.fill(29, 0, 2, 31, 1, 4, Blocks.COBBLESTONE);
        b.set(30, 1, 3, Blocks.WATER);
        for (int[] c : new int[][] {{29, 2}, {31, 2}, {29, 4}, {31, 4}}) {
            b.fill(c[0], 2, c[1], c[0], 3, c[1], Blocks.SPRUCE_FENCE);
        }
        b.fill(29, 4, 2, 31, 4, 4, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        // The windmill that fills it: a braced mast, the fan facing the street, a tail behind
        b.set(32, 0, 3, Blocks.COBBLESTONE);
        b.fill(32, 1, 3, 32, 9, 3, Blocks.SPRUCE_FENCE);
        b.set(33, 1, 3, Blocks.SPRUCE_FENCE);
        b.set(32, 1, 2, Blocks.SPRUCE_FENCE);
        b.set(32, 1, 4, Blocks.SPRUCE_FENCE);
        b.set(32, 10, 3, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
        b.fill(31, 9, 2, 33, 11, 2, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true));
        b.set(32, 10, 4, Blocks.SPRUCE_FENCE);
        b.set(32, 10, 5, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.EAST).setValue(TrapDoorBlock.OPEN, true));
        TownDecor.woodpile(b, 4, 20, 4);
        TownDecor.rainBarrel(b, 16, 12);
        TownDecor.outhouse(b, 14, 22, s(Blocks.SPRUCE_PLANKS), (DoorBlock) Blocks.SPRUCE_DOOR);
        TownLayout.shadeTree(b, 15, 27);
        b.set(21, 1, 5, Blocks.SPRUCE_FENCE);                                          // the hitching rail
        b.set(23, 1, 5, Blocks.SPRUCE_FENCE);
        b.fill(21, 2, 5, 23, 2, 5, Blocks.SPRUCE_FENCE);
        b.set(20, 1, 14, Blocks.HAY_BLOCK);
        b.set(20, 1, 15, Blocks.COMPOSTER);
        b.entity(EntityTypes.CHICKEN, 17, 1, 21);
        b.entity(EntityTypes.CHICKEN, 13, 1, 26);
        b.entity(EntityTypes.CHICKEN, 19, 1, 17);
    }
}
