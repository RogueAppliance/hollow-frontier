package com.hollowfrontier.content.steam;

import java.util.Optional;

import com.hollowfrontier.registry.ModBlockEntities;
import com.hollowfrontier.registry.ModFluids;
import com.hollowfrontier.registry.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Steam-powered ore crusher: slot 0 input, slot 1 output. Each working tick burns STEAM_PER_TICK mB;
 * a recipe takes CRUSH_TIME ticks. Hoppers: insert from top/sides, extract from the bottom.
 */
public class CrusherBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int STEAM_CAPACITY = 2_000;
    public static final int STEAM_PER_TICK = 4;
    public static final int CRUSH_TIME = 100;
    private static final int[] INPUT = {0};
    private static final int[] OUTPUT = {1};

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final FluidTank steam = FluidTank.steam(STEAM_CAPACITY, this::setChanged);
    private int progress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> steam.amount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state);
    }

    public FluidTank steamTank() {
        return steam;
    }

    /** Ticks into the current crush, out of {@link #CRUSH_TIME}. */
    public int progress() {
        return progress;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrusherBlockEntity be) {
        ItemStack input = be.items.get(0);
        Optional<RecipeHolder<CrushingRecipe>> recipe = input.isEmpty() ? Optional.empty()
                : ((ServerLevel) level).recipeAccess().getRecipeFor(ModRecipes.CRUSHING.get(), new SingleRecipeInput(input), level);
        ItemStack result = recipe.map(r -> r.value().assemble(new SingleRecipeInput(input))).orElse(ItemStack.EMPTY);
        if (result.isEmpty() || !be.canOutput(result)) {
            if (be.progress != 0) {
                be.progress = 0;
                be.setChanged();
            }
            return;
        }
        if (be.steam.drain(ModFluids.steam(), STEAM_PER_TICK) < STEAM_PER_TICK) {
            return;
        }
        if (++be.progress >= CRUSH_TIME) {
            be.progress = 0;
            input.shrink(1);
            ItemStack output = be.items.get(1);
            if (output.isEmpty()) {
                be.items.set(1, result);
            } else {
                output.grow(result.getCount());
            }
        }
        be.setChanged();
    }

    private boolean canOutput(ItemStack result) {
        ItemStack output = items.get(1);
        return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hollowfrontier.crusher");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CrusherMenu(containerId, inventory, this, data);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return direction == Direction.DOWN ? OUTPUT : INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == 1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        steam.serialize(output.child("steam"));
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        steam.deserialize(input.childOrEmpty("steam"));
        progress = input.getIntOr("progress", 0);
    }
}
