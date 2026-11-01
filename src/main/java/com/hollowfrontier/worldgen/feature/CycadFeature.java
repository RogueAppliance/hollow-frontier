package com.hollowfrontier.worldgen.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A cycad (Primordial Jungle): a stout scaly trunk one to three blocks high with a stiff crown of fronds fanning up and
 * out in every direction, the way they grew before there were flowers. The origin is the block above the ground.
 */
public record CycadFeature() implements Feature {
    public static final CycadFeature INSTANCE = new CycadFeature();
    public static final MapCodec<CycadFeature> CODEC = MapCodec.unit(INSTANCE);

    private static final int[][] DIRECTIONS = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};

    @Override
    public MapCodec<CycadFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) {
            return false;
        }
        int height = 1 + random.nextInt(3);
        for (int y = 0; y <= height + 2; y++) {
            if (!Shapes.free(level, origin.above(y))) {
                return false;
            }
        }
        BlockState trunk = Blocks.JUNGLE_WOOD.defaultBlockState();
        for (int y = 0; y < height; y++) {
            level.setBlock(origin.above(y), trunk, 2);
        }
        BlockPos top = origin.above(height);
        BlockState frond = GiantFernFeature.frond();
        Shapes.put(level, top, frond);
        Shapes.put(level, top.above(), frond);
        for (int[] d : DIRECTIONS) {
            if (random.nextInt(6) == 0) {
                continue; // a frond lost here and there
            }
            int length = 2 + random.nextInt(2);
            for (int s = 1; s <= length; s++) {
                Shapes.put(level, top.offset(d[0] * s, s >= 2 ? 1 : 0, d[1] * s), frond);
            }
        }
        return true;
    }
}
