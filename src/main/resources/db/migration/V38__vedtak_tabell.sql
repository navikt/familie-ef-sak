CREATE TABLE vedtak(
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    resultatType VARCHAR NOT NULL,
    periodeBegrunnelse VARCHAR,
    inntektBegrunnelse VARCHAR,
    perioder JSON,
    inntekter JSON)