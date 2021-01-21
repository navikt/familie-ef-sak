package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.BosituasjonDto
import no.nav.familie.ef.sak.api.dto.DokumentasjonDto
import no.nav.familie.ef.sak.api.dto.PersonMinimumDto
import no.nav.familie.ef.sak.api.dto.VedleggDto
import no.nav.familie.ef.sak.repository.domain.søknad.Bosituasjon

object BosituasjonMapper {

    fun tilDto(bosituasjon: Bosituasjon): BosituasjonDto {

        val samboerDto = bosituasjon.samboer?.let { PersonMinimumDto(it.navn, it.fødselsdato, it.fødselsnummer) }

        return BosituasjonDto(bosituasjon.delerDuBolig,
                              samboerDto,
                              bosituasjon.sammenflyttingsdato,
                              bosituasjon.datoFlyttetFraHverandre,
                              bosituasjon.tidligereSamboerFortsattRegistrertPåAdresse?.let { DokumentasjonDto(it.harSendtInnTidligere,
                                                                                                              it.dokumenter.map {
                                                                                                                  VedleggDto(it.id,
                                                                                                                             it.navn)
                                                                                                              })
                              })

    }
}