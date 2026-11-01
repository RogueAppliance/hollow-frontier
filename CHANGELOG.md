# Changelog

## Unreleased

- **Polish (phase 31).**
  - **Airships have owners.** Whoever sets one down owns it. Other players can still ride and stoke it, but only the owner (or their team) can break it, and shooting or blowing up someone else's airship does nothing to it. Fire, lava and mobs still can.
  - **A broken airship keeps its coal and steam.** The folded airship carries them (its tooltip shows how much) and puts them back when you lay it out again.
  - **Frontier folk look sharper.** Hat brims curl up at the sides. Bandanas are proper cloth over the face, or round the neck, with a tip hanging at the chin. The teacher's dress and the Kindled's robes now hang from the belt and swing out behind as they walk or ride.
  - The release jar no longer carries the dev-only screenshot and trailer rigs, and the build is ready for NeoForge's next tag clean-up.

- **Modpack friendliness (phase 30).**
  - Other mods' pipes and storage can now feed the Boiler and Crusher and take the dust out, using the same sides as hoppers.
  - Steam is tagged `c:steam`. Other mods' steam runs our machines, and the canister, airship and handcar can fill up from other mods' tanks.
  - Polar Gateways now spawn in modded snowy biomes too, but not on beaches, oceans, rivers or bare peaks. Mining towns now spawn in modded hill biomes.
  - The Outlaw Boss, Sun Archon and Mothership are tagged `c:bosses`. Mob catchers and teleporters from other mods leave them alone, along with the townsfolk, the Kindled and the men in black.
  - Any mod's iron, gold, copper or starsteel dust smelts into ingots. The Crusher takes any cobblestone and any gravel.
  - New config `hollowfrontier-mysteries-server.toml` switches the night mysteries, cattle mutilations, crop circles, and the men in black's visits and hunts on or off.

- **Dust devils (phase 29).** On clear days, whirls of dust wander over the deserts, badlands and savannas, coloured by the ground they cross. The dust really goes round: faster at the foot than at the top, winding into spiral bands, with a kink that snakes up the column.
  - Twelve blocks tall. They drag what's on the ground toward them from up to seven blocks away.
  - They pick up dropped items and anything up to the size of a sheep or a pig (chickens, rabbits, snakes, cats, wolves), carry them round and up, and fling them out the top. Creatures float down unhurt.
  - Walk into one standing up and it lifts you about six blocks, spins you hard and sets you down gently. **It takes your Cowboy Hat** and drops it downwind. Sneak to keep your feet and your hat, and to stop it dragging you in.
  - Horses rear in them and often throw their riders. Shod horses don't.
  - Where one dies down it leaves what it swept up: brush, feathers, a coin now and then. Suspicious sand it passes over glints for a moment.
  - **Ash Devils** in the Ember Badlands: grey and full of cinders, they set fire to what they carry and to the ground they cross.
  - The townsfolk talk about the one that came through today, and about your missing hat. The Gazette has a few things to say, and the men in black have a new excuse for everything.
  - New advancement: Hold On to Your Hat. Config in `hollowfrontier-dust-devils-server.toml`; ops can call one with `/hollowfrontier dustdevil [ash]`.
- **Fixed:** steam showed as the missing texture in Jade's and JEI's tank bars. It's a pale white now.

- **The Hollow Earth, overhauled (phases 25-28).**
  - **Rebuilt sites.** The Kindled ruins are dark stone now (deepslate, tuff, crystal and starsteel), weathered, with snow and moss, and they stand out against the Highlands' snow. The henge has a ritual floor, seats and offering niches. Behind the great gate's iron door are an antechamber, a hall of columns, a tomb, a vault and an ossuary. The temple has a scriptorium and a vestry.
  - **The Grays' sites:** the abandoned lab has an annex (bunks, control room, specimen store, decon airlock). The live station has an ops block, a landing pad with a parked probe, and a lit fence. The holding pens have an evidence locker full of the captives' belongings.
  - **The crystal city** has houses, a market row, a library tower and a fallen spire round its plaza. **The Sun Temple** has a sundial plaza, an avenue of pillars, sun priests at the stair, and a throne hall with a light well. **The Symmes camp** has a walk-in gondola, spilled cargo, and a dig uncovering a Kindled carving.
  - **Dungeons under the sites.** Below the gate's tomb, the **Kindled Catacombs** end at a sealed vault: put phlogiston lamps on the sockets the mural shows to open it. Under one Gray lab in three lies a **buried Gray complex**, with cells holding captives, cold storage, a reactor, a Warden spawner and the data core. Behind the Sun Archon's throne, the **undercroft** has lava-lit treasuries and trap corridors, and the hoard is down there now.
  - **New sites in every biome:** an overgrown ziggurat with an idol chamber, and a tar pit full of megafauna bones (Primordial Jungle). An abandoned phlogiston mine with a crystal cavern below, and a Kindled observatory (Crystal Steppes). A ruined starsteel foundry (Ember Badlands). Sky Shrines with a waystone on the floating islands, and Gray relays hanging from the crust.
  - **Better loot.** Each room has its own table, with a chance at rare finds and a jackpot. **Eight relics** (Kindled Idol, Crystal Tablet, Sunstone Circlet, Starsteel Mask, Gray Specimen Jar, Expedition Sextant, Ivory Carving, Star Chart) sell to a town's prospector for gold coins. **Explorers' maps** lead to a Sun Temple, the Gray pens, the crystal city or the old foundry. Gun enchantment books turn up too.
  - **The Concavian armor trim**, and three **Hollow pottery sherds** (sun, crystal, watcher) brushed out of suspicious sand and gravel at the ruins, the dig, the tar pit and the ziggurat.

- **Towns, lived in (phase 24).**
  - **Every town has a name**, like "Coyote Springs", on a welcome arch at the end of each street ("WELCOME TO... POP. 212"). The Gazette, bank notices and Letters Home use it too.
  - **No more gaps:** buildings now stand shoulder to shoulder, with side yards between them: fences and gates, rain barrels, firewood, washing on the line.
  - **The railroad runs on out of town** from the depot both ways, ending at a buffer stop, a boxcar with freight in it, or a water tank.
  - **New buildings:** a bathhouse and barber (a hot bath sobers you up), the Gazette office with its printing press, and a ranch on the edge of town with a barn, a corral and cattle.
  - **Lamps that light at dusk:** street and porch lamps come on one by one as night falls and go out in the morning. Craft them with lanterns and a daylight detector.
  - **Chairs and tables** in oak, spruce and dark oak. Sit in the chairs. The towns are furnished with them now.
  - **New townsfolk:** a doctor, a schoolteacher, a preacher, a postmaster, a gunsmith and the Gazette's editor, each with their own look, things to say and wares.
  - **Townsfolk keep a day:** work by day, the saloon in the evening, home to bed at night. A word wakes them.
- **Each townsperson sells their own trade's goods.** Only the gunsmith sells guns now; the banker changes coin; the store, smithy, livery and prospector each stock what they'd really sell. Townsfolk you've already traded with keep their old wares.
- **Fixed** (towns, after the second playtest): lamps hanging in mid-air in barns and other buildings (they hang on chains or beams now), chimney fires showing (wooden chimney pots hide them), stairs that came up straight into a bedroom, the grand saloon's lopsided window, pots on counters that hid the shopkeepers, the courthouse's doorways cut too tall, and the town lamp looking wrong by day.
- **Fixed** (towns): the courthouse's front doors had a gap between them, the jail's bars stopped short of the ceiling, the hotel clerk was hidden behind a pot, and the grand hotel's rooms had no front walls.

- **Towns, built properly (phase 23).**
  - **Level towns:** towns now only grow on dry, fairly level ground, and the whole town sits on one level. No more streets climbing hills between buildings perched on dirt cliffs.
  - **Furnished buildings:** every building is fitted out for its trade. The general store has shelves of goods and a dummy in a duster, the bars have bottles behind them, the gunsmith keeps guns in a glass case, the sheriff has a gun locker, the forge has its tools, and the livery has its tack. Homes get kitchens, tables and rugs, and a back yard with a woodpile and an outhouse.
  - **Shop signs** over every door, plus shutters and cornices on the false fronts.
  - **New buildings:** the gunsmith, the doctor ("teeth pulled"), the post and telegraph office, a two-storey homestead with chickens, and a little red schoolhouse with its bell.
  - **A livelier square:** shade trees with benches, market stalls under striped awnings, a stagecoach at its stop, and an old cannon.
  - **Better loot:** each trade has its own chest. The doctor's always holds a healing potion, the gunsmith's has powder and ammunition, the post office has letters and maps, and there's freight at the depot. House chests are fuller, too.

- **Gun enchantments (phase 22).** Guns now take enchantments at the enchanting table and the anvil, with six of their own:
  - **Rifling** (I-IV): each shot hits harder, up to +60%.
  - **Quick Load** (I-III): each chamber loads faster, up to more than twice as fast.
  - **Deadeye** (I-III): headshots do even more damage. It goes on every gun, the phlogiston guns too.
  - **Choke** (I-III, shotgun only): keeps the buckshot together.
  - **Dry Powder:** no more misfires in the rain.
  - **Frugal** (I-III): loading sometimes costs no cartridge or cap.
  - **Vanilla's Unbreaking and Mending** now work on guns too.
  - **Infinity** goes on the revolver, rifle and shotgun: loading costs nothing, though like a bow you still need one cartridge and one cap on you. It can't share a gun with Frugal or Mending.
  - **Enchanted guns shine** like any other enchanted item, in your hand, on the hotbar and held by others.
  - **Where to find them:** the enchanting table, librarians, books in outlaw camps and blacksmiths' chests (an outlaw camp's spare revolver may already be enchanted), and the town blacksmith, who sells one gun enchantment book for gold. Blacksmiths you've already traded with keep their old wares.
