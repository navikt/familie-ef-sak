package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import java.util.UUID

data class OppgaverForOpprettelseDto(
    val oppgavetyper: List<OppgaveForOpprettelseType>?,
    val kanOppretteOppgaveForInntektAutomatisk: Boolean,
)

fun OppgaverForOpprettelseDto.tilDomene(behandlingId: UUID): OppgaverForOpprettelse {
    return OppgaverForOpprettelse(behandlingId = behandlingId, oppgavetyper = this.oppgavetyper ?: emptyList())
}
