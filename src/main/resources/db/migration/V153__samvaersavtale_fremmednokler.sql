ALTER TABLE samvaersavtale
    ADD CONSTRAINT samvaersavtale_behandling_id_fk
        FOREIGN KEY (behandling_id)
            REFERENCES behandling (id);

ALTER TABLE samvaersavtale
    ADD CONSTRAINT samvaersavtale_behandling_barn_id_fk
        FOREIGN KEY (behandling_barn_id)
            REFERENCES behandling_barn (id);