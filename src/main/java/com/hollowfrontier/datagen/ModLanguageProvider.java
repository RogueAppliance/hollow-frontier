package com.hollowfrontier.datagen;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gazette.GazetteBackPage;
import com.hollowfrontier.content.frontier.talk.ChatterLines;
import com.hollowfrontier.content.ufo.FieldNotes;
import com.hollowfrontier.worldgen.structure.TownLandmarks;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTabs;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, HollowFrontier.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        ModAdvancements.TEXT.forEach((name, text) -> {
            add(ModAdvancements.key(name) + ".title", text[0]);
            add(ModAdvancements.key(name) + ".description", text[1]);
        });
        // 19: everything the frontier says (Chatter), from the one table that also says when
        ChatterLines.lines().forEach(line -> add(line.key(), line.text()));
        ChatterLines.exchanges().forEach(exchange -> {
            for (int i = 0; i < exchange.texts().size(); i++) {
                add(exchange.key(i), exchange.texts().get(i));
            }
        });
        // 21a: the Gazette's back page, the Gray field notes and Boot Hill's epitaphs, each from its own table
        for (GazetteBackPage.Kind kind : GazetteBackPage.Kind.values()) {
            add(kind.titleKey(), kind.title);
        }
        GazetteBackPage.pieces().forEach(piece -> add(piece.key(), piece.text()));
        add(FieldNotes.titleKey(), FieldNotes.TITLE_PAGE);
        for (int i = 0; i < FieldNotes.ENTRIES.size(); i++) {
            add(FieldNotes.entryKey(i), FieldNotes.ENTRIES.get(i));
        }
        for (int n = 0; n < TownLandmarks.EPITAPHS.size(); n++) {
            for (int line = 0; line < 4; line++) {
                add("sign.hollowfrontier.epitaph." + (n + 1) + "." + (line + 1), TownLandmarks.EPITAPHS.get(n).get(line));
            }
        }
        add(ModTabs.titleKey("hollow_frontier"), "Hollow Frontier");

        addBlock(ModBlocks.BRASS_BLOCK, "Block of Brass");

        addItem(ModItems.BRASS_INGOT, "Brass Ingot");
        addItem(ModItems.BRASS_NUGGET, "Brass Nugget");
        addItem(ModItems.BRASS_PLATE, "Brass Plate");
        addItem(ModItems.BRASS_GEAR, "Brass Gear");
        addItem(ModItems.PRESSURE_VALVE, "Pressure Valve");

        addItem(ModItems.PERCUSSION_CAP, "Percussion Caps");
        addItem(ModItems.REVOLVER_CARTRIDGE, "Paper Revolver Cartridge");
        addItem(ModItems.RIFLE_CARTRIDGE, "Paper Rifle Cartridge");
        addItem(ModItems.BUCKSHOT_CARTRIDGE, "Paper Buckshot Cartridge");
        addItem(ModItems.REVOLVER, "Cap & Ball Revolver");
        addItem(ModItems.PLAINS_RIFLE, "Plains Rifle");
        addItem(ModItems.SHOTGUN, "Double-Barrel Muzzleloader");
        add("tooltip.hollowfrontier.ammo", "Loaded: %s/%s");
        add("tooltip.hollowfrontier.uses_ammo", "Loads with: %s + %s");
        add("tooltip.hollowfrontier.reload_hint", "Reload key or sneak + use loads one chamber at a time");
        add("message.hollowfrontier.loaded", "Loaded %s/%s");
        add("message.hollowfrontier.misfire", "Misfire! Wet powder.");
        // Gun enchantments (22); the .desc lines are what enchantment-description mods show
        enchantment("rifling", "Rifling", "Grooves cut down the barrel: each shot hits harder.");
        enchantment("quick_load", "Quick Load", "Each chamber or barrel loads faster.");
        enchantment("deadeye", "Deadeye", "Shots to the head do even more damage.");
        enchantment("choke", "Choke", "A narrower muzzle keeps the buckshot together.");
        enchantment("dry_powder", "Dry Powder", "Rain can't make the gun misfire.");
        enchantment("frugal", "Frugal", "Loading sometimes costs no cartridge or cap.");

        addItem(ModItems.COWBOY_HAT, "Cowboy Hat");
        addItem(ModItems.DUSTER, "Duster Coat");
        addItem(ModItems.CHAPS, "Leather Chaps");
        addItem(ModItems.COWBOY_BOOTS, "Cowboy Boots");
        addItem(ModItems.LASSO, "Lasso");
        addEntityType(ModEntities.LASSO, "Lasso");
        addItem(ModItems.IRON_HORSESHOES, "Iron Horseshoes");
        addItem(ModItems.BRASS_HORSESHOES, "Brass Horseshoes");
        addItem(ModItems.SADDLEBAGS, "Saddlebags");
        add("tooltip.hollowfrontier.horseshoes", "+%s%% horse speed, higher jumps");
        add("tooltip.hollowfrontier.horse_gear_hint", "Use on a tamed horse. Sneak + shears removes gear.");
        add("container.hollowfrontier.saddlebags", "Saddlebags");

        addBlock(ModBlocks.BOUNTY_BOARD, "Small Bounty Board");
        addItem(ModItems.HANGING_BOUNTY_BOARD, "Bounty Board");
        addEntityType(ModEntities.HANGING_BOUNTY_BOARD, "Bounty Board");
        addBlock(ModBlocks.BAR_COUNTER, "Bar Counter");
        addBlock(ModBlocks.BAR_STOOL, "Bar Stool");
        addBlock(ModBlocks.SALOON_DOORS, "Saloon Doors");
        addItem(ModItems.COPPER_COIN, "Copper Coin");
        addItem(ModItems.GOLD_COIN, "Gold Coin");
        addItem(ModItems.WANTED_POSTER, "Wanted Poster");
        addItem(ModItems.OUTLAW_BADGE, "Outlaw Badge");
        addItem(ModItems.OUTLAW_SPAWN_EGG, "Outlaw Spawn Egg");
        addItem(ModItems.OUTLAW_GUNSLINGER_SPAWN_EGG, "Outlaw Gunslinger Spawn Egg");
        addItem(ModItems.OUTLAW_BOSS_SPAWN_EGG, "Outlaw Boss Spawn Egg");
        addItem(ModItems.TOWNSFOLK_SPAWN_EGG, "Townsfolk Spawn Egg");
        addItem(ModItems.BARMAID_SPAWN_EGG, "Barmaid Spawn Egg");
        addItem(ModItems.LAWMAN_SPAWN_EGG, "Lawman Spawn Egg");
        addItem(ModItems.FRONTIER_BEER, "Frontier Beer");
        addItem(ModItems.SARSAPARILLA, "Sarsaparilla");
        addItem(ModItems.COWBOY_COFFEE, "Cowboy Coffee");
        addItem(ModItems.RYE_WHISKEY, "Rye Whiskey");
        addItem(ModItems.MEZCAL, "Mezcal");
        addItem(ModItems.MOONSHINE, "Moonshine");
        addItem(ModItems.SNAKE_OIL, "Doc's Snake Oil");
        add("effect.hollowfrontier.tipsy", "Tipsy");
        add("tooltip.hollowfrontier.drink.tipsy", "+%s Tipsy (spoils your aim)");
        add("tooltip.hollowfrontier.drink.sober", "Sobers you up by %s level");
        add("tooltip.hollowfrontier.drink.sober_all", "Sobers you right up");
        add("tooltip.hollowfrontier.drink.gamble", "Cures what ails you. Probably.");
        add("message.hollowfrontier.blackout", "You black out...");
        // 21b: Doc's promises (one per outcome, in Drink.GAMBLE's order, none of them true), and the rest of a saloon night
        add("message.hollowfrontier.snake_oil", "Doc's Tonic: \"%s\"");
        String[] claims = {
                "Guaranteed to cure baldness, or your money back!",
                "Also removes rust from tools, and grudges from in-laws!",
                "Settles the nerves! Slows the blood to a gentle trot!",
                "Shrinks the liver to a manageable size!",
                "Proven against snakebite, gout and bad cards!",
                "Tastes of nothing but good health!",
                "Puts a spring in your step!",
                "Keeps both feet firmly on the ground! Endorsed by gentlemen from very far away."};
        for (int i = 0; i < claims.length; i++) {
            add("message.hollowfrontier.snake_oil." + i, claims[i]);
        }
        add("message.hollowfrontier.missing_time.bed", "You come to in somebody's bed. It's an hour later than it was, and the hat on your head isn't yours.");
        add("message.hollowfrontier.missing_time.hay", "You come to face down in a hay bale. An hour's gone somewhere, and so has your hat. Somebody left you theirs.");
        add("message.hollowfrontier.missing_time.trough", "You come to sitting in a horse trough. You've lost an hour, and somebody's put a hat on you.");
        add("item.hollowfrontier.worn_cowboy_hat", "Worn Cowboy Hat");
        add("lore.hollowfrontier.worn_cowboy_hat", "Not yours. Fits, though.");
        add("message.hollowfrontier.brawl.thrown_out", "%s throws %s out through the doors.");
        addEntityType(ModEntities.OUTLAW, "Outlaw");
        addEntityType(ModEntities.OUTLAW_GUNSLINGER, "Outlaw Gunslinger");
        addEntityType(ModEntities.OUTLAW_BOSS, "Outlaw Boss");
        addEntityType(ModEntities.TOWNSFOLK, "Townsfolk");
        addEntityType(ModEntities.BARMAID, "Barmaid");
        addEntityType(ModEntities.LAWMAN, "Lawman");
        addEntityType(ModEntities.SEAT, "Seat");
        // NPC chat lines: "<name> line"
        add("message.hollowfrontier.npc.say", "<%s> %s");
        add("entity.hollowfrontier.townsfolk.shopkeeper", "Shopkeeper");
        add("entity.hollowfrontier.townsfolk.banker", "Banker");
        add("entity.hollowfrontier.townsfolk.blacksmith", "Blacksmith");
        add("entity.hollowfrontier.townsfolk.wrangler", "Wrangler");
        add("entity.hollowfrontier.townsfolk.prospector", "Prospector");
        add("entity.hollowfrontier.townsfolk.engineer", "Railroad Engineer");
        add("entity.hollowfrontier.townsfolk.doctor", "Doctor");
        add("entity.hollowfrontier.townsfolk.teacher", "Schoolteacher");
        add("entity.hollowfrontier.townsfolk.preacher", "Preacher");
        add("entity.hollowfrontier.townsfolk.postmaster", "Postmaster");
        add("entity.hollowfrontier.townsfolk.gunsmith", "Gunsmith");
        add("entity.hollowfrontier.townsfolk.editor", "Editor");
        add("entity.hollowfrontier.lawman.sheriff", "Sheriff");
        add("entity.hollowfrontier.lawman.deputy", "Deputy");
        add("entity.hollowfrontier.lawman.marshal", "U.S. Marshal");
        add("entity.hollowfrontier.lawman.ranger", "Ranger");
        // Town cats
        add("message.hollowfrontier.sheriff_cat.loyal", "The sheriff's cat only has eyes for the Sheriff.");
        add("message.hollowfrontier.ghost_cat.no_music", "The ghost cat flickers, unmoved. Perhaps with a little music...");
        add("message.hollowfrontier.ghost_cat.raw", "The ghost cat turns up its nose. It wants its fish cooked.");
        add("message.hollowfrontier.ghost_cat.no_dress", "It slips right through the knot.");
        add("message.hollowfrontier.townsfolk.refuse", "We don't serve your kind here. Git!");

        // Wanted level
        add("hud.hollowfrontier.wanted", "WANTED");
        add("message.hollowfrontier.wanted.1", "The law has its eye on you.");
        add("message.hollowfrontier.wanted.2", "You're WANTED! Lawmen will shoot on sight.");
        add("message.hollowfrontier.wanted.3", "Dead or alive! There's a price on your head.");
        add("message.hollowfrontier.wanted.clear", "Your name is clear.");
        add("message.hollowfrontier.wanted.posse", "A posse is riding out after you!");
        add("message.hollowfrontier.wanted.fine", "Your fine is %s copper coins (gold coins count as 9).");
        add("message.hollowfrontier.wanted.paid", "Paid a %s copper coin fine. Your name is clear.");

        // Town raids
        add("raid.hollowfrontier.gang", "the %s Gang");
        add("raid.hollowfrontier.townsfolk", "Grateful townsfolk");
        add("raid.hollowfrontier.from.north", "the north");
        add("raid.hollowfrontier.from.south", "the south");
        add("raid.hollowfrontier.from.east", "the east");
        add("raid.hollowfrontier.from.west", "the west");
        add("entity.hollowfrontier.outlaw.safecracker", "Safecracker");
        add("entity.hollowfrontier.outlaw.dynamiter", "Dynamiter");
        add("entity.hollowfrontier.outlaw.bagman", "Bagman");
        add("title.hollowfrontier.raid.warning", "OUTLAWS!");
        add("title.hollowfrontier.raid.warning.sub", "Riders coming: %s");
        add("title.hollowfrontier.raid.leader_down", "Town saved!");
        add("title.hollowfrontier.raid.leader_down.sub", "%2$s is dead and %1$s breaks");
        add("title.hollowfrontier.raid.broke", "Town saved!");
        add("title.hollowfrontier.raid.broke.sub", "%s turns tail and runs");
        add("title.hollowfrontier.raid.robbed", "The vault is open!");
        add("title.hollowfrontier.raid.robbed.sub", "Stop the bagman before he rides out!");
        add("title.hollowfrontier.raid.recovered", "Loot recovered!");
        add("title.hollowfrontier.raid.recovered.sub", "The bagman is down. The money goes back in the vault.");
        add("title.hollowfrontier.raid.gave_up", "They're giving up!");
        add("title.hollowfrontier.raid.gave_up.sub", "%s rides off empty-handed");
        add("bossbar.hollowfrontier.raid.warning", "Riders coming: %s");
        add("bossbar.hollowfrontier.raid.gang", "Raid: %s (%s left)");
        add("bossbar.hollowfrontier.raid.vault", "%s is cracking the vault!");
        add("bossbar.hollowfrontier.raid.retreat", "%s is on the run");
        add("bossbar.hollowfrontier.raid.getaway", "%s is getting away with the loot!");
        add("message.hollowfrontier.raid.ride_in", "Here they come: %s, riding in from %s!");
        add("message.hollowfrontier.raid.partner", "The bell's ringing for outlaws, and you're one of them. Keep out of the gang's way and there may be a cut in it for you.");
        add("message.hollowfrontier.raid.truce", "The law has bigger problems tonight. Help defend the town and your name may come clean.");
        add("message.hollowfrontier.raid.leader.arrive", "This here's a robbery! Nobody plays the hero, nobody gets hurt!");
        add("message.hollowfrontier.raid.leader.replace", "Somebody get on that safe!");
        add("message.hollowfrontier.raid.leader.cracked", "She's open! Grab the loot and ride!");
        add("message.hollowfrontier.raid.leader.break", "Fall back! Ride, boys, ride!");
        add("message.hollowfrontier.raid.leader.give_up", "Dawn's coming. We're done here, boys!");
        add("message.hollowfrontier.raid.leader.cut", "Here's your cut, partner. Don't spend it all in one saloon.");
        add("message.hollowfrontier.raid.reward", "Much obliged, partner. Here's %s gold and %s copper for your trouble.");
        add("message.hollowfrontier.raid.pardon", "The sheriff tears up a few of your warrants.");
        add("message.hollowfrontier.raid.leader_down", "%s is down!");
        add("message.hollowfrontier.raid.safecracker_down", "The safecracker's down! The vault holds.");
        add("message.hollowfrontier.raid.over.defended", "The dust settles. %s won't be back in a hurry.");
        add("message.hollowfrontier.raid.over.held", "The town's lawmen saw off %s on their own.");
        add("message.hollowfrontier.raid.over.recovered", "%s rode off with nothing but lead: the loot is back in the vault.");
        add("message.hollowfrontier.raid.over.robbed", "%s got away with the loot.");
        add("message.hollowfrontier.raid.over.gave_up", "%s rode off into the dark.");
        add("message.hollowfrontier.raid.poster", "A Wanted Poster for %s is going up on the town's Bounty Board.");
        // The bank (16)
        add("container.hollowfrontier.bank_box", "Safe Deposit Box");
        add("message.hollowfrontier.bank.no_bank", "Bank? Out here? Find me a town with a vault and we'll talk.");
        add("message.hollowfrontier.bank.fee", "A box is one gold coin to open, partner. Free to use after that.");
        add("message.hollowfrontier.bank.opened", "One gold coin, thank you kindly. That box is yours, and nobody else's.");
        add("message.hollowfrontier.bank.owed", "The bank put back %s gold and %s copper it owed you.");
        add("message.hollowfrontier.bank.interest", "Your box earned %s copper in interest over %s day(s).");
        add("message.hollowfrontier.bank.notice.lost", "The %1$s Gang cracked the bank in %6$s near X %2$s, Z %3$s and got away with %4$s gold and %5$s copper from your box.");
        add("message.hollowfrontier.bank.notice.returned", "The %1$s Gang cracked the bank in %6$s near X %2$s, Z %3$s and took %4$s gold and %5$s copper from your box, but the bagman never made it out. Every coin is back.");
        add("gazette.hollowfrontier.raid.bank_lost", "The bank reports %s gold and %s copper gone from its depositors' boxes.");
        add("message.hollowfrontier.bounty.raider", "The sheriff hands you a fresh poster: %s, who rode on this town.");
        add("commands.hollowfrontier.raid.no_town", "You're not in a frontier town.");
        add("commands.hollowfrontier.raid.already", "This town already has a raid on.");
        add("commands.hollowfrontier.raid.started", "Raid started: %s, %s strong, going for %s.");
        add("commands.hollowfrontier.raid.bank", "the bank vault");
        add("commands.hollowfrontier.raid.strongbox", "the sheriff's strongbox");
        add("commands.hollowfrontier.raid.no_vault", "nothing in particular (no vault in town)");
        add("commands.hollowfrontier.raid.none", "No raid going on here.");
        add("commands.hollowfrontier.raid.stopped", "Raid called off.");
        add("commands.hollowfrontier.raid.status.town", "Town of %s pieces. Next raid possible in %ss. Robbed for %ss more. Poster waiting: %s.");
        add("commands.hollowfrontier.raid.status.no_poster", "none");
        add("commands.hollowfrontier.raid.status.quiet", "No raid going on.");
        add("commands.hollowfrontier.raid.status.raid", "%s, led by %s: %s, %s of %s riders left, vault %s/%s, leader at %s.");

        // Dynamite, rattlesnakes
        addItem(ModItems.DYNAMITE, "Dynamite");
        addEntityType(ModEntities.DYNAMITE, "Dynamite");
        addEntityType(ModEntities.RATTLESNAKE, "Rattlesnake");
        addItem(ModItems.RATTLESNAKE_SPAWN_EGG, "Rattlesnake Spawn Egg");
        addItem(ModItems.SNAKESKIN, "Snakeskin");
        addItem(ModItems.BANDANA, "Bandana");
        addItem(ModItems.BRASS_STAR, "Brass Star");
        add("tooltip.hollowfrontier.bandana", "Tie it round your cat's neck (dyeable)");
        add("tooltip.hollowfrontier.brass_star", "Your cat hisses at outlaws and wanted folk, and shows them up");
        add("tooltip.hollowfrontier.cat_accessory", "Use on your cat; sneak-use with an empty hand to take it back");
        addItem(ModItems.SNAKESKIN_BOOTS, "Snakeskin Boots");
        add("tooltip.hollowfrontier.snakeskin_boots.1", "Rattlesnakes won't strike unless provoked");
        add("tooltip.hollowfrontier.snakeskin_boots.2", "Immune to snakebite poison");
        add("tooltip.hollowfrontier.snakeskin_boots.3", "Faster on sand; counts as Cowboy Boots");

        // The card table (17)
        addBlock(ModBlocks.CARD_TABLE, "Card Table");
        addBlock(ModBlocks.OAK_CHAIR, "Oak Chair");
        addBlock(ModBlocks.SPRUCE_CHAIR, "Spruce Chair");
        addBlock(ModBlocks.DARK_OAK_CHAIR, "Dark Oak Chair");
        addBlock(ModBlocks.OAK_TABLE, "Oak Table");
        addBlock(ModBlocks.SPRUCE_TABLE, "Spruce Table");
        addBlock(ModBlocks.DARK_OAK_TABLE, "Dark Oak Table");
        addBlock(ModBlocks.TOWN_LAMP, "Town Lamp");
        add("container.hollowfrontier.blackjack", "Blackjack, dealt by %s");
        add("message.hollowfrontier.card_table.no_dealer", "Nobody's dealing. A barmaid has to be near to run the table.");
        add("gui.hollowfrontier.blackjack.dealer", "Dealer");
        add("gui.hollowfrontier.blackjack.you", "You");
        add("gui.hollowfrontier.blackjack.purse", "Purse: %s c");
        add("gui.hollowfrontier.blackjack.bet", "Bet: %s c");
        add("gui.hollowfrontier.blackjack.deal", "Deal");
        add("gui.hollowfrontier.blackjack.hit", "Hit");
        add("gui.hollowfrontier.blackjack.stand", "Stand");
        add("gui.hollowfrontier.blackjack.double", "Double");
        add("gui.hollowfrontier.blackjack.soft", "soft %s");
        add("gui.hollowfrontier.blackjack.shows", "shows %s");
        add("gui.hollowfrontier.blackjack.rules", "Dealer stands on 17. Blackjack pays 3:2. Limit %s c.");
        add("gui.hollowfrontier.blackjack.rules_sharp", "Dealer hits soft 17. Blackjack pays 3:2. Limit %s c.");
        add("gui.hollowfrontier.blackjack.your_move", "Hit, stand or double?");
        add("gui.hollowfrontier.blackjack.outcome.none", "");
        add("gui.hollowfrontier.blackjack.outcome.blackjack", "Blackjack! You're paid %s c.");
        add("gui.hollowfrontier.blackjack.outcome.win", "You win. You're paid %s c.");
        add("gui.hollowfrontier.blackjack.outcome.dealer_bust", "The dealer busts. You're paid %s c.");
        add("gui.hollowfrontier.blackjack.outcome.push", "Push. Your %s c comes back.");
        add("gui.hollowfrontier.blackjack.outcome.lose", "The house wins.");
        add("gui.hollowfrontier.blackjack.outcome.bust", "Bust. The house wins.");

        // Music and sounds
        addBlock(ModBlocks.SALOON_PIANO, "Saloon Piano");
        add("message.hollowfrontier.piano.playing", "The piano strikes up a rag.");
        add("message.hollowfrontier.piano.stopped", "The piano lid closes.");
        add("track.hollowfrontier.the_entertainer", "Scott Joplin - The Entertainer (1902)");
        add("track.hollowfrontier.elite_syncopations", "Scott Joplin - Elite Syncopations (1902)");
        add("track.hollowfrontier.pineapple_rag", "Scott Joplin - Pineapple Rag (1908)");
        add("track.hollowfrontier.the_strenuous_life", "Scott Joplin - The Strenuous Life (1902)");
        add("subtitles.hollowfrontier.saloon_piano.play", "Piano plays");
        add("subtitles.hollowfrontier.rattlesnake.rattle", "Rattlesnake rattles");
        add("subtitles.hollowfrontier.rattlesnake.hurt", "Rattlesnake hisses");
        add("subtitles.hollowfrontier.rattlesnake.death", "Rattlesnake dies");
        add("subtitles.hollowfrontier.dynamite.fuse", "Dynamite fuse fizzes");
        add("subtitles.hollowfrontier.coin.clink", "Coins clink");
        add("subtitles.hollowfrontier.drink.pour", "Drink poured");
        add("subtitles.hollowfrontier.wanted.raised", "Alarm bell rings");
        add("subtitles.hollowfrontier.town.blacksmith", "Blacksmith hammers");
        add("subtitles.hollowfrontier.town.horse", "Horse nickers");
        add("subtitles.hollowfrontier.town.bell", "Chapel bell tolls");
        add("subtitles.hollowfrontier.town.door", "Door creaks");
        add("subtitles.hollowfrontier.town.spurs", "Spurs jingle");
        add("subtitles.hollowfrontier.town.boardwalk", "Boots on the boardwalk");
        add("subtitles.hollowfrontier.saloon.glasses", "Glasses clink");
        add("subtitles.hollowfrontier.town.cat", "Cat meows nearby");
        add("subtitles.hollowfrontier.ghost_cat.fade", "Ghost cat fades");
        add("subtitles.hollowfrontier.raid.alarm", "Alarm clangs");
        add("subtitles.hollowfrontier.raid.hooves", "Hoofbeats approach");
        add("subtitles.hollowfrontier.raid.gunfire", "Distant gunfire");
        add("subtitles.hollowfrontier.raid.vault_work", "Safecracker works the vault");
        add("subtitles.hollowfrontier.raid.vault_blown", "Vault blown open");
        add("subtitles.hollowfrontier.raid.won", "Raid beaten off");
        add("subtitles.hollowfrontier.raid.lost", "Gang rides off with the loot");

        // The Frontier Gazette: each story is a .title and a .body
        addItem(ModItems.FRONTIER_GAZETTE, "The Frontier Gazette");
        add("gazette.hollowfrontier.masthead", "THE FRONTIER GAZETTE");
        add("gazette.hollowfrontier.edition", "Day %s. Price: one copper.");
        add("gazette.hollowfrontier.outlaws.title", "OUTLAWS SPOTTED!");
        add("gazette.hollowfrontier.outlaws.body", "A cattle drover reports a gang of desperadoes making camp near X %s, Z %s. Citizens are advised to keep clear, or to bring plenty of cartridges.");
        add("gazette.hollowfrontier.saloon.title", "WHISKEY ON THE TRAIL");
        add("gazette.hollowfrontier.saloon.body", "Weary travellers will find a roadhouse saloon pouring near X %s, Z %s. The bartender is said to be friendly and the beer only a little warm.");
        add("gazette.hollowfrontier.wanted.title", "WANTED");
        add("gazette.hollowfrontier.wanted.body", "%s, for robbing the Overland Stage. Reward: %s gold coins. Ask at any Bounty Board.");
        add("gazette.hollowfrontier.wanted_you.title", "WANTED: A DANGEROUS STRANGER");
        add("gazette.hollowfrontier.wanted_you.body", "One %s is sought by the law for crimes against decent folk. The sheriff will accept a fine of %s copper coins at any Bounty Board. Until then: shoot on sight.");
        add("gazette.hollowfrontier.quiet.1.title", "A QUIET WEEK");
        add("gazette.hollowfrontier.quiet.1.body", "Nothing worth printing happened this week, which the editor considers a mercy.");
        add("gazette.hollowfrontier.quiet.2.title", "PRICE OF BEANS UP AGAIN");
        add("gazette.hollowfrontier.quiet.2.body", "The general store reports beans have risen to two coppers a can. The editor blames the railroad.");
        add("gazette.hollowfrontier.quiet.3.title", "CHURCH SOCIAL A SUCCESS");
        add("gazette.hollowfrontier.quiet.3.body", "Pie was eaten. Hymns were sung. Nobody was shot.");
        // The latest raid (args: leader, gang surname, x, z)
        add("gazette.hollowfrontier.raid.defended.title", "TOWN SEES OFF THE %2$s GANG");
        add("gazette.hollowfrontier.raid.defended.body", "Townsfolk, lawmen and one stranger handy with a gun drove %1$s and the %2$s Gang out of %5$s near X %3$s, Z %4$s. Drinks are on the house.");
        add("gazette.hollowfrontier.raid.held.title", "LAWMEN HOLD THE LINE");
        add("gazette.hollowfrontier.raid.held.body", "The lawmen of %5$s near X %3$s, Z %4$s turned back %1$s and the %2$s Gang without a stranger's help. The sheriff has been insufferable since.");
        add("gazette.hollowfrontier.raid.recovered.title", "BANK LOOT RECOVERED");
        add("gazette.hollowfrontier.raid.recovered.body", "The %2$s Gang cracked the vault in %5$s near X %3$s, Z %4$s, but their bagman never made it out of town. Every coin is back where it belongs.");
        add("gazette.hollowfrontier.raid.robbed.title", "VAULT ROBBED!");
        add("gazette.hollowfrontier.raid.robbed.body", "%1$s and the %2$s Gang cleaned out the vault of %5$s near X %3$s, Z %4$s and rode off into the night. A reward is posted at the town's Bounty Board.");
        add("gazette.hollowfrontier.raid.gave_up.title", "RAIDERS RIDE OFF");
        add("gazette.hollowfrontier.raid.gave_up.body", "The %2$s Gang shot up %5$s near X %3$s, Z %4$s until they tired of it. %1$s is still at large; see the town's Bounty Board.");
        add("gazette.hollowfrontier.gateway.title", "EXPEDITION LOST IN THE SNOWS");
        add("gazette.hollowfrontier.gateway.body", "No word from the Polar Survey since it reached a crater of blue ice near X %s, Z %s. Their last letter spoke of a ring of carved stones that hummed when struck, and asked for more coal.");
        add("gazette.hollowfrontier.odd.1.title", "LIGHTS OVER THE BADLANDS");
        add("gazette.hollowfrontier.odd.1.body", "Three ranch hands swear they saw a silver plate fly over the mesa at midnight, humming like a beehive. The ranch has since run dry of whiskey.");
        add("gazette.hollowfrontier.odd.2.title", "THE EARTH IS HOLLOW, SAYS OLD PROSPECTOR");
        add("gazette.hollowfrontier.odd.2.body", "A grizzled miner claims that past the ice at the end of the world lies a second sun, burning inside the earth. He was last seen buying a great deal of warm socks.");
        add("gazette.hollowfrontier.odd.3.title", "STEAM CARRIAGE FRIGHTENS HORSES");
        add("gazette.hollowfrontier.odd.3.body", "An inventor drove a hissing brass contraption down Main Street on Tuesday. Two horses bolted and the inventor was fined for disturbing the peace.");
        add("gazette.hollowfrontier.odd.4.title", "STRANGE CRYSTALS FOUND");
        add("gazette.hollowfrontier.odd.4.body", "Glowing blue stones turned up in a prospector's pan. Assayers cannot name the mineral. One of them now hears humming at night.");
        add("gazette.hollowfrontier.odd.5.title", "RATTLER IN THE BATHTUB");
        add("gazette.hollowfrontier.odd.5.body", "A hotel guest found a rattlesnake coiled in her washbasin. Mind the sound of a rattle out in the desert, readers: back off slowly.");
        // 9c: rumours that point past the edge of the map
        add("gazette.hollowfrontier.odd.6.title", "AIRSHIP SIGHTED, NO BALLOON");
        add("gazette.hollowfrontier.odd.6.body", "A cigar-shaped craft crossed the county Thursday night, sweeping the ground with a bright light. Witnesses say it flew against the wind and made no sound at all. No airship company will own to it.");
        add("gazette.hollowfrontier.odd.7.title", "SYMMES' HOLE: A LECTURE");
        add("gazette.hollowfrontier.odd.7.body", "A travelling professor will speak at the hall on the late Colonel Symmes' theory: that the world is open at both poles. He tells of an expedition of '89 that went in and did not come back. Admission ten cents.");
        add("gazette.hollowfrontier.odd.8.title", "WHALER TELLS OF WARM SEAS");
        add("gazette.hollowfrontier.odd.8.body", "A whaling captain swears that past the pack ice the sea ran warm, and birds flew north toward land where no land should be. His crew would not sail on. He is buying a compass that points true north, if anyone has one.");
        // The strange grave on the rare Boot Hill (9c)
        // 26: the sign by the Kindled vault's seal
        add("sign.hollowfrontier.seal.1", "Light what");
        add("sign.hollowfrontier.seal.2", "the wall shows,");
        add("sign.hollowfrontier.seal.3", "and no more");
        // 28: the relics, the sherds, the trim, the explorers' maps
        addItem(ModItems.KINDLED_IDOL, "Kindled Idol");
        add("item.hollowfrontier.kindled_idol.desc", "A figure in crystal, arms raised, lit from within.");
        addItem(ModItems.CRYSTAL_TABLET, "Crystal Tablet");
        add("item.hollowfrontier.crystal_tablet.desc", "Writing too fine for any pen, cut into crystal.");
        addItem(ModItems.SUNSTONE_CIRCLET, "Sunstone Circlet");
        add("item.hollowfrontier.sunstone_circlet.desc", "Warm to the touch. Always.");
        addItem(ModItems.STARSTEEL_MASK, "Starsteel Mask");
        add("item.hollowfrontier.starsteel_mask.desc", "A calm face, too long and too narrow to be a man's.");
        addItem(ModItems.GRAY_SPECIMEN_JAR, "Gray Specimen Jar");
        add("item.hollowfrontier.gray_specimen_jar.desc", "Something small in cloudy fluid. Best not to shake it.");
        addItem(ModItems.EXPEDITION_SEXTANT, "Expedition Sextant");
        add("item.hollowfrontier.expedition_sextant.desc", "Engraved: Symmes Exp. No. 2. Useless where the sun never moves.");
        addItem(ModItems.IVORY_CARVING, "Ivory Carving");
        add("item.hollowfrontier.ivory_carving.desc", "A mammoth in mammoth tusk, carved by someone who'd seen one.");
        addItem(ModItems.STAR_CHART, "Star Chart");
        add("item.hollowfrontier.star_chart.desc", "Stars where there is no sky to put them in.");
        add("tooltip.hollowfrontier.relic", "A town prospector pays %s gold coins");
        addItem(ModItems.SUN_POTTERY_SHERD, "Sun Pottery Sherd");
        addItem(ModItems.CRYSTAL_POTTERY_SHERD, "Crystal Pottery Sherd");
        addItem(ModItems.WATCHER_POTTERY_SHERD, "Watcher Pottery Sherd");
        addItem(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE, "Smithing Template");
        add("trim_pattern.hollowfrontier.concavian", "Concavian Armor Trim");
        add("item.hollowfrontier.map.sun_temple", "Expedition Map: the Sun Temple");
        add("item.hollowfrontier.map.gray_pens", "Gray Chart: Holding Pens");
        add("item.hollowfrontier.map.crystal_ruins", "Kindled Chart: the Crystal City");
        add("item.hollowfrontier.map.foundry", "Mining Co. Map: the Old Foundry");
        // 27: the phlogiston mine's sign, and the prospector's notes at the tar pit
        add("sign.hollowfrontier.phlogiston_mine.1", "STEPPE CRYSTAL");
        add("sign.hollowfrontier.phlogiston_mine.2", "MINING CO.");
        add("sign.hollowfrontier.phlogiston_mine.3", "No Lamps Below");
        add("book.hollowfrontier.prospector_notes.1", "Day 3. Found the black pool the Symmes men wrote of. Bones in it bigger than a locomotive. A tusk long as a wagon tongue, and ivory fetches a price back home.");
        add("book.hollowfrontier.prospector_notes.2", "Day 5. The tar takes whatever it's given. Lost a boot to it, then the mule nearly. Hauled the tusk halfway out before the light went the color it goes here at no particular time.");
        add("book.hollowfrontier.prospector_notes.3", "Day 6. Big birds at the edge again, watching. They don't come close. Something in the trees does. It doesn't walk like a bird and it doesn't walk like a man.");
        add("book.hollowfrontier.prospector_notes.4", "Day 7. Lights over the pool last night, slow and quiet. Anyone finds this: the tusk is yours. I'm going to look.");
        add("sign.hollowfrontier.strange_grave.1", "THE AIRSHIP'S");
        add("sign.hollowfrontier.strange_grave.2", "PILOT. NOT OF");
        add("sign.hollowfrontier.strange_grave.3", "THIS WORLD.");
        add("sign.hollowfrontier.strange_grave.4", "APRIL 1897");
        // 23: the shop signs over the doors (the name, and a line under it), the stage stop and the cannon's plaque
        String[][] shops = {
                {"general_store", "GENERAL STORE", "DRY GOODS, FEED"}, {"sheriff", "SHERIFF", "& JAIL"}, {"bank", "BANK", "& TRUST CO."},
                {"hotel", "HOTEL", "ROOMS BY NIGHT"}, {"hotel_grand", "GRAND HOTEL", "BATHS 2 BITS"}, {"blacksmith", "BLACKSMITH", "SHOEING DONE"},
                {"livery", "LIVERY STABLE", "FEED & BOARD"}, {"undertaker", "UNDERTAKER", "COFFINS MADE"}, {"gunsmith", "GUNSMITH", "REPAIRS, AMMO"},
                {"doctor", "DOCTOR", "TEETH PULLED"}, {"post_office", "POST OFFICE", "& TELEGRAPH"}, {"schoolhouse", "SCHOOL", "EST. 1871"},
                {"saloon", "SALOON", "COLD BEER"}, {"saloon_cards", "CARDS", "& PIANO"}, {"stage_stop", "STAGE STOP", "MON & THU"},
                {"bathhouse", "BATHS & SHAVES", "HOT WATER"}, {"gazette_office", "THE GAZETTE", "NEWS & NOTICES"},
                {"ranch", "BAR T RANCH", "SHUT THE GATE"}};
        add("message.hollowfrontier.bath.sober", "You climb out of the tub sober, pink and steaming.");
        add("message.hollowfrontier.bath.fresh", "You climb out of the tub feeling like new.");
        for (String[] shop : shops) {
            add(com.hollowfrontier.worldgen.structure.TownDecor.signKey(shop[0]), shop[1]);
            add(com.hollowfrontier.worldgen.structure.TownDecor.signKey(shop[0]) + ".sub", shop[2]);
        }
        // 24a: the welcome arches (the name and population lines are filled in by TownNameProcessor)
        add("sign.hollowfrontier.welcome", "WELCOME TO");
        add("sign.hollowfrontier.leaving", "YOU ARE LEAVING");
        add("sign.hollowfrontier.come_again", "COME AGAIN");
        add("sign.hollowfrontier.population", "POP. %s");
        add("sign.hollowfrontier.this_town", "THIS TOWN");
        add("text.hollowfrontier.town_of", "the town of %s");
        add("text.hollowfrontier.the_town", "the town");
        add(com.hollowfrontier.worldgen.structure.TownNameProcessor.NAME, "TOWN NAME");
        add(com.hollowfrontier.worldgen.structure.TownNameProcessor.POPULATION, "POP.");
        add("sign.hollowfrontier.cannon.1", "IN MEMORY OF");
        add("sign.hollowfrontier.cannon.2", "THE 3RD");
        add("sign.hollowfrontier.cannon.3", "VOLUNTEER");
        add("sign.hollowfrontier.cannon.4", "CAVALRY");
        // Barmaids: names and greetings (flirty, frontier-friendly). "say" is the chat line format.
        add("entity.hollowfrontier.barmaid.ruby", "Miss Ruby");
        add("entity.hollowfrontier.barmaid.clementine", "Clementine");
        add("entity.hollowfrontier.barmaid.josie", "Josie");
        add("entity.hollowfrontier.barmaid.belle", "Miss Belle");
        add("entity.hollowfrontier.barmaid.lupe", "Lupe");
        add("tooltip.hollowfrontier.bounty.blank", "Blank");
        add("tooltip.hollowfrontier.bounty.target", "WANTED: %s");
        add("tooltip.hollowfrontier.bounty.location", "Hiding out near X %s, Z %s");
        add("tooltip.hollowfrontier.bounty.spotted", "Gang spotted!");
        add("tooltip.hollowfrontier.bounty.reward", "Reward: %s Gold Coins");
        add("tooltip.hollowfrontier.bounty.poster_hint", "Ride out to the hideout. Bring back the badge.");
        add("tooltip.hollowfrontier.bounty.badge_hint", "Turn in at a Bounty Board.");
        add("message.hollowfrontier.bounty.already_have", "You're already carrying a Wanted Poster.");
        // The board's face (18)
        add("message.hollowfrontier.bounty.none_up", "Nothing's posted on this board right now.");
        add("message.hollowfrontier.bounty.already_dead", "%s has already been brought in.");
        add("message.hollowfrontier.bounty.taken_by", "%s is %s's to bring in.");
        add("gui.hollowfrontier.bounty_board.wanted", "WANTED");
        add("gui.hollowfrontier.bounty_board.raid_leader", "WANTED: RAID LEADER");
        add("gui.hollowfrontier.bounty_board.reward", "%s gold");
        add("gui.hollowfrontier.bounty_board.fine", "Fine %s c");
        add("gui.hollowfrontier.bounty_board.dead_or_alive", "dead or alive");
        add("gui.hollowfrontier.bounty_board.bearing", "%s blocks %s");
        for (String[] point : new String[][] {{"north", "N"}, {"northeast", "NE"}, {"east", "E"}, {"southeast", "SE"}, {"south", "S"},
                {"southwest", "SW"}, {"west", "W"}, {"northwest", "NW"}}) {
            add("gui.hollowfrontier.bounty_board.compass." + point[0], point[1]);
        }
        add("gui.hollowfrontier.bounty_board.taken", "TAKEN");
        add("gui.hollowfrontier.bounty_board.dead", "DEAD");
        add("message.hollowfrontier.bounty.spotted", "You spot %s and their gang!");
        add("message.hollowfrontier.bounty.paid", "Collected %s Gold Coins for %s.");

        addBlock(ModBlocks.BOILER, "Boiler");
        addBlock(ModBlocks.COPPER_PIPE, "Copper Pipe");
        addBlock(ModBlocks.STEAM_TANK, "Steam Tank");
        addBlock(ModBlocks.PRESSURE_GAUGE, "Pressure Gauge");
        addBlock(ModBlocks.CRUSHER, "Steam Crusher");
        addItem(ModItems.STEAM_CANISTER, "Steam Canister");
        addItem(ModItems.STEAM_DRILL, "Steam Drill");
        addItem(ModItems.STEAM_HANDCAR, "Steam Handcar");
        addEntityType(ModEntities.STEAM_HANDCAR, "Steam Handcar");
        addBlock(ModBlocks.HANDCAR_ENGINE, "Handcar Engine");
        add("message.hollowfrontier.handcar.gauge", "Steam %s %s%%");
        add("subtitles.hollowfrontier.steam_handcar.chuff", "Handcar chuffs");
        add("subtitles.hollowfrontier.steam_handcar.whistle", "Steam whistle blows");
        addItem(ModItems.IRON_DUST, "Iron Dust");
        addItem(ModItems.GOLD_DUST, "Gold Dust");
        addItem(ModItems.COPPER_DUST, "Copper Dust");
        add("fluid_type.hollowfrontier.steam", "Steam");
        add("container.hollowfrontier.boiler", "Boiler");
        add("container.hollowfrontier.crusher", "Steam Crusher");
        add("tooltip.hollowfrontier.steam", "Steam: %s / %s mB");
        add("tooltip.hollowfrontier.steam_drill", "Uses %s mB of steam per block from Steam Canisters in your inventory");
        add("message.hollowfrontier.gauge.none", "No steam connection");
        add("message.hollowfrontier.gauge.reading", "Pressure: %s%% (%s / %s mB)");

        // Airship and the polar expedition
        addItem(ModItems.AIRSHIP_ENVELOPE, "Airship Envelope");
        addItem(ModItems.AIRSHIP_ENGINE, "Airship Engine");
        addItem(ModItems.AIRSHIP, "Airship");
        addEntityType(ModEntities.AIRSHIP, "Airship");
        add("tooltip.hollowfrontier.airship.fuel", "Burns coal or charcoal; takes steam from canisters or a steam tank under its keel");
        add("tooltip.hollowfrontier.airship.controls", "Jump to climb, sprint to descend, sneak to step off (twice, when aloft)");
        add("message.hollowfrontier.airship.gauge", "Steam %s %s%%  ·  Coal %s  ·  Altitude %s");
        add("message.hollowfrontier.airship.not_yours", "This airship belongs to %s.");
        add("message.hollowfrontier.airship.bail", "It's %s blocks to the ground. Sneak again to step off.");
        add("message.hollowfrontier.airship.gauge_bearing", "%s  ·  %s");
        add("tooltip.hollowfrontier.airship.stored", "Coal %s  ·  Steam %s%%");
        add("subtitles.hollowfrontier.airship.engine", "Airship engine thrums");
        add("subtitles.hollowfrontier.airship.vent", "Airship vents steam");
        add("subtitles.hollowfrontier.airship.stoke", "Airship firebox stoked");
        addItem(ModItems.POLAR_COMPASS, "Polar Compass");
        add("tooltip.hollowfrontier.polar_compass.unset", "Carry it and the needle finds the nearest Polar Gateway. Right-click to take a bearing now");
        add("tooltip.hollowfrontier.polar_compass.set", "Hold it to read how far. Right-click for a fresh bearing");
        add("message.hollowfrontier.polar_compass.bearing", "The needle settles: a gateway lies some %s blocks %s");
        add("message.hollowfrontier.polar_compass.lost", "The needle wanders. No gateway within reach");
        add("message.hollowfrontier.polar_compass.first", "The needle settles: a gateway lies some %s blocks %s. Hold the compass to keep reading it");
        add("message.hollowfrontier.polar_compass.reading", "Gateway: some %s blocks %s");
        add("message.hollowfrontier.polar_compass.here", "Gateway: right here");
        for (String point : new String[] {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"}) {
            add("message.hollowfrontier.direction." + point, point);
        }
        addItem(ModItems.POLAR_RESONATOR, "Polar Resonator");
        add("tooltip.hollowfrontier.polar_resonator", "Use on a Polar Gateway's ring to open it (%s mB of steam from canisters). Sneak-use to shut it");
        add("message.hollowfrontier.polar_resonator.broken_ring", "The ring is broken. It won't ring true");
        add("message.hollowfrontier.polar_resonator.already_open", "The gateway is already open");
        add("message.hollowfrontier.polar_resonator.blocked", "Something's lying in the well. Clear it first");
        add("message.hollowfrontier.polar_resonator.no_steam", "Not enough steam: the resonator needs %s mB from your canisters");
        add("message.hollowfrontier.polar_resonator.opened", "The ring begins to sing...");
        add("message.hollowfrontier.polar_resonator.closed", "The ring falls silent");
        addBlock(ModBlocks.GATEWAY_FRAME, "Gateway Frame");
        addBlock(ModBlocks.POLAR_PORTAL, "Polar Gateway");
        add("subtitles.hollowfrontier.polar_gateway.open", "Polar Gateway opens");
        add("subtitles.hollowfrontier.polar_gateway.close", "Polar Gateway closes");
        add("subtitles.hollowfrontier.polar_gateway.stone", "Gateway stone chimes");
        add("subtitles.hollowfrontier.polar_gateway.charge", "Polar Gateway hums");
        add("subtitles.hollowfrontier.polar_gateway.cross", "Rush of cold air");
        add("subtitles.hollowfrontier.polar_gateway.land", "Soft landing in snow");
        add("subtitles.hollowfrontier.hollow_earth.reveal", "A great hollow opens below");
        add("title.hollowfrontier.hollow_earth", "THE HOLLOW EARTH");
        add("title.hollowfrontier.hollow_earth.subtitle", "Beneath the ice, another sun");
        add("biome.hollowfrontier.primordial_jungle", "Primordial Jungle");
        add("biome.hollowfrontier.crystal_steppes", "Crystal Steppes");
        add("biome.hollowfrontier.ember_badlands", "Ember Badlands");
        add("biome.hollowfrontier.concavian_highlands", "Concavian Highlands");
        addBlock(ModBlocks.CRUST_ROCK, "Crust Rock");
        addBlock(ModBlocks.SHELL_BEDROCK, "Shell Bedrock");
        addBlock(ModBlocks.FERN_FROND, "Fern Frond");
        addBlock(ModBlocks.ASH, "Ash");
        addBlock(ModBlocks.ASH_VENT, "Ash Vent");
        add("subtitles.hollowfrontier.primordial_jungle.additions", "Something calls in the jungle");
        add("subtitles.hollowfrontier.crystal_steppes.additions", "Crystals chime");
        add("subtitles.hollowfrontier.ember_badlands.additions", "The ground rumbles");
        add("subtitles.hollowfrontier.concavian_highlands.additions", "Wind over the uplands");
        add("subtitles.hollowfrontier.ash_vent.hiss", "Ash vent hisses");
        addEntityType(ModEntities.FERN_SKITTER, "Fern Skitter");
        addEntityType(ModEntities.GLASSBACK_HARE, "Glassback Hare");
        addEntityType(ModEntities.ASH_SALAMANDER, "Ash Salamander");
        addEntityType(ModEntities.PALE_RAVEN, "Pale Raven");
        addItem(ModItems.FERN_SKITTER_SPAWN_EGG, "Fern Skitter Spawn Egg");
        addItem(ModItems.GLASSBACK_HARE_SPAWN_EGG, "Glassback Hare Spawn Egg");
        addItem(ModItems.ASH_SALAMANDER_SPAWN_EGG, "Ash Salamander Spawn Egg");
        addItem(ModItems.PALE_RAVEN_SPAWN_EGG, "Pale Raven Spawn Egg");
        addEntityType(ModEntities.GRAY_PROBE, "Gray Probe");
        addItem(ModItems.GRAY_PROBE_SPAWN_EGG, "Gray Probe Spawn Egg");
        // 9c: starsteel and phlogiston
        addBlock(ModBlocks.STARSTEEL_ORE, "Starsteel Ore");
        addBlock(ModBlocks.DEEPSLATE_STARSTEEL_ORE, "Deepslate Starsteel Ore");
        addBlock(ModBlocks.RAW_STARSTEEL_BLOCK, "Block of Raw Starsteel");
        addBlock(ModBlocks.STARSTEEL_BLOCK, "Block of Starsteel");
        addBlock(ModBlocks.PHLOGISTON_ORE, "Phlogiston Ore");
        addBlock(ModBlocks.DEEPSLATE_PHLOGISTON_ORE, "Deepslate Phlogiston Ore");
        addBlock(ModBlocks.PHLOGISTON_LAMP, "Phlogiston Lamp");
        addItem(ModItems.RAW_STARSTEEL, "Raw Starsteel");
        addItem(ModItems.STARSTEEL_INGOT, "Starsteel Ingot");
        addItem(ModItems.STARSTEEL_NUGGET, "Starsteel Nugget");
        addItem(ModItems.STARSTEEL_DUST, "Starsteel Dust");
        addItem(ModItems.PHLOGISTON_SHARD, "Phlogiston Shard");
        addItem(ModItems.STARSTEEL_SWORD, "Starsteel Sword");
        addItem(ModItems.STARSTEEL_PAXEL, "Starsteel Paxel");
        add("tooltip.hollowfrontier.paxel", "Pickaxe, axe and shovel in one");
        addItem(ModItems.STARSTEEL_HELMET, "Starsteel Helmet");
        addItem(ModItems.STARSTEEL_CHESTPLATE, "Starsteel Chestplate");
        addItem(ModItems.STARSTEEL_LEGGINGS, "Starsteel Leggings");
        addItem(ModItems.STARSTEEL_BOOTS, "Starsteel Boots");
        addItem(ModItems.STARSTEEL_HORSESHOES, "Starsteel Horseshoes");
        add("tooltip.hollowfrontier.sun_forged", "Sun-forged: mends itself in sunlight");
        add("tooltip.hollowfrontier.starsteel_horseshoes", "A horse shod in starsteel, and its rider, take no fall damage");
        add("subtitles.hollowfrontier.fern_skitter.ambient", "Fern skitter chirps");
        add("subtitles.hollowfrontier.fern_skitter.hurt", "Fern skitter squawks");
        add("subtitles.hollowfrontier.fern_skitter.death", "Fern skitter dies");
        add("subtitles.hollowfrontier.glassback_hare.ambient", "Glassback hare chimes");
        add("subtitles.hollowfrontier.glassback_hare.hurt", "Glassback hare squeaks");
        add("subtitles.hollowfrontier.glassback_hare.death", "Glassback hare dies");
        add("subtitles.hollowfrontier.ash_salamander.ambient", "Ash salamander croaks");
        add("subtitles.hollowfrontier.ash_salamander.hurt", "Ash salamander hisses");
        add("subtitles.hollowfrontier.ash_salamander.death", "Ash salamander dies");
        add("subtitles.hollowfrontier.pale_raven.ambient", "Pale raven croaks");
        add("subtitles.hollowfrontier.pale_raven.hurt", "Pale raven caws");
        add("subtitles.hollowfrontier.pale_raven.death", "Pale raven dies");
        add("subtitles.hollowfrontier.gray_probe.hum", "Something hums overhead");
        add("subtitles.hollowfrontier.gray_probe.hurt", "Probe clanks");
        add("subtitles.hollowfrontier.gray_probe.flee", "Probe whines away");
        add("subtitles.hollowfrontier.gray_probe.vanish", "Probe blinks out");
        // The wild megafauna (10a)
        addEntityType(ModEntities.WOOLLY_MAMMOTH, "Woolly Mammoth");
        addEntityType(ModEntities.TERROR_BIRD, "Terror Bird");
        addEntityType(ModEntities.MEGATHERIUM, "Megatherium");
        addEntityType(ModEntities.GLYPTODON, "Glyptodon");
        addItem(ModItems.WOOLLY_MAMMOTH_SPAWN_EGG, "Woolly Mammoth Spawn Egg");
        addItem(ModItems.TERROR_BIRD_SPAWN_EGG, "Terror Bird Spawn Egg");
        addItem(ModItems.MEGATHERIUM_SPAWN_EGG, "Megatherium Spawn Egg");
        addItem(ModItems.GLYPTODON_SPAWN_EGG, "Glyptodon Spawn Egg");
        addItem(ModItems.MAMMOTH_TUSK, "Mammoth Tusk");
        addItem(ModItems.RAW_MAMMOTH, "Raw Mammoth");
        addItem(ModItems.MAMMOTH_STEAK, "Mammoth Steak");
        addItem(ModItems.OLIPHANT, "Oliphant");
        addItem(ModItems.TERROR_BIRD_TALON, "Terror Bird Talon");
        addItem(ModItems.TALON_KNIFE, "Talon Knife");
        addItem(ModItems.THICK_HIDE, "Thick Hide");
        addItem(ModItems.HIDE_BARDING, "Hide Barding");
        addItem(ModItems.GLYPTODON_SCUTE, "Glyptodon Scute");
        addItem(ModItems.GLYPTODON_HELMET, "Glyptodon Shell Helmet");
        addItem(ModItems.GLYPTODON_CHESTPLATE, "Glyptodon Shell Chestplate");
        addItem(ModItems.GLYPTODON_LEGGINGS, "Glyptodon Shell Leggings");
        addItem(ModItems.GLYPTODON_BOOTS, "Glyptodon Shell Boots");
        add("tooltip.hollowfrontier.oliphant", "Calms angry beasts and scatters terror birds nearby");
        add("tooltip.hollowfrontier.talon_knife", "Skinning: what you kill drops one more hide, tusk, talon, scute or snakeskin");
        add("tooltip.hollowfrontier.hide_barding", "Horse armor. Steadies a horse against knockback");
        add("tooltip.hollowfrontier.glyptodon_shell", "Full set: sneak to shell up (projectiles do a quarter, explosions half)");
        add("subtitles.hollowfrontier.woolly_mammoth.ambient", "Mammoth rumbles");
        add("subtitles.hollowfrontier.woolly_mammoth.hurt", "Mammoth bellows");
        add("subtitles.hollowfrontier.woolly_mammoth.death", "Mammoth dies");
        add("subtitles.hollowfrontier.woolly_mammoth.trumpet", "Mammoth trumpets");
        add("subtitles.hollowfrontier.terror_bird.ambient", "Terror bird clacks");
        add("subtitles.hollowfrontier.terror_bird.hurt", "Terror bird squawks");
        add("subtitles.hollowfrontier.terror_bird.death", "Terror bird dies");
        add("subtitles.hollowfrontier.terror_bird.screech", "Terror bird screeches");
        add("subtitles.hollowfrontier.megatherium.ambient", "Megatherium grunts");
        add("subtitles.hollowfrontier.megatherium.hurt", "Megatherium roars");
        add("subtitles.hollowfrontier.megatherium.death", "Megatherium dies");
        add("subtitles.hollowfrontier.megatherium.browse", "Megatherium strips leaves");
        add("subtitles.hollowfrontier.glyptodon.ambient", "Glyptodon snuffles");
        add("subtitles.hollowfrontier.glyptodon.hurt", "Glyptodon grunts");
        add("subtitles.hollowfrontier.glyptodon.death", "Glyptodon dies");
        add("subtitles.hollowfrontier.glyptodon.hunker", "Glyptodon shells up");
        add("subtitles.hollowfrontier.glyptodon.brush", "Glyptodon is brushed");
        add("subtitles.hollowfrontier.oliphant.blow", "Oliphant sounds");
        // The Gray experiments (10b)
        addEntityType(ModEntities.GRAY_SENTINEL, "Gray Sentinel");
        addItem(ModItems.GRAY_SENTINEL_SPAWN_EGG, "Gray Sentinel Spawn Egg");
        addItem(ModItems.GRAY_TECH_FRAGMENT, "Gray Tech Fragment");
        addItem(ModItems.SIGNAL_LOCATOR, "Signal Locator");
        add("tooltip.hollowfrontier.signal_locator.set", "Tuned to a Gray signal. Use to take a new bearing.");
        add("tooltip.hollowfrontier.signal_locator.unset", "Use to listen for the nearest Gray station");
        add("message.hollowfrontier.signal_locator.bearing", "A Gray signal, about %s blocks %s");
        add("message.hollowfrontier.signal_locator.lost", "Nothing but static");
        add("subtitles.hollowfrontier.gray_sentinel.charge", "Sentinel locks on");
        add("subtitles.hollowfrontier.gray_sentinel.zap", "Sentinel fires");
        add("subtitles.hollowfrontier.gray_sentinel.alarm", "Station alarm");
        add("subtitles.hollowfrontier.signal_locator.ping", "Signal Locator pings");
        // The frontier's mysteries (11a)
        addEntityType(ModEntities.AIRSHIP_SIGHTING, "Strange Airship");
        addEntityType(ModEntities.CATTLE_CARCASS, "Cattle Carcass");
        addBlock(ModBlocks.FLATTENED_WHEAT, "Flattened Wheat");
        addItem(ModItems.STRANGE_SKULL, "Strange Skull");
        add("tooltip.hollowfrontier.strange_skull", "Dug up on Boot Hill. Hold it up to the night sky.");
        add("message.hollowfrontier.strange_skull.nothing", "Nothing answers. Try under the open sky, at night.");
        add("message.hollowfrontier.strange_skull.called", "A hum, far up. Something is coming.");
        add("message.hollowfrontier.carcass.look", "The cuts are as neat as a surgeon's, and there isn't a drop of blood. The grass round it is scorched in a ring.");
        add("subtitles.hollowfrontier.airship_sighting.hum", "Something hums high overhead");
        // 29: dust devils
        addEntityType(ModEntities.DUST_DEVIL, "Dust Devil");
        addEntityType(ModEntities.ASH_DEVIL, "Ash Devil");
        add("subtitles.hollowfrontier.dust_devil.wind", "Dust devil whirls");
        add("message.hollowfrontier.dust_devil.hat", "Your hat!");
        add("commands.hollowfrontier.dustdevil", "A dust devil spins up.");
        add("commands.hollowfrontier.dustdevil.ash", "An Ash Devil spins up.");
        add("gazette.hollowfrontier.mystery.sighting.title", "AIRSHIP SEEN AGAIN");
        add("gazette.hollowfrontier.mystery.sighting.body", "%s day(s) ago a great airship, cigar-shaped and strung with lights, crossed the sky near %s, %s, keeping no course any balloonist could name. The Editor would like a word with its owner.");
        add("gazette.hollowfrontier.mystery.mutilation.title", "STEER FOUND CUT, NO BLOOD");
        add("gazette.hollowfrontier.mystery.mutilation.body", "%s day(s) ago a steer was found dead near %s, %s, cut with a surgeon's care, in a ring of scorched grass. No tracks led to it or away. Ranchers are keeping their herds close.");
        add("gazette.hollowfrontier.mystery.crop_circle.title", "RINGS IN THE WHEAT");
        add("gazette.hollowfrontier.mystery.crop_circle.body", "%s day(s) ago farmers near %s, %s woke to find rings pressed into a ripe field, every stalk laid flat and not one broken. Nobody heard a thing.");
        add("book.hollowfrontier.pilots_log.1", "day four hundred and nine. hull breached over the dry country. drive failing. the burning stone is spent.");
        add("book.hollowfrontier.pilots_log.2", "the people here are kind. they gave me water. they cannot know what we do to their cattle, or what we are making of their lost men.");
        add("book.hollowfrontier.pilots_log.3", "if this is found: the way in is at the pole. the light inside is not a sun. the lit ones were there first. ask them.");
        add("book.hollowfrontier.pilots_log.4", "i will not go back. let the others look for me.");
        add("commands.hollowfrontier.mystery.sighting", "An airship crosses the sky.");
        add("commands.hollowfrontier.mystery.cattle", "A steer nearby has been cut.");
        add("commands.hollowfrontier.mystery.circle", "A circle is pressed into the wheat nearby.");
        add("commands.hollowfrontier.mystery.visit", "A probe is coming down to do it.");
        add("commands.hollowfrontier.mystery.no_cattle", "No grown cattle nearby.");
        add("commands.hollowfrontier.mystery.no_wheat", "No ripe wheat field nearby.");
        // The men in black (11b)
        addEntityType(ModEntities.MAN_IN_BLACK, "Man in Black");
        addItem(ModItems.MAN_IN_BLACK_SPAWN_EGG, "Man in Black Spawn Egg");
        add("message.hollowfrontier.suspicion.watched", "You feel watched.");
        add("message.hollowfrontier.suspicion.eased", "The feeling of being watched passes.");
        add("message.hollowfrontier.suspicion.hostile", "They know your face now.");
        add("message.hollowfrontier.suspicion.hunted", "Two dark figures are coming across the dark toward you.");
        add("gazette.hollowfrontier.mystery.correction.sighting.title", "A CORRECTION");
        add("gazette.hollowfrontier.mystery.correction.sighting.body", "The Gazette regrets its report of lights in the sky. Gentlemen from out of town have kindly explained that the witnesses saw a weather balloon, and the planet Venus. The Editor sees no reason to doubt them.");
        add("gazette.hollowfrontier.mystery.correction.mutilation.title", "A CORRECTION");
        add("gazette.hollowfrontier.mystery.correction.mutilation.body", "The Gazette regrets its report of a steer found strangely cut. Gentlemen from out of town have kindly explained that it was wolves. The Editor is glad to have it cleared up, and would rather not discuss it further.");
        add("gazette.hollowfrontier.mystery.correction.crop_circle.title", "A CORRECTION");
        add("gazette.hollowfrontier.mystery.correction.crop_circle.body", "The Gazette regrets its report of rings in the wheat. Gentlemen from out of town have kindly explained that it was a hailstorm, and some boys with a plank. The Editor has nothing further to add.");
        // 15a: Gray Landings (the men in black no longer come, but the correction is there in case an op lands one sooner)
        add("gazette.hollowfrontier.mystery.landing.title", "SAUCER SETS DOWN");
        add("gazette.hollowfrontier.mystery.landing.body", "%s day(s) ago a craft of the kind once seen only in the sky came down near %s, %s, and stayed the night. Grey figures were seen about it with lanterns of green fire. By morning it was gone, leaving a ring of burnt ground. The Editor no longer laughs at such reports.");
        add("gazette.hollowfrontier.mystery.correction.landing.title", "A CORRECTION");
        add("gazette.hollowfrontier.mystery.correction.landing.body", "The Gazette regrets its report of a craft set down in the night. Gentlemen from out of town have kindly explained that it was a grass fire. The Editor has been asked not to go and look.");
        add("message.hollowfrontier.landing.sealed", "Sealed. Its keepers are still about.");
        add("message.hollowfrontier.landing.cleared", "With its keepers down, the saucer lifts off in a hurry and leaves its cache behind.");
        add("message.hollowfrontier.landing.dawn", "With the dawn the saucer takes its keepers and its cargo aboard, and lifts off.");
        add("commands.hollowfrontier.ufo.landing", "A saucer is coming down nearby.");
        add("commands.hollowfrontier.ufo.landing.no_room", "There's no open, level ground nearby for a saucer to set down on.");
        add("commands.hollowfrontier.mystery.visitors", "Two men in black dusters arrive.");
        add("commands.hollowfrontier.suspicion.get", "Suspicion: %s");
        // Concavia (12a): the crystal stone, the Kindled and their guardians, and the story they tell
        addBlock(ModBlocks.CONCAVIAN_CRYSTAL, "Concavian Crystal");
        addBlock(ModBlocks.CRYSTAL_BRICKS, "Crystal Bricks");
        addBlock(ModBlocks.CRYSTAL_BRICK_STAIRS, "Crystal Brick Stairs");
        addBlock(ModBlocks.CRYSTAL_BRICK_SLAB, "Crystal Brick Slab");
        addBlock(ModBlocks.CRYSTAL_BRICK_WALL, "Crystal Brick Wall");
        addBlock(ModBlocks.CHISELED_CRYSTAL_BRICKS, "Chiseled Crystal Bricks");
        addItem(ModItems.KINDLED_SIGIL, "Kindled Sigil");
        add("tooltip.hollowfrontier.kindled_sigil", "Hold it, and the seed behind the ear shows.");
        addEntityType(ModEntities.KINDLED, "Kindled");
        addItem(ModItems.KINDLED_SPAWN_EGG, "Kindled Spawn Egg");
        addEntityType(ModEntities.CRYSTAL_GUARDIAN, "Crystal Guardian");
        addItem(ModItems.CRYSTAL_GUARDIAN_SPAWN_EGG, "Crystal Guardian Spawn Egg");
        add("message.hollowfrontier.kindled.line.0", "We were here before your people had a word for the north. This land was Concavia, and the sun inside it was ours to keep.");
        add("message.hollowfrontier.kindled.line.1", "The small grey ones came down out of the dark between the stars, hungry and polite. We let them in. That was our first mistake, and our last.");
        add("message.hollowfrontier.kindled.line.2", "They took our cities, our craft and our phlogiston. The ships your people see over the plains were never theirs. They were ours.");
        add("message.hollowfrontier.kindled.line.3", "Your outlaws are not born cruel. The grey ones take drifters off the plains, set a seed of metal behind the ear, and send them back angry.");
        add("message.hollowfrontier.kindled.line.4", "The men in the black coats are the same work, done more carefully. They chose the leash, for money and long life. Pity them, but don't trust them.");
        add("message.hollowfrontier.kindled.line.5", "A frontier at war with itself never looks up. That is all the grey ones want of the world above.");
        add("message.hollowfrontier.kindled.line.6", "Carry our sigil. In its light the seed behind the ear shows, through stone and through dark. You will know them by it.");
        add("message.hollowfrontier.kindled.line.7", "When the grey ones came, our last king went into the heart of the inner sun to hold it against them. He did not come out as he went in.");
        add("message.hollowfrontier.kindled.line.8", "If you go to the sun's temple, go armed. What sits on that throne answers to no one now. We call it the Archon.");
        add("message.hollowfrontier.kindled.refuse", "You have hurt our people. We have nothing to say to you.");
        add("message.hollowfrontier.kindled.friend", "The Kindled count you a friend.");
        add("message.hollowfrontier.kindled.allied", "The Kindled have taken you for an ally.");
        add("message.hollowfrontier.kindled.sigil", "You are given a Kindled Sigil.");
        add("message.hollowfrontier.kindled.wronged", "The Kindled will remember this.");
        add("message.hollowfrontier.guardian.gray", "The guardian's eye turns to the Gray things you carry.");
        add("message.hollowfrontier.guardian.wronged", "The guardian knows what you did.");
        add("message.hollowfrontier.guardian.struck", "The guardian turns on you.");
        add("message.hollowfrontier.guardian.warning", "The beam scorches the stone at your feet: a warning.");
        add("commands.hollowfrontier.kindled.get", "Kindled favour: %s");
        // The Sun Archon (12b): the boss, its temple's pylons, its artifacts, and the Codex
        addEntityType(ModEntities.SUN_ARCHON, "The Sun Archon");
        addItem(ModItems.SUN_ARCHON_SPAWN_EGG, "Sun Archon Spawn Egg");
        addBlock(ModBlocks.SUN_PYLON, "Sun Pylon");
        addItem(ModItems.SUNSTONE_SHARD, "Sunstone Shard");
        addItem(ModItems.PHLOGISTON_CORE, "Phlogiston Core");
        addItem(ModItems.STARSTEEL_CROWN, "Starsteel Crown");
        addItem(ModItems.SUNSTONE_LENS, "Sunstone Lens");
        addItem(ModItems.CONCAVIAN_CODEX, "Concavian Codex");
        add("tooltip.hollowfrontier.phlogiston_core", "The burning heart the last king carried. It is what the Grays came for.");
        add("tooltip.hollowfrontier.sunstone_lens", "Hold it to see in the dark. Use it to throw a beam of sunlight.");
        add("message.hollowfrontier.archon.wakes", "The Sun Archon lifts its head, and rises from its throne.");
        add("message.hollowfrontier.archon.corona", "The Archon rises into the light, and the pylons blaze to shield it!");
        add("message.hollowfrontier.archon.corona_bare", "The Archon rises into the light.");
        add("message.hollowfrontier.archon.shield_broken", "The last pylon breaks, and the Archon's shield fails!");
        add("message.hollowfrontier.archon.eclipse", "Eclipse! The Archon's light turns black.");
        add("message.hollowfrontier.archon.shielded", "The pylons' light turns the blow aside.");
        add("message.hollowfrontier.archon.falls", "The Archon falls, and the inner sun burns clean again.");
        add("message.hollowfrontier.kindled.temple", "The sun's temple stands in the Ember Badlands, near %s, %s.");
        add("death.attack.hollowfrontier.phlogiston_beam", "%1$s was burned away by %2$s");
        add("death.attack.hollowfrontier.phlogiston_beam.item", "%1$s was burned away by %2$s using %3$s");
        add("death.attack.hollowfrontier.phlogiston_beam.player", "%1$s was burned away while fighting %2$s");
        add("book.hollowfrontier.codex.title", "The Concavian Codex");
        add("book.hollowfrontier.codex.preface", "Set down by the last of the Kindled, for the one who would walk from the frontier to the inner sun. It writes itself anew each time it is opened.");
        add("book.hollowfrontier.codex.frontier", "I. The Frontier");
        add("book.hollowfrontier.codex.steam", "II. Steam and Ice");
        add("book.hollowfrontier.codex.hollow", "III. The World Within");
        add("book.hollowfrontier.codex.concavia", "IV. Concavia");
        // The Grays arrive (13a): their saucers, abductions, Wardens, and the wrecks' salvage
        addEntityType(ModEntities.GRAY_SAUCER, "Gray Saucer");
        addEntityType(ModEntities.GRAY_WARDEN, "Gray Warden");
        addEntityType(ModEntities.PHLOGISTON_BEAM, "Phlogiston Beam");
        addItem(ModItems.GRAY_WARDEN_SPAWN_EGG, "Gray Warden Spawn Egg");
        // 21c: Grays in disguise
        addEntityType(ModEntities.DISGUISED_GRAY, "Stranger");
        add("entity.hollowfrontier.disguised_gray.regular", "Mr. Gray");
        addItem(ModItems.DISGUISED_GRAY_SPAWN_EGG, "Disguised Gray Spawn Egg");
        add("message.hollowfrontier.disguised_gray.liked", "The town liked Mr. Gray. Folks look at you sideways now.");
        add("message.hollowfrontier.disguised_gray.tomorrow", "Only one a day. I am told that is how a regular behaves.");
        add("message.hollowfrontier.disguised_gray.traded", "A fair trade. Do not drink the shiny one. That is a joke. Do not drink it.");
        addBlock(ModBlocks.SAUCER_HULL, "Saucer Hull");
        addBlock(ModBlocks.SAUCER_WRECKAGE, "Saucer Wreckage");
        addBlock(ModBlocks.GRAY_CACHE, "Gray Cache");
        addItem(ModItems.ALIEN_ALLOY_PLATING, "Alien Alloy Plating");
        addItem(ModItems.DAMAGED_GRAVITY_DRIVE, "Damaged Gravity Drive");
        addItem(ModItems.BEAM_EMITTER, "Beam Emitter");
        addItem(ModItems.LENS_ARRAY, "Lens Array");
        addItem(ModItems.NAVIGATION_CORE, "Navigation Core");
        add("tooltip.hollowfrontier.alien_alloy_plating", "Hull plating off a Gray saucer. Lighter than tin, harder than steel.");
        add("tooltip.hollowfrontier.damaged_gravity_drive", "What held a saucer up, cracked in the crash. Phlogiston and starsteel might mend it.");
        add("tooltip.hollowfrontier.beam_emitter", "The eye of a saucer's lifting beam.");
        add("tooltip.hollowfrontier.lens_array", "Rows of tiny lenses that once focused something hot.");
        add("tooltip.hollowfrontier.navigation_core", "A saucer's sense of where it is. It still turns toward the poles.");
        add("message.hollowfrontier.abduction.missing_time", "You come to on the ground, far from where you were. You can't account for the last few hours, and there's an itch behind your ear.");
        add("commands.hollowfrontier.ufo.flyover", "A saucer is coming over.");
        add("commands.hollowfrontier.ufo.abduction", "A saucer is coming to stop overhead.");
        add("commands.hollowfrontier.ufo.mothership", "The Mothership is coming down over you.");
        add("commands.hollowfrontier.ufo.leash.broken", "The Grays' leash is broken in this world.");
        add("commands.hollowfrontier.ufo.leash.whole", "The Grays' leash holds in this world.");
        // Phlogiston tech (13b): the cells, the guns, the mended drive
        addItem(ModItems.EMPTY_PHLOGISTON_CELL, "Empty Phlogiston Cell");
        addItem(ModItems.PHLOGISTON_CELL, "Phlogiston Cell");
        addItem(ModItems.RAY_PISTOL, "Ray Pistol");
        addItem(ModItems.PHLOGISTON_RIFLE, "Phlogiston Rifle");
        addItem(ModItems.GRAVITY_DRIVE, "Gravity Drive");
        add("tooltip.hollowfrontier.phlogiston_cell", "Charge: %s / %s");
        add("tooltip.hollowfrontier.phlogiston_gun", "Draws %s charge a shot from the Phlogiston Cells you carry");
        add("tooltip.hollowfrontier.phlogiston_gun.pierce", "The beam goes through up to %s creatures");
        add("tooltip.hollowfrontier.gravity_drive", "What holds a saucer up, mended. It wants to rise.");
        add("message.hollowfrontier.no_charge", "No charge left in your Phlogiston Cells");
        add("subtitles.hollowfrontier.phlogiston_gun.fire", "Phlogiston gun fires");
        add("book.hollowfrontier.codex.grays", "V. The Grays");
        // The Prototype Saucer (13c): the item, the craft, its keys and its gauge
        addItem(ModItems.PROTOTYPE_SAUCER, "Prototype Saucer");
        addEntityType(ModEntities.PROTOTYPE_SAUCER, "Prototype Saucer");
        add("tooltip.hollowfrontier.prototype_saucer.charge", "Phlogiston: %s/%s");
        add("tooltip.hollowfrontier.prototype_saucer.fitted", "Fitted: %s");
        add("tooltip.hollowfrontier.prototype_saucer.no_fittings", "No fittings yet: use a beam emitter, lens array, navigation core, cloaking device or cargo hold on the saucer");
        add("tooltip.hollowfrontier.prototype_saucer.use", "Set it down on the ground and climb in. W/S/A/D fly, jump climbs, sprint descends; V heat laser, B tractor beam, H cargo hold. Land, get out and sneak-use it bare-handed to fold it up. Only you and your team can fit it or fold it.");
        add("key.hollowfrontier.saucer_laser", "Saucer: Heat Laser (hold)");
        add("key.hollowfrontier.saucer_beam", "Saucer: Tractor Beam (hold)");
        add("gamerule.hollowfrontier.saucer_beams_players", "Saucer tractor beams lift players");
        add("subtitles.hollowfrontier.prototype_saucer.hum", "Saucer hums");
        add("message.hollowfrontier.saucer.no_room", "There's no room to set the saucer down here.");
        add("message.hollowfrontier.saucer.fitted", "Fitted the %s.");
        add("message.hollowfrontier.saucer.already_fitted", "It already has a %s.");
        add("message.hollowfrontier.saucer.fold_aloft", "Land it before folding it up.");
        add("message.hollowfrontier.saucer.fold_crewed", "Everyone has to get out before it folds up.");
        add("message.hollowfrontier.saucer.refuelled", "The saucer draws phlogiston from what you carry.");
        add("message.hollowfrontier.saucer.no_phlogiston", "No phlogiston: carry a Phlogiston Cell or a phlogiston shard, or use one on the saucer");
        add("message.hollowfrontier.saucer.lift_off", " · Jump to lift off");
        add("message.hollowfrontier.saucer.gauge", "Phlogiston %s %s · Alt %s");
        add("message.hollowfrontier.saucer.heat", " · Laser heat %s%%");
        add("message.hollowfrontier.saucer.heat_over", " · Laser OVERHEATED (%s%%)");
        add("message.hollowfrontier.saucer.bearing", " · %s %sm %s");
        add("message.hollowfrontier.saucer.bearing_none", " · %s: no bearing");
        add("message.hollowfrontier.saucer.gateway", "Gateway");
        add("message.hollowfrontier.saucer.home", "Home");
        add("message.hollowfrontier.saucer.overheated", "The heat laser has overheated: let it cool.");
        add("message.hollowfrontier.saucer.struggled_free", "You struggle free of the beam.");
        // What the frontier makes of itself (15c): freed men, the ruins restored, the waystones, the aftermath news
        add("message.hollowfrontier.freed.no_town", "Out here? There's nothing out here for a man. Find me a town and I'll make something of myself in it.");
        add("message.hollowfrontier.freed.offer", "Whatever had hold of me is gone. I'd wear a star for this town, for a gold coin to start me off. Or I'd just go home and dig.");
        add("message.hollowfrontier.freed.hint", "(Use him with %s gold coin in hand to swear him in as a deputy, or sneak and use him to send him home.)");
        add("message.hollowfrontier.freed.hired", "I'll keep the peace here. Nobody's going to make me do otherwise again.");
        add("message.hollowfrontier.freed.home", "Home, then. I've a claim out past the creek, if nobody's jumped it.");
        add("message.hollowfrontier.freed.wanted", "Take a job from you? You're on the posters yourself, friend.");
        add("message.hollowfrontier.restoration.not_after", "The fountain is still. The Kindled won't build again while the Grays hold their leash on the frontier.");
        add("message.hollowfrontier.restoration.not_ally", "The fountain is still. Only an ally of the Kindled can wake it.");
        add("message.hollowfrontier.restoration.done", "The ruin stands whole again. There's nothing left to mend.");
        add("message.hollowfrontier.restoration.short", "Stage %3$s needs %1$s phlogiston shards and %2$s starsteel ingots.");
        add("message.hollowfrontier.restoration.stage.1", "The fountain drinks the phlogiston and the starsteel. With a sound like ice settling, a fallen tower climbs back out of its rubble.");
        add("message.hollowfrontier.restoration.stage.2", "Light runs along the sanctum's windows, and a second tower stands whole. The Kindled stop what they're doing to watch.");
        add("message.hollowfrontier.restoration.stage.3", "The last tower rises, the paving closes over, and on its dais at the plaza's edge the old waystone wakes.");
        addBlock(ModBlocks.WAYSTONE, "Waystone");
        String[] waystones = {"Aurvang", "Hvitholm", "Skeldra", "Norrhal", "Isvarra", "Kaldmere", "Frostvik", "Sunnhal", "Vindreth", "Eldmark", "Glimra", "Thulvan"};
        for (int i = 0; i < waystones.length; i++) {
            add("waystone.hollowfrontier.name." + i, waystones[i]);
        }
        add("waystone.hollowfrontier.name.numbered", "%s %s");
        add("waystone.hollowfrontier.gateway", "Polar Gateway (%s, %s)");
        add("gui.hollowfrontier.waystone.fare", "Fare: %s phlogiston shard");
        add("message.hollowfrontier.waystone.fare", "The waystone wants %s phlogiston shard to carry you.");
        add("message.hollowfrontier.waystone.arrived", "You step out at %s.");
        add("message.hollowfrontier.waystone.alone", "The waystone hums, but there's nowhere yet for it to send you.");
        add("book.hollowfrontier.codex.after", "VI. After");
        add("gazette.hollowfrontier.aftermath.deputy.title", "NEW STAR IN TOWN");
        add("gazette.hollowfrontier.aftermath.deputy.body", "A man who rode with the gangs until the night the sky went quiet has pinned on a deputy's star near (%2$s, %3$s). %1$s paid his first coin. The sheriff says he's the hardest worker in the office, and the only one who knows where the other gangs hole up.");
        add("gazette.hollowfrontier.aftermath.homecoming.title", "PRODIGAL RETURNS");
        add("gazette.hollowfrontier.aftermath.homecoming.body", "Another of the drifters has put his gun away and settled near (%2$s, %3$s), thanks to %1$s. He has taken up prospecting. Neighbours report that he is polite, quiet, and very bad at it.");
        add("gazette.hollowfrontier.aftermath.ruin.title", "LIGHTS UNDER THE ICE");
        add("gazette.hollowfrontier.aftermath.ruin.body", "Travellers back from the Hollow Earth say the old crystal ruins near (%2$s, %3$s) are being built up again, tower by tower. The Lantern Men who live there are said to be pleased. Nobody can say who is paying for it.");
        add("gazette.hollowfrontier.aftermath.waystone.title", "THE OLD ROADS OPEN");
        add("gazette.hollowfrontier.aftermath.waystone.body", "A standing stone in the crystal ruins near (%2$s, %3$s) has woken, and is said to carry a traveller from one ruin to the next, or up to the polar gateways, for one phlogiston shard. The railroad has been asked for comment, and is said to be worried.");
        add("gazette.hollowfrontier.aftermath.general.1.title", "QUIET SKIES");
        add("gazette.hollowfrontier.aftermath.general.1.body", "It has been weeks since anyone saw lights over the plains. Ranchers report their cattle whole, their wheat standing, and their nerves much improved.");
        add("gazette.hollowfrontier.aftermath.general.2.title", "DRIFTERS COME HOME");
        add("gazette.hollowfrontier.aftermath.general.2.body", "Men who went missing years ago are turning up at their families' doors, thin and puzzled, with no memory of where they've been. Most are just glad to be home. Their wives have questions.");
        add("gazette.hollowfrontier.aftermath.general.3.title", "BLACK COATS GONE");
        add("gazette.hollowfrontier.aftermath.general.3.body", "The gentlemen in black dusters who asked so many questions last season have not been seen since. Their hotel bill remains unpaid.");
        add("gazette.hollowfrontier.aftermath.general.4.title", "FEWER GANGS, SAYS SHERIFF");
        add("gazette.hollowfrontier.aftermath.general.4.body", "The county reports fewer gangs on the roads than in any year since the railroad came. Those still riding are said to be doing it the old-fashioned way, out of plain greed.");
        // The captives (20): the pens, the procedure, letters home and men brought in alive
        addBlock(ModBlocks.IMPLANT_RIG, "Implant Rig");
        addBlock(ModBlocks.KINDLED_SEAL, "Kindled Seal");
        addBlock(ModBlocks.SEAL_SOCKET, "Seal Socket");
        addItem(ModItems.LETTER_HOME, "Letter Home");
        add("tooltip.hollowfrontier.letter_home.to", "Addressed to %3$s, near %1$s, %2$s");
        add("tooltip.hollowfrontier.letter_home.any", "Addressed to \"anyone, any town\"");
        add("tooltip.hollowfrontier.letter_home.hint", "Bring it to that town's Bounty Board or sheriff");
        add("tooltip.hollowfrontier.bounty.captive", "Held by the Grays, deep below");
        add("bossbar.hollowfrontier.procedure", "Procedure");
        add("message.hollowfrontier.procedure.started", "Something hums to life over the operating table. The Grays have started on %s.");
        add("message.hollowfrontier.procedure.stopped", "The rig whines down and goes dark. The one on the table is still themselves.");
        add("message.hollowfrontier.procedure.finished", "The straps fall open. %s sits up, and their eyes are wrong.");
        add("message.hollowfrontier.pens.cleared", "The last of the pen's keepers is down. Nobody's coming back for this place.");
        add("message.hollowfrontier.captive.strapped", "I can't move! Stop the machine first, stop it!");
        add("message.hollowfrontier.captive.letter", "Here. I wrote it in the dark, on my knee. Get it home for me, would you?");
        add("message.hollowfrontier.captive.escort", "That's my face on your paper, ain't it. ...Fine. A cell in town beats a cold table.");
        add("message.hollowfrontier.captive.escort_hint", "%s goes with you in irons. Walk him to the board or the sheriff of the town that posted him.");
        add("message.hollowfrontier.captive.recognised", "One of the faces behind these bars is on the poster you're carrying: %s. The Grays got to him first.");
        add("message.hollowfrontier.captive.wrong_town", "%s was posted in another town. Take him there.");
        add("message.hollowfrontier.captive.turned_in", "The sheriff walks %s back to the cells. Brought in alive: %s gold, in full.");
        add("message.hollowfrontier.letter_home.no_town", "This letter is for a town's board or sheriff.");
        add("message.hollowfrontier.letter_home.wrong_town", "Wrong town. This one is for the town near %s, %s.");
        add("message.hollowfrontier.letter_home.delivered", "Word from someone the town had given up on. They pay you %s gold for it, and somebody cries.");
        add("message.hollowfrontier.letter_home.pardoned", "The sheriff decides he never saw your face on that board.");
        add("message.hollowfrontier.bounty.already_captured", "%s has already been brought in alive.");
        add("gui.hollowfrontier.bounty_board.captured", "CAPTURED");
        add("jei.hollowfrontier.info.letter_home", "Handed over by a captive freed from a Gray pen in the Hollow Earth. Bring it to the Bounty Board or sheriff of the town it's addressed to for 3 gold, and your heat cleared.");
        // The Mothership's return (15b): the rematch fittings, the hold, the owner lock
        addItem(ModItems.CLOAKING_DEVICE, "Cloaking Device");
        addItem(ModItems.CARGO_HOLD, "Cargo Hold");
        add("tooltip.hollowfrontier.cloaking_device", "Bends the light round a saucer. It hums at night, and does nothing at all by day.");
        add("tooltip.hollowfrontier.cargo_hold", "A Gray hold, roomier inside than out. Fits under a saucer's deck.");
        add("jei.hollowfrontier.info.cloaking_device", "Given up by a Mothership called after the first (its rematches), every other time. Fit it to a Prototype Saucer: at night, with a pilot aboard, nobody else can see the craft or its crew, and mobs lose track of them. The Grays' own craft can still see it.");
        add("jei.hollowfrontier.info.cargo_hold", "Given up by a Mothership called after the first (its rematches), every other time. Fit it to a Prototype Saucer for 9 slots of storage, opened from aboard with the hold key (H). What's in it stays with the saucer when it's folded.");
        add("key.hollowfrontier.saucer_hold", "Saucer: Open Cargo Hold");
        add("message.hollowfrontier.saucer.cloaked", " · Cloaked");
        add("message.hollowfrontier.saucer.no_hold", "This saucer has no cargo hold.");
        add("message.hollowfrontier.saucer.hold_outside", "Climb aboard to open the hold.");
        add("message.hollowfrontier.saucer.not_yours", "This saucer belongs to %s.");
        add("entity.hollowfrontier.mothership.rematch", "%s (Return %s)");
        add("message.hollowfrontier.mothership.returns", "The Mothership is back over the beacon, and it's brought more drones than last time.");
        add("commands.hollowfrontier.ufo.rematch", "Motherships fallen in this world: %s. The next one comes %s steps harder.");
        // The Mothership (13d): the beacon, the ship, its drones, and the leash breaking
        addBlock(ModBlocks.PHLOGISTON_BEACON, "Phlogiston Beacon");
        addEntityType(ModEntities.MOTHERSHIP, "The Mothership");
        addEntityType(ModEntities.GRAY_DRONE, "Gray Drone");
        add("subtitles.hollowfrontier.mothership.hum", "Mothership throbs");
        add("subtitles.hollowfrontier.mothership.death_ray_charge", "Death ray charges");
        add("subtitles.hollowfrontier.mothership.death_ray_fire", "Death ray strikes");
        add("message.hollowfrontier.phlogiston_beacon.not_here", "The beacon is silent. Whatever it calls is only in the skies of the Overworld.");
        add("message.hollowfrontier.phlogiston_beacon.not_night", "The beacon is silent. It is heard only at night.");
        add("message.hollowfrontier.phlogiston_beacon.no_sky", "The beacon is silent. It needs the open sky above it.");
        add("message.hollowfrontier.phlogiston_beacon.already", "The beacon is already calling.");
        add("message.hollowfrontier.phlogiston_beacon.called", "The beacon's light goes up into the night. Something up there answers.");
        add("message.hollowfrontier.mothership.coming", "The stars over the beacon go out, one by one. It's coming down.");
        add("message.hollowfrontier.mothership.arrives", "The Mothership hangs over the beacon. Its drones are waking.");
        add("message.hollowfrontier.mothership.abduction", "The Mothership raises its shield and sends out its drones. Its beam is warming.");
        add("message.hollowfrontier.mothership.exposed", "The Mothership comes down low, its belly opening on a burning core. Its shield is up again.");
        add("message.hollowfrontier.mothership.shield_down", "The Mothership's shield fails.");
        add("message.hollowfrontier.mothership.leaves", "With no one left to fight, the Mothership rises into the dark and is gone.");
        add("message.hollowfrontier.mothership.falls", "The Mothership falls burning out of the sky.");
        add("title.hollowfrontier.leash_broken", "The Leash Is Broken");
        add("title.hollowfrontier.leash_broken.subtitle", "The implants have gone dark");
        add("message.hollowfrontier.leash_broken.1", "Across the frontier, men stop where they stand and put a hand behind one ear. Whatever whispered there is quiet.");
        add("message.hollowfrontier.leash_broken.2", "The men in black dusters walk off into the dust and don't come back. No one is watching the sky for the Grays any more.");
        add("message.hollowfrontier.leash_broken.3", "The outlaws who still ride now ride by their own choice. What the frontier makes of itself is its own again.");
        // The lost Symmes expedition's journal, on a lectern at its camp (9b3: HollowBlueprints.journal)
        add("book.hollowfrontier.symmes_journal.1", "Symmes Polar Expedition, 1889.\n\nWe left the last of the pack ice on the fourth of June and found the sea running warm. The Captain says the old Colonel was right after all.");
        add("book.hollowfrontier.symmes_journal.2", "The rim of the world curves away beneath us here. The compasses spin like tops. Nine days going down the great hole, the sun behind us shrinking to a star.");
        add("book.hollowfrontier.symmes_journal.3", "Then a second sun, low and green-gold, that never sets. Ferns as tall as a church steeple. Beasts we have no names for. We have made camp by good water.");
        add("book.hollowfrontier.symmes_journal.4", "Tomkins swears the pale stones up on the moors were cut by hands, and carved with letters older than Babel. He copied some down. None of us can read them.");
        add("book.hollowfrontier.symmes_journal.5", "Lights in the sky again, though here there is no night to see them by. Something small and silver hung over the camp an hour, turning to look at each of us in turn.");
        add("book.hollowfrontier.symmes_journal.6", "Tomkins went after the lights. We found his boots, and a ring burned into the grass. The men will not sleep. We start back for the hole at first light, if it lets us.");
        add("book.hollowfrontier.symmes_journal.7", "If you find this: the way home is the way you came, up through the ice. Keep the cold ahead of you.\n\nDo not follow the lights.\n\n  E. Hartwell");
        add("block.hollowfrontier.bed.no_sleep_hollow", "The inner sun never sets: you can't sleep here, but your bed will hold your place");
        add("dimension.hollowfrontier.hollow_earth", "The Hollow Earth");

        add("key.category.hollowfrontier.main", "Hollow Frontier");
        add("key.hollowfrontier.reload", "Reload Gun");

        add("death.attack.hollowfrontier.bullet", "%1$s was shot by %2$s");
        add("death.attack.hollowfrontier.bullet.item", "%1$s was shot by %2$s using %3$s");
        add("death.attack.hollowfrontier.bullet.player", "%1$s was shot while fighting %2$s");

        // Optional compat (phase 14): JEI and Jade
        add("jei.hollowfrontier.crushing.steam", "%s mB of steam");
        add("jei.hollowfrontier.info.boiler", "Boils water into steam while it burns furnace fuel. Water comes in by bucket, by pipe, or from a water source block touching it (a well or a trough). Steam goes out to pipes, tanks and machines beside it.");
        add("jei.hollowfrontier.info.copper_pipe", "Carries steam. Joins up with boilers, tanks, crushers and other pipes, and with other mods' tanks.");
        add("jei.hollowfrontier.info.steam_tank", "Holds steam for a network of pipes. Canisters fill from it, and an airship moored over one takes steam from it.");
        add("jei.hollowfrontier.info.pressure_gauge", "Reads the highest steam pressure beside it. Use it to see the reading; a comparator gets 0 to 15 from it.");
        add("jei.hollowfrontier.info.steam_canister", "Use it on a boiler, pipe or tank to fill it. Steam tools (the Steam Drill, the Polar Resonator) draw on canisters anywhere in your inventory.");
        add("jei.hollowfrontier.info.outlaw_badge", "Carried by an Outlaw Boss. Take a Wanted Poster from a Bounty Board, bring the boss down, and turn the badge in at the board for gold coins.");
        add("jei.hollowfrontier.info.brass_star", "Never made, only found: every frontier town's sheriff keeps a cat that wears the only one.");
        add("jei.hollowfrontier.info.strange_skull", "Dug up from the strange grave on a town's Boot Hill.");
        add("jei.hollowfrontier.info.gray_tech_fragment", "Left by the Grays' work: tagged megafauna, mutilated cattle, Gray Wardens and drones, and the Gray labs under the crust.");
        add("jei.hollowfrontier.info.kindled_sigil", "Given by the Kindled to an ally, and sold by them after. With Curios installed, it can be worn as a necklace.");
        add("jei.hollowfrontier.info.sunstone_shard", "Left by the Sun Archon, and found in the Sun Temple.");
        add("jei.hollowfrontier.info.phlogiston_core", "Left by the Sun Archon when it falls. The heart of the Phlogiston Beacon.");
        add("jei.hollowfrontier.info.alien_alloy_plating", "Salvage: break the wreckage at a crash site, on the frontier or under the crust. Eight of them, round a mended gravity drive, make a Prototype Saucer.");
        add("jei.hollowfrontier.info.damaged_gravity_drive", "Salvage: found in some crash sites' wreckage, not all. Mend it with phlogiston and starsteel.");
        add("jei.hollowfrontier.info.beam_emitter", "Salvage: found in some crash sites' wreckage, not all. Fit it to a Prototype Saucer for its tractor beam.");
        add("jei.hollowfrontier.info.lens_array", "Salvage: found in some crash sites' wreckage, not all. Fit it to a Prototype Saucer for its heat laser.");
        add("jei.hollowfrontier.info.navigation_core", "Salvage: found in some crash sites' wreckage, not all. Fit it to a Prototype Saucer for bearings home and to the nearest Polar Gateway.");
        add("config.jade.plugin_hollowfrontier.steam_machine", "Steam machines");
        add("config.jade.plugin_hollowfrontier.vehicle", "Airships, handcars and saucers");
        add("jade.hollowfrontier.boiler.burning", "Firing: %ss of fuel left");
        add("jade.hollowfrontier.boiler.cold", "Cold: needs fuel");
        add("jade.hollowfrontier.crusher.progress", "Crushing: %s%%");
        add("jade.hollowfrontier.gauge", "Pressure: %s%%");
        add("jade.hollowfrontier.airship.coal", "Coal: %s");
    }

    private void enchantment(String name, String title, String description) {
        add("enchantment." + HollowFrontier.MODID + "." + name, title);
        add("enchantment." + HollowFrontier.MODID + "." + name + ".desc", description);
    }
}
