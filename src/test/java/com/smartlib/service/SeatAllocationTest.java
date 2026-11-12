package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.*;
import com.smartlib.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Seat is auto-allocated on subscription: previous seat first, else first free seat, else staff are told. */
class SeatAllocationTest {
    SeatRepository seats = mock(SeatRepository.class);
    SeatBookingRepository bookings = mock(SeatBookingRepository.class);
    SeatAllocationRepository allocations = mock(SeatAllocationRepository.class);
    NotificationService notifications = mock(NotificationService.class);
    FeatureService features = mock(FeatureService.class);
    SeatService svc;
    Library lib = new Library(); AppUser member = new AppUser(); Subscription sub = new Subscription();
    Seat a1 = seat(1L, "A1"), a2 = seat(2L, "A2"), a3 = seat(3L, "A3");

    static Seat seat(Long id, String label) { Seat s = new Seat(); s.setId(id); s.setLabel(label); return s; }

    @BeforeEach void setUp() {
        lib.setId(10L); member.setId(5L); member.setLibrary(lib); member.setFullName("Asha");
        sub.setId(99L); sub.setLibrary(lib); sub.setMember(member);
        sub.setStartDate(LocalDate.now()); sub.setEndDate(LocalDate.now().plusDays(29));
        a1.setLibrary(lib); a2.setLibrary(lib); a3.setLibrary(lib);
        when(features.enabled(10L, Feature.SEAT_BOOKING)).thenReturn(true);
        when(seats.findByLibraryIdAndActiveTrueOrderByLabel(10L)).thenReturn(List.of(a1, a2, a3));
        when(allocations.findBySubscriptionId(99L)).thenReturn(List.of());
        when(allocations.save(any())).thenAnswer(i -> i.getArgument(0));
        svc = new SeatService(seats, bookings, allocations, notifications, mock(LibraryRepository.class), mock(AppUserRepository.class),
                mock(SubscriptionService.class), features, mock(AuditService.class), mock(CurrentUser.class));
    }
    private SeatAllocation allocated() {
        ArgumentCaptor<SeatAllocation> c = ArgumentCaptor.forClass(SeatAllocation.class);
        verify(allocations).save(c.capture());
        return c.getValue();
    }

    @Test void firstFreeSeatWhenNoHistory() {
        when(allocations.findByMemberIdOrderByEndDateDesc(eq(5L), any())).thenReturn(List.of());
        svc.onSubscriptionActivated(new SubscriptionEvents.Activated(sub));
        assertEquals("A1", allocated().getSeat().getLabel());
    }
    @Test void previousSeatIsPreferredWhenFree() {
        SeatAllocation old = new SeatAllocation(); old.setSeat(a3); old.setMember(member);
        when(allocations.findByMemberIdOrderByEndDateDesc(eq(5L), any())).thenReturn(List.of(old));
        svc.onSubscriptionActivated(new SubscriptionEvents.Activated(sub));
        assertEquals("A3", allocated().getSeat().getLabel());
    }
    @Test void previousSeatTakenFallsBackToFirstFree() {
        SeatAllocation old = new SeatAllocation(); old.setSeat(a3); old.setMember(member);
        when(allocations.findByMemberIdOrderByEndDateDesc(eq(5L), any())).thenReturn(List.of(old));
        when(allocations.countForSeatOverlapping(eq(3L), any(), any())).thenReturn(1L);   // A3 now allocated to someone else
        when(allocations.countForSeatOverlapping(eq(1L), any(), any())).thenReturn(1L);   // A1 also taken
        svc.onSubscriptionActivated(new SubscriptionEvents.Activated(sub));
        assertEquals("A2", allocated().getSeat().getLabel());
    }
    @Test void seatWithOtherMembersBookingIsNotFree() {
        when(allocations.findByMemberIdOrderByEndDateDesc(eq(5L), any())).thenReturn(List.of());
        when(bookings.countOthersBetween(eq(1L), any(), any(), any(), eq(5L))).thenReturn(1L);
        svc.onSubscriptionActivated(new SubscriptionEvents.Activated(sub));
        assertEquals("A2", allocated().getSeat().getLabel());
    }
    @Test void libraryFullNotifiesStaffAndAllocatesNothing() {
        when(allocations.findByMemberIdOrderByEndDateDesc(eq(5L), any())).thenReturn(List.of());
        when(allocations.countForSeatOverlapping(anyLong(), any(), any())).thenReturn(1L);
        svc.onSubscriptionActivated(new SubscriptionEvents.Activated(sub));
        verify(allocations, never()).save(any());
        verify(notifications).notifyStaff(eq(lib), anyString(), contains("Asha"), anyString());
    }
    @Test void moduleOffMeansNoAllocation() {
        when(features.enabled(10L, Feature.SEAT_BOOKING)).thenReturn(false);
        svc.onSubscriptionActivated(new SubscriptionEvents.Activated(sub));
        verifyNoInteractions(allocations);
    }
    @Test void cancelReleasesSeat() {
        SeatAllocation al = new SeatAllocation(); al.setActive(true);
        when(allocations.findBySubscriptionId(99L)).thenReturn(List.of(al));
        svc.onSubscriptionCancelled(new SubscriptionEvents.Cancelled(sub));
        assertFalse(al.isActive());
    }
}
