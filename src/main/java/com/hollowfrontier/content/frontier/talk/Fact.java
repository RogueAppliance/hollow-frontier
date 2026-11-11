package com.hollowfrontier.content.frontier.talk;

/**
 * Something true about a moment of talk that a line can ask for ({@link Cond}): the hour and weather where the speaker
 * stands, what the listener has done, carries and wears, and what's been happening in town. Worked out by
 * {@link Context#of}.
 */
public enum Fact {
    NIGHT,
    RAIN,
    STORM,
    /** One wanted star or more. */
    WANTED,
    /** Two stars or more: townsfolk won't trade, lawmen shoot on sight. */
    OUTLAW,
    /** The men in black have warned the listener off (11b). */
    SUSPECT,
    /** The men in black are hunting the listener. */
    HUNTED(true),
    /** This town was robbed lately (worse prices, a grim banker). */
    ROBBED,
    /** There's a raid on this town right now. */
    RAID(true),
    /** The listener carries a Wanted Poster; its outlaw's name is the line's first argument. */
    POSTER,
    /** The whole cowboy outfit. */
    OUTFIT,
    /** The Starsteel Crown. */
    CROWN,
    /** A Kindled Sigil, in hand or pack. */
    SIGIL,
    /** Lately, near here: a steer cut up, a field pressed in rings, lights in the sky (11a). */
    MUTILATIONS,
    CROP_CIRCLES,
    SIGHTINGS,
    /** Lately, near here: a Gray Landing (15a). */
    LANDINGS,
    /** The listener saw a sighting lately that nobody else did: the moonshine (21b). */
    SEEING_THINGS,
    /** A Gray in disguise (21c), not yet found out, is near the speaker. */
    STRANGER,
    /** A dust devil came through near here today (29). */
    DUST_DEVIL,
    /** The listener has an airship (steam/airship) but hasn't opened a Polar Gateway yet: the road north is theirs to take (32d). */
    AIRSHIP,
    /** The listener has been handed the Polar Survey's letter (32d), so nobody tells them it's still waiting. */
    SURVEY_LETTER,
    /** A dust devil took the listener's hat a little while ago, and they haven't a hat on. */
    LOST_HAT,
    /** The listener has been to the Still Sea (hollow/still_sea, 43): the frontier has heard, and doesn't believe a word of it. */
    SEA_SEEN,
    /** It's December by the server's calendar (35, {@link com.hollowfrontier.content.frontier.season.Season}). */
    DECEMBER,
    /** It's December 24th to 26th: the truce is on, and the church social has been. */
    CHRISTMAS;

    /**
     * An urgent fact crowds out everything else: when it holds and a speaker has a line for it, that's the only kind of
     * line they'll say (nobody remarks on the weather during a raid).
     */
    public final boolean urgent;

    Fact() {
        this(false);
    }

    Fact(boolean urgent) {
        this.urgent = urgent;
    }
}
