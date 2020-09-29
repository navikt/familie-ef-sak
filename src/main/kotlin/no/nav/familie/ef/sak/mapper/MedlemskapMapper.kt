package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.kontrakter.ef.søknad.Medlemskapsdetaljer
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.springframework.stereotype.Component

@Component
class MedlemskapMapper(private val statsborgerskapMapper: StatsborgerskapMapper) {

    fun tilDto(medlemskapsdetaljer: Medlemskapsdetaljer,
               pdlSøker: PdlSøker,
               medlemskapsinfo: Medlemskapsinfo): MedlemskapDto {


        val statsborgerskap = statsborgerskapMapper.map(pdlSøker.statsborgerskap)
        return MedlemskapDto(
                søknadGrunnlag = MedlemskapSøknadGrunnlagDto(
                        bosattNorgeSisteÅrene = medlemskapsdetaljer.bosattNorgeSisteÅrene.verdi,
                        oppholderDuDegINorge = medlemskapsdetaljer.oppholderDuDegINorge.verdi,
                        utenlandsopphold = medlemskapsdetaljer.utenlandsopphold?.verdi?.map {
                            UtenlandsoppholdDto(it.fradato.verdi,
                                                it.tildato.verdi,
                                                it.årsakUtenlandsopphold.verdi)
                        } ?: emptyList()),
                registerGrunnlag = MedlemskapRegisterGrunnlagDto(
                        nåværendeStatsborgerskap = statsborgerskap.filter { it.gyldigTilOgMed == null }.map { it.land },
                        statsborgerskap = statsborgerskap,
                        oppholdstatus = OppholdstillatelseMapper.map(pdlSøker.opphold),
                        medlemskapsinfo = medlemskapsinfo //TODO map denne om til en dto
                ))
    }

}