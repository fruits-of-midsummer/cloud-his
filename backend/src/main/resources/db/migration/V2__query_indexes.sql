CREATE INDEX idx_encounter_doctor_status_time ON encounter(doctor_id,status,created_at);
CREATE INDEX idx_patient_name ON patient(name);
CREATE INDEX idx_order_encounter ON order_item(encounter_id);
CREATE INDEX idx_order_kind_department ON order_item(kind,department_id,execution_status,created_at);
CREATE INDEX idx_invoice_encounter_time ON invoice(encounter_id,created_at);
CREATE INDEX idx_movement_medicine_time ON drug_movement(medicine_id,created_at);
CREATE INDEX idx_audit_time ON audit_log(created_at);
