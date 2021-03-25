-- soknadsskjema_id
CREATE INDEX ON aksjeselskap (soknadsskjema_id);
CREATE INDEX ON arbeidsgiver (soknadsskjema_id);
CREATE INDEX ON barn (soknadsskjema_id);
CREATE INDEX ON barnepassordning (barn_id);
CREATE INDEX ON selvstendig (soknadsskjema_id);
CREATE INDEX ON tidligere_utdanning(soknadsskjema_id);
CREATE INDEX ON utenlandsopphold(soknadsskjema_id);

CREATE INDEX ON andel_tilkjent_ytelse (tilkjent_ytelse);

-- behandling
CREATE INDEX ON behandling (fagsak_id);
CREATE INDEX ON behandling_ekstern (behandling_id);
CREATE INDEX ON behandlingshistorikk (behandling_id);
CREATE INDEX ON behandlingsjournalpost (behandling_id);
CREATE INDEX ON vilkarsvurdering (behandling_id);

-- fagsak
CREATE INDEX ON fagsak_ekstern (fagsak_id);

-- oppgave
CREATE INDEX ON oppgave (behandling_id);
CREATE INDEX ON oppgave (gsak_oppgave_id);

-- grunnlag_soknad
ALTER TABLE soker ADD PRIMARY KEY (grunnlag_soknad_id);
CREATE INDEX ON vedlegg(grunnlag_soknad_id);