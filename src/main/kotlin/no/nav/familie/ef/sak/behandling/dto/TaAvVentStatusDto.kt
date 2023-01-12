package no.nav.familie.ef.sak.behandling.dto

data class TaAvVentStatusDto(
    val status: TaAvVentStatus
)

enum class TaAvVentStatus {
    OK,
    ANNEN_BEHANDLING_MÅ_FERDIGSTILLES,
    MÅ_OPPDATERES
}