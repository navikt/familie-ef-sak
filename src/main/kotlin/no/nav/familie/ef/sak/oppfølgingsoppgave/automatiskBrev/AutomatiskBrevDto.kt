package no.nav.familie.ef.sak.oppfølgingsoppgave.automatiskBrev

import java.util.UUID

data class AutomatiskBrevDto(
    val behandlingId: UUID,
    val brev: List<String>,
)
