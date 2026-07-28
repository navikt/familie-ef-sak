CREATE TABLE IF NOT EXISTS prosessering_task
(
    id            BIGSERIAL PRIMARY KEY,
    type          TEXT        NOT NULL,
    status        TEXT        NOT NULL DEFAULT 'KLAR',
    payload       TEXT,
    trigger_tid   TIMESTAMPTZ NOT NULL DEFAULT now(),
    opprettet_tid TIMESTAMPTZ NOT NULL DEFAULT now(),
    plukket_tid   TIMESTAMPTZ,
    antall_feil   INT         NOT NULL DEFAULT 0,
    stoppaarsak   TEXT,
    versjon       BIGINT      NOT NULL DEFAULT 0,
    metadata      JSONB,
    CONSTRAINT uq_prosessering_task_type_payload UNIQUE (type, payload)
);

CREATE INDEX IF NOT EXISTS idx_prosessering_task_plukk
    ON prosessering_task (trigger_tid)
    WHERE status = 'KLAR';

CREATE TABLE IF NOT EXISTS prosessering_task_logg
(
    task_id   BIGINT      NOT NULL,
    node      TEXT        NOT NULL,
    kjort_tid TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_prosessering_task_logg_task UNIQUE (task_id)
);
