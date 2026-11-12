package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface ForumQuestionRepository extends JpaRepository<ForumQuestion, Long> {
    List<ForumQuestion> findByLibraryIdAndHiddenFalseOrderByCreatedAtDesc(Long libraryId, Pageable pageable);
    List<ForumQuestion> findByLibraryIdAndCategoryIdAndHiddenFalseOrderByCreatedAtDesc(Long libraryId, Long categoryId, Pageable pageable);
    Optional<ForumQuestion> findByIdAndLibraryId(Long id, Long libraryId);
}
