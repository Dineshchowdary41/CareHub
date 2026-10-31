package com.carehub.carehub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    private AiClientService aiClientService;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        aiClientService = new AiClientService(restTemplateBuilder);
        ReflectionTestUtils.setField(aiClientService, "aiServiceUrl", "http://127.0.0.1:5000");
    }

    @Test
    @DisplayName("predictDemandWithLstm should return valid prediction map when Python service is UP")
    void testPredictDemandWithLstmSuccess() {
        Map<String, Object> fakeResponse = Map.of(
                "medicineName", "Paracetamol",
                "predicted7DayDemand", 125,
                "predicted30DayDemand", 520,
                "riskLevel", "HIGH",
                "modelName", "LSTM-DemandNet-v1"
        );

        when(restTemplate.postForEntity(eq("http://127.0.0.1:5000/ai/demand/predict"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(fakeResponse, HttpStatus.OK));

        Map<String, Object> result = aiClientService.predictDemandWithLstm(1L, "Paracetamol", 100, List.of(20, 25, 22));

        assertNotNull(result);
        assertEquals(520, result.get("predicted30DayDemand"));
        assertEquals("LSTM-DemandNet-v1", result.get("modelName"));
    }

    @Test
    @DisplayName("predictDemandWithLstm should handle connection timeout gracefully and return null")
    void testPredictDemandWithLstmTimeoutFallback() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        Map<String, Object> result = aiClientService.predictDemandWithLstm(1L, "Paracetamol", 100, List.of(20, 25, 22));

        assertNull(result); // Triggers graceful internal analytical fallback
    }

    @Test
    @DisplayName("analyzePrescription should return CNN+OCR+NLP result when Python service is UP")
    void testAnalyzePrescriptionSuccess() {
        Map<String, Object> fakeResponse = Map.of(
                "success", true,
                "cnn", Map.of("predictedClass", "VALID_PRESCRIPTION_PRINTED", "imageQuality", "GOOD"),
                "nlp", Map.of("modelName", "NLP-ClinicalRegexNER-v1"),
                "detectedMedicines", List.of(Map.of("medicineName", "Amoxicillin", "dosage", "500 mg"))
        );

        when(restTemplate.postForEntity(eq("http://127.0.0.1:5000/ai/prescription/analyze"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(fakeResponse, HttpStatus.OK));

        MockMultipartFile file = new MockMultipartFile("file", "rx.png", "image/png", new byte[]{1, 2, 3});
        Map<String, Object> result = aiClientService.analyzePrescription(file);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("cnn"));
    }

    @Test
    @DisplayName("analyzePrescription should handle service failure gracefully and return null")
    void testAnalyzePrescriptionFailureFallback() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Service Unavailable"));

        MockMultipartFile file = new MockMultipartFile("file", "rx.png", "image/png", new byte[]{1, 2, 3});
        Map<String, Object> result = aiClientService.analyzePrescription(file);

        assertNull(result); // Triggers graceful local regex parsing fallback
    }
}
