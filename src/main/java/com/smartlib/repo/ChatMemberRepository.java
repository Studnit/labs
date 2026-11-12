package com.smartlib.repo;
import com.smartlib.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
    List<ChatMember> findByRoomId(Long roomId);
    Optional<ChatMember> findByRoomIdAndUserId(Long roomId, Long userId);
    @Query("select m from ChatMember m join fetch m.room r where m.user.id = :uid and r.library.id = :lib order by r.createdAt desc")
    List<ChatMember> findRoomsOfUser(@Param("uid") Long uid, @Param("lib") Long lib);
}
