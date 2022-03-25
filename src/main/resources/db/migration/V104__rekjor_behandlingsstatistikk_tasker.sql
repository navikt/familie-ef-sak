/**
  Behandling-DVH ønsker å få rekjørt behandlingsstatistikk-tasker som har fått endret ekstern fagsakId
 */
UPDATE task
SET status='KLAR_TIL_PLUKK'
WHERE (payload::JSON ->> 'behandlingId')::VARCHAR::UUID IN (
    SELECT b.id
    FROM behandling b
    WHERE b.fagsak_id IN (
        SELECT fagsak_id
        FROM fagsak_ekstern fe
        WHERE fe.id IN
              (4999, 2473, 4216, 4273, 4379, 4383, 4384, 4386, 4388, 4393, 4397, 4399, 4401, 4403, 4405, 4407, 4408, 4409)
    )
)
  AND type = 'behandlingsstatistikkTask'
  AND opprettet_tid < '2022-03-24 11:20:00.000'