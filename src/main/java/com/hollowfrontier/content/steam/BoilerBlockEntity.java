package com.hollowfrontier.content.steam;

import com.hollowfrontier.registry.ModBlockEntities;
import com.hollowfrontier.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Burns furnace fuel to boil water into steam: STEAM_PER_TICK mB of water becomes the same amount of
 * steam each burning tick. Water comes in by bucket, any fluid pipe, or from a water source block touching it (a well or
 * trough: WATER_INTAKE mB a tick); steam is pushed to neighbors.
 * One fuel slot (hoppers can feed it from any side).
 */
public class BoilerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int WATER_CAPACITY = 8_000;
    public static final int STEAM_CAPACITY = 8_000;
    public static final int STEAM_PER_TICK = 10;
    private static final int PUSH_RATE = 200;
    public static final int WATER_INTAKE = 25;
    private static final int[] SLOTS = {0};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final FluidTank water = new FluidTank(WATER_CAPACITY, fluid -> fluid.isSame(Fluids.WATER), this::setChanged);
    private final FluidTank steam = FluidTank.steam(STEAM_CAPACITY, this::setChanged);
    private final ResourceHandler<FluidResource> fluidHandler = new CombinedResourceHandler<>(water, steam);
    private int burnTime;
    private int burnDuration;
    private SteamNetwork.Neighbours neighbours;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> burnDuration;
                case 2 -> water.amount();
                case 3 -> steam.amount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) burnTime = value;
            if (index == 1) burnDuration = value;
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER.get(), pos, state);
    }

    public ResourceHandler<FluidResource> fluidHandler() {
        return fluidHandler;
    }

    public FluidTank steamTank() {
        return steam;
    }

    public FluidTank waterTank() {
        return water;
    }

    /** Ticks of fuel left burning, and how long the current fuel burns in all (both 0 when cold). */
    public int burnTime() {
        return burnTime;
    }

    public int burnDuration() {
        return burnDuration;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity be) {
        ServerLevel serverLevel = (ServerLevel) level;
        if (be.water.space() > 0 && touchesWaterSource(level, pos)) {
            be.water.add(FluidResource.of(Fluids.WATER), WATER_INTAKE);
        }
        boolean canBoil = be.water.amount() >= STEAM_PER_TICK && be.steam.space() >= STEAM_PER_TICK;
        if (be.burnTime <= 0 && canBoil) {
            ItemStack fuel = be.items.get(0);
            int duration = ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, be.getLootContext(serverLevel, fuel), 0);
            if (duration > 0) {
                be.burnTime = be.burnDuration = duration;
                ItemStack remainder = fuel.getCraftingRemainder() == null ? ItemStack.EMPTY : fuel.getCraftingRemainder().create();
                fuel.shrink(1);
                if (fuel.isEmpty() && !remainder.isEmpty()) {
                    be.items.set(0, remainder);
                }
                be.setChanged();
            }
        }
        if (be.burnTime > 0) {
            be.burnTime--;
            if (canBoil) {
                int boiled = be.water.drain(FluidResource.of(Fluids.WATER), STEAM_PER_TICK);
                be.steam.add(ModFluids.steam(), boiled);
            }
        }
        boolean lit = be.burnTime > 0;
        if (state.getValue(BoilerBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(BoilerBlock.LIT, lit), 3);
        }
        if (be.neighbours == null) {
            be.neighbours = new SteamNetwork.Neighbours(serverLevel, pos);
        }
        SteamNetwork.push(be.neighbours, be.steam, PUSH_RATE);
    }

    private static boolean touchesWaterSource(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            FluidState fluid = level.getFluidState(pos.relative(dir));
            if (fluid.isSource() && fluid.is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hollowfrontier.boiler");
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
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.has(DataComponents.COOKING_FUEL);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new BoilerMenu(containerId, inventory, this, data);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return !stack.has(DataComponents.COOKING_FUEL);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        water.serialize(output.child("water"));
        steam.serialize(output.child("steam"));
        output.putInt("burn_time", burnTime);
        output.putInt("burn_duration", burnDuration);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        water.deserialize(input.childOrEmpty("water"));
        steam.deserialize(input.childOrEmpty("steam"));
        burnTime = input.getIntOr("burn_time", 0);
        burnDuration = input.getIntOr("burn_duration", 0);
    }
}
