ALTER TABLE soknad_soker
    DROP CONSTRAINT soker_sak_fkey,
    ADD CONSTRAINT soker_sak_fkey FOREIGN KEY (grunnlag_soknad_id) REFERENCES soknad_grunnlag (id) ON DELETE CASCADE;
ALTER TABLE andel_tilkjent_ytelse
    DROP CONSTRAINT andel_tilkjent_ytelse_tilkjent_ytelse_fkey,
    ADD CONSTRAINT andel_tilkjent_ytelse_tilkjent_ytelse_fkey FOREIGN KEY (tilkjent_ytelse) REFERENCES tilkjent_ytelse (id) ON DELETE CASCADE;
ALTER TABLE vedlegg
    DROP CONSTRAINT vedlegg_sak_id_fkey,
    ADD CONSTRAINT vedlegg_sak_id_fkey FOREIGN KEY (grunnlag_soknad_id) REFERENCES soknad_grunnlag (id) ON DELETE CASCADE;
ALTER TABLE behandling
    DROP CONSTRAINT behandling_fagsak_id_fkey,
    ADD CONSTRAINT behandling_fagsak_id_fkey FOREIGN KEY (fagsak_id) REFERENCES fagsak (id) ON DELETE CASCADE;
ALTER TABLE oppgave
    DROP CONSTRAINT oppgave_behandling_id_fkey,
    ADD CONSTRAINT oppgave_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE vilkarsvurdering
    DROP CONSTRAINT vilkar_vurdering_behandling_id_fkey,
    ADD CONSTRAINT vilkar_vurdering_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE soknad_grunnlag
    DROP CONSTRAINT grunnlag_soknad_behandling_id_fkey,
    ADD CONSTRAINT grunnlag_soknad_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE behandlingsjournalpost
    DROP CONSTRAINT behandlingsjournalpost_behandling_id_fkey,
    ADD CONSTRAINT behandlingsjournalpost_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE soknad_selvstendig
    DROP CONSTRAINT selvstendig_soknadsskjema_id_fkey,
    ADD CONSTRAINT selvstendig_soknadsskjema_id_fkey FOREIGN KEY (soknadsskjema_id) REFERENCES soknadsskjema (id) ON DELETE CASCADE;
ALTER TABLE soknad_arbeidsgiver
    DROP CONSTRAINT arbeidsgiver_soknadsskjema_id_fkey,
    ADD CONSTRAINT arbeidsgiver_soknadsskjema_id_fkey FOREIGN KEY (soknadsskjema_id) REFERENCES soknadsskjema (id) ON DELETE CASCADE;
ALTER TABLE soknad_aksjeselskap
    DROP CONSTRAINT aksjeselskap_soknadsskjema_id_fkey,
    ADD CONSTRAINT aksjeselskap_soknadsskjema_id_fkey FOREIGN KEY (soknadsskjema_id) REFERENCES soknadsskjema (id) ON DELETE CASCADE;
ALTER TABLE soknad_utenlandsopphold
    DROP CONSTRAINT utenlandsopphold_soknadsskjema_id_fkey,
    ADD CONSTRAINT utenlandsopphold_soknadsskjema_id_fkey FOREIGN KEY (soknadsskjema_id) REFERENCES soknadsskjema (id) ON DELETE CASCADE;
ALTER TABLE soknad_tidligere_utdanning
    DROP CONSTRAINT tidligere_utdanning_soknadsskjema_id_fkey,
    ADD CONSTRAINT tidligere_utdanning_soknadsskjema_id_fkey FOREIGN KEY (soknadsskjema_id) REFERENCES soknadsskjema (id) ON DELETE CASCADE;
ALTER TABLE soknad_barnepassordning
    DROP CONSTRAINT barnepassordning_barn_id_fkey,
    ADD CONSTRAINT barnepassordning_barn_id_fkey FOREIGN KEY (barn_id) REFERENCES soknad_barn (id) ON DELETE CASCADE;
ALTER TABLE behandling_ekstern
    DROP CONSTRAINT behandling_ekstern_behandling_id_fkey,
    ADD CONSTRAINT behandling_ekstern_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE fagsak_ekstern
    DROP CONSTRAINT fagsak_ekstern_fagsak_id_fkey,
    ADD CONSTRAINT fagsak_ekstern_fagsak_id_fkey FOREIGN KEY (fagsak_id) REFERENCES fagsak (id) ON DELETE CASCADE;
