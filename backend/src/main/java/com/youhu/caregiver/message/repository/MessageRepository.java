package com.youhu.caregiver.message.repository;

import com.youhu.caregiver.message.domain.Message;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByToUserIdOrderByCreatedAtDesc(Long toUserId, Pageable pageable);

    Page<Message> findByToCaregiverIdOrderByCreatedAtDesc(Long toCaregiverId, Pageable pageable);

    List<Message> findByToUserIdAndIsReadOrderByCreatedAtDesc(Long toUserId, Integer isRead);

    long countByToUserIdAndIsRead(Long toUserId, Integer isRead);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Message m set m.isRead = 1 where m.toUserId = :userId and m.isRead = 0")
    int markAllReadByUser(@Param("userId") Long userId);
}
