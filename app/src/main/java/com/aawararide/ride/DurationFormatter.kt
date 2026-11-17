package com.aawararide.ride

import java.util.Locale

object DurationFormatter {
    @JvmStatic
    fun format(totalSeconds: Long): String {
        val seconds = maxOf(0, totalSeconds)
        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        val remainder = seconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, remainder)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, remainder)
        }
    }
}
