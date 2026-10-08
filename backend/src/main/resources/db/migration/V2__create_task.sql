CREATE TABLE task (
    id           uuid         PRIMARY KEY,
    project_id   uuid         NOT NULL REFERENCES project (id),
    title        varchar(500) NOT NULL,
    description  text,
    completed_at timestamptz,
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL
);

CREATE INDEX idx_task_project_open ON task (project_id, completed_at);
