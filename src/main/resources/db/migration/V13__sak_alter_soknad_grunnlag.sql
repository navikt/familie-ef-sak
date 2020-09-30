ALTER TABLE sak RENAME to gr_soknad;

ALTER TABLE gr_soknad RENAME COLUMN saksnummer TO saksnummer_infotrygd;
ALTER TABLE gr_soknad ADD COLUMN behandling_id UUID REFERENCES behandling(id);

ALTER TABLE soker RENAME COLUMN sak TO gr_soknad_id;
ALTER TABLE barn RENAME COLUMN sak to gr_soknad_id;
