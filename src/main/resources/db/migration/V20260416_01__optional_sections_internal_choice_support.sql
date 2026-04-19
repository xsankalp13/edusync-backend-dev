-- Optional sections + internal choice support for template and evaluation.

ALTER TABLE template_section
    ADD COLUMN IF NOT EXISTS total_questions INTEGER,
    ADD COLUMN IF NOT EXISTS attempt_questions INTEGER,
    ADD COLUMN IF NOT EXISTS section_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS internal_choice_enabled BOOLEAN;

UPDATE template_section
SET total_questions = COALESCE(total_questions, question_count),
    attempt_questions = COALESCE(attempt_questions, question_count),
    section_type = COALESCE(section_type, 'NORMAL'),
    internal_choice_enabled = COALESCE(internal_choice_enabled, FALSE);

ALTER TABLE template_section
    ALTER COLUMN total_questions SET NOT NULL,
    ALTER COLUMN section_type SET NOT NULL,
    ALTER COLUMN internal_choice_enabled SET NOT NULL;

CREATE TABLE IF NOT EXISTS template_question (
    question_id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    section_id BIGINT NOT NULL,
    question_no INTEGER NOT NULL,
    marks INTEGER NOT NULL,
    question_type VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_template_question_section FOREIGN KEY (section_id)
        REFERENCES template_section(section_id) ON DELETE CASCADE,
    CONSTRAINT uq_template_question_section_no UNIQUE (section_id, question_no)
);

CREATE INDEX IF NOT EXISTS idx_template_question_section ON template_question(section_id);

UPDATE template_section ts
SET internal_choice_enabled = TRUE
WHERE EXISTS (
    SELECT 1
    FROM template_question tq
    WHERE tq.section_id = ts.section_id
      AND tq.question_type = 'INTERNAL_CHOICE'
);

CREATE TABLE IF NOT EXISTS question_option (
    option_id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    label VARCHAR(10) NOT NULL,
    CONSTRAINT fk_question_option_question FOREIGN KEY (question_id)
        REFERENCES template_question(question_id) ON DELETE CASCADE,
    CONSTRAINT uq_question_option_label UNIQUE (question_id, label)
);

CREATE INDEX IF NOT EXISTS idx_question_option_question ON question_option(question_id);

ALTER TABLE question_marks
    ADD COLUMN IF NOT EXISTS option_label VARCHAR(10);

UPDATE question_marks
SET option_label = COALESCE(option_label, '');

ALTER TABLE question_marks
    ALTER COLUMN option_label SET NOT NULL;

ALTER TABLE question_marks
    DROP CONSTRAINT IF EXISTS uq_question_marks_result_section_question;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_question_marks_result_section_question_option'
    ) THEN
        ALTER TABLE question_marks
            ADD CONSTRAINT uq_question_marks_result_section_question_option
            UNIQUE (evaluation_result_id, section_name, question_number, option_label);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_question_marks_result_section_question
    ON question_marks(evaluation_result_id, section_name, question_number);

ALTER TABLE evaluation_results
    ADD COLUMN IF NOT EXISTS section_totals JSONB,
    ADD COLUMN IF NOT EXISTS selected_questions JSONB;

