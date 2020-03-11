# familie-integrasjoner
App for saksbehandling av stønadene for enslige forsørgere.

## Bygging lokalt
Appen kjører på JRE 11. Bygging gjøres ved å kjøre `mvn clean install`. 

## Kjøring og testing lokalt
For å kjøre opp appen lokalt kan en kjøre `ApplicationLocal`.

Appen starter da opp med en in memory-database og er da tilgjengelig under `localhost:8093`.
Databasen kan aksesseres på `localhost:8093/h2-console`. Log på jdbc url `jdbc:h2:mem:testdb` med bruker `sa` og blankt passord.

Dersom man vil gå mot endepunkter som krever autentisering lokalt, kan man få et testtoken ved å gå mot 
`localhost:8093/local/jwt`. 


## Produksjonssetting
Hvis du skal deploye appen til prod, må du pushe en ny tag på master. Dette gjøres ved å kjøre tag-scriptet som ligger i 
`.github`-mappen. Da spesifiserer du om du vil bumpe major eller minor, scriptet vil da bumpe med 1 opp fra nyeste tag. 

Eksempelvis: 

Nyeste tag er `v0.5`.`./tag.sh -M` vil da pushe tagen `v1.0`, og `./tag.sh -m` vil pushe tagen `v0.6`.

Når en ny tag pushes, trigges github action workflowen som heter Deploy-Prod. 
