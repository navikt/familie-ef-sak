CREATE TABLE behandlingshistorikk
(
    id                  UUID         PRIMARY KEY,
    behandling_id       UUID         REFERENCES behandling(id),

    steg                VARCHAR      NOT NULL,
    endret_av_navn      VARCHAR      NOT NULL,
    endret_av_mail      VARCHAR      NOT NULL,

    endret_tid          TIMESTAMP    NOT NULL DEFAULT localtimestamp
)