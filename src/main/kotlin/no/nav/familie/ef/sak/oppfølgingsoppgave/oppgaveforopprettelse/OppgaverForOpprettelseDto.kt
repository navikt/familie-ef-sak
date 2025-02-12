package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.OppgaverForOpprettelse
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import java.util.UUID

data class OppgaverForOpprettelseDto(
    val oppgavetyperSomKanOpprettes: List<OppgaveForOpprettelseType>,
    val oppgavetyperSomSkalOpprettes: List<OppgaveForOpprettelseType>,
)

fun OppgaverForOpprettelseDto.tilDomene(behandlingId: UUID): OppgaverForOpprettelse =
    OppgaverForOpprettelse(
        behandlingId = behandlingId,
        oppgavetyper = this.oppgavetyperSomSkalOpprettes,
    )
