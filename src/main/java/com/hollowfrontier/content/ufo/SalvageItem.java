package com.hollowfrontier.content.ufo;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Salvage from a crashed saucer (13a): alien alloy plating, a damaged gravity drive, a beam emitter, a lens array, a
 * navigation core. They do nothing yet: 13b repairs the drive, and 13c fits them all to a craft of the player's own. Each
 * says what it is ({@code tooltip.hollowfrontier.<item>}).
 */
public class SalvageItem extends Item {
    public SalvageItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier." + BuiltInRegistries.ITEM.getKey(this).getPath()).withStyle(ChatFormatting.GRAY));
    }
}
