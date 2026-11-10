package com.hollowfrontier.worldgen;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModBlocks;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.material.VanillaMaterialConditions;
import net.minecraft.data.worldgen.material.VanillaMaterialRules;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

/**
 * What the Hollow Earth is made of (26.x "material rules", formerly surface rules), top to bottom of the list:
 * <ol>
 * <li>Bedrock on the floor, and <b>shell bedrock</b> in the top five layers (unbreakable, and it lets the light through).</li>
 * <li>Deepslate below y 0-8.</li>
 * <li>Each biome's ground: Jungle grass, podzol and moss with mud along the water; Steppe grass with coarse dirt; Badlands
 *     red sand over banded terracotta mesas, patched with basalt, blackstone and magma; Highland grass with pale moss, snow
 *     on the peaks, and calcite and diorite through the top thirty blocks of stone; the Still Sea's pale sand shores over
 *     sandstone, and gravel and clay on its deep bed.</li>
 * <li>Everything else above about y 140 is <b>crust rock</b>: the ceiling, its spires and the floating islands' cores (so
 *     none of them casts a shadow), under whatever soil a biome put on an island's top.</li>
 * </ol>
 */
public final class HollowMaterialRules {
    private HollowMaterialRules() {}

    public static final ResourceKey<MaterialRule> HOLLOW_EARTH = ResourceKey.create(Registries.MATERIAL_RULE, HollowFrontier.id("hollow_earth"));

    private static MaterialRule block(Block block) {
        return MaterialRules.state(block.defaultBlockState());
    }

