ALTER TABLE behandling ADD COLUMN resultat VARCHAR;
UPDATE behandling SET resultat='IKKE_SATT';