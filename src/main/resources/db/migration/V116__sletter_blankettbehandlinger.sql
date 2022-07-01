DELETE
FROM soknad_soker
WHERE grunnlag_soknad_id IN (SELECT soknad_grunnlag.id
                             FROM soknad_grunnlag
                                      JOIN behandling ON soknad_grunnlag.behandling_id = behandling.id
                                 AND behandling.type = 'BLANKETT');
DELETE
FROM vedlegg
WHERE grunnlag_soknad_id IN (SELECT sg.id
                             FROM soknad_grunnlag sg
                                      JOIN behandling b ON b.id = sg.behandling_id
                                 AND b.type = 'BLANKETT');
DELETE
FROM soknad_selvstendig
WHERE soknadsskjema_id IN (SELECT s.id
                           FROM soknadsskjema s
                                    JOIN soknad_grunnlag sg ON s.id = sg.soknadsskjema_id
                                    JOIN behandling b ON b.id = sg.behandling_id
                               AND b.type = 'BLANKETT');
DELETE
FROM soknad_arbeidsgiver
WHERE soknadsskjema_id IN (SELECT s.id
                           FROM soknadsskjema s
                                    JOIN soknad_grunnlag sg ON s.id = sg.soknadsskjema_id
                                    JOIN behandling b ON b.id = sg.behandling_id
                               AND b.type = 'BLANKETT');
DELETE
FROM soknad_aksjeselskap
WHERE soknadsskjema_id IN (SELECT s.id
                           FROM soknadsskjema s
                                    JOIN soknad_grunnlag sg ON s.id = sg.soknadsskjema_id
                                    JOIN behandling b ON b.id = sg.behandling_id
                               AND b.type = 'BLANKETT');
DELETE
FROM soknad_utenlandsopphold
WHERE soknadsskjema_id IN (SELECT s.id
                           FROM soknadsskjema s
                                    JOIN soknad_grunnlag sg ON s.id = sg.soknadsskjema_id
                                    JOIN behandling b ON b.id = sg.behandling_id
                               AND b.type = 'BLANKETT');
DELETE
FROM soknad_tidligere_utdanning
WHERE soknadsskjema_id IN (SELECT s.id
                           FROM soknadsskjema s
                                    JOIN soknad_grunnlag sg ON s.id = sg.soknadsskjema_id
                                    JOIN behandling b ON b.id = sg.behandling_id
                               AND b.type = 'BLANKETT');
DELETE
FROM soknad_barnepassordning
WHERE barn_id IN (SELECT b.id
                  FROM soknad_barn
                           JOIN soknadsskjema s ON soknad_barn.soknadsskjema_id = s.id
                           JOIN soknad_grunnlag sg ON s.id = sg.soknadsskjema_id
                           JOIN behandling b ON b.id = sg.behandling_id
                      AND b.type = 'BLANKETT');;
DELETE
FROM soknad_barn
WHERE (soknadsskjema_id) IN (SELECT s.id
                             FROM soknadsskjema s
                                      JOIN soknad_grunnlag sg ON s.id = sg.soknadsskjema_id
                                      JOIN behandling b ON b.id = sg.behandling_id
                                 AND b.type = 'BLANKETT');

DELETE
FROM soknadsskjema
WHERE id IN (SELECT soknadsskjema_id
             FROM soknad_grunnlag sg
                      JOIN behandling b ON b.id = sg.behandling_id
                 AND b.type = 'BLANKETT');

DELETE
FROM soknad_grunnlag
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM behandling_ekstern
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM oppgave
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM vilkarsvurdering
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM behandlingsjournalpost
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM andel_tilkjent_ytelse
WHERE tilkjent_ytelse IN (SELECT t.id
                          FROM tilkjent_ytelse t
                                   JOIN behandling b ON b.id = t.behandling_id
                          WHERE b.type = 'BLANKETT');
DELETE
FROM tilkjent_ytelse
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM behandlingshistorikk
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM blankett
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM registergrunnlag
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM vedtak
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM grunnlagsdata
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM vedtaksbrev
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM mellomlagret_brev
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM simuleringsresultat
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
UPDATE behandling
SET forrige_behandling_id = NULL
WHERE forrige_behandling_id IN (SELECT id
                                FROM behandling
                                WHERE behandling.type = 'BLANKETT');
DELETE
FROM tilbakekreving
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM mellomlagret_fritekstbrev
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM brevmottakere
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM behandling_barn
WHERE behandling_id IN (SELECT id
                        FROM behandling
                        WHERE behandling.type = 'BLANKETT');
DELETE
FROM behandling
WHERE behandling.type = 'BLANKETT';