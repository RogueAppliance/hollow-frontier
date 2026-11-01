package com.hollowfrontier.datagen;

import java.util.concurrent.CompletableFuture;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModDamageTypes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypeTagsProvider extends TagsProvider<DamageType> {
    public ModDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider, HollowFrontier.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Bullets count as projectiles: shields block them, projectile protection reduces them.
        tag(DamageTypeTags.IS_PROJECTILE).add(ModDamageTypes.BULLET);
    }
}
