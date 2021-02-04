CREATE TABLE vedtaksbrev
(
    id            UUID PRIMARY KEY,
    behandling_id UUID REFERENCES behandling (id),
    steg          VARCHAR,
    brev_request  varchar,
    pdf           BYTEA
)