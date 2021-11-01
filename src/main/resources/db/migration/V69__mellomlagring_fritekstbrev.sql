CREATE TABLE mellomlagret_fritekstbrev (
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    brev          JSON
)