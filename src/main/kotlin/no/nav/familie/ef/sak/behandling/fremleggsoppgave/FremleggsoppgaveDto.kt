package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import java.util.UUID

class FremleggsoppgaveDto(
    private val behandlingId: UUID,
    private val opprettFremleggsoppgave: Boolean
)

fun Fremleggsoppgave.tilDto(): FremleggsoppgaveDto {
    return FremleggsoppgaveDto(this.behandlingId, this.opprettFremleggsoppgave)
}