CREATE TABLE oppgaver_for_ferdigstilling
(
    behandling_id UUID PRIMARY KEY,
    fremleggsoppgave_ider_som_skal_ferdigstilles BIGINT[]
);