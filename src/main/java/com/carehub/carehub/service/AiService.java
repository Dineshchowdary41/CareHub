package com.carehub.carehub.service;

import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.entity.Medicine;
import com.carehub.carehub.repository.BillItemRepository;
import com.carehub.carehub.repository.InventoryRepository;
import com.carehub.carehub.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lightweight, explainable AI/heuristic engine for CareHub's inventory
 * intelligence features. All predictions are based on real dispensing
 * history (Bill_Items over a trailing window), not a black-box model —
 * this keeps the numbers auditable, which matters in a hospital setting.
 */
@Service
public class AiService {

    private static final int USAGE_WINDOW_DAYS = 30;
    private static final int REORDER_COVER_DAYS = 30; // suggest enough stock to cover 30 more days

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private BillItemRepository billItemRepository;

    /** Average units of a medicine dispensed per day over the trailing usage window. */
    public double averageDailyUsage(Long medicineId) {
        return averageDailyUsageSince(medicineId, USAGE_WINDOW_DAYS);
    }

    /** Average units of a medicine dispensed per day over a custom trailing window of days. */
    public double averageDailyUsageSince(Long medicineId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        int totalUsed = billItemRepository.findByMedicineSince(medicineId, since).stream()
                .mapToInt(bi -> bi.getQuantity() == null ? 0 : bi.getQuantity())
                .sum();
        return totalUsed / (double) days;
    }

    private int currentStock(Long medicineId) {
        return inventoryRepository.findByMedicine_MedicineId(medicineId).stream()
                .mapToInt(i -> i.getAvailableStock() == null ? 0 : i.getAvailableStock())
                .sum();
    }

    /** AI Stock Prediction: for every medicine with usage history, predicts days until stockout and trends. */
    public List<Map<String, Object>> stockPredictions() {
        List<Medicine> medicines = medicineRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Medicine m : medicines) {
            double avgUsage30 = averageDailyUsage(m.getMedicineId());
            double avgUsage7 = averageDailyUsageSince(m.getMedicineId(), 7);
            int stock = currentStock(m.getMedicineId());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("medicineId", m.getMedicineId());
            row.put("medicineName", m.getMedicineName());
            row.put("currentStock", stock);
            row.put("averageDailyUsage", round2(avgUsage30));

            // Trend detection logic
            String trend = "Stable";
            String trendLabel = "Stable Demand ➡️";
            if (avgUsage30 > 0) {
                double ratio = avgUsage7 / avgUsage30;
                if (ratio >= 1.30) {
                    trend = "Surging";
                    trendLabel = "Surging Demand 📈";
                } else if (ratio <= 0.70) {
                    trend = "Declining";
                    trendLabel = "Declining Demand 📉";
                }
            } else if (avgUsage7 > 0) {
                trend = "Surging";
                trendLabel = "New Demand Surge 📈";
            } else {
                trend = "Stable";
                trendLabel = "No Active Demand ➡️";
            }
            row.put("trend", trend);
            row.put("trendLabel", trendLabel);

            if (avgUsage30 <= 0) {
                row.put("daysUntilOutOfStock", null);
                row.put("prediction", stock > 0 ? "Stable — no recent usage recorded" : "No stock, no recent demand");
            } else {
                int daysLeft = (int) Math.floor(stock / avgUsage30);
                row.put("daysUntilOutOfStock", daysLeft);
                row.put("prediction", "Out of Stock in " + daysLeft + " Day" + (daysLeft == 1 ? "" : "s"));
            }
            results.add(row);
        }

        // Most urgent first
        results.sort((a, b) -> {
            Object da = a.get("daysUntilOutOfStock");
            Object db = b.get("daysUntilOutOfStock");
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return Integer.compare((int) da, (int) db);
        });
        return results;
    }

    /** Smart Reorder Recommendation: suggested quantity to cover REORDER_COVER_DAYS of demand. */
    public List<Map<String, Object>> reorderRecommendations() {
        List<Map<String, Object>> results = new ArrayList<>();
        List<Inventory> lowStock = inventoryRepository.findLowStock();

        // group by medicine so multi-batch medicines aren't recommended multiple times
        Map<Long, List<Inventory>> byMedicine = lowStock.stream()
                .collect(Collectors.groupingBy(i -> i.getMedicine().getMedicineId()));

        for (Map.Entry<Long, List<Inventory>> entry : byMedicine.entrySet()) {
            Long medicineId = entry.getKey();
            Medicine medicine = entry.getValue().get(0).getMedicine();
            double avgUsage = averageDailyUsage(medicineId);
            int stock = currentStock(medicineId);
            int minimumStock = entry.getValue().stream().mapToInt(Inventory::getMinimumStock).max().orElse(0);

            int projectedDemand = (int) Math.ceil(avgUsage * REORDER_COVER_DAYS);
            int suggestedQty = Math.max(minimumStock * 2 - stock, projectedDemand - stock);
            suggestedQty = Math.max(suggestedQty, minimumStock); // never suggest less than one refill of minimum stock

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("medicineId", medicineId);
            row.put("medicineName", medicine.getMedicineName());
            row.put("currentStock", stock);
            row.put("minimumStock", minimumStock);
            row.put("averageDailyUsage", round2(avgUsage));
            row.put("suggestedReorderQuantity", suggestedQty);
            results.add(row);
        }

        results.sort((a, b) -> Integer.compare((int) b.get("suggestedReorderQuantity"), (int) a.get("suggestedReorderQuantity")));
        return results;
    }

    /** Top selling / fast-moving medicines over the usage window. */
    public List<Map<String, Object>> fastMovingMedicines(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(USAGE_WINDOW_DAYS);
        List<Object[]> usage = billItemRepository.findUsageSince(since);
        List<Map<String, Object>> results = new ArrayList<>();
        int rank = 1;
        for (Object[] row : usage) {
            if (rank > limit) break;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("medicineId", row[0]);
            m.put("medicineName", row[1]);
            m.put("unitsSoldLast30Days", ((Number) row[2]).intValue());
            results.add(m);
        }
        return results;
    }

    /** Slow-moving medicines: in the catalog but with little/no dispensing activity. */
    public List<Map<String, Object>> slowMovingMedicines() {
        LocalDateTime since = LocalDateTime.now().minusDays(USAGE_WINDOW_DAYS);
        Set<Long> soldRecently = billItemRepository.findUsageSince(since).stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toSet());

        List<Map<String, Object>> results = new ArrayList<>();
        for (Medicine m : medicineRepository.findAll()) {
            if (!soldRecently.contains(m.getMedicineId())) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("medicineId", m.getMedicineId());
                row.put("medicineName", m.getMedicineName());
                row.put("currentStock", currentStock(m.getMedicineId()));
                row.put("note", "No sales recorded in the last " + USAGE_WINDOW_DAYS + " days");
                results.add(row);
            }
        }
        return results;
    }

    private double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
