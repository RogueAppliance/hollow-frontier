package com.hollowfrontier.worldgen.structure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

/**
 * Frontier structures from Blueprints. The saloon and outlaw camp are single-piece jigsaw structures. Towns come in three
 * styles (TownStyle: frontier, desert, mining), each its own jigsaw structure grown from a town square (TownLayout, depth
 * 3, up to 116 blocks out) with pools built from TownBlueprints.pools; every piece is rigid, and a town only stands on
 * dry, near-level ground (FrontierTownStructure). The three share one structure set, like vanilla's village types: whichever suits the biome is built.
 * Spawn rules: no hostile mob spawns inside towns or saloons; outlaws are the only monsters that spawn in camps.
 * The Hollow Earth's lore sites (9b3) are HollowSiteStructures, which find the ground under the crust themselves.
 */
public final class ModStructures {
    private ModStructures() {}

    /** Open frontier biomes: deserts, plains, savannas, badlands. */
    public static final TagKey<Biome> IS_FRONTIER = TagKey.create(Registries.BIOME, HollowFrontier.id("is_frontier"));

    public static final ResourceKey<Structure> SALOON = structure("saloon");
    public static final ResourceKey<Structure> FRONTIER_TOWN = TownStyle.FRONTIER.structure();
    /** Every town structure, one per style. */
    public static final List<ResourceKey<Structure>> TOWNS = java.util.Arrays.stream(TownStyle.values()).map(TownStyle::structure).toList();
    /** Lootr's own tag: its chests lose the marker raids find a vault by, so towns stay out of it. Harmless without Lootr. */
    public static final TagKey<Structure> LOOTR_BLACKLIST = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("lootr", "blacklist"));
    private static final int TOWN_DEPTH = 3;
    private static final int TOWN_REACH = 116; // the most a jigsaw structure with a beard may span from its centre (128 - 12)
    public static final ResourceKey<Structure> OUTLAW_CAMP = structure("outlaw_camp");
    /** A rare ice crater in frozen biomes with a Polar Gateway at the bottom (PolarBlueprints). */
    public static final ResourceKey<Structure> POLAR_GATEWAY = structure("polar_gateway");
    public static final TagKey<Biome> HAS_POLAR_GATEWAY = TagKey.create(Registries.BIOME, HollowFrontier.id("has_polar_gateway"));
    // The Hollow Earth's lore sites (9b3), placed by HollowSiteStructure on the ground under the crust (HollowBlueprints)
    /** A henge, a sealed gate or a ruined temple, in the Concavian Highlands. */
    public static final ResourceKey<Structure> KINDLED_SITE = structure("kindled_site");
    public static final ResourceKey<Structure> GRAY_LAB = structure("gray_lab");
    /** A live Gray field station (10b), where the tagged megafauna come from (HollowBlueprints.grayStation). */
    public static final ResourceKey<Structure> GRAY_STATION = structure("gray_station");
    // 20: the Gray pens, where the frontier's missing are held
    public static final ResourceKey<Structure> GRAY_PENS = structure("gray_pens");
    public static final ResourceKey<Structure> SYMMES_CAMP = structure("symmes_camp");
    /** Concavia (12a): the ruined crystal city where the last Kindled live (ConcaviaBlueprints). */
    public static final ResourceKey<Structure> CRYSTAL_RUINS = structure("crystal_ruins");
    public static final TagKey<Biome> HAS_CRYSTAL_RUINS = TagKey.create(Registries.BIOME, HollowFrontier.id("has_crystal_ruins"));
    /** The Sun Archon's ziggurat (12b) in the Ember Badlands (ConcaviaBlueprints.sunTemple). */
    public static final ResourceKey<Structure> SUN_TEMPLE = structure("sun_temple");
    public static final TagKey<Biome> HAS_SUN_TEMPLE = TagKey.create(Registries.BIOME, HollowFrontier.id("has_sun_temple"));
    /** The Sun Temples: what an allied Kindled points the way to. */
    public static final TagKey<Structure> SUN_TEMPLES = TagKey.create(Registries.STRUCTURE, HollowFrontier.id("sun_temples"));
    /** A downed Gray saucer (13a, UfoBlueprints): on the Overworld's surface, and on the Hollow Earth's floor. */
    public static final ResourceKey<Structure> CRASH_SITE = structure("crash_site");
    public static final ResourceKey<Structure> HOLLOW_CRASH_SITE = structure("hollow_crash_site");
    // 27: the wild sites, each in its own biome (the Sky Shrine on the islands over any of them)
    public static final ResourceKey<Structure> ZIGGURAT = structure(WildBlueprints.ZIGGURAT);
    public static final ResourceKey<Structure> TAR_PIT = structure(WildBlueprints.TAR_PIT);
    public static final ResourceKey<Structure> PHLOGISTON_MINE = structure(WildBlueprints.PHLOGISTON_MINE);
    public static final ResourceKey<Structure> OBSERVATORY = structure(WildBlueprints.OBSERVATORY);
    public static final ResourceKey<Structure> FOUNDRY = structure(WildBlueprints.FOUNDRY);
    public static final ResourceKey<Structure> SKY_SHRINE = structure(WildBlueprints.SKY_SHRINE);
    /** The wild sites (27), in the order {@link #WILD_SITE_SETS} spaces them. */
    public static final java.util.List<ResourceKey<Structure>> WILD_SITES = java.util.List.of(ZIGGURAT, TAR_PIT, PHLOGISTON_MINE, OBSERVATORY, FOUNDRY, SKY_SHRINE);    public static final TagKey<Biome> HAS_CRASH_SITE = TagKey.create(Registries.BIOME, HollowFrontier.id("has_crash_site"));
    public static final TagKey<Structure> CRASH_SITES = TagKey.create(Registries.STRUCTURE, HollowFrontier.id("crash_sites"));
    public static final TagKey<Biome> HAS_KINDLED_SITES = TagKey.create(Registries.BIOME, HollowFrontier.id("has_kindled_sites"));
    /** The Kindled sites: where a flushed Pale Raven flies. */
    public static final TagKey<Structure> KINDLED_SITES = TagKey.create(Registries.STRUCTURE, HollowFrontier.id("kindled_sites"));

    private static final ResourceKey<StructureTemplatePool> SALOON_POOL = pool("saloon");
    private static final ResourceKey<StructureTemplatePool> OUTLAW_CAMP_POOL = pool("outlaw_camp");
    private static final ResourceKey<StructureTemplatePool> POLAR_GATEWAY_POOL = pool("polar_gateway");
    private static final ResourceKey<StructureTemplatePool> KINDLED_SITE_POOL = pool("kindled_site");
    private static final ResourceKey<StructureTemplatePool> GRAY_LAB_POOL = pool("gray_lab");
    private static final ResourceKey<StructureTemplatePool> GRAY_STATION_POOL = pool("gray_station");
    private static final ResourceKey<StructureTemplatePool> GRAY_PENS_POOL = pool("gray_pens");
    private static final ResourceKey<StructureTemplatePool> SYMMES_CAMP_POOL = pool("symmes_camp");
    private static final ResourceKey<StructureTemplatePool> CRYSTAL_RUINS_POOL = pool("crystal_ruins");
    private static final ResourceKey<StructureTemplatePool> SUN_TEMPLE_POOL = pool("sun_temple");
    private static final ResourceKey<StructureTemplatePool> CRASH_SITE_POOL = pool("crash_site");

    public static void bootstrapPools(BootstrapContext<StructureTemplatePool> context) {
        Holder<StructureTemplatePool> empty = context.lookup(Registries.TEMPLATE_POOL).getOrThrow(Pools.EMPTY);
        singleTemplatePool(context, empty, SALOON_POOL, "saloon");
        for (TownStyle style : TownStyle.values()) {
            TownBlueprints.pools(style).forEach((name, entries) -> townPool(context, empty, style, name, entries));
        }
        singleTemplatePool(context, empty, OUTLAW_CAMP_POOL, "outlaw_camp");
        singleTemplatePool(context, empty, POLAR_GATEWAY_POOL, "polar_gateway");
        context.register(KINDLED_SITE_POOL, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single(HollowFrontier.id(HollowBlueprints.KINDLED_HENGE).toString()), 3),
                Pair.of(StructurePoolElement.single(HollowFrontier.id(HollowBlueprints.KINDLED_GATE).toString()), 2),
                Pair.of(StructurePoolElement.single(HollowFrontier.id(HollowBlueprints.KINDLED_TEMPLE).toString()), 2)),
                StructureTemplatePool.Projection.RIGID));
        // One lab in three has the buried complex under it (26)
        context.register(GRAY_LAB_POOL, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single(HollowFrontier.id(HollowBlueprints.GRAY_LAB).toString()), 2),
                Pair.of(StructurePoolElement.single(HollowFrontier.id(HollowBlueprints.GRAY_LAB_DEEP).toString()), 1)),
                StructureTemplatePool.Projection.RIGID));
        singleTemplatePool(context, empty, GRAY_STATION_POOL, HollowBlueprints.GRAY_STATION);
        context.register(GRAY_PENS_POOL, new StructureTemplatePool(empty, java.util.stream.IntStream.rangeClosed(1, HollowBlueprints.PENS_VARIANTS)
                .<Pair<java.util.function.Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>mapToObj(n -> Pair.of(StructurePoolElement.single(HollowFrontier.id(HollowBlueprints.pensTemplate(n)).toString()), 1)).toList(),
                StructureTemplatePool.Projection.RIGID));
        singleTemplatePool(context, empty, SYMMES_CAMP_POOL, HollowBlueprints.SYMMES_CAMP);
        singleTemplatePool(context, empty, CRYSTAL_RUINS_POOL, ConcaviaBlueprints.CRYSTAL_RUINS);
        singleTemplatePool(context, empty, SUN_TEMPLE_POOL, ConcaviaBlueprints.SUN_TEMPLE);
        singleTemplatePool(context, empty, CRASH_SITE_POOL, UfoBlueprints.CRASH_SITE);
        // The dungeons under the sites (26): each kit's pools, the rooms falling back on its caps
        for (DungeonBlueprints.Kit kit : DungeonBlueprints.KITS) {
            for (DungeonBlueprints.PoolSpec spec : DungeonBlueprints.pools(kit)) {
                Holder<StructureTemplatePool> fallback = spec.fallback().equals("minecraft:empty") ? empty
                        : context.lookup(Registries.TEMPLATE_POOL).getOrThrow(pool(spec.fallback().substring((HollowFrontier.MODID + ":").length())));
                List<Pair<java.util.function.Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> elements = new java.util.ArrayList<>();
                for (int i = 0; i < spec.templates().size(); i++) {
                    elements.add(Pair.of(StructurePoolElement.single(HollowFrontier.id(spec.templates().get(i)).toString()), spec.weights().get(i)));
                }
                context.register(pool(kit.id() + "/" + spec.pool()), new StructureTemplatePool(fallback, elements, StructureTemplatePool.Projection.RIGID));
            }
        }
        // 27: the wild sites, a template each; the mine's cavern hangs under it
        for (String site : java.util.List.of(WildBlueprints.ZIGGURAT, WildBlueprints.TAR_PIT, WildBlueprints.PHLOGISTON_MINE, WildBlueprints.OBSERVATORY,
                WildBlueprints.FOUNDRY, WildBlueprints.SKY_SHRINE, WildBlueprints.MINE_CAVERN)) {
            singleTemplatePool(context, empty, pool(site), site);
        }
    }

    /** How deep the jigsaw goes under a site with a dungeon (26): the shaft, the hub, the path and its end, and side rooms. */
    static final int DUNGEON_DEPTH = 6;

    public static void bootstrapStructures(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        StructureSpawnOverride noMonsters = new StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType.STRUCTURE, WeightedList.of());
        StructureSpawnOverride outlawsOnly = new StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType.STRUCTURE,
                WeightedList.<MobSpawnSettings.SpawnerData>builder()
                        .add(new MobSpawnSettings.SpawnerData(ModEntities.OUTLAW.get(), UniformInt.of(1, 2)), 3)
                        .add(new MobSpawnSettings.SpawnerData(ModEntities.OUTLAW_GUNSLINGER.get(), UniformInt.of(1, 1)), 1)
                        .build());

        register(context, SALOON, biomes, pools.getOrThrow(SALOON_POOL), 1, Map.of(MobCategory.MONSTER, noMonsters));
        for (TownStyle style : TownStyle.values()) {
            Structure.StructureSettings settings = new Structure.StructureSettings(biomes.getOrThrow(style.biomes()),
                    Map.of(MobCategory.MONSTER, noMonsters), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN);
            // Only on dry, near-level ground; mining camps put up with rougher country than the plains and desert towns
            context.register(style.structure(), new FrontierTownStructure(settings, pools.getOrThrow(pool(style.template(TownLayout.SQUARES))),
                    TOWN_DEPTH, TOWN_REACH, style == TownStyle.MINING ? 12 : 8));
        }
        register(context, OUTLAW_CAMP, biomes, pools.getOrThrow(OUTLAW_CAMP_POOL), 1, Map.of(MobCategory.MONSTER, outlawsOnly));
        // Sunk so the crater rim is at ground level; no beard (the template clears its own bowl).
        context.register(POLAR_GATEWAY, new JigsawStructure(new Structure.StructureSettings(biomes.getOrThrow(HAS_POLAR_GATEWAY), Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE), pools.getOrThrow(POLAR_GATEWAY_POOL), 1,
                ConstantHeight.of(VerticalAnchor.absolute(-PolarBlueprints.SINK)), false, Heightmap.Types.WORLD_SURFACE_WG));
        // The Hollow Earth's lore sites: no heightmap there (its top is the crust), so they find the ground themselves
        context.register(KINDLED_SITE, new HollowSiteStructure(hollowSite(biomes.getOrThrow(HAS_KINDLED_SITES), Map.of()),
                pools.getOrThrow(KINDLED_SITE_POOL), 8, DUNGEON_DEPTH));
        context.register(GRAY_LAB, new HollowSiteStructure(hollowSite(biomes.getOrThrow(HollowBiomes.HAS_GRAY_LABS), Map.of(MobCategory.MONSTER, noMonsters)),
                pools.getOrThrow(GRAY_LAB_POOL), 5, DUNGEON_DEPTH - 1)); // its rooms have more doors: a level less keeps it to a dozen or so
        context.register(GRAY_STATION, new HollowSiteStructure(hollowSite(biomes.getOrThrow(HollowBiomes.HAS_GRAY_LABS), Map.of(MobCategory.MONSTER, noMonsters)),
                pools.getOrThrow(GRAY_STATION_POOL), 5));
        context.register(GRAY_PENS, new HollowSiteStructure(hollowSite(biomes.getOrThrow(HollowBiomes.HAS_GRAY_LABS), Map.of(MobCategory.MONSTER, noMonsters)),
                pools.getOrThrow(GRAY_PENS_POOL), 4));
        context.register(SYMMES_CAMP, new HollowSiteStructure(hollowSite(biomes.getOrThrow(HollowBiomes.IS_HOLLOW_EARTH), Map.of(MobCategory.MONSTER, noMonsters)),
                pools.getOrThrow(SYMMES_CAMP_POOL), 6));
        context.register(CRYSTAL_RUINS, new HollowSiteStructure(hollowSite(biomes.getOrThrow(HAS_CRYSTAL_RUINS), Map.of(MobCategory.MONSTER, noMonsters)),
                pools.getOrThrow(CRYSTAL_RUINS_POOL), 8));
        context.register(SUN_TEMPLE, new HollowSiteStructure(hollowSite(biomes.getOrThrow(HAS_SUN_TEMPLE), Map.of(MobCategory.MONSTER, noMonsters)),
                pools.getOrThrow(SUN_TEMPLE_POOL), 8, DUNGEON_DEPTH - 1));
        // The crash sites (13a): one template for both worlds, on the surface by heightmap up top and found under the crust below
        context.register(CRASH_SITE, new JigsawStructure(new Structure.StructureSettings(biomes.getOrThrow(HAS_CRASH_SITE), Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN), pools.getOrThrow(CRASH_SITE_POOL), 1,
                ConstantHeight.of(VerticalAnchor.absolute(0)), false, Heightmap.Types.WORLD_SURFACE_WG));
        context.register(HOLLOW_CRASH_SITE, new HollowSiteStructure(hollowSite(biomes.getOrThrow(HollowBiomes.IS_HOLLOW_EARTH), Map.of()),
                pools.getOrThrow(CRASH_SITE_POOL), 6));
        // 27: the wild sites
        HolderSet<Biome> jungle = HolderSet.direct(biomes.getOrThrow(HollowBiomes.PRIMORDIAL_JUNGLE));
        HolderSet<Biome> steppes = HolderSet.direct(biomes.getOrThrow(HollowBiomes.CRYSTAL_STEPPES));
        HolderSet<Biome> badlands = HolderSet.direct(biomes.getOrThrow(HollowBiomes.EMBER_BADLANDS));
        context.register(ZIGGURAT, new HollowSiteStructure(hollowSite(jungle, Map.of(MobCategory.MONSTER, noMonsters)), pools.getOrThrow(pool(WildBlueprints.ZIGGURAT)), 6));
        context.register(TAR_PIT, new HollowSiteStructure(hollowSite(jungle, Map.of()), pools.getOrThrow(pool(WildBlueprints.TAR_PIT)), 5));
        context.register(PHLOGISTON_MINE, new HollowSiteStructure(hollowSite(steppes, Map.of(MobCategory.MONSTER, noMonsters)), pools.getOrThrow(pool(WildBlueprints.PHLOGISTON_MINE)), 5, 2));
        context.register(OBSERVATORY, new HollowSiteStructure(hollowSite(steppes, Map.of(MobCategory.MONSTER, noMonsters)), pools.getOrThrow(pool(WildBlueprints.OBSERVATORY)), 5));
        context.register(FOUNDRY, new HollowSiteStructure(hollowSite(badlands, Map.of(MobCategory.MONSTER, noMonsters)), pools.getOrThrow(pool(WildBlueprints.FOUNDRY)), 5));
        context.register(SKY_SHRINE, new HollowSiteStructure(hollowSite(biomes.getOrThrow(HollowBiomes.IS_HOLLOW_EARTH), Map.of()),
                pools.getOrThrow(pool(WildBlueprints.SKY_SHRINE)), 3, 1, true));
    }

    // Spacing/separation in chunks; salts are arbitrary but must stay unique and stable.
    public static void bootstrapSets(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
        set(context, "saloon", structures.getOrThrow(SALOON), 36, 14, 482_910_331);
        // One set for all three town styles, each tried in turn until one suits the biome (as with villages)
        context.register(ResourceKey.create(Registries.STRUCTURE_SET, HollowFrontier.id("frontier_town")), new StructureSet(
                java.util.Arrays.stream(TownStyle.values()).map(style -> StructureSet.entry(structures.getOrThrow(style.structure()))).toList(),
                new RandomSpreadStructurePlacement(44, 18, RandomSpreadType.LINEAR, 739_201_557)));
        set(context, "outlaw_camp", structures.getOrThrow(OUTLAW_CAMP), 26, 9, 118_403_926);
        set(context, "polar_gateway", structures.getOrThrow(POLAR_GATEWAY), 64, 24, 1_908_042_117);
        // Kindled sites are common enough that a raven flushed in the Highlands has one to fly to (it looks 24 chunks out);
        // the lab and the camp are rare finds. They keep apart: a camp never within 6 chunks of a lab, a Kindled site never
        // within 6 of a camp (the lab and the Kindled sites never share a biome).
        HolderGetter<StructureSet> sets = context.lookup(Registries.STRUCTURE_SET);
        // The Gray sites share one set (10b), so an abandoned lab and a live station never land on each other, and the camp
        // keeps clear of both: two in three of its spots are stations.
        context.register(setKey("gray_lab"), new StructureSet(List.of(StructureSet.entry(structures.getOrThrow(GRAY_LAB), 1),
                StructureSet.entry(structures.getOrThrow(GRAY_STATION), 2)), new RandomSpreadStructurePlacement(40, 16, RandomSpreadType.LINEAR, 1_947_070_819)));
        set(context, "symmes_camp", structures.getOrThrow(SYMMES_CAMP), 40, 14, 1_818_041_029, sets.getOrThrow(setKey("gray_lab")));
        // The pens (20) are rarer than the stations they share the badlands with, and keep clear of them
        set(context, "gray_pens", structures.getOrThrow(GRAY_PENS), 52, 18, 1_520_200_413, sets.getOrThrow(setKey("gray_lab")));
        set(context, "kindled_site", structures.getOrThrow(KINDLED_SITE), 22, 8, 1_066_310_771, sets.getOrThrow(setKey("symmes_camp")));
        // The crystal ruins (12a) are rarer than the Kindled sites, and never within 6 chunks of one
        set(context, "crystal_ruins", structures.getOrThrow(CRYSTAL_RUINS), 26, 9, 1_912_120_401, sets.getOrThrow(setKey("kindled_site")));
        // The Sun Temples (12b): few find ground level enough in the badlands (about one spot in twelve), so the spots are close
        // set, a temple every 1300-odd blocks or so; they keep clear of the Gray sites they share the badlands with
        set(context, "sun_temple", structures.getOrThrow(SUN_TEMPLE), 24, 8, 1_208_120_555, sets.getOrThrow(setKey("gray_lab")));
        // The wrecks (13a) are rare finds: well apart from the towns up top, and from the Gray sites below
        set(context, "crash_site", structures.getOrThrow(CRASH_SITE), 48, 16, 1_947_070_771, sets.getOrThrow(setKey("frontier_town")));
        set(context, "hollow_crash_site", structures.getOrThrow(HOLLOW_CRASH_SITE), 36, 12, 1_896_189_712, sets.getOrThrow(setKey("gray_lab")));
        // 27: the wild sites, each well clear of the Gray sites (the mine and the observatory share the steppes with them)
        int[][] wild = {{32, 11, 1_427_270_101}, {30, 10, 1_427_270_202}, {32, 11, 1_427_270_303}, {34, 12, 1_427_270_404}, {30, 10, 1_427_270_505},
                {20, 7, 1_427_270_606}};
        for (int i = 0; i < ModStructures.WILD_SITES.size(); i++) {
            ResourceKey<Structure> site = ModStructures.WILD_SITES.get(i);
            set(context, site.identifier().getPath(), structures.getOrThrow(site), wild[i][0], wild[i][1], wild[i][2], sets.getOrThrow(setKey("gray_lab")));
        }    }

    // A town pool: every palette variant of each listed piece in this style, sharing its weight. Streets and lanes
    // follow the terrain; buildings stay rigid (placed level at the height of the spot they join).
    private static void townPool(BootstrapContext<StructureTemplatePool> context, Holder<StructureTemplatePool> fallback, TownStyle style,
            String name, Map<String, Integer> entries) {
        List<Pair<StructurePoolElement, Integer>> elements = new java.util.ArrayList<>();
        entries.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String base = entry.getKey();
            int variants = TownPalettes.count(base);
            // Everything rigid, streets included: the town is one level (on near-level ground), so the boardwalks meet the
            // porches and the terrain blend makes the slopes round it, not dirt banks between a street and its buildings
            StructureTemplatePool.Projection projection = StructureTemplatePool.Projection.RIGID;
            for (int i = 0; i < variants; i++) {
                String template = HollowFrontier.id(style.template(TownPalettes.variant(base, i))).toString();
                // The streets carry the welcome arches, whose signs get the town's name as it's built (24a)
                var element = base.startsWith("street_") ? StructurePoolElement.single(template, TOWN_NAMES) : StructurePoolElement.single(template);
                elements.add(Pair.of(element.apply(projection), Math.max(1, entry.getValue() * 12 / variants)));
            }
        });
        context.register(pool(style.template(name)), new StructureTemplatePool(fallback, elements));
    }

    private static final Holder<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList> TOWN_NAMES = Holder.direct(
            new net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList(List.of(TownNameProcessor.INSTANCE)));

    private static Structure.StructureSettings hollowSite(HolderSet<Biome> biomes, Map<MobCategory, StructureSpawnOverride> spawnOverrides) {
        return new Structure.StructureSettings(biomes, spawnOverrides, GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN);
    }

    private static void singleTemplatePool(BootstrapContext<StructureTemplatePool> context, Holder<StructureTemplatePool> fallback,
            ResourceKey<StructureTemplatePool> key, String template) {
        context.register(key, new StructureTemplatePool(fallback,
                List.of(Pair.of(StructurePoolElement.single(HollowFrontier.id(template).toString()), 1)),
                StructureTemplatePool.Projection.RIGID));
    }

    private static void register(BootstrapContext<Structure> context, ResourceKey<Structure> key, HolderGetter<Biome> biomes,
            Holder<StructureTemplatePool> pool, int depth, Map<MobCategory, StructureSpawnOverride> spawnOverrides) {
        Structure.StructureSettings settings = new Structure.StructureSettings(
                biomes.getOrThrow(IS_FRONTIER), spawnOverrides, GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN);
        context.register(key, new JigsawStructure(settings, pool, depth, ConstantHeight.of(VerticalAnchor.absolute(0)), false,
                Heightmap.Types.WORLD_SURFACE_WG));
    }

    private static void set(BootstrapContext<StructureSet> context, String name, Holder<Structure> structure, int spacing, int separation, int salt) {
        context.register(ResourceKey.create(Registries.STRUCTURE_SET, HollowFrontier.id(name)),
                new StructureSet(structure, new RandomSpreadStructurePlacement(spacing, separation, RandomSpreadType.LINEAR, salt)));
    }

    /** A set kept {@code 6} chunks clear of another set's structures. */
    private static void set(BootstrapContext<StructureSet> context, String name, Holder<Structure> structure, int spacing, int separation, int salt,
            Holder<StructureSet> awayFrom) {
        context.register(setKey(name), new StructureSet(structure, new RandomSpreadStructurePlacement(Vec3i.ZERO,
                AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, salt,
                Optional.of(new AbstractSpreadingStructurePlacement.ExclusionZone(awayFrom, 6)), spacing, separation, RandomSpreadType.LINEAR)));
    }

    private static ResourceKey<StructureSet> setKey(String name) {
        return ResourceKey.create(Registries.STRUCTURE_SET, HollowFrontier.id(name));
    }

    private static ResourceKey<Structure> structure(String name) {
        return ResourceKey.create(Registries.STRUCTURE, HollowFrontier.id(name));
    }

    private static ResourceKey<StructureTemplatePool> pool(String name) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, HollowFrontier.id(name));
    }
}
