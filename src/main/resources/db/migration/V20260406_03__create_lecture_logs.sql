CREATE TABLE IF NOT EXISTS lecture_logs (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    schedule_id BIGINT NOT NULL,
    teacher_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    document_url VARCHAR(1024),
    has_taken_test BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_lecture_log_schedule FOREIGN KEY (schedule_id) REFERENCES schedule (id) ON DELETE CASCADE,
    CONSTRAINT fk_lecture_log_teacher FOREIGN KEY (teacher_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_lecture_logs_schedule ON lecture_logs (schedule_id);
CREATE INDEX idx_lecture_logs_teacher ON lecture_logs (teacher_id);
