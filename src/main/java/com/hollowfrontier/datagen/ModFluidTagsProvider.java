package com.hollowfrontier.datagen;

import java.util.concurrent.CompletableFuture;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModFluids;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;

/** Steam is c:steam (30), so other mods' steam runs our machines and ours runs theirs; and it's a gas. */
public class ModFluidTagsProvider extends TagsProvider<Fluid> {
    public ModFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.FLUID, lookupProvider, HollowFrontier.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ModTags.Fluids.STEAM).add(ModFluids.STEAM.key(), ModFluids.FLOWING_STEAM.key());
        tag(Tags.Fluids.GASEOUS).add(ModFluids.STEAM.key(), ModFluids.FLOWING_STEAM.key());
    }
}
