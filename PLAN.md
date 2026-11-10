# Hollow Frontier — Plan

Wild West steampunk + Hollow Earth + Concavia + UFOs, for NeoForge.

## Lore

Cowboy prospectors build steam tech to reach a Hollow Earth lit by an inner sun, entered through polar gateways that explorers call **Symmes' Hole**. Inside lie the ruins of Concavia, and the truth about what has been troubling the frontier.

**Two peoples**
- **The Kindled** are the surviving Concavians: luminous and benevolent; the frontier calls them Lantern Men. Their civilisation built the crystal cities and mastered phlogiston, then fell. A few remain, hidden. They become the player's allies (phase 12a).
- **The Grays** are the antagonists. They toppled Concavia, took its craft and phlogiston, and now run things from the shadows. They are **making the outlaws**: abducting drifters, implanting them and turning them loose on the Overworld to keep the surface weak and divided. They also **experiment on animals**. The UFO flyovers, abductions, Gray Wardens and the Mothership are theirs.

**Threads woven through the plan** (each lands in the phase named):
- **Frontier-era UFO folklore.** The world echoes the Great Airship Wave of 1896-97 (cigar-shaped "airships" with lights seen across the West) and the 1897 Aurora, Texas crash story, whose pilot was said to be buried in the town cemetery. Uses: Gazette rumours of airship sightings (9c, more in 11a), a rare strange headstone on Boot Hill (9c seed, payoff in 11a), lost Symmes expedition journals (9b), and flyovers and crash sites styled as those "airships" (13a). Folklore-flavoured and fictionalised: no real people's names on graves.
- **Cattle mutilations and crop circles** (Overworld, 11a). Ranch cattle near towns are now and then found dead with neat surgical marks, and wheat fields show flattened circles overnight: Gray experiment sites with a faint trace left behind. Wranglers grumble, the Gazette reports it, and late game a player can stake out a field and catch a probe in the act.
- **Men in black dusters** (11b). Human agents working for the Grays. They turn up after sightings, buy strange items, lean on the Gazette editor and warn the player off; unsettling neutrals at first, enemies once the player is a threat. **Decision:** they are *implanted collaborators*, the outlaws' finished product: drifters the Grays took, implanted and sent back as handlers instead of gunmen. Most do it willingly for phlogiston-age money and long life; the implant is the leash. That ties them to the outlaws (same process, better kept) and gives 13c something to break.
- **Gray experiments on megafauna** (10b). Beside the wild megafauna (10a), rare **tagged/augmented variants** (a mammoth with metal plating and a glowing implant, a faster, stranger terror bird) come from Gray field labs. They're tougher, drop Gray tech fragments, and point toward the labs.

**Progression as story:** frontier (outlaws and cattle mutilations are symptoms) → steam (reach Symmes' Hole) → Hollow Earth (find both peoples' traces and the Gray labs) → Concavia (the Kindled, the truth) → UFO/phlogiston tech (take the fight to the Grays and their men in dusters) → aftermath (the leash broken, the frontier rebuilding).

The Grays fly Concavian craft they stole, and their Gray Wardens guard the crash sites. The Crystal Guardians are Kindled constructs, still guarding the ruins against the Grays (and anyone else).

## Versions (checked 2026-10-26)

| Thing | Version | Notes |
|---|---|---|
| Minecraft | **26.3** | Latest release (2026-09-15) in Mojang's version manifest. 26.4 is still in snapshot. |
| NeoForge | **26.3.0.99** | Latest non-beta on maven.neoforged.net. |
| ModDevGradle | **2.0.148** | `net.neoforged.moddev`, with the Gradle wrapper from the official 26.3 MDK. |
| Java | **25** | Required by MC 26.x. The Gradle toolchain (foojay resolver) downloads it. |
| GeckoLib | **5.5.7** (NeoForge, 26.3) | Required dependency. Used for mobs, bosses, the airship, the saucer and gun models. |
| Mappings | Official Mojang names | 26.x ships unobfuscated, so Parchment isn't needed. |

## Dependencies

