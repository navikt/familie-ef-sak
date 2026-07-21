-- Skjema/tabell for efterlatte-prosessering (navikt/efterlatte-prosessering), pilotert av
-- SkyggekjørInfotrygdTask, se .github/prosessering/ i det repoet. Eget skjema holder
-- bibliotekets tabell adskilt fra appens øvrige tabeller, inkludert den gamle
-- no.nav.familie.prosessering-tasktabellen ("task" i public-skjemaet, se V20__Tasktabeller.sql).

CREATE SCHEMA IF NOT EXISTS prosessering;

CREATE TABLE IF NOT EXISTS prosessering.task (
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

CREATE INDEX IF NOT EXISTS idx_task_plukk
    ON prosessering.task (trigger_tid)
    WHERE status = 'KLAR';
