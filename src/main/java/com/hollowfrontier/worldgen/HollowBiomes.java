package com.hollowfrontier.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModSounds;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.ListModifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * The Hollow Earth's biomes, all our own (none borrows a vanilla builder, so none carries vanilla's sky colour).
 * <ul>
 * <li><b>Primordial Jungle</b> (warm and wet): rolling green hills under tall jungle trees and ferns, rivers, a steamy green
 *     haze, giant tree ferns and cycads among the trees, bird calls and croaks (9b1).</li>
 * <li><b>Crystal Steppes</b> (temperate and dry): open golden grassland, nearly flat, wind-swept, with crystal outcrops
 *     standing out of the grass, glints in the air and the crystals chiming (9b1); horses graze here.</li>
 * <li><b>Ember Badlands</b> (hot and dry): stepped red mesas of banded terracotta and red sand, black basalt and magma
 *     patches, ash drifting in a rusty haze, lava falling from the crust; smoking ash vents in fields of ash, drifting
 *     cinders, rumbling (9b1).</li>
 * <li><b>Concavian Highlands</b> (cold, or high ground): pale calcite and stone uplands with grey-green grass, pale moss,
 *     and snow on the peaks; pale standing stones and fallen pillars of Kindled work, the wind (9b1; 12a brings the ruins).</li>
 * <li><b>The Still Sea</b> (43a; the lowest ground, whatever the weather): an inner sea with no wind and no weather, so it
 *     lies flat as glass. Pale sand shores, kelp and seagrass in the shallows, the crust's spires standing in it as sea
 *     stacks, and falls pouring into it from overhead.</li>
 * </ul>
 * The climate table ({@link #parameters}) cuts temperature, humidity and continentalness into those regions with no
 * gaps; {@link HollowTerrain} shapes the land from the same thresholds. Spawns are vanilla animals and each biome's own
 * small creature (9b2: fern skitters, glassback hares, ash salamanders, pale ravens); the biome tags {@code has_mammoths}
 * etc. (ModBiomeTagsProvider) are where phase 10a's megafauna will hook in.
 */
public final class HollowBiomes {
    private HollowBiomes() {}

    public static final ResourceKey<Biome> PRIMORDIAL_JUNGLE = key("primordial_jungle");
    public static final ResourceKey<Biome> CRYSTAL_STEPPES = key("crystal_steppes");
    public static final ResourceKey<Biome> EMBER_BADLANDS = key("ember_badlands");
    public static final ResourceKey<Biome> CONCAVIAN_HIGHLANDS = key("concavian_highlands");
    /** The Still Sea (43a): the inner sea, where the land lies lowest. */
    public static final ResourceKey<Biome> STILL_SEA = key("still_sea");
    /** The four land biomes, as they were before the sea (43a). */
    public static final List<ResourceKey<Biome>> LANDS = List.of(PRIMORDIAL_JUNGLE, CRYSTAL_STEPPES, EMBER_BADLANDS, CONCAVIAN_HIGHLANDS);
    public static final List<ResourceKey<Biome>> ALL = List.of(PRIMORDIAL_JUNGLE, CRYSTAL_STEPPES, EMBER_BADLANDS, CONCAVIAN_HIGHLANDS, STILL_SEA);

    // Hooks for later phases: 10a adds spawn biome modifiers on these (as ModSpawns does for rattlesnakes), 10b its labs
    public static final TagKey<Biome> IS_HOLLOW_EARTH = tag("is_hollow_earth");
    /** The Hollow Earth's dry land: where the sites that stand anywhere go (the Symmes camp, crash sites, sky shrines), never the sea. */
    public static final TagKey<Biome> IS_HOLLOW_LAND = tag("is_hollow_land");
    public static final TagKey<Biome> HAS_MAMMOTHS = tag("has_mammoths");
    public static final TagKey<Biome> HAS_TERROR_BIRDS = tag("has_terror_birds");
    public static final TagKey<Biome> HAS_MEGATHERIUM = tag("has_megatherium");
    public static final TagKey<Biome> HAS_GLYPTODONS = tag("has_glyptodons");
    public static final TagKey<Biome> HAS_GRAY_LABS = tag("has_gray_labs");

    private static TagKey<Biome> tag(String name) {
        return TagKey.create(Registries.BIOME, HollowFrontier.id(name));
    }

    // Climate thresholds, shared with HollowTerrain so the land suits the biome on it
    /** Continentalness above this is Highlands, whatever the weather. */
    public static final float HIGHLAND_CONTINENTS = 0.38F;
    /** Temperature below this is Highlands (cold uplands). */
    public static final float COLD = -0.45F;
    /** Temperature above this, if dry, is Badlands. */
    public static final float HOT = 0.12F;
    /**
     * Continentalness below this is the Still Sea (43a), whatever the weather. The land biomes keep everything above it
     * exactly as they had it, and {@link HollowTerrain} only presses the land down below it.
     */
    public static final float SEA_CONTINENTS = -0.30F;
    /** Humidity above this is Jungle (if not cold or high). */
    public static final float WET = 0.02F;

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, HollowFrontier.id(name));
    }

    public static Climate.ParameterList<Holder<Biome>> parameters(HolderGetter<Biome> biomes) {
        Climate.Parameter any = Climate.Parameter.span(-1.0F, 1.0F);
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> list = lands(biomes, SEA_CONTINENTS);
        list.add(entry(any, any, Climate.Parameter.span(-1.0F, SEA_CONTINENTS), biomes.getOrThrow(STILL_SEA)));
        return new Climate.ParameterList<>(list);
    }

    /** The table as it was before the Still Sea (43a), land all the way down: the tests hold today's land against it. */
    public static Climate.ParameterList<Holder<Biome>> parametersBeforeTheSea(HolderGetter<Biome> biomes) {
        return new Climate.ParameterList<>(lands(biomes, -1.0F));
    }

    /** The four land biomes' entries, with the low ground starting at {@code lowFrom} in continentalness. */
    private static List<Pair<Climate.ParameterPoint, Holder<Biome>>> lands(HolderGetter<Biome> biomes, float lowFrom) {
        Climate.Parameter any = Climate.Parameter.span(-1.0F, 1.0F);
        Climate.Parameter low = Climate.Parameter.span(lowFrom, HIGHLAND_CONTINENTS);
        Climate.Parameter high = Climate.Parameter.span(HIGHLAND_CONTINENTS, 1.0F);
        Climate.Parameter cold = Climate.Parameter.span(-1.0F, COLD);
        Climate.Parameter mild = Climate.Parameter.span(COLD, HOT);
        Climate.Parameter warm = Climate.Parameter.span(COLD, 1.0F);
        Climate.Parameter hot = Climate.Parameter.span(HOT, 1.0F);
        Climate.Parameter dry = Climate.Parameter.span(-1.0F, WET);
        Climate.Parameter wet = Climate.Parameter.span(WET, 1.0F);
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> list = new ArrayList<>();
        list.add(entry(any, any, high, biomes.getOrThrow(CONCAVIAN_HIGHLANDS)));
        list.add(entry(cold, any, low, biomes.getOrThrow(CONCAVIAN_HIGHLANDS)));
        list.add(entry(warm, wet, low, biomes.getOrThrow(PRIMORDIAL_JUNGLE)));
        list.add(entry(hot, dry, low, biomes.getOrThrow(EMBER_BADLANDS)));
        list.add(entry(mild, dry, low, biomes.getOrThrow(CRYSTAL_STEPPES)));
        return list;
    }

    private static Pair<Climate.ParameterPoint, Holder<Biome>> entry(Climate.Parameter temperature, Climate.Parameter humidity,
            Climate.Parameter continents, Holder<Biome> biome) {
        Climate.Parameter any = Climate.Parameter.span(-1.0F, 1.0F);
        return Pair.of(Climate.parameters(temperature, humidity, continents, any, Climate.Parameter.point(0.0F), any, 0.0F), biome);
    }

    // ---------------------------------------------------------------- the biomes

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<WorldCarver> carvers = context.lookup(Registries.CARVER);
        context.register(PRIMORDIAL_JUNGLE, jungle(placed, carvers));
        context.register(CRYSTAL_STEPPES, steppes(placed, carvers));
        context.register(EMBER_BADLANDS, badlands(placed, carvers));
        context.register(CONCAVIAN_HIGHLANDS, highlands(placed, carvers));
        context.register(STILL_SEA, sea(placed, carvers));
    }

    private static Biome jungle(HolderGetter<PlacedFeature> placed, HolderGetter<WorldCarver> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(EntityTypes.PARROT, 40, 1, 2);
        mobs.addSpawn(EntityTypes.FROG, 10, 2, 5);
        mobs.addSpawn(EntityTypes.PANDA, 1, 1, 2);
        mobs.addSpawn(ModEntities.FERN_SKITTER.get(), 10, 3, 6);
        BiomeDefaultFeatures.commonSpawns(mobs);
        BiomeGenerationSettings.Builder gen = common(placed, carvers);
        HollowFeatures.jungle(gen);
        gen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.VINES);
        return base(0.95F, 0.9F, 0x2E8C7A, 0x3E9D3A, 0x2F8A2A)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0xB4CC98))
                .setAttribute(EnvironmentAttributes.FOG_START_DISTANCE, 32.0F)
                .setAttribute(EnvironmentAttributes.FOG_END_DISTANCE, 360.0F)
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, ARGB.vector3fFromRGB24(0x1E5A46))
                .modifyAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, ListModifier.append(), AmbientParticle.of(ParticleTypes.SPORE_BLOSSOM_AIR, 0.006F))
                .setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, ambient(ModSounds.JUNGLE_LOOP, ModSounds.JUNGLE_AMBIENT, 0.006))
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(ModSounds.MUSIC_JUNGLE))
                .mobSpawnSettings(mobs.build())
                .generationSettings(gen.build())
                .build();
    }

    private static Biome steppes(HolderGetter<PlacedFeature> placed, HolderGetter<WorldCarver> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(EntityTypes.HORSE, 5, 2, 6);
        mobs.addSpawn(EntityTypes.DONKEY, 1, 1, 3);
        mobs.addSpawn(EntityTypes.RABBIT, 6, 2, 3);
        mobs.addSpawn(ModEntities.GLASSBACK_HARE.get(), 8, 1, 3);
        BiomeDefaultFeatures.commonSpawns(mobs);
        BiomeGenerationSettings.Builder gen = common(placed, carvers);
        HollowFeatures.steppes(gen);
        return base(0.8F, 0.3F, 0x4A9FB0, 0xB4B95A, 0x9DAA4E)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0xDCD49A))
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, ARGB.vector3fFromRGB24(0x2D6E7E))
                .modifyAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, ListModifier.append(), AmbientParticle.of(ParticleTypes.GLOW, 0.003F))
                .setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, ambient(ModSounds.STEPPES_LOOP, ModSounds.STEPPES_AMBIENT, 0.005))
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(ModSounds.MUSIC_STEPPES))
                .mobSpawnSettings(mobs.build())
                .generationSettings(gen.build())
                .build();
    }

    private static Biome badlands(HolderGetter<PlacedFeature> placed, HolderGetter<WorldCarver> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(EntityTypes.ARMADILLO, 6, 1, 2);
        mobs.addSpawn(ModEntities.ASH_SALAMANDER.get(), 8, 1, 3);
        BiomeDefaultFeatures.commonSpawns(mobs);
        BiomeGenerationSettings.Builder gen = common(placed, carvers);
        HollowFeatures.badlands(gen);
        return base(2.0F, 0.0F, 0x8A6A4A, 0x9E814D, 0x9E814D)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0xD2A27A))
                .setAttribute(EnvironmentAttributes.FOG_END_DISTANCE, 400.0F)
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, ARGB.vector3fFromRGB24(0x5A3E2A))
                .modifyAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, ListModifier.append(), List.of(
                        new AmbientParticle(ParticleTypes.WHITE_ASH, 0.02F), new AmbientParticle(ParticleTypes.CRIMSON_SPORE, 0.006F)))
                .setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, ambient(ModSounds.BADLANDS_LOOP, ModSounds.BADLANDS_AMBIENT, 0.008))
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(ModSounds.MUSIC_BADLANDS))
                .mobSpawnSettings(mobs.build())
                .generationSettings(gen.build())
                .build();
    }

    private static Biome highlands(HolderGetter<PlacedFeature> placed, HolderGetter<WorldCarver> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(EntityTypes.GOAT, 5, 1, 3);
        mobs.addSpawn(EntityTypes.SHEEP, 6, 2, 4);
        mobs.addSpawn(EntityTypes.RABBIT, 4, 2, 3);
        mobs.addSpawn(ModEntities.PALE_RAVEN.get(), 7, 2, 4);
        BiomeDefaultFeatures.commonSpawns(mobs);
        BiomeGenerationSettings.Builder gen = common(placed, carvers);
        HollowFeatures.highlands(gen);
        return base(0.3F, 0.5F, 0x6FC3D8, 0x8FB3A0, 0x7FA392)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0xD0DCD4))
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, ARGB.vector3fFromRGB24(0x3A7A8E))
                .modifyAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, ListModifier.append(), AmbientParticle.of(ParticleTypes.SNOWFLAKE, 0.003F))
                .setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, ambient(ModSounds.HIGHLANDS_LOOP, ModSounds.HIGHLANDS_AMBIENT, 0.004))
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(ModSounds.MUSIC_HIGHLANDS))
                .mobSpawnSettings(mobs.build())
                .generationSettings(gen.build())
                .build();
    }

    // The Still Sea (43a): vanilla fish and squid for now (43b brings its own creatures). No farm animals on its beaches,
    // no ambient particles (the air doesn't move), and a pale jade water that goes dark teal with depth.
    private static Biome sea(HolderGetter<PlacedFeature> placed, HolderGetter<WorldCarver> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(EntityTypes.COD, 12, 3, 6);
        mobs.addSpawn(EntityTypes.SALMON, 8, 2, 5);
        mobs.addSpawn(EntityTypes.TROPICAL_FISH, 10, 4, 8);
        mobs.addSpawn(EntityTypes.SQUID, 4, 1, 3);
        BiomeDefaultFeatures.commonSpawns(mobs);
        BiomeGenerationSettings.Builder gen = common(placed, carvers);
        HollowFeatures.sea(gen);
        return base(0.8F, 0.5F, 0x4FB8A8, 0x9CBF7A, 0x8DB26C)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0xCFE0CC))
                .setAttribute(EnvironmentAttributes.FOG_END_DISTANCE, 420.0F)
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, ARGB.vector3fFromRGB24(0x12404A))
                .setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, ambient(ModSounds.SEA_LOOP, ModSounds.SEA_AMBIENT, 0.004))
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(ModSounds.MUSIC_SEA))
                .mobSpawnSettings(mobs.build())
                .generationSettings(gen.build())
                .build();
    }

    /**
     * A biome's sounds: its bed, a loop that is always there (45b; the game cross-fades it at a border), its own one-shots
     * now and then ({@code chance} a tick, 9b1), and the cave mood the dimension has (setting the attribute replaces the
     * dimension's, so the mood is set again here).
     */
    private static AmbientSounds ambient(Holder<SoundEvent> loop, Holder<SoundEvent> additions, double chance) {
        return new AmbientSounds(Optional.of(loop), Optional.of(AmbientMoodSettings.LEGACY_CAVE_SETTINGS),
                List.of(new AmbientAdditionsSettings(additions, chance)));
    }

    // No precipitation (nothing rains under the crust), and no SKY_COLOR: the dimension's green-gold sky stays.
    private static Biome.BiomeBuilder base(float temperature, float downfall, int water, int grass, int foliage) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(temperature)
                .downfall(downfall)
                .specialEffects(new BiomeSpecialEffects.Builder().waterColor(water).grassColorOverride(grass).foliageColorOverride(foliage).build());
    }

    // Caves, ores and the underground everywhere; amethyst geodes hint at the crystals to come (phlogiston, 9c)
    private static BiomeGenerationSettings.Builder common(HolderGetter<PlacedFeature> placed, HolderGetter<WorldCarver> carvers) {
        BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(placed, carvers);
        gen.addCarver(Carvers.CAVE);
        gen.addCarver(Carvers.CAVE_EXTRA_UNDERGROUND);
        gen.addCarver(Carvers.CANYON);
        BiomeDefaultFeatures.addDefaultCrystalFormations(gen);
        BiomeDefaultFeatures.addDefaultMonsterRoom(gen);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(gen);
        BiomeDefaultFeatures.addDefaultOres(gen);
        return gen;
    }
}
