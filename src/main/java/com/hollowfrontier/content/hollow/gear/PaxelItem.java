package com.hollowfrontier.content.hollow.gear;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The Starsteel Paxel (9c): pickaxe, axe and shovel in one, so the Hollow Earth's gear is a sword and this rather than four
 * more tools. It mines whatever any of the three mines ({@code hollowfrontier:mineable/paxel}); on use it strips, scrapes
 * and unwaxes as an axe does (its own block transformer) and, failing that, flattens paths as a shovel does.
 */
public class PaxelItem extends Item {
    public PaxelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult asAxe = super.useOn(context);
        if (asAxe != InteractionResult.PASS) {
            return asAxe;
        }
        return context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).get(BlockTransformers.SHOVEL)
                .map(shovel -> shovel.value().transformBlock(context, shovel)) // NeoForge's overload for multitools
                .orElse(InteractionResult.PASS);
    }
}
