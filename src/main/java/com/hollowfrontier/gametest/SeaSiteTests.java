package com.hollowfrontier.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.concavia.ConcavianCodexItem;
import com.hollowfrontier.content.ufo.PrototypeSaucer;
import com.hollowfrontier.content.ufo.PrototypeSaucerItem;
import com.hollowfrontier.content.ufo.SaucerFittings;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.HollowEarth;
import com.hollowfrontier.worldgen.structure.HollowSiteStructure;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.hollowfrontier.worldgen.structure.SeaBlueprints;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 43d: the Drowned Quarter, the Symmes Landing and the Sealed Hull. Where the sites go is checked on bare noise
 * columns and registry entries; the Quarter itself is built in a flooded field, as the sea would have it, to see that its
 * domes keep their air.
 */
final class SeaSiteTests {
    private SeaSiteTests() {}

    /** The seabed mode: a bed under deep water, and nothing else. Only the two sea sites may stand in the Still Sea. */
    static void seaSitesGoWhereTheyShould(GameTestHelper h) {
        int sea = HollowEarth.SEA_LEVEL;
        h.assertValueEqual(HollowSiteStructure.seabed(column(sea - 26, true)), sea - 26, "a bed under deep water");
        h.assertValueEqual(HollowSiteStructure.seabed(column(sea - 1 - HollowSiteStructure.SEABED_MIN_DEPTH, true)), sea - 1 - HollowSiteStructure.SEABED_MIN_DEPTH,
                "a bed just deep enough");
        h.assertValueEqual(HollowSiteStructure.seabed(column(sea - 10, true)), HollowSiteStructure.NO_GROUND, "the shallows");
        h.assertValueEqual(HollowSiteStructure.seabed(column(sea + 12, false)), HollowSiteStructure.NO_GROUND, "dry land");
        h.assertValueEqual(HollowSiteStructure.ground(column(sea - 26, true)), HollowSiteStructure.NO_GROUND, "the land's sites should still refuse the sea");

        var access = h.getLevel().registryAccess();
        var seaBiome = access.lookupOrThrow(Registries.BIOME).getOrThrow(HollowBiomes.STILL_SEA);
        Set<ResourceKey<?>> inTheSea = new HashSet<>();
        access.lookupOrThrow(Registries.STRUCTURE).listElements().forEach(structure -> {
            if (structure.value().biomes().contains(seaBiome)) {
                inTheSea.add(structure.key());
                h.assertValueEqual(structure.value().biomes().size(), 1, "biomes for " + structure.key().identifier() + " (a sea site belongs to the sea alone)");
            }
        });
        h.assertTrue(inTheSea.equals(Set.of(ModStructures.DROWNED_QUARTER, ModStructures.SYMMES_LANDING)), "structures in the Still Sea: " + inTheSea);
        h.succeed();
    }

