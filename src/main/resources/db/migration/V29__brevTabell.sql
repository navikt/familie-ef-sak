CREATE TABLE Brev (
    id  UUID PRIMARY KEY,
    behandling UUID REFERENCES behandling (id) ,
    pdf BYTEA
)