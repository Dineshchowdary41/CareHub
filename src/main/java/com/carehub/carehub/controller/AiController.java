package com.carehub.carehub.controller;

import com.carehub.carehub.dto.ReorderRecommendationDTO;
import com.carehub.carehub.service.AiService;
import com.carehub.carehub.service.DashboardService;
import com.carehub.carehub.service.ReorderRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private ReorderRecommendationService reorderRecommendationService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired(required = false)
    private com.carehub.carehub.service.AiClientService aiClientService;

    @GetMapping("/models/info")
    public Map<String, Object> modelsInfo() {
        if (aiClientService != null) {
            return aiClientService.getModelsInfo();
        }
        return Map.of("status", "STANDBY", "message", "AiClientService bean not initialized");
    }

    @GetMapping("/stock-predictions")
    public List<Map<String, Object>> stockPredictions() {
        return aiService.stockPredictions();
    }

    @GetMapping("/reorder-recommendations")
    public List<ReorderRecommendationDTO> reorderRecommendations() {
        return reorderRecommendationService.generateRecommendations();
    }

    @GetMapping("/smart-reorder")
    public List<ReorderRecommendationDTO> smartReorder() {
        return reorderRecommendationService.generateRecommendations();
    }

    @GetMapping("/insights")
    public Map<String, Object> aiInsights() {
        return dashboardService.aiInsightsSummary();
    }

    @GetMapping("/fast-moving")
    public List<Map<String, Object>> fastMoving(@RequestParam(defaultValue = "10") int limit) {
        return aiService.fastMovingMedicines(limit);
    }

    @GetMapping("/slow-moving")
    public List<Map<String, Object>> slowMoving() {
        return aiService.slowMovingMedicines();
    }
}
