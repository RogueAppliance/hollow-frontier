package com.hollowfrontier.content.frontier.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The latest raid anywhere, for the Gazette: which story it makes (defended, held, recovered, robbed or gave_up:
 * gazette.hollowfrontier.raid.STORY), the leader, where the town is, when it happened (game time) and what the
 * bank's safe deposit boxes lost (copper, if the gang got away with it), and the town's name (24a; blank in older saves).
 */
public record RaidReport(String story, String leader, int x, int z, long time, int bankLost, String town) {
    public static final Codec<RaidReport> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("story").forGetter(RaidReport::story),
            Codec.STRING.fieldOf("leader").forGetter(RaidReport::leader),
            Codec.INT.fieldOf("x").forGetter(RaidReport::x),
            Codec.INT.fieldOf("z").forGetter(RaidReport::z),
            Codec.LONG.fieldOf("time").forGetter(RaidReport::time),
            Codec.INT.optionalFieldOf("bank_lost", 0).forGetter(RaidReport::bankLost),
            Codec.STRING.optionalFieldOf("town", "").forGetter(RaidReport::town)).apply(i, RaidReport::new));
}
