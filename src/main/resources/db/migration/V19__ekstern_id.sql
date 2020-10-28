CREATE TABLE behandling_ekstern (
    id BIGSERIAL primary key,
    behandling UUID references behandling(id)
);

CREATE TABLE fagsak_ekstern (
    id BIGSERIAL primary key,
    fagsak UUID references fagsak(id)
);
