ALTER TABLE exam_schedule
    ADD COLUMN IF NOT EXISTS active_student_count INTEGER NOT NULL DEFAULT 0;

UPDATE exam_schedule es
SET active_student_count = COALESCE(stats.cnt, 0)
FROM (
    SELECT es2.id AS schedule_id,
           COUNT(DISTINCT st.id) AS cnt
    FROM exam_schedule es2
    JOIN students st
      ON st.is_active = true
     AND (
          (es2.section_id IS NOT NULL AND st.section_id = es2.section_id)
          OR
          (es2.section_id IS NULL AND st.section_id IN (
              SELECT sec.id FROM sections sec WHERE sec.class_id = es2.academic_class_id
          ))
     )
    GROUP BY es2.id
) stats
WHERE es.id = stats.schedule_id;

