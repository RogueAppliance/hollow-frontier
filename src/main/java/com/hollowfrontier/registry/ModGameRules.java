package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's gamerules ({@code /gamerule hollowfrontier:<name>}): in 26.3 a registry like any other. */
public final class ModGameRules {
    private ModGameRules() {}

    public static final DeferredRegister<GameRule<?>> GAME_RULES = DeferredRegister.create(Registries.GAME_RULE, HollowFrontier.MODID);

    /** 13c: whether a Prototype Saucer's tractor beam takes hold of players (creatures and items either way). */
    public static final DeferredHolder<GameRule<?>, GameRule<Boolean>> SAUCER_BEAMS_PLAYERS = bool("saucer_beams_players", GameRuleCategory.PLAYER, true);

    private static DeferredHolder<GameRule<?>, GameRule<Boolean>> bool(String name, GameRuleCategory category, boolean value) {
        return GAME_RULES.register(name, () -> new GameRule<>(category, GameRuleType.BOOL, BoolArgumentType.bool(),
                GameRuleTypeVisitor::visitBoolean, Codec.BOOL, on -> on ? 1 : 0, value, FeatureFlagSet.of()));
    }
}
