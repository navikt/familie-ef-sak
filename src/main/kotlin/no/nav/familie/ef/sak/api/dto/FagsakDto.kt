package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.api.dto.BehandlingDto
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import java.util.*

data class FagsakDto(val id: UUID,
                     val personIdent: String,
                     val stønadstype: Stønadstype,
                     val behandlinger: List<BehandlingDto>)
