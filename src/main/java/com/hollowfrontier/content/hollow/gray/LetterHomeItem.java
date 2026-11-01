package com.hollowfrontier.content.hollow.gray;

import java.util.function.Consumer;

import com.hollowfrontier.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A Letter Home (20), from a captive freed out of a Gray pen: the address on the front, and what to do with it. */
public class LetterHomeItem extends Item {
    public LetterHomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        LetterHome home = stack.get(ModDataComponents.LETTER_HOME.get());
        if (home == null || home.anyTown()) {
            builder.accept(Component.translatable("tooltip.hollowfrontier.letter_home.any").withStyle(ChatFormatting.GRAY));
        } else {
            builder.accept(Component.translatable("tooltip.hollowfrontier.letter_home.to", home.x(), home.z(), com.hollowfrontier.worldgen.structure.TownNames.phrase(home.name())).withStyle(ChatFormatting.GRAY));
        }
        builder.accept(Component.translatable("tooltip.hollowfrontier.letter_home.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
