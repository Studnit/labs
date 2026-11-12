package com.smartlib.domain;
import jakarta.persistence.*;
import java.time.*;

/** A seat reserved for a member for the whole duration of a subscription. */
@Entity @Table(name = "seat_allocation", indexes = {@Index(columnList = "seat_id"), @Index(columnList = "member_id")})
public class SeatAllocation extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Seat seat;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser member;
    @Column(name = "subscription_id") private Long subscriptionId;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    private boolean active = true;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }
    public Seat getSeat() { return seat; }
    public void setSeat(Seat seat) { this.seat = seat; }
    public AppUser getMember() { return member; }
    public void setMember(AppUser member) { this.member = member; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
