package com.hollowfrontier.gametest;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.gateway.GatewayEffects;
import com.hollowfrontier.content.hollow.gateway.HollowLanding;
import com.hollowfrontier.content.hollow.gateway.PolarGateways;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.HollowEarth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import com.mojang.logging.LogUtils;

/**
 * Phase 9a: the Hollow Earth's own world. The game test server loads no datapack dimensions, so these build the
 * dimension's generator from its registry entries and sample it directly, for several seeds: biomes from the climate,
 * and bare noise columns (terrain without surface blocks or features) for the land, the crust and landing.
 */
final class HollowEarthTests {
    private HollowEarthTests() {}

    static final long[] SEEDS = {1L, 42L, 9_000_001L, -77_777L, 20261028L};

    /** The dimension's generator, built from the registries as the level stem builds it. */
    record World(NoiseBasedChunkGenerator generator, Holder<NoiseGeneratorSettings> settings, HolderGetter<NormalNoise> noises) {
        RandomState random(long seed) {
            return RandomState.create(noises, seed, settings.value());
        }
    }

    static World world(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        Holder<NoiseGeneratorSettings> settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(HollowEarth.NOISE_SETTINGS);
        var source = MultiNoiseBiomeSource.createFromList(HollowBiomes.parameters(access.lookupOrThrow(Registries.BIOME)));
        return new World(new NoiseBasedChunkGenerator(source, settings), settings, access.lookupOrThrow(Registries.NOISE));
    }

