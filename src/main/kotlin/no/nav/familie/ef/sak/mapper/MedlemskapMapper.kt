package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.MedlemskapDto
import no.nav.familie.ef.sak.api.dto.MedlemskapRegisterGrunnlagDto
import no.nav.familie.ef.sak.api.dto.MedlemskapSøknadGrunnlagDto
import no.nav.familie.ef.sak.api.dto.UtenlandsoppholdDto
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.vurdering.medlemskap.Medlemskapshistorikk
import no.nav.familie.kontrakter.ef.søknad.Medlemskapsdetaljer
import org.springframework.stereotype.Component

@Component
class MedlemskapMapper(private val statsborgerskapMapper: StatsborgerskapMapper) {

    fun tilDto(medlemskapsdetaljer: Medlemskapsdetaljer,
               pdlSøker: PdlSøker,
               medlemskapshistorikk: Medlemskapshistorikk): MedlemskapDto {


        val statsborgerskap = statsborgerskapMapper.map(pdlSøker.statsborgerskap)
        return MedlemskapDto(
                søknadGrunnlag = MedlemskapSøknadGrunnlagDto(
                        bosattNorgeSisteÅrene = medlemskapsdetaljer.bosattNorgeSisteÅrene.verdi,
                        oppholderDuDegINorge = medlemskapsdetaljer.oppholderDuDegINorge.verdi,
                        utenlandsopphold = medlemskapsdetaljer.utenlandsopphold?.verdi?.map {
                            UtenlandsoppholdDto(it.fradato.verdi,
                                                it.tildato.verdi,
                                                it.årsakUtenlandsopphold.verdi)
                        } ?: emptyList()), //TODO emptylist?
                registerGrunnlag = MedlemskapRegisterGrunnlagDto(
                        //nåværendeStatsborgerskap - må avklare om det skal representeres som Norsk eller Norge, finner ikke "Norsk" mapping nå
                        nåværendeStatsborgerskap = statsborgerskap.filter { it.gyldigTilOgMed == null }.map { it.land },
                        statsborgerskap = statsborgerskap,
                        oppholdstatus = null
                ))


    }

}