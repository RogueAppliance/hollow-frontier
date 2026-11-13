package com.hollowfrontier.datagen;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.cat.CatCoats;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModEnchantments;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.HollowEarth;
import com.hollowfrontier.worldgen.HollowFeatures;
import com.hollowfrontier.worldgen.HollowMaterialRules;
import com.hollowfrontier.worldgen.HollowTerrain;
import com.hollowfrontier.worldgen.ModSpawns;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// In 26.x recipes, loot tables, advancements and worldgen are datapack registries, so they are gathered
// through GatherDataRegistryEntriesEvent. Client assets, lang and tags still use GatherDataEvent.
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class ModDataGen {
    private ModDataGen() {}

    @SubscribeEvent
    static void gatherRegistryEntries(GatherDataRegistryEntriesEvent event) {
        // One bootstrap per registry: combine contributors for the same registry inside a single lambda.
        // The recipe bootstrap owns the advancement registry too (recipe unlocks), so our advancement tabs go in with it.
        event.recipe((recipes, advancements) -> {
                    ModAdvancements.bootstrap(advancements);
                    return new ModRecipeProvider(recipes, advancements);
                })
                .lootTable(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(ModEntityLootProvider::new, LootContextParamSets.ENTITY),
                        new LootTableProvider.SubProviderEntry(ModChestLootProvider::new, LootContextParamSets.CHEST),
                        new LootTableProvider.SubProviderEntry(ModArchaeologyLootProvider::new, LootContextParamSets.ARCHAEOLOGY),
                        new LootTableProvider.SubProviderEntry(ModGiftLootProvider::new, LootContextParamSets.GIFT),
                        new LootTableProvider.SubProviderEntry(ModRewardLootProvider::new, LootContextParamSets.ADVANCEMENT_REWARD))
                .add(Registries.TEMPLATE_POOL, ModStructures::bootstrapPools)
                .add(Registries.STRUCTURE, ModStructures::bootstrapStructures)
                .add(Registries.STRUCTURE_SET, ModStructures::bootstrapSets)
                .add(Registries.FEATURE, context -> {
                    HollowFeatures.bootstrapFeatures(context);
                })
                .add(Registries.PLACED_FEATURE, context -> {
                    HollowFeatures.bootstrapPlacements(context);
                })
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                    ModSpawns.bootstrapBiomeModifiers(context);
                })
                .add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap)
                .add(Registries.ENCHANTMENT, ModEnchantments::bootstrap)
                .add(Registries.CAT_VARIANT, CatCoats::bootstrap)
                .add(Registries.DECORATED_POT_PATTERN, com.hollowfrontier.content.hollow.HollowPatterns::bootstrapPots) // 28
                .add(Registries.TRIM_PATTERN, com.hollowfrontier.content.hollow.HollowPatterns::bootstrapTrims)
                // The Hollow Earth (phase 9a): its own noises, terrain, material rules, biomes and dimension
                .add(Registries.NOISE, HollowTerrain::bootstrapNoises)
                .add(Registries.DENSITY_FUNCTION, HollowTerrain::bootstrapFunctions)
                .add(Registries.MATERIAL_RULE, HollowMaterialRules::bootstrap)
                .add(Registries.NOISE_SETTINGS, HollowEarth::bootstrapNoiseSettings)
                // ...and the Grays' ship (50): a world with nothing in it but what Taken sets there
                .add(Registries.DIMENSION_TYPE, context -> {
                    HollowEarth.bootstrapDimensionTypes(context);
                    com.hollowfrontier.worldgen.Aboard.bootstrapDimensionTypes(context);
                })
                .add(Registries.BIOME, context -> {
                    HollowBiomes.bootstrap(context);
                    context.register(com.hollowfrontier.worldgen.Aboard.BIOME, com.hollowfrontier.worldgen.Aboard.biome());
                })
                .add(Registries.LEVEL_STEM, context -> {
                    HollowEarth.bootstrapStems(context);
                    com.hollowfrontier.worldgen.Aboard.bootstrapStems(context);
                });
    }

    @SubscribeEvent
    static void gatherClientData(GatherDataEvent.Client event) {
        event.createProvider(ModModelProvider::new);
        event.createProvider(ModLanguageProvider::new);
        event.createBlockAndItemTags(ModBlockTagsProvider::new, ModItemTagsProvider::new);
        event.createProvider(ModDamageTypeTagsProvider::new);
        event.createProvider(ModEntityTypeTagsProvider::new);
        event.createProvider(ModFluidTagsProvider::new);
        event.createProvider(ModEnchantmentTagsProvider::new);
        event.createProvider(ModEquipmentAssetProvider::new);
        event.createProvider(ModSoundDefinitionsProvider::new);
        event.createProvider(ModParticleDescriptionProvider::new);
        event.createProvider(ModBiomeTagsProvider::new);
        event.createProvider(ModStructureTagsProvider::new);
        event.createProvider(StructureNbtProvider::new);
        event.createProvider(ModLootModifierProvider::new);
        event.createProvider(CuriosSlotsProvider::new);
    }
}
