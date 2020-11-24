CREATE TABLE konsistenavstemming (
    id            BIGSERIAL     PRIMARY KEY,
    dato          DATE          NOT NULL,
    stonadstype   VARCHAR       NOT NULL,
    unique(dato, stonadstype)
 );