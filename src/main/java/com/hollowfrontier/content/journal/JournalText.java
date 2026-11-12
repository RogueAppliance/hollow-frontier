package com.hollowfrontier.content.journal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything the Prospector's Journal says (48), with the English: the lang provider reads {@link #all}, so a line and
 * its text can't drift apart. The journal is the player's own notebook, so it's first person, short and dry, and the
 * facts in it are right. {@link #NEXT} has a line for every milestone of the Concavian Codex, said the journal's way: a
 * test holds the two lists together.
 */
public final class JournalText {
    private JournalText() {}

    private static final Map<String, String> TEXT = new LinkedHashMap<>();
    /** Milestone (an advancement's path) to the key of the journal's line for it while it's still to do. */
    public static final Map<String, String> NEXT = new LinkedHashMap<>();
    /** Thread (a Codex chapter's name) to the key of the journal's name for it. */
    public static final Map<String, String> THREAD = new LinkedHashMap<>();

    /** Person (see JournalSeen.PEOPLE) to the key of the page's name; strange doing (a MysteryLog kind, or "visit") to its line. */
    public static final Map<String, String> PERSON = new LinkedHashMap<>();
    public static final Map<String, String> STRANGE = new LinkedHashMap<>();

    public static final String SEEN = t("seen", "Creatures");
    public static final String DOINGS = t("doings", "Strange Doings");
    public static final String PEOPLE = t("people", "People");
    public static final String ROAD_TAB = t("tab.road", "Road");
    public static final String SEEN_TAB = t("tab.seen", "Beasts");
    public static final String DOINGS_TAB = t("tab.doings", "Signs");
    public static final String PEOPLE_TAB = t("tab.people", "Folk");
    public static final String TAGGED = t("tagged", "Tagged");
    public static final String STRANGE_NEAR = t("strange.near", "Day %s, by %s. %s");
    public static final String STRANGE_AT = t("strange.at", "Day %s, at %s, %s. %s");
    public static final String TALLY = t("tally", "%s set down so far.");
    public static final String TALLY_WANTED = t("tally.wanted", "The Gazette's editor would pay for a list this long. Take the book to him.");
    public static final String TALLY_SHORT = t("tally.short", "The Gazette's editor pays for ten.");
    public static final String TALLY_PAID = t("tally.paid", "The Gazette's editor has paid for the list.");
    public static final String TALLY_SOLD = t("tally.sold", "Ten of these, and with dates? That's a column. Here's %s gold for the look.");
    public static final String DOINGS_NONE = t("doings.none", "Nothing strange yet. Give it time.");

    public static final String TOWNS_TAB = t("tab.towns", "Towns");
    public static final String TOWN_FIRST = t("town.first", "First came here on day %s.");
    public static final String TOWN_STANDING = t("town.standing", "%s here. Standing %s.");
    public static final String TOWN_STAR = t("town.star", "I wear this town's star.");
    public static final String TOWN_STAR_HELD = t("town.star_held", "The sheriff has my star in his drawer until my name's clear.");
    public static final String TOWN_JOB_ROUNDS = t("town.job.rounds", "Job in hand: walk the rounds after dark, then report.");
    public static final String TOWN_JOB_BRING_IN = t("town.job.bring_in", "Job in hand: bring a drifter in, alive.");
    public static final String TOWN_JOB_WARRANT = t("town.job.warrant", "Job in hand: a warrant to carry to another town's sheriff.");
    public static final String TOWN_CLAIM = t("town.claim", "I hold a place here. %s head of branded stock on it, last I counted.");
    public static final String TOWN_BANK = t("town.bank", "In the bank: %s gold and %s copper.");
    public static final String TOWN_BANK_GOLD = t("town.bank_gold", "In the bank: %s gold.");
    public static final String TOWN_BANK_COPPER = t("town.bank_copper", "In the bank: %s copper.");
    public static final String TOWN_BANK_EMPTY = t("town.bank_empty", "A box at the bank, with nothing in it.");
    public static final String TOWN_POSTER = t("town.poster", "On the board: %s. %s gold.");
    public static final String TOWN_RAID = t("town.raid", "Last raided on day %s.");
    public static final String TOWN_DRIVE = t("town.drive", "A herd of theirs is out with me: %s head near, about %s blocks %s to go.");
    public static final String TOWN_DRIVE_AWAY = t("town.drive_away", "A herd of theirs is out with me, and I'm nowhere near it.");

    public static final String TITLE = t("title", "Prospector's Journal");
    public static final String TOOLTIP = t("tooltip", "What you've seen so far, and what's next.");
    public static final String ROAD = t("road", "The Road");
    public static final String PINNED = t("pinned", "Pinned");
    public static final String PIN = t("pin", "Pin to the first page");
    public static final String UNPIN = t("unpin", "Unpin");
    public static final String DONE = t("done", "Done");
    public static final String NEXT_HEAD = t("next", "Next");
    public static final String DONE_ALL = t("done_all", "Nothing left to do on this road.");
    public static final String SURVEY = t("survey", "The Survey's list");
    public static final String HEADING = t("heading", "The crater: about %s blocks %s of here.");
    public static final String NOTED = t("noted", "Noted: %s");
    public static final String COUNT = t("count", "%s noted");
    public static final String PAGE = t("page", "%s of %s");
    public static final String TURN_BACK = t("turn_back", "Turn back a page");
    public static final String TURN_ON = t("turn_on", "Turn the page");
    public static final String NOT_CARRIED = t("not_carried", "You aren't carrying a journal.");

    static {
        thread("frontier", "The Frontier");
        thread("steam", "The Road North");
        thread("hollow", "The World Within");
        thread("concavia", "Concavia");
        thread("grays", "The Grays");
        thread("after", "After");
        thread("sea", "The Still Sea");

        person("outlaws", "Outlaws");
        fact("outlaws", "met", "They ride in gangs: a boss, his gunslingers, and the rest. A badge off one pays at a Bounty Board.");
        fact("outlaws", "bounty", "Turned a badge in. The board paid and asked nothing.");
        fact("outlaws", "freed", "Found one a place in a town after I got him out. He took it.");
        fact("outlaws", "truth", "The Kindled say they aren't born cruel. The grey ones take drifters off the plains, set a seed of metal behind the ear, and send them back angry.");
        fact("outlaws", "loose", "The leash is broken. The ones still riding do it by their own choice now.");
        person("dusters", "The Men in Dusters");
        fact("dusters", "met", "Men in black dusters. They turn up where something strange has happened, a while after, and lean on whoever saw it.");
        fact("dusters", "warned", "One of them warned me off. I'd call it a courtesy.");
        fact("dusters", "sold", "They buy anything of the Grays', and then there never was any such thing.");
        fact("dusters", "marked", "They've come for me. Minding the Grays' business does that.");
        fact("dusters", "truth", "The Kindled say they carry the same seed as the outlaws, set more carefully, and that they chose it, for money and long life.");
        fact("dusters", "loose", "When the leash broke they walked off into the dust. None has come back.");
        person("kindled", "The Kindled");
        fact("kindled", "met", "Somebody still lives in the Highlands. They call themselves the Kindled.");
        fact("kindled", "ally", "They've given me their sigil. In its light the seed behind a man's ear shows, through stone and through dark.");
        fact("kindled", "truth", "Their story, heard through: this was Concavia, and the sun inside it was theirs to keep. The grey ones came down hungry and polite and were let in. They took the cities, the craft and the phlogiston.");
        fact("kindled", "archon", "Their last king went into the sun to hold it, and didn't come out as he went in. That was the Archon. I brought it down.");
        person("grays", "The Grays");
        fact("grays", "met", "Whatever they are, they aren't from the frontier, or from anywhere else I've been.");
        fact("grays", "caught", "Caught one of their probes at work, on a steer or in a wheat field. So that's who.");
        fact("grays", "notes", "Found a book of their field notes. They've been studying us. We're \"specimens\".");
        fact("grays", "pens", "They keep people in pens down in the Hollow. I let one out.");
        fact("grays", "truth", "The Kindled say the ships were never the Grays': they were Concavia's. And that a frontier at war with itself never looks up, which is all the Grays want of it.");
        fact("grays", "fallen", "The Mothership is down.");
        fact("grays", "loose", "The leash is broken and the implants have gone dark. They still land, some nights.");

        strange("sighting", "Lights crossing the sky.");
        strange("mutilation", "Cattle found dead and cut about.");
        strange("crop_circle", "A circle pressed into a wheat field.");
        strange("landing", "Something landed.");
        strange("visit", "Men in black dusters came asking.");

        creature("rattlesnake", "Rattles when you come near and strikes if you come nearer. The bite poisons. It backs away from cats.",
                "Snakeskin, for boots. In those a snake only bites if provoked, and the bite doesn't poison.", null);
        creature("fern_skitter", "Chicken-sized and feathered, in little flocks. Keeps off unless you creep up. It steals any food left lying and runs.",
                "Takes meat from the hand, and about one bite in three tames it. A tame one follows, and sits when told. Still a thief. A feather off one now and then.", null);
        creature("glassback_hare", "A big hare with glowing crystals down its back. It bolts unless you creep up. A carrot held out brings it in.",
                "Rabbit hide, and now and then an amethyst shard.", "One I met wore a yellow tag clipped in its ear.");
        creature("ash_salamander", "Black, with orange spots that glow. Walks on lava, can't abide water, and lies basking on anything hot. Harmless.",
                "Magma cream, now and then.", null);
        creature("pale_raven", "A white raven on the standing stones. Walk up and it flies off croaking. Worth following: where there are old ruins near, that's where it goes.",
                "A feather, now and then.", null);
        creature("woolly_mammoth", "Taller than a house door, and in herds. Left alone it grazes. Hurt one and the whole herd charges. Terror birds keep well clear of them.",
                "Tusks (they make the Oliphant), mammoth meat and leather.", "One with a tag on it goes for you on sight.");
        creature("terror_bird", "Half again as tall as a man, and it hunts. It screeches, then runs you down a little faster than you can sprint. A horse outruns it. It won't go near mammoths.",
                "A talon (for the Talon Knife), feathers, and meat like chicken. A blast on the Oliphant puts it to flight.", "One with a tag on it is faster, and fears neither mammoth nor Oliphant.");
        creature("megatherium", "A ground sloth the size of an elephant. Slow. It rears up to strip leaves off the trees. Leave it be: it's very hard to kill, and it swipes back.",
                "Thick hide (for the Hide Barding) and leather.", "One with a tag on it goes for you on sight.");
        creature("glyptodon", "An armadillo the size of a wagon, with a club for a tail. Harmless. Frighten it and it pulls in under its shell and won't budge.",
                "Bony scutes, for shell armor. It sheds them, and a brush takes one off without hurting it.", "One with a tag on it never pulls in.");
        creature("great_sea_cow", "Slow, grey and gentle, grazing the kelp in herds. It never fights back. Kelp in the hand leads it.",
                "Kelp breeds them. Blubber, which renders down to Tallow.", null);
        creature("basilosaurus", "A whale as long as a street and thin as an eel. It hunts the deep water: circles first, then comes in fast. A boat is no safer than swimming. It leaves the shallows alone.",
                "Blubber, and a tooth.", null);
        creature("plesiosaur", "The long neck you see from the shore. A fish-eater that keeps its distance, and bites back if hurt.",
                "Hand it a fish and it dives, and brings you something off the bottom: shells and kelp mostly, now and then better.", null);
        creature("lampjelly", "A lit bell of jelly the size of a pumpkin, drifting in shoals. Touch one and it stings: a few seconds of poison.",
                "Lamp Gel.", null);

        next("frontier/mint_coin", "Four copper nuggets make a coin. Mint one. Nobody out here takes anything else.");
        next("frontier/load_gun", "Get a black powder gun and load a chamber. Powder, ball, cap, and no hurrying it.");
        next("frontier/dead_or_alive", "Take the badge off an outlaw and turn it in at a Bounty Board.");
        next("frontier/lights_in_the_sky", "Folks talk about lights crossing the sky at night. Stay up and look.");

        next("steam/head_of_steam", "Set down a boiler. Water in, fire under.");
        next("steam/handcar", "Build a Steam Handcar. It beats walking the line.");
        next("steam/airship", "Build an airship. North is a long way on foot.");
        next("steam/true_north", "Get hold of a Polar Compass and take a bearing with it.");
        next("steam/open_sesame", "Wake the ring at the crater. It wants a Polar Resonator and 2,000 mB of steam.");
        next("steam/hollow_earth", "The ring's awake. Step through it.");

        next("hollow/phlogiston", "Something down here glows and doesn't burn out. Find a shard of it.");
        next("hollow/starsteel", "There's a red ore in this country. Smelt an ingot of what's in it.");
        next("hollow/concavian_highlands", "There's high ground a long way off. Climb up into it.");
        next("hollow/escaped_specimen", "Some of the beasts here wear a tag. Bring one down and see whose.");
        next("hollow/on_the_signal", "Carry a Signal Locator until it picks something up.");

        next("concavia/root", "Find the ruins of a city in the Highlands.");
        next("concavia/well_met", "Somebody still lives up here. Speak with one of them.");
        next("concavia/an_ally_below", "Win the Kindled's trust, and their sigil. They trade, so start there.");
        next("concavia/the_truth", "The Kindled have a story. Hear it through to the end.");
        next("concavia/temple_of_the_sun", "Climb the Sun Temple in the Ember Badlands.");
        next("concavia/eclipse", "Bring down the Sun Archon.");

        next("ufo/root", "See one of their saucers in the night sky.");
        next("ufo/crash_and_burn", "One of the saucers came down somewhere. Find the wreck.");
        next("ufo/salvage_rights", "Pry something out of a saucer's wreckage.");
        next("ufo/death_ray", "Make a Ray Pistol or a Phlogiston Rifle out of what they left lying about.");
        next("ufo/defying_gravity", "Mend a damaged gravity drive. It wants phlogiston and starsteel.");
        next("ufo/we_have_liftoff", "Build a Prototype Saucer and take it up.");
        next("ufo/calling_them_down", "Light a Phlogiston Beacon at night and call the Mothership down.");
        next("ufo/the_mothership_falls", "Bring the Mothership down out of the sky.");
        next("ufo/cut_loose", "See the leash on the frontier broken.");

        next("ufo/trespassers", "They still land. Clear a Gray Landing before dawn.");
        next("ufo/second_chance", "Find a freed outlaw a place in a town: a deputy's star, or the way home.");
        next("concavia/concavia_rising", "Restore a Crystal Ruins to its last stage and wake its waystone.");
        next("concavia/old_roads", "Travel by waystone.");
        next("ufo/encore", "A Mothership came back for more. Bring that one down too.");
        next("ufo/fully_fitted", "Fit a saucer with the lot (beam emitter, lens array, navigation core, cloaking device, cargo hold) and fly it.");

        next("hollow/still_sea", "Reach the Still Sea. No wind there, and no rain.");
        next("hollow/sea_cow", "Get blubber off a Great Sea Cow. Rendered to tallow it burns like coal.");
        next("hollow/harpoon", "Make a Harpoon. What it hits, it holds.");
        next("steam/submersible", "Build a Steam Submersible. Fill it with steam first: it makes none of its own.");
        next("hollow/symmes_landing", "The Symmes expedition drew a boat up somewhere on this shore. Find where.");
        next("hollow/drowned_quarter", "Find a Drowned Quarter on the bed of the Still Sea.");
        next("hollow/sealed_hull", "Take the Sealed Hull from a Drowned Quarter's vault.");
    }

    private static String t(String name, String english) {
        String key = "journal.hollowfrontier." + name;
        TEXT.put(key, english);
        return key;
    }

    private static void thread(String name, String english) {
        THREAD.put(name, t("thread." + name, english));
    }

    private static void person(String id, String english) {
        PERSON.put(id, t("people." + id, english));
    }

    private static void fact(String person, String fact, String english) {
        t("people." + person + "." + fact, english);
    }

    private static void strange(String kind, String english) {
        STRANGE.put(kind, t("strange." + kind, english));
    }

    /** A creature's lines: on sight; once tamed, bred, fed or killed; and, for those the Grays tag, once a tagged one is met. */
    private static void creature(String id, String met, String known, String tagged) {
        t("creature." + id + ".met", met);
        t("creature." + id + ".known", known);
        if (tagged != null) {
            t("creature." + id + ".tagged", tagged);
        }
    }

    public static String key(String name) {
        return "journal.hollowfrontier." + name;
    }

    private static void next(String step, String english) {
        NEXT.put(step, t("next." + step.replace('/', '.'), english));
    }

    public static Map<String, String> all() {
        return TEXT;
    }
}
