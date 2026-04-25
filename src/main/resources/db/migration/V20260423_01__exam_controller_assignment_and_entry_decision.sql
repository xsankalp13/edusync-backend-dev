CREATE TABLE IF NOT EXISTS exam_controller_assignment (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    assigned_by_user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT now(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uk_exam_controller_assignment_exam UNIQUE (exam_id),
    CONSTRAINT fk_exam_controller_assignment_exam FOREIGN KEY (exam_id) REFERENCES exams (exam_id),
    CONSTRAINT fk_exam_controller_assignment_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
    CONSTRAINT fk_exam_controller_assignment_user FOREIGN KEY (assigned_by_user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_exam_controller_assignment_staff
    ON exam_controller_assignment (staff_id);

CREATE TABLE IF NOT EXISTS exam_entry_decision (
    id BIGSERIAL PRIMARY KEY,
    exam_schedule_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    is_allowed BOOLEAN NOT NULL DEFAULT true,
    reason VARCHAR(300),
    decided_by_staff_id BIGINT NOT NULL,
    decided_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_exam_entry_decision_schedule_student UNIQUE (exam_schedule_id, student_id),
    CONSTRAINT fk_exam_entry_decision_schedule FOREIGN KEY (exam_schedule_id) REFERENCES exam_schedule (id),
    CONSTRAINT fk_exam_entry_decision_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_exam_entry_decision_staff FOREIGN KEY (decided_by_staff_id) REFERENCES staff (id)
);

CREATE INDEX IF NOT EXISTS idx_exam_entry_decision_schedule
    ON exam_entry_decision (exam_schedule_id);

