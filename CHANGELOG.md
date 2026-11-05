# Changelog

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
