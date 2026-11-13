package com.hollowfrontier.worldgen;

import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/**
 * Aboard (50): the place the Grays' ships are while somebody taken is on one. A world with nothing in it: no ground, no
 * sky, no weather, no time passing, nothing that spawns. Each ship is set down in it by {@code Taken}, well apart from any
 * other, and cleared when its last captive is off. Its one biome carries what's heard aboard: the ship's hum as a bed that
 * never stops, and the ship's own piece of the score.
 */
public final class Aboard {
    private Aboard() {}

    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, HollowFrontier.id("aboard"));
    public static final ResourceKey<LevelStem> STEM = ResourceKey.create(Registries.LEVEL_STEM, HollowFrontier.id("aboard"));
    public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, HollowFrontier.id("aboard"));
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, HollowFrontier.id("aboard"));

    public static final int MIN_Y = 0, HEIGHT = 256;

    public static void bootstrapDimensionTypes(BootstrapContext<DimensionType> context) {
        EnvironmentAttributeMap attributes = EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(0x000000))
                .set(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0x04080A))
                .set(EnvironmentAttributes.STAR_BRIGHTNESS, 0.0F)
                // The ship's light is cold: lamps here cast blue-white, not a torch's orange, and what they don't reach is
                // a dim teal, never black
                .set(EnvironmentAttributes.BLOCK_LIGHT_TINT, ARGB.vector3fFromRGB24(0xC4ECFF))
                .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, ARGB.vector3fFromRGB24(0x16262C))
                .set(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, ARGB.vector4fFromARGB32(0))
                .set(EnvironmentAttributes.CLOUD_COLOR, ARGB.vector4fFromARGB32(0))
                .set(EnvironmentAttributes.BED_RULE, new BedRule(BedRule.Rule.NEVER, BedRule.Rule.NEVER, false, false, Optional.empty()))
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                .build();
        context.register(TYPE, new DimensionType(
                true,   // fixed time
                false,  // no sky light: the only light aboard is the ship's own
                false,
                false,
                1.0,
                MIN_Y,
                HEIGHT,
                HEIGHT,
                context.lookup(Registries.BLOCK).getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.0F,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.NONE,
                CardinalLighting.Type.DEFAULT,
                attributes,
                HolderSet.empty(),
                Optional.empty()));
    }

    public static Biome biome() {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0x3F76E4).build())
                .setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, new AmbientSounds(Optional.of(ModSounds.ABOARD_LOOP), Optional.empty(), List.of()))
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(new net.minecraft.sounds.Music(ModSounds.MUSIC_ABOARD, 200, 900, true)))
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }

    public static void bootstrapStems(BootstrapContext<LevelStem> context) {
        FlatLevelGeneratorSettings nothing = new FlatLevelGeneratorSettings(Optional.empty(), context.lookup(Registries.BIOME).getOrThrow(BIOME), List.of());
        context.register(STEM, new LevelStem(context.lookup(Registries.DIMENSION_TYPE).getOrThrow(TYPE), new FlatLevelSource(nothing)));
    }
}
