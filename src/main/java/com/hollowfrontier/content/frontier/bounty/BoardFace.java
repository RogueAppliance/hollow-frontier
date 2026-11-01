package com.hollowfrontier.content.frontier.bounty;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.law.Wanted;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * What a board shows (18): its list's postings, and the wanted players near enough to have a poster of their own. Both
 * kinds of board carry one (the one-block Small Bounty Board, and the hanging Bounty Board), saved with them and sent to
 * clients to draw.
 * <p>
 * The face is a grid of cells, {@code columns} by {@code rows}, one poster to a cell ({@link #slots}): a raid leader across
 * the first two cells of the top row, then the outlaws, then wanted players in what room is left (after the outlaws, so
 * they never move the posters people click on), and last the Polar Survey's notice (32d) if there's a cell to spare.
 */
public record BoardFace(List<BoardPosting> shown, List<WantedFace> wanted) {
    public static final double WANTED_RANGE = 128.0;
    public static final int MAX_WANTED = 2;
    public static final BoardFace EMPTY = new BoardFace(List.of(), List.of());

    /** A wanted player's own poster: their name and what the fine stands at, in copper. */
    public record WantedFace(String name, int fine) {
        static final Codec<WantedFace> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(WantedFace::name),
                Codec.INT.fieldOf("fine").forGetter(WantedFace::fine)).apply(i, WantedFace::new));
    }

    /** SURVEY: the standing notice for news of the Polar Survey (32d), the same on every board; its index means nothing. */
    public enum Kind { RAIDER, WANTED, OUTLAW, SURVEY }

    /** One poster on the face: what it is ({@code index} into the postings or the wanted), and where (0..1, top left). */
    public record Slot(Kind kind, int index, float u0, float v0, float u1, float v1) {
        public boolean contains(float u, float v) {
            return u >= u0 && u <= u1 && v >= v0 && v <= v1;
        }
    }

    public static final MapCodec<BoardFace> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BoardPosting.CODEC.listOf().optionalFieldOf("shown", List.of()).forGetter(BoardFace::shown),
            WantedFace.CODEC.listOf().optionalFieldOf("wanted", List.of()).forGetter(BoardFace::wanted)).apply(i, BoardFace::new));
    public static final StreamCodec<ByteBuf, BoardFace> STREAM_CODEC = ByteBufCodecs.fromCodec(MAP_CODEC.codec());

    /** The face of the list under key today, with the wanted players around origin. */
    public static BoardFace of(ServerLevel level, String key, BlockPos origin) {
        List<BoardPosting> postings = BountyBoards.get(level).board(level, key, origin).postings();
        List<WantedFace> faces = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (faces.size() < MAX_WANTED && !player.isSpectator() && !player.isCreative() && Wanted.stars(player) >= 2
                    && player.position().closerThan(Vec3.atCenterOf(origin), WANTED_RANGE)) {
                faces.add(new WantedFace(player.getGameProfile().name(), Wanted.fine(player)));
            }
        }
        return new BoardFace(postings, List.copyOf(faces));
    }

    /** Where each poster hangs on a face of columns by rows cells. */
    public List<Slot> slots(int columns, int rows) {
        List<Slot> slots = new ArrayList<>();
        int cells = columns * rows;
        int cell = 0;
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).raider()) {
                slots.add(new Slot(Kind.RAIDER, i, 0.0F, 0.0F, 2.0F / columns, 1.0F / rows));
                cell = 2;
                break;
            }
        }
        for (int i = 0; i < shown.size() && cell < cells; i++) {
            if (!shown.get(i).raider()) {
                slots.add(cell(Kind.OUTLAW, i, cell++, columns, rows));
            }
        }
        for (int i = 0; i < wanted.size() && cell < cells; i++, cell++) {
            slots.add(cell(Kind.WANTED, i, cell, columns, rows));
        }
        if (cell < cells) {
            slots.add(cell(Kind.SURVEY, -1, cell, columns, rows)); // 32d: in the last room there is, so it never crowds out a poster
        }
        return slots;
    }

    private static Slot cell(Kind kind, int index, int cell, int columns, int rows) {
        float u = (float) (cell % columns) / columns, v = (float) (cell / columns) / rows;
        return new Slot(kind, index, u, v, u + 1.0F / columns, v + 1.0F / rows);
    }

    /** The poster at that spot on the face (0..1 across, 0..1 down), if one hangs there. */
    public @Nullable Slot slotAt(int columns, int rows, float u, float v) {
        for (Slot slot : slots(columns, rows)) {
            if (slot.contains(u, v)) {
                return slot;
            }
        }
        return null;
    }
}
