package com.hollowfrontier.datagen;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hollowfrontier.HollowFrontier;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * data/hollowfrontier/curios/entities/player.json: gives players the charm and necklace slots our items go in
 * (CuriosCompat). Written as plain JSON so datagen doesn't need Curios; Curios ignores it when absent, and so does
 * everything else.
 */
public class CuriosSlotsProvider implements DataProvider {
    private final PackOutput.PathProvider entities;

    public CuriosSlotsProvider(PackOutput output) {
        this.entities = output.createPathProvider(PackOutput.Target.DATA_PACK, "curios/entities");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject json = new JsonObject();
        JsonArray who = new JsonArray();
        who.add("minecraft:player");
        JsonArray slots = new JsonArray();
        slots.add("charm");
        slots.add("necklace");
        json.add("entities", who);
        json.add("slots", slots);
        return DataProvider.saveStable(cache, json, entities.json(HollowFrontier.id("player")));
    }

    @Override
    public String getName() {
        return "Curios slots";
    }
}
