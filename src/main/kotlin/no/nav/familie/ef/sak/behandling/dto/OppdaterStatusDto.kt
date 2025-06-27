package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus

data class OppdaterStatusDto(
    val status: BehandlingStatus,
)
