package com.hollowfrontier.content.hollow;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A relic of the Hollow Earth (28): a find from one of its sites, of no use but to look at (set it on a shelf, frame it)
 * or to sell: a town's prospector buys them for gold coins (Townsfolk), each at its {@link #price}. Its tooltip says
 * where it came from, and that it sells.
 */
public class RelicItem extends Item {
    private final int price;

    public RelicItem(Properties properties, int price) {
        super(properties);
        this.price = price;
    }

    /** What a town's prospector pays for one, in gold coins. */
    public int price() {
        return price;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        builder.accept(Component.translatable("tooltip.hollowfrontier.relic", price).withStyle(ChatFormatting.GOLD));
    }
}
