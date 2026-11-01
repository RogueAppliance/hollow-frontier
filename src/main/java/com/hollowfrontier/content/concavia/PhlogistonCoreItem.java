package com.hollowfrontier.content.concavia;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Phlogiston Core (12b): the burning heart the Sun Archon carried, all the phlogiston the old kings gathered. It does nothing yet
 * on its own; it's what the Grays came for, and phase 13 makes it the key to their downfall (the Phlogiston Beacon).
 */
public class PhlogistonCoreItem extends Item {
    public PhlogistonCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.phlogiston_core").withStyle(ChatFormatting.GRAY));
    }
}
