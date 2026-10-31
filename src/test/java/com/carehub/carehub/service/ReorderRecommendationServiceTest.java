package com.carehub.carehub.service;

import com.carehub.carehub.dto.DemandPredictionDTO;
import com.carehub.carehub.dto.ReorderRecommendationDTO;
import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.entity.Medicine;
import com.carehub.carehub.entity.Supplier;
import com.carehub.carehub.repository.AiReorderRecommendationRepository;
import com.carehub.carehub.repository.InventoryRepository;
import com.carehub.carehub.repository.MedicineRepository;
import com.carehub.carehub.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReorderRecommendationServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private DemandPredictionService demandPredictionService;

    @Mock
    private AiReorderRecommendationRepository reorderRecommendationRepository;

    @InjectMocks
    private ReorderRecommendationService reorderRecommendationService;

    private Medicine testMedicine;
    private Inventory testBatch;

    @BeforeEach
    void setUp() {
        testMedicine = new Medicine();
        testMedicine.setMedicineId(1L);
        testMedicine.setMedicineName("Amoxicillin 500mg");
        testMedicine.setCategory("Antibiotics");
        testMedicine.setUnit("Capsules");

        Supplier supplier = new Supplier();
        supplier.setSupplierId(1L);
        supplier.setSupplierName("Apex Pharma");

        testBatch = new Inventory();
        testBatch.setInventoryId(10L);
        testBatch.setMedicine(testMedicine);
        testBatch.setSupplier(supplier);
        testBatch.setAvailableStock(20);
        testBatch.setMinimumStock(50);
        testBatch.setExpiryDate(LocalDate.now().plusMonths(6));
    }

    @Test
    @DisplayName("Should recommend reorder with HIGH priority when stock is below minimum and demand is high")
    void testReorderRecommendationHighPriority() {
        when(medicineRepository.findAll()).thenReturn(List.of(testMedicine));
        when(inventoryRepository.findByMedicine_MedicineId(1L)).thenReturn(List.of(testBatch));

        DemandPredictionDTO prediction = DemandPredictionDTO.builder()
                .medicineId(1L)
                .medicineName("Amoxicillin 500mg")
                .currentStock(20)
                .predictedNextMonthDemand(150)
                .averageMonthlyUsage(120.0)
                .averageDailyUsage(4.0)
                .riskLevel("CRITICAL")
                .build();
        when(demandPredictionService.predictForMedicine(1L)).thenReturn(prediction);

        List<ReorderRecommendationDTO> recommendations = reorderRecommendationService.generateRecommendations();

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        ReorderRecommendationDTO rec = recommendations.get(0);
        assertEquals("HIGH", rec.getPriority());
        assertEquals("REORDER REQUIRED", rec.getReorderStatus());
        assertTrue(rec.getRecommendedQuantity() > 0);
        assertEquals("Apex Pharma", rec.getSupplierName());
        verify(reorderRecommendationRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Should flag expiring batches in the recommendation reason")
    void testReorderRecommendationExpiringBatch() {
        testBatch.setExpiryDate(LocalDate.now().plusDays(10)); // expires within 30 days
        testBatch.setAvailableStock(100);
        testBatch.setMinimumStock(50);

        when(medicineRepository.findAll()).thenReturn(List.of(testMedicine));
        when(inventoryRepository.findByMedicine_MedicineId(1L)).thenReturn(List.of(testBatch));

        DemandPredictionDTO prediction = DemandPredictionDTO.builder()
                .medicineId(1L)
                .medicineName("Amoxicillin 500mg")
                .currentStock(100)
                .predictedNextMonthDemand(60)
                .averageMonthlyUsage(50.0)
                .averageDailyUsage(1.7)
                .riskLevel("LOW")
                .build();
        when(demandPredictionService.predictForMedicine(1L)).thenReturn(prediction);

        List<ReorderRecommendationDTO> recommendations = reorderRecommendationService.generateRecommendations();

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        ReorderRecommendationDTO rec = recommendations.get(0);
        assertTrue(rec.getReason().contains("expire within 30 days"));
    }
}