    /**
     * The Quarter built under water: its vault, chests, record and guardians are there; a minute later its domes still hold
     * their air, each with its pool level with the floor and its lamp lit; and nothing outside the domes stands dry.
     */
    static void drownedQuarterKeepsItsAir(GameTestHelper h) {
        int depth = 20;
        for (int x = 0; x < 40; x++) {
            for (int z = 0; z < 40; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.SAND);
                for (int y = 1; y <= depth; y++) {
                    h.setBlock(new BlockPos(x, y, z), x == 0 || z == 0 || x == 39 || z == 39 ? Blocks.SANDSTONE : Blocks.WATER);
                }
            }
        }
        StructureTemplate quarter = template(h, SeaBlueprints.DROWNED_QUARTER);
        h.assertTrue(quarter.getSize().getX() <= 40 && quarter.getSize().getZ() <= 40 && quarter.getSize().getY() <= HollowSiteStructure.SEABED_MIN_DEPTH,
                "the Quarter should fit under the least water it's given: " + quarter.getSize());
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        quarter.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings().setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING),
                h.getLevel().getRandom(), Block.UPDATE_CLIENTS);

        h.assertValueEqual(LoreSiteTests.chestsRolling(quarter, ModChestLoot.DROWNED_VAULT), 1L, "the vault");
        h.assertValueEqual(LoreSiteTests.chestsRolling(quarter, ModChestLoot.DROWNED_HOME), 2L, "the small domes' chests");
        h.assertValueEqual(LoreSiteTests.chestsRolling(quarter, ModChestLoot.DROWNED_STREET), 2L, "the flooded houses' chests");
        h.assertValueEqual(h.getEntities(ModEntities.CRYSTAL_GUARDIAN.get()).size(), 3, "guardians in the domes");
        h.assertTrue(count(quarter, ModBlocks.JELLY_LAMP.get()) >= 9, "lamps: " + count(quarter, ModBlocks.JELLY_LAMP.get()));
        h.assertTrue(count(quarter, Blocks.GLASS) > 200, "the domes' glass: " + count(quarter, Blocks.GLASS));
        var lecterns = quarter.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.LECTERN);
        h.assertValueEqual(lecterns.size(), 1, "lecterns");
        WrittenBookContent record = h.getBlockEntity(lecterns.getFirst().pos(), LecternBlockEntity.class).getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
        h.assertTrue(record != null && record.pages().size() == SeaBlueprints.RECORD_PAGES, "the record on its lectern");
        for (int i = 0; i < record.pages().size(); i++) {
            h.assertTrue(record.pages().get(i).raw().getContents() instanceof TranslatableContents t
                    && t.getKey().equals("book.hollowfrontier.drowned_record." + (i + 1)), "page " + (i + 1) + " should come from the lang file");
        }
        // Nothing that can hold water stands dry outside a dome
        int floor = SeaBlueprints.DRUM + 1;
        for (StructureTemplate.StructureBlockInfo info : quarter.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CHEST)) {
            h.assertTrue(inADome(info.pos()) || info.state().getValue(BlockStateProperties.WATERLOGGED), "a dry chest under the sea at " + info.pos());
        }
        for (StructureTemplate.StructureBlockInfo info : quarter.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.DECORATED_POT)) {
            h.assertTrue(inADome(info.pos()) || info.state().getValue(BlockStateProperties.WATERLOGGED), "a dry pot under the sea at " + info.pos());
        }
        // Stir every pool and passage, as a diver would, and give the water a minute to do its worst
        for (int[] dome : List.of(SeaBlueprints.GREAT_DOME, SeaBlueprints.WEST_DOME, SeaBlueprints.EAST_DOME)) {
            for (BlockPos pos : BlockPos.betweenClosed(dome[0] - dome[2] - 2, 1, dome[1] - dome[2] - 2, dome[0] + dome[2] + 2, floor, dome[1] + dome[2] + 2)) {
                if (h.getBlockState(pos).is(Blocks.WATER)) {
                    h.getLevel().scheduleTick(h.absolutePos(pos), Fluids.WATER, 1);
                }
            }
        }
        h.runAfterDelay(60, () -> {
            for (int[] dome : List.of(SeaBlueprints.GREAT_DOME, SeaBlueprints.WEST_DOME, SeaBlueprints.EAST_DOME)) {
                int cx = dome[0], cz = dome[1], radius = dome[2];
                h.assertBlockPresent(Blocks.WATER, new BlockPos(cx, SeaBlueprints.DRUM, cz));
                h.assertBlockPresent(ModBlocks.JELLY_LAMP.get(), new BlockPos(cx, SeaBlueprints.DRUM + radius, cz));
                int air = 0, wet = 0;
                for (BlockPos pos : BlockPos.betweenClosed(cx - radius, floor, cz - radius, cx + radius, floor + radius, cz + radius)) {
                    double shell = Math.sqrt(sq(pos.getX() - cx) + sq(pos.getY() - SeaBlueprints.DRUM - 0.5) + sq(pos.getZ() - cz));
                    if (shell > radius - 0.6) {
                        continue;
                    }
                    BlockState state = h.getBlockState(pos);
                    if (state.isAir()) {
                        air++;
                    } else if (!state.getFluidState().isEmpty()) {
                        wet++;
                    }
                }
                h.assertTrue(air > radius * radius * 3, "the dome at " + cx + "," + cz + " has only " + air + " blocks of air");
                h.assertValueEqual(wet, 0, "water inside the dome at " + cx + "," + cz);
                // And it is under the sea: water right over its keystone
                h.assertBlockPresent(Blocks.WATER, new BlockPos(cx, SeaBlueprints.DRUM + radius + 2, cz));
            }
            h.succeed();
        });
    }

    private static boolean inADome(BlockPos pos) {
        for (int[] dome : List.of(SeaBlueprints.GREAT_DOME, SeaBlueprints.WEST_DOME, SeaBlueprints.EAST_DOME)) {
            if (pos.getY() > SeaBlueprints.DRUM && Math.hypot(pos.getX() - dome[0], pos.getZ() - dome[1]) < dome[2]) {
                return true;
            }
        }
        return false;
    }

    /** The Landing built: the boat's log on its lectern, the crate, the cold fire, the boat. */
    static void symmesLandingPlaces(GameTestHelper h) {
        StructureTemplate landing = template(h, SeaBlueprints.SYMMES_LANDING);
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        landing.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings(), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
        var lecterns = landing.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.LECTERN);
        h.assertValueEqual(lecterns.size(), 1, "lecterns");
        WrittenBookContent log = h.getBlockEntity(lecterns.getFirst().pos(), LecternBlockEntity.class).getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
        h.assertTrue(log != null && log.pages().size() == SeaBlueprints.LOG_PAGES && log.author().equals("T. Avery"), "the boat's log on its lectern");
        h.assertValueEqual(LoreSiteTests.chestsRolling(landing, ModChestLoot.SYMMES_LANDING), 1L, "the crate");
        h.assertTrue(count(landing, Blocks.SPRUCE_PLANKS) >= 7 && count(landing, Blocks.CAMPFIRE) == 1, "the boat and the fire");
        h.succeed();
    }

    /** The vault always gives up the Sealed Hull, and the Landing's crate always holds a chart to a Quarter and a harpoon. */
    static void seaTablesGiveWhatTheyPromise(GameTestHelper h) {
        Set<Item> vault = new HashSet<>(), home = new HashSet<>();
        for (int i = 0; i < 150; i++) {
            List<ItemStack> loot = roll(h, ModChestLoot.DROWNED_VAULT);
            h.assertTrue(loot.stream().filter(stack -> stack.is(ModItems.SEALED_HULL.get())).count() == 1, "a vault without exactly one Sealed Hull");
            loot.forEach(stack -> vault.add(stack.getItem()));
            roll(h, ModChestLoot.DROWNED_HOME).forEach(stack -> home.add(stack.getItem()));
            h.assertFalse(roll(h, ModChestLoot.DROWNED_STREET).stream().anyMatch(stack -> stack.is(ModItems.SEALED_HULL.get())), "a Sealed Hull in the street");
        }
        h.assertTrue(vault.contains(ModItems.PILOTS_BELL.get()) && vault.contains(ModItems.WATER_CLOCK.get()), "the vault never gave its relics: " + vault);
        h.assertTrue(home.contains(ModItems.WAVE_POTTERY_SHERD.get()), "the domes never gave the Wave sherd");
        for (int i = 0; i < 20; i++) {
            List<ItemStack> crate = roll(h, ModChestLoot.SYMMES_LANDING);
            h.assertTrue(crate.stream().anyMatch(stack -> stack.is(ModItems.HARPOON.get())), "a crate with no harpoon");
            h.assertTrue(crate.stream().anyMatch(stack -> stack.is(Items.MAP) || stack.is(Items.FILLED_MAP)), "a crate with no chart");
        }
        var ops = h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        String json = LootTable.DIRECT_CODEC.encodeStart(ops, h.getLevel().getServer().reloadableRegistries().getLootTable(ModChestLoot.SYMMES_LANDING)).getOrThrow().toString();
        h.assertTrue(json.contains("exploration_map") && json.contains("\"hollowfrontier:" + SeaBlueprints.DROWNED_QUARTER + "\""), "the crate's chart doesn't lead to a Drowned Quarter");
        ConcavianCodexItem.Chapter chapter = ConcavianCodexItem.CHAPTERS.getLast();
        h.assertTrue(chapter.name().equals("sea") && chapter.steps().contains("hollow/drowned_quarter") && chapter.steps().contains("steam/submersible"),
                "the Codex should close on the sea: " + chapter);
        for (String step : chapter.steps()) {
            h.assertTrue(h.getLevel().getServer().getAdvancements().get(HollowFrontier.id(step)) != null, "the Codex's sea chapter names a missing advancement: " + step);
        }
        h.succeed();
    }

    /**
     * The Sealed Hull is a fitting like the others, and extra to "fully fitted". In the water a saucer without it is pushed
     * back up whatever its helm says; one with it dives, and its crew keeps its air.
     */
    static void sealedHullLetsASaucerDive(GameTestHelper h) {
        SeaLifeTests.pool(h, 14);
        ItemStack hull = new ItemStack(ModItems.SEALED_HULL.get());
        h.assertTrue(SaucerFittings.isPart(hull) && !SaucerFittings.STANDARD.hull() && SaucerFittings.STANDARD.fit(hull).hull(), "the Sealed Hull should fit");
        h.assertTrue(SaucerFittings.ALL.full() && SaucerFittings.ALL.fit(hull).full() && !SaucerFittings.ALL.hull(), "fully fitted is still the Grays' five");
        h.assertTrue(SaucerFittings.fromMask(SaucerFittings.STANDARD.fit(hull).mask()).hull(), "the hull is lost in the mask");
        h.assertTrue(SaucerFittings.STANDARD.fit(hull).fit(hull) == SaucerFittings.STANDARD.fit(hull) || SaucerFittings.STANDARD.fit(hull).fit(hull).equals(SaucerFittings.STANDARD.fit(hull)),
                "a second hull should change nothing");

        PrototypeSaucer open = saucer(h, new Vec3(14.5, 13.0, 20.5), SaucerFittings.STANDARD);
        PrototypeSaucer sealed = saucer(h, new Vec3(26.5, 13.0, 20.5), SaucerFittings.STANDARD.fit(hull));
        Pig rider = h.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(26, 14, 20));
        h.assertTrue(rider.startRiding(sealed), "the rider couldn't get in");
        open.setHelm(0, -1);
        sealed.setHelm(0, -1);
        double openY = open.getY(), sealedY = sealed.getY();
        h.runAfterDelay(60, () -> {
            h.assertTrue(sealed.getY() < sealedY - 4.0 && sealed.awash(), "the sealed saucer didn't dive: " + sealedY + " -> " + sealed.getY());
            h.assertTrue(open.getY() > openY - 1.5, "an unsealed saucer went under: " + openY + " -> " + open.getY());
            h.assertTrue(rider.getAirSupply() >= rider.getMaxAirSupply() - 1, "the sealed saucer's rider is running out of air: " + rider.getAirSupply());
            h.succeed();
        });
    }

    private static PrototypeSaucer saucer(GameTestHelper h, Vec3 relative, SaucerFittings fittings) {
        Vec3 at = h.absoluteVec(relative);
        PrototypeSaucer saucer = ModEntities.PROTOTYPE_SAUCER.get().create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        saucer.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        saucer.outfit(fittings, PrototypeSaucer.MAX_CHARGE, BlockPos.containing(at));
        h.getLevel().addFreshEntity(saucer);
        return saucer;
    }

    private static List<ItemStack> roll(GameTestHelper h, ResourceKey<LootTable> table) {
        LootParams params = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))))
                .create(LootContextParamSets.CHEST);
        return h.getLevel().getServer().reloadableRegistries().getLootTable(table).getRandomItems(params);
    }

    private static StructureTemplate template(GameTestHelper h, String name) {
        Optional<StructureTemplate> template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(name));
        h.assertTrue(template.isPresent(), name + " template missing");
        return template.get();
    }

    private static int count(StructureTemplate template, Block block) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    /** A bare noise column: stone up to {@code top}, water over it to the surface if {@code flooded}, the crust far overhead. */
    private static NoiseColumn column(int top, boolean flooded) {
        BlockState[] states = new BlockState[HollowEarth.HEIGHT];
        for (int y = HollowEarth.MIN_Y; y <= HollowEarth.TOP_Y; y++) {
            BlockState state = Blocks.AIR.defaultBlockState();
            if (y <= top || y >= HollowEarth.CEILING_BASE) {
                state = Blocks.STONE.defaultBlockState();
            } else if (flooded && y < HollowEarth.SEA_LEVEL) {
                state = Blocks.WATER.defaultBlockState();
            }
            states[y - HollowEarth.MIN_Y] = state;
        }
        return new NoiseColumn(HollowEarth.MIN_Y, states);
    }

    private static double sq(double v) {
        return v * v;
    }
}
