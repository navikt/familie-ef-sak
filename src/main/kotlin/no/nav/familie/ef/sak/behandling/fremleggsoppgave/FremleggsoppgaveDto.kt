package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import java.util.UUID

data class FremleggsoppgaveDto(
    val inntekt: Boolean,
    val kanOppretteFremleggsoppgave: Boolean,
)

fun FremleggsoppgaveDto.tilDomene(behandlingId: UUID): Fremleggsoppgave {
    return Fremleggsoppgave(behandlingId = behandlingId, inntekt = this.inntekt)
}
