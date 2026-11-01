package com.hollowfrontier.content.steam;

import java.util.function.Predicate;

import com.hollowfrontier.registry.ModFluids;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Single-slot fluid tank restricted to one fluid (steam or water), notifying its owner on change.
 * The direct add/drain helpers are for the owning block entity's own logic (outside transactions).
 */
public class FluidTank extends FluidStacksResourceHandler {
    private final Predicate<Fluid> filter;
    private final Runnable onChanged;

    public FluidTank(int capacity, Predicate<Fluid> filter, Runnable onChanged) {
        super(1, capacity);
        this.filter = filter;
        this.onChanged = onChanged;
    }

    /** A steam tank (30): takes any mod's steam (c:steam) and keeps it as ours, so every steam machine reads one fluid. */
    public static FluidTank steam(int capacity, Runnable onChanged) {
        return new FluidTank(capacity, ModFluids::isSteam, onChanged) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                return super.insert(index, ModFluids.isSteam(resource.getFluid()) ? ModFluids.steam() : resource, amount, transaction);
            }
        };
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && filter.test(resource.getFluid());
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        onChanged.run();
    }

    public int amount() {
        return getAmountAsInt(0);
    }

    public int capacity() {
        return capacity;
    }

    public int space() {
        return capacity - amount();
    }

    public int add(FluidResource resource, int amount) {
        if (amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = insert(0, resource, amount, tx);
            tx.commit();
            return inserted;
        }
    }

    public int drain(FluidResource resource, int amount) {
        if (amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = extract(0, resource, amount, tx);
            tx.commit();
            return extracted;
        }
    }
}
