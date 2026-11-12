package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface BookIssueRepository extends JpaRepository<BookIssue, Long> {
    List<BookIssue> findByLibraryIdAndReturnedAtIsNullOrderByDueDate(Long libraryId);
    List<BookIssue> findByMemberIdOrderByIssuedAtDesc(Long memberId);
    List<BookIssue> findByMemberIdAndReturnedAtIsNullOrderByDueDate(Long memberId);
    Optional<BookIssue> findByIdAndLibraryId(Long id, Long libraryId);
    long countByLibraryIdAndReturnedAtIsNull(Long libraryId);
    long countByMemberIdAndReturnedAtIsNull(Long memberId);
    long countByLibraryIdAndReturnedAtIsNullAndDueDateBefore(Long libraryId, LocalDate date);
    boolean existsByBookId(Long bookId);
    List<BookIssue> findByReturnedAtIsNullAndDueDateBeforeAndOverdueNotifiedFalse(LocalDate date);
    List<BookIssue> findByLibraryIdAndIssuedAtBetweenOrderByIssuedAtDesc(Long libraryId, LocalDate from, LocalDate to);
    long countByLibraryIdAndIssuedAtBetween(Long libraryId, LocalDate from, LocalDate to);
}
