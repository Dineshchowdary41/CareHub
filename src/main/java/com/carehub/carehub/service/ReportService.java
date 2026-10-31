package com.carehub.carehub.service;

import com.carehub.carehub.dto.DemandPredictionDTO;
import com.carehub.carehub.dto.ReorderRecommendationDTO;
import com.carehub.carehub.entity.Appointment;
import com.carehub.carehub.repository.AppointmentRepository;
import com.carehub.carehub.repository.BillRepository;
import com.carehub.carehub.repository.DoctorRepository;
import com.carehub.carehub.repository.InventoryRepository;
import com.carehub.carehub.repository.PatientRepository;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private BillRepository billRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private DemandPredictionService demandPredictionService;
    @Autowired private ReorderRecommendationService reorderRecommendationService;

    private static final Font TITLE_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(13, 71, 161));

    private static final Font SUB_FONT =
            FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);

    private static final Font HEADER_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

    private static final Font CELL_FONT =
            FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

    private static final Color HEADER_BG = new Color(21, 101, 192);

    // ---------------------------------------------------------------- PDF

    private ByteArrayOutputStream newPdf(String title, String[] headers, List<String[]> rows) throws DocumentException {
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Paragraph t = new Paragraph("CareHub 3.0 — " + title, TITLE_FONT);
        document.add(t);
        Paragraph sub = new Paragraph("Generated on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")), SUB_FONT);
        sub.setSpacingAfter(16);
        document.add(sub);

        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(6);
            table.addCell(cell);
        }
        for (String[] row : rows) {
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, CELL_FONT));
                cell.setPadding(5);
                table.addCell(cell);
            }
        }
        if (rows.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No records found.", CELL_FONT));
            empty.setColspan(headers.length);
            empty.setPadding(8);
            table.addCell(empty);
        }
        document.add(table);
        document.close();
        return out;
    }

    public byte[] inventoryPdf() throws DocumentException {
        String[] headers = {"Medicine", "Batch No", "Supplier", "Expiry", "Unit Price", "Available", "Minimum"};
        List<String[]> rows = inventoryRepository.findAll().stream().map(i -> new String[]{
                i.getMedicine() != null ? i.getMedicine().getMedicineName() : "—",
                i.getBatchNo(),
                i.getSupplier() != null ? i.getSupplier().getSupplierName() : "—",
                String.valueOf(i.getExpiryDate()),
                String.valueOf(i.getUnitPrice()),
                String.valueOf(i.getAvailableStock()),
                String.valueOf(i.getMinimumStock())
        }).toList();
        return newPdf("Inventory Report", headers, rows).toByteArray();
    }

    public byte[] patientsPdf() throws DocumentException {
        String[] headers = {"Name", "DOB", "Gender", "Phone", "Blood Group", "Registered"};
        List<String[]> rows = patientRepository.findAll().stream().map(p -> new String[]{
                p.getName(), String.valueOf(p.getDateOfBirth()), p.getGender(),
                p.getPhone(), p.getBloodGroup(), String.valueOf(p.getCreatedAt())
        }).toList();
        return newPdf("Patient Report", headers, rows).toByteArray();
    }

    public byte[] doctorsPdf() throws DocumentException {
        String[] headers = {"Name", "Specialization", "Phone", "Email"};
        List<String[]> rows = doctorRepository.findAll().stream().map(d -> new String[]{
                d.getName(), d.getSpecialization(), d.getPhone(), d.getEmail()
        }).toList();
        return newPdf("Doctor Report", headers, rows).toByteArray();
    }

    public byte[] revenuePdf() throws DocumentException {
        String[] headers = {"Bill ID", "Patient", "Amount", "Status", "Date"};
        List<String[]> rows = billRepository.findAll().stream().map(b -> new String[]{
                String.valueOf(b.getBillId()),
                b.getPatient() != null ? b.getPatient().getName() : "—",
                String.valueOf(b.getTotalAmount()),
                b.getPaymentStatus(),
                String.valueOf(b.getBillDate())
        }).toList();
        return newPdf("Revenue / Billing Report", headers, rows).toByteArray();
    }

    public byte[] appointmentsPdf() throws DocumentException {
        String[] headers = {"ID", "Patient", "Doctor", "Date & Time", "Reason", "Status"};
        List<String[]> rows = appointmentRepository.findAll().stream().map(a -> new String[]{
                String.valueOf(a.getAppointmentId()),
                a.getPatient() != null ? a.getPatient().getName() : "—",
                a.getDoctor() != null ? a.getDoctor().getName() : "—",
                String.valueOf(a.getAppointmentDateTime()),
                a.getReason(),
                a.getStatus()
        }).toList();
        return newPdf("Appointments Report", headers, rows).toByteArray();
    }

    public byte[] aiDemandPdf() throws DocumentException {
        String[] headers = {"Medicine", "Stock", "Monthly Usage", "Predicted Demand", "Trend", "Risk", "Recommended Stock"};
        List<DemandPredictionDTO> list = demandPredictionService.predictAllMedicines();
        List<String[]> rows = list.stream().map(p -> new String[]{
                p.getMedicineName(),
                String.valueOf(p.getCurrentStock()),
                String.valueOf(p.getAverageMonthlyUsage()),
                String.valueOf(p.getPredictedNextMonthDemand()),
                p.getTrend(),
                p.getRiskLevel(),
                String.valueOf(p.getRecommendedStockLevel())
        }).toList();
        return newPdf("AI Demand Prediction Report", headers, rows).toByteArray();
    }

    public byte[] aiReorderPdf() throws DocumentException {
        String[] headers = {"Medicine", "Supplier", "Stock", "Min Stock", "Predicted Demand", "Reorder Qty", "Priority", "Status"};
        List<ReorderRecommendationDTO> list = reorderRecommendationService.generateRecommendations();
        List<String[]> rows = list.stream().map(r -> new String[]{
                r.getMedicineName(),
                r.getSupplierName(),
                String.valueOf(r.getCurrentStock()),
                String.valueOf(r.getMinimumStock()),
                String.valueOf(r.getPredictedDemand()),
                String.valueOf(r.getRecommendedQuantity()),
                r.getPriority(),
                r.getReorderStatus()
        }).toList();
        return newPdf("AI Smart Reorder Recommendations Report", headers, rows).toByteArray();
    }

    // -------------------------------------------------------------- Excel

    private byte[] newExcel(String sheetName, String[] headers, List<Object[]> rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Object[] r : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < r.length; c++) {
                    Cell cell = row.createCell(c);
                    Object v = r[c];
                    if (v == null) cell.setCellValue("");
                    else if (v instanceof Number n) cell.setCellValue(n.doubleValue());
                    else cell.setCellValue(String.valueOf(v));
                }
            }
            for (int c = 0; c < headers.length; c++) sheet.autoSizeColumn(c);

            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] inventoryExcel() throws Exception {
        String[] headers = {"Medicine", "Batch No", "Supplier", "Expiry", "Unit Price", "Available", "Minimum"};
        List<Object[]> rows = inventoryRepository.findAll().stream().map(i -> new Object[]{
                i.getMedicine() != null ? i.getMedicine().getMedicineName() : "—",
                i.getBatchNo(),
                i.getSupplier() != null ? i.getSupplier().getSupplierName() : "—",
                String.valueOf(i.getExpiryDate()),
                i.getUnitPrice(),
                i.getAvailableStock(),
                i.getMinimumStock()
        }).toList();
        return newExcel("Inventory", headers, rows);
    }

    public byte[] patientsExcel() throws Exception {
        String[] headers = {"Name", "DOB", "Gender", "Phone", "Blood Group", "Registered"};
        List<Object[]> rows = patientRepository.findAll().stream().map(p -> new Object[]{
                p.getName(), String.valueOf(p.getDateOfBirth()), p.getGender(),
                p.getPhone(), p.getBloodGroup(), String.valueOf(p.getCreatedAt())
        }).toList();
        return newExcel("Patients", headers, rows);
    }

    public byte[] doctorsExcel() throws Exception {
        String[] headers = {"Name", "Specialization", "Phone", "Email"};
        List<Object[]> rows = doctorRepository.findAll().stream().map(d -> new Object[]{
                d.getName(), d.getSpecialization(), d.getPhone(), d.getEmail()
        }).toList();
        return newExcel("Doctors", headers, rows);
    }

    public byte[] revenueExcel() throws Exception {
        String[] headers = {"Bill ID", "Patient", "Amount", "Status", "Date"};
        List<Object[]> rows = billRepository.findAll().stream().map(b -> new Object[]{
                b.getBillId(),
                b.getPatient() != null ? b.getPatient().getName() : "—",
                b.getTotalAmount(),
                b.getPaymentStatus(),
                String.valueOf(b.getBillDate())
        }).toList();
        return newExcel("Revenue", headers, rows);
    }

    public byte[] appointmentsExcel() throws Exception {
        String[] headers = {"ID", "Patient", "Doctor", "Date & Time", "Reason", "Status"};
        List<Object[]> rows = appointmentRepository.findAll().stream().map(a -> new Object[]{
                a.getAppointmentId(),
                a.getPatient() != null ? a.getPatient().getName() : "—",
                a.getDoctor() != null ? a.getDoctor().getName() : "—",
                String.valueOf(a.getAppointmentDateTime()),
                a.getReason(),
                a.getStatus()
        }).toList();
        return newExcel("Appointments", headers, rows);
    }

    public byte[] aiDemandExcel() throws Exception {
        String[] headers = {"Medicine", "Stock", "Monthly Usage", "Predicted Demand", "Trend", "Risk", "Recommended Stock"};
        List<DemandPredictionDTO> list = demandPredictionService.predictAllMedicines();
        List<Object[]> rows = list.stream().map(p -> new Object[]{
                p.getMedicineName(),
                p.getCurrentStock(),
                p.getAverageMonthlyUsage(),
                p.getPredictedNextMonthDemand(),
                p.getTrend(),
                p.getRiskLevel(),
                p.getRecommendedStockLevel()
        }).toList();
        return newExcel("AI Demand Prediction", headers, rows);
    }

    public byte[] aiReorderExcel() throws Exception {
        String[] headers = {"Medicine", "Supplier", "Stock", "Min Stock", "Predicted Demand", "Reorder Qty", "Priority", "Status"};
        List<ReorderRecommendationDTO> list = reorderRecommendationService.generateRecommendations();
        List<Object[]> rows = list.stream().map(r -> new Object[]{
                r.getMedicineName(),
                r.getSupplierName(),
                r.getCurrentStock(),
                r.getMinimumStock(),
                r.getPredictedDemand(),
                r.getRecommendedQuantity(),
                r.getPriority(),
                r.getReorderStatus()
        }).toList();
        return newExcel("AI Reorder Recommendations", headers, rows);
    }
}
