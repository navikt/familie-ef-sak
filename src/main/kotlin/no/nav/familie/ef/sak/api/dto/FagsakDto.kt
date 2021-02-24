package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import java.util.UUID

data class FagsakDto(val id: UUID,
                     val personIdent: String,
                     val stønadstype: Stønadstype,
                     val behandlinger: List<BehandlingDto>)

fun Fagsak.tilDto(behandlinger: List<BehandlingDto>): FagsakDto =
        FagsakDto(id = this.id,
                  personIdent = this.hentAktivIdent(),
                  stønadstype = this.stønadstype,
                  behandlinger = behandlinger)