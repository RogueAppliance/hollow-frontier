package com.hollowfrontier.content.steam;

import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shift-click for machine menus laid out as [machine slots..., player inventory (36)]. */
final class MenuUtil {
    private MenuUtil() {}

    @FunctionalInterface
    interface Mover {
        boolean move(ItemStack stack, int start, int end, boolean backwards);
    }

    static ItemStack quickMove(AbstractContainerMenu menu, List<Slot> slots, Player player, int index, int machineSlots, Mover mover) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int total = slots.size();
        if (index < machineSlots) {
            if (!mover.move(stack, machineSlots, total, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!mover.move(stack, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
