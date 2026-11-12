package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIdAndLibraryId(Long id, Long libraryId);
    long countByLibraryId(Long libraryId);
    @Query("select b from Book b where b.library.id = :lib and (lower(b.title) like lower(concat('%', :q, '%')) or lower(coalesce(b.author,'')) like lower(concat('%', :q, '%')) or lower(coalesce(b.isbn,'')) like lower(concat('%', :q, '%')) or lower(coalesce(b.category,'')) like lower(concat('%', :q, '%'))) order by b.title")
    List<Book> search(@Param("lib") Long lib, @Param("q") String q);
}
