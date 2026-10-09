-- Owned by the task module. Deleting a task (also in bulk, when its project is
-- deleted) removes its links; deleting a label is handled by the task module via
-- LabelDeletedEvent, so label_id has no cascade.
CREATE TABLE task_label (
    task_id  uuid NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    label_id uuid NOT NULL REFERENCES label (id),
    PRIMARY KEY (task_id, label_id)
);

CREATE INDEX idx_task_label_label ON task_label (label_id);
