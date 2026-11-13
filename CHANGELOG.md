# Changelog

## 1.4.0 — High noon, Taken, a stake in town, and the Hollow heard (unreleased)

Four things at once: a duel in the street at noon, waking aboard the Grays' ship, a town that knows you (with a journal to keep it all in), and a Hollow you can hear. Existing worlds get all of it.

### High noon

An Outlaw Boss used to be a fight at the gang's camp and nothing else. Now and then one sends word instead: the street, at noon. Existing worlds get all of it.

- **Called out:** about one Wanted Poster in three says its boss would sooner settle it in the street. Be within 200 blocks of that town near noon and you're asked, over the hotbar: Y to go out, N to stay put. No costs nothing; the boss asks once more the next day, then it's the camp as usual.
- **The count:** yes puts you on a mark in the street with the boss twelve paces off. You can't walk, your eyes are held down from the horizon and your trigger is locked. The bell tolls three times, then there's a wait nobody can time, then the word.
- **The draw:** the gun is yours again and it shoots as it always does. The first hit decides it, whatever the boss's health. Pull the trigger before the word and nothing fires, and your gun stays locked half a second longer for each pull.
- **Your own gun, for nothing:** your revolver or ray pistol is fetched to your hand and fires through the duel without using caps, rounds or charge; afterwards it's back in its slot with what was loaded before. With no handgun on you, the law lends a plain revolver and has it back however it ends.
- **As fast as the price:** a boss with a 3 gold bounty fires 0.9 seconds after the word, one with 7 or more in 0.45. The boss hits five times in six. If both miss, both keep shooting.
- **Win,** and the boss is dead in the street, the badge pays half as much again, and the town thinks better of you.
- **Lose,** and you're left at one heart with a tenth of your coin gone (never more than the bounty), the town thinks a little less of you, and the boss is back at camp worth half as much again. With `lethalDuels` on, the shot kills.
- **A duel is between two:** nobody else can hurt either of you until it's over, you can't hurt anybody else, and the town gets out of the line of fire and watches.
- **Afterwards** the town talks about it and the Gazette prints it, either way. The journal notes it under outlaws and on the town's page.
- **Three advancements:** High Noon, Nobody Faster and One Was Enough.
- **Settings:** in `hollowfrontier-duel-server.toml`, `duels` turns it off, `callOutChance` sets how many posters call out (0.33), `duelReach` how near town you must be (200), and `lethalDuels` whether losing kills (off).
- **Keys:** two new ones, Y and N, used only while you're being asked. Both can be rebound.
- Never asked on Peaceful, where there are no outlaws to ask.

### Taken

The Grays' beam used to cost you an hour and a walk home. Now, once they have reason to keep you, you wake up aboard. Existing worlds get all of it.

