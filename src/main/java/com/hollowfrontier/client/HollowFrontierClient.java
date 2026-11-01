package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.compat.Compat;
import com.hollowfrontier.compat.iris.IrisCompat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

// Client-only entry point: renderers, screens, sky effects and key mappings get registered from here.
@Mod(value = HollowFrontier.MODID, dist = Dist.CLIENT)
public class HollowFrontierClient {
    public HollowFrontierClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(FMLClientSetupEvent.class, event -> {
            if (Compat.isLoaded(Compat.IRIS)) {
                event.enqueueWork(IrisCompat::assignPipelines);
            }
        });
    }
}
