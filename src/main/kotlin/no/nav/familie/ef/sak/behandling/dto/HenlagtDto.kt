package no.nav.familie.ef.sak.behandling.dto

data class HenlagtDto(
    val årsak: HenlagtÅrsak,
    val skalSendeHenleggelsesbrev: Boolean = false,
    val saksbehandlerSignatur: String,
)
