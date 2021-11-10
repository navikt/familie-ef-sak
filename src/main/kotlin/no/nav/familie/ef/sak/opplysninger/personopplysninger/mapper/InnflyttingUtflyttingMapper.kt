package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.felles.kodeverk.KodeverkService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.InnflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.UtflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class InnflyttingUtflyttingMapper(val kodeverkService: KodeverkService) {

    fun mapInnflytting(innflytting: List<InnflyttingTilNorge>): List<InnflyttingDto> =
            innflytting.map {
                InnflyttingDto(it.fraflyttingsland?.let { land -> kodeverkService.hentLand(land, LocalDate.now()) },
                               it.folkeregistermetadata.gyldighetstidspunkt?.toLocalDate(),
                               it.fraflyttingsstedIUtlandet)
            }.sortedByDescending { it.dato ?: LocalDate.MIN }

    fun mapUtflytting(utflytting: List<UtflyttingFraNorge>): List<UtflyttingDto> =
            utflytting.map {
                UtflyttingDto(it.tilflyttingsland?.let { land -> kodeverkService.hentLand(land, LocalDate.now()) },
                              it.utflyttingsdato,
                              it.tilflyttingsstedIUtlandet)
            }.sortedByDescending { it.dato ?: LocalDate.MIN }
}
