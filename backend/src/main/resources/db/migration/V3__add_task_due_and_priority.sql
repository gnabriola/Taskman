-- Due date and time are wall-clock values in the user's time zone, not instants:
-- "5pm" stays 5pm when the user travels.
ALTER TABLE task
    ADD COLUMN due_date date,
    ADD COLUMN due_time time,
    ADD COLUMN priority integer NOT NULL DEFAULT 4,
    ADD CONSTRAINT task_due_time_needs_date CHECK (due_time IS NULL OR due_date IS NOT NULL),
    ADD CONSTRAINT task_priority_range CHECK (priority BETWEEN 1 AND 4);
