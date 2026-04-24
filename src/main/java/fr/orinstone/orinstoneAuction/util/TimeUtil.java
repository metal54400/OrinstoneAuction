package fr.orinstone.orinstoneAuction.util;

import java.util.concurrent.TimeUnit;

public final class TimeUtil {
    private TimeUtil() {}

    public static String formatRemaining(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long days = TimeUnit.SECONDS.toDays(totalSeconds);
        long hours = TimeUnit.SECONDS.toHours(totalSeconds) % 24L;
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60L;

        if (days > 0L) {
            return days + "d " + hours + "h";
        }
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
