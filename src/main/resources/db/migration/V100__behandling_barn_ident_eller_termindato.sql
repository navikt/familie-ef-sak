ALTER TABLE behandling_barn
    ADD CONSTRAINT behandling_barn_ident_fodsel_termindato_check
        CHECK (person_ident IS NOT NULL OR fodsel_termindato IS NOT NULL);
