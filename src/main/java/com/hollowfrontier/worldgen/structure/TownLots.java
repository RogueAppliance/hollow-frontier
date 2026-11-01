package com.hollowfrontier.worldgen.structure;

import static com.hollowfrontier.worldgen.structure.TownBlueprints.s;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Full lots for the buildings on street and lane lots (24a). Lots are 12 apart, and the buildings 7 to 11 wide, so a
 * street had bare ground between its buildings, where grass, flowers and trees came up after the town was built. Each
 * such building is now drawn on a lot {@value #WIDTH} wide: the building, and a side yard on each side it doesn't fill.
 * Lots abut (12 apart, 12 wide), so a street is solid frontage. A side yard carries the boardwalk on along the front,
 * has a fence and gate across its mouth, hard ground, and what a yard has by its width: one wide, a rain barrel and
 * crates; two, firewood and crates; three or more, a washing line too. The connector stays at the building's middle, so
 * a lot isn't centred on it when the yards differ in width; neighbours on one side of a street all turn the same way, so
 * they still meet exactly.
 */
public final class TownLots {
    private TownLots() {}

    public static final int WIDTH = 12;

    /** The building on a full lot (or the building itself, if it's already as wide). */
    static Blueprint inLot(Blueprint building) {
        int w = building.sizeX(), d = building.sizeZ();
        if (w >= WIDTH) {
            return building;
        }
        int left = (WIDTH - w) / 2, right = WIDTH - w - left;
        Blueprint b = new Blueprint(building.name());
        b.paste(building, left, 0, 0);
        int seed = building.name().hashCode();
        yard(b, building, 0, left, d, 0, seed);
        yard(b, building, left + w, right, d, w - 1, seed >>> 3);
        return b;
    }

    // A side yard x0..x0+width-1, its walk copied from the building's front at column edge.
    private static void yard(Blueprint b, Blueprint building, int x0, int width, int d, int edge, int seed) {
        if (width <= 0) {
            return;
        }
        int x1 = x0 + width - 1;
        b.air(x0, 1, 0, x1, 8, d - 1);
        for (int z = 0; z < d; z++) {
            BlockState front = z < 2 ? building.get(new BlockPos(edge, 0, z)) : null;
            b.fill(x0, 0, z, x1, 0, z, front != null && !front.is(Blocks.JIGSAW) ? front : s(Blocks.COARSE_DIRT));
        }
        // The mouth: a fence across with a gate, and a fence along the back
        b.fill(x0, 1, 2, x1, 1, 2, Blocks.SPRUCE_FENCE);
        b.set(x0 + width / 2, 1, 2, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        b.fill(x0, 1, d - 1, x1, 1, d - 1, Blocks.SPRUCE_FENCE);
        int far = d - 2;
        switch (Math.min(width, 3)) {
            case 1 -> {
                b.set(x0, 1, 3, water(2));                                 // a rain barrel under the eaves
                b.set(x0, 1, far, TownDecor.barrel(Direction.UP));
                if (far - 1 > 3) {
                    b.set(x0, 1, far - 1, TownDecor.barrel(Direction.NORTH));
                    b.set(x0, 2, far, Blocks.BARREL);
                }
            }
            case 2 -> {
                int wood = (seed & 1) == 0 ? x0 : x1, path = wood == x0 ? x1 : x0;
                b.fill(wood, 1, 4, wood, 2, Math.min(far, 6), TownBlueprints.log(Blocks.OAK_LOG, Direction.Axis.Z));
                b.set(wood, 1, far, TownDecor.barrel(Direction.UP));
                b.set(wood, 2, far, Blocks.BARREL);
                b.set(path, 1, 3, water(2));
                if (far > 7) {
                    b.set(wood, 1, far - 1, Blocks.HAY_BLOCK);
                }
            }
            default -> {
                int line = x0 + width / 2, side = (seed & 1) == 0 ? x0 : x1;
                // The washing line: two posts, string between, sheets and shirts pegged on it
                if (far >= 8) {
                    b.fill(line, 1, 4, line, 3, 4, Blocks.SPRUCE_FENCE);
                    b.fill(line, 1, 8, line, 3, 8, Blocks.SPRUCE_FENCE);
                    b.fill(line, 3, 5, line, 3, 7, Blocks.TRIPWIRE);
                    b.set(line, 2, 5, Blocks.STAINED_GLASS_PANE.white());
                    b.set(line, 2, 7, (seed & 2) == 0 ? Blocks.STAINED_GLASS_PANE.lightBlue() : Blocks.STAINED_GLASS_PANE.red());
                }
                b.set(side, 1, 3, water(2));
                b.set(side, 1, far, TownDecor.barrel(Direction.UP));
                b.set(side, 2, far, Blocks.BARREL);
                b.set(side == x0 ? x1 : x0, 1, far, Blocks.HAY_BLOCK);
            }
        }
    }
}
