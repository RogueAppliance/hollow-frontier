package com.hollowfrontier.content.frontier.horse;

import java.util.Map;
import java.util.WeakHashMap;

import com.hollowfrontier.registry.ModAttachments;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Live view of a horse's saddlebags. One instance per horse is shared by all viewers (so two players
 * can't duplicate items), and every change is written straight back to the SADDLEBAGS attachment.
 */
public class SaddlebagContainer extends SimpleContainer {
    public static final int SIZE = 18;
    private static final Map<AbstractHorse, SaddlebagContainer> OPEN = new WeakHashMap<>();

    private final AbstractHorse horse;
    private boolean loading;

    private SaddlebagContainer(AbstractHorse horse) {
        super(SIZE);
        this.horse = horse;
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        horse.getData(ModAttachments.SADDLEBAGS.get()).copyInto(items);
        loading = true;
        for (int i = 0; i < SIZE; i++) {
            setItem(i, items.get(i));
        }
        loading = false;
    }

    public static SaddlebagContainer of(AbstractHorse horse) {
        return OPEN.computeIfAbsent(horse, SaddlebagContainer::new);
    }

    /** Drops the shared view (gear removed or horse died); open menus close via stillValid. */
    public static void forget(AbstractHorse horse) {
        OPEN.remove(horse);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!loading && horse.hasData(ModAttachments.SADDLEBAGS.get())) {
            horse.setData(ModAttachments.SADDLEBAGS.get(), ItemContainerContents.fromItems(getItems()));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return horse.isAlive() && horse.hasData(ModAttachments.SADDLEBAGS.get()) && OPEN.get(horse) == this
                && player.distanceToSqr(horse) < 64.0;
    }
}
