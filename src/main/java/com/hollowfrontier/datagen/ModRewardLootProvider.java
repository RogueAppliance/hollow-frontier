package com.hollowfrontier.datagen;

import com.hollowfrontier.registry.ModItems;

import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

// Advancement rewards (ModAdvancements): a little something for the challenges, on top of their XP.
public class ModRewardLootProvider implements LootTableSubProvider {
    private final LootTableSubProvider.Context context;

    public ModRewardLootProvider(LootTableSubProvider.Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        context.accept(ModAdvancements.REWARD_RECOVERED, items(ModItems.GOLD_COIN.get(), 2));   // the bank's thanks
        context.accept(ModAdvancements.REWARD_BLACKOUT, items(ModItems.COWBOY_COFFEE.get(), 1)); // you'll want this
        context.accept(ModAdvancements.REWARD_GHOST_CAT, items(Items.COOKED_SALMON, 3));        // for its next supper
        context.accept(ModAdvancements.REWARD_LONG_HAUL, items(ModItems.GOLD_COIN.get(), 1));
    }

    private static LootTable.Builder items(ItemLike item, int count) {
        return LootTable.lootTable().withPool(LootPool.lootPool().add(LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(count)))));
    }
}
