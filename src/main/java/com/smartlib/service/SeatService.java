package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.repo.SeatAllocationRepository;
import com.smartlib.repo.SeatBookingRepository;
import com.smartlib.repo.AppUserRepository;
import com.smartlib.repo.SeatRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service

public class SeatService {
    public record SeatView(Seat seat, boolean free, boolean mine, boolean reserved, String bookedBy) {}

    private final SeatRepository seats;
    private final SeatBookingRepository bookings;
    private final SeatAllocationRepository allocations;
    private final NotificationService notifications;
    private final LibraryRepository libraries;
    private final AppUserRepository users;
    private final SubscriptionService subscriptions;
    private final FeatureService features;
    private final AuditService audit;
    private final CurrentUser ctx;


    public SeatService(SeatRepository seats, SeatBookingRepository bookings, SeatAllocationRepository allocations, NotificationService notifications, LibraryRepository libraries, AppUserRepository users, SubscriptionService subscriptions, FeatureService features, AuditService audit, CurrentUser ctx) {
        this.seats = seats;
        this.bookings = bookings;
        this.allocations = allocations;
        this.notifications = notifications;
        this.libraries = libraries;
        this.users = users;
        this.subscriptions = subscriptions;
        this.features = features;
        this.audit = audit;
        this.ctx = ctx;
    }
    @Transactional(readOnly = true)
    public List<SeatView> availability(LocalDate date, LocalTime start, LocalTime end) {
        Long lib = ctx.requireLibraryId();
        Map<Long, SeatBooking> taken = bookings.overlapping(lib, date, BookingStatus.BOOKED, start, end).stream()
                .collect(Collectors.toMap(b -> b.getSeat().getId(), b -> b, (a, b) -> a));
        Map<Long, SeatAllocation> reserved = allocations.overlapping(lib, date, date).stream()
                .collect(Collectors.toMap(a -> a.getSeat().getId(), a -> a, (a, b) -> a));
        return seats.findByLibraryIdAndActiveTrueOrderByLabel(lib).stream().map(s -> {
            SeatAllocation al = reserved.get(s.getId());
            if (al != null) {
                boolean mine = al.getMember().getId().equals(ctx.id());
                return new SeatView(s, mine, mine, true, mine ? ctx.details().getFullName() : (ctx.staff() ? al.getMember().getFullName() : "Reserved"));
            }
            SeatBooking b = taken.get(s.getId());
            boolean mine = b != null && b.getMember().getId().equals(ctx.id());
            return new SeatView(s, b == null, mine, false, b == null ? null : (mine || ctx.staff() ? b.getMember().getFullName() : "Booked"));
        }).toList();
    }

    /** The seat reserved for the current member today (via subscription), if any. */
    @Transactional(readOnly = true)
    public Optional<SeatAllocation> myAllocation() {
        return allocations.currentFor(ctx.id(), LocalDate.now(), PageRequest.of(0, 1)).stream().findFirst();
    }
    @Transactional(readOnly = true)
    public Optional<SeatAllocation> allocationOf(Long memberId) {
        return allocations.currentFor(memberId, LocalDate.now(), PageRequest.of(0, 1)).stream().findFirst();
    }
    @Transactional(readOnly = true)
    public List<SeatAllocation> activeAllocations() {
        return allocations.findByLibraryIdAndActiveTrueAndEndDateGreaterThanEqualOrderBySeatLabel(ctx.requireLibraryId(), LocalDate.now());
    }

    private boolean seatFree(Seat seat, Long memberId, LocalDate from, LocalDate to) {
        return seat.isActive()
                && allocations.countForSeatOverlapping(seat.getId(), from, to) == 0
                && bookings.countOthersBetween(seat.getId(), BookingStatus.BOOKED, from, to, memberId) == 0;
    }

    /**
     * Picks a seat for [from,to]: the member's previous seat if it is free for the whole period,
     * otherwise the first free seat by label. Returns empty when the library is full.
     */
    private Optional<Seat> pickSeat(Library lib, AppUser member, LocalDate from, LocalDate to) {
        List<SeatAllocation> history = allocations.findByMemberIdOrderByEndDateDesc(member.getId(), PageRequest.of(0, 5));
        for (SeatAllocation old : history) {
            Seat prev = old.getSeat();
            if (prev.getLibrary().getId().equals(lib.getId()) && seatFree(prev, member.getId(), from, to)) return Optional.of(prev);
        }
        return seats.findByLibraryIdAndActiveTrueOrderByLabel(lib.getId()).stream()
                .filter(s -> seatFree(s, member.getId(), from, to)).findFirst();
    }

