package com.hollowfrontier.content.frontier.horse;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Fitted to a tamed horse by right-clicking it (see HorseGear). Speed is a fraction of base speed; jump is added flat. */
public class HorseshoeItem extends Item {
    private final int tier;
    private final double speedBonus;
    private final double jumpBonus;

    public HorseshoeItem(int tier, double speedBonus, double jumpBonus, Properties properties) {
        super(properties);
        this.tier = tier;
        this.speedBonus = speedBonus;
        this.jumpBonus = jumpBonus;
    }

    public int tier() {
        return tier;
    }

    public double speedBonus() {
        return speedBonus;
    }

    public double jumpBonus() {
        return jumpBonus;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.horseshoes", Math.round(speedBonus * 100)).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.hollowfrontier.horse_gear_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
