package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.kontrakter.felles.ef.StønadType

data class BehandlingerPerStatus(val stonadstype: StønadType,
                                 val status: BehandlingStatus,
                                 val antall: Int)
