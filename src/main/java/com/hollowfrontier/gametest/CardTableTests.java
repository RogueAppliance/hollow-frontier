package com.hollowfrontier.gametest;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.content.frontier.saloon.Blackjack;
import com.hollowfrontier.content.frontier.saloon.BlackjackMenu;
import com.hollowfrontier.content.frontier.saloon.CardTableBlock;
import com.hollowfrontier.content.frontier.saloon.Hostess;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// The card table (17): blackjack's rules on stacked decks, the coins a hand moves, and the barmaid who deals.
final class CardTableTests {
    private CardTableTests() {}

    // Cards by rank in spades (id = rank): an ace, the pips by value, and a king
    private static final int ACE = 0, TWO = 1, FIVE = 4, SIX = 5, SEVEN = 6, NINE = 8, TEN = 9, KING = 12;
    private static final Blackjack.Rules HOUSE = new Blackjack.Rules(false, 18), SHARP = new Blackjack.Rules(true, 36);

    /** A deck dealt in this order (player, dealer, player, dealer, then draws), the rest of the 52 after. */
    private static List<Integer> stacked(int... top) {
        List<Integer> deck = new ArrayList<>();
        for (int card : top) {
            deck.add(card);
        }
        for (int card = 0; card < 52; card++) {
            if (!deck.contains(card)) {
                deck.add(card);
            }
        }
        return deck;
    }

    private static List<Integer> hand(int... cards) {
        List<Integer> out = new ArrayList<>();
        for (int card : cards) {
            out.add(card);
        }
        return out;
    }

    /** Aces count 11 when they can and 1 when they must; picture cards are 10. */
    static void handValues(GameTestHelper h) {
        h.assertTrue(Blackjack.total(hand(ACE, SIX)) == 17 && Blackjack.soft(hand(ACE, SIX)), "A6 should be soft 17");
        h.assertTrue(Blackjack.total(hand(ACE, SIX, TEN)) == 17 && !Blackjack.soft(hand(ACE, SIX, TEN)), "A6T should be hard 17");
        h.assertTrue(Blackjack.total(hand(ACE, ACE, NINE)) == 21, "AA9 should be 21");
        h.assertTrue(Blackjack.total(hand(KING, 13 + KING)) == 20, "two kings should be 20");
        h.assertTrue(Blackjack.natural(hand(ACE, KING)) && !Blackjack.natural(hand(SEVEN, SEVEN + 13, SEVEN + 26)), "naturals");
        h.succeed();
    }

    /** The house stands on a soft 17; Josie's table hits it. */
    static void dealerSoft17(GameTestHelper h) {
        // Player ten and nine (19); dealer ace and six (soft 17); then a two
        Blackjack house = new Blackjack(HOUSE, stacked(TEN, ACE, NINE, SIX, TWO), 2);
        house.stand();
        h.assertTrue(house.dealer().size() == 2 && house.outcome() == Blackjack.Outcome.WIN, "house: " + house.dealer() + " " + house.outcome());
        Blackjack sharp = new Blackjack(SHARP, stacked(TEN, ACE, NINE, SIX, TWO), 2);
        sharp.stand();
        h.assertTrue(sharp.dealer().size() == 3 && sharp.outcome() == Blackjack.Outcome.PUSH, "sharp: " + sharp.dealer() + " " + sharp.outcome());
        h.succeed();
    }

    /** A natural pays 3:2 rounded down, a win double, a push the stake; a bust nothing; a double down doubles it all. */
    static void payouts(GameTestHelper h) {
        Blackjack natural = new Blackjack(HOUSE, stacked(ACE, TEN, KING, SIX), 5);
        h.assertTrue(natural.outcome() == Blackjack.Outcome.BLACKJACK && natural.payout() == 12, "natural: " + natural.outcome() + " " + natural.payout());
        Blackjack bust = new Blackjack(HOUSE, stacked(TEN, TEN + 13, KING, SEVEN, TEN + 26), 4);
        bust.hit();
        h.assertTrue(bust.outcome() == Blackjack.Outcome.BUST && bust.payout() == 0, "bust: " + bust.outcome());
        // Player five and six (11), dealer ten and seven (17); doubling draws a ten for 21
        Blackjack doubled = new Blackjack(HOUSE, stacked(FIVE, TEN, SIX, SEVEN, TEN + 13), 4);
        doubled.doubleDown();
        h.assertTrue(doubled.outcome() == Blackjack.Outcome.WIN && doubled.bet() == 8 && doubled.payout() == 16,
                "double: " + doubled.outcome() + " bet " + doubled.bet() + " paid " + doubled.payout());
        Blackjack push = new Blackjack(HOUSE, stacked(TEN, TEN + 13, SEVEN, SEVEN + 13), 3);
        push.stand();
        h.assertTrue(push.outcome() == Blackjack.Outcome.PUSH && push.payout() == 3, "push: " + push.outcome());
        Blackjack dealerNatural = new Blackjack(HOUSE, stacked(TEN, ACE, NINE, KING), 3);
        h.assertTrue(dealerNatural.outcome() == Blackjack.Outcome.LOSE, "the dealer's natural should end the hand: " + dealerNatural.outcome());
        h.succeed();
    }

