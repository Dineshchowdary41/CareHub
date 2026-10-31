package com.carehub.carehub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.*;

/**
 * REST Client communicating with the Python AI Microservice (Port 5000).
 * Connects Spring Boot to the LSTM, CNN, and NLP models with graceful fallback.
 */
@Service
public class AiClientService {

    private static final Logger log = LoggerFactory.getLogger(AiClientService.class);

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://127.0.0.1:5000}")
    private String aiServiceUrl;

    public AiClientService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(3000))
                .setReadTimeout(Duration.ofMillis(6000))
                .build();
    }

    /**
     * Checks if Python AI microservice is active and healthy.
     */
    public boolean isServiceAvailable() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(aiServiceUrl + "/health", Map.class);
            return response.getStatusCode().is2xxSuccessful() && "UP".equals(response.getBody().get("status"));
        } catch (Exception e) {
            log.debug("Python AI microservice is currently unreachable: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Module 1: Call LSTM Demand Forecasting Endpoint.
     */
    public Map<String, Object> predictDemandWithLstm(Long medicineId, String medicineName, int currentStock, List<Integer> usageHistory) {
        try {
            String url = aiServiceUrl + "/ai/demand/predict";
            Map<String, Object> request = new HashMap<>();
            request.put("medicineId", medicineId);
            request.put("medicineName", medicineName);
            request.put("currentStock", currentStock);
            request.put("usageHistory", usageHistory != null ? usageHistory : Collections.emptyList());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (Exception e) {
            log.warn("LSTM AI demand forecasting call failed for medicine '{}' ({}), falling back to analytical model: {}",
                    medicineName, medicineId, e.getMessage());
        }
        return null;
    }

    /**
     * Module 2, 3, 4: Call Unified CNN + OCR + NLP Prescription Analysis Endpoint.
     */
    public Map<String, Object> analyzePrescription(MultipartFile file) {
        try {
            String url = aiServiceUrl + "/ai/prescription/analyze";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            if (file != null && !file.isEmpty()) {
                ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename() != null ? file.getOriginalFilename() : "prescription.png";
                    }
                };
                body.add("file", resource);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (Exception e) {
            log.warn("Prescription AI analysis (CNN+OCR+NLP) call failed, falling back to local clinical regex engine: {}",
                    e.getMessage());
        }
        return null;
    }

    /**
     * Module 11 & 12: Retrieve Model Info and Real Evaluation Metrics.
     */
    public Map<String, Object> getModelsInfo() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(aiServiceUrl + "/ai/models/info", Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve AI models info from Python service: {}", e.getMessage());
        }

        // Return local architecture metadata when microservice is offline
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("timestamp", new Date().toString());
        fallback.put("serviceStatus", "OFFLINE (Analytical Engine Active)");
        fallback.put("lstm", Map.of(
                "modelName", "LSTM-DemandNet-v1",
                "task", "Medicine Demand Time-Series Forecasting",
                "status", "STANDBY (Start Python Service on Port 5000)"
        ));
        fallback.put("cnn", Map.of(
                "modelName", "CNN-PrescriptionNet-v1",
                "task", "Prescription Document & Image Quality Analysis",
                "status", "STANDBY (Start Python Service on Port 5000)"
        ));
        fallback.put("nlp", Map.of(
                "modelName", "NLP-ClinicalRegexNER-v1",
                "task", "Prescription Clinical Text Understanding",
                "status", "ACTIVE (Java Regex Engine)"
        ));
        return fallback;
    }
}
