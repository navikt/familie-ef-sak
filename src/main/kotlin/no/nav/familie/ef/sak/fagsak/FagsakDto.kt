package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import java.util.UUID

data class FagsakDto(val id: UUID,
                     val personIdent: String,
                     val stønadstype: Stønadstype,
                     val erLøpende: Boolean,
                     val behandlinger: List<BehandlingDto>)

fun Fagsak.tilDto(behandlinger: List<BehandlingDto>, erLøpende: Boolean): FagsakDto =
        FagsakDto(id = this.id,
                  personIdent = this.hentAktivIdent(),
                  stønadstype = this.stønadstype,
                  erLøpende = erLøpende,
                  behandlinger = behandlinger)