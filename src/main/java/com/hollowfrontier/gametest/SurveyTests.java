package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BoardFace;
import com.hollowfrontier.content.frontier.bounty.BoardPosting;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.survey.SurveyBounty;
import com.hollowfrontier.content.frontier.survey.SurveyLetterItem;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.frontier.talk.Fact;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// 32d: the road north: the Polar Survey's letter, its notice on the bounty boards, and the nudges toward the gateway.
final class SurveyTests {
    private SurveyTests() {}

    static void award(ServerPlayer player, String name) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(HollowFrontier.id(name));
        for (String criterion : advancement.value().criteria().keySet()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().countItem(item);
    }

    /** A postmaster hands a newcomer the letter once, and none at all to someone who has already opened a gateway. */
    static void postmasterHandsOverTheLetter(GameTestHelper h) {
        Townsfolk postmaster = ModEntities.TOWNSFOLK.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)), EntitySpawnReason.SPAWN_ITEM_USE);
        postmaster.setFolk(Folk.POSTMASTER);
        postmaster.setNoAi(true);
        ServerPlayer newcomer = TestPlayers.survival(h, new BlockPos(4, 1, 2), "hf-survey-newcomer");
        postmaster.mobInteract(newcomer, InteractionHand.MAIN_HAND);
        h.assertValueEqual(count(newcomer, ModItems.SURVEY_LETTER.get()), 1, "letters after one visit");
        h.assertTrue(newcomer.getData(ModAttachments.SURVEY_LETTER.get()), "the hand-over wasn't remembered");
        postmaster.mobInteract(newcomer, InteractionHand.MAIN_HAND);
        h.assertValueEqual(count(newcomer, ModItems.SURVEY_LETTER.get()), 1, "letters after a second visit");
        // and once they have it, nobody tells them the letter's still waiting
        Context had = Context.of(h.getLevel(), h.absolutePos(new BlockPos(4, 1, 2)), newcomer);
        h.assertTrue(had.has(Fact.SURVEY_LETTER), "no SURVEY_LETTER once the letter's handed over");
        h.assertTrue(com.hollowfrontier.content.frontier.talk.Chatter.candidates(List.of("postmaster", "townsfolk"),
                        com.hollowfrontier.content.frontier.talk.Moment.TALK, had).stream()
                .noneMatch(line -> line.text().contains("Polar Survey")), "still told about the waiting letter");

        ServerPlayer veteran = TestPlayers.survival(h, new BlockPos(6, 1, 2), "hf-survey-veteran");
        award(veteran, "steam/open_sesame");
        postmaster.mobInteract(veteran, InteractionHand.MAIN_HAND);
        h.assertValueEqual(count(veteran, ModItems.SURVEY_LETTER.get()), 0, "letters for someone who has opened a gateway");
        postmaster.discard();
        h.succeed();
    }

    /** The letter ticks off what the Survey asked for as the reader gets it, and says what's left. */
    static void letterTicksOffTheRoad(GameTestHelper h) {
        ServerPlayer reader = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-survey-reader");
        ItemStack letter = new ItemStack(ModItems.SURVEY_LETTER.get());
        String before = text(SurveyLetterItem.write(reader, letter));
        h.assertFalse(before.contains("✔"), "ticked off with nothing done: " + before);
        award(reader, "steam/head_of_steam");
        award(reader, "steam/airship");
        reader.getInventory().add(new ItemStack(ModItems.POLAR_RESONATOR.get()));
        String after = text(SurveyLetterItem.write(reader, letter));
        long ticks = after.chars().filter(c -> c == '✔').count();
        h.assertValueEqual((int) ticks, 3, "steps ticked off (boiler, airship, resonator)");
        h.assertTrue(after.contains("book.hollowfrontier.survey_letter.step.compass.how"), "no word on finding a compass");
        h.assertFalse(after.contains("book.hollowfrontier.survey_letter.step.airship.how"), "still says how to get an airship");
        reader.setItemInHand(InteractionHand.MAIN_HAND, letter);
        letter.use(h.getLevel(), reader, InteractionHand.MAIN_HAND);
        h.assertTrue(letter.has(DataComponents.WRITTEN_BOOK_CONTENT), "using it wrote nothing");
        h.succeed();
    }

    // The book's pages as their keys (translatable) and plain text, to look for ticks and steps
    private static String text(net.minecraft.world.item.component.WrittenBookContent book) {
        StringBuilder out = new StringBuilder();
        for (var page : book.pages()) {
            page.raw().visit((style, part) -> {
                out.append(part);
                return java.util.Optional.empty();
            }, net.minecraft.network.chat.Style.EMPTY);
            keys(page.raw(), out);
        }
        return out.toString();
    }

    private static void keys(net.minecraft.network.chat.Component component, StringBuilder out) {
        if (component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
            out.append(' ').append(t.getKey());
        }
        component.getSiblings().forEach(sibling -> keys(sibling, out));
    }

    /**
     * The notice takes the last free cell on a board (not when the board is full); used, it hands the letter to anyone due
     * it, and pays its reward once to a player who has opened a gateway.
     */
    static void surveyNoticePaysOnce(GameTestHelper h) {
        List<BoardPosting> outlaws = List.of(posting("A"), posting("B"), posting("C"));
        List<BoardFace.Slot> roomy = new BoardFace(outlaws, List.of()).slots(2, 2);
        h.assertTrue(roomy.size() == 4 && roomy.get(3).kind() == BoardFace.Kind.SURVEY, "the notice should take the fourth cell: " + roomy);
        List<BoardFace.Slot> full = new BoardFace(outlaws, List.of(new BoardFace.WantedFace("Somebody", 40))).slots(2, 2);
        h.assertTrue(full.stream().noneMatch(s -> s.kind() == BoardFace.Kind.SURVEY), "the notice crowded out a wanted poster");

        ServerLevel level = h.getLevel();
        Vec3 at = h.absoluteVec(new Vec3(2, 1, 2));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-survey-hunter");
        SurveyBounty.use(level, player, at);
        h.assertValueEqual(count(player, ModItems.SURVEY_LETTER.get()), 1, "letters from the notice");
        h.assertValueEqual(count(player, ModItems.GOLD_COIN.get()), 0, "paid before opening a gateway");
        award(player, "steam/open_sesame");
        SurveyBounty.use(level, player, at);
        h.assertValueEqual(count(player, ModItems.GOLD_COIN.get()), SurveyBounty.REWARD, "the reward");
        SurveyBounty.use(level, player, at);
        h.assertValueEqual(count(player, ModItems.GOLD_COIN.get()), SurveyBounty.REWARD, "paid twice");
        h.succeed();
    }

    private static BoardPosting posting(String name) {
        return BoardPosting.open(new BountyData(java.util.UUID.randomUUID(), name, 0, 0, 5, false), false, 0);
    }

    /**
     * The nudges: townsfolk know a listener has an airship and hasn't gone north (and stop once they have), and the
     * prospector sells a Polar Compass for gold.
     */
    static void frontierNudgesNorth(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-survey-flier");
        BlockPos here = h.absolutePos(new BlockPos(2, 1, 2));
        h.assertFalse(Context.of(level, here, player).has(Fact.AIRSHIP), "AIRSHIP without an airship");
        award(player, "steam/airship");
        h.assertTrue(Context.of(level, here, player).has(Fact.AIRSHIP), "no AIRSHIP with an airship built");
        award(player, "steam/open_sesame");
        h.assertFalse(Context.of(level, here, player).has(Fact.AIRSHIP), "AIRSHIP after the gateway's open");

        Townsfolk prospector = ModEntities.TOWNSFOLK.get().spawn(level, h.absolutePos(new BlockPos(4, 1, 4)), EntitySpawnReason.SPAWN_ITEM_USE);
        prospector.setFolk(Folk.PROSPECTOR);
        prospector.setNoAi(true);
        h.assertTrue(prospector.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.POLAR_COMPASS.get()) && o.getCostA().is(ModItems.GOLD_COIN.get())),
                "the prospector doesn't sell a Polar Compass");
        prospector.discard();
        h.succeed();
    }
}
