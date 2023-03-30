package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import org.springframework.data.annotation.Id
import java.util.UUID

data class OppgaverForOpprettelse(
    @Id
    val behandlingId: UUID,
    val oppgavetyper: List<OppgaveForOpprettelseType>,
    val opprettelseTattStillingTil: Boolean,
)
