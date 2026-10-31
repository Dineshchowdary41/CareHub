package com.carehub.carehub.controller;

import com.carehub.carehub.dto.PatientMedicalHistoryDTO;
import com.carehub.carehub.entity.MedicalHistory;
import com.carehub.carehub.service.MedicalHistoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MedicalHistoryController {

    @Autowired
    private MedicalHistoryService medicalHistoryService;

    @GetMapping("/patients/{patientId}/history")
    public PatientMedicalHistoryDTO getPatientHistory(@PathVariable Long patientId) {
        return medicalHistoryService.getPatientFullHistory(patientId);
    }

    @PostMapping("/patients/{patientId}/history")
    public MedicalHistory addRecord(@PathVariable Long patientId, @Valid @RequestBody MedicalHistory record) {
        return medicalHistoryService.addRecord(patientId, record);
    }

    @DeleteMapping("/medical-history/{historyId}")
    public void deleteRecord(@PathVariable Long historyId) {
        medicalHistoryService.deleteRecord(historyId);
    }
}
