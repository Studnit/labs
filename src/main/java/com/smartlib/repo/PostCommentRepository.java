package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findByPostIdAndHiddenFalseOrderByCreatedAt(Long postId);
    long countByPostIdAndHiddenFalse(Long postId);
}
