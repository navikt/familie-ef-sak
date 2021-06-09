ALTER TABLE andel_tilkjent_ytelse ADD COLUMN IF NOT EXISTS inntekt BIGINT NOT NULL default 0;
ALTER TABLE andel_tilkjent_ytelse ADD COLUMN IF NOT EXISTS inntektsreduksjon BIGINT NOT NULL DEFAULT 0;
ALTER TABLE andel_tilkjent_ytelse ADD COLUMN IF NOT EXISTS samordningsfradrag BIGINT NOT NULL DEFAULT 0;