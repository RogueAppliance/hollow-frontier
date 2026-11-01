package com.hollowfrontier.content.steam;

import com.hollowfrontier.registry.ModMenus;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Data slots: 0 burn time, 1 burn duration, 2 water mB, 3 steam mB.
public class BoilerMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public BoilerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(4));
    }

    public BoilerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.BOILER.get(), containerId);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, 0, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.has(DataComponents.COOKING_FUEL);
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public float burnFraction() {
        int duration = data.get(1);
        return duration <= 0 ? 0 : (float) data.get(0) / duration;
    }

    public int water() {
        return data.get(2);
    }

    public int steam() {
        return data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return MenuUtil.quickMove(this, slots, player, slotIndex, 1, this::moveItemStackTo);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
