UPDATE vedtak
SET saksbehandler_ident = subquery.opprettet_av
FROM (SELECT opprettet_av, behandling_id FROM behandlingshistorikk WHERE steg = 'SEND_TIL_BESLUTTER') as subquery
WHERE vedtak.behandling_id = subquery.behandling_id;

UPDATE vedtak
SET beslutter_ident = subquery.opprettet_av
FROM (SELECT opprettet_av, behandling_id FROM behandlingshistorikk WHERE steg = 'BESLUTTE_VEDTAK') as subquery
WHERE vedtak.behandling_id = subquery.behandling_id;