CREATE TABLE blankett(
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    pdf           BYTEA
);