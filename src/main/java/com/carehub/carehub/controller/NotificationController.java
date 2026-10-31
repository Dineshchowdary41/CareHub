package com.carehub.carehub.controller;

import com.carehub.carehub.entity.Notification;
import com.carehub.carehub.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst()
                    .orElse("Admin");
        }
        return "Admin";
    }

    @GetMapping
    public List<Notification> getNotifications() {
        return notificationService.getNotificationsForRole(getCurrentUserRole());
    }

    @GetMapping("/unread-count")
    public Map<String, Object> getUnreadCount() {
        long count = notificationService.getUnreadCount(getCurrentUserRole());
        Map<String, Object> res = new HashMap<>();
        res.put("unreadCount", count);
        return res;
    }

    @PostMapping
    public Notification create(@RequestBody Notification notification) {
        return notificationService.createNotification(notification);
    }

    @PatchMapping("/{id}/read")
    public Notification markRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    @PatchMapping("/mark-all-read")
    public Map<String, String> markAllRead() {
        notificationService.markAllAsRead(getCurrentUserRole());
        Map<String, String> res = new HashMap<>();
        res.put("message", "All notifications marked as read");
        return res;
    }
}
