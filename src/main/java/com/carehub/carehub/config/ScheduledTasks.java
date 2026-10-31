package com.carehub.carehub.config;

import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.service.AlertService;
import com.carehub.carehub.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inventory Automation: once a day, sweep every low-stock batch and email
 * its supplier a restock request (Supplier Email automation from the
 * CareHub spec). Runs shortly after midnight; also see the on-demand
 * "Notify" button on the Suppliers page and the automatic email fired when
 * a Purchase Order is approved.
 */
@Component
public class ScheduledTasks {

    @Autowired
    private AlertService alertService;

    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 15 0 * * *") // 00:15 every day
    public void dailyLowStockDigest() {
        List<Inventory> lowStock = alertService.lowStock();
        int sent = 0;
        for (Inventory batch : lowStock) {
            if (batch.getSupplier() != null) {
                boolean ok = emailService.sendLowStockAlert(batch.getSupplier(), batch);
                if (ok) sent++;
            }
        }
        if (!lowStock.isEmpty()) {
            System.out.println("CareHub: daily low-stock sweep found " + lowStock.size()
                    + " batch(es) below minimum, sent " + sent + " supplier email(s).");
        }
    }
}
