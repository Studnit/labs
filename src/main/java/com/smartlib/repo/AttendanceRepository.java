package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByLibraryIdAndCheckOutIsNullOrderByCheckInDesc(Long libraryId);
    long countByLibraryIdAndCheckOutIsNull(Long libraryId);
    Optional<Attendance> findFirstByUserIdAndCheckOutIsNullOrderByCheckInDesc(Long userId);
    List<Attendance> findByUserIdOrderByCheckInDesc(Long userId, Pageable pageable);
    Optional<Attendance> findByIdAndLibraryId(Long id, Long libraryId);
    List<Attendance> findByLibraryIdAndCheckInBetweenOrderByCheckInDesc(Long libraryId, LocalDateTime from, LocalDateTime to);
    long countByLibraryIdAndCheckInBetween(Long libraryId, LocalDateTime from, LocalDateTime to);
    List<Attendance> findByCheckOutIsNullAndCheckInBefore(LocalDateTime before);
    @Query("select count(distinct a.user.id) from Attendance a where a.library.id = :lib and a.checkIn between :from and :to")
    long countDistinctUsers(@Param("lib") Long lib, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select a from Attendance a where a.user.id = :user and a.checkIn < :to and (a.checkOut is null or a.checkOut > :from)")
    List<Attendance> overlappingForUser(@Param("user") Long user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select a from Attendance a where a.library.id = :lib and a.checkIn < :to and (a.checkOut is null or a.checkOut > :from)")
    List<Attendance> overlappingForLibrary(@Param("lib") Long lib, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
