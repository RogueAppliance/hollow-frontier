package com.hollowfrontier.content.frontier.bounty;

import java.util.function.Consumer;

import com.hollowfrontier.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Wanted Poster (poster = true) or Outlaw Badge; both carry a BOUNTY component and show it in the tooltip. */
public class BountyItem extends Item {
    private final boolean poster;

    public BountyItem(boolean poster, Properties properties) {
        super(properties);
        this.poster = poster;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        BountyData bounty = stack.get(ModDataComponents.BOUNTY.get());
        if (bounty == null) {
            builder.accept(Component.translatable("tooltip.hollowfrontier.bounty.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        builder.accept(Component.translatable("tooltip.hollowfrontier.bounty.target", bounty.target()).withStyle(ChatFormatting.GOLD));
        if (poster) {
            builder.accept(Component.translatable("tooltip.hollowfrontier.bounty.location", bounty.x(), bounty.z()).withStyle(ChatFormatting.GRAY));
            if (bounty.captive()) {
                builder.accept(Component.translatable("tooltip.hollowfrontier.bounty.captive").withStyle(ChatFormatting.DARK_AQUA));
            } else if (bounty.spawned()) {
                builder.accept(Component.translatable("tooltip.hollowfrontier.bounty.spotted").withStyle(ChatFormatting.RED));
            } else if (com.hollowfrontier.content.frontier.duel.Duels.callsOut(bounty)) {
                // 51: this one would sooner settle it in the street
                builder.accept(Component.translatable(com.hollowfrontier.content.frontier.duel.DuelText.POSTER_LINE).withStyle(ChatFormatting.GOLD));
            }
        }
        builder.accept(Component.translatable("tooltip.hollowfrontier.bounty.reward", bounty.reward()).withStyle(ChatFormatting.YELLOW));
        builder.accept(Component.translatable(poster ? "tooltip.hollowfrontier.bounty.poster_hint" : "tooltip.hollowfrontier.bounty.badge_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
