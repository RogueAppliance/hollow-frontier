package com.hollowfrontier.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hollowfrontier.content.concavia.ConcavianCodexItem;
import com.hollowfrontier.content.frontier.survey.SurveyLetterItem;
import com.hollowfrontier.content.journal.Journal;
import com.hollowfrontier.content.journal.JournalData;
import com.hollowfrontier.content.journal.JournalSeen;
import com.hollowfrontier.content.journal.JournalText;
import com.hollowfrontier.network.JournalPagesPayload;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

// 48a: the Prospector's Journal: the road's threads against the Codex and the letter, and what the client is sent.
final class JournalTests {
    private JournalTests() {}

    private static Journal.Thread thread(ServerPlayer player, String name) {
        return Journal.threads(player).stream().filter(thread -> thread.name().equals(name)).findFirst().orElse(null);
    }

    private static List<String> names(ServerPlayer player) {
        return Journal.threads(player).stream().map(Journal.Thread::name).toList();
    }

    /** The journal has a name for every chapter of the Codex and a line of its own for every milestone, and for nothing else. */
    static void roadMatchesTheCodex(GameTestHelper h) {
        List<String> chapters = new ArrayList<>(), steps = new ArrayList<>();
        for (ConcavianCodexItem.Chapter chapter : ConcavianCodexItem.CHAPTERS) {
            chapters.add(chapter.name());
            steps.addAll(chapter.steps());
        }
        h.assertValueEqual(new ArrayList<>(JournalText.THREAD.keySet()), chapters, "the journal's threads against the Codex's chapters");
        h.assertValueEqual(new ArrayList<>(JournalText.NEXT.keySet()), steps, "the journal's next steps against the Codex's milestones");
        h.assertValueEqual(new HashSet<>(steps).size(), steps.size(), "milestones, each on one thread only");
        for (String step : steps) {
            h.assertTrue(h.getLevel().getServer().getAdvancements().get(com.hollowfrontier.HollowFrontier.id(step)) != null, "no such advancement: " + step);
            h.assertTrue(Journal.threadOf(step) != null, "no thread for " + step);
        }
        h.assertTrue(Journal.threadOf("frontier/riding_for_the_brand") == null, "a thread for something off the road");
        h.succeed();
    }

    /** A thread has a page once its subject is met and not before; it holds what's done and the first step not done, and nothing past that. */
    static void threadsOpenWhenMet(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-journal-road");
        h.assertValueEqual(names(player), List.of("frontier"), "a newcomer's threads");
        Journal.Thread frontier = thread(player, "frontier");
        h.assertTrue(frontier.done().isEmpty() && frontier.next().equals("frontier/mint_coin"), "a newcomer's frontier page: " + frontier);
        // out of order: what's been done is down, and the next step is still the first one not done
        SurveyTests.award(player, "frontier/lights_in_the_sky");
        frontier = thread(player, "frontier");
        h.assertValueEqual(frontier.done(), List.of("frontier/lights_in_the_sky"), "done on the frontier");
        h.assertValueEqual(frontier.next(), "frontier/mint_coin", "next on the frontier");
        // the Grays wait for a saucer, the Hollow for the gateway, and what comes after for the leash
        h.assertTrue(thread(player, "grays") == null && thread(player, "hollow") == null && thread(player, "after") == null, "pages ahead of the story: " + names(player));
        SurveyTests.award(player, "ufo/root");
        SurveyTests.award(player, "steam/hollow_earth");
        h.assertValueEqual(names(player), List.of("frontier", "steam", "hollow", "grays"), "threads after a saucer and the gateway");
        h.assertValueEqual(thread(player, "grays").next(), "ufo/crash_and_burn", "next with the Grays");
        h.assertTrue(thread(player, "hollow").done().isEmpty() && thread(player, "hollow").next().equals("hollow/phlogiston"), "the Hollow on arriving");
        SurveyTests.award(player, "ufo/cut_loose");
        h.assertTrue(thread(player, "after") != null, "no page for what comes after, with the leash broken");
        // nothing past the next step goes to the client
        JournalPagesPayload pages = Journal.pages(player);
        for (Journal.Thread thread : pages.threads()) {
            ConcavianCodexItem.Chapter chapter = ConcavianCodexItem.CHAPTERS.stream().filter(c -> c.name().equals(thread.name())).findFirst().orElseThrow();
            Set<String> sent = new HashSet<>(thread.done());
            sent.add(thread.next());
            for (String step : chapter.steps()) {
                boolean reached = ConcavianCodexItem.reached(player, step);
                h.assertTrue(sent.contains(step) == (reached || step.equals(thread.next())), thread.name() + " sends " + step + " wrongly");
            }
            h.assertValueEqual(thread.next(), chapter.steps().stream().filter(step -> !ConcavianCodexItem.reached(player, step)).findFirst().orElse(""), "next on " + thread.name());
        }
        // a road walked to its end has no next step
        for (String step : ConcavianCodexItem.CHAPTERS.getFirst().steps()) {
            SurveyTests.award(player, step);
        }
        h.assertValueEqual(thread(player, "frontier").next(), "", "next on a finished road");
        h.succeed();
    }

