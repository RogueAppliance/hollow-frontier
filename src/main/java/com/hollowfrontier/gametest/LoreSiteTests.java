package com.hollowfrontier.gametest;

import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.creature.GlassbackHare;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.worldgen.HollowEarth;
import com.hollowfrontier.worldgen.structure.HollowBlueprints;
import com.hollowfrontier.worldgen.structure.HollowSiteStructure;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 9b3: the Hollow Earth's lore sites. Like HollowEarthTests these sample the dimension's generator directly (the
 * game test server loads no datapack dimensions), here to see each site find somewhere to stand under the crust; then
 * they build the sites from their templates in a test garden and check what's in them, and try the Gray probe.
 */
final class LoreSiteTests {
    private LoreSiteTests() {}

    // ---------------------------------------------------------------- finding the ground

    /** The ground scan: dry land is found, lakes and floating islands are refused. */
    static void groundSkipsLakesAndIslands(GameTestHelper h) {
        h.assertValueEqual(HollowSiteStructure.ground(column(80, Blocks.STONE, null)), 80, "plain land");
        h.assertValueEqual(HollowSiteStructure.ground(column(63, Blocks.WATER, null)), HollowSiteStructure.NO_GROUND, "a lake");
        h.assertValueEqual(HollowSiteStructure.ground(column(70, Blocks.LAVA, null)), HollowSiteStructure.NO_GROUND, "lava");
        h.assertValueEqual(HollowSiteStructure.ground(column(80, Blocks.STONE, 200)), HollowSiteStructure.NO_GROUND, "under an island");
        h.assertValueEqual(HollowSiteStructure.ground(column(175, Blocks.STONE, null)), 175, "high uplands");
        h.succeed();
    }

    /** A bare noise column: stone up to {@code top} (with {@code surface} as its top block), an island at {@code island}. */
    private static NoiseColumn column(int top, Block surface, Integer island) {
        BlockState[] states = new BlockState[HollowEarth.HEIGHT];
        for (int y = HollowEarth.MIN_Y; y <= HollowEarth.TOP_Y; y++) {
            BlockState state = Blocks.AIR.defaultBlockState();
            if (y < top) {
                state = Blocks.STONE.defaultBlockState();
            } else if (y == top) {
                state = surface.defaultBlockState();
            } else if (island != null && y > island - 6 && y <= island) {
                state = Blocks.STONE.defaultBlockState();
            } else if (y >= HollowEarth.CEILING_BASE) {
                state = Blocks.STONE.defaultBlockState();
            }
            states[y - HollowEarth.MIN_Y] = state;
        }
        return new NoiseColumn(HollowEarth.MIN_Y, states);
    }

