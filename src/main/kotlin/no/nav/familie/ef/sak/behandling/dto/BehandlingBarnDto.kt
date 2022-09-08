package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto

data class BehandlingBarnDto(
    val nyeBarn: List<BarnMinimumDto>,
    val harBarnISisteIverksatteBehandling: Boolean
)