    /** Steam opens with the Survey's letter and carries the letter's own list, and its heading, until the gateway's open. */
    static void surveyListAndHeading(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-journal-survey");
        h.assertTrue(thread(player, "steam") == null, "steam before the letter");
        player.setData(ModAttachments.SURVEY_LETTER.get(), true);
        Journal.Thread steam = thread(player, "steam");
        h.assertTrue(steam != null && steam.surveyDone().isEmpty(), "steam with the letter: " + steam);
        h.assertValueEqual(steam.surveyNext(), SurveyLetterItem.STEPS.getFirst().name(), "the Survey's first want");
        h.assertValueEqual(steam.craterBlocks(), 0, "a heading with no letter carried");
        SurveyTests.award(player, "steam/head_of_steam");
        steam = thread(player, "steam");
        h.assertValueEqual(steam.surveyDone(), List.of("boiler"), "the Survey's list after a boiler");
        h.assertValueEqual(steam.surveyNext(), "canister", "the Survey's next want");
        h.assertValueEqual(steam.next(), "steam/handcar", "the road's next step");
        // a carried letter knows where the crater is
        ItemStack letter = new ItemStack(ModItems.SURVEY_LETTER.get());
        letter.set(ModDataComponents.SURVEY_GATEWAY.get(), GlobalPos.of(h.getLevel().dimension(), player.blockPosition().offset(0, 0, -1234)));
        player.getInventory().add(letter);
        steam = thread(player, "steam");
        h.assertValueEqual(steam.craterBlocks(), 1200, "blocks to the crater");
        h.assertValueEqual(steam.craterWay(), "north", "the way to the crater");
        // with the gateway open the letter's business is done
        SurveyTests.award(player, "steam/open_sesame");
        steam = thread(player, "steam");
        h.assertTrue(steam.surveyDone().isEmpty() && steam.surveyNext().isEmpty() && steam.craterBlocks() == 0, "the Survey's list after the gateway: " + steam);
        h.succeed();
    }

    /** Only a thread the player has can be pinned; and a journal is handed out once, and only in a town. */
    static void pinAndHandOut(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-journal-pin");
        Journal.pin(player, "grays");
        h.assertValueEqual(Journal.pages(player).pinned(), "", "pinned, of a thread not met");
        Journal.pin(player, "frontier");
        h.assertValueEqual(Journal.pages(player).pinned(), "frontier", "pinned");
        Journal.pin(player, "");
        h.assertValueEqual(Journal.pages(player).pinned(), "", "unpinned");
        h.assertFalse(Journal.carried(player), "a journal from nowhere");
        h.assertFalse(Journal.handOut(h.getLevel(), player), "a journal handed out away from any town");
        h.assertTrue(player.getData(ModAttachments.JOURNAL.get()).given(), "the hand-out wasn't remembered");
        player.getInventory().add(new ItemStack(ModItems.PROSPECTORS_JOURNAL.get()));
        h.assertTrue(Journal.carried(player), "a carried journal isn't seen");
        h.succeed();
    }

