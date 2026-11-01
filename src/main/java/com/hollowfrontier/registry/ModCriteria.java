package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.advancement.ModTrigger;

import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Advancement criterion triggers for mod events (each a {@link ModTrigger}, fired from the code path where the event
 * happens). Vanilla triggers cover the rest (items, crafting, trades, kills). Later phases add theirs here.
 */
public final class ModCriteria {
    private ModCriteria() {}

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, HollowFrontier.MODID);

    // Frontier
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> GUN_LOADED = register("gun_loaded");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> HORSE_SHOD = register("horse_shod");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> LASSOED = register("lassoed");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> BOUNTY_CLAIMED = register("bounty_claimed");
    /** Value: the stars reached. */
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> WANTED_LEVEL = register("wanted_level");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> WANTED_CLEARED = register("wanted_cleared");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> BLACKED_OUT = register("blacked_out");
    // The card table (17): a natural blackjack; a hand won with five cards or more; five hands lost in a row at one sitting
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> BLACKJACK_NATURAL = register("blackjack_natural");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> FIVE_CARD_CHARLIE = register("five_card_charlie");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> CLEANED_OUT = register("cleaned_out");
    // The bank (16): opened a box; drew interest; shot the bagman carrying coins from your own box
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> BANK_OPENED = register("bank_opened");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> BANK_INTEREST = register("bank_interest");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> BANK_SAVED = register("bank_saved");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> GHOST_CAT_TAMED = register("ghost_cat_tamed");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> CAT_DRESSED = register("cat_dressed");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> RAID_DEFENDED = register("raid_defended");
    /** The bagman shot before he got the town's money out. */
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> LOOT_RECOVERED = register("loot_recovered");
    /** A 3-star outlaw paid a cut of a robbery. */
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> RAID_CUT = register("raid_cut");
    // Steam
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> CANISTER_FILLED = register("canister_filled");
    /** Value: blocks driven under steam on one ride. */
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> HANDCAR_DRIVEN = register("handcar_driven");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> HANDCAR_WHISTLE = register("handcar_whistle");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> HANDCAR_REFUELLED = register("handcar_refuelled");
    // Airship and the polar expedition
    /** Value: the height (block y) an airship reached with the player at the helm. */
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> AIRSHIP_ALTITUDE = register("airship_altitude");
    /** A Polar Compass found a gateway. */
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> POLAR_BEARING = register("polar_bearing");
    // The Gray experiments (10b): a tagged beast brought down; a station picked up on the Signal Locator
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SPECIMEN_KILLED = register("specimen_killed");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SIGNAL_FOUND = register("signal_found");
    // The frontier's mysteries (11a): seeing a sighting; bringing down a probe at work
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SIGHTING_SEEN = register("sighting_seen");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> CAUGHT_IN_THE_ACT = register("caught_in_the_act");
    // The men in black (11b): warned off; sold them something; marked
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> WARNED_OFF = register("warned_off");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SOLD_TO_THEM = register("sold_to_them");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> MARKED = register("marked");
    // The Kindled (12a): met one; became their ally; brought down a guardian
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> MET_KINDLED = register("met_kindled");
    /** Value: how many lines of their story the player has heard (Kindled.LINES in all). */
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> KINDLED_STORY = register("kindled_story");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> KINDLED_ALLY = register("kindled_ally");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> GUARDIAN_KILLED = register("guardian_killed");
    // The Sun Archon (12b): the Lens's beam struck something; the Codex opened
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SUNBEAM_HIT = register("sunbeam_hit");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> CODEX_READ = register("codex_read");
    // The Grays arrive (13a): saw a saucer; was taken by one; slipped its beam
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SAUCER_SEEN = register("saucer_seen");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> ABDUCTED = register("abducted");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> BEAM_ESCAPED = register("beam_escaped");
    // The Prototype Saucer (13c): launched one; flew it (value: altitude); lifted something with its beam
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SAUCER_LAUNCHED = register("saucer_launched");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SAUCER_FLOWN = register("saucer_flown");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SAUCER_BEAMED = register("saucer_beamed");
    // The Mothership (13d): called one down with a Phlogiston Beacon; was there when the leash broke
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> MOTHERSHIP_SUMMONED = register("mothership_summoned");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> LEASH_BROKEN = register("leash_broken");
    // Gray Landings (15a): was there when every guard of one went down before dawn
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> LANDING_CLEARED = register("landing_cleared");
    // The Mothership's return (15b): was there when a rematch fell; flew a saucer with every fitting
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> MOTHERSHIP_REMATCH = register("mothership_rematch");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SAUCER_FULLY_FITTED = register("saucer_fully_fitted");
    // What the frontier makes of itself (15c): gave a freed man a new life; restored a ruin (value: the stage); travelled by waystone
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> SECOND_CHANCE = register("second_chance");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> RUIN_RESTORED = register("ruin_restored");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> WAYSTONE_TRAVELLED = register("waystone_travelled");
    // 20: the captives
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> CAPTIVE_FREED = register("captive_freed");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> PROCEDURE_STOPPED = register("procedure_stopped");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> LETTER_DELIVERED = register("letter_delivered");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> BROUGHT_IN_ALIVE = register("brought_in_alive");
    // 21b: came to somewhere else in town, with somebody else's hat
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> MISSING_TIME_DRUNK = register("missing_time_drunk");
    // 21c: Grays in disguise, found out or kept
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> GRAY_EXPOSED = register("gray_exposed");
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> REGULAR_CUSTOMER = register("regular_customer");
    // 29: a dust devil took the player's hat
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> HAT_BLOWN = register("hat_blown");
    /** A Polar Resonator opened a gateway. */
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> GATEWAY_OPENED = register("gateway_opened");

    /** Fires a trigger for a player (a no-op for anything else, e.g. a mob or a client-side entity). */
    public static void fire(Entity entity, DeferredHolder<CriterionTrigger<?>, ModTrigger> trigger) {
        fire(entity, trigger, 1);
    }

    public static void fire(Entity entity, DeferredHolder<CriterionTrigger<?>, ModTrigger> trigger, int value) {
        if (entity instanceof ServerPlayer player) {
            trigger.get().trigger(player, value);
        }
    }

    private static DeferredHolder<CriterionTrigger<?>, ModTrigger> register(String name) {
        return TRIGGERS.register(name, ModTrigger::new);
    }
}
