package com.hollowfrontier.worldgen.structure;

import java.util.Random;

import com.hollowfrontier.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Hollow Earth sites' stonework (25): the palettes they're built from, drawn block by block so no two walls read the
 * same, and the weather that has been at them since (snow on what's open to the sky, moss on the floors, vines, rubble).
 * <p>
 * <b>Kindled:</b> dark deepslate tile footings and steps, tuff brick walling with polished courses, crystal brick and
 * chiseled crystal for what was carved, starsteel trim, phlogiston light. Quartz is kept for columns, as an accent. The dark
 * stone is what stands the Highlands' sites out against the snow; before 25 they were all quartz and calcite.
 */
final class Masonry {
    private Masonry() {}

    /** Kindled walling: tuff bricks, with polished tuff and the odd chiseled or rough block. */
    static BlockState kindledWall(Random r) {
        int n = r.nextInt(24);
        Block block = n < 15 ? Blocks.TUFF_BRICKS : n < 20 ? Blocks.POLISHED_TUFF : n < 22 ? Blocks.TUFF : Blocks.CHISELED_TUFF_BRICKS;
        return block.defaultBlockState();
    }

    /** Kindled footings and steps: deepslate tiles, about one in four cracked. */
    static BlockState kindledBase(Random r) {
        return (r.nextInt(4) == 0 ? Blocks.CRACKED_DEEPSLATE_TILES : Blocks.DEEPSLATE_TILES).defaultBlockState();
    }

    /** Kindled paving: deepslate tiles, polished tuff and tuff bricks, now and then a crystal brick. */
    static BlockState kindledPaving(Random r) {
        int n = r.nextInt(14);
        Block block = n < 5 ? Blocks.DEEPSLATE_TILES : n < 7 ? Blocks.CRACKED_DEEPSLATE_TILES : n < 10 ? Blocks.POLISHED_TUFF
                : n < 13 ? Blocks.TUFF_BRICKS : ModBlocks.CRYSTAL_BRICKS.get();
        return block.defaultBlockState();
    }

    static BlockState crystalBricks() {
        return ModBlocks.CRYSTAL_BRICKS.get().defaultBlockState();
    }

    static BlockState chiseledCrystal() {
        return ModBlocks.CHISELED_CRYSTAL_BRICKS.get().defaultBlockState();
    }

    static BlockState starsteel() {
        return ModBlocks.STARSTEEL_BLOCK.get().defaultBlockState();
    }

    static BlockState phlogistonLamp() {
        return ModBlocks.PHLOGISTON_LAMP.get().defaultBlockState();
    }

    /** A stair facing {@code facing} (its tall back toward it). */
    static BlockState stair(Block stair, Direction facing) {
        return stair.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    /** Fills the box with Kindled walling (each block drawn on its own). */
    static void kindledFill(Blueprint b, Random r, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    b.set(x, y, z, kindledWall(r));
                }
            }
        }
    }

    /** The highest drawn, non-air block in the column at or below {@code fromY}, or -1. */
    static int top(Blueprint b, int x, int z, int fromY) {
        for (int y = fromY; y >= 0; y--) {
            BlockState state = b.get(new BlockPos(x, y, z));
            if (state != null && !state.isAir()) {
                return y;
            }
        }
        return -1;
    }

    private static boolean solidTop(BlockState state) {
        return state.isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Direction.UP);
    }

    /**
     * Snow on what's open to the sky in the box: on each column's top face (a solid block with nothing over it in the
     * blueprint), a layer or two, more at random, with {@code chance}. Roofed floors stay bare.
     */
    static void snow(Blueprint b, Random r, int x0, int z0, int x1, int z1, double chance) {
        int high = b.sizeY();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int y = top(b, x, z, high);
                if (y < 0 || y + 1 >= high + 1 || r.nextDouble() >= chance) {
                    continue;
                }
                BlockState state = b.get(new BlockPos(x, y, z));
                if (solidTop(state)) {
                    b.set(x, y + 1, z, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + r.nextInt(r.nextInt(4) == 0 ? 3 : 1)));
                }
            }
        }
    }

    /**
     * A carpet ({@code carpet}: pale moss in the Highlands, moss in the jungle) on floors in the box at {@code y}: a solid
     * block below and air drawn over it, with {@code chance}.
     */
    static void carpet(Blueprint b, Random r, int x0, int z0, int x1, int z1, int y, Block carpet, double chance) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                BlockState below = b.get(new BlockPos(x, y - 1, z)), at = b.get(new BlockPos(x, y, z));
                if (below != null && solidTop(below) && (at == null || at.isAir()) && r.nextDouble() < chance) {
                    b.set(x, y, z, carpet);
                }
            }
        }
    }

    /**
     * Vines hung down the outside of walls in the box: each spot that's air with a solid block beside it gets a vine on
     * that side, with {@code chance}, trailing down a few blocks.
     */
    static void vines(Blueprint b, Random r, int x0, int y0, int z0, int x1, int y1, int z1, double chance) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y1; y >= y0; y--) {
                for (int z = z0; z <= z1; z++) {
                    BlockState at = b.get(new BlockPos(x, y, z));
                    if (at == null || !at.isAir() || r.nextDouble() >= chance) {
                        continue;
                    }
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        BlockState wall = b.get(new BlockPos(x + side.getStepX(), y, z + side.getStepZ()));
                        if (wall != null && wall.isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, side.getOpposite())) {
                            BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(side), true);
                            int length = 1 + r.nextInt(4);
                            for (int k = 0; k < length && y - k >= y0; k++) {
                                BlockState below = b.get(new BlockPos(x, y - k, z));
                                if (below == null || !below.isAir()) {
                                    break;
                                }
                                b.set(x, y - k, z, vine);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Rubble strewn round (cx, cz) on the floor at {@code y}, out to {@code radius}: {@code count} tries, each a piece
     * from {@code bits} (stairs turned at random) on a spot with nothing drawn on it yet and a solid floor under it.
     */
    static void rubble(Blueprint b, Random r, int cx, int y, int cz, double radius, int count, BlockState... bits) {
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = r.nextDouble() * radius;
            int x = cx + (int) Math.round(Math.cos(a) * d), z = cz + (int) Math.round(Math.sin(a) * d);
            if (x < 0 || z < 0) {
                continue;
            }
            BlockState at = b.get(new BlockPos(x, y, z)), below = b.get(new BlockPos(x, y - 1, z));
            if ((at != null && !at.isAir()) || below == null || !solidTop(below)) {
                continue;
            }
            BlockState bit = bits[r.nextInt(bits.length)];
            if (bit.hasProperty(StairBlock.FACING)) {
                bit = bit.setValue(StairBlock.FACING, Direction.from2DDataValue(r.nextInt(4)));
            }
            b.set(x, y, z, bit);
        }
    }

    /** Knocks blocks out of the top {@code depth} courses of what's drawn in the box, with {@code chance}: a ragged top. */
    static void crumble(Blueprint b, Random r, int x0, int z0, int x1, int z1, int depth, double chance) {
        int high = b.sizeY();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int y = top(b, x, z, high);
                for (int k = 0; k < depth && y - k > 1; k++) {
                    if (r.nextDouble() < chance) {
                        b.set(x, y - k, z, Blocks.AIR);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    /** Swaps stone bricks for mossy and cracked ones here and there (a sweep over the whole blueprint). */
    static void ageStoneBricks(Blueprint b, Random r) {
        b.map((pos, state) -> {
            if (state.is(Blocks.STONE_BRICKS)) {
                int n = r.nextInt(6);
                return n == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : n == 1 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : state;
            }
            return state;
        });
    }
}
