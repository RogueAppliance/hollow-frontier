package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.saloon.TipsyEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    private ModMobEffects() {}

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, HollowFrontier.MODID);

    /** Saloon liquor: stacks per drink (amplifier = level - 1), spoils gun aim, brings on nausea from level III. */
    public static final DeferredHolder<MobEffect, TipsyEffect> TIPSY = MOB_EFFECTS.register("tipsy", TipsyEffect::new);
}
