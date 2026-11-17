package com.aawararide.ride;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class DurationFormatterTest {
    @Test
    public void formatsDurationsUnderOneHourAsMinutesAndSeconds() {
        assertEquals("05:09", DurationFormatter.format(309));
    }

    @Test
    public void formatsDurationsOverOneHourAsHoursMinutesAndSeconds() {
        assertEquals("01:02:03", DurationFormatter.format(3723));
    }
}
