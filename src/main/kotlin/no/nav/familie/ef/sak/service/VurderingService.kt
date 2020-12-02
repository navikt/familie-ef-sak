package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.mapper.AleneomsorgMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.mapper.SivilstandMapper
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class VurderingService(private val behandlingService: BehandlingService,
                       private val pdlClient: PdlClient,
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
                                                                                                 delvurdering.resultat)
                                                                         })
                )
        return vilkårsvurderingRepository.update(nyVilkårsvurdering).id
    }

    private fun validerDelvilkår(vurdering: VilkårsvurderingDto,
                                 vilkårsvurdering: Vilkårsvurdering) {
        val innkommendeDelvurderinger = vurdering.delvilkårsvurderinger.map { it.type }.toSet()
        val lagredeDelvurderinger = vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger.map { it.type }.toSet()

        if (innkommendeDelvurderinger.size != lagredeDelvurderinger.size
            || !innkommendeDelvurderinger.containsAll(lagredeDelvurderinger)) {
            error("Delvilkårstyper motsvarer ikke de som finnes lagrede på vilkåret")
        }
    }

    fun hentInngangsvilkår(behandlingId: UUID): InngangsvilkårDto {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val fnr = søknad.fødselsnummer
        val pdlSøker = pdlClient.hentSøker(fnr)

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = søknad.medlemskap,
                                                 pdlSøker = pdlSøker)
        val sivilstand = SivilstandMapper.tilDto(sivilstandsdetaljer = søknad.sivilstand,
                                                 pdlSøker = pdlSøker)
        val registergrunnlag = InngangsvilkårGrunnlagDto(medlemskap, sivilstand)
        val vurderinger = hentVurderinger(behandlingId, søknad, registergrunnlag)

        return InngangsvilkårDto(vurderinger = vurderinger,
                                 grunnlag = registergrunnlag)
    }

    private fun hentVurderinger(behandlingId: UUID,
                                søknad: SøknadsskjemaOvergangsstønad,
                                registerGrunnlag: InngangsvilkårGrunnlagDto): List<VilkårsvurderingDto> {
        return hentEllerOpprettVurderingerForInngangsvilkår(behandlingId, søknad, registerGrunnlag )
                .map {
                    VilkårsvurderingDto(id = it.id,
                                        behandlingId = it.behandlingId,
                                        resultat = it.resultat,
                                        vilkårType = it.type,
                                        begrunnelse = it.begrunnelse,
                                        unntak = it.unntak,
                                        endretAv = it.sporbar.endret.endretAv,
                                        endretTid = it.sporbar.endret.endretTid,
                                        delvilkårsvurderinger = it.delvilkårsvurdering.delvilkårsvurderinger.map { delvurdering ->
                                            DelvilkårsvurderingDto(delvurdering.type,
                                                                   delvurdering.resultat)
                                        })
                }
    }

    private fun hentEllerOpprettVurderingerForInngangsvilkår(behandlingId: UUID,
                                                             søknad: SøknadsskjemaOvergangsstønad,
                                                             registerGrunnlag: InngangsvilkårGrunnlagDto)
    : List<Vilkårsvurdering> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)

        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            return lagredeVilkårsvurderinger
        }

        val nyeVilkårsvurderinger = VilkårType.hentInngangsvilkår()
                .filter {
                    lagredeVilkårsvurderinger.find { vurdering -> vurdering.type == it } == null
                }
                .map {
                    val delvilkårsvurderinger = it.delvilkår
                            .filter { delvilkårType -> erDelvilkårAktueltForSøknaden(delvilkårType, søknad, registerGrunnlag) }
                            .map { delvilkårType -> Delvilkårsvurdering(delvilkårType) }
                    Vilkårsvurdering(behandlingId = behandlingId,
                                     type = it,
                                     delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurderinger))
                }

        vilkårsvurderingRepository.insertAll(nyeVilkårsvurderinger)

        return lagredeVilkårsvurderinger + nyeVilkårsvurderinger
    }

    /**
     * Filtrerer bort delvikår som ikke skall vurderes iht data i søknaden
     */
    private fun erDelvilkårAktueltForSøknaden(it: DelvilkårType,
                                              søknad: SøknadsskjemaOvergangsstønad,
                                              register: InngangsvilkårGrunnlagDto): Boolean {

        val sivilstandType = register.sivilstand.registergrunnlag.type

        return when (it) {
            DelvilkårType.DOKUMENTERT_EKTESKAP -> (sivilstandType == Sivilstandstype.UGIFT  || sivilstandType == Sivilstandstype.UOPPGITT) && søknad.sivilstand.erUformeltGift == true
            DelvilkårType.DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE ->  (sivilstandType == Sivilstandstype.UGIFT || sivilstandType == Sivilstandstype.UOPPGITT) && søknad.sivilstand.erUformeltSeparertEllerSkilt == true
            DelvilkårType.SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON ->  (sivilstandType == Sivilstandstype.GIFT  ||  sivilstandType == Sivilstandstype.REGISTRERT_PARTNER ) && søknad.sivilstand.søktOmSkilsmisseSeparasjon == true
            DelvilkårType.SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING -> (sivilstandType == Sivilstandstype.SEPARERT || sivilstandType == Sivilstandstype.SEPARERT_PARTNER)
            DelvilkårType.KRAV_SIVILSTAND -> true

            else -> true
        }
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

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()

    fun vurderAleneomsorg(behandlingId: UUID): Aleneomsorg {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val fnrSøker = "" //TODO
        val pdlSøker = pdlClient.hentSøker(fnrSøker)


        val barn = pdlSøker.familierelasjoner
                .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .let { pdlClient.hentBarn(it) }
                .filter { it.value.fødsel.firstOrNull()?.fødselsdato != null }
                .filter { it.value.fødsel.first().fødselsdato!!.plusYears(18).isAfter(LocalDate.now()) }

        val barneforeldreFraSøknad =
                søknad.barn.mapNotNull {
                    it.annenForelder?.person?.fødselsnummer
                }

        val barneforeldre = barn.map { it.value.familierelasjoner }
                .flatten()
                .filter { it.relatertPersonsIdent != fnrSøker && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .plus(barneforeldreFraSøknad)
                .distinct()
                .let { pdlClient.hentAndreForeldre(it) }

        return AleneomsorgMapper.tilDto(pdlSøker,
                                        barn,
                                        barneforeldre,
                                        søknad)
    }


}
