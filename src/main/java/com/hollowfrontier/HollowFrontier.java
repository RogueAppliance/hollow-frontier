package com.hollowfrontier;

import org.slf4j.Logger;

import com.hollowfrontier.content.frontier.brass.BrassConfig;
import com.hollowfrontier.content.frontier.raid.RaidConfig;
import com.hollowfrontier.content.ufo.UfoConfig;
import com.hollowfrontier.registry.ModRegistries;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(HollowFrontier.MODID)
public class HollowFrontier {
    public static final String MODID = "hollowfrontier";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HollowFrontier(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistries.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SYNCED, RaidConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.STARTUP, BrassConfig.SPEC);
        // A second per-world config needs a file name of its own
        modContainer.registerConfig(ModConfig.Type.SYNCED, UfoConfig.SPEC, MODID + "-ufo-server.toml");
        modContainer.registerConfig(ModConfig.Type.SYNCED, com.hollowfrontier.content.frontier.saloon.SaloonConfig.SPEC, MODID + "-saloon-server.toml");
        modContainer.registerConfig(ModConfig.Type.SYNCED, com.hollowfrontier.content.frontier.weather.DustDevilConfig.SPEC, MODID + "-dust-devils-server.toml");
        modContainer.registerConfig(ModConfig.Type.SYNCED, com.hollowfrontier.content.frontier.mystery.MysteryConfig.SPEC, MODID + "-mysteries-server.toml");
        modContainer.registerConfig(ModConfig.Type.SYNCED, com.hollowfrontier.content.frontier.town.TownConfig.SPEC, MODID + "-towns-server.toml");
        modContainer.registerConfig(ModConfig.Type.SYNCED, com.hollowfrontier.content.frontier.season.SeasonConfig.SPEC, MODID + "-season-server.toml");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
