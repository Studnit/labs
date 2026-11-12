package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {
    List<ContentReport> findByLibraryIdAndStatusOrderByCreatedAtDesc(Long libraryId, ReportStatus status);
    long countByLibraryIdAndStatus(Long libraryId, ReportStatus status);
    Optional<ContentReport> findByIdAndLibraryId(Long id, Long libraryId);
}
