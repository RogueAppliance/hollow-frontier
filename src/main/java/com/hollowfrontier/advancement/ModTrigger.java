package com.hollowfrontier.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * One criterion trigger type per mod event (raid defended, ghost cat tamed, handcar driven...), all sharing this class.
 * The game code fires it where the event happens; an advancement may ask for a value of at least {@code min} (stars,
 * blocks driven), and optionally the usual player predicate. Registered in {@link com.hollowfrontier.registry.ModCriteria}.
 */
public class ModTrigger extends SimpleCriterionTrigger<ModTrigger.Instance> {
    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, 1);
    }

    public void trigger(ServerPlayer player, int value) {
        trigger(player, instance -> instance.min().map(min -> value >= min).orElse(true));
    }

    public Criterion<Instance> criterion() {
        return createCriterion(new Instance(Optional.empty(), Optional.empty()));
    }

    public Criterion<Instance> atLeast(int min) {
        return createCriterion(new Instance(Optional.empty(), Optional.of(min)));
    }

    public record Instance(Optional<Holder<LootItemCondition>> player, Optional<Integer> min) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                        LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                        Codec.INT.optionalFieldOf("min").forGetter(Instance::min))
                .apply(i, Instance::new));
    }
}
