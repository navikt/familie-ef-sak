package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.SagtOppEllerRedusertStillingDto
import no.nav.familie.ef.sak.repository.domain.søknad.Situasjon

object SagtOppEllerRedusertStillingMapper {

    fun tilDto(situasjon: Situasjon): SagtOppEllerRedusertStillingDto {
        val sagtOppEllerRedusertStilling = situasjon.sagtOppEllerRedusertStilling
        return SagtOppEllerRedusertStillingDto(sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling,
                                               årsak = situasjon.oppsigelseReduksjonÅrsak,
                                               dato = situasjon.oppsigelseReduksjonTidspunkt)
    }

}
