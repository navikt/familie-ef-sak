package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.InngangsvilkårGrunnlagDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.ef.sak.mapper.*
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vurdering.utledDelvilkårResultat
import no.nav.familie.ef.sak.vurdering.validerDelvilkår
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class VurderingService(private val behandlingService: BehandlingService,
                       private val pdlClient: PdlClient,
                       private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
                       private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                       private val medlemskapMapper: MedlemskapMapper) {

    fun oppdaterVilkår(vilkårsvurderingDto: VilkårsvurderingDto): UUID {
        val vilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurderingDto.id)

        val behandlingId = vilkårsvurdering.behandlingId
        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            throw Feil("Bruker prøver å oppdatere en vilkårsvurdering der behandling=$behandlingId er låst for videre redigering",
                       "Behandlingen er låst for videre redigering")
        }

        validerDelvilkår(vilkårsvurderingDto, vilkårsvurdering)

        val nyVilkårsvurdering =
                vilkårsvurdering.copy(resultat = vilkårsvurderingDto.resultat,
                                      begrunnelse = vilkårsvurderingDto.begrunnelse,
                                      unntak = vilkårsvurderingDto.unntak,
                                      delvilkårsvurdering =
                                      DelvilkårsvurderingWrapper(vilkårsvurderingDto.delvilkårsvurderinger
                                                                         .map { delvurdering ->
                                                                             Delvilkårsvurdering(delvurdering.type,
                                                                                                 delvurdering.resultat,
                                                                                                 delvurdering.årsak,
                                                                                                 delvurdering.begrunnelse)
                                                                         })
                )
        return vilkårsvurderingRepository.update(nyVilkårsvurdering).id
    }

    fun hentInngangsvilkår(behandlingId: UUID): InngangsvilkårDto {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val grunnlag = hentGrunnlag(søknad.fødselsnummer, søknad)
        val vurderinger = hentVurderinger(behandlingId, søknad, grunnlag)
        return InngangsvilkårDto(vurderinger = vurderinger, grunnlag = grunnlag)
    }

    private fun hentGrunnlag(fnr: String,
                             søknad: SøknadsskjemaOvergangsstønad): InngangsvilkårGrunnlagDto {
        val pdlSøker = pdlClient.hentSøker(fnr)
        val pdlBarn = hentPdlBarn(pdlSøker)
        val barneForeldre = hentPdlBarneForeldre(søknad, pdlBarn)
        val medlUnntak = familieIntegrasjonerClient.hentMedlemskapsinfo(ident = fnr)

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = søknad.medlemskap,
                                                 medlUnntak = medlUnntak,
                                                 pdlSøker = pdlSøker)

        val sivilstand = SivilstandMapper.tilDto(sivilstandsdetaljer = søknad.sivilstand,
                                                 pdlSøker = pdlSøker)
        val bosituasjon = BosituasjonMapper.tilDto(søknad.bosituasjon)

        val barnMedSamvær = mapBarnmedSamvær(pdlBarn, barneForeldre, søknad, pdlSøker)

        val sivilstandsplaner = SivilstandsplanerMapper.tilDto(sivilstandsplaner = søknad.sivilstandsplaner)

        return InngangsvilkårGrunnlagDto(medlemskap, sivilstand, bosituasjon, barnMedSamvær, sivilstandsplaner)
    }

    private fun mapBarnmedSamvær(pdlBarn: Map<String, PdlBarn>,
                                 barneForeldre: Map<String, PdlAnnenForelder>,
                                 søknad: SøknadsskjemaOvergangsstønad,
                                 pdlSøker: PdlSøker): List<BarnMedSamværDto> {
        val registergrunnlag = BarnMedSamværMapper.mapRegistergrunnlag(pdlBarn, barneForeldre, søknad, pdlSøker.bostedsadresse)
        return BarnMedSamværMapper.slåSammenBarnMedSamvær(søknadsgrunnlag = BarnMedSamværMapper.mapSøknadsgrunnlag(søknad.barn),
                                                          registergrunnlag = registergrunnlag).sortedByDescending {
            if (it.registergrunnlag != null)
                it.registergrunnlag.fødselsnummer?.let { fødsesnummer -> Fødselsnummer(fødsesnummer).fødselsdato }
            else {
                it.søknadsgrunnlag.fødselTermindato
            }
        }
    }

    private fun hentVurderinger(behandlingId: UUID,
                                søknad: SøknadsskjemaOvergangsstønad,
                                registergrunnlag: InngangsvilkårGrunnlagDto): List<VilkårsvurderingDto> {
        val delvilkårMetadata = DelvilkårMetadata(sivilstandstype = registergrunnlag.sivilstand.registergrunnlag.type)
        return hentEllerOpprettVurderingerForInngangsvilkår(behandlingId, søknad, delvilkårMetadata)
                .map {
                    VilkårsvurderingDto(id = it.id,
                                        behandlingId = it.behandlingId,
                                        resultat = it.resultat,
                                        vilkårType = it.type,
                                        begrunnelse = it.begrunnelse,
                                        unntak = it.unntak,
                                        barnId = it.barnId,
                                        endretAv = it.sporbar.endret.endretAv,
                                        endretTid = it.sporbar.endret.endretTid,
                                        delvilkårsvurderinger = it.delvilkårsvurdering.delvilkårsvurderinger.map { delvurdering ->
                                            DelvilkårsvurderingDto(delvurdering.type,
                                                                   delvurdering.resultat,
                                                                   delvurdering.årsak,
                                                                   delvurdering.begrunnelse)
                                        })
                }
    }

    private fun hentEllerOpprettVurderingerForInngangsvilkår(behandlingId: UUID,
                                                             søknad: SøknadsskjemaOvergangsstønad,
                                                             delvilkårMetadata: DelvilkårMetadata): List<Vilkårsvurdering> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)

        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            return lagredeVilkårsvurderinger
        }

        val nyeVilkårsvurderinger: List<Vilkårsvurdering> = VilkårType.hentInngangsvilkår()
                .filter {
                    lagredeVilkårsvurderinger.find { vurdering -> vurdering.type == it } == null // Sjekk barnId ?
                }
                .map {vilkårType ->
                    if (vilkårType == VilkårType.ALENEOMSORG) {
                        søknad.barn.map {
                            lagNyVilkårsvurdering(vilkårType, søknad, delvilkårMetadata, behandlingId, it.id)
                        }

                    } else {
                        listOf(lagNyVilkårsvurdering(vilkårType, søknad, delvilkårMetadata, behandlingId))
                    }
                }.flatten()

        vilkårsvurderingRepository.insertAll(nyeVilkårsvurderinger)

        return lagredeVilkårsvurderinger + nyeVilkårsvurderinger
    }

    private fun lagNyVilkårsvurdering(it: VilkårType,
                                      søknad: SøknadsskjemaOvergangsstønad,
                                      delvilkårMetadata: DelvilkårMetadata,
                                      behandlingId: UUID,
                                      barnId: UUID? = null): Vilkårsvurdering {
        val delvilkårsvurderinger = it.delvilkår
                .map { delvilkårType ->
                    Delvilkårsvurdering(delvilkårType,
                                        utledDelvilkårResultat(delvilkårType, søknad, delvilkårMetadata))
                }
        return Vilkårsvurdering(behandlingId = behandlingId,
                                type = it,
                                barnId = barnId,
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurderinger))
    }


    fun hentInngangsvilkårSomManglerVurdering(behandlingId: UUID): List<VilkårType> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)
        val inngangsvilkår = VilkårType.hentInngangsvilkår()

        return inngangsvilkår.filter {
            lagredeVilkårsvurderinger.any { vurdering ->
                vurdering.type == it
                && vurdering.resultat == Vilkårsresultat.IKKE_VURDERT
            }
            || lagredeVilkårsvurderinger.none { vurdering -> vurdering.type == it }
        }
    }


    private fun hentMedlInfo(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()


    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()


    private fun hentPdlBarneForeldre(søknad: SøknadsskjemaOvergangsstønad,
                                     barn: Map<String, PdlBarn>): Map<String, PdlAnnenForelder> {
        val barneforeldreFraSøknad =
                søknad.barn.mapNotNull {
                    it.annenForelder?.person?.fødselsnummer
                }

        val barneforeldre = barn.map { it.value.familierelasjoner }
                .flatten()
                .filter { it.relatertPersonsIdent != søknad.fødselsnummer && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .plus(barneforeldreFraSøknad)
                .distinct()
                .let { pdlClient.hentAndreForeldre(it) }
        return barneforeldre
    }

    private fun hentPdlBarn(pdlSøker: PdlSøker): Map<String, PdlBarn> {
        val barn = pdlSøker.familierelasjoner
                .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .let { pdlClient.hentBarn(it) }
                .filter { it.value.fødsel.gjeldende()?.fødselsdato != null }
                .filter { it.value.fødsel.first().fødselsdato!!.plusYears(18).isAfter(LocalDate.now()) }
        return barn
    }


}