ALTER TABLE task_logg
    DROP CONSTRAINT henvendelse_logg_henvendelse_id_fkey,
    ADD CONSTRAINT henvendelse_logg_henvendelse_id_fkey FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE;
ALTER TABLE tilkjent_ytelse
    DROP CONSTRAINT tilkjent_ytelse_behandling_id_fkey,
    ADD CONSTRAINT tilkjent_ytelse_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE andel_tilkjent_ytelse
    DROP CONSTRAINT andel_tilkjent_ytelse_kilde_behandling_id_fkey,
    ADD CONSTRAINT andel_tilkjent_ytelse_kilde_behandling_id_fkey FOREIGN KEY (kilde_behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE behandlingshistorikk
    DROP CONSTRAINT behandlingshistorikk_behandling_id_fkey,
    ADD CONSTRAINT behandlingshistorikk_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE blankett
    DROP CONSTRAINT blankett_behandling_id_fkey,
    ADD CONSTRAINT blankett_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE registergrunnlag
    DROP CONSTRAINT registergrunnlag_behandling_id_fkey,
    ADD CONSTRAINT registergrunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE vedtak
    DROP CONSTRAINT vedtak_behandling_id_fkey,
    ADD CONSTRAINT vedtak_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE grunnlagsdata
    DROP CONSTRAINT grunnlagsdata_behandling_id_fkey,
    ADD CONSTRAINT grunnlagsdata_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE vedtaksbrev
    DROP CONSTRAINT vedtaksbrev_behandling_id_fkey,
    ADD CONSTRAINT vedtaksbrev_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE mellomlagret_brev
    DROP CONSTRAINT mellomlagret_brev_behandling_id_fkey,
    ADD CONSTRAINT mellomlagret_brev_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE simuleringsresultat
    DROP CONSTRAINT simuleringsresultat_behandling_id_fkey,
    ADD CONSTRAINT simuleringsresultat_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE behandling
    DROP CONSTRAINT behandling_forrige_behandling_id_fkey,
    ADD CONSTRAINT behandling_forrige_behandling_id_fkey FOREIGN KEY (forrige_behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE tilbakekreving
    DROP CONSTRAINT tilbakekreving_behandling_id_fkey,
    ADD CONSTRAINT tilbakekreving_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE mellomlagret_fritekstbrev
    DROP CONSTRAINT mellomlagret_fritekstbrev_behandling_id_fkey,
    ADD CONSTRAINT mellomlagret_fritekstbrev_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE mellomlagret_frittstaende_brev
    DROP CONSTRAINT mellomlagret_frittstaende_brev_fagsak_id_fkey,
    ADD CONSTRAINT mellomlagret_frittstaende_brev_fagsak_id_fkey FOREIGN KEY (fagsak_id) REFERENCES fagsak (id) ON DELETE CASCADE;
ALTER TABLE brevmottakere
    DROP CONSTRAINT brevmottakere_behandling_id_fkey,
    ADD CONSTRAINT brevmottakere_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE behandling_barn
    DROP CONSTRAINT behandling_barn_behandling_id_fkey,
    ADD CONSTRAINT behandling_barn_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE person_ident
    DROP CONSTRAINT person_ident_fagsak_person_id_fkey,
    ADD CONSTRAINT person_ident_fagsak_person_id_fkey FOREIGN KEY (fagsak_person_id) REFERENCES fagsak_person (id) ON DELETE CASCADE;
ALTER TABLE fagsak
    DROP CONSTRAINT fagsak_fagsak_person_id_fkey,
    ADD CONSTRAINT fagsak_fagsak_person_id_fkey FOREIGN KEY (fagsak_person_id) REFERENCES fagsak_person (id) ON DELETE CASCADE;
ALTER TABLE terminbarn_oppgave
    DROP CONSTRAINT terminbarn_oppgave_fagsak_id_fkey,
    ADD CONSTRAINT terminbarn_oppgave_fagsak_id_fkey FOREIGN KEY (fagsak_id) REFERENCES fagsak (id) ON DELETE CASCADE;
ALTER TABLE soknad_barn
    ADD FOREIGN KEY (soknadsskjema_id) REFERENCES soknadsskjema (id) ON DELETE CASCADE;
