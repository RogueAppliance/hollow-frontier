package com.hollowfrontier.content.frontier.gazette;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hollowfrontier.content.frontier.bounty.OutlawNames;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.mystery.MenInBlack;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.frontier.mystery.Mysteries;
import com.hollowfrontier.content.frontier.raid.RaidReport;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * The Frontier Gazette. Unread, it's blank; the first time it's opened the edition is printed for where and when you
 * are: rumours with coordinates of the nearest outlaw camp and roadhouse saloon, a wanted column (you, if you're
 * wanted), the latest town raid (if it was this week) and an odd-news item. It then reads like a written book
 * (vanilla book screen).
 * The odd news is a Polar Gateway's crater when one is within reach (phase 8); otherwise a rumour, among them (9c) airship
 * lights, Symmes' Hole and warm seas past the pole. Before it (11a), the week's strange doings within reach, if any: an
 * airship sighting, a mutilated steer or a crop circle, with where and how many days ago. Phase 13a adds crash-site rumours.
 * Once the leash is broken (15c), a page on the aftermath ({@link Aftermath}). Last (21a), the back page ({@link GazetteBackPage}).
 */
public class GazetteItem extends Item {
    public static final int SEARCH_CHUNKS = 48;
    public static final int ODD_NEWS = 8;
    public static final int QUIET_NEWS = 3;
    public static final int AFTERMATH_NEWS = 4;
    // How long a raid stays news.
    public static final long RAID_NEWS_TICKS = 7 * 24000L;

    public GazetteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            if (!stack.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
                stack.set(DataComponents.WRITTEN_BOOK_CONTENT, print(serverLevel, serverPlayer));
                level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            serverPlayer.openItemGui(stack, hand);
        }
        return InteractionResult.SUCCESS;
    }

    /** Prints an edition for this reader. Unresolved, so openItemGui syncs the stack before the book screen opens. */
    public static WrittenBookContent print(ServerLevel level, ServerPlayer reader) {
        RandomSource random = level.getRandom();
        BlockPos here = reader.blockPosition();
        long day = level.getGameTime() / 24000L + 1; // days since the world began, give or take
        List<Filterable<Component>> pages = new ArrayList<>();

        // Front page: masthead and the outlaw rumour.
        MutableComponent front = masthead(day);
        BlockPos camp = nearest(level, ModStructures.OUTLAW_CAMP, here);
        front.append(camp != null
                ? story("outlaws", camp.getX(), camp.getZ())
                : story("quiet." + (1 + random.nextInt(QUIET_NEWS))));
        pages.add(Filterable.passThrough(front));

        // Page two: travel news.
        BlockPos saloon = nearest(level, ModStructures.SALOON, here);
        pages.add(Filterable.passThrough(saloon != null
                ? story("saloon", saloon.getX(), saloon.getZ())
                : story("quiet." + (1 + random.nextInt(QUIET_NEWS)))));

        // Page three: the wanted column. The reader, if the law is after them; otherwise some desperado.
        pages.add(Filterable.passThrough(Wanted.stars(reader) > 0
                ? story("wanted_you", reader.getName().getString(), Wanted.fine(reader))
                : story("wanted", OutlawNames.random(random), 2 + random.nextInt(5))));

        // The latest town raid, if there was one in the last week: how it went and who led it.
        RaidReport raid = level.dimension() == Level.OVERWORLD ? TownRaids.get(level).lastReport().orElse(null) : null;
        if (raid != null && level.getGameTime() - raid.time() < RAID_NEWS_TICKS) {
            String gang = raid.leader().substring(raid.leader().lastIndexOf(' ') + 1);
            MutableComponent page = story("raid." + raid.story(), raid.leader(), gang, raid.x(), raid.z(), com.hollowfrontier.worldgen.structure.TownNames.phrase(raid.town()));
            if (raid.bankLost() > 0) {
                page.append(" ").append(Component.translatable("gazette.hollowfrontier.raid.bank_lost", raid.bankLost() / 9, raid.bankLost() % 9));
            }
            pages.add(Filterable.passThrough(page));
        }

        // 11a: strange doings in the territory this week (the newest sighting, mutilation or crop circle within reach)
        Optional<MysteryLog.Sign> mystery = level.dimension() == Level.OVERWORLD ? Mysteries.news(level, here) : Optional.empty();
        // (11b) unless the men in black have been round since: then the Editor prints a correction instead
        mystery.ifPresent(sign -> pages.add(Filterable.passThrough(MenInBlack.corrected(level, sign)
                ? story("mystery.correction." + sign.kind().getSerializedName())
                : story("mystery." + sign.kind().getSerializedName(), Mysteries.daysAgo(level, sign), sign.x(), sign.z()))));

        // 15c: once the leash is broken, the aftermath: the latest freed man settled or ruin mended, or how the frontier's taking it
        if (Leash.isBroken(level)) {
            Optional<Aftermath.Event> news = Aftermath.get(level).latest().filter(e -> level.getGameTime() - e.time() < RAID_NEWS_TICKS);
            pages.add(Filterable.passThrough(news.isPresent()
                    ? story("aftermath." + news.get().kind().getSerializedName(), news.get().who(), news.get().x(), news.get().z())
                    : story("aftermath.general." + (1 + random.nextInt(AFTERMATH_NEWS)))));
        }

        // Page four: odd news from out past the edge of the map; a Polar Gateway's crater when there's one within reach.
        BlockPos gateway = nearest(level, ModStructures.POLAR_GATEWAY, here);
        pages.add(Filterable.passThrough(gateway != null
                ? story("gateway", gateway.getX(), gateway.getZ())
                : story("odd." + (1 + random.nextInt(ODD_NEWS)))));

        // 21a: the back page, classifieds, letters or corrections that fit how far the reader has got
        pages.add(Filterable.passThrough(GazetteBackPage.page(Context.of(level, here, reader), random)));

        return new WrittenBookContent(Filterable.passThrough("The Frontier Gazette"), "The Editor", 0, pages, false);
    }

    private static MutableComponent masthead(long day) {
        return Component.empty()
                .append(Component.translatable("gazette.hollowfrontier.masthead").withStyle(ChatFormatting.BOLD))
                .append("\n")
                .append(Component.translatable("gazette.hollowfrontier.edition", day).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY))
                .append("\n\n");
    }

    // A story: bold headline then body text, from gazette.hollowfrontier.<key>.title / .body.
    private static MutableComponent story(String key, Object... args) {
        return Component.empty()
                .append(Component.translatable("gazette.hollowfrontier." + key + ".title").withStyle(ChatFormatting.BOLD))
                .append("\n\n")
                .append(Component.translatable("gazette.hollowfrontier." + key + ".body", args));
    }

    private static BlockPos nearest(ServerLevel level, ResourceKey<Structure> key, BlockPos from) {
        var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(key);
        if (structure.isEmpty()) {
            return null;
        }
        Pair<BlockPos, ?> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(structure.get()), from, SEARCH_CHUNKS, false);
        return found == null ? null : found.getFirst();
    }
}
