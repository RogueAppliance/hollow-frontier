package com.hollowfrontier.content.frontier.brass;

import java.util.Collection;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModTags;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * {@code hollowfrontier:brass_alloying}: gates the crafting-table brass recipe, so in a pack with another brass mod (Create's
 * mixer, say) we don't undercut that mod's way of making it. See {@link BrassConfig}.
 */
public record BrassAlloyingCondition() implements ICondition {
    public static final BrassAlloyingCondition INSTANCE = new BrassAlloyingCondition();
    public static final MapCodec<BrassAlloyingCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(IContext context) {
        return allowed(BrassConfig.alloying(), context.getTag(ModTags.Items.INGOTS_BRASS));
    }

    /** Whether alloying is on, given the setting and what's in c:ingots/brass this reload. */
    public static boolean allowed(BrassConfig.Alloying setting, Collection<Holder<Item>> brassIngots) {
        return switch (setting) {
            case ALWAYS -> true;
            case NEVER -> false;
            case AUTO -> brassIngots.stream().allMatch(BrassAlloyingCondition::ours);
        };
    }

    private static boolean ours(Holder<Item> item) {
        return item.unwrapKey().map(key -> key.identifier().getNamespace().equals(HollowFrontier.MODID)).orElse(true);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
