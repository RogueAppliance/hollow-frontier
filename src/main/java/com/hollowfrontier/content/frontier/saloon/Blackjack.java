package com.hollowfrontier.content.frontier.saloon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import net.minecraft.util.RandomSource;

/**
 * One hand of blackjack against the house (17): the rules and nothing else, so tests can stack the deck.
 * <p>
 * Cards are 0..51: rank {@code card % 13} (0 ace, 1-9 two to ten, 10-12 jack, queen, king), suit {@code card / 13}
 * (spades, hearts, diamonds, clubs). Each hand is dealt from a fresh deck. The dealer stands on 17 (on a soft 17 too,
 * unless {@link Rules#hitSoft17}), a natural pays 3:2 rounded down, and a player may double down on their first two
 * cards. A natural on either side ends the hand at once. No splitting and no insurance.
 */
public final class Blackjack {
    public enum Phase { PLAYING, SETTLED }

    public enum Outcome {
        NONE, BLACKJACK, WIN, DEALER_BUST, PUSH, LOSE, BUST;

        public boolean won() {
            return this == BLACKJACK || this == WIN || this == DEALER_BUST;
        }

        public boolean lost() {
            return this == LOSE || this == BUST;
        }
    }

    /** House rules: whether the dealer hits a soft 17 (Josie's table), and the most a hand may be bet. */
    public record Rules(boolean hitSoft17, int maxBet) {}

    private final Rules rules;
    private final Deque<Integer> deck;
    private final List<Integer> player = new ArrayList<>();
    private final List<Integer> dealer = new ArrayList<>();
    private int bet;
    private boolean doubled;
    private Phase phase = Phase.PLAYING;
    private Outcome outcome = Outcome.NONE;

    /** A hand bet at {@code bet}, dealt from {@code deck} (top card first): player, dealer, player, dealer. */
    public Blackjack(Rules rules, List<Integer> deck, int bet) {
        this.rules = rules;
        this.deck = new ArrayDeque<>(deck);
        this.bet = bet;
        player.add(draw());
        dealer.add(draw());
        player.add(draw());
        dealer.add(draw());
        if (natural(dealer)) {
            settle(natural(player) ? Outcome.PUSH : Outcome.LOSE);
        } else if (natural(player)) {
            settle(Outcome.BLACKJACK);
        }
    }

    /** A shuffled 52-card deck. */
    public static List<Integer> shuffled(RandomSource random) {
        List<Integer> deck = new ArrayList<>(52);
        for (int card = 0; card < 52; card++) {
            deck.add(card);
        }
        for (int i = deck.size() - 1; i > 0; i--) {
            Collections.swap(deck, i, random.nextInt(i + 1));
        }
        return deck;
    }

    // ---------------------------------------------------------------- cards

    public static int rank(int card) {
        return card % 13;
    }

    public static int suit(int card) {
        return card / 13;
    }

    /** An ace counts 1 here (see {@link #total}), picture cards 10. */
    public static int points(int card) {
        return Math.min(10, rank(card) + 1);
    }

    /** The best total: one ace counts 11 if that doesn't bust the hand. */
    public static int total(List<Integer> hand) {
        int sum = 0;
        boolean ace = false;
        for (int card : hand) {
            sum += points(card);
            ace |= rank(card) == 0;
        }
        return ace && sum + 10 <= 21 ? sum + 10 : sum;
    }

    /** Whether an ace is counting 11. */
    public static boolean soft(List<Integer> hand) {
        int sum = 0;
        boolean ace = false;
        for (int card : hand) {
            sum += points(card);
            ace |= rank(card) == 0;
        }
        return ace && sum + 10 <= 21;
    }

    public static boolean natural(List<Integer> hand) {
        return hand.size() == 2 && total(hand) == 21;
    }

    // ---------------------------------------------------------------- play

    public void hit() {
        if (phase != Phase.PLAYING) {
            return;
        }
        player.add(draw());
        if (total(player) > 21) {
            settle(Outcome.BUST);
        } else if (total(player) == 21) {
            stand();
        }
    }

    /** The player stands: the dealer draws to 17 (and on a soft 17 at a hit-soft-17 table), and the hands are compared. */
    public void stand() {
        if (phase != Phase.PLAYING) {
            return;
        }
        while (total(dealer) < 17 || rules.hitSoft17() && total(dealer) == 17 && soft(dealer)) {
            dealer.add(draw());
        }
        int mine = total(player), theirs = total(dealer);
        settle(theirs > 21 ? Outcome.DEALER_BUST : mine > theirs ? Outcome.WIN : mine == theirs ? Outcome.PUSH : Outcome.LOSE);
    }

    public boolean canDouble() {
        return phase == Phase.PLAYING && player.size() == 2 && !doubled;
    }

    /** Doubles the bet (the caller has already taken the second stake), takes exactly one card, and stands. */
    public void doubleDown() {
        if (!canDouble()) {
            return;
        }
        doubled = true;
        bet *= 2;
        player.add(draw());
        if (total(player) > 21) {
            settle(Outcome.BUST);
        } else {
            stand();
        }
    }

    /**
     * What comes back to the player once it's settled, stake included: a natural its stake plus 3:2 (rounded down), a win
     * twice the stake, a push the stake; nothing for a loss.
     */
    public int payout() {
        return switch (outcome) {
            case BLACKJACK -> bet + bet * 3 / 2;
            case WIN, DEALER_BUST -> 2 * bet;
            case PUSH -> bet;
            default -> 0;
        };
    }

    private int draw() {
        Integer card = deck.poll();
        return card != null ? card : 0; // a 52-card deck can't run out in one hand (11 cards at most each), but be safe
    }

    private void settle(Outcome result) {
        outcome = result;
        phase = Phase.SETTLED;
    }

    // ---------------------------------------------------------------- state

    public List<Integer> player() {
        return List.copyOf(player);
    }

    public List<Integer> dealer() {
        return List.copyOf(dealer);
    }

    public int bet() {
        return bet;
    }

    public boolean doubled() {
        return doubled;
    }

    public Phase phase() {
        return phase;
    }

    public Outcome outcome() {
        return outcome;
    }

    public Rules rules() {
        return rules;
    }
}
