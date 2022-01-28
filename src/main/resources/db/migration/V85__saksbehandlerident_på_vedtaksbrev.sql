ALTER TABLE vedtaksbrev
    ADD COLUMN saksbehandlerident VARCHAR,
    ADD COLUMN beslutterident VARCHAR;

update vedtaksbrev
set saksbehandlerident = 'IKKE_SATT';


