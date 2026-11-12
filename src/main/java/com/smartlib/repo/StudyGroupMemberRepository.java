package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface StudyGroupMemberRepository extends JpaRepository<StudyGroupMember, Long> {
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    Optional<StudyGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    List<StudyGroupMember> findByGroupId(Long groupId);
    long countByGroupId(Long groupId);
}
