ALTER TABLE behandling
    ADD COLUMN behandlingsresultat_henlagt_arsak VARCHAR;
UPDATE behandling
SET behandlingsresultat_henlagt_arsak = 'BEHANDLES_I_GOSYS'
WHERE resultat = 'HENLAGT'
  and type = 'BLANKETT';
UPDATE behandling
SET behandlingsresultat_henlagt_arsak = 'FEILREGISTRERT'
WHERE resultat = 'HENLAGT'
  and type != 'BLANKETT';