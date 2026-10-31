package com.carehub.carehub.service;

import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.entity.Notification;
import com.carehub.carehub.repository.AppointmentRepository;
import com.carehub.carehub.repository.BillRepository;
import com.carehub.carehub.repository.InventoryRepository;
import com.carehub.carehub.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private AlertService alertService;

    public List<Notification> getNotificationsForRole(String role) {
        // Auto-refresh dynamic alerts
        syncAlertNotifications();

        if (role == null || role.isBlank() || "ADMIN".equalsIgnoreCase(role)) {
            return notificationRepository.findTop50ByOrderByCreatedAtDesc();
        }
        return notificationRepository.findByTargetRoleOrAll(role);
    }

    public long getUnreadCount(String role) {
        if (role == null || role.isBlank() || "ADMIN".equalsIgnoreCase(role)) {
            return notificationRepository.countByIsReadFalse();
        }
        return notificationRepository.countUnreadByRole(role);
    }

    public Notification createNotification(Notification n) {
        return notificationRepository.save(n);
    }

    public Notification markAsRead(Long id) {
        Notification n = notificationRepository.findById(id).orElse(null);
        if (n != null) {
            n.setIsRead(true);
            return notificationRepository.save(n);
        }
        return null;
    }

    @Transactional
    public void markAllAsRead(String role) {
        if (role == null || role.isBlank() || "ADMIN".equalsIgnoreCase(role)) {
            notificationRepository.markAllAsRead();
        } else {
            notificationRepository.markAllAsReadForRole(role);
        }
    }

    /**
     * Periodically or on-demand synchronizes real database state (low stock, expiring medicines,
     * today's appointments, pending bills) into system notifications.
     */
    @Transactional
    public void syncAlertNotifications() {
        // 1. Low stock alerts
        List<Inventory> lowStock = alertService.lowStock();
        for (Inventory i : lowStock) {
            String medName = i.getMedicine() != null ? i.getMedicine().getMedicineName() : "Medicine";
            String title = "Low Stock: " + medName;
            String msg = medName + " (Batch " + i.getBatchNo() + ") is below minimum level. Stock: "
                    + i.getAvailableStock() + ", Min: " + i.getMinimumStock();
            createIfNotExists(title, msg, "LOW_STOCK", "DANGER", "Pharmacist", "inventory.html");
        }

        // 2. Expiry alerts (within 30 days)
        List<Inventory> expiring = alertService.expiringWithin(30);
        for (Inventory i : expiring) {
            String medName = i.getMedicine() != null ? i.getMedicine().getMedicineName() : "Medicine";
            String title = "Expiry Warning: " + medName;
            String msg = medName + " (Batch " + i.getBatchNo() + ") expires on " + i.getExpiryDate()
                    + " (" + i.getAvailableStock() + " units remaining).";
            createIfNotExists(title, msg, "EXPIRY", "WARNING", "Pharmacist", "inventory.html");
        }

        // 3. Today's appointments
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long todayApts = appointmentRepository.countAppointmentsBetween(startOfDay, endOfDay);
        if (todayApts > 0) {
            String title = "Appointments Today";
            String msg = "There are " + todayApts + " patient appointment(s) scheduled for today.";
            createIfNotExists(title, msg, "APPOINTMENT", "INFO", "Doctor", "appointments.html");
        }

        // 4. Pending bills
        long pendingBills = billRepository.countPendingBills();
        if (pendingBills > 0) {
            String title = "Pending Invoices";
            String msg = pendingBills + " patient bill(s) are currently pending collection or payment.";
            createIfNotExists(title, msg, "BILLING", "WARNING", "Receptionist", "bills.html");
        }
    }

    private void createIfNotExists(String title, String message, String type, String severity, String targetRole, String linkUrl) {
        // Simple check to prevent duplicate flood within the day
        List<Notification> existing = notificationRepository.findTop50ByOrderByCreatedAtDesc();
        boolean exists = existing.stream().anyMatch(n -> title.equalsIgnoreCase(n.getTitle())
                && n.getCreatedAt() != null && n.getCreatedAt().toLocalDate().isEqual(LocalDate.now()));
        if (!exists) {
            Notification n = Notification.builder()
                    .title(title)
                    .message(message)
                    .type(type)
                    .severity(severity)
                    .targetRole(targetRole)
                    .linkUrl(linkUrl)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(n);
        }
    }
}
