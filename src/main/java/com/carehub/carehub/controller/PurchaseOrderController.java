package com.carehub.carehub.controller;

import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.entity.PurchaseOrder;
import com.carehub.carehub.entity.Supplier;
import com.carehub.carehub.repository.InventoryRepository;
import com.carehub.carehub.service.EmailService;
import com.carehub.carehub.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @GetMapping
    public List<PurchaseOrder> getAll() {
        return purchaseOrderService.getAll();
    }

    @GetMapping("/{id}")
    public PurchaseOrder getOne(@PathVariable Long id) {
        return purchaseOrderService.getOne(id);
    }

    @PostMapping
    public PurchaseOrder create(@Valid @RequestBody PurchaseOrder order) {
        return purchaseOrderService.create(order);
    }

    @PatchMapping("/{id}/approve")
    public PurchaseOrder approve(@PathVariable Long id) {
        PurchaseOrder order = purchaseOrderService.approve(id);
        // Fire the supplier notification email as part of approval, matching the
        // "Automatic Notifications" / Supplier Email automation feature.
        Supplier supplier = order.getSupplier();
        List<Inventory> batches = inventoryRepository.findByMedicine_MedicineId(order.getMedicine().getMedicineId());
        if (!batches.isEmpty()) {
            emailService.sendLowStockAlert(supplier, batches.get(0));
        }
        return order;
    }

    @PatchMapping("/{id}/reject")
    public PurchaseOrder reject(@PathVariable Long id) {
        return purchaseOrderService.reject(id);
    }

    @PatchMapping("/{id}/received")
    public PurchaseOrder markReceived(@PathVariable Long id) {
        return purchaseOrderService.markReceived(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        purchaseOrderService.delete(id);
    }
}
