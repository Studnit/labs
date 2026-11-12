package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface SeatBookingRepository extends JpaRepository<SeatBooking, Long> {
    @Query("select b from SeatBooking b where b.library.id = :lib and b.bookingDate = :d and b.status = :status and b.startTime < :end and b.endTime > :start")
    List<SeatBooking> overlapping(@Param("lib") Long lib, @Param("d") LocalDate d, @Param("status") BookingStatus status, @Param("start") LocalTime start, @Param("end") LocalTime end);
    @Query("select count(b) from SeatBooking b where b.member.id = :member and b.bookingDate = :d and b.status = :status and b.startTime < :end and b.endTime > :start")
    long countMemberOverlapping(@Param("member") Long member, @Param("d") LocalDate d, @Param("status") BookingStatus status, @Param("start") LocalTime start, @Param("end") LocalTime end);
    List<SeatBooking> findByMemberIdOrderByBookingDateDescStartTimeDesc(Long memberId, Pageable pageable);
    List<SeatBooking> findByLibraryIdAndBookingDateGreaterThanEqualAndStatusOrderByBookingDateAscStartTimeAsc(Long libraryId, LocalDate from, BookingStatus status);
    Optional<SeatBooking> findByIdAndLibraryId(Long id, Long libraryId);
    boolean existsBySeatIdAndStatusAndBookingDateGreaterThanEqual(Long seatId, BookingStatus status, LocalDate date);
    @Query("select count(b) from SeatBooking b where b.seat.id = :seat and b.status = :status and b.bookingDate between :from and :to and b.member.id <> :member")
    long countOthersBetween(@Param("seat") Long seat, @Param("status") BookingStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("member") Long member);
}
