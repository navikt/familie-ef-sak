# familie-ef-sak
App for saksbehandling av stønadene for enslige forsørgere.

## Swagger
http://localhost:8093/swagger-ui.html

## Bygging lokalt
Appen kjører på JRE 11. Bygging gjøres ved å kjøre `mvn clean install`.


### Autentisering lokalt
Dersom man vil gjøre autentiserte kall mot andre tjenester eller vil kjøre applikasjonen sammen med frontend, må man sette opp følgende miljø-variabler:

#### Client id & client secret
secret kan hentes fra cluster med
`kubectl -n teamfamilie get secret azuread-familie-ef-sak-lokal -o json | jq '.data | map_values(@base64d)'`

* `AZURE_APP_CLIENT_ID` (fra secret)
* `AZURE_APP_CLIENT_SECRET` (fra secret)
* Scope for den aktuelle tjenesten (`FAMILIE_INTEGRASJONER_SCOPE`, ...)

Alle disse variablene finnes i applikasjonens mappe for preprod-fss på vault. Merk at client id og client secret har andre navn i applikasjonens mappe. 
Disse kan alternativt hentes fra azure-mappen i vault, der vil de ha riktig navn. Variablene legges inn under ApplicationLocal -> Edit Configurations -> Environment Variables. 

### Kjøring med in-memory-database
For å kjøre opp appen lokalt, kan en kjøre `ApplicationLocal`.

Appen starter da opp med en in memory-database og er da tilgjengelig under `localhost:8093`.
Databasen kan aksesseres på `localhost:8093/h2-console`. Log på jdbc url `jdbc:h2:mem:testdb` med bruker `sa` og blankt passord.

### Kjøring med postgres-database
For å kjøre opp appen lokalt med en postgres-database, kan en kjøre `ApplicationLocalPostgres`.
App'en vil starte opp en container med siste versjon av postgres. 

For å kjøre opp postgres containern så kjører man `docker-compose up`
For å ta ned containern så kjører man `docker-compose down`
For å slette volymen `docker-compose down -v`

### GCP
GCP bruker secrets i stedet for vault.
Anbefaler å bruke [modify-secrets](https://github.com/rajatjindal/kubectl-modify-secret)

#### Database
[Nais doc - Postgres](https://doc.nais.io/persistence/postgres/)

[Nais doc - Responsibilities](https://doc.nais.io/persistence/responsibilities/)

### Koble til database i preprod:
[Oppskrift på databasetilkobling](https://github.com/navikt/familie/blob/cb403dbf0e7e5af2f5b0d8168d89dae87ce318c4/doc/utvikling/gcp/gcp_kikke_i_databasen.md)

For å koble til preprod kan du kjøre kommandoene:
1. `gcloud auth login`
2. `gcp-db teamfamilie-dev-ae07 familie-ef-sak`
3. Url: `jdbc:postgresql://localhost:5432/familie-ef-sak`
4. Brukernavn: `fornavn.etternavn@nav.no` som brukernavn 
5. Passord: Lim inn det som ligger i clipboard fra steg 2

## Produksjonssetting
Applikasjonen vil deployes til produksjon ved ny commit på main-branchen. Det er dermed tilstrekkelig å merge PR for å trigge produksjonsbygget. 

## Roller
Testbrukeren som opprettes i IDA må ha minst en av følgende roller:
- 0000-GA-Enslig-Forsorger-Beslutter
- 0000-GA-Enslig-Forsorger-Saksbehandler

## Testdata
- Registering av arbeidssøker - https://arbeidssokerregistrering.dev.nav.no/

## Manuell vedlikehold
[Satsendring barnetilsyn](doc/BarnetilsynSatsendring_README.md)

## Kode generert av GitHub Copilot

Dette repoet bruker GitHub Copilot til å generere kode.