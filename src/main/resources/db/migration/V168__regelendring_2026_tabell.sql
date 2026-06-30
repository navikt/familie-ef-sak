CREATE TABLE regelendring_2026
(
    behandling_id          UUID        PRIMARY KEY REFERENCES behandling (id),
    er_regelendring_2026   BOOLEAN     NOT NULL,
    begrunnelse            TEXT        NOT NULL
);
