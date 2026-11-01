package com.hollowfrontier.content.frontier.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One poster pinned to a Bounty Board (18): the bounty, whether it's a raid leader who got away (shown big, across the top),
 * how it stands, who took it, and the day it went up.
 */
public record BoardPosting(BountyData bounty, boolean raider, Status status, String takenBy, long postedDay) {
    public enum Status {
        /** Up for grabs. */
        OPEN,
        /** A hunter has the poster ({@link #takenBy}). */
        TAKEN,
        /** The badge came back: stamped DEAD until the next dawn. */
        DEAD,
        /** The man himself came back alive, out of a Gray pen (20): stamped CAPTURED until the next dawn. */
        CAPTURED;

        /** Collected, one way or the other: it comes down at dawn. */
        public boolean closed() {
            return this == DEAD || this == CAPTURED;
        }
    }

    public static final Codec<BoardPosting> CODEC = RecordCodecBuilder.create(i -> i.group(
            BountyData.CODEC.fieldOf("bounty").forGetter(BoardPosting::bounty),
            Codec.BOOL.optionalFieldOf("raider", false).forGetter(BoardPosting::raider),
            Codec.STRING.xmap(Status::valueOf, Status::name).fieldOf("status").forGetter(BoardPosting::status),
            Codec.STRING.optionalFieldOf("taken_by", "").forGetter(BoardPosting::takenBy),
            Codec.LONG.fieldOf("posted").forGetter(BoardPosting::postedDay)).apply(i, BoardPosting::new));

    public static BoardPosting open(BountyData bounty, boolean raider, long day) {
        return new BoardPosting(bounty, raider, Status.OPEN, "", day);
    }

    public BoardPosting taken(String by) {
        return new BoardPosting(bounty, raider, Status.TAKEN, by, postedDay);
    }

    public BoardPosting dead() {
        return closed(Status.DEAD);
    }

    public BoardPosting closed(Status how) {
        return new BoardPosting(bounty, raider, how, takenBy, postedDay);
    }
}
