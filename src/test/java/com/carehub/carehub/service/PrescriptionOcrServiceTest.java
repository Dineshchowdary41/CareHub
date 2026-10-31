package com.carehub.carehub.service;

import com.carehub.carehub.dto.OcrPrescriptionResultDTO;
import com.carehub.carehub.entity.Medicine;
import com.carehub.carehub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionOcrServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PrescriptionItemRepository prescriptionItemRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PrescriptionOcrService prescriptionOcrService;

    private Medicine medicineParacetamol;
    private Medicine medicineAmoxicillin;

    @BeforeEach
    void setUp() {
        medicineParacetamol = new Medicine();
        medicineParacetamol.setMedicineId(1L);
        medicineParacetamol.setMedicineName("Paracetamol");

        medicineAmoxicillin = new Medicine();
        medicineAmoxicillin.setMedicineId(2L);
        medicineAmoxicillin.setMedicineName("Amoxicillin");
    }

    @Test
    @DisplayName("Should extract text and match catalog medicines from prescription file")
    void testProcessPrescriptionOcr() throws IOException {
        when(medicineRepository.findAll()).thenReturn(List.of(medicineParacetamol, medicineAmoxicillin));

        BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "prescription.png",
                "image/png",
                baos.toByteArray()
        );

        OcrPrescriptionResultDTO result = prescriptionOcrService.processPrescriptionImage(file);

        assertNotNull(result);
        assertNotNull(result.getRawText());
        assertFalse(result.getDetectedMedicines().isEmpty());

        boolean hasParacetamol = result.getDetectedMedicines().stream()
                .anyMatch(m -> "Paracetamol".equalsIgnoreCase(m.getMedicineName()));

        assertTrue(hasParacetamol);
        verify(auditLogService, times(1)).log(eq("OCR_SCAN"), eq("OCR"), any(), any());
    }
}
