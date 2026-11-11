package com.hollowfrontier.datagen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.concavia.Kindled;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.HollowEarth;
import com.hollowfrontier.worldgen.structure.ConcaviaBlueprints;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.ChangeDimensionTrigger;
import net.minecraft.advancements.triggers.ConsumeItemTrigger;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.triggers.KilledTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.advancements.triggers.RecipeCraftedTrigger;
import net.minecraft.advancements.triggers.StartRidingTrigger;
import net.minecraft.advancements.triggers.TradeTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The advancement tabs. Each tab is its own method with ids {@code hollowfrontier:<tab>/<name>}; titles and descriptions are
 * collected in {@link #TEXT} for the language provider (keys {@code advancements.hollowfrontier.<tab>.<name>.title|description}).
 * <ul>
 * <li><b>frontier</b>: guns, horses, the saloon, the law and bounties, raids, town cats.</li>
 * <li><b>steam</b>: brass, the boiler, canisters, the crusher and drill, the handcar and the depot; then (phase 8) the
 *     airship and the polar gateway, both hung off {@code steam/handcar}.</li>
 * <li><b>hollow</b> (9c): the Hollow Earth's four lands, phlogiston and starsteel, and its lore sites as hidden finds.</li>
 * <li><b>concavia</b> (12a): the crystal ruins, the Kindled, their story and their trust, the guardians.</li>
 * <li><b>ufo</b> (13a): the saucers, abductions, crash sites and their salvage, the Wardens.</li>
 * <li>Later phases add to these tabs, or a tab each as a new method here, so
 *     nothing above has to move.</li>
 * </ul>
 * Mod events use {@link ModCriteria} triggers; everything else uses vanilla ones.
 */
public final class ModAdvancements {
    private ModAdvancements() {}

    /** name -> {title, description}, in tree order. */
    public static final Map<String, String[]> TEXT = new LinkedHashMap<>();

    // Reward loot tables (ModRewardLootProvider).
    public static final ResourceKey<LootTable> REWARD_RECOVERED = reward("loot_recovered");
    public static final ResourceKey<LootTable> REWARD_BLACKOUT = reward("blackout");
    public static final ResourceKey<LootTable> REWARD_GHOST_CAT = reward("ghost_cat");
    public static final ResourceKey<LootTable> REWARD_LONG_HAUL = reward("long_haul");

    private static ResourceKey<LootTable> reward(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, HollowFrontier.id("rewards/" + name));
    }

    static {
        // Text lives here so the language provider (a separate datagen event) and the bootstrap agree.
        text("frontier/root", "Hollow Frontier", "Saddle up. The frontier's wide open, and most of it wants you dead");
        text("frontier/mint_coin", "Coin of the Realm", "Mint a copper coin from four copper nuggets");
        text("frontier/extra_extra", "Extra! Extra!", "Pick up a copy of the Frontier Gazette. Read all about it");
        text("frontier/money_in_the_bank", "Money in the Bank", "Sneak up to a town's Banker and open a safe deposit box");
        text("frontier/compound_interest", "Compound Interest", "Come back to your safe deposit box a day later and collect the interest");
        text("frontier/saved_the_savings", "Saved the Savings", "Shoot the bagman before he rides off with coins from your own safe deposit box");
        text("frontier/load_gun", "Powder, Ball and Cap", "Load a chamber of a black powder gun, one careful step at a time");
        text("frontier/six_for_six", "Six for Six", "Load every chamber of a Cap & Ball Revolver");
        text("frontier/rattler", "Don't Tread on Me", "Kill a rattlesnake. It did warn you");
        text("frontier/snakeskin_boots", "Snakeskin Swagger", "Make Snakeskin Boots. Rattlers give the wearer a wide berth");
        text("frontier/dead_or_alive", "Dead or Alive", "Turn in an outlaw's badge at a Bounty Board");
        text("frontier/or_alive", "...Or Alive", "Bring an outlaw from a Gray pen to the town that posted him, alive");
        text("frontier/hold_the_line", "Hold the Line", "Help a town see off an outlaw raid");
        // 47: a stake in town
        text("frontier/known_around_town", "Known Around Town", "Do enough for a town that it knows your name");
        text("frontier/the_star", "The Star", "Be sworn in as a town's deputy. The sheriff asks a Neighbour");
        text("frontier/an_honest_days_work", "An Honest Day's Work", "Do a job for the sheriff");
        text("frontier/the_old_place", "The Old Place", "Take up a deed to a homestead of your own");
        text("frontier/riding_for_the_brand", "Riding for the Brand", "Put your brand on a cow with a hot iron");
        text("frontier/head_em_up", "Head 'Em Up", "Bring a herd in at the end of a drive");
        text("frontier/pillar_of_the_town", "Pillar of the Town", "Stand as high in a town as anyone can");
        text("frontier/not_so_fast", "Not So Fast, Partner", "Shoot the Safecracker before he rides out with the town's money");
        text("frontier/well_heeled", "Well Heeled", "Fit horseshoes to a tamed horse");
        text("frontier/lights_in_the_sky", "Lights in the Sky", "See one of the airships that cross the frontier by night");
        text("frontier/caught_in_the_act", "Caught in the Act", "Bring down a Gray probe while it's at work on a steer or a wheat field");
        text("frontier/not_of_this_world", "Not of This World", "Dig up the airship pilot buried on Boot Hill");
        text("frontier/nothing_to_see", "Nothing to See Here", "Sell one of the Grays' things to a man in a black duster");
        text("frontier/word_to_the_wise", "A Word to the Wise", "Get warned off by a man in a black duster");
        text("frontier/marked_man", "Marked Man", "Mind the Grays' business until the men in black come for you");
        text("frontier/rope_em_in", "Rope 'Em In", "Catch a critter with a lasso");
        text("frontier/dressed_for_the_range", "Dressed for the Range", "Get the whole cowboy outfit: hat, duster, chaps and boots. Worn together, they ride faster");
        text("frontier/hold_on_to_your_hat", "Hold On to Your Hat", "Lose your hat to a dust devil. Keep low next time");
        text("frontier/pull_up_a_stool", "Pull Up a Stool", "Take a seat at the bar");
        text("frontier/wet_your_whistle", "Wet Your Whistle", "Have a drink from the saloon");
        text("frontier/missing_time_question", "Missing Time?", "Come to somewhere else in town with an hour gone and somebody else's hat on");
        text("frontier/lights_out", "Lights Out", "Drink past Tipsy IV. The floor came up to meet you");
        text("frontier/twenty_one", "Twenty-One", "Get dealt a natural blackjack at a saloon card table");
        text("frontier/five_card_charlie", "Five-Card Charlie", "Win a hand of blackjack holding five cards or more");
        text("frontier/cleaned_out", "Cleaned Out", "Lose five hands of blackjack in a row. The house always wins");
        text("frontier/reputation", "A Certain Reputation", "Get on the wrong side of the law");
        text("frontier/clean_slate", "Clean Slate", "Pay off your fine at a Bounty Board. Crime doesn't pay, but you do");
        text("frontier/most_wanted", "Most Wanted", "Earn three wanted stars. Hear those hoofbeats? That's the posse");
        text("frontier/honor_among_thieves", "Honor Among Thieves", "Take your cut of a town robbery. Your mother would be so proud");
        text("frontier/dressed_to_the_nines", "Dressed to the Nines", "Put a bandana, a hat or a star on your own cat");
        text("frontier/deputized", "Deputized", "Get your hands on a brass star. Its owner is very attached to it");
        text("frontier/nine_lives", "Nine Lives, Give or Take", "Something pale haunts the undertaker's. They say it likes a hot supper, and a song");

        text("steam/root", "Steam Age", "Copper and a little gold make brass. Brass and fire make progress");
        text("steam/head_of_steam", "A Head of Steam", "Set down a boiler. Water in, fire under, steam out");
        text("steam/canned_steam", "Canned Weather", "Fill a steam canister from a boiler, pipe or tank");
        text("steam/grind_it_out", "Grind It Out", "Get ore dust from a Steam Crusher. Two for the price of one");
        text("steam/hole_lotta_steam", "Hole Lotta Steam", "Make a Steam Drill");
        text("steam/handcar", "Iron Horse, Junior", "Build a Steam Handcar");
        text("steam/toot_toot", "Toot Toot!", "Blow a handcar's steam whistle");
        text("steam/water_stop", "Water Stop", "Fill a handcar to the brim at a standpipe. Depots keep one by the track");
        text("steam/laying_track", "Laying Track", "Buy rails from a railroad engineer");
        text("steam/long_haul", "Transcontinental (Nearly)", "Drive a steam handcar a thousand blocks without getting off");
        text("steam/airship", "Lighter Than Air", "Build an airship");
        text("steam/submersible", "No Fire Below", "Build a Steam Submersible. It runs on steam it was given, not steam it makes");
        text("steam/above_the_clouds", "Above the Clouds", "Take an airship up past height 200");
        text("steam/true_north", "True North", "Take a bearing with a Polar Compass");
        text("steam/open_sesame", "Open Sesame", "Wake a Polar Gateway with a Polar Resonator and 2,000 mB of steam");
        text("steam/hollow_earth", "Journey to the Centre", "Step through a Polar Gateway into the Hollow Earth");

        text("hollow/root", "Under Another Sun", "Come down through the crust into the Hollow Earth");
        text("hollow/primordial_jungle", "Here Be Dinosaurs", "Walk the Primordial Jungle");
        text("hollow/crystal_steppes", "Fields of Glass", "Cross the Crystal Steppes");
        text("hollow/ember_badlands", "Hot Under the Collar", "Brave the Ember Badlands");
        text("hollow/concavian_highlands", "High Country", "Climb into the Concavian Highlands");
        text("hollow/world_within", "A World Within", "Set foot in all four of the Hollow Earth's lands");
        text("hollow/phlogiston", "Bottled Lightning", "Find a shard of phlogiston. It glows, and it burns a long, long time");
        text("hollow/phlogiston_lamp", "Never Needs Trimming", "Make a Phlogiston Lamp");
        text("hollow/starsteel", "Red Gold", "Smelt a starsteel ingot");
        text("hollow/sun_forged", "Sun-Forged", "Wear a full suit of starsteel armor. Stand in the sun and it mends itself");
        text("hollow/fleet_of_hoof", "Fleet of Hoof", "Shoe a horse in starsteel. Neither of you need fear a fall again");
        text("hollow/older_than_babel", "Older Than Babel", "Find the work of the Concavians in the Highlands");
        text("hollow/under_observation", "Under Observation", "Find what the Grays left behind");
        text("hollow/lost_and_found", "Lost and Found", "Find the last camp of the Symmes expedition");
        text("hollow/woolly_thinking", "Woolly Thinking", "Take a tusk from a woolly mammoth");
        text("hollow/oliphant", "Sound the Horn", "Make an Oliphant. One blast calms angry beasts and scatters terror birds");
        text("hollow/dont_run", "Don't Run", "Bring down a terror bird. You can't outrun one on foot anyway");
        text("hollow/shell_game", "Shell Game", "Get a scute off a glyptodon (a brush does it kindly)");
        text("hollow/shell_suit", "Armadillo Chic", "Get a whole suit of glyptodon shell armor");
        text("hollow/big_game","Big Game", "Bring down a mammoth, a terror bird, a megatherium and a glyptodon");
        // 43: the Still Sea
        text("hollow/still_sea", "No Tide Tables", "Reach the Still Sea, where nothing blows and nothing rains");
        text("hollow/sea_cow", "Rendering", "Get blubber off a Great Sea Cow. It burns like coal once it's tallow");
        text("hollow/lamp_gel", "Light Without Fire", "Get Lamp Gel from a Lampjelly. Mind the sting");
        text("hollow/harpoon", "On the Line", "Make a Harpoon. What it hits, it holds");
        text("hollow/plesiosaur_fetch", "Good Neck", "Give a Plesiosaur a fish and see what it brings up");
        text("hollow/basilosaurus", "Deep Water", "Bring down a Basilosaurus");
        text("hollow/symmes_landing", "Not Launching Her Again", "Find where the Symmes expedition drew up its boat");
        text("hollow/drowned_quarter", "Under Glass", "Find a Drowned Quarter on the bed of the Still Sea");
        text("hollow/sealed_hull", "Made for the Day the River Rose", "Take the Sealed Hull from a Drowned Quarter's vault");
        text("hollow/still_running", "Still Running", "Find a Gray field station that hasn't been abandoned");
        text("hollow/escaped_specimen", "Escaped Specimen", "Bring down a beast the Grays have tagged");
        text("hollow/on_the_signal", "On the Signal", "Pick up a Gray station's signal on a Signal Locator");
        text("hollow/jailbreak", "Jailbreak", "Free a captive from a Gray pen");
        text("hollow/not_on_my_watch", "Not on My Watch", "Stop the Grays' procedure before it's finished");
        text("hollow/special_delivery", "Special Delivery", "Bring a freed captive's Letter Home to their town");

        text("concavia/root", "The Crystal City", "Find the ruins of a Concavian city in the Highlands");
        text("concavia/crystal_clear", "Crystal Clear", "Get hold of some Concavian Crystal");
        text("concavia/well_met", "Well Met", "Speak with one of the Kindled");
        text("concavia/the_truth", "The Truth Is Down Here", "Hear the Kindled's story through to its end");
        text("concavia/an_ally_below", "An Ally Below", "Win the Kindled's trust, and their sigil");
        text("concavia/glass_jaw", "Glass Jaw", "Bring down a Crystal Guardian");
        text("concavia/required_reading", "Required Reading", "Open the Concavian Codex");
        text("concavia/temple_of_the_sun", "Temple of the Sun", "Climb the Sun Temple in the Ember Badlands");
        text("concavia/eclipse", "Eclipse", "Bring down the Sun Archon");
        text("concavia/heart_of_the_sun", "Heart of the Sun", "Take the Phlogiston Core from the Archon");
        text("concavia/uneasy_lies_the_head", "Uneasy Lies the Head", "Wear the last king's Starsteel Crown");
        text("concavia/let_there_be_light", "Let There Be Light", "Strike something with the Sunstone Lens's beam");

        text("ufo/root", "Close Encounters", "See a Gray saucer in the night sky");
        text("ufo/missing_time", "Missing Time", "Be taken up by a saucer's beam");
        text("ufo/not_today", "Not Today", "Slip out of a saucer's beam by keeping low");
        text("ufo/crash_and_burn", "Crash and Burn", "Find a downed Gray saucer");
        text("ufo/salvage_rights", "Salvage Rights", "Pry something out of a saucer's wreckage");
        text("ufo/some_assembly_required", "Some Assembly Required", "Hold every kind of saucer salvage at once: plating, gravity drive, beam emitter, lens array and navigation core");
        text("ufo/take_me_to_your_leader", "Take Me to Your Leader", "Bring down a Gray Warden");
        text("ufo/lightning_in_a_bottle", "Lightning in a Bottle", "Charge a Phlogiston Cell");
        text("ufo/death_ray", "Death Ray", "Make a Ray Pistol or a Phlogiston Rifle");
        text("ufo/defying_gravity", "Defying Gravity", "Mend a damaged gravity drive with phlogiston and starsteel");
        text("ufo/we_have_liftoff", "We Have Liftoff", "Craft a Prototype Saucer and take it up");
        text("ufo/beam_me_up", "Beam Me Up", "Lift something with your saucer's tractor beam");
        text("ufo/high_flyer", "Strange Lights Over the Frontier", "Take your saucer above the clouds");
        text("ufo/calling_them_down", "Calling Them Down", "Light a Phlogiston Beacon at night and call the Mothership");
        text("ufo/dogfight", "Dogfight", "Bring down one of the Mothership's drones");
        text("ufo/the_mothership_falls", "The Mothership Falls", "Bring the Mothership down out of the sky");
        text("ufo/cut_loose", "Cut Loose", "See the Grays' leash on the frontier broken");
        text("ufo/trespassers", "Trespassers", "Clear a Gray Landing before dawn");
        text("ufo/not_from_around_here", "Not From Around Here", "Find out a Gray in disguise, with a Sunstone Lens or a fist");
        text("ufo/regular_customer", "Regular Customer", "Leave a disguised Gray be until it's the saloon's regular, then have a word with it");
        text("ufo/encore", "Encore", "Bring down a Mothership that came back for more");
        text("ufo/second_chance", "A Second Chance", "Give a freed outlaw a new life in a town: a deputy's star, or a way home");
        text("concavia/concavia_rising", "Concavia Rising", "Restore a Crystal Ruins to its last stage and wake its waystone");
        text("concavia/old_roads", "Old Roads", "Travel by waystone");
        text("ufo/fully_fitted", "Fully Fitted", "Fly a saucer with every fitting: beam emitter, lens array, navigation core, cloaking device and cargo hold");
        text("ufo/legend_of_the_frontier", "Legend of the Frontier", "Bring down an Outlaw Boss, the Sun Archon and the Mothership. They'll sing about you in the saloons");
    }

    private static void text(String name, String title, String description) {
        TEXT.put(name, new String[] {title, description});
    }

    public static void bootstrap(BootstrapContext<Advancement> output) {
        new Tabs(output).frontier();
        new Tabs(output).steam();
        new Tabs(output).hollow();
        new Tabs(output).concavia();
        new Tabs(output).ufo();
    }

    private static final class Tabs {
        private final BootstrapContext<Advancement> output;
        private final HolderGetter<Item> items;
        private final HolderGetter<Block> blocks;
        private final HolderGetter<EntityType<?>> entities;

        Tabs(BootstrapContext<Advancement> output) {
            this.output = output;
            this.items = output.lookup(Registries.ITEM);
            this.blocks = output.lookup(Registries.BLOCK);
            this.entities = output.lookup(Registries.ENTITY_TYPE);
        }

        void frontier() {
            AdvancementHolder root = Advancement.Builder.advancement()
                    .rootDisplay(ModItems.COWBOY_HAT.get(), title("frontier/root"), description("frontier/root"),
                            Identifier.withDefaultNamespace("block/stripped_spruce_log"), AdvancementType.TASK, false, false, false)
                    .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                    .save(output, id("frontier/root"));

            // Money and news
            AdvancementHolder mint = task(root, "frontier/mint_coin", ModItems.COPPER_COIN.get())
                    .addCriterion("minted", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(
                            output.lookup(Registries.RECIPE).getOrThrow(ResourceKey.create(Registries.RECIPE, HollowFrontier.id("copper_coin"))))))
                    .save(output, id("frontier/mint_coin"));
            AdvancementHolder gazette = task(mint, "frontier/extra_extra", ModItems.FRONTIER_GAZETTE.get())
                    .addCriterion("gazette", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.FRONTIER_GAZETTE.get()))
                    .save(output, id("frontier/extra_extra"));
            // The bank (16)
            AdvancementHolder bank = task(mint, "frontier/money_in_the_bank", ModItems.GOLD_COIN.get())
                    .addCriterion("opened", ModCriteria.BANK_OPENED.get().criterion())
                    .save(output, id("frontier/money_in_the_bank"));
            task(bank, "frontier/compound_interest", ModItems.COPPER_COIN.get())
                    .addCriterion("interest", ModCriteria.BANK_INTEREST.get().criterion())
                    .save(output, id("frontier/compound_interest"));
            display(bank, "frontier/saved_the_savings", Items.BUNDLE, AdvancementType.GOAL, false)
                    .addCriterion("saved", ModCriteria.BANK_SAVED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("frontier/saved_the_savings"));
            // The frontier's mysteries (11a), off the Gazette that reports them
            AdvancementHolder lights = task(gazette, "frontier/lights_in_the_sky", Items.GLOWSTONE_DUST)
                    .addCriterion("seen", ModCriteria.SIGHTING_SEEN.get().criterion())
                    .save(output, id("frontier/lights_in_the_sky"));
            display(lights, "frontier/caught_in_the_act", ModItems.GRAY_TECH_FRAGMENT.get(), AdvancementType.GOAL, false)
                    .addCriterion("caught", ModCriteria.CAUGHT_IN_THE_ACT.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("frontier/caught_in_the_act"));
            display(lights, "frontier/not_of_this_world", ModItems.STRANGE_SKULL.get(), AdvancementType.GOAL, true)
                    .addCriterion("skull", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.STRANGE_SKULL.get()))
                    .save(output, id("frontier/not_of_this_world"));
            // The men in black (11b)
            task(lights, "frontier/nothing_to_see", ModItems.GOLD_COIN.get())
                    .addCriterion("sold", ModCriteria.SOLD_TO_THEM.get().criterion())
                    .save(output, id("frontier/nothing_to_see"));
            AdvancementHolder warned = task(lights, "frontier/word_to_the_wise", Items.DYE.black())
                    .addCriterion("warned", ModCriteria.WARNED_OFF.get().criterion())
                    .save(output, id("frontier/word_to_the_wise"));
            display(warned, "frontier/marked_man", Items.SKELETON_SKULL, AdvancementType.GOAL, false)
                    .addCriterion("marked", ModCriteria.MARKED.get().criterion())
                    .save(output, id("frontier/marked_man"));

            // Guns, snakes, bounties, raids
            AdvancementHolder loadGun = task(root, "frontier/load_gun", ModItems.REVOLVER_CARTRIDGE.get())
                    .addCriterion("loaded", ModCriteria.GUN_LOADED.get().criterion())
                    .save(output, id("frontier/load_gun"));
            task(loadGun, "frontier/six_for_six", ModItems.REVOLVER.get())
                    .addCriterion("six", ModCriteria.GUN_LOADED.get().atLeast(6))
                    .save(output, id("frontier/six_for_six"));
            AdvancementHolder rattler = task(loadGun, "frontier/rattler", ModItems.SNAKESKIN.get())
                    .addCriterion("rattlesnake", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(entities, ModEntities.RATTLESNAKE.get())))
                    .save(output, id("frontier/rattler"));
            task(rattler, "frontier/snakeskin_boots", ModItems.SNAKESKIN_BOOTS.get())
                    .addCriterion("boots", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SNAKESKIN_BOOTS.get()))
                    .save(output, id("frontier/snakeskin_boots"));
            AdvancementHolder bounty = display(loadGun, "frontier/dead_or_alive", ModItems.OUTLAW_BADGE.get(), AdvancementType.GOAL, false)
                    .addCriterion("claimed", ModCriteria.BOUNTY_CLAIMED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(30))
                    .save(output, id("frontier/dead_or_alive"));
            // 20: the man himself, walked in from a Gray pen
            display(bounty, "frontier/or_alive", Items.LEAD, AdvancementType.GOAL, false)
                    .addCriterion("alive", ModCriteria.BROUGHT_IN_ALIVE.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("frontier/or_alive"));
            AdvancementHolder raid = display(bounty, "frontier/hold_the_line", ModItems.PLAINS_RIFLE.get(), AdvancementType.GOAL, false)
                    .addCriterion("defended", ModCriteria.RAID_DEFENDED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("frontier/hold_the_line"));
            display(raid, "frontier/not_so_fast", ModItems.GOLD_COIN.get(), AdvancementType.CHALLENGE, false)
                    .addCriterion("recovered", ModCriteria.LOOT_RECOVERED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100).addLootTable(output.lookup(Registries.LOOT_TABLE).getOrThrow(REWARD_RECOVERED)))
                    .save(output, id("frontier/not_so_fast"));

            // 47: a stake in town
            AdvancementHolder known = task(bounty, "frontier/known_around_town", ModItems.FRONTIER_GAZETTE.get())
                    .addCriterion("known", ModCriteria.STANDING_RANK.get().criterion())
                    .save(output, id("frontier/known_around_town"));
            AdvancementHolder star = display(known, "frontier/the_star", ModItems.DEPUTY_BADGE.get(), AdvancementType.GOAL, false)
                    .addCriterion("sworn", ModCriteria.DEPUTISED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("frontier/the_star"));
            task(star, "frontier/an_honest_days_work", ModItems.COPPER_COIN.get())
                    .addCriterion("job", ModCriteria.JOB_DONE.get().criterion())
                    .save(output, id("frontier/an_honest_days_work"));
            AdvancementHolder place = display(known, "frontier/the_old_place", ModItems.DEED.get(), AdvancementType.GOAL, false)
                    .addCriterion("homesteader", ModCriteria.HOMESTEADER.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("frontier/the_old_place"));
            task(place, "frontier/riding_for_the_brand", ModItems.BRANDING_IRON.get())
                    .addCriterion("branded", ModCriteria.BRANDED.get().criterion())
                    .save(output, id("frontier/riding_for_the_brand"));
            display(known, "frontier/head_em_up", Items.LEAD, AdvancementType.GOAL, false)
                    .addCriterion("drive", ModCriteria.DRIVE_DONE.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("frontier/head_em_up"));
            display(known, "frontier/pillar_of_the_town", ModItems.GOLD_COIN.get(), AdvancementType.CHALLENGE, false)
                    .addCriterion("pillar", ModCriteria.STANDING_RANK.get().atLeast(3))
                    .rewards(AdvancementRewards.Builder.experience(200))
                    .save(output, id("frontier/pillar_of_the_town"));

            // Horses
            AdvancementHolder shod = task(root, "frontier/well_heeled", ModItems.IRON_HORSESHOES.get())
                    .addCriterion("shod", ModCriteria.HORSE_SHOD.get().criterion())
                    .save(output, id("frontier/well_heeled"));
            task(shod, "frontier/rope_em_in", ModItems.LASSO.get())
                    .addCriterion("lassoed", ModCriteria.LASSOED.get().criterion())
                    .save(output, id("frontier/rope_em_in"));
            task(shod, "frontier/dressed_for_the_range", ModItems.DUSTER.get())
                    .addCriterion("outfit", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.COWBOY_HAT.get(), ModItems.DUSTER.get(),
                            ModItems.CHAPS.get(), ModItems.COWBOY_BOOTS.get()))
                    .save(output, id("frontier/dressed_for_the_range"));
            // 29: easy come, easy go
            task(shod, "frontier/hold_on_to_your_hat", ModItems.COWBOY_HAT.get())
                    .addCriterion("blown", ModCriteria.HAT_BLOWN.get().criterion())
                    .save(output, id("frontier/hold_on_to_your_hat"));

            // The saloon
            AdvancementHolder stool = task(root, "frontier/pull_up_a_stool", ModItems.BAR_STOOL.get())
                    .addCriterion("seated", StartRidingTrigger.TriggerInstance.playerStartsRiding(
                            EntityPredicate.Builder.entity().vehicle(EntityPredicate.Builder.entity().of(entities, ModEntities.SEAT.get()))))
                    .save(output, id("frontier/pull_up_a_stool"));
            Advancement.Builder drink = task(stool, "frontier/wet_your_whistle", ModItems.RYE_WHISKEY.get());
            for (var item : List.of(ModItems.FRONTIER_BEER, ModItems.SARSAPARILLA, ModItems.COWBOY_COFFEE, ModItems.RYE_WHISKEY,
                    ModItems.MEZCAL, ModItems.MOONSHINE, ModItems.SNAKE_OIL)) {
                drink.addCriterion(item.getId().getPath(), ConsumeItemTrigger.TriggerInstance.usedItem(items, item.get()));
            }
            AdvancementHolder drank = drink.requirements(AdvancementRequirements.Strategy.OR).save(output, id("frontier/wet_your_whistle"));
            display(drank, "frontier/lights_out", ModItems.MOONSHINE.get(), AdvancementType.CHALLENGE, true)
                    .addCriterion("blacked_out", ModCriteria.BLACKED_OUT.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(25).addLootTable(output.lookup(Registries.LOOT_TABLE).getOrThrow(REWARD_BLACKOUT)))
                    .save(output, id("frontier/lights_out"));
            // 21b: somewhere else in town, an hour gone, somebody else's hat
            display(drank, "frontier/missing_time_question", ModItems.COWBOY_HAT.get(), AdvancementType.GOAL, true)
                    .addCriterion("woke", ModCriteria.MISSING_TIME_DRUNK.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(25))
                    .save(output, id("frontier/missing_time_question"));
            // The card table (17)
            AdvancementHolder twentyOne = task(stool, "frontier/twenty_one", ModItems.CARD_TABLE.get())
                    .addCriterion("natural", ModCriteria.BLACKJACK_NATURAL.get().criterion())
                    .save(output, id("frontier/twenty_one"));
            display(twentyOne, "frontier/five_card_charlie", ModItems.GOLD_COIN.get(), AdvancementType.GOAL, false)
                    .addCriterion("charlie", ModCriteria.FIVE_CARD_CHARLIE.get().criterion())
                    .save(output, id("frontier/five_card_charlie"));
            display(twentyOne, "frontier/cleaned_out", ModItems.COPPER_COIN.get(), AdvancementType.TASK, true)
                    .addCriterion("cleaned_out", ModCriteria.CLEANED_OUT.get().criterion())
                    .save(output, id("frontier/cleaned_out"));

            // The law
            AdvancementHolder reputation = task(root, "frontier/reputation", ModItems.WANTED_POSTER.get())
                    .addCriterion("wanted", ModCriteria.WANTED_LEVEL.get().atLeast(1))
                    .save(output, id("frontier/reputation"));
            task(reputation, "frontier/clean_slate", ModItems.COPPER_COIN.get())
                    .addCriterion("paid", ModCriteria.WANTED_CLEARED.get().criterion())
                    .save(output, id("frontier/clean_slate"));
            AdvancementHolder mostWanted = display(reputation, "frontier/most_wanted", ModItems.REVOLVER.get(), AdvancementType.CHALLENGE, true)
                    .addCriterion("three_stars", ModCriteria.WANTED_LEVEL.get().atLeast(3))
                    .save(output, id("frontier/most_wanted"));
            display(mostWanted, "frontier/honor_among_thieves", ModItems.DYNAMITE.get(), AdvancementType.CHALLENGE, true)
                    .addCriterion("cut", ModCriteria.RAID_CUT.get().criterion())
                    .save(output, id("frontier/honor_among_thieves"));

            // Town cats
            AdvancementHolder dressed = task(root, "frontier/dressed_to_the_nines", ModItems.BANDANA.get())
                    .addCriterion("dressed", ModCriteria.CAT_DRESSED.get().criterion())
                    .save(output, id("frontier/dressed_to_the_nines"));
            display(dressed, "frontier/deputized", ModItems.BRASS_STAR.get(), AdvancementType.CHALLENGE, false)
                    .addCriterion("star", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.BRASS_STAR.get()))
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("frontier/deputized"));
            display(dressed, "frontier/nine_lives", Items.COOKED_SALMON, AdvancementType.CHALLENGE, false)
                    .addCriterion("ghost", ModCriteria.GHOST_CAT_TAMED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100).addLootTable(output.lookup(Registries.LOOT_TABLE).getOrThrow(REWARD_GHOST_CAT)))
                    .save(output, id("frontier/nine_lives"));
        }

        void steam() {
            AdvancementHolder root = Advancement.Builder.advancement()
                    .rootDisplay(ModItems.BRASS_GEAR.get(), title("steam/root"), description("steam/root"),
                            HollowFrontier.id("block/brass_block"), AdvancementType.TASK, false, false, false)
                    .addCriterion("copper_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COPPER_INGOT))
                    .addCriterion("brass_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, ModTags.Items.INGOTS_BRASS))) // any mod's brass
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .save(output, id("steam/root"));
            AdvancementHolder boiler = task(root, "steam/head_of_steam", ModItems.BOILER.get())
                    .addCriterion("boiler", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, ModBlocks.BOILER.get()))
                    .save(output, id("steam/head_of_steam"));
            AdvancementHolder canister = task(boiler, "steam/canned_steam", ModItems.STEAM_CANISTER.get())
                    .addCriterion("filled", ModCriteria.CANISTER_FILLED.get().criterion())
                    .save(output, id("steam/canned_steam"));
            Advancement.Builder dust = task(boiler, "steam/grind_it_out", ModItems.IRON_DUST.get());
            for (var item : List.of(ModItems.IRON_DUST, ModItems.GOLD_DUST, ModItems.COPPER_DUST)) {
                dust.addCriterion(item.getId().getPath(), InventoryChangeTrigger.TriggerInstance.hasItems(item.get()));
            }
            dust.requirements(AdvancementRequirements.Strategy.OR).save(output, id("steam/grind_it_out"));
            task(canister, "steam/hole_lotta_steam", ModItems.STEAM_DRILL.get())
                    .addCriterion("drill", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.STEAM_DRILL.get()))
                    .save(output, id("steam/hole_lotta_steam"));
            AdvancementHolder handcar = display(canister, "steam/handcar", ModItems.STEAM_HANDCAR.get(), AdvancementType.GOAL, false)
                    .addCriterion("handcar", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.STEAM_HANDCAR.get()))
                    .save(output, id("steam/handcar"));
            task(handcar, "steam/toot_toot", Items.GOAT_HORN)
                    .addCriterion("whistle", ModCriteria.HANDCAR_WHISTLE.get().criterion())
                    .save(output, id("steam/toot_toot"));
            task(handcar, "steam/water_stop", ModItems.STEAM_TANK.get())
                    .addCriterion("refuelled", ModCriteria.HANDCAR_REFUELLED.get().criterion())
                    .save(output, id("steam/water_stop"));
            task(handcar, "steam/laying_track", Items.RAIL)
                    // Only the engineer sells rails, so any trade that hands over rails will do.
                    .addCriterion("rails", CriteriaTriggers.TRADE.createCriterion(new TradeTrigger.TriggerInstance(Optional.empty(), Optional.empty(),
                            Optional.of(ItemPredicate.Builder.item().of(items, Items.RAIL).build()))))
                    .save(output, id("steam/laying_track"));
            display(handcar, "steam/long_haul", Items.POWERED_RAIL, AdvancementType.CHALLENGE, false)
                    .addCriterion("driven", ModCriteria.HANDCAR_DRIVEN.get().atLeast(1000))
                    .rewards(AdvancementRewards.Builder.experience(100).addLootTable(output.lookup(Registries.LOOT_TABLE).getOrThrow(REWARD_LONG_HAUL)))
                    .save(output, id("steam/long_haul"));

            // Phase 8: the airship, and the way into the Hollow Earth (phase 9 roots its own tab at hollow/root)
            AdvancementHolder airship = display(handcar, "steam/airship", ModItems.AIRSHIP.get(), AdvancementType.GOAL, false)
                    .addCriterion("airship", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.AIRSHIP.get()))
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("steam/airship"));
            display(airship, "steam/submersible", ModItems.STEAM_SUBMERSIBLE.get(), AdvancementType.GOAL, false) // 43c
                    .addCriterion("submersible", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.STEAM_SUBMERSIBLE.get()))
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("steam/submersible"));
            display(airship, "steam/above_the_clouds", ModItems.AIRSHIP_ENVELOPE.get(), AdvancementType.CHALLENGE, false)
                    .addCriterion("altitude", ModCriteria.AIRSHIP_ALTITUDE.get().atLeast(200))
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("steam/above_the_clouds"));
            AdvancementHolder bearing = task(handcar, "steam/true_north", ModItems.POLAR_COMPASS.get())
                    .addCriterion("bearing", ModCriteria.POLAR_BEARING.get().criterion())
                    .save(output, id("steam/true_north"));
            AdvancementHolder opened = task(bearing, "steam/open_sesame", ModItems.POLAR_RESONATOR.get())
                    .addCriterion("opened", ModCriteria.GATEWAY_OPENED.get().criterion())
                    .save(output, id("steam/open_sesame"));
            display(opened, "steam/hollow_earth", ModItems.GATEWAY_FRAME.get(), AdvancementType.GOAL, false)
                    .addCriterion("entered", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(HollowEarth.LEVEL))
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("steam/hollow_earth"));
        }

        // Phase 9c: the Hollow Earth's own tab: its lands, phlogiston and starsteel, and its lore sites as hidden finds
        void hollow() {
            HolderGetter<Biome> biomes = output.lookup(Registries.BIOME);
            HolderGetter<Structure> structures = output.lookup(Registries.STRUCTURE);
            AdvancementHolder root = Advancement.Builder.advancement()
                    .rootDisplay(ModItems.PHLOGISTON_SHARD.get(), title("hollow/root"), description("hollow/root"),
                            HollowFrontier.id("block/crust_rock"), AdvancementType.TASK, false, false, false)
                    .addCriterion("entered", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(HollowEarth.LEVEL))
                    .save(output, id("hollow/root"));
            Advancement.Builder everywhere = display(root, "hollow/world_within", ModItems.CRUST_ROCK.get(), AdvancementType.CHALLENGE, false)
                    .rewards(AdvancementRewards.Builder.experience(100));
            List<ItemLike> icons = List.of(ModItems.FERN_FROND.get(), Items.AMETHYST_CLUSTER, ModItems.ASH_VENT.get(), Items.CALCITE);
            List<ResourceKey<Biome>> lands = List.of(HollowBiomes.PRIMORDIAL_JUNGLE, HollowBiomes.CRYSTAL_STEPPES, HollowBiomes.EMBER_BADLANDS,
                    HollowBiomes.CONCAVIAN_HIGHLANDS);
            AdvancementHolder last = root;
            for (int i = 0; i < lands.size(); i++) {
                String name = "hollow/" + lands.get(i).identifier().getPath();
                var inLand = PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inBiome(biomes.getOrThrow(lands.get(i))));
                last = task(root, name, icons.get(i)).addCriterion("visited", inLand).save(output, id(name));
                everywhere.addCriterion(lands.get(i).identifier().getPath(),
                        PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inBiome(biomes.getOrThrow(lands.get(i)))));
            }
            everywhere.parent(last).save(output, id("hollow/world_within"));
            AdvancementHolder phlogiston = task(root, "hollow/phlogiston", ModItems.PHLOGISTON_SHARD.get())
                    .addCriterion("shard", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.PHLOGISTON_SHARD.get()))
                    .save(output, id("hollow/phlogiston"));
            task(phlogiston, "hollow/phlogiston_lamp", ModItems.PHLOGISTON_LAMP.get())
                    .addCriterion("lamp", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.PHLOGISTON_LAMP.get()))
                    .save(output, id("hollow/phlogiston_lamp"));
            AdvancementHolder ingot = task(root, "hollow/starsteel", ModItems.STARSTEEL_INGOT.get())
                    .addCriterion("ingot", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.STARSTEEL_INGOT.get()))
                    .save(output, id("hollow/starsteel"));
            display(ingot, "hollow/sun_forged", ModItems.STARSTEEL_CHESTPLATE.get(), AdvancementType.GOAL, false)
                    .addCriterion("suit", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.STARSTEEL_HELMET.get(),
                            ModItems.STARSTEEL_CHESTPLATE.get(), ModItems.STARSTEEL_LEGGINGS.get(), ModItems.STARSTEEL_BOOTS.get()))
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("hollow/sun_forged"));
            task(ingot, "hollow/fleet_of_hoof", ModItems.STARSTEEL_HORSESHOES.get())
                    .addCriterion("shod", ModCriteria.HORSE_SHOD.get().atLeast(3))
                    .save(output, id("hollow/fleet_of_hoof"));
            // The wild megafauna (10a)
            AdvancementHolder tusk = task(root, "hollow/woolly_thinking", ModItems.MAMMOTH_TUSK.get())
                    .addCriterion("tusk", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.MAMMOTH_TUSK.get()))
                    .save(output, id("hollow/woolly_thinking"));
            task(tusk, "hollow/oliphant", ModItems.OLIPHANT.get())
                    .addCriterion("oliphant", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.OLIPHANT.get()))
                    .save(output, id("hollow/oliphant"));
            AdvancementHolder bird = task(root, "hollow/dont_run", ModItems.TERROR_BIRD_TALON.get())
                    .addCriterion("terror_bird", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(entities, ModEntities.TERROR_BIRD.get())))
                    .save(output, id("hollow/dont_run"));
            AdvancementHolder scute = task(root, "hollow/shell_game", ModItems.GLYPTODON_SCUTE.get())
                    .addCriterion("scute", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.GLYPTODON_SCUTE.get()))
                    .save(output, id("hollow/shell_game"));
            task(scute, "hollow/shell_suit", ModItems.GLYPTODON_CHESTPLATE.get())
                    .addCriterion("suit", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.GLYPTODON_HELMET.get(),
                            ModItems.GLYPTODON_CHESTPLATE.get(), ModItems.GLYPTODON_LEGGINGS.get(), ModItems.GLYPTODON_BOOTS.get()))
                    .save(output, id("hollow/shell_suit"));
            Advancement.Builder bigGame = display(bird, "hollow/big_game", ModItems.THICK_HIDE.get(), AdvancementType.CHALLENGE, false)
                    .rewards(AdvancementRewards.Builder.experience(150));
            for (var beast : List.of(ModEntities.WOOLLY_MAMMOTH, ModEntities.TERROR_BIRD, ModEntities.MEGATHERIUM, ModEntities.GLYPTODON)) {
                bigGame.addCriterion(beast.getId().getPath(), KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(entities, beast.get())));
            }
            bigGame.save(output, id("hollow/big_game"));
            // 43: the Still Sea, its creatures and the Harpoon (a side road: not part of World Within or Big Game)
            AdvancementHolder sea = task(root, "hollow/still_sea", Items.KELP)
                    .addCriterion("visited", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inBiome(biomes.getOrThrow(HollowBiomes.STILL_SEA))))
                    .save(output, id("hollow/still_sea"));
            task(sea, "hollow/sea_cow", ModItems.BLUBBER.get())
                    .addCriterion("blubber", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.BLUBBER.get()))
                    .save(output, id("hollow/sea_cow"));
            task(sea, "hollow/lamp_gel", ModItems.LAMP_GEL.get())
                    .addCriterion("gel", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.LAMP_GEL.get()))
                    .save(output, id("hollow/lamp_gel"));
            task(sea, "hollow/plesiosaur_fetch", Items.COD)
                    .addCriterion("fetched", ModCriteria.PLESIOSAUR_FETCHED.get().criterion())
                    .save(output, id("hollow/plesiosaur_fetch"));
            AdvancementHolder harpoon = task(sea, "hollow/harpoon", ModItems.HARPOON.get())
                    .addCriterion("harpoon", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.HARPOON.get()))
                    .save(output, id("hollow/harpoon"));
            display(harpoon, "hollow/basilosaurus", ModItems.BASILOSAUR_TOOTH.get(), AdvancementType.GOAL, false)
                    .addCriterion("basilosaurus", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(entities, ModEntities.BASILOSAURUS.get())))
                    .save(output, id("hollow/basilosaurus"));
            // 43d: the sea's sites, and what the Quarter's vault holds
            found(sea, "hollow/symmes_landing", Items.SPRUCE_BOAT, structures.getOrThrow(ModStructures.SYMMES_LANDING));
            found(sea, "hollow/drowned_quarter", ModItems.JELLY_LAMP.get(), structures.getOrThrow(ModStructures.DROWNED_QUARTER));
            display(sea, "hollow/sealed_hull", ModItems.SEALED_HULL.get(), AdvancementType.CHALLENGE, true)
                    .addCriterion("hull", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SEALED_HULL.get()))
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("hollow/sealed_hull"));
            // The Gray experiments (10b)
            found(root, "hollow/still_running", Items.SEA_LANTERN, structures.getOrThrow(ModStructures.GRAY_STATION));
            AdvancementHolder specimen = task(bird, "hollow/escaped_specimen", ModItems.GRAY_TECH_FRAGMENT.get())
                    .addCriterion("specimen", ModCriteria.SPECIMEN_KILLED.get().criterion())
                    .save(output, id("hollow/escaped_specimen"));
            task(specimen, "hollow/on_the_signal", ModItems.SIGNAL_LOCATOR.get())
                    .addCriterion("signal", ModCriteria.SIGNAL_FOUND.get().criterion())
                    .save(output, id("hollow/on_the_signal"));
            // The captives (20)
            AdvancementHolder jailbreak = task(specimen, "hollow/jailbreak", Items.IRON_BARS)
                    .addCriterion("freed", ModCriteria.CAPTIVE_FREED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("hollow/jailbreak"));
            display(jailbreak, "hollow/not_on_my_watch", ModItems.IMPLANT_RIG.get(), AdvancementType.GOAL, false)
                    .addCriterion("stopped", ModCriteria.PROCEDURE_STOPPED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("hollow/not_on_my_watch"));
            task(jailbreak, "hollow/special_delivery", ModItems.LETTER_HOME.get())
                    .addCriterion("delivered", ModCriteria.LETTER_DELIVERED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("hollow/special_delivery"));
            // The lore sites (9b3), hidden until found
            found(root, "hollow/older_than_babel", Items.LODESTONE, structures.getOrThrow(ModStructures.KINDLED_SITE));
            found(root, "hollow/under_observation", Items.SKELETON_SKULL, structures.getOrThrow(ModStructures.GRAY_LAB));
            found(root, "hollow/lost_and_found", Items.WRITABLE_BOOK, structures.getOrThrow(ModStructures.SYMMES_CAMP));
        }

        // Phase 12a: Concavia's own tab, rooted at finding the crystal ruins: the Kindled, their story and their trust
        void concavia() {
            HolderGetter<Structure> structures = output.lookup(Registries.STRUCTURE);
            AdvancementHolder root = Advancement.Builder.advancement()
                    .rootDisplay(ModItems.CONCAVIAN_CRYSTAL.get(), title("concavia/root"), description("concavia/root"),
                            HollowFrontier.id("block/crystal_bricks"), AdvancementType.TASK, false, false, false)
                    .addCriterion("found", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structures.getOrThrow(ModStructures.CRYSTAL_RUINS))))
                    .save(output, id("concavia/root"));
            task(root, "concavia/crystal_clear", ModItems.CONCAVIAN_CRYSTAL.get())
                    .addCriterion("crystal", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.CONCAVIAN_CRYSTAL.get()))
                    .save(output, id("concavia/crystal_clear"));
            AdvancementHolder met = task(root, "concavia/well_met", ModItems.KINDLED_SPAWN_EGG.get())
                    .addCriterion("met", ModCriteria.MET_KINDLED.get().criterion())
                    .save(output, id("concavia/well_met"));
            AdvancementHolder ally = display(met, "concavia/an_ally_below", ModItems.KINDLED_SIGIL.get(), AdvancementType.GOAL, false)
                    .addCriterion("ally", ModCriteria.KINDLED_ALLY.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("concavia/an_ally_below"));
            display(ally, "concavia/the_truth", Items.WRITTEN_BOOK, AdvancementType.GOAL, false)
                    .addCriterion("story", ModCriteria.KINDLED_STORY.get().atLeast(Kindled.LINES))
                    .save(output, id("concavia/the_truth"));
            // 15c: the ruins restored, and their waystones
            AdvancementHolder rising = display(ally, "concavia/concavia_rising", ModItems.PHLOGISTON_LAMP.get(), AdvancementType.CHALLENGE, false)
                    .addCriterion("restored", ModCriteria.RUIN_RESTORED.get().atLeast(ConcaviaBlueprints.STAGES))
                    .rewards(AdvancementRewards.Builder.experience(500))
                    .save(output, id("concavia/concavia_rising"));
            task(rising, "concavia/old_roads", ModItems.WAYSTONE.get())
                    .addCriterion("travelled", ModCriteria.WAYSTONE_TRAVELLED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("concavia/old_roads"));
            task(root, "concavia/glass_jaw", ModItems.CHISELED_CRYSTAL_BRICKS.get())
                    .addCriterion("guardian", ModCriteria.GUARDIAN_KILLED.get().criterion())
                    .save(output, id("concavia/glass_jaw"));
            // 12b: the Codex, the temple, the Archon and what it leaves
            task(root, "concavia/required_reading", ModItems.CONCAVIAN_CODEX.get())
                    .addCriterion("read", ModCriteria.CODEX_READ.get().criterion())
                    .save(output, id("concavia/required_reading"));
            AdvancementHolder temple = display(ally, "concavia/temple_of_the_sun", ModItems.SUN_PYLON.get(), AdvancementType.GOAL, false)
                    .addCriterion("found", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structures.getOrThrow(ModStructures.SUN_TEMPLE))))
                    .save(output, id("concavia/temple_of_the_sun"));
            AdvancementHolder eclipse = display(temple, "concavia/eclipse", ModItems.SUNSTONE_SHARD.get(), AdvancementType.CHALLENGE, false)
                    .addCriterion("archon", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(entities, ModEntities.SUN_ARCHON.get())))
                    .rewards(AdvancementRewards.Builder.experience(250))
                    .save(output, id("concavia/eclipse"));
            task(eclipse, "concavia/heart_of_the_sun", ModItems.PHLOGISTON_CORE.get())
                    .addCriterion("core", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.PHLOGISTON_CORE.get()))
                    .save(output, id("concavia/heart_of_the_sun"));
            task(eclipse, "concavia/uneasy_lies_the_head", ModItems.STARSTEEL_CROWN.get())
                    .addCriterion("crowned", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.STARSTEEL_CROWN.get()))
                    .save(output, id("concavia/uneasy_lies_the_head"));
            task(eclipse, "concavia/let_there_be_light", ModItems.SUNSTONE_LENS.get())
                    .addCriterion("beam", ModCriteria.SUNBEAM_HIT.get().criterion())
                    .save(output, id("concavia/let_there_be_light"));
        }

        // Phase 13a: the Grays' own tab, rooted at seeing one of their saucers
        void ufo() {
            HolderGetter<Structure> structures = output.lookup(Registries.STRUCTURE);
            AdvancementHolder root = Advancement.Builder.advancement()
                    .rootDisplay(ModItems.BEAM_EMITTER.get(), title("ufo/root"), description("ufo/root"),
                            HollowFrontier.id("block/saucer_hull"), AdvancementType.TASK, false, false, false)
                    .addCriterion("seen", ModCriteria.SAUCER_SEEN.get().criterion())
                    .save(output, id("ufo/root"));
            display(root, "ufo/missing_time", Items.ENDER_EYE, AdvancementType.GOAL, true)
                    .addCriterion("taken", ModCriteria.ABDUCTED.get().criterion())
                    .save(output, id("ufo/missing_time"));
            task(root, "ufo/not_today", Items.LEATHER_BOOTS)
                    .addCriterion("escaped", ModCriteria.BEAM_ESCAPED.get().criterion())
                    .save(output, id("ufo/not_today"));
            AdvancementHolder crash = display(root, "ufo/crash_and_burn", ModItems.SAUCER_HULL.get(), AdvancementType.GOAL, false)
                    .addCriterion("surface", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structures.getOrThrow(ModStructures.CRASH_SITE))))
                    .addCriterion("hollow", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structures.getOrThrow(ModStructures.HOLLOW_CRASH_SITE))))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("ufo/crash_and_burn"));
            Advancement.Builder salvage = task(crash, "ufo/salvage_rights", ModItems.ALIEN_ALLOY_PLATING.get());
            for (var part : List.of(ModItems.ALIEN_ALLOY_PLATING, ModItems.DAMAGED_GRAVITY_DRIVE, ModItems.BEAM_EMITTER, ModItems.LENS_ARRAY, ModItems.NAVIGATION_CORE)) {
                salvage.addCriterion(part.getId().getPath(), InventoryChangeTrigger.TriggerInstance.hasItems(part.get()));
            }
            AdvancementHolder salvaged = salvage.requirements(AdvancementRequirements.Strategy.OR).save(output, id("ufo/salvage_rights"));
            display(salvaged, "ufo/some_assembly_required", ModItems.NAVIGATION_CORE.get(), AdvancementType.CHALLENGE, false)
                    .addCriterion("all", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.ALIEN_ALLOY_PLATING.get(), ModItems.DAMAGED_GRAVITY_DRIVE.get(),
                            ModItems.BEAM_EMITTER.get(), ModItems.LENS_ARRAY.get(), ModItems.NAVIGATION_CORE.get()))
                    .rewards(AdvancementRewards.Builder.experience(150))
                    .save(output, id("ufo/some_assembly_required"));
            task(root, "ufo/take_me_to_your_leader", ModItems.GRAY_TECH_FRAGMENT.get())
                    .addCriterion("warden", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(entities, ModEntities.GRAY_WARDEN.get())))
                    .save(output, id("ufo/take_me_to_your_leader"));
            // 13b: phlogiston tech
            AdvancementHolder cell = task(root, "ufo/lightning_in_a_bottle", ModItems.PHLOGISTON_CELL.get())
                    .addCriterion("cell", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.PHLOGISTON_CELL.get()))
                    .save(output, id("ufo/lightning_in_a_bottle"));
            task(cell, "ufo/death_ray", ModItems.RAY_PISTOL.get())
                    .addCriterion("pistol", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.RAY_PISTOL.get()))
                    .addCriterion("rifle", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.PHLOGISTON_RIFLE.get()))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .save(output, id("ufo/death_ray"));
            AdvancementHolder drive = display(salvaged, "ufo/defying_gravity", ModItems.GRAVITY_DRIVE.get(), AdvancementType.GOAL, false)
                    .addCriterion("drive", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.GRAVITY_DRIVE.get()))
                    .save(output, id("ufo/defying_gravity"));
            // 13c: the Prototype Saucer
            AdvancementHolder liftoff = display(drive, "ufo/we_have_liftoff", ModItems.PROTOTYPE_SAUCER.get(), AdvancementType.GOAL, false)
                    .addCriterion("launched", ModCriteria.SAUCER_LAUNCHED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("ufo/we_have_liftoff"));
            task(liftoff, "ufo/beam_me_up", ModItems.BEAM_EMITTER.get())
                    .addCriterion("beamed", ModCriteria.SAUCER_BEAMED.get().criterion())
                    .save(output, id("ufo/beam_me_up"));
            task(liftoff, "ufo/high_flyer", Items.PHANTOM_MEMBRANE)
                    .addCriterion("high", ModCriteria.SAUCER_FLOWN.get().atLeast(192))
                    .save(output, id("ufo/high_flyer"));
            // 13d: the Mothership, and the end of the story
            AdvancementHolder called = display(liftoff, "ufo/calling_them_down", ModItems.PHLOGISTON_BEACON.get(), AdvancementType.GOAL, false)
                    .addCriterion("summoned", ModCriteria.MOTHERSHIP_SUMMONED.get().criterion())
                    .save(output, id("ufo/calling_them_down"));
            task(called, "ufo/dogfight", ModItems.LENS_ARRAY.get())
                    .addCriterion("drone", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(entities, ModEntities.GRAY_DRONE.get())))
                    .save(output, id("ufo/dogfight"));
            AdvancementHolder fallen = display(called, "ufo/the_mothership_falls", ModItems.ALIEN_ALLOY_PLATING.get(), AdvancementType.CHALLENGE, false)
                    .addCriterion("mothership", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(entities, ModEntities.MOTHERSHIP.get())))
                    .rewards(AdvancementRewards.Builder.experience(500))
                    .save(output, id("ufo/the_mothership_falls"));
            AdvancementHolder loose = display(fallen, "ufo/cut_loose", ModItems.KINDLED_SIGIL.get(), AdvancementType.GOAL, false)
                    .addCriterion("broken", ModCriteria.LEASH_BROKEN.get().criterion())
                    .save(output, id("ufo/cut_loose"));
            // 15a: the Grays fall back
            task(loose, "ufo/trespassers", ModItems.GRAY_CACHE.get())
                    .addCriterion("cleared", ModCriteria.LANDING_CLEARED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("ufo/trespassers"));
            // 21c: the Grays left behind, trying to pass
            task(loose, "ufo/not_from_around_here", ModItems.SUNSTONE_LENS.get())
                    .addCriterion("exposed", ModCriteria.GRAY_EXPOSED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id("ufo/not_from_around_here"));
            display(loose, "ufo/regular_customer", ModItems.SARSAPARILLA.get(), AdvancementType.GOAL, false)
                    .addCriterion("regular", ModCriteria.REGULAR_CUSTOMER.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("ufo/regular_customer"));
            // 15c: a freed man given a new life
            task(loose, "ufo/second_chance", ModItems.GOLD_COIN.get())
                    .addCriterion("settled", ModCriteria.SECOND_CHANCE.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(100))
                    .save(output, id("ufo/second_chance"));
            // 15b: the Mothership's return, and the fittings only it gives up
            AdvancementHolder encore = display(fallen, "ufo/encore", ModItems.CLOAKING_DEVICE.get(), AdvancementType.GOAL, false)
                    .addCriterion("rematch", ModCriteria.MOTHERSHIP_REMATCH.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(250))
                    .save(output, id("ufo/encore"));
            display(encore, "ufo/fully_fitted", ModItems.CARGO_HOLD.get(), AdvancementType.CHALLENGE, false)
                    .addCriterion("fitted", ModCriteria.SAUCER_FULLY_FITTED.get().criterion())
                    .rewards(AdvancementRewards.Builder.experience(500))
                    .save(output, id("ufo/fully_fitted"));
            // 14: the capstone, one of each of the story's three bosses
            Advancement.Builder legend = display(fallen, "ufo/legend_of_the_frontier", ModItems.COWBOY_HAT.get(), AdvancementType.CHALLENGE, false)
                    .rewards(AdvancementRewards.Builder.experience(1000));
            for (var boss : List.of(ModEntities.OUTLAW_BOSS, ModEntities.SUN_ARCHON, ModEntities.MOTHERSHIP)) {
                legend.addCriterion(boss.getId().getPath(), KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(entities, boss.get())));
            }
            legend.save(output, id("ufo/legend_of_the_frontier"));
        }

        /** A hidden goal for setting foot in a structure. */
        private void found(AdvancementHolder parent, String name, ItemLike icon, Holder<Structure> structure) {
            display(parent, name, icon, AdvancementType.GOAL, true)
                    .addCriterion("found", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structure)))
                    .rewards(AdvancementRewards.Builder.experience(50))
                    .save(output, id(name));
        }

        private Advancement.Builder task(AdvancementHolder parent, String name, ItemLike icon) {
            return display(parent, name, icon, AdvancementType.TASK, false);
        }

        private Advancement.Builder display(AdvancementHolder parent, String name, ItemLike icon, AdvancementType type, boolean hidden) {
            return Advancement.Builder.advancement().parent(parent)
                    .display(icon.asItem(), title(name), description(name), type, true, type != AdvancementType.TASK, hidden);
        }
    }

    private static String id(String name) {
        return HollowFrontier.id(name).toString();
    }

    private static Component title(String name) {
        return Component.translatable(key(name) + ".title");
    }

    private static Component description(String name) {
        return Component.translatable(key(name) + ".description");
    }

    public static String key(String name) {
        return "advancements." + HollowFrontier.MODID + "." + name.replace('/', '.');
    }
}
