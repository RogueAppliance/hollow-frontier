package com.hollowfrontier.worldgen.structure;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import com.hollowfrontier.registry.ModFeatures;
import com.hollowfrontier.worldgen.HollowEarth;
import com.hollowfrontier.worldgen.HollowFeatures;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * A one-piece structure standing on the Hollow Earth's floor (9b3: the Kindled sites, the Gray field lab, the Symmes camp).
 * Vanilla's structures find the ground from a heightmap, whose top here is the crust, so they'd all be built on the roof
 * of the world. This one reads the bare noise column instead, the way the features' open-air scan does
 * ({@link HollowFeatures}): from just under the ceiling down through air to the first solid block.
 * <p>
 * It picks a piece from its pool (rotated at random), finds the ground at the piece's middle and its four corners, and
 * tries again a little way off (up to five spots) until it finds a place in its biomes where all five are dry land (not
 * a lake, not lava), none is the top of a floating island, and they lie within {@code max_slope} blocks of each other. The piece's bottom layer (its floor) is then set at their average, and
 * the terrain blend (BEARD_THIN, in the structure's settings) builds the land up under it and cuts it back round it.
 */
public final class HollowSiteStructure extends Structure {
    public static final MapCodec<HollowSiteStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Codec.intRange(0, 32).optionalFieldOf("max_slope", 3).forGetter(s -> s.maxSlope),
            Codec.intRange(1, 20).optionalFieldOf("size", 1).forGetter(s -> s.size),
            Codec.BOOL.optionalFieldOf("island", false).forGetter(s -> s.island),
            Codec.BOOL.optionalFieldOf("seabed", false).forGetter(s -> s.seabed),
            Codec.BOOL.optionalFieldOf("shore", false).forGetter(s -> s.shore)).apply(i, HollowSiteStructure::new));
    /** A shore site (43d) stands no higher than this over the sea, with open water within {@link #SHORE_REACH} blocks of it. */
    public static final int SHORE_MAX_HEIGHT = 4, SHORE_REACH = 10;
    /** A seabed site (43d) wants at least this much water over its floor, so it stands wholly under the Still Sea. */
    public static final int SEABED_MIN_DEPTH = 18;
    /** Ground above this is a floating island's top (they're flat a few blocks either side of 200), not the land. */
    public static final int HIGHEST_GROUND = 188;
    /** What {@link #ground} returns for a column with no dry land to build on. */
    public static final int NO_GROUND = Integer.MIN_VALUE;
    /** Where in and round its chunk a site tries to stand, in turn, until one spot has the ground (and the biome) for it. */
    private static final int[][] TRIES = {{0, 0}, {24, 0}, {0, 24}, {-24, 0}, {0, -24}};

    private final Holder<StructureTemplatePool> startPool;
    private final int maxSlope;
    /** How deep the jigsaw goes: 1 for a site alone, more for one with a dungeon under it (26). */
    private final int size;
    /** Whether it stands on a floating island's top (27: the Sky Shrine) rather than on the land. */
    private final boolean island;
    /** Whether it stands on the bed of the Still Sea (43d: the Drowned Quarter), under {@link #SEABED_MIN_DEPTH} of water or more. */
    private final boolean seabed;
    /** Whether it stands at the water's edge (43d: the Symmes Landing): on dry land just over sea level, with the sea beside it. */
    private final boolean shore;

    public HollowSiteStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxSlope) {
        this(settings, startPool, maxSlope, 1, false, false);
    }

    public HollowSiteStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxSlope, int size) {
        this(settings, startPool, maxSlope, size, false, false);
    }

    public HollowSiteStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxSlope, int size, boolean island) {
        this(settings, startPool, maxSlope, size, island, false);
    }

    public HollowSiteStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxSlope, int size, boolean island, boolean seabed) {
        this(settings, startPool, maxSlope, size, island, seabed, false);
    }

    public HollowSiteStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxSlope, int size, boolean island, boolean seabed,
            boolean shore) {
        super(settings);
        this.startPool = startPool;
        this.maxSlope = maxSlope;
        this.size = size;
        this.island = island;
        this.seabed = seabed;
        this.shore = shore;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // The big sites rarely find level ground at the first spot in hill country, so try a few round the chunk
        for (int[] offset : TRIES) {
            BlockPos origin = new BlockPos(context.chunkPos().getMinBlockX() + offset[0], 0, context.chunkPos().getMinBlockZ() + offset[1]);
            // Depth 1, not 0: at depth 0 vanilla never hands the start piece to the builder. A site alone has no
            // connectors; one with a dungeon (26) has one in its bottom layer, pointing down, and the rooms grow from it.
            Optional<GenerationStub> stub = JigsawPlacement.addPieces(context, startPool, Optional.empty(), size, origin, false, Optional.empty(),
                    new JigsawStructure.MaxDistance(64, 64), PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
            if (stub.isEmpty()) {
                return Optional.empty();
            }
            StructurePiecesBuilder pieces = stub.get().getPiecesBuilder();
            List<StructurePiece> all = pieces.build().pieces();
            // The site stands by its own piece, the first placed: the rooms under it don't count toward the ground
            BoundingBox box = all.getFirst().getBoundingBox();
            OptionalInt ground = groundUnder(context, box);
            if (ground.isEmpty()) {
                continue;
            }
            if (shore && !onTheShore(context, box, ground.getAsInt())) {
                continue;
            }
            int dy = ground.getAsInt() - box.minY();
            if (pieces.getBoundingBox().minY() + dy < HollowEarth.MIN_Y + 8) {
                continue; // the dungeon would run into the shell
            }
            pieces.offsetPiecesVertically(dy);
            if (all.size() > 1) {
                pieces = buried(context, pieces);
            }
            GenerationStub placed = new GenerationStub(new BlockPos(box.getCenter().getX(), ground.getAsInt() + 1, box.getCenter().getZ()), Either.right(pieces));
            if (context.isValidBiome(placed)) {
                return Optional.of(placed);
            }
        }
        return Optional.empty();
    }

    /** Whether ground at {@code y} under {@code box} is a shore: low over the sea, with water off one of the box's four sides. */
    private static boolean onTheShore(GenerationContext context, BoundingBox box, int y) {
        if (y < HollowEarth.SEA_LEVEL || y > HollowEarth.SEA_LEVEL + SHORE_MAX_HEIGHT) {
            return false;
        }
        int cx = box.getCenter().getX(), cz = box.getCenter().getZ();
        int[][] off = {{box.minX() - SHORE_REACH, cz}, {box.maxX() + SHORE_REACH, cz}, {cx, box.minZ() - SHORE_REACH}, {cx, box.maxZ() + SHORE_REACH}};
        for (int[] p : off) {
            NoiseColumn column = context.chunkGenerator().getBaseColumn(p[0], p[1], context.heightAccessor(), context.randomState());
            if (column.getBlock(HollowEarth.SEA_LEVEL - 1).getFluidState().is(net.minecraft.tags.FluidTags.WATER) && column.getBlock(HollowEarth.SEA_LEVEL + 1).isAir()) {
                return true;
            }
        }
        return false;
    }

    /** The same pieces, the site's own first and every room after it {@link BuriedPiece buried} (the land left alone round it). */
    private static StructurePiecesBuilder buried(GenerationContext context, StructurePiecesBuilder pieces) {
        StructurePiecesBuilder out = new StructurePiecesBuilder();
        List<StructurePiece> all = pieces.build().pieces();
        out.addPiece(all.getFirst());
        for (StructurePiece piece : all.subList(1, all.size())) {
            out.addPiece(piece instanceof PoolElementStructurePiece room ? new BuriedPiece(context.structureTemplateManager(), room) : piece);
        }
        return out;
    }

    /** The ground under the box's middle and corners on average, if all five are dry land within the slope allowed. */
    private OptionalInt groundUnder(GenerationContext context, BoundingBox box) {
        int[][] points = {{box.getCenter().getX(), box.getCenter().getZ()}, {box.minX(), box.minZ()}, {box.minX(), box.maxZ()},
                {box.maxX(), box.minZ()}, {box.maxX(), box.maxZ()}};
        int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE, sum = 0;
        for (int[] p : points) {
            NoiseColumn column = context.chunkGenerator().getBaseColumn(p[0], p[1], context.heightAccessor(), context.randomState());
            int y = seabed ? seabed(column) : island ? islandTop(column) : ground(column);
            if (y == NO_GROUND) {
                return OptionalInt.empty();
            }
            lowest = Math.min(lowest, y);
            highest = Math.max(highest, y);
            sum += y;
        }
        return highest - lowest <= maxSlope ? OptionalInt.of(Math.round(sum / (float) points.length)) : OptionalInt.empty();
    }

    /**
     * The top of the land in a bare noise column: the first solid block below the ceiling with air above it, or
     * {@link #NO_GROUND} if that's water or lava (a lake), a floating island, or nothing at all.
     */
    public static int ground(NoiseColumn column) {
        for (int y = HollowFeatures.SCAN_FROM; y > HollowEarth.MIN_Y; y--) {
            BlockState state = column.getBlock(y);
            if (state.isAir()) {
                continue;
            }
            return !state.getFluidState().isEmpty() || y > HIGHEST_GROUND ? NO_GROUND : y;
        }
        return NO_GROUND;
    }

    /**
     * The bed of the Still Sea in a bare noise column: the first solid block under the water, if there's water at the
     * surface here and at least {@link #SEABED_MIN_DEPTH} of it; {@link #NO_GROUND} otherwise (land, shallows, a sea stack).
     */
    public static int seabed(NoiseColumn column) {
        if (!column.getBlock(HollowEarth.SEA_LEVEL - 1).getFluidState().is(net.minecraft.tags.FluidTags.WATER) || !column.getBlock(HollowEarth.SEA_LEVEL + 1).isAir()) {
            return NO_GROUND;
        }
        for (int y = HollowEarth.SEA_LEVEL - 2; y > HollowEarth.MIN_Y; y--) {
            BlockState state = column.getBlock(y);
            if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                continue;
            }
            return state.isSolid() && HollowEarth.SEA_LEVEL - 1 - y >= SEABED_MIN_DEPTH ? y : NO_GROUND;
        }
        return NO_GROUND;
    }

    /** The top of a floating island in a bare noise column, or {@link #NO_GROUND} if the column has none (only land, or water). */
    public static int islandTop(NoiseColumn column) {
        for (int y = HollowFeatures.SCAN_FROM; y > HIGHEST_GROUND; y--) {
            BlockState state = column.getBlock(y);
            if (state.isAir()) {
                continue;
            }
            return state.getFluidState().isEmpty() ? y : NO_GROUND;
        }
        return NO_GROUND;
    }

    @Override
    public StructureType<?> type() {
        return ModFeatures.HOLLOW_SITE.get();
    }
}
