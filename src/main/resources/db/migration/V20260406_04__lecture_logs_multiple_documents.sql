ALTER TABLE lecture_logs DROP COLUMN document_url;

CREATE TABLE IF NOT EXISTS lecture_log_documents (
    lecture_log_id BIGINT NOT NULL,
    document_url VARCHAR(1024) NOT NULL,
    CONSTRAINT fk_lecture_log_documents_log FOREIGN KEY (lecture_log_id) REFERENCES lecture_logs (id) ON DELETE CASCADE
);

CREATE INDEX idx_lecture_log_documents_log_id ON lecture_log_documents (lecture_log_id);
