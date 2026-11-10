package com.hollowfrontier.datagen;

import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ModChestLoot;

import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * What a brush turns up in the Hollow sites' suspicious sand and gravel (28): one find a block, as vanilla's digs. The
 * Hollow sherds come out of the ground here, with the odd relic among the bones and the broken brick.
 */
public class ModArchaeologyLootProvider implements LootTableSubProvider {
    private final LootTableSubProvider.Context context;

    public ModArchaeologyLootProvider(LootTableSubProvider.Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        context.accept(ModChestLoot.DIG_CRYSTAL_RUINS, one()
                .add(entry(ModItems.CRYSTAL_POTTERY_SHERD.get(), 4))
                .add(entry(ModItems.CRYSTAL_TABLET.get(), 1))
                .add(entry(Items.AMETHYST_SHARD, 6))
                .add(entry(ModItems.STARSTEEL_NUGGET.get(), 6))
                .add(entry(ModItems.PHLOGISTON_SHARD.get(), 2))
                .add(entry(Items.BRICK, 5)).wrap());
        context.accept(ModChestLoot.DIG_SYMMES, one()
                .add(entry(ModItems.SUN_POTTERY_SHERD.get(), 4))
                .add(entry(ModItems.IVORY_CARVING.get(), 1))
                .add(entry(ModItems.EXPEDITION_SEXTANT.get(), 1))
                .add(entry(Items.BONE, 6))
                .add(entry(Items.BRICK, 5))
                .add(entry(Items.GOLD_NUGGET, 4)).wrap());
        context.accept(ModChestLoot.DIG_TAR_PIT, one()
                .add(entry(ModItems.WATCHER_POTTERY_SHERD.get(), 3))
                .add(entry(ModItems.IVORY_CARVING.get(), 2))
                .add(entry(ModItems.MAMMOTH_TUSK.get(), 3))
                .add(entry(Items.BONE, 8))
                .add(entry(ModItems.TERROR_BIRD_TALON.get(), 3)).wrap());
        // 43d: silt in the Drowned Quarter's houses: the Wave sherd, and what a river port dropped
        context.accept(ModChestLoot.DIG_DROWNED_QUARTER, one()
                .add(entry(ModItems.WAVE_POTTERY_SHERD.get(), 4))
                .add(entry(ModItems.CRYSTAL_POTTERY_SHERD.get(), 1))
                .add(entry(ModItems.PILOTS_BELL.get(), 1))
                .add(entry(Items.CLAY_BALL, 6))
                .add(entry(Items.GOLD_NUGGET, 5))
                .add(entry(ModItems.LAMP_GEL.get(), 3)).wrap());
        context.accept(ModChestLoot.DIG_ZIGGURAT, one()
                .add(entry(ModItems.SUN_POTTERY_SHERD.get(), 3))
                .add(entry(ModItems.CRYSTAL_POTTERY_SHERD.get(), 3))
                .add(entry(ModItems.KINDLED_IDOL.get(), 1))
                .add(entry(Items.EMERALD, 3))
                .add(entry(Items.GOLD_NUGGET, 6)).wrap());
    }

    /** A one-roll pool, wrapped in a table by {@link Pool#wrap}. */
    private static Pool one() {
        return new Pool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)));
    }

    private record Pool(LootPool.Builder pool) {
        Pool add(UniformContainerBase.Builder<?> entry) {
            pool.add(entry);
            return this;
        }

        LootTable.Builder wrap() {
            return LootTable.lootTable().withPool(pool);
        }
    }

    private static UniformContainerBase.Builder<?> entry(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }
}
