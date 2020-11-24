CREATE TABLE konsistensavstemming (
    id            UUID          PRIMARY KEY,
    dato          DATE          NOT NULL,
    stonadstype   VARCHAR       NOT NULL,
    unique(dato, stonadstype)
 );