package com.hollowfrontier.worldgen.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Pale Kindled stonework (Concavian Highlands), older than anything on the surface world: a lone standing stone of
 * calcite and dressed diorite, sometimes capped with carved quartz; a quartz pillar toppled in the grass beside its
 * broken stump; or, rarely, a ring of short stones round a carved floor stone. Moss has crept over some. Ravens (9b2)
 * perch on them, and phase 12a's ruins are of the same work. The origin is the block above the ground.
 */
public record StandingStoneFeature() implements Feature {
    public static final StandingStoneFeature INSTANCE = new StandingStoneFeature();
    public static final MapCodec<StandingStoneFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<StandingStoneFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!Shapes.ground(level.getBlockState(origin.below())) || !Shapes.free(level, origin) || !flat(level, origin)) {
            return false;
        }
        int kind = random.nextInt(10);
        if (kind < 6) {
            return stone(level, random, origin, 3 + random.nextInt(3), true);
        }
        if (kind < 9) {
            return toppledPillar(level, random, origin);
        }
        return ring(level, random, origin);
    }

    /**
     * The builders chose level ground: within two blocks of the origin, the ground is never more than a block off its
     * height (the first photos had pillars stair-stepping up hillsides).
     */
    private static boolean flat(WorldGenLevel level, BlockPos origin) {
        int ground = origin.getY() - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int y = Shapes.surface(level, origin.getX() + dx, origin.getZ() + dz, ground + 3, ground - 3);
                if (y == Integer.MIN_VALUE || Math.abs(y - ground) > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /** One standing stone, set a block into the ground. */
    private static boolean stone(WorldGenLevel level, RandomSource random, BlockPos base, int height, boolean cap) {
        for (int y = 0; y < height; y++) {
            if (!Shapes.free(level, base.above(y))) {
                return false;
            }
        }
        level.setBlock(base.below(), Blocks.CALCITE.defaultBlockState(), 2);
        for (int y = 0; y < height; y++) {
            level.setBlock(base.above(y), random.nextInt(3) == 0 ? Blocks.POLISHED_DIORITE.defaultBlockState() : Blocks.CALCITE.defaultBlockState(), 2);
        }
        BlockPos top = base.above(height);
        if (cap && random.nextBoolean()) {
            Shapes.put(level, top, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        } else if (random.nextInt(3) == 0) {
            Shapes.put(level, top, Blocks.PALE_MOSS_CARPET.defaultBlockState());
        }
        return true;
    }

    /** A fallen pillar lying flat along the ground, its stump still standing; it breaks off where the ground rises or falls. */
    private static boolean toppledPillar(WorldGenLevel level, RandomSource random, BlockPos origin) {
        Direction along = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int stump = 1 + random.nextInt(2);
        for (int y = 0; y < stump; y++) {
            if (!Shapes.put(level, origin.above(y), Blocks.QUARTZ_PILLAR.defaultBlockState())) {
                return false;
            }
        }
        BlockState lying = Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, along.getAxis());
        int length = 3 + random.nextInt(3);
        int ground = origin.getY() - 1;
        for (int s = 2; s < 2 + length; s++) {
            BlockPos column = origin.relative(along, s);
            if (Shapes.surface(level, column.getX(), column.getZ(), ground + 2, ground - 2) != ground) {
                break;
            }
            Shapes.put(level, column, lying);
            if (random.nextInt(4) == 0) {
                Shapes.put(level, column.above(), Blocks.PALE_MOSS_CARPET.defaultBlockState());
            }
        }
        return true;
    }

    /** Six to eight short stones round a carved floor stone, each set on the ground where it stands. */
    private static boolean ring(WorldGenLevel level, RandomSource random, BlockPos origin) {
        level.setBlock(origin.below(), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), 2);
        int stones = 6 + random.nextInt(3);
        double radius = 4 + random.nextInt(2);
        double start = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < stones; i++) {
            double angle = start + i * (Math.PI * 2 / stones);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = Shapes.surface(level, x, z, origin.getY() + 4, origin.getY() - 6);
            if (y != Integer.MIN_VALUE) {
                stone(level, random, new BlockPos(x, y + 1, z), 2 + random.nextInt(2), false);
            }
        }
        return true;
    }
}
