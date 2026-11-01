package com.hollowfrontier.compat;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.steam.CrushingRecipe;
import com.hollowfrontier.registry.ModRecipes;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

/** The crushing recipes the server sent (see {@link Compat#onDatapackSync}), kept for JEI on the client. */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class SyncedRecipes {
    private static List<RecipeHolder<CrushingRecipe>> crushing = List.of();

    private SyncedRecipes() {}

    public static List<RecipeHolder<CrushingRecipe>> crushing() {
        return crushing;
    }

    // Ahead of JEI, which starts up (and asks its plugins for recipes) from this same event
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onRecipesReceived(RecipesReceivedEvent event) {
        if (event.getRecipeTypes().contains(ModRecipes.CRUSHING.get())) {
            crushing = List.copyOf(event.getRecipeMap().byType(ModRecipes.CRUSHING.get()));
        }
    }
}
