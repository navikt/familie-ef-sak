CREATE TABLE behandling_ekstern (
    id BIGSERIAL primary key,
    behandling_id UUID references behandling(id)
);

CREATE TABLE fagsak_ekstern (
    id BIGSERIAL primary key,
    fagsak_id UUID references fagsak(id)
);
