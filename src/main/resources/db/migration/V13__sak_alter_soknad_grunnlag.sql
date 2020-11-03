ALTER TABLE sak RENAME to grunnlag_soknad;

ALTER TABLE grunnlag_soknad ADD COLUMN behandling_id UUID REFERENCES behandling(id);
ALTER TABLE grunnlag_soknad RENAME COLUMN saksnummer TO saksnummer_infotrygd;

ALTER TABLE soker RENAME COLUMN sak TO grunnlag_soknad_id;
ALTER TABLE barn RENAME COLUMN sak to grunnlag_soknad_id;
ALTER TABLE vedlegg RENAME COLUMN sak_id to grunnlag_soknad_id;
