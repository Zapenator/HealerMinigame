package com.zapenator.healerMinigame.util;

/**
 * Formats elapsed millisecond durations for display.
 */
public final class TimeFormat {

    private TimeFormat() {
    }

    /**
     * Formats a duration as {@code "12.34s"} or, past a minute, {@code "1m 05.20s"}.
     */
    public static String format(long millis) {
        if (millis < 60_000L) {
            return String.format("%.2fs", millis / 1000.0);
        }
        long minutes = millis / 60_000L;
        double seconds = (millis % 60_000L) / 1000.0;
        return String.format("%dm %05.2fs", minutes, seconds);
    }
}
