package com.hollowfrontier.registry;

import java.util.function.Supplier;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.brass.BrassAlloyingCondition;
import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Datapack load conditions (the "neoforge:conditions" list on recipes and other data).
public final class ModConditions {
    private ModConditions() {}

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, HollowFrontier.MODID);

    public static final Supplier<MapCodec<BrassAlloyingCondition>> BRASS_ALLOYING =
            CONDITIONS.register("brass_alloying", () -> BrassAlloyingCondition.CODEC);
}
