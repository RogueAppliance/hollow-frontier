package com.hollowfrontier.content.steam;

import java.util.function.Consumer;

import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModFluids;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Portable steam: right-click any steam holder (boiler, pipe, tank) to fill. Steam tools such as the
 * Steam Drill draw from canisters anywhere in the inventory. The item bar shows the fill level.
 */
public class SteamCanisterItem extends Item {
    public static final int CAPACITY = 4_000;

    public SteamCanisterItem(Properties properties) {
        super(properties);
    }

    public static int steam(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STEAM.get(), 0);
    }

    public static void setSteam(ItemStack stack, int amount) {
        stack.set(ModDataComponents.STEAM.get(), Math.max(0, Math.min(CAPACITY, amount)));
    }

    /** Takes up to amount mB from canisters in the player's inventory; returns how much was taken. */
    public static int drawFromInventory(Player player, int amount) {
        if (player.hasInfiniteMaterials()) {
            return amount;
        }
        int taken = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && taken < amount; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof SteamCanisterItem) {
                int held = steam(stack);
                int use = Math.min(held, amount - taken);
                if (use > 0) {
                    setSteam(stack, held - use);
                    taken += use;
                }
            }
        }
        return taken;
    }

    public static boolean hasSteam(Player player, int amount) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof SteamCanisterItem) {
                total += steam(stack);
                if (total >= amount) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        int space = CAPACITY - steam(stack);
        if (space <= 0) {
            return InteractionResult.PASS;
        }
        ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, context.getClickedPos(), context.getClickedFace());
        if (handler == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            try (Transaction tx = Transaction.openRoot()) {
                int drained = ModFluids.extractSteam(handler, space, tx);
                if (drained > 0) {
                    tx.commit();
                    setSteam(stack, steam(stack) + drained);
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        ModCriteria.CANISTER_FILLED.get().trigger(player);
                    }
                    level.playSound(null, context.getClickedPos(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6F, 1.6F);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * steam(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xD8D8E0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.steam", steam(stack), CAPACITY).withStyle(ChatFormatting.GRAY));
    }
}
