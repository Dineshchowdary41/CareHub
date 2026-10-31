package com.carehub.carehub.controller;

import com.carehub.carehub.entity.AuditLog;
import com.carehub.carehub.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public List<AuditLog> getLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username) {
        if (module != null || action != null || username != null) {
            return auditLogService.searchLogs(module, action, username);
        }
        return auditLogService.getRecentLogs();
    }
}
