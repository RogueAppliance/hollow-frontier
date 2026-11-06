package com.hollowfrontier.content.frontier.raid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.phys.Vec3;

/**
 * Where a raid plays out, read off the town when it starts: the town's key (its structure start chunk) and bounds,
 * the middle of the main street (where the gang pins down the street and the law forms up), the street end the gang
 * rides in by (it dismounts there) and the open ground past it where it gathers and rides out to, the chapel bell, and
 * the vault: the chests the gang is after (the bank's, or failing a bank the sheriff's strongbox), the block the
 * safecracker works on (the vault door, or the chest) and where he stands to do it.
 */
public record TownSite(long key, BoundingBox bounds, BlockPos rally, BlockPos edge, BlockPos approach, Optional<BlockPos> bell,
        List<BlockPos> vault, Optional<BlockPos> crackAt, Optional<BlockPos> workAt, int size) {
    public static final int BANK_VAULT = 2;
    public static final int STRONGBOX = 1;
    // How far past the town's edge the gang gathers before riding in.
    private static final int APPROACH_DISTANCE = 20;

    public static final Codec<TownSite> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("key").forGetter(TownSite::key),
            BoundingBox.CODEC.fieldOf("bounds").forGetter(TownSite::bounds),
            BlockPos.CODEC.fieldOf("rally").forGetter(TownSite::rally),
            BlockPos.CODEC.fieldOf("edge").forGetter(TownSite::edge),
            BlockPos.CODEC.fieldOf("approach").forGetter(TownSite::approach),
            BlockPos.CODEC.optionalFieldOf("bell").forGetter(TownSite::bell),
            BlockPos.CODEC.listOf().fieldOf("vault").forGetter(TownSite::vault),
            BlockPos.CODEC.optionalFieldOf("crack_at").forGetter(TownSite::crackAt),
            BlockPos.CODEC.optionalFieldOf("work_at").forGetter(TownSite::workAt),
            Codec.INT.fieldOf("size").forGetter(TownSite::size)).apply(i, TownSite::new));

    public boolean contains(BlockPos pos) {
        return bounds.isInside(pos);
    }

    public boolean hasVault() {
        return workAt.isPresent() && !vault.isEmpty();
    }

    /** The town structure start at pos, if pos is in a frontier town (of any style). */
    public static Optional<StructureStart> townAt(ServerLevel level, BlockPos pos) {
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (var key : ModStructures.TOWNS) {
            Optional<StructureStart> start = structures.get(key).map(town -> level.structureManager().getStructureAt(pos, town.value()))
                    .filter(StructureStart::isValid);
            if (start.isPresent()) {
                return start;
            }
        }
        return Optional.empty();
    }

    /** The key a town's raid state is saved under. */
    public static long keyOf(StructureStart start) {
        return start.getChunkPos().pack();
    }

    /** Reads the town around pos, if pos is in one. */
    public static Optional<TownSite> find(ServerLevel level, BlockPos pos, RandomSource random) {
        return townAt(level, pos).map(start -> of(level, start, random));
    }

    /**
     * Reads a town from its structure start: the town square is its jigsaw start piece (the rally point is its middle),
     * and the gang comes in down one of the streets off it, at random, dismounting near its far end.
     */
    public static TownSite of(ServerLevel level, StructureStart start, RandomSource random) {
        BoundingBox square = start.getPieces().getFirst().getBoundingBox();
        BlockPos center = square.getCenter();
        BlockPos rally = new BlockPos(center.getX(), square.minY() + 1, center.getZ());
        BoundingBox bounds = start.getBoundingBox();
        List<BoundingBox> streets = start.getPieces().stream()
                .filter(p -> p instanceof PoolElementStructurePiece piece && piece.getElement().toString().contains("street_"))
                .map(StructurePiece::getBoundingBox).toList();
        Direction toEnd;
        int toStreetEnd;
        if (streets.isEmpty()) {
            toEnd = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            toStreetEnd = 12;
        } else {
            BoundingBox street = streets.get(random.nextInt(streets.size()));
            BlockPos mid = street.getCenter();
            toEnd = Direction.getApproximateNearest(mid.getX() - rally.getX(), 0, mid.getZ() - rally.getZ());
            toStreetEnd = toEnd.getAxis() == Direction.Axis.X
                    ? (toEnd == Direction.EAST ? street.maxX() - rally.getX() : rally.getX() - street.minX())
                    : (toEnd == Direction.SOUTH ? street.maxZ() - rally.getZ() : rally.getZ() - street.minZ());
        }
        int toTownEdge = switch (toEnd) {
            case EAST -> bounds.maxX() - rally.getX();
            case WEST -> rally.getX() - bounds.minX();
            case SOUTH -> bounds.maxZ() - rally.getZ();
            default -> rally.getZ() - bounds.minZ();
        };
        BlockPos edge = ground(level, rally.relative(toEnd, Math.max(2, toStreetEnd - 3)));
        BlockPos approach = rally.relative(toEnd, toTownEdge + APPROACH_DISTANCE);
        return scan(level, keyOf(start), bounds, rally, edge, approach, start.getPieces().size());
    }

    // Streets follow the terrain, so points along them take the ground's height.
    private static BlockPos ground(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) ? level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos) : pos;
    }

    /**
     * Builds a site from known street points, finding the bell and vault among the loaded block entities in bounds
     * (used for real towns by find, and directly by game tests and the photo booth).
     */
    public static TownSite scan(ServerLevel level, long key, BoundingBox bounds, BlockPos rally, BlockPos edge, BlockPos approach, int size) {
        List<BlockPos> chests = new ArrayList<>();
        int bestKind = 0;
        Optional<BlockPos> bell = Optional.empty();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (!bounds.isInside(pos)) {
                        continue;
                    }
                    if (be instanceof BellBlockEntity && (bell.isEmpty() || pos.distSqr(rally) < bell.get().distSqr(rally))) {
                        bell = Optional.of(pos.immutable());
                    }
                    if (be instanceof ChestBlockEntity && be.hasData(ModAttachments.TOWN_VAULT.get())) {
                        int kind = be.getData(ModAttachments.TOWN_VAULT.get());
                        if (kind > bestKind) {
                            bestKind = kind;
                            chests.clear();
                        }
                        if (kind == bestKind) {
                            chests.add(pos.immutable());
                        }
                    }
                }
            }
        }
        if (chests.isEmpty()) {
            return new TownSite(key, bounds, rally, edge, approach, bell, List.of(), Optional.empty(), Optional.empty(), size);
        }
        // Two banks in one town: the gang goes for the one nearest the middle of the street.
        BlockPos first = chests.stream().min(Comparator.comparingDouble(p -> p.distSqr(rally))).orElseThrow();
        List<BlockPos> vault = chests.stream().filter(p -> p.closerThan(first, 8)).toList();
        // Only a bank has a vault door. A sheriff's strongbox stands by the jail, whose cell doors are iron too.
        Optional<BlockPos> door = bestKind == 2 ? vaultDoor(level, vault) : Optional.empty();
        BlockPos crackAt;
        BlockPos workAt;
        if (door.isPresent()) {
            // Stand on the door's far side from the chests (the bank lobby).
            crackAt = door.get();
            Direction.Axis axis = level.getBlockState(crackAt).getValue(DoorBlock.FACING).getAxis();
            Vec3 chestsAt = vault.stream().map(Vec3::atCenterOf).reduce(Vec3.ZERO, Vec3::add).scale(1.0 / vault.size());
            BlockPos a = crackAt.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
            BlockPos b = crackAt.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
            workAt = Vec3.atCenterOf(a).distanceToSqr(chestsAt) > Vec3.atCenterOf(b).distanceToSqr(chestsAt) ? a : b;
        } else {
            // A strongbox: work it from in front of the lid.
            crackAt = first;
            BlockState chest = level.getBlockState(first);
            workAt = chest.hasProperty(ChestBlock.FACING) ? first.relative(chest.getValue(ChestBlock.FACING)) : first.above();
        }
        return new TownSite(key, bounds, rally, edge, approach, bell, vault, Optional.of(crackAt), Optional.of(workAt), size);
    }

    // An iron door (its lower half) within a few blocks of the vault chests.
    private static Optional<BlockPos> vaultDoor(ServerLevel level, List<BlockPos> vault) {
        BlockPos best = null;
        for (BlockPos chest : vault) {
            for (BlockPos pos : BlockPos.betweenClosed(chest.offset(-3, -1, -3), chest.offset(3, 1, 3))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.IRON_DOOR) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                        && (best == null || pos.distSqr(chest) < best.distSqr(chest))) {
                    best = pos.immutable();
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
