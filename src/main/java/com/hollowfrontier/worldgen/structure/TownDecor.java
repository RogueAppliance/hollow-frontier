package com.hollowfrontier.worldgen.structure;

import static com.hollowfrontier.worldgen.structure.TownBlueprints.log;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.s;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.slab;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.stairs;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.water;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModBlocks;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The small things that make a town building look lived in, shared by the buildings in TownBlueprints, TownShops and
 * TownLandmarks: the shop's name over its door, shutters and a cornice on the false front, stoves with their pipes out
 * through the roof, tables and chairs, rugs, and the back yard (a woodpile, a rain barrel, the outhouse). Same rules as
 * the buildings: drawn facing north, front at z 0, floor at y 0.
 */
public final class TownDecor {
    private TownDecor() {}

    /** Lang key of a shop sign's line: sign.hollowfrontier.shop.&lt;name&gt; (the name) and .&lt;name&gt;.sub (the line under it). */
    public static String signKey(String name) {
        return "sign.hollowfrontier.shop." + name;
    }

    /** The shop's sign over its door on the false front (z 2): its name, and a line under it, painted white on dark oak. */
    static void shopSign(Blueprint b, int x, int y, String name) {
        b.wallSign(x, y, 1, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, DyeColor.WHITE, false, "", signKey(name), signKey(name) + ".sub");
    }

