package no.nav.familie.ef.sak.behandling.dto

import java.util.UUID

data class TaAvVentStatusDto(
    val status: TaAvVentStatus,
    val nyForrigeBehandlingId: UUID? = null
)

enum class TaAvVentStatus {
    OK,
    ANNEN_BEHANDLING_MÅ_FERDIGSTILLES,
    MÅ_NULSTILLE_VEDTAK
}
