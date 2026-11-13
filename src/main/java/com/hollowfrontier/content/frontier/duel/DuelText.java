package com.hollowfrontier.content.frontier.duel;

/** Every line high noon (51) says: translation keys, with the English in ModLanguageProvider. */
public final class DuelText {
    private DuelText() {}

    private static final String M = "message.hollowfrontier.duel.";
    private static final String H = "hud.hollowfrontier.duel.";

    /** On the poster, and said when it's taken: its boss means to settle it in the street. */
    public static final String POSTER_LINE = "tooltip.hollowfrontier.bounty.calls_out";
    public static final String WORD_SENT = M + "word_sent";

    /** The question over the hotbar (the boss's name), and its two keys (each key's name, then the seconds left). */
    public static final String ASK = H + "ask";
    public static final String ASK_KEYS = H + "ask_keys";
    public static final String MARK = H + "mark";
    public static final String[] TOLLS = {H + "toll_1", H + "toll_2", H + "toll_3"};
    public static final String DRAW = H + "draw";
    public static final String WON = H + "won";
    public static final String LOST = H + "lost";

    public static final String DECLINED = M + "declined";
    public static final String DECLINED_LAST = M + "declined_last";
    public static final String TOO_LATE = M + "too_late";
    public static final String LENT = M + "lent";
    public static final String LENT_BACK = M + "lent_back";
    public static final String FLINCH = M + "flinch";
    public static final String GREETING = M + "greeting";
    public static final String WON_LINE = M + "won";
    public static final String WON_HEAD = M + "won_head";
    public static final String LOST_LINE = M + "lost";
    public static final String LOST_PURSE = M + "lost_purse";
    public static final String LOST_SAYS = M + "lost_says";
    public static final String STOOD_DOWN = M + "stood_down";
    public static final String CALLED_OFF = M + "called_off";
    /** A line of talk: who, and what they said. */
    public static final String SAYS = M + "says";

    public static final String KEY_YES = "key.hollowfrontier.duel_yes";
    public static final String KEY_NO = "key.hollowfrontier.duel_no";
}