    /**
     * Open shutters either side of each front window of a shopfront (the windows from {@link TownBlueprints#shopfront},
     * 2 wide either side of the door, two high from y 1 + {@code low}), flat against the facade.
     */
    static void shutters(Blueprint b, int w, Block trapdoor) {
        int cx = w / 2;
        BlockState shutter = trapdoor.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true);
        for (int x : new int[] {cx - 4, cx - 1, cx + 1, cx + 4}) {
            if (x >= 0 && x < w) {
                b.fill(x, 2, 1, x, 3, 1, shutter);
            }
        }
    }

    /**
     * Pilasters up the corners of a shopfront's false front and brackets under its cornice: upside-down stairs under the
     * top trim, at each end and every three blocks.
     */
    static void cornice(Blueprint b, int w, int h, int rise, BlockState post, Block bracket) {
        for (int x : new int[] {0, w - 1}) {
            b.fill(x, h + 1, 2, x, h + rise, 2, post);
        }
        for (int x = 0; x < w; x += 3) {
            b.set(x, h + rise - 1, 1, stairs(bracket, Direction.SOUTH, Half.TOP));
        }
        b.set(w - 1, h + rise - 1, 1, stairs(bracket, Direction.SOUTH, Half.TOP));
    }

    /** A back door in the back wall (z = d - 1), opening out to the yard. */
    static void backDoor(Blueprint b, int x, int d, DoorBlock door) {
        b.door(x, 1, d - 1, door, Direction.SOUTH, DoorHingeSide.LEFT);
    }

    /**
     * A cast-iron stove (a furnace) facing {@code facing}, its pipe up from the top through the roof to {@code top}, with a
     * smouldering campfire capping the pipe so there's smoke over the roof.
     */
    static void stove(Blueprint b, int x, int y, int z, Direction facing, int top) {
        b.set(x, y, z, TownBlueprints.facing(Blocks.FURNACE, facing));
        b.fill(x, y + 1, z, x, top - 1, z, Blocks.POLISHED_DEEPSLATE_WALL);
        b.set(x, top, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        chimneyPot(b, x, top, z);
    }

    /**
     * The pot on a chimney: open spruce trapdoors (solid, dark; iron ones showed the fire through their holes) stood round
     * the campfire at (x, y, z) that makes its smoke, so the fire is
     * hidden and only the smoke shows. Each goes only where nothing else is drawn (the roof may be there).
     */
    static void chimneyPot(Blueprint b, int x, int y, int z) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int sx = x + side.getStepX(), sz = z + side.getStepZ();
            BlockState there = sx < 0 || sz < 0 ? null : b.get(new net.minecraft.core.BlockPos(sx, y, sz));
            if (sx >= 0 && sz >= 0 && (there == null || there.isAir())) {
                b.set(sx, y, sz, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, side).setValue(TrapDoorBlock.OPEN, true));
            }
        }
    }

    /** A table (24c, TableBlock) of the wood of {@code fence} (oak, spruce or dark oak; anything else oak). */
    static void table(Blueprint b, int x, int y, int z, Block fence, Block plate) {
        b.set(x, y, z, fence == Blocks.SPRUCE_FENCE ? ModBlocks.SPRUCE_TABLE.get() : fence == Blocks.DARK_OAK_FENCE ? ModBlocks.DARK_OAK_TABLE.get()
                : ModBlocks.OAK_TABLE.get());
    }

    /**
     * A chair (24c, ChairBlock) with its back toward {@code back} (the sitter faces the other way), of the wood of
     * {@code stairs} (oak, spruce or dark oak; anything else oak).
     */
    static void chair(Blueprint b, int x, int y, int z, Block stairs, Direction back) {
        Block chair = stairs == Blocks.SPRUCE_STAIRS ? ModBlocks.SPRUCE_CHAIR.get() : stairs == Blocks.DARK_OAK_STAIRS ? ModBlocks.DARK_OAK_CHAIR.get()
                : ModBlocks.OAK_CHAIR.get();
        b.set(x, y, z, chair.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, back.getOpposite()));
    }

    /** A town lamp (24c), standing or hung: dark by day, lit from dusk. */
    static BlockState lamp(boolean hanging) {
        return ModBlocks.TOWN_LAMP.get().defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, hanging);
    }

    /** A rug of {@code field} with a border of {@code border}, the inclusive box at floor y (carpets go on top, y + 1). */
    static void rug(Blueprint b, int x1, int y, int z1, int x2, int z2, DyeColor field, DyeColor border) {
        b.fill(x1, y, z1, x2, y, z2, Blocks.CARPET.pick(border));
        if (x2 - x1 >= 2 && z2 - z1 >= 2) {
            b.fill(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, Blocks.CARPET.pick(field));
        }
    }

    /** A barrel lying on its side, its lid toward {@code facing} (crates and kegs). */
    static BlockState barrel(Direction facing) {
        return Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing);
    }

    /** Firewood: split logs stacked two high along x under a slab lean-to roof. */
    static void woodpile(Blueprint b, int x, int z, int length) {
        b.fill(x, 1, z, x + length - 1, 2, z, log(Blocks.OAK_LOG, Direction.Axis.Z));
        b.fill(x, 3, z, x + length - 1, 3, z, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
    }

    /** A rain barrel: a cauldron part full, by a downpipe. */
    static void rainBarrel(Blueprint b, int x, int z) {
        b.set(x, 1, z, water(2));
    }

    /**
     * The outhouse: a 4 x 3 shed of {@code wall} (x..x+3, z..z+2) under a slab roof, its door in the west end, a step of
     * floor inside it and the seat at the back (a composter: it's a frontier town). A crescent cut over the door.
     */
    static void outhouse(Blueprint b, int x, int z, BlockState wall, DoorBlock door) {
        b.walls(x, 1, z, x + 3, 2, z + 2, wall);
        b.air(x + 1, 1, z + 1, x + 2, 2, z + 1);
        b.set(x + 2, 1, z + 1, Blocks.COMPOSTER);
        b.fill(x, 3, z, x + 3, 3, z + 2, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.door(x, 1, z + 1, door, Direction.WEST, DoorHingeSide.LEFT);
        b.set(x, 3, z + 1, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM));
    }

    /** A planter box of the wall's own wood with a pot plant (style palettes swap the plant: cactus, fern...). */
    static void planter(Blueprint b, int x, int y, int z, Block plant) {
        b.set(x, y, z, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP));
        b.set(x, y + 1, z, plant);
    }

    /** A bench along x (two stairs, backs toward {@code back}) with arms of open trapdoors. */
    static void bench(Blueprint b, int x, int y, int z, Direction back) {
        Direction along = back.getClockWise();
        b.set(x, y, z, stairs(Blocks.OAK_STAIRS, back, Half.BOTTOM));
        b.set(x + along.getStepX(), y, z + along.getStepZ(), stairs(Blocks.OAK_STAIRS, back, Half.BOTTOM));
    }

    /** A bar stool from the saloon (sit on it); used for chairs in shops. */
    static BlockState stool() {
        return ModBlocks.BAR_STOOL.get().defaultBlockState();
    }

    static String id(String path) {
        return HollowFrontier.id(path).toString();
    }

    /** A lit candle stub in a holder on a table or shelf. */
    static BlockState candle(DyeColor color) {
        return Blocks.DYED_CANDLE.pick(color).defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true);
    }
}
