-- =============================================================================
-- CareHub 3.0: Database Schema Migration & Tables Setup
-- Database: carehub_v2 (MySQL 8.0+)
-- =============================================================================

USE `carehub_v2`;

-- 1. Appointments Table
CREATE TABLE IF NOT EXISTS `appointments` (
    `appointment_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `patient_id` BIGINT NOT NULL,
    `doctor_id` BIGINT NULL,
    `appointment_date` DATE NOT NULL,
    `appointment_time` TIME NOT NULL,
    `reason` VARCHAR(500),
    `status` VARCHAR(50) DEFAULT 'SCHEDULED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_appt_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients`(`patient_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_appt_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `users`(`user_id`) ON DELETE SET NULL,
    INDEX `idx_appt_date` (`appointment_date`),
    INDEX `idx_appt_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Patient Medical History Table
CREATE TABLE IF NOT EXISTS `medical_history` (
    `history_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `patient_id` BIGINT NOT NULL,
    `doctor_id` BIGINT NULL,
    `diagnosis` VARCHAR(255),
    `notes` TEXT,
    `treatment_date` DATE NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_hist_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients`(`patient_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_hist_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `users`(`user_id`) ON DELETE SET NULL,
    INDEX `idx_hist_patient` (`patient_id`),
    INDEX `idx_hist_date` (`treatment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Notifications Table
CREATE TABLE IF NOT EXISTS `notifications` (
    `notification_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `message` TEXT NOT NULL,
    `category` VARCHAR(50) NOT NULL, -- LOW_STOCK, EXPIRY_WARNING, APPOINTMENT_REMINDER, NEW_PRESCRIPTION, PENDING_BILL, AI_RECOMMENDATION
    `recipient_role` VARCHAR(50) NULL,
    `is_read` BIT(1) DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_notif_read` (`is_read`),
    INDEX `idx_notif_cat` (`category`),
    INDEX `idx_notif_role` (`recipient_role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Central Activity / Audit Logs Table
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `log_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `action` VARCHAR(100) NOT NULL,
    `module` VARCHAR(100) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `user_role` VARCHAR(50),
    `description` TEXT,
    `entity_id` BIGINT NULL,
    `ip_address` VARCHAR(50),
    `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_audit_module` (`module`),
    INDEX `idx_audit_user` (`username`),
    INDEX `idx_audit_time` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. AI Medicine Demand Predictions Table
CREATE TABLE IF NOT EXISTS `ai_predictions` (
    `prediction_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `medicine_id` BIGINT NOT NULL,
    `current_stock` INT DEFAULT 0,
    `average_monthly_usage` DOUBLE DEFAULT 0,
    `predicted_demand` INT DEFAULT 0,
    `risk_level` VARCHAR(50) DEFAULT 'LOW_RISK',
    `recommended_stock` INT DEFAULT 0,
    `confidence_score` DOUBLE DEFAULT 0.85,
    `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_aipred_med` FOREIGN KEY (`medicine_id`) REFERENCES `medicines`(`medicine_id`) ON DELETE CASCADE,
    INDEX `idx_aipred_med` (`medicine_id`),
    INDEX `idx_aipred_risk` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. AI Smart Reorder Recommendations Table
CREATE TABLE IF NOT EXISTS `ai_reorder_recommendations` (
    `recommendation_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `medicine_id` BIGINT NOT NULL,
    `current_stock` INT DEFAULT 0,
    `predicted_demand` INT DEFAULT 0,
    `minimum_stock` INT DEFAULT 0,
    `recommended_quantity` INT DEFAULT 0,
    `priority` VARCHAR(50) DEFAULT 'LOW', -- HIGH, MEDIUM, LOW
    `reason` TEXT,
    `reorder_required` BIT(1) DEFAULT 0,
    `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_aireorder_med` FOREIGN KEY (`medicine_id`) REFERENCES `medicines`(`medicine_id`) ON DELETE CASCADE,
    INDEX `idx_aireorder_med` (`medicine_id`),
    INDEX `idx_aireorder_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. AI Prescription Image & NLP Analysis Table (CareHub 3.0 CNN + OCR + NLP)
CREATE TABLE IF NOT EXISTS `ai_prescription_analysis` (
    `analysis_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `prescription_id` BIGINT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `image_quality` VARCHAR(50) DEFAULT 'GOOD',
    `sharpness_score` DOUBLE DEFAULT 0.0,
    `cnn_classification` VARCHAR(100) DEFAULT 'VALID_PRESCRIPTION_PRINTED',
    `cnn_confidence` DOUBLE DEFAULT 0.85,
    `ocr_text` TEXT,
    `nlp_entity_count` INT DEFAULT 0,
    `model_name` VARCHAR(100) DEFAULT 'CNN+OCR+NLP',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_aipresc_analysis` FOREIGN KEY (`prescription_id`) REFERENCES `prescriptions`(`prescription_id`) ON DELETE SET NULL,
    INDEX `idx_aipresc_quality` (`image_quality`),
    INDEX `idx_aipresc_cnn` (`cnn_classification`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
