package com.hollowfrontier.content.frontier.bounty;

import com.hollowfrontier.registry.ModItems;

import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The coins a player carries, counted in copper (a gold coin is worth 9). Paying takes every coin and hands back the
 * change in gold first, so a purse stays tidy; what won't fit drops at the player's feet. Fines and the card table use it.
 */
public final class Purse {
    private Purse() {}

    public static final int GOLD = 9;

    /** The player's coins in copper. */
    public static int count(Player player) {
        Inventory inventory = player.getInventory();
        return inventory.countItem(ModItems.COPPER_COIN.get()) + GOLD * inventory.countItem(ModItems.GOLD_COIN.get());
    }

    /** Takes {@code amount} copper's worth, if the player has it. */
    public static boolean pay(Player player, int amount) {
        int purse = count(player);
        if (purse < amount) {
            return false;
        }
        if (amount <= 0) {
            return true;
        }
        Item copper = ModItems.COPPER_COIN.get();
        Item gold = ModItems.GOLD_COIN.get();
        player.getInventory().clearOrCountMatchingItems(stack -> stack.is(copper) || stack.is(gold), false, -1, player.inventoryMenu.getCraftSlots());
        give(player, purse - amount);
        return true;
    }

    /** Gives {@code amount} copper's worth, in gold coins and copper. */
    public static void give(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        add(player, new ItemStack(ModItems.GOLD_COIN.get(), amount / GOLD));
        add(player, new ItemStack(ModItems.COPPER_COIN.get(), amount % GOLD));
    }

    private static void add(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }
}
