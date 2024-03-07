package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.felles.kodeverk.KodeverkService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.InnflyttingUtflyttingMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.OppholdstillatelseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.StatsborgerskapMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Medlemskap
import no.nav.familie.ef.sak.vilkår.dto.MedlemskapDto
import no.nav.familie.ef.sak.vilkår.dto.MedlemskapRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.MedlemskapSøknadsgrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.UtenlandsoppholdDto
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class MedlemskapMapper(
    private val statsborgerskapMapper: StatsborgerskapMapper,
    private val innflyttingUtflyttingMapper: InnflyttingUtflyttingMapper,
    private val adresseMapper: AdresseMapper,
    private val kodeverkService: KodeverkService,
) {

    fun tilDto(
        grunnlagsdata: GrunnlagsdataDomene,
        medlemskapsdetaljer: Medlemskap?,
    ): MedlemskapDto {
        return MedlemskapDto(
            søknadsgrunnlag = medlemskapsdetaljer?.let { mapSøknadsgrunnlag(it) },
            registergrunnlag = mapRegistergrunnlag(grunnlagsdata.søker, grunnlagsdata.medlUnntak),
        )
    }

    fun mapSøknadsgrunnlag(medlemskapsdetaljer: Medlemskap): MedlemskapSøknadsgrunnlagDto {
        return MedlemskapSøknadsgrunnlagDto(
            bosattNorgeSisteÅrene = medlemskapsdetaljer.bosattNorgeSisteÅrene,
            oppholderDuDegINorge = medlemskapsdetaljer.oppholderDuDegINorge,
            oppholdsland = medlemskapsdetaljer.oppholdsland?.let { kodeverkService.hentLand(it, LocalDate.now()) },
            utenlandsopphold = medlemskapsdetaljer.utenlandsopphold.map {
                UtenlandsoppholdDto(
                    fraDato = it.fradato,
                    tilDato = it.tildato,
                    land = it.land?.let { land -> kodeverkService.hentLand(land, LocalDate.now()) },
                    årsak = it.årsakUtenlandsopphold,
                    personidentUtland = it.personidentUtland,
                    adresseUtland = it.adresseUtland,
                )
            },
        )
    }

    fun mapRegistergrunnlag(
        søker: Søker,
        medlUnntak: Medlemskapsinfo,
    ): MedlemskapRegistergrunnlagDto {
        val statsborgerskap = statsborgerskapMapper.map(søker.statsborgerskap)
        return MedlemskapRegistergrunnlagDto(
            nåværendeStatsborgerskap =
            statsborgerskap.filter { it.gyldigTilOgMedDato == null }
                .map { it.land },
            statsborgerskap = statsborgerskap,
            oppholdstatus = OppholdstillatelseMapper.map(søker.opphold),
            bostedsadresse = søker.bostedsadresse.map(adresseMapper::tilAdresse),
            innflytting = innflyttingUtflyttingMapper.mapInnflytting(søker.innflyttingTilNorge),
            utflytting = innflyttingUtflyttingMapper.mapUtflytting(søker.utflyttingFraNorge),
            folkeregisterpersonstatus = søker.folkeregisterpersonstatus.gjeldende()
                ?.let(Folkeregisterpersonstatus::fraPdl),
            medlUnntak = medlUnntak.tilDto(),
        )
    }
}
