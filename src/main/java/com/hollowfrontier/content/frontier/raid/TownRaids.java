package com.hollowfrontier.content.frontier.raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;

/**
 * Every town's raid state, saved with the Overworld: per-town records keyed by the town's structure start (cooldown,
 * robbed-until, a waiting poster), the raids in progress (at most one per town) and the latest raid for the Gazette.
 * Ticked by RaidEvents; also answers the town's questions (is there a raid here? are we robbed? any poster?).
 */
public class TownRaids extends SavedData {
    public static final Codec<TownRaids> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, TownRecord.CODEC).fieldOf("towns").forGetter(TownRaids::townsByName),
            TownRaid.CODEC.listOf().fieldOf("active").forGetter(d -> List.copyOf(d.active.values())),
            RaidReport.CODEC.optionalFieldOf("last_report").forGetter(d -> d.lastReport)).apply(i, TownRaids::new));
    public static final SavedDataType<TownRaids> TYPE = new SavedDataType<>(HollowFrontier.id("town_raids"), TownRaids::new, CODEC);

    private final Map<Long, TownRecord> towns = new HashMap<>();
    private final Map<Long, TownRaid> active = new LinkedHashMap<>();
    Optional<RaidReport> lastReport;

    public TownRaids() {
        lastReport = Optional.empty();
    }

    private TownRaids(Map<String, TownRecord> towns, List<TownRaid> active, Optional<RaidReport> lastReport) {
        towns.forEach((key, record) -> this.towns.put(Long.parseLong(key), record));
        for (TownRaid raid : active) {
            this.active.put(raid.site().key(), raid);
        }
        this.lastReport = lastReport;
    }

    private Map<String, TownRecord> townsByName() {
        Map<String, TownRecord> out = new HashMap<>();
        towns.forEach((key, record) -> out.put(Long.toString(key), record));
        return out;
    }

    public static TownRaids get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // ---------------------------------------------------------------- queries

    /** The town's record, made on first sight with the first-raid grace period. */
    public TownRecord record(long key, long now) {
        return towns.computeIfAbsent(key, k -> {
            setDirty();
            return new TownRecord(now + RaidConfig.firstRaidGrace(), 0, Optional.empty(), Optional.empty());
        });
    }

    /** Every town with a record (raided, or due one), by structure start. */
    public java.util.Set<Long> knownTowns() {
        return java.util.Set.copyOf(towns.keySet());
    }

    public Optional<TownRecord> recordIfKnown(long key) {
        return Optional.ofNullable(towns.get(key));
    }

    public Optional<RaidReport> lastReport() {
        return lastReport;
    }

    public @Nullable TownRaid raidIn(long key) {
        return active.get(key);
    }

    public @Nullable TownRaid byId(UUID id) {
        for (TownRaid raid : active.values()) {
            if (raid.id().equals(id)) {
                return raid;
            }
        }
        return null;
    }

    public List<TownRaid> active() {
        return List.copyOf(active.values());
    }

    /** The raid going on where pos is (inside the town's bounds), if any. */
    public @Nullable TownRaid raidAt(BlockPos pos) {
        for (TownRaid raid : active.values()) {
            if (raid.site().contains(pos)) {
                return raid;
            }
        }
        return null;
    }

    public static @Nullable TownRaid raidAt(ServerLevel level, BlockPos pos) {
        return level.dimension() == ServerLevel.OVERWORLD ? get(level).raidAt(pos) : null;
    }

    /** While a raid is on, the town's lawmen leave one- and two-star players be: they have bigger problems. */
    public static boolean truceAt(ServerLevel level, BlockPos pos) {
        return raidAt(level, pos) != null;
    }

    /** The record of the raided town whose bounds hold pos. */
    private Optional<TownRecord> recordAt(BlockPos pos) {
        for (TownRecord record : towns.values()) {
            if (record.contains(pos)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    /** Whether the town at pos was robbed recently (worse prices, a grim banker). */
    public static boolean isRobbed(ServerLevel level, BlockPos pos) {
        return level.dimension() == ServerLevel.OVERWORLD && get(level).recordAt(pos).map(r -> r.robbed(level.getGameTime())).orElse(false);
    }

    /** Takes the town's waiting raid poster (a Bounty Board at pos hands it out once), if there is one. */
    public static Optional<BountyData> claimPoster(ServerLevel level, BlockPos pos) {
        if (level.dimension() != ServerLevel.OVERWORLD) {
            return Optional.empty();
        }
        TownRaids data = get(level);
        Optional<TownRecord> record = data.recordAt(pos);
        if (record.isEmpty() || record.get().poster.isEmpty()) {
            return Optional.empty();
        }
        Optional<BountyData> poster = record.get().poster;
        record.get().poster = Optional.empty();
        data.setDirty();
        return poster;
    }

    // ---------------------------------------------------------------- starting and stopping

    /**
     * Starts a raid on the site (warning first). Refused (empty) if that town already has one going.
     * gangSize includes the leader.
     */
    public Optional<TownRaid> start(ServerLevel level, TownSite site, RaidTimings timings, int gangSize, boolean mounted) {
        if (active.containsKey(site.key())) {
            return Optional.empty();
        }
        record(site.key(), level.getGameTime()).bounds = Optional.of(site.bounds());
        TownRaid raid = TownRaid.create(level, site, timings, gangSize, mounted);
        active.put(site.key(), raid);
        raid.announceWarning(level);
        setDirty();
        return Optional.of(raid);
    }

    /** Calls off the town's raid (the gang just goes). False if there wasn't one. */
    public boolean stop(ServerLevel level, long key) {
        TownRaid raid = active.remove(key);
        if (raid == null) {
            return false;
        }
        raid.abandon(level, this);
        setDirty();
        return true;
    }

    // ---------------------------------------------------------------- ticking

    void tick(ServerLevel level) {
        for (TownRaid raid : List.copyOf(active.values())) {
            if (raid.tick(level, this)) {
                active.remove(raid.site().key());
                if (raid.outcome() == TownRaid.Outcome.NONE) {
                    raid.abandon(level, this);
                } else {
                    raid.finish(level, this);
                }
                setDirty();
            }
        }
        if (level.getGameTime() % 10 == 0) {
            for (ServerPlayer player : level.players()) {
                int alert = 0;
                for (TownRaid raid : active.values()) {
                    if (raid.watchArea().contains(player.position())) {
                        alert = raid.phase() == TownRaid.Phase.WARNING ? 1 : 2;
                    }
                }
                if (player.getData(ModAttachments.RAID_ALERT.get()) != alert) {
                    player.setData(ModAttachments.RAID_ALERT.get(), alert);
                }
            }
        }
    }

    /**
     * At night, each town with a survival player in it may be raided: past its cooldown, no other raid on in the
     * world, nobody in town with a posse after them, and a roll of the dice.
     */
    void rollForRaids(ServerLevel level) {
        if (!RaidConfig.enabled() || level.getDifficulty() == Difficulty.PEACEFUL || !level.isDarkOutside() || !active.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Map<Long, StructureStart> occupied = new LinkedHashMap<>();
        List<ServerPlayer> hunted = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !player.getData(ModAttachments.IN_TOWN.get())) {
                continue;
            }
            TownSite.townAt(level, player.blockPosition()).ifPresent(start -> {
                occupied.putIfAbsent(TownSite.keyOf(start), start);
                if (Wanted.stars(player) >= 3) {
                    hunted.add(player);
                }
            });
        }
        for (Map.Entry<Long, StructureStart> town : occupied.entrySet()) {
            TownRecord record = record(town.getKey(), now);
            AABB bounds = AABB.of(town.getValue().getBoundingBox());
            boolean posse = hunted.stream().anyMatch(p -> bounds.contains(p.position()))
                    || !level.getEntitiesOfClass(Lawman.class, bounds.inflate(32.0), Lawman::isPosse).isEmpty();
            if (now < record.nextRaid || posse || level.getRandom().nextDouble() >= RaidConfig.chance()) {
                continue;
            }
            TownSite site = TownSite.of(level, town.getValue(), level.getRandom());
            start(level, site, RaidConfig.timings(), TownRaid.gangSizeFor(level, site), true);
            return; // one at a time
        }
    }
}
