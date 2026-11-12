package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.repo.AppUserRepository;
import com.smartlib.repo.SubscriptionPlanRepository;
import com.smartlib.repo.SubscriptionRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service

public class SubscriptionService {
    private final SubscriptionRepository subs;
    private final SubscriptionPlanRepository plans;
    private final AppUserRepository users;
    private final NotificationService notifications;
    private final AuditService audit;
    private final CurrentUser ctx;
    private final ApplicationEventPublisher events;

    public SubscriptionService(SubscriptionRepository subs, SubscriptionPlanRepository plans, AppUserRepository users, NotificationService notifications, AuditService audit, CurrentUser ctx, ApplicationEventPublisher events) {
        this.events = events;
        this.subs = subs;
        this.plans = plans;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
        this.ctx = ctx;
    }
    public boolean hasActive(Long memberId) {
        return subs.findFirstByMemberIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(memberId, SubscriptionStatus.ACTIVE, LocalDate.now()).isPresent();
    }
    /** Days remaining on the member's furthest-running active plan including queued renewals (0 if none). */
    public long daysLeft(Long memberId) {
        return current(memberId).map(x -> java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), x.getEndDate()) + 1).orElse(0L);
    }
    public Optional<Subscription> current(Long memberId) {
        return subs.findFirstByMemberIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(memberId, SubscriptionStatus.ACTIVE, LocalDate.now());
    }

    /** Starts a new subscription, or queues a renewal right after the current one ends. */
    @Transactional
    public Subscription activate(AppUser member, SubscriptionPlan plan) {
        if (member.getRole() != Role.MEMBER) throw new BusinessException("Subscriptions are for members only");
        if (!plan.getLibrary().getId().equals(member.libraryId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        LocalDate start = current(member.getId()).map(s -> s.getEndDate().plusDays(1)).orElse(LocalDate.now());
        Subscription s = new Subscription();
        s.setLibrary(member.getLibrary()); s.setMember(member); s.setPlan(plan);
        s.setStartDate(start); s.setEndDate(start.plusDays(plan.getDurationDays() - 1L));
        s = subs.save(s);
        events.publishEvent(new SubscriptionEvents.Activated(s));
        notifications.notify(member, "Subscription active", plan.getName() + " valid until " + s.getEndDate(), "/subscriptions");
        audit.log(member.getLibrary(), ctx.id(), ctx.details() == null ? "system" : ctx.details().getFullName(),
                "SUBSCRIPTION_ACTIVATED", member.getEmail() + " " + plan.getName() + " until " + s.getEndDate());
        return s;
    }

    @Transactional
    public Subscription assign(Long memberId, Long planId) {
        Long lib = ctx.requireLibraryId();
        AppUser m = users.findByIdAndLibraryId(memberId, lib).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        SubscriptionPlan p = plans.findByIdAndLibraryId(planId, lib).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!p.isActive()) throw new BusinessException("This plan is not active");
        return activate(m, p);
    }

    @Transactional
    public void cancel(Long id) {
        Subscription s = subs.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        s.setStatus(SubscriptionStatus.CANCELLED);
        events.publishEvent(new SubscriptionEvents.Cancelled(s));
        audit.log("SUBSCRIPTION_CANCELLED", s.getMember().getEmail());
    }

    @Transactional
    public SubscriptionPlan createPlan(String name, int days, BigDecimal price) {
        if (days < 1 || price.signum() < 0) throw new BusinessException("Invalid plan values");
        SubscriptionPlan p = new SubscriptionPlan();
        p.setLibrary(users.getReferenceById(ctx.id()).getLibrary());
        p.setName(name.trim()); p.setDurationDays(days); p.setPrice(price);
        audit.log("PLAN_CREATED", name);
        return plans.save(p);
    }

    @Transactional
    public void togglePlan(Long id) {
        SubscriptionPlan p = plans.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.setActive(!p.isActive());
    }

    @Transactional(readOnly = true) public List<SubscriptionPlan> plans() { return plans.findByLibraryIdOrderByPrice(ctx.requireLibraryId()); }
    @Transactional(readOnly = true) public List<SubscriptionPlan> activePlans() { return plans.findByLibraryIdAndActiveTrueOrderByPrice(ctx.requireLibraryId()); }
    @Transactional(readOnly = true) public SubscriptionPlan plan(Long id) {
        return plans.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @Transactional(readOnly = true) public List<Subscription> all() { return subs.findByLibraryIdOrderByEndDateDesc(ctx.requireLibraryId()); }
    @Transactional(readOnly = true) public List<Subscription> mine() { return subs.findByMemberIdOrderByEndDateDesc(ctx.id()); }
    @Transactional(readOnly = true) public List<Subscription> expiringSoon(int days) {
        return subs.findByLibraryIdAndStatusAndEndDateBetweenOrderByEndDate(ctx.requireLibraryId(), SubscriptionStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusDays(days));
    }
}
