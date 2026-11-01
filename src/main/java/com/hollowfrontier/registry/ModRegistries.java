package com.hollowfrontier.registry;

import net.neoforged.bus.api.IEventBus;

public final class ModRegistries {
    private ModRegistries() {}

    public static void register(IEventBus modEventBus) {
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModRecipes.BOOK_CATEGORIES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModDataSerializers.SERIALIZERS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModTabs.TABS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modEventBus);
        ModConditions.CONDITIONS.register(modEventBus);
        ModPlacements.PLACEMENTS.register(modEventBus);
        ModFeatures.FEATURE_TYPES.register(modEventBus);
        ModFeatures.STRUCTURE_TYPES.register(modEventBus);
        ModFeatures.STRUCTURE_PROCESSORS.register(modEventBus);
        ModFeatures.STRUCTURE_PIECES.register(modEventBus);
        ModCriteria.TRIGGERS.register(modEventBus);
        ModGameRules.GAME_RULES.register(modEventBus);
        com.hollowfrontier.gametest.ModGameTests.FUNCTIONS.register(modEventBus);
    }
}
