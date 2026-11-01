package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.content.frontier.gazette.GazetteItem;
import com.hollowfrontier.content.frontier.mystery.ManInBlack;
import com.hollowfrontier.content.frontier.mystery.MenInBlack;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 11b: the men in black dusters. Suspicion rising, crossing its lines and fading; a visit booked by a mystery and
 * paid; the Gazette printing a correction after; buying the Grays' things back; a word of warning; shooting a marked
 * player and leaving the rest be; sent hunting; what killing the Grays' things does. On the 40-block field.
 */
final class MenInBlackTests {
    private MenInBlackTests() {}

    /** Suspicion rises to its lines and fades a point at a time; a creative player never draws any. */
    static void suspicionRisesAndFades(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        Suspicion.add(player, 30);
        h.assertTrue(Suspicion.warned(player) && !Suspicion.hunted(player), "30 should be warned, not hunted");
        Suspicion.add(player, 40);
        h.assertTrue(Suspicion.hunted(player), "70 should be hunted");
        Suspicion.add(player, 500);
        h.assertValueEqual(Suspicion.level(player), Suspicion.MAX, "it should top out");
        long now = h.getLevel().getGameTime();
        Suspicion.set(player, new Suspicion(Suspicion.MAX, now - Suspicion.COOL_TICKS, 0), Suspicion.MAX);
        Suspicion.tick(player);
        h.assertValueEqual(Suspicion.level(player), Suspicion.MAX - 1, "it should fade a point");
        ServerPlayer creative = TestPlayers.creative(h, new BlockPos(10, 1, 10));
        Suspicion.add(creative, 50);
        h.assertValueEqual(Suspicion.level(creative), 0, "a creative player drew suspicion");
        h.succeed();
    }

    /** A mystery books a visit for half a day on; due, they come, and the visit is marked done. */
    static void visitFollowsMystery(GameTestHelper h) {
        TestPlayers.survival(h, new BlockPos(20, 1, 20));
        BlockPos spot = h.absolutePos(new BlockPos(20, 1, 20));
        MysteryLog log = MysteryLog.get(h.getLevel());
        long now = h.getLevel().getGameTime();
        log.record(MysteryLog.Kind.SIGHTING, now, spot);
        h.assertTrue(log.dueVisit().isPresent() && log.dueVisit().get().time() == now + MenInBlack.VISIT_DELAY, "the visit wasn't booked");
        log.bookVisit(now, spot); // due at once, for the test
        List<ManInBlack> came = MenInBlack.visitIfDue(h.getLevel());
        h.assertTrue(!came.isEmpty() && came.size() <= 2, "nobody came: " + came.size());
        h.assertTrue(log.dueVisit().isEmpty() && log.lastVisit().isPresent(), "the visit wasn't marked done");
        h.assertTrue(came.stream().allMatch(m -> Math.hypot(m.getX() - spot.getX(), m.getZ() - spot.getZ()) < 20 && !m.isHunting()), "they came too far off, or hunting");
        came.forEach(ManInBlack::discard);
        h.succeed();
    }

    private static boolean mentions(Component component, String prefix) {
        if (component.getContents() instanceof TranslatableContents t && t.getKey().startsWith(prefix)) {
            return true;
        }
        return component.getSiblings().stream().anyMatch(c -> mentions(c, prefix));
    }

    /** After their visit, the Gazette prints a correction in place of the report. */
    static void gazettePrintsCorrection(GameTestHelper h) {
        ServerPlayer reader = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        MysteryLog log = MysteryLog.get(h.getLevel());
        long now = h.getLevel().getGameTime();
        // Stamped ahead, so these are the newest round here whatever the neighbouring tests log
        log.record(MysteryLog.Kind.CROP_CIRCLE, now + 3000, reader.blockPosition());
        log.visited(now + 3001, reader.blockPosition());
        WrittenBookContent paper = GazetteItem.print(h.getLevel(), reader);
        h.assertTrue(paper.pages().stream().anyMatch(p -> mentions(p.raw(), "gazette.hollowfrontier.mystery.correction.crop_circle")),
                "no correction in the Gazette");
        h.succeed();
    }

    /** Held out to one, the Grays' things are bought (the stack, in gold), and he minds you less. */
    static void theyBuyItBack(GameTestHelper h) {
        ManInBlack man = h.spawn(ModEntities.MAN_IN_BLACK.get(), new BlockPos(20, 1, 22));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        Suspicion.add(player, 20); // under the warning line
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRAY_TECH_FRAGMENT.get(), 3));
        man.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertFalse(player.getInventory().hasAnyMatching(s -> s.is(ModItems.GRAY_TECH_FRAGMENT.get())), "the fragments weren't taken");
        h.assertValueEqual(player.getInventory().countItem(ModItems.GOLD_COIN.get()), 2, "gold paid for three fragments");
        h.assertValueEqual(Suspicion.level(player), 0, "suspicion after selling");
        h.succeed();
    }

    /** A player they're wary of, close by, gets a word; one they've marked gets shot at; one they don't mind is left be. */
    static void warningThenGunfire(GameTestHelper h) {
        ManInBlack man = h.spawn(ModEntities.MAN_IN_BLACK.get(), new BlockPos(20, 1, 20));
        ServerPlayer wary = TestPlayers.survival(h, new BlockPos(20, 1, 24));
        Suspicion.add(wary, Suspicion.WARN + 5);
        ServerPlayer clean = TestPlayers.survival(h, new BlockPos(16, 1, 20));
        boolean[] warnedFirst = {false};
        h.onEachTick(() -> {
            if (man.hasWarned(wary) && !warnedFirst[0]) {
                warnedFirst[0] = true;
                h.assertTrue(man.getTarget() == null, "he shouldn't shoot a merely wary player");
                Suspicion.add(wary, Suspicion.HOSTILE); // now marked
            }
        });
        h.succeedWhen(() -> h.assertTrue(warnedFirst[0] && man.getTarget() == wary && man.getTarget() != clean,
                "warned " + warnedFirst[0] + ", target " + man.getTarget()));
    }

    /** Sent hunting, they come from 20-30 blocks off, after the marked player. */
    static void sentHunting(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        Suspicion.add(player, Suspicion.HOSTILE + 10);
        List<ManInBlack> came = MenInBlack.arrive(h.getLevel(), player.blockPosition(), 2, player);
        h.assertTrue(!came.isEmpty(), "no hunters came");
        h.assertTrue(came.stream().allMatch(m -> m.isHunting() && m.getTarget() == player), "they aren't hunting the player");
        came.forEach(ManInBlack::discard);
        h.succeed();
    }

    /** Killing a probe (or a tagged beast) is noticed. */
    static void killingTheirThingsIsNoticed(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        GrayProbe probe = h.spawn(ModEntities.GRAY_PROBE.get(), new BlockPos(20, 4, 26));
        probe.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 100.0F);
        h.assertValueEqual(Suspicion.level(player), Suspicion.KILL_PROBE, "suspicion after killing a probe");
        h.succeed();
    }
}
