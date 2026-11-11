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
35. ✅ 2026-11-10 — **A frontier December** (in 1.3.0): content that comes on by the server's date and leaves by itself. Spec in PHASE_SPECS.md; calls in DECISIONS.md.
    - ✅ 2026-11-10 — 35a The season and the lamps: `Season` and `hollowfrontier-season-server.toml` (`mode`, `truce`); Town Lamps dress and undress on the random tick that lights them.
    - ✅ 2026-11-10 — 35b The town tree (a GeckoLib entity stood up by `TownPresence`, gone in January) and the Christmas Parcel (one a year from the postmaster, or the shopkeeper where there's no post office) with its Fruitcake.
    - ✅ 2026-11-10 — 35c The saloon: Eggnog and Hot Cider on the barmaids' lists in December only; three carols, every other tune, set by a `carols` state the server gives the piano.
    - ✅ 2026-11-10 — 35d Words and the truce: `Fact.DECEMBER` and `Fact.CHRISTMAS`, 31 lines, five exchanges and eight Gazette pieces; no raid starts from the 24th to the 26th, and a sheriff waives one one-star fine a year.
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
44. ✅ 2026-11-10 — Known gaps closed (with 1.3.0): ammo pips on a black powder gun's slot, the reload animation at Quick Load's pace, tails on the dusters (outlaws, men in black, the Marshal), the Fern Frond redrawn.
45. ✅ 2026-11-10 — **The Hollow, heard** (for 1.4.0, not yet released). The Hollow Earth gets a score and a soundscape of its own. Today it plays five borrowed vanilla biome tracks (jungle, meadow, badlands, grove, lush caves) over the cave mood, with re-pitched vanilla one-shots; its only original music is the reveal swell. No new items, mobs or blocks: this phase is sound only. Three parts, one session each, in order. Log all of it under one CHANGELOG heading.

    **Ground rules**
    - **Ours or vanilla, nothing third-party.** Music and beds are synthesized in `tools/music` (additive, no samples, as `Swell` is), so every recording stays ours under the MIT licence. Creature and frontier one-shots stay re-pitched vanilla events in `sounds.json`.
    - **One musical idea.** The score grows from the reveal swell (open E major with the ninth, soft strings and wordless voices over a drone), so the swell reads as the overture. Slow, sparse, no percussion: it has to sit under hours of play.
    - **The right sliders.** Score and boss themes on Music, beds and far-off calls on Ambient, stings on Ambient too. A piano or record in earshot still holds the music (`TownSoundscape.onSelectMusic` stays in charge of that).
    - **Client-only and server-safe.** Everything that picks or plays a sound lives in `client/`; common classes only name sound events. Run the pack's server smoke script before release.
    - **Budget:** at most 10 MB added to the jar. Music is stereo Vorbis at about 60 kbps (it isn't played from a position); beds are short seamless loops (20 to 40 s). 45a used 5.7 MB.
    - **Checked by ear.** Spectrograms and bar counts catch a broken file, not a bad one. Each part ends with the user listening in `runClient`; a dev-only client command, `/hfaudition`, plays the phase's sounds one after another, each named in chat.
    - **A switch.** A client option `hollowScore` (on by default) turns our score off and puts vanilla's biome music back, for players and packs with their own music.

    - ✅ 2026-11-10 — 45a **The score.** Built and heard: the user listened and said it sounds good. `tools/music/HollowScore.java` renders five pieces of 2 to 3 minutes, one per biome, each a variation on the swell's chord: the Primordial Jungle warm and close, the Crystal Steppes glassy and bright, the Ember Badlands low and dry, the Concavian Highlands thin and high, the Still Sea slow and deep. Sound events `music.hollow.<biome>`, set as each biome's `BACKGROUND_MUSIC` in `HollowBiomes` with vanilla's usual gaps between plays. The `hollowScore` switch, the audition command, ASSETS.md entries, and a test that every Hollow biome's music is ours and every `ModSounds` event has a `sounds.json` entry whose files exist. *Done when:* the build passes, the five pieces play in their biomes in `runClient`, and the user has listened to them.
    - ✅ 2026-11-10 — 45b **The beds and the distance.** Built and heard: the user had the beds turned down 30%, then everything in the phase down another 20%. A looping bed per biome in the `AmbientSounds` loop slot that `HollowBiomes.ambient` leaves empty (insects and drip, a crystal hum, a low rumble and hiss, thin wind, still water against stone), rendered by `tools/music/HollowBeds.java` from filtered noise. A new `client/HollowSoundscape`, modelled on `TownSoundscape`: far-off megafauna calls from 40 to 80 blocks out by biome (a mammoth's trumpet across the steppes, a Basilosaurus's song over deep water), a rare rumble from the crust overhead, and quiet near a Gray site. Under water in the Still Sea, a deep bed that darkens with depth; aboard the Steam Submersible, a steam thrum and hull creaks that come more often the deeper it sits. *Done when:* the build passes, each biome's bed loops without a seam in `runClient`, the far calls are heard from the right direction, and the user has listened.
    - ✅ 2026-11-10 — 45c **The moments.** Built, and the listening pass signed off by the user. Boss themes for the Sun Archon and the Mothership (both visits), picked through `SelectMusicEvent` while that boss's bar shows and faded out on its end; the Outlaw Boss and raids keep what they have. Five short stings, each played once per player per world and tied to an advancement that already exists: the first Kindled met, the Sun Temple's throne hall, the first Gray complex, the first dome of the Drowned Quarter, and the leash broken. Then the full listening pass: every sound from 45a to 45c and the three carols from 35c, with the "Not checked by ear" open item closed or its failures fixed. *Done when:* the build and the server smoke test pass, both themes start and stop with their fights, each sting plays once and only once, and the user has signed off the listening pass.
46. **The frontier, felt** (specced and built 2026-11-10, for 1.4.0 with phase 45; the user asked for all three parts as one job). *Built: the build, game tests and server smoke test pass, and a `runPhoto` scene (`-PphotoScene=feel`) shot the impacts, the haze, a gallop on sand and a mammoth, and measured a real shot's kick and dynamite's shake in the client. Waiting on the user's look in `runClient`; what nobody has seen or heard yet is in Open items.* Weight on the things a player does most: firing, riding, flying, and standing near something very large. Phase 45 was what the world sounds like; this is what it feels like to act in it. No new items, mobs, blocks or recipes, and nothing a server's rules depend on changes. Three parts, one session each, in order (46a makes the camera code the other two use).

    **What's there now** (so nothing is built twice): the guns already have a recoil animation on the model, a muzzle flash and powder cloud, a tracer, and three puffs of smoke where a ball hits a block, the same on every surface. Nothing moves the camera. Horses raise no dust. The airship has its engine beat, vent and stoke sounds and nothing for wind or height. Megafauna make no mark on the ground or the player when they walk.

    **Ground rules**
    - **Felt, not changed.** Every effect is for the eyes and ears. The camera kick doesn't move where a gun points: the view goes back to exactly where it was, and the shot was already resolved on the server. Damage, spread, reload and cooldown are untouched, and a test holds the gun numbers where they are.
    - **Two sliders, both off-able.** `cameraKick` and `cameraShake` in `hollowfrontier-client.toml`, 0.0 to 1.0, default 1.0. At 0 that effect is gone entirely. Some players are made ill by a moving camera, so the defaults are modest and the page says where the switch is. Vanilla's own Distortion Effects and Damage Tilt settings are left alone.
    - **One camera class.** `client/CameraFeel` holds every kick and shake as a decaying offset added in `ViewportEvent.ComputeCameraAngles`; nothing else touches the view. A cap on the total, so two things at once can't throw the view about.
    - **The world sees it too.** Whatever one player sees of a shot or a footfall, players nearby see: particles and sounds go from the server as they do now, or are worked out on each client from what it can already see (an entity's speed, its animation). New packets only where neither will do.
    - **Vanilla particles and sounds.** Block-chip, dust, cloud and splash particles, and re-pitched vanilla sound events in `sounds.json` (wind and creaks are frontier sounds, not sci-fi, so they're remixes, not synthesized). So nothing new to draw, shader packs are unaffected, and the jar doesn't grow.
    - **Kept cheap.** Particle counts are capped per event and scale with the game's own Particles setting; nothing scans more than the entities already in view.
    - **The numbers live in common classes** (as `HollowAmbience` does for 45b) so game tests can hold them; the client classes only play things. Each part ends with `runPhoto` shots of what can be photographed and a `runClient` look by the user at what can't (a kick is motion).

    - 46a (built) **The guns.** *The kick:* firing nudges the view up and a little to one side, then eases back over about a quarter of a second; the Blunderbuss most, the rifle next, the pistols least, and the two phlogiston guns hardly at all (a ray has no powder behind it). *What a ball hits:* chips of the block that was struck and that block's own hit sound, in place of the same three puffs everywhere; a spark and now and then a ricochet's whine off stone and metal; a splash on water; a scorch that glows for a moment where a ray lands. A hit on a creature gets a dull thud for the shooter, apart from the headshot's ding. *Near misses:* a ball passing within two blocks of a player who isn't the shooter makes a short crack-and-whiz for them alone, so being shot at by outlaws sounds like it. *The haze:* three or more black powder shots from one place inside a few seconds leave a thin white haze that hangs for several seconds, so a gunfight in a saloon looks like one. Common numbers in `content/frontier/gun/GunFeel`. *Done when:* the build and game tests pass (kick ordered by gun, impact kind by block tag, every gun's damage unchanged), the server smoke test passes, impacts on four surfaces and the haze are shot in `runPhoto`, and the user has fired each gun in `runClient` with the kick at 1.0 and at 0.
    - 46b (built) **Riding and flying.** *Horses* (every horse, donkey and mule, vanilla's included): at a gallop, dust kicked up behind in the colour of the ground, more on sand and dry dirt, none on grass in rain or on stone; shod horses ring on stone (a horseshoe finally heard). *The lasso:* a rising whirl while it's wound up, and a creak on the line while something pulls against it. *The airship:* wind that rises with speed and with height, a creak from the envelope when it turns or climbs hard, wisps of cloud past the deck at cloud height, and a slow sway of a degree or so at the helm that grows in a turn. *The handcar:* the clack of rail joints, quicker with speed, and sparks from the wheels when it brakes. *Done when:* the build, game tests (dust by ground block, wind by speed and height) and smoke test pass, a gallop on sand and the airship in cloud are shot in `runPhoto`, and the user has ridden and flown in `runClient`.
    - 46c (built) **The big things.** *Footfalls:* a Woolly Mammoth, Megatherium or Glyptodon walking within about twelve blocks is felt: a low thud, dust at its feet, and a shake that falls away with distance, a charge more than a walk. A Basilosaurus's ram shakes the submersible's crew. *Blasts:* dynamite, a vault blown in a raid and the Mothership's death ray shake the view by distance, on top of the sounds they have. *Arrivals:* the Mothership coming in overhead is felt before it's seen, a slow tremor while it settles. All through `CameraFeel` and the `cameraShake` slider. Then a pass over the whole phase at both sliders' extremes. *Done when:* the build, game tests (shake falls with distance and is zero past its range, the cap holds) and smoke test pass, and the user has stood near a mammoth, set off dynamite and watched a Mothership arrive in `runClient`, once at 1.0 and once at 0.

## Open items

- **Publishing:** 1.0.0 was uploaded on 2026-11-01 (CurseForge project 1778329, Modrinth Mvsn4kHn) 1.1.0 and 1.1.2 to CurseForge on 2026-11-05 (1.1.1 went out inside 1.1.2), and 1.1.3 to CurseForge on 2026-11-06, 1.1.4 to CurseForge on 2026-11-07, and 1.3.0 on 2026-11-10. Log later changes under a new heading above it in CHANGELOG.md. How to release: RELEASING.md.
- **Trailer leftovers:** the plain (no-shader) trailer, the vertical short and the page GIFs predate the current trailer world and the ember phlogiston. Re-film or re-cut them before use. A true 4K trailer would need re-filming at 4K (AMD VSR on the 1440p monitor); the current 4K file is an upscale.
- **The Kindled's look:** kept for now (the user's call); mock-ups of alternatives exist (see DECISIONS.md, art passes).
- **Known gaps:** none listed. Phase 44 closed the four that were here; the frock coats (doctor, preacher, banker) stay without tails by choice.
- **Phase 46, not seen in play:** checked in the client by `runPhoto` only (`-PphotoScene=feel` and `shod`): horseshoes drawn on all three tiers, the lasso's wind-up loop and its loop in the air (both added 2026-11-11 after the user's first look), the kick on a real shot, impacts on stone, boards, sand, iron and water, the haze, hoof dust on sand, a mammoth's dust and shake, dynamite's shake. Not seen or heard by anyone: every new sound (ricochet, near miss, thud, shod hooves, lasso, line creak, airship wind and creak, handcar clack, footfall, dynamite's thump), the airship's lean and cloud wisps, the handcar's sparks, a rammed submersible, the Mothership's tremor, and both sliders at 0. The spec's airship-in-cloud photo wasn't taken. The user's `runClient` look is what marks 46 done.
- **Phase 45, not seen in play:** the user signed off the listening pass on 2026-11-10 (the carols from 35c with it). Nobody has yet watched a boss theme start and stop with a real Sun Archon or Mothership fight, or a sting play once on its advancement; `client/BossMusic` has no automated test. Check both before 1.4.0 is uploaded.

## Risks

- 26.x APIs (datagen events, transfer API, rendering/sky effects, data components) differ from 1.21-era tutorials. Check the NeoForge docs and the 26.3 MDK before guessing.
- `gradlew build` and the game tests catch most things, but they have client classes on the classpath, so they can't catch a dedicated-server crash: run `tests/server-smoke.sh` in the modpack repo against the built jar for anything touching client or GeckoLib code in common classes. The game test server also loads no datapack dimensions and isn't a client: compat crashes (Jade) and anything visual only show in `runClient` or `runPhoto`. Keep logic deterministic and data in datagen so build-time errors catch what they can.
