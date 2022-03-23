package no.nav.familie.ef.sak.fagsak.dto

import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.util.UUID

data class FagsakDto(val id: UUID,
                     val fagsakPersonId: UUID,
                     val personIdent: String,
                     val stønadstype: StønadType,
                     val erLøpende: Boolean,
                     val erMigrert: Boolean,
                     val behandlinger: List<BehandlingDto>,
                     val eksternId: Long)

fun Fagsak.tilDto(behandlinger: List<BehandlingDto>, erLøpende: Boolean): FagsakDto =
        FagsakDto(id = this.id,
                  fagsakPersonId = this.fagsakPersonId,
                  personIdent = this.hentAktivIdent(),
                  stønadstype = this.stønadstype,
                  erLøpende = erLøpende,
                  erMigrert = this.migrert,
                  behandlinger = behandlinger,
                  eksternId = this.eksternId.id)