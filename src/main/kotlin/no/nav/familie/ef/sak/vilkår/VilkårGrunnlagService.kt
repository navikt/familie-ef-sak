package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMedSamværMapper
import no.nav.familie.ef.sak.opplysninger.mapper.SivilstandMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknadsverdier
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.AdresseopplysningerMapper
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.AktivitetMapper
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.BosituasjonMapper
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SagtOppEllerRedusertStillingMapper
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SivilstandsplanerMapper
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.BarnepassDto
import no.nav.familie.ef.sak.vilkår.dto.PersonaliaDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

/**
 * Denne klassen håndterer henting av VilkårGrunnlagDto
 */
@Service
class VilkårGrunnlagService(
    private val medlemskapMapper: MedlemskapMapper,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val fagsakService: FagsakService,
    private val barnMedsamværMapper: BarnMedSamværMapper,
    private val adresseMapper: AdresseMapper,
) {
    fun hentGrunnlag(
        behandlingId: UUID,
        søknad: Søknadsverdier?,
        personident: String,
        barn: List<BehandlingBarn>,
    ): VilkårGrunnlagDto {
        val registergrunnlagData = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        val grunnlagsdata = registergrunnlagData.grunnlagsdata

        val aktivitet =
            søknad?.let {
                AktivitetMapper.tilDto(
                    aktivitet = it.aktivitet,
                    situasjon = it.situasjon,
                    søknadBarn = it.barn,
                    it.datoPåbegyntSøknad,
                )
            }
        val søknadsbarn = søknad?.barn ?: emptyList()
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val barnMedSamvær =
            mapBarnMedSamvær(
                søknad?.fødselsnummer ?: personident,
                grunnlagsdata,
                barn,
                søknadsbarn,
                stønadstype,
                registergrunnlagData.opprettetTidspunkt.toLocalDate(),
            )
        val medlemskap = medlemskapMapper.tilDto(grunnlagsdata, søknad?.medlemskap)
        val sivilstand = SivilstandMapper.tilDto(grunnlagsdata, søknad?.sivilstand)
        val sivilstandsplaner = SivilstandsplanerMapper.tilDto(sivilstandsplaner = søknad?.sivilstandsplaner)
        val sagtOppEllerRedusertStilling = søknad?.situasjon?.let { SagtOppEllerRedusertStillingMapper.tilDto(situasjon = it) }

        return VilkårGrunnlagDto(
            personalia =
                PersonaliaDto(
                    navn = NavnDto.fraNavn(grunnlagsdata.søker.navn),
                    personIdent = personident,
                    bostedsadresse =
                        grunnlagsdata.søker.bostedsadresse
                            .gjeldende()
                            ?.let { adresseMapper.tilAdresse(it) },
                    fødeland =
                        grunnlagsdata.søker.fødsel
                            .first()
                            .fødeland,
                ),
            tidligereVedtaksperioder = grunnlagsdata.tidligereVedtaksperioder.tilDto(),
            medlemskap = medlemskap,
            sivilstand = sivilstand,
            bosituasjon = søknad?.let { BosituasjonMapper.tilDto(it.bosituasjon) },
            barnMedSamvær = barnMedSamvær,
            sivilstandsplaner = sivilstandsplaner,
            aktivitet = aktivitet,
            sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling,
            registeropplysningerOpprettetTid = registergrunnlagData.opprettetTidspunkt,
            adresseopplysninger = AdresseopplysningerMapper.tilDto(søknad?.adresseopplysninger),
            dokumentasjon = søknad?.dokumentasjon,
            harAvsluttetArbeidsforhold = grunnlagsdata.harAvsluttetArbeidsforhold,
            kontantstøttePerioder = grunnlagsdata.kontantstøttePerioder,
        )
    }

    private fun mapBarnMedSamvær(
        personIdentSøker: String,
        grunnlagsdata: GrunnlagsdataDomene,
        barn: List<BehandlingBarn>,
        søknadsbarn: Collection<SøknadBarn>,
        stønadstype: StønadType,
        grunnlagsdataOpprettet: LocalDate,
    ): List<BarnMedSamværDto> {
        val barnMedSamværRegistergrunnlag =
            barnMedsamværMapper.mapRegistergrunnlag(
                personIdentSøker,
                grunnlagsdata.barn,
                grunnlagsdata.annenForelder,
                barn,
                søknadsbarn,
                grunnlagsdata.søker.bostedsadresse,
                grunnlagsdataOpprettet,
            )
        val søknadsgrunnlag = barnMedsamværMapper.mapSøknadsgrunnlag(barn, søknadsbarn)
        val barnepass: List<BarnepassDto> =
            when (stønadstype) {
                StønadType.BARNETILSYN -> barnMedsamværMapper.mapBarnepass(barn, søknadsbarn)
                else -> emptyList()
            }
        return barnMedsamværMapper
            .slåSammenBarnMedSamvær(søknadsgrunnlag, barnMedSamværRegistergrunnlag, barnepass)
            .sortedByDescending {
                it.registergrunnlag.fødselsdato ?: it.søknadsgrunnlag.fødselTermindato
            }
    }
}
