-- Every project, label and task belongs to a user. Existing rows cannot be given an
-- owner, so this migration expects empty tables (the dev database was reset for it).
--
-- task.owner_id duplicates the owner of its project on purpose: the task module can
-- scope every query (including filters across all projects) by owner without asking
-- the project module. The application keeps the two in sync.
ALTER TABLE project ADD COLUMN owner_id uuid NOT NULL REFERENCES app_user (id);
ALTER TABLE label   ADD COLUMN owner_id uuid NOT NULL REFERENCES app_user (id);
ALTER TABLE task    ADD COLUMN owner_id uuid NOT NULL REFERENCES app_user (id);

CREATE INDEX idx_project_owner ON project (owner_id);
CREATE INDEX idx_task_owner ON task (owner_id);

-- Label names are now unique per user, not globally.
DROP INDEX uq_label_name;
CREATE UNIQUE INDEX uq_label_owner_name ON label (owner_id, lower(name));
