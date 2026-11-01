package com.hollowfrontier.worldgen.structure;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;

import com.hollowfrontier.registry.ModFeatures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

/**
 * A frontier town (TownLayout), built only where the land suits a town: dry and near enough level across the ground it
 * spreads over. Frontier towns were laid out on flat ground by a river or a railroad, not over hills, and every piece of
 * the town is rigid (one level throughout, the terrain blend building the land up or cutting it back round the edges),
 * so a town on a hillside would stand on a cliff of fill. Before a town is grown the ground is read at its middle and at
 * sixteen points in two rings round it (26 and 52 out): none may be under water, and all must be within
 * {@code max_slope} of each other. Twelve more points 80 out (the lanes and fields) must be dry too. The town then stands at the middle reading of those (the median), so a dip or a
 * knoll at one point moves it least. If a spot fails, four more round the chunk are tried, as HollowSiteStructure does.
 */
public final class FrontierTownStructure extends Structure {
    public static final MapCodec<FrontierTownStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Codec.intRange(1, 20).fieldOf("size").forGetter(s -> s.size),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistance),
            Codec.intRange(0, 64).optionalFieldOf("max_slope", 8).forGetter(s -> s.maxSlope)).apply(i, FrontierTownStructure::new));
    /** The rings the ground is read on, and how many points round each. */
    static final int[] RINGS = {26, 52};
    static final int POINTS = 8;
    /** A third ring, further out, only for water: the town's edges mustn't run out over a lake. */
    static final int SHORE = 80;
    static final int SHORE_POINTS = 12;
    /** Where in and round its chunk a town tries to stand, in turn. */
    private static final int[][] TRIES = {{8, 8}, {40, 8}, {8, 40}, {-24, 8}, {8, -24}};

    private final Holder<StructureTemplatePool> startPool;
    private final int size;
    private final int maxDistance;
    private final int maxSlope;

    public FrontierTownStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size, int maxDistance, int maxSlope) {
        super(settings);
        this.startPool = startPool;
        this.size = size;
        this.maxDistance = maxDistance;
        this.maxSlope = maxSlope;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        for (int[] offset : TRIES) {
            int x = context.chunkPos().getMinBlockX() + offset[0], z = context.chunkPos().getMinBlockZ() + offset[1];
            OptionalInt ground = levelGround(context, x, z, maxSlope);
            if (ground.isEmpty()) {
                continue;
            }
            // Not projected to a heightmap: the start's floor goes at the height given (the first free block over the ground)
            Optional<GenerationStub> stub = JigsawPlacement.addPieces(context, startPool, Optional.empty(), size, new BlockPos(x, ground.getAsInt(), z), false,
                    Optional.empty(), new JigsawStructure.MaxDistance(maxDistance), PoolAliasLookup.EMPTY, JigsawStructure.DEFAULT_DIMENSION_PADDING,
                    JigsawStructure.DEFAULT_LIQUID_SETTINGS);
            if (stub.isPresent()) {
                return stub;
            }
        }
        return Optional.empty();
    }

    /**
     * The first free height over the ground round (x, z), the median of the readings, if none is water and they lie within
     * {@code maxSlope} of each other; otherwise empty.
     */
    public static OptionalInt levelGround(GenerationContext context, int x, int z, int maxSlope) {
        int[] heights = new int[1 + RINGS.length * POINTS];
        int n = 0;
        heights[n++] = dryHeight(context, x, z);
        for (int ring : RINGS) {
            for (int i = 0; i < POINTS; i++) {
                double a = Math.PI * 2 * i / POINTS;
                heights[n++] = dryHeight(context, x + (int) Math.round(Math.cos(a) * ring), z + (int) Math.round(Math.sin(a) * ring));
            }
        }
        Arrays.sort(heights);
        if (heights[0] == Integer.MIN_VALUE || heights[heights.length - 1] - heights[0] > maxSlope) {
            return OptionalInt.empty();
        }
        // The outskirts (lanes, fields, the depot) reach further: no lake there either, whatever its lie
        for (int i = 0; i < SHORE_POINTS; i++) {
            double a = Math.PI * 2 * (i + 0.5) / SHORE_POINTS;
            if (dryHeight(context, x + (int) Math.round(Math.cos(a) * SHORE), z + (int) Math.round(Math.sin(a) * SHORE)) == Integer.MIN_VALUE) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.of(heights[heights.length / 2]);
    }

    // The first free block over dry ground, or MIN_VALUE where there's water on top (the ocean floor lies lower).
    private static int dryHeight(GenerationContext context, int x, int z) {
        var generator = context.chunkGenerator();
        int surface = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        int floor = generator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        return floor < surface ? Integer.MIN_VALUE : surface;
    }

    @Override
    public StructureType<?> type() {
        return ModFeatures.FRONTIER_TOWN.get();
    }
}
