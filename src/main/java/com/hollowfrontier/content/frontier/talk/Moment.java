package com.hollowfrontier.content.frontier.talk;

import java.util.Locale;

/** When a line is said. Each speaker's pool is split by moment ({@link ChatterLines}). */
public enum Moment {
    /** Coming near a speaker for the first time in a while. */
    GREET,
    /** Using a speaker: opening a trade screen, or just a word. */
    TALK,
    /** Two townsfolk talking where a player can hear ({@link ChatterLines.Exchange}). */
    GOSSIP,
    // Barks, shouted to everyone near
    /** An outlaw or a lawman sets his sights on a player. */
    SPOT,
    /** An outlaw's gun runs dry. */
    RELOAD,
    /** The player an outlaw is fighting stops to reload (21a). */
    TAUNT,
    /** An outlaw is badly hurt (a third of his health left). */
    HURT,
    /** Raiders ride out. */
    RUN,
    /** A lawman brings down an outlaw, or a wanted player. */
    ARREST,
    /** A posse rides in after a wanted player. */
    POSSE,
    /** A lawman musters for a raid on his town. */
    MUSTER,
    // One-offs, said to one player
    /** A jailbird heckles a passer-by. */
    JEER,
    /** The leash breaks and an implanted man comes to himself. */
    FREED,
    /** A man in black warns a player off. */
    WARN,
    /** A man in black buys something of the Grays'. */
    BOUGHT,
    /** A captive let out of a Gray pen tells how they got there (20). */
    RESCUED,
    /** A saloon brawl starts (21b): the brawlers, and the barmaid, shout. */
    BRAWL,
    /** A Gray in disguise is found out (21c): the townsfolk near it shout. */
    EXPOSED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
