CREATE TABLE samvaersavtale
(
    id                 uuid PRIMARY KEY,
    behandling_id      uuid NOT NULL,
    behandling_barn_id uuid NOT NULL,
    uker               JSON NOT NULL
);

ALTER TABLE samvaersavtale
    ADD CONSTRAINT samvaersavtale_behandling_id_behandling_barn_id_unique UNIQUE (behandling_id, behandling_barn_id)