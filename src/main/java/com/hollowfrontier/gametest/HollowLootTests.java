package com.hollowfrontier.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.hollow.HollowPatterns;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ConcaviaBlueprints;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.hollowfrontier.worldgen.structure.WildBlueprints;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * The Hollow loot (28): every room's table rolls, and its rare tier turns up in time; the explorers' maps point where
 * their names say; the prospector buys every relic at its price; the digs hold their suspicious blocks and the brush finds
 * the Hollow sherds in them; the sherds' patterns and the trim are registered, and the trim has its recipes.
 */
final class HollowLootTests {
    private HollowLootTests() {}

    private static net.minecraft.world.entity.Entity brusher;

    private static List<ItemStack> roll(GameTestHelper h, ResourceKey<LootTable> key, boolean archaeology) {
        LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(key);
        LootParams.Builder params = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, h.absoluteVec(Vec3.ZERO));
        if (archaeology) { // a brush in someone's hand
            if (brusher == null || brusher.isRemoved()) {
                brusher = h.spawn(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, new BlockPos(1, 1, 1));
            }
            params.withParameter(LootContextParams.THIS_ENTITY, brusher).withParameter(LootContextParams.TOOL, new ItemStack(Items.BRUSH));
        }
        return table.getRandomItems(params.create(archaeology ? LootContextParamSets.ARCHAEOLOGY : LootContextParamSets.CHEST));
    }

    /** Each Hollow room's table, rolled 150 times: never empty, and the items its tier promises all turn up. */
    static void hollowTablesRoll(GameTestHelper h) {
        record Expect(ResourceKey<LootTable> table, List<Item> rare) {}
        List<Expect> expect = List.of(
                new Expect(ModChestLoot.KINDLED_VAULT, List.of(ModItems.KINDLED_IDOL.get(), ModItems.CRYSTAL_TABLET.get())),
                new Expect(ModChestLoot.CATACOMB_VAULT, List.of(ModItems.KINDLED_IDOL.get(), ModItems.SUNSTONE_CIRCLET.get(), ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get())),
                new Expect(ModChestLoot.CRYSTAL_LIBRARY, List.of(ModItems.CRYSTAL_TABLET.get(), ModItems.STAR_CHART.get())),
                new Expect(ModChestLoot.SUN_TEMPLE, List.of(ModItems.STARSTEEL_MASK.get(), ModItems.SUNSTONE_SHARD.get())),
                new Expect(ModChestLoot.GRAY_COMPLEX_CORE, List.of(ModItems.GRAY_SPECIMEN_JAR.get(), Items.MAP)),
                new Expect(ModChestLoot.SYMMES_WRECK, List.of(ModItems.EXPEDITION_SEXTANT.get(), Items.MAP)),
                new Expect(ModChestLoot.TAR_PIT, List.of(ModItems.IVORY_CARVING.get())),
                new Expect(ModChestLoot.OBSERVATORY, List.of(ModItems.STAR_CHART.get())),
                new Expect(ModChestLoot.FOUNDRY, List.of(ModItems.STARSTEEL_MASK.get(), ModItems.STARSTEEL_INGOT.get())),
                new Expect(ModChestLoot.SKY_SHRINE, List.of(ModItems.STAR_CHART.get(), ModItems.KINDLED_IDOL.get())),
                new Expect(ModChestLoot.ZIGGURAT, List.of(ModItems.KINDLED_IDOL.get())));
        for (Expect e : expect) {
            Set<Item> seen = new HashSet<>();
            for (int i = 0; i < 150; i++) {
                List<ItemStack> loot = roll(h, e.table(), false);
                h.assertFalse(loot.isEmpty(), e.table().identifier() + " rolled nothing");
                loot.forEach(stack -> seen.add(stack.getItem()));
            }
            for (Item item : e.rare()) {
                h.assertTrue(seen.contains(item), e.table().identifier() + " never gave " + item + " in 150 rolls");
            }
        }
        h.succeed();
    }

    /** The maps: each table names the structure its map's for (read off the table itself). */
    static void mapsPointTheRightWay(GameTestHelper h) {
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for (Object[] pair : new Object[][] {{ModChestLoot.SYMMES_CAMP, "hollowfrontier:sun_temple"}, {ModChestLoot.GRAY_LAB_CONTROL, "hollowfrontier:gray_pens"},
                {ModChestLoot.KINDLED_SCRIPTORIUM, "hollowfrontier:crystal_ruins"}, {ModChestLoot.PHLOGISTON_MINE, "hollowfrontier:" + WildBlueprints.FOUNDRY}}) {
            @SuppressWarnings("unchecked")
            ResourceKey<LootTable> key = (ResourceKey<LootTable>) pair[0];
            LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(key);
            String json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().toString();
            h.assertTrue(json.contains("exploration_map") && json.contains("\"" + pair[1] + "\""), key.identifier() + "'s map doesn't lead to " + pair[1]);
        }
        h.succeed();
    }

    /** A town's prospector buys each relic (and each of the Grays' gadgets, 32c), one at a time, for its price in gold coins. */
    static void prospectorBuysRelics(GameTestHelper h) {
        Townsfolk prospector = ModEntities.TOWNSFOLK.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)), EntitySpawnReason.SPAWN_ITEM_USE);
        prospector.setFolk(Folk.PROSPECTOR);
        prospector.setNoAi(true);
        List<MerchantOffer> offers = prospector.getOffers();
        for (var relic : ModItems.RELICS) {
            h.assertTrue(offers.stream().anyMatch(o -> o.getCostA().is(relic.get()) && o.getResult().is(ModItems.GOLD_COIN.get())
                    && o.getResult().getCount() == relic.get().price()), "the prospector won't buy " + relic.getId());
        }
        for (var gadget : ModItems.GADGETS) {
            h.assertTrue(offers.stream().anyMatch(o -> o.getCostA().is(gadget.get()) && o.getResult().is(ModItems.GOLD_COIN.get())
                    && o.getResult().getCount() == gadget.get().price()), "the prospector won't buy " + gadget.getId());
        }
        prospector.discard();
        h.succeed();
    }

    /**
     * The digs: suspicious blocks with their tables in the crystal city, the Symmes dig, the tar pit and the ziggurat; the
     * brush's tables give the Hollow sherds; the sherds' patterns and the trim are registered; the trim has both recipes.
     */
    static void digsAndPatterns(GameTestHelper h) {
        for (Object[] site : new Object[][] {{ConcaviaBlueprints.CRYSTAL_RUINS, ModChestLoot.DIG_CRYSTAL_RUINS}, {WildBlueprints.TAR_PIT, ModChestLoot.DIG_TAR_PIT},
                {WildBlueprints.ZIGGURAT, ModChestLoot.DIG_ZIGGURAT}, {com.hollowfrontier.worldgen.structure.HollowBlueprints.SYMMES_CAMP, ModChestLoot.DIG_SYMMES},
                {com.hollowfrontier.worldgen.structure.SeaBlueprints.DROWNED_QUARTER, ModChestLoot.DIG_DROWNED_QUARTER}}) {
            StructureTemplate t = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id((String) site[0])).orElseThrow();
            @SuppressWarnings("unchecked")
            ResourceKey<LootTable> table = (ResourceKey<LootTable>) site[1];
            long digs = java.util.stream.Stream.of(Blocks.SUSPICIOUS_SAND, Blocks.SUSPICIOUS_GRAVEL)
                    .flatMap(b -> t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), b).stream())
                    .filter(info -> info.nbt() != null && info.nbt().getStringOr("LootTable", "").equals(table.identifier().toString())).count();
            h.assertTrue(digs >= 2, site[0] + " has " + digs + " places to brush");
        }
        Set<Item> found = new HashSet<>();
        for (ResourceKey<LootTable> dig : List.of(ModChestLoot.DIG_CRYSTAL_RUINS, ModChestLoot.DIG_SYMMES, ModChestLoot.DIG_TAR_PIT, ModChestLoot.DIG_DROWNED_QUARTER)) {
            for (int i = 0; i < 80; i++) {
                roll(h, dig, true).forEach(stack -> found.add(stack.getItem()));
            }
        }
        ModItems.SHERDS.forEach(sherd -> h.assertTrue(found.contains(sherd.get()), "no brush turns up " + sherd.getId()));
        var access = h.getLevel().registryAccess();
        HollowPatterns.POTS.forEach(key -> h.assertTrue(access.lookupOrThrow(Registries.DECORATED_POT_PATTERN).get(key).isPresent(), key + " isn't registered"));
        h.assertTrue(access.lookupOrThrow(Registries.TRIM_PATTERN).get(HollowPatterns.CONCAVIAN).isPresent(), "the trim pattern isn't registered");
        var recipes = h.getLevel().getServer().getRecipeManager();
        for (String id : List.of("concavian_armor_trim_smithing_template", "concavian_armor_trim_smithing_template_smithing_trim")) {
            h.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, HollowFrontier.id(id))).isPresent(), "no recipe " + id);
        }
        if (brusher != null) {
            brusher.discard();
        }
        h.succeed();
    }
}
