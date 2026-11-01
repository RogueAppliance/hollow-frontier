package com.hollowfrontier.compat.jei;

import java.util.function.Supplier;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.compat.Compat;
import com.hollowfrontier.compat.SyncedRecipes;
import com.hollowfrontier.content.steam.CrushingRecipe;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * JEI (optional): a Steam Crusher category, and info pages ({@link Compat#INFO_PAGES}) for what no recipe shows: how
 * steam is made and moved, and where the things only found in the world come from. Only loaded by JEI.
 */
@JeiPlugin
public class HollowJeiPlugin implements IModPlugin {
    private static final Supplier<IRecipeHolderType<CrushingRecipe>> CRUSHING = IRecipeHolderType.createDeferred(ModRecipes.CRUSHING);

    @Override
    public Identifier getPluginUid() {
        return HollowFrontier.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CrushingCategory(CRUSHING.get(), registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CRUSHING.get(), SyncedRecipes.crushing());
        for (Supplier<? extends Item> item : Compat.INFO_PAGES) {
            registration.addIngredientInfo(item.get(), Component.translatable(Compat.infoKey(item.get())));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(CRUSHING.get(), ModItems.CRUSHER.get());
    }
}
