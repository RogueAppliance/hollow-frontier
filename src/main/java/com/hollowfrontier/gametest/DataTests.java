package com.hollowfrontier.gametest;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.brass.BrassAlloyingCondition;
import com.hollowfrontier.content.frontier.brass.BrassConfig;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;
import com.hollowfrontier.worldgen.ModSpawns;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.hollowfrontier.worldgen.structure.TownBlueprints;
import com.hollowfrontier.worldgen.structure.TownLayout;
import com.hollowfrontier.worldgen.structure.TownPalettes;
import com.hollowfrontier.worldgen.structure.TownStyle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Datagen output actually loads in a running server: recipes, loot, tags, worldgen, structures.
final class DataTests {
    private DataTests() {}

    static void recipesLoad(GameTestHelper h) {
        for (String id : List.of("brass_ingot_alloying", "brass_gear", "revolver", "plains_rifle", "revolver_cartridge", "percussion_cap",
                "cowboy_hat", "lasso", "brass_horseshoes", "copper_coin", "gold_coin", "dynamite", "snakeskin_boots", "saloon_piano", "gold_coin_from_copper_coins", "copper_coin_from_gold_coin", "bounty_board", "bar_stool", "saloon_doors", "boiler",
                "copper_pipe", "steam_drill", "crushing/iron_dust_from_raw_iron", "iron_ingot_from_smelting_iron_dust")) {
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, HollowFrontier.id(id));
            h.assertTrue(h.getLevel().recipeAccess().byKey(key).isPresent(), "missing recipe " + id);
        }
        h.succeed();
    }

    // Crafting-table brass steps aside once another mod puts its brass in c:ingots/brass (unless the config says otherwise).
    static void brassStepsAside(GameTestHelper h) {
        var ours = List.<Holder<Item>>of(ModItems.BRASS_INGOT);
        var shared = List.<Holder<Item>>of(ModItems.BRASS_INGOT, Items.GOLD_INGOT.builtInRegistryHolder()); // stands in for create:brass_ingot
        h.assertTrue(BrassAlloyingCondition.allowed(BrassConfig.Alloying.AUTO, ours), "auto: alloying off with only our brass");
        h.assertTrue(BrassAlloyingCondition.allowed(BrassConfig.Alloying.AUTO, List.of()), "auto: alloying off with no brass tag");
        h.assertFalse(BrassAlloyingCondition.allowed(BrassConfig.Alloying.AUTO, shared), "auto: alloying on beside another mod's brass");
        h.assertTrue(BrassAlloyingCondition.allowed(BrassConfig.Alloying.ALWAYS, shared), "always: alloying off");
        h.assertFalse(BrassAlloyingCondition.allowed(BrassConfig.Alloying.NEVER, ours), "never: alloying on");
        h.succeed();
    }

    static void lootTablesLoad(GameTestHelper h) {
        var registries = h.getLevel().getServer().reloadableRegistries();
        for (ResourceKey<LootTable> key : List.of(ModChestLoot.SALOON, ModChestLoot.OUTLAW_CAMP, ModChestLoot.SHERIFF_OFFICE, ModChestLoot.BANK,
                ModChestLoot.BLACKSMITH, ModChestLoot.LIVERY,
                lootKey("entities/outlaw"), lootKey("entities/outlaw_boss"), lootKey("blocks/brass_block"), lootKey("blocks/boiler"))) {
            h.assertTrue(registries.getLootTable(key) != LootTable.EMPTY, "missing loot table " + key.identifier());
        }
        h.succeed();
    }

    /** On Lootr's structure blacklist a town's chests won't open at all: its hook cancels the unpack, then the tag refuses the conversion. */
    static void townsStayOffLootrBlacklist(GameTestHelper h) {
        var structures = h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var blacklist = net.minecraft.tags.TagKey.create(Registries.STRUCTURE, net.minecraft.resources.Identifier.fromNamespaceAndPath("lootr", "blacklist"));
        for (ResourceKey<Structure> town : ModStructures.TOWNS) {
            h.assertFalse(structures.getOrThrow(town).is(blacklist), town.identifier() + " on Lootr's blacklist");
        }
        h.succeed();
    }

    static void tagsApply(GameTestHelper h) {
        h.assertTrue(new ItemStack(ModItems.BRASS_INGOT.get()).is(ModTags.Items.INGOTS_BRASS), "brass ingot not in c:ingots/brass");
        h.assertTrue(new ItemStack(ModItems.STARSTEEL_ORE.get()).is(Tags.BlockItems.ORES.item()), "starsteel ore not in c:ores");
        h.assertTrue(new ItemStack(ModItems.IRON_DUST.get()).is(ModTags.Items.DUSTS), "iron dust not in c:dusts");
        h.assertTrue(new ItemStack(ModItems.COWBOY_HAT.get()).is(ItemTags.HEAD_ARMOR), "cowboy hat not head armor");
        h.assertTrue(ModBlocks.SALOON_DOORS.get().defaultBlockState().is(net.minecraft.tags.BlockTags.WOODEN_DOORS), "saloon doors not wooden doors");
        h.succeed();
    }

    static void worldgenRegistered(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        h.assertTrue(access.lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).get(ModSpawns.ADD_OUTLAW_SPAWNS).isPresent(), "outlaw spawns missing");
        h.assertTrue(access.lookupOrThrow(Registries.DAMAGE_TYPE).get(ModDamageTypes.BULLET).isPresent(), "bullet damage type missing");
        for (ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> s : java.util.stream.Stream.concat(Stream.of(ModStructures.SALOON, ModStructures.OUTLAW_CAMP), ModStructures.TOWNS.stream()).toList()) {
            h.assertTrue(access.lookupOrThrow(Registries.STRUCTURE).get(s).isPresent(), "structure missing " + s.identifier());
        }
        h.succeed();
    }

    static void structureTemplatesLoad(GameTestHelper h) {
        Map<String, Vec3i> expected = Map.of(
                "saloon", new Vec3i(13, 10, 13),
                "outlaw_camp", new Vec3i(15, 5, 15));
        expected.forEach((name, size) -> {
            StructureTemplate template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(name)).orElse(null);
            h.assertTrue(template != null, "template missing " + name);
            h.assertTrue(template.getSize().equals(size), name + " size " + template.getSize() + " != " + size);
        });
        h.succeed();
    }

    /**
     * Every town piece loads in every style: squares have their exits, lots and centre mark; streets and lanes all their
     * lots; every building variant exactly one street-facing connector.
     */
    static void townPiecesConnect(GameTestHelper h) {
        var templates = h.getLevel().getStructureTemplateManager();
        // square: 4 exits + 8 lots, 3 exits + 6 lots + a grand lot, 2 exits + 6 lots + a grand lot; each + the centre mark
        Map<String, Integer> layout = Map.of("street_long", 8, "street_short", 6, "lane", 6, "farm_track", 2,
                "square_cross_bandstand", 13, "square_cross_well", 13, "square_tee_bandstand", 11, "square_pair_well", 10,
                "rail_spur", 1, "rail_boxcar", 1);
        for (TownStyle style : TownStyle.values()) {
            layout.forEach((name, connectors) -> {
                StructureTemplate piece = templates.get(HollowFrontier.id(style.template(name))).orElse(null);
                h.assertTrue(piece != null, "missing " + style.template(name));
                int count = piece.getJigsaws(BlockPos.ZERO, Rotation.NONE).size();
                h.assertTrue(count == connectors, style.template(name) + " has " + count + " connectors, expected " + connectors);
            });
            for (var building : TownBlueprints.buildings()) {
                String base = building.name().substring("town/".length());
                for (int i = 0; i < TownPalettes.count(base); i++) {
                    String name = style.template(TownPalettes.variant(base, i));
                    StructureTemplate piece = templates.get(HollowFrontier.id(name)).orElse(null);
                    h.assertTrue(piece != null, "missing " + name);
                    // (the depot also has the railroad's two, 24a)
                    List<StructureTemplate.JigsawBlockInfo> jigsaws = piece.getJigsaws(BlockPos.ZERO, Rotation.NONE).stream()
                            .filter(j -> !String.valueOf(j.name()).equals(TownLayout.RAIL)).toList();
                    h.assertTrue(jigsaws.size() == 1 && String.valueOf(jigsaws.get(0).name()).equals(TownBlueprints.FRONT)
                            && jigsaws.get(0).pos().getZ() == 0, name + " needs one front connector on its z 0 row");
                    if (base.equals("depot")) {
                        h.assertTrue(piece.getJigsaws(BlockPos.ZERO, Rotation.NONE).size() == 3, name + " needs the railroad's two connectors");
                    }
                }
            }
        }
        h.succeed();
    }

    /**
     * Lays out towns of each style on paper (the jigsaw pieces the real terrain would give, nothing placed) and checks what
     * every town must have: grown from a square, two or more streets, exactly one hotel (the town jukebox) and one
     * saloon, the law or a bank, a wheat field (for crop circles), and nothing past the jigsaw's reach. Towns only stand on
     * dry, near-level ground (23), so spots are tried until a dozen of each style have stood; each is all on one level.
     */
    static void townLayoutRules(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var generator = level.getChunkSource().getGenerator();
        var randomState = level.getChunkSource().randomState();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        java.util.regex.Pattern template = java.util.regex.Pattern.compile("hollowfrontier:town/[a-z_/0-9]+");
        int pieces = 0, towns = 0, oldPlaces = 0;
        for (TownStyle style : TownStyle.values()) {
            var structure = structures.getOrThrow(style.structure()).value();
            int stood = 0;
            for (int i = 0; i < 80 && stood < 12; i++) {
                var chunk = new net.minecraft.world.level.ChunkPos(3000 + 41 * i, -5000 + 67 * i + 900 * style.ordinal());
                var context = new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(level.registryAccess(), generator,
                        generator.getBiomeSource(), randomState.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED), randomState, level.getStructureTemplateManager(), level.getSeed(), chunk, level, biome -> true);
                var stub = structure.findValidGenerationPoint(context);
                if (stub.isEmpty()) {
                    continue; // not level or dry enough here
                }
                stood++;
                var placed = stub.get().getPiecesBuilder().build().pieces();
                int ground = placed.getFirst().getBoundingBox().minY();
                h.assertTrue(placed.stream().allMatch(p -> p.getBoundingBox().minY() == ground), style + " town at " + chunk + " isn't all on one level");
                List<String> names = placed.stream().map(p -> p instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pool
                        ? template.matcher(pool.getElement().toString()).results().map(java.util.regex.MatchResult::group).findFirst().orElse("?") : "?").toList();
                String where = style + " town at " + chunk + ": " + names;
                h.assertTrue(names.getFirst().contains("/square_"), where + " doesn't start from a square");
                h.assertTrue(names.stream().filter(n -> n.contains("/street_")).count() >= 2, where + " has fewer than two streets");
                h.assertTrue(names.stream().filter(n -> n.contains("/hotel")).count() == 1, where + " needs exactly one hotel");
                h.assertTrue(names.stream().filter(n -> n.contains("/saloon")).count() == 1, where + " needs exactly one saloon");
                long law = names.stream().filter(n -> n.contains("/sheriff_office")).count();
                h.assertTrue(law <= 1 && (law == 1 || names.stream().anyMatch(n -> n.contains("/bank"))), where + " has no law and no bank");
                h.assertTrue(names.stream().anyMatch(n -> n.contains("/wheat_field")), where + " has no wheat field for crop circles");
                var bounds = net.minecraft.world.level.levelgen.structure.StructurePiece.createBoundingBox(placed.stream());
                h.assertTrue(bounds.getXSpan() <= 2 * 116 + 1 && bounds.getZSpan() <= 2 * 116 + 1, where + " spans " + bounds);
                pieces += placed.size();
                towns++;
                oldPlaces += names.stream().anyMatch(n -> n.contains("/old_place")) ? 1 : 0; // 47b
            }
            h.assertTrue(stood >= 12, "only " + stood + " " + style + " towns found level ground in 80 tries");
        }
        HollowFrontier.LOGGER.info("[DataTests] {} towns laid out, {} pieces on average, {} with an Old Place standing", towns, pieces / towns, oldPlaces);
        h.assertTrue(oldPlaces * 4 >= towns, "only " + oldPlaces + " of " + towns + " towns have an Old Place standing (47b)");
        h.succeed();
    }

    /** Bars, panes and fences in generated templates come out joined up, not as lone posts. */
    static void templatesConnectBlocks(GameTestHelper h) {
        var templates = h.getLevel().getStructureTemplateManager();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        Map<String, net.minecraft.world.level.block.Block> checks = Map.of(
                "town/sheriff_office", Blocks.IRON_BARS, "town/general_store", Blocks.GLASS_PANE, "town/street_long", Blocks.SPRUCE_FENCE);
        checks.forEach((name, block) -> {
            StructureTemplate template = templates.get(HollowFrontier.id(name)).orElseThrow();
            boolean joined = template.filterBlocks(BlockPos.ZERO, settings, block).stream().anyMatch(info -> {
                BlockState s = info.state();
                return s.getValue(CrossCollisionBlock.EAST) || s.getValue(CrossCollisionBlock.WEST)
                        || s.getValue(CrossCollisionBlock.NORTH) || s.getValue(CrossCollisionBlock.SOUTH);
            });
            h.assertTrue(joined, "no connected " + block + " in " + name);
        });
        h.succeed();
    }

    /** The blacksmith's forge is open-fronted: nothing walls off its front rows (once a plank wall stood one row in). It stands one in on its lot (TownLots). */
    static void blacksmithFrontIsOpen(GameTestHelper h) {
        StructureTemplate forge = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id("town/blacksmith")).orElseThrow();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        for (net.minecraft.world.level.block.Block block : List.of(Blocks.SPRUCE_PLANKS, Blocks.COBBLESTONE)) {
            for (var info : forge.filterBlocks(BlockPos.ZERO, settings, block)) {
                BlockPos p = info.pos();
                h.assertFalse(p.getX() >= 2 && p.getX() <= 8 && p.getY() >= 1 && p.getY() <= 4 && p.getZ() >= 2 && p.getZ() <= 3,
                        block + " blocks the forge front at " + p);
            }
        }
        h.succeed();
    }

    /** Places the saloon high above the test (out of other tests' way) and checks blocks, chest loot and the bartender. */
    static void saloonPlaces(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        StructureTemplate saloon = level.getStructureTemplateManager().get(HollowFrontier.id("saloon")).orElseThrow();
        BlockPos origin = h.absolutePos(new BlockPos(0, 80, 0));
        saloon.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        h.assertTrue(level.getBlockState(origin.offset(2, 1, 9)).is(ModBlocks.BAR_COUNTER.get()), "bar counter not placed");
        h.assertTrue(level.getEntitiesOfClass(com.hollowfrontier.content.frontier.bounty.BountyBoardEntity.class, new AABB(origin.offset(11, 1, 4)).inflate(2))
                .stream().anyMatch(board -> board.survives() && board.columns() == 2), "bounty board not hung");
        h.assertTrue(level.getBlockState(origin.offset(5, 1, 3)).is(ModBlocks.SALOON_DOORS.get()), "saloon doors not placed");
        h.assertTrue(level.getBlockState(origin.offset(11, 1, 8)).is(ModBlocks.SALOON_PIANO.get()), "saloon piano not placed");
        h.assertTrue(level.getBlockEntity(origin.offset(10, 1, 11)) instanceof ChestBlockEntity chest
                && ModChestLoot.SALOON.equals(chest.getLootTable()), "saloon chest has no loot table");
        h.assertTrue(!level.getEntitiesOfClass(Barmaid.class, new AABB(origin).inflate(16)).isEmpty(), "barmaid not spawned");
        h.succeed();
    }

    private static ResourceKey<LootTable> lootKey(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, HollowFrontier.id(path));
    }
}
