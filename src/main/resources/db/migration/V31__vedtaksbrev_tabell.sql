CREATE TABLE vedtaksbrev(
    behandling_id       UUID PRIMARY KEY REFERENCES behandling (id),
    steg                VARCHAR,
    utkast_brev_request VARCHAR,
    brev_request        VARCHAR,
    utkast_pdf          BYTEA,
    pdf                 BYTEA)
