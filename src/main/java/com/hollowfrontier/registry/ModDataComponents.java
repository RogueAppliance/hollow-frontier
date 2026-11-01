package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.ufo.SaucerFittings;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HollowFrontier.MODID);

    // Rounds currently loaded in a gun.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LOADED_AMMO =
            DATA_COMPONENTS.registerComponentType("loaded_ammo", builder -> builder
                    .persistent(Codec.intRange(0, 64))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    // Game time the gun started loading its current chamber; absent when not reloading. Drives the reload animation.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RELOAD_START =
            DATA_COMPONENTS.registerComponentType("reload_start", builder -> builder
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    // Game time of the gun's last shot. Drives the fire (hammer + recoil) animation.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LAST_FIRED =
            DATA_COMPONENTS.registerComponentType("last_fired", builder -> builder
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    // Steam (mB) held by a Steam Canister.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STEAM =
            DATA_COMPONENTS.registerComponentType("steam", builder -> builder
                    .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    // 31a: the coal (burn ticks) a folded airship still has in its firebox; its steam goes on STEAM.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AIRSHIP_FUEL =
            DATA_COMPONENTS.registerComponentType("airship_fuel", builder -> builder
                    .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    // Bounty on a Wanted Poster / Outlaw Badge.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BountyData>> BOUNTY =
            DATA_COMPONENTS.registerComponentType("bounty", builder -> builder
                    .persistent(BountyData.CODEC)
                    .networkSynchronized(BountyData.STREAM_CODEC));

    // 13c: a folded Prototype Saucer's fittings, and the phlogiston it holds (absent on a new one: it comes primed)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SaucerFittings>> SAUCER_FITTINGS =
            DATA_COMPONENTS.registerComponentType("saucer_fittings", builder -> builder
                    .persistent(SaucerFittings.CODEC)
                    .networkSynchronized(SaucerFittings.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PHLOGISTON_CHARGE =
            DATA_COMPONENTS.registerComponentType("phlogiston_charge", builder -> builder
                    .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    // 20: where a Letter Home from a freed captive is going
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.hollowfrontier.content.hollow.gray.LetterHome>> LETTER_HOME =
            DATA_COMPONENTS.registerComponentType("letter_home", builder -> builder
                    .persistent(com.hollowfrontier.content.hollow.gray.LetterHome.CODEC)
                    .networkSynchronized(com.hollowfrontier.content.hollow.gray.LetterHome.STREAM_CODEC));

    // 32b: where a Polar Compass last searched for a gateway, so it searches again only after a long way
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.GlobalPos>> BEARING_FROM =
            DATA_COMPONENTS.registerComponentType("bearing_from", builder -> builder
                    .persistent(net.minecraft.core.GlobalPos.CODEC)
                    .networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC));

    // 32: game time a Polar Resonator last woke a ring. Drives its ring animation (the fork shivers, the needle swings).
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RESONATED =
            DATA_COMPONENTS.registerComponentType("resonated", builder -> builder
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    // 32d: where the Survey Letter's gateway is (the nearest to the post office it was collected from)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.GlobalPos>> SURVEY_GATEWAY =
            DATA_COMPONENTS.registerComponentType("survey_gateway", builder -> builder
                    .persistent(net.minecraft.core.GlobalPos.CODEC)
                    .networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC));
}
