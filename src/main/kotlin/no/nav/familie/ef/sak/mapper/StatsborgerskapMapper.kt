package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.StatsborgerskapDto
import no.nav.familie.ef.sak.integration.dto.pdl.Statsborgerskap
import no.nav.familie.ef.sak.service.KodeverkService
import no.nav.familie.ef.sak.util.datoEllerIdag
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