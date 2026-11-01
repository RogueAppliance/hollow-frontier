package com.hollowfrontier.worldgen.feature;

import com.hollowfrontier.registry.ModBlocks;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A crystal outcrop (Crystal Steppes): a pale calcite footing in the grass with three to six leaning spires of crystal
 * standing out of it, each tipped with a glowing cluster and studded with buds, and now and then phlogiston ore glowing in the
 * footing (9c).
 * The origin is the block above the ground.
 */
public record CrystalOutcropFeature() implements Feature {
    public static final CrystalOutcropFeature INSTANCE = new CrystalOutcropFeature();
    public static final MapCodec<CrystalOutcropFeature> CODEC = MapCodec.unit(INSTANCE);

    private static final Block[] BUDS = {Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD};

    @Override
    public MapCodec<CrystalOutcropFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!Shapes.ground(level.getBlockState(origin.below())) || !Shapes.free(level, origin)) {
            return false;
        }
        // The footing: a ragged disc of calcite in place of the turf
        int radius = 2 + random.nextInt(2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 0.3 || random.nextDouble() < d / (radius + 1.5) * 0.5) {
                    continue;
                }
                int y = Shapes.surface(level, origin.getX() + dx, origin.getZ() + dz, origin.getY() + 2, origin.getY() - 4);
                if (y != Integer.MIN_VALUE) {
                    BlockPos ground = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    // Now and then a vein of phlogiston shows in the footing (9c): the crystals grow from it
                    level.setBlock(ground, (random.nextInt(9) == 0 ? ModBlocks.PHLOGISTON_ORE.get() : Blocks.CALCITE).defaultBlockState(), 2);
                    if (d < radius - 0.5 && random.nextInt(4) == 0) {
                        Shapes.put(level, ground.above(), bud(random, Direction.UP));
                    }
                }
            }
        }
        // The spires: the first the tallest, each leaning a little its own way
        int spires = 3 + random.nextInt(4);
        for (int i = 0; i < spires; i++) {
            int x = origin.getX() + (i == 0 ? 0 : random.nextInt(2 * radius - 1) - radius + 1);
            int z = origin.getZ() + (i == 0 ? 0 : random.nextInt(2 * radius - 1) - radius + 1);
            int y = Shapes.surface(level, x, z, origin.getY() + 2, origin.getY() - 4);
            if (y == Integer.MIN_VALUE) {
                continue;
            }
            int height = i == 0 ? 4 + random.nextInt(3) : 2 + random.nextInt(4);
            int leanX = random.nextInt(3) - 1, leanZ = random.nextInt(3) - 1;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
            for (int h = 1; h <= height; h++) {
                pos.move(Direction.UP);
                if (h > 1 && h % 3 == 0) {
                    pos.move(leanX, 0, leanZ);
                }
                if (!Shapes.put(level, pos, Blocks.AMETHYST_BLOCK.defaultBlockState())) {
                    break;
                }
                // Buds on the flanks
                Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                if (random.nextInt(3) == 0) {
                    Shapes.put(level, pos.relative(side), bud(random, side));
                }
                if (h == height) {
                    Shapes.put(level, pos.above(), Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
                }
            }
        }
        return true;
    }

    private static BlockState bud(RandomSource random, Direction facing) {
        Block block = random.nextInt(4) == 0 ? Blocks.AMETHYST_CLUSTER : BUDS[random.nextInt(BUDS.length)];
        return block.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
    }
}
