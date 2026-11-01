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

Craft a Concavian Codex (a book and a phlogiston shard) once you reach the Hollow Earth. It keeps track of how far you've come and says what's next. Before that, the advancement tabs (Hollow Frontier, Steam Age) lay out the frontier and the road north.

## Settings

- `hollowfrontier-server.toml` (per world): outlaw raids on towns, how often, how big, and what defenders earn.
- `hollowfrontier-ufo-server.toml` (per world): saucer flyovers, and whether their beams take players.
- `hollowfrontier-saloon-server.toml` and `hollowfrontier-dust-devils-server.toml` (per world): saloon brawls and missing time; dust devils.
- `hollowfrontier-mysteries-server.toml` (per world): airship sightings, cattle mutilations, crop circles, and the men in black's visits and hunts, each switchable.
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

[MIT](LICENSE): use it, put it in your modpack, fork it, as long as the license notice comes along. The exceptions are the textures reworked from vanilla (Mojang's) and the bundled third-party recordings (public domain or CC0), which [LICENSE](LICENSE) spells out. Art and sound sources are listed in [ASSETS.md](ASSETS.md). The changes in each version are in [CHANGELOG.md](CHANGELOG.md).
