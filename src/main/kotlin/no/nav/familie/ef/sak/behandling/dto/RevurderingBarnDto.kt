package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.barn.BehandlingBarn
import java.time.LocalDate
import java.util.UUID

data class RevurderingBarnDto(val personIdent: String? = null,
                              val navn: String? = null,
                              val fødselTerminDato: LocalDate? = null) {

    fun tilBehandlingBarn(behandlingId: UUID): BehandlingBarn = BehandlingBarn(
            behandlingId = behandlingId,
            søknadBarnId = null,
            personIdent = this.personIdent,
            navn = this.navn,
            fødselTermindato = this.fødselTerminDato)
}

fun List<RevurderingBarnDto>.tilBehandlingBarn(behandlingId: UUID): List<BehandlingBarn> = this.map {
    it.tilBehandlingBarn(behandlingId)
}


