ALTER TABLE andel_tilkjent_ytelse ADD COLUMN IF NOT EXISTS inntekt BIGINT default 0;
ALTER TABLE andel_tilkjent_ytelse ADD COLUMN IF NOT EXISTS inntektsreduksjon BIGINT default 0;
ALTER TABLE andel_tilkjent_ytelse ADD COLUMN IF NOT EXISTS samordningsfradrag BIGINT default 0;