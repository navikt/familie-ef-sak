# G-omregning 
## WIP: Dokumentasjon, valg, rutiner og manuelle steg. Førsteutkast 

Noen av kommentarene i dette dokumentet går litt ut over implementering av kode.  
Viktig med forklaring på hvordan og hvorfor koden har blitt som den har blitt, hvilke 
valg vi har tatt og hva vi jobber med å endre før neste g-omregning.   



### Teknisk - flyt og valg

#### Flyt: 

##### 1. Start - scheduler + sql

**Når G er vedtatt:** 
1. Vurder om vi skal skru av scheduler (se under) 
2. Legg inn ny G i no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
3. Nye behandlinger vil nå bruke ny G
4. Kjør gjerne en test med 1 fagsak (se ManuellGOmregningController under) før man setter på scheduler?
5. Se "etterarbeid under"

G-omregning starter vanligvis med at en scheduler finner kandidater for g-omregning (sql) 
`no.nav.familie.ef.sak.behandling.grunnbelop.GOmregningTaskServiceScheduler`

Scheduler kjøres typisk en gang i uka (tirsdager kl 15) og vil prøve å finne ferdigstilte fagsaker med gammel G uten samordning.

Saker som var åpne ved første g-omregning vil bli forsøkt kjørt i neste ukes batch hvis de er ferdigstilt i mellomtiden. 

Det finnes også en controller hvor man også kan kjøre omregning på _en_ fagsak
`no.nav.familie.ef.sak.forvaltning.ManuellGOmregningController`

##### 2. Hvordan utføres gomregning (kode)
1. Data kopieres fra forrige behandling -> ny G - behandling
2. Inntekten justeres ihht G [HER ER DET GJORT NOEN VALG!!!]
3. Ytelsen beregnes på nytt [HER ER DET GJORT NOEN VALG!!!]
4. Iverksetting uten brev
5. `familie-ef-iverksett`: Iverksetter mot økonomi og sender melding til Ditt NAV
6. Task som poller status fra iverksett (vanlig flyt)

### Noen valg det er fint å vite om
#### Indeksjustering av inntekt.
`no.nav.familie.ef.sak.beregning.BeregningKt.finnGrunnbeløpsPerioder`
Inntekten indeksjusteres ved g-omregning. Forslag til å slutte med dette er utarbeidet (mars 2024), men ikke løst. 
For hver inntektsperiode vi skal endre:
1. Vi finner omregningsfaktor 
2. Vi finner total inntekt (dag + mnd + år). 
3. Total inntekt rundes ned til nærmeste 1000 (finne F - forventet inntekt). *1
4. Vi multipliserer F med omregningsfaktor (se pkt 1)
5. Dette resultatet runder vi av til nærmeste 100. *2
6. Evt. dagsats og månedsats vil nulles på det nye G-vedtaket og det G-avrundede beløpet settes på "årsinntekt". *3 

*1) Vi velger å tolke alle inntekter (totalinntekt) som "uavrundet inntekter" (reell).
Vi runder derfor ned til nærmeste 1000 før vi justerer inntekt.
Dette gjør vi for å behandle g-regulering på samme måte som vanlige beregninger.
Alternativ er "ikke rund ned g-beløp til nærmeste 1000" - gir potensielt feil ved
eksisterende beløp som tilfeldigvis er 100 (pre - warning)

*2) I beregning vil vi ikke runde ned beløp modulus 100 med rest. 

*3) Her mister vi detaljer i vedtak (mnd/dag). Dette kan føre til problemer med beregning i etterkant.
Dersom vi revurderer og gjør ny beregning etter en g-omregning vil bruker få mer utbetalt. Dette fordi vi i beregning avrunder inntekt ned til nærmeste 1000. 
Dette har vi løst med et hack hvor inntekter som slutter på hele 100 ikke rundes ned (vi gjetter på og antar at dette er en g-beløps-inntekt).

En del av punktene ovenfor har vært kilde til mye diskusjon på teamet. Kan dette i noen tilfeller føre til forskjellsbehandling av stønadsmottakere? 
Regler og rutiner er nok et etterslep fra en tid hvor vi jobbet med stormaskin, andre beløp (naturlig inflasjon) og praktiske løsninger for å få dette til å fly. 
Vi jobber med å modernisere regelverk og rutiner her. Husk å følge opp disse diskusjonene i god tid før g-omregning. 

#### Gomregning - validering av ordinere behandlinger/iverksettinger under og etter g-omregning

Når vi har lagt inn ny G er det fint om vi ikke iverksetter med "gammel" g på nye perioder. Dette gjelder spesiellt revurderinger fra juni hvor det også er løpende i mai - da er det vanskeligere å finne 
igjen mai for regulering senere. For at åpne behandlinger, f.eks. en behandling som er sendt til beslutter, ikke må utføres på nytt tillater vi likevel å iverksette med gammel G i en overgangsperiode. Denne valideringen ligger i

`no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper.validerGrunnbeløpsmåned`

Valideringen utføres når et vedtak besluttes. Lengden på overgangsperioden bestemmes utifra `fristGOmregning` som vi har hardkodet til 1. juni. Det betyr at hvis koden oppdateres med ny G 20. mai, vil man i perioden 20.mai - 1. juni få lov til å iverksette både med nyeste og nest nyeste G. `fristGOmregning` kan justeres utifra hvor strenge vi ønsker å være.

#### Etterarbeid/sjekkliste:

* `FinnBehandlingerMedGammelGTask` vil rapportere de som må håndteres manuelt (samordningsfradrag) den 1. i hver måned. De i denne listen som har samordning eller sanksjon skal overleveres til coachene. 

* Gå igjennom de med sanksjon - disse må revurderes manuelt 
(NB! ikke mulig å g-omregne mai-løpende, juni-sanksjon, juli-løpende). Denne vil kaste feil. 

* Sjekk om det ligger noen som IKKE er g-omregnet. Typisk 0-utbetaling i juni og utover  - de som er tagget med 2022 etter g-omregningsdag

* Oppdater veiviser med barnetilsyn 6G (vilkår for å kunne motta = inntekt under 6G) 

* Oppdater veiviser med inntekt som tipper OS over i 0-beløp (2023 - )

* Sjekk om det er riktig G brukt på nav.no? 

* Kanskje sjekke om noen har fått avslag på førstegangsbehandling i mai på "BT" med begrunnelse over 6 gamle G?  

