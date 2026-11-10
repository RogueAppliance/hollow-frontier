package com.hollowfrontier.worldgen.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A stalk of kelp on the Still Sea's bed (43a), as vanilla's but placed where it's told (vanilla's looks the bed up in the
 * ocean-floor heightmap, which under the crust is the roof of the world). The origin is the water on the bed. Kelp keeps to
 * the shelf: water at least {@link #MIN_DEPTH} deep and no more than {@link #MAX_DEPTH}, so the deep is open water and the
 * very edge is left to the seagrass. The stalk stops short of the surface, and its tip is old enough not to grow much more.
 */
public record SeaKelpFeature() implements Feature {
    public static final SeaKelpFeature INSTANCE = new SeaKelpFeature();
    public static final MapCodec<SeaKelpFeature> CODEC = MapCodec.unit(INSTANCE);
    public static final int MIN_DEPTH = 4;
    public static final int MAX_DEPTH = 26;

    @Override
    public MapCodec<SeaKelpFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int depth = SeaGrassFeature.depth(level, origin, MAX_DEPTH + 1);
        if (depth < MIN_DEPTH || depth > MAX_DEPTH || !Blocks.KELP.defaultBlockState().canSurvive(level, origin)) {
            return false;
        }
        int height = Math.min(depth - 1, 2 + random.nextInt(10));
        BlockState stalk = Blocks.KELP_PLANT.defaultBlockState();
        for (int y = 0; y < height - 1; y++) {
            level.setBlock(origin.above(y), stalk, 2);
        }
        level.setBlock(origin.above(height - 1), Blocks.KELP.defaultBlockState().setValue(KelpBlock.AGE, 20 + random.nextInt(4)), 2);
        return true;
    }
}
