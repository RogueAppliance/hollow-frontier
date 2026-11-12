package com.hollowfrontier.content.ufo.taken;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What being taken has done to a player (50), kept on them through death and through leaving the game: how many times
 * the Grays have kept them, whether they're aboard now, where they were taken from, and their <b>kit</b>, everything they
 * had on them, held here until they get it back. Nothing in the kit is anywhere else, so it can't be lost or doubled:
 * {@link #stash} moves it in, {@link #restore} moves it out, and every way off the ship ends in a restore.
 */
public record TakenData(int times, boolean aboard, Optional<GlobalPos> from, List<Stashed> kit) {
    public static final TakenData NONE = new TakenData(0, false, Optional.empty(), List.of());

    /** One stack of the kit, and the inventory slot it came out of. */
    public record Stashed(int slot, ItemStack stack) {
        public static final Codec<Stashed> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("slot").forGetter(Stashed::slot),
                ItemStack.CODEC.fieldOf("stack").forGetter(Stashed::stack)).apply(i, Stashed::new));
    }

    public static final Codec<TakenData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("times", 0).forGetter(TakenData::times),
            Codec.BOOL.optionalFieldOf("aboard", false).forGetter(TakenData::aboard),
            GlobalPos.CODEC.optionalFieldOf("from").forGetter(TakenData::from),
            Stashed.CODEC.listOf().optionalFieldOf("kit", List.of()).forGetter(TakenData::kit)).apply(i, TakenData::new));

    public static TakenData of(Player player) {
        return player.getData(ModAttachments.TAKEN.get());
    }

    private static void set(Player player, TakenData data) {
        player.setData(ModAttachments.TAKEN.get(), data);
    }

    /** Taken from {@code from}: one more time, and aboard. */
    public static void board(Player player, GlobalPos from) {
        TakenData data = of(player);
        set(player, new TakenData(data.times + 1, true, Optional.of(from), data.kit));
    }

    /** Off the ship and back on the ground: no longer aboard, and nowhere to be put back to. */
    public static void landed(Player player) {
        TakenData data = of(player);
        set(player, new TakenData(data.times, false, Optional.empty(), data.kit));
    }

    /** Everything the player has on them (pack, armour, off hand) goes into the kit. How many stacks went. */
    public static int stash(Player player) {
        TakenData data = of(player);
        List<Stashed> kit = new ArrayList<>(data.kit);
        Inventory inventory = player.getInventory();
        int moved = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                kit.add(new Stashed(slot, stack.copy()));
                inventory.setItem(slot, ItemStack.EMPTY);
                moved++;
            }
        }
        set(player, new TakenData(data.times, data.aboard, data.from, List.copyOf(kit)));
        return moved;
    }

    /**
     * The kit goes back: each stack to the slot it came from, and anything found aboard that was sitting there moves to
     * wherever there's room (or to the player's feet). How many stacks came back (0: there was nothing held).
     */
    public static int restore(Player player) {
        TakenData data = of(player);
        if (data.kit.isEmpty()) {
            return 0;
        }
        // Emptied first, so nothing that goes wrong part-way can hand the kit out twice
        set(player, new TakenData(data.times, data.aboard, data.from, List.of()));
        Inventory inventory = player.getInventory();
        List<ItemStack> moved = new ArrayList<>();
        for (Stashed stashed : data.kit) {
            ItemStack stack = stashed.stack().copy();
            if (stashed.slot() >= inventory.getContainerSize()) {
                moved.add(stack);
                continue;
            }
            // Whatever was picked up aboard and sits where the kit goes makes room for it
            ItemStack inTheWay = inventory.getItem(stashed.slot());
            if (!inTheWay.isEmpty()) {
                moved.add(inTheWay.copy());
            }
            inventory.setItem(stashed.slot(), stack);
        }
        for (ItemStack stack : moved) {
            BoardUse.give(player, stack);
        }
        return data.kit.size();
    }

    public boolean hasKit() {
        return !kit.isEmpty();
    }
}
