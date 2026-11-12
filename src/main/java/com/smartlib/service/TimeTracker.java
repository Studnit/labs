package com.smartlib.service;

import com.smartlib.domain.Attendance;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/** Pure helpers that turn attendance sessions into minutes per day / month. Open sessions count up to "now". */
public final class TimeTracker {
    private TimeTracker() {}

    /** Minutes of one session that fall inside [from, to). */
    public static long minutesWithin(LocalDateTime in, LocalDateTime out, LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        LocalDateTime end = out == null ? now : out;
        LocalDateTime s = in.isAfter(from) ? in : from;
        LocalDateTime e = end.isBefore(to) ? end : to;
        if (e.isBefore(s) || e.isEqual(s)) return 0;
        return Duration.between(s, e).toMinutes();
    }

    public static long totalMinutes(List<Attendance> list, LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        long sum = 0;
        for (Attendance a : list) sum += minutesWithin(a.getCheckIn(), a.getCheckOut(), from, to, now);
        return sum;
    }

    /** Minutes for every day of the month (index 0 = day 1). Sessions spanning midnight are split across days. */
    public static long[] dailyMinutes(List<Attendance> list, YearMonth month, LocalDateTime now) {
        long[] out = new long[month.lengthOfMonth()];
        for (int d = 1; d <= out.length; d++) {
            LocalDate day = month.atDay(d);
            out[d - 1] = totalMinutes(list, day.atStartOfDay(), day.plusDays(1).atStartOfDay(), now);
        }
        return out;
    }

    /** "2h 5m" / "45m" — kept simple so the Marathi translator can pattern-match it. */
    public static String hm(long minutes) {
        if (minutes < 0) minutes = 0;
        return minutes < 60 ? minutes + "m" : (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}
