package com.carehub.carehub.repository;

import com.carehub.carehub.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop200ByOrderByTimestampDesc();

    List<AuditLog> findByModuleOrderByTimestampDesc(String module);

    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:module IS NULL OR a.module = :module) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> searchLogs(@Param("module") String module,
                             @Param("action") String action,
                             @Param("username") String username);
}
