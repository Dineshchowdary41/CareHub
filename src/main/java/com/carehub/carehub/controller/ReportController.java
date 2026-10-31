package com.carehub.carehub.controller;

import com.carehub.carehub.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    private ResponseEntity<byte[]> file(byte[] bytes, String filename, MediaType type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).contentType(type).body(bytes);
    }

    @GetMapping("/inventory/pdf")
    public ResponseEntity<byte[]> inventoryPdf() throws Exception {
        return file(reportService.inventoryPdf(), "carehub-inventory-report.pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/inventory/excel")
    public ResponseEntity<byte[]> inventoryExcel() throws Exception {
        return file(reportService.inventoryExcel(), "carehub-inventory-report.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/patients/pdf")
    public ResponseEntity<byte[]> patientsPdf() throws Exception {
        return file(reportService.patientsPdf(), "carehub-patients-report.pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/patients/excel")
    public ResponseEntity<byte[]> patientsExcel() throws Exception {
        return file(reportService.patientsExcel(), "carehub-patients-report.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/doctors/pdf")
    public ResponseEntity<byte[]> doctorsPdf() throws Exception {
        return file(reportService.doctorsPdf(), "carehub-doctors-report.pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/doctors/excel")
    public ResponseEntity<byte[]> doctorsExcel() throws Exception {
        return file(reportService.doctorsExcel(), "carehub-doctors-report.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/revenue/pdf")
    public ResponseEntity<byte[]> revenuePdf() throws Exception {
        return file(reportService.revenuePdf(), "carehub-revenue-report.pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/revenue/excel")
    public ResponseEntity<byte[]> revenueExcel() throws Exception {
        return file(reportService.revenueExcel(), "carehub-revenue-report.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/appointments/pdf")
    public ResponseEntity<byte[]> appointmentsPdf() throws Exception {
        return file(reportService.appointmentsPdf(), "carehub-appointments-report.pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/appointments/excel")
    public ResponseEntity<byte[]> appointmentsExcel() throws Exception {
        return file(reportService.appointmentsExcel(), "carehub-appointments-report.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/ai-demand/pdf")
    public ResponseEntity<byte[]> aiDemandPdf() throws Exception {
        return file(reportService.aiDemandPdf(), "carehub-ai-demand-report.pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/ai-demand/excel")
    public ResponseEntity<byte[]> aiDemandExcel() throws Exception {
        return file(reportService.aiDemandExcel(), "carehub-ai-demand-report.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/ai-reorder/pdf")
    public ResponseEntity<byte[]> aiReorderPdf() throws Exception {
        return file(reportService.aiReorderPdf(), "carehub-ai-reorder-report.pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/ai-reorder/excel")
    public ResponseEntity<byte[]> aiReorderExcel() throws Exception {
        return file(reportService.aiReorderExcel(), "carehub-ai-reorder-report.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
