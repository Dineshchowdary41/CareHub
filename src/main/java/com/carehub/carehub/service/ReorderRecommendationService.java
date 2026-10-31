package com.carehub.carehub.service;

import com.carehub.carehub.dto.DemandPredictionDTO;
import com.carehub.carehub.dto.ReorderRecommendationDTO;
import com.carehub.carehub.entity.*;
import com.carehub.carehub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReorderRecommendationService {

    private static final int DEFAULT_LEAD_TIME_DAYS = 5;
    private static final int TARGET_COVER_DAYS = 30; // 30-day stock cover target

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private DemandPredictionService demandPredictionService;

    @Autowired
    private AiReorderRecommendationRepository reorderRecommendationRepository;

    @Transactional
    public List<ReorderRecommendationDTO> generateRecommendations() {
        List<Medicine> medicines = medicineRepository.findAll();
        List<ReorderRecommendationDTO> recommendations = new ArrayList<>();

        for (Medicine m : medicines) {
            DemandPredictionDTO prediction = demandPredictionService.predictForMedicine(m.getMedicineId());
            ReorderRecommendationDTO rec = evaluateMedicineReorder(m, prediction);
            if (rec != null) {
                recommendations.add(rec);
                saveRecommendationSnapshot(m, rec);
            }
        }

        // Sort: HIGH priority first, then MEDIUM, then LOW; then largest recommended qty
        recommendations.sort((a, b) -> {
            int pComp = Integer.compare(priorityScore(a.getPriority()), priorityScore(b.getPriority()));
            if (pComp != 0) return pComp;
            return Integer.compare(b.getRecommendedQuantity(), a.getRecommendedQuantity());
        });

        return recommendations;
    }

    private ReorderRecommendationDTO evaluateMedicineReorder(Medicine m, DemandPredictionDTO pred) {
        Long mId = m.getMedicineId();
        List<Inventory> batches = inventoryRepository.findByMedicine_MedicineId(mId);

        int currentStock = pred.getCurrentStock();
        int minStock = batches.stream()
                .mapToInt(i -> i.getMinimumStock() == null ? 0 : i.getMinimumStock())
                .max()
                .orElse(20);

        // Find associated supplier from latest batch or default
        Supplier supplier = batches.stream()
                .map(Inventory::getSupplier)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> supplierRepository.findAll().stream().findFirst().orElse(null));

        // Check for expiring batches in next 30 days
        LocalDate now = LocalDate.now();
        int expiringStockSoon = batches.stream()
                .filter(b -> b.getExpiryDate() != null && b.getExpiryDate().isBefore(now.plusDays(30)))
                .mapToInt(b -> b.getAvailableStock() == null ? 0 : b.getAvailableStock())
                .sum();

        int effectiveUsableStock = Math.max(0, currentStock - expiringStockSoon);
        int predictedDemand = pred.getPredictedNextMonthDemand();
        double dailyUsage = pred.getAverageDailyUsage();

        // Calculate lead time consumption
        int leadTimeDays = DEFAULT_LEAD_TIME_DAYS;
        int leadTimeDemand = (int) Math.ceil(dailyUsage * leadTimeDays);

        // Reorder decision logic
        boolean isBelowMinimum = currentStock <= minStock;
        boolean isExpiringRisk = expiringStockSoon > 0 && effectiveUsableStock < minStock;
        boolean isDemandDeficit = effectiveUsableStock < (predictedDemand + leadTimeDemand);

        if (!isBelowMinimum && !isExpiringRisk && !isDemandDeficit && currentStock > 0) {
            // Stock is adequate, no reorder needed
            return null;
        }

        // Recommended quantity calculation:
        // Target is enough stock to cover predicted demand + 25% safety stock + minimum stock buffer
        int targetInventory = Math.max(minStock * 2, (int) Math.ceil(predictedDemand * 1.25) + minStock);
        int recommendedQty = Math.max(minStock, targetInventory - effectiveUsableStock);

        // Priority and Reason determination
        String priority;
        String reorderStatus;
        StringBuilder reason = new StringBuilder();

        if (currentStock == 0 || (pred.getDaysUntilStockout() != null && pred.getDaysUntilStockout() <= 7)) {
            priority = "HIGH";
            reorderStatus = "REORDER REQUIRED";
            reason.append("Critical shortage alert: Available stock (").append(currentStock)
                    .append(") will be exhausted in ");
            if (pred.getDaysUntilStockout() != null) {
                reason.append(pred.getDaysUntilStockout()).append(" days. ");
            } else {
                reason.append("less than a week. ");
            }
        } else if (currentStock <= minStock || isExpiringRisk) {
            priority = "HIGH";
            reorderStatus = "REORDER REQUIRED";
            reason.append("Stock (").append(currentStock).append(") is at or below minimum threshold (")
                    .append(minStock).append("). ");
            if (expiringStockSoon > 0) {
                reason.append(expiringStockSoon).append(" units expire within 30 days. ");
            }
        } else if (effectiveUsableStock < predictedDemand) {
            priority = "MEDIUM";
            reorderStatus = "REORDER RECOMMENDED";
            reason.append("Predicted demand (").append(predictedDemand)
                    .append(") exceeds effective stock (").append(effectiveUsableStock)
                    .append("). Restock before next demand peak. ");
        } else {
            priority = "LOW";
            reorderStatus = "STOCK OPTIMAL";
            reason.append("Stock is currently sufficient, but proactive replenishment suggested.");
        }

        return ReorderRecommendationDTO.builder()
                .medicineId(mId)
                .medicineName(m.getMedicineName())
                .category(m.getCategory() != null ? m.getCategory() : "General")
                .supplierId(supplier != null ? supplier.getSupplierId() : null)
                .supplierName(supplier != null ? supplier.getSupplierName() : "Unassigned Supplier")
                .supplierEmail(supplier != null ? supplier.getEmail() : null)
                .currentStock(currentStock)
                .minimumStock(minStock)
                .predictedDemand(predictedDemand)
                .recommendedQuantity(recommendedQty)
                .priority(priority)
                .reason(reason.toString().trim())
                .leadTimeDays(leadTimeDays)
                .reorderStatus(reorderStatus)
                .daysUntilStockout(pred.getDaysUntilStockout())
                .build();
    }

    private void saveRecommendationSnapshot(Medicine m, ReorderRecommendationDTO rec) {
        Supplier supplier = null;
        if (rec.getSupplierId() != null) {
            supplier = supplierRepository.findById(rec.getSupplierId()).orElse(null);
        }

        AiReorderRecommendation entity = AiReorderRecommendation.builder()
                .medicine(m)
                .supplier(supplier)
                .currentStock(rec.getCurrentStock())
                .minimumStock(rec.getMinimumStock())
                .predictedDemand(rec.getPredictedDemand())
                .recommendedQuantity(rec.getRecommendedQuantity())
                .priority(rec.getPriority())
                .reason(rec.getReason())
                .status("OPEN")
                .createdAt(LocalDateTime.now())
                .build();
        reorderRecommendationRepository.save(entity);
    }

    private int priorityScore(String p) {
        return switch (p) {
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }
}
