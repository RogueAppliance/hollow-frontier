package com.hollowfrontier.registry;

import java.util.function.Supplier;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.cat.CatGiftModifier;
import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Global loot modifier types; the modifiers themselves are datagen'd into data/hollowfrontier/loot_modifiers/.
public final class ModLootModifiers {
    private ModLootModifiers() {}

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, HollowFrontier.MODID);

    public static final Supplier<MapCodec<CatGiftModifier>> CAT_GIFT = LOOT_MODIFIERS.register("cat_gift", () -> CatGiftModifier.CODEC);
}
