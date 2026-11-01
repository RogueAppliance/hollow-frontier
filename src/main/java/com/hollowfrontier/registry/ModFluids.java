package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Steam: a gas fluid that only lives in tanks and pipes (no world block, no bucket).
 * Moved through NeoForge's fluid capability, so other mods' tanks/pipes can carry it too.
 */
public final class ModFluids {
    private ModFluids() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, HollowFrontier.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, HollowFrontier.MODID);

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = FLUID_TYPES.register("steam", () -> new FluidType(
            FluidType.Properties.create()
                    .descriptionId("fluid_type.hollowfrontier.steam")
                    .density(-1000)
                    .viscosity(200)
                    .temperature(400)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STEAM = FLUIDS.register("steam",
            () -> new BaseFlowingFluid.Source(steamProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_STEAM = FLUIDS.register("flowing_steam",
            () -> new BaseFlowingFluid.Flowing(steamProperties()));

    private static BaseFlowingFluid.Properties steamProperties() {
        return new BaseFlowingFluid.Properties(STEAM_TYPE, STEAM, FLOWING_STEAM);
    }

    public static FluidResource steam() {
        return FluidResource.of(STEAM.get());
    }

    /** Ours or any other mod's steam (c:steam, 30). */
    public static boolean isSteam(Fluid fluid) {
        return fluid.is(ModTags.Fluids.STEAM);
    }

    /** Draws up to {@code amount} mB of steam from a handler: ours first, then any other mod's steam it holds. */
    public static int extractSteam(ResourceHandler<FluidResource> handler, int amount, TransactionContext transaction) {
        FluidResource ours = steam();
        int got = handler.extract(ours, amount, transaction);
        for (int i = 0; i < handler.size() && got < amount; i++) {
            FluidResource held = handler.getResource(i);
            if (!held.isEmpty() && !held.equals(ours) && isSteam(held.getFluid())) {
                got += handler.extract(i, held, amount - got, transaction);
            }
        }
        return got;
    }
}
