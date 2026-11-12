package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface LibraryRepository extends JpaRepository<Library, Long> {
    Optional<Library> findBySlug(String slug);
    Optional<Library> findByQrToken(String token);
    boolean existsBySlug(String slug);
    List<Library> findByActiveTrueOrderByName();
}
