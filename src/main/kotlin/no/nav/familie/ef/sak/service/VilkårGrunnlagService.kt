package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.api.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.mapper.AktivitetMapper
import no.nav.familie.ef.sak.mapper.BarnMedSamværMapper
import no.nav.familie.ef.sak.mapper.BosituasjonMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.mapper.SagtOppEllerRedusertStillingMapper
import no.nav.familie.ef.sak.mapper.SivilstandMapper
import no.nav.familie.ef.sak.mapper.SivilstandsplanerMapper
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
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

        return VilkårGrunnlagDto(medlemskap = medlemskap,
                                 sivilstand = sivilstand,
                                 bosituasjon = BosituasjonMapper.tilDto(søknad.bosituasjon),
                                 barnMedSamvær = barnMedSamvær,
                                 sivilstandsplaner = sivilstandsplaner,
                                 aktivitet = aktivitet,
                                 sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling,
                                 lagtTilEtterFerdigstilling = registergrunnlagData.lagtTilEtterFerdigstilling)
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