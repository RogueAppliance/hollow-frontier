package com.hollowfrontier.worldgen;

import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModBlocks;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;

/**
 * The Hollow Earth (phase 9a): a world of its own under a thick crust ceiling, lit by an inner sun that never moves.
 * <ul>
 * <li><b>Layout</b> (y): bedrock floor at -64, stone and deepslate, the land mostly 64-110 with the Highlands up to about
 *     180, a sea level of 63 for lakes and rivers, open air above, then the <b>crust</b> from about 236 (it sways 12 either
 *     way, and spires hang far below it) up to 314, capped by unbreakable shell bedrock to 319. See {@link HollowTerrain}.</li>
 * <li><b>Light:</b> the crust is {@code crust_rock}, which lets sky light straight through (CrustRockBlock), so the land
 *     under it is in full daylight as if the sky were open. The sun itself is drawn under the ceiling by the client
 *     (client/InnerSun), because a sky-box sun would be hidden behind the rock.</li>
 * <li><b>Time</b> is fixed: always noon light, no day or night. Beds can't be slept in (it's never dark) but still set
 *     your spawn. Clocks and ordinary compasses spin, as in the Nether; the Polar Compass points to the nearest return
 *     gateway. Nothing rains under the crust (no biome here has precipitation).</li>
 * <li><b>Sky:</b> the dimension owns the green-gold sky colour; none of our biomes sets {@code SKY_COLOR}, so it can never
 *     be overridden with vanilla blue (phase 8's bug). Biomes only tint the fog.</li>
 * </ul>
 * Mobs: hostile ones spawn only in the dark (caves, under thick canopies), since the open land is always at light 15.
 * The dimension type has a ceiling (the crust), so chunk-generation spawning looks down past it for the ground (9b2).
 */
public final class HollowEarth {
    private HollowEarth() {}

    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, HollowFrontier.id("hollow_earth"));
    public static final ResourceKey<LevelStem> STEM = ResourceKey.create(Registries.LEVEL_STEM, HollowFrontier.id("hollow_earth"));
    public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, HollowFrontier.id("hollow_earth"));
    public static final ResourceKey<NoiseGeneratorSettings> NOISE_SETTINGS = ResourceKey.create(Registries.NOISE_SETTINGS, HollowFrontier.id("hollow_earth"));

    public static final ResourceKey<Biome> PRIMORDIAL_JUNGLE = HollowBiomes.PRIMORDIAL_JUNGLE;

    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    /** The highest block (the last layer of shell bedrock). */
    public static final int TOP_Y = MIN_Y + HEIGHT - 1;
    public static final int SEA_LEVEL = 63;
    /** The crust's underside on average; it sways {@link #CEILING_SWAY} either way, and spires hang below it. */
    public static final int CEILING_BASE = 236;
    public static final int CEILING_SWAY = 12;
    /** The lowest the underside can be, spires aside. Air under it is open sky as far as the client is concerned. */
    public static final int CEILING_LOWEST = CEILING_BASE - CEILING_SWAY;
    /** Above this, plain stone is crust rock (with a ragged join a few blocks either way). */
    public static final int CRUST_ROCK_FROM = 140;
    /** Shell bedrock fills the top five layers, as vanilla bedrock does a Nether roof. */
    public static final int SHELL_FROM = TOP_Y - 4;
    /** The inner sun's colour, which also tints the fog and the sky at the horizon. */
    public static final int SKY = 0xA8C890;
    public static final int FOG = 0xC9CF96;

    public static void bootstrapDimensionTypes(BootstrapContext<DimensionType> context) {
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
        EnvironmentAttributeMap attributes = EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(SKY))
                .set(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(FOG))
                // A golden haze: the ground fades out in the distance and the crust overhead shows through it, softened
                .set(EnvironmentAttributes.FOG_START_DISTANCE, 48.0F)
                .set(EnvironmentAttributes.FOG_END_DISTANCE, 480.0F)
                .set(EnvironmentAttributes.SKY_FOG_END_DISTANCE, 200.0F)
                // The vanilla sky box only shows at the horizon, between the land and the crust: no sunset, no moon, no stars
                .set(EnvironmentAttributes.SUN_ANGLE, 0.0F)
                .set(EnvironmentAttributes.MOON_ANGLE, 180.0F)
                .set(EnvironmentAttributes.STAR_BRIGHTNESS, 0.0F)
                .set(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, ARGB.vector4fFromARGB32(0))
                .set(EnvironmentAttributes.CLOUD_COLOR, ARGB.vector4fFromARGB32(ARGB.color(0, 0xE8E0B0)))
                .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, ARGB.vector3fFromRGB24(0x141810))
                .set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
                // The inner sun never sets: nobody sleeps here, but a bed still sets your spawn
                .set(EnvironmentAttributes.BED_RULE, new BedRule(BedRule.Rule.NEVER, BedRule.Rule.ALWAYS, false, false,
                        Optional.of(Component.translatable("block.hollowfrontier.bed.no_sleep_hollow"))))
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                // Lava runs as in the Nether, so the Badlands' lavafalls reach the ground in a minute rather than three
                .set(EnvironmentAttributes.FAST_LAVA, true)
                .build();
        context.register(TYPE, new DimensionType(
                true,   // fixed time
                true,   // sky light (it comes down through the crust: crust rock doesn't block it)
                true,   // a ceiling (the crust): chunk-generation spawning looks down past it for the ground (9b2). It doesn't
                        // touch the sky light, which is its own flag; it also rules out weather and draws maps the Nether way.
                false,
                1.0,    // 1:1 with the Overworld, so a gateway leads to the same x/z inside
                MIN_Y,
                HEIGHT,
                HEIGHT,
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.1F,
                new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0),
                DimensionType.Skybox.OVERWORLD,
                CardinalLighting.Type.DEFAULT,
                attributes,
                HolderSet.empty(),
                Optional.empty()));
    }

    public static void bootstrapNoiseSettings(BootstrapContext<NoiseGeneratorSettings> context) {
        context.register(NOISE_SETTINGS, new NoiseGeneratorSettings(
                NoiseSettings.create(MIN_Y, HEIGHT),
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                HollowTerrain.router(context.lookup(Registries.DENSITY_FUNCTION)),
                context.lookup(Registries.MATERIAL_RULE).getOrThrow(HollowMaterialRules.HOLLOW_EARTH),
                List.of(),
                SEA_LEVEL,
                false,
                Optional.of(HollowTerrain.aquifers(context.lookup(Registries.DENSITY_FUNCTION), context.lookup(Registries.NOISE))),
                false,
                NoiseGeneratorSettings.DebugFunctions.EMPTY));
    }

    public static void bootstrapStems(BootstrapContext<LevelStem> context) {
        context.register(STEM, new LevelStem(context.lookup(Registries.DIMENSION_TYPE).getOrThrow(TYPE),
                new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromList(HollowBiomes.parameters(context.lookup(Registries.BIOME))),
                        context.lookup(Registries.NOISE_SETTINGS).getOrThrow(NOISE_SETTINGS))));
    }

    /** Whether a block is part of the crust overhead (what the inner sun hangs beneath, and what landing looks under). */
    public static boolean isCrust(BlockState state) {
        return state.is(ModBlocks.CRUST_ROCK.get()) || state.is(ModBlocks.SHELL_BEDROCK.get());
    }
}
