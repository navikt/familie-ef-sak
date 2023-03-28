package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import no.nav.familie.kontrakter.ef.iverksett.FremleggsoppgaveType
import org.springframework.data.annotation.Id
import java.util.UUID

data class OpprettFremleggsoppgave(
    @Id
    val behandlingId: UUID,
    val oppgaveType: List<FremleggsoppgaveType>,
)
