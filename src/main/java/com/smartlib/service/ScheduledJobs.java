package com.smartlib.service;

import com.smartlib.config.AppProperties;
import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.BookIssueRepository;
import com.smartlib.repo.SubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@Component

public class ScheduledJobs {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScheduledJobs.class);
    private final SubscriptionRepository subs;
    private final BookIssueRepository issues;
    private final AttendanceService attendance;
    private final NotificationService notifications;
    private final FeatureService features;
    private final AppProperties props;


    public ScheduledJobs(SubscriptionRepository subs, BookIssueRepository issues, AttendanceService attendance, NotificationService notifications, FeatureService features, AppProperties props) {
        this.subs = subs;
        this.issues = issues;
        this.attendance = attendance;
        this.notifications = notifications;
        this.features = features;
        this.props = props;
    }
    /** Daily 00:10 — expire subscriptions, close stale attendance. */
    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void nightly() {
        LocalDate today = LocalDate.now();
        var expired = subs.findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, today);
        expired.forEach(s -> {
            s.setStatus(SubscriptionStatus.EXPIRED);
            notifications.notify(s.getMember(), "Subscription expired", "Your plan ended on " + s.getEndDate() + ". Renew to keep access.", "/subscriptions");
        });
        int closed = attendance.autoCheckoutStale();
        log.info("Nightly job: {} subscriptions expired, {} sessions auto-closed", expired.size(), closed);
    }

    /** Daily 09:00 — renewal reminders and overdue book alerts (only for libraries with the module enabled). */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void reminders() {
        LocalDate today = LocalDate.now();
        for (Subscription s : subs.findByStatusAndEndDateBetweenAndReminderSentFalse(SubscriptionStatus.ACTIVE, today, today.plusDays(props.reminderDays()))) {
            if (!features.enabled(s.getLibrary().getId(), Feature.SUBSCRIPTIONS)) continue;
            notifications.notify(s.getMember(), "Renewal reminder", "Your subscription ends on " + s.getEndDate() + " (" + s.daysLeft() + " days left).", "/subscriptions");
            s.setReminderSent(true);
        }
        for (BookIssue i : issues.findByReturnedAtIsNullAndDueDateBeforeAndOverdueNotifiedFalse(today)) {
            if (!features.enabled(i.getLibrary().getId(), Feature.BOOK_MANAGEMENT)) continue;
            notifications.notify(i.getMember(), "Book overdue", i.getBook().getTitle() + " was due on " + i.getDueDate() + ". Please return it.", "/books/mine");
            i.setOverdueNotified(true);
        }
    }
}
