package com.aawararide.ride;

import java.util.Locale;

public final class DurationFormatter {
    private DurationFormatter() {
    }

    public static String format(long totalSeconds) {
        long seconds = Math.max(0, totalSeconds);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainder = seconds % 60;
        return hours > 0
                ? String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, remainder)
                : String.format(Locale.US, "%02d:%02d", minutes, remainder);
    }
}
