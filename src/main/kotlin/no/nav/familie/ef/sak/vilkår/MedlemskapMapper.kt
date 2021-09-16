package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.opplysninger.personopplysninger.UtflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.InnflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.OppholdstillatelseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.StatsborgerskapMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.Medlemskap
import no.nav.familie.ef.sak.service.KodeverkService
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class MedlemskapMapper(private val statsborgerskapMapper: StatsborgerskapMapper,
                       private val kodeverkService: KodeverkService,
                       private val adresseMapper: AdresseMapper) {

    fun tilDto(grunnlagsdata: GrunnlagsdataDomene,
               medlemskapsdetaljer: Medlemskap): MedlemskapDto {
        return MedlemskapDto(søknadsgrunnlag = mapSøknadsgrunnlag(medlemskapsdetaljer),
                             registergrunnlag = mapRegistergrunnlag(grunnlagsdata.søker, grunnlagsdata.medlUnntak))
    }

    fun mapSøknadsgrunnlag(medlemskapsdetaljer: Medlemskap): MedlemskapSøknadsgrunnlagDto {
        return MedlemskapSøknadsgrunnlagDto(bosattNorgeSisteÅrene = medlemskapsdetaljer.bosattNorgeSisteÅrene,
                                            oppholderDuDegINorge = medlemskapsdetaljer.oppholderDuDegINorge,
                                            utenlandsopphold = medlemskapsdetaljer.utenlandsopphold.map {
                                                UtenlandsoppholdDto(it.fradato,
                                                                    it.tildato,
                                                                    it.årsakUtenlandsopphold)
                                            })
    }

    fun mapRegistergrunnlag(søker: Søker,
                            medlUnntak: Medlemskapsinfo): MedlemskapRegistergrunnlagDto {
        val statsborgerskap = statsborgerskapMapper.map(søker.statsborgerskap)
        return MedlemskapRegistergrunnlagDto(nåværendeStatsborgerskap =
                                             statsborgerskap.filter { it.gyldigTilOgMedDato == null }
                                                     .map { it.land },
                                             statsborgerskap = statsborgerskap,
                                             oppholdstatus = OppholdstillatelseMapper.map(søker.opphold),
                                             bostedsadresse = søker.bostedsadresse.map(adresseMapper::tilAdresse),
                                             innflytting = mapInnflytting(søker.innflyttingTilNorge),
                                             utflytting = mapUtflytting(søker.utflyttingFraNorge),
                                             folkeregisterpersonstatus = søker.folkeregisterpersonstatus.gjeldende()
                                                     ?.let(Folkeregisterpersonstatus::fraPdl),
                                             medlUnntak = medlUnntak.tilDto())
    }

    private fun mapInnflytting(innflyttingTilNorge: List<InnflyttingTilNorge>): List<InnflyttingDto> =
            innflyttingTilNorge.map { innflytting ->
                InnflyttingDto(fraflyttingsland = innflytting.fraflyttingsland?.let {
                    kodeverkService.hentLand(it, LocalDate.now()) ?: it
                },
                               dato = null)
            }

    private fun mapUtflytting(utflyttingFraNorge: List<UtflyttingFraNorge>): List<UtflyttingDto> =
            utflyttingFraNorge.map { utflytting ->
                UtflyttingDto(tilflyttingsland = utflytting.tilflyttingsland?.let {
                    kodeverkService.hentLand(it, LocalDate.now()) ?: it
                },
                              dato = null)
            }

}