package com.carehub.carehub.service;

import com.carehub.carehub.entity.AuditLog;
import com.carehub.carehub.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository repository;

    public AuditLog log(String action, String module, String description, String entityId) {
        String username = "System";
        String userRole = "System";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            username = auth.getName();
            userRole = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst()
                    .orElse("User");
        }

        AuditLog entry = AuditLog.builder()
                .username(username)
                .userRole(userRole)
                .action(action)
                .module(module)
                .description(description)
                .entityId(entityId)
                .timestamp(LocalDateTime.now())
                .build();

        return repository.save(entry);
    }

    public List<AuditLog> getRecentLogs() {
        return repository.findTop200ByOrderByTimestampDesc();
    }

    public List<AuditLog> searchLogs(String module, String action, String username) {
        String mod = (module != null && !module.isBlank() && !"ALL".equalsIgnoreCase(module)) ? module : null;
        String act = (action != null && !action.isBlank() && !"ALL".equalsIgnoreCase(action)) ? action : null;
        String user = (username != null && !username.isBlank()) ? username.trim() : null;
        return repository.searchLogs(mod, act, user);
    }
}
