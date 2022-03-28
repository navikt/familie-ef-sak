/**
  Skriver over eksisterende fagsak_ekstern med id=2473 for å gjøre plass for en ny fagsak
 */
UPDATE fagsak_ekstern SET id = 4999 WHERE id = 2473;

/**
  Oppdaterer fagsak_ekstern med id fra behandling_ekstern da vi har sendt behandling_ekstern i stedet for fagsak_ekstern til iverksett
  Disse har sen blitt iverksatte mot oppdrag.
  For å ikke patche data i iverksett, familie-oppdrag og i økonomi så patcher vi disse i ef-sak
 */
UPDATE fagsak_ekstern SET id = 2473 WHERE id = 1991;
UPDATE fagsak_ekstern SET id = 4216 WHERE id = 3261;
UPDATE fagsak_ekstern SET id = 4273 WHERE id = 3300;
UPDATE fagsak_ekstern SET id = 4379 WHERE id = 3373;
UPDATE fagsak_ekstern SET id = 4383 WHERE id = 3376;
UPDATE fagsak_ekstern SET id = 4384 WHERE id = 3377;
UPDATE fagsak_ekstern SET id = 4386 WHERE id = 3378;
UPDATE fagsak_ekstern SET id = 4388 WHERE id = 3379;
UPDATE fagsak_ekstern SET id = 4393 WHERE id = 3381;
UPDATE fagsak_ekstern SET id = 4397 WHERE id = 3382;
UPDATE fagsak_ekstern SET id = 4399 WHERE id = 3383;
UPDATE fagsak_ekstern SET id = 4401 WHERE id = 3384;
UPDATE fagsak_ekstern SET id = 4403 WHERE id = 3385;
UPDATE fagsak_ekstern SET id = 4405 WHERE id = 3387;
UPDATE fagsak_ekstern SET id = 4407 WHERE id = 3388;
UPDATE fagsak_ekstern SET id = 4408 WHERE id = 3389;
UPDATE fagsak_ekstern SET id = 4409 WHERE id = 3390;