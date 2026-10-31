# CareHub 3.0 — AI-Powered Hospital Inventory & Management System

CareHub 3.0 is a full-stack, enterprise-grade hospital management and intelligent inventory optimization platform built on **Spring Boot 3.3 + Spring Security + Spring Data JPA + MySQL 8.0** with a responsive, modern **HTML5 / CSS3 / JavaScript** frontend.

CareHub 3.0 builds upon all existing hospital inventory and administration capabilities, introducing 10 major modules including predictive analytics, smart procurement recommendations, clinical prescription OCR scanning, longitudinal medical histories, appointment scheduling, and full operational auditing.

---

## 1. What's New in CareHub 3.0

1. **AI Medicine Demand Prediction:** Time-series moving averages, demand growth trend ratios, risk classification (`CRITICAL_SHORTAGE`, `HIGH_RISK`, `MODERATE`, `LOW_RISK`), and target safety stock buffers based on real dispensing data.
2. **AI Smart Reorder Recommendation:** Real-time stock reorder engine evaluating minimum thresholds, supplier lead-times, and batch expiry with 1-click Purchase Order generation.
3. **AI Prescription OCR Studio:** Doctor/Pharmacist prescription image scanning with fuzzy catalog drug detection, dosage/frequency parsing, and strict interactive clinician verification.
4. **Appointment Management:** End-to-end appointment lifecycle scheduling (`SCHEDULED` $\rightarrow$ `CONFIRMED` $\rightarrow$ `COMPLETED` / `CANCELLED`) with today's visit filters and status metrics.
5. **Patient Medical History:** Longitudinal health records consolidating chronological diagnoses, consultation notes, prescriptions, appointments, and billing invoices.
6. **Central Notification Center:** Reactive global bell with live unread counter badge, category filters (`LOW_STOCK`, `EXPIRY_WARNING`, `APPOINTMENT_REMINDER`, `PENDING_BILL`, `AI_RECOMMENDATION`), and 1-click read/dismiss workflows.
7. **Advanced Dashboard Analytics:** 9 live KPI counters and 6 dynamic Chart.js visualizations (Monthly Revenue, Medicine Consumption, Patient Registrations, Appointment Distribution, Stock Status, AI Predicted vs Current Stock).
8. **Activity & Audit Log:** Centralized system-wide audit tracking capturing user logins, patient registrations, medicine edits, dispenses, and OCR scans with instant search and filtering.
9. **Medicine Search & Multi-criteria Filtering:** Instant debounce live search, dynamic category filtering, stock status pills (`Available`, `Low Stock`, `Expiring Soon`, `Expired`), and column sorting.
10. **Role-Based Access Control (RBAC):** Spring Security enforcement across all endpoints with dynamic role-scoped UI sidebar navigation for `ADMIN`, `DOCTOR`, `PHARMACIST`, and `RECEPTIONIST`.

---

## 2. Technology Stack & Database Architecture

| Layer | Technology |
| :--- | :--- |
| **Backend** | Spring Boot 3.3.4, Java 17+, Spring Data JPA, Spring Security, Bean Validation |
| **Database** | MySQL 8.0+ (17 relational tables, UTF8MB4, auto-managed via Hibernate DDL) |
| **Security** | BCrypt password hashing, session-based authentication, role-based endpoint protection |
| **AI & OCR** | Java analytical prediction engine, regex clinical parser & catalog fuzzy matching (plus optional Python Flask microservice in `ai_service/`) |
| **Reporting** | OpenPDF (PDF export) + Apache POI (Excel export) |
| **Frontend** | Responsive Vanilla HTML5, modern CSS3, JavaScript (ES6+), Chart.js |
| **Build Tool** | Apache Maven |

### 17 Database Tables
CareHub 3.0 integrates 17 relational entities:
- **Core Entities:** `users`, `doctors`, `patients`, `suppliers`, `medicines`, `inventory_batches`, `prescriptions`, `prescription_items`, `bills`, `bill_items`, `purchase_orders`
- **CareHub 3.0 Upgrades:** `appointments`, `medical_history`, `notifications`, `audit_logs`, `ai_predictions`, `ai_reorder_recommendations`

The DDL migration script is located at:
- `carehub_v3_migration.sql`

---

## 3. Quick Start & Setup

### Prerequisites
- Java Development Kit (JDK 17 or higher)
- MySQL Server 8.0+
- Modern Web Browser (Chrome, Firefox, Edge)

### Database Configuration
Ensure MySQL is running on `localhost:3306`. Check or update database credentials in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/carehub_v2?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
```

### Launch Backend Application
Run from project root:
```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
The application will launch on: **`http://localhost:8080`**

---

## 4. Default Test Credentials

All accounts are pre-seeded and verified for immediate testing:

