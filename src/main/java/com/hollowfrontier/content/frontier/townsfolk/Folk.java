package com.hollowfrontier.content.frontier.townsfolk;

import java.util.Locale;

/**
 * Who a townsfolk is: their look (textures/entity/townsfolk_<id>.png on the shared humanoid model) and the name in
 * their trade screen. Town buildings pick theirs (the bank gets a banker); anyone else gets one at random.
 */
public enum Folk {
    /** Tan hat, green vest, white shirtsleeves. */
    SHOPKEEPER,
    /** Bowler, charcoal suit, watch chain. */
    BANKER,
    /** Leather apron, sooty hat, big mustache. */
    BLACKSMITH,
    /** Straw hat, red work shirt, blue neckerchief, denim. */
    WRANGLER,
    /** Battered hat, white whiskers, red long johns. */
    PROSPECTOR,
    /** Railroad engineer at the depot: hickory-stripe cap, red neckerchief, denim overalls. Never rolled at random. */
    ENGINEER,
    // 24c: the trades that keep to their own buildings, never rolled at random either
    /** The doctor: black bowler and frock coat, grey whiskers. */
    DOCTOR,
    /** The schoolteacher: a straw boater, a blue dress and white blouse. */
    TEACHER,
    /** The preacher: a flat black hat, black coat, white collar. */
    PREACHER,
    /** The postmaster: a green eyeshade, sleeve garters, grey vest. */
    POSTMASTER,
    /** The gunsmith: brown hat, red shirt under a leather apron. */
    GUNSMITH,
    /** The Gazette's editor: a green eyeshade, inky apron, ink-black vest. */
    EDITOR;

    /** The folk a hotel guest or a spawn egg may turn out to be (the engineer and the 24c trades keep to their buildings). */
    public static final Folk[] RANDOM = {SHOPKEEPER, BANKER, BLACKSMITH, WRANGLER, PROSPECTOR};

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String nameKey() {
        return "entity.hollowfrontier.townsfolk." + id();
    }

    public String texture() {
        return "townsfolk_" + id();
    }

    public static Folk byId(String id) {
        for (Folk f : values()) {
            if (f.id().equals(id)) {
                return f;
            }
        }
        return SHOPKEEPER;
    }
}
