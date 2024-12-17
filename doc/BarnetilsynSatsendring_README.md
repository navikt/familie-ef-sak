#Satsendringsrutine - barnetilsyn

## Finn mulige fagsaker som er berørt av nye maks-satser
SQL: 
```sql
SELECT  gib.fagsak_id, v.*, gib.id behandling_id, v.behandling_id behandling_id_for_vedtak, v.barnetilsyn
FROM gjeldende_iverksatte_behandlinger gib
JOIN tilkjent_ytelse ty ON ty.behandling_id = gib.id
JOIN andel_tilkjent_ytelse aty ON ty.id = aty.tilkjent_ytelse
JOIN vedtak v ON v.behandling_id = aty.kilde_behandling_id
WHERE aty.stonad_tom >= '<år som satsendres>-01-01' 
AND gib.stonadstype = 'BARNETILSYN' 
AND aty.belop IN (<gammel sats for 1 barn>, <gammel sats for 2 barn>, <gammel sats for 3 eller flere barn>);
```

Gammel sats finnes i `satserForBarnetilsyn` i `BeregningBarnetilsynUtil.kt`.

Det er verdt å merke seg at spørringen finner bare alle andeler med likt beløp som makssatser, og tar ikke hensyn til antall barn. 
Dermed er det en sjanse for at det f.eks. gis match på en som treffer makssats for 1 barn, selv om det er en behandling med to barn.
Spørringen egner seg derfor ikke til automatisering, og må forbedres dersom før den eventuelt brukes i en automatisert rutine.

Det er laget en scheduler `BarnetilsynSatsendringScheduler` som oppretter task av type `barnetilsynSatsendring` hvis den ikke finnes fra før. 
Scheduleren er ikke skrudd på nå, kommenter inn @Scheduled-annotasjonen i `BarnetilsynSatsendringScheduler`
Når tasken kjører så kaller den på en service som logger fagsakIds som skal satsendres. Tasken kan rekjøres fra prosessering ved behov.
(Potensiell forbedring: Legg til knapp i prosessering som kaller endepunkt i ef-sak som logger satsendringskandidatene.)

## Ikke-vedtatte satser
Det ble testet med egne ikke-vedtatte satser i preprod og lokalt.

## Gjennomføringsrutine
- Ta ut rapport med de som må satsjusteres. Det er brukere som treffer maks-sats på barnetilsyn i perioden med ny sats.
- Sjekk om vi har behandlinger som er i pipeline (ikke ennå besluttet) - dersom det er noen av disse som burde beregnes på nytt, må man følge opp disse sakene. Se SQL under.
- Deploy versjon med nye og gjeldende satser slik at alle behandlinger som gjøres fra nå, blir riktig i perioder med nye satser.

Det kan potensielt bli feil dersom det lagres andre beløp på vedtak enn det saksbehandler har sett ved beregning. Feil i brev?

Gjennomfør manuell revurdering type satsendring før utbetaling i januar, sammen med saksbehandlere.

```sql
SELECT b.id, v.barnetilsyn, aty.* FROM fagsak f
JOIN behandling b ON f.id=b.fagsak_id
JOIN tilkjent_ytelse ty ON b.id = ty.behandling_id
JOIN andel_tilkjent_ytelse aty ON aty.tilkjent_ytelse=ty.id
JOIN vedtak v ON v.behandling_id = b.id
WHERE stonadstype='BARNETILSYN' AND status != 'FERDIGSTILT';
```

Se om `stonad_tom` er senere enn start på ny periode av maks-sats, og om noen av disse har `belop` som er høyere enn maks-sats.
