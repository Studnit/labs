package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.*;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.ExportService.Table;
import com.smartlib.web.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service

public class ReportService {
    /** Sections are null when the corresponding module is disabled for the library. */
    public record Summary(LocalDate from, LocalDate to, Long visits, Long uniqueVisitors, BigDecimal revenue, Long newMembers,
                          Long booksIssued, Long guests, List<String> labels, List<Long> visitsSeries, List<BigDecimal> revenueSeries) {}

    private final AttendanceRepository attendance;
    private final PaymentRepository payments;
    private final AppUserRepository users;
    private final BookIssueRepository issues;
    private final GuestRepository guests;
    private final SubscriptionRepository subs;
    private final FeatureService features;
    private final CurrentUser ctx;


    public ReportService(AttendanceRepository attendance, PaymentRepository payments, AppUserRepository users, BookIssueRepository issues, GuestRepository guests, SubscriptionRepository subs, FeatureService features, CurrentUser ctx) {
        this.attendance = attendance;
        this.payments = payments;
        this.users = users;
        this.issues = issues;
        this.guests = guests;
        this.subs = subs;
        this.features = features;
        this.ctx = ctx;
    }
    private static LocalDateTime start(LocalDate d) { return d.atStartOfDay(); }
    private static LocalDateTime end(LocalDate d) { return d.plusDays(1).atStartOfDay().minusNanos(1); }

    private void checkRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new BusinessException("'To' date must be after 'From' date");
        if (ChronoUnit.DAYS.between(from, to) > 366) throw new BusinessException("Maximum range is one year");
    }

    @Transactional(readOnly = true)
    public Summary summary(LocalDate from, LocalDate to) {
        checkRange(from, to);
        Long lib = ctx.requireLibraryId();
        boolean att = features.enabled(lib, Feature.QR_ATTENDANCE), pay = features.enabled(lib, Feature.PAYMENTS);
        boolean bk = features.enabled(lib, Feature.BOOK_MANAGEMENT), gs = features.enabled(lib, Feature.GUEST_MANAGEMENT);
        List<String> labels = new ArrayList<>(); List<Long> vs = new ArrayList<>(); List<BigDecimal> rs = new ArrayList<>();
        Map<LocalDate, Long> perDay = new HashMap<>();
        if (att) for (Attendance a : attendance.findByLibraryIdAndCheckInBetweenOrderByCheckInDesc(lib, start(from), end(to))) perDay.merge(a.getCheckIn().toLocalDate(), 1L, Long::sum);
        Map<LocalDate, BigDecimal> revDay = new HashMap<>();
        if (pay) for (Payment p : payments.findByLibraryIdAndStatusAndPaidAtBetweenOrderByPaidAtDesc(lib, PaymentStatus.PAID, start(from), end(to))) revDay.merge(p.getPaidAt().toLocalDate(), p.getAmount(), BigDecimal::add);
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            labels.add(d.toString()); vs.add(perDay.getOrDefault(d, 0L)); rs.add(revDay.getOrDefault(d, BigDecimal.ZERO));
        }
        return new Summary(from, to,
                att ? attendance.countByLibraryIdAndCheckInBetween(lib, start(from), end(to)) : null,
                att ? attendance.countDistinctUsers(lib, start(from), end(to)) : null,
                pay ? payments.sumPaid(lib, PaymentStatus.PAID, start(from), end(to)) : null,
                users.countJoined(lib, Role.MEMBER, start(from), end(to)),
                bk ? issues.countByLibraryIdAndIssuedAtBetween(lib, from, to) : null,
                gs ? guests.countByLibraryIdAndCheckInBetween(lib, start(from), end(to)) : null,
                labels, att ? vs : null, pay ? rs : null);
    }

    public static Feature featureFor(String type) {
        return switch (type) {
            case "attendance" -> Feature.QR_ATTENDANCE;
            case "payments" -> Feature.PAYMENTS;
            case "subscriptions" -> Feature.SUBSCRIPTIONS;
            case "books" -> Feature.BOOK_MANAGEMENT;
            case "guests" -> Feature.GUEST_MANAGEMENT;
            case "members" -> Feature.REPORTS;
            default -> throw new BusinessException("Unknown report");
        };
    }

    @Transactional(readOnly = true)
    public Table table(String type, LocalDate from, LocalDate to) {
        checkRange(from, to);
        Long lib = ctx.requireLibraryId();
        features.require(lib, featureFor(type));
        List<List<Object>> rows = new ArrayList<>();
        switch (type) {
            case "attendance" -> {
                for (Attendance a : attendance.findByLibraryIdAndCheckInBetweenOrderByCheckInDesc(lib, start(from), end(to)))
                    rows.add(List.of(a.getUser().getFullName(), String.valueOf(a.getUser().getMemberCode()), a.getCheckIn(), a.getCheckOut() == null ? "" : a.getCheckOut(), a.minutes(), a.getMethod().name()));
                return new Table("Attendance", List.of("Member", "Code", "Check-in", "Check-out", "Minutes", "Method"), rows);
            }
            case "payments" -> {
                for (Payment p : payments.findByLibraryIdAndStatusAndPaidAtBetweenOrderByPaidAtDesc(lib, PaymentStatus.PAID, start(from), end(to)))
                    rows.add(List.of(String.valueOf(p.getReceiptNo()), p.getPaidAt(), p.getMember().getFullName(), String.valueOf(p.getPurpose()), p.getMethod().name(), p.getAmount()));
                return new Table("Payments", List.of("Receipt", "Paid at", "Member", "Purpose", "Method", "Amount"), rows);
            }
            case "subscriptions" -> {
                for (Subscription s : subs.findByLibraryIdOrderByEndDateDesc(lib))
                    rows.add(List.of(s.getMember().getFullName(), s.getPlan() == null ? "" : s.getPlan().getName(), s.getStartDate().toString(), s.getEndDate().toString(), s.getStatus().name()));
                return new Table("Subscriptions", List.of("Member", "Plan", "Start", "End", "Status"), rows);
            }
            case "books" -> {
                for (BookIssue i : issues.findByLibraryIdAndIssuedAtBetweenOrderByIssuedAtDesc(lib, from, to))
                    rows.add(List.of(i.getBook().getTitle(), i.getMember().getFullName(), i.getIssuedAt().toString(), i.getDueDate().toString(), i.getReturnedAt() == null ? "" : i.getReturnedAt().toString(), i.getFine() == null ? BigDecimal.ZERO : i.getFine()));
                return new Table("Book issues", List.of("Book", "Member", "Issued", "Due", "Returned", "Fine"), rows);
            }
            case "guests" -> {
                for (Guest g : guests.findByLibraryIdAndCheckInBetweenOrderByCheckInDesc(lib, start(from), end(to)))
                    rows.add(List.of(g.getName(), String.valueOf(g.getPhone()), String.valueOf(g.getPurpose()), g.getCheckIn(), g.getCheckOut() == null ? "" : g.getCheckOut()));
                return new Table("Guests", List.of("Name", "Phone", "Purpose", "Check-in", "Check-out"), rows);
            }
            default -> {
                for (AppUser u : users.findByLibraryIdAndRole(lib, Role.MEMBER))
                    rows.add(List.of(u.getFullName(), String.valueOf(u.getMemberCode()), u.getEmail(), String.valueOf(u.getPhone()), u.getStatus().name(), u.getCreatedAt()));
                return new Table("Members", List.of("Name", "Code", "Email", "Phone", "Status", "Joined"), rows);
            }
        }
    }
}
