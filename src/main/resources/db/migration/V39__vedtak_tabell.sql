CREATE TABLE vedtak(
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    resultat_type VARCHAR NOT NULL,
    periode_begrunnelse VARCHAR,
    inntekt_begrunnelse VARCHAR,
    perioder JSON,
    inntekter JSON)