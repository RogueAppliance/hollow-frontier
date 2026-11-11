# Hollow Frontier

Wild West steampunk on a Hollow Earth frontier, for Minecraft 26.3 (NeoForge).

Ride out with a cap-and-ball revolver and a lasso, collect bounties, and drink in the saloon. Build boilers, crushers, handcars and an airship, then sail north to a Polar Gateway and fall through Symmes' Hole into a world lit by an inner sun. Down there you'll find megafauna, the ruins of Concavia and the Kindled who survived it, and the Grays: the ones flying Concavia's stolen saucers, and the ones behind the outlaws. In the end you build a saucer of your own and take the fight to their Mothership.

## Requirements

| | Version |
|---|---|
| Minecraft | 26.3 |
| NeoForge | 26.3.0.99 or newer |
| GeckoLib | 5.5.7 or newer (required) |
| Java | 25 (ships with the 26.3 launcher) |

Install it on both the client and the server.

## Optional mods

Hollow Frontier works on its own. If these are installed, it uses them:

- **JEI**: a Steam Crusher recipe category, and info pages for steam blocks and for things only found in the world (crash-site salvage, boss drops, the sheriff's brass star and so on).
- **Jade**: a boiler's fire, a crusher's progress and a pressure gauge's reading; an airship's steam and coal, a handcar's steam, and a saucer's phlogiston and fittings.
- **Curios**: wear the Sunstone Lens as a charm (night vision) and the Kindled Sigil as a necklace (shows the implanted). Without Curios, both work held in either hand.

Shaders are up to you; Complementary Reimagined and Bliss are good fits. The sky and beams draw with vanilla render types so shader packs can handle them. No shaders are bundled.

## Where to start

A new world starts you in the nearest town's square, and on a dedicated server with operators you'll want `spawn-protection=0`, or other players can't use the doors and chests around it. Talk to a town's postmaster first: the Polar Survey's last letter is waiting, and it keeps track of the road north to a Polar Gateway (what to build, and roughly where the crater is). Once you reach the Hollow Earth, craft a Concavian Codex (a book and a phlogiston shard): it keeps track of how far you've come and says what's next. The advancement tabs (Hollow Frontier, Steam Age) lay out the rest.

## December

By the server's date, and gone again in January: town lamps dressed with a sprig and a ribbon, a tree in each town's square with candles lit after dark, a Christmas Parcel from the postmaster (one a year, with a Fruitcake in it), Eggnog and Hot Cider at the saloon, carols on the piano, and a good deal of talk about what the lights in the sky really were. From December 24th to 26th no outlaw raid starts, and a sheriff will let one small fine go. No snow is added. It's all switchable in `hollowfrontier-season-server.toml`.

## Settings

- `hollowfrontier-server.toml` (per world): outlaw raids on towns, how often, how big, and what defenders earn.
- `hollowfrontier-ufo-server.toml` (per world): saucer flyovers, and whether their beams take players.
- `hollowfrontier-saloon-server.toml` and `hollowfrontier-dust-devils-server.toml` (per world): saloon brawls and missing time; dust devils.
- `hollowfrontier-mysteries-server.toml` (per world): airship sightings, cattle mutilations, crop circles, and the men in black's visits and hunts, each switchable.
- `hollowfrontier-towns-server.toml` (per world): whether towns replace the townsfolk and lawmen they lose, and how long each new arrival takes.
  It also holds `spawnAtTown`, on by default, which starts new worlds in the nearest town. A world chooses its spawn once, so set this in the `config` folder before creating the world.
- `hollowfrontier-season-server.toml` (per world): December. `mode` is `AUTO` (by the server's date), `ALWAYS` or `NEVER`; `truce` is the Christmas truce.
- `hollowfrontier-startup.toml`: whether brass can be alloyed at a crafting table (turn it off if another mod makes brass).

## In a modpack

- Materials use the `c:` tags (`c:ingots/brass`, `c:dusts/iron`, `c:ores/starsteel` and so on), and our recipes take any mod's.
- Steam is `c:steam`. Our tanks take any mod's steam, and the canister, airship and handcar draw it from other mods' tanks. The Boiler and Crusher slots work with any mod's pipes.
- Spawns and structures go by biome tags (`hollowfrontier:has_*`), which a datapack can change. Polar Gateways spawn in any `c:is_snowy` biome.
- Bosses are tagged `c:bosses`, and they can't be caught or teleported by other mods' tools. Neither can the townsfolk.
- Gamerule `hollowfrontier:saucer_beams_players`: whether a player's saucer can beam up other players.

Ops have `/hollowfrontier raid|mystery|kindled|ufo ...` for setting off raids, sightings and saucers by hand.

## Bugs and ideas

Report bugs in [Issues](https://github.com/RogueAppliance/hollow-frontier/issues) (the form asks for your versions and a log link from [mclo.gs](https://mclo.gs)). Questions and open-ended ideas go in [Discussions](https://github.com/RogueAppliance/hollow-frontier/discussions).

## Building

```
.\gradlew.bat build
```

The jar lands in `build/libs/`. Assets come from datagen: after changing datagen, run `.\gradlew.bat runData genPlaceholders` first. `.\gradlew.bat runGameTestServer` runs the game tests headless.

## Credits and license

Made by RogueAppliance.

**License:** [MIT](LICENSE). Use it, put it in your modpack, fork it, as long as the license notice comes along. Two things aren't covered:
- The textures reworked from vanilla Minecraft's (marked in [ASSETS.md](ASSETS.md)) belong to Mojang.
- The build files from the NeoForge MDK keep their own MIT notice ([TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt)).

**Art and sound:** everything else is the mod's own work. The art is drawn by code in `tools/textures` and `tools/models`. Every bundled sound (the piano, the Hollow Earth's reveal, the ray guns and the saucers) is synthesized in `tools/music` with no samples. The other sound effects are Minecraft's own sounds, re-pitched in `sounds.json`, not copied. [ASSETS.md](ASSETS.md) lists every file and where it came from.

**Music:** the saloon piano plays Scott Joplin's ragtime ("The Entertainer", "Elite Syncopations", "Pineapple Rag", "The Strenuous Life"), public domain, rendered by our own piano synthesizer from the [Mutopia Project](https://www.mutopiaproject.org)'s public-domain scores.

**Built on** [NeoForge](https://neoforged.net) and [GeckoLib](https://github.com/bernie-g/geckolib), with optional support for JEI, Jade, Curios and Iris.

**The trailer** uses "Still Pickin'" by Kevin MacLeod ([incompetech.com](https://incompetech.com)), licensed under [Creative Commons: By Attribution 4.0](http://creativecommons.org/licenses/by/4.0/), and was filmed with the Complementary Reimagined shader pack by EminGT. Neither is part of the mod.

The changes in each version are in [CHANGELOG.md](CHANGELOG.md).
