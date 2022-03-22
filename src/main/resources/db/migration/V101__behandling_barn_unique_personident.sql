CREATE UNIQUE INDEX ON behandling_barn (behandling_id, person_ident)
    WHERE person_ident IS NOT NULL;