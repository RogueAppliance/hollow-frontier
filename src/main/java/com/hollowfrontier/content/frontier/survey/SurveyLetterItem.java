package com.hollowfrontier.content.frontier.survey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.hollowfrontier.content.concavia.ConcavianCodexItem;
import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.content.hollow.gateway.PolarCompassItem;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

/**
 * The Polar Survey's last letter (32d): the frontier's counterpart to the Concavian Codex, and what points a player from
 * town to the gateway. The first time a player talks to a town's postmaster (or takes the Survey's notice off a bounty
 * board) they're handed it, unless they've already opened a gateway. It reads as the Survey's plea, then roughly where
 * the crater is from where the reader stands, then what the Survey asked for, each thing ticked off as the reader gets it
 * ({@link #STEPS}) with a word on how. Like the Codex it writes itself afresh each time it's opened.
 */
public class SurveyLetterItem extends Item {
    /** One thing the Survey asked for ({@code book.hollowfrontier.survey_letter.step.<name>}, with a {@code .how}), and when it's done. */
    public record Step(String name, Predicate<ServerPlayer> done) {}

    public static final List<Step> STEPS = List.of(
            new Step("boiler", reached("steam/head_of_steam")),
            new Step("canister", reached("steam/canned_steam")),
            new Step("airship", reached("steam/airship")),
            new Step("compass", reached("steam/true_north")),
            new Step("resonator", player -> player.getInventory().contains(stack -> stack.is(ModItems.POLAR_RESONATOR.get()))
                    || ConcavianCodexItem.reached(player, "steam/open_sesame")),
            new Step("gateway", reached("steam/open_sesame")));
    private static final int PER_PAGE = 3;

    public SurveyLetterItem(Properties properties) {
        super(properties);
    }

    private static Predicate<ServerPlayer> reached(String advancement) {
        return player -> ConcavianCodexItem.reached(player, advancement);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            stack.set(DataComponents.WRITTEN_BOOK_CONTENT, write(serverPlayer, stack));
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverPlayer.openItemGui(stack, hand);
        }
        return InteractionResult.SUCCESS;
    }

    /** Whether this player is due the letter: never handed one, and no gateway opened yet. */
    public static boolean due(ServerPlayer player) {
        return !player.getData(ModAttachments.SURVEY_LETTER.get()) && !ConcavianCodexItem.reached(player, "steam/open_sesame");
    }

    /** Hands the letter over, if the player is due it, with its gateway found from {@code from}. */
    public static boolean handOver(ServerLevel level, ServerPlayer player, BlockPos from) {
        if (!due(player)) {
            return false;
        }
        ItemStack letter = new ItemStack(ModItems.SURVEY_LETTER.get());
        PolarCompassItem.bearing(level, from).ifPresent(gateway -> letter.set(ModDataComponents.SURVEY_GATEWAY.get(), GlobalPos.of(level.dimension(), gateway)));
        player.setData(ModAttachments.SURVEY_LETTER.get(), true);
        BoardUse.give(player, letter);
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /** The letter as it reads for this player now. */
    public static WrittenBookContent write(ServerPlayer reader, ItemStack letter) {
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(Component.empty()
                .append(Component.translatable("book.hollowfrontier.survey_letter.title").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_BLUE))
                .append("\n\n")
                .append(Component.translatable("book.hollowfrontier.survey_letter.plea"))));
        pages.add(Filterable.passThrough(Component.empty()
                .append(Component.translatable("book.hollowfrontier.survey_letter.plea_2"))
                .append("\n\n")
                .append(Component.translatable("book.hollowfrontier.survey_letter.signed").withStyle(ChatFormatting.ITALIC))));
        pages.add(Filterable.passThrough(where(reader, letter)));
        for (int from = 0; from < STEPS.size(); from += PER_PAGE) {
            MutableComponent page = Component.empty();
            if (from == 0) {
                page.append(Component.translatable("book.hollowfrontier.survey_letter.needs").withStyle(ChatFormatting.BOLD)).append("\n\n");
            }
            for (Step step : STEPS.subList(from, Math.min(from + PER_PAGE, STEPS.size()))) {
                page.append(line(reader, step)).append("\n");
            }
            pages.add(Filterable.passThrough(page));
        }
        pages.add(Filterable.passThrough(Component.translatable("book.hollowfrontier.survey_letter.reward", SurveyBounty.REWARD)));
        return new WrittenBookContent(Filterable.passThrough("The Polar Survey"), "E. Crane", 0, pages, false);
    }

    // How far and which way the crater lies from the reader, if the letter knows (and they're in the same world)
    private static Component where(ServerPlayer reader, ItemStack letter) {
        GlobalPos gateway = letter.get(ModDataComponents.SURVEY_GATEWAY.get());
        if (gateway == null && reader.level() instanceof ServerLevel level) {
            Optional<BlockPos> found = PolarCompassItem.bearing(level, reader.blockPosition());
            if (found.isPresent()) {
                gateway = GlobalPos.of(level.dimension(), found.get());
                letter.set(ModDataComponents.SURVEY_GATEWAY.get(), gateway);
            }
        }
        if (gateway == null || gateway.dimension() != reader.level().dimension()) {
            return Component.translatable("book.hollowfrontier.survey_letter.where_unknown");
        }
        BlockPos pos = gateway.pos();
        int distance = Math.max(100, (int) Math.round(Math.hypot(pos.getX() - reader.getX(), pos.getZ() - reader.getZ()) / 100) * 100);
        return Component.translatable("book.hollowfrontier.survey_letter.where", distance,
                Component.translatable("message.hollowfrontier.direction." + PolarCompassItem.direction(reader.blockPosition(), pos)));
    }

    private static Component line(ServerPlayer reader, Step step) {
        String key = "book.hollowfrontier.survey_letter.step." + step.name();
        if (step.done().test(reader)) {
            return Component.literal("✔ ").withStyle(ChatFormatting.DARK_GREEN)
                    .append(Component.translatable(key).withStyle(ChatFormatting.DARK_GREEN));
        }
        return Component.literal("○ ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable(key).withStyle(ChatFormatting.BLACK))
                .append(Component.literal("\n").append(Component.translatable(key + ".how")).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
