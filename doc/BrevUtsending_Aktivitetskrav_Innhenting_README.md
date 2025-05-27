# Rutine for automatisk utsendelse av brev for innhenting av aktivitetsplikt

## Bakgrunn

Saksbehandler må en gang i året sende ut brev til brukere som har skolegang som aktivitet 
for å vurdere aktivitetsplikten etter endt semester.
[Favro kort.](https://favro.com/organization/98c34fb974ce445eac854de0/a64c6aad9b0d61ef6c0290bd?card=NAV-8258)

## Hvordan er automatiseringen løst?

For personer som er i utdanning lages det ved vedtak om overgangsstønad en fremleggsoppgave for innhenting av
aktivitetsplikt. I 2023 tilsvarte dette ca ~1600 oppgaver. Disse oppgavene ble flyttet av saksbehandlere til mappe 
*64 utdanning*, med frist 17. mai. Brevmaler [todo] ligger i Sanity.

### Aktivering av utsendelsflyten
For å aktivere den automatiske brevutsendingen må man sende en `POST-request` via [swagger](https://familie-ef-sak.intern.nav.no/swagger-ui/index.html#/) mot endepunktet
`https://familie-ef-sak.intern.nav.no/api/automatisk-brev-innhenting-aktivitetsplikt/opprett-tasks`. Man må sende
med følgende verdier i request-bodyen:
- **liveRun** `[true, false]`: Bruk `false` om du kun vil verifisere at koden finner riktig antall oppgaver som ligger i mappen.
Velg `true` om du ønsker å lagre ned tasks for generering og utsending av brev basert på oppgavesøket.
- **taskLimit** `[Int]`: Et øvre tak for hvor mange oppgaver som hentes ut.

I tillegg er man nødt til å [gå inn i unleash og aktivere feature toggle for automatisk brevinnhenting av aktivitetsplikt for prod miljøet.](https://teamfamilie-unleash-web.iap.nav.cloud.nais.io/projects/default/features/familie.ef.sak.automatiske-brev-innhenting-aktivitetsplikt)

### Teknisk

Før man iverksetter automatisk utsending må man huske å manuelt oppdatere enkelte datoer i koden: 
1. `oppgaveAktivitetspliktFrist` i `AutomatiskBrevInnhentingAktivitetspliktService` i `familie-ef-sak`
2. `FRIST_OPPFØLGINGSOPPGAVE` og `FRIST_OPPRINNELIG_OPPGAVE` i `OppdaterAktivitetspliktInnhentingOppgaveTask` i `familie-ef-iverksett`

Ved `liveRun` satt til `true` ved innsending av `POST-request` vil følgende skje:
1. Det gjøres et søk etter oppgaver med frist satt til `17.05`, tema `ENF` og mappe `64 Utdanning` uten en tilordnet ressurs
2. Deretter lagres det ned en task av typen`SendAktivitetspliktBrevTilIverksettTask` med ansvar for å generere brev og 
sende over data for videre flyt til `familie-ef-iverksett`.
2. Der vil det lagres ned en task av typen `JournalførFrittståendeBrevTask` med ansvar for å journalføre brevet. Selve
brevet lagres også ned i tabellen `aktivitetsplikt_brev` i iverksett.
3. Når journalføringen er utført oppdateres databaseobjektet i `aktivitetsplikt_brev` med en `journalpost_id`. Deretter 
lagres det ned en task av typen `DistribuerFrittståendeBrevTask` med ansvar for distribuering av brevet. 
4. Til slutt lagres det ned en task av typen `OppdaterAktivitetspliktOppgaveTask` som vil oppdatere den opprinnelige
oppgaven med en ny frist, prioritet og beskrivelse.
