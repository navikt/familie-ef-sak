package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMedSamværMapper
import no.nav.familie.ef.sak.opplysninger.mapper.SivilstandMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.AktivitetMapper
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.BosituasjonMapper
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SagtOppEllerRedusertStillingMapper
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SivilstandsplanerMapper
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.TidligereInnvilgetVedtakDto
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
                     søknad: SøknadsskjemaOvergangsstønad?,
                     personident: String,
                     barn: List<BehandlingBarn>): VilkårGrunnlagDto {
        val registergrunnlagData = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        val grunnlagsdata = registergrunnlagData.grunnlagsdata

        val aktivitet = søknad?.let { AktivitetMapper.tilDto(aktivitet = it.aktivitet, situasjon = it.situasjon, søknadBarn = it.barn) }
        val søknadsbarn = søknad?.barn ?: emptyList()
        val barnMedSamvær = mapBarnMedSamvær(søknad?.fødselsnummer ?: personident, grunnlagsdata, barn, søknadsbarn)
        val medlemskap = medlemskapMapper.tilDto(grunnlagsdata, søknad?.medlemskap)
        val sivilstand = SivilstandMapper.tilDto(grunnlagsdata, søknad?.sivilstand)
        val sivilstandsplaner = SivilstandsplanerMapper.tilDto(sivilstandsplaner = søknad?.sivilstandsplaner)
        val sagtOppEllerRedusertStilling = søknad?.let { SagtOppEllerRedusertStillingMapper.tilDto(situasjon = it.situasjon) }

        return VilkårGrunnlagDto(tidligereVedtaksperioder = mapTidligereVedtaksperioder(grunnlagsdata.tidligereVedtaksperioder),
                                 medlemskap = medlemskap,
                                 sivilstand = sivilstand,
                                 bosituasjon = søknad?.let { BosituasjonMapper.tilDto(it.bosituasjon) },
                                 barnMedSamvær = barnMedSamvær,
                                 sivilstandsplaner = sivilstandsplaner,
                                 aktivitet = aktivitet,
                                 sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling,
                                 lagtTilEtterFerdigstilling = registergrunnlagData.lagtTilEtterFerdigstilling,
                                 registeropplysningerOpprettetTid = registergrunnlagData.opprettetTidspunkt)
    }

    private fun mapTidligereVedtaksperioder(tidligereVedtaksperioder: TidligereVedtaksperioder?): TidligereVedtaksperioderDto {
        val infotrygd = tidligereVedtaksperioder?.infotrygd?.let {
            TidligereInnvilgetVedtakDto(harTidligereOvergangsstønad = it.harTidligereOvergangsstønad,
                                        harTidligereBarnetilsyn = it.harTidligereBarnetilsyn,
                                        harTidligereSkolepenger = it.harTidligereSkolepenger)
        }
        return TidligereVedtaksperioderDto(infotrygd = infotrygd)
    }

    private fun mapBarnMedSamvær(personIdentSøker: String,
                                 grunnlagsdata: GrunnlagsdataDomene,
                                 barn: List<BehandlingBarn>,
                                 søknadsbarn: Collection<SøknadBarn>): List<BarnMedSamværDto> {
        val barnMedSamværRegistergrunnlag = BarnMedSamværMapper.mapRegistergrunnlag(personIdentSøker,
                                                                                    grunnlagsdata.barn,
                                                                                    grunnlagsdata.annenForelder,
                                                                                    barn,
                                                                                    søknadsbarn,
                                                                                    grunnlagsdata.søker.bostedsadresse)
        return BarnMedSamværMapper.slåSammenBarnMedSamvær(BarnMedSamværMapper.mapSøknadsgrunnlag(barn, søknadsbarn),
                                                          barnMedSamværRegistergrunnlag)
                .sortedByDescending {
                    it.registergrunnlag.fødselsnummer?.let { fødsesnummer -> Fødselsnummer(fødsesnummer).fødselsdato }
                    ?: it.søknadsgrunnlag.fødselTermindato
                }
    }
}