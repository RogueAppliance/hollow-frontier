package com.hollowfrontier.content.frontier.stake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.hollowfrontier.HollowFrontier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Who owns which Old Place (47b), saved with the Overworld: each claim by its stake, with its town, its owner and the day
 * its mailbox last had the paper. Also the next town along from each town, once it has been looked for (a search costs,
 * and towns don't move).
 */
public class StakeData extends SavedData {
    /** How far a claim reaches from its stake, each way along x and z: the whole lot, and room to build on. */
    public static final int RADIUS = 34;

    public record Claim(BlockPos stake, long town, UUID owner, String ownerName, long mailDay) {
        public static final Codec<Claim> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("stake").forGetter(Claim::stake),
                Codec.LONG.fieldOf("town").forGetter(Claim::town),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Claim::owner),
                Codec.STRING.fieldOf("owner_name").forGetter(Claim::ownerName),
                Codec.LONG.optionalFieldOf("mail_day", -1L).forGetter(Claim::mailDay)).apply(i, Claim::new));

        public boolean contains(BlockPos pos) {
            return Math.abs(pos.getX() - stake.getX()) <= RADIUS && Math.abs(pos.getZ() - stake.getZ()) <= RADIUS;
        }

        public Claim mailed(long day) {
            return new Claim(stake, town, owner, ownerName, day);
        }
    }

    private record Known(long town, Neighbours.Neighbour next) {
        static final Codec<Known> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("town").forGetter(Known::town),
                Neighbours.Neighbour.CODEC.fieldOf("next").forGetter(Known::next)).apply(i, Known::new));
    }

    public static final Codec<StakeData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Claim.CODEC.listOf().fieldOf("claims").forGetter(data -> List.copyOf(data.claims.values())),
            Known.CODEC.listOf().optionalFieldOf("neighbours", List.of()).forGetter(data -> data.neighbours.entrySet().stream()
                    .map(e -> new Known(e.getKey(), e.getValue())).toList())).apply(i, StakeData::new));
    public static final SavedDataType<StakeData> TYPE = new SavedDataType<>(HollowFrontier.id("stakes"), StakeData::new, CODEC);

    private final Map<BlockPos, Claim> claims = new HashMap<>();
    private final Map<Long, Neighbours.Neighbour> neighbours = new HashMap<>();

    public StakeData() {}

    private StakeData(List<Claim> claims, List<Known> neighbours) {
        claims.forEach(claim -> this.claims.put(claim.stake(), claim));
        neighbours.forEach(known -> this.neighbours.put(known.town(), known.next()));
    }

    public static StakeData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<Claim> at(BlockPos stake) {
        return Optional.ofNullable(claims.get(stake));
    }

    /** The claim pos lies on, if any. */
    public Optional<Claim> containing(BlockPos pos) {
        return claims.values().stream().filter(claim -> claim.contains(pos)).findFirst();
    }

    public Optional<Claim> of(UUID owner, long town) {
        return claims.values().stream().filter(claim -> claim.owner().equals(owner) && claim.town() == town).findFirst();
    }

    public List<Claim> of(UUID owner) {
        return claims.values().stream().filter(claim -> claim.owner().equals(owner)).toList();
    }

    public List<Claim> in(long town) {
        return claims.values().stream().filter(claim -> claim.town() == town).toList();
    }

    public List<Claim> all() {
        return new ArrayList<>(claims.values());
    }

    public void put(Claim claim) {
        claims.put(claim.stake(), claim);
        setDirty();
    }

    public void remove(BlockPos stake) {
        if (claims.remove(stake) != null) {
            setDirty();
        }
    }

    public Optional<Neighbours.Neighbour> neighbour(long town) {
        return Optional.ofNullable(neighbours.get(town));
    }

    public void neighbour(long town, Neighbours.Neighbour next) {
        neighbours.put(town, next);
        setDirty();
    }
}
