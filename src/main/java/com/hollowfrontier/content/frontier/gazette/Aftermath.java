package com.hollowfrontier.content.frontier.gazette;

import java.util.Locale;
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
 * The frontier after the leash (15c), as the Gazette hears of it: the latest freed man sworn in or sent home, or ruin
 * mended in the Hollow Earth, with who did it and where. Only the newest is kept; the Gazette prints it while it's a
 * week fresh, and otherwise one of its general pieces on the aftermath. Kept with the Overworld's saved data.
 */
public class Aftermath extends SavedData {
    public enum Kind implements StringRepresentable {
        DEPUTY, HOMECOMING, RUIN, WAYSTONE;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** What happened, who did it, where, and when (game time). */
    public record Event(Kind kind, String who, int x, int z, long time) {
        static final Codec<Event> CODEC = RecordCodecBuilder.create(i -> i.group(
                Kind.CODEC.fieldOf("kind").forGetter(Event::kind),
                Codec.STRING.fieldOf("who").forGetter(Event::who),
                Codec.INT.fieldOf("x").forGetter(Event::x),
                Codec.INT.fieldOf("z").forGetter(Event::z),
                Codec.LONG.fieldOf("time").forGetter(Event::time)).apply(i, Event::new));
    }

    public static final Codec<Aftermath> CODEC = Event.CODEC.optionalFieldOf("latest").xmap(Aftermath::new, a -> a.latest).codec();
    public static final SavedDataType<Aftermath> TYPE = new SavedDataType<>(HollowFrontier.id("aftermath"), Aftermath::new, CODEC);

    private Optional<Event> latest = Optional.empty();

    public Aftermath() {}

    private Aftermath(Optional<Event> latest) {
        this.latest = latest;
    }

    public static Aftermath get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<Event> latest() {
        return latest;
    }

    public void record(ServerLevel level, Kind kind, String who, BlockPos where) {
        latest = Optional.of(new Event(kind, who, where.getX(), where.getZ(), level.getGameTime()));
        setDirty();
    }
}
