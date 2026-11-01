# Releasing

How a version goes up on Modrinth and CurseForge. The store page text is [CURSEFORGE.md](CURSEFORGE.md) (both stores take the same Markdown); the changes are [CHANGELOG.md](CHANGELOG.md).

## Every release

1. Bump `mod_version` in `gradle.properties` and add a `## x.y.z` section at the top of CHANGELOG.md.
2. `.\gradlew.bat prepareRelease`. It runs the full build, then fills `build/release/` with:
   - `hollowfrontier-neoforge-26.3-x.y.z.jar`, the file to upload;
   - `description.md`, the store page without its notes-to-self (the `<!-- IMAGE -->` markers stay, to show where each GIF goes);
   - `changelog.md`, the newest changelog section, for the version's changelog box.
3. Upload the jar to both stores (fields below), then add the date to the changelog heading, commit, tag `vx.y.z`, and make a GitHub release with the same jar.

## Before the first upload

- **Make the GitHub repo public.** The jar's metadata (issue tracker, mod page) and both store pages link to it.
- **Put the trailer on YouTube** (`trailer/out/hollow_frontier_trailer_shaders.mp4`). Its description must credit "Still Pickin'" by Kevin MacLeod (incompetech.com, CC BY 4.0) and Complementary Reimagined by EminGT, as the README does.
- Both stores review a new project before it's public (from hours to a few days), so it's worth submitting both on the same day.

## Modrinth (modrinth.com → Create a project)

| Field | Value |
|---|---|
| Name / URL | Hollow Frontier / `hollow-frontier` |
| Summary | Wild West steampunk on a Hollow Earth frontier: black powder guns, living towns, airships, mammoths, Concavia and the Grays. |
| Icon | `branding/avatar_400x400.png` |
| Description | `build/release/description.md` |
| Categories | Featured: Adventure, Worldgen, Technology. Also: Mobs, Equipment, Transportation, Economy. |
| Environment | Client and server, required on both |
| License | MIT |
| Links | Issues: `https://github.com/RogueAppliance/hollow-frontier/issues`. Source: `https://github.com/RogueAppliance/hollow-frontier`. Wiki and Discord: leave empty. |
| Gallery | The 12 GIFs in `branding/gifs` (each under 3.5 MB), titled as in the page's IMAGE markers; the banner `branding/curseforge_banner_1920x480.png` as the featured image. Copy each gallery image's link into the description at its marker. |

**Version:** number `1.0.0+26.3`, name `Hollow Frontier 1.0.0`, channel Release, loader NeoForge, game version 26.3, changelog from `build/release/changelog.md`. Dependencies: GeckoLib (required); JEI, Jade, Curios (optional).

## CurseForge (authors.curseforge.com → Create project → Minecraft → Mods)

| Field | Value |
|---|---|
| Name | Hollow Frontier |
| Summary | as Modrinth's |
| Logo | `branding/avatar_400x400.png` |
| Description | switch the editor to Markdown, then paste `build/release/description.md` |
| Main category | Adventure and RPG |
| Other categories | World Gen (Dimensions, Biomes, Structures, Mobs), Technology, Armor, Tools, and Weapons |
| License | MIT |
| Source / Issues | as Modrinth's |
| Images | the same GIFs and banner; the trailer under Videos once it's on YouTube |

**File:** display name `Hollow Frontier 1.0.0`, release type Release, game version 26.3, loader NeoForge, Java 25, environment Client + Server, changelog from `build/release/changelog.md` (Markdown). Related projects: GeckoLib (required dependency); JEI, Jade, Curios (optional dependency).
