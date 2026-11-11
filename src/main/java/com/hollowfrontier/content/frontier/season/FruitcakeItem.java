package com.hollowfrontier.content.frontier.season;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A Fruitcake (35b), out of a Christmas Parcel. Food: it takes five seconds to eat, fills you right up and slows you for
 * half a minute (its food and consumable components, in ModItems). No townsperson has an offer for one, and it says so.
 */
public class FruitcakeItem extends Item {
    public FruitcakeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.fruitcake").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
