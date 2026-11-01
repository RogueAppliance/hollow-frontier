package com.hollowfrontier.datagen;

import com.hollowfrontier.content.frontier.cat.CatGiftModifier;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

// Gift tables: a tamed cat's frontier morning gifts (CatGiftModifier swaps these in for some vanilla gifts).
public class ModGiftLootProvider implements LootTableSubProvider {
    private final LootTableSubProvider.Context context;

    public ModGiftLootProvider(LootTableSubProvider.Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        // One small thing: a coin, a few caps, a nugget, a snakeskin, or now and then the morning paper.
        context.accept(CatGiftModifier.FRONTIER_GIFTS, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .add(item(ModItems.COPPER_COIN.get(), 1, 1, 30))
                .add(item(ModItems.PERCUSSION_CAP.get(), 2, 4, 25))
                .add(item(ModItems.BRASS_NUGGET.get(), 1, 2, 25))
                .add(item(ModItems.SNAKESKIN.get(), 1, 1, 15))
                .add(item(ModItems.FRONTIER_GAZETTE.get(), 1, 1, 5))));
    }

    private static UniformContainerBase.Builder<?> item(ItemLike item, int min, int max, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight).apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
    }
}
