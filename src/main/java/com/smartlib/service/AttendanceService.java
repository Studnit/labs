package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.AppUserRepository;
import com.smartlib.repo.AttendanceRepository;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service

public class AttendanceService {
    public record ScanResult(boolean checkedIn, LocalDateTime at, long minutes) {}

    private final AttendanceRepository repo;
    private final AppUserRepository users;
    private final LibraryRepository libraries;
    private final SubscriptionService subscriptions;
    private final FeatureService features;
    private final AuditService audit;
    private final CurrentUser ctx;


    public AttendanceService(AttendanceRepository repo, AppUserRepository users, LibraryRepository libraries, SubscriptionService subscriptions, FeatureService features, AuditService audit, CurrentUser ctx) {
        this.repo = repo;
        this.users = users;
        this.libraries = libraries;
        this.subscriptions = subscriptions;
        this.features = features;
        this.audit = audit;
        this.ctx = ctx;
    }
    @Transactional(readOnly = true)
    public Library libraryForToken(String token) {
        Library lib = libraries.findByQrToken(token).filter(Library::isActive).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Data isolation: a QR code from another library behaves as if it did not exist.
        if (!lib.getId().equals(ctx.libraryId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return lib;
    }

    @Transactional(readOnly = true)
    public Optional<Attendance> open(Long userId) { return repo.findFirstByUserIdAndCheckOutIsNullOrderByCheckInDesc(userId); }

    /** QR scan toggles between check-in and check-out. */
    @Transactional
    public ScanResult scan(String token) {
        Library lib = libraryForToken(token);
        AppUser u = ctx.user();
        if (u.getRole() != Role.MEMBER) throw new BusinessException("Only members can use QR attendance");
        Optional<Attendance> open = open(u.getId());
        if (open.isPresent()) {
            Attendance a = open.get();
            a.setCheckOut(LocalDateTime.now());
            return new ScanResult(false, a.getCheckOut(), a.minutes());
        }
        if (features.enabled(lib.getId(), Feature.SUBSCRIPTIONS) && !subscriptions.hasActive(u.getId()))
            throw new BusinessException("Your subscription is not active. Please renew to check in.");
        Attendance a = new Attendance();
        a.setLibrary(lib); a.setUser(u); a.setCheckIn(LocalDateTime.now()); a.setMethod(AttendanceMethod.QR);
        repo.save(a);
        return new ScanResult(true, a.getCheckIn(), 0);
    }

    @Transactional
    public void manualCheckIn(Long userId, LocalDateTime at, String note) {
        AppUser m = users.findByIdAndLibraryId(userId, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (m.getRole() != Role.MEMBER) throw new BusinessException("Select a member");
        if (open(m.getId()).isPresent()) throw new BusinessException(m.getFullName() + " is already checked in");
        if (at != null && at.isAfter(LocalDateTime.now().plusMinutes(1))) throw new BusinessException("Time cannot be in the future");
        Attendance a = new Attendance();
        a.setLibrary(m.getLibrary()); a.setUser(m); a.setCheckIn(at == null ? LocalDateTime.now() : at);
        a.setMethod(AttendanceMethod.MANUAL); a.setNote(note); a.setRecordedBy(ctx.id());
        repo.save(a);
        audit.log("ATTENDANCE_MANUAL_IN", m.getEmail());
    }

    @Transactional
    public void manualCheckOut(Long attendanceId, LocalDateTime at) {
        Attendance a = repo.findByIdAndLibraryId(attendanceId, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (a.getCheckOut() != null) throw new BusinessException("Already checked out");
        LocalDateTime out = at == null ? LocalDateTime.now() : at;
        if (out.isBefore(a.getCheckIn())) throw new BusinessException("Check-out cannot be before check-in");
        if (out.isAfter(LocalDateTime.now().plusMinutes(1))) throw new BusinessException("Time cannot be in the future");
        a.setCheckOut(out);
        audit.log("ATTENDANCE_MANUAL_OUT", a.getUser().getEmail());
    }

    @Transactional(readOnly = true) public List<Attendance> present() { return repo.findByLibraryIdAndCheckOutIsNullOrderByCheckInDesc(ctx.requireLibraryId()); }
    @Transactional(readOnly = true) public List<Attendance> today() {
        LocalDate d = LocalDate.now();
        return repo.findByLibraryIdAndCheckInBetweenOrderByCheckInDesc(ctx.requireLibraryId(), d.atStartOfDay(), d.plusDays(1).atStartOfDay());
    }
    @Transactional(readOnly = true) public List<Attendance> history(Long userId) { return repo.findByUserIdOrderByCheckInDesc(userId, PageRequest.of(0, 60)); }

    /** Nightly: close sessions left open from previous days so analytics stay accurate. */
    @Transactional
    public int autoCheckoutStale() {
        LocalDateTime cutoff = LocalDate.now().atStartOfDay();
        List<Attendance> stale = repo.findByCheckOutIsNullAndCheckInBefore(cutoff);
        for (Attendance a : stale) {
            a.setCheckOut(a.getCheckIn().toLocalDate().atTime(23, 59));
            a.setNote((a.getNote() == null ? "" : a.getNote() + " ") + "[auto check-out]");
        }
        return stale.size();
    }

    public record TimeStats(long todayMinutes, long monthMinutes, int daysAttended, List<String> labels, List<Double> values) {
        public String today() { return TimeTracker.hm(todayMinutes); }
        public String month() { return TimeTracker.hm(monthMinutes); }
        public String average() { return daysAttended == 0 ? "0m" : TimeTracker.hm(monthMinutes / daysAttended); }
    }

    /** In/out time of the signed-in member: today, this month, and per-day minutes for the chart. */
    @Transactional(readOnly = true)
    public TimeStats myTimeStats() {
        LocalDateTime now = LocalDateTime.now();
        YearMonth ym = YearMonth.from(now);
        List<Attendance> list = repo.overlappingForUser(ctx.id(), ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay());
        return stats(list, ym, now);
    }

    /** Same numbers for the whole library (all members), for staff dashboards. */
    @Transactional(readOnly = true)
    public TimeStats libraryTimeStats() {
        LocalDateTime now = LocalDateTime.now();
        YearMonth ym = YearMonth.from(now);
        List<Attendance> list = repo.overlappingForLibrary(ctx.requireLibraryId(), ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay());
        return stats(list, ym, now);
    }

    private static TimeStats stats(List<Attendance> list, YearMonth ym, LocalDateTime now) {
        long[] daily = TimeTracker.dailyMinutes(list, ym, now);
        List<String> labels = new java.util.ArrayList<>(); List<Double> values = new java.util.ArrayList<>();
        int attended = 0;
        for (int i = 0; i < daily.length; i++) { labels.add(String.valueOf(i + 1)); values.add(Math.round(daily[i] / 6.0) / 10.0); if (daily[i] > 0) attended++; }
        return new TimeStats(daily[now.getDayOfMonth() - 1], java.util.Arrays.stream(daily).sum(), attended, labels, values);
    }
}
