ALTER TABLE andel_tilkjent_ytelse DROP COLUMN tilkjent_ytelse_key;
ALTER TABLE andel_tilkjent_ytelse ALTER COLUMN id SET NOT NULL;
ALTER TABLE andel_tilkjent_ytelse ADD PRIMARY KEY (id);