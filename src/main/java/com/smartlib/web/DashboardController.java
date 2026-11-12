package com.smartlib.web;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.*;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller

public class DashboardController {
    private final CurrentUser ctx;
    private final FeatureService features;
    private final AppUserRepository users;
    private final AttendanceRepository attendance;
    private final SubscriptionRepository subs;
    private final PaymentRepository payments;
    private final BookIssueRepository issues;
    private final GuestRepository guests;
    private final ContentReportRepository reports;
    private final SubscriptionService subscriptions;
    private final AttendanceService attendanceService;
    private final SeatService seatService;
    private final AnnouncementService announcements;


    public DashboardController(CurrentUser ctx, FeatureService features, AppUserRepository users, AttendanceRepository attendance, SubscriptionRepository subs, PaymentRepository payments, BookIssueRepository issues, GuestRepository guests, ContentReportRepository reports, SubscriptionService subscriptions, AttendanceService attendanceService, SeatService seatService, AnnouncementService announcements) {
        this.ctx = ctx;
        this.features = features;
        this.users = users;
        this.attendance = attendance;
        this.subs = subs;
        this.payments = payments;
        this.issues = issues;
        this.guests = guests;
        this.reports = reports;
        this.subscriptions = subscriptions;
        this.attendanceService = attendanceService;
        this.seatService = seatService;
        this.announcements = announcements;
    }
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model m) {
        Role role = ctx.role();
        if (role == Role.SUPER_ADMIN) return "redirect:/super/libraries";
        Long lib = ctx.requireLibraryId();
        m.addAttribute("announcements", announcements.list(5));
        if (ctx.staff()) { m.addAttribute("stats", staffStats(lib)); return "dashboard-staff"; }
        if (role == Role.MEMBER) { memberData(lib, m); return "dashboard-member"; }
        return "dashboard-guest";
    }

    private Map<String, Object> staffStats(Long lib) {
        Map<String, Object> s = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDateTime t0 = today.atStartOfDay(), t1 = today.plusDays(1).atStartOfDay();
        s.put("members", users.countByLibraryIdAndRoleAndStatus(lib, Role.MEMBER, UserStatus.ACTIVE));
        s.put("pending", users.countByLibraryIdAndRoleAndStatus(lib, Role.MEMBER, UserStatus.PENDING));
        if (features.enabled(lib, Feature.QR_ATTENDANCE)) {
            s.put("present", attendance.countByLibraryIdAndCheckOutIsNull(lib));
            s.put("visitsToday", attendance.countByLibraryIdAndCheckInBetween(lib, t0, t1));
            List<String> labels = new ArrayList<>(); List<Long> values = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                labels.add(d.getDayOfWeek().toString().substring(0, 3));
                values.add(attendance.countByLibraryIdAndCheckInBetween(lib, d.atStartOfDay(), d.plusDays(1).atStartOfDay()));
            }
            s.put("chartLabels", labels); s.put("chartValues", values);
            var ts = attendanceService.libraryTimeStats();
            s.put("hoursToday", ts.today()); s.put("hoursMonth", ts.month());
        }
        if (features.enabled(lib, Feature.SUBSCRIPTIONS)) {
            s.put("activeSubs", subs.countByLibraryIdAndStatus(lib, SubscriptionStatus.ACTIVE));
            s.put("expiring", subs.countByLibraryIdAndStatusAndEndDateBetween(lib, SubscriptionStatus.ACTIVE, today, today.plusDays(7)));
        }
        if (features.enabled(lib, Feature.PAYMENTS)) {
            s.put("revenueToday", payments.sumPaid(lib, PaymentStatus.PAID, t0, t1));
            s.put("revenueMonth", payments.sumPaid(lib, PaymentStatus.PAID, today.withDayOfMonth(1).atStartOfDay(), t1));
            s.put("pendingPayments", payments.countByLibraryIdAndStatus(lib, PaymentStatus.PENDING));
        }
        if (features.enabled(lib, Feature.BOOK_MANAGEMENT)) {
            s.put("booksOut", issues.countByLibraryIdAndReturnedAtIsNull(lib));
            s.put("overdue", issues.countByLibraryIdAndReturnedAtIsNullAndDueDateBefore(lib, today));
        }
        if (features.enabled(lib, Feature.GUEST_MANAGEMENT)) s.put("guestsToday", guests.countByLibraryIdAndCheckInBetween(lib, t0, t1));
        if (features.enabled(lib, Feature.COMMUNITY_FEED) || features.enabled(lib, Feature.DISCUSSION_FORUM))
            s.put("openReports", reports.countByLibraryIdAndStatus(lib, ReportStatus.OPEN));
        return s;
    }

    private void memberData(Long lib, Model m) {
        Long me = ctx.id();
        if (features.enabled(lib, Feature.SUBSCRIPTIONS)) {
            m.addAttribute("subscription", subscriptions.current(me).orElse(null));
            long left = subscriptions.daysLeft(me);
            m.addAttribute("daysLeft", left);
            m.addAttribute("canRenew", left <= PaymentService.RENEW_WINDOW_DAYS);
        }
        if (features.enabled(lib, Feature.SEAT_BOOKING)) m.addAttribute("allocation", seatService.myAllocation().orElse(null));
        if (features.enabled(lib, Feature.QR_ATTENDANCE)) {
            m.addAttribute("openSession", attendanceService.open(me).orElse(null));
            m.addAttribute("timeStats", attendanceService.myTimeStats());
        }
        if (features.enabled(lib, Feature.BOOK_MANAGEMENT)) m.addAttribute("myBooks", issues.findByMemberIdAndReturnedAtIsNullOrderByDueDate(me));
        if (features.enabled(lib, Feature.SEAT_BOOKING))
            m.addAttribute("myBookings", seatService.mine().stream()
                    .filter(b -> b.getStatus() == BookingStatus.BOOKED && !b.getBookingDate().isBefore(LocalDate.now())).toList());
    }
}
