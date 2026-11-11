package com.hollowfrontier.datagen;

import java.util.concurrent.CompletableFuture;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModEntities;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.Tags;

/** Convention entity tags (30), so other mods' mob catchers, teleporters and boss checks leave ours alone. */
public class ModEntityTypeTagsProvider extends TagsProvider<EntityType<?>> {
    public ModEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ENTITY_TYPE, lookupProvider, HollowFrontier.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(Tags.EntityTypes.BOSSES).add(ModEntities.OUTLAW_BOSS.key(), ModEntities.SUN_ARCHON.key(), ModEntities.MOTHERSHIP.key());
        // Bosses and the guardians of their sites stay put: a boss in a jar or across the map breaks its fight
        tag(Tags.EntityTypes.TELEPORTING_NOT_SUPPORTED).addTag(Tags.EntityTypes.BOSSES)
                .add(ModEntities.CRYSTAL_GUARDIAN.key(), ModEntities.GRAY_WARDEN.key(), ModEntities.TOWN_TREE.key());
        // ...and so do the folk a town or a story keeps track of (their town, their errand, their disguise)
        tag(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED).addTag(Tags.EntityTypes.BOSSES)
                .add(ModEntities.CRYSTAL_GUARDIAN.key(), ModEntities.GRAY_WARDEN.key(), ModEntities.TOWNSFOLK.key(),
                        ModEntities.BARMAID.key(), ModEntities.LAWMAN.key(), ModEntities.KINDLED.key(),
                        ModEntities.MAN_IN_BLACK.key(), ModEntities.DISGUISED_GRAY.key(), ModEntities.TOWN_TREE.key());
    }
}
