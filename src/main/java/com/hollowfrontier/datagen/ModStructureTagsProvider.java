package com.hollowfrontier.datagen;

import java.util.concurrent.CompletableFuture;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.levelgen.structure.Structure;

public class ModStructureTagsProvider extends TagsProvider<Structure> {
    public ModStructureTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.STRUCTURE, lookupProvider, HollowFrontier.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Where a flushed Pale Raven flies (9b2/9b3)
        tag(ModStructures.KINDLED_SITES).add(ModStructures.KINDLED_SITE, ModStructures.CRYSTAL_RUINS);
        tag(ModStructures.SUN_TEMPLES).add(ModStructures.SUN_TEMPLE);
        tag(ModStructures.CRASH_SITES).add(ModStructures.CRASH_SITE, ModStructures.HOLLOW_CRASH_SITE);
    }
}
