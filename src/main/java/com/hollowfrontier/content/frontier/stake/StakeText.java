package com.hollowfrontier.content.frontier.stake;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Everything phase 47 says, with the English (the lang provider reads {@link #all}, so a line and its text can't drift
 * apart, as with ChatterLines). Keys are {@code message.hollowfrontier.stake.<name>}.
 */
public final class StakeText {
    private StakeText() {}

    private static final Map<String, String> TEXT = new LinkedHashMap<>();

    // Standing
    public static final String RANK_STRANGER = raw("standing.hollowfrontier.stranger", "Stranger");
    public static final String RANK_REGULAR = raw("standing.hollowfrontier.regular", "Regular");
    public static final String RANK_NEIGHBOUR = raw("standing.hollowfrontier.neighbour", "Neighbour");
    public static final String RANK_PILLAR = raw("standing.hollowfrontier.pillar", "Pillar of the Town");
    public static final String UP = raw("message.hollowfrontier.standing.up", "%s knows your name now: %s.");
    public static final String DOWN = raw("message.hollowfrontier.standing.down", "%s thinks less of you: %s.");
    public static final String WAIVED = t("waived", "You're known here. I'll let this one go. Once a year, mind.");

    // The badge
    public static final String SWORN_IN = t("sworn_in", "Raise your right hand. Good enough. You're a deputy of %s.");
    public static final String OTHER_STAR = t("other_star", "You're wearing %s's star. Hand it over to me if you mean to wear ours (sneak and ask again).");
    public static final String NEW_STAR = t("new_star", "Lost your star? Here. They come out of my pay, deputy.");
    public static final String SUSPENDED = t("suspended", "Your star stays in my drawer until your name's clear.");
    public static final String STAR_TAKEN = t("star_taken", "The sheriff of %s wants that star back.");
    public static final String REINSTATED = t("reinstated", "Name's clear. Here's your star. Don't make me do that twice.");
    public static final String BADGE_TOWN = t("badge_town", "Deputy of %s");
    public static final String BOUNTY_BONUS = t("bounty_bonus", "A gold coin on top, for one of our own.");

    // Jobs
    public static final String JOB_ROUNDS = t("job.rounds", "Walk the rounds tonight: %s. Then come and tell me it's quiet.");
    public static final String JOB_BRING_IN = t("job.bring_in", "A drifter's been cutting fences %s paces %s of here. Wing him, rope him and walk him in. Alive.");
    public static final String JOB_WARRANT = t("job.warrant", "Take this warrant to the sheriff of %s, %s paces %s. He'll pay you for the ride.");
    public static final String JOB_REMIND = t("job.remind", "You've a job in hand, deputy.");
    public static final String JOB_NONE = t("job.none", "Nothing for you just now. Come by after dark, or tomorrow.");
    public static final String ROUNDS_NEXT = t("rounds.next", "Rounds: %s of %s. Next, %s: %s paces %s.");
    public static final String ROUNDS_REPORT = t("rounds.report", "Rounds walked. Report to the sheriff.");
    public static final String ROUNDS_SCOUT = t("rounds.scout", "Somebody's watching the bank from the dark.");
    public static final String ROUNDS_DONE = t("rounds.done", "Quiet, was it? That's how I like it. Here's your pay.");
    public static final String POINT_SQUARE = t("point.square", "the square");
    public static final String POINT_BANK = t("point.bank", "the strongroom");
    public static final String POINT_BELL = t("point.bell", "the bell");
    public static final String POINT_EDGE = t("point.edge", "the end of the street");
    public static final String POINT_TRAIL = t("point.trail", "the trail out of town");
    public static final String QUARRY_NEAR = t("quarry.near", "That'll be %s, up ahead.");
    public static final String QUARRY_FIGHT = t("quarry.fight", "He's got too much fight in him yet. Wing him first.");
    public static final String QUARRY_ROPED = t("quarry.roped", "%s throws his gun down. Walk him back to the sheriff.");
    public static final String QUARRY_DEAD = t("quarry.dead", "The sheriff wanted that one alive. No pay for a dead man.");
    public static final String BROUGHT_IN = t("brought_in", "Alive and all. That's the job done right.");
    public static final String WARRANT_OWN = t("warrant.own", "That's my own handwriting, deputy. It goes to %s.");
    public static final String WARRANT_SERVED = t("warrant.served", "From %s? Long ride. Here's %s copper for it.");
    public static final String WARRANT_TO = t("warrant.to", "For the sheriff of %s");
    public static final String PAID = t("paid", "Paid %s copper.");

    // The Old Place
    public static final String DEED_FOR = t("deed.for", "A place of your own at %s");
    public static final String DEED_HINT = t("deed.hint", "Use it on the stake at the Old Place, or on open ground near town to have one raised.");
    public static final String DEED_STANDING = t("deed.standing", "The bank sells land to neighbours. You're not one yet.");
    public static final String DEED_WRONG_TOWN = t("deed.wrong_town", "This deed is for land at %s.");
    public static final String DEED_OWNED = t("deed.owned", "This is the %s place already.");
    public static final String DEED_HAVE_ONE = t("deed.have_one", "You've a place at %s already.");
    public static final String DEED_STANDS = t("deed.stands", "There's an Old Place standing empty at %s: %s paces %s. The deed is for that.");
    public static final String DEED_TOO_FAR = t("deed.too_far", "That's too far from %s for the bank to call it town land.");
    public static final String DEED_IN_TOWN = t("deed.in_town", "That's town ground. Pick a spot outside it.");
    public static final String DEED_ROUGH = t("deed.rough", "The ground's too rough here. It wants a level acre.");
    public static final String DEED_WET = t("deed.wet", "The ground's too wet here.");
    public static final String DEED_BUILT = t("deed.built", "Somebody's built here already.");
    public static final String DEED_RAISED = t("deed.raised", "The town turns out with hammers. By the time the dust settles, the place is standing.");
    public static final String CLAIMED = t("claimed", "It's yours: the %s place, %s.");
    public static final String FOR_SALE = t("for_sale", "The Old Place. For sale: ask at the bank in %s.");
    public static final String FOR_SALE_NOWHERE = t("for_sale_nowhere", "The Old Place. For sale: ask at the bank.");
    public static final String STAKE_OWNED = t("stake.owned", "The %s place. %s head of branded stock on it.");
    public static final String SIGN_THE = raw("sign.hollowfrontier.old_place.the", "THE");
    public static final String SIGN_PLACE = raw("sign.hollowfrontier.old_place.place", "PLACE");
    public static final String SIGN_OLD = raw("sign.hollowfrontier.old_place.old", "THE OLD PLACE");
    public static final String SIGN_FOR_SALE = raw("sign.hollowfrontier.old_place.for_sale", "FOR SALE");
    public static final String SIGN_ASK = raw("sign.hollowfrontier.old_place.ask", "ask at the bank");
    public static final String MAIL_NOT_YOURS = t("mail.not_yours", "That's the %s place's mail.");
    public static final String BELL = t("bell", "The bell's ringing in %s. They'll want every gun.");

    // The brand
    public static final String IRON_COLD = t("iron.cold", "The iron's cold. Heat it at a campfire or a furnace.");
    public static final String IRON_HOT = t("iron.hot", "The iron's hot.");
    public static final String IRON_NO_PLACE = t("iron.no_place", "A brand has to be registered to a place. You've no place.");
    public static final String BRANDED = t("branded", "Branded: the %s place, %s.");
    public static final String BRAND_THEIRS = t("brand.theirs", "That one wears the %s brand already.");
    public static final String BRAND_NAME = t("brand.name", "%s brand");
    public static final String RUSTLERS = t("rustlers", "Rustlers! They're after the stock!");
    public static final String RUSTLED = t("rustled", "One of your steers went off on a rustler's rope.");

    // The drives
    public static final String DRIVE_OFFER = t("drive.offer", "%s head to go to %s, %s paces %s. Paid per head that gets there. They're yours to mind from now.");
    public static final String DRIVE_OFFER_TRAIL = t("drive.offer_trail", "%s head to go to the buyer on the trail, %s paces %s. Paid per head that gets there.");
    public static final String FENCE_CUT = t("fence_cut", "Somebody's cut your fence.");
    public static final String DRIVE_HELD = t("drive.held", "The boys are holding them outside town, %s paces %s. Get behind them and they'll walk.");
    public static final String DRIVE_BUSY = t("drive.busy", "You've a herd of mine out already. %s paces %s yet.");
    public static final String DRIVE_STANDING = t("drive.standing", "I don't hand my stock to strangers. Get known here first.");
    public static final String DRIVE_NO_ROOM = t("drive.no_room", "No room to turn a herd out here.");
    public static final String DRIVE_IN = t("drive.in", "%s of %s head came in. %s copper, and the hands take them on to the pens.");
    public static final String DRIVE_LOST = t("drive.lost", "Not a one came in. Word of that gets back.");
    public static final String DRIVE_HEADING = t("drive.heading", "The herd: %s head near. %s paces %s to go.");
    public static final String DRIVE_STAMPEDE = t("drive.stampede", "Stampede!");
    public static final String DRIVE_AMBUSH = t("drive.ambush", "Riders on the back trail!");
    public static final String SOLD = t("sold", "%s head of yours, come %s paces. %s copper.");
    public static final String WRANGLER_NOTHING = t("wrangler.nothing", "No drive today. Bring me branded stock and I'll buy it.");

    // Compass points
    private static final String[] POINTS = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};

    static {
        for (String point : POINTS) {
            raw("stake.hollowfrontier.dir." + point.replace('-', '_'), point);
        }
    }

    private static String t(String name, String english) {
        return raw("message.hollowfrontier.stake." + name, english);
    }

    private static String raw(String key, String english) {
        TEXT.put(key, english);
        return key;
    }

    /** Every key and its English, for the lang provider. */
    public static Map<String, String> all() {
        return Map.copyOf(TEXT);
    }

    /** A line spoken to one player: "&lt;Sheriff&gt; ...". */
    public static void say(ServerPlayer player, Entity speaker, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.npc.say", speaker.getDisplayName(), Component.translatable(key, args)));
    }

    public static int paces(BlockPos from, BlockPos to) {
        return (int) Math.round(Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ()));
    }

    /** Which way {@code to} lies from {@code from}, to the nearest of eight points. */
    public static Component direction(BlockPos from, BlockPos to) {
        double angle = Math.toDegrees(Math.atan2(to.getX() - from.getX(), -(to.getZ() - from.getZ())));
        int index = Math.floorMod((int) Math.round(angle / 45.0), 8);
        return Component.translatable("stake.hollowfrontier.dir." + POINTS[index].replace('-', '_'));
    }
}