    private static int flags(ServerPlayer player, String id) {
        return player.getData(ModAttachments.JOURNAL.get()).creature(id);
    }

    /** A creature has a page on first sight within a few blocks and not before; more once one is killed; and the tag's line once a tagged one is met. */
    static void creatureOnFirstSight(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-journal-seen");
        var skitter = com.hollowfrontier.registry.ModEntities.FERN_SKITTER.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)).offset(0, 0, 14), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        skitter.setNoAi(true);
        JournalSeen.look(player);
        h.assertValueEqual(flags(player, "fern_skitter"), 0, "a page for a skitter fourteen blocks off");
        h.assertTrue(JournalSeen.creatures(player).isEmpty(), "creature pages before any is met");
        skitter.snapTo(player.getX() + 3, player.getY(), player.getZ());
        JournalSeen.look(player);
        h.assertValueEqual(flags(player, "fern_skitter"), JournalData.MET, "a skitter three blocks off");
        h.assertValueEqual(JournalSeen.creatures(player), List.of(new JournalSeen.Creature("fern_skitter", JournalData.MET)), "creature pages sent");
        // killed by the player: what it's good for is known
        skitter.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 100.0F);
        h.assertValueEqual(flags(player, "fern_skitter"), JournalData.MET | JournalData.KNOWN, "a skitter killed");
        // a first kill is a page too
        var raven = com.hollowfrontier.registry.ModEntities.PALE_RAVEN.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)).offset(0, 0, 14), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        raven.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 100.0F);
        h.assertValueEqual(flags(player, "pale_raven"), JournalData.MET | JournalData.KNOWN, "a raven killed from afar");
        // one the Grays have tagged
        var hare = com.hollowfrontier.registry.ModEntities.GLASSBACK_HARE.get().spawn(h.getLevel(), player.blockPosition().offset(2, 0, 0), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        hare.setNoAi(true);
        hare.setEarTag(true);
        JournalSeen.look(player);
        h.assertValueEqual(flags(player, "glassback_hare"), JournalData.MET | JournalData.TAGGED, "a tagged hare met");
        // a cow isn't ours to write up
        net.minecraft.world.entity.EntityTypes.COW.spawn(h.getLevel(), player.blockPosition().offset(1, 0, 1), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        JournalSeen.look(player);
        h.assertValueEqual(player.getData(ModAttachments.JOURNAL.get()).creatures().size(), 3, "creature pages");
        h.succeed();
    }

    /** A strange doing goes in the book of whoever was near, once, and the editor pays for ten of them, once. */
    static void strangeDoingsInRange(GameTestHelper h) {
        ServerPlayer near = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-journal-near");
        ServerPlayer far = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-journal-far");
        far.snapTo(near.getX() + JournalSeen.WITNESS + 40, near.getY(), near.getZ());
        var log = com.hollowfrontier.content.frontier.mystery.MysteryLog.get(h.getLevel());
        long now = h.getLevel().getGameTime();
        log.record(com.hollowfrontier.content.frontier.mystery.MysteryLog.Kind.CROP_CIRCLE, now, near.blockPosition().offset(30, 0, 0));
        log.record(com.hollowfrontier.content.frontier.mystery.MysteryLog.Kind.SIGHTING, now - 5000, near.blockPosition().offset(10, 0, 0));
        for (ServerPlayer player : List.of(near, far)) {
            JournalSeen.witness(h.getLevel(), player, 200);
            JournalSeen.witness(h.getLevel(), player, 200);
        }
        JournalData data = near.getData(ModAttachments.JOURNAL.get());
        h.assertValueEqual(data.strange().size(), 1, "doings in the book of the one who was near (one new, one long past)");
        h.assertValueEqual(data.strange().getFirst().kind(), "crop_circle", "what was written down");
        h.assertValueEqual(data.strange().getFirst().day(), now / 24000 + 1, "the day written down");
        h.assertValueEqual(far.getData(ModAttachments.JOURNAL.get()).strangeCount(), 0, "doings in the book of the one who was far off");
        // the editor's tally: not for nine, not without the book, and once
        for (int i = 0; i < JournalSeen.TALLY - 2; i++) {
            near.setData(ModAttachments.JOURNAL.get(), near.getData(ModAttachments.JOURNAL.get()).witnessed(new JournalData.Strange("sighting", i + 2, i, 0, "")));
        }
        near.getInventory().add(new ItemStack(ModItems.PROSPECTORS_JOURNAL.get()));
        h.assertFalse(JournalSeen.sellTally(near), "the editor paid for nine");
        near.setData(ModAttachments.JOURNAL.get(), near.getData(ModAttachments.JOURNAL.get()).witnessed(new JournalData.Strange("visit", 20, 0, 0, "")));
        near.getInventory().clearContent();
        h.assertFalse(JournalSeen.sellTally(near), "the editor paid with no book to look at");
        near.getInventory().add(new ItemStack(ModItems.PROSPECTORS_JOURNAL.get()));
        h.assertTrue(JournalSeen.sellTally(near), "the editor didn't pay for ten");
        h.assertValueEqual(com.hollowfrontier.content.frontier.bounty.Purse.count(near), JournalSeen.TALLY_PAY, "what the editor paid");
        h.assertFalse(JournalSeen.sellTally(near), "the editor paid twice");
        // only the latest are kept, but the count goes on
        for (int i = 0; i < JournalData.STRANGE_KEPT + 5; i++) {
            near.setData(ModAttachments.JOURNAL.get(), near.getData(ModAttachments.JOURNAL.get()).witnessed(new JournalData.Strange("sighting", 100 + i, i, 0, "")));
        }
        h.assertValueEqual(near.getData(ModAttachments.JOURNAL.get()).strange().size(), JournalData.STRANGE_KEPT, "doings kept");
        h.assertValueEqual(near.getData(ModAttachments.JOURNAL.get()).strangeCount(), JournalSeen.TALLY + JournalData.STRANGE_KEPT + 5, "doings counted");
        h.succeed();
    }

    private static List<String> facts(ServerPlayer player, String person) {
        return JournalSeen.people(player).stream().filter(page -> page.id().equals(person)).map(JournalSeen.Person::facts).findFirst().orElse(null);
    }

    /** A person's page is there once they're met or something is learned of them, and has a paragraph for each thing learned and no more. */
    static void peopleByFact(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-journal-people");
        h.assertTrue(JournalSeen.people(player).isEmpty(), "people pages for a newcomer: " + JournalSeen.people(player));
        var outlaw = com.hollowfrontier.registry.ModEntities.OUTLAW.get().spawn(h.getLevel(), player.blockPosition().offset(3, 0, 0), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        outlaw.setNoAi(true);
        JournalSeen.look(player);
        h.assertValueEqual(facts(player, "outlaws"), List.of("met"), "outlaws, met");
        h.assertTrue(facts(player, "grays") == null && facts(player, "kindled") == null && facts(player, "dusters") == null, "pages for people not met");
        outlaw.discard();
        SurveyTests.award(player, "frontier/dead_or_alive");
        h.assertValueEqual(facts(player, "outlaws"), List.of("met", "bounty"), "outlaws, with a bounty turned in");
        // an advancement alone opens a page (an old world's journal), and the Kindled's story adds to three pages
        SurveyTests.award(player, "ufo/root");
        h.assertValueEqual(facts(player, "grays"), List.of("met"), "the Grays, by a saucer seen");
        SurveyTests.award(player, "concavia/the_truth");
        h.assertValueEqual(facts(player, "outlaws"), List.of("met", "bounty", "truth"), "outlaws, with the truth");
        h.assertValueEqual(facts(player, "grays"), List.of("met", "truth"), "the Grays, with the truth");
        h.assertValueEqual(facts(player, "dusters"), List.of("truth"), "the men in dusters, by the truth alone");
        // the Grays' field notes, carried
        ItemStack notes = new ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
        notes.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT, new net.minecraft.world.item.component.WrittenBookContent(
                net.minecraft.server.network.Filterable.passThrough(com.hollowfrontier.content.ufo.FieldNotes.TITLE), com.hollowfrontier.content.ufo.FieldNotes.AUTHOR, 0, List.of(), true));
        player.getInventory().add(notes);
        JournalSeen.look(player);
        h.assertValueEqual(facts(player, "grays"), List.of("met", "notes", "truth"), "the Grays, with their notes found");
        h.succeed();
    }

    /** Every creature, person, paragraph and strange doing the server can send has its line. */
    static void textCoversWhatIsSeen(GameTestHelper h) {
        Set<String> keys = JournalText.all().keySet();
        for (String id : JournalSeen.CREATURES) {
            h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(com.hollowfrontier.HollowFrontier.id(id)), "no such creature: " + id);
            h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(com.hollowfrontier.HollowFrontier.id(id + "_spawn_egg")), "no egg to sketch " + id + " from");
            h.assertTrue(keys.contains(JournalText.key("creature." + id + ".met")) && keys.contains(JournalText.key("creature." + id + ".known")), "no lines for " + id);
            h.assertValueEqual(keys.contains(JournalText.key("creature." + id + ".tagged")), JournalSeen.TAGGABLE.contains(id), "a tag's line for " + id);
        }
        JournalSeen.PEOPLE.forEach((id, facts) -> {
            h.assertTrue(JournalText.PERSON.containsKey(id), "no name for " + id);
            for (JournalSeen.Fact fact : facts) {
                h.assertTrue(keys.contains(JournalText.key("people." + id + "." + fact.name())), "no paragraph for " + id + "." + fact.name());
            }
        });
        for (var kind : com.hollowfrontier.content.frontier.mystery.MysteryLog.Kind.values()) {
            h.assertTrue(JournalText.STRANGE.containsKey(kind.getSerializedName()), "no line for " + kind);
        }
        h.assertTrue(JournalText.STRANGE.containsKey("visit"), "no line for a visit");
        h.succeed();
    }

    /** A town has a page once walked into, dated, and each line of it is the town's own record. */
    static void townPages(GameTestHelper h) {
        long first = 480_001L, second = 480_002L;
        var level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-journal-towns");
        com.hollowfrontier.content.frontier.stake.Standing.add(player, first, 34);
        h.assertTrue(com.hollowfrontier.content.journal.JournalTowns.towns(player).isEmpty(), "a page for a town never walked into");
        h.assertTrue(com.hollowfrontier.content.journal.JournalTowns.enter(level, player, first), "no page on walking in");
        h.assertFalse(com.hollowfrontier.content.journal.JournalTowns.enter(level, player, first), "a second page for the same town");
        com.hollowfrontier.content.journal.JournalTowns.enter(level, player, second);
        var pages = com.hollowfrontier.content.journal.JournalTowns.towns(player);
        h.assertValueEqual(pages.size(), 2, "town pages");
        var page = pages.stream().filter(p -> p.name().equals(com.hollowfrontier.content.frontier.town.Towns.name(level, first))).findFirst().orElseThrow();
        h.assertValueEqual(page.firstDay(), level.getGameTime() / 24000 + 1, "the day first come");
        h.assertValueEqual(page.points(), 34, "standing");
        h.assertValueEqual(page.rank(), "neighbour", "rank");
        h.assertTrue(page.star() == 0 && page.job().isEmpty() && page.head() == -1 && page.bank() == -1 && page.poster().isEmpty() && page.raidDay() == 0
                && page.driveHead() == -1, "lines for what isn't so: " + page);
        // the star, a job, a claim, a box at the bank, a poster, a raid, a herd out
        com.hollowfrontier.content.frontier.stake.Deputies.swearIn(level, player, first);
        player.setData(ModAttachments.SHERIFF_JOB.get(), new com.hollowfrontier.content.frontier.stake.SheriffJob(
                com.hollowfrontier.content.frontier.stake.SheriffJob.Kind.WARRANT, first, 0, List.of(), java.util.Optional.empty()));
        BlockPos stake = h.absolutePos(new BlockPos(2, 1, 2));
        com.hollowfrontier.content.frontier.stake.StakeData.get(level).put(new com.hollowfrontier.content.frontier.stake.StakeData.Claim(stake, first, player.getUUID(), "Tester", -1));
        var box = com.hollowfrontier.content.frontier.bank.TownBanks.get(level).open(first, player.getUUID(), 0);
        box.setItem(0, new ItemStack(ModItems.GOLD_COIN.get(), 2));
        box.setItem(1, new ItemStack(ModItems.COPPER_COIN.get(), 3));
        var record = com.hollowfrontier.content.frontier.raid.TownRaids.get(level).record(first, level.getGameTime());
        record.poster = java.util.Optional.of(new com.hollowfrontier.content.frontier.bounty.BountyData(java.util.UUID.randomUUID(), "Black Tom", 0, 0, 7, false));
        record.lastRaid = 24000L * 4 + 100;
        player.setData(ModAttachments.DRIVE.get(), new com.hollowfrontier.content.frontier.stake.Drives.Drive(java.util.UUID.randomUUID(), first, java.util.Optional.empty(),
                player.blockPosition().offset(0, 0, 300), 8));
        try {
            page = com.hollowfrontier.content.journal.JournalTowns.towns(player).stream()
                    .filter(p -> p.name().equals(com.hollowfrontier.content.frontier.town.Towns.name(level, first))).findFirst().orElseThrow();
            h.assertValueEqual(page.star(), 1, "the star");
            h.assertValueEqual(page.job(), "warrant", "the job in hand");
            h.assertValueEqual(page.head(), 0, "branded head on the claim");
            h.assertValueEqual(page.bank(), 2 * com.hollowfrontier.content.frontier.bounty.Purse.GOLD + 3, "the bank");
            h.assertValueEqual(page.poster(), "Black Tom", "the poster");
            h.assertValueEqual(page.reward(), 7, "the reward");
            h.assertValueEqual(page.raidDay(), 5L, "the day last raided");
            h.assertValueEqual(page.driveHead(), 0, "head near, of a herd out");
            h.assertValueEqual(page.driveBlocks(), 300, "blocks to go");
            h.assertValueEqual(page.driveWay(), "south", "the way to go");
            var other = com.hollowfrontier.content.journal.JournalTowns.towns(player).stream()
                    .filter(p -> p.name().equals(com.hollowfrontier.content.frontier.town.Towns.name(level, second))).findFirst().orElseThrow();
            h.assertTrue(other.star() == 0 && other.job().isEmpty() && other.head() == -1 && other.bank() == -1 && other.driveHead() == -1 && other.points() == 0,
                    "the first town's business on the second's page: " + other);
            // and it all goes to the client and comes back the same
            var buf = io.netty.buffer.Unpooled.buffer();
            com.hollowfrontier.content.journal.JournalTowns.Town.STREAM_CODEC.encode(buf, page);
            h.assertValueEqual(com.hollowfrontier.content.journal.JournalTowns.Town.STREAM_CODEC.decode(buf), page, "a town page sent and read back");
        } finally {
            com.hollowfrontier.content.frontier.stake.StakeData.get(level).remove(stake);
        }
        h.succeed();
    }

    /** What the journal keeps comes through a death. */
    static void keptOnDeath(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-journal-death");
        player.setData(ModAttachments.JOURNAL.get(), new JournalData("frontier", true));
        ServerPlayer back = h.getLevel().getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
        try {
            h.assertValueEqual(back.getData(ModAttachments.JOURNAL.get()), new JournalData("frontier", true), "the journal after a death");
        } finally {
            TestPlayers.leave(back);
        }
        h.succeed();
    }
}
