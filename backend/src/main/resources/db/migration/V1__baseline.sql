CREATE TABLE IF NOT EXISTS department (
 id VARCHAR(36) PRIMARY KEY, code VARCHAR(32) NOT NULL UNIQUE, name VARCHAR(80) NOT NULL,
 kind VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS app_user (
 id VARCHAR(36) PRIMARY KEY, username VARCHAR(40) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL,
 display_name VARCHAR(60) NOT NULL, role_code VARCHAR(24) NOT NULL, department_id VARCHAR(36),
 active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (department_id) REFERENCES department(id)
);
CREATE TABLE IF NOT EXISTS schedule (
 id VARCHAR(36) PRIMARY KEY, doctor_id VARCHAR(36) NOT NULL, visit_date DATE NOT NULL,
 period VARCHAR(8) NOT NULL, level_name VARCHAR(20) NOT NULL, fee_cents BIGINT NOT NULL,
 quota INT NOT NULL, booked INT NOT NULL DEFAULT 0,
 UNIQUE(doctor_id, visit_date, period), FOREIGN KEY(doctor_id) REFERENCES app_user(id),
 CHECK(fee_cents >= 0), CHECK(quota > 0), CHECK(booked >= 0 AND booked <= quota)
);
CREATE TABLE IF NOT EXISTS patient (
 id VARCHAR(36) PRIMARY KEY, case_no VARCHAR(40) NOT NULL UNIQUE, name VARCHAR(60) NOT NULL,
 gender VARCHAR(8) NOT NULL, birth_date DATE, age INT, settle_type VARCHAR(20) NOT NULL,
 id_card VARCHAR(32), address VARCHAR(200), phone VARCHAR(24),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS encounter (
 id VARCHAR(36) PRIMARY KEY, visit_no VARCHAR(40) NOT NULL UNIQUE, patient_id VARCHAR(36) NOT NULL,
 schedule_id VARCHAR(36) NOT NULL, doctor_id VARCHAR(36) NOT NULL, department_id VARCHAR(36) NOT NULL,
 status VARCHAR(24) NOT NULL, book_requested BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(patient_id) REFERENCES patient(id), FOREIGN KEY(schedule_id) REFERENCES schedule(id),
 FOREIGN KEY(doctor_id) REFERENCES app_user(id), FOREIGN KEY(department_id) REFERENCES department(id)
);
CREATE TABLE IF NOT EXISTS medical_record (
 encounter_id VARCHAR(36) PRIMARY KEY, chief_complaint VARCHAR(2000) NOT NULL,
 present_illness VARCHAR(4000), treatment_history VARCHAR(2000), past_history VARCHAR(2000),
 allergy_history VARCHAR(2000), physical_exam VARCHAR(2000), initial_diagnosis VARCHAR(1000),
 suggestions VARCHAR(2000), updated_by VARCHAR(36) NOT NULL, version INT NOT NULL DEFAULT 1,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(encounter_id) REFERENCES encounter(id), FOREIGN KEY(updated_by) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS diagnosis (
 encounter_id VARCHAR(36) PRIMARY KEY, disease_code VARCHAR(32), disease_name VARCHAR(200) NOT NULL,
 opinion VARCHAR(2000), doctor_id VARCHAR(36) NOT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(encounter_id) REFERENCES encounter(id), FOREIGN KEY(doctor_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS disease (
 code VARCHAR(32) PRIMARY KEY, name VARCHAR(200) NOT NULL
);
CREATE TABLE IF NOT EXISTS service_item (
 id VARCHAR(36) PRIMARY KEY, code VARCHAR(32) NOT NULL UNIQUE, name VARCHAR(100) NOT NULL,
 kind VARCHAR(12) NOT NULL, specification VARCHAR(100), price_cents BIGINT NOT NULL,
 department_id VARCHAR(36) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 FOREIGN KEY(department_id) REFERENCES department(id), CHECK(price_cents > 0)
);
CREATE TABLE IF NOT EXISTS medicine (
 id VARCHAR(36) PRIMARY KEY, code VARCHAR(32) NOT NULL UNIQUE, name VARCHAR(100) NOT NULL,
 pinyin VARCHAR(80), unit VARCHAR(20) NOT NULL, manufacturer VARCHAR(120), formulation VARCHAR(40),
 price_cents BIGINT NOT NULL, stock INT NOT NULL DEFAULT 0, active BOOLEAN NOT NULL DEFAULT TRUE,
 CHECK(price_cents > 0), CHECK(stock >= 0)
);
CREATE TABLE IF NOT EXISTS prescription (
 id VARCHAR(36) PRIMARY KEY, encounter_id VARCHAR(36) NOT NULL, doctor_id VARCHAR(36) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(encounter_id) REFERENCES encounter(id), FOREIGN KEY(doctor_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS order_item (
 id VARCHAR(36) PRIMARY KEY, encounter_id VARCHAR(36) NOT NULL, kind VARCHAR(12) NOT NULL,
 service_item_id VARCHAR(36), medicine_id VARCHAR(36), prescription_id VARCHAR(36),
 item_name VARCHAR(120) NOT NULL, unit_price_cents BIGINT NOT NULL, quantity INT NOT NULL,
 amount_cents BIGINT NOT NULL, billing_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
 execution_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', department_id VARCHAR(36),
 doctor_id VARCHAR(36) NOT NULL, purpose VARCHAR(1000), body_part VARCHAR(100), notes VARCHAR(1000),
 drug_usage VARCHAR(80), dosage VARCHAR(80), frequency VARCHAR(80), days INT,
 executor_id VARCHAR(36), registered_at TIMESTAMP, completed_at TIMESTAMP,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(encounter_id) REFERENCES encounter(id), FOREIGN KEY(service_item_id) REFERENCES service_item(id),
 FOREIGN KEY(medicine_id) REFERENCES medicine(id), FOREIGN KEY(prescription_id) REFERENCES prescription(id),
 FOREIGN KEY(department_id) REFERENCES department(id), FOREIGN KEY(doctor_id) REFERENCES app_user(id),
 FOREIGN KEY(executor_id) REFERENCES app_user(id),
 CHECK(quantity > 0), CHECK(unit_price_cents >= 0), CHECK(amount_cents = unit_price_cents * quantity)
);
CREATE TABLE IF NOT EXISTS execution_result (
 id VARCHAR(36) PRIMARY KEY, order_id VARCHAR(36) NOT NULL UNIQUE, content VARCHAR(8000) NOT NULL,
 author_id VARCHAR(36) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES order_item(id), FOREIGN KEY(author_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS invoice (
 id VARCHAR(36) PRIMARY KEY, receipt_no VARCHAR(40) NOT NULL UNIQUE, encounter_id VARCHAR(36) NOT NULL,
 request_key VARCHAR(80) NOT NULL UNIQUE, fingerprint VARCHAR(100) NOT NULL,
 total_cents BIGINT NOT NULL, received_cents BIGINT NOT NULL, change_cents BIGINT NOT NULL,
 pay_method VARCHAR(20) NOT NULL, cashier_id VARCHAR(36) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(encounter_id) REFERENCES encounter(id), FOREIGN KEY(cashier_id) REFERENCES app_user(id),
 CHECK(total_cents >= 0), CHECK(received_cents >= total_cents), CHECK(change_cents = received_cents - total_cents)
);
CREATE TABLE IF NOT EXISTS invoice_line (
 id VARCHAR(36) PRIMARY KEY, invoice_id VARCHAR(36) NOT NULL, order_id VARCHAR(36) NOT NULL UNIQUE,
 amount_cents BIGINT NOT NULL, FOREIGN KEY(invoice_id) REFERENCES invoice(id), FOREIGN KEY(order_id) REFERENCES order_item(id)
);
CREATE TABLE IF NOT EXISTS refund (
 id VARCHAR(36) PRIMARY KEY, invoice_line_id VARCHAR(36) NOT NULL UNIQUE, amount_cents BIGINT NOT NULL,
 reason VARCHAR(500) NOT NULL, operator_id VARCHAR(36) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(invoice_line_id) REFERENCES invoice_line(id), FOREIGN KEY(operator_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS drug_movement (
 id VARCHAR(36) PRIMARY KEY, medicine_id VARCHAR(36) NOT NULL, order_id VARCHAR(36),
 kind VARCHAR(20) NOT NULL, quantity INT NOT NULL, reason VARCHAR(500), operator_id VARCHAR(36) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(medicine_id) REFERENCES medicine(id), FOREIGN KEY(order_id) REFERENCES order_item(id),
 FOREIGN KEY(operator_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS audit_log (
 id VARCHAR(36) PRIMARY KEY, actor_id VARCHAR(36) NOT NULL, action_name VARCHAR(80) NOT NULL,
 entity_id VARCHAR(100) NOT NULL, detail VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(actor_id) REFERENCES app_user(id)
);
