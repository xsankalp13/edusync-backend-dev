-- Sensitive staff PII/banking details are isolated in a dedicated table.

CREATE TABLE IF NOT EXISTS staff_sensitive_info (
    id BIGINT PRIMARY KEY,
    aadhaar_number VARCHAR(64),
    pan_number VARCHAR(20),
    passport_number VARCHAR(20),
    apaar_id VARCHAR(30),
    bank_name VARCHAR(100),
    bank_account_number VARCHAR(64),
    bank_ifsc_code VARCHAR(20),
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relation VARCHAR(50),
    CONSTRAINT fk_staff_sensitive_info_staff
        FOREIGN KEY (id) REFERENCES staff (id)
);

