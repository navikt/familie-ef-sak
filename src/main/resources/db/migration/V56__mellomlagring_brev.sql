CREATE TABLE mellomlagret_brev (
    behandling_id  UUID PRIMARY KEY REFERENCES behandling (id),
    brevverdier    VARCHAR,
    brevmal        VARCHAR,
    sanity_versjon VARCHAR,
    opprettet_tid  DATE
)