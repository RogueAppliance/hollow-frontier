package com.hollowfrontier.worldgen.structure;

import java.util.List;
import java.util.Map;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Paint jobs: each building comes in a few versions built from the same blueprint with some blocks swapped (the wood
 * of its walls, its brick), so a street of general stores doesn't look like one store copied. Variant 1 is the
 * blueprint as drawn ("town/house"), later ones get a suffix ("town/house_2"). A town style's palette goes on top.
 */
public final class TownPalettes {
    private TownPalettes() {}

    private static final Map<Block, Block> AS_DRAWN = Map.of();

    private static final Map<String, List<Map<Block, Block>>> VARIANTS = Map.ofEntries(
            Map.entry("house", List.of(AS_DRAWN,
                    Map.of(Blocks.BIRCH_PLANKS, Blocks.OAK_PLANKS, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_OAK_LOG),
                    Map.of(Blocks.BIRCH_PLANKS, Blocks.ACACIA_PLANKS, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_ACACIA_LOG),
                    Map.of(Blocks.BIRCH_PLANKS, Blocks.PALE_OAK_PLANKS, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_PALE_OAK_LOG,
                            Blocks.SPRUCE_STAIRS, Blocks.DARK_OAK_STAIRS, Blocks.SPRUCE_SLAB, Blocks.DARK_OAK_SLAB))),
            Map.entry("general_store", List.of(AS_DRAWN,
                    Map.of(Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG),
                    Map.of(Blocks.OAK_PLANKS, Blocks.ACACIA_PLANKS, Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_ACACIA_LOG))),
            Map.entry("sheriff_office", List.of(AS_DRAWN,
                    Map.of(Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_LOG, Blocks.DARK_OAK_LOG,
                            Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD))),
            Map.entry("bank", List.of(AS_DRAWN,
                    Map.of(Blocks.BRICKS, Blocks.POLISHED_GRANITE, Blocks.BRICK_SLAB, Blocks.POLISHED_GRANITE_SLAB))),
            Map.entry("hotel", List.of(AS_DRAWN,
                    Map.of(Blocks.BIRCH_PLANKS, Blocks.OAK_PLANKS),
                    Map.of(Blocks.BIRCH_PLANKS, Blocks.PALE_OAK_PLANKS))),
            Map.entry("hotel_grand", List.of(AS_DRAWN,
                    Map.of(Blocks.BIRCH_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG))),
            Map.entry("saloon", List.of(AS_DRAWN,
                    Map.of(Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_DARK_OAK_LOG))),
            Map.entry("saloon_grand", List.of(AS_DRAWN,
                    Map.of(Blocks.SPRUCE_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_MANGROVE_LOG))),
            Map.entry("blacksmith", List.of(AS_DRAWN,
                    Map.of(Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_LOG, Blocks.DARK_OAK_LOG))),
            Map.entry("livery", List.of(AS_DRAWN,
                    Map.of(Blocks.SPRUCE_PLANKS, Blocks.MANGROVE_PLANKS),           // a red barn
                    Map.of(Blocks.SPRUCE_PLANKS, Blocks.OAK_PLANKS))),
            Map.entry("adobe", List.of(AS_DRAWN,
                    Map.of(Blocks.MUD_BRICKS, Blocks.TERRACOTTA),
                    Map.of(Blocks.MUD_BRICKS, Blocks.SMOOTH_SANDSTONE, Blocks.MUD_BRICK_SLAB, Blocks.SMOOTH_SANDSTONE_SLAB,
                            Blocks.MUD_BRICK_WALL, Blocks.SANDSTONE_WALL))),
            Map.entry("courthouse", List.of(AS_DRAWN,
                    Map.of(Blocks.BRICKS, Blocks.STONE_BRICKS, Blocks.QUARTZ_PILLAR, Blocks.STRIPPED_BIRCH_LOG))),
            Map.entry("homestead", List.of(AS_DRAWN,
                    Map.of(Blocks.BIRCH_PLANKS, Blocks.OAK_PLANKS, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_OAK_LOG),
                    Map.of(Blocks.BIRCH_PLANKS, Blocks.PALE_OAK_PLANKS, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_DARK_OAK_LOG,
                            Blocks.SPRUCE_STAIRS, Blocks.DARK_OAK_STAIRS, Blocks.SPRUCE_SLAB, Blocks.DARK_OAK_SLAB))),
            Map.entry("gunsmith", List.of(AS_DRAWN,
                    Map.of(Blocks.ACACIA_PLANKS, Blocks.SPRUCE_PLANKS))),
            Map.entry("church", List.of(AS_DRAWN,
                    Map.of(Blocks.BIRCH_PLANKS, Blocks.PALE_OAK_PLANKS, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_PALE_OAK_LOG))));

    /** The palettes of a building (by base name, e.g. "house"); one (as drawn) for anything not listed. */
    public static List<Map<Block, Block>> of(String base) {
        return VARIANTS.getOrDefault(base, List.of(AS_DRAWN));
    }

    public static int count(String base) {
        return of(base).size();
    }

    /** "house" for the first variant, "house_2" for the second and so on. */
    public static String variant(String base, int index) {
        return index == 0 ? base : base + "_" + (index + 1);
    }
}
