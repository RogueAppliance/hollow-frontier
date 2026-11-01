package com.hollowfrontier.worldgen.feature;

import com.hollowfrontier.registry.ModBlocks;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A giant tree fern (Primordial Jungle): a slender dark trunk 4-8 blocks tall under a crown of six to eight long fronds
 * that spread level from the top and droop at the tips, like a parasol. Grows on dirt, grass, podzol, mud or moss, where
 * the whole column is open. The origin is the block above the ground.
 */
public record GiantFernFeature() implements Feature {
    public static final GiantFernFeature INSTANCE = new GiantFernFeature();
    public static final MapCodec<GiantFernFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<GiantFernFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) {
            return false;
        }
        int height = 4 + random.nextInt(5);
        for (int y = 0; y <= height + 1; y++) {
            if (!Shapes.free(level, origin.above(y))) {
                return false;
            }
        }
        BlockState trunk = Blocks.DARK_OAK_LOG.defaultBlockState();
        for (int y = 0; y < height; y++) {
            level.setBlock(origin.above(y), trunk, 2);
        }
        BlockPos top = origin.above(height);
        BlockState frond = frond();
        Shapes.put(level, top, frond);
        int arms = 6 + random.nextInt(3);
        double start = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < arms; i++) {
            double angle = start + i * (Math.PI * 2 / arms) + (random.nextDouble() - 0.5) * 0.4;
            int length = 3 + random.nextInt(2);
            for (int s = 1; s <= length; s++) {
                int dy = s <= 2 ? 0 : 2 - s; // level for two blocks, then drooping one a block
                Shapes.put(level, top.offset((int) Math.round(Math.cos(angle) * s), dy, (int) Math.round(Math.sin(angle) * s)), frond);
            }
        }
        return true;
    }

    /** A frond as worldgen places it: persistent, so it never decays away from the trunk. */
    static BlockState frond() {
        return ModBlocks.FERN_FROND.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }
}
