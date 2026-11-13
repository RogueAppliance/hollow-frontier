package com.hollowfrontier.content.frontier.stake;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * The next town along from a town (47): where a warrant or a herd goes. First among the towns the world already knows
 * (a board looked at, a raid, a claim), then by asking the map, a few places a second while a player is in the town
 * ({@link #scout}). The answer is kept (StakeData); "none in reach" is kept for the session.
 */
public final class Neighbours {
    private Neighbours() {}

    private static final int SPOTS_A_LOOK = 3;
    private static final Map<Long, Search> SEARCHES = new HashMap<>();
    /** Towns with no neighbour in reach, as found this session. */
    private static final Set<Long> NONE = new HashSet<>();
    private static long world;

    public record Neighbour(long key, String name, BlockPos pos) {
        public static final Codec<Neighbour> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("key").forGetter(Neighbour::key),
                Codec.STRING.fieldOf("name").forGetter(Neighbour::name),
                BlockPos.CODEC.fieldOf("pos").forGetter(Neighbour::pos)).apply(i, Neighbour::new));
    }

    /** The next town along, if it's known yet: kept from before, or the nearest town the world already knows of. */
    public static Optional<Neighbour> of(ServerLevel anyLevel, long town) {
        ServerLevel level = anyLevel.getServer().overworld();
        StakeData data = StakeData.get(level);
        Optional<Neighbour> kept = data.neighbour(town);
        if (kept.isPresent()) {
            return kept;
        }
        BlockPos here = Towns.middle(town);
        int reach = StakeConfig.warrantReach();
        Set<Long> known = new HashSet<>(BountyBoards.get(level).towns());
        known.addAll(TownRaids.get(level).knownTowns());
        data.all().forEach(claim -> known.add(claim.town()));
        known.remove(town);
        Long best = known.stream().filter(k -> StakeText.paces(here, Towns.middle(k)) <= reach)
                .min((a, b) -> Integer.compare(StakeText.paces(here, Towns.middle(a)), StakeText.paces(here, Towns.middle(b)))).orElse(null);
        if (best == null) {
            return Optional.empty();
        }
        Neighbour next = new Neighbour(best, Towns.name(level, best), Towns.middle(best));
        data.neighbour(town, next);
        return Optional.of(next);
    }

    /**
     * Looks a little further for {@code town}'s neighbour: called once a second while a player is in the town, it asks
     * the map about three more places a town could stand, nearest first, until one has a town or they've all been asked.
     * (Asked all at once, as it first was, the game stood still for half a second and found nothing: on a played world
     * the nearest town was 3018 blocks off, the 58th place to look.)
     */
    public static void scout(ServerLevel anyLevel, long town) {
        ServerLevel level = anyLevel.getServer().overworld();
        if (level.getSeed() != world) {
            world = level.getSeed();
            SEARCHES.clear();
            NONE.clear();
        }
        if (NONE.contains(town) || of(level, town).isPresent()) {
            return;
        }
        Search search = SEARCHES.computeIfAbsent(town, key -> new Search(spots(level, town, StakeConfig.warrantReach())));
        BlockPos here = Towns.middle(town);
        for (int i = 0; i < SPOTS_A_LOOK && search.next < search.spots.size(); i++) {
            ChunkPos spot = search.spots.get(search.next++);
            if (Towns.start(level, spot.pack()).isPresent()) {
                long key = spot.pack();
                StakeData.get(level).neighbour(town, new Neighbour(key, Towns.name(level, key), Towns.middle(key)));
                SEARCHES.remove(town);
                HollowFrontier.LOGGER.info("The next town from {} is {}, {} blocks off (place {} of {} looked at)", Towns.name(level, town), Towns.name(level, key),
                        StakeText.paces(here, Towns.middle(key)), search.next, search.spots.size());
                return;
            }
        }
        if (search.next >= search.spots.size()) {
            SEARCHES.remove(town);
            NONE.add(town);
            HollowFrontier.LOGGER.info("No town within {} blocks of {} ({} places looked at)", StakeConfig.warrantReach(), Towns.name(level, town), search.spots.size());
        }
    }

    private static final class Search {
        final List<ChunkPos> spots;
        int next;

        Search(List<ChunkPos> spots) {
            this.spots = spots;
        }
    }

    /**
     * Every chunk within {@code reach} where the map would start a town if the land there suits one (each cell of the
     * towns' grid has one such chunk), nearest first, without {@code town}'s own. The map's own search answers with the
     * first town in a ring of cells, which from next door is the town that asked; this asks each cell in turn instead.
     */
    public static List<ChunkPos> spots(ServerLevel level, long town, int reach) {
        ChunkPos home = ChunkPos.unpack(town);
        BlockPos here = Towns.middle(town);
        var state = level.getChunkSource().getGeneratorState();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Set<ChunkPos> spots = new HashSet<>();
        for (var key : ModStructures.TOWNS) {
            var holder = structures.get(key);
            if (holder.isEmpty()) {
                continue;
            }
            for (StructurePlacement placement : state.getPlacementsForStructure(holder.get())) {
                if (placement instanceof RandomSpreadStructurePlacement spread) {
                    int spacing = spread.spacing();
                    int cells = reach / (spacing * 16) + 1;
                    for (int dx = -cells; dx <= cells; dx++) {
                        for (int dz = -cells; dz <= cells; dz++) {
                            ChunkPos spot = spread.getPotentialStructureChunk(state.getLevelSeed(), home.x() + dx * spacing, home.z() + dz * spacing);
                            if (!spot.equals(home) && StakeText.paces(here, spot.getMiddleBlockPosition(64)) <= reach) {
                                spots.add(spot);
                            }
                        }
                    }
                }
            }
        }
        List<ChunkPos> nearest = new ArrayList<>(spots);
        nearest.sort(Comparator.comparingInt(spot -> StakeText.paces(here, spot.getMiddleBlockPosition(64))));
        return nearest;
    }
}