- **Less smoke in your eyes.** Your own gun's smoke now puffs from the end of the barrel, low and to the right, instead of right where you're aiming, and there's less of it. Other players still see a cloud at your gun, a lighter one than before.
- **Fixed:** holding right-click with a gun made it bob up and down, and a held reload kept firing each chamber as soon as it was loaded. Now holding right-click fires until the gun is empty, then reloads all the way without bobbing. Let go and click again to fire mid-reload.
- **Headshots.** A shot to the head now hits harder: double for the Plains Rifle and Phlogiston Rifle, and half again for the revolver, the shotgun (pellet by pellet) and the ray pistol. You'll see sparks fly and hear a "ding" that only you get. Outlaws and Grays don't get headshots on you.
- **Defending a town no longer makes you wanted.** While a gang is raiding a town, a stray shot or pellet that clips a townsperson or a deputy is forgiven. Killing one still counts.
- **Faster steam networks.** Pipes, tanks and boilers now remember their neighbours instead of looking them up every tick, so big steam builds cost the server less. Also, waking up from Missing Time? no longer causes a brief server hitch.
- **Fixed:** the faces sketched on Wanted Posters flickered as you moved. Also fixed: at the card table, a dealer with a long name ("Miss Belle", "Clementine") ran the title into your purse.
- **Glow under shaders (Iris).** With a shader pack on, the mod's glowing parts now glow as vanilla's do: the saucers' lights and beam cores, the Grays' implants, phlogiston, and every other lit-up part of the mod's creatures. Before, a pack lit them like ordinary surfaces, so they went dim at night. Iris stays optional.
- **Frontier humour: Grays in disguise (phase 21c).** Once the leash is broken, a Gray stranded on the frontier now and then turns up in town of an evening, mostly at the saloon, in a duster and a hat far too big for it. It holds a beer it never drinks and tries very hard to talk cowboy. The townsfolk half-notice.
  - **Expose it** with a Sunstone Lens or a fist: the hat comes off, the town panics, and it runs, dropping a Gray tech fragment.
  - **Or leave it be:** after three visits it stays for good as the saloon's regular, **Mr. Gray**, on his own bar stool. Once a day he'll trade a Phlogiston Cell for a Sarsaparilla. The town gets fond of him, so exposing him will cost you.
  - **Advancements:** Not From Around Here and Regular Customer.
- **Frontier humour: a night at the saloon (phase 21b).**
  - **Slurring:** from Tipsy II, what folks say to you and your own Gazette come out a little slurred, and you hiccup now and then. It works in every language.
  - **Moonshine sightings:** moonshine at Tipsy III may show you an airship crossing the night sky. Nobody else sees it, and the barmaid has heard it all before.
  - **Missing Time?:** now and then, when a heavy drunk wears off in town, you come to in a bed, on a hay bale or in a horse trough nearby, an hour gone, wearing somebody else's hat. It's never outside the town or anywhere dangerous, and servers can turn it off in `hollowfrontier-saloon-server.toml`. It comes with a hidden advancement.
  - **Doc's Snake Oil** tells you what it's doing to you, every time. It's always wrong.
  - **Bar brawls:** townsfolk drink at night now, and two who've had a few may come to blows. It's fists only and nobody gets hurt, but the barmaid shouts, the loser gets thrown out the door, and the piano stops playing until it's over. The piano also stops for gunfights and raids.
  - **Bad likeness:** your own poster on the Bounty Board is drawn by an artist who's never seen you. The sheriff has noticed.
  - **13 new lines of talk.**
- **The captives (phase 20).** Deep in the Hollow Earth, the Grays hold men taken from the frontier in **Gray pens**: a cell block and an operating room, kept by Gray Wardens and sentinels. They're rarer than the field stations.
  - **The procedure:** come near and the Grays start on the captive strapped to the table, with a "Procedure" bar that fills over a minute. Kill the Wardens working the rig, or break the rig, to stop it. If it finishes, the captive gets up implanted, armed and hostile.
  - **Freeing them:** press the button by a cell door and talk to the one inside. They'll tell you how they got there, give you a **Letter Home**, and walk off. A freed outlaw can then be hired or sent home in a town, like the men the leash let go.
  - **Letters Home:** bring one to the Bounty Board or sheriff of the town it's addressed to for 3 gold, and the town forgets your heat.
  - **Brought in alive:** carry a Wanted Poster near a pen and its man may be in one of the cells. Free him and he comes with you in irons, even through a gateway. Walk him to the board or sheriff of the town that posted him for the full reward, and the poster gets a CAPTURED stamp.
  - **After the leash:** there's no procedure. The pens are just held, and clearing their keepers counts as Trespassers.
  - **Advancements:** Jailbreak, Not on My Watch, Special Delivery and ...Or Alive.
- **What the frontier makes of itself (phase 15c).** Once the leash is broken:
  - **Freed outlaws settle down.** Talk to one in a town. Hand him a gold coin and he pins on a deputy's star for that town; sneak and use him and he settles there as a prospector. Anyone with two wanted stars can't hire him.
  - **Restore the Crystal Ruins.** As an ally of the Kindled, bring phlogiston shards and starsteel ingots to a ruin's fountain and use one on it. There are three stages: a fallen tower rises, then another and the sanctum lights up, then the last tower rises, the paving is mended and the ruin's **waystone** wakes. The stages cost 16, 24 and 32 phlogiston shards with 4, 8 and 12 starsteel. Each stage opens new Kindled trades, ending with a Phlogiston Core.
  - **Waystones.** Every woken waystone can carry you to any other, or to any Polar Gateway that's been crossed, for one phlogiston shard. A phlogiston shard used on a crossed gateway's ring takes you back to the waystones. It's the mod's first fast travel.
  - **The Gazette** reports the new deputies, homecomings and restored ruins, and how the frontier is taking the quiet.
  - **The Concavian Codex** gets a last chapter, "After".
  - **Advancements:** A Second Chance, Concavia Rising and Old Roads.
- **The Mothership's return (phase 15b).** Each Mothership called after the first is a step harder, up to four steps: more health, a drone more in every wave, and a death ray that comes more often. The boss bar shows which return it is.
  - **New fittings:** each rematch gives up a saucer fitting, the **Cloaking Device** and the **Cargo Hold** by turns, so two rematches give you both.
  - **Cloaking Device:** at night, with a pilot aboard, nobody else can see your saucer and its crew turn invisible. Mobs hunting you lose track of you. The Mothership and its drones aren't fooled.
  - **Cargo Hold:** 9 slots, opened from aboard with H (rebindable). What's in it stays with the saucer when you fold it.
  - **Your saucer is yours:** only whoever set it down, or their team, can fit parts to it, open its hold or fold it. Anyone can still ride along. A saucer set down before this update belongs to the first player to do one of those things.
  - **Advancements:** Encore and Fully Fitted. Ops can check or set the count with `/hollowfrontier ufo rematch`.
- **Towns grow wheat.** A cart track at the end of every street leads back to a fenced wheat field, and fields can also stand out past the lanes. Each field is four plots round lily-padded water holes, with a scarecrow, hay and a hand cart on the headland. The wheat is ripe from the start, so the Grays always have a field near town, and each field is big enough for a whole crop circle. Each street has one shop lot fewer. Only newly generated towns get the fields.
- **Crop circles land in the middle of the field.** They used to land wherever the search hit the wheat, often at the edge, where the pattern was cut off.
- **Fixed stairs that needed a jump.** The staircases in the hotel, grand hotel, grand saloon and courthouse ended a block short of the floor above. Some also started against a wall, so you had to get on from the side. You can now walk straight up every flight. Only newly generated towns get the fix.
- **Fixed the depot's back door, for real this time.** The step up to the platform stood right outside the door, so you knocked your head on the wall above it and couldn't get out. The door now opens at floor level into a notch in the platform, with the step one block further out. Only newly generated towns get the fix.
- **Removed the frontier music discs.** "Maple Leaf Rag", "Old West Style" and "Spaghetti Western" are gone, along with the Golden Oldies advancement. Shopkeepers no longer sell them and town chests no longer hold them. Hotel lobbies still have a jukebox, now empty, for your own records. The Saloon Piano keeps playing.
- **Fixed blocked doors.** The railroad depot's back door opened straight into the raised platform. There's now a step up. A gold block stood right behind the bank's vault door, and it's been moved into the vault's corner. Only newly generated towns get the fix.
- **Frontier humour: words (phase 21a).** The frontier's dry humour gets more room.
  - **The Gazette's back page:** every edition ends with one column: classifieds, letters to the editor or corrections, 34 pieces in all. Which ones print depends on how far your story has got and on what's been happening nearby.
  - **Gray field notes:** abandoned labs, Gray stations and landing caches now and then hold a book of a Gray observer's notes on cowpokes. There are 16 notes in eight different copies.
  - **Boot Hill epitaphs:** three joke headstones on every Boot Hill, 12 in all, with two new Boot Hill layouts to carry them. The airship pilot's grave stays serious.
  - **The men in black explain things:** asked about a recent sighting, mutilation or crop circle, they have a cover story ready. It's usually Venus.
  - **Reloading under fire:** outlaws now jeer at a player who stops to reload, and have more to say about their own reloading. That's 19 new Chatter lines, 312 in all.
- **The Cowboy Hat looks like a cowboy hat.** When worn it's now a real hat: a tall pinched crown, a dark band with a brass buckle, and a wide brim with curled-up sides, instead of a brown box on the head. It shows on players, armor stands and mobs.
- **Banks (phase 16).** Sneak and use a town's Banker to open your own safe deposit box in that town's bank.
  - **The box:** 18 slots that only you can open. The first visit costs one gold coin, and it's free after that. Anyone with two wanted stars gets turned away.
  - **Interest:** coins in a box earn about 3% a day: 1 copper for every 32 copper's worth, up to 8 copper a day. It's paid when you visit, for up to 7 days since your last visit.
  - **Raids:** when a gang cracks the vault, it also takes a quarter of the coins in every box in town, even the boxes of players who are offline. Shoot the bagman and every coin goes back. Owners hear what happened at once if they're online, or when they next log in or visit. The Gazette reports what the bank lost. The share can be changed with `depositsAtRisk` in the raid config, and 0 turns it off.
  - **Advancements:** Money in the Bank, Compound Interest and Saved the Savings.
