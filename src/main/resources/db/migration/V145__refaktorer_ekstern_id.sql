---- Flytt ekstern_id fra egen tabell til behandling -----
ALTER TABLE behandling
    ADD COLUMN ekstern_id BIGSERIAL;

---- Oppdater eksisterende ekstern_id fra egen tabell til behandling -----
UPDATE behandling
SET ekstern_id = be.id
from behandling_ekstern be
where be.behandling_id = behandling.id;

--- Strengere constraint på ekstern_id - ikke null og må være unik ----
ALTER TABLE behandling ALTER COLUMN ekstern_id SET NOT NULL; -- må testes
ALTER TABLE behandling ADD CONSTRAINT b_ekstern_id_unique UNIQUE(ekstern_id);

---- Flytt ekstern_id fra egen tabell til fagsak -----
ALTER TABLE fagsak
    ADD COLUMN ekstern_id BIGSERIAL;

---- Oppdater eksisterende ekstern_id fra egen tabell til fagsak -----
UPDATE fagsak
SET ekstern_id = fe.id
from fagsak_ekstern fe
where fe.fagsak_id = fagsak.id;

--- Strengere constraint på ekstern_id - ikke null og må være unik ----
ALTER TABLE fagsak ALTER COLUMN ekstern_id SET NOT NULL; -- må testes
ALTER TABLE fagsak ADD CONSTRAINT f_ekstern_id_unique UNIQUE(ekstern_id);

