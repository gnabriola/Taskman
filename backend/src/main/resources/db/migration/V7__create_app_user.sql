-- "user" is a reserved word in PostgreSQL, hence app_user.
CREATE TABLE app_user (
    id            uuid         PRIMARY KEY,
    email         varchar(254) NOT NULL,
    display_name  varchar(100) NOT NULL,
    password_hash varchar(100) NOT NULL,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE UNIQUE INDEX uq_app_user_email ON app_user (lower(email));
