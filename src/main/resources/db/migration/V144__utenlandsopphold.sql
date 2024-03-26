ALTER TABLE soknad_utenlandsopphold ADD COLUMN personident_eos_land VARCHAR;
ALTER TABLE soknad_utenlandsopphold ADD COLUMN adresse_eos_land VARCHAR;
ALTER TABLE soknad_utenlandsopphold ADD COLUMN kan_ikke_oppgi_personident BOOLEAN;
ALTER TABLE soknad_utenlandsopphold ADD COLUMN er_eos_land BOOLEAN;