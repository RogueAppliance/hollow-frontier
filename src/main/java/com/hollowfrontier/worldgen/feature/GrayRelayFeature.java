package com.hollowfrontier.worldgen.feature;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.worldgen.structure.WildBlueprints;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * A Gray relay (27) hung under the crust: the {@link WildBlueprints#GRAY_RELAY} template, placed with its anchor (its top
 * two layers) set into the rock it hangs from. The origin is the ceiling block found over open air; the relay goes in
 * only where there's air enough under it for its deck and dish, so it never hangs into a spire or down onto the land.
 */
public record GrayRelayFeature() implements Feature {
    public static final GrayRelayFeature INSTANCE = new GrayRelayFeature();
    public static final MapCodec<GrayRelayFeature> CODEC = MapCodec.unit(INSTANCE);
    /** How far the template reaches below its anchor's top: its height less the anchor's two layers set in the rock. */
    public static final int HANG = 14;

    @Override
    public MapCodec<GrayRelayFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        StructureTemplate template = level.getLevel().getStructureTemplateManager().get(HollowFrontier.id(WildBlueprints.GRAY_RELAY)).orElse(null);
        if (template == null) {
            return false;
        }
        int w = template.getSize().getX(), d = template.getSize().getZ();
        BlockPos corner = origin.offset(-w / 2, -HANG, -d / 2);
        // Open air under the whole of it, from the dish up to just under the rock, at its middle and its corners
        for (int[] p : new int[][] {{w / 2, d / 2}, {0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}}) {
            for (int y = -2; y < HANG; y++) {
                if (!level.getBlockState(corner.offset(p[0], y, p[1])).isAir()) {
                    return false;
                }
            }
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(Rotation.getRandom(random))
                .setRotationPivot(new BlockPos(w / 2, 0, d / 2)).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        return template.placeInWorld(level, corner, corner, settings, random, Block.UPDATE_CLIENTS);
    }
}
