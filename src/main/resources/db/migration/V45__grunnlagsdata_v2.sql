CREATE TABLE GRUNNLAGDATA
(
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    data          JSON         NOT NULL
    )