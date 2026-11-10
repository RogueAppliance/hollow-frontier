# Changelog

## 1.3.0 — The Still Sea (unreleased)

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

## 1.1.4 — New worlds start in town, new faces, and safecrackers find the way in

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
