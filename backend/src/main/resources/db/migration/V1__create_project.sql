CREATE TABLE project (
    id         uuid         PRIMARY KEY,
    name       varchar(120) NOT NULL,
    created_at timestamptz  NOT NULL,
    updated_at timestamptz  NOT NULL
);
