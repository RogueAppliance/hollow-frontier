package com.hollowfrontier.content.frontier.stake;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;

/**
 * The job a deputy has in hand (47a), on the player. {@code points} are where it takes them: the rounds' stops in order,
 * the spot a drifter was last seen, or the town a warrant goes to. {@code progress} is how many stops are walked (rounds)
 * or 1 once the drifter's roped. {@code quarry} is the drifter, once he's turned up.
 */
public record SheriffJob(Kind kind, long town, int progress, List<BlockPos> points, Optional<UUID> quarry) {
    public enum Kind implements StringRepresentable {
        /** Walk the town's stops after dark. */
        ROUNDS,
        /** Rope a drifter and walk him in alive. */
        BRING_IN,
        /** Carry a warrant to another town's sheriff. */
        WARRANT;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final Codec<SheriffJob> CODEC = RecordCodecBuilder.create(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(SheriffJob::kind),
            Codec.LONG.fieldOf("town").forGetter(SheriffJob::town),
            Codec.INT.fieldOf("progress").forGetter(SheriffJob::progress),
            BlockPos.CODEC.listOf().fieldOf("points").forGetter(SheriffJob::points),
            UUIDUtil.CODEC.optionalFieldOf("quarry").forGetter(SheriffJob::quarry)).apply(i, SheriffJob::new));

    /** The attachment's default: never read, the job is only looked at where hasData says there is one. */
    public static final SheriffJob NONE = new SheriffJob(Kind.ROUNDS, 0, 0, List.of(), Optional.empty());

    public SheriffJob advanced() {
        return new SheriffJob(kind, town, progress + 1, points, quarry);
    }

    public SheriffJob withQuarry(UUID outlaw) {
        return new SheriffJob(kind, town, progress, points, Optional.of(outlaw));
    }

    public boolean walked() {
        return kind == Kind.ROUNDS && progress >= points.size();
    }
}
