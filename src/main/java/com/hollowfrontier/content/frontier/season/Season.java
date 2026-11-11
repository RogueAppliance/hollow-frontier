package com.hollowfrontier.content.frontier.season;

import java.time.LocalDate;
import java.time.Month;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.Level;

/**
 * The frontier's December (35): content that comes on by the real date and leaves by itself. The date is the server's
 * own local date, never a client's, so everything seasonal reaches clients by what the server does to the world
 * (blockstates, entities, talk). {@link SeasonConfig} can hold it on or off for a world. Ask on the server only: a client
 * level always answers no.
 */
public final class Season {
    private Season() {}

    /** How a world keeps the season: by the calendar, all year, or never. */
    public enum Mode {
        AUTO,
        ALWAYS,
        NEVER
    }

    /**
     * Holds the date for a whole run (yyyy-mm-dd): the game test server sets it to a day in July, so the suite reads the
     * same in December as in any other month. Unset, the date is the real one.
     */
    public static final String DATE_PROPERTY = "hollowfrontier.season.date";
    private static final Supplier<LocalDate> REAL = real();
    private static Supplier<LocalDate> clock = REAL;

    private static Supplier<LocalDate> real() {
        String held = System.getProperty(DATE_PROPERTY);
        if (held == null || held.isBlank()) {
            return LocalDate::now;
        }
        LocalDate date = LocalDate.parse(held.trim());
        return () -> date;
    }
    private static @Nullable Mode modeOverride;

    /** The clock seam: game tests set the date here (null puts the real one back). */
    public static void setClock(@Nullable Supplier<LocalDate> source) {
        clock = source == null ? REAL : source;
    }

    /** Holds the mode whatever the config says, for game tests (null lets the config decide again). */
    public static void setMode(@Nullable Mode mode) {
        modeOverride = mode;
    }

    public static LocalDate today() {
        return clock.get();
    }

    public static Mode mode() {
        return modeOverride != null ? modeOverride : SeasonConfig.mode();
    }

    /** All of December (or all year, or never, by the world's mode). */
    public static boolean isDecember(Level level) {
        return !level.isClientSide() && december(mode(), today());
    }

    /** December 24th to 26th, the days vanilla's chests turn into presents. `always` doesn't make it Christmas all year. */
    public static boolean isChristmas(Level level) {
        return !level.isClientSide() && christmas(mode(), today());
    }

    public static boolean december(Mode mode, LocalDate date) {
        return switch (mode) {
            case ALWAYS -> true;
            case NEVER -> false;
            case AUTO -> date.getMonth() == Month.DECEMBER;
        };
    }

    public static boolean christmas(Mode mode, LocalDate date) {
        return mode != Mode.NEVER && date.getMonth() == Month.DECEMBER && date.getDayOfMonth() >= 24 && date.getDayOfMonth() <= 26;
    }

    /** The calendar year, for what happens once a year (a parcel, a waived fine). */
    public static int year() {
        return today().getYear();
    }

    /** Whether the Christmas truce holds: no raid starts, and a sheriff lets one fine go. */
    public static boolean truce(Level level) {
        return SeasonConfig.truce() && isChristmas(level);
    }
}
