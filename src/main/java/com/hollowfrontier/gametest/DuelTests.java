package com.hollowfrontier.gametest;

import java.util.List;
import java.util.UUID;

import com.hollowfrontier.content.frontier.bounty.BoardPosting;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.content.frontier.duel.DuelData;
import com.hollowfrontier.content.frontier.duel.Duels;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.frontier.outlaw.OutlawBoss;
import com.hollowfrontier.content.frontier.stake.Standing;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// 51, high noon: who is called out and when, the locked trigger and the flinch, the free gun and the lent one, and both
// endings. The test world has no towns, so each test names its own (a key nothing else uses) and its own two marks.
final class DuelTests {
    private DuelTests() {}

    private static Duels.Ground ground(GameTestHelper h) {
        return new Duels.Ground(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 1, 1))), Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 1, 8))));
    }

    // A bounty whose boss is one who calls out (or one who isn't)
    private static BountyData bounty(int reward, boolean callsOut) {
        while (true) {
            BountyData bounty = new BountyData(UUID.randomUUID(), "Test Pete", 0, 0, reward, false);
            if (Duels.callsOut(bounty) == callsOut) {
                return bounty;
            }
        }
    }

    private static ItemStack poster(ServerPlayer player, BountyData bounty) {
        ItemStack poster = new ItemStack(ModItems.WANTED_POSTER.get());
        poster.set(ModDataComponents.BOUNTY.get(), bounty);
        player.getInventory().setItem(8, poster);
        return poster;
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    private static boolean anyLent(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (Duels.lent(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private static void fire(GameTestHelper h, ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        h.assertTrue(held.getItem() instanceof GunItem, "no gun in hand: " + held);
        held.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
    }

    private static boolean rooted(ServerPlayer player) {
        return player.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 0.0;
    }

    /** One poster in three or so; only near noon; twice at most and a day apart; never once he's been found at his camp. His speed and his price go by the bounty. */
    static void calledOutByTheRule(GameTestHelper h) {
        int calls = 0;
        for (int i = 0; i < 3000; i++) {
            if (Duels.callsOut(UUID.randomUUID(), 0.33)) {
                calls++;
            }
        }
        h.assertTrue(calls > 840 && calls < 1140, "one in three posters should call out: " + calls + " of 3000");
        h.assertFalse(Duels.callsOut(UUID.randomUUID(), 0.0), "called out with the chance at nothing");
        BountyData caller = bounty(4, true), quiet = bounty(4, false);
        DuelData fresh = DuelData.NONE;
        h.assertTrue(Duels.mayAsk(fresh, caller, 5900, 3), "not asked just before noon");
        h.assertFalse(Duels.mayAsk(fresh, quiet, 5900, 3), "a boss who doesn't call out asked");
        h.assertFalse(Duels.mayAsk(fresh, caller, Duels.ASK_FROM - 1, 3), "asked too early");
        h.assertFalse(Duels.mayAsk(fresh, caller, Duels.ASK_UNTIL, 3), "asked too late");
        h.assertFalse(Duels.mayAsk(fresh, caller, 18000, 3), "asked at midnight");
        h.assertFalse(Duels.mayAsk(fresh, caller.markSpawned(), 5900, 3), "asked though he's at his camp");
        DuelData once = fresh.asked(caller.id(), 3);
        h.assertFalse(Duels.mayAsk(once, caller, 5900, 3), "asked twice in a day");
        h.assertTrue(Duels.mayAsk(once, caller, 5900, 4), "not asked again the next day");
        h.assertFalse(Duels.mayAsk(once.asked(caller.id(), 4), caller, 5900, 5), "asked a third time");
        h.assertFalse(Duels.mayAsk(fresh.closed(caller.id()), caller, 5900, 9), "asked after it was fought");

        h.assertValueEqual(Duels.reactionTicks(Duels.CHEAP), Duels.SLOWEST, "the cheapest boss's reaction");
        h.assertValueEqual(Duels.reactionTicks(1), Duels.SLOWEST, "a cheaper boss's reaction");
        h.assertValueEqual(Duels.reactionTicks(Duels.DEAR), Duels.FASTEST, "the dearest boss's reaction");
        h.assertValueEqual(Duels.reactionTicks(40), Duels.FASTEST, "a dearer boss's reaction");
        for (int reward = Duels.CHEAP; reward < Duels.DEAR; reward++) {
            h.assertTrue(Duels.reactionTicks(reward + 1) <= Duels.reactionTicks(reward), "a dearer boss was slower at " + reward);
        }
        h.assertValueEqual(Duels.raised(4), 6, "half as much again on 4");
        h.assertValueEqual(Duels.raised(3), 5, "half as much again on 3");
        h.succeed();
    }

    /** The question can be refused for nothing; yes starts it and the poster reads "spotted"; called off, the poster is as it was. */
    static void askedAndAnswered(GameTestHelper h) {
        long town = 510_001L;
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 6), "hf-duel-asked");
        BountyData bounty = bounty(4, true);
        ItemStack poster = poster(player, bounty);
        Duels.ask(player, bounty, town, ground(h), 3);
        h.assertTrue(Duels.asked(player), "not asked");
        h.assertValueEqual(DuelData.of(player).times(bounty.id()), 1, "times asked");
        h.assertTrue(Duels.answer(player, false) == null && !Duels.asked(player) && Duels.of(player) == null, "no started a duel");
        h.assertFalse(poster.get(ModDataComponents.BOUNTY.get()).spawned(), "a refusal touched the poster");
        h.assertTrue(Duels.answer(player, true) == null, "an answer to nothing started a duel");

        Duels.ask(player, bounty, town, ground(h), 4);
        Duels.Duel duel = Duels.answer(player, true);
        h.assertTrue(duel != null && Duels.of(player) == duel, "yes didn't start a duel");
        h.assertTrue(player.position().distanceTo(ground(h).player()) < 0.1, "not set on the mark: " + player.position());
        h.assertTrue(rooted(player), "free to walk off the mark");
        h.assertTrue(poster.get(ModDataComponents.BOUNTY.get()).spawned(), "his camp would still fill while he's in the street");
        h.assertFalse(Duels.mayAsk(DuelData.of(player), bounty, 5900, 9), "asked a third time");
        duel.runTo(10);
        OutlawBoss boss = duel.boss();
        h.assertTrue(boss != null && boss.isAlive() && boss.isDuelling() && boss.isNoAi(), "no boss on his mark");
        h.assertTrue(boss.getMainHandItem().is(ModItems.REVOLVER.get()), "he didn't put up the shotgun: " + boss.getMainHandItem());
        duel.leave();
        h.assertTrue(duel.over() && Duels.of(player) == null && !boss.isAlive(), "called off, and still on");
        h.assertFalse(poster.get(ModDataComponents.BOUNTY.get()).spawned(), "called off, and his camp stays empty");
        h.assertFalse(rooted(player), "still rooted after it was called off");
        h.succeed();
    }

    /** The player's own gun is fetched to hand, fires for nothing, and goes back as it was; with none, the town's is lent and taken back, however the duel ends. */
    static void gunFreeAndLentGunBack(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 6), "hf-duel-gun");
        Inventory inventory = player.getInventory();
        ItemStack revolver = new ItemStack(ModItems.REVOLVER.get());
        GunItem.setAmmo(revolver, 2);
        inventory.setItem(5, revolver);
        inventory.setItem(0, new ItemStack(Items.BREAD));
        inventory.setItem(1, new ItemStack(ModItems.REVOLVER_CARTRIDGE.get(), 3));
        inventory.setItem(2, new ItemStack(ModItems.PERCUSSION_CAP.get(), 3));
        inventory.setSelectedSlot(0);
        int wear = revolver.getDamageValue();

        Duels.Duel duel = Duels.begin(h.getLevel(), player, bounty(4, true), null, ground(h));
        h.assertTrue(player.getMainHandItem() == revolver, "their own revolver isn't in hand: " + player.getMainHandItem());
        h.assertValueEqual(GunItem.getAmmo(revolver), 6, "loaded for the duel");
        duel.runTo(duel.drawAt());
        player.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition().add(0, 10, 0)); // at the sky: nobody is hit
        fire(h, player);
        fire(h, player);
        h.assertValueEqual(GunItem.getAmmo(revolver), 6, "a duel's shots spent rounds");
        h.assertValueEqual(revolver.getDamageValue(), wear, "a duel's shots wore the gun");
        h.assertTrue(duel.outcome() == Duels.Outcome.NONE, "two shots at the sky decided it: " + duel.outcome());
        duel.leave();
        h.assertTrue(inventory.getItem(5) == revolver && inventory.getItem(0).is(Items.BREAD), "the gun and the bread aren't back where they were");
        h.assertValueEqual(GunItem.getAmmo(revolver), 2, "what was loaded before");
        h.assertTrue(count(player, ModItems.REVOLVER_CARTRIDGE.get()) == 3 && count(player, ModItems.PERCUSSION_CAP.get()) == 3, "cartridges or caps were used");

        // No handgun: the town's, taken back when it's called off
        inventory.setItem(5, ItemStack.EMPTY);
        duel = Duels.begin(h.getLevel(), player, bounty(4, true), null, ground(h));
        h.assertTrue(Duels.lent(player.getMainHandItem()) && GunItem.getAmmo(player.getMainHandItem()) == 6, "no loaded revolver lent: " + player.getMainHandItem());
        h.assertValueEqual(count(player, Items.BREAD), 1, "the bread that was in hand");
        duel.leave();
        h.assertFalse(anyLent(player), "the lent revolver was kept after it was called off");

        // Taken back when it's won
        duel = Duels.begin(h.getLevel(), player, bounty(4, true), null, ground(h));
        duel.runTo(duel.drawAt());
        player.lookAt(EntityAnchorArgument.Anchor.EYES, duel.boss().position().add(0, 0.9, 0));
        fire(h, player);
        h.assertTrue(duel.outcome() == Duels.Outcome.WON, "a shot at his middle didn't win it: " + duel.outcome());
        duel.runTo(duel.endAt());
        h.assertTrue(duel.over() && !anyLent(player), "the lent revolver was kept after a win");

        // And when the player leaves the game with it
        duel = Duels.begin(h.getLevel(), player, bounty(4, true), null, ground(h));
        h.assertTrue(anyLent(player), "nothing lent the fourth time");
        TestPlayers.leave(player);
        h.assertTrue(duel.over() && !anyLent(player), "the lent revolver left the game with them");
        h.succeed();
    }

    /** Before the draw the gun doesn't fire and a pull costs half a second, a held trigger once; after it, it's locked that much longer. */
    static void flinchCostsTime(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 6), "hf-duel-flinch");
        Duels.Duel duel = Duels.begin(h.getLevel(), player, bounty(4, true), null, ground(h));
        duel.landsNext = false;
        duel.runTo(50);
        OutlawBoss boss = duel.boss();
        player.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(0, 0.9, 0));
        fire(h, player);
        h.assertTrue(boss.isAlive() && duel.outcome() == Duels.Outcome.NONE, "a shot before the draw landed");
        h.assertValueEqual(duel.flinches(), 1, "flinches after one pull");
        duel.runTo(54);
        fire(h, player); // the same held trigger
        h.assertValueEqual(duel.flinches(), 1, "flinches for a held trigger");
        duel.runTo(54 + Duels.FLINCH_GAP);
        fire(h, player);
        h.assertValueEqual(duel.flinches(), 2, "flinches after a second pull");
        h.assertValueEqual(duel.unlockAt(), duel.drawAt() + 2 * Duels.FLINCH_TICKS, "when the gun comes free");
        duel.runTo(duel.unlockAt() - 1);
        fire(h, player);
        h.assertTrue(boss.isAlive() && duel.flinches() == 2, "the gun fired while a flinch was being paid for");
        duel.runTo(duel.unlockAt());
        fire(h, player);
        h.assertTrue(duel.outcome() == Duels.Outcome.WON && !boss.isAlive(), "the gun didn't come free: " + duel.outcome());
        h.succeed();
    }

    /** The first hit wins it: he's dead whatever his health, his badge pays half as much again, the town thinks better of the winner, and nobody else was hurt. */
    static void wonInTheStreet(GameTestHelper h) {
        long town = 510_002L;
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 6), "hf-duel-won");
        ItemStack revolver = new ItemStack(ModItems.REVOLVER.get());
        player.getInventory().setItem(0, revolver);
        player.getInventory().setSelectedSlot(0);
        Standing.add(player, town, 10);
        BountyData bounty = bounty(4, true);
        poster(player, bounty);
        Cow cow = h.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(6, 1, 1));
        Duels.Duel duel = Duels.begin(h.getLevel(), player, bounty, town, ground(h));
        duel.landsNext = false;
        duel.runTo(duel.drawAt());
        OutlawBoss boss = duel.boss();
        float full = boss.getHealth();
        boss.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 30.0F);
        player.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 5.0F);
        h.assertTrue(boss.getHealth() == full && player.getHealth() == player.getMaxHealth(), "somebody outside the duel hurt one of the two");
        // A stray shot does a bystander no harm
        player.lookAt(EntityAnchorArgument.Anchor.EYES, cow.position().add(0, 0.7, 0));
        fire(h, player);
        h.assertTrue(cow.getHealth() == cow.getMaxHealth(), "a duellist's stray shot hurt a cow");
        // To the head, but not with the first shot
        player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(boss.getX(), boss.getEyeY(), boss.getZ()));
        fire(h, player);
        h.assertTrue(duel.outcome() == Duels.Outcome.WON && !boss.isAlive(), "a hit didn't end it: " + duel.outcome());
        h.assertFalse(duel.cleanKill(), "a second shot counted as the first");
        DuelData data = DuelData.of(player);
        h.assertTrue(data.wins() == 1 && data.won(town) == 1 && data.last() == DuelData.WON && data.losses() == 0, "the win isn't on record: " + data);
        h.assertValueEqual(Standing.of(player).points(town), 10 + Duels.WON_STANDING, "standing after a win");
        h.runAfterDelay(2, () -> {
            List<ItemEntity> badges = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(2, 1, 8))).inflate(4),
                    e -> e.getItem().is(ModItems.OUTLAW_BADGE.get()));
            h.assertValueEqual(badges.size(), 1, "badges on the ground");
            h.assertValueEqual(badges.getFirst().getItem().get(ModDataComponents.BOUNTY.get()).reward(), Duels.raised(bounty.reward()), "what his badge pays");
            badges.forEach(ItemEntity::discard);
            duel.runTo(duel.endAt());
            h.assertTrue(duel.over() && !rooted(player) && Duels.of(player) == null, "it didn't end");
            h.assertFalse(Duels.mayAsk(DuelData.of(player), bounty, 5900, 99), "a dead man asked again");
            h.succeed();
        });
    }

    /** He fires when his reaction says, not before; hit, the loser is left at one heart, a tenth of their coin lighter, less thought of, and he's worth more on the poster and the board. */
    static void lostAndLeftStanding(GameTestHelper h) {
        long town = 510_003L;
        String key = "town:" + town;
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 6), "hf-duel-lost");
        Standing.add(player, town, 10);
        Purse.give(player, 5 * Purse.GOLD);
        BountyData bounty = bounty(4, true);
        ItemStack poster = poster(player, bounty);
        BountyBoards boards = BountyBoards.get(h.getLevel());
        boards.put(key, List.of(BoardPosting.open(bounty, false, 3).taken("hf-duel-lost")), BountyBoards.day(h.getLevel()));
        Duels.Duel duel = Duels.begin(h.getLevel(), player, bounty, town, ground(h));
        duel.landsNext = true;
        duel.lethal = false;
        h.assertValueEqual(duel.reaction(), Duels.reactionTicks(4), "his reaction");
        duel.runTo(duel.drawAt() + duel.reaction() - 1);
        h.assertTrue(duel.outcome() == Duels.Outcome.NONE, "he fired before his reaction was up");
        duel.runTo(duel.drawAt() + duel.reaction());
        h.assertTrue(duel.outcome() == Duels.Outcome.LOST, "he didn't fire on time: " + duel.outcome());
        h.assertTrue(player.isAlive() && player.getHealth() == Duels.LOSER_HEALTH, "the loser's health: " + player.getHealth());
        h.assertValueEqual(Purse.count(player), 5 * Purse.GOLD - 5 * Purse.GOLD * Duels.PURSE_PERCENT / 100, "the purse after");
        h.assertValueEqual(Standing.of(player).points(town), 10 - Duels.LOST_STANDING, "standing after a loss");
        BountyData after = poster.get(ModDataComponents.BOUNTY.get());
        h.assertTrue(after.reward() == Duels.raised(4) && !after.spawned(), "the poster after: " + after);
        h.assertValueEqual(boards.peek(key).orElseThrow().postings().getFirst().bounty().reward(), Duels.raised(4), "the board's price on him");
        DuelData data = DuelData.of(player);
        h.assertTrue(data.losses() == 1 && data.lost(town) == 1 && data.last() == DuelData.LOST, "the loss isn't on record: " + data);
        h.assertFalse(Duels.mayAsk(data, after, 5900, 99), "he asked again after winning");
        OutlawBoss boss = duel.boss();
        h.assertTrue(boss != null && boss.isAlive(), "he left before he'd taken the purse");
        duel.runTo(duel.endAt());
        h.assertTrue(duel.over() && !boss.isAlive() && !rooted(player) && !anyLent(player), "it didn't end, or he's still there, or the lent gun is");
        h.succeed();
    }

    /** With the switch on his shot kills, and the town's revolver doesn't fall with what the loser carried. */
    static void lethalBySwitch(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 6), "hf-duel-lethal");
        // A player just arrived can't be hurt for three seconds (the game's own rule), so the duel waits that out
        h.runAfterDelay(70, () -> lethal(h, player));
    }

    private static void lethal(GameTestHelper h, ServerPlayer player) {
        Duels.Duel duel = Duels.begin(h.getLevel(), player, bounty(4, true), null, ground(h));
        duel.landsNext = true;
        duel.lethal = true;
        h.assertTrue(anyLent(player), "nothing lent");
        duel.runTo(duel.drawAt() + duel.reaction());
        h.assertTrue(duel.outcome() == Duels.Outcome.LOST && player.isDeadOrDying(), "his shot didn't kill: " + player.getHealth());
        h.runAfterDelay(2, () -> {
            h.assertTrue(duel.over(), "a duel with a dead man in it went on");
            List<ItemEntity> dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(2, 1, 1))).inflate(6),
                    e -> Duels.lent(e.getItem()));
            h.assertTrue(dropped.isEmpty() && !anyLent(player), "the town's revolver fell with the loser");
            h.succeed();
        });
    }
}
