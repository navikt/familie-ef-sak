CREATE TABLE behandling_ekstern (
    ekstern_id BIGSERIAL primary key,
    behandling UUID references behandling(id)
);

-- alter table behandling
--   add column ekstern_id bigint references behandling_ekstern(ekstern_id);
--

CREATE TABLE fagsak_ekstern (
    id BIGSERIAL primary key,
    fagsak UUID references fagsak(id)
);
