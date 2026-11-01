package com.hollowfrontier.content.steam.airship;

import java.util.Locale;
import java.util.function.Consumer;

import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * A folded airship: use it on the ground to lay it out, bow the way you face. It needs room for the envelope above.
 * One broken up keeps its coal and steam ({@link ModDataComponents#AIRSHIP_FUEL}, {@link ModDataComponents#STEAM}), and
 * whoever lays it out owns it.
 */
public class AirshipItem extends Item {
    public AirshipItem(Properties properties) {
        super(properties);
    }

    /** A folded airship holding {@code fuel} burn ticks of coal and {@code steam} mB. */
    public static ItemStack of(int fuel, int steam) {
        ItemStack stack = new ItemStack(ModItems.AIRSHIP.get());
        if (fuel > 0) {
            stack.set(ModDataComponents.AIRSHIP_FUEL.get(), fuel);
        }
        if (steam > 0) {
            stack.set(ModDataComponents.STEAM.get(), steam);
        }
        return stack;
    }

    public static int fuel(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AIRSHIP_FUEL.get(), 0);
    }

    public static int steam(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STEAM.get(), 0);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos at = context.getClickedFace() == Direction.UP ? clicked.above() : clicked.relative(context.getClickedFace());
        Airship ship = ModEntities.AIRSHIP.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (ship == null) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        Vec3 pos = Vec3.atBottomCenterOf(at);
        ship.snapTo(pos.x, pos.y, pos.z, player != null ? player.getYRot() : 0.0F, 0.0F);
        if (!level.noCollision(ship, ship.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            ItemStack stack = context.getItemInHand();
            ship.setFuel(fuel(stack));
            ship.setSteam(steam(stack));
            ship.setCustomName(stack.getCustomName());
            if (player != null) {
                ship.setOwner(player);
            }
            level.addFreshEntity(ship);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        int fuel = fuel(stack), steam = steam(stack);
        if (fuel > 0 || steam > 0) {
            String coal = String.format(Locale.ROOT, "%.1f", fuel / (double) Airship.COAL_TICKS);
            builder.accept(Component.translatable("tooltip.hollowfrontier.airship.stored", coal, steam * 100 / Airship.CAPACITY)
                    .withStyle(ChatFormatting.GOLD));
        }
        builder.accept(Component.translatable("tooltip.hollowfrontier.airship.fuel").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.hollowfrontier.airship.controls").withStyle(ChatFormatting.GRAY));
    }
}
