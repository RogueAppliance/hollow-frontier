package com.hollowfrontier.content.frontier.raid;

import java.util.Optional;

import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * What a town remembers between raids (mutable, saved in TownRaids): the earliest game time the next raid may come,
 * until when it's sore from being robbed, its bounds (once it has been raided), and a Wanted Poster for a raid leader who got away, waiting on its board.
 */
public final class TownRecord {
    public static final Codec<TownRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("next_raid").forGetter(r -> r.nextRaid),
            Codec.LONG.fieldOf("robbed_until").forGetter(r -> r.robbedUntil),
            BountyData.CODEC.optionalFieldOf("poster").forGetter(r -> r.poster),
            BoundingBox.CODEC.optionalFieldOf("bounds").forGetter(r -> r.bounds)).apply(i, TownRecord::new));

    public long nextRaid;
    public long robbedUntil;
    public Optional<BountyData> poster;
    public Optional<BoundingBox> bounds;

    public TownRecord(long nextRaid, long robbedUntil, Optional<BountyData> poster, Optional<BoundingBox> bounds) {
        this.bounds = bounds;
        this.nextRaid = nextRaid;
        this.robbedUntil = robbedUntil;
        this.poster = poster;
    }

    public boolean contains(BlockPos pos) {
        return bounds.isPresent() && bounds.get().isInside(pos);
    }

    public boolean robbed(long now) {
        return now < robbedUntil;
    }
}
