package com.hollowfrontier.content.frontier.cat;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Something for your cat to wear (Bandana, Brass Star): right-click your own tamed cat to put it on, sneak-right-click with
 * an empty hand to take it back (TownCats). Its tooltip says so, plus what the item adds.
 */
public class CatAccessoryItem extends Item {
    private final String id;

    public CatAccessoryItem(String id, Properties properties) {
        super(properties);
        this.id = id;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier." + id).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.hollowfrontier.cat_accessory").withStyle(ChatFormatting.DARK_GRAY));
    }
}
