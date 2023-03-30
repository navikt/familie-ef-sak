package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import java.util.UUID

data class OppgaverForOpprettelseDto(
    val oppgavetyperSomKanOpprettes: List<OppgaveForOpprettelseType>,
    val oppgavetyperSomSkalOpprettes: List<OppgaveForOpprettelseType>,
    val opprettelseTattStillingTil: Boolean
)

fun OppgaverForOpprettelseDto.tilDomene(behandlingId: UUID): OppgaverForOpprettelse {
    return OppgaverForOpprettelse(
        behandlingId = behandlingId,
        oppgavetyper = this.oppgavetyperSomSkalOpprettes,
        opprettelseTattStillingTil = this.opprettelseTattStillingTil
    )
}
