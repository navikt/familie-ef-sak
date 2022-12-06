#Satsendringsrutine - barnetilsyn

## Finn fagsaker som er berørt av nye maks-satser
SQL: `SELECT  gib.fagsak_id, v.*, gib.id behandling_id, v.behandling_id behandling_id_for_vedtak, v.barnetilsyn
FROM gjeldende_iverksatte_behandlinger gib
JOIN tilkjent_ytelse ty ON ty.behandling_id = gib.id
JOIN andel_tilkjent_ytelse aty ON ty.id = aty.tilkjent_ytelse
JOIN vedtak v ON v.behandling_id = aty.kilde_behandling_id
WHERE aty.stonad_tom >= '<år som satsendres>-01-01' AND gib.stonadstype = 'BARNETILSYN' AND aty.belop in (<gammel sats for 1 barn>, <gammel sats for 2 barn>, <gammel sats for 3 eller flere barn>);`


Det er laget en scheduler `BarnetilsynSatsendringScheduler` som oppretter task av type `barnetilsynSatsendring` hvis den ikke finnes fra før. 
Scheduleren er ikke skrudd på nå, kommenter inn @Scheduled-annotasjonen i `BarnetilsynSatsendringScheduler`
Når tasken kjører så kaller den på en service som logger fagsakIds som skal satsendres. Tasken kan rekjøres fra prosessering ved behov.
(Potensiell forbedring: Legg til knapp i prosessering som kaller endepunkt i ef-sak som logger satsendringskandidatene.)

## Ikke-vedtatte satser
Det ble testet med egne ikke-vedtatte satser i preprod og lokalt.

## Gjennomføringsrutine
Ta ut rapport med de som må satsjusteres. Det er brukere som treffer maks-sats på barnetilsyn i perioden med ny sats.
Deploy versjon med nye og gjeldende satser slik at alle behandlinger som gjøres fra nå, blir riktig i perioder med nye satser. Best at det skjer rett etter pkt 1.
Før vi deployer ny sats sjekker vi behandlinger som er i pipeline (ikke ennå besluttet) - dersom det er noen av disse som burde beregnes på nytt, må man følge opp disse sakene. Se SQL under.
Det kan potensielt bli feil dersom det lagres andre beløp på vedtak enn det saksbehandler har sett ved beregning. Feil i brev?

Gjennomfør manuell revurdering type satsendring før utbetaling i januar, sammen med saksbehandlere.


`select b.id, v.barnetilsyn, aty.* from fagsak f
join behandling b ON f.id=b.fagsak_id
join tilkjent_ytelse ty ON b.id = ty.behandling_id
join andel_tilkjent_ytelse aty ON aty.tilkjent_ytelse=ty.id
join vedtak v ON v.behandling_id = b.id
where stonadstype='BARNETILSYN' AND status != 'FERDIGSTILT';`

PS: Sjekk antall som treffer max-sats for januar neste år i oktober, slik at man eventuelt har tid til å implementere en automatisk revurdering med satsendring som årsak.
