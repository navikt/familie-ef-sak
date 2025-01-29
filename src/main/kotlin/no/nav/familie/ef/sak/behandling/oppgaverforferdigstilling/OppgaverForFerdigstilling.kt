package no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling

import org.springframework.data.annotation.Id
import java.util.UUID

data class OppgaverForFerdigstilling(
    @Id
    val behandlingId: UUID,
    val fremleggsoppgaveIderSomSkalFerdigstilles: List<Long>,
)