The user allows existing mods, shaders and texture packs. Availability on 26.3 NeoForge was checked on Modrinth (2026-10-26).

- **Required:** NeoForge, GeckoLib. That's all.
- **Optional compat (compileOnly + soft-loaded):** JEI (MIT) for the crusher's recipes and info pages, Jade for machine and vehicle tooltips, and Curios (LGPL) for the Sunstone Lens (charm) and Kindled Sigil (necklace), which otherwise work held in either hand. Each sits behind a `ModList.isLoaded` check in its own `compat/` package. Iris is `compileOnly` too, for the glow-mask compat.
- **Dev-only runtime (optional):** Sodium + Iris, with `-Pshaders`, for checking renders under shaders and filming the shader trailer. They're never required.
- **Wanted but not on 26.3:** Create (newest is 1.21.1), Valkyrien Skies (1.20.1, Forge), TaCZ guns (1.20.1, Forge). Dropping to 1.21.1 for Create alone isn't worth losing GeckoLib/JEI/Iris parity and the newer API, so we stay on 26.3 and build steam, airships and guns ourselves. If Create gets a 26.3 port, add optional steam↔Create compat (our boiler feeding Create steam engines).
- **Not needed:** TerraBlender (we add no Overworld biomes; the Hollow Earth uses its own biome source), Architectury (NeoForge-only mod), Patchouli (not on 26.3; the Concavian Codex is a vanilla-style written book plus advancements).
- **Shaders:** used on the player's side only. Recommended in README: Complementary Reimagined and Bliss (both 26.3). Their licenses forbid bundling, so they're never shipped.
- **Texture packs:** used only as a style reference (e.g. Faithful 32x is on 26.3). Their art is never copied or bundled unless the pack is CC0 or CC-BY with attribution, and anything like that gets recorded in ASSETS.md.

## Standing decisions

