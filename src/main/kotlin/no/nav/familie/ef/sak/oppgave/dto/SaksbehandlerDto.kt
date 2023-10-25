package no.nav.familie.ef.sak.oppgave.dto

data class SaksbehandlerDto(
    val fornavn: String,
    val etternavn: String,
    val rolle: SaksbehandlerRolle,
)

enum class SaksbehandlerRolle {
    IKKE_SATT,
    INNLOGGET_SAKSBEHANDLER,
    ANNEN_SAKSBEHANDLER,
    OPPGAVE_FINNES_IKKE,
    OPPGAVE_HAR_ANNET_TEMA_ENN_ENF,
    UTVIKLER_MED_VEILDERROLLE,
}
