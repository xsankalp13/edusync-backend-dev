ALTER TABLE staff_sensitive_info
    ADD COLUMN IF NOT EXISTS bank_account_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS bank_account_holder_name VARCHAR(100);
