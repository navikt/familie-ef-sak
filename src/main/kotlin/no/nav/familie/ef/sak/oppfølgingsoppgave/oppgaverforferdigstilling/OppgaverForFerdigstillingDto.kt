package no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling

import java.util.UUID

data class OppgaverForFerdigstillingDto(
    val behandlingId: UUID,
    val oppgaveIder: List<Long>,
)
