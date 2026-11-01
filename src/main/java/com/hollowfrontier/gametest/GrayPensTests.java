package com.hollowfrontier.gametest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BoardPosting;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.hollow.gray.Captive;
import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.content.hollow.gray.ImplantRigBlockEntity;
import com.hollowfrontier.content.hollow.gray.LetterHome;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.HollowBlueprints;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 20: the captives. The pens hold who they should, restrained; the procedure runs and gets up hostile, or stops
 * when its surgeons fall or its rig breaks (and the strapped one can't be freed while it runs); a freed captive hands
 * over a letter that pays in its own town only; a poster's man is found in a cell, walked in alive, and paid for in
 * full at the right board only; and after the leash the pens run no procedure and are cleared like a landing. The
 * world's leash is only broken and mended again inside one tick.
 */
final class GrayPensTests {
    private GrayPensTests() {}

    /** Pens of this variant placed square at the yard's corner, with their people. The rig's position. */
    private static BlockPos pens(GameTestHelper h, int variant) {
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        var template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(HollowBlueprints.pensTemplate(variant))).orElseThrow();
        template.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings().setFinalizeEntities(true), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
        return origin.offset(HollowBlueprints.RIG_X, HollowBlueprints.FLOOR + HollowBlueprints.RIG_Y, HollowBlueprints.RIG_Z);
    }

    private static ImplantRigBlockEntity rig(GameTestHelper h, BlockPos at) {
        if (h.getLevel().getBlockEntity(at) instanceof ImplantRigBlockEntity rig) {
            return rig;
        }
        h.fail("no implant rig at " + at);
        return null;
    }

    private static ItemStack held(ServerPlayer player, net.minecraft.world.item.Item item) {
        for (ItemStack stack : player.getInventory()) {
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static AABB yard(GameTestHelper h) {
        return new AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(48, 28, 48);
    }

    private static <T extends Mob> T captive(GameTestHelper h, net.minecraft.world.entity.EntityType<T> type, BlockPos at, Captive state) {
        T mob = type.create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        mob.snapTo(Vec3.atBottomCenterOf(h.absolutePos(at)));
        mob.setData(ModAttachments.CAPTIVE.get(), state);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }

    /**
     * The pens are a Hollow Earth structure; their fullest version holds four in the cells and one on the table, every one
     * still, unarmed and speaking as a captive; four Wardens keep them, two of them at the table; the rig hangs over it.
     */
    static void pensHoldTheirCaptives(GameTestHelper h) {
        h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ModStructures.GRAY_PENS).isPresent(), "no gray_pens structure");
        BlockPos at = pens(h, 3);
        ServerLevel level = h.getLevel();
        h.assertTrue(level.getBlockState(at).is(ModBlocks.IMPLANT_RIG.get()), "no rig over the table");
        List<Mob> held = level.getEntitiesOfClass(Mob.class, yard(h), Captives::isHeld);
        h.assertValueEqual(held.size(), 5, "captives held");
        h.assertValueEqual(held.stream().filter(Captives::onTable).count(), 1L, "captives on the table");
        h.assertTrue(held.stream().anyMatch(m -> m instanceof Lawman) && held.stream().anyMatch(m -> m instanceof Townsfolk)
                && held.stream().anyMatch(m -> m instanceof AbstractOutlaw), "not the mix of folk: " + held);
        for (Mob mob : held) {
            h.assertTrue(mob.isNoAi() && mob.getMainHandItem().isEmpty(), mob + " isn't restrained");
            h.assertValueEqual(Chatter.voices(mob), List.of("captive"), "voice");
        }
        List<GrayWarden> wardens = level.getEntitiesOfClass(GrayWarden.class, yard(h), w -> w.entityTags().contains(Captives.GUARD));
        h.assertValueEqual(wardens.size(), 4, "wardens keeping it");
        h.assertValueEqual(Captives.surgeons(level, at, 64).size(), 2, "surgeons");
        h.assertTrue(rig(h, at).patient(level, at) != null, "the rig can't find the one on the table");
        level.getEntitiesOfClass(Entity.class, yard(h), e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
        h.succeed();
    }

    /** A player comes; the bar fills; the blacksmith on the table gets up an armed, hostile outlaw. */
    static void procedureImplants(GameTestHelper h) {
        BlockPos at = pens(h, 3);
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(16, 1, 24));
        try {
            ImplantRigBlockEntity rig = rig(h, at);
            Mob patient = rig.patient(level, at);
            h.assertTrue(patient instanceof Townsfolk, "the blacksmith isn't on the table: " + patient);
            rig.watch(level, at);
            h.assertValueEqual(rig.stage(), ImplantRigBlockEntity.Stage.RUNNING, "stage once a player's near");
            h.assertTrue(ImplantRigBlockEntity.operatingOn(level, patient), "the rig isn't at work on him");
            h.assertValueEqual(Captives.free(level, player, patient), "strapped", "freeing him off a running table");
            rig.setProgress(ImplantRigBlockEntity.DURATION - 1);
            ImplantRigBlockEntity.serverTick(level, at, level.getBlockState(at), rig);
            h.assertValueEqual(rig.stage(), ImplantRigBlockEntity.Stage.OVER, "stage after the bar fills");
            h.assertTrue(patient.isRemoved(), "the blacksmith is still a blacksmith");
            List<AbstractOutlaw> got = level.getEntitiesOfClass(AbstractOutlaw.class, new AABB(at).inflate(3, 6, 3), o -> Captives.of(o) == null);
            h.assertTrue(got.size() == 1 && !got.get(0).getMainHandItem().isEmpty() && !got.get(0).isNoAi() && !got.get(0).isFreed(),
                    "no implanted outlaw got up: " + got);
        } finally {
            TestPlayers.leave(player);
            level.getEntitiesOfClass(Entity.class, yard(h), e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
        }
        h.succeed();
    }

    /**
     * Every surgeon down stops it (Not on My Watch), and the one on the table, still held, can then be freed: a letter in
     * hand, and Jailbreak. In a second pen, breaking the rig stops it too.
     */
    static void procedureStops(GameTestHelper h) {
        BlockPos at = pens(h, 1);
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(16, 1, 24));
        try {
            ImplantRigBlockEntity rig = rig(h, at);
            Mob patient = rig.patient(level, at);
            h.assertTrue(rig.start(level, at), "it didn't start");
            Captives.surgeons(level, at, 64).forEach(Entity::discard);
            rig.setProgress(19);
            ImplantRigBlockEntity.serverTick(level, at, level.getBlockState(at), rig);
            h.assertValueEqual(rig.stage(), ImplantRigBlockEntity.Stage.OVER, "stage with the surgeons down");
            h.assertTrue(AdvancementTests.earned(player, "hollow/not_on_my_watch"), "Not on My Watch not earned");
            h.assertTrue(Captives.onTable(patient) && patient.isAlive(), "the one on the table isn't still held");
            h.assertValueEqual(Captives.free(level, player, patient), "freed", "freeing him");
            h.assertTrue(Captives.of(patient) == null && !patient.isNoAi() && ((AbstractOutlaw) patient).isFreed(), "he wasn't let go");
            h.assertTrue(player.getInventory().contains(s -> s.is(ModItems.LETTER_HOME.get()) && s.has(ModDataComponents.LETTER_HOME.get())), "no letter");
            h.assertTrue(AdvancementTests.earned(player, "hollow/jailbreak"), "Jailbreak not earned");
            // A fresh pen: the rig broken mid-procedure
            level.getEntitiesOfClass(Entity.class, yard(h), e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
            at = pens(h, 2);
            rig = rig(h, at);
            h.assertTrue(rig.start(level, at), "the second didn't start");
            Mob second = rig.patient(level, at);
            level.destroyBlock(at, false);
            h.assertValueEqual(rig.stage(), ImplantRigBlockEntity.Stage.OVER, "stage with the rig broken");
            h.assertTrue(Captives.isHeld(second) && second.isAlive(), "the second patient wasn't spared");
        } finally {
            TestPlayers.leave(player);
            level.getEntitiesOfClass(Entity.class, yard(h), e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
        }
        h.succeed();
    }

    /** A letter pays in its own town only (and clears the bringer's heat); out of a town, or in another, it's refused. */
    static void letterGoesHome(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        try {
            Townsfolk folk = captive(h, ModEntities.TOWNSFOLK.get(), new BlockPos(4, 1, 4), Captive.HELD);
            Captives.restrain(folk);
            h.assertValueEqual(Captives.free(level, player, folk), "freed", "freeing a townsperson");
            ItemStack letter = held(player, ModItems.LETTER_HOME.get());
            h.assertTrue(letter.is(ModItems.LETTER_HOME.get()), "no letter handed over");
            letter.set(ModDataComponents.LETTER_HOME.get(), new LetterHome(4242L, 8, 8, "Dry Gulch"));
            Wanted.addHeat(player, 2);
            BlockPos board = h.absolutePos(new BlockPos(2, 1, 2));
            h.assertFalse(Captives.deliverLetter(level, player, letter, board, Optional.empty()), "taken out of town");
            h.assertFalse(Captives.deliverLetter(level, player, letter, board, Optional.of(99L)), "taken in the wrong town");
            int coins = player.getInventory().countItem(ModItems.GOLD_COIN.get());
            h.assertTrue(Captives.deliverLetter(level, player, letter, board, Optional.of(4242L)), "refused in its own town");
            h.assertTrue(letter.isEmpty(), "the letter's still in hand");
            h.assertValueEqual(player.getInventory().countItem(ModItems.GOLD_COIN.get()) - coins, Captives.LETTER_REWARD, "gold paid");
            h.assertValueEqual(Wanted.of(player).heat(), 0, "heat after");
            h.assertTrue(AdvancementTests.earned(player, "hollow/special_delivery"), "Special Delivery not earned");
            h.assertTrue(LetterHome.ANY.isFor(99L) && !new LetterHome(1L, 0, 0, "").isFor(2L), "who a letter's for");
            folk.discard();
        } finally {
            TestPlayers.leave(player);
        }
        h.succeed();
    }

    /**
     * A hunter's poster names a man in a cell; freed by them he goes along in irons, keeps up when left behind, is refused
     * at the wrong board, and at the right one is paid for in full: the poster gone, the board's stamped CAPTURED.
     */
    static void outlawBroughtInAlive(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-bounty-hunter");
        String key = "test:" + UUID.randomUUID();
        try {
            AbstractOutlaw man = captive(h, ModEntities.OUTLAW.get(), new BlockPos(4, 1, 4), Captive.HELD);
            Captives.restrain(man);
            BountyData bounty = new BountyData(UUID.randomUUID(), "Test 'Caged' Calloway", 0, 0, 5, false);
            ItemStack poster = new ItemStack(ModItems.WANTED_POSTER.get());
            poster.set(ModDataComponents.BOUNTY.get(), bounty);
            player.getInventory().add(poster);
            BountyBoards.get(level).put(key, List.of(BoardPosting.open(bounty, false, 0).taken("hf-bounty-hunter")), 0);
            h.assertValueEqual(Captives.recognise(level, player, man.blockPosition()), bounty.target(), "the man on the poster");
            h.assertTrue(held(player, ModItems.WANTED_POSTER.get()).get(ModDataComponents.BOUNTY.get()).captive(), "the poster doesn't say he's held");
            h.assertValueEqual(Captives.free(level, player, man), "escorted", "freeing him");
            h.assertTrue(man.isJailed() && Captives.isPrisonerOf(man, player) && !man.isNoAi(), "he isn't a prisoner");
            // Left behind, he catches up
            player.teleportTo(player.getX() + 30, player.getY(), player.getZ());
            Captives.FollowCaptorGoal follow = new Captives.FollowCaptorGoal(man);
            h.assertTrue(follow.canUse(), "he doesn't follow");
            follow.tick();
            h.assertTrue(man.distanceTo(player) < 3.0, "he didn't keep up: " + man.distanceTo(player));
            int coins = player.getInventory().countItem(ModItems.GOLD_COIN.get());
            h.assertValueEqual(Captives.turnIn(level, player, player.blockPosition(), "elsewhere"), 1, "prisoners near");
            h.assertTrue(man.isAlive() && player.getInventory().countItem(ModItems.GOLD_COIN.get()) == coins, "paid at the wrong board");
            h.assertValueEqual(Captives.turnIn(level, player, player.blockPosition(), key), 1, "prisoners near");
            h.assertTrue(man.isRemoved(), "he wasn't locked up");
            h.assertValueEqual(player.getInventory().countItem(ModItems.GOLD_COIN.get()) - coins, bounty.reward(), "reward paid");
            h.assertFalse(player.getInventory().contains(s -> s.is(ModItems.WANTED_POSTER.get())), "the poster's still carried");
            h.assertValueEqual(BountyBoards.get(level).peek(key).orElseThrow().postings().get(0).status(), BoardPosting.Status.CAPTURED, "the board's stamp");
            h.assertTrue(AdvancementTests.earned(player, "frontier/or_alive"), "...Or Alive not earned");
        } finally {
            BountyBoards.get(level).put(key, List.of(), 0);
            TestPlayers.leave(player);
        }
        h.succeed();
    }

    /** After the leash: a player near starts nothing; with every keeper down, the pen's cleared (Trespassers). */
    static void pensAfterTheLeash(GameTestHelper h) {
        BlockPos at = pens(h, 1);
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(16, 1, 24));
        boolean was = Leash.isBroken(level);
        try {
            Leash.set(level, true);
            ImplantRigBlockEntity rig = rig(h, at);
            rig.watch(level, at);
            h.assertValueEqual(rig.stage(), ImplantRigBlockEntity.Stage.WAITING, "stage after the leash");
            h.assertFalse(rig.cleared(), "cleared with its keepers up");
            Captives.guards(level, at, 64).forEach(Entity::discard);
            rig.watch(level, at);
            h.assertTrue(rig.cleared(), "not cleared with its keepers down");
            h.assertTrue(AdvancementTests.earned(player, "ufo/trespassers"), "Trespassers not earned");
        } finally {
            Leash.set(level, was);
            TestPlayers.leave(player);
            level.getEntitiesOfClass(Entity.class, yard(h), e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
        }
        h.succeed();
    }
}
