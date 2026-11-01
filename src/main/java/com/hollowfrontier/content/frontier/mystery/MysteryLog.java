package com.hollowfrontier.content.frontier.mystery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The frontier's strange doings (11a), saved with the Overworld: the latest few sightings, mutilations and crop circles
 * (kind, when, where), for the Gazette to print and the townsfolk to grumble about, and the night the last one happened
 * (so a night has at most one); and (11b) the men in black's next visit, booked after a mystery, and their last.
 */
public class MysteryLog extends SavedData {
    /** How many records are kept (the oldest go first). */
    public static final int KEPT = 16;

    public enum Kind implements StringRepresentable {
        SIGHTING, MUTILATION, CROP_CIRCLE,
        /** A Gray Landing (15a), once the leash is broken. */
        LANDING;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Sign(Kind kind, long time, int x, int z) {
        public static final Codec<Sign> CODEC = RecordCodecBuilder.create(i -> i.group(
                Kind.CODEC.fieldOf("kind").forGetter(Sign::kind),
                Codec.LONG.fieldOf("time").forGetter(Sign::time),
                Codec.INT.fieldOf("x").forGetter(Sign::x),
                Codec.INT.fieldOf("z").forGetter(Sign::z)).apply(i, Sign::new));

        public double distanceTo(BlockPos pos) {
            return Math.hypot(pos.getX() - x, pos.getZ() - z);
        }
    }

    /** Where the men in black are due to turn up (11b): after a mystery, at {@code time}; and where they last did. */
    public record Visit(long time, BlockPos pos) {
        public static final Codec<Visit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("time").forGetter(Visit::time),
                BlockPos.CODEC.fieldOf("pos").forGetter(Visit::pos)).apply(i, Visit::new));
    }

    public static final Codec<MysteryLog> CODEC = RecordCodecBuilder.create(i -> i.group(
            Sign.CODEC.listOf().fieldOf("signs").forGetter(log -> List.copyOf(log.signs)),
            Codec.LONG.optionalFieldOf("last_night", -1L).forGetter(log -> log.lastNight),
            Visit.CODEC.optionalFieldOf("due_visit").forGetter(log -> log.dueVisit),
            Visit.CODEC.optionalFieldOf("last_visit").forGetter(log -> log.lastVisit)).apply(i, MysteryLog::new));
    public static final SavedDataType<MysteryLog> TYPE = new SavedDataType<>(HollowFrontier.id("frontier_mysteries"), MysteryLog::new, CODEC);

    private final List<Sign> signs = new ArrayList<>();
    private long lastNight;
    private Optional<Visit> dueVisit = Optional.empty();
    private Optional<Visit> lastVisit = Optional.empty();

    public MysteryLog() {
        lastNight = -1;
    }

    private MysteryLog(List<Sign> signs, long lastNight, Optional<Visit> dueVisit, Optional<Visit> lastVisit) {
        this.signs.addAll(signs);
        this.lastNight = lastNight;
        this.dueVisit = dueVisit;
        this.lastVisit = lastVisit;
    }

    public static MysteryLog get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Which night a game time falls in (a night runs from dusk to the next morning). */
    public static long night(long gameTime) {
        return (gameTime + 12000) / 24000;
    }

    public void record(Kind kind, long time, BlockPos pos) {
        signs.add(new Sign(kind, time, pos.getX(), pos.getZ()));
        while (signs.size() > KEPT) {
            signs.removeFirst();
        }
        lastNight = night(time);
        bookVisit(time + MenInBlack.VISIT_DELAY, pos); // the men in black follow (11b)
        setDirty();
    }

    /** Whether something has already happened tonight. */
    public boolean happenedTonight(long now) {
        return lastNight == night(now);
    }

    /** The latest sign (of any kind, or of {@code kind} if given) within {@code range} of {@code pos} and {@code window} ticks. */
    public Optional<Sign> latest(Kind kind, BlockPos pos, double range, long now, long window) {
        return signs.stream()
                .filter(s -> (kind == null || s.kind() == kind) && now - s.time() <= window && s.distanceTo(pos) <= range)
                .max(Comparator.comparingLong(Sign::time));
    }

    /** Books the men in black's visit to {@code pos} at {@code time} (the next mystery moves it). */
    public void bookVisit(long time, BlockPos pos) {
        dueVisit = Optional.of(new Visit(time, pos.immutable()));
        setDirty();
    }

    public Optional<Visit> dueVisit() {
        return dueVisit;
    }

    /** The booked visit has happened (now, at {@code pos}). */
    public void visited(long time, BlockPos pos) {
        dueVisit = Optional.empty();
        lastVisit = Optional.of(new Visit(time, pos.immutable()));
        setDirty();
    }

    public Optional<Visit> lastVisit() {
        return lastVisit;
    }

    public List<Sign> signs() {
        return List.copyOf(signs);
    }
}
