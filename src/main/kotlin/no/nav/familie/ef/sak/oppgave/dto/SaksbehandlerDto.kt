package no.nav.familie.ef.sak.oppgave.dto

data class SaksbehandlerDto(
    val fornavn: String,
    val etternavn: String,
    val rolle: SaksbehandlerRolle,
)

/*  I tilfeller hvor saksbehandler manuelt oppretter en revurdering eller en førstegangsbehandling vil oppgaven
 *  som returneres fra oppgavesystemet være null. Dette skjer fordi oppgavesystemet bruker litt tid av variabel
 *  lengde på å opprette den tilhørende behandle-sak-oppgaven til den opprettede behandlingen.
 *
 *  Dersom null blir returnert og behandlingen befinner seg i steget REVURDERING_ÅRSAK,
 *  VILKÅR eller BEREGNE_YTELSE anser vi det som svært sannsynlig at det er den innloggede saksbehandleren som
 *  er ansvarlig for behandlingen - oppgaven har bare ikke rukket å bli opprettet enda. I dette tilfellet
 *  returnerer vi OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER til frontend.
 *
 *  Dersom null returneres og behandlingen ikke befinner seg i et av de nevnte stegene returnerer vi
 *  OPPGAVE_FINNES_IKKE til frontend.
 * */
enum class SaksbehandlerRolle {
    IKKE_SATT,
    INNLOGGET_SAKSBEHANDLER,
    ANNEN_SAKSBEHANDLER,
    OPPGAVE_FINNES_IKKE,
    OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER,
    OPPGAVE_TILHØRER_IKKE_ENF,
    UTVIKLER_MED_VEILDERROLLE,
}