- **Blackjack at the saloon (phase 17).** A new **Card Table** (green carpet, paper and planks) stands in new saloons, and you can craft one for your own.
  - **The dealer:** the saloon's barmaid deals, so a table only plays with one nearby. At **Josie's** table the dealer hits soft 17 and the limit is doubled.
  - **Rules:** the dealer stands on 17, a natural pays 3:2, and you can double down on your first two cards. Bets run from 1 to 18 copper coins, and doubled again if you're Tipsy II or worse. Your stake comes out of your coins and winnings go straight back.
  - **Fair play:** walking away mid-hand stands for you, so you can't dodge a loss. Anyone with two wanted stars gets turned away.
  - **Advancements:** Twenty-One, Five-Card Charlie and Cleaned Out.
- **Bounty Boards show their posters (phase 18).**
  - **A new Bounty Board you hang on a wall like a painting:** a cork board in a wooden frame, two blocks high. It's three blocks wide (six posters) where there's room, or two (four).
  - **Where they hang:** saloons and sheriff's offices now come with one, and the grand saloon's is three wide.
  - **Crafting:** three sticks over three paper over three planks, or two Small Bounty Boards.
  - **The old one-block board:** it's now the Small Bounty Board. It has four posters on each open side, and stands alone in the town square.
  - **Each poster:** a sketch of the outlaw's face, their name, the reward, and how far and which way the hideout lies.
  - **Taking one:** take the poster you're looking at. It's stamped TAKEN with your name, so nobody else can have it. Bring the badge back and it's stamped DEAD.
  - **Fresh posters:** new outlaws go up at dawn, and stale posters come down.
  - **Towns:** the boards in a town share one list. A raid leader who got away from the town goes up big across the top.
  - **Wanted players:** anyone with two wanted stars nearby gets a poster of their own, showing their fine. Pay it at the board and it comes down.
  - **Older worlds:** boards that are already placed put up their posters the first time they're used, or within a minute or two.
- **The frontier talks (phase 19).** 221 new lines, 279 in all, and what folk say follows what's going on.
  - **What they notice:** the time of day and the weather, your wanted stars, whether the men in black are after you, a robbery or a raid in town, the poster you're carrying (they'll name the outlaw), the cowboy outfit, the Starsteel Crown or a Kindled Sigil, and the strange doings lately.
  - **The story:** what they talk about changes as it goes on: once you've been down a polar gateway, once you've seen a saucer, and once the leash is broken.
  - **Greetings:** townsfolk, barmaids, lawmen and freed men say hello when you come near, once a day each. Every townsperson and lawman has a few lines when you talk to them, and so does each barmaid.
  - **Overheard:** now and then two townsfolk near you trade a bit of gossip.
  - **In a fight:** outlaws call out when they spot you, when they reload, when they're badly hurt and when the gang rides out, and Outlaw Bosses taunt you. Lawmen shout when they spot a wanted player, when they get their man, when a posse rides in and when a raid hits town.
  - **No flooding:** nobody repeats a line you just heard, and there are cooldowns for each speaker and for each listener.
- **Gray Landings (phase 15a).** Once the leash is broken the Grays stop taking people, but they don't leave. Every few nights a saucer comes down near a player, in the Overworld or the Hollow Earth.
  - **The landing:** it sets down for the night in a ring of scorched ground, on open, level land away from towns. Two to four Gray Wardens and two drones keep it, round a sealed **Gray Cache**.
  - **Clearing it:** bring down every guard before dawn and the saucer flees, leaving the cache unsealed. Open it for alien alloy plating, charged phlogiston cells, phlogiston shards, Gray tech and now and then a whole part (a gravity drive, beam emitter, lens array or navigation core).
  - **Dawn:** if it isn't cleared by morning, the saucer takes its guards and the cache back aboard and lifts off. Sleeping through the night counts as morning.
  - **News:** the Gazette reports Overworld landings, and townsfolk, wranglers, barmaids and lawmen talk about them (10 new lines).
  - **Config:** a new `landings` section in `hollowfrontier-ufo-server.toml` (enabled, interval in nights, chance). There are no landings on Peaceful. Ops can call one down with `/hollowfrontier ufo landing`.
  - **Advancement:** Trespassers, for clearing a landing before dawn.
- **Fixed:** in singleplayer, a Polar Gateway fall that was still going when you left a world no longer keeps you safe from fall damage in the next world you open.
- **Fixed:** a Polar Gateway that was waking up when its chunk unloaded now finishes once you're back, instead of staying half lit.
- **Performance:** town cats looking for an evening campfire check far fewer blocks and never load chunks to do it. Pale Ravens with no standing stone nearby stop searching for a while before they look again.
- **Tests:** 274 game tests, all passing (38 new).

## 1.0.0 — Phase 14: Polish and release (2026-10-29)

The first release: everything below, from the frontier to the Mothership, plus:

