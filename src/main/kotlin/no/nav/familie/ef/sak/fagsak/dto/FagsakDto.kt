package no.nav.familie.ef.sak.fagsak.dto

import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import java.util.UUID

data class FagsakDto(val id: UUID,
                     val personIdent: String,
                     val stønadstype: Stønadstype,
                     val erLøpende: Boolean,
                     val behandlinger: List<BehandlingDto>,
                     val eksternId: Long)

fun Fagsak.tilDto(behandlinger: List<BehandlingDto>, erLøpende: Boolean): FagsakDto =
        FagsakDto(id = this.id,
                  personIdent = this.hentAktivIdent(),
                  stønadstype = this.stønadstype,
                  erLøpende = erLøpende,
                  behandlinger = behandlinger,
                  eksternId = this.eksternId.id)