package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByMemberIdOrderByEndDateDesc(Long memberId);
    Optional<Subscription> findFirstByMemberIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(Long memberId, SubscriptionStatus status, LocalDate date);
    List<Subscription> findByLibraryIdOrderByEndDateDesc(Long libraryId);
    List<Subscription> findByLibraryIdAndStatusAndEndDateBetweenOrderByEndDate(Long libraryId, SubscriptionStatus status, LocalDate from, LocalDate to);
    long countByLibraryIdAndStatus(Long libraryId, SubscriptionStatus status);
    long countByLibraryIdAndStatusAndEndDateBetween(Long libraryId, SubscriptionStatus status, LocalDate from, LocalDate to);
    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDate date);
    List<Subscription> findByStatusAndEndDateBetweenAndReminderSentFalse(SubscriptionStatus status, LocalDate from, LocalDate to);
    Optional<Subscription> findByIdAndLibraryId(Long id, Long libraryId);
}
