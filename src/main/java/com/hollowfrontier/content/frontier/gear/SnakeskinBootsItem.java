package com.hollowfrontier.content.frontier.gear;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// Snakeskin Boots: the perks live in SnakeskinBoots and Rattlesnake; this just lists them in the tooltip.
public class SnakeskinBootsItem extends Item {
    public static final int PERKS = 3;

    public SnakeskinBootsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        for (int i = 1; i <= PERKS; i++) {
            builder.accept(Component.translatable("tooltip.hollowfrontier.snakeskin_boots." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
