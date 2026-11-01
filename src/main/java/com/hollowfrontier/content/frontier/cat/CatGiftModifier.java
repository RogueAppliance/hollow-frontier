package com.hollowfrontier.content.frontier.cat;

import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModLootModifiers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * A tamed cat's morning gift is sometimes frontier odds and ends instead of the vanilla string or feather: with
 * {@code chance}, the gift is rolled from {@code table} instead (a coin, caps, a brass nugget, snakeskin, rarely the
 * morning paper). One small item either way, so cats never become a coin farm.
 */
public class CatGiftModifier extends LootModifier {
    public static final ResourceKey<LootTable> FRONTIER_GIFTS = ResourceKey.create(Registries.LOOT_TABLE, HollowFrontier.id("gameplay/cat_morning_gift"));

    public static final MapCodec<CatGiftModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).and(i.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(m -> m.chance),
            ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(m -> m.table)
    )).apply(i, CatGiftModifier::new));

    private final float chance;
    private final ResourceKey<LootTable> table;

    public CatGiftModifier(Optional<Holder<LootItemCondition>> condition, int priority, float chance, ResourceKey<LootTable> table) {
        super(condition, priority);
        this.chance = chance;
        this.table = table;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() >= chance) {
            return generatedLoot;
        }
        ObjectArrayList<ItemStack> gift = new ObjectArrayList<>();
        // Raw, like neoforge:add_table: other modifiers still see the result through the cat's own table.
        context.getResolver().lookupOrThrow(Registries.LOOT_TABLE).get(table)
                .ifPresent(t -> t.value().getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), gift::add)));
        return gift.isEmpty() ? generatedLoot : gift;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.CAT_GIFT.get();
    }
}
