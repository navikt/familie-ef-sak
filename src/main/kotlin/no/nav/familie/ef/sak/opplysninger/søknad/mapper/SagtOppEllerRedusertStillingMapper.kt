package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.Situasjon
import no.nav.familie.ef.sak.vilkår.dto.SagtOppEllerRedusertStillingDto

object SagtOppEllerRedusertStillingMapper {

    fun tilDto(situasjon: Situasjon): SagtOppEllerRedusertStillingDto {
        val sagtOppEllerRedusertStilling = situasjon.sagtOppEllerRedusertStilling
        return SagtOppEllerRedusertStillingDto(sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling,
                                               årsak = situasjon.oppsigelseReduksjonÅrsak,
                                               dato = situasjon.oppsigelseReduksjonTidspunkt)
    }

}
