package com.carehub.carehub.service;

import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.entity.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the "please restock" email to a supplier when a medicine dips
 * below its minimum stock level. In academic/demo environments where no
 * SMTP server is configured, the send failure is caught and logged instead
 * of blowing up the request — the important part (record-keeping / the UI
 * flow) still works.
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public boolean sendLowStockAlert(Supplier supplier, Inventory inventory) {
        if (supplier == null || supplier.getEmail() == null || supplier.getEmail().isBlank()) {
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(supplier.getEmail());
            message.setSubject("CareHub Restock Request — " + inventory.getMedicine().getMedicineName());
            message.setText(
                    "Dear " + supplier.getSupplierName() + ",\n\n" +
                    "Medicine: " + inventory.getMedicine().getMedicineName() + "\n" +
                    "Batch No: " + inventory.getBatchNo() + "\n" +
                    "Current Stock: " + inventory.getAvailableStock() + "\n" +
                    "Minimum Stock: " + inventory.getMinimumStock() + "\n\n" +
                    "Please supply new stock at your earliest convenience.\n\n" +
                    "Regards,\nCareHub"
            );
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            // No SMTP configured in this environment, or delivery failed — don't crash the request.
        	ex.printStackTrace();
            return false;
        }
    }
}
