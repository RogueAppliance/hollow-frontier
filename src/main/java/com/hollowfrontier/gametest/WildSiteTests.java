package com.hollowfrontier.gametest;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.worldgen.feature.GrayRelayFeature;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.hollowfrontier.worldgen.structure.WildBlueprints;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * The wild sites (27): each holds what makes it itself (and a chest of its own loot); the mine hangs its cavern from a
 * connector in its bottom layer; the relay hangs from a ceiling and nowhere else.
 */
final class WildSiteTests {
    private WildSiteTests() {}

    private static StructureTemplate template(GameTestHelper h, String name) {
        return h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(name)).orElseThrow(() -> new IllegalStateException(name + " missing"));
    }

    private static int count(StructureTemplate t, Block block) {
        return t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    static void wildSitesHoldTheirParts(GameTestHelper h) {
        StructureTemplate ziggurat = template(h, WildBlueprints.ZIGGURAT);
        h.assertValueEqual(LoreSiteTests.chestsRolling(ziggurat, ModChestLoot.ZIGGURAT), 1L, "the idol chamber's chest");
        h.assertTrue(count(ziggurat, Blocks.JUNGLE_LEAVES) >= 30 && count(ziggurat, Blocks.MOSSY_STONE_BRICKS) > 300, "the overgrown tiers");
        h.assertTrue(count(ziggurat, ModBlocks.PHLOGISTON_LAMP.get()) >= 3 && count(ziggurat, ModBlocks.STARSTEEL_BLOCK.get()) == 1, "the idol's eyes and head");
        StructureTemplate tar = template(h, WildBlueprints.TAR_PIT);
        h.assertValueEqual(LoreSiteTests.chestsRolling(tar, ModChestLoot.TAR_PIT), 1L, "the prospector's chest");
        h.assertTrue(count(tar, Blocks.BONE_BLOCK) >= 30 && count(tar, Blocks.LECTERN) == 1, "the skeletons and the notes");
        StructureTemplate mine = template(h, WildBlueprints.PHLOGISTON_MINE);
        var down = mine.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW);
        h.assertTrue(down.size() == 1 && down.getFirst().pos().getY() == 0 && JigsawBlock.getFrontFacing(down.getFirst().state()) == Direction.DOWN,
                "the mine's shaft connector");
        StructureTemplate cavern = template(h, WildBlueprints.MINE_CAVERN);
        var up = cavern.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW);
        h.assertTrue(up.size() == 1 && JigsawBlock.getFrontFacing(up.getFirst().state()) == Direction.UP
                && up.getFirst().nbt().getStringOr("name", "").equals(down.getFirst().nbt().getStringOr("target", "")), "the cavern hangs from the mine");
        h.assertTrue(LoreSiteTests.chestsRolling(cavern, ModChestLoot.PHLOGISTON_MINE_CAVERN) == 1 && count(cavern, ModBlocks.BOILER.get()) == 1
                && count(cavern, ModBlocks.CONCAVIAN_CRYSTAL.get()) > 20, "the cavern's drill, crystal and chest");
        StructureTemplate observatory = template(h, WildBlueprints.OBSERVATORY);
        h.assertTrue(LoreSiteTests.chestsRolling(observatory, ModChestLoot.OBSERVATORY) == 1 && count(observatory, Blocks.LADDER) >= 18
                && count(observatory, Blocks.GLASS) >= 10, "the observatory's ladder, lens and chest");
        StructureTemplate foundry = template(h, WildBlueprints.FOUNDRY);
        h.assertTrue(LoreSiteTests.chestsRolling(foundry, ModChestLoot.FOUNDRY) == 1 && count(foundry, Blocks.LAVA_CAULDRON) == 2
                && count(foundry, Blocks.BLAST_FURNACE) >= 5, "the foundry's crucibles, furnaces and chest");
        StructureTemplate shrine = template(h, WildBlueprints.SKY_SHRINE);
        var stone = shrine.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.WAYSTONE.get());
        h.assertValueEqual(stone.size(), 1, "the shrine's waystone");
        h.assertTrue(LoreSiteTests.chestsRolling(shrine, ModChestLoot.SKY_SHRINE) == 1, "the shrine's chest");
        StructureTemplate relay = template(h, WildBlueprints.GRAY_RELAY);
        h.assertTrue(LoreSiteTests.chestsRolling(relay, ModChestLoot.GRAY_RELAY) == 1 && count(relay, Blocks.IRON_BLOCK) >= 50, "the relay's anchor and cache");
        h.succeed();
    }

    /**
     * The relay: under a slab of stone with open air below, it hangs (its anchor in the stone, its deck below, the probe
     * on watch); with the ground too close under the ceiling, it doesn't.
     */
    static void relayHangs(GameTestHelper h) {
        BlockPos ceiling = new BlockPos(8, 18, 8);
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                h.setBlock(new BlockPos(x, 18, z), Blocks.STONE);
                h.setBlock(new BlockPos(x, 19, z), Blocks.STONE);
            }
        }
        var level = h.getLevel();
        boolean hung = GrayRelayFeature.INSTANCE.place(level, level.getChunkSource().getGenerator(), level.getRandom(), h.absolutePos(ceiling));
        h.assertTrue(hung, "the relay didn't hang from open stone");
        h.assertBlockPresent(Blocks.IRON_BLOCK, ceiling);
        h.assertTrue(h.getEntities(ModEntities.GRAY_PROBE.get()).size() == 1, "no probe on watch");
        h.getEntities(ModEntities.GRAY_PROBE.get()).forEach(net.minecraft.world.entity.Entity::discard);
        // Now a floor close under it: no room, no relay
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                h.setBlock(new BlockPos(x, 8, z), Blocks.STONE);
            }
        }
        boolean again = GrayRelayFeature.INSTANCE.place(level, level.getChunkSource().getGenerator(), level.getRandom(), h.absolutePos(new BlockPos(8, 18, 9)));
        h.assertFalse(again, "a relay hung down into the ground");
        h.succeed();
    }
}
