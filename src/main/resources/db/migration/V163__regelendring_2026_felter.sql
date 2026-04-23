ALTER TABLE soknadsskjema
    ADD COLUMN er_regelendring_2026 BOOLEAN DEFAULT false,
    ADD COLUMN hva_situasjon       VARCHAR,
    ADD COLUMN har_inntekt         VARCHAR;
