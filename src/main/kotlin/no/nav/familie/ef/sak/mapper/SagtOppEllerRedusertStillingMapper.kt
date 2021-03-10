package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.DokumentasjonDto
import no.nav.familie.ef.sak.api.dto.SagtOppEllerRedusertStillingDto
import no.nav.familie.ef.sak.api.dto.VedleggDto
import no.nav.familie.ef.sak.repository.domain.søknad.Situasjon

object SagtOppEllerRedusertStillingMapper {

    fun tilDto(situasjon: Situasjon): SagtOppEllerRedusertStillingDto {
        val sagtOppEllerRedusertStilling = situasjon.sagtOppEllerRedusertStilling
        return SagtOppEllerRedusertStillingDto(sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling,
                                               årsak = situasjon.oppsigelseReduksjonÅrsak,
                                               dato = situasjon.oppsigelseReduksjonTidspunkt,
                                               dokumentasjon = situasjon.oppsigelseDokumentasjon?.let {
                                                   DokumentasjonDto(it.harSendtInnTidligere,
                                                                    it.dokumenter.map { dokument ->
                                                                        VedleggDto(dokument.id,
                                                                                   dokument.navn)
                                                                    })
                                               })
    }

}
