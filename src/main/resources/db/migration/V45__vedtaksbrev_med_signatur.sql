DROP TABLE vedtaksbrev;
CREATE TABLE vedtaksbrev (
    behandling_id             UUID PRIMARY KEY REFERENCES behandling (id),
    saksbehandler_brevrequest VARCHAR,
    brevmal                   VARCHAR,
    saksbehandlersignatur     VARCHAR,
    besluttersignatur         VARCHAR,
    beslutter_pdf             BYTEA
)