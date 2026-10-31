package com.carehub.carehub.controller;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.repository.InventoryRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryRepository repository;

    @GetMapping
    public List<Inventory> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Inventory getOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found with id " + id));
    }

    @GetMapping("/by-medicine/{medicineId}")
    public List<Inventory> getByMedicine(@PathVariable Long medicineId) {
        return repository.findByMedicine_MedicineId(medicineId);
    }

    @PostMapping
    public Inventory create(@Valid @RequestBody Inventory inventory) {
        return repository.save(inventory);
    }

    @PutMapping("/{id}")
    public Inventory update(@PathVariable Long id, @Valid @RequestBody Inventory updated) {
        Inventory existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found with id " + id));
        existing.setMedicine(updated.getMedicine());
        existing.setSupplier(updated.getSupplier());
        existing.setBatchNo(updated.getBatchNo());
        existing.setExpiryDate(updated.getExpiryDate());
        existing.setUnitPrice(updated.getUnitPrice());
        existing.setAvailableStock(updated.getAvailableStock());
        existing.setMinimumStock(updated.getMinimumStock());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Inventory record not found with id " + id);
        }
        repository.deleteById(id);
    }
}
