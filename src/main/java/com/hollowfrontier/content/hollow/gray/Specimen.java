package com.hollowfrontier.content.hollow.gray;

/**
 * A beast the Grays can tag (10b): the four megafauna. A tagged one wears riveted plating and a glowing implant, is
 * tougher and quicker, and is driven by the implant: see {@link Specimens}.
 */
public interface Specimen {
    boolean isTagged();

    /** Tags (or untags) it, applying or removing the implant's boosts; a newly tagged beast is healed to its new maximum. */
    void setTagged(boolean tagged);
}
