package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "subscription", indexes = {@Index(columnList = "library_id,status"), @Index(columnList = "member_id")})

public class Subscription extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser member;
    @ManyToOne(fetch = FetchType.LAZY) private SubscriptionPlan plan;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SubscriptionStatus status = SubscriptionStatus.ACTIVE;
    private boolean reminderSent;
    public long daysLeft() { return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate); }

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public AppUser getMember() { return member; }
    public void setMember(AppUser member) { this.member = member; }

    public SubscriptionPlan getPlan() { return plan; }
    public void setPlan(SubscriptionPlan plan) { this.plan = plan; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
}
