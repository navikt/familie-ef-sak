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
import no.nav.familie.ef.sak.vurdering.utledDelvilkårResultat
import no.nav.familie.ef.sak.vurdering.validerDelvilkår
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

    fun hentInngangsvilkår(behandlingId: UUID): InngangsvilkårDto {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val fnr = søknad.fødselsnummer
        val pdlSøker = pdlClient.hentSøker(fnr)

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = søknad.medlemskap,
                                                 personIdent = fnr,
                                                 pdlSøker = pdlSøker)
        val sivilstand = SivilstandMapper.tilDto(sivilstandsdetaljer = søknad.sivilstand,
                                                 pdlSøker = pdlSøker)
        val registergrunnlag = InngangsvilkårGrunnlagDto(medlemskap, sivilstand)
        val delvilkårMetadata = DelvilkårMetadata(sivilstandstype = registergrunnlag.sivilstand.registergrunnlag.type)
        val vurderinger = hentVurderinger(behandlingId, søknad, delvilkårMetadata)

        return InngangsvilkårDto(vurderinger = vurderinger,
                                 grunnlag = registergrunnlag)
    }

    private fun hentVurderinger(behandlingId: UUID,
                                søknad: SøknadsskjemaOvergangsstønad,
                                delvilkårMetadata: DelvilkårMetadata): List<VilkårsvurderingDto> {
        return hentEllerOpprettVurderingerForInngangsvilkår(behandlingId, søknad, delvilkårMetadata)
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
                                                             delvilkårMetadata: DelvilkårMetadata): List<Vilkårsvurdering> {
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
                            .map { delvilkårType ->
                                Delvilkårsvurdering(delvilkårType,
                                                    utledDelvilkårResultat(delvilkårType, søknad, delvilkårMetadata))
                            }
                    Vilkårsvurdering(behandlingId = behandlingId,
                                     type = it,
                                     delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurderinger))
                }

        vilkårsvurderingRepository.insertAll(nyeVilkårsvurderinger)

        return lagredeVilkårsvurderinger + nyeVilkårsvurderinger
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