    private SeatAllocation allocate(Library lib, AppUser member, Seat seat, Subscription sub) {
        SeatAllocation a = new SeatAllocation();
        a.setLibrary(lib); a.setMember(member); a.setSeat(seat);
        a.setSubscriptionId(sub.getId()); a.setStartDate(sub.getStartDate()); a.setEndDate(sub.getEndDate());
        a = allocations.save(a);
        notifications.notify(member, "Seat allocated", "Seat " + seat.getLabel() + " is reserved for you until " + sub.getEndDate(), "/seats");
        return a;
    }

    /** Runs inside the activating transaction. */
    @EventListener
    public void onSubscriptionActivated(SubscriptionEvents.Activated ev) {
        Subscription sub = ev.subscription();
        Library lib = sub.getLibrary();
        if (!features.enabled(lib.getId(), Feature.SEAT_BOOKING)) return;
        if (!allocations.findBySubscriptionId(sub.getId()).isEmpty()) return;
        Optional<Seat> seat = pickSeat(lib, sub.getMember(), sub.getStartDate(), sub.getEndDate());
        if (seat.isPresent()) allocate(lib, sub.getMember(), seat.get(), sub);
        else notifications.notifyStaff(lib, "No seat available",
                sub.getMember().getFullName() + " subscribed from " + sub.getStartDate() + " but every seat is taken", "/seats");
    }

    @EventListener
    public void onSubscriptionCancelled(SubscriptionEvents.Cancelled ev) {
        allocations.findBySubscriptionId(ev.subscription().getId()).forEach(a -> a.setActive(false));
    }

    /** Member asks for a seat for their current subscription (e.g. when none was free earlier). */
    @Transactional
    public SeatAllocation allocateForMe() {
        AppUser me = ctx.user();
        features.require(me.libraryId(), Feature.SEAT_BOOKING);
        Subscription sub = subscriptions.current(me.getId()).orElseThrow(() -> new BusinessException("Take a subscription to get a seat"));
        if (!allocations.findBySubscriptionId(sub.getId()).stream().filter(SeatAllocation::isActive).toList().isEmpty())
            throw new BusinessException("A seat is already reserved for you");
        Seat seat = pickSeat(me.getLibrary(), me, sub.getStartDate(), sub.getEndDate()).orElseThrow(() -> new BusinessException("No seat is free for your subscription period right now"));
        return allocate(me.getLibrary(), me, seat, sub);
    }

