-- HRMS baseline schema (white-label, single-tenant deployment)
-- Safe for repeated execution in existing environments.

CREATE TABLE IF NOT EXISTS hrms_calendar_events (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    academic_year VARCHAR(20) NOT NULL,
    event_date DATE NOT NULL,
    day_type VARCHAR(30) NOT NULL,
    title VARCHAR(150),
    description VARCHAR(1000),
    applies_to_staff BOOLEAN NOT NULL DEFAULT TRUE,
    applies_to_students BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Compatibility for existing schemas where the calendar date column was created as `date`.
ALTER TABLE hrms_calendar_events
    ADD COLUMN IF NOT EXISTS event_date DATE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'hrms_calendar_events'
          AND column_name = 'date'
    ) THEN
        EXECUTE 'UPDATE hrms_calendar_events SET event_date = COALESCE(event_date, "date") WHERE event_date IS NULL';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_hrms_calendar_event_year_date
    ON hrms_calendar_events (academic_year, event_date) WHERE is_active = TRUE;

CREATE TABLE IF NOT EXISTS hrms_leave_type_configs (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    leave_code VARCHAR(20) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    annual_quota INTEGER NOT NULL,
    carry_forward_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    max_carry_forward INTEGER NOT NULL DEFAULT 0,
    encashment_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    min_days_before_apply INTEGER NOT NULL DEFAULT 0,
    max_consecutive_days INTEGER,
    requires_document BOOLEAN NOT NULL DEFAULT FALSE,
    document_required_after_days INTEGER,
    is_paid BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_hrms_leave_type_code
    ON hrms_leave_type_configs (leave_code);

CREATE TABLE IF NOT EXISTS hrms_leave_type_config_grades (
    leave_type_id BIGINT NOT NULL,
    grade_code VARCHAR(30),
    CONSTRAINT fk_hrms_leave_type_grade_leave_type
        FOREIGN KEY (leave_type_id) REFERENCES hrms_leave_type_configs (id)
);

CREATE TABLE IF NOT EXISTS hrms_leave_applications (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    staff_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    total_days NUMERIC(10,2) NOT NULL,
    is_half_day BOOLEAN NOT NULL DEFAULT FALSE,
    half_day_type VARCHAR(20),
    reason VARCHAR(500) NOT NULL,
    attachment_url VARCHAR(1024),
    status VARCHAR(20) NOT NULL,
    applied_on TIMESTAMP NOT NULL,
    reviewed_by_staff_id BIGINT,
    reviewed_on TIMESTAMP,
    review_remarks VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_hrms_leave_application_staff
        FOREIGN KEY (staff_id) REFERENCES staff (id),
    CONSTRAINT fk_hrms_leave_application_leave_type
        FOREIGN KEY (leave_type_id) REFERENCES hrms_leave_type_configs (id),
    CONSTRAINT fk_hrms_leave_application_reviewer
        FOREIGN KEY (reviewed_by_staff_id) REFERENCES staff (id)
);

CREATE TABLE IF NOT EXISTS hrms_leave_balances (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    staff_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    total_quota NUMERIC(10,2) NOT NULL DEFAULT 0,
    used NUMERIC(10,2) NOT NULL DEFAULT 0,
    carried_forward NUMERIC(10,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uk_hrms_leave_balance_staff_type_year UNIQUE (staff_id, leave_type_id, academic_year),
    CONSTRAINT fk_hrms_leave_balance_staff
        FOREIGN KEY (staff_id) REFERENCES staff (id),
    CONSTRAINT fk_hrms_leave_balance_leave_type
        FOREIGN KEY (leave_type_id) REFERENCES hrms_leave_type_configs (id)
);

CREATE TABLE IF NOT EXISTS hrms_staff_grades (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    grade_code VARCHAR(30) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    teaching_wing VARCHAR(40) NOT NULL,
    pay_band_min NUMERIC(12,2) NOT NULL,
    pay_band_max NUMERIC(12,2) NOT NULL,
    sort_order INTEGER NOT NULL,
    min_years_for_promotion INTEGER,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_hrms_staff_grade_code
    ON hrms_staff_grades (grade_code);

CREATE TABLE IF NOT EXISTS hrms_staff_grade_assignments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    staff_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    promotion_order_ref VARCHAR(120),
    promoted_by_staff_id BIGINT,
    remarks VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_hrms_staff_grade_assignment_staff
        FOREIGN KEY (staff_id) REFERENCES staff (id),
    CONSTRAINT fk_hrms_staff_grade_assignment_grade
        FOREIGN KEY (grade_id) REFERENCES hrms_staff_grades (id),
    CONSTRAINT fk_hrms_staff_grade_assignment_promoter
        FOREIGN KEY (promoted_by_staff_id) REFERENCES staff (id)
);

CREATE TABLE IF NOT EXISTS hrms_salary_components (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    component_code VARCHAR(40) NOT NULL,
    component_name VARCHAR(120) NOT NULL,
    component_type VARCHAR(20) NOT NULL,
    calculation_method VARCHAR(40) NOT NULL,
    default_value NUMERIC(12,2) NOT NULL,
    is_taxable BOOLEAN NOT NULL DEFAULT TRUE,
    is_statutory BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_hrms_salary_component_code
    ON hrms_salary_components (component_code);

CREATE TABLE IF NOT EXISTS hrms_salary_templates (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    template_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    grade_id BIGINT,
    academic_year VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_hrms_salary_template_grade
        FOREIGN KEY (grade_id) REFERENCES hrms_staff_grades (id)
);

CREATE TABLE IF NOT EXISTS hrms_salary_template_components (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    template_id BIGINT NOT NULL,
    component_id BIGINT NOT NULL,
    value NUMERIC(12,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_hrms_salary_template_component_template
        FOREIGN KEY (template_id) REFERENCES hrms_salary_templates (id),
    CONSTRAINT fk_hrms_salary_template_component_component
        FOREIGN KEY (component_id) REFERENCES hrms_salary_components (id),
    CONSTRAINT uk_hrms_template_component UNIQUE (template_id, component_id)
);

CREATE TABLE IF NOT EXISTS hrms_staff_salary_mappings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    staff_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    remarks VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_hrms_staff_salary_mapping_staff
        FOREIGN KEY (staff_id) REFERENCES staff (id),
    CONSTRAINT fk_hrms_staff_salary_mapping_template
        FOREIGN KEY (template_id) REFERENCES hrms_salary_templates (id)
);

CREATE TABLE IF NOT EXISTS hrms_staff_salary_component_overrides (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    mapping_id BIGINT NOT NULL,
    component_id BIGINT NOT NULL,
    override_value NUMERIC(12,2) NOT NULL,
    reason VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_hrms_salary_override_mapping
        FOREIGN KEY (mapping_id) REFERENCES hrms_staff_salary_mappings (id),
    CONSTRAINT fk_hrms_salary_override_component
        FOREIGN KEY (component_id) REFERENCES hrms_salary_components (id)
);

CREATE TABLE IF NOT EXISTS hrms_payroll_runs (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    pay_year INTEGER NOT NULL,
    pay_month INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    processed_on TIMESTAMP NOT NULL,
    remarks VARCHAR(500),
    total_staff INTEGER NOT NULL,
    total_gross NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_deductions NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_net NUMERIC(12,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS hrms_payroll_entries (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    payroll_run_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    mapping_id BIGINT NOT NULL,
    gross_pay NUMERIC(12,2) NOT NULL,
    total_deductions NUMERIC(12,2) NOT NULL,
    net_pay NUMERIC(12,2) NOT NULL,
    remarks VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_hrms_payroll_entry_run
        FOREIGN KEY (payroll_run_id) REFERENCES hrms_payroll_runs (id),
    CONSTRAINT fk_hrms_payroll_entry_staff
        FOREIGN KEY (staff_id) REFERENCES staff (id),
    CONSTRAINT fk_hrms_payroll_entry_mapping
        FOREIGN KEY (mapping_id) REFERENCES hrms_staff_salary_mappings (id)
);

CREATE TABLE IF NOT EXISTS hrms_payslips (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    payroll_run_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    pay_month INTEGER NOT NULL,
    pay_year INTEGER NOT NULL,
    total_working_days INTEGER NOT NULL DEFAULT 0,
    days_present INTEGER NOT NULL DEFAULT 0,
    days_absent INTEGER NOT NULL DEFAULT 0,
    lop_days NUMERIC(10,2) NOT NULL DEFAULT 0,
    gross_pay NUMERIC(12,2) NOT NULL,
    total_deductions NUMERIC(12,2) NOT NULL,
    net_pay NUMERIC(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_hrms_payslip_run
        FOREIGN KEY (payroll_run_id) REFERENCES hrms_payroll_runs (id),
    CONSTRAINT fk_hrms_payslip_staff
        FOREIGN KEY (staff_id) REFERENCES staff (id)
);

CREATE TABLE IF NOT EXISTS hrms_payslip_line_items (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    payslip_id BIGINT NOT NULL,
    component_code VARCHAR(50) NOT NULL,
    component_name VARCHAR(150) NOT NULL,
    component_type VARCHAR(20) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_hrms_payslip_line_item_payslip
        FOREIGN KEY (payslip_id) REFERENCES hrms_payslips (id)
);


