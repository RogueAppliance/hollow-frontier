package com.hollowfrontier.datagen;

import java.util.Set;

import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(LootTableSubProvider.Context context) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.BRASS_BLOCK.get());
        dropSelf(ModBlocks.BOUNTY_BOARD.get());
        dropSelf(ModBlocks.BAR_COUNTER.get());
        dropSelf(ModBlocks.BAR_STOOL.get());
        dropSelf(ModBlocks.SALOON_PIANO.get());
        dropSelf(ModBlocks.CARD_TABLE.get());
        for (var furniture : java.util.List.of(ModBlocks.OAK_CHAIR, ModBlocks.SPRUCE_CHAIR, ModBlocks.DARK_OAK_CHAIR, ModBlocks.OAK_TABLE,
                ModBlocks.SPRUCE_TABLE, ModBlocks.DARK_OAK_TABLE, ModBlocks.TOWN_LAMP)) {
            dropSelf(furniture.get());
        }
        add(ModBlocks.SALOON_DOORS.get(), this::createDoorTable);
        dropSelf(ModBlocks.BOILER.get());
        dropSelf(ModBlocks.COPPER_PIPE.get());
        dropSelf(ModBlocks.STEAM_TANK.get());
        dropSelf(ModBlocks.PRESSURE_GAUGE.get());
        dropSelf(ModBlocks.CRUSHER.get());
        // Unbreakable in survival; a creative player breaking one gets nothing, like an end portal frame.
        add(ModBlocks.GATEWAY_FRAME.get(), noDrop());
        dropSelf(ModBlocks.CRUST_ROCK.get());
        dropSelf(ModBlocks.JELLY_LAMP.get()); // 43b
        // Fronds drop themselves to shears or silk touch, otherwise now and then a fern; a vent crumbles to ash unless silk-touched
        add(ModBlocks.FERN_FROND.get(), block -> createSilkTouchOrShearsDispatchTable(block,
                applyExplosionCondition(block, LootItem.lootTableItem(Items.FERN)).when(LootItemRandomChanceCondition.randomChance(0.125F))));
        dropSelf(ModBlocks.ASH.get());
        add(ModBlocks.ASH_VENT.get(), block -> createSilkTouchDispatchTable(block,
                applyExplosionDecay(block, LootItem.lootTableItem(ModItems.ASH.get()).apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 4))))));
        // The Hollow Earth's ores (9c): raw starsteel like raw gold; phlogiston shards one or two at a time, with fortune
        add(ModBlocks.STARSTEEL_ORE.get(), block -> createOreDrop(block, ModItems.RAW_STARSTEEL.get()));
        add(ModBlocks.DEEPSLATE_STARSTEEL_ORE.get(), block -> createOreDrop(block, ModItems.RAW_STARSTEEL.get()));
        add(ModBlocks.PHLOGISTON_ORE.get(), this::phlogistonOre);
        add(ModBlocks.DEEPSLATE_PHLOGISTON_ORE.get(), this::phlogistonOre);
        dropSelf(ModBlocks.RAW_STARSTEEL_BLOCK.get());
        dropSelf(ModBlocks.STARSTEEL_BLOCK.get());
        dropSelf(ModBlocks.PHLOGISTON_LAMP.get());
        // Concavia's crystal stone (12a): each drops itself (a double slab two)
        for (var block : java.util.List.of(ModBlocks.CONCAVIAN_CRYSTAL, ModBlocks.CRYSTAL_BRICKS, ModBlocks.CRYSTAL_BRICK_STAIRS,
                ModBlocks.CRYSTAL_BRICK_WALL, ModBlocks.CHISELED_CRYSTAL_BRICKS)) {
            dropSelf(block.get());
        }
        add(ModBlocks.CRYSTAL_BRICK_SLAB.get(), this::createSlabItemTable);
        // A broken Sun Pylon (12b) gives up its sunstone, unless silk-touched
        add(ModBlocks.SUN_PYLON.get(), block -> createSilkTouchDispatchTable(block, applyExplosionDecay(block, LootItem.lootTableItem(ModItems.SUNSTONE_SHARD.get()))));
        // 13a: a saucer's hull plates come away whole; its wreckage gives up alloy plating, and as often as not one of the
        // craft's working parts (no wreck holds them all)
        dropSelf(ModBlocks.SAUCER_HULL.get());
        dropSelf(ModBlocks.PHLOGISTON_BEACON.get()); // 13d
        // 20: a broken implant rig is scrap: Gray tech fragments
        add(ModBlocks.IMPLANT_RIG.get(), block -> LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(ModItems.GRAY_TECH_FRAGMENT.get()).apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3))))));
        add(ModBlocks.SAUCER_WRECKAGE.get(), block -> LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                        .add(LootItem.lootTableItem(ModItems.ALIEN_ALLOY_PLATING.get()).apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2)))))
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                        .add(EmptyLootItem.emptyItem().setWeight(6))
                        .add(LootItem.lootTableItem(ModItems.DAMAGED_GRAVITY_DRIVE.get()).setWeight(2))
                        .add(LootItem.lootTableItem(ModItems.BEAM_EMITTER.get()).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.LENS_ARRAY.get()).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.NAVIGATION_CORE.get()).setWeight(2))));
        // A crop circle's wheat gives the grain back, no seed
        add(ModBlocks.FLATTENED_WHEAT.get(), block -> createSingleItemTable(Items.WHEAT));
    }

    private LootTable.Builder phlogistonOre(Block block) {
        return createSilkTouchDispatchTable(block, applyExplosionDecay(block, LootItem.lootTableItem(ModItems.PHLOGISTON_SHARD.get())
                .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2)))
                .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }

    // Only validate our own blocks; every one of them must get a loot table in generate().
    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().<Block>map(holder -> holder.get()).toList();
    }
}
