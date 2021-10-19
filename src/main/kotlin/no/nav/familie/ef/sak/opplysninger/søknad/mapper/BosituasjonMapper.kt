package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonMinimumDto
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Bosituasjon
import no.nav.familie.ef.sak.vilkår.dto.BosituasjonDto
import no.nav.familie.ef.sak.vilkår.dto.DokumentasjonDto
import no.nav.familie.ef.sak.vilkår.dto.VedleggDto

object BosituasjonMapper {

    fun tilDto(bosituasjon: Bosituasjon): BosituasjonDto {

        val samboerDto = bosituasjon.samboer?.let { PersonMinimumDto(it.navn, it.fødselsdato, it.fødselsnummer) }

        return BosituasjonDto(bosituasjon.delerDuBolig,
                              samboerDto,
                              bosituasjon.sammenflyttingsdato,
                              bosituasjon.datoFlyttetFraHverandre,
                              bosituasjon.tidligereSamboerFortsattRegistrertPåAdresse?.let {
                                  DokumentasjonDto(it.harSendtInnTidligere,
                                                   it.dokumenter.map { dokument ->
                                                       VedleggDto(dokument.id,
                                                                  dokument.navn)
                                                   })
                              })

    }
}