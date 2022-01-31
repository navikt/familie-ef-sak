CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
INSERT INTO behandling_barn(id, behandling_id, navn, person_ident, fodsel_termindato, soknad_barn_id)
SELECT uuid_generate_v4(),
       soknad_grunnlag.behandling_id,
       soknad_barn.navn,
       soknad_barn.fodselsnummer,
       soknad_barn.fodsel_termindato,
       soknad_barn.id
FROM soknad_barn
         JOIN soknad_grunnlag ON soknad_grunnlag.soknadsskjema_id = soknad_barn.soknadsskjema_id;