- **Taken:** the first time a saucer's beam draws you up after the men in black have started minding you, you aren't set down. You wake in a pen on the Grays' ship with nothing on you. After that, about one abduction in four keeps you again. Sneaking or stepping out of the beam still gets you off, and nobody is kept once the leash is broken.
- **Nothing is ever lost.** Everything you were carrying is held for you and is waiting in the ship's hold. However you leave the ship, you have all of it when you're back on the ground.
- **The ship:** pens, a hold, a surgery, a pod bay and an engine room off a ring corridor, in a place of its own. Other people from the frontier are held there, and one is already on the table.
- **You wake fed and whole,** and a bar across the top shows the one coming for you: it fills as it nears your pen, then runs down while its back is turned. That's when to go.
- **The crew** see you at twelve blocks walking and four sneaking, and not at all in the vents (the grilles come off in your hands; nothing else aboard breaks). Keep low and any of them within twenty blocks shows through the walls. There are crates to stand behind round the ring and by the pens' door. Stay in a Gray's sights for seven seconds and the alarm goes up: the doors shut, and open only while one of the crew walks through.
- **The people you free pitch in:** they follow you, each hands over bread they'd kept back, and when a Gray comes after anybody they go for it with their hands.
- **The Shock Rod,** off the surgeons' tray: a blow from it stuns a Gray for three seconds, and anything carrying their implant too (outlaws, men in dusters, tagged beasts). It takes a charge a blow from the Phlogiston Cells you carry, and it's yours to keep. Used on the rig, it stops the procedure for good.
- **In the hold** there's something they've been at with their implants. Let it out and it goes for the crew, not for you.
- **The ways out:** an escape pod sets you down a few hundred blocks from where you were taken, with anyone you freed who reached it. Or pull the three cells out of the engine first: you get ninety seconds, the lights go out from the middle, the deck rolls, the crew run for it, and a crash site is waiting a short walk from where your pod lands.
- **No law aboard:** nothing you do on the ship earns a wanted star.
- **Dying aboard isn't dying.** You wake on the ground as from missing time, with all your things. Leaving the game aboard does the same when you come back.
- **Afterwards** the town has heard, and has its own account of where you were. So do the men in dusters, and the Gazette.
- **The journal** notes it: a line under strange doings, and two more paragraphs on the Grays.
- **Five advancements:** Taken, Checked Out Early, Nobody Left Behind, What Goes Up and Never There.
- **Sound:** the ship has a hum, an alarm and a piece of music of its own, all made for it.
- **Settings:** in `hollowfrontier-ufo-server.toml`, `taken` turns it off (every abduction is missing time again), `suspicion` sets how much the men in black must mind you first (30), and `again` the chance of being kept after the first time (0.25). `abductPlayers` off still turns off all of it.
- **For ops:** `/hollowfrontier ufo taken` keeps you now; `/hollowfrontier ufo taken leave` sets you down.
- **On servers:** two players caught in one beam wake on one ship. Curios slots are left alone.

### A stake in town

A town can now know you. Existing worlds get all of it; only the Old Place standing ready on a town's outskirts needs a town that's new.

- **Standing:** every town keeps its own opinion of you: Stranger, Regular, Neighbour, Pillar of the Town. Bring a bounty to its board, help see off a raid on it, walk one of its people home, or work for its sheriff, and it rises. Shoot up the street and it falls. It's kept when you die, and one town's opinion is nothing to the next.
- **What being known gets you:** a Neighbour pays a coin less for anything dear, and a Pillar a coin less again. A Regular has one small fine a year let go by the sheriff.
- **The Deputy's Badge:** a town's sheriff swears in a Neighbour (in a town with no sheriff, ask any of its lawmen). With the star on you, bounties brought to that town's board pay a gold coin over, and its lawmen come when something goes for you there. Earn two wanted stars anywhere and the sheriff takes it back until your name is clear. One town's star at a time.
- **The sheriff's jobs,** one a day for his deputy. After dark, walk the rounds and report. By day, bring in a drifter alive (shoot him down to half, throw a lasso on him, walk him back), or carry a warrant to the next town's sheriff.
- **The Old Place:** a homestead standing empty at the edge of town, for sale. A two-storey house with a veranda and a balcony over it, a parlour with a hearth and a piano, a kitchen with the table still laid, two bedrooms upstairs; a red barn with a hay loft, a horse, a donkey and tack; a corral of cattle, a kitchen garden, a well and its windmill. Four chests of what was left behind. A Neighbour buys the Deed from the banker or the postmaster and uses it on the stake by the gate. Where a town has no Old Place standing (in a world made before this update, say), use the deed on level ground near town and the town raises one. The land round it is yours to build on.
- **The Mailbox** at your gate gets the Gazette every morning. You can craft more of them.
- **The Branding Iron:** heat it at any fire and put your brand on your cattle. The stake at your gate counts them. Rustlers have heard: they cut the fence and lead a cow off on a rope, so be quick, or keep your stock behind walls.
- **Cattle drives:** sneak and use a wrangler. He'll have a herd held outside town for you, bound for the next town or a buyer down the trail, paid per head that gets there. Stay close to the herd and it walks on; fall behind and it stops; lead one on a rope and the rest follow. A gunshot stampedes them, so think before you answer the riders on your back trail. He also buys your own branded stock, and pays more the farther it's come.
- **Seven advancements,** from Known Around Town to Pillar of the Town.
- **The Prospector's Journal:** your own notebook (a book, leather and a feather; anyone starting out in a town has one). Use it, or press J while you carry it. It keeps the road as you've walked it: a page for each part you've met, what you've done, and the one next thing to do, with the Polar Survey's list and the way to the crater while the gateway is shut. Pin a page to the front. It also keeps what you've seen: a page for each of the mod's creatures once you've met it (more once you've hunted, tamed or bred one), the strange doings you were near with the day and the place (the Gazette's editor pays for a list of ten), what you've learned of the outlaws, the men in dusters, the Kindled and the Grays, and a page for every town you've walked into: when you first came, your standing, your star and the sheriff's job in hand, your place and stock, a herd you have out, what's in the bank, who's wanted on the board and when it was last raided. It says nothing about what you haven't seen. A line over the hotbar says when something's noted (`journalNotes` in the client settings turns that off).
- **Settings:** `hollowfrontier-stake-server.toml` switches standing, deeds and drives, and sets the deed's price and the distances.
- **On servers:** a claim isn't land protection. Only its stake and its mailbox are the owner's alone. Use your usual claim mod for the rest.

