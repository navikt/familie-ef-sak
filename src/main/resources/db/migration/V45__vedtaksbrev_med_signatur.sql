DROP TABLE vedtaksbrev;
CREATE TABLE vedtaksbrev (
    behandling_id             UUID PRIMARY KEY REFERENCES behandling (id),
    saksbehandler_brevrequest VARCHAR,
    brevmal                   VARCHAR,
    beslutter_pdf             BYTEA,
    saksbehandlersignatur     VARCHAR,
    besluttersignatur         VARCHAR
)