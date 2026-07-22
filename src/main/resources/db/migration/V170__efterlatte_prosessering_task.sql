-- Skjema/tabell for det nye, transaksjonelle outbox-baserte task-rammeverket (no.nav.efterlatte:prosessering-*),
-- pilotert av skyggekjøringInfotrygd-task. Biblioteket leverer ikke migrering selv (se
-- prosessering-postgres/src/main/resources/schema.sql i navikt/efterlatte-prosessering), så tabellen speiles her.
CREATE SCHEMA IF NOT EXISTS prosessering;

CREATE TABLE IF NOT EXISTS prosessering.task
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
    versjon       BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_prosessering_task_plukk
    ON prosessering.task (trigger_tid)
    WHERE status = 'KLAR';

-- Viderefører dedup-garantien fra den gamle task_payload_type_idx (no.nav.familie.prosessering, se
-- V20__Tasktabeller.sql): hindrer at flere podder oppretter duplikate tasks for identisk (type, payload),
-- f.eks. samtidig skyggekjøring av samme kall mot familie-ef-infotrygd-replika.
CREATE UNIQUE INDEX IF NOT EXISTS idx_prosessering_task_type_payload
    ON prosessering.task (type, payload);
