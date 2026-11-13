package com.hollowfrontier.content.frontier.stake;

import java.util.function.Consumer;

import com.hollowfrontier.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Phase 47's items: the Deputy's Badge, the warrant, the deed and the branding iron. */
public final class StakeItems {
    private StakeItems() {}

    private static void townLine(ItemStack stack, Consumer<Component> builder, String key) {
        TownRef town = stack.get(ModDataComponents.TOWN.get());
        if (town != null) {
            builder.accept(Component.translatable(key, town.name()).withStyle(ChatFormatting.GRAY));
        }
    }

    /** A town's star (47a): carried, it makes its sworn deputy one ({@link Deputies}). It says which town's it is. */
    public static class Badge extends Item {
        public Badge(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            townLine(stack, builder, StakeText.BADGE_TOWN);
        }
    }

    /** A warrant for another town's sheriff (47a): handed to him, it's served ({@link SheriffJobs#serve}). */
    public static class Warrant extends Item {
        public Warrant(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            townLine(stack, builder, StakeText.WARRANT_TO);
        }
    }

    /** A deed to a place at one town (47b): used on an Old Place's stake, or on open ground to have one raised ({@link OldPlace}). */
    public static class Deed extends Item {
        public Deed(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            TownRef deed = context.getItemInHand().get(ModDataComponents.TOWN.get());
            if (deed == null || context.getPlayer() == null) {
                return InteractionResult.PASS;
            }
            if (context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player) {
                BlockPos ground = context.getClickedPos();
                // Used on grass or a bush: the ground is what it grows from
                if (level.getBlockState(ground).canBeReplaced()) {
                    ground = ground.below();
                }
                OldPlace.raise(level, player, ground, player.getDirection(), context.getItemInHand(), deed);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            townLine(stack, builder, StakeText.DEED_FOR);
            builder.accept(Component.translatable(StakeText.DEED_HINT).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * A branding iron (47b): heated at any fire (a lit campfire, furnace, smoker or blast furnace) it stays hot a minute,
     * and a hot iron used on a cow marks it with the brand of the holder's place ({@link Herds#brand}).
     */
    public static class BrandingIron extends Item {
        public static final int HOT_TICKS = 1200;

        public BrandingIron(Properties properties) {
            super(properties);
        }

        public static boolean hot(ItemStack stack, Level level) {
            Long until = stack.get(ModDataComponents.HOT_UNTIL.get());
            return until != null && until > level.getGameTime();
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            BlockState state = level.getBlockState(context.getClickedPos());
            if (!state.hasProperty(BlockStateProperties.LIT) || !state.getValue(BlockStateProperties.LIT)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                context.getItemInHand().set(ModDataComponents.HOT_UNTIL.get(), level.getGameTime() + HOT_TICKS);
                level.playSound(null, context.getClickedPos(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 0.7F);
                if (context.getPlayer() != null) {
                    context.getPlayer().sendOverlayMessage(Component.translatable(StakeText.IRON_HOT));
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (!Herds.isCattle(target)) {
                return InteractionResult.PASS;
            }
            if (player.level() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
                // In creative the hand's stack is a copy: mark and wear the one actually held
                Herds.brand(level, serverPlayer, target, player.getItemInHand(hand), hand);
            }
            return InteractionResult.SUCCESS;
        }
    }
}