    public static void bootstrap(BootstrapContext<MaterialRule> context) {
        HolderGetter<MaterialRule> rules = context.lookup(Registries.MATERIAL_RULE);
        HolderGetter<MaterialCondition> conditions = context.lookup(Registries.MATERIAL_CONDITION);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        MaterialCondition onFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.ON_FLOOR);
        MaterialCondition underFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.UNDER_FLOOR);
        MaterialCondition veryDeepUnderFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.VERY_DEEP_UNDER_FLOOR);
        MaterialCondition notUnderwater = MaterialRules.getCondition(conditions, VanillaMaterialConditions.NOT_UNDERWATER);
        MaterialCondition notUnderDeepWater = MaterialRules.getCondition(conditions, VanillaMaterialConditions.NOT_UNDER_DEEP_WATER);
        MaterialCondition steep = MaterialRules.steep();
        MaterialCondition byTheWater = MaterialRules.not(MaterialRules.yBlockCheck(VerticalAnchor.absolute(HollowEarth.SEA_LEVEL + 2), 0));
        MaterialCondition belowCrust = MaterialRules.not(MaterialRules.yBlockCheck(VerticalAnchor.absolute(HollowEarth.CRUST_ROCK_FROM), 0));

        MaterialRule grass = block(Blocks.GRASS_BLOCK);
        MaterialRule dirt = block(Blocks.DIRT);
        MaterialRule stone = block(Blocks.STONE);
        MaterialRule gravel = block(Blocks.GRAVEL);

        MaterialRule jungle = MaterialRules.sequence(
                MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                        MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(steep, MaterialRules.sequence(
                                        MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.PATCH, 0.0), block(Blocks.MOSS_BLOCK)), stone)),
                                MaterialRules.ifTrue(byTheWater, block(Blocks.MUD)),
                                MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.SURFACE, 0.3), block(Blocks.PODZOL)),
                                MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.PATCH, 0.45), block(Blocks.MOSS_BLOCK)),
                                grass)),
                        MaterialRules.ifTrue(notUnderDeepWater, block(Blocks.MUD)),
                        MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.GRAVEL, 0.0), gravel),
                        block(Blocks.CLAY))),
                MaterialRules.ifTrue(underFloor, MaterialRules.sequence(MaterialRules.ifTrue(byTheWater, block(Blocks.MUD)), dirt)));

        MaterialRule steppes = MaterialRules.sequence(
                MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                        MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(steep, MaterialRules.sequence(
                                        MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.SURFACE, 0.0), gravel), block(Blocks.ANDESITE))),
                                MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.SURFACE, 0.35), block(Blocks.COARSE_DIRT)),
                                MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.PATCH, 0.55), block(Blocks.ROOTED_DIRT)),
                                grass)),
                        gravel)),
                MaterialRules.ifTrue(underFloor, MaterialRules.sequence(MaterialRules.ifTrue(notUnderDeepWater, dirt), gravel)));

        MaterialRule badlands = MaterialRules.ifTrue(belowCrust, MaterialRules.sequence(
                MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                        MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(steep, MaterialRules.bandlands()),
                                MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.NETHER_STATE_SELECTOR, 0.45), block(Blocks.BASALT)),
                                MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.NETHERRACK, 0.62), block(Blocks.BLACKSTONE)),
                                MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.PATCH, 0.7), block(Blocks.MAGMA_BLOCK)),
                                block(Blocks.RED_SAND))),
                        block(Blocks.RED_SAND))),
                MaterialRules.ifTrue(underFloor, block(Blocks.RED_SANDSTONE)),
                // The mesas are striped through and through, as vanilla badlands are
                MaterialRules.ifTrue(MaterialRules.yBlockCheck(VerticalAnchor.absolute(HollowEarth.SEA_LEVEL - 6), 0), MaterialRules.bandlands())));

        MaterialCondition snowline = MaterialRules.yBlockCheck(VerticalAnchor.absolute(140), 2);
        MaterialCondition iceline = MaterialRules.yBlockCheck(VerticalAnchor.absolute(158), 2);
        MaterialRule paleStone = MaterialRules.sequence(
                MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.CALCITE, -0.08, 0.08), block(Blocks.CALCITE)),
                MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.SWAMP, -0.06, 0.06), block(Blocks.DIORITE)),
                stone);
        MaterialRule highlands = MaterialRules.ifTrue(MaterialRules.not(MaterialRules.yBlockCheck(VerticalAnchor.absolute(190), 0)), MaterialRules.sequence(
                MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                        MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(iceline, MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.PACKED_ICE, 0.0, 0.25), block(Blocks.PACKED_ICE))),
                                MaterialRules.ifTrue(snowline, block(Blocks.SNOW_BLOCK)),
                                MaterialRules.ifTrue(steep, MaterialRules.sequence(
                                        MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.CALCITE, -0.1, 0.1), block(Blocks.CALCITE)), stone)),
                                MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.PATCH, 0.35), block(Blocks.PALE_MOSS_BLOCK)),
                                MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.SURFACE, 0.4), block(Blocks.COARSE_DIRT)),
                                grass)),
                        gravel)),
                MaterialRules.ifTrue(underFloor, MaterialRules.sequence(MaterialRules.ifTrue(snowline, block(Blocks.SNOW_BLOCK)), dirt)),
                // Pale stone under the uplands; deeper down (and in the crust's spires) the usual rules carry on
                MaterialRules.ifTrue(veryDeepUnderFloor, paleStone)));

        // The Still Sea (43a): pale sand beaches and shallows, a darker bed of gravel and clay in deep water; rock where the
        // shore is steep, and grass on anything that stands well clear of the water (islets, a headland)
        MaterialCondition clearOfTheWater = MaterialRules.yBlockCheck(VerticalAnchor.absolute(HollowEarth.SEA_LEVEL + 6), 0);
        MaterialRule sand = block(Blocks.SAND);
        MaterialRule sea = MaterialRules.sequence(
                MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                        MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(steep, stone),
                                MaterialRules.ifTrue(clearOfTheWater, grass),
                                sand)),
                        MaterialRules.ifTrue(notUnderDeepWater, sand),
                        MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.GRAVEL, 0.0), gravel),
                        MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.PATCH, 0.5), block(Blocks.CLAY)),
                        sand)),
                MaterialRules.ifTrue(underFloor, MaterialRules.sequence(MaterialRules.ifTrue(clearOfTheWater, dirt), block(Blocks.SANDSTONE))));

        context.register(HOLLOW_EARTH, MaterialRules.sequence(
                MaterialRules.getRule(rules, VanillaMaterialRules.BEDROCK_FLOOR),
                MaterialRules.ifTrue(MaterialRules.not(MaterialRules.verticalGradient("hollowfrontier:shell_bedrock",
                        VerticalAnchor.belowTop(5), VerticalAnchor.top())), block(ModBlocks.SHELL_BEDROCK.get())),
                MaterialRules.ifTrue(MaterialRules.verticalGradient("hollowfrontier:deepslate", VerticalAnchor.absolute(0), VerticalAnchor.absolute(8)),
                        block(Blocks.DEEPSLATE)),
                MaterialRules.ifTrue(MaterialRules.isBiome(biomes, HollowBiomes.PRIMORDIAL_JUNGLE), jungle),
                MaterialRules.ifTrue(MaterialRules.isBiome(biomes, HollowBiomes.CRYSTAL_STEPPES), steppes),
                MaterialRules.ifTrue(MaterialRules.isBiome(biomes, HollowBiomes.EMBER_BADLANDS), badlands),
                MaterialRules.ifTrue(MaterialRules.isBiome(biomes, HollowBiomes.CONCAVIAN_HIGHLANDS), highlands),
                MaterialRules.ifTrue(MaterialRules.isBiome(biomes, HollowBiomes.STILL_SEA), sea),
                MaterialRules.ifTrue(MaterialRules.not(MaterialRules.verticalGradient("hollowfrontier:crust_rock",
                        VerticalAnchor.absolute(HollowEarth.CRUST_ROCK_FROM - 6), VerticalAnchor.absolute(HollowEarth.CRUST_ROCK_FROM + 6))),
                        block(ModBlocks.CRUST_ROCK.get()))));
    }
}
