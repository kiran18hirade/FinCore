CREATE DATABASE IF NOT EXISTS loan_approval_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE loan_approval_db;
SET FOREIGN_KEY_CHECKS=0;
DROP VIEW IF EXISTS vw_customer_dashboard;
DROP VIEW IF EXISTS vw_application_details;
DROP TABLE IF EXISTS repayment_schedule;
DROP TABLE IF EXISTS loans;
DROP TABLE IF EXISTS loan_evaluations;
DROP TABLE IF EXISTS loan_applications;
DROP TABLE IF EXISTS kyc_details;
DROP TABLE IF EXISTS employment_details;
DROP TABLE IF EXISTS loan_products;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS customers;
SET FOREIGN_KEY_CHECKS=1;

CREATE TABLE customers (
 customer_id INT AUTO_INCREMENT PRIMARY KEY,
 full_name VARCHAR(120) NOT NULL,
 email VARCHAR(150) NOT NULL UNIQUE,
 phone VARCHAR(15) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL,
 date_of_birth DATE NOT NULL,
 address VARCHAR(255) NOT NULL,
 city VARCHAR(80) NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_phone CHECK (CHAR_LENGTH(phone) BETWEEN 10 AND 15)
);

CREATE TABLE employment_details (
 employment_id INT AUTO_INCREMENT PRIMARY KEY,
 customer_id INT NOT NULL UNIQUE,
 employment_type ENUM('SALARIED','SELF_EMPLOYED','BUSINESS','OTHER') NOT NULL,
 company_name VARCHAR(150),
 monthly_income DECIMAL(14,2) NOT NULL CHECK(monthly_income > 0),
 experience_years DECIMAL(4,1) DEFAULT 0 CHECK(experience_years >= 0),
 FOREIGN KEY(customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE TABLE kyc_details (
 kyc_id INT AUTO_INCREMENT PRIMARY KEY,
 customer_id INT NOT NULL UNIQUE,
 pan_number VARCHAR(10) NOT NULL UNIQUE,
 aadhaar_last4 CHAR(4) NOT NULL,
 kyc_status ENUM('VERIFIED','PENDING','REJECTED') DEFAULT 'VERIFIED',
 FOREIGN KEY(customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE TABLE loan_products (
 loan_type_id INT AUTO_INCREMENT PRIMARY KEY,
 loan_type VARCHAR(30) NOT NULL UNIQUE,
 min_amount DECIMAL(14,2) NOT NULL,
 max_amount DECIMAL(14,2) NOT NULL,
 annual_interest_rate DECIMAL(5,2) NOT NULL,
 max_tenure_months INT NOT NULL,
 min_credit_score INT NOT NULL DEFAULT 650,
 max_emi_ratio DECIMAL(5,4) NOT NULL DEFAULT 0.50,
 min_monthly_income DECIMAL(14,2) NOT NULL DEFAULT 20000,
 CHECK(max_amount >= min_amount), CHECK(max_tenure_months > 0), CHECK(max_emi_ratio > 0 AND max_emi_ratio <= 1)
);

CREATE TABLE loan_applications (
 application_id INT AUTO_INCREMENT PRIMARY KEY,
 customer_id INT NOT NULL,
 loan_type_id INT NOT NULL,
 requested_amount DECIMAL(14,2) NOT NULL,
 tenure_months INT NOT NULL,
 purpose VARCHAR(255) NOT NULL,
 existing_emi DECIMAL(14,2) DEFAULT 0 CHECK(existing_emi >= 0),
 credit_score INT NOT NULL CHECK(credit_score BETWEEN 300 AND 900),
 status ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
 rejection_reason VARCHAR(1000),
 applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 decided_at DATETIME NULL,
 FOREIGN KEY(customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
 FOREIGN KEY(loan_type_id) REFERENCES loan_products(loan_type_id)
);

CREATE TABLE loan_evaluations (
 evaluation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
 application_id INT NOT NULL UNIQUE,
 income_check BOOLEAN NOT NULL,
 credit_score_check BOOLEAN NOT NULL,
 emi_check BOOLEAN NOT NULL,
 amount_check BOOLEAN NOT NULL,
 tenure_check BOOLEAN NOT NULL,
 kyc_check BOOLEAN NOT NULL,
 overall_result ENUM('APPROVED','REJECTED') NOT NULL,
 reason VARCHAR(1000),
 evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(application_id) REFERENCES loan_applications(application_id) ON DELETE CASCADE
);

CREATE TABLE loans (
 loan_id INT AUTO_INCREMENT PRIMARY KEY,
 application_id INT NOT NULL UNIQUE,
 customer_id INT NOT NULL,
 principal_amount DECIMAL(14,2) NOT NULL,
 annual_interest_rate DECIMAL(5,2) NOT NULL,
 tenure_months INT NOT NULL,
 monthly_emi DECIMAL(14,2) NOT NULL,
 outstanding_principal DECIMAL(14,2) NOT NULL,
 loan_status ENUM('ACTIVE','CLOSED') DEFAULT 'ACTIVE',
 start_date DATE NOT NULL,
 FOREIGN KEY(application_id) REFERENCES loan_applications(application_id) ON DELETE RESTRICT,
 FOREIGN KEY(customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT
);

CREATE TABLE repayment_schedule (
 schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
 loan_id INT NOT NULL,
 installment_no INT NOT NULL,
 due_date DATE NOT NULL,
 emi_amount DECIMAL(14,2) NOT NULL,
 principal_component DECIMAL(14,2) NOT NULL,
 interest_component DECIMAL(14,2) NOT NULL,
 status ENUM('PENDING','PAID','OVERDUE') DEFAULT 'PENDING',
 paid_at DATETIME NULL,
 UNIQUE(loan_id, installment_no),
 FOREIGN KEY(loan_id) REFERENCES loans(loan_id) ON DELETE CASCADE
);

CREATE TABLE audit_logs (
 log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
 application_id INT,
 action_type VARCHAR(60) NOT NULL,
 old_status VARCHAR(30),
 new_status VARCHAR(30),
 description VARCHAR(1000),
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(application_id) REFERENCES loan_applications(application_id) ON DELETE SET NULL
);

CREATE INDEX idx_app_customer ON loan_applications(customer_id);
CREATE INDEX idx_app_status ON loan_applications(status);
CREATE INDEX idx_schedule_loan ON repayment_schedule(loan_id);
