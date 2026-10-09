-- recurrence holds a subset of an RFC 5545 RRULE, e.g. FREQ=WEEKLY;BYDAY=MO,WE,FR.
-- recurrence_from_completion is Todoist's "every!": count from the completion date.
ALTER TABLE task
    ADD COLUMN recurrence varchar(200),
    ADD COLUMN recurrence_from_completion boolean NOT NULL DEFAULT false,
    ADD CONSTRAINT task_recurrence_needs_due_date CHECK (recurrence IS NULL OR due_date IS NOT NULL);
