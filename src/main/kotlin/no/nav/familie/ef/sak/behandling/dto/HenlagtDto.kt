package no.nav.familie.ef.sak.behandling.dto

data class HenlagtDto(
    val årsak: HenlagtÅrsak,
    val sendHenlagtBrev: Boolean = false,
)
