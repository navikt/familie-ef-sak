package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import org.springframework.data.annotation.Id
import java.util.UUID

data class Fremleggsoppgave(
    @Id
    val behandlingId: UUID,
    val inntekt: Boolean
)