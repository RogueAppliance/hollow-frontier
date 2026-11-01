# Hollow Frontier — Decisions

Calls made in place of asking, and the reasons behind them. PLAN.md keeps the standing ones; this is the full log, condensed on 2026-11-01 to what still holds (superseded calls folded into what shipped; the long form is in git history, and the original phase specs are in PHASE_SPECS.md). Add new decisions under their phase, or under "Since phase 31" for work that isn't one. Record the call and why, not what the code already shows.

## Engine and tool notes

- **26.x datagen split:** recipes, loot tables, advancements and worldgen are datapack registries gathered through `GatherDataRegistryEntriesEvent` (`event.recipe(...)`, `.lootTable(...)`, `.add(Registries.X, ...)`). Models, lang and tags go through `GatherDataEvent.Client`. `runData` uses `clientData()`, so it covers both. The registry-entries event takes one bootstrap per registry, so merge contributors (e.g. ore + spawn biome modifiers) in one lambda, and pass all loot sub-providers in one `lootTable(...)` call.
- **26.3 API notes:**
  - `Holder.getKey()` is deprecated for removal: use `key()`. Configured features are `Registries.FEATURE` holding `Feature` instances (`new OreFeature(List<BlockReplacement>, size)`). `RecipeBuilder.save(output, id)` throws when `id` equals the default (`ModRecipeProvider.save()` handles it); vanilla's `nineBlockStorageRecipes` writes `minecraft:` ids, so use our `storage()` helper.
  - Input is SDL, not GLFW (no `org.lwjgl.glfw`). Key mappings use `InputConstants.Type.KEYBOARD` + `InputConstants.KEY_*`; categories are `new KeyMapping.Category(id)` registered through `RegisterKeyMappingsEvent.registerCategory`, lang key `key.category.<ns>.<path>`.
  - `Entity.hurtMarked` is gone: `push()` sets `needsSync`, and a pushed player needs a `ClientboundSetEntityMotionPacket`. An entity's `move` throws if run on the client. Armor uses `Item.Properties.humanoidArmor(ArmorMaterial, ArmorType)` plus an `EquipmentAssetProvider` (layers humanoid, humanoid_leggings, humanoid_baby). Entity types without loot (projectiles) need `EntityType.Builder.noLootTable()`.
  - `Player.displayClientMessage` → `sendSystemMessage` / `sendOverlayMessage`; `Player.drop(stack, thrown, Prediction)`.
  - In BuiltInRegistries, `ENTITY_TYPE` registers before `ITEM` (spawn eggs can call `.get()`) and `DATA_COMPONENT_TYPE` after (don't set mod components in item constructors). During datagen items have no components bound, so no `ItemStack` can be made there: written books and sign text are encoded by hand through the vanilla codecs (`WrittenBookContent.CODEC`, `SignText.CODEC`).
  - `Blocks.RED_WOOL` etc. are `Blocks.WOOL.red()` (`ColorCollection`); `Blocks.COPPER_BLOCK` is a `WeatheringCopperCollection` (`.weathering().unaffected()`). Block `codec()`/`simpleCodec` are gone. Loot entry builders are `UniformContainerBase.Builder`. Custom block models in datagen use NeoForge's `ExtendedModelTemplateBuilder`, registered with `BlockModelGenerators.createSimpleBlock(block, plainVariant(model))`. Structure NBT is written with `NbtIo.writeCompressed` plus `NbtUtils.addCurrentDataVersion`.
  - Transfer API: `ResourceHandler<FluidResource>` with `Transaction.openRoot()`; `FluidStacksResourceHandler` is the base tank; `Capabilities.Fluid.BLOCK` registered via `RegisterCapabilitiesEvent`. A fluid's sprites come from a client fluid model (`RegisterFluidModelsEvent`), or Jade and JEI draw it as the missing texture.
  - Screens override `extractBackground(GuiGraphicsExtractor, ...)`. `getCraftingRemainder()` returns an `ItemStackTemplate`. Recipes extend `SingleItemRecipe` and need a registered `RecipeBookCategory`.
  - Deprecated tags: use `Tags.BlockItems.CHESTS_WOODEN.item()` and `Tags.BlockItems.GLASS_PANES.item()`. `#minecraft:dirt` no longer holds grass, mud or moss: "ground plants grow on" is `#minecraft:substrate_overworld`. `TemptGoal` needs the `tempt_range` attribute.
- **GeckoLib 5:** assets go in `assets/<ns>/geckolib/models/entity/<name>.geo.json` and `geckolib/animations/entity/<name>.animation.json`, textures in `textures/entity/<name>.png`. Use `DefaultedEntityGeoModel(id).withAltTexture(id)`. `ItemInHandGeoLayer` needs the renderer's `R` bound to `LivingEntityRenderState & GeoRenderState`. Drive animations from synced entity data or item components, never trigger packets from server logic, so mobs and other players see them. `hardcodedHeadRotation` replaces a bone's rotation, so look rotation goes on a `neck` bone and is added to the animation. Rotation sign: a positive X tips a bone's +Y end forward (-Z). API source: `geckolib-neoforge-26.3-5.5.7-sources.jar` on the GeckoLib Cloudsmith maven.
- **Glowing GeckoLib mobs:** GeckoLib 5.5.7 never draws the entity outline, so `GlowingGeoRenderer` (the base of all our GeckoLib entity renderers) submits the posed model again with vanilla's `outline` render type when the entity glows. Always white (team colours would need GeckoLib to pass a vertex colour). Drop it if a GeckoLib update renders outlines itself.
- **Testing:** game tests live in `com.hollowfrontier.gametest` (main source set; inert unless NeoForge enables game tests), on `test_platform`, `test_garden` (16) and `test_yard` (48) structures from the blueprint datagen. Fake players don't fire tick events, so tick-driven logic exposes a public per-player method (e.g. `CowboySetBonus.update`). A test player is only hurt once the server counts its client loaded (wait ~80 ticks). The game test server loads no datapack dimensions, so Hollow Earth tests build the generator from the registries, and crossing tests stand the Nether in for it. `HurtByTargetGoal` ignores a hurt stamped tick 0. Tests that make fake players remove them when they finish (`TestPlayers.leave`), and mobs a test relies on are pinned so they can't despawn. Known timing-sensitive tests: `salamander_basks_on_vent`, `raven_perches_on_stone`, `posse_rides_out` (each fails now and then and passes on a rerun).
- **Art pipeline:** flat placeholders (`genPlaceholders`) → procedural pixel art (`tools/textures/TextureArt.java`). Reworked vanilla textures are made by TextureArt straight from the patched game jar in `build/moddev/artifacts` (without it, the hand-drawn fallbacks stay). Models with many cubes are built by code (`tools/models/*Models.java`: guns, airship, barmaid, saucer...), since hand-written per-face UVs are error-prone; animation JSON stays hand-written. `runPhoto` (PhotoBooth) is the visual check; it releases every forceload when it starts and quits (left over, they made every start load thousands of chunks).
- **Datagen** covers recipes, loot tables, block/item models, blockstates, tags, lang (en_us), advancements, worldgen and structure NBT. Nothing generated is hand-edited. Run `runData` before `genPlaceholders`, as separate commands: `genPlaceholders` reads the models, and run first it makes placeholders for whatever the old models named.

## By phase

### Phase 2: materials
- **Brass** = 3 copper ingots + 1 gold nugget → 4. Zinc was cut in 9c: the early game uses vanilla metals, copper is plentiful and barely used by vanilla, and brass stays as the steam tech's look.
- Plates are 4 ingots → 2 (no hammer tool); gears 4 ingots + 1 iron nugget → 2.
- **Other mods' brass:** every brass input takes the `c:` tags. The crafting-table alloy has a `hollowfrontier:brass_alloying` load condition: with `alloying = AUTO` (startup config, the default) it switches off when another namespace's item is in `c:ingots/brass` (e.g. Create's), so we don't undercut that mod. ALWAYS/NEVER override. Startup config because this NeoForge has no COMMON type and the condition runs before any world config loads.

### Phase 3: guns (black powder)
- **Percussion muzzleloaders, period-accurate:** Cap & Ball Revolver (1851 Navy), Plains Rifle (Hawken), Double-Barrel Muzzleloader. Hitscan with a tracer; ammo and reload state in item components.
- **Loading:** one chamber per `loadTicks`, timed by a `reload_start` game-time component, ticking only while held. Every load costs a paper cartridge and a percussion cap; firing or the reload key cancels.
- **Wet powder:** rain gives a 20% misfire chance, underwater always misfires; a misfire spends the round. Ammo shows in the tooltip, so the item bar keeps showing durability. Mob gun damage scales with difficulty; mob guns are weaker (`GunTypes.OUTLAW_*`).
- **Display:** first and third person use no rotation; the GUI uses Euler `[90, tilt-90, 90]` because NeoForge datagen writes `right_rotation` beside a `left_rotation` the 26.3 loader ignores. Players hold every gun in the crossbow pose (a one-handed revolver pose would need a custom `ArmPose`).
- **Outlaw aiming:** outlaws only shoot standing still with the gun raised for 10 ticks, closing to about 70% of range. A synced `GunPose` (lowered, aiming, reloading) drives an arm controller over the walk swing; while aiming the body snaps to the head, or vanilla's body lag leaves the arms off-target.
- **Headshots** (2026-10-31): a player's shot at head height (height only, so generous on four-legged beasts, on purpose) does more: the rifles double, the rest half again. Mobs never get the bonus. The shooter alone hears a "ding".

### Phase 4: cowboy gear
- **Cowboy armor:** defense 2/4/3/2, durability multiplier 10, leather repair. Set bonus +20% mount speed, a transient modifier refreshed every 10 ticks.
- **Lasso:** consumed on a catch, so it can't duplicate vanilla leads; leashes only within `leashSnapDistance`, otherwise just pulls.
- **Horse gear:** any tamed `AbstractHorse`, stored as attachments (`horseshoe_tier`, `saddlebags`); horseshoe stats are permanent attribute modifiers. Saddlebags use the vanilla 9x2 chest menu. Main-hand only.
- **Dynamite:** thrown and fused (2.5 s, power 2.5, breaks blocks like TNT), bounces off blocks, stops against mobs, credited to the thrower.

### Phase 5: outlaws, bounties and townsfolk
- **Gangs** spawn lazily, the first time a poster carrier comes within 64 blocks of the hideout (chunks are loaded then); `BountyData.spawned` stops a second spawn. Overworld only.
- **Rewards:** the poster is used up when the badge is turned in; badges pay even without the poster.
- **Coins:** copper and gold, minted from 4 vanilla nuggets (2x2), 9 copper = 1 gold both ways.
- **Townsfolk** are `AbstractVillager`s with fixed coin trades in code (not 26.x data-driven `TradeSets`). All humanoids share one GeckoLib model (`humanoid`) with per-entity textures. Trades sound like coins or a pour, not a villager.
- **Barmaids:** a `Townsfolk` with no wander goal, on her own feminine model. Five hostesses (`Hostess`: Miss Ruby, Clementine, Josie, Miss Belle, Lupe), each with a look, greetings and two cheaper house specialties. Her name is a translatable custom name, kept if a player renames her. Flirty but PG.
- **Drinks** are trade and chest loot only. **Tipsy** (I-IV) stacks per liquor; each level adds gun spread, III brings nausea, past IV a blackout. Coffee takes a level off, sarsaparilla clears it. Effects are applied in code (`DrinkItem.finishUsingItem`) because Tipsy stacking can't be data-driven.
- **Lawmen** (Sheriff, Deputy, U.S. Marshal, Ranger) are `PathfinderMob`s, not monsters, and never despawn.

### Phase 6: frontier structures
- **Blueprints in code** written to NBT by datagen, with no rotation in the blueprint itself (pieces face north; the jigsaw turns them). Template entities are finalized on placement, so camp outlaws get their guns. Structure `spawn_overrides`: no monsters in towns and saloons; outlaws only in camps.
- **Connected blocks** (fences, bars, panes, walls) keep their connections in the block state and placement copies states as saved, so `Blueprint.toNbt` settles them first.
- **The town is a jigsaw** (see phases 23-24 for what it became).

### Phase 7: steam
- **Transfer:** every holder pushes steam each tick to lower-pressure neighbours (`SteamNetwork.push`). No network graph: simple and stateless; long lines settle over a few ticks.
- **Conversion:** 1 mB water → 1 mB steam, 10 mB a tick while burning; a coal makes 16 B (about 40 crushes or 640 drill blocks).
- **Machines** extend `BaseContainerBlockEntity`, so fuel resolves through `COOKING_FUEL` and hoppers work.
- **Steam Drill:** a pickaxe+shovel `Tool` rule set; the no-steam slowdown is per player (`PlayerEvent.BreakSpeed`), since `Item` is a shared singleton. Steam comes from canisters.
- **Steam Handcar** rides rails; depot standpipes refuel it.

### Phase 8: airship and polar gateway
- **Airship:** a `VehicleEntity` moved on the server only, clients interpolating (as the handcar). The pilot flies with their own keys via `getLastClientInput`: jump climbs, sprint descends (sneak is vanilla's dismount). A second passenger sits aft. The hit box is the gondola; the envelope is only drawn (the renderer widens its culling box). Third person pulls back to 12 blocks aboard (`CalculateDetachedCameraDistanceEvent`).
- **Fuel:** coal or charcoal by hand, boiled into a 16 B tank; no water needed (a closed condenser loop). With no steam it sinks gently and never falls; parked it burns nothing. It also fills from canisters or a steam holder under its keel (a mooring mast). Only its owner can break it, and its fuel is kept when broken (31a).
- **Polar Gateway:** a ring of 12 unbreakable frame stones in an ice crater in snowy biomes (`c:is_snowy` since phase 30). Gateways are found, not built (the frame item is creative-only). The **Polar Resonator** spends 2,000 mB of steam to wake one: this is what gates the Hollow Earth behind steam tech.
- **Polar Compass:** stores the nearest gateway as a lodestone tracker with no lodestone, so the vanilla compass model points it. In the Hollow Earth it points to the nearest return gateway.
- **Crossing:** a vanilla `Portal` block. The Hollow Earth is 1:1 with the Overworld; the first crossing builds a return gateway at the same x/z and records the pair in `GatewayLinks`, so later trips land at the same spot.
- **Made a show of:** the ring lights stone by stone, then the well floods and a beam rises (vanilla beacon code, shader-safe). No nether swirl or portal sound: a frost overlay and rising hum instead. On the way in, players fall down **Symmes' shaft** (bored through the crust on the first crossing, bedrock-capped so it's dark), hang for the reveal swell and a first-visit title card, then drift down onto the ring. The server keeps them safe (hidden Slow Falling, fall distance reset, fall damage cancelled during the descent).
- **Left as is:** vanilla's "experimental settings" warning shows once per world (any extra dimension triggers it; hiding it needs a mixin into world loading). "Loading terrain" shows briefly between worlds.

### Phase 9a: the Hollow Earth's world
- **Layout:** y -64..319. Land mostly 64-110, Highlands to ~180, sea level 63. The crust's underside sways round y 236 with hanging spires and rare floating islands near y 200; crust to 314, then shell bedrock. About 150 blocks of open air. Caves come from vanilla carvers, so the crust and islands stay solid.
- **Crust rock lets sky light through** (so does shell bedrock). Sky light comes from the top of the world, so a ceiling of ordinary stone would leave the land dark (monsters everywhere, no crops); a Nether-style dimension was rejected for that reason. Mined crust rock is a building stone.
- **Terrain follows the climate**, as vanilla's does: one set of noises picks the biome and shapes the land (Highlands where high or cold, Badlands hot and dry with mesa terraces, Steppes dry and temperate, Jungle the rest), thresholds shared in `HollowBiomes`. Regions are ~600-1000 blocks.
- **Biomes** are built from scratch (no vanilla builder, so no vanilla sky colour). The dimension owns the green-gold sky; biomes tint only the fog. No rain. Haze from 48 to 480 blocks.
- **The inner sun** is drawn *in* the world, not the sky box (the crust would hide a sky-box sun): emissive quads on vanilla's `eyes` render type (shader packs treat it like glowing mob parts), hung under the crust over the camera and scaled to look the same size. Time is fixed; beds set spawn but can't be slept in.
- **Placement under a ceiling:** heightmaps find the crust, so vanilla features are re-placed with `hollowfrontier:open_air_scan` (down through air to the first solid block). Everything later that places on the ground must do the same. Falls of water and lava come from springs in the crust's underside; `FAST_LAVA` is dimension-wide so lavafalls reach the ground.
- **Spawning:** the dimension has `hasCeiling` true so `NaturalSpawner` looks down past the roof (it doesn't affect sky light).
- **Landing** (`HollowLanding`): the highest level, open-air floor below y 188 with a roof above; never the crust top, an island or a cave. The return gateway clears trees above it.
- **Hooks:** biome tags `has_mammoths`, `has_terror_birds`, `has_megatherium`, `has_glyptodons`, `has_gray_labs`, so other biomes can join by tag.

### Phase 9b: flora, creatures and lore sites
- **Split** into 9b1-9b3 (one session each).
- **Flora:** five unconfigured features (tree fern, cycad, crystal outcrop, ash vent, standing stonework), grown before the jungle trees so they take their ground first. The jungle was thinned to 18 trees a chunk (at 30 the understory was black). Glowworm vines on the crust were cut: 150 blocks up in daylight, nobody could see them. Vanilla blocks where they do the job (amethyst outcrops; calcite, diorite and quartz stonework).
- **Ambience:** per-biome vanilla-remix sounds and particles; the "cinder moths" idea became particles, not mobs.
- **Small creatures** (`PathfinderMob` + GeckoLib): the Fern Skitter (steals dropped food; tameable with meat since 2026-10-31), Glassback Hare (glowing back crystals), Ash Salamander (walks on lava like a strider, which needs the strider's liquid collision shape too, and basks on vents) and Pale Raven (perches on stonework; flushed, it flies toward the nearest Kindled site, a breadcrumb). Vanilla drops only.
- **Lore sites:** our own structure type, `HollowSiteStructure`: reads the bare noise column at the piece's middle and corners, refuses water, lava, islands and slopes over `max_slope`, sets the floor at the average and beards the land to it. It tries five spots round its chunk, since big footprints rarely find level ground. Blueprints build their floor at layer 0 (or the beard leaves foundations bare). Sites are landmarks 25-37 blocks across, with exclusion zones so camps, labs and Kindled sites don't land on each other, and trees and big flora skip any chunk a site references.
- **The sites:** Kindled henge, gate or temple (Highlands); the abandoned Gray lab; the Symmes expedition camp round its crashed airship, with the journal (1889, "E. Hartwell", a made-up name).
- **Gray Probe:** hovers watching the nearest player; bolts when approached, stared at (the Enderman test) or hit, then blinks out.

### Phase 9c: ores and gear
- **Starsteel** (the lore keeps orichalcum as the Concavians' own word, after Plato): a tier between diamond and netherite. Tools mine as diamond, 1800 uses, speed 10 (the fastest), enchantability 22; armor has diamond's defense and toughness 2.5 but no knockback resistance (netherite keeps that). One **Starsteel Paxel** instead of four tools (it strips through NeoForge's multitool `transformBlock(context, holder)`, since vanilla refuses a transformer that isn't the stack's own). Art: crimson with pale-gold edges and an ice-blue phlogiston stone, so it doesn't read as copper or gold.
- **Sun-forged:** worn or held starsteel mends 1 point every 5 s in full sky light by day (always under the inner sun). Slow, so it doesn't replace Mending, but a reason to wear it under the open sky.
- **Starsteel Horseshoes:** +30% speed, +0.15 jump, no fall damage for horse and rider (horses skip the fall event, so incoming fall damage is cancelled instead).
- **Phlogiston** burns as long as a coal block, lights the Phlogiston Lamp, and its ore glows. Ores are underground, so plain height ranges; both need an iron pickaxe.
- **Overworld seeds:** Gazette rumours, and a rare strange grave on Boot Hill ("The airship's pilot. Not of this world. April 1897", after the Aurora story) with a chest.
- **Reworked vanilla art** was allowed here (recoloured and tweaked, not copies, each marked in ASSETS.md): the metals, ores and armor are reworked iron, emerald, gold and redstone.

### Phase 10a: wild megafauna
- **The four** (GeckoLib, `Megafauna` holds what they share): the Woolly Mammoth (neutral; the herd closes ranks when one is hit, and stragglers walk back to the herd), Terror Bird (hostile by day, since the Hollow has no night; hunts small creatures too and keeps clear of mammoths, so a herd is a refuge), Megatherium (neutral; browses tree leaves, only with mobGriefing) and Glyptodon (passive; hunkers when threatened, sheds scutes like an armadillo).
- **Terror birds stay scarce:** as monsters they'd come and go in numbers, so `checkSpawnRules` passes 1 natural spawn in 4 and never with 2 already within 96 blocks. It runs with its own move controller (vanilla sprinting would add 30%).
- **Spawns** go through biome modifiers on 9a's tags; big mobs also need headroom.
- **Drops and uses:** the Oliphant (calms mammoths and megatheria, scares terror birds), Mammoth Steak, Talon Knife (a kill made with it gives one more hide, tusk, talon or scute: `#hollowfrontier:skinnings`; a built-in Looting I was the first idea, but items can't carry data-driven enchantments at registration), Hide Barding, and Glyptodon shell armor (the full set shells you up while sneaking: projectiles a quarter, explosions half).

### Phase 10b: Gray experiments
- **Tagged specimens** are a synced `GrayTagged` flag on the megafauna, not four more entity types: permanent modifiers (health x1.5, armor +6, speed and attack up), and the implant overrides their nature (tagged mammoths hunt players, the tagged glyptodon never hunkers, none heed the Oliphant). They drop Gray Tech Fragments.
- **Where they come from:** the live station's pens and loose specimens tethered to it; separately, 3% of wild megafauna spawns are escapees, a hint that a station is near. Checking for a nearby station at spawn was ruled out: chunk-generation spawns can't safely read neighbouring chunks' structure references.
- **Live Gray field station:** shares the `gray_lab` structure set with the abandoned lab so the two never overlap. No beacon beam to mark it: beacons don't light under the crust.
- **Gray Sentinel:** a probe that never flees; it beams a visible player for 2 s (breaking sight stops it), stuns them and sets every tagged specimen within 48 on them.
- **Signal Locator:** takes a bearing on the nearest live station, like the Polar Compass.

### Phase 11a: sightings and signs
- **How often:** Overworld nights, a 6% roll every minute, at most one mystery a night (about one night in two). A night slept through rolls 30% for a mutilation or circle found by morning, since nothing ticks while the night is skipped. All switchable since phase 30.
- **Airship sightings** are cosmetic cigars of lights after the 1896-97 wave, crossing well inside simulation distance so they never freeze in unticked chunks.
- **Mutilations** leave a `CattleCarcass` (vanilla's cow, reworked) with faintly glowing cuts and a ring of coarse dirt; **crop circles** lay ripe wheat flat as Flattened Wheat. Fields are found by looking down each column for the topmost ripe wheat (the surface heightmap fails under a roof).
- **The stake-out:** an awake player 20-64 blocks off sees a Gray Probe do the job, and can spook it or kill it mid-job.
- **Reported:** `MysteryLog` feeds the Gazette and the townsfolk's grumbles.
- **The Boot Hill grave's payoff:** the Strange Skull and the pilot's Logbook (in vanilla's `minecraft:alt` glyph font, so it can't be read; the English underneath is a clue).

### Phase 11b: men in black dusters
- **Implanted collaborators,** the outlaws' finished product: drifters the Grays took and sent back as handlers, most of them willingly. That ties them to the outlaws and gives the Mothership's fall something to break.
- **Suspicion** (a player attachment, 0-100, kept through death: "they don't forget a face") rises for killing the Grays' things, using their gear and being seen carrying it; it falls by selling those things to the men in black and slowly over time. 25 warned, 60 marked (shot on sight, hunted). Creative players draw none.
- **They arrive after mysteries** (a visit booked half a day on), lean on the Gazette editor (a "correction" runs instead of the report for 3 days), buy strange goods, and hunt marked players at night.

### Phase 12a: the Kindled
- **The Kindled** are the surviving Concavians, luminous and benevolent: traders in phlogiston shards and the player's allies. Their story is told a line at a talk, nine lines in three tiers of standing, ending at the truth about the Grays, the outlaws' implants and the men in black.
- **Standing** (a player attachment, kept through death): Friend at 4, Ally at 12 (the Kindled Sigil, given once). Hurting a Kindled costs standing and rouses the guardians; below 0 they won't trade and the guardians shoot on sight. Ally-only offers are taken off the merchant's list while anyone else trades, since offers belong to the merchant, not the player.
- **Crystal Guardians** hold their posts and target the Grays' things, those who've wronged the Kindled, and strangers carrying Gray goods. A guardian's first shot at a stranger whose only fault is what they carry is a harmless warning (three guardians had put a survival player at 3 hearts in two seconds).
- **Kindled Sigil:** held, it makes outlaws and men in black within 32 glow: the implant shows.
- **Crystal Ruins** in the Highlands, never near a Kindled site; ravens fly there too. The crystal is a building stone that drops itself.

### Phase 12b: the Sun Archon
- **Who it is:** the last king of Concavia, who went into the inner sun to hold it against the Grays and came out corrupted. Its Sun Temple stands in the Ember Badlands, set close (24/8 chunks) because few badlands spots are level enough; allied Kindled point the way.
- **Three phases:** the King (staff and solar beam), Corona (shielded above the arena while any Sun Pylon stands: break the pylons) and Eclipse (faster, fire, a sunburst). With no survival player near for 10 s it resets to its throne, healed, pylons restored: losing costs nothing you can't come back for.
- **Drops:** one Phlogiston Core and one Starsteel Crown (pools without looting, so never two), sunstone shards; +6 Kindled standing to everyone near.
- **Phlogiston beam damage** (`hollowfrontier:phlogiston_beam`) is shared by every beam in the mod; the Crown halves it.
- **The Concavian Codex** is a written book that rewrites itself when opened: the mod's advancement guide, chapter by chapter.

### Phase 13a: the Grays arrive
- **The Grays fly saucers,** the Concavian craft they stole. The 11a cigar "airships" stay as what frontier folk took their lights for. Wrecks are saucers too.
- **Flyovers and abductions:** Overworld nights, a 5% roll a minute. 40% stop to beam an animal or an upright player; sneaking or stepping out breaks the hold. Players get Levitation (their movement is their client's). A taken player gets **missing time**: set down 300-700 blocks away, dazed, +10 suspicion. `abductPlayers` turns that off.
- **Gray Wardens** keep their distance and fire rays, and blink away when hurt. Two guard each crash site; a few roam the Gray-lab biomes.
- **Crash sites** in the Overworld (by heightmap, clear of towns) and the Hollow Earth (as a hollow site), with Saucer Wreckage blocks that give alien alloy and one working part 10 times in 16, so a full set takes a few wrecks.

### Phase 13b: phlogiston tech
- **3D like the frontier guns,** and the planned Phlogiston Generator was dropped: steam stays fuelled as it was (shards already burn in the boiler).
- **Phlogiston Cells:** 64 charges kept as durability; guns draw from the most worn cell first so they empty one at a time; an empty cell plus a shard recharges it.
- **The guns:** straight hitscan beams, no loading, no misfires. Ray Pistol 7 a shot; Phlogiston Rifle 20 (raised from 16 in phase 14, as it was under the Plains Rifle) and goes through three creatures.
- **The beam you see** is a short-lived entity drawn with vanilla's beacon-beam code from each gun's muzzle (set per gun from photos). From behind the shooter (F5) it runs away from the camera, so that view can't show it.
- **Sounds are synthesized** (`tools/music/Laser.java`): falling-pitch sweeps on detuned soft squares, checked by spectrogram. Borrowed vanilla sounds didn't read as lasers. Synthesized is the rule for sci-fi sounds from here on.
- **The gravity drive** is mended from the damaged one with phlogiston and starsteel (13c's heart).

### Phase 13c: the Prototype Saucer
- **Crafted, not built on a cradle** (the cradle was fiddly in playtest): 8 alien alloy around the mended gravity drive. Set it down like the airship; sneak-use folds it back up with fittings and charge kept as components. A new one comes primed with a cell's charge and the tractor beam and heat laser fitted; only the navigation core is fitted by hand.
- **Flight:** it faces where the pilot looks; W/S, A/D slide, jump climbs, sprint descends, no keys hovers. About three times the airship's speed. Run dry, it draws from the pilot's cells or shards, and the gauge says what to carry. Vertical speed brakes harder than it accelerates so it stops where you let go. Nothing harms it but `/kill`.
- **Heat laser** (V): bolts aimed at what the screen's middle points at, sent by the pilot's client (`SaucerAim`), since the third-person camera is off the pilot's eye line; the server falls back to the eye line if the aim is stale or out of reach. It overheats; it scorches the ground, never inside a town.
- **Tractor beam** (B): lifts creatures, items and players (unless the gamerule `hollowfrontier:saucer_beams_players` is off); a carried player struggles free by holding jump.
- **Camera:** third person rides 2.5 blocks above the pilot's line of sight (the mod's access transformer opens `Camera.move`, since NeoForge only exposes distance), easing out while beaming. A laser sight with a heat bar shows in third person too. The crew's first-person view skips the dome.
- **The model** is code-built: each layer a true 24-gon of bars whose ends sit on the circle (squares turned 15° apart made a star). The saucer draws translucent after the players so the pilot shows through the glass.
- **The crew can use what they hold aboard** (their crosshair is inside its hit box, so uses are passed through).

### Phase 13d: the Mothership
- **The Phlogiston Beacon** (the Archon's Core in starsteel and fragments) calls it, at night, under the open sky (checked by the blocks overhead: sky light lags a block just placed). It can call again any night, one ship at a time within 512 blocks.
- **The fight:** a 600 hp ship that passes through the world (no physics, so it can't snag at that size). Drones, then a shield behind a wave of 4 drones (only that wave holds it, or the shield never fell in playtest) and an abduction beam, then exposed with its core open. Its death ray gives a 2 s warning ring. With no one to fight for 20 s it leaves.
- **Fought from a saucer too:** the drones chase saucers and the death ray targets pilots, so it's a dogfight up there. The saucer's laser does 12 a bolt for this (6 barely scratched it).
- **The ending** (`Leash`, the first fall in a world, once): outlaws near the wreck throw down their guns, men in black vanish, suspicion is wiped and never rises again, the men in black stop coming, and the Grays stop abducting. A third of wild outlaws still spawn: those who still ride, ride by choice. Later Motherships can still be fought for salvage.
- **Sounds synthesized** (hum, death-ray charge, fall).

### Phase 14: polish and release
- **Compat** (all optional, soft-loaded, `compileOnly` APIs with the full mods in `localRuntime`): JEI 31.17.0.100, Jade 26.3.5, Curios 17.0.0-beta.3+26.3 (the only 26.3 build). Each lives in its own `compat/` package, loaded only behind `isLoaded`; the JEI info-page list and lang keys live outside the JEI package so datagen and tests never load JEI.
- **JEI recipes:** 26.x clients only get recipe-book types, so `OnDatapackSyncEvent.sendRecipes(CRUSHING)` sends ours and `SyncedRecipes` catches them at HIGHEST priority, ahead of JEI. The boiler gets an info page, not a category.
- **Jade:** server data providers for what isn't synced (burn time, progress, coal). Jade refuses a data provider that is also a tooltip provider and the refusal stops mod loading, so each has a separate `*Data` provider. Client runs are the check for compat: the game test server didn't catch it.
- **Curios:** the Sunstone Lens (charm) and the Kindled Sigil (necklace). The capability is registered in `RegisterCapabilitiesEvent` (common setup is too late). With Curios loaded, test players need `NetworkRegistry.configureMockConnection`.
- **Balance:** bosses climb 80 → 300 → 600 health; starsteel sits between diamond and netherite.
- **Release:** 1.0.0, built but not uploaded; publishing is the user's call. MIT licence (the user's choice, so it can go in modpacks), with only the reworked-vanilla textures excluded: every bundled sound is our own (2026-11-01: the credits were corrected, as they still named third-party recordings long after the last one was replaced). The mod metadata links the GitHub repo and its issues, and credits the piano's Joplin scores.
- **Post-release audit** (2026-10-29; its findings are all fixed): the project became a git repo, with the author identity in the repo's own config. Gateway effects clear on server stop (a descent carried into the next singleplayer world kept the player fall-safe). AI scans read only loaded chunks' block entities and never load chunks.

### Phase 15: aftermath
- **15a, the holdouts:** after the leash, Gray saucers land at night under guard (the 13a `GraySaucer` with two more states; the only Gray saucer that's saved). Clear the guards and the cache opens at once, not at dawn. Nights follow the Overworld's clock in both worlds (the inner sun never sets). Never on Peaceful (the Wardens would vanish and the cache be free). Guards are tagged with their saucer so two landings never mix.
- **15b, the Mothership's return:** each rematch is harder (+15% health, more drones, a faster death ray, capped at 4 steps because vanilla caps max health at 1024; the ray's warning stays the same so it's still fair). Rematches give the **Cloaking Device** and the **Cargo Hold** in turn (dropped in code, since the loot table can't see the count).
  - **The cloak** works at night with a pilot aboard; others don't see the craft and mobs lose the crew, but the Mothership and its drones see through it (or every rematch would be trivial).
  - **The hold** opens with its own key (H): a vehicle with a custom inventory takes over E for everyone aboard. Folding keeps its contents in vanilla's `container` component.
  - **Owner lock:** fitting, the hold and folding are the owner's (and their scoreboard team's); riding isn't locked.
- **15c, what the frontier makes of itself:** freed outlaws can be sworn in as deputies (a gold coin, in a town) or settled as prospectors (vanilla `convertTo`, so the name carries over).
  - **Restoring the Crystal Ruins** in three paid stages (an endgame sink of 72 phlogiston and 24 starsteel, needs the broken leash and a Kindled ally). Each stage template is a **delta** from the one before, drawn with the same random draws, so it fits any ruin already generated. Stage 3 trades sell a Phlogiston Core: a second road to the beacon.
  - **Waystones** link restored ruins and crossed Polar Gateways for a phlogiston shard. The travel screen sends names as menu data and answers through `clickMenuButton`: no new payload.

### Phase 16: the bank
- **The box is the saved data:** each safe deposit box is a container held in `TownBanks`, one per town and owner, and every window is a vanilla chest menu on that same object, so a raid emptying a box shows in an open window.
- **Interest** counts days on the Bounty Board's dawn clock (a night slept through counts) and compounds within a 7-day window.
- **A raid's share** is taken by worth, gold first; short of copper the gang takes a little less rather than make change. A bagman brought down returns every coin.

### Phase 17: the card table
- **Blackjack** against the barmaid within 8 blocks; refused at two wanted stars. Cards drawn with flat fills and the font, so there's no GUI texture to keep in step. A sitting starts at a 5-coin bet (starting at the limit staked a whole purse on the first click). Coins go through the shared `Purse` helper.

### Phase 18: the bounty board
- **The main board is a hanging entity** (two high, three wide where it fits), because a one-block board was too small to read. It copies the painting's anchor, so templates place and rotate it like any hanging entity. The one-block board (id `bounty_board`, kept for older worlds) stays as the "Small Bounty Board" for the freestanding square board.
- **Posters** are drawn flat with sign-style text (no new textures): a face sketch, name, reward and bearing, stamped TAKEN or DEAD. Wanted players come last, so they never move the posters people are clicking on. A raid leader who got away is pinned on the town's board.
- **Lists** live in `BountyBoards`, keyed by town or by board position. Older boards get their block entity on first use (the server creates a missing one when asked).
- **Tests clean up their fake players** (`TestPlayers.leave`), since leftover survival players broke the Archon's reset test.

### Phase 19: voices (Chatter)
- **One table:** every line lives in `talk/ChatterLines` with its condition and its English; the lang provider reads it from there, so a line can't lose its text or condition. New keys are `chatter.hollowfrontier.<voice>.<moment>.<n>`.
- **Voices:** a speaker has its own voice and a shared one (a wrangler is "wrangler" and "townsfolk"; Josie is "josie" and "barmaid").
- **Picking:** an unconditional line weighs 1, each fact or story act it asks for adds 5, so what fits usually wins without crowding out the rest. Raids and being hunted are urgent: nothing else is said. The last 12 lines a player heard aren't repeated while anything else fits. Cooldowns (not saved) keep a whole gang reloading to one line.
- **Acts** come from the player's own advancements; the last act is the world's (once the leash is broken).
- **A Kindled Sigil counts if it's in the inventory,** so Chatter needs nothing from Curios.
- **Left out of Chatter:** scripted speech that carries numbers or story (raid shouts and pay, bank notices, the Kindled's nine lines).

### Phase 20: the captives
- **The held state is an attachment** (`Captive`), so it works for outlaws, townsfolk and lawmen without touching each class; a held mob has no AI and no gun.
- **Gray pens** in the Gray-lab biomes. Who's held comes from three template variants, since a template's entities are fixed. Cells open the vanilla way (an iron door and a button), or break the bars.
- **The procedure lives on the rig's block entity** (saves with the chunk, no saved data): it starts when a player comes near, runs 60 s with a bar, and stops when every surgeon is down or the rig is broken (`preRemoveSideEffects`, so a chunk unload doesn't count). Finished, the captive gets up an armed outlaw. After the leash there's no procedure.
- **Freed captives** hand over a **Letter Home** for a town (towns had no names then, so it carries the town's structure key and position) that pays at that town's board or sheriff.
- **Brought in alive:** a hunter carrying an unclaimed poster near a pen finds its man held there; freed, he's jailed and follows the hunter, across worlds too, and pays in full at the board, which stamps the poster CAPTURED. The advancement is "...Or Alive", under the badge turn-in's "Dead or Alive".

### Phase 21: frontier humour
- **Tone:** deadpan, from the world and its existing voice. The story stays serious (the captives, the strange grave and the Codex get no jokes); the humour is how the frontier reacts to it. No memes, fourth-wall jokes, references to other games or films, or real people's names.
- **21a, words:** the Gazette's back page (`GazetteBackPage`, English kept in the class like `ChatterLines`, picked by Chatter's weights; length-checked by a test so nothing runs off a book page). Field notes come in eight fixed copies, each in a loot pool, so no custom loot function is needed. Epitaphs are baked into three Boot Hill layouts (no processor picks signs per town). Outlaws taunt a reloading player.
- **21b, Tipsy and the saloon:**
  - **Slurring is client-side on the translated text** (seeded by the text, so it's steady), not extra lang keys: it costs translators nothing and the server sends everyone the same line.
  - **Moonshine sightings** are client-only `AirshipSighting`s (a shorter run, since an entity started outside the client's loaded chunks never ticks), never logged.
  - **Missing Time?** fires when a strong Tipsy effect expires in a town: you wake on a bed, a hay bale (the jail) or in a trough, with a worn Cowboy Hat (a named, worn `cowboy_hat` stack, not a new item). Kept safe on purpose: town spots only, nothing lost, no hostile near. **The clock isn't moved** (in multiplayer it would cost everyone the hour); the message says it instead. Config switch for servers that don't want players moved.
  - **Brawls:** the house pours townsfolk a round at night; tipsy pairs fight with shoves, not damage, and the loser is set outside. The piano hushes during brawls, raids and gunfights (a `hushed` block state).
  - **Bad likeness:** a wanted player's poster shows one of four bad drawings.
- **21c, Grays in disguise:** after the leash, a "Stranger" visits town saloons in the evening (its own model rather than a clothing layer, which would need a second model kept in step). Three unexposed visits and it stays as **Mr. Gray**, the regular, who trades a charged cell for a Sarsaparilla once a day. The Lens or a hit exposes it; exposing the regular costs heat (the game has no goodwill score).

### Phase 22: gun enchantments
- **Effects in code** (`GunEnchantments.apply`), not effect components: vanilla's damage and spread effects only reach arrows and tridents, and the guns are hitscan. Mobs never hold enchanted guns.
- **The six:** Rifling (damage), Quick Load (load time), Deadeye (headshots), Choke (shotgun spread), Dry Powder, Frugal. Costs and weights copy the nearest vanilla enchantment. Tags `enchantable/gun`, `/muzzleloader` and `/shotgun` decide which guns take which; the phlogiston guns take only Deadeye.
- **Vanilla Infinity** is accepted on muzzleloaders through `supportsEnchantment`/`isPrimaryItemFor` (adding the guns to `#enchantable/bow` would bring Power, Punch and Flame too). Frugal is exclusive with it.
- **Glint:** GeckoLib draws no enchantment glint, so `GlintGeoLayer` redraws the model in vanilla's glint-only pass.
- **Held right-click:** vanilla repeats a held use every 4 ticks, which bobbed the gun and fired each chamber as it loaded; `GunTrigger` cancels held repeats while loading.
- **Muzzle smoke** is sent per player: the shooter's own cloud starts at their barrel and is thinner, so it doesn't hide the target.

### Phase 23: towns, built properly
- **Level towns** (`FrontierTownStructure`): a town reads the ground at its middle and on two rings, is refused on water or a spread over 8 blocks (12 for mining camps), and stands at the median; every piece is rigid and the land is bearded to it. Hills had left streets running up and down between flat buildings. A wider ring need only be dry (a mining camp once ran out over a frozen lake).
- **Furnished** with 26.x shelves showing each trade's goods; guns on show sit behind glass or bars. No iron or gold blocks in the décor, so a building can't be quarried for a fortune.
- **Shop signs** are translation keys. New buildings: gunsmith, doctor, post office, homestead, schoolhouse; side windows on every shopfront.

### Phase 24: towns, lived in
- **Full lots:** every street building stands on a 12-wide lot with a side yard, so neighbours meet with no gaps.
- **Town names** ("Coyote Springs", POP. 40-339) come from the world seed and the town's key, written onto the welcome arches by a structure processor (`hollowfrontier:town_name`). The Gazette, bank and letters use them.
- **The railroad** runs out of the depot through a shared pool. Town styles now reach the square, streets and lanes too.
- **New:** bathhouse and barber (a soak in any town water cauldron sobers you), the Gazette office, the ranch; chairs and tables you can sit at (`SeatEntity` takes any `SeatBlock`); town lamps that light one by one at dusk on random ticks (never in the Hollow Earth); six new folk placed in their buildings.
- **Each folk sells only their own trade's goods;** the gunsmith alone sells guns, the banker changes coin.
- **The day** (`Routine`): work, the saloon in the evening, bed at night, through vanilla's home restriction. Asleep they don't move; a word wakes them.
- **Hanging lanterns** always get a chain up to something (`Blueprint.hangLanterns`); chimney campfires get trapdoor pots so the flames don't show.

### Phase 25: Hollow sites rebuilt
- **Palette:** Kindled sites in dark deepslate and tuff with crystal where something was carved and quartz for columns. Starsteel reads as loud red, so it's a cap, a clasp or a keystone at most. Drawn block by block from `Masonry` (with snow, moss, vines and rubble) so no two walls match.
- **Coordinates kept** wherever code or tests depend on them (the pens, the ruins' restoration deltas, the gate). The crystal city's districts use their own seed and are drawn the same at every restoration stage.

### Phase 26: Hollow dungeons
- **Jigsaw under a site:** `HollowSiteStructure` gained a jigsaw depth, reading the ground off the site's own piece. Connectors point down from the bottom layer (a child can only fill a parent's box otherwise).
- **No pits:** the terrain beard acts on every rigid piece, so buried rooms are `BuriedPiece`s with `TerrainAdjustment.NONE`, saved as their own piece type so they keep it after a reload.
- **One shape per kit:** shaft → hub → path → end room, with side rooms; the path's connectors have priority so the end room is always placed (every Sun Temple gets its undercroft, where its hoard now lies).
- **Seal puzzle** with no block entity: the sockets check each other when what stands on one changes, whatever the rotation.
- **Dropped** from the spec: the henge's crypt and the Kindled temple's undercroft.

### Phase 27: new Hollow sites
- Overgrown Ziggurat, Tar Pit, Abandoned Phlogiston Mine (with a buried cavern), Kindled Observatory, Starsteel Foundry and the Sky Shrine on the floating islands (`HollowSiteStructure` gained an island mode).
- **The Gray relay is a feature,** hung from the crust's underside, not a structure.

### Phase 28: Hollow loot
- **Tiers:** every Hollow table gets a rare and a jackpot pool per room on top of its own.
- **Relics are items, not blocks** (eight more block models weren't worth it); the town prospector buys them at their price.
- **Maps** are vanilla exploration maps to one structure; under the ceiling they render cave-style, and the marker is what counts.
- **Trim and sherds are data-driven in 26.3** (`TrimPattern`, `DecoratedPotPattern` registry entries); trim textures are palettised per file, so no atlas is needed. Archaeology spots in the crystal city, the dig, the tar pit and the ziggurat.

### Phase 29: dust devils
- **One class, two types** (`dust_devil`, `ash_devil`), unsaved, drawn entirely in particles.
- **Its own spawner,** not a biome spawn entry, so it never takes a mob-cap slot. It dies down out of its country, in rain, after dark or over water.
- **Lifting players** the saucer beam's way (short Levitation and Slow Falling, since server-side motion doesn't move a player). It carries items and small creatures (with Slow Falling, so it kills no chickens), draws loose things in, and spooks unshod horses.
- **Hats:** only the Cowboy Hat blows off, and sneaking keeps it on. Curse of Binding keeps it too.
- **Not a mystery:** its passes are logged apart from `MysteryLog`, since a mystery there would bring the men in black. They use dust devils as a cover story instead.
- **The swirl** is a particle of ours that looks its devil up every tick and orbits it (vanilla dust ignores the push it's given and read as a cloud).

### Phase 30: modpack friendliness
- **Item capability:** NeoForge only wraps vanilla's containers, so the Boiler and Crusher register `WorldlyContainerWrapper` themselves.
- **`c:steam`** is ours (NeoForge has no constant). Our tanks turn any `c:steam` into ours, so each machine handles one fluid; we push only our own steam out.
- **Tags:** Polar Gateways in `#c:is_snowy` (minus beaches, oceans, rivers and peaks); mining towns in the hill tags; `c:bosses` holds the three bosses, and nothing tagged can be teleported. Townsfolk and story mobs can't be captured, since a town or the story tracks each one. Dust smelting takes `c:dusts/*` under the old recipe ids, so datapack overrides keep working.
- **Mystery config:** a kind switched off rolls a sighting instead. Visits and hunts are checked in the event handlers, so tests and commands still call them directly.

### Phase 31: polish
- **31a, airship owner:** whoever lays it out owns it; only they, their team or a creative player can break it. Unlike the saucer anyone can ride and stoke it (lending it is the point). Only player-caused damage is refused, so it isn't indestructible. Breaking it keeps its coal and steam on the item (dropping coal would round down and lose the steam).
- **31b:** no deprecation warnings (`Tags.BlockItems.*.item()`, `Holder.key()`). The dev rigs (`PhotoBooth`, `Trailer`, `TrailerRecorder`) live in a `dev` source set: in every dev run, out of the jar.
- **31c, the humanoid model:** skins grew to 64x96, old layout kept. Every hat gets brim curls turned up 28°; a bandana bone that becomes a neckerchief when pulled down; dress and robe skirts.

### Between phases 7 and 8: the frontier filled out (2026-10-27 to 10-28)
- **Towns** (what phases 23-24 later built on): a jigsaw town grows from a 39×39 square holding everything a town has exactly one of (the law or a bank, the hotel, the saloon, and a gunsmith since 2026-10-31, since the gunsmith alone sells guns), with streets of shops and homes, street ends (chapel, water tower, depot, town limits, or a lane out to the outskirts) and grand landmarks (courthouse, church, grand hotel and saloon). Reach 116, the most a bearded jigsaw allows. Up to four paint jobs per building, and three styles (Frontier, Desert, Mining), each its own structure in one set, tried in turn like vanilla's village types. `Blueprint.hardenGround` turns bare dirt to packed mud, or worldgen grows trees in the square.
  - **A wheat field in every town,** laid out for the crop circle (its water holes sit in the pattern's gaps), since circles had only ever happened on players' own farms. Circles are centred on the ripe wheat.
  - **Stairs and doors are walked by a fake player in tests** (`town_stairs_climb`, `town_doors_are_clear`), which caught flights a step short and a depot door you couldn't get out of.
- **Lawmen** reuse the outlaw gun AI through `GunWielder`. Their bullets pass through townsfolk, players and prisoners unless that's their target. Hurt townsfolk call lawmen onto the attacker.
- **The jail:** the sheriff's cell holds an unarmed, heckling outlaw. Breaking him out isn't a feature.
- **Seats:** a stool or chair mounts the player on an invisible `SeatEntity`. It can't be `noSave`: 26.3 refuses riders on types that can't serialize.
- **Rattlesnakes** (desert, badlands, savanna): rattle, then strike with poison. **Snakeskin Boots** (a transmute recipe, so the boots keep wear and enchantments) keep snakes off you and count for the cowboy set.
- **Every item and mob needs a use** (a recipe input, a mechanic, a trade it unlocks, or a drop with one of those); selling to townsfolk alone doesn't count. Standing rule.
- **Wanted level** (a synced player attachment, 0-3 stars): heat for hurting townsfolk and lawmen, less for killing free outlaws. Two stars and the law shoots on sight and townsfolk won't trade; three brings posses. Cleared by a fine at the Bounty Board or by dying. Creative players are never wanted. During a raid, stray shots that hurt townsfolk or lawmen add no heat (kills still do).
- **The Frontier Gazette** prints an edition into the book on first read (nearest camp, a saloon, a wanted column, odd news), opened in the vanilla book screen.
- **Sounds** are mod sound events built from re-pitched vanilla sound events in `sounds.json`, so real recordings can replace any entry without code. Sci-fi sounds are synthesized instead (from 13b).
- **Music:** real recordings only where licences are clean; the music discs were removed at the user's request (2026-10-30). **The Saloon Piano's ragtime is rendered in-house:** public-domain Joplin scores from the Mutopia Project through `tools/music/PianoRoll.java`, an additive piano synth with no samples (no sample licence either), voiced as an old upright. It plays its tunes in order with a "Now Playing" line. Kept at Vorbis q4 (the user's call; note-block versions can't hold stride bass or the swing).
- **The town soundscape:** a synced `in_town` flag and ambient one-shots (anvil, horses, boardwalk steps, the bell).
- **Town cats** are vanilla cats with a synced `town_cat` attachment, so taming, breeding and gifts all keep working. Coats (Desert, Saloon) are datagen'd `cat_variant`s; presence, coat and accessory are rolled on join, so every town differs. Town cats don't flee players, lounge on pianos, bars and hay, gather at the fire pit at dusk, and keep snakes off. The sheriff's cat wears the town's only brass star, which spots outlaws and wanted players (a reason to own it). A ghost cat haunts some undertakers (tamed with cooked fish while music plays). Morning gifts come through a global loot modifier, replacing rather than adding, so it's one small item.
- **Goal throttles:** goal selectors only try `canUse` every other tick, depending on the mob's id, so a `tickCount % n == 0` check never passes for odd ids. Use game time + id (`RaidGoals.due`).
- **Town raids:** a gang rides on a town at night (2.5% a check with a player inside, a day's grace and 3 days' cooldown, one raid in the world at a time, never with a 3-star player or posse there). The leader is an Outlaw Boss with a bounty; a safecracker works the vault, a dynamiter lobs dynamite that never breaks blocks, and the bagman carries 75% of the loot: bring him down and it all goes back. Townsfolk shelter, lawmen form a line and call a truce. Defenders are paid and lose heat; a leader who escapes goes on the town's board. Mobs can't steer horses in 26.3, so the raid paths the horses itself. Config is per-world `SYNCED` (FML has no `SERVER` type). A raid that loses its players or its chunks is called off.
- **Steam in town:** the **Steam Handcar** is a `Minecart` subclass (rails and slopes for free) carrying a display block, as the furnace cart does. The depot has a standpipe, a parked handcar and a Railroad Engineer, so players meet steam in town first. Boilers draw from water source blocks beside them.
- **Cut then:** a train coming to town (nowhere for it to come from), train robbery, steam guns.
- **Advancements** are built in `datagen/ModAdvancements` (one method per tab, one table of titles); mod events use one shared `ModTrigger` registered as several trigger types. In 26.3 the recipe bootstrap owns the advancement registry, so tabs are added inside that lambda.
- **Creative menu:** themed tabs built only in `registry/ModTabs`, ordered by hand section by section; the `every_item_in_a_tab` test fails until a new item has a place. Items also join the vanilla tabs beside their relatives.
- **The worn Cowboy Hat** is real geometry swapped in through NeoForge's armor-model hook, wrapping whichever humanoid armor model would have drawn the helmet, so players, stands and mobs all wear it.

## Since phase 31

### Art passes (2026-10-30, the user's picks from texture reviews)
- **Icons** have a dark outline and top-left light, like vanilla's. Reworked vanilla where it already read well. The polar portal is animated (32 frames). Compass and locator needles are one-pixel Bresenham lines, so they don't break on diagonals. Spawn eggs each carry a small likeness of their mob, as vanilla's now do.
- **Mobs:** megafauna fur in long hanging strands; NPC cloth lit at the top with creases. **Kept as they were (the user's call):** the NPC faces (a detailed pass was tried and turned down), the Sun Archon (two redesigns tried), and the Kindled's look (mock-ups of five new looks, a middle ground of mixed skin tones with an ivory robe, and purple eyes were made; the middle ground was the advice, so they don't read as one pale race. Still open).
- **Phlogiston is ember orange** with a white-hot core (the user's pick of eight palettes): it reads as fire and stays clear of the Archon's gold and the brass. Phlogiston is the material wherever it's drawn as a stone or a light. **The Grays' tech stays teal** (implants, saucer lights, the Ray Pistol's green, the guardians' eyes and beams, the Kindled's own eyes), via `Concavia.GLOW`. "Lantern Men" is what frontier folk call the Kindled.

### Optimization (2026-10-31, from a read-through, not profiled)
- **Steam neighbour lookups are cached** (`BlockCapabilityCache` per side), safe because our fluid capabilities ignore the side and NeoForge drops a cache when the block changes or its chunk unloads.
- **Missing Time? scans only inside the town's box.**

### Trailer and store page (2026-10-30 onward, dev source set only)
- **The rig** (`Trailer`, `TrailerRecorder`) films each shot in `trailer/edit.csv` from `run/saves/hf_trailer`, recording the render target through ffmpeg at a steady 60 fps (repeating or skipping frames as the game's rate wobbles). `cut.ps1` cuts and scores; `short.ps1` makes the 9:16 short from the finished cut. Pinned spots (main street, pasture) keep re-shoots framed the same; `-PtrailerStreet` / `-PtrailerPasture` take `search` or x,z.
- **Music:** an original synthesized score (`tools/music/TrailerScore.java`), cued from event times in `edit.csv`. The western half is Kevin MacLeod's "Still Pickin'" (CC BY 4.0, kept local): credit it wherever the trailer is posted. The game's own sounds come from the dev run's downloaded assets at cut time and are never copied into the repo.
- **Shaders:** `-Pshaders` films through Iris 1.11.7 and Complementary Reimagined r5.9.3 (licence allows content creation; credit it) into `trailer/clips_shaders`. Sodium 0.9.3-beta.1 (the 0.9.2 Iris bundles crashes on NeoForge 26.3.0.65+). The run turns off NeoForge's dev-only strict Blaze3D validation, which Iris trips. The pack is never bundled. Two Hollow particles that the pack doesn't fade into the haze are suppressed in the shader shoot only.
- **Glow under shaders, for players too:** `compat/iris/IrisCompat` assigns GeckoLib's glow-mask pipeline to Iris's emissive entities, or glowing parts went dim at night under a pack. Iris is `compileOnly`.
- **The trailer world** is regenerated from its seed when structures change (earlier worlds are kept as `run/saves/hf_trailer_*` backups). In a fresh world the Hollow vista and saucer flight outrun generation and film sky, so re-shoot them once the land exists. The vista skips spots with trees in the camera's path or the herd's box.
- **Mod fixes the trailer found:** poster sketch details z-fought the skin under them (now a tenth of a unit in front); the card table's title squeezes to fit a long dealer name.
- **Left as is (the user's call):** under the pack the abduction loses its red horizon, and the pack draws its own clouds under the crust.
- **Branding** ("Two Worlds", the user's pick of four): the banner tears a sundown ride against the Prototype Saucer over the Hollow's herd, with the wanted-poster logo between. Generated by `branding/source` (HTML through headless Edge). Page GIFs in `branding/gifs`, cut from the shader clips.
- **CURSEFORGE.md** covers everything through phase 31; its `PAGE SETTINGS` comment holds the form fields. The source link needs the GitHub repo made public first.

### Going public (2026-11-01, on request)
- **Clean history:** the repo was recreated with one starting commit, so the old history (with the author's real name and email, and the old place names) isn't public. It's kept locally in `.git/old-history.bundle`. Commits use the GitHub handle and its noreply address (repo-local git config).
- **Issues:** bug and suggestion forms (`.github/ISSUE_TEMPLATE`), blank issues off, questions sent to Discussions, a pinned Known issues post (#1, label `known`).
- **One 1.0.0 changelog entry:** nothing had shipped, so the per-phase development log (with superseded content like zinc) became a single player-facing 1.0.0 entry. The development story is in this file.
- **Removed:** AUDIT.md (a snapshot whose findings are all fixed) and `branding/old` (the replaced sheriff's-star logo set).
- **CI:** `.github/workflows/build.yml` runs `gradlew build` on Java 25 for every push and pull request.

### Store upload prep (2026-11-01, on request)
- **Jar name** carries the loader and MC version (`hollowfrontier-neoforge-26.3-1.0.0.jar`), as both stores list every MC version's file side by side. The mod version itself stays `1.0.0`; Modrinth's version number is `1.0.0+26.3`.
- **One page for both stores:** CURSEFORGE.md stays the source (both take Markdown); `prepareRelease` strips its notes-to-self into `build/release/description.md` and pulls the newest changelog section, and RELEASING.md holds each store's form fields. Uploading stays manual: both stores review a new project by hand, so publishing from CI can wait until the projects exist.
- **In-game logo:** the mod list shows the wanted-poster logo (`logoFile`), copied from `branding` at build time rather than kept twice.
- **Jar cleanup:** datagen's `.cache/` hash files had shipped in every jar (the MDK's exclude never matched); now excluded.
- **Store images:** the page embeds the full GIFs and banner straight from this repo (raw.githubusercontent.com), since CurseForge caps uploads at 2 MB and shrinking the GIFs that far left them 400 px wide. The galleries get 14 1080p stills from the shader trailer clips instead (`branding/stills`). Moving or renaming anything in `branding/gifs` breaks the live store pages.
