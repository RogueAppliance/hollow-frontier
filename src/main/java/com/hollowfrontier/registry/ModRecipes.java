package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.steam.CrushingRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    private ModRecipes() {}

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, HollowFrontier.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, HollowFrontier.MODID);
    public static final DeferredRegister<RecipeBookCategory> BOOK_CATEGORIES = DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, HollowFrontier.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushingRecipe>> CRUSHING = TYPES.register("crushing",
            id -> RecipeType.simple(id));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrushingRecipe>> CRUSHING_SERIALIZER = SERIALIZERS.register("crushing",
            () -> new RecipeSerializer<>(CrushingRecipe.MAP_CODEC, CrushingRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> CRUSHING_BOOK_CATEGORY = BOOK_CATEGORIES.register("crushing",
            RecipeBookCategory::new);
}
