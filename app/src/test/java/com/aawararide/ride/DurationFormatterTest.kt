package com.aawararide.ride

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatterTest {
    @Test
    fun formatsDurationsUnderOneHourAsMinutesAndSeconds() {
        assertEquals("05:09", DurationFormatter.format(309))
    }

    @Test
    fun formatsDurationsOverOneHourAsHoursMinutesAndSeconds() {
        assertEquals("01:02:03", DurationFormatter.format(3723))
    }
}
