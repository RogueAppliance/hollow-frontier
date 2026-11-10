package com.hollowfrontier.content.concavia;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.datagen.ModAdvancements;
import com.hollowfrontier.registry.ModCriteria;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
 * The Concavian Codex (12b): the Kindled's guide to the road from the frontier to the inner sun, a book that writes itself
 * afresh for its reader every time it's opened. Each chapter ({@link #CHAPTERS}) is the road's milestones, the mod's
 * advancements: those the reader has reached are ticked off, and those still ahead say what to do.
 */
public class ConcavianCodexItem extends Item {
    /** Chapter name ({@code book.hollowfrontier.codex.<name>}) and the advancements it walks through, in order. */
    public record Chapter(String name, List<String> steps) {}

    public static final List<Chapter> CHAPTERS = List.of(
            new Chapter("frontier", List.of("frontier/mint_coin", "frontier/load_gun", "frontier/dead_or_alive", "frontier/lights_in_the_sky")),
            new Chapter("steam", List.of("steam/head_of_steam", "steam/handcar", "steam/airship", "steam/true_north", "steam/open_sesame",
                    "steam/hollow_earth")),
            new Chapter("hollow", List.of("hollow/phlogiston", "hollow/starsteel", "hollow/concavian_highlands", "hollow/escaped_specimen",
                    "hollow/on_the_signal")),
            new Chapter("concavia", List.of("concavia/root", "concavia/well_met", "concavia/an_ally_below", "concavia/the_truth",
                    "concavia/temple_of_the_sun", "concavia/eclipse")),
            new Chapter("grays", List.of("ufo/root", "ufo/crash_and_burn", "ufo/salvage_rights", "ufo/death_ray", "ufo/defying_gravity",
                    "ufo/we_have_liftoff", "ufo/calling_them_down", "ufo/the_mothership_falls", "ufo/cut_loose")),
            // 15: what comes after the leash
            new Chapter("after", List.of("ufo/trespassers", "ufo/second_chance", "concavia/concavia_rising", "concavia/old_roads",
                    "ufo/encore", "ufo/fully_fitted")),
            // 43: a side road, and the only chapter the Kindled who wrote the rest never saw the end of
            new Chapter("sea", List.of("hollow/still_sea", "hollow/sea_cow", "hollow/harpoon", "steam/submersible", "hollow/symmes_landing",
                    "hollow/drowned_quarter", "hollow/sealed_hull")));
    /** Milestones to a page: a book page holds about fourteen short lines. */
    private static final int PER_PAGE = 3;

    public ConcavianCodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            stack.set(DataComponents.WRITTEN_BOOK_CONTENT, write(serverPlayer));
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
            ModCriteria.fire(serverPlayer, ModCriteria.CODEX_READ);
            serverPlayer.openItemGui(stack, hand);
        }
        return InteractionResult.SUCCESS;
    }

    /** The book as it reads for this player now. */
    public static WrittenBookContent write(ServerPlayer reader) {
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(Component.empty()
                .append(Component.translatable("book.hollowfrontier.codex.title").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA))
                .append("\n\n")
                .append(Component.translatable("book.hollowfrontier.codex.preface"))));
        for (Chapter chapter : CHAPTERS) {
            for (int from = 0; from < chapter.steps().size(); from += PER_PAGE) {
                MutableComponent page = Component.empty();
                if (from == 0) {
                    page.append(Component.translatable("book.hollowfrontier.codex." + chapter.name()).withStyle(ChatFormatting.BOLD)).append("\n\n");
                }
                for (String step : chapter.steps().subList(from, Math.min(from + PER_PAGE, chapter.steps().size()))) {
                    page.append(milestone(reader, step)).append("\n");
                }
                pages.add(Filterable.passThrough(page));
            }
        }
        return new WrittenBookContent(Filterable.passThrough("Concavian Codex"), "The Kindled", 0, pages, false);
    }

    private static Component milestone(ServerPlayer reader, String step) {
        String key = ModAdvancements.key(step);
        if (reached(reader, step)) {
            return Component.literal("✔ ").withStyle(ChatFormatting.DARK_GREEN)
                    .append(Component.translatable(key + ".title").withStyle(ChatFormatting.DARK_GREEN));
        }
        return Component.literal("○ ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable(key + ".title").withStyle(ChatFormatting.BLACK))
                .append(Component.literal("\n").append(Component.translatable(key + ".description")).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    public static boolean reached(ServerPlayer reader, String step) {
        AdvancementHolder advancement = reader.level().getServer().getAdvancements().get(HollowFrontier.id(step));
        return advancement != null && reader.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
