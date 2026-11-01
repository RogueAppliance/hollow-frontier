package com.hollowfrontier.content.frontier.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** How long a raid's beats last, in ticks: the warning, the safecracker's job, the fight before the gang gives up, the getaway. */
public record RaidTimings(int warning, int vault, int timeout, int retreat) {
    public static final Codec<RaidTimings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("warning").forGetter(RaidTimings::warning),
            Codec.INT.fieldOf("vault").forGetter(RaidTimings::vault),
            Codec.INT.fieldOf("timeout").forGetter(RaidTimings::timeout),
            Codec.INT.fieldOf("retreat").forGetter(RaidTimings::retreat)).apply(i, RaidTimings::new));
}
