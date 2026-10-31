package com.carehub.carehub.repository;

import com.carehub.carehub.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByOrderByCreatedAtDesc();

    @Query("SELECT n FROM Notification n WHERE (n.targetRole IS NULL OR n.targetRole = 'ALL' OR UPPER(n.targetRole) = UPPER(:role)) ORDER BY n.createdAt DESC")
    List<Notification> findByTargetRoleOrAll(@Param("role") String role);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isRead = false AND (n.targetRole IS NULL OR n.targetRole = 'ALL' OR UPPER(n.targetRole) = UPPER(:role))")
    long countUnreadByRole(@Param("role") String role);

    long countByIsReadFalse();

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE (n.targetRole IS NULL OR n.targetRole = 'ALL' OR UPPER(n.targetRole) = UPPER(:role))")
    void markAllAsReadForRole(@Param("role") String role);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true")
    void markAllAsRead();
}
