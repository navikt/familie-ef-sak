package no.nav.familie.ef.sak.fagsak.dto

import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.fagsak.domain.FagsakMedPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import java.util.UUID

data class FagsakDto(val id: UUID,
                     val fagsakPersonId: UUID,
                     val personIdent: String,
                     val stønadstype: Stønadstype,
                     val erLøpende: Boolean,
                     val erMigrert: Boolean,
                     val behandlinger: List<BehandlingDto>,
                     val eksternId: Long)

fun FagsakMedPerson.tilDto(behandlinger: List<BehandlingDto>, erLøpende: Boolean): FagsakDto =
        FagsakDto(id = this.id,
                  fagsakPersonId = this.fagsakPersonId,
                  personIdent = this.hentAktivIdent(),
                  stønadstype = this.stønadstype,
                  erLøpende = erLøpende,
                  erMigrert = this.migrert,
                  behandlinger = behandlinger,
                  eksternId = this.eksternId.id)