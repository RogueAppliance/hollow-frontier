package com.hollowfrontier.content.steam;

import com.hollowfrontier.registry.ModMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Data slots: 0 progress, 1 steam mB. Slot 0 input, slot 1 output (take-only).
public class CrusherMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public CrusherMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(2));
    }

    public CrusherMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.CRUSHER.get(), containerId);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, 0, 56, 35));
        addSlot(new Slot(container, 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public float progress() {
        return (float) data.get(0) / CrusherBlockEntity.CRUSH_TIME;
    }

    public int steam() {
        return data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return MenuUtil.quickMove(this, slots, player, slotIndex, 2, this::moveItemStackTo);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
