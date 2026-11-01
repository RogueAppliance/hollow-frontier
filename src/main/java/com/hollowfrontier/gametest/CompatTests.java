package com.hollowfrontier.gametest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.gson.JsonObject;
import com.hollowfrontier.compat.Compat;
import com.hollowfrontier.content.steam.CrushingRecipe;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModRecipes;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

// Optional compat (phase 14): the data JEI, Jade and Curios read, and Curios itself when it's installed (it is in dev runs).
final class CompatTests {
    private CompatTests() {}

    private static JsonObject lang(GameTestHelper h) {
        try (InputStream in = CompatTests.class.getResourceAsStream("/assets/hollowfrontier/lang/en_us.json")) {
            h.assertTrue(in != null, "no en_us.json on the classpath");
            return GsonHelper.parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Every JEI info page and Jade line has its text, and the Curios slot tags hold the wearable items. */
    static void compatTextAndTags(GameTestHelper h) {
        JsonObject lang = lang(h);
        List<String> missing = new ArrayList<>();
        for (Supplier<? extends Item> item : Compat.INFO_PAGES) {
            if (!lang.has(Compat.infoKey(item.get()))) missing.add(Compat.infoKey(item.get()));
        }
        for (String key : List.of("jei.hollowfrontier.crushing.steam", "config.jade.plugin_hollowfrontier.steam_machine",
                "config.jade.plugin_hollowfrontier.vehicle", "jade.hollowfrontier.boiler.burning", "jade.hollowfrontier.crusher.progress")) {
            if (!lang.has(key)) missing.add(key);
        }
        h.assertTrue(missing.isEmpty(), "no lang for " + missing);
        h.assertTrue(new ItemStack(ModItems.SUNSTONE_LENS.get()).is(ModTags.Items.CURIOS_CHARM), "the Sunstone Lens should be a charm");
        h.assertTrue(new ItemStack(ModItems.KINDLED_SIGIL.get()).is(ModTags.Items.CURIOS_NECKLACE), "the Kindled Sigil should be a necklace");
        h.succeed();
    }

    /** What JEI's crusher category is handed: each crushing recipe has something in and a stack out. */
    static void crushingRecipesShowInAndOut(GameTestHelper h) {
        var recipes = h.getLevel().recipeAccess().recipeMap().byType(ModRecipes.CRUSHING.get());
        h.assertTrue(recipes.size() >= 4, "expected the dust recipes, got " + recipes.size());
        for (RecipeHolder<CrushingRecipe> holder : recipes) {
            h.assertTrue(!holder.value().input().isEmpty() && holder.value().output().count() > 0, "empty crushing recipe " + holder.id());
        }
        h.succeed();
    }

    /** Worn in Curios slots, the lens lends night vision and the sigil shows the implanted, as they do held. */
    static void curiosWornArtifactsWork(GameTestHelper h) {
        if (!Compat.isLoaded(Compat.CURIOS)) {
            h.succeed(); // nothing to test without Curios
            return;
        }
        ServerPlayer player = TestPlayers.creative(h, new BlockPos(2, 1, 2));
        h.assertTrue(CuriosProbe.wear(player, "charm", new ItemStack(ModItems.SUNSTONE_LENS.get())), "players should have a charm slot");
        h.assertTrue(CuriosProbe.wear(player, "necklace", new ItemStack(ModItems.KINDLED_SIGIL.get())), "players should have a necklace slot");
        var outlaw = h.spawn(ModEntities.OUTLAW.get(), new BlockPos(5, 1, 5));
        outlaw.setNoAi(true);
        h.onEachTick(() -> {
            player.doTick(); // test players aren't ticked by a connection
            CuriosProbe.tickWorn(player);
        });
        h.succeedWhen(() -> {
            h.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "the worn lens should lend night vision ("
                    + CuriosProbe.describe(player, new ItemStack(ModItems.SUNSTONE_LENS.get())) + ")");
            h.assertTrue(outlaw.hasEffect(MobEffects.GLOWING), "the worn sigil should show the outlaw");
        });
    }
}