    /**
     * Each site, tried at its structure set's spots across several seeds, finds places to stand: its floor on the land
     * (no deeper than the slope it allows under the middle), far below the crust.
     */
    static void sitesStandOnTheGround(GameTestHelper h) {
        HollowEarthTests.World world = HollowEarthTests.world(h);
        var access = h.getLevel().registryAccess();
        LevelHeightAccessor height = LevelHeightAccessor.create(HollowEarth.MIN_Y, HollowEarth.HEIGHT);
        StringBuilder report = new StringBuilder();
        for (ResourceKey<Structure> key : List.of(ModStructures.KINDLED_SITE, ModStructures.GRAY_LAB, ModStructures.GRAY_STATION, ModStructures.SYMMES_CAMP,
                ModStructures.CRYSTAL_RUINS, ModStructures.SUN_TEMPLE, ModStructures.HOLLOW_CRASH_SITE, ModStructures.ZIGGURAT, ModStructures.TAR_PIT,
                ModStructures.PHLOGISTON_MINE, ModStructures.OBSERVATORY, ModStructures.FOUNDRY, ModStructures.SKY_SHRINE)) {
            HollowSiteStructure site = (HollowSiteStructure) access.lookupOrThrow(Registries.STRUCTURE).getOrThrow(key).value();
            // The live station shares the abandoned lab's set (10b)
            String set = key == ModStructures.GRAY_STATION ? "gray_lab" : key.identifier().getPath();
            RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) access.lookupOrThrow(Registries.STRUCTURE_SET)
                    .getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, HollowFrontier.id(set))).value().placement();
            int tried = 0, found = 0, dungeons = 0, rooms = 0;
            for (long seed : HollowEarthTests.SEEDS) {
                RandomState random = world.random(seed);
                Climate.Sampler climate = random.createClimateSampler(SamplerContext.builder().enableCaches().build());
                for (int gx = -4; gx < 4 && found < 4; gx++) {
                    for (int gz = -4; gz < 4 && found < 4; gz++) {
                        ChunkPos chunk = placement.getPotentialStructureChunk(seed, gx * placement.spacing(), gz * placement.spacing());
                        Structure.GenerationContext context = new Structure.GenerationContext(access, world.generator(),
                                world.generator().getBiomeSource(), climate, random, h.getLevel().getStructureTemplateManager(),
                                seed, chunk, height, biome -> site.biomes().contains(biome));
                        tried++;
                        Optional<Structure.GenerationStub> stub = site.findValidGenerationPoint(context);
                        if (stub.isEmpty()) {
                            continue;
                        }
                        found++;
                        // The site stands by its own piece, the first; a dungeon's rooms (26) lie under it
                        List<net.minecraft.world.level.levelgen.structure.StructurePiece> pieces = stub.get().getPiecesBuilder().build().pieces();
                        BoundingBox box = pieces.getFirst().getBoundingBox();
                        int floor = box.minY();
                        if (pieces.size() > 1) {
                            boolean end = pieces.stream().anyMatch(p -> p instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece room
                                    && room.getElement().toString().contains("/end"));
                            h.assertTrue(key == ModStructures.PHLOGISTON_MINE ? pieces.size() == 2 : pieces.size() >= 5 && end, key.identifier() + " at " + stub.get().position() + ": a dungeon of " + pieces.size()
                                    + " pieces " + (end ? "" : "without its end room"));
                            h.assertTrue(pieces.subList(1, pieces.size()).stream().allMatch(p -> p instanceof com.hollowfrontier.worldgen.structure.BuriedPiece
                                    && p.getBoundingBox().maxY() < floor), key.identifier() + ": a dungeon room isn't buried under the site");
                            dungeons++;
                            rooms += pieces.size() - 1;
                        }
                        BlockPos middle = stub.get().position();
                        int ground = key == ModStructures.SKY_SHRINE ? HollowSiteStructure.islandTop(world.generator().getBaseColumn(middle.getX(), middle.getZ(), height, random)) : HollowSiteStructure.ground(world.generator().getBaseColumn(middle.getX(), middle.getZ(), height, random));
                        h.assertTrue(ground != HollowSiteStructure.NO_GROUND && Math.abs(ground - floor) <= 8,
                                key.identifier() + " at " + middle + ": floor " + floor + " but the ground is at " + ground);
                        // (The Sky Shrine stands on its island, wherever that floats; the islands are under the crust already)
                        h.assertTrue(key == ModStructures.SKY_SHRINE || box.maxY() < HollowEarth.CEILING_LOWEST - 20, key.identifier() + " reaches up to the crust: " + box);
                    }
                }
            }
            report.append(key.identifier().getPath()).append(' ').append(found).append('/').append(tried);
            if (dungeons > 0) {
                report.append(" (").append(dungeons).append(" with dungeons, ").append(rooms / dungeons).append(" rooms each)");
            }
            report.append("  ");
            if (key == ModStructures.SUN_TEMPLE || key == ModStructures.PHLOGISTON_MINE) {
                h.assertTrue(dungeons == found, key.identifier() + " without what hangs under it: " + dungeons + "/" + found);
            }
            h.assertTrue(found >= 2, key.identifier() + " found nowhere to stand: " + report);
        }
        HollowFrontier.LOGGER.info("[LoreSiteTests] sites placed per spots tried: {}", report);
        h.succeed();
    }

    // ---------------------------------------------------------------- what's in them

    private static StructureTemplate template(GameTestHelper h, String name) {
        Optional<StructureTemplate> template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(name));
        h.assertTrue(template.isPresent(), name + " template missing");
        return template.get();
    }

    private static int count(StructureTemplate template, Block block) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    /** Where the template keeps its (first) block of a kind, in its own coordinates. */
    private static StructureTemplate.StructureBlockInfo first(GameTestHelper h, StructureTemplate template, Block block) {
        var found = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block);
        h.assertFalse(found.isEmpty(), "no " + block + " in the template");
        return found.getFirst();
    }

    /** The template's chests that roll {@code table} (25: the sites have a chest to each room now). */
    static long chestsRolling(StructureTemplate template, ResourceKey<LootTable> table) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CHEST).stream().filter(chest -> rolls(chest, table)).count();
    }

    private static boolean rolls(StructureTemplate.StructureBlockInfo chest, ResourceKey<LootTable> table) {
        return chest.nbt() != null && chest.nbt().getStringOr("LootTable", "").equals(table.identifier().toString());
    }

    /**
     * The Kindled sites: the henge's obelisk (a lodestone in each face, a soul fire on top) and its ring of dark stones with
     * their offering niches, the gate (a door of iron with no way to open it; behind it the tomb, the vault and the
     * ossuary), the temple's colonnade, mural and back rooms; and the raven's tag. Rebuilt in 25: dark stone, not quartz.
     */
    static void kindledSitesHoldTheirStones(GameTestHelper h) {
        StructureTemplate henge = template(h, HollowBlueprints.KINDLED_HENGE);
        h.assertValueEqual(count(henge, Blocks.LODESTONE), 4, "lodestones in the obelisk");
        h.assertTrue(count(henge, Blocks.SOUL_CAMPFIRE) == 1 && count(henge, Blocks.TUFF_BRICKS) > 150, "the henge's fire and stones: " + count(henge, Blocks.TUFF_BRICKS));
        h.assertTrue(count(henge, Blocks.DECORATED_POT) >= 4 && count(henge, Blocks.CALCITE) == 0, "the niches' pots, and no calcite left");
        StructureTemplate gate = template(h, HollowBlueprints.KINDLED_GATE);
        h.assertTrue(count(gate, Blocks.IRON_BLOCK) >= 40, "the great door should be iron: " + count(gate, Blocks.IRON_BLOCK));
        h.assertValueEqual(count(gate, Blocks.IRON_DOOR) + count(gate, Blocks.STONE_BUTTON) + count(gate, Blocks.LEVER)
                + count(gate, Blocks.STONE_PRESSURE_PLATE), 0, "ways to open the gate");
        h.assertValueEqual(chestsRolling(gate, ModChestLoot.KINDLED_VAULT), 1L, "the vault chest");
        h.assertValueEqual(chestsRolling(gate, ModChestLoot.KINDLED_TOMB), 1L, "the tomb's chest");
        h.assertTrue(count(gate, Blocks.BONE_BLOCK) >= 8 && count(gate, Blocks.QUARTZ_PILLAR) >= 20, "the ossuary and the hall's columns");
        StructureTemplate temple = template(h, HollowBlueprints.KINDLED_TEMPLE);
        h.assertTrue(count(temple, Blocks.LAPIS_BLOCK) >= 16 && count(temple, Blocks.GLOWSTONE) == 1, "the mural's ring and sun");
        h.assertTrue(count(temple, Blocks.QUARTZ_PILLAR) >= 100, "the colonnade: " + count(temple, Blocks.QUARTZ_PILLAR));
        h.assertValueEqual(chestsRolling(temple, ModChestLoot.KINDLED_SCRIPTORIUM), 2L, "the scriptorium's and vestry's chests");
        h.assertTrue(count(temple, Blocks.LECTERN) == 2 && count(temple, Blocks.CHISELED_BOOKSHELF) >= 8, "the scriptorium");
        var structures = h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        h.assertTrue(structures.getOrThrow(ModStructures.KINDLED_SITES).stream().anyMatch(s -> s.is(ModStructures.KINDLED_SITE)),
                "kindled_sites tag lacks the Kindled site (the ravens would have nowhere to fly)");
        h.succeed();
    }

    private static void place(GameTestHelper h, StructureTemplate template) {
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        template.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings(), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
    }

    /** The lab built: its tagged hare caged, two probes watching, the bench chest's loot, tanks still full. */
    static void grayLabPlaces(GameTestHelper h) {
        StructureTemplate lab = template(h, HollowBlueprints.GRAY_LAB);
        place(h, lab);
        List<GlassbackHare> hares = h.getEntities(ModEntities.GLASSBACK_HARE.get());
        h.assertTrue(hares.size() == 1 && hares.getFirst().hasEarTag(), "the lab's hare should be there and ear-tagged: " + hares.size());
        List<GrayProbe> probes = h.getEntities(ModEntities.GRAY_PROBE.get());
        h.assertTrue(probes.size() == 2 && probes.stream().noneMatch(GrayProbe::isFleeing), "two probes, watching: " + probes.size());
        h.assertTrue(probes.stream().allMatch(p -> p.getY() > h.absolutePos(BlockPos.ZERO).getY() + 6), "the probes should hang high");
        h.assertValueEqual(chestsRolling(lab, ModChestLoot.GRAY_LAB), 1L, "the bench chest");
        h.assertValueEqual(chestsRolling(lab, ModChestLoot.GRAY_LAB_STORE) + chestsRolling(lab, ModChestLoot.GRAY_LAB_CONTROL), 2L, "the annex's chests");
        h.assertBlockPresent(Blocks.SEA_PICKLE, first(h, lab, Blocks.SEA_PICKLE).pos());
        GlassbackHare wild = h.spawn(ModEntities.GLASSBACK_HARE.get(), new BlockPos(2, 1, 2));
        h.assertFalse(wild.hasEarTag(), "a wild hare shouldn't be tagged");
        // The tanks don't leak
        var water = lab.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.WATER);
        h.assertTrue(water.size() >= 5, "the tanks should hold water: " + water.size());
        h.runAfterDelay(20, () -> {
            water.forEach(info -> h.assertBlockPresent(Blocks.WATER, info.pos()));
            h.succeed();
        });
    }

    /** The camp built: the journal on its lectern (every page from the lang file), the stores, the cold fire, the wreck. */
    static void symmesCampPlaces(GameTestHelper h) {
        StructureTemplate camp = template(h, HollowBlueprints.SYMMES_CAMP);
        place(h, camp);
        LecternBlockEntity lectern = h.getBlockEntity(first(h, camp, Blocks.LECTERN).pos(), LecternBlockEntity.class);
        WrittenBookContent book = lectern.getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
        h.assertTrue(book != null, "no journal on the lectern");
        h.assertValueEqual(book.pages().size(), HollowBlueprints.JOURNAL_PAGES, "journal pages");
        for (int i = 0; i < book.pages().size(); i++) {
            h.assertTrue(book.pages().get(i).raw().getContents() instanceof TranslatableContents t
                    && t.getKey().equals("book.hollowfrontier.symmes_journal." + (i + 1)), "page " + (i + 1) + " should come from the lang file");
        }
        h.assertValueEqual(book.author(), "E. Hartwell", "journal author");
        h.assertValueEqual(chestsRolling(camp, ModChestLoot.SYMMES_CAMP), 1L, "the stores chest");
        h.assertValueEqual(chestsRolling(camp, ModChestLoot.SYMMES_WRECK) + chestsRolling(camp, ModChestLoot.SYMMES_DIG), 2L, "the gondola's and the dig's chests");
        h.assertTrue(count(camp, Blocks.SCAFFOLDING) >= 4 && count(camp, Blocks.SPRUCE_DOOR) == 2, "the dig's scaffolding and the gondola's door");
        h.assertBlockProperty(first(h, camp, Blocks.CAMPFIRE).pos(), CampfireBlock.LIT, false);
        h.assertBlockPresent(ModBlocks.BOILER.get(), first(h, camp, ModBlocks.BOILER.get()).pos());
        h.assertTrue(count(camp, Blocks.WOOL.white()) > 150, "the airship's envelope: " + count(camp, Blocks.WOOL.white()));
        h.succeed();
    }

    // ---------------------------------------------------------------- the Gray probe

    /** Left alone it hangs where it is, watching. */
    static void probeHovers(GameTestHelper h) {
        GrayProbe probe = h.spawn(ModEntities.GRAY_PROBE.get(), new BlockPos(8, 6, 8));
        Vec3 start = probe.position();
        h.runAfterDelay(40, () -> {
            h.assertFalse(probe.isFleeing(), "it fled from no one");
            h.assertTrue(probe.position().distanceTo(start) < 0.5, "it drifted: " + probe.position().distanceTo(start));
            h.succeed();
        });
    }

    /** Come close and it shoots up and away, and blinks out. */
    static void probeFleesFromPlayer(GameTestHelper h) {
        GrayProbe probe = h.spawn(ModEntities.GRAY_PROBE.get(), new BlockPos(8, 6, 8));
        double startY = probe.getY();
        TestPlayers.survival(h, new BlockPos(8, 1, 3));
        h.runAfterDelay(8, () -> {
            h.assertTrue(probe.isRemoved() || (probe.isFleeing() && probe.getY() > startY + 1), "it didn't bolt (y " + (probe.getY() - startY) + ")");
            h.succeedWhen(() -> h.assertTrue(probe.isRemoved(), "it should blink out"));
        });
    }

    /** From across the garden, a look is enough; a back turned isn't. */
    static void probeFleesWhenStaredAt(GameTestHelper h) {
        GrayProbe probe = h.spawn(ModEntities.GRAY_PROBE.get(), new BlockPos(14, 9, 14));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(1, 1, 1));
        h.assertTrue(player.distanceTo(probe) > GrayProbe.NOTICE_RANGE, "the player starts too close");
        player.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition().add(player.getEyePosition().subtract(probe.getEyePosition())));
        h.runAfterDelay(20, () -> {
            h.assertFalse(probe.isFleeing(), "it fled from a turned back");
            player.lookAt(EntityAnchorArgument.Anchor.EYES, probe.getEyePosition());
            h.succeedWhen(() -> h.assertTrue(probe.isFleeing() || probe.isRemoved(), "it should bolt when stared at"));
        });
    }

    /** Hit it and it bolts rather than fights. */
    static void probeFleesWhenHit(GameTestHelper h) {
        GrayProbe probe = h.spawn(ModEntities.GRAY_PROBE.get(), new BlockPos(8, 6, 8));
        probe.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 1.0F);
        h.assertTrue(probe.isFleeing() && probe.isAlive(), "a hit probe should flee");
        h.succeed();
    }
}