| Role | Username | Password | Permitted Modules |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` *(or `Dinesh`)* | `Admin@123` | Full unrestricted access to all 10 modules, AI hub, audit logs, reports |
| **DOCTOR** | `dr.smith` | `Doctor@123` | Appointments, Patient History, Prescription OCR Studio, Prescriptions |
| **PHARMACIST** | `pharma.john` | `Pharma@123` | Medicines & Stock Search, AI Smart Reorder, Dispensing, POs |
| **RECEPTIONIST** | `nurse.reception` | `Recep@123` | Patient Registration, Appointments, Billing, Notifications |

---

## 5. REST API Reference

| Module | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Authentication** | `POST` | `/api/auth/login` | Session login with username and password |
| | `POST` | `/api/auth/logout` | Invalidate active session and record audit log |
| | `GET` | `/api/auth/me` | Current authenticated user and role details |
| **AI Demand** | `GET` | `/api/ai/demand-predictions` | Demand predictions, safety stock, risk levels |
| | `GET` | `/api/ai/demand-predictions/{medicineId}` | Specific medicine demand forecast |
| **AI Reorder** | `GET` | `/api/ai/reorder-recommendations` | Smart reorder list, recommended quantities, priorities |
| | `GET` | `/api/ai/insights` | Combined shortage risks & high-priority reorders |
| **Prescription OCR** | `POST` | `/api/ai/prescription-ocr` | Multipart prescription image OCR scan & drug match |
| | `POST` | `/api/ai/prescription-ocr/confirm` | Confirm verified medicines and persist prescription |
| **Appointments** | `GET` | `/api/appointments` | List all appointments with patient/doctor details |
| | `GET` | `/api/appointments/today` | List visits scheduled for the current date |
| | `GET` | `/api/appointments/stats` | Status counts (Scheduled, Confirmed, Completed, Cancelled) |
| | `POST` | `/api/appointments` | Book new appointment |
| | `PATCH` | `/api/appointments/{id}/status?status=...` | Update appointment status |
| **Patient History** | `GET` | `/api/patients/{id}/history` | Longitudinal profile (Diagnoses, Prescriptions, Visits, Bills) |
| | `POST` | `/api/medical-history` | Record new consultation diagnosis |
| **Notifications** | `GET` | `/api/notifications` | List alerts (optional category filter) |
| | `GET` | `/api/notifications/unread-count` | Live unread counter badge |
| | `PATCH` | `/api/notifications/{id}/read` | Mark individual alert read |
| | `POST` | `/api/notifications/mark-all-read` | Mark all alerts read |
| **Audit Logs** | `GET` | `/api/audit-logs` | Activity audit trail with user and module filtering |
| **Medicines** | `GET` | `/api/medicines/search` | Multi-criteria search (keyword, category, stockStatus, sort) |
| | `GET` | `/api/medicines/categories` | Distinct medicine categories list |
| **Dashboard** | `GET` | `/api/dashboard/summary` | Real-time counts, revenue, and AI insights |
| | `GET` | `/api/dashboard/charts` | 6 chart series for Chart.js rendering |
| **Reports** | `GET` | `/api/reports/appointments/pdf` & `/excel` | Export appointment report |
| | `GET` | `/api/reports/ai-demand/pdf` & `/excel` | Export AI demand forecast report |
| | `GET` | `/api/reports/ai-reorder/pdf` & `/excel` | Export AI reorder recommendation report |

---

## 6. Frontend Navigation Directory

| Screen | File | Description |
| :--- | :--- | :--- |
| **Dashboard** | `dashboard.html` | Real-time KPIs, AI Insights banner, 6 Chart.js charts |
| **Appointments** | `appointments.html` | Booking modal, status workflow, today's schedule |
| **Patient History** | `patient-history.html` | Longitudinal profile, consultation tabs, diagnosis modal |
| **Prescription OCR** | `prescription-ocr.html` | OCR image upload, catalog matching, clinician verification |
| **AI Insights & Reorder** | `ai-dashboard.html` | Demand forecasts, reorder proposals, 1-click PO generator |
| **Notifications** | `notifications.html` | Alert center, category filter pills, mark-as-read actions |
| **Audit Logs** | `audit-logs.html` | Activity trail, username search, module filters |
| **Medicines** | `medicines.html` | Inventory catalog, live search, stock health badges |
| **Patients** | `patients.html` | Patient directory with direct link to Medical History |
| **Prescriptions** | `prescriptions.html` | Clinical prescriptions with direct link to OCR Studio |
| **Reports** | `reports.html` | PDF and Excel export center |
| **Login** | `login.html` | Role-based authentication portal |

---

## 7. Optional Python AI Companion Microservice

For deployments requiring advanced machine learning (e.g. Scikit-Learn regression or Tesseract OCR), an optional Python Flask microservice is provided in the `ai_service/` folder:

```bash
cd ai_service
pip install -r requirements.txt
python app.py
```
This service runs on port `5000` and seamlessly exposes `/predict-demand`, `/recommend-reorder`, and `/ocr-prescription` endpoints conforming to the CareHub 3.0 JSON specification.
