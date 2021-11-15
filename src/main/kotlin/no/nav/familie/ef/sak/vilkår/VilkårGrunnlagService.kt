package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.opplysninger.mapper.BarnMedSamværMapper
import no.nav.familie.ef.sak.opplysninger.mapper.SivilstandMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.AktivitetMapper
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.BosituasjonMapper
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SagtOppEllerRedusertStillingMapper
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SivilstandsplanerMapper
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.FinnesTidligereVedtaksperioder
import no.nav.familie.ef.sak.vilkår.dto.TidligereVedtaksperioderDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Denne klassen håndterer henting av VilkårGrunnlagDto
 */
@Service
class VilkårGrunnlagService(private val medlemskapMapper: MedlemskapMapper,
                            private val grunnlagsdataService: GrunnlagsdataService) {


    fun hentGrunnlag(behandlingId: UUID,
                     søknad: SøknadsskjemaOvergangsstønad): VilkårGrunnlagDto {
        val registergrunnlagData = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        val grunnlagsdata = registergrunnlagData.grunnlagsdata

        val aktivitet = AktivitetMapper.tilDto(aktivitet = søknad.aktivitet, situasjon = søknad.situasjon, barn = søknad.barn)
        val barnMedSamvær = mapBarnMedSamvær(grunnlagsdata, søknad)
        val medlemskap = medlemskapMapper.tilDto(grunnlagsdata, søknad.medlemskap)
        val sivilstand = SivilstandMapper.tilDto(grunnlagsdata, søknad.sivilstand)
        val sivilstandsplaner = SivilstandsplanerMapper.tilDto(sivilstandsplaner = søknad.sivilstandsplaner)
        val sagtOppEllerRedusertStilling = SagtOppEllerRedusertStillingMapper.tilDto(situasjon = søknad.situasjon)

        return VilkårGrunnlagDto(tidligereVedtaksperioder = mapTidligereVedtaksperioder(grunnlagsdata.tidligereVedtaksperioder),
                                 medlemskap = medlemskap,
                                 sivilstand = sivilstand,
                                 bosituasjon = BosituasjonMapper.tilDto(søknad.bosituasjon),
                                 barnMedSamvær = barnMedSamvær,
                                 sivilstandsplaner = sivilstandsplaner,
                                 aktivitet = aktivitet,
                                 sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling,
                                 lagtTilEtterFerdigstilling = registergrunnlagData.lagtTilEtterFerdigstilling)
    }

    private fun mapTidligereVedtaksperioder(tidligereVedtaksperioder: TidligereVedtaksperioder?): TidligereVedtaksperioderDto {
        val infotrygd = tidligereVedtaksperioder?.infotrygd?.let {
            FinnesTidligereVedtaksperioder(overgangsstønad = it.overgangsstønad,
                                           barnetilsyn = it.barnetilsyn,
                                           skolepenger = it.skolepenger)
        }
        return TidligereVedtaksperioderDto(
                infotrygd = infotrygd
        )
    }

    private fun mapBarnMedSamvær(grunnlagsdata: GrunnlagsdataDomene,
                                 søknad: SøknadsskjemaOvergangsstønad): List<BarnMedSamværDto> {
        val søker = grunnlagsdata.søker
        val barnMedSamværRegistergrunnlag = BarnMedSamværMapper.mapRegistergrunnlag(grunnlagsdata.barn,
                                                                                    grunnlagsdata.annenForelder,
                                                                                    søknad,
                                                                                    søker.bostedsadresse)
        return BarnMedSamværMapper.slåSammenBarnMedSamvær(BarnMedSamværMapper.mapSøknadsgrunnlag(søknad.barn),
                                                          barnMedSamværRegistergrunnlag)
                .sortedByDescending {
                    it.registergrunnlag.fødselsnummer?.let { fødsesnummer -> Fødselsnummer(fødsesnummer).fødselsdato }
                    ?: it.søknadsgrunnlag.fødselTermindato
                }
    }
}