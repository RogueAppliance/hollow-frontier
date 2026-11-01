# Hollow Frontier (NeoForge mod)

- Versions: MC 26.3, NeoForge 26.3.0.x, ModDevGradle 2.0.x, GeckoLib 5.5.7, Java 25 (Gradle toolchain). Full table + standing decisions in PLAN.md; the full decisions log is DECISIONS.md.
- Mod id `hollowfrontier`, package `com.hollowfrontier`: `registry/` (Mod* registries + ModTags), `worldgen/` (feature/placement/biome-modifier bootstraps; `structure/` blueprints → NBT), `content/{frontier,steam,hollow,concavia,ufo}/` (steam: `SteamNetwork`/`FluidTank` for all steam holders), `entity/`, `network/` (payloads), `client/` (renderers, sky, screens, keys), `datagen/`, `compat/` (optional JEI/Jade, soft-loaded only).
- Build/verify: `.\gradlew.bat build`. After datagen changes, first run `.\gradlew.bat runData genPlaceholders`. That's the only verification step.
- Datagen entry point is `datagen/ModDataGen`: recipes/loot/advancements/worldgen go through `GatherDataRegistryEntriesEvent`; models/lang/tags through `GatherDataEvent.Client`.
- Launch the client or server (`runClient`/`runServer`/`runPhoto`) yourself whenever it helps testing; no need to ask.
- Game tests: `.\gradlew.bat runGameTestServer` (headless). Use it when asked to test; add tests in `gametest/` for new features.
- Work one PLAN.md phase at a time. Mark it done in PLAN.md when the build passes.
- All recipes/loot/models/blockstates/tags/lang/worldgen/structure NBT come from datagen. Never hand-edit `src/generated`.
- New textures: placeholders via `genPlaceholders` (model refs + `placeholders.txt` with optional WxH), and list every texture/geo model in ASSETS.md. GeckoLib geo/animation JSON is hand-written under `assets/hollowfrontier/geckolib/`.
- Only NeoForge + GeckoLib are required. Other mods (JEI, Jade, Curios; Iris/Sodium for dev) are optional compat only. Never bundle third-party shaders or textures unless the license allows it (record it in ASSETS.md). Exception: reworked vanilla textures (recoloured and tweaked, not plain copies) may be bundled; mark each one in ASSETS.md with its vanilla source.
- Make reasonable calls without asking, and note them in DECISIONS.md (under their phase).
- Summaries: under 10 lines, no code.
