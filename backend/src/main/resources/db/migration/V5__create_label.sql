CREATE TABLE label (
    id         uuid        PRIMARY KEY,
    name       varchar(60) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

-- Names are unique regardless of case: @Errands and @errands are the same label.
CREATE UNIQUE INDEX uq_label_name ON label (lower(name));
