package com.hollowfrontier.content.ufo.taken;

/**
 * Every line of phase 50 (lang from datagen). Two voices: the player's own notes ({@code NOTE_}: short, dry, what a
 * prospector would think) and the other captives ({@code SAY_}). The Grays say nothing. Nothing here explains what they
 * are or what the rig is for: the Codex and the Kindled do that.
 */
public final class TakenText {
    private TakenText() {}

    private static final String M = "message.hollowfrontier.taken.";

    // Waking, and the pen
    public static final String NOTE_WAKE = M + "wake", NOTE_EMPTY_HANDS = M + "empty_hands", NOTE_PEN_OPEN = M + "pen_open";
    public static final String SAY_NEW = M + "say.new", SAY_WAIT = M + "say.wait", SAY_GO = M + "say.go", SAY_PENS_OPEN = M + "say.pens_open";
    // Round the ship
    public static final String NOTE_KIT = M + "kit", NOTE_KIT_NONE = M + "kit_none", NOTE_TRAY = M + "tray", NOTE_RIG_SHORTED = M + "rig_shorted";
    public static final String NOTE_PENS = M + "pens", NOTE_HOLD = M + "hold", NOTE_VENT = M + "vent";
    public static final String NOTE_SEEN = M + "seen", NOTE_ALARM = M + "alarm", NOTE_ALARM_OVER = M + "alarm_over";
    public static final String CONSOLE_DEAD = M + "console_dead", SOCKET_EMPTY = M + "socket_empty", CANT_BREAK = M + "cant_break";
    // The engine and the fall
    public static final String NOTE_CELL_1 = M + "cell_1", NOTE_CELL_2 = M + "cell_2", NOTE_FALLING = M + "falling", NOTE_FALL_LATE = M + "fall_late";
    public static final String BAR_FALLING = "bossbar.hollowfrontier.ship_falling", BAR_COMING = "bossbar.hollowfrontier.taken_coming",
            BAR_TURNED = "bossbar.hollowfrontier.taken_turned";
    public static final String NOTE_LISTEN = M + "listen", SAY_BREAD = M + "say.bread";
    // The pods
    public static final String NOTE_POD = M + "pod";
    // Back on the ground, one line for each way off
    public static final String LANDED_POD = M + "landed.pod", LANDED_FELL = M + "landed.fell", LANDED_DIED = M + "landed.died",
            LANDED_CRASHED = M + "landed.crashed", LANDED_LEFT = M + "landed.left";
    public static final String NOTE_KIT_BACK = M + "kit_back", NOTE_CRASH_SITE = M + "crash_site", NOTE_LEFT_BEHIND = M + "left_behind";
    public static final String COMMAND_NOT_ABOARD = "commands.hollowfrontier.ufo.taken.not_aboard", COMMAND_NO_SHIP = "commands.hollowfrontier.ufo.taken.no_ship";
}
