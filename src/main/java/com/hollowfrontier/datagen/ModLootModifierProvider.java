package com.hollowfrontier.datagen;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.cat.CatGiftModifier;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

// data/hollowfrontier/loot_modifiers/: global loot modifiers.
public class ModLootModifierProvider extends GlobalLootModifierProvider {
    /** Share of a tamed cat's morning gifts that are frontier gifts instead of vanilla ones. */
    public static final float CAT_GIFT_CHANCE = 0.35F;

    public ModLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, HollowFrontier.MODID);
    }

    @Override
    protected void start() {
        add("cat_morning_gift", new CatGiftModifier(
                Optional.of(Holder.direct(LootTableIdCondition.builder(BuiltInLootTables.CAT_MORNING_GIFT.identifier()).build())),
                IGlobalLootModifier.DEFAULT_PRIORITY, CAT_GIFT_CHANCE, CatGiftModifier.FRONTIER_GIFTS));
    }
}
