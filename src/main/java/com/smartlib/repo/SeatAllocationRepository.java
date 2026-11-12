package com.smartlib.repo;

import com.smartlib.domain.SeatAllocation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, Long> {
    /** Active allocations in a library that overlap the [from, to] date range. */
    @Query("select a from SeatAllocation a where a.library.id = :lib and a.active = true and a.startDate <= :to and a.endDate >= :from")
    List<SeatAllocation> overlapping(@Param("lib") Long lib, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select count(a) from SeatAllocation a where a.seat.id = :seat and a.active = true and a.startDate <= :to and a.endDate >= :from")
    long countForSeatOverlapping(@Param("seat") Long seat, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select a from SeatAllocation a where a.member.id = :member and a.active = true and a.startDate <= :d and a.endDate >= :d order by a.startDate desc")
    List<SeatAllocation> currentFor(@Param("member") Long member, @Param("d") LocalDate d, Pageable pageable);

    /** Most recent allocations (any state) for a member, newest first. */
    List<SeatAllocation> findByMemberIdOrderByEndDateDesc(Long memberId, Pageable pageable);

    List<SeatAllocation> findByLibraryIdAndActiveTrueAndEndDateGreaterThanEqualOrderBySeatLabel(Long libraryId, LocalDate from);
    List<SeatAllocation> findBySubscriptionId(Long subscriptionId);
    Optional<SeatAllocation> findByIdAndLibraryId(Long id, Long libraryId);
}
