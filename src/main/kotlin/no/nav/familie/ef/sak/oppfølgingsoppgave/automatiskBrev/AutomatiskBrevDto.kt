package no.nav.familie.ef.sak.oppfølgingsoppgave.automatiskBrev

import no.nav.familie.ef.sak.brev.Brevmal
import java.util.UUID

data class AutomatiskBrevDto(
    val behandlingId: UUID,
    val brevSomSkalSendes: List<Brevmal>,
)
