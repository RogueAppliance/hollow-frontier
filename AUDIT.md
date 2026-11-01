# Hollow Frontier audit (2026-10-29, v1.0.0)

## Health check

- `gradlew build`: passes. A clean compile showed 7 `[removal]` deprecation warnings (fixed in 31b): `Items.GLASS_BLOCKS*` / `GLASS_PANES_COLORLESS` in `ModRecipeProvider`, and `IHolderExtension.getKey()` in `TabTests`. These will break on a future NeoForge update. (The first audit build was cached, so it missed them.)
- `runGameTestServer`: **all 236 tests pass** in about 35 s, with no mod warnings or errors in the log.
- Crash reports in `run/crash-reports`: the two from 2026-10-29 are already fixed. The `CattleCarcass` client-move crash (v0.7.0) is now guarded to the server side. The 21:27 crash was the loader's error screen and does not involve mod code.
- Dedicated-server safety: no client classes are loaded from common code. The GeckoLib `createGeoRenderer` hooks are the standard lazy pattern, and the headless test server (a dedicated server) loads cleanly.
- Network: the two client→server payloads check their input. Reload acts only on the gun in hand. Saucer control is accepted only from the pilot, and the reported sight is range-checked, which also rejects NaN and infinity.
- Configs: ranges are bounded (`rollInterval` ≥ 20, so `% 0` can't happen), and `maxGang` is clamped to at least `minGang`.

Overall the code is careful and consistent. The findings below are mostly edge cases and housekeeping, not structural problems.

## Findings, most important first

Findings 1–3 were fixed on 2026-10-29, right after the audit. See CHANGELOG "Unreleased" and DECISIONS.md.

### 1. No version control (high) — fixed: git repo on `main`, 1.0.0 as the first commit
The project is not a git repository. That means 44k lines of Java, 236 tests and a released 1.0.0 jar with no history and no backup. A bad edit or a disk failure can't be undone. **Fix:** `git init` and a first commit. The `.gitignore` already excludes `run/` (2.5 GB) and `build/`. Pushing to a private GitHub repo would also allow CI (build plus game tests on every push).

### 2. Gateway state leaks between worlds and is lost on restart (medium) — fixed
`GatewayEffects.WAKING` and `DESCENTS` are static lists that are never cleared, and nothing in the mod listens for `ServerStoppedEvent`.
- In singleplayer, leaving a world mid-descent and opening another one carries the entry over. The player's UUID is the same in every world, so `isDescending` cancels their fall damage there for up to the fall's length. Stale wakes also keep ticking against the new world's level keys.
- A wake is dropped without opening the ring if its chunk unloads, and the resonator's steam has already been paid by then. (A server stop mid-wake leaving the ring half lit is documented, intended behaviour.)
**Fixed:** both lists are cleared on `ServerStoppedEvent`, and a wake now waits while its chunk is unloaded.

### 3. Large block scans in mob AI (medium, performance) — fixed (cat: block entities of loaded chunks only; raven: 200-tick back-off)
- `CatHearthGoal.findFire` scans 81×9×81 blocks (about 59k reads) for each town cat that has no fire. It runs at most every 2400 ticks per cat, but a 40-block radius reaches past the entity-ticking border. On a `ServerLevel`, `getBlockState` there can force chunks to load synchronously.
- `PaleRaven.PerchGoal` scans 33×13×33 blocks (about 14k reads) on a 1-in-40 roll, and does it again every time when no perch is nearby. Its roll uses `nextInt(40)` rather than `reducedTickDelay`.
- `AshSalamander` (about 3k reads) and `Megatherium` are fine.
**Fix:** skip positions outside loaded chunks (`level.hasChunkAt`). Shrink the cat search or cache the result per town. Give the raven a cooldown after a search that finds nothing.

### 4. Multiplayer: anyone can take anyone's saucer or airship (medium, design) — fixed: saucer in phase 15b, airship in 31a (owner lock)
`PrototypeSaucer.fold` only checks that the craft is landed and has no crew, so any player can sneak-use it and pocket someone else's endgame craft, fittings and charge included. The airship can be broken by anyone, the same way as a vanilla boat. **Suggestion:** record an owner when the craft is placed, and let only the owner (or their team) fold or break it.

### 5. An airship loses its fuel when broken (low) — fixed in 31a (coal and steam kept on the item)
`Airship.getDropItem` returns a plain airship item, so up to 8000 ticks of fuel (5 coal) and its stored steam are lost. **Fix:** drop the leftover coal, or store fuel and steam on the item the way the saucer stores its charge.

### 6. Shipping and docs housekeeping (low) — fixed: PhotoBooth moved to a dev source set and the humanoid model upgraded (31b, 31c); PLAN.md was slimmed earlier
- `client/PhotoBooth.java` (2311 lines, a dev-only screenshot rig) is in the release jar. It is gated behind `-Dhollowfrontier.photo`, so it's harmless, but it could move to its own source set that `runPhoto` adds.
- `PLAN.md` is 177 KB, mostly the Decisions log, and every session loads it. Moving finished phases' decisions to `DECISIONS.md` and keeping PLAN.md to the phase list would cut that load.
- ASSETS.md still marks `geckolib/models/entity/humanoid.geo.json` as "placeholder boxes", although the photo booth shots look finished. Either upgrade the model (hat brims, a duster skirt, bandanas as their own bones) or update the label.
- `client/TownSoundscape.PIANOS` keeps positions from the previous world after a world change. This is harmless, because the sounds have already stopped.

## Ideas for after 1.0

- **Saucer fittings already listed in DECISIONS.md (phase 13c):** a cloak (invisible at night), crop-circle stamping (a prank the Gazette reports), a cargo hold.
- **CI:** once the project is on GitHub, a workflow that runs `build` and `runGameTestServer`.
- **Publishing:** the jar is built but not uploaded. Modrinth and CurseForge pages would need screenshots from `runPhoto`.
- **Server operators:** a `/hollowfrontier` subcommand that lists active raids, wakes and saucers, which helps when debugging a live server.
