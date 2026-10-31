package com.carehub.carehub.service;

import com.carehub.carehub.dto.DemandPredictionDTO;
import com.carehub.carehub.entity.AiPrediction;
import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.entity.Medicine;
import com.carehub.carehub.repository.AiPredictionRepository;
import com.carehub.carehub.repository.BillItemRepository;
import com.carehub.carehub.repository.InventoryRepository;
import com.carehub.carehub.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DemandPredictionService {

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private BillItemRepository billItemRepository;

    @Autowired
    private AiPredictionRepository aiPredictionRepository;

    @Autowired(required = false)
    private AiClientService aiClientService;

    /**
     * Compute AI Demand Predictions for all medicines using real dispensing history and LSTM model.
     */
    @Transactional
    public List<DemandPredictionDTO> predictAllMedicines() {
        List<Medicine> medicines = medicineRepository.findAll();
        List<DemandPredictionDTO> predictions = new ArrayList<>();

        for (Medicine m : medicines) {
            DemandPredictionDTO dto = predictMedicineDemand(m);
            predictions.add(dto);

            // Persist prediction snapshot
            savePredictionSnapshot(m, dto);
        }

        // Sort by risk priority: CRITICAL > HIGH > MEDIUM > LOW
        predictions.sort(Comparator.comparingInt(this::riskPriorityScore));
        return predictions;
    }

    /**
     * Compute AI Demand Prediction for a single medicine.
     */
    public DemandPredictionDTO predictForMedicine(Long medicineId) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found with id " + medicineId));
        return predictMedicineDemand(medicine);
    }

    private DemandPredictionDTO predictMedicineDemand(Medicine m) {
        Long mId = m.getMedicineId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since30d = now.minusDays(30);
        LocalDateTime since60d = now.minusDays(60);

        int usage30d = getUsageSince(mId, since30d);
        int usage60d = getUsageSince(mId, since60d);
        int older30dUsage = Math.max(0, usage60d - usage30d);

        int currentStock = getCurrentStock(mId);

        double avgDailyUsage = round2(usage30d / 30.0);
        double avgMonthlyUsage = round2(usage60d > 0 ? (usage60d / 2.0) : (double) usage30d);

        // Build recent daily history array for LSTM sequence input (14 days)
        List<Integer> dailyHistory = new ArrayList<>();
        for (int i = 14; i >= 1; i--) {
            LocalDateTime dayStart = now.minusDays(i);
            LocalDateTime dayEnd = now.minusDays(i - 1);
            int dayQty = billItemRepository.findByMedicineSince(mId, dayStart).stream()
                    .filter(bi -> bi.getBill() != null && bi.getBill().getBillDate() != null && bi.getBill().getBillDate().isBefore(dayEnd))
                    .mapToInt(bi -> bi.getQuantity() == null ? 0 : bi.getQuantity())
                    .sum();
            dailyHistory.add(dayQty);
        }
        // If empty daily distribution but has 30d usage, distribute across 14 days
        if (dailyHistory.stream().mapToInt(Integer::intValue).sum() == 0 && usage30d > 0) {
            int dAvg = Math.max(1, (int) Math.round(usage30d / 30.0));
            dailyHistory = new ArrayList<>(Collections.nCopies(14, dAvg));
        }

        // Attempt LSTM prediction via Python AI service
        Map<String, Object> lstmResult = null;
        if (aiClientService != null) {
            lstmResult = aiClientService.predictDemandWithLstm(mId, m.getMedicineName(), currentStock, dailyHistory);
        }

        int predictedNextMonthDemand;
        Integer predicted7DayDemand = null;
        String modelName;
        String riskLevel;
        Integer daysUntilStockout;
        int recommendedStockLevel;
        String summary;
        String trend;
        String trendLabel;

        if (lstmResult != null && lstmResult.containsKey("predicted30DayDemand")) {
            // Powered by genuine LSTM neural network
            predictedNextMonthDemand = ((Number) lstmResult.get("predicted30DayDemand")).intValue();
            if (lstmResult.containsKey("predicted7DayDemand")) {
                predicted7DayDemand = ((Number) lstmResult.get("predicted7DayDemand")).intValue();
            } else {
                predicted7DayDemand = (int) Math.ceil(predictedNextMonthDemand * 7.0 / 30.0);
            }
            modelName = (String) lstmResult.getOrDefault("modelName", "LSTM-DemandNet-v1");
            riskLevel = (String) lstmResult.getOrDefault("riskLevel", "LOW");
            daysUntilStockout = lstmResult.get("daysUntilStockout") != null ?
                    ((Number) lstmResult.get("daysUntilStockout")).intValue() : null;
            recommendedStockLevel = lstmResult.containsKey("recommendedStockLevel") ?
                    ((Number) lstmResult.get("recommendedStockLevel")).intValue() :
                    Math.max((int) Math.ceil(predictedNextMonthDemand * 1.25), getMinimumStock(mId) * 2);
            summary = (String) lstmResult.getOrDefault("summary", "LSTM model forecasted future consumption trajectory.");
            trend = (String) lstmResult.getOrDefault("trend", "Stable");
            trendLabel = "Surging".equalsIgnoreCase(trend) ? "Surging (LSTM Forecast) 📈" : "Stable (LSTM Forecast) ➡️";
        } else {
            // Graceful fallback to analytical moving-average regression
            modelName = "Analytical-MovingAverage";

            double trendMultiplier = 1.0;
            if (older30dUsage > 0 && usage30d > 0) {
                double ratio = (double) usage30d / older30dUsage;
                int pctChange = (int) Math.round((ratio - 1.0) * 100);
                if (ratio >= 1.20) {
                    trend = "Surging";
                    trendLabel = "Surging (+" + pctChange + "%) 📈";
                    trendMultiplier = 1.22;
                } else if (ratio <= 0.80) {
                    trend = "Declining";
                    trendLabel = "Declining (" + pctChange + "%) 📉";
                    trendMultiplier = 0.85;
                } else {
                    trend = "Stable";
                    trendLabel = "Stable Demand ➡️";
                    trendMultiplier = 1.05;
                }
            } else if (usage30d > 0) {
                trend = "Surging";
                trendLabel = "New Demand Surge 📈";
                trendMultiplier = 1.20;
            } else {
                trend = "Stable";
                trendLabel = "No Recent Demand ➡️";
                trendMultiplier = 1.0;
            }

            if (avgMonthlyUsage > 0) {
                predictedNextMonthDemand = (int) Math.ceil(avgMonthlyUsage * trendMultiplier);
            } else {
                predictedNextMonthDemand = 0;
            }
            predicted7DayDemand = (int) Math.ceil(predictedNextMonthDemand * 7.0 / 30.0);

            if (avgDailyUsage > 0) {
                daysUntilStockout = (int) Math.floor(currentStock / avgDailyUsage);
            } else {
                daysUntilStockout = null;
            }

            if (currentStock == 0 && predictedNextMonthDemand > 0) {
                riskLevel = "CRITICAL";
            } else if (daysUntilStockout != null && daysUntilStockout <= 7) {
                riskLevel = "CRITICAL";
            } else if (currentStock < predictedNextMonthDemand * 0.5) {
                riskLevel = "HIGH";
            } else if (currentStock < predictedNextMonthDemand) {
                riskLevel = "MEDIUM";
            } else {
                riskLevel = "LOW";
            }

            int minStock = getMinimumStock(mId);
            recommendedStockLevel = Math.max(
                    (int) Math.ceil(predictedNextMonthDemand * 1.25),
                    minStock * 2
            );

            if (predictedNextMonthDemand == 0) {
                summary = currentStock > 0 ? "Adequate stock with no recent dispensing." : "Out of stock, zero historical demand.";
            } else if ("CRITICAL".equals(riskLevel)) {
                summary = "Immediate stockout risk! Predicted demand (" + predictedNextMonthDemand + ") severely exceeds stock (" + currentStock + ").";
            } else if ("HIGH".equals(riskLevel)) {
                summary = "High risk of shortage within the month. Restock recommended.";
            } else {
                summary = "Stock levels align well with predicted demand trajectory.";
            }
        }

        return DemandPredictionDTO.builder()
                .medicineId(mId)
                .medicineName(m.getMedicineName())
                .category(m.getCategory() != null ? m.getCategory() : "General")
                .unit(m.getUnit() != null ? m.getUnit() : "Units")
                .currentStock(currentStock)
                .historicalUsage30d(usage30d)
                .historicalUsage60d(usage60d)
                .averageDailyUsage(avgDailyUsage)
                .averageMonthlyUsage(avgMonthlyUsage)
                .predictedNextMonthDemand(predictedNextMonthDemand)
                .predicted7DayDemand(predicted7DayDemand)
                .modelName(modelName)
                .trend(trend)
                .trendLabel(trendLabel)
                .riskLevel(riskLevel)
                .recommendedStockLevel(recommendedStockLevel)
                .daysUntilStockout(daysUntilStockout)
                .predictionSummary(summary)
                .build();
    }

    private int getUsageSince(Long medicineId, LocalDateTime since) {
        return billItemRepository.findByMedicineSince(medicineId, since).stream()
                .mapToInt(bi -> bi.getQuantity() == null ? 0 : bi.getQuantity())
                .sum();
    }

    private int getCurrentStock(Long medicineId) {
        return inventoryRepository.findByMedicine_MedicineId(medicineId).stream()
                .mapToInt(i -> i.getAvailableStock() == null ? 0 : i.getAvailableStock())
                .sum();
    }

    private int getMinimumStock(Long medicineId) {
        return inventoryRepository.findByMedicine_MedicineId(medicineId).stream()
                .mapToInt(i -> i.getMinimumStock() == null ? 0 : i.getMinimumStock())
                .max()
                .orElse(20);
    }

    private void savePredictionSnapshot(Medicine m, DemandPredictionDTO dto) {
        AiPrediction p = AiPrediction.builder()
                .medicine(m)
                .currentStock(dto.getCurrentStock())
                .historicalUsageMonthly(dto.getHistoricalUsage30d())
                .averageDailyUsage(dto.getAverageDailyUsage())
                .predictedNextMonthDemand(dto.getPredictedNextMonthDemand())
                .predicted7DayDemand(dto.getPredicted7DayDemand())
                .modelName(dto.getModelName())
                .trendDirection(dto.getTrend())
                .riskLevel(dto.getRiskLevel())
                .recommendedStockLevel(dto.getRecommendedStockLevel())
                .daysUntilStockout(dto.getDaysUntilStockout())
                .predictedAt(LocalDateTime.now())
                .build();
        aiPredictionRepository.save(p);
    }

    private int riskPriorityScore(DemandPredictionDTO d) {
        return switch (d.getRiskLevel()) {
            case "CRITICAL" -> 1;
            case "HIGH" -> 2;
            case "MEDIUM" -> 3;
            case "LOW" -> 4;
            default -> 5;
        };
    }

    private double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
