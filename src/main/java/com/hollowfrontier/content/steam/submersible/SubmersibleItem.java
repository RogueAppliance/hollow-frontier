package com.hollowfrontier.content.steam.submersible;

import java.util.function.Consumer;

import com.hollowfrontier.content.hollow.sea.SeaLife;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Steam Submersible, folded (43c): use it looking at water to launch it there, bow the way you face, as a boat is
 * launched. It won't be set down on dry land, where it could never move again. One broken up keeps its steam
 * ({@link ModDataComponents#STEAM}), and whoever launches it owns it.
 */
public class SubmersibleItem extends Item {
    public SubmersibleItem(Properties properties) {
        super(properties);
    }

    /** A folded submersible holding {@code steam} mB. */
    public static ItemStack of(int steam) {
        ItemStack stack = new ItemStack(ModItems.STEAM_SUBMERSIBLE.get());
        if (steam > 0) {
            stack.set(ModDataComponents.STEAM.get(), steam);
        }
        return stack;
    }

    public static int steam(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STEAM.get(), 0);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        return launch(level, player, player.getItemInHand(hand), hit.getBlockPos()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /** Launches it in the water at {@code water} (the block looked at). False, with a word to the player, if that isn't water or there's no room. */
    public static boolean launch(Level level, Player player, ItemStack stack, BlockPos water) {
        if (!SeaLife.isWater(level, water)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.submersible.needs_water").withStyle(ChatFormatting.YELLOW));
            }
            return false;
        }
        SteamSubmersible sub = ModEntities.STEAM_SUBMERSIBLE.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (sub == null) {
            return false;
        }
        // Awash: the hull just under in the top block of water, the dome out
        BlockPos.MutableBlockPos top = water.mutable();
        while (SeaLife.isWater(level, top.above())) {
            top.move(0, 1, 0);
        }
        Vec3 pos = Vec3.atBottomCenterOf(top);
        sub.snapTo(pos.x, pos.y - 0.25, pos.z, player.getYRot(), 0.0F);
        if (!level.noCollision(sub, sub.getBoundingBox())) {
            return false;
        }
        if (!level.isClientSide()) {
            sub.setSteam(steam(stack));
            sub.setCustomName(stack.getCustomName());
            sub.setOwner(player);
            level.addFreshEntity(sub);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, top);
            stack.consume(1, player);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.submersible.stored", steam(stack) * 100 / SteamSubmersible.CAPACITY)
                .withStyle(ChatFormatting.GOLD));
        builder.accept(Component.translatable("tooltip.hollowfrontier.submersible.charge").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.hollowfrontier.submersible.controls").withStyle(ChatFormatting.GRAY));
    }
}
