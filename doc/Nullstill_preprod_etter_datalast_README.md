# Datalast i preprod

Hvert halvår kjøres det datalast i preprod mot økonomi og pdl (q1).
Dette gjør at alt vi har iverksett i preprod er ute av synk med de eksterne miljøene etter datalasten og følgelig blir testpersonene ubrukelige.

For å komme i en gyldig (og bra) tilstand må vi nullstille vår database etter datalast.

Her følger et script som kan kjøres mot preprod (IKKE PROD!!!):

```sql

-- Script for å slette gammel data ved datalast i preprod --
-- Bytt ut datoen med dato for datalast og kjør scriptet i preprod--

delete from simuleringsresultat where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from behandling_barn where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from behandling_ekstern where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');

delete from soknad_aksjeselskap where soknadsskjema_id not in (select soknadsskjema_id from soknad_grunnlag join behandling b ON soknad_grunnlag.behandling_id = b.id WHERE vedtakstidspunkt>'2023-07-12');
delete from soknad_arbeidsgiver where soknadsskjema_id not in (select soknadsskjema_id from soknad_grunnlag join behandling b ON soknad_grunnlag.behandling_id = b.id WHERE vedtakstidspunkt>'2023-07-12');
delete from soknad_selvstendig where soknadsskjema_id not in (select soknadsskjema_id from soknad_grunnlag join behandling b ON soknad_grunnlag.behandling_id = b.id WHERE vedtakstidspunkt>'2023-07-12');
delete from soknad_tidligere_utdanning where soknadsskjema_id not in (select soknadsskjema_id from soknad_grunnlag join behandling b ON soknad_grunnlag.behandling_id = b.id WHERE vedtakstidspunkt>'2023-07-12');
delete from soknad_utenlandsopphold where soknadsskjema_id not in (select soknadsskjema_id from soknad_grunnlag join behandling b ON soknad_grunnlag.behandling_id = b.id WHERE vedtakstidspunkt>'2023-07-12');
delete from soknad_soker where grunnlag_soknad_id not in (select soknad_grunnlag.id from soknad_grunnlag join behandling b ON soknad_grunnlag.behandling_id = b.id WHERE vedtakstidspunkt>'2023-07-12');
delete from soknad_barnepassordning where barn_id not in (select barn_id from soknad_grunnlag join soknad_barn on soknad_grunnlag.soknadsskjema_id = soknad_barn.soknadsskjema_id join behandling b ON soknad_grunnlag.behandling_id = b.id WHERE vedtakstidspunkt>'2023-07-12');
delete from soknad_barn where soknadsskjema_id not in (select soknadsskjema_id from soknad_grunnlag join behandling b ON soknad_grunnlag.behandling_id = b.id WHERE vedtakstidspunkt>'2023-07-12');
delete from soknadsskjema where id not in (select soknadsskjema_id from soknad_grunnlag join behandling b ON soknad_grunnlag.behandling_id = b.id WHERE vedtakstidspunkt>'2023-07-12');
delete from soknad_grunnlag where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');

delete from andel_tilkjent_ytelse where tilkjent_ytelse not in (select tilkjent_ytelse.id from tilkjent_ytelse join behandling b ON tilkjent_ytelse.behandling_id = b.id where vedtakstidspunkt>'2023-07-12');
delete from tilkjent_ytelse where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from oppgave where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from vilkarsvurdering where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from behandlingshistorikk where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from behandlingshistorikk where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');

delete from vedtaksbrev where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from brevmottakere where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from blankett where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from vedtak where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from mellomlagret_brev where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from mellomlagret_fritekstbrev where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from behandlingsjournalpost where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from grunnlagsdata where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from registergrunnlag where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from tilbakekreving where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from arsak_revurdering where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from oppgaver_for_opprettelse where behandling_id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');
delete from behandling where id not in (select id from behandling where vedtakstidspunkt > '2023-07-12');


```