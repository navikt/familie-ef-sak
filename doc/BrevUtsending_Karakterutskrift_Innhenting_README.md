# Rutine for automatisk utsendelse av brev for innhenting av karakterutskrift

## Bakgrunn

Saksbehandler må en gang i året sende ut brev til brukere som har skolegang som aktivitet for å innhente oppnådde 
skolepoeng for den aktuelle perioden. Dette er en prosess som tidligere ble utført manuelt men som nå er automatisert.
[Favro kort.](https://favro.com/organization/98c34fb974ce445eac854de0/a64c6aad9b0d61ef6c0290bd?card=NAV-8258)

## Hvordan er automatiseringen løst?

For personer som er i utdanning lages det ved vedtak om overgangsstønad en fremleggsoppgave for innhenting av
karakterutskrift. I 2023 tilsvarte dette ca ~1600 oppgaver. Disse oppgavene ble flyttet av saksbehandlere til mappe 
*64 utdanning*, med frist 17. og 18. mai for de som er i hovedperioden og den utvidede perioden respektivt. Basert på 
hvilken periode brukeren er i (og således fristen på oppgaven) skal bruker få et av to mulige brev med info om innsending av 
karakterutskrift. Brevmaler for [hovedperiode](https://familie-brev.sanity.studio/ef-brev/desk/dokumentmal;6a24d0a4-1dbe-49db-aeb4-fe2fb4e60e7c)
og [utvidet periode](https://familie-brev.sanity.studio/ef-brev/desk/dokumentmal;7c079f02-fd14-41d1-96a7-796103e9d1e9)
ligger i Sanity.

### Aktivering av utsendelsflyten
For å aktivere den automatiske brevutsendingen må man sende en `POST-request` via postman mot endepunktet
`https://familie-ef-sak.intern.nav.no/api/automatisk-brev-innhenting-karakterutskrift/opprett-tasks`. Man må sende
med følgende verdier i request-bodyen:
- **liveRun** `[true, false]`: Bruk `false` om du kun vil verifisere at koden finner riktig antall oppgaver som ligger i mappen.
Velg `true` om du ønsker å lagre ned tasks for generering og utsending av brev basert på oppgavesøket.
- **frittståendeBrevType** `[enumer for HOVEDPERIODE eller UTVIDET_PERIODE]`: Leter etter oppgaver med frist 17.05 og 
18.05, respektivt.
- **taskLimit** `[Int]`: Et øvre tak for hvor mange oppgaver som hentes ut.

I tillegg er man nødt til å [gå inn i unleash og aktivere feature toggle for automatisk brevinnhenting av karakterutskrift for prod miljøet.](https://unleash.nais.io/#/features/strategies/familie.ef.sak.automatiske-brev-innhenting-karakterutskrift)

### Teknisk
Ved `liveRun` satt til `true` ved innsending av `POST-request` vil følgende skje:
1. Det gjøres et søk etter oppgaver med frist satt til enten 17.05 eller 18.05 - avhengig av om man sender inn enumen for
hovedperiode eller utvidet periode.
2. Deretter lagres det ned en task av typen`SendKarakterutskriftBrevTilIverksettTask` med ansvar for å generere brev og 
sende over data for videre flyt til `familie-ef-iverksett`.
2. Der vil det lagres ned en task av typen `JournalførFrittståendeBrevTask` med ansvar for å journalføre brevet. Selve
brevet lagres også ned i tabellen `karakterutskrift_brev` i iverksett.
3. Når journalføringen er utført oppdateres databaseobjektet i `karakterutskrift_brev` med en `journalpost_id`. Deretter 
lagres det ned en task av typen `DistribuerFrittståendeBrevTask` med ansvar for distribuering av brevet. 
4. Til slutt lagres det ned en task av typen `OppdaterKarakterinnhentingOppgaveTask` som vil oppdatere den opprinnelige
oppgaven med en ny frist, prioritet og beskrivelse.

### Forbedringer
Kanskje man vil lage et enkelt GUI for dette i fremtiden, slik at saksbehandlere har tilgang til å aktivere flyten
for automatisk brevutsending selv. Denne funksjonaliteten kunne vært tilgangskontrollert slik at kun en eller flere 
saksbehandlere har tilgang til å aktivere flyten, for eksempel.