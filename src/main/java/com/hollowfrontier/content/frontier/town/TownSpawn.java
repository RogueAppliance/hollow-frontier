package com.hollowfrontier.content.frontier.town;

import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Starts a new world in a town: the world spawn goes on open ground in the town square nearest where the game would have put
 * it. With no town in reach it leaves the spawn to the game. A world only chooses a spawn once, so existing worlds keep theirs.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class TownSpawn {
    private TownSpawn() {}

    /** How far to look, in steps of the towns' 40-chunk grid: about 10,000 blocks, past the furthest town measured. A failed search costs the square of this. */
    static final int SEARCH_RINGS = 16;

    @SubscribeEvent
    static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (!TownConfig.spawnAtTown() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        long start = System.nanoTime();
        Optional<BlockPos> spot = find(level);
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (spot.isEmpty()) {
            HollowFrontier.LOGGER.info("No town in reach of the spawn after {} ms; leaving it to the game", ms);
            return;
        }
        HollowFrontier.LOGGER.info("World spawn set in the town square at {}, found in {} ms", spot.get(), ms);
        event.getSettings().setSpawn(LevelData.RespawnData.of(level.dimension(), spot.get(), 0.0F, 0.0F));
        // Left at 10, each arrival would drop on a random spot round it, roofs included
        level.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, level.getServer());
        event.setCanceled(true); // which also skips the game's own bonus chest
        if (level.getServer().getWorldGenSettings().options().generateBonusChest()) {
            level.registryAccess().lookup(Registries.FEATURE).flatMap(features -> features.get(MiscOverworldFeatures.BONUS_CHEST))
                    .ifPresent(chest -> chest.value().place(level, level.getChunkSource().getGenerator(), level.getRandom(), spot.get()));
        }
    }

    /** Where a new world should start: open ground in the nearest town's square, if there's a town in reach. */
    public static Optional<BlockPos> find(ServerLevel level) {
        var chunks = level.getChunkSource();
        BlockPos origin = chunks.getGenerator().getOrigin(chunks.randomState()).getMiddleBlockPosition(0);
        BlockPos town = level.findNearestMapStructure(ModStructures.TOWN_TAG, origin, SEARCH_RINGS, false);
        if (town == null) {
            return Optional.empty();
        }
        ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(town.getX()), SectionPos.blockToSectionCoord(town.getZ()), ChunkStatus.STRUCTURE_STARTS);
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        return ModStructures.TOWNS.stream().map(key -> chunk.getStartForStructure(structures.getValueOrThrow(key)))
                .filter(found -> found != null && found.isValid()).findFirst().map(TownSpawn::square)
                .map(square -> landing(level, square).orElse(middle(square))).filter(level.getWorldBorder()::isWithinBounds);
    }

    // The town square is the jigsaw start piece, as for TownSite
    private static BoundingBox square(StructureStart start) {
        return start.getPieces().getFirst().getBoundingBox();
    }

    private static BlockPos middle(BoundingBox square) {
        BlockPos center = square.getCenter();
        return new BlockPos(center.getX(), square.minY() + 1, center.getZ());
    }

    /**
     * The nearest spot to the square's middle with a clear yard around it: solid paving underfoot and nothing above it, so
     * it's out of doors, clear of the well or bandstand, the market stalls and the shade trees.
     */
    public static Optional<BlockPos> landing(BlockGetter level, BoundingBox square) {
        BlockPos middle = middle(square);
        for (BlockPos pos : BlockPos.spiralAround(middle, square.getXSpan() / 2, Direction.EAST, Direction.SOUTH)) {
            if (clearYard(level, pos, square.maxY())) {
                return Optional.of(pos.immutable());
            }
        }
        return Optional.empty();
    }

    private static boolean clearYard(BlockGetter level, BlockPos pos, int top) {
        for (BlockPos column : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            if (!clearColumn(level, column, top)) {
                return false;
            }
        }
        return true;
    }

    // Up to the top of the square, which every roof in it is under
    private static boolean clearColumn(BlockGetter level, BlockPos pos, int top) {
        BlockState ground = level.getBlockState(pos.below());
        if (ground.getCollisionShape(level, pos.below()).isEmpty() || !ground.getFluidState().isEmpty()) {
            return false;
        }
        BlockPos.MutableBlockPos above = pos.mutable();
        for (int y = pos.getY(); y <= top; y++) {
            BlockState state = level.getBlockState(above.setY(y));
            if (!state.getCollisionShape(level, above).isEmpty() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
