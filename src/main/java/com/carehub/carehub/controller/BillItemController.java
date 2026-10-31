package com.carehub.carehub.controller;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.BillItem;
import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.repository.BillItemRepository;
import com.carehub.carehub.repository.InventoryRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/bill-items")
public class BillItemController {

    @Autowired
    private BillItemRepository repository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private com.carehub.carehub.service.AuditLogService auditLogService;

    @GetMapping
    public List<BillItem> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public BillItem getOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill item not found with id " + id));
    }

    @GetMapping("/by-bill/{billId}")
    public List<BillItem> getByBill(@PathVariable Long billId) {
        return repository.findByBill_BillId(billId);
    }

    /**
     * Creating a bill item represents medicine actually being dispensed to a
     * patient, so it automatically decrements available stock — earliest
     * expiring batch first (FIFO) — which is what feeds the AI usage/stock
     * prediction engine.
     */
    @PostMapping
    @Transactional
    public BillItem create(@Valid @RequestBody BillItem item) {
        BillItem saved = repository.save(item);
        decrementStockFifo(item.getMedicine().getMedicineId(), item.getQuantity());
        auditLogService.log("DISPENSE", "Inventory",
                "Dispensed " + item.getQuantity() + " unit(s) of medicine ID " + item.getMedicine().getMedicineId() + " (Bill ID " + item.getBill().getBillId() + ")",
                String.valueOf(saved.getBillItemId()));
        return saved;
    }

    @PutMapping("/{id}")
    public BillItem update(@PathVariable Long id, @Valid @RequestBody BillItem updated) {
        BillItem existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill item not found with id " + id));
        existing.setBill(updated.getBill());
        existing.setMedicine(updated.getMedicine());
        existing.setQuantity(updated.getQuantity());
        existing.setUnitPrice(updated.getUnitPrice());
        existing.setTotalPrice(updated.getTotalPrice());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Bill item not found with id " + id);
        }
        repository.deleteById(id);
    }

    private void decrementStockFifo(Long medicineId, int quantity) {
        List<Inventory> batches = inventoryRepository.findByMedicine_MedicineId(medicineId).stream()
                .filter(b -> b.getAvailableStock() != null && b.getAvailableStock() > 0)
                .sorted(Comparator.comparing(Inventory::getExpiryDate))
                .toList();

        int remaining = quantity;
        for (Inventory batch : batches) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, batch.getAvailableStock());
            batch.setAvailableStock(batch.getAvailableStock() - take);
            inventoryRepository.save(batch);
            remaining -= take;
        }
        // If remaining > 0, stock ran short — the bill still records the dispensing
        // for billing purposes, but inventory can't go negative.
    }
}