    /** Staff assigns or changes a member's seat for their current subscription. seatId null = auto-pick. */
    @Transactional
    public SeatAllocation staffAllocate(Long memberId, Long seatId) {
        Long libId = ctx.requireLibraryId();
        features.require(libId, Feature.SEAT_BOOKING);
        AppUser member = users.findByIdAndLibraryId(memberId, libId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Subscription sub = subscriptions.current(memberId).orElseThrow(() -> new BusinessException("This member has no active subscription"));
        Library lib = member.getLibrary();
        // release the current allocation first so the member's own seat doesn't block the change
        allocations.findBySubscriptionId(sub.getId()).forEach(a -> a.setActive(false));
        allocations.flush();
        Seat seat = seatId == null
                ? pickSeat(lib, member, sub.getStartDate(), sub.getEndDate()).orElseThrow(() -> new BusinessException("No seat is free for that period"))
                : seats.findByIdAndLibraryId(seatId, libId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!seatFree(seat, memberId, sub.getStartDate(), sub.getEndDate())) throw new BusinessException("Seat " + seat.getLabel() + " is not free for " + sub.getStartDate() + " – " + sub.getEndDate());
        SeatAllocation a = allocate(lib, member, seat, sub);
        audit.log("SEAT_ALLOCATED", member.getEmail() + " → " + seat.getLabel());
        return a;
    }

    @Transactional
    public SeatBooking book(Long seatId, LocalDate date, LocalTime start, LocalTime end) {
        AppUser me = ctx.user();
        if (me.getRole() != Role.MEMBER) throw new BusinessException("Only members can book seats");
        Library lib = me.getLibrary();
        Seat seat = seats.findByIdAndLibraryId(seatId, lib.getId()).filter(Seat::isActive).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!end.isAfter(start)) throw new BusinessException("End time must be after start time");
        if (date.isBefore(LocalDate.now())) throw new BusinessException("Cannot book a past date");
        if (date.isEqual(LocalDate.now()) && end.isBefore(LocalTime.now())) throw new BusinessException("That time slot has already passed");
        if (start.isBefore(lib.getOpenTime()) || end.isAfter(lib.getCloseTime()))
            throw new BusinessException("The library is open " + lib.getOpenTime() + " – " + lib.getCloseTime());
        if (features.enabled(lib.getId(), Feature.SUBSCRIPTIONS) && !subscriptions.hasActive(me.getId()))
            throw new BusinessException("An active subscription is required to book a seat");
        if (!allocations.currentFor(me.getId(), date, PageRequest.of(0, 1)).isEmpty())
            throw new BusinessException("Your seat is already reserved for you through your subscription");
        if (allocations.countForSeatOverlapping(seatId, date, date) > 0)
            throw new BusinessException("That seat is reserved for another member");
        if (!bookings.overlapping(lib.getId(), date, BookingStatus.BOOKED, start, end).stream().noneMatch(b -> b.getSeat().getId().equals(seatId)))
            throw new BusinessException("That seat was just taken for this slot");
        if (bookings.countMemberOverlapping(me.getId(), date, BookingStatus.BOOKED, start, end) > 0)
            throw new BusinessException("You already have a seat booked in this time slot");
        SeatBooking b = new SeatBooking();
        b.setLibrary(lib); b.setSeat(seat); b.setMember(me); b.setBookingDate(date); b.setStartTime(start); b.setEndTime(end);
        return bookings.save(b);
    }

    @Transactional
    public void cancel(Long id) {
        SeatBooking b = bookings.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ctx.staff() && !b.getMember().getId().equals(ctx.id())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (b.getStatus() == BookingStatus.BOOKED) b.setStatus(BookingStatus.CANCELLED);
    }

    @Transactional(readOnly = true) public List<SeatBooking> mine() { return bookings.findByMemberIdOrderByBookingDateDescStartTimeDesc(ctx.id(), PageRequest.of(0, 30)); }
    @Transactional(readOnly = true) public List<SeatBooking> upcoming() {
        return bookings.findByLibraryIdAndBookingDateGreaterThanEqualAndStatusOrderByBookingDateAscStartTimeAsc(ctx.requireLibraryId(), LocalDate.now(), BookingStatus.BOOKED);
    }
    @Transactional(readOnly = true) public List<Seat> allSeats() { return seats.findByLibraryIdOrderByLabel(ctx.requireLibraryId()); }

    @Transactional
    public int addSeats(String prefix, int count, String zone) {
        if (count < 1 || count > 200) throw new BusinessException("Count must be between 1 and 200");
        Library lib = libraries.getReferenceById(ctx.requireLibraryId());
        int created = 0;
        String pre = prefix == null ? "" : prefix.trim();
        for (int i = 1; i <= count; i++) {
            String label = pre + (count == 1 && !pre.isEmpty() ? "" : String.valueOf(i));
            if (label.isBlank() || seats.existsByLibraryIdAndLabel(lib.getId(), label)) continue;
            Seat s = new Seat(); s.setLibrary(lib); s.setLabel(label); s.setZone(zone);
            seats.save(s); created++;
        }
        audit.log("SEATS_ADDED", created + " seat(s)");
        return created;
    }

    @Transactional
    public void toggle(Long seatId) {
        Seat s = seats.findByIdAndLibraryId(seatId, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (s.isActive() && bookings.existsBySeatIdAndStatusAndBookingDateGreaterThanEqual(seatId, BookingStatus.BOOKED, LocalDate.now()))
            throw new BusinessException("Seat has upcoming bookings; cancel them first");
        if (s.isActive() && allocations.countForSeatOverlapping(seatId, LocalDate.now(), LocalDate.now().plusYears(5)) > 0)
            throw new BusinessException("Seat is allocated to a subscriber; re-assign them first");
        s.setActive(!s.isActive());
    }
}
