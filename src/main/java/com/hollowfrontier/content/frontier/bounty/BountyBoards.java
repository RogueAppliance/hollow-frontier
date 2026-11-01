package com.hollowfrontier.content.frontier.bounty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * What every Bounty Board has pinned up (18), saved with the Overworld. Boards in one frontier town share one list (keyed by
 * the town's structure start); a board anywhere else keeps its own (keyed by dimension and position). A list is put right
 * the first time it's looked at after dawn: collected posters come down, open ones older than {@link #OPEN_DAYS} days and
 * taken ones older than {@link #TAKEN_DAYS} are replaced, and fresh outlaws go up until there are {@link #OUTLAWS}. A raid
 * leader who got away from this town goes up big as soon as the town knows it (TownRaids' waiting poster).
 */
public class BountyBoards extends SavedData {
    public static final int OUTLAWS = 3;
    public static final int OPEN_DAYS = 3, TAKEN_DAYS = 7;
    public static final int MIN_DISTANCE = 200, MAX_DISTANCE = 500;

    /** One list of postings, and the day it was last put right. */
    public static final class Board {
        static final Codec<Board> CODEC = RecordCodecBuilder.create(i -> i.group(
                BoardPosting.CODEC.listOf().fieldOf("postings").forGetter(b -> b.postings),
                Codec.LONG.fieldOf("day").forGetter(b -> b.day)).apply(i, Board::new));

        private final List<BoardPosting> postings;
        private long day;

        Board(List<BoardPosting> postings, long day) {
            this.postings = new ArrayList<>(postings);
            this.day = day;
        }

        public List<BoardPosting> postings() {
            return List.copyOf(postings);
        }

        public long day() {
            return day;
        }
    }

    public static final Codec<BountyBoards> CODEC = Codec.unboundedMap(Codec.STRING, Board.CODEC)
            .xmap(BountyBoards::new, b -> b.boards);
    public static final SavedDataType<BountyBoards> TYPE = new SavedDataType<>(HollowFrontier.id("bounty_boards"), BountyBoards::new, CODEC);

    private final Map<String, Board> boards = new HashMap<>();

    public BountyBoards() {}

    private BountyBoards(Map<String, Board> boards) {
        this.boards.putAll(boards);
    }

    public static BountyBoards get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The list a board at pos shares: its town's, or its own. */
    public static String keyFor(ServerLevel level, BlockPos pos) {
        if (level.dimension() == Level.OVERWORLD) {
            var town = TownSite.townAt(level, pos);
            if (town.isPresent()) {
                return "town:" + TownSite.keyOf(town.get());
            }
        }
        return "board:" + level.dimension().identifier() + ":" + pos.asLong();
    }

    /** Days since the world began, turning over at dawn. */
    public static long day(Level level) {
        return level.getDefaultClockTime() / 24000L;
    }

    /** The list under key, put right for today (and with any raid leader the town at origin has waiting). */
    public Board board(ServerLevel level, String key, BlockPos origin) {
        Board board = boards.computeIfAbsent(key, k -> {
            setDirty();
            return new Board(List.of(), Long.MIN_VALUE);
        });
        long today = day(level);
        Optional<BountyData> raider = TownRaids.claimPoster(level, origin);
        if (raider.isPresent()) {
            board.postings.removeIf(BoardPosting::raider);
            board.postings.addFirst(BoardPosting.open(raider.get(), true, today));
            setDirty();
        }
        if (board.day != today) {
            refresh(board, today, origin, level.getRandom());
            setDirty();
        }
        return board;
    }

    /** The dawn round: down with the collected and the stale, up with fresh outlaws. Public so game tests can drive it. */
    public static void refresh(Board board, long today, BlockPos origin, RandomSource random) {
        board.postings.removeIf(p -> p.status().closed()
                || p.status() == BoardPosting.Status.OPEN && today - p.postedDay() >= OPEN_DAYS
                || p.status() == BoardPosting.Status.TAKEN && today - p.postedDay() >= TAKEN_DAYS);
        long outlaws = board.postings.stream().filter(p -> !p.raider()).count();
        for (long i = outlaws; i < OUTLAWS; i++) {
            board.postings.add(BoardPosting.open(newBounty(origin, random), false, today));
        }
        board.day = today;
    }

    /** A new outlaw boss hiding out 200-500 blocks from origin. */
    public static BountyData newBounty(BlockPos origin, RandomSource random) {
        float angle = random.nextFloat() * Mth.TWO_PI;
        int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
        int x = origin.getX() + Math.round(Mth.cos(angle) * distance);
        int z = origin.getZ() + Math.round(Mth.sin(angle) * distance);
        return new BountyData(UUID.randomUUID(), OutlawNames.random(random), x, z, 3 + random.nextInt(4), false);
    }

    /** Replaces the posting for the same bounty in that list. */
    public void update(String key, BoardPosting posting) {
        Board board = boards.get(key);
        if (board == null) {
            return;
        }
        for (int i = 0; i < board.postings.size(); i++) {
            if (board.postings.get(i).bounty().id().equals(posting.bounty().id())) {
                board.postings.set(i, posting);
                setDirty();
                return;
            }
        }
    }

    /** A badge came back: wherever its poster hangs, stamp it DEAD. Whether one did. */
    public boolean markDead(UUID bounty) {
        return close(bounty, BoardPosting.Status.DEAD);
    }

    /** The bounty collected ({@code how}: DEAD or CAPTURED): wherever its poster hangs, stamp it so. Whether one was. */
    public boolean close(UUID bounty, BoardPosting.Status how) {
        boolean found = false;
        for (Board board : boards.values()) {
            for (int i = 0; i < board.postings.size(); i++) {
                BoardPosting posting = board.postings.get(i);
                if (posting.bounty().id().equals(bounty) && !posting.status().closed()) {
                    board.postings.set(i, posting.closed(how));
                    found = true;
                }
            }
        }
        if (found) {
            setDirty();
        }
        return found;
    }

    /** The towns (by structure start) whose boards have been looked at: towns the world knows of (20: where captives come from). */
    public List<Long> towns() {
        return boards.keySet().stream().filter(k -> k.startsWith("town:")).map(k -> Long.parseLong(k.substring(5))).toList();
    }

    /** The list a bounty's poster hangs on, if it still hangs anywhere (20: where a prisoner has to be brought). */
    public Optional<String> keyHolding(UUID bounty) {
        return boards.entrySet().stream().filter(e -> e.getValue().postings.stream().anyMatch(p -> p.bounty().id().equals(bounty)))
                .map(Map.Entry::getKey).findFirst();
    }

    /** For tests: the list under key as it stands, if there is one. */
    public Optional<Board> peek(String key) {
        return Optional.ofNullable(boards.get(key));
    }

    /** For tests: a list made up from these postings, last put right on day. */
    public void put(String key, List<BoardPosting> postings, long day) {
        boards.put(key, new Board(postings, day));
        setDirty();
    }
}
