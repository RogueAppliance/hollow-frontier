package com.hollowfrontier.content.frontier.saloon;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** A bottled saloon drink (sold by the barmaid). Drinking leaves the glass bottle (usingConvertsTo). */
public class DrinkItem extends Item {
    private final Drink drink;

    public DrinkItem(Drink drink, Properties properties) {
        super(properties);
        this.drink = drink;
    }

    public Drink drink() {
        return drink;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            drink.drink(entity);
            if (drink == Drink.MOONSHINE && entity instanceof net.minecraft.server.level.ServerPlayer player) {
                Moonshine.drunk(player); // 21b: it may show them something
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        for (Drink.Dose dose : drink.effects()) {
            Component name = Component.translatable(dose.effect().value().getDescriptionId());
            if (dose.amplifier() > 0) {
                name = Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + dose.amplifier()));
            }
            builder.accept(Component.translatable("potion.withDuration", name, duration(dose.seconds())).withStyle(ChatFormatting.BLUE));
        }
        if (drink.gamble()) {
            builder.accept(Component.translatable("tooltip.hollowfrontier.drink.gamble").withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (drink.tipsy() > 0) {
            builder.accept(Component.translatable("tooltip.hollowfrontier.drink.tipsy", drink.tipsy()).withStyle(ChatFormatting.GOLD));
        } else if (drink.tipsy() < 0) {
            builder.accept(Component.translatable(drink.tipsy() <= -TipsyEffect.MAX_AMPLIFIER - 1
                    ? "tooltip.hollowfrontier.drink.sober_all" : "tooltip.hollowfrontier.drink.sober", -drink.tipsy()).withStyle(ChatFormatting.GREEN));
        }
    }

    private static String duration(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
