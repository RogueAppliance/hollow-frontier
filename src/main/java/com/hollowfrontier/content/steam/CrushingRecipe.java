package com.hollowfrontier.content.steam;

import com.hollowfrontier.registry.ModRecipes;
import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/** Steam Crusher recipe: one input item -> result stack (e.g. raw ore -> 2 dust). Same JSON shape as stonecutting. */
public class CrushingRecipe extends SingleItemRecipe {
    public static final MapCodec<CrushingRecipe> MAP_CODEC = simpleMapCodec(CrushingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = simpleStreamCodec(CrushingRecipe::new);

    public CrushingRecipe(CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result) {
        super(commonInfo, ingredient, result);
    }

    /** What it crushes to (for recipe viewers). */
    public ItemStackTemplate output() {
        return result();
    }

    @Override
    public RecipeSerializer<CrushingRecipe> getSerializer() {
        return ModRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<CrushingRecipe> getType() {
        return ModRecipes.CRUSHING.get();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModRecipes.CRUSHING_BOOK_CATEGORY.get();
    }
}
