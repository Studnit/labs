package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "payment", indexes = {@Index(columnList = "library_id,status"), @Index(columnList = "member_id")})

public class Payment extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser member;
    @ManyToOne(fetch = FetchType.LAZY) private SubscriptionPlan plan;
    @ManyToOne(fetch = FetchType.LAZY) private Subscription subscription;
    @Column(precision = 10, scale = 2, nullable = false) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentMethod method;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status = PaymentStatus.PENDING;
    @Column(unique = true) private String receiptNo;
    private String reference;
    private String purpose;
    private Long recordedBy;
    private LocalDateTime paidAt;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public AppUser getMember() { return member; }
    public void setMember(AppUser member) { this.member = member; }

    public SubscriptionPlan getPlan() { return plan; }
    public void setPlan(SubscriptionPlan plan) { this.plan = plan; }

    public Subscription getSubscription() { return subscription; }
    public void setSubscription(Subscription subscription) { this.subscription = subscription; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
