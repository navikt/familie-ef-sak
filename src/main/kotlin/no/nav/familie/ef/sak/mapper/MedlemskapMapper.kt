package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.integration.dto.pdl.Folkeregistermetadata
import no.nav.familie.ef.sak.integration.dto.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.repository.domain.søknad.Medlemskap
import no.nav.familie.ef.sak.service.KodeverkService
import no.nav.familie.ef.sak.util.min
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class MedlemskapMapper(private val statsborgerskapMapper: StatsborgerskapMapper,
                       private val kodeverkService: KodeverkService,
                       private val adresseMapper: AdresseMapper) {

    fun tilDto(medlemskapsdetaljer: Medlemskap,
               pdlSøker: PdlSøker): MedlemskapDto {

        val statsborgerskap = statsborgerskapMapper.map(pdlSøker.statsborgerskap)
        val søknadsgrunnlag = MedlemskapSøknadsgrunnlagDto(
                bosattNorgeSisteÅrene = medlemskapsdetaljer.bosattNorgeSisteÅrene,
                oppholderDuDegINorge = medlemskapsdetaljer.oppholderDuDegINorge,
                utenlandsopphold = medlemskapsdetaljer.utenlandsopphold.map {
                    UtenlandsoppholdDto(it.fradato,
                                        it.tildato,
                                        it.årsakUtenlandsopphold)
                } )
        val registergrunnlag = MedlemskapRegistergrunnlagDto(
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
                søknadsgrunnlag = søknadsgrunnlag,
                registergrunnlag = registergrunnlag)
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

}