    private static BlackjackMenu menu(GameTestHelper h, ServerPlayer player, List<Integer> deck) {
        BlockPos table = h.absolutePos(new BlockPos(6, 1, 6));
        h.getLevel().setBlockAndUpdate(table, ModBlocks.CARD_TABLE.get().defaultBlockState());
        return new BlackjackMenu(0, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), table), HOUSE, () -> deck);
    }

    /** The stake comes out of the purse on the deal and the winnings go back in, gold and all. */
    static void menuMovesCoins(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 4));
        player.getInventory().add(new ItemStack(ModItems.GOLD_COIN.get(), 2));
        player.getInventory().add(new ItemStack(ModItems.COPPER_COIN.get(), 2));
        BlackjackMenu menu = menu(h, player, stacked(ACE, TEN, KING, SIX));
        h.assertTrue(menu.bet() == BlackjackMenu.OPENING_BET, "the bet should start at 5: " + menu.bet());
        menu.clickMenuButton(player, BlackjackMenu.BET_LESS);
        menu.clickMenuButton(player, BlackjackMenu.BET_LESS);
        h.assertTrue(menu.bet() == 3, "bet after -2: " + menu.bet());
        h.assertTrue(menu.clickMenuButton(player, BlackjackMenu.DEAL), "deal refused");
        // 20 - 3 + (3 + 4 for a natural)
        h.assertTrue(Purse.count(player) == 24, "purse after a natural: " + Purse.count(player));
        h.assertFalse(menu.clickMenuButton(player, BlackjackMenu.HIT), "a settled hand can't be hit");
        TestPlayers.leave(player);
        h.succeed();
    }

    /** Closing the table mid-hand stands and settles it: a loss stays lost, a win is paid. */
    static void leavingSettles(GameTestHelper h) {
        ServerPlayer loser = TestPlayers.survival(h, new BlockPos(6, 1, 4));
        loser.getInventory().add(new ItemStack(ModItems.COPPER_COIN.get(), 10));
        BlackjackMenu losing = menu(h, loser, stacked(TEN, TEN + 13, SEVEN, NINE)); // 17 against 19
        h.assertTrue(losing.bet() == 5 && losing.clickMenuButton(loser, BlackjackMenu.DEAL), "deal refused");
        losing.removed(loser);
        h.assertTrue(losing.hand().outcome() == Blackjack.Outcome.LOSE && Purse.count(loser) == 5, "left a losing hand: " + Purse.count(loser));

        ServerPlayer winner = TestPlayers.survival(h, new BlockPos(4, 1, 4));
        winner.getInventory().add(new ItemStack(ModItems.COPPER_COIN.get(), 10));
        BlackjackMenu winning = menu(h, winner, stacked(TEN, TEN + 13, NINE, SEVEN)); // 19 against 17
        winning.clickMenuButton(winner, BlackjackMenu.DEAL);
        winning.removed(winner);
        h.assertTrue(winning.hand().outcome() == Blackjack.Outcome.WIN && Purse.count(winner) == 15, "left a winning hand: " + Purse.count(winner));
        TestPlayers.leave(loser, winner);
        h.succeed();
    }

    /** A table deals only with a barmaid near, and Josie's runs the hard rules. */
    static void needsADealer(GameTestHelper h) {
        BlockPos table = new BlockPos(6, 1, 6);
        h.setBlock(table, ModBlocks.CARD_TABLE.get());
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 4));
        BlockPos abs = h.absolutePos(table);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(Direction.UP, 0.5), Direction.UP, abs, false);
        h.getLevel().getBlockState(abs).useWithoutItem(h.getLevel(), player, hit);
        h.assertFalse(player.containerMenu instanceof BlackjackMenu, "a table dealt with nobody there");
        Barmaid josie = h.spawnWithNoFreeWill(ModEntities.BARMAID.get(), new BlockPos(3, 1, 3));
        josie.setHostess(Hostess.JOSIE);
        h.getLevel().getBlockState(abs).useWithoutItem(h.getLevel(), player, hit);
        h.assertTrue(player.containerMenu instanceof BlackjackMenu, "no game with a barmaid by the table");
        BlackjackMenu menu = (BlackjackMenu) player.containerMenu;
        h.assertTrue(menu.hitSoft17() && menu.maxBet() == CardTableBlock.SHARP_MAX_BET, "Josie's table should hit soft 17 with a 36 limit");
        player.closeContainer();
        TestPlayers.leave(player);
        h.succeed();
    }
}
