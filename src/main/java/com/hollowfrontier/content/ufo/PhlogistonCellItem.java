package com.hollowfrontier.content.ufo;

import java.util.function.Consumer;

import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A Phlogiston Cell (13b): a brass-capped glass cell with a phlogiston shard burning in it, {@link #CHARGES} charges to a cell, worn
 * down as its durability. The phlogiston guns draw on whatever cells their holder carries ({@link #draw}); a cell drawn dry
 * turns into an Empty Phlogiston Cell, which a phlogiston shard charges again at the crafting table.
 */
public class PhlogistonCellItem extends Item {
    public static final int CHARGES = 64;

    public PhlogistonCellItem(Properties properties) {
        super(properties);
    }

    /** The charges the player carries, across all their cells. */
    public static int charges(Player player) {
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.PHLOGISTON_CELL.get())) {
                total += stack.getMaxDamage() - stack.getDamageValue();
            }
        }
        return total;
    }

    /**
     * Draws {@code cost} charges from the player's cells, the most worn first (so they empty one at a time), turning each
     * drained cell into an empty one; false, and nothing drawn, if they don't carry that many. Free in creative.
     */
    public static boolean draw(Player player, int cost) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (charges(player) < cost) {
            return false;
        }
        Inventory inventory = player.getInventory();
        int left = cost;
        while (left > 0) {
            int best = -1;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.is(ModItems.PHLOGISTON_CELL.get()) && (best < 0 || stack.getDamageValue() > inventory.getItem(best).getDamageValue())) {
                    best = slot;
                }
            }
            ItemStack cell = inventory.getItem(best);
            int taken = Math.min(left, cell.getMaxDamage() - cell.getDamageValue());
            left -= taken;
            if (cell.getDamageValue() + taken >= cell.getMaxDamage()) {
                inventory.setItem(best, new ItemStack(ModItems.EMPTY_PHLOGISTON_CELL.get()));
            } else {
                cell.setDamageValue(cell.getDamageValue() + taken);
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.phlogiston_cell", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage())
                .withStyle(ChatFormatting.GRAY));
    }
}
