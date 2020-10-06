package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.integration.dto.pdl.Folkeregistermetadata
import no.nav.familie.ef.sak.integration.dto.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.service.KodeverkService
import no.nav.familie.kontrakter.ef.søknad.Medlemskapsdetaljer
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class MedlemskapMapper(private val statsborgerskapMapper: StatsborgerskapMapper,
                       private val kodeverkService: KodeverkService,
                       private val adresseMapper: AdresseMapper) {

    fun tilDto(medlemskapsdetaljer: Medlemskapsdetaljer,
               pdlSøker: PdlSøker): MedlemskapDto {

        val statsborgerskap = statsborgerskapMapper.map(pdlSøker.statsborgerskap)
        val søknadGrunnlag = MedlemskapSøknadGrunnlagDto(
                bosattNorgeSisteÅrene = medlemskapsdetaljer.bosattNorgeSisteÅrene.verdi,
                oppholderDuDegINorge = medlemskapsdetaljer.oppholderDuDegINorge.verdi,
                utenlandsopphold = medlemskapsdetaljer.utenlandsopphold?.verdi?.map {
                    UtenlandsoppholdDto(it.fradato.verdi,
                                        it.tildato.verdi,
                                        it.årsakUtenlandsopphold.verdi)
                } ?: emptyList())
        val registerGrunnlag = MedlemskapRegisterGrunnlagDto(
                nåværendeStatsborgerskap = statsborgerskap.filter { it.gyldigTilOgMedDato == null }.map { it.land },
                statsborgerskap = statsborgerskap,
                oppholdstatus = OppholdstillatelseMapper.map(pdlSøker.opphold),
                bostedsadresse = pdlSøker.bostedsadresse.map(adresseMapper::tilAdresse),
                innflytting = mapInnflytting(pdlSøker.innflyttingTilNorge),
                utflytting = mapUtflytting(pdlSøker.utflyttingFraNorge),
                folkeregisterpersonstatus = pdlSøker.folkeregisterpersonstatus.firstOrNull()
                        ?.let { Folkeregisterpersonstatus.fraPdl(it) }
        )
        return MedlemskapDto(
                søknadGrunnlag = søknadGrunnlag,
                registerGrunnlag = registerGrunnlag)
    }

    private fun mapInnflytting(innflyttingTilNorge: List<InnflyttingTilNorge>): List<InnflyttingDto> =
            innflyttingTilNorge.map { innflytting ->
                InnflyttingDto(fraflyttingsland = innflytting.fraflyttingsland?.let {
                    kodeverkService.hentLand(it, LocalDate.now()) ?: it
                }, dato = finnMinDatoRegistrert(innflytting.folkeregistermetadata)?.toLocalDate())
            }

    private fun mapUtflytting(utflyttingFraNorge: List<UtflyttingFraNorge>): List<UtflyttingDto> =
            utflyttingFraNorge.map { utflytting ->
                UtflyttingDto(tilflyttingsland = utflytting.tilflyttingsland?.let {
                    kodeverkService.hentLand(it, LocalDate.now()) ?: it
                }, dato = finnMinDatoRegistrert(utflytting.folkeregistermetadata)?.toLocalDate())
            }

    private fun finnMinDatoRegistrert(folkeregistermetadata: Folkeregistermetadata) =
            min(folkeregistermetadata.gyldighetstidspunkt, folkeregistermetadata.opphørstidspunkt)

    private fun min(dato1: LocalDateTime?, dato2: LocalDateTime?): LocalDateTime? =
            if (dato1 == null) {
                dato2
            } else if (dato2 == null || dato1.isBefore(dato2)) {
                dato1
            } else {
                dato2
            }

}