package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.worldgen.structure.Blueprint;
import com.hollowfrontier.worldgen.structure.ModChestLoot;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

// Structures used only by game tests (written to data/hollowfrontier/structure/ by the same datagen provider).
public final class TestBlueprints {
    private TestBlueprints() {}

    public static final String PLATFORM = "test_platform";
    public static final String RAID_TOWN = "test_raid_town";
    public static final String GARDEN = "test_garden";
    public static final String FIELD = "test_field";
    public static final String YARD = "test_yard";

    public static List<Blueprint> all() {
        return List.of(platform(), raidTown(), garden(), field(), yard());
    }

    // 16 x 16 x 16 for worldgen features (HollowFloraTests): grass on dirt at y 0, cleared air above.
    private static Blueprint garden() {
        Blueprint b = new Blueprint(GARDEN);
        b.fill(0, 0, 0, 15, 0, 15, Blocks.GRASS_BLOCK);
        b.air(0, 1, 0, 15, 15, 15);
        return b;
    }

    // 40 x 24 x 40 for the Hollow Earth's lore sites (LoreSiteTests): grass at y 0, cleared air above.
    private static Blueprint field() {
        Blueprint b = new Blueprint(FIELD);
        b.fill(0, 0, 0, 39, 0, 39, Blocks.GRASS_BLOCK);
        b.air(0, 1, 0, 39, 23, 39);
        return b;
    }

    // 48 x 28 x 48 for the live Gray station (GrayTests), which is 45 across: grass at y 0, cleared air above.
    private static Blueprint yard() {
        Blueprint b = new Blueprint(YARD);
        b.fill(0, 0, 0, 47, 0, 47, Blocks.GRASS_BLOCK);
        b.air(0, 1, 0, 47, 27, 47);
        return b;
    }

    // 12 x 6 x 12: stone floor at y 0, cleared air above.
    private static Blueprint platform() {
        Blueprint b = new Blueprint(PLATFORM);
        b.fill(0, 0, 0, 11, 0, 11, Blocks.STONE);
        b.air(0, 1, 0, 11, 5, 11);
        return b;
    }

    /**
     * 24 x 8 x 24 stand-in town for raid tests (RaidTests): a street along z 4..8, a hut (x 2..8, z 12..20, door at
     * 5,1,12) to hide in, a bank (x 14..21, z 12..21, front door 17,1,12) whose vault (iron door 17,1,18) holds one marked
     * chest at 17,1,20, a bell at 2,1,2 and a Bounty Board at 10,1,2.
     */
    private static Blueprint raidTown() {
        Blueprint b = new Blueprint(RAID_TOWN);
        b.fill(0, 0, 0, 23, 0, 23, Blocks.STONE);
        b.air(0, 1, 0, 23, 7, 23);
        // The hut
        b.walls(2, 1, 12, 8, 3, 20, Blocks.SPRUCE_PLANKS.defaultBlockState());
        b.fill(1, 4, 11, 9, 4, 21, Blocks.SPRUCE_PLANKS);
        b.door(5, 1, 12, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        // The bank and its vault
        b.walls(14, 1, 12, 21, 4, 21, Blocks.BRICKS.defaultBlockState());
        b.fill(13, 5, 11, 22, 5, 22, Blocks.BRICKS);
        b.door(17, 1, 12, (DoorBlock) Blocks.DARK_OAK_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.fill(15, 1, 18, 20, 4, 18, Blocks.STONE_BRICKS);
        b.door(17, 1, 18, (DoorBlock) Blocks.IRON_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.chest(17, 1, 20, Direction.NORTH, ModChestLoot.BANK).vault(17, 1, 20, 2);
        // The bell and the board
        b.set(2, 1, 2, Blocks.BELL);
        b.set(10, 1, 2, ModBlocks.BOUNTY_BOARD.get());
        return b;
    }
}
