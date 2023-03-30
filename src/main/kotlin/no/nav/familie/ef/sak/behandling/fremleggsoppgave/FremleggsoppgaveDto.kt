package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import no.nav.familie.kontrakter.ef.iverksett.FremleggsoppgaveType
import java.util.UUID

data class FremleggWrapper(
    val oppgavetyperSomKanOpprettes: List<FremleggsoppgaveType>,
    val oppgavetyperSomSkalOpprettes: List<FremleggsoppgaveType>,
    val opprettelseTattStillingTil: Boolean
)

fun FremleggWrapper.tilDomene(behandlingId: UUID): Fremleggsoppgave {
    return Fremleggsoppgave(
        behandlingId = behandlingId,
        oppgavetyper = this.oppgavetyperSomSkalOpprettes,
        opprettelseTattStillingTil = this.opprettelseTattStillingTil
    )
}