- **Works with JEI, Jade and Curios when they're installed** (none of them is needed):
  - **JEI:** a Steam Crusher category, and info pages for the steam blocks and for things you only find in the world (crash salvage, the Archon's and outlaw bosses' drops, the sheriff's star).
  - **Jade:** a boiler's fire, a crusher's progress, a gauge's reading, an airship's steam and coal, a handcar's steam, and a saucer's phlogiston and fittings.
  - **Curios:** wear the Sunstone Lens as a charm and the Kindled Sigil as a necklace.
- **Advancements:** Dressed for the Range (the whole cowboy outfit), Armadillo Chic (a full suit of glyptodon shell), and the capstone **Legend of the Frontier**: bring down an Outlaw Boss, the Sun Archon and the Mothership.
- **Balance:** the Phlogiston Rifle hits for 20 (was 16, less than the Plains Rifle it replaces).
- **Housekeeping:** old unused textures from earlier names (orichalcum, the lever rifle, shotgun shells, the Saucer Cradle) are gone from the jar. A README covers requirements, optional mods and settings.
- **Tests:** 236 game tests, all passing (3 new), run with JEI, Jade and Curios loaded.

## Phase 13d: The Mothership (2026-10-29)

- **The Phlogiston Beacon.** Set the Sun Archon's Phlogiston Core in starsteel and Gray tech, over a phlogiston lamp. Light it at night under the open sky, and a violet beam goes up. Something answers.
- **The Mothership.** A twelve-block Gray craft comes down out of the night to hang over the beacon, and fights in three phases:
  - **Drones and the death ray:** waves of drones, and a death ray that marks the ground under you a moment before it falls.
  - **Abduction:** a shield that holds until you bring down the four drones it sends out (no more come while it's up), and an abduction beam that lifts anyone who isn't sneaking and drops them from the height.
  - **Exposed core:** at the last it comes down low with its belly open on a burning core that takes extra harm.
- **How to fight it:** from the ground with bows and phlogiston guns, or from your Prototype Saucer, in a dogfight with its drones. The saucer's heat laser now hits twice as hard, and you can eat (or use anything you hold) while aboard.
- **The leash breaks.** The first time it falls, the Grays' hold on the men they implanted is gone:
  - The outlaws nearby throw down their guns.
  - The men in black walk off for good, and no one is watched any more.
  - The abductions stop, and far fewer outlaws ride the wild.
- **Gray Drones:** small, quick, and they shoot.
- **Sounds:** a deep throb, and a death ray that whines up before it falls. Both are ours, synthesized.
- **Advancements:** Calling Them Down, Dogfight, The Mothership Falls, Cut Loose. The Codex's last chapter now tells it to the end.
- **Commands (ops):** `/hollowfrontier ufo mothership`, and `ufo leash`, `ufo leash break` and `ufo leash mend`.
- **Tests:** 233 game tests, all passing (7 new).

## Phase 13c: The Prototype Saucer (2026-10-29)

- **Build your own saucer.** Craft a Prototype Saucer from eight plates of alien alloy round the mended gravity drive, and set it down on the ground. It comes with its tractor beam and heat laser; use a navigation core on it to fit bearings. Landed and empty, sneak-use it bare-handed to fold it back into an item, fittings and phlogiston kept.
- **Fly it.** A pilot and a passenger under a ribbed glass dome; you can see the crew through it. It faces where you look; W/S drive it, A/D slide it, jump climbs, sprint descends, and it hovers when you let go. About three times as fast as the airship, running on phlogiston: a new saucer comes with 64 charge, and when it runs dry it draws on the Phlogiston Cells (or phlogiston shards) you carry. The gauge tells you when it has none. In first person the dome is hidden, so you see out clear; its hum is quiet.
- **Heat laser** (hold V): bolts of orange light that land where your sight is, burning what they hit and scorching the ground (never in town). A laser sight with a heat bar shows while you pilot. In third person the camera rides above the saucer, so it sits under the sight instead of behind it. Fire too long and it overheats.
- **Tractor beam** (hold B): a column of light that lifts creatures, items and players up under the saucer and carries them along, then lets them down gently. While it's on, the third-person camera eases back and up so you can see what you're lifting. Carried players can struggle free by jumping, and `/gamerule hollowfrontier:saucer_beams_players false` stops it taking players at all.
- **Navigation core:** your gauge shows the way to the nearest Polar Gateway and back to where you set the saucer down.
- **It hums.** A theremin-ish flying-saucer warble of our own.
- **Advancements:** We Have Liftoff, Beam Me Up, Strange Lights Over the Frontier.
- **Tests:** 226 game tests, all passing (the saucer flown from a pilot's own keys, and the laser hitting what the pilot's sight is on, among them).

## Phase 13b: Phlogiston tech (2026-10-29)

- **Ray Pistol and Phlogiston Rifle:** the Grays' guns, rebuilt in starsteel and brass, and fully 3D like the frontier guns: a glowing phlogiston tube on top, starsteel fins down the barrel, a lit emitter at the muzzle. They fire a real beam of light from the muzzle to the target with a laser shot of their own, and there's no loading and no misfiring: each shot draws on your Phlogiston Cells. The pistol is quick and light; the rifle hits hard at long range and its beam goes through three creatures in a row. Build them from Gray tech fragments, phlogiston, starsteel and brass.
- **Phlogiston Cells:** 64 charges a cell, shown on the bar. A drained cell goes empty, and a phlogiston shard charges it again.
- **The gravity drive, mended:** set a damaged drive in phlogiston and starsteel and it works again, ready for the saucer you'll build.
- **Advancements:** Lightning in a Bottle, Death Ray, Defying Gravity. The Concavian Codex has a new chapter, "The Grays".
- **Tests:** 218 game tests, all passing (3 new).

## Phase 13a: The Grays arrive (2026-10-29)

- **Saucers at night.** Now and then a Gray saucer crosses the Overworld sky, low and fast, its rim strung with green lights. Sometimes one stops, and a pale beam comes down: it takes livestock, and it takes people who don't keep their heads down. Sneak, or step out of the light, and it lets you go. If it doesn't, you'll come to somewhere else, a long way off, with hours you can't account for and an itch behind your ear.
- **Gray Wardens:** tall grey Grays with great black eyes and ray pistols. They keep their distance and burn you with green rays, and when you hit them they blink away. They guard the wrecks, and roam the Hollow Earth near the Gray labs.
- **Crash sites,** rare, on the frontier and under the crust: a scorched furrow ending in a downed saucer, nose in the dirt, still lit. Break its wreckage for salvage: alien alloy plating, and maybe a damaged gravity drive, a beam emitter, a lens array or a navigation core. No wreck holds them all.
- **New tabs:** "The Grays" creative tab, and an advancement tab: Close Encounters, Missing Time, Not Today, Crash and Burn, Salvage Rights, Some Assembly Required, Take Me to Your Leader.
- **Config:** `hollowfrontier-ufo-server.toml` (per world) turns the flyovers, or just player abductions, off.
- **For ops:** `/hollowfrontier ufo flyover|abduction`.
- **Tests:** 215 game tests, all passing (8 new).

## Phase 12b: The Sun Archon (2026-10-29)

- **The Sun Temple** in the Ember Badlands: a stepped ziggurat of crystal and gold with a sun laid in the arena on top, four Sun Pylons at its corners, and a throne. Once you're their ally, the Kindled will tell you where to find it.
- **The Sun Archon:** the last king of Concavia, who went into the inner sun to save it and came out as something else. It waits on its throne until you come close, then fights in three phases under a boss bar. First it walks you down and burns you with a solar beam. Then it rises into the light, raining fire, and while the pylons stand nothing can touch it: break them. At the end it eclipses, faster and harder, loosing sunbursts that throw everyone near. Walk away and it goes back to its throne, healed, and the pylons are made whole.
- **What it leaves:** the **Phlogiston Core** (the heart the Grays came for; its use comes later), the **Starsteel Crown** (a helmet that can't be withered and halves phlogiston beams), and sunstone shards.
- **Sunstone Lens:** hold it to see in the dark, and use it to throw a beam of sunlight that burns, twice as hard against the undead and the Grays.
- **Concavian Codex:** a book that writes itself for you each time you open it, ticking off how far you've come from the frontier to the inner sun and saying what's next. Craft it from a book and a phlogiston shard, or buy one from a Kindled.
- **Advancements:** Required Reading, Temple of the Sun, Eclipse, Heart of the Sun, Uneasy Lies the Head, Let There Be Light.
- **Tests:** 207 game tests, all passing (9 new).

## Phase 12a: The Kindled (2026-10-29)

- **Crystal Ruins** in the Concavian Highlands: a broken crystal city round a phlogiston fountain, its towers fallen but one, and a sanctum still standing where the last Kindled live. Crystal Guardians keep its door. A Pale Raven flushed in the Highlands may fly there too.
- **The Kindled:** tall, pale, quietly glowing traders who deal in phlogiston shards. They buy what the Grays leave behind (fragments, locators, the pilot's skull) and some plainer things, and sell their crystal stone, starsteel and phlogiston lamps. Talk to them, and each time they tell you a little more: who they were, what the Grays took from them, and what the Grays are doing to the frontier's drifters. Some of it they'll only tell a friend.
- **Become their ally.** Every trade wins a little favour, and trading them the Grays' things wins more. Make a friend of them and their guardians stop minding what you carry. Make an ally of them and they give you a **Kindled Sigil**: hold it, and every outlaw and man in black within 32 blocks glows, even through walls. Allies also get two trades no one else does.
- **Crystal Guardians:** crystal constructs on stone legs that charge a pale beam and fire. They fire on anything of the Grays', on anyone who's hurt a Kindled, and on strangers who walk in carrying Gray things (they give one warning shot first). Hurt a Kindled and the Kindled won't deal with you for a while.
- **Concavian crystal stone:** Concavian Crystal (see-through, glowing), Crystal Bricks with stairs, slabs and walls, and Chiseled Crystal Bricks with glowing phlogiston seams. Craft the crystal from glass, quartz and a phlogiston shard.
- **A new Concavia creative tab and advancement tab:** The Crystal City, Crystal Clear, Well Met, An Ally Below, The Truth Is Down Here, Glass Jaw.
- **For ops:** `/hollowfrontier kindled get|set <favour>`.
- **Tests:** 198 game tests, all passing (8 new).

## Phase 11b: Men in black dusters (2026-10-29)

- **They come after the lights.** The day after a sighting, a mutilation or a crop circle, one or two men in black dusters turn up in town: pale, polite, a little too still, with a faint light behind one ear. Soon after, the Gazette prints a correction (it was a weather balloon, or wolves, or some boys with a plank).
- **They buy it back.** Hold out a Gray tech fragment, the pilot's skull, a Signal Locator or a sheaf of flattened wheat, and they'll pay gold for it, and think a little better of you.
- **Suspicion:** poke at the Grays' business (shoot their probes, kill their specimens, call down their airships, follow their signals, carry their things around) and the men in black notice. At first you'll just feel watched, and they'll have a quiet word. Push it, and they'll shoot on sight and come hunting you at night, two at a time. It fades with time, and they don't forget a face, not even after you die.
- **Advancements:** Nothing to See Here, A Word to the Wise, Marked Man.
- **For ops:** `/hollowfrontier mystery visitors`, `/hollowfrontier suspicion get|set`.
- **Tests:** 190 game tests, all passing (7 new).

## Phase 11a: Sightings and signs (2026-10-29)

- **Something's going on out there.** About one night in two, something strange happens near you:
  - **Airship sightings:** a great dark cigar strung with lights crosses the sky, silent but for a hum, and is gone.
  - **Cattle mutilations:** a steer found dead and cut with a surgeon's neatness, no blood, in a ring of scorched grass. Break up the carcass for leather (and sometimes something that isn't leather).
  - **Crop circles:** rings pressed into a ripe wheat field overnight, every stalk laid flat.
- **Stake it out.** If you're awake and not too close, you can watch it done: a Gray probe comes down and works at it with a beam of light. Sneak up and bring it down before it finishes, and it drops Gray tech fragments. Walk up on it and it's gone.
- **The talk of the town:** the Gazette reports the week's strange doings with where and when, and townsfolk grumble about them (wranglers especially).
- **The Boot Hill grave pays off:** dig up the strange grave to find the airship pilot's skull and his logbook, written in a script nobody can read. Hold the skull up to the night sky and see what comes.
- **New:** Flattened Wheat, the Strange Skull, `/hollowfrontier mystery` for ops, and three Frontier advancements (Lights in the Sky, Caught in the Act, Not of This World).
- **Tests:** 183 game tests, all passing (8 new).

## Phase 10b: Gray experiments (2026-10-29)

- **Live Gray field stations** stand in the Jungle, the Steppes and the Badlands. Unlike the abandoned lab, these are lit and working: tagged beasts in glass pens, a hare on the operating table, glow squid in the tanks, a humming phlogiston reactor, a mast with its dish.
- **Gray Sentinels** keep watch over them. These probes don't run. One that sees you locks on with a red beam, then jolts you, slows you, and sets every tagged beast nearby on you. Break its line of sight, or shoot it down first.
- **Tagged megafauna:** mammoths, terror birds, megatheria and glyptodons in riveted plating, with a glowing implant. They're tougher and faster, and the implant drives them: tagged mammoths and megatheria charge on sight, a tagged terror bird isn't afraid of mammoths, and none of them pay the Oliphant any mind. Now and then one turns up far from any station, having escaped.
- **Gray Tech Fragments** come from tagged beasts, sentinels and station chests. Build a **Signal Locator** from them (with a compass and a phlogiston shard) to home in on the nearest station.
- **Advancements:** Still Running, Escaped Specimen, On the Signal.
- **Tests:** 175 game tests, all passing (9 new).

## Phase 10a: Hollow Earth megafauna (2026-10-29)

- **Woolly Mammoths** roam the Highlands and the Steppes in herds. Leave them be and they graze. Hurt one and it trumpets and charges, tusks first, and the whole herd comes with it.
- **Terror Birds** hunt the Steppes and the Badlands by day: tall flightless hunters that screech when they spot you and run a little faster than you can sprint. Ride a horse, or stay near a mammoth herd: terror birds won't go near mammoths.
- **Megatheria**, giant ground sloths, browse the Jungle. They rear up on their hind legs to strip leaves off the trees. They're slow, and almost impossible to knock about. Don't make one angry.
- **Glyptodons**, giant armadillos with shells of bony scutes, wander the Jungle and the Steppes. Frighten one and it hunkers down under its shell. Brush it for a scute now and then, as you would an armadillo.
- **What they leave you:** blow an **Oliphant** (a horn made from a mammoth's tusk) to calm angry beasts and send terror birds running. The **Talon Knife** is quick, and skins an extra hide, tusk, talon, scute or snakeskin from whatever you kill with it. **Hide Barding** is tough horse armor made from megatherium hide. **Glyptodon Shell Armor**, made from scutes, means arrows and bullets barely scratch you while you sneak. **Mammoth Steaks** are the most filling meat around.
- **Advancements:** Woolly Thinking, Sound the Horn, Don't Run, Shell Game, and the challenge Big Game (bring down one of each).
- **Tests:** 166 game tests, all passing (14 new).

## Phase 9c: Hollow Earth ores and gear (2026-10-29)

- **Starsteel**, the Concavians' crimson-and-gold metal (they called it orichalcum), deep under the Hollow Earth's floor (and richer in the Ember Badlands' red rock). Mine it with iron or better, smelt it, or crush it to double it.
- **Starsteel gear** (its ores, ingots and armor drawn after vanilla's iron, in crimson and gold): a sword, a **paxel** (pickaxe, axe and shovel in one: it strips logs and makes paths too) and a suit of armor, each set with a phlogiston stone. A tier between diamond and netherite. Lasts longer than diamond, digs faster than anything, takes enchantments as well as gold, and its armor is tougher than diamond's. And it's **sun-forged**: stand in the sun and it slowly mends itself. Under the Hollow Earth's sun, that's always.
- **Starsteel Horseshoes:** the fastest shoes yet, and a horse shod in starsteel (and whoever's riding it) takes no fall damage.
- **Phlogiston:** glowing ice-blue crystal, scarce everywhere below and common under the Crystal Steppes and the Highlands. Phlogiston ore glows in the dark, and shows in the crystal outcrops. A shard burns as long as a block of coal, in a furnace or a boiler, and lights a **Phlogiston Lamp**, the brightest light there is.
- **Talk on the frontier:** the Gazette now runs stories of a silent airship with no balloon, a lecture on Symmes' Hole, and a whaler who found warm seas past the pole. And on the rare Boot Hill there's one grave that isn't like the others. Dig it up, if you dare.
- **A Hollow Earth advancement tab:** the four lands, phlogiston and starsteel, and the lore sites as hidden finds. The Hollow Earth creative tab is full now, under a phlogiston shard.
- **No more zinc:** the frontier runs on vanilla metals now. Brass is 3 copper ingots and a gold nugget (makes 4), the general store buys copper instead, and the Steam Age advancements start from your first copper or brass ingot. Old zinc items in existing worlds disappear.
- **Brass plays nice with other mods:** every recipe takes any mod's brass. When another mod adds its own brass (Create, for one), the crafting-table brass recipe steps aside so that mod's way of making brass is the one that counts. Pack makers can force it on or off with `alloying` in `config/hollowfrontier-startup.toml`.
- **Tests:** 152 game tests, all passing (9 new).

## Phase 9b3: Hollow Earth lore sites (2026-10-29)

- **Someone was here before you.** Three kinds of place to find on the Hollow Earth's floor:
  - **Kindled sites** (Concavian Highlands), one of three: a great **henge**, a ring of trilithons round an obelisk with a lodestone in each face and a blue fire burning on top, its smoke climbing high enough to steer by; a **sealed gate**, a stepped quartz front 21 blocks high cut into a snow-capped hill between two spear-bearing guardians, its great iron door shut on a hall of columns, a tomb and a vault; and a ruined **temple**, a colonnade round a roofless chamber with a mural of the world as its builders saw it, a shell with a sun inside and an opening at each pole. Flush a Pale Raven and it now flies toward the nearest one.
  - **An abandoned Gray field lab** (Jungle, Steppes, Badlands): a dome over restraint tables and specimen tanks, a scorched landing pad, an antenna mast with a dish, and a block of cages, one holding a Glassback Hare with a yellow tag in its ear. Two small silver things hang over it, watching you.
  - **A lost Symmes expedition camp** (anywhere below): the expedition's airship lies crashed across the camp, its envelope collapsed and its gondola broken off, inside a palisade smashed where it fell and burned where something landed. There's a command tent with the expedition's journal on a lectern (read it), a watchtower, stores, and three graves, one of them open and empty.
- **Gray Probe:** a little hovering saucer with a band of teal lights. It turns to watch you, and if you come close, stare at it or hit it, it shoots up and away and blinks out. It has a spawn egg in the Hollow Earth tab.
- **Clearings:** trees, bamboo and the bigger plants and rocks don't grow where a site stands. Sites never crowd each other.
- **Tests:** 143 game tests, all passing (9 new).

## Phase 9b2: Hollow Earth creatures (2026-10-28)

- **Something lives down here now.** Each biome has a small creature of its own:
  - **Fern Skitter** (Primordial Jungle): little striped feathered dinosaurs that run in flocks. They keep their distance unless you sneak, and if you hurt one the whole flock scatters. Drop food near them and one will dart in, snatch it and run off to eat it. Catch the thief and you get your food back.
  - **Glassback Hare** (Crystal Steppes): a big buff hare with glowing crystals growing along its back. Very shy, but it'll come for a carrot. Sometimes drops an amethyst shard.
  - **Ash Salamander** (Ember Badlands): a black lizard with glowing orange spots. It walks across lava, hates water, and likes to lie basking on the ash vents. Sometimes drops magma cream.
  - **Pale Raven** (Concavian Highlands): white ravens that perch on the standing stones. Get close without sneaking and they take off, croaking. Follow them: one day they'll lead somewhere.
- **Animals spawn properly in the Hollow Earth:** the horses, rabbits, goats and others that should have been there from the start now appear as new land generates, instead of hardly ever.
- **Spawn eggs** for all four, in the Hollow Earth creative tab.
- **Tests:** 134 game tests, all passing (10 new).

## Phase 9b1: Hollow Earth flora and ambience (2026-10-28)

- **Each biome looks like itself now:**
  - **Primordial Jungle:** giant tree ferns, their crowns of fronds spread like parasols over slender dark trunks, and squat cycads with stiff fronds fanning up out of them.
  - **Crystal Steppes:** outcrops of crystal stand out of the golden grass, leaning spires on pale calcite footings, tipped with glowing clusters.
  - **Ember Badlands:** smoking ash vents on little cones of basalt, in grey fields of ash with magma glowing through. Their smoke rises high over the mesas. They're hot underfoot (sneak across).
  - **Concavian Highlands:** pale standing stones, fallen quartz pillars beside their broken stumps, and now and then a ring of stones round a carved floor. Older than anything on the surface.
- **A lighter jungle:** the Primordial Jungle has fewer trees, so its floor is no longer pitch dark and the tree ferns stand in the light.
- **Sounds and air:** each biome has its own sounds now: big birds, frogs and something old calling in the Jungle; crystals chiming on the Steppes; rumbles and the mutter of lava in the Badlands; wind over the Highlands. Glints drift over the Steppes, and cinders over the Badlands.
- **New blocks:** **Fern Frond** (shear it for leaves; it sometimes drops a fern), **Ash** (falls like sand; craft it into grey dye) and **Ash Vent** (smokes forever; mine it for ash, or silk-touch it). All three are in the Hollow Earth creative tab.
- **Tests:** 124 game tests, all passing (8 new, which grow each feature on a test plot).

## Phase 9a: The Hollow Earth (2026-10-28)

- **A world of its own:** the Hollow Earth now has its own land and sky. A thick crust of golden rock arches about 150 blocks above the ground, with great stone spires hanging down from it (some reach all the way to the hills) and rare floating islands drifting beneath. Waterfalls pour from the ceiling, and in the red country, falls of lava. The crust is capped with unbreakable shell bedrock, so there's no digging out the top.
- **The inner sun** hangs straight overhead, always: a white-gold sun in a golden-green haze that never sets. It's always day down here. You can't sleep, but a bed still sets your spawn. Clocks and ordinary compasses spin; the Polar Compass still finds the way home.
- **Four biomes:**
  - **Primordial Jungle:** rolling green hills under towering jungle trees, ferns and bamboo, rivers with muddy banks, a steamy green haze.
  - **Crystal Steppes:** open golden grassland to the horizon, a lone acacia here and there, wild horses grazing.
  - **Ember Badlands:** stepped mesas of banded red rock, black basalt and smouldering magma, ash on the wind, lava falling from the crust.
  - **Concavian Highlands:** pale stone uplands with grey-green grass and pale moss, snow and ice on the peaks.
- **Symmes' Hole:** the crossing is now a fall into another world. You come out at the top of a dark shaft bored through the crust, frosted at its mouth, with rings of light rushing past as you drop. Then you burst out under the ceiling and hang in the air for a breath, with the Hollow Earth spread out below, the inner sun overhead and the music swelling. Then you drift gently down to the return gateway. Your first time in, the title card greets you at that moment. The fall can't hurt you.
- **Safer arrivals:** the return gateway now always lands on solid, level ground under the ceiling. It's never on top of the crust, never on a floating island, never in lava, and if a tree stands in the way it gets cut down.
- **New block:** Crust Rock, building stone that oddly lets light through. It works like cobblestone for stone tools and furnaces. It sits in the new **Hollow Earth** creative tab.
- **Fixed:** the Hollow Earth's sky is no longer plain blue.
- **Tests:** 116 game tests, all passing (9 new, which check the dimension across five world seeds).
- **Note:** a Hollow Earth from an earlier version keeps its old terrain where it was already explored. Use a new world.

## Phase 8: Airship & polar gateway (2026-10-28)

- **Airship:** a steam dirigible for two. Craft an **Airship Envelope** (leather, phantom membranes, string) and an **Airship Engine** (brass gears, a plate, copper pipes, a furnace, a pressure valve), then put them together with two leads and any boat.
  - **Flying:** forward and back drive the propeller, left and right turn, **jump climbs** and **sprint descends**. Sneak to step off. The pilot sees steam, coal and altitude on screen.
  - **Fuel:** feed it coal or charcoal by hand (up to five), and its boiler keeps the envelope full of steam. A steam canister tops it up too, and parked over a steam tank, pipe or boiler it takes on steam by itself, like a mooring mast.
  - **Safe landings:** out of steam, it can't climb or drive and drifts gently down. It never falls.
- **Polar Gateways:** rare craters of blue ice in the snowy north, with a ring of carved stones lying in the floor and a lost expedition's camp on the slope.
  - **Polar Compass** (a compass, brass plates and packed ice): use it to take a bearing, and the needle points to the nearest gateway and tells you roughly how far.
  - **Polar Resonator** (amethyst, brass gears and plates, a pressure valve, copper pipe): use it on the ring with 2,000 mB of steam in your canisters and the gateway opens. Sneak-use it to shut a gateway.
  - **Into the Hollow Earth:** step into the light. You come out at the same spot deep inside, beside a return gateway that leads back to the one you came through. Phase 9 will make it a world of its own; for now it's lit by an unsetting sun under a green-gold sky.
- **Crossing in style:** the resonator wakes the ring one stone at a time, each chiming higher, until the well floods with light and a beam shoots up into the sky (you can see it from far off). Stand in the well and frost creeps over your sight while the ring hums higher and snow swirls round you. Then a white flash, a rush of cold air, and you drift down from high above the return gateway in a column of light. Your first time in, a title welcomes you to the Hollow Earth. Coming home lifts you up out of the well. No more nether portal noises.
- **The Frontier Gazette** reports the nearest gateway's crater, where a polar expedition went missing.
- **Advancements:** five more on the Steam Age tab: Lighter Than Air, Above the Clouds, True North, Open Sesame and Journey to the Centre.
- **Tests:** 107 game tests, all passing (9 new for the airship and the gateways).
- **Checked in-game** with the photo booth: the airship, the crater, the gateway opening, a trip into the Hollow Earth and back. The airship's helm wheel is now an open spoked ring (it used to block the pilot's view), and the third-person camera pulls back far enough to see the whole airship.
- **Known:** the Hollow Earth's sky is still plain blue; the green-gold sky arrives with its proper biomes in phase 9.

## Black powder guns (2026-10-27)

- **New creative menu:** the single Hollow Frontier tab is now three: **Frontier** (guns, ammo, cowboy gear, horse gear, money and bounties, drinks, discs, cat accessories, spawn eggs), **Frontier Building** (saloon furniture, the piano, the Bounty Board, brass and zinc blocks) and **Steam Age** (zinc and brass, parts, the canister, drill and handcar, the machines). Everything is in a sensible order, with handy variants: every gun both empty and fully loaded, an empty and a full steam canister, and the bandana in all 16 colours. Mod items also show up in the vanilla tabs next to their relatives (guns by the crossbow, drinks by the honey bottle, discs after the vanilla discs, and so on).
- **Copper Pipe** looks like a pipe in your inventory now, not a dot.

- **Steam Handcar:** ride the rails under steam. Craft one from a pressure valve, two brass gears, a minecart, two brass plates and a copper pipe, set it on a rail and climb aboard.
  - **Hold forward** to open the throttle, **back** to brake, **jump** to blow the whistle. Under steam it runs nearly twice as fast as a minecart and shoves any cart in front of it along the line. A gauge on screen shows how much steam is left.
  - **Fill it up** by using a steam canister on it, or stop beside a steam tank, pipe or boiler next to the rail and it takes on steam by itself.
- **The railroad depot comes alive:** the station agent is now a **Railroad Engineer** who sells rails, powered rails, full steam canisters and pressure valves, and buys ore dust from your crusher. Down by the track there's a **steam standpipe** (a boiler over a well, already fired up) where a handcar can fill up, and a handcar waits on the siding for anyone who wants to try it.
- **Boilers fill themselves** from a water source block next to them.
- **Advancements:** two new tabs, **Hollow Frontier** and **Steam Age**, with 33 advancements between them: how-tos (load a gun, mint a coin, pull up a bar stool, shoe a horse), goals (claim a bounty, hold off a raid, build a handcar) and a few challenges and secrets, some with a reward. A few stay hidden until you stumble into them.
- **The Saloon Piano has a repertoire:** it now plays four Scott Joplin rags in turn (The Entertainer, Elite Syncopations, Pineapple Rag and The Strenuous Life) on a slightly out-of-tune saloon upright, and names each tune as it starts. They're our own recordings of the public-domain scores, so all of the mod's music is free to share anywhere.

- **Bigger, grander towns:** frontier towns now grow from a **town square** like a village, with two to four streets leading off it, side lanes, and outskirts.
  - **The square** has a bandstand or a well, a flagpole, the Bounty Board, a horse trough and the evening fire pit, with the sheriff's office (or the bank), the hotel and the saloon facing onto it.
  - **Grand buildings:** a brick **courthouse** with columns and a clock tower (its bell sounds the raid alarm), a white **church** with a tall steeple, a three-storey **grand hotel**, and a two-storey **saloon** with a balcony, a long bar and rooms upstairs.
  - **Down the streets** are shops and homes, lamp posts, telegraph poles and parked wagons. At the far ends you'll find a chapel, a water tower, a **railroad depot** with a stretch of track, or a lane of homes and gardens running out to a **windpump**, a **corral** and **Boot Hill**.
  - **No two towns alike:** buildings come in several paint jobs (other woods, a red barn, a granite bank), and towns follow the lie of the land instead of sitting on one flat slab.
  - **Three kinds of town:** clapboard frontier towns on the plains and savanna, **desert mission towns** of whitewashed sandstone and red tile in deserts and badlands, and **mining towns** of timber, stone and slate up in the pines.
  - Generate a new world (or explore new ground) to see them. Towns already in a world were laid out with the old pieces and may come out patchy where their unexplored parts generate.

- **Outlaw raids:** now and then, at night, a gang rides on the frontier town you're in.
  - **The warning:** the chapel bell starts ringing (in towns without a chapel, someone beats the alarm triangle), and you hear hoofbeats and gunshots coming from out of town. A title names the gang, and a bar at the top of the screen counts down. You have about 20 seconds to get ready.
  - **The town takes cover:** townsfolk and barmaids hurry indoors and shut the door behind them, and the street goes quiet. Lawmen form a line across the middle of the street. While the raid lasts they leave one- and two-star players alone; they have bigger problems.
  - **The gang:** 4 to 8 riders on horseback, more in bigger towns and on harder difficulties, led by a named boss. They get off their horses at the end of the street. Gunslingers hold the street, a **Dynamiter** lights a stick (watch for the sparks in his raised hand) and lobs it at lawmen and anyone shooting at them, and the **Safecracker** heads straight for the bank vault, or the sheriff's strongbox if the town has no bank. Their dynamite never damages the town. The sheriff's cat makes raiders glow when they come near.
  - **The vault:** you can watch the Safecracker work: sparks, cracks spreading over the vault door, and the bar turning yellow as he gets closer. Kill him and the job starts over, though someone else may pick up his tools. If the vault opens, the Safecracker grabs the loot and runs. Catch him before he rides out of town and the money goes back in the vault.
  - **Winning:** shoot the leader, or enough of the gang that the rest break and run, and the town is saved. Everyone who fought is paid on the spot in copper and gold: more for kills, the leader, a Safecracker, and every lawman still standing. The law also tears up a good part of your record, which clears up to two wanted stars. The leader drops his badge for the usual bounty. Horses the gang leaves behind are yours to tame.
  - **Losing:** if the gang gets away with the loot, the town is sore for a couple of days. Every trade costs a coin more, and the banker will tell you why. If the leader escapes, win or lose, his Wanted Poster goes up on the town's Bounty Board, and he can be hunted down at his hideout.
  - **Outlaws welcome:** a three-star outlaw is one of the gang's own. Stay out of their way and you'll get a cut of a robbery (the law will hear about it). Fire on them and they'll fire back.
  - **The Frontier Gazette** reports the latest raid: who led it and how it went.
  - For server operators: `/hollowfrontier raid start|stop|status` starts, ends or reports on a raid in the town you're standing in. How often raids come, gang size, timings, payouts and heat relief are all in the mod's per-world config.
- **Banks** have a gate in the teller's cage, so you (and safecrackers) can get to the vault.
- **Blacksmiths** are open-fronted again: a plank wall had been standing just inside the opening, blocking the forge. Only newly generated towns get the fix.
- **Sheriff's cats** now all follow their Sheriff. Before, about half of them stayed put at the office.
- **Outlaws no longer shoot each other** when one is in another's line of fire, and lawmen's shots pass by horses.

- **Town cats:** frontier towns and saloons have cats. There's a saloon cat asleep on the bar, a barn cat on the livery hay, cats on porches and hitching rails, and some lots have none. They're ordinary cats you can tame, breed and sleep beside, but town cats are used to people and won't run off when you ride in.
  - **Two new coats:** Desert (sandy, faint stripes; common in desert towns, and village cats in hot biomes sometimes have it) and Saloon (scruffy and patchy, with a nicked ear). Kittens can inherit either.
  - **They dress the part:** plenty of town cats wear a red or blue bandana, and a few wear a tiny cowboy hat with their ears poking through the brim.
  - **Evening fire:** every town now has a fire pit at the end of the street. At dusk the town's cats wander over and loaf around it.
  - **The Sheriff's cat** trots after the Sheriff like a deputy, sitting by him at his desk. It wears a brass star on a leather collar. It hisses at outlaws and at anyone with two or more wanted stars, and makes them glow for a few seconds so the law can spot them. It won't take fish from strangers while the Sheriff is alive.
  - **Snakes keep away:** rattlesnakes back off from cats, so towns are safer at the edges, and barn cats sometimes hunt snakes near town. A cornered snake still bites.
  - **Morning gifts:** a tamed cat sometimes brings a copper coin, percussion caps, a brass nugget, a snakeskin, or the morning paper.
  - **Dress your own cat:** use a **Bandana** (craft wool + string for 2, and dye it any colour), a Cowboy Hat or the **Brass Star** on your tamed cat. Sneak-use with an empty hand to take it back. You get the star by taming a sheriff's cat; it's the only way. A cat wearing the star spots outlaws for you.
  - **Something pale at the undertaker's...** In a few towns, a ghost cat haunts the undertaker's at night. It fades if you rush it, and it won't eat just anything. Rumour has it the right supper and a little music will win it over. A tamed ghost cat keeps phantoms away from a wide area.
  - **Sounds:** you'll hear a stray meow or purr around town.
- **Glowing works on frontier mobs:** outlaws, lawmen, townsfolk, barmaids and rattlesnakes now show the Glowing outline (from the sheriff's cat, spectral arrows or `/effect`). Before, they only gave off the effect's particles.

- **Dynamite:** throw a lit stick (paper + 2 gunpowder + string makes 2) for blast mining or bandits. It bounces and goes off after 2.5 seconds. Blacksmiths and prospectors sell it.
- **Rattlesnakes** live in deserts, badlands and savannas. Get close and one coils up and rattles a warning; step closer and it bites with poison. They drop snakeskin, which wranglers and prospectors buy.
- **Towns come alive with sound:** hammering blacksmiths, horses, spurs, creaking doors, boots on the boardwalk and the chapel bell. Every town now has a hotel, and its lobby has the town's jukebox, loaded with a record that plays once for the first visitor. Background music now pauses while a record or the piano plays nearby, so the two never overlap. Every saloon has a **Saloon Piano** playing Scott Joplin rags (right-click to start or stop it); craft your own from planks, a note block and bones.
- **Three new music discs** with real old-time recordings: the US Marine Band's 1906 "Maple Leaf Rag", "Old West Style" and "Spaghetti Western". Buy them from shopkeepers or find them in town chests; they play in any jukebox.
- **New sounds** for rattlesnakes, dynamite, coins, poured drinks and the wanted bell, all with subtitles.
- **Snakeskin Boots:** craft Cowboy Boots with 2 Snakeskin (they keep their enchantments and wear). Rattlesnakes leave the wearer alone unless provoked, their bite won't poison you, you're faster on sand, and they still count for the cowboy set bonus.
- **Wanted level:** hurting townsfolk or lawmen puts stars on your head (top-left of the screen). One star and the law watches you; two and lawmen shoot on sight and nobody will trade; three and a posse rides out after you. Lie low and it cools off, pay your fine at a Bounty Board, or die and start clean. Shooting outlaws helps your reputation.
- **The Frontier Gazette:** a one-copper newspaper from shopkeepers and bankers. Each copy prints a fresh edition when you open it: where outlaws were last seen, a saloon on the trail, the wanted column (maybe you) and some very odd news.
- **Townsfolk trades** now differ a little by trade: shopkeepers and bankers sell the Gazette, blacksmiths and prospectors sell dynamite, and wranglers and prospectors buy snakeskin.
- **Copper coins:** the Brass Coin is now the **Copper Coin** (id `copper_coin`). Coins can be minted: 4 copper nuggets → 1 Copper Coin, 4 gold nuggets → 1 Gold Coin (2x2, works in the inventory grid). 9 copper coins still trade for 1 gold coin and back.
- **Bar stools** have a tufted leather cushion, and you can sit on them (right-click; sneak to stand).
- **Lawmen:** Sheriffs, Deputies, U.S. Marshals and Rangers keep the peace in towns. They shoot outlaws and hostile mobs, come running when a townsperson is hurt, and never hit bystanders. The sheriff's office has a sheriff, a deputy and an outlaw locked in a cell who heckles passers-by; the bank has a deputy on guard.
- **Townsfolk** now come as Shopkeepers, Bankers, Blacksmiths, Wranglers and Prospectors, each with their own look. Male NPC skins no longer have a stray sideburn down one cheek.
- **Saloon drinks:** saloons (standalone and in towns) now have a **Barmaid** behind the bar who sells drinks for copper coins:
  - **Frontier Beer**: fills you up, short Regeneration. **Rye Whiskey**: Strength + Resistance. **Mezcal**: Night Vision + Jump Boost. **Moonshine**: Fire Resistance + Strength II. **Doc's Snake Oil**: one random effect, good or bad.
  - **Tipsy:** liquor stacks a new Tipsy effect (I-IV). Every level makes guns less accurate, Tipsy III brings bouts of swaying, and one drink too many blacks you out.
  - **Cowboy Coffee** (Haste) takes off a level of Tipsy, and **Sarsaparilla** (Speed) sobers you right up.
  - Saloon chests now hold a few bottles too.
- **Five barmaids, each her own person:** Miss Ruby, Clementine, Josie, Miss Belle and Lupe each have their own look (corsets, lace, aprons, ruffled skirts, hairstyles, hats and flowers), idle pose, name over her head, greetings when you open her menu, and house specialties poured cheaper. She pours a drink when a trade goes through.
- **No more villager noises** from townsfolk and barmaids: a coin clink or a pour instead.
- **Frontier towns rebuilt:** every town is now laid out differently. A main street of boardwalks, lamp posts, hitching rails and troughs is lined with false-fronted buildings drawn at random: sheriff's office and jail, bank with a vault, two-storey hotel with a balcony, general store, blacksmith (with a smoking forge), livery stable with horses, undertaker, clapboard houses and adobes. The saloon caps one end of the street and a chapel with a bell tower or a water tower caps the other. New loot in the bank, blacksmith and livery.
- **Guns are now 1800s percussion muzzleloaders** with 3D GeckoLib models and animations:
  - **Cap & Ball Revolver** (1851 Navy style): six chambers, open-top case-hardened frame, octagonal barrel, loading lever, brass guard and backstrap, walnut grip.
  - **Plains Rifle** (Hawken style, replaces the Lever-Action Rifle, id `plains_rifle`): single shot, 18 damage, 100-block range. Browned octagon barrel, ramrod in brass thimbles, sidelock, double set triggers, cheek piece.
  - **Double-Barrel Muzzleloader** (id unchanged, `shotgun`): two barrels of buckshot, twin hammers, ramrod.
- **Loading is one chamber at a time.** Right-click when empty, sneak + right-click to top up, or press the reload key (again to stop). Each chamber takes 0.75 s (revolver), 2.5 s (rifle) or 1.75 s (shotgun). Firing or putting the gun away stops loading; loaded chambers stay loaded.
- **Ammo:** each load uses a paper cartridge plus a **Percussion Cap** (new; copper ingot + gunpowder → 16). Paper cartridges are paper + gunpowder + iron nuggets. `shotgun_shell` is now **Paper Buckshot Cartridge** (`buckshot_cartridge`). Caps are in chest loot, outlaw drops and the townsfolk's trades.
- **Animations:** the revolver turns its cylinder and works the loading lever; the long guns tilt up while the ramrod is drawn, rammed and returned. Firing drops the hammer and kicks the gun. Outlaws' guns play the same reload animation during their reload pause.
- **Outlaws stop and aim:** they walk into range, plant their feet, square up to the target and raise the gun for half a second before the first shot. The revolver is aimed one-handed, long guns two-handed, and they hold the gun low to reload.
- **Black powder effects:** a flash and a thick white smoke cloud on every shot. Rain gives a 20% misfire chance and underwater shots always misfire.
- **Players** hold guns up in both hands (crossbow aiming pose).
- **Tests:** 28 game tests, all passing. New ones cover chamber-by-chamber loading, caps being required, and firing interrupting a reload.
- **Not checked in-game yet:** the hand, GUI and third-person transforms were worked out from the render code, not seen. Run `gradlew runPhoto` to check them.

## Testing & art pass (2026-10-26)

- **Game tests:** 25 automated tests run headless with `gradlew runGameTestServer`, and all pass. They cover:
  - **Data:** recipes, loot tables, tags, worldgen/damage registries, structure templates and saloon placement (blocks, chest loot, bartender).
  - **Combat and gear:** guns (damage, ammo, durability, reload, shotgun pellet stacking), lasso leashing, cowboy set bonus, and horseshoes/saddlebags/shears.
  - **Outlaws and bounties:** armed spawning, outlaws shooting players, boss badge drop, the bounty board poster/turn-in loop, gang spawning at the hideout, and townsfolk trades.
  - **Steam:** boiler, pipes to tank, crusher (with and without steam), pressure gauge, canister fill/draw, and steam drill tool rules.
- **Fix (found by the tests):** outlaw shoot animations used GeckoLib's trigger packet, which crashed the server if a connected player hadn't negotiated GeckoLib's channel. The animation is now driven by synced entity data, which also shows correctly to players who start watching mid-shot.
- **Art:** every texture now has pixel art (v1) drawn by `tools/textures/TextureArt.java`. No flat placeholders remain.
  - **Skins:** Outlaw, Outlaw Gunslinger, Outlaw Boss and Townsfolk.
  - **Items (37):** guns, ammo, materials, dusts, cowboy gear, horse gear, coins, poster, badge, spawn eggs, saloon doors, steam canister and steam drill.
  - **Blocks (24 tiles):** zinc ores, metal and raw blocks, bounty board, bar counter and stool, saloon doors, boiler (with a lit firebox), copper pipe, steam tank, crusher and pressure gauge.
  - **Cowboy armor layers (3).**
  - **Checked:** everything was checked in-game. That includes the mobs from the front, side and back, guns in hand, block rows, an item-frame wall of all 50 items, and an armor stand.
- **Known limit:** the cowboy hat renders as a banded box on the head, because vanilla armor geometry can't make a brim.
- **Dev tool:** `gradlew runPhoto`, a photo-booth client that stages the mod's humanoids and saves screenshots, then quits.
- Internal: `CowboySetBonus.update(player)` and `BountyEvents.checkPosters(player)` are now public, so tests can call them without tick events.

## 0.7.0 — Phase 7: Steam power (2026-10-26)

- **Steam** fluid (`hollowfrontier:steam`, a gas with no world block or bucket). It moves through NeoForge's standard fluid capability, so it works with other mods' pipes and tanks.
- **Boiler:** burns any furnace fuel and turns 10 mB of water into 10 mB of steam per burning tick (8 B water / 8 B steam tanks). Fill it with water buckets or fluid pipes. It glows while lit, and its menu has a fuel slot plus water, steam and flame gauges. Hoppers can feed fuel.
- **Copper Pipe:** connects automatically to anything with a fluid capability. It holds 500 mB and moves up to 200 mB per side per tick.
- **Steam Tank:** a 16 B buffer.
- **Steam transfer:** each holder pushes to neighbors with lower pressure (fill fraction), moving half the difference, so networks settle instead of sloshing.
- **Pressure Gauge:** right-click to see the highest neighboring pressure. Comparators read it as 0–15, updated twice a second.
- **Steam Crusher:** uses 4 mB of steam per tick, 100 ticks per item, and runs the new `hollowfrontier:crushing` recipe type. Its menu shows a progress arrow and a steam gauge. Hoppers insert from the top and sides and pull output from the bottom.
- **Crushing recipes:** raw iron, gold, copper and zinc (or their ore blocks) → 2 dusts, cobblestone → gravel, gravel → sand. Dusts smelt or blast back into ingots, doubling ore yield.
- **Steam Canister:** holds 4 B of steam. Right-click a boiler, pipe or tank to fill it; the item bar shows the fill.
- **Steam Drill:** an iron-tier pickaxe and shovel in one (durability 750). It uses 25 mB of steam per block from canisters in your inventory and mines at speed 9. With no steam it still mines, but at bare-hand speed.
- New `c:dusts/*` tags and recipes for every new block and item.
- 18 new placeholder textures. Machine GUIs are drawn without textures for now.

## 0.6.0 — Phase 6: Frontier structures (2026-10-26)

- Structures are built from code: a `Blueprint` builder describes each structure, and a datagen provider writes it to a structure `.nbt` (no client needed).
- Three new structures in desert, plains, savanna and badlands biomes (`#hollowfrontier:is_frontier`). All are single-piece jigsaw structures that blend into the terrain (`beard_thin`):
  - **Saloon:** porch, false front, swinging doors, bar with stools, tables, lanterns, bounty board, loot chest and a Townsfolk bartender. Spacing 36 / separation 14 chunks.
  - **Frontier Town:** dirt-path main street ending at a saloon, plus a general store, a sheriff office with a jail cell and bounty board, two houses, lamp posts, hitching rails and a water trough. Townsfolk in the shops; a loot chest in every building. Spacing 44 / 18.
  - **Outlaw Camp:** three tents around a campfire, seating logs, a loot chest and a gang of two Outlaws plus a Gunslinger. Spacing 26 / 9.
- Spawn rules: hostile mobs can't spawn inside saloons or towns; inside camps, only outlaws spawn as monsters.
- Five chest loot tables (saloon, general store, sheriff office, town house, outlaw camp) with coins, ammo, materials and rare guns.
- Saloon furniture:
  - Bar Counter: full block with top/side/bottom textures; recipe makes 3.
  - Bar Stool: seat on a post, with a matching shape; recipe makes 2.
  - Saloon Doors: a wooden door pair; recipe makes 2.
- Fixed `genPlaceholders`: it now reads only the `textures` entries of model files, so it no longer makes stray PNGs for blockstate model ids.
- 7 new placeholder textures.

## 0.5.0 — Phase 5: Outlaws & bounties (2026-10-26)

- New mobs (GeckoLib, shared humanoid model with walk/idle/shoot animations; guns shown in hand):
  - Outlaw (revolver, 20 HP).
  - Outlaw Gunslinger (rifle, 24 HP, long range).
  - Outlaw Boss (shotgun, 80 HP, 6 armor, boss bar, named, never despawns).
  - Townsfolk (trader).
- Outlaws close to ~70% of gun range and fire hitscan shots with mob-tuned guns, pausing to reload. Hurting one alerts the gang. They hunt players, townsfolk and villagers.
- Outlaws spawn at night in desert, plains, savanna and badlands biomes (`#hollowfrontier:has_outlaws`): weight 20 in groups of 1–3, plus Gunslingers at weight 6.
- Townsfolk sell ammo, guns, lassos, horseshoes and saddlebags for coins, and buy zinc, leather, wheat and emeralds for brass coins. They flee outlaws and never despawn.
- Brass Coin and Gold Coin (9 brass = 1 gold, exchangeable both ways). Coins can't be crafted from metal; they come from outlaws, bounties and trading.
- Bounty loop:
  - Right-click a Bounty Board to get a Wanted Poster: a randomly named boss hiding 200–500 blocks away, reward 3–6 gold coins, one poster at a time.
  - When you carry the poster within 64 blocks of the hideout, the boss and three henchmen appear there.
  - The boss drops an Outlaw Badge. Turn it in at any Bounty Board for the gold; the matching poster is consumed.
- Loot tables: outlaws drop ammo and brass coins, with a 3% gun drop. The boss always drops a shotgun, shells and coins.
- Spawn eggs for all four mobs. 13 new placeholder textures; the GeckoLib model and animation are hand-written placeholders.

## 0.4.0 — Phase 4: Cowboy gear (2026-10-26)

- Cowboy armor set: Cowboy Hat, Duster Coat, Leather Chaps, Cowboy Boots. Defense 2/4/3/2 (between leather and chainmail), enchantability 15, repaired with leather.
- Set bonus: wearing all four pieces gives the horse you ride +20% speed (removed when you dismount or take a piece off).
- Lasso (stacks to 16): thrown like a snowball. It yanks the target toward you (big animals budge less) and leashes it if it's leashable and within leash range. A successful catch uses up the lasso (it becomes the leash); a miss drops it where it lands. Pulling players works too.
- Horseshoes: Iron (+10% speed, +0.05 jump) and Brass (+20% speed, +0.1 jump). Right-click a tamed horse to fit them; the old pair drops.
- Saddlebags: right-click a tamed horse to attach 18 slots of storage. Sneak + right-click empty-handed to open them. All viewers share one inventory, so items can't be duplicated.
- Sneak + right-click with shears strips horseshoes and saddlebags (dropping their contents). Gear also drops when the horse dies.
- Recipes for all of the above. Armor gets the slot tags (head_armor etc.) so armor enchantments apply.
- `genPlaceholders` now accepts `path WxH` in placeholders.txt (used for 64x32 armor layers). 11 new placeholder textures.

## 0.3.0 — Phase 3: Guns & ammo (2026-10-26)

- Revolver (6 rounds, 7 dmg, 40 m), Lever-Action Rifle (5 rounds, 13 dmg, 96 m, no spread) and Double-Barrel Shotgun (2 shells, 8 pellets × 3.5 dmg, 20 m, wide spread).
- Hitscan firing with crit-particle tracers, muzzle smoke and impact smoke. Shotgun pellet damage is combined per target so invulnerability frames don't cancel pellets.
- New `hollowfrontier:bullet` damage type (tagged as a projectile, so shields block it) with death messages.
- Loaded rounds are stored on the gun (`loaded_ammo` data component) and shown in the tooltip. Right-click fires; an empty gun reloads on right-click. The **R** key (rebindable, "Hollow Frontier" category) reloads early via a client→server packet. Reloading fills the whole magazine from inventory ammo and puts the gun on cooldown. Creative mode has unlimited ammo.
- Ammo: Revolver Cartridge, Rifle Cartridge and Shotgun Shell, crafted from brass nuggets, gunpowder and copper/iron nuggets (and paper for shells).
- Guns have durability (600/450/400) and are repaired with brass ingots. They're crafted from iron, brass gear, brass plate and planks.
- `GunMechanics.fire` is shared, so phase 5 outlaws can use the same guns.
- 6 new placeholder textures. Gun sounds are vanilla stand-ins (listed in ASSETS.md).

## 0.2.0 — Phase 2: Materials & ores (2026-10-26)

- Zinc: Zinc Ore and Deepslate Zinc Ore (stone pickaxe, drop raw zinc with Fortune), Raw Zinc, Zinc Ingot, Zinc Nugget, Block of Raw Zinc, Block of Zinc.
- Brass: Brass Ingot (3 copper + 1 zinc → 4), Brass Nugget, Block of Brass, Brass Plate (4 ingots → 2), Brass Gear (4 ingots + iron nugget → 2).
- Pressure Valve crafting component (gear, plates, copper, redstone), used by later steam machines.
- Zinc ore generates in every Overworld biome: 10 veins of size 9 per chunk, weighted between y -32 and 96 and peaking around y 32.
- Recipes for smelting, blasting and storage blocks, plus block loot tables, models, en_us names and `c:` convention tags (ores, raw materials, ingots, nuggets, plates, gears, storage blocks), all from datagen.
- Recipes use tag inputs (e.g. `#c:ingots/copper`), so zinc and copper from other mods work too.
- The creative tab icon is now the brass ingot. 13 placeholder textures were generated.

## 0.1.0 — Phase 1: Scaffold (2026-10-26)

- Project set up from the official NeoForge 26.3 ModDevGradle MDK: MC 26.3, NeoForge 26.3.0.99, ModDevGradle 2.0.148, Gradle 9.2.1, Java 25 toolchain.
- GeckoLib 5.5.7 added as a required dependency (Cloudsmith maven, `com.geckolib` group).
- Mod id `hollowfrontier` with common and client entry points.
- Empty registries: blocks, items, block entities, entities, menus, data components, sounds, creative tabs. Includes a "Hollow Frontier" creative tab that lists every mod item automatically.
- Datagen wired: recipes and block loot through the 26.x registry-entries event; models, en_us lang, and block/item tags through the client data event.
- `genPlaceholders` Gradle task creates 16x16 placeholder PNGs for missing textures (from model JSONs plus `placeholders.txt`).
- Added ASSETS.md (empty).
