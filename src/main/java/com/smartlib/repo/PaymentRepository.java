package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByLibraryIdOrderByCreatedAtDesc(Long libraryId, Pageable pageable);
    List<Payment> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<Payment> findByIdAndLibraryId(Long id, Long libraryId);
    long countByLibraryIdAndStatus(Long libraryId, PaymentStatus status);
    List<Payment> findByLibraryIdAndStatusAndPaidAtBetweenOrderByPaidAtDesc(Long libraryId, PaymentStatus status, LocalDateTime from, LocalDateTime to);
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.library.id = :lib and p.status = :status and p.paidAt between :from and :to")
    BigDecimal sumPaid(@Param("lib") Long lib, @Param("status") PaymentStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
