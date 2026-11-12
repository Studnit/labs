package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<AppUser> findByIdAndLibraryId(Long id, Long libraryId);
    List<AppUser> findByLibraryIdAndRoleInAndStatus(Long libraryId, Collection<Role> roles, UserStatus status);
    long countByLibraryIdAndRoleAndStatus(Long libraryId, Role role, UserStatus status);
    long countByLibraryIdAndStatus(Long libraryId, UserStatus status);
    List<AppUser> findByLibraryIdAndStatus(Long libraryId, UserStatus status);
    List<AppUser> findByLibraryIdAndRole(Long libraryId, Role role);
    @Query("select u from AppUser u where u.library.id = :lib and " +
           "(lower(u.fullName) like lower(concat('%', :q, '%')) or lower(u.email) like lower(concat('%', :q, '%')) or lower(coalesce(u.memberCode,'')) like lower(concat('%', :q, '%'))) " +
           "order by u.fullName")
    List<AppUser> search(@Param("lib") Long lib, @Param("q") String q);
    @Query("select u from AppUser u where u.library.id = :lib and u.role in :roles and u.status = :status and lower(u.fullName) like lower(concat('%', :q, '%')) order by u.fullName")
    List<AppUser> searchByRoles(@Param("lib") Long lib, @Param("q") String q, @Param("roles") Collection<Role> roles, @Param("status") UserStatus status);
    @Query("select count(u) from AppUser u where u.library.id = :lib and u.role = :role and u.createdAt between :from and :to")
    long countJoined(@Param("lib") Long lib, @Param("role") Role role, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @org.springframework.data.jpa.repository.Query("select u.status from AppUser u where u.id = :id")
    Optional<com.smartlib.domain.UserStatus> statusOf(@org.springframework.data.repository.query.Param("id") Long id);
}
