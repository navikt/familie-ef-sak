ALTER TABLE soknad_utenlandsopphold ADD COLUMN personident VARCHAR;
ALTER TABLE soknad_utenlandsopphold ADD COLUMN adresse VARCHAR;
UPDATE soknad_utenlandsopphold RENAME COLUMN arsak_utenlandsopphold TO arsak;