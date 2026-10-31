package com.carehub.carehub.controller;

import com.carehub.carehub.service.AlertService;
import com.carehub.carehub.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return dashboardService.summary();
    }

    @GetMapping("/low-stock")
    public List<Map<String, Object>> lowStock() {
        return dashboardService.lowStockAlerts();
    }

    @GetMapping("/expiring")
    public List<Map<String, Object>> expiring(@RequestParam(defaultValue = "30") int days) {
        return dashboardService.expiryAlerts(days);
    }

    @GetMapping("/charts")
    public Map<String, Object> charts() {
        return dashboardService.charts();
    }
}
