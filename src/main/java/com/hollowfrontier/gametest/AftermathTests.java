package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gazette.Aftermath;
import com.hollowfrontier.content.frontier.gazette.GazetteItem;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.outlaw.FreedMen;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.concavia.KindledStanding;
import com.hollowfrontier.content.concavia.Restoration;
import com.hollowfrontier.content.concavia.Waystones;
import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ConcaviaBlueprints;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 15c: what the frontier makes of itself. A freed outlaw hired as a deputy or sent home (and neither out of town,
 * nor by a wanted man); a Crystal Ruins restored stage by stage (its blocks, its Kindled's trades, its waystone); the
 * fountain's refusals and its cost; travel by waystone and its fare; and the Gazette's aftermath page. The world's leash
 * is only broken and mended again inside one tick.
 */
final class AftermathTests {
    private AftermathTests() {}

    private static AbstractOutlaw freedMan(GameTestHelper h, BlockPos at) {
        AbstractOutlaw outlaw = ModEntities.OUTLAW.get().create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        outlaw.snapTo(Vec3.atBottomCenterOf(h.absolutePos(at)));
        h.getLevel().addFreshEntity(outlaw);
        outlaw.setFreed();
        return outlaw;
    }

    /** For a gold coin a freed man becomes a deputy, armed and posted where he stood; the coin is spent and the deed earned. */
    static void freedManSwornIn(GameTestHelper h) {
        AbstractOutlaw man = freedMan(h, new BlockPos(20, 1, 20));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(18, 1, 20));
        ItemStack coins = new ItemStack(ModItems.GOLD_COIN.get(), 3);
        Lawman deputy = FreedMen.hire(h.getLevel(), player, man, coins);
        h.assertTrue(deputy != null && deputy.isAlive(), "no deputy");
        h.assertValueEqual(deputy.getRank(), Lawman.Rank.DEPUTY, "rank");
        h.assertTrue(deputy.getMainHandItem().is(ModItems.SHOTGUN.get()), "the deputy's unarmed");
        h.assertTrue(deputy.hasHome() && deputy.getHomePosition().equals(h.absolutePos(new BlockPos(20, 1, 20))), "the deputy has no post");
        h.assertTrue(man.isRemoved(), "the freed man is still about");
        h.assertValueEqual(coins.getCount(), 3 - FreedMen.WAGE, "coins left");
        h.assertTrue(AdvancementTests.earned(player, "ufo/second_chance"), "A Second Chance not earned");
        h.assertValueEqual(Aftermath.get(h.getLevel()).latest().map(e -> e.kind()).orElse(null), Aftermath.Kind.DEPUTY, "the Gazette's news");
        deputy.discard();
        h.succeed();
    }

    /** Sent home, he's a Prospector; a wanted man can't hire him; out of town, he says to find him one. */
    static void freedManGoesHome(GameTestHelper h) {
        AbstractOutlaw man = freedMan(h, new BlockPos(20, 1, 20));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(18, 1, 20));
        h.assertValueEqual(FreedMen.talk(h.getLevel(), player, man, InteractionHand.MAIN_HAND), "no_town", "talking out of town");
        Wanted.addHeat(player, Wanted.MAX_HEAT);
        ItemStack coin = new ItemStack(ModItems.GOLD_COIN.get());
        h.assertTrue(FreedMen.hire(h.getLevel(), player, man, coin) == null && coin.getCount() == 1 && man.isAlive(), "a wanted man hired him");
        Wanted.clear(player);
        Townsfolk prospector = FreedMen.sendHome(h.getLevel(), player, man);
        h.assertTrue(prospector != null && prospector.getFolk() == Folk.PROSPECTOR, "he didn't settle as a prospector");
        h.assertTrue(man.isRemoved(), "the freed man is still about");
        prospector.discard();
        h.succeed();
    }

    /** A ruin placed square at the yard's corner, and its key (this test's own, so its stage never meets another's). */
    private static Restoration.Ruin ruin(GameTestHelper h) {
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        var template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(ConcaviaBlueprints.CRYSTAL_RUINS)).orElseThrow();
        template.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings().setIgnoreEntities(true), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
        return new Restoration.Ruin(origin.asLong(), origin, Rotation.NONE);
    }

    /**
     * Stage by stage: a fallen tower stands whole, then the sanctum's windows are lit, then the waystone wakes on its dais
     * and joins the network; the Kindled's trades grow with each; past the last, nothing more happens.
     */
    static void ruinRestoresInStages(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Restoration.Ruin ruin = ruin(h);
        BlockPos raised = ruin.toWorld(new BlockPos(34, 12, 8));
        BlockPos window = ruin.toWorld(new BlockPos(ConcaviaBlueprints.SANCTUM_X0, 4, ConcaviaBlueprints.SANCTUM_Z0 + 2));
        BlockPos stone = ruin.toWorld(ConcaviaBlueprints.WAYSTONE);
        h.assertTrue(level.getBlockState(raised).isAir(), "the fallen tower already stands at " + raised);
        h.assertTrue(level.getBlockState(window).is(ModBlocks.CONCAVIAN_CRYSTAL.get()), "no window crystal to light");
        h.assertValueEqual(Restoration.stageOffers(0).size(), 0, "trades before any stage");
        try {
            h.assertValueEqual(Restoration.advance(level, ruin), 1, "stage");
            h.assertTrue(level.getBlockState(raised).is(ModBlocks.CRYSTAL_BRICKS.get()), "the tower wasn't raised");
            h.assertTrue(level.getBlockState(window).is(ModBlocks.CONCAVIAN_CRYSTAL.get()), "the sanctum was lit at stage 1");
            h.assertValueEqual(Restoration.advance(level, ruin), 2, "stage");
            h.assertTrue(level.getBlockState(window).is(ModBlocks.PHLOGISTON_LAMP.get()), "the sanctum wasn't lit");
            h.assertFalse(level.getBlockState(stone).is(ModBlocks.WAYSTONE.get()), "a waystone before the last stage");
            h.assertValueEqual(Restoration.advance(level, ruin), 3, "stage");
            h.assertTrue(level.getBlockState(stone).is(ModBlocks.WAYSTONE.get()), "no waystone at " + stone);
            h.assertTrue(Waystones.get(level).at(level.dimension(), stone).isPresent(), "the waystone didn't wake");
            h.assertValueEqual(Restoration.advance(level, ruin), 3, "stage past the last");
            h.assertValueEqual(Restoration.get(level).stage(ruin), 3, "stage kept");
            List<ItemStack> sold = Restoration.stageOffers(3).stream().map(o -> o.getResult()).toList();
            h.assertTrue(sold.stream().anyMatch(s -> s.is(ModItems.PHLOGISTON_CORE.get())) && sold.stream().anyMatch(s -> s.is(ModItems.PHLOGISTON_CELL.get())),
                    "the last stage's trades: " + sold);
        } finally {
            Waystones.get(level).forget(level.dimension(), stone);
        }
        h.succeed();
    }

    /**
     * The fountain: still while the leash holds, and for anyone but an ally; it wants the whole stage's cost, and takes it.
     * (The leash is broken and mended in this tick.)
     */
    static void fountainWantsAnAllyAndTheCost(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Restoration.Ruin ruin = ruin(h);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 6));
        h.assertTrue(Restoration.onFountain(ruin, ruin.toWorld(ConcaviaBlueprints.FOUNTAIN).east(2)), "the fountain's rim isn't on it");
        h.assertFalse(Restoration.onFountain(ruin, ruin.toWorld(ConcaviaBlueprints.FOUNTAIN).east(8)), "8 off counts as the fountain");
        boolean was = Leash.isBroken(level);
        try {
            Leash.set(level, false);
            h.assertValueEqual(Restoration.restore(level, player, ruin), "not_after", "with the leash whole");
            Leash.set(level, true);
            h.assertValueEqual(Restoration.restore(level, player, ruin), "not_ally", "a stranger");
            KindledStanding.add(player, KindledStanding.ALLIED);
            player.getInventory().add(new ItemStack(ModItems.PHLOGISTON_SHARD.get(), Restoration.PHLOGISTON[1]));
            player.getInventory().add(new ItemStack(ModItems.STARSTEEL_INGOT.get(), Restoration.STARSTEEL[1] - 1));
            h.assertValueEqual(Restoration.restore(level, player, ruin), "short", "a starsteel short");
            player.getInventory().add(new ItemStack(ModItems.STARSTEEL_INGOT.get(), 1));
            h.assertTrue(Restoration.restore(level, player, ruin) == null, "refused with the cost in hand");
            h.assertValueEqual(Restoration.get(level).stage(ruin), 1, "stage");
            h.assertValueEqual(player.getInventory().countItem(ModItems.PHLOGISTON_SHARD.get()) + player.getInventory().countItem(ModItems.STARSTEEL_INGOT.get()), 0,
                    "what's left of the cost");
        } finally {
            Leash.set(level, was);
        }
        h.succeed();
    }

    /** A waystone carries a player to another for a phlogiston shard, and won't without one; a lone stone has nowhere to send them. */
    static void waystonesCarryForAFare(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos a = h.absolutePos(new BlockPos(4, 1, 4)), b = h.absolutePos(new BlockPos(30, 1, 30));
        h.setBlock(new BlockPos(4, 1, 4), ModBlocks.WAYSTONE.get());
        h.setBlock(new BlockPos(30, 1, 30), ModBlocks.WAYSTONE.get());
        Waystones network = Waystones.get(level);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(4, 1, 6));
        try {
            Waystones.Stone first = network.wake(level.dimension(), a, a.south(2));
            h.assertTrue(network.fromStone(level, first).stream().noneMatch(d -> d.at().equals(a.south(2))), "a stone lists itself");
            Waystones.Stone second = network.wake(level.dimension(), b, b.south(2));
            h.assertTrue(network.wake(level.dimension(), b, b.south(2)) == second, "a stone woke twice");
            Waystones.Destination there = network.fromStone(level, first).stream().filter(d -> d.at().equals(b.south(2))).findFirst().orElseThrow();
            h.assertFalse(Waystones.travel(player, there), "travelled without the fare");
            player.getInventory().add(new ItemStack(ModItems.PHLOGISTON_SHARD.get(), 2));
            h.assertTrue(Waystones.travel(player, there), "didn't travel with the fare");
            h.assertTrue(player.blockPosition().equals(b.south(2)), "came out at " + player.blockPosition() + ", not " + b.south(2));
            h.assertValueEqual(player.getInventory().countItem(ModItems.PHLOGISTON_SHARD.get()), 2 - Waystones.FARE, "shards left");
            h.assertTrue(AdvancementTests.earned(player, "concavia/old_roads"), "Old Roads not earned");
        } finally {
            network.forget(level.dimension(), a);
            network.forget(level.dimension(), b);
        }
        h.succeed();
    }

    /** With the leash broken, the Gazette prints the latest aftermath news (a freed man sworn in); with it whole, none. */
    static void gazetteReportsTheAftermath(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer reader = TestPlayers.survival(h, new BlockPos(4, 1, 4));
        boolean was = Leash.isBroken(level);
        try {
            Leash.set(level, false);
            h.assertFalse(prints(level, reader, "gazette.hollowfrontier.aftermath."), "aftermath news with the leash whole");
            Leash.set(level, true);
            Aftermath.get(level).record(level, Aftermath.Kind.DEPUTY, "Tester", h.absolutePos(BlockPos.ZERO));
            h.assertTrue(prints(level, reader, "gazette.hollowfrontier.aftermath.deputy.title"), "no deputy story");
        } finally {
            Leash.set(level, was);
        }
        h.succeed();
    }

    private static boolean prints(ServerLevel level, ServerPlayer reader, String keyStart) {
        return GazetteItem.print(level, reader).pages().stream().anyMatch(page -> mentions(page.raw(), keyStart));
    }

    private static boolean mentions(Component component, String keyStart) {
        if (component.getContents() instanceof TranslatableContents t && t.getKey().startsWith(keyStart)) {
            return true;
        }
        return component.getSiblings().stream().anyMatch(c -> mentions(c, keyStart));
    }
}
