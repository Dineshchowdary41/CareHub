package com.carehub.carehub.controller;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.entity.Supplier;
import com.carehub.carehub.repository.InventoryRepository;
import com.carehub.carehub.repository.SupplierRepository;
import com.carehub.carehub.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired
    private SupplierRepository repository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public List<Supplier> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Supplier getOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id " + id));
    }

    @PostMapping
    public Supplier create(@Valid @RequestBody Supplier supplier) {
        return repository.save(supplier);
    }

    @PutMapping("/{id}")
    public Supplier update(@PathVariable Long id, @Valid @RequestBody Supplier updated) {
        Supplier existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id " + id));
        existing.setSupplierName(updated.getSupplierName());
        existing.setPhone(updated.getPhone());
        existing.setEmail(updated.getEmail());
        existing.setAddress(updated.getAddress());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Supplier not found with id " + id);
        }
        repository.deleteById(id);
    }

    /** Manually (re-)send the low-stock restock email for a given inventory batch. */
    @PostMapping("/{id}/notify/{inventoryId}")
    public ResponseEntity<?> notify(@PathVariable Long id, @PathVariable Long inventoryId) {
        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id " + id));
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found with id " + inventoryId));
        boolean sent = emailService.sendLowStockAlert(supplier, inventory);
        return ResponseEntity.ok(Map.of(
                "sent", sent,
                "message", sent ? "Notification email sent to " + supplier.getEmail()
                                 : "Could not send email (no SMTP configured or supplier has no email on file)."
        ));
    }
}
