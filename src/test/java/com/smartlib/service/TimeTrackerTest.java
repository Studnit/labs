package com.smartlib.service;

import com.smartlib.domain.Attendance;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for the daily/monthly in-out time calculation. */
class TimeTrackerTest {
    private static Attendance a(String in, String out) {
        Attendance x = new Attendance();
        x.setCheckIn(LocalDateTime.parse(in));
        x.setCheckOut(out == null ? null : LocalDateTime.parse(out));
        return x;
    }
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-11-12T18:00:00");

    @Test void simpleSessionInsideDay() {
        assertEquals(150, TimeTracker.minutesWithin(LocalDateTime.parse("2026-11-12T09:00:00"), LocalDateTime.parse("2026-11-12T11:30:00"),
                LocalDateTime.parse("2026-11-12T00:00:00"), LocalDateTime.parse("2026-11-13T00:00:00"), NOW));
    }
    @Test void openSessionCountsUntilNow() {
        assertEquals(60, TimeTracker.minutesWithin(LocalDateTime.parse("2026-11-12T17:00:00"), null,
                LocalDateTime.parse("2026-11-12T00:00:00"), LocalDateTime.parse("2026-11-13T00:00:00"), NOW));
    }
    @Test void sessionAcrossMidnightIsSplit() {
        var s = a("2026-11-11T23:00:00", "2026-11-12T01:30:00");
        assertEquals(60, TimeTracker.totalMinutes(List.of(s), LocalDateTime.parse("2026-11-11T00:00:00"), LocalDateTime.parse("2026-11-12T00:00:00"), NOW));
        assertEquals(90, TimeTracker.totalMinutes(List.of(s), LocalDateTime.parse("2026-11-12T00:00:00"), LocalDateTime.parse("2026-11-13T00:00:00"), NOW));
    }
    @Test void sessionOutsideWindowIsZero() {
        assertEquals(0, TimeTracker.minutesWithin(LocalDateTime.parse("2026-11-10T09:00:00"), LocalDateTime.parse("2026-11-10T10:00:00"),
                LocalDateTime.parse("2026-11-12T00:00:00"), LocalDateTime.parse("2026-11-13T00:00:00"), NOW));
    }
    @Test void monthlyTotalAndDailyBreakdown() {
        var list = List.of(a("2026-11-01T09:00:00", "2026-11-01T10:00:00"), a("2026-11-12T09:00:00", "2026-11-12T12:00:00"), a("2026-10-31T22:00:00", "2026-11-01T01:00:00"));
        long[] d = TimeTracker.dailyMinutes(list, YearMonth.of(2026, 11), NOW);
        assertEquals(30, d.length);
        assertEquals(60 + 60, d[0]);          // day 1: own session + spill-over from Oct 31
        assertEquals(180, d[11]);             // day 12
        assertEquals(300, java.util.Arrays.stream(d).sum());
    }
    @Test void formatting() {
        assertEquals("0m", TimeTracker.hm(0)); assertEquals("45m", TimeTracker.hm(45));
        assertEquals("2h 5m", TimeTracker.hm(125)); assertEquals("0m", TimeTracker.hm(-5));
    }
}
