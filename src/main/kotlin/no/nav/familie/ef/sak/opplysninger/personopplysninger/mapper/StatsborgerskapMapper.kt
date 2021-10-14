package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.felles.kodeverk.KodeverkService
import no.nav.familie.ef.sak.felles.util.datoEllerIdag
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.ef.sak.vilkår.dto.StatsborgerskapDto
import org.springframework.stereotype.Component

@Component
class StatsborgerskapMapper(private val kodeverkService: KodeverkService) {

    fun map(statsborgerskap: List<Statsborgerskap>): List<StatsborgerskapDto> {
        return statsborgerskap.map {
            val land = kodeverkService.hentLand(it.land, datoEllerIdag(it.gyldigFraOgMed)) ?: it.land
            StatsborgerskapDto(land = land,
                               gyldigFraOgMedDato = it.gyldigFraOgMed,
                               gyldigTilOgMedDato = it.gyldigTilOgMed)
        }
    }
}