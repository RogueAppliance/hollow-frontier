package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModFluids;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

/**
 * How steam looks wherever a fluid is drawn: Jade's tank bars, JEI, any screen that shows a fluid. Steam never sits in
 * the world, but without a fluid model its sprite is the missing texture. Vanilla's water sprites (referenced, not
 * copied), tinted a pale steam-white instead of the biome's blue.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class SteamFluidModel {
    private SteamFluidModel() {}

    public static final int TINT = 0xFFE6EAEE;

    private static final FluidModel.Unbaked STEAM = new FluidModel.Unbaked(
            new Material(Identifier.withDefaultNamespace("block/water_still")),
            new Material(Identifier.withDefaultNamespace("block/water_flow")),
            null,
            FluidTintSources.constant(TINT));

    @SubscribeEvent
    static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(STEAM, ModFluids.STEAM, ModFluids.FLOWING_STEAM);
    }
}
