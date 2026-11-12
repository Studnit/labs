package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.AppUserRepository;
import com.smartlib.repo.PaymentRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service

public class PaymentService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PaymentRepository payments;
    private final AppUserRepository users;
    private final SubscriptionService subscriptions;
    private final PaymentGateway gateway;
    private final FeatureService features;
    private final NotificationService notifications;
    private final AuditService audit;
    private final CurrentUser ctx;


    public PaymentService(PaymentRepository payments, AppUserRepository users, SubscriptionService subscriptions, PaymentGateway gateway, FeatureService features, NotificationService notifications, AuditService audit, CurrentUser ctx) {
        this.payments = payments;
        this.users = users;
        this.subscriptions = subscriptions;
        this.gateway = gateway;
        this.features = features;
        this.notifications = notifications;
        this.audit = audit;
        this.ctx = ctx;
    }
    private String receiptNo(Long libId) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append("ABCDEFGHJKLMNPQRSTUVWXYZ23456789".charAt(RANDOM.nextInt(32)));
        return "RCP-" + libId + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + sb;
    }


    /** Members with a healthy active plan can't buy another until it is close to ending. */
    public static final int RENEW_WINDOW_DAYS = 7;
    public void assertCanBuy() { requireRenewable(ctx.id()); }
    private void requireRenewable(Long memberId) {
        long left = subscriptions.daysLeft(memberId);
        if (left > RENEW_WINDOW_DAYS)
            throw new BusinessException("You already have an active subscription (" + left + " days left). You can renew in the last " + RENEW_WINDOW_DAYS + " days.");
    }

    private AppUser member(Long id) {
        return users.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Staff records a cash/UPI payment received over the counter (optionally buying a plan). */
    @Transactional
    public Payment collect(Long memberId, Long planId, BigDecimal amount, PaymentMethod method, String reference, String purpose) {
        if (method == PaymentMethod.ONLINE) throw new BusinessException("Online payments are made by the member");
        AppUser m = member(memberId);
        SubscriptionPlan plan = null;
        if (planId != null) {
            features.require(m.libraryId(), Feature.SUBSCRIPTIONS);
            plan = subscriptions.plan(planId);
            amount = plan.getPrice();
        }
        if (amount == null || amount.signum() <= 0) throw new BusinessException("Enter a valid amount");
        Payment p = new Payment();
        p.setLibrary(m.getLibrary()); p.setMember(m); p.setPlan(plan); p.setAmount(amount); p.setMethod(method);
        p.setReference(reference); p.setPurpose(plan != null ? plan.getName() + " subscription" : purpose); p.setRecordedBy(ctx.id());
        return markPaid(payments.save(p));
    }

    /** Member starts buying a plan. UPI waits for staff verification; ONLINE continues at the gateway. */
    @Transactional
    public Payment checkout(Long planId, PaymentMethod method, String reference) {
        features.require(ctx.requireLibraryId(), Feature.SUBSCRIPTIONS);
        AppUser m = ctx.user();
        SubscriptionPlan plan = subscriptions.plan(planId);
        if (!plan.isActive()) throw new BusinessException("This plan is not available");
        requireRenewable(m.getId());
        if (method == PaymentMethod.CASH) throw new BusinessException("Please pay cash at the desk — staff will record it");
        if (method == PaymentMethod.UPI && (reference == null || reference.isBlank())) throw new BusinessException("Enter your UPI transaction reference");
        if (reference != null && reference.length() > 60) throw new BusinessException("Reference is too long");
        Payment p = new Payment();
        p.setLibrary(m.getLibrary()); p.setMember(m); p.setPlan(plan); p.setAmount(plan.getPrice()); p.setMethod(method);
        p.setReference(reference); p.setPurpose(plan.getName() + " subscription");
        p = payments.save(p);
        if (method == PaymentMethod.UPI)
            notifications.notifyStaff(m.getLibrary(), "UPI payment to verify", m.getFullName() + " paid " + p.getAmount() + " (ref " + reference + ")", "/payments");
        return p;
    }

    @Transactional
    public Payment gatewayConfirm(Long id) {
        Payment p = payments.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!p.getMember().getId().equals(ctx.id()) || p.getMethod() != PaymentMethod.ONLINE) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (p.getStatus() != PaymentStatus.PENDING) throw new BusinessException("Payment already processed");
        PaymentGateway.Result r = gateway.charge(p);
        if (!r.success()) { p.setStatus(PaymentStatus.FAILED); throw new BusinessException("Payment failed"); }
        p.setReference(r.reference());
        return markPaid(p);
    }

    @Transactional
    public void verify(Long id, boolean approve) {
        Payment p = payments.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getStatus() != PaymentStatus.PENDING) throw new BusinessException("Payment already processed");
        p.setRecordedBy(ctx.id());
        if (approve) markPaid(p);
        else {
            p.setStatus(PaymentStatus.FAILED);
            notifications.notify(p.getMember(), "Payment rejected", "We could not verify your payment of " + p.getAmount(), "/payments");
            audit.log("PAYMENT_REJECTED", "id=" + id);
        }
    }

    /** Creates a pending cash fine (e.g. late book return) for staff to collect. */
    @Transactional
    public Payment createFine(AppUser member, BigDecimal amount, String purpose) {
        Payment p = new Payment();
        p.setLibrary(member.getLibrary()); p.setMember(member); p.setAmount(amount); p.setMethod(PaymentMethod.CASH); p.setPurpose(purpose);
        return payments.save(p);
    }

    @Transactional
    public Payment markPaid(Payment p) {
        if (p.getStatus() == PaymentStatus.PAID) return p;
        p.setStatus(PaymentStatus.PAID);
        p.setPaidAt(LocalDateTime.now());
        p.setReceiptNo(receiptNo(p.getLibrary().getId()));
        if (p.getPlan() != null && p.getSubscription() == null) p.setSubscription(subscriptions.activate(p.getMember(), p.getPlan()));
        p = payments.save(p);
        notifications.notify(p.getMember(), "Payment received", p.getAmount() + " received — receipt " + p.getReceiptNo(), "/payments");
        audit.log(p.getLibrary(), ctx.id(), ctx.details() == null ? "system" : ctx.details().getFullName(),
                "PAYMENT_PAID", p.getReceiptNo() + " " + p.getAmount() + " " + p.getMethod());
        return p;
    }

    @Transactional(readOnly = true)
    public List<Payment> recent() { return payments.findByLibraryIdOrderByCreatedAtDesc(ctx.requireLibraryId(), PageRequest.of(0, 200)); }
    @Transactional(readOnly = true)
    public List<Payment> mine() { return payments.findByMemberIdOrderByCreatedAtDesc(ctx.id()); }

    /** Receipt visible to staff of the library or to the paying member. */
    @Transactional(readOnly = true)
    public Payment receipt(Long id) {
        Payment p = payments.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ctx.staff() && !p.getMember().getId().equals(ctx.id())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (p.getStatus() != PaymentStatus.PAID) throw new BusinessException("Receipt is available after payment is confirmed");
        return p;
    }
    @Transactional(readOnly = true)
    public Payment get(Long id) {
        Payment p = payments.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ctx.staff() && !p.getMember().getId().equals(ctx.id())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return p;
    }
}
