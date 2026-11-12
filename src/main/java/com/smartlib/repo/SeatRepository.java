package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByLibraryIdOrderByLabel(Long libraryId);
    List<Seat> findByLibraryIdAndActiveTrueOrderByLabel(Long libraryId);
    Optional<Seat> findByIdAndLibraryId(Long id, Long libraryId);
    boolean existsByLibraryIdAndLabel(Long libraryId, String label);
}
