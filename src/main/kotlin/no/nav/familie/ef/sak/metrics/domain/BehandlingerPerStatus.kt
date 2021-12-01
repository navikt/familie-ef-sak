package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype

data class BehandlingerPerStatus(val stonadstype: Stønadstype,
                                 val status: BehandlingStatus,
                                 val antall: Int)
