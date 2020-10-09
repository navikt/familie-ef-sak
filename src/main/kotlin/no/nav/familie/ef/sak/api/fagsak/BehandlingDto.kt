package no.nav.familie.ef.sak.api.fagsak

import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import java.util.*

data class BehandlingDto (
        val id: UUID,
        val type: BehandlingType,
        val aktiv: Boolean,
        val status: BehandlingStatus
)