    /** The bare noise terrain of one seed, column by column, as HollowLanding reads it. */
    static HollowLanding.Terrain terrain(World world, RandomState random) {
        LevelHeightAccessor height = LevelHeightAccessor.create(HollowEarth.MIN_Y, HollowEarth.HEIGHT);
        Map<Long, NoiseColumn> columns = new HashMap<>();
        return new HollowLanding.Terrain() {
            @Override
            public BlockState get(int x, int y, int z) {
                return columns.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> world.generator().getBaseColumn(x, z, height, random)).getBlock(y);
            }

            @Override
            public int minY() {
                return HollowEarth.MIN_Y;
            }

            @Override
            public int maxY() {
                return HollowEarth.TOP_Y;
            }
        };
    }

    /** Its registry entries: dimension type, noise settings, the biomes, and none of them borrowed. */
    static void hollowEarthRegistered(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        DimensionType type = access.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(HollowEarth.TYPE).value();
        h.assertTrue(type.hasSkyLight() && type.hasFixedTime(), "the Hollow Earth should be always lit, with fixed time");
        h.assertTrue(type.minY() == HollowEarth.MIN_Y && type.height() == HollowEarth.HEIGHT, "wrong height: " + type.minY() + " +" + type.height());
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(HollowEarth.NOISE_SETTINGS).value();
        h.assertTrue(settings.seaLevel() == HollowEarth.SEA_LEVEL, "sea level " + settings.seaLevel());
        for (ResourceKey<Biome> biome : HollowBiomes.ALL) {
            h.assertTrue(access.lookupOrThrow(Registries.BIOME).get(biome).isPresent(), "missing biome " + biome.identifier());
        }
        var source = world(h).generator().getBiomeSource();
        h.assertTrue(source.possibleBiomes().size() == HollowBiomes.ALL.size()
                && source.possibleBiomes().stream().allMatch(b -> HollowBiomes.ALL.stream().anyMatch(b::is)), "the biome source should offer exactly the four biomes");
        h.succeed();
    }

    /** No Hollow Earth biome sets a sky colour (phase 8's blue sky came from one that did), and the dimension sets its own. */
    static void skyIsNotOverridden(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        DimensionType type = access.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(HollowEarth.TYPE).value();
        h.assertTrue(type.attributes().contains(EnvironmentAttributes.SKY_COLOR), "the dimension should own the sky colour");
        for (ResourceKey<Biome> key : HollowBiomes.ALL) {
            Biome biome = access.lookupOrThrow(Registries.BIOME).getOrThrow(key).value();
            h.assertFalse(biome.getAttributes().contains(EnvironmentAttributes.SKY_COLOR), key.identifier() + " overrides the sky colour");
            h.assertFalse(biome.hasPrecipitation(), key.identifier() + " shouldn't rain under the crust");
        }
        h.succeed();
    }

    /** Every biome turns up, in a fair share, for every seed (a 40 x 40 grid 96 blocks apart: ~3.8 km square). */
    static void everyBiomeAppears(GameTestHelper h) {
        World world = world(h);
        Map<ResourceKey<Biome>, Integer> total = new HashMap<>();
        for (long seed : SEEDS) {
            BiomeResolver biomes = world.generator().getBiomeSource().createUncachedResolver(world.random(seed));
            Map<ResourceKey<Biome>, Integer> counts = new HashMap<>();
            int samples = 0;
            for (int i = -20; i < 20; i++) {
                for (int j = -20; j < 20; j++) {
                    Holder<Biome> biome = biomes.getNoiseBiome((i * 96) >> 2, 80 >> 2, (j * 96) >> 2);
                    counts.merge(biome.unwrapKey().orElseThrow(), 1, Integer::sum);
                    samples++;
                }
            }
            for (ResourceKey<Biome> key : HollowBiomes.ALL) {
                double share = counts.getOrDefault(key, 0) / (double) samples;
                h.assertTrue(share >= 0.05, "seed " + seed + ": " + key.identifier().getPath() + " covers only " + Math.round(share * 100) + "%: " + counts);
                total.merge(key, counts.getOrDefault(key, 0), Integer::sum);
            }
        }
        LogUtils.getLogger().info("[{}] Hollow Earth biome shares over {} seeds: {}", HollowFrontier.MODID, SEEDS.length, total);
        h.succeed();
    }

    /**
     * The shape, from bare noise: across seeds, nearly every column has land (or water) under a roof of crust with plenty
     * of open air between, the shell is solid at the top, and nothing generates on the roof.
     */
    static void landAndCrustTakeShape(GameTestHelper h) {
        World world = world(h);
        int columns = 0;
        int open = 0;
        long gaps = 0;
        int islands = 0;
        for (long seed : SEEDS) {
            HollowLanding.Terrain terrain = terrain(world, world.random(seed));
            for (int i = 0; i < 40; i++) {
                int x = (int) ((i * 2654435761L + seed) % 6000) - 3000;
                int z = (int) ((i * 40503L + seed * 7) % 6000) - 3000;
                columns++;
                HollowLanding.Column column = HollowLanding.column(terrain, x, z);
                h.assertTrue(!terrain.get(x, HollowEarth.TOP_Y - 2, z).isAir(), "seed " + seed + " " + x + "," + z + ": the shell is open at the top");
                if (column.kind() == HollowLanding.Kind.NONE) {
                    continue; // a spire or pillar standing on the land here, all the way up
                }
                int ceiling = HollowLanding.ceilingAbove(terrain, new BlockPos(x, column.floorY(), z)).orElse(Integer.MAX_VALUE);
                h.assertTrue(ceiling != Integer.MAX_VALUE, "seed " + seed + " " + x + "," + z + ": land with no crust over it");
                open++;
                gaps += ceiling - column.floorY();
                for (int y = column.floorY() + HollowLanding.HEADROOM; y < ceiling; y++) {
                    if (!terrain.get(x, y, z).isAir()) {
                        islands++;
                        break;
                    }
                }
            }
        }
        double average = gaps / (double) Math.max(1, open);
        LogUtils.getLogger().info("[{}] Hollow Earth columns: {} of {} open to the crust, average open air {} blocks", HollowFrontier.MODID, open, columns, Math.round(average));
        h.assertTrue(open >= columns * 0.9, "only " + open + " of " + columns + " columns have land under open air");
        h.assertTrue(average >= 100, "the crust should stand well clear of the land: " + Math.round(average) + " blocks on average");
        h.succeed();
    }

    /**
     * landing() across many seeds and spots: always on solid, dry, level ground (or water at worst) under the crust, never
     * on top of it, never in lava, never inside a floating island, with headroom over it.
     */
    static void landingIsSafe(GameTestHelper h) {
        World world = world(h);
        Map<HollowLanding.Kind, Integer> kinds = new EnumMap<>(HollowLanding.Kind.class);
        int level = 0;
        int tries = 0;
        for (long seed : SEEDS) {
            HollowLanding.Terrain terrain = terrain(world, world.random(seed));
            for (int i = 0; i < 12; i++) {
                int x = (int) ((i * 7919L + seed * 31) % 8000) - 4000;
                int z = (int) ((i * 104729L - seed) % 8000) - 4000;
                HollowLanding.Landing landing = HollowLanding.find(terrain, x, z);
                tries++;
                kinds.merge(landing.kind(), 1, Integer::sum);
                BlockPos pos = landing.pos();
                String where = "seed " + seed + " near " + x + "," + z + " -> " + pos.toShortString() + " " + landing.kind();
                h.assertTrue(landing.kind() == HollowLanding.Kind.LAND || landing.kind() == HollowLanding.Kind.WATER, where + ": nowhere to land");
                h.assertTrue(pos.getY() < HollowLanding.ISLAND_FLOOR && pos.getY() < HollowEarth.CEILING_LOWEST - HollowLanding.HEADROOM, where + ": too high (the crust or an island)");
                BlockState ground = terrain.get(pos.getX(), pos.getY(), pos.getZ());
                h.assertFalse(ground.is(Blocks.LAVA), where + ": in lava");
                if (landing.kind() == HollowLanding.Kind.LAND) {
                    h.assertTrue(ground.isSolid(), where + ": not on solid ground: " + ground);
                }
                for (int dy = 1; dy <= HollowLanding.HEADROOM; dy++) {
                    h.assertTrue(terrain.get(pos.getX(), pos.getY() + dy, pos.getZ()).isAir(), where + ": no headroom at +" + dy);
                }
                h.assertTrue(HollowLanding.ceilingAbove(terrain, pos).isPresent(), where + ": no crust over it");
                if (landing.level()) {
                    level++;
                }
            }
        }
        LogUtils.getLogger().info("[{}] Hollow Earth landings: {} ({} of {} level)", HollowFrontier.MODID, kinds, level, tries);
        h.assertTrue(level >= tries * 0.8, "only " + level + " of " + tries + " landings were on level ground");
        h.succeed();
    }

    /** A canopy isn't a floor, and lava rules a spot out: a made-up column of ground, tree, lava and crust. */
    static void landingSkipsTreesAndLava(GameTestHelper h) {
        HollowLanding.Terrain terrain = new HollowLanding.Terrain() {
            @Override
            public BlockState get(int x, int y, int z) {
                if (y >= 220) {
                    return ModBlocks.CRUST_ROCK.get().defaultBlockState();
                }
                if (x < 8 && y == 70) {
                    return Blocks.LAVA.defaultBlockState(); // a lava lake to the west
                }
                if (y <= 70) {
                    return Blocks.STONE.defaultBlockState();
                }
                if (x == 12 && z == 0 && y <= 78) {
                    return Blocks.JUNGLE_LOG.defaultBlockState(); // a tree right where we asked
                }
                if (Math.abs(x - 12) <= 2 && Math.abs(z) <= 2 && y >= 76 && y <= 80) {
                    return Blocks.JUNGLE_LEAVES.defaultBlockState();
                }
                return Blocks.AIR.defaultBlockState();
            }

            @Override
            public int minY() {
                return 0;
            }

            @Override
            public int maxY() {
                return 255;
            }
        };
        HollowLanding.Landing under = HollowLanding.find(terrain, 12, 0);
        h.assertTrue(under.kind() == HollowLanding.Kind.LAND && under.pos().getY() == 70, "the tree's canopy was taken for the ground: " + under);
        HollowLanding.Landing lava = HollowLanding.find(terrain, 0, 0);
        h.assertTrue(lava.kind() == HollowLanding.Kind.LAND && lava.pos().getX() >= 8 + 3, "landed in or beside the lava: " + lava);
        h.succeed();
    }

    /** Building a return gateway under a tree clears the tree off it. */
    static void returnGatewayClearsTrees(GameTestHelper h) {
        BlockPos center = new BlockPos(6, 1, 6);
        for (int y = 2; y <= 5; y++) {
            h.setBlock(center.offset(1, y - 1, 0), Blocks.OAK_LOG);
        }
        h.setBlock(center.offset(-2, 4, 1), Blocks.OAK_LEAVES);
        PolarGateways.buildReturnGateway(h.getLevel(), h.absolutePos(center));
        h.assertTrue(PolarGateways.isRing(h.getLevel(), h.absolutePos(center)), "no ring built");
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = 1; dy <= 4; dy++) {
                    BlockState state = h.getBlockState(center.offset(dx, dy, dz));
                    h.assertFalse(state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES), "tree left over the gateway at " + dx + "," + dy + "," + dz);
                }
            }
        }
        h.succeed();
    }

    /** Crust rock lets sky light through (the land under the ceiling depends on it); shell bedrock too. */
    static void crustLetsLightThrough(GameTestHelper h) {
        h.assertTrue(ModBlocks.CRUST_ROCK.get().defaultBlockState().getLightDampening() == 0, "crust rock blocks light");
        h.assertTrue(ModBlocks.CRUST_ROCK.get().defaultBlockState().propagatesSkylightDown(), "crust rock stops sky light");
        h.assertTrue(ModBlocks.SHELL_BEDROCK.get().defaultBlockState().getLightDampening() == 0, "shell bedrock blocks light");
        h.assertTrue(ModBlocks.SHELL_BEDROCK.get().defaultBlockState().getDestroySpeed(h.getLevel(), BlockPos.ZERO) < 0, "shell bedrock should be unbreakable");
        h.succeed();
    }

    /** A player on the way down from a crossing can't be hurt by the fall, however hard they hit. */
    static void crossingFallCantKill(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        GatewayEffects.descend(player, true, new GatewayEffects.Fall(player.position(), player.getBlockY() + 40), false);
        float health = player.getHealth();
        player.causeFallDamage(250.0, 1.0F, player.damageSources().fall());
        h.assertTrue(player.getHealth() == health && player.isAlive(), "the fall hurt: " + health + " -> " + player.getHealth());
        player.setOnGround(true); // fake players don't move themselves: stand it on the ground, and the descent should end
        h.succeedWhen(() -> h.assertFalse(GatewayEffects.isDescending(player), "the descent never ended on the ground"));
    }
}