The ones every session needs. The full log (engine notes, each phase's calls and the work since) is in DECISIONS.md; the original phase specs are in PHASE_SPECS.md.

- Mod id `hollowfrontier`, base package `com.hollowfrontier`. Display name "Hollow Frontier".
- The Overworld has no real poles, so a **Polar Gateway** is a rare ice-crater structure in frozen biomes. A **Polar Compass** points to the nearest one. A steam-tech **Polar Resonator** opens it, which gates the Hollow Earth behind steam tech.
- **Brass** = vanilla copper + a little gold. No zinc or tin: the ore list stays small, and copper gets a use.
- **Steam** is a NeoForge fluid (mB) moved through the standard fluid capability/transfer API. There's no custom energy system. Boilers turn water + furnace fuel into steam.
- **Guns** are hitscan (raycast) with a short tracer particle. Ammo and reload state live in item data components, and there's a cooldown per gun. This avoids projectile-entity edge cases.
- **Lasso** is a thrown entity. On a hit it leashes the mob, reusing vanilla leash logic, and pulls small mobs toward the player.
- **Horse upgrades** = horseshoes (tiered speed/jump attribute modifiers) + saddlebags (extra inventory). They're applied by right-clicking a tamed horse and stored as an attachment.
- **Bounties:** a Bounty Board block gives out Wanted Posters naming a nearby Outlaw Boss. Turning in its badge pays out in Gold coins. Copper and gold coins are minted from 4 vanilla nuggets (2x2) and exchange 9 copper = 1 gold both ways.
- **Structures** (saloon, town, ruins, crash sites, gateway) are defined as code blueprints. A datagen provider writes them to `.nbt` templates, so no client is needed to author them. They're placed by jigsaw/template structures.
- **Hollow Earth sky:** fixed time, always lit, with a custom dimension sky effect that draws a static "inner sun" overhead plus a hazy crust "ceiling" color. The terrain is normal noise with a high bedrock crust ceiling, not a true inverted sphere.
- **Rendering stays shader-safe:** the custom sky and beams use vanilla render types, so Iris shader packs don't break them. Phase 9a notes any limits found.
- **Placeholder art:** a Gradle task (`genPlaceholders`, java.awt, no extra tools) writes flat-color 16x16 PNGs with a 1px border for every missing texture. GeckoLib entities get a minimal box `.geo.json`. Everything is listed in `ASSETS.md`.
- **License:** MIT (chosen by the user 2026-10-29 so the mod can go in modpacks freely), copyright and `authors` under the handle RogueAppliance. `LICENSE` excludes only the reworked-vanilla textures (Mojang's); every bundled sound is ours (the piano's Joplin compositions are public domain); the MDK's own MIT notice stays in `TEMPLATE_LICENSE.txt` for the build files that came from it. The jar carries the license as `LICENSE_hollowfrontier`.
- **Creative tabs:** one Hollow Frontier tab (since 32a), its sections ordered by hand in `registry/ModTabs` only, spawn eggs last; a test fails until a new item has a place.
- **Every item and mob needs a use:** a recipe input, a mechanic, a trade it unlocks, or a drop with one of those. Selling to townsfolk alone doesn't count.
- **Sound:** sci-fi sounds (lasers, beams, saucers) are synthesized in `tools/music`; frontier effects are re-pitched vanilla sound events; music is in-house or public domain/CC0. Credit anything CC BY wherever it's used.
- **Humour** is deadpan and comes from how the frontier reacts; the story itself stays serious. No memes, fourth-wall jokes, references to other works, or real people's names.

## Content

What's in the mod is described in README.md (short) and CURSEFORGE.md (the full store page). Keep those current rather than a list here.

## Phases

Each phase ends with `gradlew build` passing. Keep phases small and finish each one before starting the next. Each entry is one session's job; start a session with "Do phase N per PLAN.md". Every phase also adds game tests in `gametest/`, placeholder art via `genPlaceholders` (then `TextureArt`), ASSETS.md entries, lang, a CHANGELOG entry, runPhoto shots where something is visual, and its decisions in DECISIONS.md.

All phases so far are done. Their original specs are in PHASE_SPECS.md. Add new phases below as numbered entries with a spec and a *Done when:* line, and shorten each to one line here once it's done.

1. ✅ 2026-10-26 — Scaffold: MDK, Java 25 toolchain, registries, datagen wiring, `genPlaceholders`.
2. ✅ 2026-10-26 — Materials and worldgen ores: brass, gears, plates.
3. ✅ 2026-10-26 — Guns and ammo (reworked 2026-10-27 into black powder muzzleloaders with GeckoLib models).
4. ✅ 2026-10-26 — Cowboy gear: armor set, lasso, horseshoes, saddlebags.
5. ✅ 2026-10-26 — Outlaws and bounties: outlaws, Outlaw Boss, Bounty Board, coins, Townsfolk.
6. ✅ 2026-10-26 — Frontier structures: blueprint→NBT datagen, Saloon, Frontier Town, Outlaw Camp.
7. ✅ 2026-10-26 — Steam power: boiler, pipes, tank, gauge, crusher, canister, drill.
8. ✅ 2026-10-28 — Airship and polar gateway.
9. ✅ The Hollow Earth: 9a the world itself (2026-10-28); 9b1 flora and ambience (2026-10-28); 9b2 small creatures (2026-10-29); 9b3 lore sites (2026-10-29); 9c ores and gear (2026-10-29).
10. ✅ 2026-10-29 — Megafauna: 10a wild megafauna; 10b Gray experiments.
11. ✅ 2026-10-29 — Frontier mysteries: 11a sightings and signs; 11b men in black dusters.
12. ✅ 2026-10-29 — Concavia: 12a the Kindled; 12b the Sun Archon.
13. ✅ 2026-10-29 — The Grays and their craft: 13a the Grays arrive; 13b phlogiston tech; 13c the Prototype Saucer; 13d the Mothership.
14. ✅ 2026-10-29 — Polish and release.
15. ✅ 2026-10-30 — Aftermath (after `Leash.isBroken`): 15a the holdouts; 15b the Mothership's return; 15c what the frontier makes of itself.
16. ✅ 2026-10-30 — The bank: safe deposit boxes, interest, raids on deposits.
17. ✅ 2026-10-29 — The card table: blackjack against the house.
18. ✅ 2026-10-29 — The saloon bounty board: posted bounties everyone sees.
19. ✅ 2026-10-30 — Voices: the shared `Chatter` system for every speaking NPC.
20. ✅ 2026-10-31 — The captives: Gray holding pens and freeing captives.
21. ✅ Frontier humour: 21a words (2026-10-30); 21b Tipsy and the saloon (2026-10-31); 21c Grays in disguise (2026-10-31).
22. ✅ 2026-10-31 — Gun enchantments: Rifling, Quick Load, Deadeye, Choke, Dry Powder, Frugal, and vanilla Infinity.
23. ✅ 2026-10-31 — Towns, built properly: level, dry sites; furnished buildings with shop signs; gunsmith, doctor, post office, homestead, schoolhouse; a livelier square; loot per trade.
24. ✅ 2026-10-31 — Towns, lived in: full lots and side yards, named welcome arches, the railroad out of town; bathhouse, Gazette office, ranch; town lamps, chairs and tables, six new folk, daily routines.
25. ✅ 2026-10-31 — Hollow sites rebuilt: dark Kindled stone, real interiors, the crystal city's districts, the Sun Temple's approach and throne hall, the Symmes gondola and dig.
26. ✅ 2026-10-31 — Hollow dungeons: Kindled Catacombs (sealed vault puzzle), the buried Gray complex, the Sun Temple undercroft, grown by jigsaw under their sites.
27. ✅ 2026-10-31 — New Hollow sites: ziggurat, tar pit, phlogiston mine and cavern, observatory, foundry, Sky Shrine on the islands, Gray relays under the crust.
28. ✅ 2026-10-31 — Hollow loot: tiered room tables, eight relics the prospector buys, explorers' maps, the Concavian trim, Hollow sherds and archaeology.
29. ✅ 2026-10-31 — Dust devils: whirls over the dry country that lift, fling and take hats, spook unshod horses and leave odds and ends; Ash Devils in the Ember Badlands; talk, Gazette and men in black cover stories.
30. ✅ 2026-10-31 — Modpack friendliness: item capabilities on the Boiler and Crusher, `c:steam`, `c:is_snowy` Polar Gateways, `c:bosses` and capture/teleport tags, tag-based dust smelting, mystery and men-in-black switches.
31. ✅ 2026-10-31 — Polish: 31a airship owner lock and fuel kept when broken; 31b no deprecation warnings, dev rigs out of the jar; 31c humanoid model with brim curls, bandanas, and dress and robe skirts (duster tails on hold).
32. ✅ 2026-11-01 — Playtest fixes for 1.1.0 (from the 2026-11-01 playtest; the user's picks are in DECISIONS.md). Log each part under a new CHANGELOG heading.
    - ✅ 2026-11-01 — 32a Fixes: items usable aboard the airship and no self-damage from its riders; carpet kept off town door thresholds and a routine back-off after failed paths; one Hollow Frontier creative tab.
    - ✅ 2026-11-01 — 32b Airship safety and the Polar Compass: a second sneak to get off aloft, slow falling after, unattended ships come down; the compass takes its own bearings on the ring's middle and reads out while held (in the gauge line aboard).
    - ✅ 2026-11-01 — 32c Crash cache and resonator: a sealed Gray Cargo Pod in the crash sites' cockpit with salvage and five Gray gadgets (each with a trick, all bought by the prospector); the Polar Resonator holds its own charge and draws on your airship nearby.
    - ✅ 2026-11-01 — 32d The road north: the Polar Survey's letter from the postmaster (a checklist like the Codex, with a heading to the crater), its notice on the bounty boards paying once a gateway's open, and Chatter, Gazette and prospector nudges.
33. ✅ 2026-11-05 — Late-game guns: Ray Pistol 10 and Phlogiston Rifle 30, beams that set alight, the Focus enchantment, damage in every gun's tooltip, and a test holding the phlogiston guns ahead of black powder.
34. ✅ 2026-11-05 — Crash fixes: the Megatherium's browse and the Fern Skitter's snatch no longer crash the world on the tick after they end.
35. **A frontier December.** Content that comes on by the real date, and leaves by itself. Each part is one session; 35a goes first and the rest in any order. Log all of it under one CHANGELOG heading (1.2.0), with a December section in CURSEFORGE.md and README.md. To reach players this year it has to be uploaded by late November.

    **Ground rules**
    - **The date is the server's.** `content/frontier/season/Season` reads the server's local date: `isDecember(level)` for all of December, `isChristmas(level)` for Dec 24–26 (vanilla's Christmas chest window). A per-world config, `hollowfrontier-season-server.toml`, holds `mode` (`auto`, `always`, `never`; default `auto`) and `truce` (default true). Tests set the date through a clock seam in `Season`.
    - **No client date and no new payloads.** Everything reaches the client through paths that already exist: blockstates, entities, chatter, the Gazette, the piano.
    - **Nothing to clean up.** Every world change undoes itself in January. Items players already have stay and keep working.
    - **Old worlds get all of it.** No worldgen changes, so nothing depends on a chunk being new.
    - **No snow.** Weather and biomes stay as they are. Desert towns get a dry Christmas, and folk remark on it.
    - **Tone:** deadpan, from how the frontier reacts. The story stays serious. The figure with the sleigh is never named, and nothing quotes a later work (no red noses).

    - **35a The season and the lamps.** `Season` and its config. Town Lamps get a `FESTIVE` property, a sprig and red ribbon on the existing lit and unlit models, and flip it on the random tick that already handles `LIT` (one `setBlock` for both), so a town dresses lamp by lamp in December and undresses the same way in January. New art listed in ASSETS.md.
      *Done when:* a game test shows lamps dressing and undressing by date, `mode` overrides the date both ways, and a `runPhoto` shot shows a dressed street at dusk.
    - **35b The town tree and the parcel.** A town tree: a GeckoLib entity, not a block, so it can't damage a town or a player's build. About seven blocks of spruce with tin stars and candles (a glow mask at night). `TownPresence.update` spawns one on clear ground nearest `TownSite.rally` when a player is in a December town and none stands within range; it discards itself once the month is over. No collision, invulnerable, can't be pushed, and tagged against capture and teleporting. A Christmas Parcel: in December, the first talk with a postmaster hands over one parcel per player per calendar year, in the style of the Polar Survey letter (the year kept in an attachment that survives death). Pre-1.1.0 towns may have no post office, so a shopkeeper hands it over there. Use it to open it, as with the Gray Cargo Pod: a loot table of coins, paper cartridges, percussion caps, coffee, and a Fruitcake. The Fruitcake is food that takes five seconds to eat, fills you up and slows you for 30 seconds. Nobody will buy one.
      *Done when:* tests cover one tree per town, no tree after December, no spawn outside the Overworld, and one parcel a year; a `runPhoto` shot shows the tree in a square at night.
    - **35c The saloon.** Two December drinks. Eggnog: Absorption and 1 Tipsy. Hot Cider: Regeneration, thaws freezing, no Tipsy. Barmaid offers are built once, so these are added when trading opens in December and removed at other times. Three carols for the Saloon Piano from public-domain compositions: Jingle Bells (1857), God Rest Ye Merry, Gentlemen, and O Come, All Ye Faithful. Write the MIDI from the old scores, not from a modern arrangement, and render through `tools/music` like the Joplin rags. In December `nextTune` plays a carol every other tune. Sources go in ASSETS.md.
      *Done when:* tests show the offers come and go with the date and the repertoire changes; the carols are checked by ear in `runClient`, and they stop for a brawl like every other tune.
    - **35d Words and the truce.** `Fact.DECEMBER` and `Fact.CHRISTMAS`, set in `Context.of`. About 30 chatter lines and a few exchanges across the voices, plus Gazette back-page pieces under the existing length limits. Threads:
      - **The sleigh.** With `DECEMBER` and `SIGHTINGS`, children and old-timers decide the lights in the sky were a sleigh, the Gazette corrects them, and the men in black adopt it as a cover story, worse each time ("It was a sleigh. We have looked into it. You may stop asking.").
      - **The Gray at the church social:** `CHRISTMAS` and `STRANGER` lines about a guest who sings every carol half a beat behind everyone else.
      - **The Kindled:** they live under a sun that never sets and have the month explained to them. They find the idea of a shortest day sad.
      - **The jail:** the cell's outlaw gets a plate and has opinions on it.
      - **The truce.** With `truce` on, no outlaw raids start during `isChristmas`; a posse already after a player isn't called off. A sheriff waives one fine per player per Christmas, using the Wanted fine path, so it only reaches players with one star. At two stars the law still shoots.
      *Done when:* tests show December lines only appear in season and fit their length limits, no raid starts during the truce, and the waived fine works once a year.
36. ✅ 2026-11-05 — Dedicated servers: the mod starts and plays on a real server (1.1.1). Found while testing the Saddles N' Saucers modpack; every earlier release stopped at startup.
37. ✅ 2026-11-05 — Town searches: a biome check before the ground survey (1.1.2), and Terralith's level dry biomes in the town tags.
38. ✅ 2026-11-05 — Raids reach the vault (1.1.3): mounted gangs keep their door skill on foot, and a strongbox no longer takes a cell door for a vault door.
39. ✅ 2026-11-06 — Safecrackers find the way in (1.1.4): he looks 128 blocks ahead for a route, so a bank whose front door faces away from him no longer stops a raid.
40. ✅ 2026-11-06 — Towns replace their dead (1.1.4): residents who die leave a place on their town's list, and replacements arrive one a day, in the same job at the same post, out of players' sight.
41. ✅ 2026-11-06 — Town chests under Lootr (1.1.4): towns come off its blacklist so ordinary chests go per-player, and vault chests roll their loot on load so they stay plain and can be robbed.
42. ✅ 2026-11-06 — New worlds start in town (1.1.4): the world spawn goes on open ground in the nearest town's square, with `spawnAtTown` to turn it off and the game's own spawn when no town is in reach. Replaces the starting map idea.
43. ✅ 2026-11-09 — **The Still Sea.** A fifth Hollow Earth biome: an inner sea, the mod's first swimming creatures, a steam submersible, and a drowned district of Concavia on the seabed. It's a side road: the Codex's existing chapters and the main story don't change. Each part is one session; 43a goes first, 43b and 43c in either order, 43d last. Log all of it under one CHANGELOG heading (1.3.0), with a Still Sea section in CURSEFORGE.md and README.md. Numbered 43 because main already holds 36–42 (the 1.1.1–1.1.4 fixes): start it from main, after phase 35 has shipped.

    **Ground rules**
    - **The land doesn't move.** No noise seed and none of the four land biomes' thresholds change, so existing land keeps its shape and its biome everywhere the sea isn't. The sea takes only the lowest continentalness, where `HollowTerrain`'s land (72 + 30 x continents) already dips to about sea level 63.
    - **Old worlds get the sea in new chunks only.** Explored chunks stay as they are, so an old world can show a step where they meet new sea. Say so in the changelog, and check how it looks on a copy of a 1.1.x world before release. Everything that isn't terrain (the creatures' spawns, the submersible, recipes) works in old worlds at once.
    - **Under the ceiling.** Heightmaps find the crust, so seabed features need a scan that carries on down through water (`open_air_scan` stops at the first solid block from the air). The crust's spires and waterfalls come down into the sea as they do the land.
    - **Not a vanilla ocean.** The biome stays out of `minecraft:is_ocean` and the vanilla `has_structure` tags, so no monuments, shipwrecks or drowned turn up. Spawns and sites go by our own `hollowfrontier:has_*` tags, as the megafauna do.
    - **Any age of the deep past** (the user's call, 2026-11-09): the land's megafauna are Ice Age, but the sea may hold older things, plesiosaurs included. Down there, nothing ever had a reason to die out.
    - **The standing rules hold:** every item and mob needs a use; no references to other works (no named submarines or captains); sounds are re-pitched vanilla or synthesized.
    - **Lore: the sea is younger than Concavia.** When Concavia fell, the Grays broke the crust over its harbour district, and the falls have been filling the basin ever since. That's why water pours from the crust at all. The Kindled remember the district dry. It's told in what's found down there, not in new Kindled speech (their nine lines stay scripted).

    - ✅ 2026-11-09 — 43a The sea itself: `still_sea` where continentalness is under -0.30 (18% of the Hollow), a beach, a shelf and a bed 15 to 35 down, kelp and seagrass by a seabed scan, `is_hollow_land` for the sites; the land biomes and their terrain held exactly as they were by test. Not shot: a sea stack, and the seam where old dry land meets new sea (see DECISIONS.md).
    - ✅ 2026-11-09 — 43b Sea life: Great Sea Cow (grazes kelp, breeds, Blubber), Basilosaurus (circles and strikes over deep water, teeth), Plesiosaur (dives for a fish, brings up finds), Lampjelly (stings, Lamp Gel), placed by `SeaLife`'s own spawner by depth; Tallow, the Jelly Lamp, the Harpoon and Toothed Harpoon (a line weapon since the playtest: it holds what it hits, reels in the small and is towed by the big; a 3D model held and thrown); six advancements. The Plesiosaur's look toward the Drowned Quarter waits on 43d (`SeaLife.quarterBearing`).
    - ✅ 2026-11-09 — 43c The Steam Submersible: a fireless brass boat for two (16 B of steam from canisters or any steam holder beside the hull), launched on water, owner-locked, folding with its steam; rises dry, holds depth for nothing, keeps its crew in air and lamplight; a hull of 40 that a Basilosaurus rams; recipe, "No Fire Below", Jade. The user's playtest led to an art pass on the model (a glazed dome, ballast tanks, lit ports).
    - ✅ 2026-11-09 — 43d The Drowned Quarter: a sunken Kindled river port placed by a new seabed mode, with three glass domes that hold their air, Crystal Guardians in them, a vault that always holds the Sealed Hull (a sixth saucer fitting: the saucer dives), two relics, the Wave sherd and a dig; the Symmes Landing on the beaches (shore mode) with a chart and the boat's log; a seventh Codex chapter; the frontier's disbelief in Chatter and the Gazette.

## Open items

- **Publishing:** 1.0.0 was uploaded on 2026-11-01 (CurseForge project 1778329, Modrinth Mvsn4kHn) 1.1.0 and 1.1.2 to CurseForge on 2026-11-05 (1.1.1 went out inside 1.1.2), and 1.1.3 to CurseForge on 2026-11-06. Modrinth was still reviewing the project, so its first update, the newest version, goes up there once it's approved. Log later changes under a new heading above it in CHANGELOG.md. How to release: RELEASING.md.
- **Trailer leftovers:** the plain (no-shader) trailer, the vertical short and the page GIFs predate the current trailer world and the ember phlogiston. Re-film or re-cut them before use. A true 4K trailer would need re-filming at 4K (AMD VSR on the 1440p monitor); the current 4K file is an upscale.
- **The Kindled's look:** kept for now (the user's call); mock-ups of alternatives exist (see DECISIONS.md, art passes).
- **Known gaps:** Quick Load speeds loading but not the reload animation; loaded and empty guns look the same in the creative grid; duster tails are on hold; the fern frond still reads as scattered pixels.

## Risks

- 26.x APIs (datagen events, transfer API, rendering/sky effects, data components) differ from 1.21-era tutorials. Check the NeoForge docs and the 26.3 MDK before guessing.
- `gradlew build` and the game tests catch most things, but they have client classes on the classpath, so they can't catch a dedicated-server crash: run `tests/server-smoke.sh` in the modpack repo against the built jar for anything touching client or GeckoLib code in common classes. The game test server also loads no datapack dimensions and isn't a client: compat crashes (Jade) and anything visual only show in `runClient` or `runPhoto`. Keep logic deterministic and data in datagen so build-time errors catch what they can.
