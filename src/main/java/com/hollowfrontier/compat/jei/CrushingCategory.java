package com.hollowfrontier.compat.jei;

import com.hollowfrontier.content.steam.CrusherBlockEntity;
import com.hollowfrontier.content.steam.CrushingRecipe;
import com.hollowfrontier.registry.ModItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The Steam Crusher's recipes: one item in, its dust out, for {@link CrusherBlockEntity#STEAM_PER_TICK} mB a tick. */
public class CrushingCategory extends AbstractRecipeCategory<RecipeHolder<CrushingRecipe>> {
    private static final int WIDTH = 82;
    private static final int HEIGHT = 34;

    public CrushingCategory(IRecipeHolderType<CrushingRecipe> type, IGuiHelper gui) {
        super(type, Component.translatable("container.hollowfrontier.crusher"), gui.createDrawableItemLike(ModItems.CRUSHER.get()), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrushingRecipe> holder, IFocusGroup focuses) {
        builder.addInputSlot(1, 1).setStandardSlotBackground().add(holder.value().input());
        builder.addOutputSlot(61, 1).setOutputSlotBackground().add(holder.value().output());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<CrushingRecipe> holder, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(CrusherBlockEntity.CRUSH_TIME).setPosition(26, 1);
        int steam = CrusherBlockEntity.CRUSH_TIME * CrusherBlockEntity.STEAM_PER_TICK;
        builder.addText(Component.translatable("jei.hollowfrontier.crushing.steam", steam), WIDTH, 10).setPosition(0, 22);
    }
}
