CREATE TABLE Soker (
    sak           UUID REFERENCES sak (id),
    navn          VARCHAR,
    fodselsnummer VARCHAR
);

CREATE TABLE barn (
    sak                     UUID REFERENCES sak (id),
    navn                    VARCHAR,
    fodselsnummer           VARCHAR,
    fodselsdato             DATE NOT NULL,
    har_Samme_Adresse       BOOLEAN,
    forelder2_navn          VARCHAR,
    forelder2_fodselsnummer VARCHAR,
    forelder2_bosatt_Norge  BOOLEAN,
    forelder2_adresse       VARCHAR,
    forelder2_postnummer    VARCHAR,
    forelder2_poststedsnavn VARCHAR,
    forelder2_land          VARCHAR
);

