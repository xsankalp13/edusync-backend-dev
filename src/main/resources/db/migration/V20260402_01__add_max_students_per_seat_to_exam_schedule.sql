-- Migration: Add max_students_per_seat column to exam_schedule
ALTER TABLE exam_schedule ADD COLUMN IF NOT EXISTS max_students_per_seat INTEGER NOT NULL DEFAULT 1;
