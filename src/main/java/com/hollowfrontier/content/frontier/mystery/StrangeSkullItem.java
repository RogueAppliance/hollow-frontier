package com.hollowfrontier.content.frontier.mystery;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Boot Hill pilot's skull (11a), from the strange grave: grey, too big in the brow, with great dark sockets. Held up to
 * the open sky at night in the Overworld, it calls them: an airship comes over and hangs above whoever holds it for
 * {@link #HOVER_TICKS} with its lights on them, then goes on its way. Once a night ({@link #COOLDOWN}).
 */
public class StrangeSkullItem extends Item {
    public static final int HOVER_TICKS = 160;
    public static final int COOLDOWN = 12000;

    public StrangeSkullItem(Properties properties) {
        super(properties);
    }

    /** Whether the skull can call them from where {@code player} stands: night, the Overworld, open sky. */
    public static boolean canCall(Level level, Player player) {
        return level.dimension() == Level.OVERWORLD && level.isDarkOutside() && level.canSeeSky(player.blockPosition().above());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (!canCall(level, player)) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.strange_skull.nothing"));
            return InteractionResult.FAIL;
        }
        Mysteries.sighting(server, player.position(), server.getRandom(), HOVER_TICKS);
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.strange_skull.called"));
        player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN);
        if (player instanceof ServerPlayer serverPlayer) {
            Suspicion.add(serverPlayer, Suspicion.CALL_SIGHTING); // and they notice who called (11b)
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.strange_skull").withStyle(ChatFormatting.GRAY));
    }
}
