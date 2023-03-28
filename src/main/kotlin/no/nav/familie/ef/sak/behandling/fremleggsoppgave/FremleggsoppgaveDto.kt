package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import no.nav.familie.kontrakter.ef.iverksett.FremleggsoppgaveType
import java.util.UUID

data class FremleggsoppgaveDto(
    val fremleggsoppgaveTyper: List<FremleggsoppgaveType>?,
    val kanOppretteFremleggsoppgave: Boolean,
)

fun FremleggsoppgaveDto.tilDomene(behandlingId: UUID): OpprettFremleggsoppgave {
    return OpprettFremleggsoppgave(behandlingId = behandlingId, oppgavetyper = this.fremleggsoppgaveTyper ?: emptyList())
}