### The Hollow, heard, and the frontier, felt

Two updates in one, and neither adds an item, a mob or a block: the Hollow Earth gets a sound of its own, and the things you do most get some weight. Existing worlds get all of it.

### The Hollow, heard

- **A score for the Hollow Earth:** five original pieces, one for each biome, in place of the vanilla biome music that played there before. They grow from the swell you hear as you first fall out of the crust: the Primordial Jungle's is warm and close, the Crystal Steppes' glassy with bells, the Ember Badlands' low and dry, the Concavian Highlands' thin and high with one wordless voice, the Still Sea's slow and deep. They play on the Music slider with the game's usual gaps, and a saloon piano or a record in earshot still holds them.
- **A bed under every biome:** each Hollow biome now has a quiet sound that is always there under its birds and chimes: insects and dripping water in the Primordial Jungle, a wide wind and the crystals' faint hum on the Crystal Steppes, a low rumble and a hiss of heat in the Ember Badlands, thin wind round old stone in the Concavian Highlands, and next to nothing on the Still Sea, where nothing blows. They cross-fade as you walk from one biome to the next, and sit on the Ambient slider.
- **Things heard from far off:** now and then a creature that lives where you are calls from a long way away, in a direction you can turn to: a mammoth across the steppes, a terror bird in the badlands, something singing over deep water. Rarely, the crust rumbles overhead.
- **Gray sites are quiet.** With Gray machines near, nothing calls and nothing rumbles.
- **Under the Still Sea** the surface goes away and a deep, dark sound comes up, louder and lower the further down you go.
- **Aboard the Steam Submersible** you hear its works, fuller under way, and the hull creaks: now and then just under the surface, often at depth.
- **Boss themes:** the Sun Archon and the Mothership each have a theme of their own. It starts when the boss's bar comes up and fades out over a few seconds when the fight ends, however it ends. The Archon's is the Hollow's own music turned hard and bright, a choir over a pulse; the Mothership's is in a key as far from the Hollow's as a key can be, cold bells over a machine's beat.
- **Five moments:** a few seconds of music the first time you meet one of the Kindled, enter the Sun Temple, find a Gray station still running, reach the Drowned Quarter, and break the leash. Each plays once for each player in a world, and only that player hears it.
- **A switch:** `hollowScore` in the new `hollowfrontier-client.toml` (on by default). Turn it off and the Hollow plays vanilla's biome music as it did before, with no boss themes, for players and packs with music of their own. The beds, the far-off calls and the five moments aren't music and stay; they're on the Ambient slider.
- **For pack makers:** the pieces are the sound events `hollowfrontier:music.hollow.<biome>`, so a resource pack can replace any of them.

