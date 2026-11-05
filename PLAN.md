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

## Open items

- **Publishing:** 1.0.0 was uploaded on 2026-11-01 (CurseForge project 1778329, Modrinth Mvsn4kHn) and 1.1.0 to CurseForge on 2026-11-05. Modrinth was still reviewing the project, so 1.1.0 goes up there once it's approved. Log later changes under a new heading above it in CHANGELOG.md. How to release: RELEASING.md.
- **Trailer leftovers:** the plain (no-shader) trailer, the vertical short and the page GIFs predate the current trailer world and the ember phlogiston. Re-film or re-cut them before use. A true 4K trailer would need re-filming at 4K (AMD VSR on the 1440p monitor); the current 4K file is an upscale.
- **The Kindled's look:** kept for now (the user's call); mock-ups of alternatives exist (see DECISIONS.md, art passes).
- **Known gaps:** Quick Load speeds loading but not the reload animation; loaded and empty guns look the same in the creative grid; duster tails are on hold; the fern frond still reads as scattered pixels.

## Risks

- 26.x APIs (datagen events, transfer API, rendering/sky effects, data components) differ from 1.21-era tutorials. Check the NeoForge docs and the 26.3 MDK before guessing.
- `gradlew build` and the game tests catch most things, but the game test server loads no datapack dimensions and isn't a client: compat crashes (Jade) and anything visual only show in `runClient` or `runPhoto`. Keep logic deterministic and data in datagen so build-time errors catch what they can.
