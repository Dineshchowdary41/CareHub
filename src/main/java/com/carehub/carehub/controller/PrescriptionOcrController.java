package com.carehub.carehub.controller;

import com.carehub.carehub.dto.OcrPrescriptionResultDTO;
import com.carehub.carehub.entity.Prescription;
import com.carehub.carehub.service.PrescriptionOcrService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class PrescriptionOcrController {

    @Autowired
    private PrescriptionOcrService prescriptionOcrService;

    @PostMapping(value = "/prescription-ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OcrPrescriptionResultDTO scanPrescription(@RequestParam("file") MultipartFile file) {
        return prescriptionOcrService.processPrescriptionImage(file);
    }

    @PostMapping("/prescription-ocr/confirm")
    public Prescription confirmPrescription(@RequestBody ConfirmPrescriptionRequest req) {
        return prescriptionOcrService.confirmPrescription(
                req.getPatientId(),
                req.getDoctorId(),
                req.getNotes(),
                req.getItems()
        );
    }

    @Data
    public static class ConfirmPrescriptionRequest {
        private Long patientId;
        private Long doctorId;
        private String notes;
        private List<OcrPrescriptionResultDTO.DetectedMedicineItem> items;
    }
}