### The frontier, felt

Nothing here changes what anything does. Guns hit as hard and as true as they did, and horses, airships and beasts move as they did.

- **Guns kick.** Firing throws your view up and a little to one side, and it settles back in a quarter of a second to exactly where it was, so your aim doesn't move. The Blunderbuss kicks most, the rifle next, the revolver least, and the phlogiston guns hardly at all.
- **A ball knows what it hit.** Chips of the block it struck and that block's own sound, in place of the same puff of smoke everywhere. Stone, metal and glass throw a spark, and now and then the ball whines off. Water splashes. A ray leaves a scorch that glows for a moment.
- **Hits and misses are heard.** A hit on a creature lands with a thud for the shooter. A ball that passes within two blocks of you, fired by anyone else, cracks past your ear, so being shot at sounds like it.
- **Gunfights leave a haze.** Three or more black powder shots from one spot inside a few seconds leave thin white smoke hanging for several seconds.
- **Horses raise dust.** Every horse, donkey and mule at a gallop kicks up the ground it's on: a cloud on sand, gravel and dry dirt, a little on grass, a puff on snow, none on stone or boards. Rain lays it. A shod horse rings on stone.
- **Horseshoes show.** A shod horse, donkey or mule wears them: a band of the shoe's metal round each hoof, iron grey, brass or starsteel crimson, and the shoe itself under the foot.
- **The lasso whirls.** Hold use and the loop whirls over your head on a line from your raised hand, swishing as it comes round; let go to throw. A plain click still throws at once, and the throw is the same however long you wind it. In the air it's a loop of rope spinning on a line back to your hand, where it used to be the item's flat picture. A lead or lasso with something pulling on the end of it creaks.
- **The airship:** wind over the deck that rises with speed and with height, a creak from the envelope when you turn or climb hard, wisps of cloud past the deck at cloud height, and a deck that leans a little into a turn.
- **The handcar** clacks over rail joints, quicker with speed, and throws sparks when it brakes.
- **Big things are felt.** A Woolly Mammoth, Megatherium or Glyptodon walking near you lands each foot with a thud, dust and a small shake of the view, harder at a charge. Dynamite, a vault blown in a raid and the Mothership's death ray shake the view by how close you are. A Basilosaurus ramming your submersible jolts it. The Mothership coming in is a tremor before it's a sight.
- **Two sliders:** `cameraKick` and `cameraShake` in `hollowfrontier-client.toml`, each from 0 to 1. At 0 that movement is gone entirely. If a moving view makes you unwell, set both to 0; nothing else changes.
- **For pack makers:** every new sound is a sound event of the mod's built from vanilla sounds, so a resource pack can replace any of them, and no new particle types are added.

## 1.3.0 — The Still Sea and a frontier December (2026-11-10)

Two updates in one: an inner sea for the Hollow Earth, and a frontier Christmas that comes and goes by the date.

### The Still Sea

