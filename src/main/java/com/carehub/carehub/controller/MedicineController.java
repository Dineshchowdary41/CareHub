package com.carehub.carehub.controller;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.entity.Medicine;
import com.carehub.carehub.repository.InventoryRepository;
import com.carehub.carehub.repository.MedicineRepository;
import com.carehub.carehub.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    @Autowired
    private MedicineRepository repository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public List<Medicine> getAll() {
        return repository.findAll();
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return repository.findDistinctCategories();
    }

    @GetMapping("/search")
    public List<Map<String, Object>> searchMedicines(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String direction) {

        String q = (query != null && !query.isBlank()) ? query.trim() : null;
        String cat = (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) ? category.trim() : null;

        List<Medicine> matched = (q != null || cat != null) ?
                repository.searchMedicines(q, cat) : repository.findAll();

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> enrichedList = new ArrayList<>();

        for (Medicine m : matched) {
            List<Inventory> batches = inventoryRepository.findByMedicine_MedicineId(m.getMedicineId());
            int totalStock = batches.stream().mapToInt(b -> b.getAvailableStock() == null ? 0 : b.getAvailableStock()).sum();
            int minStock = batches.stream().mapToInt(b -> b.getMinimumStock() == null ? 0 : b.getMinimumStock()).max().orElse(0);

            LocalDate earliestExpiry = batches.stream()
                    .map(Inventory::getExpiryDate)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            String batchNumbers = batches.stream()
                    .map(Inventory::getBatchNo)
                    .filter(Objects::nonNull)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("—");

            boolean isExpired = earliestExpiry != null && earliestExpiry.isBefore(today);
            boolean isExpiringSoon = earliestExpiry != null && !isExpired && earliestExpiry.isBefore(today.plusDays(30));
            boolean isLowStock = totalStock <= minStock;
            boolean isAvailable = totalStock > 0;

            // Apply status filter
            if ("AVAILABLE".equalsIgnoreCase(status) && !isAvailable) continue;
            if ("LOW_STOCK".equalsIgnoreCase(status) && !isLowStock) continue;
            if ("EXPIRING_SOON".equalsIgnoreCase(status) && !isExpiringSoon) continue;
            if ("EXPIRED".equalsIgnoreCase(status) && !isExpired) continue;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("medicineId", m.getMedicineId());
            map.put("medicineName", m.getMedicineName());
            map.put("category", m.getCategory() != null ? m.getCategory() : "General");
            map.put("unit", m.getUnit() != null ? m.getUnit() : "Units");
            map.put("description", m.getDescription());
            map.put("availableStock", totalStock);
            map.put("minimumStock", minStock);
            map.put("earliestExpiry", earliestExpiry);
            map.put("batchNumbers", batchNumbers);
            map.put("isLowStock", isLowStock);
            map.put("isExpiringSoon", isExpiringSoon);
            map.put("isExpired", isExpired);

            enrichedList.add(map);
        }

        // Apply sorting
        boolean desc = "desc".equalsIgnoreCase(direction);
        enrichedList.sort((a, b) -> {
            int res;
            switch (sortBy.toLowerCase()) {
                case "quantity" -> res = Integer.compare((int) a.get("availableStock"), (int) b.get("availableStock"));
                case "category" -> res = String.valueOf(a.get("category")).compareToIgnoreCase(String.valueOf(b.get("category")));
                case "expiry" -> {
                    LocalDate d1 = (LocalDate) a.get("earliestExpiry");
                    LocalDate d2 = (LocalDate) b.get("earliestExpiry");
                    if (d1 == null && d2 == null) res = 0;
                    else if (d1 == null) res = 1;
                    else if (d2 == null) res = -1;
                    else res = d1.compareTo(d2);
                }
                default -> res = String.valueOf(a.get("medicineName")).compareToIgnoreCase(String.valueOf(b.get("medicineName")));
            }
            return desc ? -res : res;
        });

        return enrichedList;
    }

    @GetMapping("/{id}")
    public Medicine getOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id " + id));
    }

    @PostMapping
    public Medicine create(@Valid @RequestBody Medicine medicine) {
        Medicine saved = repository.save(medicine);
        auditLogService.log("CREATE", "Medicine", "Medicine added: " + saved.getMedicineName(), String.valueOf(saved.getMedicineId()));
        return saved;
    }

    @PutMapping("/{id}")
    public Medicine update(@PathVariable Long id, @Valid @RequestBody Medicine updated) {
        Medicine existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id " + id));
        existing.setMedicineName(updated.getMedicineName());
        existing.setCategory(updated.getCategory());
        existing.setUnit(updated.getUnit());
        existing.setDescription(updated.getDescription());
        Medicine saved = repository.save(existing);
        auditLogService.log("UPDATE", "Medicine", "Medicine updated: " + saved.getMedicineName(), String.valueOf(saved.getMedicineId()));
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Medicine m = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id " + id));
        repository.deleteById(id);
        auditLogService.log("DELETE", "Medicine", "Medicine deleted: " + m.getMedicineName(), String.valueOf(id));
    }
}
