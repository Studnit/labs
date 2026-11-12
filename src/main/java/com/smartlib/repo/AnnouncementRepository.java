package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByLibraryIdOrderByPinnedDescCreatedAtDesc(Long libraryId, Pageable pageable);
    Optional<Announcement> findByIdAndLibraryId(Long id, Long libraryId);
}