- **The Still Sea:** the Hollow Earth has a sea. Where the land lay lowest it now runs down pale sand beaches into deep water, with kelp forests and seagrass on the shelf, open water beyond, and falls pouring into it from the crust. Nothing blows and nothing rains down there, so it lies flat. About a fifth of the Hollow is sea.
- **Sea life:** four creatures live in the Still Sea. **Great Sea Cows** graze the kelp shallows in herds; feed them kelp to lead and breed them. A **Plesiosaur** keeps its distance until you hold out a fish; feed it one and it dives to the bed and comes back with something off the bottom, now and then a sherd or a relic. **Lampjellies** drift in glowing shoals and sting if touched. The **Basilosaurus** hunts anyone swimming or boating over deep water: it circles, then comes in. It leaves the shallows alone.
- **What they give:** Blubber renders to **Tallow** in a furnace, which burns as long as coal (in a Boiler too) and makes candles with string. Lamp Gel and glass make the **Jelly Lamp**, as bright as a sea lantern.
- **The Harpoon:** a line weapon, and the sea's own, since black powder misfires underwater. Use it to throw it; it flies as true through water as through air. What it hits takes a jab of 3 and is held on the line. Small things (fish, jellies, most mobs, other players, items afloat) are reeled in to you. Big things (a sea cow, a Plesiosaur, a Basilosaurus) tow you along behind them instead, boat and all. Sneak to let go; the line also gives out after ten seconds or if it's stretched past 32 blocks. Then the harpoon comes back to your pack. The line is a real rope from your hand to the harpoon. Iron, a stick and a lead make one; two Basilosaur teeth make it a **Toothed Harpoon**: a jab of 5, a 48-block line and twice the hold. Held and in flight it's a 3D model.
- **The Steam Submersible:** a brass boat for two that goes under. It has no fire aboard: it runs on steam it was given, 16 buckets of it, from canisters or by floating beside a steam tank, pipe or boiler, so a boiler on the shore now has a job. Use it on water to launch it. Jump to rise, sprint to dive, and let go to hold your depth, which costs nothing. Its crew doesn't drown and sees by its lamp. Run dry and it comes up by itself. It works in any sea, the Overworld's included. Brass plates, glass, Tallow, Lamp Gel, copper blocks and a brass gear make one.
- **A Basilosaurus rams it.** The crew is safe behind the hull, which takes five rams in a row before it breaks up; it mends slowly if left alone. Like the airship, only its owner can break it up, and it folds with its steam.
- **The Drowned Quarter:** on the bed of the Still Sea, under eighteen blocks of water or more, lies what was Concavia's river port: a quay, a street of broken columns still lit by Jelly Lamps, flooded houses with silt to brush, and three glass domes that never let the water in. Swim under a dome's wall and you come up inside, in air. Crystal Guardians are still at their posts in there. The great dome holds the port's vault, and a record of the day the water came.
- **The Sealed Hull:** always in the Quarter's vault. Fitted to a Prototype Saucer it lets the saucer go underwater, and its crew keeps its air. Without it, water now pushes a saucer back to the surface; before, a saucer could fly into the sea and drown its pilot.
- **Fittings you can see:** a saucer with the Sealed Hull wears two bands of sea-green plate round its rim with glowing seams, and one with the Cargo Hold carries a strapped locker under its stern. Until now neither showed.
- **The Symmes Landing:** on the sea's beaches, where the expedition drew up its boat and would not launch it again. Its log says why, and its crate holds a chart to the nearest Quarter and a harpoon.
- **Finds:** two new relics the prospector buys (the River Pilot's Bell and the Kindled Water Clock) and a fourth pottery sherd, the Wave. A Plesiosaur that brings something up now surfaces looking toward the nearest Quarter.
- **The Codex** gains a seventh chapter for the sea, townsfolk have things to say to anyone who claims to have seen it, and the Gazette prints a few of them.
- **Jade** shows a submersible's steam and hull.
- **Ten advancements** for the sea: nine in a branch of their own in the Hollow Earth tab, and the submersible in Steam Age.
- **Existing worlds:** the sea appears in Hollow Earth chunks nobody has visited yet. Places already explored stay as they are. About two thirds of what becomes sea was shallow lake before, so the join is mostly under water; where old dry land meets new sea there may be a step. Nothing else moves: the four land biomes, their terrain and their sites are where they were.
- **For pack makers:** the new biome is `hollowfrontier:still_sea`. It is in `hollowfrontier:is_hollow_earth` and in no vanilla ocean tag, so vanilla's ocean structures and mobs don't appear there. A new tag, `hollowfrontier:is_hollow_land`, holds the four land biomes.

### A frontier December

Everything here comes on by the date and leaves by itself. The date is the server's own, so every player on a server sees the same thing. Nothing needs cleaning up in January, and existing worlds get all of it.

- **The lamps:** through December a town's lamps dress themselves one by one, a sprig and a red ribbon on each, and undress the same way in January. Town Lamps you've placed yourself do it too.
- **The town tree:** a December town has a spruce in its square, about seven blocks tall, in a tub, hung with tin stars and candles that are lit after dark. It can't be hit, pushed or walked into, and it damages nothing: it's there when someone is in town, and gone once the month is over.
- **The Christmas Parcel:** in December the first time you talk to a town's postmaster he hands you a parcel, one a year for each player. In a town with no post office the shopkeeper has it. Use it to open it: coins, and things a frontier relative would send (cartridges, caps, coffee), and a **Fruitcake**.
- **The Fruitcake** takes five seconds to eat, fills you right up and slows you for half a minute. Nobody will buy one.
- **Two December drinks** at the saloon: **Eggnog** (Absorption, and 1 Tipsy) and **Hot Cider** (Regeneration, thaws you if you're freezing, and doesn't make you Tipsy). The barmaids pour them in December only.
- **Carols:** in December the saloon piano works a carol in every other tune: Jingle Bells, God Rest Ye Merry, Gentlemen, and O Come, All Ye Faithful. They stop for a brawl like every other tune.
- **Talk:** about thirty new lines across the frontier's voices, a few overheard exchanges, and Gazette back-page pieces. If there have been lights in the sky, the children decide it was a sleigh, the Gazette corrects them, and the men in black adopt it. The Kindled, who live under a sun that never sets, have the shortest day explained to them.
- **The truce:** from December 24th to 26th no outlaw raid starts. A raid or a posse already under way isn't called off. Over those three days a sheriff will let one fine go, once a year: talk to him with one star. At two stars the law still shoots.
- **No snow:** weather and biomes stay as they are. Desert towns get a dry Christmas, and folk remark on it.
- **Settings:** `hollowfrontier-season-server.toml`, per world. `mode` is `AUTO` (by the server's date), `ALWAYS` (December's things all year) or `NEVER`. Christmas itself always goes by the date, so `ALWAYS` doesn't stop raids for good. `truce` turns the truce and the waived fine off.

### Also in this version

- **Guns show what's in them:** a black powder gun's slot now has a row of small pips along the top, one for each chamber or barrel, brass when loaded and dark when not. Before, a loaded gun and an empty one were the same picture.
- **Quick Load:** the reload animation now keeps pace with the faster loading. Before, the gun loaded sooner but the hands moved at the old speed and were cut off mid-stroke.
- **Dusters have tails:** the outlaws', the men in black's and the U.S. Marshal's long coats now hang to the knee, open in front, and swing as they walk and ride.
- **Fern Fronds redrawn:** the crowns of the Jungle's tree ferns read as fronds now, each with a midrib and leaflets. Before, they looked like scattered pixels.

## 1.1.4 — New worlds start in town, new faces, and safecrackers find the way in (2026-11-07)

- **New worlds start in town:** a new world now puts you on open ground in the nearest town's square, and you come back there if you die with no bed. With no town within about 10,000 blocks you start where the game would have put you. Existing worlds keep their spawn. Set `spawnAtTown` in `hollowfrontier-towns-server.toml`, in the config folder, before creating the world. On a dedicated server with operators, set `spawn-protection=0`, or other players can't use the doors and chests round the square.
- **Towns replace their dead:** when a townsperson or lawman a town started with dies, someone new slowly takes their place: one at a time, about one a Minecraft day, in the same job at the same post, and out of players' sight. A town never grows past what it started with, and nobody arrives during a raid. It works in existing towns, for deaths from this version on. Set `arrivals` and `arrivalTime` in `hollowfrontier-towns-server.toml`.
- **Lootr:** town chests are per-player Lootr chests again, and they open. 1.1.3 kept towns out of Lootr to protect raids, but with Lootr installed that stopped town chests opening at all, and a raid could find an empty vault. Bank vaults and the sheriff's strongbox stay ordinary shared chests, so raids can still rob them. They fill when their chunk loads, in existing towns too.
- **Raids:** the safecracker now finds his way round to a bank's front door. Before, he gave up after looking 40 blocks ahead, so he could end up standing at the bank's back wall and never reach the vault.
- **Town talk:** seeing a saucer no longer makes folk talk as if you've been to the Hollow Earth. Lines about going down the hole now wait until you have.
- **Town searches:** a search that finds no town is faster in rough country such as amplified worlds. Towns still stand in the same places.
- **Startup warning:** the warning about a deprecated `logoFile` property is gone.

## 1.1.3 — Raids reach the vault (2026-11-06)

- **Raids:** a gang that rides in can now get through doors on foot. Before, outlaws learned doors while mounted, so the skill went to the horse, and a mounted raid could never reach a vault indoors.
- **Sheriff's strongbox:** the safecracker now works it from in front of the lid. Before, a jail cell's iron door beside it was taken for a vault door, and he was sent behind it, where he could never start.
- **Lootr:** towns now stay out of Lootr's conversion, so raids can still find their vaults. Town chests are shared rather than per-player when Lootr is installed.

## 1.1.2 — Faster town searches (2026-11-05)

- **Town searches:** `/locate` for a town is many times faster, and no longer risks stopping a dedicated server when no matching biome is near. Before, a town's ground survey ran at every candidate spot, even in the wrong biome. A few more towns may appear along biome edges in new chunks.
- **Terralith:** its Hot Shrubland, Shrubland and Yosemite Lowlands biomes can now hold towns and outlaws, which brings desert towns a good deal closer. Without Terralith nothing changes.

## 1.1.1 — Dedicated servers (2026-11-05, first uploaded in 1.1.2)

- **Dedicated servers:** Hollow Frontier now starts on a dedicated server. Before, every release stopped at startup with a missing client class (`net.minecraft.client.model.Model`), because the guns, Steam Drill, Sunstone Lens and Polar Resonator touched client-only GeckoLib code as they loaded. Single-player was never affected.

## 1.1.0 — Playtest fixes and phlogiston guns (2026-11-05)

- **Crash fixes:** a world could crash when a Megatherium finished browsing or gave up on reaching leaves, or when a Fern Skitter snatched food. Both are fixed.
- **Airship:** you can eat, fire a gun and use a compass aboard; clicks no longer land on the airship around you. Your own swings from aboard no longer damage it either. You can still stoke it with coal or top it up from a canister while flying.
- **Townsfolk:** rugs and aisles in town buildings stop a tile short of the door, where townsfolk got caught on them (in newly generated towns). In any town, a townsperson who can't reach where their routine sends them now waits a while before trying again, instead of working a door open and shut.
- **Getting off an airship:** more than 4 blocks up, the first sneak only tells you how far down it is; sneak again to step off. Leaving a ship aloft, by choice or not, gives you slow falling all the way down. A ship left with nobody aboard for 30 seconds sinks gently to the ground and waits there.
- **Polar Compass:** it takes its own bearings as you carry it (and again every 256 blocks, so a nearer gateway takes over), and now points at the middle of the gateway's ring rather than a spot nearby. Hold it to read how far and which way; at the helm the reading joins the airship's gauge line.
- **Crash sites:** a sealed Gray Cargo Pod stands in the cockpit (in newly generated crash sites). Use it to break the seal: it spills salvage and at least one of five new Gray gadgets, which the prospector buys for gold and which each do one small thing: the Humming Doohickey hums at nearby ore, the Self-Warming Rock keeps powder snow from freezing you, the Backward Watch tells you how long until dusk or dawn, the Restless Marble hops you up and lets you down gently, and the Unopenable Box doesn't open (usually).
- **Polar Resonator:** it now holds one opening's worth of steam itself (charge it on a boiler, tank or pipe, like a canister). Opening a gateway takes its own charge first, then canisters you carry, then your airship if it's within 16 blocks. Its tooltip and the Open Sesame advancement say it needs steam.
- **The road north:** every newly generated town now has a post office on its square. The first time you talk to a town's postmaster, you're handed the Polar Survey's last letter. Like the Concavian Codex, it ticks off the road as you go (a boiler, steam to carry, an airship, a Polar Compass, a Polar Resonator, the gateway), says how to get each, and gives a rough heading to the nearest gateway. Every bounty board with room now carries a notice, "News of the Polar Survey", that pays 8 gold once you've opened a gateway. Townsfolk mention the Survey (more so once you have an airship), the Gazette points to the letter, and the prospector sells a Polar Compass for 3 gold.
- **Polar Resonator, redrawn:** a brass tuning fork with cyan-lit tips on a steam gauge. Held, it's a 3D model; when it wakes a gateway's ring the fork shivers and the gauge needle swings into the red until the well opens.
- **Steam Drill and Sunstone Lens, held:** both are 3D models in hand now (their icons stay as they were). The drill holds steady while it works and its bit spins as you drill with steam; the lens's sunstone glows and flares when it throws a beam.
- **Creative menu:** everything is in one Hollow Frontier tab (it was six), in the same order, with all the spawn eggs together at the end.
- **Phlogiston guns:** now a real step up from black powder. The Ray Pistol hits for 10 (was 7) and the Phlogiston Rifle for 30 (was 20), and a beam sets what it hits alight. A new enchantment, Focus, does for them what Rifling does for black powder guns; before, a fully enchanted Plains Rifle could out-damage the Phlogiston Rifle. Find it at the enchanting table, from librarians and in general loot.
- **Gun tooltips:** every gun now shows what it hits for, enchantments counted.
- **Beams from the saddle:** a phlogiston gun fired while riding no longer hits your own mount.
## 1.0.0 — First release (2026-11-01)

The first public release, for Minecraft 26.3 on NeoForge (needs GeckoLib).

- **The frontier:** black powder revolvers, rifles and muzzleloaders you load by hand, with six gun enchantments. Towns with names, saloons, sheriffs, banks, a newspaper, a railroad depot and townsfolk who keep a daily routine. Bounties, wanted posters, wanted stars and posses, outlaw raids on towns at night, blackjack, saloon drinks and brawls, dust devils, rattlesnakes and town cats.
- **The steam age:** boilers, copper pipes, tanks and gauges (`c:steam`, so other mods' pipes and tanks work), the Steam Crusher, Steam Drill, Steam Handcar, and a coal-fired airship.
- **The Hollow Earth:** wake a Polar Gateway and fall through Symmes' Hole into a world under a golden crust with an inner sun. Four biomes, megafauna (woolly mammoths, terror birds, ground sloths, glyptodons), starsteel and phlogiston, lore sites, dungeons and loot.
- **Concavia:** the Crystal Ruins and the Kindled, their story, and the Sun Archon boss fight.
- **The Grays:** sightings, cattle mutilations, crop circles, men in black, abductions, Gray stations and pens, crash sites, phlogiston weapons, your own Prototype Saucer, and the Mothership boss fight.
- **After the Mothership:** Gray landings, the Mothership's return, freed outlaws settling down, restoring the Crystal Ruins and their waystones, and a stranger at the saloon.
- **Optional support** for JEI, Jade and Curios, glowing parts under Iris shader packs, and per-world configs for raids, mysteries, saucers, the saloon and dust devils.
