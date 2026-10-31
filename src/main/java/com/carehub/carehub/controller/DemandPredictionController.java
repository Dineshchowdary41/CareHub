package com.carehub.carehub.controller;

import com.carehub.carehub.dto.DemandPredictionDTO;
import com.carehub.carehub.service.DemandPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class DemandPredictionController {

    @Autowired
    private DemandPredictionService demandPredictionService;

    @GetMapping("/demand-predictions")
    public List<DemandPredictionDTO> getAllPredictions() {
        return demandPredictionService.predictAllMedicines();
    }

    @GetMapping("/demand-predictions/{medicineId}")
    public DemandPredictionDTO getPredictionForMedicine(@PathVariable Long medicineId) {
        return demandPredictionService.predictForMedicine(medicineId);
    }
}
