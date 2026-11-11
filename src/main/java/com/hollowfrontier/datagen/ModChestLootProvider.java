package com.hollowfrontier.datagen;

import java.util.List;
import java.util.Optional;

import com.hollowfrontier.content.frontier.weather.DustDevils;
import com.hollowfrontier.content.ufo.FieldNotes;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;
import com.hollowfrontier.worldgen.structure.ModChestLoot;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.ListOperation;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetBookCoverFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.loot.functions.SetWrittenBookPagesFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public class ModChestLootProvider implements LootTableSubProvider {
    private final LootTableSubProvider.Context context;

    public ModChestLootProvider(LootTableSubProvider.Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        // 43b: what a Plesiosaur brings up from the bed of the Still Sea: mostly what any seabed has, now and then something
        // the Kindled left (a sherd, or a relic the prospector will buy)
        LootPool.Builder dive = pool(1, 2)
                .add(item(Items.KELP, 1, 3, 30))
                .add(item(Items.SEAGRASS, 1, 2, 12))
                .add(item(Items.CLAY_BALL, 1, 4, 16))
                .add(item(Items.BONE, 1, 2, 10))
                .add(item(Items.NAUTILUS_SHELL, 1, 1, 8))
                .add(item(Items.PRISMARINE_SHARD, 1, 2, 6))
                .add(item(ModItems.LAMP_GEL.get(), 1, 2, 8));
        ModItems.SHERDS.forEach(sherd -> dive.add(item(sherd.get(), 1, 1, 1)));
        ModItems.RELICS.forEach(relic -> dive.add(item(relic.get(), 1, 1, 1)));
        context.accept(com.hollowfrontier.content.hollow.sea.SeaLife.DIVE_LOOT, LootTable.lootTable().withPool(dive));
        // 29: what a dust devil drops where it dies down: brush and feathers it swept up, now and then something of somebody's
        context.accept(DustDevils.LOOT, LootTable.lootTable().withPool(pool(1, 2)
                .add(EmptyLootItem.emptyItem().setWeight(30))
                .add(item(Items.STICK, 1, 3, 20))
                .add(item(Items.DEAD_BUSH, 1, 1, 15))
                .add(item(Items.FEATHER, 1, 2, 10))
                .add(item(ModItems.COPPER_COIN.get(), 1, 2, 12))
                .add(item(Items.RABBIT_HIDE, 1, 1, 5))
                .add(item(ModItems.SNAKESKIN.get(), 1, 1, 4))
                .add(item(ModItems.FRONTIER_GAZETTE.get(), 1, 1, 3))
                .add(item(ModItems.GOLD_COIN.get(), 1, 1, 2))
                .add(item(ModItems.BANDANA.get(), 1, 1, 2))));
        accept(ModChestLoot.SALOON, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(ModItems.COPPER_COIN.get(), 2, 6, 20))
                .add(item(ModItems.REVOLVER_CARTRIDGE.get(), 4, 10, 15))
                .add(item(Items.BREAD, 1, 3, 15))
                .add(item(Items.GOLD_NUGGET, 2, 6, 10))
                .add(item(ModItems.RYE_WHISKEY.get(), 1, 2, 12))
                .add(item(ModItems.FRONTIER_BEER.get(), 1, 3, 12))
                .add(item(ModItems.SARSAPARILLA.get(), 1, 2, 8))
                .add(item(ModItems.MOONSHINE.get(), 1, 1, 4))
                .add(item(Items.GLASS_BOTTLE, 1, 3, 10))
                .add(item(ModItems.GOLD_COIN.get(), 1, 1, 3))));
        accept(ModChestLoot.GENERAL_STORE, LootTable.lootTable().withPool(pool(4, 7)
                .add(item(Items.COPPER_INGOT, 2, 5, 15))
                .add(item(Items.LEATHER, 2, 5, 15))
                .add(item(Items.STRING, 2, 6, 15))
                .add(item(ModItems.BRASS_NUGGET.get(), 3, 9, 15))
                .add(item(ModItems.BUCKSHOT_CARTRIDGE.get(), 2, 6, 10))
                .add(item(ModItems.PERCUSSION_CAP.get(), 8, 16, 12))
                .add(item(Items.LEAD, 1, 2, 8))
                .add(item(ModItems.IRON_HORSESHOES.get(), 1, 1, 3))
                .add(item(ModItems.FRONTIER_GAZETTE.get(), 1, 1, 6))));
        accept(ModChestLoot.SHERIFF_OFFICE, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(ModItems.REVOLVER_CARTRIDGE.get(), 6, 12, 20))
                .add(item(ModItems.RIFLE_CARTRIDGE.get(), 3, 8, 15))
                .add(item(ModItems.PERCUSSION_CAP.get(), 8, 16, 15))
                .add(item(Items.IRON_INGOT, 1, 3, 10))
                .add(item(ModItems.LASSO.get(), 1, 2, 10))
                .add(item(ModItems.COPPER_COIN.get(), 2, 5, 10))
                .add(item(ModItems.REVOLVER.get(), 1, 1, 2))));
        // A home's chest (23): the pantry, the coal, clothes and the odd keepsake, and now and then the savings under the linen
        accept(ModChestLoot.TOWN_HOUSE, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.BREAD, 1, 4, 20))
                .add(item(Items.WHEAT, 2, 6, 12))
                .add(item(Items.COAL, 1, 4, 15))
                .add(item(Items.APPLE, 1, 3, 10))
                .add(item(Items.BAKED_POTATO, 1, 3, 10))
                .add(item(Items.COOKED_BEEF, 1, 2, 8))
                .add(item(Items.PUMPKIN_PIE, 1, 1, 5))
                .add(item(Items.CANDLE, 1, 3, 8))
                .add(item(Items.STRING, 1, 4, 8))
                .add(item(Items.LEATHER, 1, 2, 6))
                .add(item(ModItems.BANDANA.get(), 1, 1, 4))
                .add(item(ModItems.COWBOY_COFFEE.get(), 1, 2, 5))
                .add(item(ModItems.FRONTIER_GAZETTE.get(), 1, 1, 4))
                .add(item(ModItems.COPPER_COIN.get(), 1, 4, 10)))
                .withPool(pool(1, 1)
                        .add(EmptyLootItem.emptyItem().setWeight(14))
                        .add(item(ModItems.GOLD_COIN.get(), 1, 1, 3))
                        .add(item(Items.GOLD_NUGGET, 2, 5, 2))
                        .add(item(Items.CLOCK, 1, 1, 1))));
        // A hotel room: what the last guest left behind
        accept(ModChestLoot.HOTEL_ROOM, LootTable.lootTable().withPool(pool(2, 5)
                .add(item(ModItems.COPPER_COIN.get(), 1, 5, 18))
                .add(item(Items.PAPER, 1, 3, 10))
                .add(item(ModItems.FRONTIER_GAZETTE.get(), 1, 1, 8))
                .add(item(ModItems.RYE_WHISKEY.get(), 1, 1, 8))
                .add(item(ModItems.COWBOY_COFFEE.get(), 1, 2, 8))
                .add(item(Items.CANDLE, 1, 2, 8))
                .add(item(ModItems.REVOLVER_CARTRIDGE.get(), 2, 6, 8))
                .add(item(ModItems.BANDANA.get(), 1, 1, 5))
                .add(item(Items.BOOK, 1, 1, 5))
                .add(item(ModItems.WANTED_POSTER.get(), 1, 1, 3))
                .add(item(ModItems.GOLD_COIN.get(), 1, 1, 3))
                .add(item(ModItems.COWBOY_HAT.get(), 1, 1, 2))));
        // The undertaker's: candles and flowers for the burying, nails and planks for the boxes, and what the departed carried
        accept(ModChestLoot.UNDERTAKER, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.CANDLE, 2, 5, 15))
                .add(item(Items.IRON_NUGGET, 3, 9, 12))
                .add(item(Items.SPRUCE_PLANKS, 4, 12, 12))
                .add(item(Items.POPPY, 1, 4, 10))
                .add(item(Items.BONE, 1, 4, 10))
                .add(item(Items.GOLD_NUGGET, 1, 4, 8))     // gold teeth
                .add(item(ModItems.COPPER_COIN.get(), 2, 6, 10))
                .add(item(ModItems.REVOLVER_CARTRIDGE.get(), 2, 6, 6))
                .add(item(ModItems.WANTED_POSTER.get(), 1, 1, 4))
                .add(item(ModItems.COWBOY_BOOTS.get(), 1, 1, 2))
                .add(item(ModItems.REVOLVER.get(), 1, 1, 1))));
        // The gunsmith's: powder, caps, cartridges of every kind, brass and iron for the work, now and then a gun book
        accept(ModChestLoot.GUNSMITH, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.GUNPOWDER, 2, 6, 18))
                .add(item(ModItems.PERCUSSION_CAP.get(), 8, 16, 16))
                .add(item(ModItems.REVOLVER_CARTRIDGE.get(), 4, 10, 14))
                .add(item(ModItems.RIFLE_CARTRIDGE.get(), 3, 8, 12))
                .add(item(ModItems.BUCKSHOT_CARTRIDGE.get(), 3, 8, 12))
                .add(item(ModItems.BRASS_INGOT.get(), 1, 3, 10))
                .add(item(ModItems.BRASS_NUGGET.get(), 3, 9, 10))
                .add(item(Items.IRON_INGOT, 1, 3, 8))
                .add(item(ModItems.BRASS_GEAR.get(), 1, 2, 6))
                .add(gunBook(5))
                .add(item(ModItems.REVOLVER.get(), 1, 1, 2).apply(gunEnchantment().when(LootItemRandomChanceCondition.randomChance(0.5F))))));
        // The doctor's: a healing draught or two, the remedies, bandages and something for the nerves
        accept(ModChestLoot.DOCTOR, LootTable.lootTable()
                .withPool(pool(1, 2).add(LootItem.lootTableItem(Items.POTION).apply(SetPotionFunction.setPotion(Potions.HEALING))))
                .withPool(pool(3, 5)
                        .add(item(Items.GLASS_BOTTLE, 1, 4, 15))
                        .add(item(Items.GLISTERING_MELON_SLICE, 1, 2, 10))
                        .add(item(Items.HONEY_BOTTLE, 1, 2, 10))
                        .add(item(Items.STRING, 2, 6, 12))    // for stitches
                        .add(item(Items.PAPER, 1, 4, 10))
                        .add(item(Items.SUGAR, 1, 4, 8))
                        .add(item(Items.MILK_BUCKET, 1, 1, 5))
                        .add(item(ModItems.SNAKE_OIL.get(), 1, 1, 8))
                        .add(item(ModItems.RYE_WHISKEY.get(), 1, 1, 6))
                        .add(item(Items.GOLDEN_CARROT, 1, 2, 4))
                        .add(item(Items.GOLDEN_APPLE, 1, 1, 1))));
        // The post office: letters, newsprint and stationery, a parcel's coins, and a map someone never collected
        accept(ModChestLoot.POST_OFFICE, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.PAPER, 3, 9, 20))
                .add(item(ModItems.FRONTIER_GAZETTE.get(), 1, 2, 14))
                .add(item(Items.WRITABLE_BOOK, 1, 1, 8))
                .add(item(Items.INK_SAC, 1, 3, 10))
                .add(item(Items.FEATHER, 1, 4, 10))
                .add(item(Items.STRING, 1, 3, 8))
                .add(item(ModItems.COPPER_COIN.get(), 2, 8, 12))
                .add(item(ModItems.GOLD_COIN.get(), 1, 1, 3))
                .add(item(ModItems.WANTED_POSTER.get(), 1, 1, 4))
                .add(item(Items.MAP, 1, 1, 4))
                .add(item(Items.COMPASS, 1, 1, 2))));
        // The schoolhouse: books and slates, an apple for the teacher, chalk (bone meal) and the dunce's spare marbles
        accept(ModChestLoot.SCHOOLHOUSE, LootTable.lootTable().withPool(pool(2, 5)
                .add(item(Items.BOOK, 1, 3, 15))
                .add(item(Items.PAPER, 2, 6, 15))
                .add(item(Items.APPLE, 1, 3, 15))
                .add(item(Items.BONE_MEAL, 1, 4, 10))
                .add(item(Items.WRITABLE_BOOK, 1, 1, 6))
                .add(item(Items.INK_SAC, 1, 2, 8))
                .add(item(Items.COMPASS, 1, 1, 3))
                .add(item(Items.CLOCK, 1, 1, 2))
                .add(item(ModItems.COPPER_COIN.get(), 1, 3, 6))));
        // Freight off the railroad: crates for the stores and the mines
        // 24b. The bathhouse: towels, soap (honey), the barber's things, and what the customers left in their trousers
        accept(ModChestLoot.BATHHOUSE, LootTable.lootTable().withPool(pool(2, 5)
                .add(item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1, 3, 15))
                .add(item(Items.HONEY_BOTTLE, 1, 2, 10))
                .add(item(Items.SHEARS, 1, 1, 6))
                .add(item(Items.LEATHER, 1, 2, 8))
                .add(item(Items.STRING, 1, 3, 8))
                .add(item(ModItems.COWBOY_COFFEE.get(), 1, 2, 6))
                .add(item(ModItems.COPPER_COIN.get(), 2, 6, 14))
                .add(item(ModItems.BANDANA.get(), 1, 1, 4))
                .add(item(ModItems.GOLD_COIN.get(), 1, 1, 2))));
        // The Gazette office: newsprint, ink and type, back numbers, and the odd tip-off (a wanted poster)
        accept(ModChestLoot.GAZETTE_OFFICE, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.PAPER, 4, 12, 20))
                .add(item(Items.INK_SAC, 2, 5, 14))
                .add(item(Items.DYE.black(), 1, 3, 8))
                .add(item(ModItems.FRONTIER_GAZETTE.get(), 1, 3, 14))
                .add(item(Items.FEATHER, 1, 4, 8))
                .add(item(Items.BOOK, 1, 2, 6))
                .add(item(Items.WRITABLE_BOOK, 1, 1, 5))
                .add(item(Items.IRON_NUGGET, 2, 6, 6))    // spare type
                .add(item(ModItems.WANTED_POSTER.get(), 1, 1, 5))
                .add(item(ModItems.COPPER_COIN.get(), 2, 6, 10))));
        // The ranch house: rations, rope and tack, beef, a branding iron (an iron ingot) and the hands' wages
        accept(ModChestLoot.RANCH, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.COOKED_BEEF, 2, 5, 16))
                .add(item(Items.LEATHER, 2, 5, 14))
                .add(item(Items.LEAD, 1, 2, 12))
                .add(item(Items.WHEAT, 3, 9, 12))
                .add(item(ModItems.LASSO.get(), 1, 1, 6))
                .add(item(ModItems.IRON_HORSESHOES.get(), 1, 1, 5))
                .add(item(Items.IRON_INGOT, 1, 2, 5))
                .add(item(ModItems.COWBOY_COFFEE.get(), 1, 3, 8))
                .add(item(ModItems.COPPER_COIN.get(), 2, 7, 12))
                .add(item(Items.SADDLE, 1, 1, 3))
                .add(item(ModItems.GOLD_COIN.get(), 1, 1, 2))));
        accept(ModChestLoot.FREIGHT, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.COAL, 4, 12, 16))
                .add(item(Items.RAIL, 4, 12, 10))
                .add(item(Items.IRON_INGOT, 1, 4, 10))
                .add(item(ModItems.BRASS_INGOT.get(), 1, 3, 8))
                .add(item(Items.LEATHER, 2, 5, 10))
                .add(item(Items.WHEAT, 4, 10, 10))
                .add(item(ModItems.FRONTIER_BEER.get(), 1, 3, 8))
                .add(item(ModItems.DYNAMITE.get(), 1, 3, 6))
                .add(item(ModItems.BRASS_GEAR.get(), 1, 2, 5))
                .add(item(ModItems.STEAM_CANISTER.get(), 1, 1, 3))
                .add(item(ModItems.COPPER_COIN.get(), 2, 6, 8))));
        accept(ModChestLoot.BANK, LootTable.lootTable().withPool(pool(4, 7)
                .add(item(ModItems.COPPER_COIN.get(), 4, 12, 25))
                .add(item(ModItems.GOLD_COIN.get(), 1, 3, 12))
                .add(item(Items.GOLD_INGOT, 1, 3, 10))
                .add(item(Items.GOLD_NUGGET, 4, 12, 15))
                .add(item(Items.EMERALD, 1, 4, 10))
                .add(item(ModItems.WANTED_POSTER.get(), 1, 1, 3))
                .add(item(Items.PAPER, 2, 6, 10))));
        accept(ModChestLoot.BLACKSMITH, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.IRON_INGOT, 2, 6, 20))
                .add(item(ModItems.BRASS_INGOT.get(), 1, 4, 15))
                .add(item(ModItems.IRON_HORSESHOES.get(), 1, 1, 8))
                .add(item(ModItems.BRASS_HORSESHOES.get(), 1, 1, 3))
                .add(item(Items.COAL, 3, 8, 15))
                .add(item(Items.IRON_NUGGET, 4, 12, 12))
                .add(item(ModItems.PERCUSSION_CAP.get(), 8, 16, 8))
                .add(item(ModItems.DYNAMITE.get(), 1, 3, 6))
                .add(gunBook(4))));
        accept(ModChestLoot.LIVERY, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.SADDLE, 1, 1, 12))
                .add(item(Items.LEAD, 1, 2, 15))
                .add(item(Items.WHEAT, 4, 12, 20))
                .add(item(Items.APPLE, 2, 5, 12))
                .add(item(Items.GOLDEN_CARROT, 1, 2, 4))
                .add(item(ModItems.IRON_HORSESHOES.get(), 1, 1, 6))
                .add(item(ModItems.SADDLEBAGS.get(), 1, 1, 4))));
        accept(ModChestLoot.OUTLAW_CAMP, LootTable.lootTable().withPool(pool(4, 8)
                .add(item(ModItems.COPPER_COIN.get(), 3, 9, 20))
                .add(item(Items.GUNPOWDER, 2, 6, 15))
                .add(item(ModItems.PERCUSSION_CAP.get(), 6, 12, 15))
                .add(item(ModItems.REVOLVER_CARTRIDGE.get(), 4, 10, 15))
                .add(item(ModItems.RIFLE_CARTRIDGE.get(), 2, 6, 10))
                .add(item(ModItems.GOLD_COIN.get(), 1, 2, 5))
                .add(item(Items.GOLD_INGOT, 1, 2, 5))
                .add(item(ModItems.DYNAMITE.get(), 1, 4, 10))
                // A gang's spare revolver may have had some work done on it
                .add(item(ModItems.REVOLVER.get(), 1, 1, 3).apply(gunEnchantment().when(LootItemRandomChanceCondition.randomChance(0.5F))))
                .add(gunBook(5))));
        // The frozen expedition's camp by a Polar Gateway: rations, fuel, kit, and amethyst for the resonator (a hint).
        accept(ModChestLoot.POLAR_GATEWAY, LootTable.lootTable().withPool(pool(4, 7)
                .add(item(Items.COOKED_BEEF, 2, 5, 15))
                .add(item(Items.COAL, 3, 8, 15))
                .add(item(ModItems.COPPER_COIN.get(), 3, 8, 12))
                .add(item(ModItems.PERCUSSION_CAP.get(), 6, 12, 10))
                .add(item(ModItems.RIFLE_CARTRIDGE.get(), 2, 6, 10))
                .add(item(Items.AMETHYST_SHARD, 1, 3, 12))
                .add(item(ModItems.BRASS_GEAR.get(), 1, 2, 8))
                .add(item(ModItems.GOLD_COIN.get(), 1, 2, 5))
                .add(item(ModItems.STEAM_CANISTER.get(), 1, 1, 6))
                .add(item(ModItems.POLAR_COMPASS.get(), 1, 1, 2))));
        // The Hollow Earth's lore sites (9b3). The vault behind the Kindled door: the builders' blue stone, quartz and crystal.
        accept(ModChestLoot.KINDLED_VAULT, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.LAPIS_LAZULI, 3, 8, 15))
                .add(item(Items.QUARTZ, 4, 10, 15))
                .add(item(Items.AMETHYST_SHARD, 2, 5, 12))
                .add(item(Items.GOLD_INGOT, 1, 3, 8))
                .add(item(ModItems.GOLD_COIN.get(), 1, 3, 6))
                .add(item(Items.SOUL_LANTERN, 1, 2, 6))
                .add(item(Items.COMPASS, 1, 1, 5))
                .add(item(Items.LODESTONE, 1, 1, 2))
                .add(item(Items.ECHO_SHARD, 1, 1, 2))));
        // The Kindled's stores in the crystal ruins' sanctum (12a): phlogiston, their stone and metal, and the odd thing kept
        accept(ModChestLoot.CRYSTAL_RUINS, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.PHLOGISTON_SHARD.get(), 2, 5, 1)))
                .withPool(pool(3, 6)
                        .add(item(ModItems.CONCAVIAN_CRYSTAL.get(), 4, 12, 14))
                        .add(item(ModItems.CHISELED_CRYSTAL_BRICKS.get(), 2, 6, 10))
                        .add(item(ModItems.STARSTEEL_INGOT.get(), 1, 3, 10))
                        .add(item(ModItems.STARSTEEL_NUGGET.get(), 3, 9, 12))
                        .add(item(Items.AMETHYST_SHARD, 3, 8, 12))
                        .add(item(Items.LAPIS_LAZULI, 3, 8, 10))
                        .add(item(Items.GOLDEN_APPLE, 1, 1, 5))
                        .add(item(Items.ECHO_SHARD, 1, 2, 4))
                        .add(item(Items.TOTEM_OF_UNDYING, 1, 1, 1))));
        // The Sun Archon's hoard behind its throne (12b): gold and starsteel, phlogiston, and the sunstone the temple was lit with
        accept(ModChestLoot.SUN_TEMPLE, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.SUNSTONE_SHARD.get(), 1, 2, 1)))
                .withPool(pool(4, 7)
                        .add(item(Items.GOLD_INGOT, 3, 9, 14))
                        .add(item(ModItems.STARSTEEL_INGOT.get(), 2, 4, 12))
                        .add(item(ModItems.PHLOGISTON_SHARD.get(), 2, 6, 12))
                        .add(item(ModItems.GOLD_COIN.get(), 2, 6, 10))
                        .add(item(Items.GOLDEN_APPLE, 1, 2, 6))
                        .add(item(Items.ENCHANTED_GOLDEN_APPLE, 1, 1, 1))
                        .add(item(Items.EXPERIENCE_BOTTLE, 2, 5, 8))
                        .add(item(Items.BLAZE_POWDER, 2, 5, 8))));
        // The Gray lab's bench: what was left behind. Labs, stations and landings (21a) now and then hold the observer's notes.
        accept(ModChestLoot.GRAY_LAB, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.IRON_INGOT, 1, 4, 12))
                .add(item(Items.REDSTONE, 3, 8, 12))
                .add(item(Items.GLASS_BOTTLE, 1, 4, 12))
                .add(item(Items.GLOWSTONE_DUST, 2, 6, 10))
                .add(item(Items.AMETHYST_SHARD, 1, 3, 10))
                .add(item(Items.COPPER_INGOT, 2, 5, 8))
                .add(item(Items.RABBIT_HIDE, 1, 2, 8))
                .add(item(ModItems.GRAY_TECH_FRAGMENT.get(), 1, 2, 6))
                .add(item(Items.ENDER_PEARL, 1, 1, 4)))
                .withPool(fieldNotes(2)));
        // A live Gray station (10b): always a few fragments, and the stuff of their work
        accept(ModChestLoot.GRAY_STATION, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.GRAY_TECH_FRAGMENT.get(), 2, 4, 1)))
                .withPool(pool(3, 6)
                        .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 3, 10))
                        .add(item(ModItems.STARSTEEL_NUGGET.get(), 2, 6, 10))
                        .add(item(Items.REDSTONE, 4, 10, 12))
                        .add(item(Items.GLASS_BOTTLE, 2, 5, 10))
                        .add(item(Items.EXPERIENCE_BOTTLE, 1, 3, 8))
                        .add(item(Items.NAME_TAG, 1, 1, 5))
                        .add(item(Items.LEAD, 1, 2, 6))
                        .add(item(Items.ENDER_PEARL, 1, 2, 5))
                        .add(item(Items.GLOW_INK_SAC, 1, 3, 6)))
                .withPool(fieldNotes(3)));
        // A Gray Landing's cache (15a): plating and charged cells every time, Gray odds and ends, and now and then a whole part
        // 32c: a crash site's cargo pod: salvage, always one of the Grays' gadgets, now and then a second
        LootPool.Builder gadgets = pool(1, 1);
        ModItems.GADGETS.forEach(gadget -> gadgets.add(item(gadget.get(), 1, 1, 1)));
        LootPool.Builder another = pool(1, 1).add(EmptyLootItem.emptyItem().setWeight(15));
        ModItems.GADGETS.forEach(gadget -> another.add(item(gadget.get(), 1, 1, 1)));
        // 35b: a Christmas Parcel: always a Fruitcake and some coin, and two of the things a frontier relative would send
        accept(ModChestLoot.CHRISTMAS_PARCEL, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.FRUITCAKE.get(), 1, 1, 1)))
                .withPool(pool(1, 1).add(item(ModItems.COPPER_COIN.get(), 6, 12, 1)))
                .withPool(pool(2, 2)
                        .add(item(ModItems.REVOLVER_CARTRIDGE.get(), 8, 16, 10))
                        .add(item(ModItems.RIFLE_CARTRIDGE.get(), 5, 10, 8))
                        .add(item(ModItems.PERCUSSION_CAP.get(), 12, 24, 10))
                        .add(item(ModItems.COWBOY_COFFEE.get(), 1, 2, 10))
                        .add(item(ModItems.GOLD_COIN.get(), 1, 1, 3))));
        accept(ModChestLoot.CRASH_SITE, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.ALIEN_ALLOY_PLATING.get(), 1, 2, 1)))
                .withPool(pool(2, 3)
                        .add(item(ModItems.GRAY_TECH_FRAGMENT.get(), 1, 2, 10))
                        .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 2, 8))
                        .add(item(Items.REDSTONE, 2, 6, 8))
                        .add(item(ModItems.STARSTEEL_NUGGET.get(), 1, 3, 6))
                        .add(item(ModItems.EMPTY_PHLOGISTON_CELL.get(), 1, 1, 6)))
                .withPool(gadgets)
                .withPool(another));
        accept(ModChestLoot.GRAY_LANDING, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.ALIEN_ALLOY_PLATING.get(), 2, 4, 1)))
                .withPool(pool(1, 1).add(item(ModItems.PHLOGISTON_CELL.get(), 1, 2, 1)))
                .withPool(pool(2, 4)
                        .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 3, 10))
                        .add(item(ModItems.GRAY_TECH_FRAGMENT.get(), 1, 2, 10))
                        .add(item(ModItems.STARSTEEL_NUGGET.get(), 2, 5, 8))
                        .add(item(ModItems.EMPTY_PHLOGISTON_CELL.get(), 1, 2, 6))
                        .add(item(Items.REDSTONE, 3, 8, 8)))
                .withPool(pool(1, 1)
                        .add(EmptyLootItem.emptyItem().setWeight(9))
                        .add(item(ModItems.DAMAGED_GRAVITY_DRIVE.get(), 1, 1, 1))
                        .add(item(ModItems.BEAM_EMITTER.get(), 1, 1, 1))
                        .add(item(ModItems.LENS_ARRAY.get(), 1, 1, 1))
                        .add(item(ModItems.NAVIGATION_CORE.get(), 1, 1, 1)))
                .withPool(fieldNotes(4)));
        // The Symmes camp's stores: rations, fuel, paper, instruments, and now and then the way home
        accept(ModChestLoot.SYMMES_CAMP, LootTable.lootTable().withPool(pool(4, 7)
                .add(item(Items.BREAD, 2, 5, 15))
                .add(item(Items.COOKED_COD, 2, 4, 12))
                .add(item(Items.COAL, 2, 6, 12))
                .add(item(Items.PAPER, 2, 6, 10))
                .add(item(ModItems.COPPER_COIN.get(), 3, 8, 10))
                .add(item(ModItems.RIFLE_CARTRIDGE.get(), 2, 6, 8))
                .add(item(Items.LEAD, 1, 2, 6))
                .add(item(Items.COMPASS, 1, 1, 6))
                .add(item(Items.MAP, 1, 1, 6))
                .add(item(Items.SPYGLASS, 1, 1, 4))
                .add(item(ModItems.GOLD_COIN.get(), 1, 2, 4))
                .add(item(ModItems.POLAR_COMPASS.get(), 1, 1, 2))));
        // 25: the rebuilt Hollow sites' rooms (interim tables; phase 28 tiers them)
        accept(ModChestLoot.KINDLED_TOMB, LootTable.lootTable().withPool(pool(3, 5)
                .add(item(Items.GOLD_INGOT, 1, 3, 10))
                .add(item(ModItems.STARSTEEL_NUGGET.get(), 2, 6, 12))
                .add(item(Items.LAPIS_LAZULI, 3, 8, 10))
                .add(item(Items.BONE, 2, 5, 10))
                .add(item(Items.CANDLE, 1, 3, 8))
                .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 2, 5))
                .add(item(Items.ECHO_SHARD, 1, 1, 2))));
        accept(ModChestLoot.KINDLED_SCRIPTORIUM, LootTable.lootTable().withPool(pool(3, 5)
                .add(item(Items.PAPER, 2, 6, 14))
                .add(item(Items.BOOK, 1, 3, 12))
                .add(item(Items.INK_SAC, 1, 3, 10))
                .add(item(Items.LAPIS_LAZULI, 2, 5, 8))
                .add(item(Items.AMETHYST_SHARD, 1, 4, 8))
                .add(item(Items.CANDLE, 1, 2, 8))
                .add(item(Items.COMPASS, 1, 1, 4))));
        accept(ModChestLoot.KINDLED_OFFERING, LootTable.lootTable().withPool(pool(1, 2)
                .add(item(Items.GOLD_NUGGET, 2, 6, 14))
                .add(item(Items.AMETHYST_SHARD, 1, 3, 10))
                .add(item(Items.LAPIS_LAZULI, 1, 4, 10))
                .add(item(ModItems.STARSTEEL_NUGGET.get(), 1, 3, 8))
                .add(item(Items.BONE_MEAL, 1, 3, 6))));
        accept(ModChestLoot.GRAY_LAB_STORE, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.GLASS_BOTTLE, 2, 5, 14))
                .add(item(Items.SLIME_BALL, 1, 3, 8))
                .add(item(Items.FERMENTED_SPIDER_EYE, 1, 2, 8))
                .add(item(Items.RABBIT_HIDE, 1, 3, 10))
                .add(item(ModItems.THICK_HIDE.get(), 1, 2, 6))
                .add(item(ModItems.GLYPTODON_SCUTE.get(), 1, 2, 5))
                .add(item(Items.GLOW_INK_SAC, 1, 3, 8))
                .add(item(ModItems.GRAY_TECH_FRAGMENT.get(), 1, 1, 4))));
        accept(ModChestLoot.GRAY_LAB_CONTROL, LootTable.lootTable().withPool(pool(2, 5)
                .add(item(Items.REDSTONE, 3, 9, 14))
                .add(item(Items.COPPER_INGOT, 2, 5, 10))
                .add(item(Items.QUARTZ, 2, 6, 10))
                .add(item(ModItems.GRAY_TECH_FRAGMENT.get(), 1, 2, 8))
                .add(item(ModItems.EMPTY_PHLOGISTON_CELL.get(), 1, 1, 4))
                .add(item(Items.MAP, 1, 1, 4)))
                .withPool(fieldNotes(3)));
        accept(ModChestLoot.GRAY_STATION_STORE, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.WHEAT, 4, 12, 14))
                .add(item(Items.HAY_BLOCK, 1, 2, 8))
                .add(item(Items.LEAD, 1, 2, 10))
                .add(item(Items.NAME_TAG, 1, 1, 6))
                .add(item(ModItems.RAW_MAMMOTH.get(), 1, 3, 8))
                .add(item(Items.GLASS_BOTTLE, 1, 4, 8))
                .add(item(ModItems.GRAY_TECH_FRAGMENT.get(), 1, 1, 5))));
        accept(ModChestLoot.GRAY_PENS_BELONGINGS, LootTable.lootTable().withPool(pool(3, 5)
                .add(item(ModItems.COPPER_COIN.get(), 3, 9, 14))
                .add(item(ModItems.REVOLVER_CARTRIDGE.get(), 3, 8, 12))
                .add(item(ModItems.PERCUSSION_CAP.get(), 4, 10, 10))
                .add(item(ModItems.GOLD_COIN.get(), 1, 2, 6))
                .add(item(ModItems.BANDANA.get(), 1, 1, 6))
                .add(item(ModItems.FRONTIER_GAZETTE.get(), 1, 1, 6))
                .add(item(ModItems.OUTLAW_BADGE.get(), 1, 1, 3))
                .add(item(ModItems.REVOLVER.get(), 1, 1, 2))));
        accept(ModChestLoot.SYMMES_WRECK, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.COAL, 3, 8, 14))
                .add(item(ModItems.BRASS_GEAR.get(), 1, 3, 10))
                .add(item(ModItems.BRASS_PLATE.get(), 1, 3, 10))
                .add(item(ModItems.COPPER_PIPE.get(), 2, 5, 8))
                .add(item(ModItems.STEAM_CANISTER.get(), 1, 1, 5))
                .add(item(ModItems.RIFLE_CARTRIDGE.get(), 2, 6, 8))
                .add(item(ModItems.GOLD_COIN.get(), 1, 3, 5))
                .add(item(ModItems.POLAR_COMPASS.get(), 1, 1, 2))));
        accept(ModChestLoot.SYMMES_DIG, LootTable.lootTable().withPool(pool(2, 5)
                .add(item(Items.BRUSH, 1, 1, 8))
                .add(item(Items.IRON_SHOVEL, 1, 1, 6))
                .add(item(Items.PAPER, 2, 5, 10))
                .add(item(Items.BRICK, 1, 4, 8))
                .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 2, 5))
                .add(item(ModItems.STARSTEEL_NUGGET.get(), 1, 4, 8))
                .add(item(Items.TORCH, 3, 8, 10))));
        accept(ModChestLoot.CRYSTAL_HOME, LootTable.lootTable().withPool(pool(2, 4)
                .add(item(Items.AMETHYST_SHARD, 1, 4, 12))
                .add(item(ModItems.CONCAVIAN_CRYSTAL.get(), 1, 4, 10))
                .add(item(Items.CANDLE, 1, 3, 10))
                .add(item(Items.WOOL.white(), 1, 3, 8))
                .add(item(ModItems.STARSTEEL_NUGGET.get(), 1, 4, 8))
                .add(item(Items.GOLD_NUGGET, 2, 5, 8))));
        accept(ModChestLoot.CRYSTAL_LIBRARY, LootTable.lootTable().withPool(pool(3, 5)
                .add(item(Items.BOOK, 1, 3, 14))
                .add(item(Items.PAPER, 2, 6, 12))
                .add(item(Items.LAPIS_LAZULI, 2, 6, 10))
                .add(item(Items.EXPERIENCE_BOTTLE, 1, 3, 6))
                .add(item(Items.AMETHYST_SHARD, 1, 3, 8))
                .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 2, 4))));
        accept(ModChestLoot.CRYSTAL_MARKET, LootTable.lootTable().withPool(pool(3, 5)
                .add(item(ModItems.STARSTEEL_NUGGET.get(), 2, 6, 12))
                .add(item(Items.AMETHYST_SHARD, 2, 5, 10))
                .add(item(Items.GOLD_NUGGET, 2, 6, 10))
                .add(item(ModItems.CONCAVIAN_CRYSTAL.get(), 2, 5, 8))
                .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 2, 5))
                .add(item(Items.EMERALD, 1, 2, 4))));
        accept(ModChestLoot.SUN_TEMPLE_OFFERING, LootTable.lootTable().withPool(pool(2, 4)
                .add(item(Items.GOLD_INGOT, 1, 3, 12))
                .add(item(Items.GOLD_NUGGET, 3, 9, 12))
                .add(item(Items.BLAZE_POWDER, 1, 3, 8))
                .add(item(Items.GLOWSTONE_DUST, 2, 5, 8))
                .add(item(ModItems.GOLD_COIN.get(), 1, 3, 6))
                .add(item(ModItems.SUNSTONE_SHARD.get(), 1, 1, 1))));
        // 26: the dungeons' end rooms (interim; 28 tiers them)
        accept(ModChestLoot.CATACOMB_VAULT, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.PHLOGISTON_SHARD.get(), 3, 6, 1)))
                .withPool(pool(4, 7)
                        .add(item(Items.GOLD_INGOT, 2, 6, 12))
                        .add(item(ModItems.STARSTEEL_INGOT.get(), 2, 4, 12))
                        .add(item(Items.DIAMOND, 1, 2, 6))
                        .add(item(Items.EMERALD, 2, 5, 8))
                        .add(item(Items.GOLDEN_APPLE, 1, 2, 6))
                        .add(item(Items.ECHO_SHARD, 1, 2, 4))
                        .add(item(ModItems.SUNSTONE_SHARD.get(), 1, 1, 2))
                        .add(item(Items.ENCHANTED_GOLDEN_APPLE, 1, 1, 1))));
        accept(ModChestLoot.GRAY_COMPLEX_CORE, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.PHLOGISTON_CELL.get(), 1, 3, 1)))
                .withPool(pool(1, 1).add(item(ModItems.ALIEN_ALLOY_PLATING.get(), 2, 4, 1)))
                .withPool(pool(2, 4)
                        .add(item(ModItems.GRAY_TECH_FRAGMENT.get(), 2, 4, 12))
                        .add(item(ModItems.PHLOGISTON_SHARD.get(), 2, 4, 10))
                        .add(item(ModItems.EMPTY_PHLOGISTON_CELL.get(), 1, 2, 8))
                        .add(item(Items.REDSTONE_BLOCK, 1, 2, 6)))
                .withPool(pool(1, 1)
                        .add(EmptyLootItem.emptyItem().setWeight(4))
                        .add(item(ModItems.DAMAGED_GRAVITY_DRIVE.get(), 1, 1, 1))
                        .add(item(ModItems.BEAM_EMITTER.get(), 1, 1, 1))
                        .add(item(ModItems.LENS_ARRAY.get(), 1, 1, 1))
                        .add(item(ModItems.NAVIGATION_CORE.get(), 1, 1, 1)))
                .withPool(fieldNotes(2)));
        // 27: the wild sites (interim; 28 tiers them)
        accept(ModChestLoot.ZIGGURAT, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.GOLD_INGOT, 1, 4, 12))
                .add(item(Items.EMERALD, 1, 3, 8))
                .add(item(ModItems.STARSTEEL_INGOT.get(), 1, 3, 10))
                .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 3, 8))
                .add(item(Items.COCOA_BEANS, 2, 6, 10))
                .add(item(Items.BAMBOO, 3, 9, 8))
                .add(item(Items.GOLDEN_APPLE, 1, 1, 3))));
        // 43d: the Drowned Quarter. Its counting-house vault always holds the Sealed Hull, over what a river port banked;
        // the domes held homes and shops; the flooded houses have what the water left. And the Symmes Landing's crate:
        // always a chart to the nearest Quarter and a harpoon, over a boat's stores.
        accept(ModChestLoot.DROWNED_VAULT, LootTable.lootTable()
                .withPool(pool(1, 1).add(LootItem.lootTableItem(ModItems.SEALED_HULL.get())))
                .withPool(pool(4, 6)
                        .add(item(ModItems.PHLOGISTON_SHARD.get(), 2, 5, 12))
                        .add(item(ModItems.STARSTEEL_INGOT.get(), 1, 3, 8))
                        .add(item(ModItems.GOLD_COIN.get(), 3, 8, 12))
                        .add(item(Items.GOLD_INGOT, 2, 5, 10))
                        .add(item(Items.AMETHYST_SHARD, 3, 8, 10))
                        .add(item(Items.PRISMARINE_CRYSTALS, 2, 6, 8))
                        .add(item(Items.EMERALD, 1, 4, 6))
                        .add(item(Items.HEART_OF_THE_SEA, 1, 1, 1))));
        accept(ModChestLoot.DROWNED_HOME, LootTable.lootTable().withPool(pool(3, 5)
                .add(item(Items.GOLD_NUGGET, 3, 9, 12))
                .add(item(ModItems.LAMP_GEL.get(), 1, 3, 10))
                .add(item(Items.NAUTILUS_SHELL, 1, 1, 6))
                .add(item(Items.AMETHYST_SHARD, 1, 4, 8))
                .add(item(Items.CLAY_BALL, 2, 6, 10))
                .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 2, 6))
                .add(item(Items.EMERALD, 1, 2, 4))));
        accept(ModChestLoot.DROWNED_STREET, LootTable.lootTable().withPool(pool(2, 4)
                .add(item(Items.BONE, 1, 3, 10))
                .add(item(Items.CLAY_BALL, 1, 4, 12))
                .add(item(Items.PRISMARINE_SHARD, 1, 3, 10))
                .add(item(Items.IRON_NUGGET, 2, 6, 8))
                .add(item(Items.GOLD_NUGGET, 1, 4, 6))
                .add(item(Items.KELP, 1, 3, 8))));
        accept(ModChestLoot.SYMMES_LANDING, LootTable.lootTable()
                .withPool(pool(1, 1).add(map(com.hollowfrontier.worldgen.structure.ModStructures.DROWNED_QUARTER, "drowned_quarter")))
                .withPool(pool(1, 1).add(LootItem.lootTableItem(ModItems.HARPOON.get())))
                .withPool(pool(3, 5)
                        .add(item(Items.COAL, 2, 6, 10))
                        .add(item(Items.LEAD, 1, 2, 8))
                        .add(item(ModItems.RIFLE_CARTRIDGE.get(), 2, 6, 8))
                        .add(item(Items.COOKED_COD, 1, 3, 8))
                        .add(item(Items.COMPASS, 1, 1, 4))
                        .add(item(ModItems.TALLOW.get(), 1, 3, 6))));
        accept(ModChestLoot.TAR_PIT, LootTable.lootTable().withPool(pool(3, 5)
                .add(item(ModItems.MAMMOTH_TUSK.get(), 1, 2, 10))
                .add(item(ModItems.TERROR_BIRD_TALON.get(), 1, 2, 8))
                .add(item(Items.BONE, 3, 8, 14))
                .add(item(ModItems.GOLD_COIN.get(), 1, 3, 8))
                .add(item(ModItems.RIFLE_CARTRIDGE.get(), 2, 6, 8))
                .add(item(Items.GOLD_NUGGET, 3, 9, 10))));
        accept(ModChestLoot.PHLOGISTON_MINE, LootTable.lootTable().withPool(pool(3, 6)
                .add(item(Items.TORCH, 4, 12, 14))
                .add(item(Items.RAIL, 2, 8, 10))
                .add(item(Items.IRON_PICKAXE, 1, 1, 4))
                .add(item(ModItems.DYNAMITE.get(), 1, 3, 8))
                .add(item(ModItems.COPPER_COIN.get(), 3, 9, 10))
                .add(item(Items.BREAD, 1, 4, 10))
                .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 2, 6))));
        accept(ModChestLoot.PHLOGISTON_MINE_CAVERN, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.PHLOGISTON_SHARD.get(), 2, 5, 1)))
                .withPool(pool(3, 5)
                        .add(item(ModItems.CONCAVIAN_CRYSTAL.get(), 2, 6, 12))
                        .add(item(Items.AMETHYST_SHARD, 3, 8, 12))
                        .add(item(ModItems.STEAM_CANISTER.get(), 1, 1, 6))
                        .add(item(ModItems.BRASS_GEAR.get(), 1, 3, 8))
                        .add(item(ModItems.DYNAMITE.get(), 2, 4, 8))
                        .add(item(ModItems.STEAM_DRILL.get(), 1, 1, 1))));
        accept(ModChestLoot.OBSERVATORY, LootTable.lootTable().withPool(pool(3, 5)
                .add(item(Items.SPYGLASS, 1, 1, 6))
                .add(item(Items.COMPASS, 1, 1, 8))
                .add(item(Items.CLOCK, 1, 1, 6))
                .add(item(Items.PAPER, 3, 8, 12))
                .add(item(Items.LAPIS_LAZULI, 3, 8, 10))
                .add(item(ModItems.STARSTEEL_NUGGET.get(), 2, 6, 10))
                .add(item(ModItems.SUNSTONE_SHARD.get(), 1, 1, 1))));
        accept(ModChestLoot.FOUNDRY, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.STARSTEEL_INGOT.get(), 2, 5, 1)))
                .withPool(pool(3, 5)
                        .add(item(ModItems.RAW_STARSTEEL.get(), 2, 6, 12))
                        .add(item(ModItems.STARSTEEL_NUGGET.get(), 4, 12, 12))
                        .add(item(Items.COAL, 4, 12, 12))
                        .add(item(Items.IRON_INGOT, 2, 5, 8))
                        .add(item(Items.BLAZE_POWDER, 1, 3, 6))
                        .add(item(ModItems.STARSTEEL_HORSESHOES.get(), 1, 1, 2))));
        accept(ModChestLoot.SKY_SHRINE, LootTable.lootTable().withPool(pool(2, 4)
                .add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 3, 10))
                .add(item(Items.FEATHER, 2, 6, 10))
                .add(item(Items.PHANTOM_MEMBRANE, 1, 2, 4))
                .add(item(Items.AMETHYST_SHARD, 2, 5, 10))
                .add(item(Items.ENDER_PEARL, 1, 2, 6))
                .add(item(ModItems.CONCAVIAN_CRYSTAL.get(), 1, 4, 8))));
        accept(ModChestLoot.GRAY_RELAY, LootTable.lootTable()
                .withPool(pool(1, 1).add(item(ModItems.GRAY_TECH_FRAGMENT.get(), 1, 3, 1)))
                .withPool(pool(2, 4)
                        .add(item(ModItems.PHLOGISTON_CELL.get(), 1, 1, 6))
                        .add(item(ModItems.EMPTY_PHLOGISTON_CELL.get(), 1, 2, 8))
                        .add(item(ModItems.ALIEN_ALLOY_PLATING.get(), 1, 2, 6))
                        .add(item(Items.REDSTONE, 4, 10, 12))
                        .add(item(ModItems.LENS_ARRAY.get(), 1, 1, 1)))
                .withPool(fieldNotes(3)));
        // What was buried with the airship's pilot (the strange grave on Boot Hill): always a phlogiston shard, and odds and ends
        // 11a: the pilot himself: his skull, his logbook in a script no one can read, and bits of his craft
        accept(ModChestLoot.STRANGE_GRAVE, LootTable.lootTable()
                .withPool(pool(1, 1).add(LootItem.lootTableItem(ModItems.STRANGE_SKULL.get())))
                .withPool(pool(1, 1).add(LootItem.lootTableItem(Items.WRITTEN_BOOK).apply(pilotsLogCover()).apply(pilotsLog())))
                .withPool(pool(1, 1).add(item(ModItems.GRAY_TECH_FRAGMENT.get(), 1, 2, 1)))
                .withPool(pool(1, 1).add(item(ModItems.PHLOGISTON_SHARD.get(), 1, 2, 1)))
                .withPool(pool(2, 4)
                        .add(item(Items.IRON_NUGGET, 2, 6, 10))
                        .add(item(Items.GOLD_NUGGET, 1, 4, 8))
                        .add(item(Items.PAPER, 1, 2, 6))
                        .add(item(Items.COMPASS, 1, 1, 3))
                        .add(item(Items.GLOWSTONE_DUST, 1, 3, 4))));
    }

    // ---------------------------------------------------------------- 28: the Hollow rooms' tiers

    /**
     * A Hollow room's tiers (28), over its common finds: one chance in {@code rareIn} at one of {@code rare}, and one in
     * {@code jackpotIn} at one of {@code jackpot} (0 for none). Relics, the explorers' maps, gun books, sherds, the trim.
     */
    private record Tiers(int rareIn, List<UniformContainerBase.Builder<?>> rare, int jackpotIn, List<UniformContainerBase.Builder<?>> jackpot) {
        void addTo(LootTable.Builder table) {
            table.withPool(chance(rareIn, rare));
            if (jackpotIn > 0) {
                table.withPool(chance(jackpotIn, jackpot));
            }
        }

        private static LootPool.Builder chance(int in, List<UniformContainerBase.Builder<?>> entries) {
            LootPool.Builder pool = pool(1, 1);
            if (in > 1) {
                pool.add(EmptyLootItem.emptyItem().setWeight(entries.size() * (in - 1)));
            }
            entries.forEach(pool::add);
            return pool;
        }
    }

    private java.util.Map<net.minecraft.resources.ResourceKey<LootTable>, Tiers> tiers;

    /** Every table goes out through here: a Hollow room's gets its rare and jackpot pools added on the way (28). */
    private void accept(net.minecraft.resources.ResourceKey<LootTable> key, LootTable.Builder table) {
        if (tiers == null) {
            tiers = hollowTiers();
        }
        Tiers t = tiers.get(key);
        if (t != null) {
            t.addTo(table);
        }
        context.accept(key, table);
    }

    private static UniformContainerBase.Builder<?> one(ItemLike item) {
        return LootItem.lootTableItem(item).setWeight(1);
    }

    /** An explorer's map to the nearest of {@code to} (its name from lang: {@code item.hollowfrontier.map.<name>}). */
    private UniformContainerBase.Builder<?> map(net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> to, String name) {
        return LootItem.lootTableItem(Items.MAP).setWeight(1)
                .apply(net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction.makeExplorationMap(
                                net.minecraft.core.HolderSet.direct(context.lookup(Registries.STRUCTURE).getOrThrow(to)))
                        .setMapDecoration(net.minecraft.world.level.saveddata.maps.MapDecorationTypes.RED_X).setZoom((byte) 1).setSearchRadius(60)
                        .setSkipKnownStructures(false))
                .apply(net.minecraft.world.level.storage.loot.functions.SetNameFunction.setName(Component.translatable("item.hollowfrontier.map." + name),
                        net.minecraft.world.level.storage.loot.functions.SetNameFunction.Target.ITEM_NAME));
    }

    private UniformContainerBase.Builder<?> book() {
        return gunBook(1);
    }

    /** Which Hollow rooms get which tiers. */
    private java.util.Map<net.minecraft.resources.ResourceKey<LootTable>, Tiers> hollowTiers() {
        var sunTemple = map(com.hollowfrontier.worldgen.structure.ModStructures.SUN_TEMPLE, "sun_temple");
        java.util.Map<net.minecraft.resources.ResourceKey<LootTable>, Tiers> t = new java.util.HashMap<>();
        UniformContainerBase.Builder<?> trim = one(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get());
        // The Kindled sites, the catacombs and the crystal city
        t.put(ModChestLoot.KINDLED_VAULT, new Tiers(3, List.of(one(ModItems.KINDLED_IDOL.get()), one(ModItems.CRYSTAL_TABLET.get()), book(),
                map(com.hollowfrontier.worldgen.structure.ModStructures.CRYSTAL_RUINS, "crystal_ruins")), 8, List.of(trim, one(ModItems.SUNSTONE_CIRCLET.get()))));
        t.put(ModChestLoot.KINDLED_TOMB, new Tiers(4, List.of(one(ModItems.KINDLED_IDOL.get()), one(ModItems.STARSTEEL_MASK.get()), book()), 12,
                List.of(one(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get()))));
        t.put(ModChestLoot.KINDLED_SCRIPTORIUM, new Tiers(3, List.of(one(ModItems.CRYSTAL_TABLET.get()), one(ModItems.STAR_CHART.get()),
                map(com.hollowfrontier.worldgen.structure.ModStructures.CRYSTAL_RUINS, "crystal_ruins")), 0, List.of()));
        t.put(ModChestLoot.KINDLED_OFFERING, new Tiers(10, List.of(one(ModItems.CRYSTAL_POTTERY_SHERD.get()), one(ModItems.SUN_POTTERY_SHERD.get())), 0, List.of()));
        t.put(ModChestLoot.CATACOMB_VAULT, new Tiers(1, List.of(one(ModItems.KINDLED_IDOL.get()), one(ModItems.SUNSTONE_CIRCLET.get()), one(ModItems.STARSTEEL_MASK.get())),
                2, List.of(one(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get()), book())));
        t.put(ModChestLoot.CRYSTAL_RUINS, new Tiers(3, List.of(one(ModItems.CRYSTAL_TABLET.get()), one(ModItems.STAR_CHART.get())), 10,
                List.of(one(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get()))));
        t.put(ModChestLoot.CRYSTAL_HOME, new Tiers(5, List.of(one(ModItems.CRYSTAL_POTTERY_SHERD.get()), one(ModItems.CRYSTAL_TABLET.get())), 0, List.of()));
        t.put(ModChestLoot.CRYSTAL_LIBRARY, new Tiers(2, List.of(one(ModItems.CRYSTAL_TABLET.get()), one(ModItems.STAR_CHART.get()), book()), 8,
                List.of(one(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get()))));
        t.put(ModChestLoot.CRYSTAL_MARKET, new Tiers(3, List.of(one(ModItems.STARSTEEL_MASK.get()), one(ModItems.SUN_POTTERY_SHERD.get()),
                one(ModItems.CRYSTAL_POTTERY_SHERD.get())), 0, List.of()));
        // The Sun Temple and its undercroft
        t.put(ModChestLoot.SUN_TEMPLE, new Tiers(2, List.of(one(ModItems.STARSTEEL_MASK.get()), book()), 3,
                List.of(one(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get()), one(ModItems.SUNSTONE_CIRCLET.get()))));
        t.put(ModChestLoot.SUN_TEMPLE_OFFERING, new Tiers(5, List.of(one(ModItems.SUN_POTTERY_SHERD.get()), one(ModItems.STARSTEEL_MASK.get())), 0, List.of()));
        // The Grays
        var pens = (java.util.function.Supplier<UniformContainerBase.Builder<?>>) () -> map(com.hollowfrontier.worldgen.structure.ModStructures.GRAY_PENS, "gray_pens");
        t.put(ModChestLoot.GRAY_LAB, new Tiers(3, List.of(one(ModItems.GRAY_SPECIMEN_JAR.get()), pens.get()), 0, List.of()));
        t.put(ModChestLoot.GRAY_LAB_STORE, new Tiers(3, List.of(one(ModItems.GRAY_SPECIMEN_JAR.get()), one(ModItems.WATCHER_POTTERY_SHERD.get())), 0, List.of()));
        t.put(ModChestLoot.GRAY_LAB_CONTROL, new Tiers(2, List.of(pens.get(), one(ModItems.WATCHER_POTTERY_SHERD.get())), 0, List.of()));
        t.put(ModChestLoot.GRAY_STATION, new Tiers(4, List.of(one(ModItems.GRAY_SPECIMEN_JAR.get())), 0, List.of()));
        t.put(ModChestLoot.GRAY_STATION_STORE, new Tiers(4, List.of(one(ModItems.GRAY_SPECIMEN_JAR.get())), 0, List.of()));
        t.put(ModChestLoot.GRAY_COMPLEX_CORE, new Tiers(1, List.of(one(ModItems.GRAY_SPECIMEN_JAR.get())), 2, List.of(pens.get())));
        t.put(ModChestLoot.GRAY_PENS_BELONGINGS, new Tiers(3, List.of(book(), one(ModItems.EXPEDITION_SEXTANT.get())), 0, List.of()));
        t.put(ModChestLoot.GRAY_RELAY, new Tiers(3, List.of(one(ModItems.GRAY_SPECIMEN_JAR.get()), pens.get()), 0, List.of()));
        // The expedition's leavings, and the wild sites
        t.put(ModChestLoot.SYMMES_CAMP, new Tiers(2, List.of(sunTemple, one(ModItems.EXPEDITION_SEXTANT.get())), 0, List.of()));
        t.put(ModChestLoot.SYMMES_WRECK, new Tiers(2, List.of(one(ModItems.EXPEDITION_SEXTANT.get()),
                map(com.hollowfrontier.worldgen.structure.ModStructures.SUN_TEMPLE, "sun_temple")), 6, List.of(book())));
        t.put(ModChestLoot.SYMMES_DIG, new Tiers(3, List.of(one(ModItems.IVORY_CARVING.get()), one(ModItems.CRYSTAL_TABLET.get()), one(ModItems.SUN_POTTERY_SHERD.get())), 0, List.of()));
        t.put(ModChestLoot.ZIGGURAT, new Tiers(2, List.of(one(ModItems.KINDLED_IDOL.get()), one(ModItems.STARSTEEL_MASK.get())), 6,
                List.of(one(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get()))));
        t.put(ModChestLoot.TAR_PIT, new Tiers(2, List.of(one(ModItems.IVORY_CARVING.get()), one(ModItems.EXPEDITION_SEXTANT.get())), 0, List.of()));
        var foundry = (java.util.function.Supplier<UniformContainerBase.Builder<?>>) () -> map(com.hollowfrontier.worldgen.structure.ModStructures.FOUNDRY, "foundry");
        t.put(ModChestLoot.PHLOGISTON_MINE, new Tiers(3, List.of(foundry.get(), book()), 0, List.of()));
        t.put(ModChestLoot.PHLOGISTON_MINE_CAVERN, new Tiers(3, List.of(one(ModItems.CRYSTAL_TABLET.get()), foundry.get()), 0, List.of()));
        t.put(ModChestLoot.OBSERVATORY, new Tiers(2, List.of(one(ModItems.STAR_CHART.get()), one(ModItems.CRYSTAL_TABLET.get())), 8,
                List.of(one(ModItems.SUNSTONE_CIRCLET.get()))));
        t.put(ModChestLoot.FOUNDRY, new Tiers(2, List.of(one(ModItems.STARSTEEL_MASK.get()), book()), 6,
                List.of(one(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get()))));
        // 43d: the Drowned Quarter and the Symmes Landing
        t.put(ModChestLoot.DROWNED_VAULT, new Tiers(1, List.of(one(ModItems.PILOTS_BELL.get()), one(ModItems.WATER_CLOCK.get()), one(ModItems.KINDLED_IDOL.get())),
                2, List.of(one(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get()), one(ModItems.WAVE_POTTERY_SHERD.get()))));
        t.put(ModChestLoot.DROWNED_HOME, new Tiers(3, List.of(one(ModItems.WAVE_POTTERY_SHERD.get()), one(ModItems.PILOTS_BELL.get()),
                one(ModItems.CRYSTAL_TABLET.get())), 0, List.of()));
        t.put(ModChestLoot.DROWNED_STREET, new Tiers(4, List.of(one(ModItems.WAVE_POTTERY_SHERD.get()), one(ModItems.WATER_CLOCK.get())), 0, List.of()));
        t.put(ModChestLoot.SYMMES_LANDING, new Tiers(2, List.of(one(ModItems.EXPEDITION_SEXTANT.get()), book()), 0, List.of()));
        t.put(ModChestLoot.SKY_SHRINE, new Tiers(2, List.of(one(ModItems.STAR_CHART.get()), one(ModItems.KINDLED_IDOL.get())), 6,
                List.of(one(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get()))));
        return t;
    }

    /** One in {@code chance} chests of this kind gets a copy of the Gray field notes (21a), any of its variants. */
    private static LootPool.Builder fieldNotes(int chance) {
        LootPool.Builder pool = pool(1, 1).add(EmptyLootItem.emptyItem().setWeight(FieldNotes.VARIANTS * (chance - 1)));
        for (int v = 0; v < FieldNotes.VARIANTS; v++) {
            pool.add(LootItem.lootTableItem(Items.WRITTEN_BOOK).apply(fieldNotesCover()).apply(fieldNotesPages(v)));
        }
        return pool;
    }

    static LootItemFunction.Builder fieldNotesPages(int variant) {
        List<Filterable<Component>> pages = new java.util.ArrayList<>();
        pages.add(Filterable.passThrough(Component.translatable(FieldNotes.titleKey())));
        for (int i : FieldNotes.entries(variant)) {
            pages.add(Filterable.passThrough(Component.translatable(FieldNotes.entryKey(i))));
        }
        return () -> new SetWrittenBookPagesFunction(Optional.empty(), pages, ListOperation.ReplaceAll.INSTANCE);
    }

    static LootItemFunction.Builder fieldNotesCover() {
        return () -> new SetBookCoverFunction(Optional.empty(), Optional.of(Filterable.passThrough(FieldNotes.TITLE)),
                Optional.of(FieldNotes.AUTHOR), Optional.of(0));
    }

    /** The pages of the pilot's logbook (11a). */
    public static final int PILOTS_LOG_PAGES = 4;

    /**
     * The Boot Hill pilot's logbook (11a): a written book whose pages (book.hollowfrontier.pilots_log.<n>) are printed in the
     * enchanting table's glyphs, so no one on the frontier can read a word of it.
     */
    static LootItemFunction.Builder pilotsLog() {
        FontDescription glyphs = new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));
        List<Filterable<Component>> pages = new java.util.ArrayList<>();
        for (int i = 1; i <= PILOTS_LOG_PAGES; i++) {
            pages.add(Filterable.passThrough(Component.translatable("book.hollowfrontier.pilots_log." + i).withStyle(style -> style.withFont(glyphs))));
        }
        return () -> new SetWrittenBookPagesFunction(Optional.empty(), pages, ListOperation.ReplaceAll.INSTANCE);
    }

    /** Its cover: "Logbook", by "?", worn (a copy of a copy). */
    static LootItemFunction.Builder pilotsLogCover() {
        return () -> new SetBookCoverFunction(Optional.empty(), Optional.of(Filterable.passThrough("Logbook")), Optional.of("?"), Optional.of(3));
    }

    // An enchanted book with one of the gun enchantments (22), at a random level
    private UniformContainerBase.Builder<?> gunBook(int weight) {
        return LootItem.lootTableItem(Items.BOOK).setWeight(weight).apply(gunEnchantment());
    }

    private EnchantRandomlyFunction.Builder gunEnchantment() {
        return EnchantRandomlyFunction.randomEnchantment().withOneOf(context.lookup(Registries.ENCHANTMENT).getOrThrow(ModTags.Enchantments.GUN));
    }

    private static LootPool.Builder pool(int minRolls, int maxRolls) {
        return LootPool.lootPool().setRolls(ContextIntProviders.between(minRolls, maxRolls));
    }

    private static UniformContainerBase.Builder<?> item(ItemLike item, int min, int max, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight).apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
    }
}
