package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByLibraryIdAndHiddenFalseOrderByPinnedDescCreatedAtDesc(Long libraryId, Pageable pageable);
    Optional<Post> findByIdAndLibraryId(Long id, Long libraryId);
}
