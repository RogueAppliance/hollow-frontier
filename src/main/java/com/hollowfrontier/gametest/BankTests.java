package com.hollowfrontier.gametest;

import java.util.UUID;

import com.hollowfrontier.content.frontier.bank.BankBox;
import com.hollowfrontier.content.frontier.bank.BankNotice;
import com.hollowfrontier.content.frontier.bank.Banking;
import com.hollowfrontier.content.frontier.bank.TownBanks;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

/**
 * The bank (16): opening a box, keeping it, interest. Each test banks under a town key of its own (its own position),
 * so tests running side by side never share a box. Raids on the bank are in RaidTests, which has the town.
 */
final class BankTests {
    private BankTests() {}

    private static long town(GameTestHelper h) {
        return h.absolutePos(BlockPos.ZERO).asLong();
    }

    private static int purse(ServerPlayer player) {
        return player.getInventory().countItem(ModItems.COPPER_COIN.get()) + 9 * player.getInventory().countItem(ModItems.GOLD_COIN.get());
    }

    /** The first box costs a gold coin (no coin, no box); after that it's free, and the window is the box itself. */
    static void boxOpensForAGoldCoin(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(1, 1, 1));
        long town = town(h);
        h.assertFalse(Banking.open(level, player, null, town), "opened a box without paying");
        h.assertTrue(TownBanks.get(level).box(town, player.getUUID()) == null, "a refused box exists anyway");
        player.getInventory().add(new ItemStack(ModItems.GOLD_COIN.get(), 1));
        h.assertTrue(Banking.open(level, player, null, town), "paid and still refused");
        BankBox box = TownBanks.get(level).box(town, player.getUUID());
        h.assertTrue(box != null && purse(player) == 0, "box " + box + ", purse left " + purse(player));
        h.assertTrue(player.containerMenu instanceof ChestMenu menu && menu.getContainer() == box, "the window isn't the box: " + player.containerMenu);
        h.assertTrue(AdvancementTests.earned(player, "frontier/money_in_the_bank"), "opening a box earned nothing");
        player.closeContainer();
        h.assertTrue(Banking.open(level, player, null, town), "charged again for the same box");
        h.assertTrue(player.containerMenu instanceof ChestMenu menu && menu.getContainer() == box, "second visit got a different box");
        player.closeContainer();
        TestPlayers.leave(player);
        h.succeed();
    }

    /** Boxes are private: one player's box isn't another's, and a box in one town isn't the same owner's box in another. */
    static void boxesArePrivate(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TownBanks bank = TownBanks.get(level);
        long town = town(h);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        BankBox boxA = bank.open(town, a, 0);
        boxA.setItem(0, new ItemStack(ModItems.GOLD_COIN.get(), 5));
        BankBox boxB = bank.open(town, b, 0);
        h.assertTrue(boxB != boxA && boxB.isEmpty(), "b sees a's coins");
        h.assertTrue(bank.box(town + 1, a) == null, "a's box follows them to another town");
        h.assertTrue(bank.box(town, a) == boxA, "a got a copy, not the box");
        h.succeed();
    }

    /** What's in a box (and when it was last visited) goes through the save codec and back. */
    static void boxSurvivesSave(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TownBanks bank = TownBanks.get(level);
        long town = town(h);
        UUID owner = UUID.randomUUID();
        BankBox box = bank.open(town, owner, 42);
        box.setItem(3, new ItemStack(ModItems.GOLD_COIN.get(), 7));
        box.setItem(17, new ItemStack(ModItems.REVOLVER.get()));
        bank.post(owner, new BankNotice("McGraw", 10, -20, 13, false, "Dry Gulch"));
        RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        TownBanks loaded = TownBanks.CODEC.parse(ops, TownBanks.CODEC.encodeStart(ops, bank).getOrThrow()).getOrThrow();
        BankBox back = loaded.box(town, owner);
        h.assertTrue(back != null && back.lastDay() == 42, "box or its last visit lost: " + back);
        h.assertTrue(back.getItem(3).is(ModItems.GOLD_COIN.get()) && back.getItem(3).getCount() == 7 && back.getItem(17).is(ModItems.REVOLVER.get()),
                "contents changed in the save");
        h.assertTrue(loaded.collect(owner).equals(java.util.List.of(new BankNotice("McGraw", 10, -20, 13, false, "Dry Gulch"))), "unheard news lost in the save");
        bank.collect(owner);
        h.succeed();
    }

    /** About 3% a day: 1 copper per full 32, at most 8 a day, compounding, for at most 7 days. */
    static void interestRules(GameTestHelper h) {
        h.assertTrue(Banking.interest(31, 5) == 0, "interest on less than 32 c");
        h.assertTrue(Banking.interest(64, 1) == 2, "64 c for a day: " + Banking.interest(64, 1));
        h.assertTrue(Banking.interest(320, 1) == 8, "daily cap: " + Banking.interest(320, 1));
        h.assertTrue(Banking.interest(64, 7) == 14, "a week on 64 c: " + Banking.interest(64, 7));
        // 63 c earns 1 the first day; 64 c earns 2 the next.
        h.assertTrue(Banking.interest(63, 2) == 3, "compounding: " + Banking.interest(63, 2));
        h.assertTrue(Banking.interest(1000, 30) == Banking.interest(1000, 7) && Banking.interest(1000, 7) == 56, "more than 7 days counted");
        h.assertTrue(Banking.interest(1000, 0) == 0, "interest on the same day");
        h.succeed();
    }

    /** Coming back days later pays the interest into the box, once, and resets the clock. */
    static void interestPaysOnVisit(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(1, 1, 1));
        long town = town(h);
        long today = BountyBoards.day(level);
        BankBox box = TownBanks.get(level).open(town, player.getUUID(), today - 3);
        box.setItem(0, new ItemStack(ModItems.GOLD_COIN.get(), 8)); // 72 c
        int expected = Banking.interest(72, 3);
        h.assertTrue(Banking.open(level, player, null, town), "couldn't open the box");
        player.closeContainer();
        h.assertTrue(expected > 0 && box.worth() == 72 + expected, "box worth " + box.worth() + ", expected " + (72 + expected));
        h.assertTrue(box.lastDay() == today, "visit not recorded");
        h.assertTrue(AdvancementTests.earned(player, "frontier/compound_interest"), "drawing interest earned nothing");
        Banking.open(level, player, null, town);
        player.closeContainer();
        h.assertTrue(box.worth() == 72 + expected, "interest paid twice in a day: " + box.worth());
        TestPlayers.leave(player);
        h.succeed();
    }

    /** News of a raid waits for an owner who was away, and is told once. */
    static void newsWaitsForTheOwner(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        UUID away = UUID.randomUUID();
        Banking.notify(level.getServer(), java.util.Map.of(away, 11), "McGraw", "Dry Gulch", 1, 2, false);
        TownBanks bank = TownBanks.get(level);
        java.util.List<BankNotice> news = bank.collect(away);
        h.assertTrue(news.size() == 1 && news.getFirst().copper() == 11 && !news.getFirst().returned(), "news: " + news);
        h.assertTrue(bank.collect(away).isEmpty(), "news told twice");
        h.succeed();
    }

    /** A Banker opens boxes only for a sneaking player, in a town, and not for anyone with two stars. */
    static void bankerServesOnlyInTown(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(1, 1, 1));
        player.getInventory().add(new ItemStack(ModItems.GOLD_COIN.get(), 1));
        Townsfolk banker = ModEntities.TOWNSFOLK.get().spawn(level, h.absolutePos(new BlockPos(2, 1, 1)), EntitySpawnReason.SPAWN_ITEM_USE);
        banker.setFolk(Folk.BANKER);
        banker.setNoAi(true);
        player.setShiftKeyDown(true);
        banker.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertFalse(player.containerMenu instanceof ChestMenu, "a box opened out in the wilds");
        h.assertTrue(purse(player) == 9, "charged for no box");
        Wanted.addHeat(player, 6);
        banker.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertFalse(player.containerMenu instanceof ChestMenu, "served a wanted man");
        player.closeContainer();
        banker.discard();
        TestPlayers.leave(player);
        h.succeed();
    }
}
