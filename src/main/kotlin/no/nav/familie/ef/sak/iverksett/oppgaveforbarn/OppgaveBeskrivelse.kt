package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

object OppgaveBeskrivelse {

    fun beskrivelseBarnFyllerEttÅr(): String {
        return "Barn 1 år. Vurder aktivitetsplikten."
    }

    fun beskrivelseBarnBlirSeksMnd(): String {
        return "Barn 1/2 år. Send varsel om aktivitetsplikt."
    }

    val informereLokalkontorOmOvergangsstønad = "Bruker har søkt om overgangsstønad. Til informasjon."

    val innstillingOmBrukersUtdanning =
        "Vi trenger en vurdering fra dere fordi bruker tar/skal ta utdanning. Dere må gjøre vurderingen etter retningslinjene til folketrygdloven § 15-6 første ledd bokstav c og § 5 i Forskrift om stønad til enslig mor eller far.\n\n" +
            "Dere må skrive vurderingen i et notat i Gosys med tittelen «Innstilling utdanning». Når notatet er ferdigstilt, sender dere en Gosys-oppgave med tema «Enslig forsørger» til NAV Arbeid og ytelser 4489.\n\n" +
            "Dere må sende oss vurderingen innen 14 dager. Innstillingen må inneholde:\n\n" +
            "· Opplysninger om tidligere utdanning og arbeidserfaring.\n" +
            "\t-Hvis utdanningen ikke er fullført, må dere skrive det. \n\n" +
            "· En vurdering av om brukeren har yrkeskompetanse og om det er nødvendig med utdanning.\n" +
            "\t-Det er som hovedregel ikke nødvendig med ny utdanning hvis brukeren allerede har yrkeskompetanse. Vi kan bare gjøre unntak fra denne regelen hvis yrkeskompetansen er utdatert eller ikke forenlig med omsorgen for små barn. Vi legger til grunn at vanlig turnusarbeid er forenlig med omsorgen for små barn.\n" +
            "\t-Vi kan ikke vurdere sykdom som en grunn til å ta ny utdanning.\n\n" +
            "· Opplysninger om utdanningen brukeren tar/skal ta.\n" +
            "\t-Vi trenger opplysninger om i hvilken periode brukeren skal studere og studiebelastning\n" +
            "\t-Vi trenger også en konkret plan for gjennomføringen og målet med utdanningen.\n\n" +
            "· En vurdering av om utdanningen er hensiktsmessig for å få eller beholde arbeid.\n" +
            "\t-Vurderingen må ta utgangspunkt i arbeidsmarkedets behov og brukerens behov/muligheter.\n" +
            "\t-Opplysninger om utdanningen er offentlig eller privat\n" +
            "\t-Hvis utdanningen er privat, må dere vurdere om brukeren har særlig grunn til å velge privat utdanning. Det er fordi det offentlige utdanningstilbudet i utgangspunktet er tilstrekkelig.\n" +
            "\t-Hvis det gjelder privat utdanning på videregående skole, må dere vurdere om brukeren har ungdomsrett eller voksenrett til offentlig videregående utdanning. Hvis brukeren ikke har en slik rett, må dette dokumenteres.\n" +
            "\t-Vi trenger også opplysninger om brukeren har utgifter til skolepenger.\n\n" +
            "· En konklusjon som svarer på om utdanningen er nødvendig og hensiktsmessig for å få eller beholde arbeid."
}
