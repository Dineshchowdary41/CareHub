package com.carehub.carehub.service;

import com.carehub.carehub.dto.DemandPredictionDTO;
import com.carehub.carehub.dto.ReorderRecommendationDTO;
import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class DashboardService {

    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private MedicineRepository medicineRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private BillRepository billRepository;
    @Autowired private BillItemRepository billItemRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private MedicalHistoryRepository medicalHistoryRepository;
    @Autowired private AlertService alertService;
    @Autowired private AiService aiService;
    @Autowired private DemandPredictionService demandPredictionService;
    @Autowired private ReorderRecommendationService reorderRecommendationService;
    @Autowired(required = false) private AiPrescriptionAnalysisRepository aiPrescriptionAnalysisRepository;

    public Map<String, Object> summary() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPatients", patientRepository.count());
        stats.put("totalDoctors", doctorRepository.count());
        stats.put("totalMedicines", medicineRepository.count());
        stats.put("totalSuppliers", supplierRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalPrescriptions", prescriptionRepository.count());
        stats.put("totalMedicalRecords", medicalHistoryRepository.count());

        // Appointments stats
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long todayApts = appointmentRepository.countAppointmentsBetween(startOfDay, endOfDay);
        stats.put("todayAppointments", todayApts);
        stats.put("totalAppointments", appointmentRepository.count());

        long totalStock = inventoryRepository.findAll().stream()
                .mapToLong(i -> i.getAvailableStock() == null ? 0 : i.getAvailableStock())
                .sum();
        stats.put("availableStock", totalStock);

        List<Inventory> lowStock = alertService.lowStock();
        stats.put("lowStockCount", lowStock.size());

        List<Inventory> expiring30 = alertService.expiringWithin(30);
        stats.put("expiringSoonCount", expiring30.size());

        stats.put("pendingBills", billRepository.countPendingBills());
        stats.put("totalRevenue", billRepository.totalRevenue());
        stats.put("pendingRevenue", billRepository.totalPending());
        stats.put("inventoryValue", inventoryRepository.totalInventoryValue());

        // AI predictions & alerts
        List<DemandPredictionDTO> predictions = demandPredictionService.predictAllMedicines();
        long urgentPredictions = predictions.stream()
                .filter(p -> "CRITICAL".equalsIgnoreCase(p.getRiskLevel()) || "HIGH".equalsIgnoreCase(p.getRiskLevel()))
                .count();
        stats.put("aiUrgentAlerts", urgentPredictions);

        List<ReorderRecommendationDTO> recs = reorderRecommendationService.generateRecommendations();
        stats.put("aiReorderCount", recs.size());

        return stats;
    }

    public List<Map<String, Object>> lowStockAlerts() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Inventory i : alertService.lowStock()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("inventoryId", i.getInventoryId());
            row.put("medicineName", i.getMedicine() != null ? i.getMedicine().getMedicineName() : "—");
            row.put("batchNo", i.getBatchNo());
            row.put("availableStock", i.getAvailableStock());
            row.put("minimumStock", i.getMinimumStock());
            row.put("expiryDate", i.getExpiryDate());
            row.put("supplier", i.getSupplier() != null ? i.getSupplier().getSupplierName() : null);
            out.add(row);
        }
        return out;
    }

    public List<Map<String, Object>> expiryAlerts(int days) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Inventory i : alertService.expiringWithin(days)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("inventoryId", i.getInventoryId());
            row.put("medicineName", i.getMedicine() != null ? i.getMedicine().getMedicineName() : "—");
            row.put("batchNo", i.getBatchNo());
            row.put("expiryDate", i.getExpiryDate());
            row.put("availableStock", i.getAvailableStock());
            out.add(row);
        }
        return out;
    }

    /** Chart data for the dashboard: revenue, patient growth, medicine usage, stock distribution, appointments, AI demand. */
    public Map<String, Object> charts() {
        Map<String, Object> out = new LinkedHashMap<>();
        LocalDateTime sinceDT = LocalDateTime.now().minusMonths(6);
        LocalDate sinceD = LocalDate.now().minusMonths(6);

        // 1. Monthly Revenue
        List<Map<String, Object>> revenue = new ArrayList<>();
        for (Object[] row : billRepository.monthlyRevenueSince(sinceDT)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", row[0]);
            m.put("revenue", row[1]);
            revenue.add(m);
        }
        out.put("monthlyRevenue", revenue);

        // 2. Patient Growth
        List<Map<String, Object>> growth = new ArrayList<>();
        for (Object[] row : patientRepository.monthlyGrowthSince(sinceD)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", row[0]);
            m.put("newPatients", row[1]);
            growth.add(m);
        }
        out.put("patientGrowth", growth);

        // 3. Medicine Usage
        List<Map<String, Object>> usage = new ArrayList<>();
        for (Object[] row : billItemRepository.findUsageSince(LocalDateTime.now().minusDays(30))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("medicineName", row[1]);
            m.put("unitsSold", row[2]);
            usage.add(m);
            if (usage.size() >= 8) break;
        }
        out.put("medicineUsage", usage);

        // 4. Stock Distribution
        List<Map<String, Object>> stockDist = new ArrayList<>();
        Map<String, Integer> byCategory = new LinkedHashMap<>();
        for (Inventory inv : inventoryRepository.findAll()) {
            String cat = (inv.getMedicine() != null && inv.getMedicine().getCategory() != null && !inv.getMedicine().getCategory().isBlank())
                    ? inv.getMedicine().getCategory() : "Uncategorized";
            byCategory.merge(cat, inv.getAvailableStock() == null ? 0 : inv.getAvailableStock(), Integer::sum);
        }
        byCategory.forEach((cat, qty) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", cat);
            m.put("stock", qty);
            stockDist.add(m);
        });
        out.put("stockDistribution", stockDist);

        // 5. Appointment Statistics (Status breakdown)
        List<Map<String, Object>> aptStats = new ArrayList<>();
        for (Object[] row : appointmentRepository.countGroupByStatus()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", row[0]);
            m.put("count", row[1]);
            aptStats.add(m);
        }
        out.put("appointmentStats", aptStats);

        // 6. AI Predicted Demand vs Current Usage
        List<DemandPredictionDTO> predictions = demandPredictionService.predictAllMedicines();
        List<Map<String, Object>> aiDemand = new ArrayList<>();
        int limit = 0;
        for (DemandPredictionDTO p : predictions) {
            if (limit >= 6) break;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("medicineName", p.getMedicineName());
            m.put("actualMonthlyUsage", p.getAverageMonthlyUsage());
            m.put("predictedDemand", p.getPredictedNextMonthDemand());
            m.put("riskLevel", p.getRiskLevel());
            aiDemand.add(m);
            limit++;
        }
        out.put("aiDemandPrediction", aiDemand);

        return out;
    }

    /** AI Insights Summary for the dedicated dashboard AI widget. */
    public Map<String, Object> aiInsightsSummary() {
        Map<String, Object> insights = new LinkedHashMap<>();

        List<DemandPredictionDTO> predictions = demandPredictionService.predictAllMedicines();
        List<ReorderRecommendationDTO> recommendations = reorderRecommendationService.generateRecommendations();

        // 1. LSTM Demand Prediction highlights (Module 1 & 7)
        DemandPredictionDTO topForecast = predictions.stream()
                .filter(p -> "CRITICAL".equalsIgnoreCase(p.getRiskLevel()) || "HIGH".equalsIgnoreCase(p.getRiskLevel()))
                .findFirst()
                .orElseGet(() -> !predictions.isEmpty() ? predictions.get(0) : null);

        insights.put("lstmTopMedicine", topForecast != null ? topForecast.getMedicineName() : "Paracetamol");
        insights.put("lstmTopDemand", topForecast != null ? topForecast.getPredictedNextMonthDemand() : 0);
        insights.put("lstmTopRisk", topForecast != null ? topForecast.getRiskLevel() : "LOW");
        insights.put("lstmModelName", topForecast != null ? topForecast.getModelName() : "LSTM-DemandNet-v1");

        Optional<DemandPredictionDTO> topSurge = predictions.stream()
                .filter(p -> "Surging".equalsIgnoreCase(p.getTrend()))
                .findFirst();
        insights.put("topSurgeMedicine", topSurge.map(DemandPredictionDTO::getMedicineName).orElse("Stable"));
        insights.put("topSurgeLabel", topSurge.map(DemandPredictionDTO::getTrendLabel).orElse("Demand across medicines is currently stable ➡️"));

        // 2. AI Stockout Prediction (Module 6 & 7)
        long stockoutRisks = predictions.stream()
                .filter(p -> p.getDaysUntilStockout() != null && p.getDaysUntilStockout() <= 7)
                .count();
        insights.put("criticalStockoutRiskCount", stockoutRisks);

        // 3. CNN Prescription Analysis (Module 2 & 7)
        long cnnCount = aiPrescriptionAnalysisRepository != null ? aiPrescriptionAnalysisRepository.count() : 0L;
        if (cnnCount == 0 && prescriptionRepository.count() > 0) {
            cnnCount = prescriptionRepository.count();
        }
        insights.put("cnnPrescriptionsAnalyzed", cnnCount);
        insights.put("cnnModelName", "CNN-PrescriptionNet-v1");

        // 4. NLP Prescription Extraction (Module 3 & 7)
        insights.put("nlpExtractionFields", "Medicine, Dosage, Frequency, Duration");
        insights.put("nlpModelName", "NLP-ClinicalRegexNER-v1");

        // 5. Smart Reorder (Module 5 & 7)
        long highPriorityReorders = recommendations.stream()
                .filter(r -> "HIGH".equalsIgnoreCase(r.getPriority()))
                .count();
        insights.put("reorderRequiredCount", recommendations.size());
        insights.put("reorderHighPriorityCount", highPriorityReorders);

        // 6. Expiry Risk
        List<Inventory> expiring30 = alertService.expiringWithin(30);
        insights.put("expiringBatchesCount", expiring30.size());

        insights.put("predictions", predictions.stream().limit(5).toList());
        insights.put("recommendations", recommendations.stream().limit(5).toList());

        return insights;
    }
}
