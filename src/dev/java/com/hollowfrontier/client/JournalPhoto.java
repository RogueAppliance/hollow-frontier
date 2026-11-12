package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.journal.Journal;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The Prospector's Journal on screen (`runPhoto -PphotoScene=journal`): a player part of the way down the road opens the
 * book by using it, and it's shot at GUI scales 2 and 3 on the frontier's page, on steam's (with the Survey's list and
 * the heading) and with a thread pinned; then a milestone is reached with the book shut, for the "noted" line. Pictures
 * are `hf_journal_*`; the log says what the server sent.
 */
final class JournalPhoto {
    private JournalPhoto() {}

    static final boolean ENABLED = "journal".equals(System.getProperty("hollowfrontier.photo.scene"));

    private static int tick;
    private static String townName = "";

    private static void award(ServerPlayer player, String name) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(HollowFrontier.id(name));
        for (String criterion : advancement.value().criteria().keySet()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    static void tick(Minecraft mc, MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        int at = tick++;
        switch (at) {
            case 0 -> {
                mc.options.guiScale().set(2);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.getInventory().clearContent();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PROSPECTORS_JOURNAL.get()));
                ItemStack letter = new ItemStack(ModItems.SURVEY_LETTER.get());
                letter.set(ModDataComponents.SURVEY_GATEWAY.get(), GlobalPos.of(player.level().dimension(), player.blockPosition().offset(900, 0, -2600)));
                player.getInventory().add(letter);
                player.setData(ModAttachments.SURVEY_LETTER.get(), true);
                for (String step : new String[] {"frontier/mint_coin", "frontier/load_gun", "frontier/lights_in_the_sky", "steam/head_of_steam",
                        "steam/canned_steam", "steam/handcar", "ufo/root", "ufo/crash_and_burn"}) {
                    award(player, step);
                }
                Journal.pin(player, "");
                // 48b: creatures met (one known, one tagged), strange doings and people
                com.hollowfrontier.content.journal.JournalData data = player.getData(ModAttachments.JOURNAL.get());
                for (String id : com.hollowfrontier.content.journal.JournalSeen.CREATURES) {
                    data = data.creature(id, id.equals("woolly_mammoth") ? 7 : id.equals("fern_skitter") ? 3 : 1);
                }
                if (data.strangeCount() == 0) {
                    String[] kinds = {"sighting", "mutilation", "crop_circle", "visit", "sighting", "landing", "sighting", "mutilation", "visit", "sighting", "crop_circle"};
                    for (int i = 0; i < kinds.length; i++) {
                        data = data.witnessed(new com.hollowfrontier.content.journal.JournalData.Strange(kinds[i], 3 + i * 2, 120 + i * 37, -640 + i * 53, i % 3 == 0 ? "" : "Dry Gulch"));
                    }
                }
                player.setData(ModAttachments.JOURNAL.get(), data.person("outlaws.met").person("dusters.met").person("grays.notes"));
                // 48c: the town the booth stands in, if it's in one, with some standing, a box at the bank and a raid on record
                net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
                long town = com.hollowfrontier.content.frontier.town.Towns.near(level, player.blockPosition(), 400).map(s -> s.getChunkPos().pack()).orElse(480_010L);
                com.hollowfrontier.content.journal.JournalTowns.enter(level, player, town);
                com.hollowfrontier.content.journal.JournalTowns.enter(level, player, 480_011L);
                if (com.hollowfrontier.content.frontier.stake.Standing.of(player).points(town) < 30) {
                    com.hollowfrontier.content.frontier.stake.Standing.add(player, town, 34);
                }
                com.hollowfrontier.content.frontier.bank.TownBanks.get(level).open(town, player.getUUID(), 0).setItem(0, new ItemStack(ModItems.GOLD_COIN.get(), 3));
                com.hollowfrontier.content.frontier.raid.TownRaids.get(level).record(town, level.getGameTime()).lastRaid = Math.max(0, level.getGameTime() - 48000);
                townName = com.hollowfrontier.content.frontier.town.Towns.name(level, town);
            }
            case 60 -> {
                // the way a player opens it: the item used
                player.gameMode.useItem(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
                HollowFrontier.LOGGER.info("[Journal] sent: {}", Journal.pages(player));
            }
            case 80 -> {
                HollowFrontier.LOGGER.info("[Journal] {}", mc.gui.screen() instanceof JournalScreen ? "PASS the item opens the screen" : "FAIL no screen: " + mc.gui.screen());
                PhotoBooth.shot(mc, "hf_journal_1_scale2_latest.png");
            }
            case 82 -> show(mc, "seen", "woolly_mammoth");
            case 84 -> PhotoBooth.shot(mc, "hf_journal_8_scale2_mammoth.png");
            case 85 -> show(mc, "towns", townName);
            case 86 -> {
                PhotoBooth.shot(mc, "hf_journal_12_scale2_town.png");
                show(mc, "doings", "");
            }
            case 88 -> PhotoBooth.shot(mc, "hf_journal_9_scale2_doings.png");
            case 90 -> pick(mc, "frontier");
            case 100 -> PhotoBooth.shot(mc, "hf_journal_2_scale2_frontier.png");
            case 110 -> pick(mc, "steam");
            case 120 -> PhotoBooth.shot(mc, "hf_journal_3_scale2_steam.png");
            case 130 -> {
                Journal.pin(player, "steam");
                Journal.open(player);
                pick(mc, "grays");
            }
            case 150 -> PhotoBooth.shot(mc, "hf_journal_4_scale2_pinned.png");
            case 160 -> mc.options.guiScale().set(3);
            case 180 -> PhotoBooth.shot(mc, "hf_journal_5_scale3_pinned.png");
            case 182 -> show(mc, "people", "grays");
            case 184 -> PhotoBooth.shot(mc, "hf_journal_10_scale3_grays.png");
            case 185 -> show(mc, "towns", townName);
            case 186 -> {
                PhotoBooth.shot(mc, "hf_journal_13_scale3_town.png");
                show(mc, "seen", "fern_skitter");
            }
            case 188 -> PhotoBooth.shot(mc, "hf_journal_11_scale3_skitter.png");
            case 190 -> pick(mc, "steam");
            case 200 -> PhotoBooth.shot(mc, "hf_journal_6_scale3_steam.png");
            case 210 -> {
                mc.gui.setScreen(null);
                mc.options.guiScale().set(2);
            }
            case 230 -> {   // with the book shut, the next thing on some thread is done: the note
                String step = Journal.threads(player).stream().map(Journal.Thread::next).filter(next -> !next.isEmpty()).findFirst().orElseThrow();
                HollowFrontier.LOGGER.info("[Journal] reaching {}", step);
                award(player, step);
            }
            case 245 -> PhotoBooth.shot(mc, "hf_journal_7_noted.png");
            case 300 -> {
                mc.options.guiScale().set(0);
                PhotoBooth.quit(mc, server);
            }
            default -> {}
        }
    }

    private static void pick(Minecraft mc, String thread) {
        show(mc, "road", thread);
    }

    private static void show(Minecraft mc, String part, String entry) {
        if (mc.gui.screen() instanceof JournalScreen screen) {
            screen.show(part, entry);
        }
    }
}
