package com.hollowfrontier.content.frontier.bank;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * One player's safe deposit box in one town's bank. The box is the saved state itself, not a copy of it: TownBanks
 * holds exactly one per (town, player), every menu on it is a view of the same object, and each change marks the bank
 * dirty. So two windows can't duplicate items, and a raid that empties it while it's open shows in the open window.
 * Remembers the day of the last visit (for interest) and coins owed back that didn't fit when they were returned.
 */
public class BankBox extends SimpleContainer {
    public static final int SIZE = 18;
    public static final Codec<BankBox> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemContainerContents.CODEC.fieldOf("items").forGetter(b -> ItemContainerContents.fromItems(b.getItems())),
            Codec.LONG.fieldOf("last_day").forGetter(b -> b.lastDay),
            Codec.INT.optionalFieldOf("owed", 0).forGetter(b -> b.owed)).apply(i, BankBox::new));

    long lastDay;
    int owed;
    @Nullable TownBanks bank;
    // The banker who opened it, while a window is open: walk away from him and it closes.
    @Nullable Entity banker;

    BankBox(long lastDay) {
        super(SIZE);
        this.lastDay = lastDay;
    }

    private BankBox(ItemContainerContents items, long lastDay, int owed) {
        this(lastDay);
        this.owed = owed;
        NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        items.copyInto(list);
        for (int slot = 0; slot < SIZE; slot++) {
            getItems().set(slot, list.get(slot));
        }
    }

    public long lastDay() {
        return lastDay;
    }

    public int owed() {
        return owed;
    }

    /** What the coins in the box are worth, in copper (a gold coin is 9). */
    public int worth() {
        return countItem(ModItems.COPPER_COIN.get()) + 9 * countItem(ModItems.GOLD_COIN.get());
    }

    /** Puts coins worth copper in (gold where it can); returns what didn't fit, in copper. */
    int deposit(int copper) {
        int left = add(ModItems.GOLD_COIN.get(), copper / 9) * 9 + add(ModItems.COPPER_COIN.get(), copper % 9);
        setChanged();
        return left;
    }

    private int add(Item coin, int count) {
        return count <= 0 ? 0 : addItem(new ItemStack(coin, count)).getCount();
    }

    /**
     * Takes a share of the coins (by worth, rounded down): gold first, then copper for the rest. If the copper runs
     * short the gang takes a little less rather than make change. Returns what was taken, in copper.
     */
    int rob(double share) {
        int gold = countItem(ModItems.GOLD_COIN.get());
        int copper = countItem(ModItems.COPPER_COIN.get());
        int target = (int) Math.floor((gold * 9 + copper) * share);
        int takeGold = Math.min(gold, target / 9);
        int takeCopper = Math.min(copper, target - takeGold * 9);
        remove(ModItems.GOLD_COIN.get(), takeGold);
        remove(ModItems.COPPER_COIN.get(), takeCopper);
        setChanged();
        return takeGold * 9 + takeCopper;
    }

    private void remove(Item coin, int count) {
        for (int slot = 0; slot < SIZE && count > 0; slot++) {
            ItemStack stack = getItem(slot);
            if (stack.is(coin)) {
                int take = Math.min(count, stack.getCount());
                stack.shrink(take);
                count -= take;
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (bank != null) {
            bank.setDirty();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return banker == null || banker.isAlive() && player.distanceToSqr(banker) < 64.0;
    }
}
