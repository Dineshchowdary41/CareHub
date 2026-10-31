package com.carehub.carehub.service;

import com.carehub.carehub.dto.DemandPredictionDTO;
import com.carehub.carehub.entity.BillItem;
import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.entity.Medicine;
import com.carehub.carehub.repository.AiPredictionRepository;
import com.carehub.carehub.repository.BillItemRepository;
import com.carehub.carehub.repository.InventoryRepository;
import com.carehub.carehub.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandPredictionServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private BillItemRepository billItemRepository;

    @Mock
    private AiPredictionRepository aiPredictionRepository;

    @InjectMocks
    private DemandPredictionService demandPredictionService;

    private Medicine testMedicine;

    @BeforeEach
    void setUp() {
        testMedicine = new Medicine();
        testMedicine.setMedicineId(1L);
        testMedicine.setMedicineName("Paracetamol 500mg");
        testMedicine.setCategory("Analgesics");
        testMedicine.setUnit("Tablets");
    }

    @Test
    @DisplayName("Should generate demand prediction and classify as CRITICAL when stock is 0 and usage is high")
    void testPredictDemandCriticalShortage() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        
        Inventory inv = new Inventory();
        inv.setAvailableStock(0);
        inv.setMinimumStock(50);
        when(inventoryRepository.findByMedicine_MedicineId(1L)).thenReturn(List.of(inv));

        BillItem b1 = new BillItem();
        b1.setQuantity(120);
        BillItem b2 = new BillItem();
        b2.setQuantity(100);

        when(billItemRepository.findByMedicineSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(b1), List.of(b1, b2));

        DemandPredictionDTO dto = demandPredictionService.predictForMedicine(1L);

        assertNotNull(dto);
        assertEquals("Paracetamol 500mg", dto.getMedicineName());
        assertEquals(0, dto.getCurrentStock());
        assertTrue(dto.getPredictedNextMonthDemand() > 0);
        assertEquals("CRITICAL", dto.getRiskLevel());
    }

    @Test
    @DisplayName("Should classify as LOW when stock significantly exceeds predicted demand")
    void testPredictDemandLowRisk() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        
        Inventory inv = new Inventory();
        inv.setAvailableStock(500);
        inv.setMinimumStock(50);
        when(inventoryRepository.findByMedicine_MedicineId(1L)).thenReturn(List.of(inv));

        BillItem b1 = new BillItem();
        b1.setQuantity(20);

        when(billItemRepository.findByMedicineSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(b1), List.of(b1));

        DemandPredictionDTO dto = demandPredictionService.predictForMedicine(1L);

        assertNotNull(dto);
        assertEquals(500, dto.getCurrentStock());
        assertEquals("LOW", dto.getRiskLevel());
    }

    @Test
    @DisplayName("Should handle medicines with zero historical dispensing gracefully")
    void testPredictDemandZeroHistory() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        
        Inventory inv = new Inventory();
        inv.setAvailableStock(50);
        inv.setMinimumStock(20);
        when(inventoryRepository.findByMedicine_MedicineId(1L)).thenReturn(List.of(inv));

        when(billItemRepository.findByMedicineSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList(), Collections.emptyList());

        DemandPredictionDTO dto = demandPredictionService.predictForMedicine(1L);

        assertNotNull(dto);
        assertEquals(0.0, dto.getAverageMonthlyUsage());
        assertEquals(0, dto.getPredictedNextMonthDemand());
    }
}
