package com.hollowfrontier.content.frontier.saloon;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModMenus;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Blackjack at a Card Table (17). The hand lives on the server only ({@link Blackjack}); the client gets what it may see
 * through data slots: the cards (the dealer's hole card face down until the hand is settled), the bet, the outcome, the
 * table's limit and the player's purse. The screen's buttons come back through vanilla's {@code clickMenuButton}.
 * <p>
 * The stake is taken from the player's coins when the cards are dealt, and what a hand pays comes straight back to their
 * inventory. Closing the table mid-hand stands for them and settles it, so walking off can't dodge a loss.
 */
public class BlackjackMenu extends AbstractContainerMenu {
    public static final int MAX_CARDS = 11; // the most cards a hand can hold without busting (four aces, four twos, three threes)
    // Data slots: the player's cards, the dealer's, then the rest. A card is sent as 1 + its id, 0 for none, HIDDEN face down.
    public static final int PLAYER_CARDS = 0, DEALER_CARDS = MAX_CARDS, BET = 2 * MAX_CARDS, PHASE = BET + 1, OUTCOME = BET + 2,
            MAX_BET = BET + 3, PURSE = BET + 4, HIT_SOFT_17 = BET + 5, PAYOUT = BET + 6, DATA = BET + 7;
    public static final int HIDDEN = 53;
    /** PHASE values: no hand yet, a hand in play, a hand settled (shown until the next deal). */
    public static final int NO_HAND = 0, PLAYING = 1, SETTLED = 2;
    public static final int DEAL = 0, HIT = 1, STAND = 2, DOUBLE = 3, BET_LESS = 4, BET_MORE = 5, BET_LESS_5 = 6, BET_MORE_5 = 7;
    /** Hands lost in a row for Cleaned Out. */
    public static final int CLEANED_OUT = 5;
    public static final int PURSE_SHOWN = 30000; // data slots go over the wire as shorts
    /** The bet a sitting starts at (a careful one: the limit would stake a whole purse on the first click). */
    public static final int OPENING_BET = 5;

    private final SimpleContainerData data = new SimpleContainerData(DATA);
    private final ContainerLevelAccess access;
    private final Blackjack.@Nullable Rules rules;
    private final @Nullable Supplier<List<Integer>> shoe;
    private final @Nullable Player player;
    private @Nullable Blackjack hand;
    private int lossStreak;

    /** Client side: everything comes from the data slots. */
    public BlackjackMenu(int containerId, Inventory inventory) {
        super(ModMenus.BLACKJACK.get(), containerId);
        this.access = ContainerLevelAccess.NULL;
        this.rules = null;
        this.shoe = null;
        this.player = null;
        addDataSlots(data);
    }

    /** Server side: a table at {@code access} with these house rules, dealing each hand from a deck {@code shoe} gives. */
    public BlackjackMenu(int containerId, Inventory inventory, ContainerLevelAccess access, Blackjack.Rules rules, Supplier<List<Integer>> shoe) {
        super(ModMenus.BLACKJACK.get(), containerId);
        this.access = access;
        this.rules = rules;
        this.shoe = shoe;
        this.player = inventory.player;
        addDataSlots(data);
        data.set(MAX_BET, rules.maxBet());
        data.set(HIT_SOFT_17, rules.hitSoft17() ? 1 : 0);
        data.set(BET, Math.min(rules.maxBet(), OPENING_BET));
        data.set(PURSE, Math.min(PURSE_SHOWN, Purse.count(inventory.player)));
    }

    // ---------------------------------------------------------------- what the screen reads

    public int card(int slot) {
        return data.get(slot);
    }

    public int bet() {
        return data.get(BET);
    }

    public int phase() {
        return data.get(PHASE);
    }

    public Blackjack.Outcome outcome() {
        return Blackjack.Outcome.values()[Mth.clamp(data.get(OUTCOME), 0, Blackjack.Outcome.values().length - 1)];
    }

    public int maxBet() {
        return data.get(MAX_BET);
    }

    public int purse() {
        return data.get(PURSE);
    }

    public boolean hitSoft17() {
        return data.get(HIT_SOFT_17) != 0;
    }

    public int payout() {
        return data.get(PAYOUT);
    }

    /** Whether a button would do anything now (the screen greys out the rest). */
    public boolean allowed(int button) {
        boolean between = phase() != PLAYING;
        return switch (button) {
            case DEAL -> between && purse() >= bet() && bet() > 0;
            case HIT, STAND -> !between;
            case DOUBLE -> !between && countCards(PLAYER_CARDS) == 2 && purse() >= bet();
            case BET_LESS, BET_LESS_5 -> between && bet() > 1;
            case BET_MORE, BET_MORE_5 -> between && bet() < maxBet();
            default -> false;
        };
    }

    private int countCards(int from) {
        int n = 0;
        while (n < MAX_CARDS && data.get(from + n) != 0) {
            n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- play (server)

    /** The hand in play or last settled, for tests. */
    public @Nullable Blackjack hand() {
        return hand;
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (rules == null || shoe == null || clicker != player) {
            return false;
        }
        boolean between = hand == null || hand.phase() == Blackjack.Phase.SETTLED;
        switch (id) {
            case BET_LESS, BET_MORE, BET_LESS_5, BET_MORE_5 -> {
                if (!between) {
                    return false;
                }
                int step = id == BET_LESS ? -1 : id == BET_MORE ? 1 : id == BET_LESS_5 ? -5 : 5;
                data.set(BET, Mth.clamp(data.get(BET) + step, 1, rules.maxBet()));
            }
            case DEAL -> {
                int bet = Mth.clamp(data.get(BET), 1, rules.maxBet());
                if (!between || !Purse.pay(clicker, bet)) {
                    return false;
                }
                hand = new Blackjack(rules, shoe.get(), bet);
                sound(SoundEvents.BOOK_PUT, 1.4F);
                sound(SoundEvents.BOOK_PAGE_TURN, 1.6F);
                afterMove(clicker);
            }
            case HIT -> {
                if (between) {
                    return false;
                }
                hand.hit();
                sound(SoundEvents.BOOK_PAGE_TURN, 1.6F);
                afterMove(clicker);
            }
            case STAND -> {
                if (between) {
                    return false;
                }
                hand.stand();
                afterMove(clicker);
            }
            case DOUBLE -> {
                if (between || !hand.canDouble() || !Purse.pay(clicker, hand.bet())) {
                    return false;
                }
                hand.doubleDown();
                sound(SoundEvents.BOOK_PAGE_TURN, 1.6F);
                afterMove(clicker);
            }
            default -> {
                return false;
            }
        }
        sync(clicker);
        return true;
    }

    // Once the hand is over: pay out, and keep count for the advancements
    private void afterMove(Player clicker) {
        if (hand == null || hand.phase() != Blackjack.Phase.SETTLED) {
            return;
        }
        int paid = hand.payout();
        Purse.give(clicker, paid);
        if (paid > 0) {
            sound(ModSounds.COIN_CLINK.get(), 1.0F);
        }
        Blackjack.Outcome outcome = hand.outcome();
        lossStreak = outcome.lost() ? lossStreak + 1 : outcome.won() ? 0 : lossStreak;
        if (clicker instanceof ServerPlayer server) {
            if (outcome == Blackjack.Outcome.BLACKJACK) {
                ModCriteria.fire(server, ModCriteria.BLACKJACK_NATURAL);
            }
            if (outcome.won() && hand.player().size() >= 5) {
                ModCriteria.fire(server, ModCriteria.FIVE_CARD_CHARLIE);
            }
            if (lossStreak >= CLEANED_OUT) {
                ModCriteria.fire(server, ModCriteria.CLEANED_OUT);
            }
        }
    }

    // Writes the hand into the data slots: the dealer's hole card stays face down until the hand is over
    private void sync(Player clicker) {
        data.set(PURSE, Math.min(PURSE_SHOWN, Purse.count(clicker)));
        if (hand == null) {
            data.set(PHASE, NO_HAND);
            return;
        }
        boolean settled = hand.phase() == Blackjack.Phase.SETTLED;
        List<Integer> mine = hand.player(), theirs = hand.dealer();
        for (int i = 0; i < MAX_CARDS; i++) {
            data.set(PLAYER_CARDS + i, i < mine.size() ? mine.get(i) + 1 : 0);
            data.set(DEALER_CARDS + i, i < theirs.size() ? (i == 1 && !settled ? HIDDEN : theirs.get(i) + 1) : 0);
        }
        data.set(PHASE, settled ? SETTLED : PLAYING);
        data.set(OUTCOME, hand.outcome().ordinal());
        data.set(PAYOUT, settled ? hand.payout() : 0);
    }

    private void sound(SoundEvent sound, float pitch) {
        access.execute((level, pos) -> level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8F, pitch));
    }

    @Override
    public void broadcastChanges() {
        if (player != null) {
            data.set(PURSE, Math.min(PURSE_SHOWN, Purse.count(player)));
        }
        super.broadcastChanges();
    }

    // Walking off mid-hand stands and settles it
    @Override
    public void removed(Player leaving) {
        super.removed(leaving);
        if (hand != null && hand.phase() == Blackjack.Phase.PLAYING && leaving == player) {
            hand.stand();
            afterMove(leaving);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player viewer) {
        return stillValid(access, viewer, ModBlocks.CARD_TABLE.get());
    }
}
