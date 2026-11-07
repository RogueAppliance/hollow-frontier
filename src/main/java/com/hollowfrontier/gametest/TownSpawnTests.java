package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.town.TownConfig;
import com.hollowfrontier.content.frontier.town.TownSpawn;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.hollowfrontier.worldgen.structure.TownStyle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

// New worlds starting in a town. The test world has no towns to find; real searches and landings are in DECISIONS, "New worlds start in town".
final class TownSpawnTests {
    private TownSpawnTests() {}

    private static final List<String> SQUARES = List.of("square_cross_bandstand", "square_cross_well", "square_tee_bandstand", "square_tee_well",
            "square_pair_bandstand", "square_pair_well");

    /** Every square of every style has open paving to land on, under the sky, whatever stands in its middle. */
    static void everySquareHasALanding(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        for (TownStyle style : TownStyle.values()) {
            for (String name : SQUARES) {
                String id = style.template(name);
                StructureTemplate square = level.getStructureTemplateManager().get(HollowFrontier.id(id)).orElseThrow();
                square.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
                BoundingBox box = square.getBoundingBox(settings, origin);
                BlockPos spot = TownSpawn.landing(level, box).orElse(null);
                h.assertTrue(spot != null, id + " has no clear landing");
                h.assertTrue(spot.getY() == box.minY() + 1 && box.isInside(spot), id + " landed at " + spot + " in " + box);
                h.assertFalse(level.getBlockState(spot.below()).getCollisionShape(level, spot.below()).isEmpty(), id + " has no ground at " + spot);
                for (BlockPos above = spot; above.getY() <= box.maxY(); above = above.above()) {
                    h.assertTrue(level.getBlockState(above).isAir(), id + " has " + level.getBlockState(above) + " over the landing");
                }
                BlockPos.betweenClosed(box.minX(), box.minY() + 1, box.minZ(), box.maxX(), box.maxY(), box.maxZ())
                        .forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
            }
        }
        h.succeed();
    }

    /** A stand-in square, roofed but for one yard, with a pond in the only other gap: the landing has to pick the yard. */
    static void landingKeepsOutFromUnderRoofs(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos floor = h.absolutePos(new BlockPos(0, 1, 0));
        BoundingBox box = new BoundingBox(floor.getX(), floor.getY(), floor.getZ(), floor.getX() + 12, floor.getY() + 5, floor.getZ() + 12);
        BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.minY(), box.maxZ())
                .forEach(pos -> level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS));
        BlockPos.betweenClosed(box.minX(), box.maxY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())
                .forEach(pos -> level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS));
        h.assertTrue(TownSpawn.landing(level, box).isEmpty(), "landed under a whole roof");
        // The gaps: a pond by the middle, and a dry yard in the far corner
        BlockPos pond = floor.offset(3, 0, 3);
        BlockPos yard = floor.offset(10, 0, 10);
        for (BlockPos hole : BlockPos.betweenClosed(pond.offset(-1, 0, -1), pond.offset(1, 4 + 1, 1))) {
            level.setBlock(hole, hole.getY() == floor.getY() ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos hole : BlockPos.betweenClosed(yard.offset(-1, 5, -1), yard.offset(1, 5, 1))) {
            level.setBlock(hole, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        BlockPos spot = TownSpawn.landing(level, box).orElse(null);
        h.assertTrue(yard.above().equals(spot), "landed at " + spot + ", not the yard at " + yard.above());
        h.succeed();
    }

    /** No town to find (this world has no structures): the game keeps its own spawn, and the setting is on by default. */
    static void noTownLeavesTheSpawnAlone(GameTestHelper h) {
        h.assertFalse(h.getLevel().getServer().getWorldGenSettings().options().generateStructures(), "this world can find towns");
        h.assertTrue(TownSpawn.find(h.getLevel()).isEmpty(), "found a town in a world without structures");
        h.assertTrue(TownConfig.spawnAtTown(), "new worlds should start in town by default");
        h.succeed();
    }

    static void townsTagHoldsEveryTown(GameTestHelper h) {
        var structures = h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (var town : ModStructures.TOWNS) {
            h.assertTrue(structures.getOrThrow(town).is(ModStructures.TOWN_TAG), town.identifier() + " isn't in the towns tag");
        }
        h.succeed();
    }
}
