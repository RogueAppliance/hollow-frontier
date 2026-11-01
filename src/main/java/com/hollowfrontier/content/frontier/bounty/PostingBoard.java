package com.hollowfrontier.content.frontier.bounty;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Anything with wanted posters on it (18): the one-block Small Bounty Board and the hanging Bounty Board. */
public interface PostingBoard {
    /** The list it shows (BountyBoards), worked out the first time it's asked. */
    String key(ServerLevel level);

    /** What it shows now. */
    BoardFace face();

    /** Looks at its list and the wanted players around, and sends itself out if what it shows has changed. */
    void sync(ServerLevel level);

    /** Where bearings, raids and wanted players are measured from. */
    BlockPos origin();

    int columns();

    int rows();
}
