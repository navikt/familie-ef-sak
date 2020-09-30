ALTER TABLE sak RENAME to gr_soknad;

ALTER TABLE gr_soknad RENAME COLUMN saksnummer TO saksnummer_infotrygd;

ALTER TABLE gr_soknad ADD COLUMN behandling_id REFERENCES behandling(id);
