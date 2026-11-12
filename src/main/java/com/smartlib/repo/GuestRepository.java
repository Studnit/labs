package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    List<Guest> findByLibraryIdOrderByCheckInDesc(Long libraryId, Pageable pageable);
    List<Guest> findByLibraryIdAndCheckInBetweenOrderByCheckInDesc(Long libraryId, LocalDateTime from, LocalDateTime to);
    long countByLibraryIdAndCheckInBetween(Long libraryId, LocalDateTime from, LocalDateTime to);
    Optional<Guest> findByIdAndLibraryId(Long id, Long libraryId);
}
